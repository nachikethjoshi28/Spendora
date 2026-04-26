package com.example.dailyexpensetracker.ui.tabs

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailyexpensetracker.data.local.TransactionEntity
import com.example.dailyexpensetracker.ui.theme.*
import com.example.dailyexpensetracker.ui.viewmodel.ExpenseViewModel
import com.example.dailyexpensetracker.ui.viewmodel.ProfileUiState
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun ExpenseTrackerScreen(viewModel: ExpenseViewModel) {
    val profileState by viewModel.profileState.collectAsState()
    val context = LocalContext.current

    when (profileState) {
        is ProfileUiState.LoggedOut -> {
            LoginScreen(
                onLoginSuccess = {
                    Firebase.auth.currentUser?.let { user ->
                        viewModel.signIn(user.uid, user.email ?: "", user.displayName)
                    }
                },
                onEmailLogin = { email, pass ->
                    Firebase.auth.signInWithEmailAndPassword(email, pass)
                        .addOnSuccessListener { result ->
                            result.user?.let { viewModel.signIn(it.uid, it.email ?: "", it.displayName) }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Login Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                },
                onEmailSignUp = { email, pass ->
                    Firebase.auth.createUserWithEmailAndPassword(email, pass)
                        .addOnSuccessListener { result ->
                            result.user?.let { viewModel.signIn(it.uid, it.email ?: "", it.displayName) }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Sign Up Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                },
                onPhoneLogin = { /* Phone auth logic */ },
                onVerifyOtp = { /* OTP verify logic */ }
            )
        }
        is ProfileUiState.NotRegistered -> {
            RegistrationScreen(viewModel) { username, dob ->
                viewModel.completeRegistration(username, dob)
            }
        }
        is ProfileUiState.Success -> {
            MainScaffold(viewModel)
        }
        is ProfileUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FintechAccent)
            }
        }
    }
}

@Composable
fun MainScaffold(viewModel: ExpenseViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    
    val tabs = listOf("Home", "Insights", "Add", "Friends", "Profile")
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    LaunchedEffect(editingTransaction) {
        if (editingTransaction != null) {
            selectedTab = 2
        }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 8.dp,
                    modifier = Modifier.clip(RoundedCornerShape(24.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { 
                                selectedTab = index 
                                if (index != 2) editingTransaction = null
                            },
                            label = { 
                                Text(
                                    title, 
                                    fontSize = 10.sp, 
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal 
                                ) 
                            },
                            icon = {
                                if (index == 2) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selectedTab == 2) FintechAccent else Color.Transparent,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = title,
                                                tint = if (selectedTab == 2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Icon(
                                        imageVector = when (index) {
                                            0 -> Icons.Default.Home
                                            1 -> Icons.Default.BarChart
                                            3 -> Icons.Default.People
                                            else -> Icons.Default.Person
                                        },
                                        contentDescription = title
                                    )
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FintechAccent,
                                selectedTextColor = FintechAccent,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                when (selectedTab) {
                    0 -> HomeTab(
                        viewModel = viewModel, 
                        onEditTransaction = { editingTransaction = it }
                    )
                    1 -> InsightsTab(viewModel)
                    2 -> AddTransactionScreen(
                        viewModel = viewModel,
                        editingTransaction = editingTransaction,
                        onSave = { 
                            editingTransaction = null
                            selectedTab = 0 
                        },
                        snackbarHostState = snackbarHostState
                    )
                    3 -> FriendsTab(viewModel, onEditTransaction = { editingTransaction = it })
                    4 -> ProfileTab(viewModel, onEditTransaction = { editingTransaction = it })
                }
            }
        }
    }
}
