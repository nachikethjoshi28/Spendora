package com.example.dailyexpensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.dailyexpensetracker.data.ExpenseRepository
import com.example.dailyexpensetracker.data.local.AppDatabase
import com.example.dailyexpensetracker.ui.tabs.ExpenseTrackerScreen
import com.example.dailyexpensetracker.ui.theme.SpendoraTheme
import com.example.dailyexpensetracker.ui.viewmodel.ExpenseViewModel
import com.example.dailyexpensetracker.ui.viewmodel.ExpenseViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val db = AppDatabase.getDatabase(this)
        val repository = ExpenseRepository(
            db.transactionDao(), 
            db.categoryDao(), 
            db.accountDao(),
            db.friendDao()
        )
        val viewModel: ExpenseViewModel by viewModels { ExpenseViewModelFactory(repository) }

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            
            SpendoraTheme(darkTheme = isDarkMode) {
                ExpenseTrackerScreen(viewModel)
            }
        }
    }
}
