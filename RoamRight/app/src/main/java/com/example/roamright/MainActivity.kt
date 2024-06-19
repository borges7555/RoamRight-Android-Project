package com.example.roamright

import ProfilePage
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.roamright.ui.theme.RoamRightTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this) // Ensure Firebase is initialized
        setContent {
            RoamRightTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    var loggedIn by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var showLoginScreen by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    val navController = rememberNavController()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (loggedIn) {
            NavHost(navController = navController, startDestination = "profile") {
                composable("profile") {
                    ProfilePage(username = username, onLogout = {
                        loggedIn = false
                        username = ""
                        showLoginScreen = true
                    }, navController = navController)
                }
                composable("map") {
                    MapPage(username = username, navController = navController)
                }
                //composable("analytics") { AnalyticsPage(navController) }
            }
        } else {
            if (showLoginScreen) {
                LoginScreen(
                    onLoginSuccess = { enteredUsername ->
                        username = enteredUsername
                        loggedIn = true
                    },
                    onSignUpClick = {
                        showLoginScreen = false
                    },
                    onError = { message ->
                        errorMessage = message
                    }
                )
            } else {
                SignUpScreen(
                    onSignUpSuccess = { newUsername ->
                        username = newUsername
                        loggedIn = true
                    },
                    onLoginClick = {
                        showLoginScreen = true
                    },
                    onError = { message ->
                        errorMessage = message
                    }
                )
            }
        }

        if (errorMessage.isNotEmpty()) {
            Snackbar(
                action = {
                    TextButton(onClick = { errorMessage = "" }) {
                        Text("Dismiss")
                    }
                },
                modifier = Modifier.padding(8.dp)
            ) { Text(errorMessage) }
        }
    }
}
