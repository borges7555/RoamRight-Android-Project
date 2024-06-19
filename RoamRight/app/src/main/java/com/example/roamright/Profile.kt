package com.example.roamright

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun ProfilePage(username: String, onLogout: () -> Unit, navController: NavController) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    var personalInfo by remember { mutableStateOf("Informação inserida pelo user sobre si mesmo") }
    val photoDetails = remember { mutableStateListOf<PhotoDetail>() }

    LaunchedEffect(Unit) {
        loadUserInfo(userId) { info ->
            personalInfo = info
        }
        loadImagesFromFirebase(userId) { details ->
            photoDetails.addAll(details)
        }
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            HeaderP()
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0xFF9FA8DA))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Personal Info",
                        color = Color.White,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        value = personalInfo,
                        onValueChange = { personalInfo = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF9FA8DA))
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { saveUserInfo(userId, personalInfo) }) {
                        Text(text = "Save", color = Color.White)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0xFF9FA8DA))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Visited Places",
                        color = Color.White,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    photoDetails.forEach { photoDetail ->
                        VisitedPlaceItem(photoDetail = photoDetail, onDelete = { deletePhoto(photoDetail, photoDetails) })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .background(Color(0xFFD32F2F), shape = CircleShape)
            ) {
                Text(text = "Logout", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun HeaderP() {
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
fun VisitedPlaceItem(photoDetail: PhotoDetail, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF9FA8DA))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Location: ${photoDetail.location.latitude}, ${photoDetail.location.longitude}",
                color = Color.White
            )
            Image(
                painter = rememberAsyncImagePainter(photoDetail.imageUrl),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onDelete) {
            Text(text = "Delete", color = Color.White)
        }
    }
}

fun deletePhoto(photoDetail: PhotoDetail, photoDetails: MutableList<PhotoDetail>) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()
    db.collection("users").document(userId).collection("photos").whereEqualTo("timestamp", photoDetail.timestamp)
        .get()
        .addOnSuccessListener { snapshot ->
            for (document in snapshot.documents) {
                document.reference.delete()
            }
            photoDetails.remove(photoDetail)

            // Update the total photos taken
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val userData = document.toObject(UserData::class.java)
                    val updatedPhotosTaken = (userData?.photosTaken ?: 1) - 1
                    db.collection("users").document(userId)
                        .update("photosTaken", updatedPhotosTaken)
                        .addOnSuccessListener {
                            Log.d("ProfilePage", "Updated photosTaken count")
                        }
                        .addOnFailureListener { e ->
                            Log.e("ProfilePage", "Failed to update photosTaken count: $e")
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfilePage", "Failed to get user data: $e")
                }
        }
        .addOnFailureListener { e ->
            Log.e("ProfilePage", "Failed to delete photo: $e")
        }
}


fun loadImagesFromFirebase(userId: String, onComplete: (List<PhotoDetail>) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("users").document(userId).collection("photos")
        .get()
        .addOnSuccessListener { result ->
            val photoDetails = result.mapNotNull { document ->
                document.toObject(PhotoDetail::class.java)
            }
            Log.d("ProfilePage", "Loaded image metadata: ${photoDetails.size} items")
            onComplete(photoDetails)
        }
        .addOnFailureListener { e ->
            Log.e("ProfilePage", "Failed to load image metadata from Firestore", e)
            onComplete(emptyList())
        }
}

fun saveUserInfo(userId: String, personalInfo: String) {
    val db = FirebaseFirestore.getInstance()
    db.collection("users").document(userId)
        .update("personalInfo", personalInfo)
        .addOnSuccessListener {
            Log.d("ProfilePage", "User info saved successfully")
        }
        .addOnFailureListener { e ->
            Log.e("ProfilePage", "Failed to save user info: $e")
        }
}

fun loadUserInfo(userId: String, onComplete: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("users").document(userId)
        .get()
        .addOnSuccessListener { document ->
            if (document != null) {
                val personalInfo = document.getString("personalInfo") ?: "Informação inserida pelo user sobre si mesmo"
                onComplete(personalInfo)
            }
        }
        .addOnFailureListener { e ->
            Log.e("ProfilePage", "Failed to load user info: $e")
            onComplete("Informação inserida pelo user sobre si mesmo")
        }
}
