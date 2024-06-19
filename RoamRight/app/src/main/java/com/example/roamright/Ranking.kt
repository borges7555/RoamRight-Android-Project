package com.example.roamright

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun RankingPage(username: String) {
    var rankings by remember { mutableStateOf<List<UserRanking>>(emptyList()) }
    LaunchedEffect(Unit) {
        rankings = fetchUserRankings()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Header()
        Spacer(modifier = Modifier.height(16.dp))
        rankings.forEachIndexed { index, userRanking ->
            RankingItem(rank = index + 1, userRanking = userRanking)
            Spacer(modifier = Modifier.height(8.dp))
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
            text = "Ranking",
            color = Color.White,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RankingItem(rank: Int, userRanking: UserRanking) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "$rank. ${userRanking.username}",
                fontSize = 20.sp
            )
            Text(
                text = "${userRanking.locationsVisited} locations",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

data class UserRanking(
    val username: String,
    val locationsVisited: Int
)

suspend fun fetchUserRankings(): List<UserRanking> {
    val db = FirebaseFirestore.getInstance()
    val photosCollection = db.collection("photos")
    val userLocations = mutableMapOf<String, MutableSet<String>>()

    try {
        val snapshot = photosCollection.get().await()
        for (document in snapshot.documents) {
            val username = document.getString("username") ?: continue
            val latitude = document.getDouble("latitude") ?: continue
            val longitude = document.getDouble("longitude") ?: continue
            val locationKey = "$latitude,$longitude"

            if (!userLocations.containsKey(username)) {
                userLocations[username] = mutableSetOf()
            }
            userLocations[username]?.add(locationKey)
        }
    } catch (e: Exception) {
        // Handle exception
    }

    return userLocations.map { (username, locations) ->
        UserRanking(username, locations.size)
    }.sortedByDescending { it.locationsVisited }
}
