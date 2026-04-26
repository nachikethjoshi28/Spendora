package com.example.dailyexpensetracker.ui.tabs

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailyexpensetracker.ui.theme.FintechAccent
import com.example.dailyexpensetracker.ui.theme.FintechCard
import com.example.dailyexpensetracker.ui.theme.FintechDeepDark
import com.example.dailyexpensetracker.ui.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RegistrationScreen(
    viewModel: ExpenseViewModel,
    onComplete: (String, Long) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var dob by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isChecking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val calendar = Calendar.getInstance().apply { timeInMillis = dob }

    val isValidUsername = remember(username) {
        username.length >= 4 && username.all { it.isLetterOrDigit() }
    }

    Surface(color = FintechDeepDark, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = FintechAccent,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Setup Your Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "These details help us personalize your experience and sync your data across devices.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { input ->
                    val filtered = input.filter { it.isLetterOrDigit() }
                    username = filtered
                    errorMessage = null
                },
                label = { Text("Username") },
                placeholder = { Text("e.g. johndoe123") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                isError = errorMessage != null || (username.isNotEmpty() && !isValidUsername),
                supportingText = {
                    if (errorMessage != null) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                    } else if (username.isNotEmpty() && !isValidUsername) {
                        Text("At least 4 alphanumeric characters", color = MaterialTheme.colorScheme.error)
                    }
                },
                leadingIcon = { Icon(Icons.Default.Person, null, tint = FintechAccent) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = FintechCard,
                    unfocusedContainerColor = FintechCard,
                    focusedBorderColor = FintechAccent,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    DatePickerDialog(context, { _, year, month, day ->
                        calendar.set(year, month, day)
                        dob = calendar.timeInMillis
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White, containerColor = FintechCard),
                border = androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent)
            ) {
                Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(20.dp), tint = FintechAccent)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "DOB: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dob))}",
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (isValidUsername) {
                        scope.launch {
                            isChecking = true
                            try {
                                val isAvailable = viewModel.checkUsernameAvailability(username)
                                if (isAvailable) {
                                    onComplete(username, dob)
                                } else {
                                    errorMessage = "Username already taken"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Network error. Please try again."
                            } finally {
                                isChecking = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FintechAccent),
                enabled = isValidUsername && !isChecking
            ) {
                if (isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Profile & Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Your profile is securely synced with your account.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}
