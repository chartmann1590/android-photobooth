package com.charles.photobooth.network

import android.content.Context
import com.charles.photobooth.settings.SmtpSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.ConnectException
import java.util.Date
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.AuthenticationFailedException
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMultipart
import javax.net.ssl.SSLHandshakeException

class SmtpEmailClient(
    private val context: Context,
    private val settings: SmtpSettings,
) {

    suspend fun testConnection(): NetworkTestResult = withContext(Dispatchers.IO) {
        if (settings.host.isBlank()) {
            return@withContext NetworkTestResult(false, "SMTP host not set")
        }
        val session = Session.getInstance(buildSmtpProperties(), object : javax.mail.Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(settings.username, settings.password)
            }
        })
        try {
            val transport = session.getTransport("smtp")
            transport.connect(settings.host, settings.port, settings.username, settings.password)
            transport.close()
            NetworkTestResult(true, "Connected to ${settings.host}:${settings.port}")
        } catch (e: AuthenticationFailedException) {
            NetworkTestResult(false, "Authentication failed — check username/password")
        } catch (e: MessagingException) {
            // JavaMail wraps the underlying transport problem; inspect the cause to give
            // a more actionable message than the generic "Could not connect" string.
            val cause = e.cause
            when {
                cause is SSLHandshakeException ->
                    NetworkTestResult(false, "TLS handshake failed — wrong port or TLS setting?")
                cause is ConnectException ->
                    NetworkTestResult(false, "Could not reach ${settings.host} on port ${settings.port}")
                e.message?.contains("Couldn't connect", ignoreCase = true) == true ->
                    NetworkTestResult(false, "Could not reach ${settings.host} on port ${settings.port}")
                else -> NetworkTestResult(false, e.message ?: "SMTP test failed")
            }
        } catch (e: Exception) {
            NetworkTestResult(false, e.message ?: "SMTP test failed")
        }
    }

    private fun buildSmtpProperties(): Properties = Properties().apply {
        put("mail.smtp.host", settings.host)
        put("mail.smtp.port", settings.port.toString())
        put("mail.smtp.auth", "true")
        put("mail.smtp.connectiontimeout", "15000")
        put("mail.smtp.timeout", "30000")

        if (settings.port == 465) {
            put("mail.smtp.ssl.enable", "true")
            put("mail.smtp.ssl.checkserveridentity", "true")
        } else if (settings.useSslTls) {
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.ssl.checkserveridentity", "true")
        }
    }

    suspend fun sendPhotoEmail(
        to: String,
        subject: String,
        body: String,
        attachment: File,
    ) = withContext(Dispatchers.IO) {
        if (!attachment.exists()) {
            throw IllegalStateException("Attachment file not found: ${attachment.name}")
        }

        val session = Session.getInstance(buildSmtpProperties(), object : javax.mail.Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(settings.username, settings.password)
            }
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(settings.fromAddress, settings.fromName))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                setSubject(subject)
                sentDate = Date()
            }

            val textPart = MimeBodyPart().apply {
                setText(body)
            }

            val filePart = MimeBodyPart().apply {
                dataHandler = DataHandler(FileDataSource(attachment))
                fileName = attachment.name
            }

            val multipart = MimeMultipart().apply {
                addBodyPart(textPart)
                addBodyPart(filePart)
            }

            message.setContent(multipart)
            Transport.send(message)
        } catch (e: MessagingException) {
            throw IllegalStateException("Failed to send email: ${e.message}", e)
        }
    }
}
