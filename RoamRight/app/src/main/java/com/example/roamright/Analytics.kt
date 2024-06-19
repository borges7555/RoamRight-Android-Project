package com.example.roamright

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData

    fun loadUserData(userId: String) {
        viewModelScope.launch {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    _userData.value = document.toObject(UserData::class.java)
                }
                .addOnFailureListener {
                    _userData.value = null
                }
        }
    }

    fun updateUserData(userId: String, userData: UserData) {
        db.collection("users").document(userId).set(userData)
            .addOnSuccessListener {
                loadUserData(userId)
            }
    }

    fun updateDistanceWalked(userId: String, newDistance: Double) {
        getUserData(userId) { userData ->
            val updatedData = userData?.copy(distanceWalked = userData.distanceWalked + newDistance) ?: UserData(distanceWalked = newDistance)
            updateUserData(userId, updatedData)
        }
    }

    fun incrementPhotosTaken(userId: String) {
        getUserData(userId) { userData ->
            val updatedData = userData?.copy(photosTaken = userData.photosTaken + 1) ?: UserData(photosTaken = 1)
            updateUserData(userId, updatedData)
        }
    }

    fun updatePersonalInfo(userId: String, personalInfo: String) {
        getUserData(userId) { userData ->
            val updatedData = userData?.copy(personalInfo = personalInfo) ?: UserData(personalInfo = personalInfo)
            updateUserData(userId, updatedData)
        }
    }

    private fun getUserData(userId: String, onComplete: (UserData?) -> Unit) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                onComplete(document.toObject(UserData::class.java))
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }
}


@Composable
fun AnalyticsPage(navController: NavController, userViewModel: UserViewModel = viewModel()) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val userData by userViewModel.userData.collectAsState()

    LaunchedEffect(userId) {
        if (userId != null) {
            userViewModel.loadUserData(userId)
        }
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val distanceInKilometers = (userData?.distanceWalked ?: 0.0) / 1000
            val formattedDistance = String.format("%.1f", distanceInKilometers)
            Text("Total Distance Walked: $formattedDistance km", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Total Photos Taken: ${userData?.photosTaken ?: 0}", style = MaterialTheme.typography.titleLarge)
        }
    }
}