package com.example.roamright

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Environment
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
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.tasks.await

val storage = FirebaseStorage.getInstance("gs://roamright-17076.appspot.com")
val storageRef = storage.reference

data class PhotoDetail(
    val imageUrl: String,
    val location: LatLng,
    val timestamp: Long
)

fun saveImageMetadataLocally(context: Context, userId: String, photoDetail: PhotoDetail) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("RoamRightPrefs", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val photoDetails = getLocalImageMetadata(context, userId).toMutableList()
    photoDetails.add(photoDetail)
    val serializedPhotoDetails = photoDetails.joinToString(";") {
        "${it.imageUrl},${it.location.latitude},${it.location.longitude},${it.timestamp}"
    }
    editor.putString(userId, serializedPhotoDetails)
    editor.apply()
}

fun getLocalImageMetadata(context: Context, userId: String): List<PhotoDetail> {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("RoamRightPrefs", Context.MODE_PRIVATE)
    val serializedPhotoDetails = sharedPreferences.getString(userId, "")
    return serializedPhotoDetails?.split(";")?.mapNotNull {
        val parts = it.split(",")
        if (parts.size == 4) {
            val imageUrl = parts[0]
            val latitude = parts[1].toDoubleOrNull()
            val longitude = parts[2].toDoubleOrNull()
            val timestamp = parts[3].toLongOrNull()
            if (latitude != null && longitude != null && timestamp != null) {
                PhotoDetail(imageUrl, LatLng(latitude, longitude), timestamp)
            } else {
                null
            }
        } else {
            null
        }
    } ?: emptyList()
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapPage(username: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val locationPermissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var clickedLocation by remember { mutableStateOf<LatLng?>(null) }
    var pictureLocation by remember { mutableStateOf<LatLng?>(null) }
    var pictureFile by remember { mutableStateOf<File?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    val mapProperties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = true)) }
    val cameraPositionState = rememberCameraPositionState()

    val photoDetails = remember { mutableStateListOf<PhotoDetail>() }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            userLocation?.let { location ->
                pictureLocation = LatLng(location.latitude, location.longitude)
                pictureFile?.let { file ->
                    coroutineScope.launch {
                        val uri = Uri.fromFile(file)
                        val url = uploadPhoto(uri, username)
                        if (url != null) {
                            val photoDetail = PhotoDetail(url, pictureLocation!!, System.currentTimeMillis())
                            saveImageMetadataLocally(context, username, photoDetail)
                            photoDetails.add(photoDetail)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (locationPermissionState.status.isGranted) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                userLocation = location
                if (location != null) {
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15f))
                    }
                }
            }
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(username) {
        photoDetails.clear()
        photoDetails.addAll(getLocalImageMetadata(context, username))
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

    Box(modifier = Modifier.fillMaxSize()) {
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Camera")
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    onLogout()
                }
            ) {
                Text("Logout")
            }
        }
    }

    if (showDialog && pictureFile != null) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(
                modifier = Modifier
                    .padding(16.dp)
                    .wrapContentSize()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Photo taken at the location", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    val bitmap = BitmapFactory.decodeFile(pictureFile!!.absolutePath)
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Taken Picture",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp) // Set the desired height for the image
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showDialog = false }) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

suspend fun uploadPhoto(uri: Uri, userId: String): String? {
    val fileRef = storageRef.child("images/$userId/${uri.lastPathSegment}")
    return try {
        fileRef.putFile(uri).await()
        fileRef.downloadUrl.await().toString()
    } catch (e: Exception) {
        null
    }
}

@Composable
fun DisplayUserImages(userId: String) {
    val context = LocalContext.current
    var photoDetails by remember { mutableStateOf(emptyList<PhotoDetail>()) }

    LaunchedEffect(userId) {
        photoDetails = getLocalImageMetadata(context, userId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        photoDetails.forEach { detail ->
            Image(
                painter = rememberAsyncImagePainter(detail.imageUrl),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
        }
    }
}
