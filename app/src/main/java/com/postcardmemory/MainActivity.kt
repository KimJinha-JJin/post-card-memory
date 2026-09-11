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
import androidx.compose.runtime.LaunchedEffect
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
import com.postcardmemory.utils.TodayVisit
import com.postcardmemory.utils.VisitRecordStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 프로세스 안에서 인트로를 한 번만 보여주기 위한 in-memory 플래그.
 * 앱 프로세스가 살아있는 동안(화면 회전 등 Activity 재생성 포함)에는
 * 유지되지만, 프로세스가 새로 시작되면 다시 false로 돌아온다 — 별도의
 * 영속 저장소(DB/SharedPreferences)를 두지 않는다.
 *
 * [todayVisit]도 같은 수명을 쓴다. 방문 판정은 프로세스당 정확히 한 번만
 * 하면 되므로(하루 1회 판정 자체는 저장된 날짜가 보장한다), 이 값이 이미
 * 채워져 있으면 다시 저장소를 건드리지 않는다 — recomposition이나 Activity
 * 재생성으로 방문이 다시 기록되지 않는다.
 */
private object AppIntroState {
    var hasShownIntro = false
    var todayVisit: TodayVisit? = null
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

                    var todayVisit by remember {
                        mutableStateOf(AppIntroState.todayVisit)
                    }

                    // 오늘 방문 기록은 프로세스 시작마다 한 번만, IO 디스패처에서
                    // 판정한다. 인트로 애니메이션은 이 결과를 기다리지 않고
                    // 바로 시작되며, 값이 도착하면 소인만 나중에 나타난다.
                    LaunchedEffect(Unit) {
                        if (AppIntroState.todayVisit == null) {
                            val recorded = withContext(Dispatchers.IO) {
                                VisitRecordStorage.recordTodayVisit(
                                    applicationContext
                                )
                            }

                            AppIntroState.todayVisit = recorded
                            todayVisit = recorded
                        }
                    }

                    Crossfade(
                        targetState = showIntro,
                        animationSpec = tween(180),
                        label = "appIntroToGallery"
                    ) { intro ->
                        if (intro) {
                            AppIntroScreen(
                                visitRecord = todayVisit?.record,
                                isFirstVisitToday =
                                    todayVisit?.isFirstVisitToday == true,
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
