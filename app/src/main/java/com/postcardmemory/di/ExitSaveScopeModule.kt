package com.postcardmemory.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 화면을 나가는 순간 시작된 "마지막 저장"만을 위한, ViewModel보다 오래
 * 사는 scope.
 *
 * 화면 이탈 경로는 저장이 끝나기를 무한정 기다릴 수 없어 상한 시간을 두는데,
 * 저장 자체가 그 대기 coroutine 안에서 직접 실행되면 상한 시간이 지나는
 * 순간 저장까지 함께 취소돼 사용자의 마지막 편집이 사라진다. 대기(UI)와
 * 저장(작업)의 생명주기를 분리하기 위해 저장만 이 scope로 옮긴다.
 *
 * 남용 금지: 일반적인 화면 상태 갱신이나 조회는 계속 viewModelScope를 쓴다.
 * 이 scope는 "이미 사용자가 만든 데이터를 디스크에 확정하는 짧은 쓰기"에만
 * 사용한다. 화면이 사라진 뒤에도 돌기 때문에 여기에 UI 의존 작업을 넣으면
 * 누수가 된다.
 *
 * SupervisorJob을 쓰므로 한 저장이 실패해도 다음 저장이 막히지 않는다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ExitSaveScope

@Module
@InstallIn(SingletonComponent::class)
object ExitSaveScopeModule {

    @Provides
    @Singleton
    @ExitSaveScope
    fun provideExitSaveScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
