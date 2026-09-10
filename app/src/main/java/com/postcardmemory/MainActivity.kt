package com.postcardmemory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.postcardmemory.ui.camera.CameraScreen
import com.postcardmemory.ui.detail.DetailScreen
import com.postcardmemory.ui.futuremail.FutureMailboxScreen
import com.postcardmemory.ui.gallery.GalleryScreen
import com.postcardmemory.ui.intro.AppIntroScreen
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.PostCardMemoryTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 프로세스 안에서 인트로를 한 번만 보여주기 위한 in-memory 플래그.
 * 앱 프로세스가 살아있는 동안(화면 회전 등 Activity 재생성 포함)에는
 * 유지되지만, 프로세스가 새로 시작되면 다시 false로 돌아온다 — 별도의
 * 영속 저장소(DB/SharedPreferences)를 두지 않는다.
 */
private object AppIntroState {
    var hasShownIntro = false
}

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
                    var showIntro by remember {
                        mutableStateOf(!AppIntroState.hasShownIntro)
                    }

                    Crossfade(
                        targetState = showIntro,
                        animationSpec = tween(180),
                        label = "appIntroToGallery"
                    ) { intro ->
                        if (intro) {
                            AppIntroScreen(
                                onFinished = {
                                    AppIntroState.hasShownIntro = true
                                    showIntro = false
                                }
                            )
                        } else {
                            MainNavHost()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainNavHost() {
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
