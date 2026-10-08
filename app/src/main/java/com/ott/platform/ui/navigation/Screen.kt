package com.ott.platform.ui.navigation

/**
 * Screen routes for Compose Navigation.
 */
sealed class Screen(val route: String) {
    // Auth & Onboarding
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Subscription : Screen("subscription")
    
    // Main App (Bottom Nav)
    object Home : Screen("home")
    object Search : Screen("search")
    object MyList : Screen("mylist")
    object Profile : Screen("profile")
    
    // Details & Player
    object ContentDetail : Screen("content/{contentId}") {
        fun createRoute(contentId: String) = "content/$contentId"
    }
    object Player : Screen("player/{contentId}") {
        fun createRoute(contentId: String) = "player/$contentId"
    }
}
