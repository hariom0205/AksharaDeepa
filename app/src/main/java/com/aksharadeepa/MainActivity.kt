package com.aksharadeepa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aksharadeepa.ui.screens.*
import com.aksharadeepa.ui.theme.AksharaDeepaTheme
import com.aksharadeepa.viewmodel.MainViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AksharaDeepaTheme {
                AksharaApp()
            }
        }
    }
}

@Composable
fun AksharaApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute != "splash" && currentRoute != null) {
                BottomNavigationBar(navController = navController)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("splash") {
                SplashScreen {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
            composable("home") { HomeScreen(navController, viewModel) }
            composable("books") { BooksScreen(navController, viewModel) }
            composable("syllabus") { SyllabusScreen(navController, viewModel) }
            composable("profile") { ProfileScreen(viewModel) }
            composable("flashcards/{chapterId}") { backStackEntry ->
                val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
                FlashcardScreen(chapterId, viewModel, navController)
            }
            composable("reader/{chapterId}") { backStackEntry ->
                val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
                BookReaderScreen(chapterId, viewModel, navController)
            }
            composable("quiz/{subjectId}/{chapterId}") { backStackEntry ->
                val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
                val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
                QuizScreen(subjectId, chapterId, viewModel, navController)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf("home", "books", "syllabus", "profile")
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom bar on quiz screen
    if (currentRoute?.startsWith("quiz") == true) return

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = when (item) {
                            "home" -> Icons.Default.Home
                            "books" -> Icons.Default.Book
                            "profile" -> Icons.Default.AccountCircle
                            else -> Icons.AutoMirrored.Filled.List
                        },
                        contentDescription = item
                    )
                },
                label = { Text(item.replaceFirstChar { it.uppercase() }) },
                selected = currentRoute == item,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                ),
                onClick = {
                    navController.navigate(item) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
