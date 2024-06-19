import android.graphics.BitmapFactory
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.roamright.BottomNavigationBar
import com.example.roamright.PhotoDetail
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfilePage(username: String, onLogout: () -> Unit, navController: NavController) {
    var personalInfo by remember { mutableStateOf("Informação inserida pelo user sobre si mesmo") }
    val photoDetails = remember { mutableStateListOf<PhotoDetail>() }

    LaunchedEffect(Unit) {
        photoDetails.addAll(fetchUserPhotos(username))
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
            Header()
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0xFF9FA8DA), shape = CircleShape)
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
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0xFF9FA8DA), shape = CircleShape)
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
                        VisitedPlaceItem(photoDetail = photoDetail)
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
fun Header() {
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
fun VisitedPlaceItem(photoDetail: PhotoDetail) {
    //val bitmap = BitmapFactory.decodeFile(photoDetail.filePath)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF9FA8DA), shape = CircleShape)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Rua BlaBlaBla ${photoDetail.location.latitude}, ${photoDetail.location.longitude}",
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Button(onClick = { /* Show photo in a dialog */ }) {
            Text(text = "Foto", color = Color.White)
        }
    }
}

suspend fun fetchUserPhotos(username: String): List<PhotoDetail> {
    val db = FirebaseFirestore.getInstance()
    val photosCollection = db.collection("photos")
    val photoDetails = mutableListOf<PhotoDetail>()

    try {
        val snapshot = photosCollection.whereEqualTo("username", username).get().await()
        for (document in snapshot.documents) {
            val filePath = document.getString("filePath") ?: continue
            val latitude = document.getDouble("latitude") ?: continue
            val longitude = document.getDouble("longitude") ?: continue
            val timestamp = document.getLong("timestamp") ?: continue

            photoDetails.add(PhotoDetail(filePath, LatLng(latitude, longitude), timestamp))
        }
    } catch (e: Exception) {
        // Handle exception
    }

    return photoDetails
}
