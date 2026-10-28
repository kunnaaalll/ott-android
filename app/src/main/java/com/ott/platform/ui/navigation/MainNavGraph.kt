package com.ott.platform.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ott.platform.ui.screens.auth.AuthState
import com.ott.platform.ui.screens.auth.AuthViewModel
import com.ott.platform.ui.screens.auth.LoginScreen
import com.ott.platform.ui.screens.auth.RegisterScreen
import com.ott.platform.ui.screens.content.ContentDetailScreen
import com.ott.platform.ui.screens.main.MainScreen
import com.ott.platform.ui.screens.splash.SplashScreen
import com.ott.platform.ui.screens.subscription.SubscriptionCheckScreen
import com.ott.platform.ui.screens.subscription.SubscriptionScreen

@Composable
fun MainNavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Unauthenticated) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute != Screen.Splash.route && currentRoute != Screen.Login.route && currentRoute != Screen.Register.route) {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = { isAuthenticated ->
                    val destination = if (isAuthenticated) Screen.SubscriptionCheck.route else Screen.Login.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.SubscriptionCheck.route) {
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
                    navController.navigate(Screen.SubscriptionCheck.route) {
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
        
        composable(Screen.SubscriptionCheck.route) {
            SubscriptionCheckScreen(
                onSubscriptionActive = {
                    navController.navigate("main") {
                        popUpTo(Screen.SubscriptionCheck.route) { inclusive = true }
                    }
                },
                onSubscriptionInactive = {
                    navController.navigate(Screen.Subscription.route) {
                        popUpTo(Screen.SubscriptionCheck.route) { inclusive = true }
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
                    // Because app is subscription only, going back either exits or logs out
                    // Here we just sign out for simplicity, or we can close the app
                    authViewModel.logout()
                }
            )
        }
        
        composable("main") {
            MainScreen(
                onNavigateToContent = { contentId ->
                    navController.navigate(Screen.ContentDetail.createRoute(contentId))
                },
                onSignOut = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSubscription = {
                    navController.navigate(Screen.Subscription.route)
                }
            )
        }
        
        composable(
            route = Screen.ContentDetail.route,
            arguments = listOf(
                navArgument("contentId") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val contentId = backStackEntry.arguments?.getString("contentId")
            ContentDetailScreen(
                contentId = contentId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { id ->
                    navController.navigate(Screen.Player.createRoute(id))
                },
                onNavigateToContent = { id ->
                    navController.navigate(Screen.ContentDetail.createRoute(id))
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onNavigateToSubscription = {
                    navController.navigate(Screen.Subscription.route)
                }
            )
        }
    }
}
