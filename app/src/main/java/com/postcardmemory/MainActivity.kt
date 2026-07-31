package com.postcardmemory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.postcardmemory.ui.camera.CameraScreen
import com.postcardmemory.ui.detail.DetailScreen
import com.postcardmemory.ui.futuremail.FutureMailboxScreen
import com.postcardmemory.ui.gallery.GalleryScreen
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.PostCardMemoryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PostCardMemoryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BrutalWhite
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "gallery",
                        modifier = Modifier.safeDrawingPadding()
                    ) {
                        composable("gallery") {
                            GalleryScreen(
                                onNavigateToCamera = {
                                    navController.navigate("camera")
                                },
                                onNavigateToDetail = { id ->
                                    navController.navigate("detail/$id")
                                },
                                onNavigateToFutureMailbox = {
                                    navController.navigate("futureMailbox")
                                }
                            )
                        }

                        composable("futureMailbox") {
                            FutureMailboxScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("camera") {
                            CameraScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("detail/{postcardId}") { backStackEntry ->
                            val id =
                                backStackEntry.arguments
                                    ?.getString("postcardId")
                                    ?.toLongOrNull()
                                    ?: 0L

                            DetailScreen(
                                postcardId = id,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
