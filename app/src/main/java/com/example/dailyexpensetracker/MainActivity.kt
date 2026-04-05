package com.example.dailyexpensetracker

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dailyexpensetracker.data.ExpenseRepository
import com.example.dailyexpensetracker.data.local.AppDatabase
import com.example.dailyexpensetracker.ui.screens.ExpenseTrackerScreen
import com.example.dailyexpensetracker.ui.screens.LoginScreen
import com.example.dailyexpensetracker.ui.screens.RegistrationScreen
import com.example.dailyexpensetracker.ui.theme.SpendoraTheme
import com.example.dailyexpensetracker.ui.viewmodel.ExpenseViewModel
import com.example.dailyexpensetracker.ui.viewmodel.ExpenseViewModelFactory
import com.example.dailyexpensetracker.ui.viewmodel.ProfileUiState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = ExpenseRepository(db.transactionDao(), db.categoryDao(), db.accountDao())

        setContent {
            SpendoraTheme {
                val viewModel: ExpenseViewModel = viewModel(
                    factory = ExpenseViewModelFactory(repository)
                )
                
                val profileState by viewModel.profileState.collectAsState()
                var firebaseUser by remember { mutableStateOf(Firebase.auth.currentUser) }
                var isOtpSent by remember { mutableStateOf(false) }

                // Listen for Auth changes and immediately sync with ViewModel
                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener { auth ->
                        firebaseUser = auth.currentUser
                        val uid = auth.currentUser?.uid
                        viewModel.setUid(uid)
                        // Proactively ensure profile exists if we have a user
                        if (uid != null) {
                            viewModel.signIn(
                                uid, 
                                auth.currentUser?.email ?: auth.currentUser?.phoneNumber ?: "", 
                                auth.currentUser?.displayName
                            )
                        }
                    }
                    Firebase.auth.addAuthStateListener(listener)
                    onDispose {
                        Firebase.auth.removeAuthStateListener(listener)
                    }
                }

                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(this, gso)

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)!!
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        Firebase.auth.signInWithCredential(credential)
                    } catch (e: ApiException) {
                        Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }

                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        Firebase.auth.signInWithCredential(credential)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    isOtpSent = false
                                } else {
                                    Toast.makeText(this@MainActivity, "Verification failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Log.e("MainActivity", "Phone verification failed", e)
                        Toast.makeText(this@MainActivity, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                        isOtpSent = false
                    }

                    override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                        storedVerificationId = verificationId
                        resendToken = token
                        isOtpSent = true
                        Toast.makeText(this@MainActivity, "OTP Sent", Toast.LENGTH_SHORT).show()
                    }
                }

                when (val state = profileState) {
                    is ProfileUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is ProfileUiState.LoggedOut -> {
                        LoginScreen(
                            onLoginSuccess = {
                                launcher.launch(googleSignInClient.signInIntent)
                            },
                            onEmailLogin = { email, password ->
                                if (email.isBlank() || password.isBlank()) {
                                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                                    return@LoginScreen
                                }
                                Firebase.auth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        if (!task.isSuccessful) {
                                            Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            },
                            onEmailSignUp = { email, password ->
                                if (email.isBlank() || password.isBlank()) {
                                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                                    return@LoginScreen
                                }
                                Firebase.auth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        if (!task.isSuccessful) {
                                            Toast.makeText(this, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            },
                            onPhoneLogin = { phoneNumber ->
                                if (phoneNumber.isBlank()) {
                                    Toast.makeText(this, "Please enter phone number", Toast.LENGTH_SHORT).show()
                                    return@LoginScreen
                                }
                                val options = PhoneAuthOptions.newBuilder(Firebase.auth)
                                    .setPhoneNumber(phoneNumber)
                                    .setTimeout(60L, TimeUnit.SECONDS)
                                    .setActivity(this)
                                    .setCallbacks(callbacks)
                                    .build()
                                PhoneAuthProvider.verifyPhoneNumber(options)
                            },
                            onVerifyOtp = { otp ->
                                if (otp.isBlank()) {
                                    Toast.makeText(this, "Please enter OTP", Toast.LENGTH_SHORT).show()
                                    return@LoginScreen
                                }
                                storedVerificationId?.let { id ->
                                    val credential = PhoneAuthProvider.getCredential(id, otp)
                                    Firebase.auth.signInWithCredential(credential)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                isOtpSent = false
                                            } else {
                                                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                }
                            },
                            isOtpSent = isOtpSent
                        )
                    }
                    is ProfileUiState.Success -> {
                        ExpenseTrackerScreen(viewModel = viewModel)
                    }
                    is ProfileUiState.NotRegistered -> {
                        RegistrationScreen(
                            viewModel = viewModel,
                            onComplete = { username, dob ->
                                viewModel.completeRegistration(username, dob)
                            }
                        )
                    }
                }
            }
        }
    }
}
