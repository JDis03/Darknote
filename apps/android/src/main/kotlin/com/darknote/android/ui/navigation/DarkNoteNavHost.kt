package com.darknote.android.ui.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.darknote.android.SnippetListViewModel
import com.darknote.android.viewmodel.AuthViewModel
import com.darknote.android.ui.screens.EditorScreen
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
        composable(
            Screen.Home.route,
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) }
        ) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToEditor = { snippetId ->
                    navController.navigate(Screen.Editor.createRoute(snippetId))
                }
            )
        }

        composable(
            Screen.Editor.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) { backStackEntry ->
            val snippetId = backStackEntry.arguments?.getString("snippetId") ?: ""
            EditorScreen(
                snippetId = snippetId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Screen.Settings.route,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) {
            SettingsScreen(
                authViewModel = authViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
