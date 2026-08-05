# DetailScreen·DetailViewModel 구조 감사 — 제2차 결과 보고서

- 대작업: DetailScreen·DetailViewModel 구조 감사 및 단계적 안정화
- 차수: 제2차 — 저장 에러 다이얼로그 동작 고정 및 분리 준비
- 실제 작업일: 2026-08-06
- 조사 방식: 코드 정독 + Grep/텍스트 앵커 기반 정적 구조 테스트 추가. 저장·복구·Mutex·revision 로직, 다이얼로그 UI/문구/버튼은 변경하지 않음.

---

## 1. 최종 판정

**제2차 완료**

단, 아래 §9 "자동검증 결과"에서 밝히듯 **Kotlin 컴파일과 테스트 실행 자체는 이번 환경에서 CLI로 수행하지 못했다**(저장소에 `gradlew`/`gradlew.bat`가 없고, 시스템에 전역 `gradle`도 설치돼 있지 않음 — CLAUDE.md §8이 전제하는 "Android Studio에서 실기기/빌드 검증" 방식에 의존하는 프로젝트로 보임). 이 부분은 `사용자 검증 대기`로 명시한다.

---

## 2. Git 기준 상태

| 항목 | 값 |
|---|---|
| 브랜치 | `feature/photo-sticker` |
| 시작 HEAD | `7fe0047` |
| 종료 HEAD | `7fe0047` (이번 보고서 작성 시점까지 커밋 없음 — 아래 §16 참고) |
| origin/feature/photo-sticker | `7fe0047` (fetch 후 일치) |
| 작업트리 | `?? .kotlin/`(빌드 산출물), `?? docs/`(제1차+제2차 문서), `?? app/src/test/.../SaveErrorDialogStructureTest.kt`(신규 테스트) — 전부 untracked, tracked 파일 변경 없음 |
| 제1차 보고서 커밋 여부 | **미커밋** — 아래 §16에서 사유와 처리 방식 설명 |
| 제2차 테스트 커밋 여부 | **미커밋** — 동일 |
| push 여부 | 해당 없음(커밋 자체가 없음) |

---

## 3. 다이얼로그 목록

### 3-0. 실제 개수 재확인 (1단계)

작업지시서는 "저장 에러 다이얼로그 7종"이라 표현했다. `DetailScreen.kt` 5878~6151줄을 다시 정독한 결과, **이 구간에는 실제로 `AlertDialog(` 호출이 7개** 있으나, **그중 1개(5878~5912줄)는 에러가 아니라 "내보내기 성공" 다이얼로그**다. 즉 정확히는 **저장 에러 다이얼로그 6종 + 성공 다이얼로그 1종 = 총 7개**다. 숫자 "7"은 정확하지만 구성이 "에러 7종"이 아니므로 이후 문서에서는 "저장 결과(성공1+에러6) 다이얼로그 7종"으로 표현을 정정한다.

파일 전체에는 `AlertDialog(` 호출이 총 14개 있으며(글귀/템플릿/삭제/미래엽서 관련이 나머지 7개), 이번 조사·테스트 대상은 5878~6151줄의 7개로 한정했다.

### 3-1. 다이얼로그별 상세

