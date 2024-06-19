package com.example.roamright

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Assessment

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        NavigationItem.Profile,
        NavigationItem.Map,
        NavigationItem.Analytics
    )
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        navController.popBackStack(navController.graph.startDestinationId, false)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

sealed class NavigationItem(var route: String, var icon: ImageVector, var title: String) {
    object Profile : NavigationItem("profile", Icons.Default.Person, "Profile")
    object Map : NavigationItem("map", Icons.Default.Map, "Map")
    object Analytics : NavigationItem("analytics", Icons.Default.Assessment, "Analytics")
}
