package com.example.roamright

import PhotoDetail
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
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
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapPage(username: String) {
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
                    uploadPhoto(file, username) { url ->
                        savePhotoDetailToFirestore(username, url, pictureLocation!!)
                        photoDetails.add(PhotoDetail(url, pictureLocation!!, System.currentTimeMillis()))
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
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Open Camera")
        }
    }

    if (showDialog && pictureFile != null) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Surface(modifier = Modifier.padding(16.dp).wrapContentSize()) {
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

private fun uploadPhoto(file: File, username: String, onComplete: (String) -> Unit) {
    val storage = FirebaseStorage.getInstance()
    val storageRef = storage.reference
    val userPhotosRef = storageRef.child("photos/$username/${file.name}")
    val uri = Uri.fromFile(file)
    userPhotosRef.putFile(uri)
        .addOnSuccessListener {
            userPhotosRef.downloadUrl.addOnSuccessListener { uri ->
                onComplete(uri.toString())
            }
        }
        .addOnFailureListener {
            // Handle unsuccessful uploads
        }
}

private fun savePhotoDetailToFirestore(username: String, url: String, location: LatLng) {
    val db = FirebaseFirestore.getInstance()
    val photoDetail = hashMapOf(
        "username" to username,
        "url" to url,
        "latitude" to location.latitude,
        "longitude" to location.longitude,
        "timestamp" to System.currentTimeMillis()
    )
    db.collection("photos").add(photoDetail)
}