| # | 식별 이름 | 위치(줄) | 표시 조건 | 제목 | 본문 | 버튼 | dismiss | 확인 callback |
|---|---|---|---|---|---|---|---|---|
| 1 | 내보내기 성공 | 5878~5912 | `exportState is ExportState.Success` | "저장 완료!" (검정) | 고정 문자열("1:1 포스트카드 이미지를... Pictures/PostcardMemory 앨범에서...") | 확인 1개("확인") | 외부탭/뒤로가기 → `resetExportState()`(확인과 동일) | `viewModel.resetExportState()` |
| 2 | 내보내기 실패 | 5914~5949 | `exportState as? ExportState.Error` | "저장하지 못했어" (코랄) | `exportError.message`(동적) | 확인 1개 | 확인과 동일 | `viewModel.resetExportState()` |
| 3 | 배경 저장 실패 | 5951~5989 | `backgroundUpdateState as? BackgroundUpdateState.Error` | "배경을 저장하지 못했어" (코랄) | `backgroundError.message`(동적) | 확인 1개 | 확인과 동일 | `viewModel.resetBackgroundUpdateState()` |
| 4 | 사진 교체 실패 | 5991~6031 | `imageUpdateState as? ImageUpdateState.Error` | "사진을 바꾸지 못했어" (코랄) | 고정 안내문("기존 사진은 그대로 유지했어") + `imageError.message` | 확인 1개 | 확인과 동일 | `viewModel.resetImageUpdateState()` |
| 5 | 폰트 저장 실패 | 6033~6071 | `fontUpdateState as? FontUpdateState.Error` | "폰트를 저장하지 못했어" (코랄) | `fontError.message`(동적) | 확인 1개 | 확인과 동일 | `viewModel.resetFontUpdateState()` |
| 6 | 레이아웃 저장 실패 | 6073~6111 | `layoutUpdateState as? LayoutUpdateState.Error` | "레이아웃을 저장하지 못했어" (코랄) | `layoutError.message`(동적) | 확인 1개 | 확인과 동일 | `viewModel.resetLayoutUpdateState()` |
| 7 | 날짜 형식 저장 실패 | 6113~6151 | `dateFormatUpdateState as? DateFormatUpdateState.Error` | "날짜 형식을 저장하지 못했어" (코랄) | `dateFormatError.message`(동적) | 확인 1개 | 확인과 동일 | `viewModel.resetDateFormatUpdateState()` |

**7개 전부 `dismissButton`(취소 버튼)이 없고, `DialogProperties` 오버라이드도 없다** — Material3 `AlertDialog` 기본값대로 뒤로가기와 다이얼로그 바깥 탭 모두 `onDismissRequest`를 호출하며, 7개 전부 `onDismissRequest`가 확인 버튼과 **완전히 동일한** reset 함수를 호출한다. 즉 "취소"라는 개념 자체가 없다 — 사용자가 이 다이얼로그를 벗어나는 유일한 방법은 상태를 Idle로 되돌리는 것뿐이며, 재시도·롤백·파일 삭제 등 어떤 부수효과도 다이얼로그 자체에서 발생하지 않는다(§5 참고).

---

## 4. 공통 구조와 차이

### 완전히 동일한 구조 (7개 전부)
- `AlertDialog` 1개, `title`/`text`/`confirmButton` 3개 슬롯만 사용(취소 버튼 없음)
- `onDismissRequest`와 `confirmButton.onClick`이 동일한 `viewModel.reset*State()` 1개만 호출
- 확인 버튼 문구는 7개 전부 "확인"(`BrutalBlack`, `SemiBold`)
- `DialogProperties` 오버라이드 없음(기본 dismiss 동작)

### 일부만 다른 구조
- 제목 텍스트: 7개 전부 다름
- 제목 색상: 성공 1개만 `BrutalBlack`, 에러 6개는 `BrutalCoral`
- 본문: 성공 1개는 고정 문자열, 에러 5개는 `{상태}.message`를 그대로 노출, **에러 1개(사진 교체 실패)만 고정 안내문("기존 사진은 그대로 유지했어")을 메시지 앞에 붙임** — 데이터 손실 우려가 가장 큰 항목이라 안심 문구를 추가한 것으로 보이나, 코드 주석은 없어 의도 확인은 추정임
- 호출하는 reset 함수: 7개 전부 다른 함수(대응하는 상태별로 1:1)

