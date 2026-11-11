package com.hartmannstudios.direct

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Base64
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Direct-download (GitHub / charleshartmann.com) billing for Hartmann Studios apps.
 * This file is only compiled into the DIRECT build (-Pstore=direct). The Play build compiles
 * src/play/java/.../DirectBilling.kt instead: a no-op stub with the same API.
 *
 * Purchases go through Stripe Checkout, created by the shared Hartmann Studios billing Worker
 * (/v2/checkout). Checkout opens in a full-screen WebView; when Stripe sends the buyer back to
 * /v2/return?session_id=..., the app asks the Worker for the license (/v2/entitlement) and caches
 * the unlocked product keys. "Restore purchases" asks again by email. No Stripe key is in the app.
 */
object DirectBilling {
    const val isDirect = true
    const val BASE_URL = "https://livecaptionn-premium.charles-h-hartmann1.workers.dev"
    const val DOWNLOAD_PAGE = "https://charleshartmann.com/apps/"
    private const val TAG = "DirectBilling"
    private const val OFFLINE_GRACE_S = 14L * 24 * 3600

    @Volatile private var appKey = ""
    private var prefs: SharedPreferences? = null
    private val main = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    /** HTTP status of the last license-server call on this thread (0 = no answer). 429 = the server's per-email lookup limit. */
    private val lastHttpCode = ThreadLocal<Int>()
    /** The server's own sentence when it answers code "plan_limit" (its daily plan limit is hit); shown on Restore. */
    private val lastServerNotice = ThreadLocal<String?>()

    /** Call once from Application.onCreate (or the first Activity). [app] is the app key, e.g. "nightsay". */
    @JvmStatic
    fun init(context: Context, app: String) {
        appKey = app
        if (prefs == null) prefs = context.applicationContext.getSharedPreferences("hs_direct_billing", Context.MODE_PRIVATE)
        expireIfStale()
        val p = prefs ?: return
        val email = p.getString("email", null) ?: return
        if (System.currentTimeMillis() / 1000 >= p.getLong("revalidate_after", 0)) {
            Thread { fetchByEmail(email) }.start()
        }
    }

    /** Product keys this buyer owns (catalog keys, e.g. "nightsay_premium:monthly"). */
    @JvmStatic
    fun entitlements(): Set<String> {
        val raw = prefs?.getString("entitlements", null) ?: return emptySet()
        return try { val a = JSONArray(raw); (0 until a.length()).map { a.getString(it) }.toSet() } catch (e: Exception) { emptySet() }
    }

    /** True when the buyer owns any of [keys]; with no keys, true when the buyer owns anything. */
    @JvmStatic
    fun isEntitled(vararg keys: String): Boolean {
        val e = entitlements()
        return if (keys.isEmpty()) e.isNotEmpty() else keys.any { it in e }
    }

    @JvmStatic fun email(): String? = prefs?.getString("email", null)
    @JvmStatic fun addListener(l: () -> Unit) { listeners.add(l) }
    @JvmStatic fun removeListener(l: () -> Unit) { listeners.remove(l) }

    /** Starts a Stripe Checkout purchase of [productKey]. [onResult] runs on the main thread (true = unlocked). */
    @JvmStatic
    @JvmOverloads
    fun purchase(activity: Activity, productKey: String, onResult: ((Boolean) -> Unit)? = null) {
        askEmail(activity, "Checkout with Stripe", "Your receipt and license are tied to this email. Use the same email to restore on another phone.") { email ->
            val dlg = progress(activity, "Opening secure checkout…")
            Thread {
                val res = post("/v2/checkout", JSONObject().put("app", appKey).put("product", productKey).put("email", email))
                main.post {
                    dlg.dismiss()
                    val url = res?.optString("checkoutUrl").orEmpty()
                    if (url.isBlank()) {
                        toast(activity, res?.optString("error")?.takeIf { it.isNotBlank() } ?: "Couldn't reach checkout. Check your connection and try again.")
                        onResult?.invoke(false)
                    } else {
                        Log.i(TAG, "checkout opened app=$appKey product=$productKey session=" + Regex("cs_(live|test)_[A-Za-z0-9]+").find(url)?.value)
                        showCheckout(activity, url, onResult)
                    }
                }
            }.start()
        }
    }

