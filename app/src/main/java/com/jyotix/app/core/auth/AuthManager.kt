package com.jyotix.app.core.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.jyotix.app.BuildConfig
import com.jyotix.app.core.db.JyotixDatabase
import com.jyotix.app.core.db.entities.UserEntity
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Manages phone-based authentication via Firebase Phone Auth.
 *
 * Flow:
 *   1. [sendVerificationCode] — sends an SMS OTP to the given phone number.
 *   2. User enters the code in the UI.
 *   3. [verifyCode] — exchanges the code for a Firebase credential and signs in.
 *
 * On successful sign-in, a local [UserEntity] is created (or updated) in the
 * Room database.
 *
 * ## Required setup
 *
 * 1. Add `app/google-services.json` (downloaded from Firebase Console).
 * 2. Enable Phone Authentication in the Firebase Console under
 *    Authentication → Sign-in method.
 * 3. For testing on physical devices, add SHA-1 / SHA-256 fingerprints of
 *    your debug keystore to Firebase project settings.
 * 4. For Iranian phone numbers, use international format (+98…).
 *
 * If Firebase Phone Auth is unavailable (e.g., during offline testing), the
 * class falls back to a local-only profile keyed by the phone number — useful
 * for development without network access.
 */
class AuthManager(private val context: Context) {

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: JyotixDatabase by lazy { JyotixDatabase.get(context) }

    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    @Volatile private var lastError: String? = null

    /** Current Firebase user phone, or null if not signed in. */
    val currentPhone: String? get() = firebaseAuth.currentUser?.phoneNumber

    /** Whether the user is currently signed in via Firebase. */
    val isSignedIn: Boolean get() = firebaseAuth.currentUser != null

    /**
     * Send an SMS verification code to [phoneNumber] (E.164 format, e.g. +989123456789).
     *
     * Calls [callback] with the verification ID on success or with null and an error message on failure.
     */
    fun sendVerificationCode(
        activity: Activity,
        phoneNumber: String,
        callback: (verificationId: String?, error: String?) -> Unit
    ) {
        val normalizedPhone = normalizePhone(phoneNumber)
        if (!BuildConfig.FIREBASE_PHONE_AUTH_ENABLED.toBoolean()) {
            // Offline / development mode — synthesize a verification ID and log to console
            Log.w(TAG, "Firebase Phone Auth disabled; using offline mode.")
            val fakeId = "offline-${System.currentTimeMillis()}"
            storedVerificationId = fakeId
            callback(fakeId, null)
            return
        }
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(normalizedPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Auto-retrieved: sign in directly
                    signInWithCredential(credential) { success, err ->
                        if (success) callback("AUTO", null)
                        else callback(null, err)
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e(TAG, "Phone verification failed", e)
                    lastError = e.message ?: "Verification failed"
                    callback(null, lastError)
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    storedVerificationId = verificationId
                    resendToken = token
                    callback(verificationId, null)
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /**
     * Verify the SMS code entered by the user.
     *
     * Returns true on success (and persists the local user profile), false on
     * failure (with [error] set).
     */
    suspend fun verifyCode(code: String): Pair<Boolean, String?> {
        val vId = storedVerificationId ?: return false to "No verification in progress."
        // Offline mode: any 6-digit code accepted
        if (vId.startsWith("offline-")) {
            if (code.length == 6 && code.all { it.isDigit() }) {
                val phone = "offline-${vId.removePrefix("offline-")}"
                saveUser(phone)
                return true to null
            }
            return false to "Invalid offline code."
        }
        return try {
            val credential = PhoneAuthProvider.getCredential(vId, code)
            signInWithCredentialSuspending(credential)
            saveUser(firebaseAuth.currentUser?.phoneNumber ?: "unknown")
            true to null
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            false to "Invalid verification code."
        } catch (e: Throwable) {
            false to (e.message ?: "Unknown auth error")
        }
    }

    private suspend fun signInWithCredentialSuspending(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential).await()
    }

    private fun signInWithCredential(
        credential: PhoneAuthCredential,
        completion: (Boolean, String?) -> Unit
    ) {
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveUserAsync(firebaseAuth.currentUser?.phoneNumber ?: "unknown")
                    completion(true, null)
                } else {
                    completion(false, task.exception?.message ?: "Sign-in failed")
                }
            }
    }

    private fun saveUserAsync(phone: String) {
        Thread { runCatching { saveUser(phone) } }.start()
    }

    private suspend fun saveUser(phone: String) {
        db.userDao().upsert(
            UserEntity(
                phone = phone,
                displayName = "User-$phone"
            )
        )
    }

    /** Sign out of Firebase and clear local state. */
    fun signOut() {
        firebaseAuth.signOut()
        storedVerificationId = null
        resendToken = null
    }

    /** Normalize Iranian phone numbers to E.164 (+98…). */
    private fun normalizePhone(raw: String): String {
        var s = raw.trim().replace("\\s".toRegex(), "")
        if (s.startsWith("00")) s = "+" + s.removePrefix("00")
        if (!s.startsWith("+")) {
            when {
                s.startsWith("0") -> s = "+98" + s.removePrefix("0")
                s.startsWith("9") && s.length == 10 -> s = "+98$s"
                else -> s = "+$s"
            }
        }
        return s
    }

    companion object {
        private const val TAG = "AuthManager"
    }
}