### 독립 유지가 필요한 구조
없음 — 7개 전부 "state(제목/본문/색상) + 확인 callback 1개"라는 동일한 최소 계약으로 표현 가능하며, 억지로 합쳤을 때 의미가 불명확해지는 지점을 찾지 못했다. 상태 타입(`ExportState`/`BackgroundUpdateState`/...)이 서로 다른 sealed 타입이라는 점은 있으나, 이는 DetailScreen에서 조건 분기와 파라미터 구성으로 흡수 가능하며 다이얼로그 자체는 원시 상태 타입을 몰라도 된다(§11 설계안 참고).

---

## 5. 상태 및 callback 흐름 (2·3단계)

| 항목 | 확인 결과 |
|---|---|
| 상태 소유 위치 | 7개 전부 `DetailViewModel`의 `StateFlow`(`exportState`, `backgroundUpdateState`, `imageUpdateState`, `fontUpdateState`, `layoutUpdateState`, `dateFormatUpdateState`) — `DetailScreen`은 `collectAsState()`로 구독만 함, 로컬 상태 아님 |
| 상태 설정 위치 | 각 저장 함수(`exportPostcardToGallery`, `updateBackgroundColor`/`updateBackgroundPattern`, `updatePostcardImage`, `updateMessageFont`, `updateLayoutStyle`, `updateDateFormat`, `DetailViewModel.kt`)의 `catch` 블록에서 `.Error(message)`로 설정. 성공 다이얼로그는 `exportPostcardToGallery`의 성공 분기에서 `.Success(uri)`로 설정 |
| 상태 해제 위치 | 다이얼로그의 확인 버튼 또는 dismiss(둘 다 동일) → `viewModel.reset*State()` → 해당 `StateFlow.value = *.Idle` (그 외 어떤 상태도 건드리지 않음, `DetailViewModel.kt` 3862~4059, 4497~4500 확인) |
| callback 결과 | reset 함수 6개 전부 **단순 상태 대입만 수행** — 재시도 호출, 롤백 호출, 파일 삭제/복구, 코루틴 시작, 다른 다이얼로그 상태 변경 중 어느 것도 하지 않음(코드 근거: §1 함수 본문 4줄 이내, 부수효과 없음) |
| 롤백과의 관계 | 저장 실패 시 롤백(제1차 감사 §5-2에서 확인한 "조건부 롤백")은 **이 다이얼로그가 뜨기 전에 이미 ViewModel 안에서 끝나 있다.** 다이얼로그는 이미 벌어진 실패를 "확인"만 시키는 통지 창이며, 확인을 누르지 않아도(다이얼로그를 계속 띄워둬도) 롤백 자체는 이미 완료된 상태다 |

### 동시 표시 가능성 (2단계 필수 확인 항목)

`exportState`/`backgroundUpdateState`/`imageUpdateState`/`fontUpdateState`/`layoutUpdateState`/`dateFormatUpdateState`는 **서로 완전히 독립된 StateFlow**이며, 각각 독립된 저장 Job으로 갱신된다(제1차 감사 §5-1 확인). 코드상 이 6개 상태가 동시에 Error가 되는 것을 막는 상호 배제 로직은 없다. 예를 들어 사용자가 배경색과 폰트를 거의 동시에 바꾸고 두 저장이 모두 느린 디스크에서 실패하면, `backgroundUpdateState`와 `fontUpdateState`가 동시에 `Error`가 되어 **두 AlertDialog가 같은 프레임에 함께 컴포지션**될 수 있다. Compose의 `AlertDialog`는 각각 별도의 `Popup`/`Dialog` 윈도우로 뜨므로, 이 경우 무엇이 위에 보이는지에 대한 명시적 우선순위 로직이 없다(코드 순서상 나중에 선언된 것이 나중에 컴포지션되는 정도의 암묵적 순서만 있음: 배경→사진→폰트→레이아웃→날짜형식). **이번 차수에서는 수정하지 않고 사실만 기록한다.**

---

## 6. 기존 테스트 조사

