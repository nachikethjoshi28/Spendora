package com.example.dailyexpensetracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class LoginMode {
    EMAIL_SIGNIN, EMAIL_SIGNUP, PHONE
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onEmailLogin: (String, String) -> Unit,
    onEmailSignUp: (String, String) -> Unit,
    onPhoneLogin: (String) -> Unit,
    onVerifyOtp: (String) -> Unit,
    isOtpSent: Boolean = false
) {
    var mode by remember { mutableStateOf(LoginMode.EMAIL_SIGNIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when(mode) {
                    LoginMode.EMAIL_SIGNIN -> "Welcome Back"
                    LoginMode.EMAIL_SIGNUP -> "Create Account"
                    LoginMode.PHONE -> if (isOtpSent) "Verify OTP" else "Phone Login"
                },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when(mode) {
                    LoginMode.EMAIL_SIGNIN -> "Sign in to continue tracking your expenses"
                    LoginMode.EMAIL_SIGNUP -> "Join Spendora to track your expenses"
                    LoginMode.PHONE -> if (isOtpSent) "Enter the code sent to your phone" else "Sign in with your phone number"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (mode == LoginMode.PHONE) {
                if (!isOtpSent) {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number (with country code)") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        placeholder = { Text("+1 1234567890") },
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { otp = it },
                        label = { Text("6-Digit OTP") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            } else {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    when(mode) {
                        LoginMode.EMAIL_SIGNIN -> onEmailLogin(email, password)
                        LoginMode.EMAIL_SIGNUP -> onEmailSignUp(email, password)
                        LoginMode.PHONE -> {
                            if (isOtpSent) onVerifyOtp(otp)
                            else onPhoneLogin(phoneNumber)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = when(mode) {
                        LoginMode.EMAIL_SIGNIN -> "Sign In"
                        LoginMode.EMAIL_SIGNUP -> "Sign Up"
                        LoginMode.PHONE -> if (isOtpSent) "Verify OTP" else "Send OTP"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = {
                mode = if (mode == LoginMode.PHONE) LoginMode.EMAIL_SIGNIN 
                       else LoginMode.PHONE
            }) {
                Text(
                    text = if (mode == LoginMode.PHONE) "Switch to Email Login" else "Sign in with Phone Number",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (mode != LoginMode.PHONE) {
                TextButton(onClick = {
                    mode = if (mode == LoginMode.EMAIL_SIGNIN) LoginMode.EMAIL_SIGNUP else LoginMode.EMAIL_SIGNIN
                }) {
                    Text(
                        text = if (mode == LoginMode.EMAIL_SIGNIN) "Don't have an account? Sign Up" else "Already have an account? Sign In",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(" OR ", modifier = Modifier.padding(horizontal = 8.dp), color = Color.Gray)
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onLoginSuccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
            ) {
                Text("Continue with Google", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "By continuing, you agree to our Terms of Service and Privacy Policy.",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
