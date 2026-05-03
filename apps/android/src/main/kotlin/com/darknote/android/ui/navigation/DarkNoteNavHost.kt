package com.darknote.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.darknote.android.SnippetListViewModel
import com.darknote.android.viewmodel.AuthViewModel
import com.darknote.android.ui.screens.HomeScreen
import com.darknote.android.ui.screens.SettingsScreen

@Composable
fun DarkNoteNavHost(
    viewModel: SnippetListViewModel,
    authViewModel: AuthViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                authViewModel = authViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}