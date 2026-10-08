package com.ott.platform.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ott.platform.ui.screens.auth.LoginScreen
import com.ott.platform.ui.screens.auth.RegisterScreen
import com.ott.platform.ui.screens.content.ContentDetailScreen
import com.ott.platform.ui.screens.subscription.SubscriptionScreen
import com.ott.platform.ui.screens.main.MainScreen
import com.ott.platform.ui.screens.splash.SplashScreen

@Composable
fun MainNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("main") {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Subscription.route) {
            SubscriptionScreen(
                onSubscribeSuccess = {
                    navController.navigate("main") {
                        popUpTo(Screen.Subscription.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("main") {
            MainScreen(
                onNavigateToContent = { contentId ->
                    navController.navigate(Screen.ContentDetail.createRoute(contentId))
                }
            )
        }
        
        composable(Screen.ContentDetail.route) { backStackEntry ->
            val contentId = backStackEntry.arguments?.getString("contentId")
            ContentDetailScreen(
                contentId = contentId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { id ->
                    navController.navigate(Screen.Player.createRoute(id))
                }
            )
        }
    }
}