| 구분 | 결과 |
|---|---|
| 이 7개 다이얼로그를 직접 검증하는 기존 테스트 | **없음** — `app/src/test` 전체에서 `ExportState`/`BackgroundUpdateState`/`ImageUpdateState`/`FontUpdateState`/`LayoutUpdateState`/`DateFormatUpdateState`를 참조하는 테스트 파일은 이번에 추가한 `SaveErrorDialogStructureTest.kt` 이전에는 0개였다(Grep으로 전수 확인) |
| 간접적으로 관련된 테스트 | `BackgroundColorSaveRaceTest`, `StyleSaveRaceTest`, `TemplateStyleSaveRollbackTest` — 하지만 이들은 **ViewModel의 저장/롤백 로직**(Fake로 재현)만 검증하며, "Error 상태가 됐을 때 어떤 다이얼로그가 뜨고 확인을 누르면 무엇이 호출되는가"는 검증 범위 밖이다. 즉 이 다이얼로그들의 **표시·문구·callback 자체를 보장하는 테스트는 간접적으로도 없었다** |
| 누락 영역 | 7개 다이얼로그의 문구/색상/버튼 구성/reset 함수 매핑 전체 — 이번 차수에서 `SaveErrorDialogStructureTest.kt`로 처음 고정함 |

테스트 이름만 보지 않고 각 파일의 assertion 내용을 직접 확인했으며(제1차 감사에서 이미 상세 확인, 이번 차수에서 관련 3개 파일 재확인), 위 판정은 assertion 근거를 기준으로 한 것이다.

---

## 7. 추가한 테스트

| 항목 | 내용 |
|---|---|
| 테스트 파일 | `app/src/test/java/com/postcardmemory/ui/detail/SaveErrorDialogStructureTest.kt` (신규) |
| 테스트 수 | 8개(`@Test` 8개: 다이얼로그 7개 각각 1개 + 전체 개수·순서 확인 1개) |
| 새 테스트 도우미 | 클래스 내부 `private object Source`(소스 텍스트 앵커 슬라이싱), `assertSingleDialogWithConfirmOnly`/`assertResetFunction`/`assertTitleColor`(공통 구조 검증 3종) — 모두 이 파일 안에서만 쓰는 지역 도우미이며, 프로젝트 전역에 새 추상화를 추가하지 않음 |
| 검증 내용 | (1) 7개 앵커가 코드에 실제로 등장하는 순서대로 정확히 1회씩 존재, (2) 다이얼로그별 `AlertDialog`/`confirmButton` 정확히 1개, `dismissButton` 0개, `DialogProperties` 오버라이드 없음, (3) `onDismissRequest`와 `confirmButton`이 호출하는 함수명이 서로 같고 기대한 reset 함수와 일치, (4) 제목 색상이 성공/에러에 맞게 `BrutalBlack`/`BrutalCoral`, (5) 개별 차이(성공 다이얼로그의 고정 문구, 5개 에러의 `.message` 직접 노출, 사진 교체 실패만의 고정 안내문) |

### 왜 텍스트 기반 정적 구조 테스트인가 (6단계 판단 근거)

우선순위 1(기존 테스트 패턴 재사용)과 2(Compose UI 테스트)를 먼저 검토했다.