    /** Looks up purchases made with an email (another phone, reinstall). */
    @JvmStatic
    @JvmOverloads
    fun restore(activity: Activity, onResult: ((Boolean) -> Unit)? = null) {
        askEmail(activity, "Restore purchases", "Enter the email you used at checkout.") { email ->
            val dlg = progress(activity, "Checking your purchases…")
            Thread {
                lastServerNotice.set(null)
                val ok = fetchByEmail(email)
                val limited = !ok && lastHttpCode.get() == 429
                val notice = if (ok) null else lastServerNotice.get()
                main.post {
                    dlg.dismiss()
                    val owned = isEntitled()
                    toast(activity, if (limited) "Too many restore attempts — try again later." else if (notice != null) notice else if (!ok) "Couldn't reach the license server. Try again." else if (owned) "Purchases restored. Thank you!" else "No purchases found for $email.")
                    onResult?.invoke(ok && owned)
                }
            }.start()
        }
    }

    /** Opens Stripe's customer portal (cancel / change a subscription). */
    @JvmStatic
    fun manageSubscription(activity: Activity) {
        Thread {
            val url = get("/v2/portal")?.optString("portalUrl").orEmpty()
            main.post {
                if (url.isBlank()) toast(activity, "Couldn't open subscription management.")
                else try { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { toast(activity, url) }
            }
        }.start()
    }

    /** Opens the Hartmann Studios download page (replaces Play Store links in direct builds). */
    @JvmStatic
    fun openDownloadPage(context: Context) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOAD_PAGE)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { }
    }

    // ── internals ──
    @SuppressLint("SetJavaScriptEnabled")
    private fun showCheckout(activity: Activity, url: String, onResult: ((Boolean) -> Unit)?) {
        val d = Dialog(activity, android.R.style.Theme_DeviceDefault_Light_NoActionBar)
        val root = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        val bar = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(16, 8, 16, 8) }
        val title = TextView(activity).apply { text = "Secure checkout (Stripe)"; setTextColor(Color.DKGRAY); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
        val close = Button(activity).apply { text = "Close" }
        bar.addView(title); bar.addView(close)
        val web = WebView(activity)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        var finished = false
        fun finish(sessionId: String?) {
            if (finished) return
            finished = true
            try { d.dismiss() } catch (e: Exception) { }
            if (sessionId.isNullOrBlank()) { onResult?.invoke(false); return }
            val dlg = progress(activity, "Unlocking…")
            Thread {
                var ok = false
                for (i in 0 until 10) {
                    if (fetchBySession(sessionId)) { ok = true; break }
                    Thread.sleep(2000)
                }
                main.post {
                    dlg.dismiss()
                    toast(activity, if (ok) "Unlocked. Thank you!" else "Payment received; your unlock is still on its way. Use Restore purchases in a minute.")
                    onResult?.invoke(ok)
                }
            }.start()
        }
        fun handle(u: Uri): Boolean {
            val s = u.toString()
            if (s.startsWith("$BASE_URL/v2/return") || (u.scheme ?: "").startsWith("hs.")) {
                val cancel = u.getQueryParameter("cancel") == "1"
                Log.i(TAG, "checkout returned cancel=$cancel")
                finish(if (cancel) null else u.getQueryParameter("session_id"))
                return true
            }
            if (u.scheme == "http" || u.scheme == "https") return false
            try { activity.startActivity(Intent(Intent.ACTION_VIEW, u)) } catch (e: Exception) { }
            return true
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = handle(request.url)
            @Deprecated("old API")
            override fun shouldOverrideUrlLoading(view: WebView, u: String): Boolean = handle(Uri.parse(u))
        }
        close.setOnClickListener { finish(null) }
        d.setOnCancelListener { finish(null) }
        root.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        d.setContentView(root)
        d.show()
        web.loadUrl(url)
    }

    private fun fetchBySession(sessionId: String): Boolean {
        val r = post("/v2/entitlement", JSONObject().put("app", appKey).put("sessionId", sessionId)) ?: return false
        if (!r.has("entitlements")) return false
        save(r)
        return r.optJSONArray("entitlements")?.length() ?: 0 > 0
    }

    private fun fetchByEmail(email: String): Boolean {
        val r = post("/v2/entitlement", JSONObject().put("app", appKey).put("email", email)) ?: return false
        if (!r.has("entitlements")) { lastServerNotice.set(if (r.optString("code") == "plan_limit") r.optString("error").takeIf { it.isNotBlank() } else null); return false }
        save(r)
        return true
    }

    private fun save(r: JSONObject) {
        val p = prefs ?: return
        val token = r.optString("licenseToken")
        p.edit()
            .putString("email", r.optString("email").ifBlank { p.getString("email", null) ?: "" })
            .putString("entitlements", (r.optJSONArray("entitlements") ?: JSONArray()).toString())
            .putString("license", token)
            .putLong("license_exp", tokenExp(token))
            .putLong("revalidate_after", r.optLong("revalidateAfter", 0))
            .apply()
        Log.i(TAG, "license saved app=$appKey entitlements=" + r.optJSONArray("entitlements"))
        main.post { for (l in listeners) try { l() } catch (e: Exception) { } }
    }

    private fun tokenExp(token: String): Long = try {
        val body = token.substringBefore('.')
        JSONObject(String(Base64.decode(body, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))).optLong("exp", 0)
    } catch (e: Exception) { 0 }

    private fun expireIfStale() {
        val p = prefs ?: return
        val exp = p.getLong("license_exp", 0)
        if (exp > 0 && System.currentTimeMillis() / 1000 > exp + OFFLINE_GRACE_S) p.edit().remove("entitlements").apply()
    }

    private fun askEmail(activity: Activity, title: String, msg: String, then: (String) -> Unit) {
        val input = EditText(activity).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            hint = "you@example.com"
            setText(email().orEmpty())
        }
        val box = LinearLayout(activity).apply { setPadding(48, 16, 48, 0); addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)) }
        AlertDialog.Builder(activity).setTitle(title).setMessage(msg).setView(box)
            .setPositiveButton("Continue") { _, _ ->
                val e = input.text.toString().trim().lowercase()
                if (Patterns.EMAIL_ADDRESS.matcher(e).matches()) { prefs?.edit()?.putString("email", e)?.apply(); then(e) }
                else toast(activity, "Please enter a valid email.")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun progress(activity: Activity, msg: String): AlertDialog =
        AlertDialog.Builder(activity).setMessage(msg).setCancelable(false).show()

    private fun toast(c: Context, m: String) = Toast.makeText(c, m, Toast.LENGTH_LONG).show()

    private fun get(path: String): JSONObject? = http("GET", path, null)
    private fun post(path: String, body: JSONObject): JSONObject? = http("POST", path, body)
    private fun http(method: String, path: String, body: JSONObject?): JSONObject? {
        var c: HttpURLConnection? = null
        lastHttpCode.set(0)
        return try {
            c = (URL(BASE_URL + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method; connectTimeout = 15000; readTimeout = 20000
                setRequestProperty("Accept", "application/json")
                if (body != null) { doOutput = true; setRequestProperty("Content-Type", "application/json") }
            }
            if (body != null) c.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = c.responseCode
            lastHttpCode.set(code)
            val text = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) Log.w(TAG, "$method $path -> $code ${text.take(200)}")
            JSONObject(text.ifBlank { "{}" })
        } catch (e: Exception) {
            Log.w(TAG, "$method $path failed: $e"); null
        } finally { c?.disconnect() }
    }
}