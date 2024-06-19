package com.example.roamright

import android.content.Context
import android.location.Geocoder
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale

@Composable
fun ProfilePage(username: String, onLogout: () -> Unit, navController: NavController) {
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: return
    var personalInfo by remember { mutableStateOf("Informação inserida pelo user sobre si mesmo") }
    var email by remember { mutableStateOf("") }
    val photoDetails = remember { mutableStateListOf<PhotoDetail>() }
    var emailError by remember { mutableStateOf("") }

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
                    .background(Brush.linearGradient(listOf(Color(0xFF6A1B9A), Color(0xFF8E24AA))))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Personal Info",
                        color = Color.White,
                        fontSize = 20.sp,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        value = personalInfo,
                        onValueChange = { personalInfo = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(8.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { saveUserInfo(userId, personalInfo) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text(text = "Save", color = Color.White)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFF1976D2), Color(0xFF2196F3))))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Visited Places",
                        color = Color.White,
                        fontSize = 20.sp,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    photoDetails.forEach { photoDetail ->
                        VisitedPlaceItem(context = context, photoDetail = photoDetail, onDelete = { deletePhoto(photoDetail, photoDetails) })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }


            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Brush.linearGradient(listOf(Color(0xFF1976D2), Color(0xFF2196F3))))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Enter User Email",
                        color = Color.White,
                        fontSize = 20.sp,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(8.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            emailError = ""
                            loadImagesByEmail(email, photoDetails) { error ->
                                emailError = error
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text(text = "Load Photos", color = Color.White)
                    }
                    if (emailError.isNotEmpty()) {
                        Text(
                            text = emailError,
                            color = Color.Red,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }


            Button(
                onClick = onLogout,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(Color(0xFFD32F2F))
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
            .height(100.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(sameColor, sameColor.copy(alpha = 0.7f))
                ),
                shape = CircleShape
            )
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = "Profile",
            color = Color.White,
            fontSize = 30.sp,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun VisitedPlaceItem(context: Context, photoDetail: PhotoDetail, onDelete: () -> Unit) {
    var address by remember { mutableStateOf("Loading address...") }

    LaunchedEffect(photoDetail) {
        address = getAddressFromLatLng(context, photoDetail.location.latitude, photoDetail.location.longitude)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Color(0xFF64B5F6), Color(0xFF2196F3))))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = address,
                color = Color.White
            )
            Image(
                painter = rememberAsyncImagePainter(photoDetail.imageUrl),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onDelete,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
        ) {
            Text(text = "Delete", color = Color.White)
        }
    }
}

fun getAddressFromLatLng(context: Context, lat: Double, lng: Double): String {
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses = geocoder.getFromLocation(lat, lng, 1)
    if (addresses != null) {
        return if (addresses.isNotEmpty()) {
            val address = addresses[0]
            address.getAddressLine(0)
        } else {
            "Address not found"
        }
    }
    return ""
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

fun loadImagesByEmail(email: String, photoDetails: MutableList<PhotoDetail>, onEmailError: (String) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("emailToUserId").document(email).get()
        .addOnSuccessListener { document ->
            val userId = document.getString("userId")
            if (userId != null) {
                db.collection("users").document(userId).collection("photos")
                    .get()
                    .addOnSuccessListener { result ->
                        val newPhotoDetails = result.mapNotNull { photoDocument ->
                            photoDocument.toObject(PhotoDetail::class.java)
                        }
                        photoDetails.clear()
                        photoDetails.addAll(newPhotoDetails)
                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfilePage", "Failed to load image metadata from Firestore", e)
                        onEmailError("Failed to load images.")
                    }
            } else {
                Log.e("ProfilePage", "User not found with email: $email")
                onEmailError("User not found.")
                photoDetails.clear()
            }
        }
        .addOnFailureListener { e ->
            Log.e("ProfilePage", "Failed to find user with email: $email", e)
            onEmailError("Permission denied.")
            photoDetails.clear()
        }
}


fun mapEmailToUserId(userId: String, email: String) {
    val db = FirebaseFirestore.getInstance()
    val emailToUserId = hashMapOf("userId" to userId)
    db.collection("emailToUserId").document(email)
        .set(emailToUserId)
        .addOnSuccessListener {
            Log.d("ProfilePage", "Email to UserId mapping saved successfully")
        }
        .addOnFailureListener { e ->
            Log.e("ProfilePage", "Failed to save email to UserId mapping: $e")
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