- **기존 테스트 패턴 재사용**: 이 프로젝트의 기존 테스트는 전부 "Context/Uri/Room에 의존하지 않는 순수 함수만 추출해 검증"하는 패턴이다(`ConfirmSaveLogicTest.kt`, `DraftRestoreLogicTest.kt` 등 상단 주석에 반복 명시). 그런데 이 7개 다이얼로그는 추출 가능한 순수 판정 로직이 없다 — "상태가 Error면 보여준다"는 조건 자체가 이미 원자적이라 더 쪼갤 게 없고, 진짜 검증 대상은 Composable의 실제 구조(버튼 개수, callback 배선)다. 그대로 재사용할 수 없었다.
- **Compose UI 테스트**: `app/build.gradle.kts`를 확인한 결과 `androidx.compose.ui:ui-test-junit4` 등 Compose UI 테스트 의존성이 프로젝트에 전혀 없다. 이를 추가하려면 build 설정 변경(새 테스트 의존성, 계측 테스트 러너 구성)이 필요한데, 이는 "이번 차수에서 허용되는 변경"(테스트 코드/도우미/fixture 추가, 아주 작은 가시성 변경) 범위를 넘는 인프라 도입이라 판단해 보류했다.
- 그래서 **우선순위 4(정적 구조 테스트)**를 선택했다. `DetailScreen.kt` 소스를 텍스트로 읽어 앵커 기반으로 각 다이얼로그 블록을 잘라내고, 정규식으로 "다이얼로그 개수·버튼 구성·callback이 호출하는 함수명"이라는 **텍스트 수준의 불변식**을 검증한다. 런타임 렌더링을 검증하지는 못하지만, 제3차에서 다이얼로그를 다른 파일로 옮길 때 이 텍스트 패턴이 새 위치에서도 유지되는지(또는 이 테스트를 의도적으로 갱신했는지)를 최소한으로 확인할 수 있다.

---

## 8. 자동 테스트가 불가능한 항목

| 항목 | 이유 | 회귀 위험 | 제3차 이후 확인 방법 | 실기기 검증 필요 여부 |
|---|---|---|---|---|
| 다이얼로그가 실제로 화면에 렌더링되는지 | Compose UI 테스트 인프라 없음(§7 근거) | 중 — 텍스트 구조가 맞아도 실제 컴포지션 트리에서 조건이 잘못 배치되면(예: 잘못된 `Box` 안에 넣어 clipping됨) 텍스트 테스트로는 못 잡음 | 제3차에서 다이얼로그를 새 컴포저블로 옮긴 직후, 각 상태를 하나씩 인위적으로 트리거해 실제 표시 확인 | 예 |
| 확인/뒤로가기/외부탭 클릭 시 실제 재구성(recomposition) 결과 | 위와 동일 | 낮음 — reset 함수 자체는 순수 상태 대입이라 부수효과 위험은 낮으나, "다이얼로그가 실제로 닫히는지"는 런타임 확인 필요 | 위와 동일 실기기 시나리오에 포함 | 예 |
| 6개 에러 상태가 동시에 발생했을 때 실제 겹침 렌더링 모습 | 인위적으로 여러 저장을 동시에 실패시키는 재현이 어렵고, 렌더링 결과는 런타임 확인만 가능 | 낮음(데이터 손상 없음, UX 문제) | 재현 필요 시 저장 함수에 임시 강제 실패를 넣어 실기기 확인(프로덕션 코드 변경 필요 — 이번 차수 범위 밖) | 예(선택적) |
| `imageError` 안내 문구가 실제 다국어/폰트 환경에서 잘리지 않는지 | 텍스트 레이아웃은 정적 테스트로 검증 불가 | 낮음 | 실기기 육안 확인 | 예(선택적) |

자동 테스트가 없다는 사실을 숨기지 않기 위해, 정적 구조 테스트가 검증하는 범위(텍스트 패턴·callback 배선)와 검증하지 못하는 범위(실제 렌더링·리컴포지션)를 명확히 구분해 위 표로 남긴다.

---

## 9. 자동검증 결과

