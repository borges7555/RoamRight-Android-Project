package com.example.roamright

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roamright.ui.theme.RoamRightTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (loggedIn) {
            MapPage(username)
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
