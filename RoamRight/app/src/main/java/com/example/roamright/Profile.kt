package com.example.roamright

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roamright.ui.theme.RoamRightTheme

@Composable
fun ProfilePage(username: String){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Header()
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart,
        ) {
            Text(text = "Welcome, $username!\n", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
fun Header(){
    val sameColor = MaterialTheme.colorScheme.primary
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(color = sameColor, shape = CircleShape)
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = "Profile",
            color = Color.White,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PersonalInfo(){
    val sameColor = MaterialTheme.colorScheme.primary
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(color = sameColor, shape = CircleShape)
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = "Personal Info\n" +
                    "bla bla bla",
            color = Color.White,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
    }
}