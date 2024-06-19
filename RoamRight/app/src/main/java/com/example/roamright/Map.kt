package com.example.roamright

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class CustomLatLng(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0
)

data class PhotoDetail(
    var imageUrl: String = "",
    var location: CustomLatLng = CustomLatLng(),
    var timestamp: Long = 0L
)

data class UserData(
    val distanceWalked: Double = 0.0,
    val photosTaken: Int = 0,
    val personalInfo: String = ""
)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapPage(
    username: String,
    navController: NavController,
    userViewModel: UserViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var lastLocation by remember { mutableStateOf<Location?>(null) }
    var clickedLocation by remember { mutableStateOf<LatLng?>(null) }
    var pictureLocation by remember { mutableStateOf<LatLng?>(null) }
    var pictureFile by remember { mutableStateOf<File?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedPhotoDetail by remember { mutableStateOf<PhotoDetail?>(null) }
    val mapProperties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = true)) }
    val cameraPositionState = rememberCameraPositionState()

    val photoDetails = remember { mutableStateListOf<PhotoDetail>() }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            userLocation?.let { location ->
                pictureLocation = LatLng(location.latitude, location.longitude)
                pictureFile?.let { file ->
                    val uri = Uri.fromFile(file)
                    val photoDetail = PhotoDetail(uri.toString(), CustomLatLng(location.latitude, location.longitude), System.currentTimeMillis())
                    Log.d("MapPage", "Uploading image to Firebase: $uri")
                    uploadImageToFirebase(context, FirebaseAuth.getInstance().currentUser?.uid ?: "", photoDetail)
                    photoDetails.add(photoDetail)
                }
                FirebaseAuth.getInstance().currentUser?.uid?.let { userViewModel.incrementPhotosTaken(it) }
            }
        } else {
            Log.e("MapPage", "Failed to take picture")
        }
    }

    LaunchedEffect(Unit) {
        if (locationPermissionState.status.isGranted) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                userLocation = location
                lastLocation = location
                if (location != null) {
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f))
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                com.google.android.gms.location.LocationRequest.create().apply {
                    interval = 10000
                    fastestInterval = 5000
                    priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
                },
                object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                        val newLocation = locationResult.lastLocation
                        if (lastLocation != null && newLocation != null) {
                            val distance = lastLocation!!.distanceTo(newLocation).toDouble()
                            FirebaseAuth.getInstance().currentUser?.uid?.let { userViewModel.updateDistanceWalked(it, distance) }
                        }
                        lastLocation = newLocation
                    }
                },
                null
            )
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(username) {
        photoDetails.clear()
        FirebaseAuth.getInstance().currentUser?.uid?.let {
            loadImagesFromFirebase(context, it, photoDetails)
        }
    }

    val createImageFile: () -> File = {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            pictureFile = this
        }
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            if (locationPermissionState.status.isGranted) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    properties = mapProperties,
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng ->
                        clickedLocation = latLng
                    }
                ) {

                    clickedLocation?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "Destination"
                        )
                    }

                    pictureLocation?.let { location ->
                        Marker(
                            state = MarkerState(position = location),
                            title = "Picture taken here",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                            onClick = {
                                selectedPhotoDetail = photoDetails.find { detail ->
                                    detail.location.latitude == location.latitude && detail.location.longitude == location.longitude
                                }
                                showDialog = true
                                true
                            }
                        )
                    }

                    photoDetails.forEach { photoDetail ->
                        Marker(
                            state = MarkerState(position = LatLng(photoDetail.location.latitude, photoDetail.location.longitude)),
                            title = "Photo Location",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                            onClick = {
                                selectedPhotoDetail = photoDetail
                                showDialog = true
                                true
                            }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Button(
                    onClick = {
                        if (cameraPermissionState.status.isGranted) {
                            pictureFile = createImageFile()
                            val photoURI: Uri = FileProvider.getUriForFile(
                                context,
                                "com.example.roamright.fileprovider",
                                pictureFile!!
                            )
                            cameraLauncher.launch(photoURI)
                        } else {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp)
                ) {
                    Text("Open Camera")
                }
            }
        }

        if (showDialog && selectedPhotoDetail != null) {
            Dialog(onDismissRequest = { showDialog = false }) {
                Surface(
                    modifier = Modifier
                        .padding(16.dp)
                        .wrapContentSize()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Photo taken at the location", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        selectedPhotoDetail?.let { detail ->
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(detail.imageUrl)
                                        .crossfade(true)
                                        .build()
                                ),
                                contentDescription = "Taken Picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp) // Set the desired height for the image
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showDialog = false }) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

fun uploadImageToFirebase(context: Context, userId: String, photoDetail: PhotoDetail) {
    val storageRef = FirebaseStorage.getInstance().reference
    val fileRef = storageRef.child("images/$userId/${photoDetail.timestamp}.jpg")
    fileRef.putFile(Uri.parse(photoDetail.imageUrl))
        .addOnSuccessListener {
            fileRef.downloadUrl.addOnSuccessListener { uri ->
                Log.d("MapPage", "Image uploaded to Firebase: $uri")
                saveImageMetadataToFirestore(userId, photoDetail.copy(imageUrl = uri.toString()))
            }.addOnFailureListener { e ->
                Log.e("MapPage", "Failed to get download URL", e)
            }
        }
        .addOnFailureListener { e ->
            Log.e("MapPage", "Image upload failed", e)
        }
}

fun saveImageMetadataToFirestore(userId: String, photoDetail: PhotoDetail) {
    val db = FirebaseFirestore.getInstance()
    db.collection("users").document(userId).collection("photos").add(photoDetail)
        .addOnSuccessListener {
            Log.d("MapPage", "Image metadata saved to Firestore")
        }
        .addOnFailureListener { e ->
            Log.e("MapPage", "Failed to save image metadata to Firestore", e)
        }
}

fun loadImagesFromFirebase(context: Context, userId: String, photoDetails: MutableList<PhotoDetail>) {
    val db = FirebaseFirestore.getInstance()
    db.collection("users").document(userId).collection("photos")
        .get()
        .addOnSuccessListener { result ->
            for (document in result) {
                val photoDetail = document.toObject(PhotoDetail::class.java)
                photoDetails.add(photoDetail)
                Log.d("MapPage", "Loaded image metadata: ${photoDetail.imageUrl}")
            }
        }
        .addOnFailureListener { e ->
            Log.e("MapPage", "Failed to load image metadata from Firestore", e)
        }
}

fun saveUserData(userId: String, userData: UserData) {
    val db = FirebaseFirestore.getInstance()
    db.collection("users").document(userId)
        .set(userData)
        .addOnSuccessListener {
            Log.d("MapPage", "User data saved to Firestore")
        }
        .addOnFailureListener { e ->
            Log.e("MapPage", "Failed to save user data to Firestore", e)
        }
}

fun getUserData(userId: String, onComplete: (UserData?) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("users").document(userId)
        .get()
        .addOnSuccessListener { document ->
            if (document != null) {
                val userData = document.toObject(UserData::class.java)
                Log.d("MapPage", "User data retrieved from Firestore: $userData")
                onComplete(userData)
            } else {
                onComplete(null)
            }
        }
        .addOnFailureListener { e ->
            Log.e("MapPage", "Failed to get user data from Firestore", e)
            onComplete(null)
        }
}

val userId = FirebaseAuth.getInstance().currentUser?.uid

fun updateDistanceWalked(newDistance: Double) {
    if (userId != null) {
        getUserData(userId) { userData ->
            val updatedData = userData?.copy(distanceWalked = userData.distanceWalked + newDistance) ?: UserData(distanceWalked = newDistance)
            saveUserData(userId, updatedData)
        }
    }
}

fun incrementPhotosTaken() {
    if (userId != null) {
        getUserData(userId) { userData ->
            val updatedData = userData?.copy(photosTaken = userData.photosTaken + 1) ?: UserData(photosTaken = 1)
            saveUserData(userId, updatedData)
        }
    }
}

suspend fun loadBitmapFromUrl(imageUrl: String): Bitmap? {
    return try {
        val url = URL(imageUrl)
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        val input: InputStream = connection.inputStream
        BitmapFactory.decodeStream(input)
    } catch (e: Exception) {
        Log.e("MapPage", "Error loading image from URL", e)
        null
    }
}