| 검증 | 결과 |
|---|---|
| Kotlin 컴파일 | **실행하지 못함** — 저장소에 `gradlew`/`gradlew.bat`가 없고(`gradle/wrapper/gradle-wrapper.properties`만 존재, wrapper 스크립트·jar 없음), 이 환경에 전역 `gradle`도 설치돼 있지 않음(`gradle -v` → command not found). 대신 추가한 `SaveErrorDialogStructureTest.kt`를 직접 재독해 구문(중첩 `object`, `by lazy`, 구조 분해 람다, `check`/`error` 표준 함수, 이스케이프된 정규식)을 수동으로 재검토했으며, 기존 테스트 파일들의 import·구조 스타일과 일치함을 확인했다 |
| 관련 테스트(`SaveErrorDialogStructureTest`) | **실행하지 못함**(위와 동일 사유) — 앵커 문자열 7개 + 종료 앵커가 `DetailScreen.kt` 실제 소스에 예상 순서로 정확히 1회씩 존재함은 `Grep`으로 별도 확인함(§3-0) |
| 전체 단위 테스트 | **실행하지 못함**(위와 동일 사유) |
| 테스트 수 변화 | 8개 순증(신규 파일 1개) |
| 기존 테스트 실패 여부 | 확인 불가(실행 못함). 기존 테스트 파일은 이번 차수에서 전혀 건드리지 않았으므로(수정한 파일 없음) 회귀 가능성은 이론상 없음 |
| `DetailScreen.kt` 프로덕션 동작 변경 여부 | **변경 없음** — `git diff --stat` 결과 tracked 파일 변경 0건 |
| 저장·롤백 로직 변경 여부 | **변경 없음** |
| 예상하지 못한 파일 변경 | 없음(`git status --short` 결과 `.kotlin/`, `docs/`, 신규 테스트 파일 1개만 untracked) |

**이 항목은 제2차 완료 조건 중 "관련 테스트가 통과함", "전체 단위 테스트가 통과함", "Kotlin 컴파일이 통과함"을 자동으로는 충족하지 못했다는 뜻이다.** 코드 검토로 컴파일 가능성은 높다고 판단하지만, 성공했다고 단정하지 않는다. **사용자 검증 대기**: Android Studio에서 `SaveErrorDialogStructureTest`를 포함한 단위 테스트 실행 확인 필요.

---

## 10. 프로덕션 코드 변경 여부

**변경 없음.** `DetailScreen.kt`, `DetailViewModel.kt`를 포함한 모든 기존 프로덕션 Kotlin 파일은 이번 차수에서 1바이트도 수정하지 않았다(`git diff --stat` 결과 없음). 테스트 접근을 위한 최소 가시성 변경(`internal` 등)도 필요하지 않았다 — 정적 구조 테스트가 소스를 텍스트로 읽는 방식이라 프로덕션 코드의 가시성 자체를 건드릴 필요가 없었기 때문이다.

---

## 11. 제3차 분리 설계안

작업지시서 9단계의 권장 형태 중 **"의미가 같으면 통합, 다르면 분리"** 원칙에 따라 검토한 결과, 7개 다이얼로그는 전부 "상태 확인 알림창"이라는 동일한 의미를 가지므로(§4에서 확인한 대로 독립 유지가 필요한 개별 다이얼로그가 없음) **통합형 단일 컴포넌트**를 권장한다.

### 권장 새 파일
`app/src/main/java/com/postcardmemory/ui/detail/SaveResultAlertDialog.kt`

### 권장 Composable 구조

```kotlin
@Composable
fun SaveResultAlertDialog(
    title: String,
    titleColor: Color,
    body: String,
    onAcknowledge: () -> Unit,
)
```

- 원시 sealed 상태 타입(`ExportState`, `BackgroundUpdateState` 등)을 이 컴포넌트가 알 필요가 없다 — DetailScreen이 각 상태를 조건 분기하면서 이 4개 파라미터로 변환해 호출한다.
- `onAcknowledge` 하나만 받고 내부에서 `confirmButton`과 `onDismissRequest` 양쪽에 동일하게 연결한다(현재 동작과 100% 동일 — 7개 전부 확인/dismiss가 같은 함수를 호출하므로 파라미터를 둘로 나눌 이유가 없다. 만약 나중에 하나라도 둘을 다르게 처리해야 하는 다이얼로그가 생기면 그때 파라미터를 분리한다).

