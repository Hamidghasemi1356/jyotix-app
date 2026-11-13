package com.jyotix.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jyotix.app.JyotixApp
import com.jyotix.app.R
import com.jyotix.app.ui.components.JyotixButton
import com.jyotix.app.ui.components.JyotixPanel
import com.jyotix.app.ui.components.JyotixTopBar
import com.jyotix.app.ui.theme.ErrorRed
import com.jyotix.app.ui.theme.GoldenPrimary
import com.jyotix.app.ui.theme.OffWhite
import kotlinx.coroutines.launch

/**
 * Phone number entry → SMS code verification → sign-in.
 *
 * Uses [com.jyotix.app.core.auth.AuthManager.sendVerificationCode] and
 * [com.jyotix.app.core.auth.AuthManager.verifyCode] under the hood.
 */
@Composable
fun LoginScreen(
    onVerified: () -> Unit,
    onBack: () -> Unit,
    onRegister: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as JyotixApp
    val authManager = app.authManager
    val scope = rememberCoroutineScope()

    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.jyotix.app.ui.theme.NavyBackground)
            .verticalScroll(rememberScrollState())
    ) {
        JyotixTopBar(title = stringResourceFa(R.string.login), onBack = onBack)

        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (verificationId == null) {
                // Step 1: phone number entry
                Text(
                    text = stringResourceFa(R.string.enter_phone_prompt),
                    color = OffWhite,
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; error = null },
                    label = { Text(stringResourceFa(R.string.phone_number), color = OffWhite) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle = TextStyle(color = OffWhite, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(text = error ?: "", color = ErrorRed, fontSize = 13.sp)
                }
                JyotixButton(
                    text = stringResourceFa(R.string.send_code),
                    onClick = {
                        if (phone.isBlank()) {
                            error = "شماره تلفن را وارد کنید."
                            return@JyotixButton
                        }
                        sending = true
                        status = "در حال ارسال کد..."
                        val activity = context as? android.app.Activity
                        if (activity == null) {
                            error = "ارسال کد ممکن نیست (Activity در دسترس نیست)."
                            sending = false
                            return@JyotixButton
                        }
                        authManager.sendVerificationCode(activity, phone) { vid, err ->
                            sending = false
                            if (err != null) {
                                error = err
                                status = ""
                            } else if (vid != null) {
                                if (vid == "AUTO") {
                                    // Auto-verification succeeded — proceed
                                    scope.launch { onVerified() }
                                } else {
                                    verificationId = vid
                                    status = "کد ارسال شد."
                                }
                            }
                        }
                    },
                    enabled = !sending,
                    modifier = Modifier.fillMaxWidth()
                )
                if (status.isNotEmpty()) {
                    Text(text = status, color = GoldenPrimary, fontSize = 13.sp)
                }
            } else {
                // Step 2: verification code entry
                Text(
                    text = stringResourceFa(R.string.enter_code_prompt).format(phone),
                    color = OffWhite,
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter { c -> c.isDigit() }.take(6); error = null },
                    label = { Text(stringResourceFa(R.string.verification_code), color = OffWhite) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    textStyle = TextStyle(color = OffWhite, fontSize = 18.sp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(text = error ?: "", color = ErrorRed, fontSize = 13.sp)
                }
                JyotixButton(
                    text = stringResourceFa(R.string.verify),
                    onClick = {
                        if (code.length != 6) {
                            error = "کد باید ۶ رقم باشد."
                            return@JyotixButton
                        }
                        verifying = true
                        scope.launch {
                            val (ok, err) = authManager.verifyCode(code)
                            verifying = false
                            if (ok) {
                                onVerified()
                            } else {
                                error = err ?: "تأیید ناموفق."
                            }
                        }
                    },
                    enabled = !verifying,
                    modifier = Modifier.fillMaxWidth()
                )
                JyotixButton(
                    text = "ارسال مجدد کد",
                    onClick = {
                        verificationId = null
                        code = ""
                        status = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit
) {
    // Registration flow is the same phone-OTP flow as login (Firebase Phone Auth).
    // The user provides a phone number; a new local UserEntity is created on first verification.
    LoginScreen(onVerified = onRegistered, onBack = onBack, onRegister = onRegister)
}

@Composable
private fun stringResourceFa(id: Int): String = androidx.compose.ui.res.stringResource(id)
