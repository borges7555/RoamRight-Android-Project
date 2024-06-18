package com.example.roamright

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SignUpScreen(onSignUpSuccess: (String) -> Unit, onLoginClick: () -> Unit, onError: (String) -> Unit) {
    val username = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showSnackbar) {
        if (showSnackbar) {
            snackbarHostState.showSnackbar(snackbarMessage)
            showSnackbar = false
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("RoamRight", style = MaterialTheme.typography.headlineLarge)

                Spacer(modifier = Modifier.height(24.dp))

                TextField(
                    value = username.value,
                    onValueChange = { username.value = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = password.value,
                    onValueChange = { password.value = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (username.value.isNotEmpty() && password.value.isNotEmpty()) {
                            auth.createUserWithEmailAndPassword(username.value, password.value)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        onSignUpSuccess(username.value)
                                    } else {
                                        onError(task.exception?.message ?: "Sign up failed")
                                        snackbarMessage = task.exception?.message ?: "Sign up failed"
                                        showSnackbar = true
                                    }
                                }
                        } else {
                            onError("Please enter both username and password")
                            snackbarMessage = "Please enter both username and password"
                            showSnackbar = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign Up")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Already have an account? Login",
                    modifier = Modifier.clickable { onLoginClick() },
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    )
}
