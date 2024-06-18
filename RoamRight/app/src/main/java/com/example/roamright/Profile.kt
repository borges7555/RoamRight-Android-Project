import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import java.text.SimpleDateFormat
import java.util.*

data class PhotoDetail(
    val filePath: String,
    val location: LatLng,
    val timestamp: Long
)

@Composable
fun ProfilePage(username: String, photoDetails: List<PhotoDetail>) {
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
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Your Photos", style = MaterialTheme.typography.titleLarge)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            photoDetails.forEach { photo ->
                PhotoItem(photoDetail = photo)
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
fun PhotoItem(photoDetail: PhotoDetail) {
    val bitmap = BitmapFactory.decodeFile(photoDetail.filePath)
    val locationText = "Location: ${photoDetail.location.latitude}, ${photoDetail.location.longitude}"
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val dateText = "Time: ${dateFormat.format(Date(photoDetail.timestamp))}"

    Column(modifier = Modifier
        .fillMaxWidth()
        .background(Color.LightGray, shape = MaterialTheme.shapes.medium)
        .padding(16.dp)
    ) {
        Text(text = "Photo Taken", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Taken Photo",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = locationText, style = MaterialTheme.typography.bodyMedium)
        Text(text = dateText, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true)
@Composable
fun ProfilePagePreview() {
    val photoDetails = listOf(
        PhotoDetail(
            filePath = "/path/to/photo1.jpg",
            location = LatLng(37.7749, -122.4194),
            timestamp = System.currentTimeMillis()
        ),
        PhotoDetail(
            filePath = "/path/to/photo2.jpg",
            location = LatLng(34.0522, -118.2437),
            timestamp = System.currentTimeMillis()
        )
    )
    ProfilePage(username = "User123", photoDetails = photoDetails)
}
