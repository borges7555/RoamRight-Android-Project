package com.example.roamright

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.roamright.ui.theme.RoamRightTheme

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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (loggedIn) {
            WelcomeScreen(username)
        } else {
            LoginScreen(onLoginSuccess = { enteredUsername ->
                username = enteredUsername
                loggedIn = true
            })
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    RoamRightTheme {
        MainScreen()
    }
}
