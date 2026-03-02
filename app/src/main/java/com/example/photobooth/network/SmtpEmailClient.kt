package com.example.photobooth.network

import android.content.Context
import com.example.photobooth.settings.SmtpSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress
import java.util.Date
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class SmtpEmailClient(
    private val context: Context,
    private val settings: SmtpSettings,
) {
    data class TestResult(val success: Boolean, val message: String)

    suspend fun testConnection(): TestResult = withContext(Dispatchers.IO) {
        return@withContext try {
            InetAddress.getByName(settings.host)
            TestResult(true, "DNS lookup successful")
        } catch (e: Exception) {
            TestResult(false, "DNS lookup failed: ${e.message}")
        }
    }

    suspend fun sendPhotoEmail(
        to: String,
        subject: String,
        body: String,
        attachment: File,
    ) = withContext(Dispatchers.IO) {
        val props = java.util.Properties().apply {
            put("mail.smtp.host", settings.host)
            put("mail.smtp.port", settings.port.toString())
            if (settings.useSslTls) {
                put("mail.smtp.starttls.enable", "true")
            }
            put("mail.smtp.auth", "true")
        }

        val session = Session.getInstance(props, object : javax.mail.Authenticator() {
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