### 전달할 state
`title: String`, `titleColor: Color`, `body: String` — 3개 모두 호출부(DetailScreen)에서 각 상태값(`exportError.message` 등)으로부터 즉시 계산해 전달. 새 컴포넌트는 상태를 소유하지 않고 그리기만 한다.

### 전달할 callback
`onAcknowledge: () -> Unit` 1개. DetailScreen 쪽 호출부에서 `{ viewModel.resetXxxState() }`로 채운다 — **새 컴포넌트 내부에서 `viewModel`을 직접 조회하지 않는다.**

### 이동할 코드 범위
- `AlertDialog(...)` 호출 자체(제목/본문/확인 버튼 Composable 트리)만 새 파일의 `SaveResultAlertDialog`로 이동
- `DetailScreen.kt`에는 7개의 짧은 호출부만 남는다. 예:
  ```kotlin
  (exportState as? ExportState.Error)?.let { exportError ->
      SaveResultAlertDialog(
          title = "저장하지 못했어",
          titleColor = BrutalCoral,
          body = exportError.message,
          onAcknowledge = { viewModel.resetExportState() }
      )
  }
  ```
- `imageError` 케이스만 `body` 계산식이 `"사진을 바꾸지 못했어. 기존 사진은 그대로 유지했어.\n" + imageError.message`로 다른 것을 그대로 유지

### 이동하지 않을 로직
- 상태 조건 분기(`is X.Success`, `as? X.Error`)는 DetailScreen에 그대로 유지 — 새 컴포넌트는 "보여줄지 말지"를 스스로 판단하지 않는다(호출 여부 자체가 이미 조건문 안에 있으므로)
- `viewModel.reset*State()` 호출은 DetailScreen이 만든 람다 안에 그대로 유지
- 저장·롤백·Mutex·revision 로직은 대상이 아님(애초에 이 구간에 없음)

### 제3차 진행 시 반드시 지킬 것
- 이동 후 `SaveErrorDialogStructureTest.kt`가 더 이상 (구조가 달라졌으므로) 그대로 통과하지 못할 것이 예상됨 — 이는 정상이며, 제3차에서 이 테스트를 "새 파일 구조에 맞는 검증"으로 의도적으로 갱신해야 한다(테스트가 실패하는 것이 아니라, 검증 대상 파일이 바뀌었으므로 앵커 위치를 새 파일 기준으로 재정의). 이 테스트를 그냥 삭제하지 말고 갱신하는 방식으로 "동작 변경 없음"의 기준선으로 계속 활용할 것을 권고한다.
- 문구·색상·버튼 동작은 그대로 유지(§11 설계는 값의 재배치일 뿐, 값 자체는 바꾸지 않음)

---

## 12. 발견했지만 수정하지 않은 사항

| # | 사항 | 위치 | 영향 | 별도 작업 필요 여부 |
|---|---|---|---|---|
| 1 | "저장 에러 다이얼로그 7종"이라는 제1차 표현이 부정확 — 실제로는 에러 6종 + 성공 1종 | `DetailScreen.kt` 5878~5912(성공), 5914~6151(에러 6종) | 문서 표현 문제, 기능적 영향 없음. 이 문서에서 표현을 정정함(§3-0) | 아니오 |
| 2 | 6개 에러 상태(배경/사진/폰트/레이아웃/날짜형식/내보내기)가 서로 독립적이라 이론상 동시에 여러 다이얼로그가 겹쳐 뜰 수 있음, 우선순위 로직 없음 | `DetailScreen.kt` 5878~6151 전체 | UX 혼란 가능성(데이터 손상 없음) — 제1차에서도 유사 계열 지적 없었던 신규 발견 | 예 — 제3차 분리 시 설계에 반영할지, 별도 UX 개선 과제로 둘지 판단 필요. 이번 차수에서는 수정하지 않음 |
| 3 | `imageError` 다이얼로그만 고정 안내 문구가 붙는 이유가 코드 주석으로 설명돼 있지 않음(추정: 사진 손실 우려가 가장 크기 때문으로 보이나 근거 문서 없음) | `DetailScreen.kt` 5991~6031 | 낮음 — 의도 추정이 필요하다는 사실만 기록 | 아니오 |

