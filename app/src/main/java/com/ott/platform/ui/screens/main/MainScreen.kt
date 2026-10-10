package com.ott.platform.ui.screens.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ott.platform.ui.navigation.Screen
import com.ott.platform.ui.screens.home.HomeScreen
import com.ott.platform.ui.screens.mylist.MyListScreen
import com.ott.platform.ui.screens.profile.ProfileScreen
import com.ott.platform.ui.screens.search.SearchScreen
import com.ott.platform.ui.theme.OttBackground
import com.ott.platform.ui.theme.OttRed

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(Screen.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Search : BottomNavItem(Screen.Search.route, "Search", Icons.Filled.Search, Icons.Outlined.Search)
    object MyList : BottomNavItem(Screen.MyList.route, "My List", Icons.Filled.List, Icons.Outlined.List)
    object Profile : BottomNavItem(Screen.Profile.route, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun MainScreen(
    onNavigateToContent: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Search,
        BottomNavItem.MyList,
        BottomNavItem.Profile
    )

    Scaffold(
        containerColor = OttBackground,
        bottomBar = {
            NavigationBar(
                containerColor = OttBackground,
                contentColor = Color.White
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(text = item.title, style = MaterialTheme.typography.labelSmall) },
                        selected = isSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OttRed,
                            selectedTextColor = OttRed,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        ),
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(onNavigateToContent = onNavigateToContent)
            }
            composable(Screen.Search.route) {
                SearchScreen(onNavigateToContent = onNavigateToContent)
            }
            composable(Screen.MyList.route) {
                MyListScreen(onNavigateToContent = onNavigateToContent)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onSignOut = onSignOut,
                    onNavigateToSubscription = { /* TODO */ }
                )
            }
        }
    }
}