제1차에서 발견한 두 위험(`revertToConfirmedState` 파일 미검증, `awaitPendingStyleSaves` 2초 타임아웃)은 이번 차수 범위와 무관해 재조사하지 않았으며, 제1차 보고서(§12)에 이미 기록돼 있다.

치명적 결함(데이터 손실/삭제/Migration 실패/크래시)에 해당하는 사항은 발견되지 않아 작업을 중단하지 않았다.

---

## 13. 대작업 진행 위치

```text
실제 작업일: 2026-08-06
완료 위치: 제2차 완료
다음 시작 위치: 제3차 1단계
전체 대작업: 제1차~제10차 중 제2차 완료
```

---

## 14. 반드시 유지할 기존 기능 — 영향 확인

이번 차수는 §13에 나열된 기존 기능(사진/배경/텍스트/스티커/도장 8종/낙서/Undo·Redo/저장 전반/공유/내보내기/미리보기/미래엽서/날짜 형식/Room 등) 중 **어느 것도 코드를 수정하지 않았으므로** 전부 영향 없음. 새로 추가한 것은 테스트 파일 1개와 문서 2개뿐이다.

---

## 15. 사용자 검증 대기

- Android Studio에서 `./gradlew testDebugUnitTest` 또는 IDE 내장 테스트 실행으로 `SaveErrorDialogStructureTest`의 8개 테스트가 실제로 컴파일·통과하는지 확인 필요(§9)
- 위 확인에서 만약 정규식/앵커 매칭이 예상과 다르게 동작해 실패하는 테스트가 있다면, 프로덕션 코드가 아니라 테스트 파일 쪽 문제이므로 이번 차수 범위 안에서 별도로 보고 후 수정 필요

---

## 16. 제1차 보고서 커밋 처리 (§8 관련)

작업지시서 §8은 "권장 방식: 제1차 보고서를 `Document detail editor structure audit` 메시지로 별도 커밋"을 제시하되, "예외: 현재 승인 범위상 커밋하지 않는 경우 untracked 상태를 보고하고 이후 별도 문서 커밋 대상으로 유지"도 함께 허용했다.

이번 세션에서는 **예외 경로를 택해 아직 커밋하지 않았다.** 이유:

- 이 프로젝트의 git 작업 방식은 "커밋 대상 diff를 사용자에게 먼저 보여주고 명시적 확인을 받은 뒤 commit+push를 함께 수행"하는 방식으로 굳어져 있다(과거 세션에서 반복 확인된 사용자 선호).
- 작업지시서 §16도 "push는 사용자의 기존 승인 범위와 현재 작업 규칙에 따른다"고 명시해, 커밋 자체를 이 문서만으로 자동 승인된 것으로 단정하지 않는 편이 안전하다고 판단했다.

현재 커밋 대기 중인 변경은 다음 두 그룹으로, **이미 명확히 분리돼 있다**(하나로 섞이지 않음):

- **커밋 후보 1** — 문서만: `docs/detail-structure-audit-2026-08-06.md`(제1차), `docs/detail-structure-audit-phase2-2026-08-06.md`(제2차, 이 파일)
- **커밋 후보 2** — 테스트만: `app/src/test/java/com/postcardmemory/ui/detail/SaveErrorDialogStructureTest.kt`

사용자가 확인 후 "커밋해줘"라고 지시하면, 작업지시서 §16이 제시한 커밋 메시지(`Document detail editor structure audit`, `Lock save error dialog behavior with tests`)로 두 커밋을 순서대로 만들고 각 커밋 후 `git status`와 테스트 결과(가능한 범위에서)를 다시 보고할 준비가 되어 있다.
