# 테스트 보호지도 — 82일차 최신화 (원본 79일차 마감)

확인일: 2026-09-23 (원본 79일차 확인일 2026-09-20) / 기준 브랜치: `feature/photo-sticker` / HEAD: `000e7cecfbda660f6d4f43f3355db14494479cb3`

기존 78~81일차 감사와 실제 실행 결과를 기능 중심으로 정리했고, 82일차에는 그중 현재 코드·검증 결과와 달라진 숫자·상태만 최신화했어. 82일차에도 새 안정성 감사나 테스트 실행은 하지 않았고, 이번 문서 작업으로 앱 동작이나 기존 엽서 데이터가 달라지지는 않아.

## 먼저 읽는 지도

**계산·문자 저장·개별 파일 저장은 꽤 믿을 만해. 실제 화면 조작, 화면을 나가는 순간, Android 사진 처리는 자동검증 공백이 커.** 아래 등급은 테스트가 다루는 범위에 대한 평가야. 오류가 없다는 보장이나 코드 커버리지 측정값은 아니야.

| 기능 | 보호 수준 | 수동 확인 | 핵심 경계 |
|---|---|---|---|
| 엽서 편집 데이터 / 초안 | 강함 | 아니오: 데이터·파일 단위 | 화면 이탈·재진입은 별도 항목 |
| 상세 화면 저장 | 중간 | 예 | 실제 완료 버튼부터 저장·복원까지 공백 |
| 배경색 / 스타일 저장 경합 | 중간 | 예 | 실제 연결 계측 2건 80~81일차 실행 통과, replica 25개는 여전히 별개 |
| 파일 저장 / 삭제 | 중간 | 예: 전체 삭제 흐름 | 개별 파일 helper·DB 삭제 gate는 강함(81일차 instrumentation), 고아 파일 정리는 진단 전용 |
| Room database / migration | 중간 | 아니오: 별도 Android 자동검증 필요 | emulator 실제 실행 확인(80일차), CI 자동 실행은 아직 없음 |
| 뒷면 작성 / 저장 / 렌더링 | 중간 | 예 | 가장 구체적인 UI 계측이 있지만 최신 실행 부족 |
| 앞면 preview / exporter | 중간 | 예 | 수치 계산은 직접 검사, 실제 그림 비교 부족 |
| 방문 기록 / 달력 / 자정 처리 | 중간 | 예 | 계산·파일은 강함, 화면 갱신은 간접 검사 |
| 미래 우체통 | 중간 | 예 | 도착 계산은 직접 검사, 실제 묶음 개봉은 공백 |
| 갤러리 | 중간 | 예 | 검색·달력 계산과 실제 조작은 별개 |
| 스티커 / 테이프 / 낙서 | 중간 | 예 | 데이터·수치는 강함, 터치·그리기는 공백 |
| 카메라 / 사진 처리 | 약함 | 예 | Android 실제 처리 연결 부족 |
| 앱 시작 | 약함 | 예 | 시작 흐름은 주로 구조 검사 |
| navigation: 화면 이동 | 약함 | 예 | 실제 화면 이동 자동검증 없음 |
| ViewModel / lifecycle: 화면 상태 수명 | 약함 | 예 | 일부 helper·계측 외에는 replica 중심 |
| 실제 Compose interaction: 버튼·터치 | 약함 | 예 | 뒷면 표시 3건(81일차 실제 실행 통과) 외 상호작용 공백 |

미확인 등급을 붙인 기능은 없어. **instrumentation(emulator) 13건은 80~81일차에 검증 전용 emulator에서 실제로 실행해 13/13 통과를 확인했어 — 다만 이 실행은 로컬이고 GitHub Actions CI에는 아직 포함되지 않았어.** 80일차부터 push/PR 시 JVM 테스트·빌드는 GitHub Actions로 자동 실행돼 — 아래 "자동 실행 여부" 참고. 수동 확인 표시는 향후 해당 기능 변경 시 참고하는 지도이며, 오늘 전부 다시 확인하라는 요청은 아니야.

## 테스트를 읽는 기준

- **강함**: 실제 production 코드(앱이 사용하는 코드)를 직접 검사하고 주요 실패 상황도 어느 정도 다뤄.
- **중간**: 일부 직접 검증은 있지만 중요한 실제 Android·화면 경로가 비어 있어.
- **약함**: 구조 검사나 Fake 중심이거나 실제 사용 상황 검증이 거의 없어.
- **미확인**: 현재 자료로는 판단 근거가 부족해.

| 테스트 방식 | 무엇을 증명하나 | 무엇을 증명하지 못하나 |
|---|---|---|
| production 직접 테스트 | 앱과 같은 함수의 입력·출력 또는 저장 결과 | 그 함수를 실제 화면이 올바르게 사용하는지 전부 |
| Fake / replica | 가짜 저장소·복제 모델에서 설계한 경합 규칙 | 실제 DetailViewModel 동작 |
| 구조 테스트 | 소스의 선언·등록·호출 형태와 순서 | 실제 버튼 클릭·화면 표시 결과 |
| Compose UI | 실제 Compose 화면의 표시·측정 등 검사한 시나리오 | 검사하지 않은 화면·터치·navigation |
| instrumentation | Android 환경의 Room·Bitmap·ViewModel 등 | 존재·컴파일만으로 실제 실행 성공 |

JVM은 일반 컴퓨터에서 실행하는 테스트이고, instrumentation은 Android 환경에서 실행하는 테스트야. Compose UI 3건은 instrumentation 13건 안에 포함돼. 구조·replica 역시 JVM 750건 안에 포함되므로 서로 더해서 총수로 쓰면 안 돼.

현재 실측은 JVM `@Test` 750개, 테스트를 담은 파일 80개와 공용 helper 파일 1개야. 테스트 클래스 XML은 81개여서 과거 문서의 “81 files”와 소스 파일 수를 혼동하면 안 돼. 구조 테스트는 148개(구조 전용 파일에 있는 140개 + 혼합 파일에 있는 8개), 명시적 Fake/replica는 최소 28개이며 핵심 DetailViewModel replica는 25개야. instrumentation은 13개/6파일(81일차에 `PostcardDeletionOrchestrationTest` 3건 추가), Compose UI는 3개, Robolectric은 없어.

JVM 750개는 81일차에 실제로 재실행해 750/750 통과를 확인했고, 82일차에는 재검증 없이 그 결과를 그대로 썼어. instrumentation 13개는 80~81일차에 검증 전용 emulator(API 37)에서 실제로 전부 실행해 **13/13 통과**를 확인했어 — 존재·컴파일만이 아니라 실제 실행 결과야. 다만 이 실행은 GitHub Actions CI가 아니라 로컬 emulator에서 이뤄졌고, CI는 여전히 instrumentation을 자동 실행하지 않아(아래 "자동 실행 여부" 참고).

근거: [Gradle 테스트 설정](../../app/build.gradle.kts), [구조 테스트의 도입 이유](../../app/src/test/java/com/postcardmemory/testsupport/StructureTestSource.kt), [79일차까지 원문 기록](archive/HANDOFF-through-2026-09-20-before-close.md), [과거 계측 실행과 사고 기록](archive/HANDOFF-through-2026-09-12.md).

## 기능별 상세 지도

### 기능: 엽서 편집 데이터 / 초안

- **보호 수준:** 강함 — 데이터 변환과 개별 초안 파일 저장 범위.
- **현재 보호하는 테스트:** `PostcardEditDraftTest`, `PostcardDraftStorageTest`, `ConfirmedEditStateStorageTest`, `DraftRestoreLogicTest`.
- **실제 production 직접 검증:** 초안 저장 형식 변환, 옛 형식 읽기, 손상된 항목 처리, revision(저장 순서 번호), 실제 임시 파일 저장·덮어쓰기·읽기 실패 시 보존.
- **간접 검증:** 화면 이탈 중 초안 flush(대기 중 저장을 즉시 수행)는 별도 replica와 helper 테스트로 일부 확인해.
- **현재 믿어도 되는 것:** 검증한 데이터 형식과 파일 실패 조건에서는 기존 초안을 보존하고 다시 읽는 방어가 있어.
- **아직 믿으면 안 되는 것:** 화면에서 편집한 모든 상태가 초안에 빠짐없이 전달되는지, 화면 종료·재생성 시 전체 복원이 되는지.
- **수동 확인 필요:** 아니오 — 직렬화·파일 실패를 사용자가 재현할 필요는 없어. 화면 재진입 확인은 상세 저장·lifecycle 항목에 포함해.

근거: [초안 테스트](../../app/src/test/java/com/postcardmemory/utils/PostcardDraftStorageTest.kt), [production 초안 저장](../../app/src/main/java/com/postcardmemory/utils/PostcardDraftStorage.kt).

### 기능: 상세 화면 저장

- **보호 수준:** 중간.
- **현재 보호하는 테스트:** `ConfirmSaveLogicTest`, `ConfirmSaveHistoryClearStructureTest`, `UpdateMessageSaveMutexStructureTest`, `ExitSaveTimeoutTest`, `PostcardBackSaveTest`.
- **실제 production 직접 검증:** 완료 판정 함수, 저장 대기 시간과 저장 수명을 분리하는 helper. 뒷면 일부는 실제 ViewModel·Repository·Room을 쓰는 계측이 있어.
- **간접 검증:** 완료 뒤 편집 이력 초기화·글귀 저장 Mutex(저장 순서를 한 줄로 세우는 장치)·오류 안내는 구조 검사. 이탈 저장에는 replica도 있어.
- **현재 믿어도 되는 것:** 개별 저장 성공 여부를 합쳐 완료를 판정하는 규칙과 저장 helper의 일부 실패 방어.
- **아직 믿으면 안 되는 것:** 실제 완료 버튼에서 모든 꾸미기 파일 저장과 초안 정리까지 끝나는 전체 과정, 모든 저장 필드의 복원.
- **수동 확인 필요:** 예 — 향후 변경 빌드에서 대표 꾸미기 저장 후 재진입, 완료/원래대로, 오류 안내 확인. 의도적인 저장 실패는 별도 테스트 환경의 자동검증 대상이야.

근거: [완료 판정 테스트](../../app/src/test/java/com/postcardmemory/ui/detail/ConfirmSaveLogicTest.kt), [실제 상세 저장](../../app/src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt).

### 기능: 배경색 / 스타일 저장 경합

- **보호 수준:** 중간 (계측 2건은 80~81일차 emulator 실행으로 통과 확인됨).
- **현재 보호하는 테스트:** `BackgroundColorSaveRaceTest` 11개, `StyleSaveRaceTest` 3개, `BackgroundColorSaveJobStructureTest`, `PostcardBackgroundColorSaveRaceTest` 계측 2개.
- **실제 production 직접 검증:** 실제 ViewModel·Room으로 오래된 배경색 저장 취소와 실패 후 다음 저장을 검사하는 테스트가 존재해. **`PostcardBackgroundColorSaveRaceTest` 2/2는 80~81일차 검증 전용 emulator(API 37)에서 실제로 통과했어.**
- **간접 검증:** JVM replica는 최신 값 재읽기·취소·실패 복원 규칙을 설명해. 실제 배경색 코드의 이전 Job 취소 여부는 구조 검사도 있어.
- **현재 믿어도 되는 것:** 경합 설계에 대한 실행 가능한 모형이 있고 production 연결 검사 일부가 작성돼 있어.
- **아직 믿으면 안 되는 것:** replica 25개를 실제 ViewModel 통과 25개로 해석하는 것. 배경색 replica에는 production과 달리 이전 Job을 취소하지 않는 모형과 현재 UI 호출자가 없는 배경 이미지 교체 가정이 있어.
- **수동 확인 필요:** 예 — 색을 연속 변경하거나 슬라이더를 조절한 뒤 재진입해 마지막 값 확인. 드문 실패·취소 순서는 수동 확인만으로 보장할 수 없어.

근거: [배경색 replica](../../app/src/test/java/com/postcardmemory/ui/detail/BackgroundColorSaveRaceTest.kt), [실제 연결 계측](../../app/src/androidTest/java/com/postcardmemory/PostcardBackgroundColorSaveRaceTest.kt).

### 기능: 파일 저장 / 삭제

- **보호 수준:** 중간 — 개별 파일 저장·소유권 판정과 DB 삭제 gate(81일차부터)는 강함, 고아 파일 정리는 진단 전용으로 약함.
- **현재 보호하는 테스트:** `AtomicFileReplaceTest`, `AppFileOwnershipTest`, `ProvisionalFileTest`, `PostcardDeletionManagerTest`, `OrphanFileDiagnosticsTest`, `PostcardTempCleanupTest`, 공유 캐시 정리 테스트, `PostcardDeletionOrchestrationTest`(instrumentation, 81일차 신규).
- **실제 production 직접 검증:** 실제 임시 파일로 교체·쓰기 실패·취소·소유 범위·엽서별 파일 정리·오래된 캐시 정리를 검사해. **81일차부터는 실제 Room + 실제 filesDir로 `DB 삭제 실패 → 파일 삭제 0건·DB 행 유지`, `DB 삭제 성공 → 소유 파일 실제 삭제`, `동일 삭제 재호출 → 멱등성 안전`까지 3건을 검증 전용 emulator에서 실제 실행해 3/3 통과를 확인했어.**
- **간접 검증:** `PostcardDeletionManagerTest`는 이름과 달리 파일 정리 helper만 호출해(JVM, 실제 Room 없이 파일 helper만 검사).
- **현재 믿어도 되는 것:** 검증한 파일 경계와 실패 조건에서 다른 엽서 파일·앱 외부 경로를 보호하는 안전망, 그리고 DB 삭제가 실패하면 사용자 파일이 절대 지워지지 않는다는 것.
- **아직 믿으면 안 되는 것:** DB 삭제 성공 직후 파일 정리 전에 프로세스가 종료돼 생기는 고아 파일(DB 행 없음 + 파일 잔존)의 자동 방지 — 이건 설계상 수용한 약한 위험이고 `OrphanFileDiagnostics`는 읽기 전용 진단이지 자동 삭제가 아니야. Android URI 권한·기기 파일시스템 모든 조건도 아직 공백이야.
- **수동 확인 필요:** 예 — 향후 삭제 변경 시 별도 테스트용 엽서의 삭제·나머지 엽서 표시 확인. DB 성공/실패 gate 자체는 이제 자동 재현되므로 그 부분을 사용자 수동 QA로 떠넘기지 않아.

근거: [파일 정리 테스트 범위](../../app/src/test/java/com/postcardmemory/utils/PostcardDeletionManagerTest.kt), [실제 삭제 순서](../../app/src/main/java/com/postcardmemory/utils/PostcardDeletionManager.kt), [DB-우선 삭제 gate 계측](../../app/src/androidTest/java/com/postcardmemory/PostcardDeletionOrchestrationTest.kt).

### 기능: Room database / migration

- **보호 수준:** 중간 (80일차부터 실제 emulator 실행으로 한 단계 더 검증됨).
- **현재 보호하는 테스트:** `PostcardMigrationRegistrationStructureTest`, `PostcardBackMigrationTest`, `PostcardFullMigrationChainTest`.
- **실제 production 직접 검증:** Android 계측 3건에 실제 migration SQL, 기존 행 보존, 최신 DAO 읽기·쓰기, 재개방 검사가 작성돼 있어. **80일차에 emulator(`PostcardMemory_Test`, API 37)에서 3건 전부 실제 실행해 통과 확인함.**
- **간접 검증:** migration 선언·등록 연속성과 schema 파일 존재는 구조 검사. 형태 자체가 계약이라 유효한 안전망이야.
- **현재 믿어도 되는 것:** migration 등록 누락 감시와 실제 SQL을 검사할 테스트 기반이 있고, 최신 코드 기준 1→19 전체 chain 실행이 실제로 통과함을 확인했어.
- **아직 믿으면 안 되는 것:** v1은 과거 코드에서 복원한 수동 schema이고 중간 버전 모든 실제 사용자 데이터 조합을 대표하지 않아. 이 실행은 CI 자동 실행이 아니라 로컬 emulator에서의 1회성 수동 실행이라 push마다 자동 보호되지는 않아.
- **80일차에 실제로 발견·수정한 버그:** `message`/`futureMailDeliverAt`/`envelopeStyle` 3개 컬럼의 migration SQL DEFAULT 선언이 entity/schema export와 어긋나 있었어. 버전 증가·새 migration 없이 기존 migration SQL 3줄만 최소 수정했고, 재실행으로 통과를 확인했어. 상세는 [CI-AUDIT-80.md](CI-AUDIT-80.md).
- **수동 확인 필요:** 아니오 — SQL 보존 검증은 이제 실제 emulator 실행으로 확인됨. 다만 이 실행은 CI에 편입되지 않았으니 다음에 migration을 또 건드리면 다시 실제 emulator에서 확인해야 해.

근거: [전체 migration 계측](../../app/src/androidTest/java/com/postcardmemory/PostcardFullMigrationChainTest.kt), [18→19 계측](../../app/src/androidTest/java/com/postcardmemory/PostcardBackMigrationTest.kt), [migration 수정](../../app/src/main/java/com/postcardmemory/data/PostcardDatabase.kt).

### 기능: 뒷면 작성 / 저장 / 렌더링

- **보호 수준:** 중간 (81일차 emulator 실행으로 계측 4건 전부 통과 확인됨).
- **현재 보호하는 테스트:** `PostcardBackFaceTest`, `PostcardWritingRecordTest`, `PostcardBackSaveTest`, `PostcardBackRenderingTest`.
- **실제 production 직접 검증:** 문구·최초 작성 시각 규칙, 실제 ViewModel·Room 저장 실패/복원, Compose 뒷면 캡처, 공유 PNG·갤러리 출력의 Bitmap 동일성, 장문 fitting(글자가 들어가도록 크기 맞춤). **`PostcardBackSaveTest`(2/2)와 `PostcardBackRenderingTest`(3/3)는 81일차 검증 전용 emulator(API 37)에서 실제로 통과했어.**
- **간접 검증:** 계측의 DAO 대역은 특정 쓰기만 지연·실패시키며 핵심 저장 처리는 production이 수행해.
- **현재 믿어도 되는 것:** 순수 작성 규칙은 직접 검사돼. Android 검증용 테스트 4건도 최신 코드 기준으로 실제 통과했어.
- **아직 믿으면 안 되는 것:** 실제 키보드 입력·커서·모든 글꼴 크기·모든 본문 형태 — 계측은 대표 시나리오만 검사해.
- **수동 확인 필요:** 예 — 본문·추신 입력, 재진입, 장문 읽기, 현재 뒷면 공유 결과의 가독성 확인.

근거: [뒷면 렌더링 계측](../../app/src/androidTest/java/com/postcardmemory/PostcardBackRenderingTest.kt), [뒷면 저장 계측](../../app/src/androidTest/java/com/postcardmemory/PostcardBackSaveTest.kt).

### 기능: 앞면 preview / exporter

- **보호 수준:** 중간.
- **현재 보호하는 테스트:** `PostcardOverlayExportLogicTest`, `StickerPositionCalculationsTest`, `PostcardRenderSpecLayoutStyleTest`, `LabelStickerLayerOrderStructureTest`.
- **실제 production 직접 검증:** overlay(꾸미기 요소) 좌표·크기·정규화와 출력용 데이터 조립.
- **간접 검증:** 앞면 레이어 순서는 구조 검사. **실제 앞면 레이어 순서의 화면 검증은 구조 검사가 사실상 유일한 방어선**이야.
- **현재 믿어도 되는 것:** 테스트한 수치 계산과 측정값이 없을 때 기본 크기 처리.
- **아직 믿으면 안 되는 것:** preview와 exporter가 최종적으로 같은 그림을 그리는지, 글자·EXIF·사진 decoding·잘림·MediaStore 전체 과정.
- **수동 확인 필요:** 예 — 대표 앞면을 preview와 공유/저장 이미지로 비교해 위치·크기·순서·잘림을 확인해.

근거: [출력 계산 테스트](../../app/src/test/java/com/postcardmemory/ui/detail/PostcardOverlayExportLogicTest.kt), [production exporter](../../app/src/main/java/com/postcardmemory/utils/PostcardImageExporter.kt).

### 기능: 방문 기록 / 달력 / 자정 처리

- **보호 수준:** 중간 — 계산과 파일은 강함, 실제 화면 연결은 간접 보호.
- **현재 보호하는 테스트:** `VisitRecordTest`, `VisitRecordStorageTest`, `VisitHistoryStorageTest`, `VisitCalendarTest`, `VisitCalendarMonthLoadingTest`, `DayBoundaryTest`, `VisitDayBoundaryDefinitionTest`.
- **실제 production 직접 검증:** 같은 날 중복 방문 억제, 날짜·월 경계, 읽기 실패 시 기존 기록 보존, 실제 방문 표식 파일, 월별 읽기, 자정 신호 helper.
- **간접 검증:** 화면의 자정 신호 연결·방문 생성 분리는 구조 검사. 새 소비자가 모두 감시된다는 전역 보장으로 해석하면 안 돼.
- **현재 믿어도 되는 것:** 검증한 날짜와 파일 조건에서 방문 수·월별 기록 계산.
- **아직 믿으면 안 되는 것:** 앱을 켜 둔 실제 자정 전환·절전 복귀·화면 재구성에서 모든 표시가 갱신되는지.
- **수동 확인 필요:** 예 — 다른 달 탐색과 정상 재실행 표시 확인. 자정은 자연적으로 넘길 기회에 관찰하며 기기 시간을 강제로 바꾸지 않아.

근거: [방문 파일 테스트](../../app/src/test/java/com/postcardmemory/utils/VisitRecordStorageTest.kt), [월별 연결 검사](../../app/src/test/java/com/postcardmemory/ui/gallery/VisitCalendarMonthLoadingTest.kt).

### 기능: 미래 우체통

- **보호 수준:** 중간.
- **현재 보호하는 테스트:** `FutureMailLogicTest`, `FutureMailOpeningGuardTest`, `FutureMailTimeBoundaryTest`.
- **실제 production 직접 검증:** 도착·D-day·진행률·그룹화, 실패·취소 뒤 개봉 중 표시를 풀어 재시도하게 하는 가드.
- **간접 검증:** **묶음 전체를 한 번에 개봉하는 DB 호출과 화면 자정 연결은 구조 검사가 유일한 자동 방어선**이야.
- **현재 믿어도 되는 것:** 지정 날짜에 대한 계산과 개봉 재시도 가드 규칙.
- **아직 믿으면 안 되는 것:** 실제 ViewModel+Room 묶음 개봉의 원자성(전부 성공하거나 전부 유지), 실제 화면 표시·이동.
- **수동 확인 필요:** 예 — 테스트용 우편의 발송·도착·개봉·갤러리 복귀. 중간 DB 실패의 원자성은 별도 자동검증이 필요해.

근거: [가드와 구조 검사](../../app/src/test/java/com/postcardmemory/ui/futuremail/FutureMailOpeningGuardTest.kt), [production 우체통](../../app/src/main/java/com/postcardmemory/ui/futuremail/FutureMailboxViewModel.kt).

### 기능: 갤러리

- **보호 수준:** 중간.
- **현재 보호하는 테스트:** `GallerySearchFilterTest`, `GalleryCalendarCellsTest`, `GalleryMemoryDensityTest`, `GalleryPagerTargetIndexTest`, `GalleryRetroClockTest`, 보기 선택·월 grid 구조 테스트.
- **실제 production 직접 검증:** 검색 결과·순서 유지, 달력 칸, 월별 엽서 수, 페이지 목표 index, 시계 표시 계산.
- **간접 검증:** **보기 선택·drawer·월 grid의 UI 배선은 구조 검사 중심의 유일한 자동 방어선**이야.
- **현재 믿어도 되는 것:** 테스트 입력에 대한 검색·집계·달력·표시 계산.
- **아직 믿으면 안 되는 것:** 실제 선택·drag·스크롤·보기 전환·카드 animation·양 목장/연못 놀이 동작.
- **수동 확인 필요:** 예 — 3열 목록, 검색, 보기 전환, 카드 진입, 해당 작업에서 바뀐 놀이 동작 확인.

근거: [검색 테스트](../../app/src/test/java/com/postcardmemory/ui/gallery/GallerySearchFilterTest.kt), [production 갤러리](../../app/src/main/java/com/postcardmemory/ui/gallery/GalleryScreen.kt).

### 기능: 스티커 / 테이프 / 낙서

- **보호 수준:** 중간 — 데이터·수치 범위는 강함.
- **현재 보호하는 테스트:** `LabelStickerItemTest`, `MaskingTapeItemTest`, `TextStickerItemTest`, `PostcardSealItemTest`, `DoodleStrokeTest`, `DoodleLineSnapTest`, 위치·export 계산 테스트.
- **실제 production 직접 검증:** 저장 형식·옛 값 호환, 위치/크기, 선 스냅, 지우개 충돌 계산, 테이프 윤곽점 등.
- **간접 검증:** **생성·편집 toolbar·다이얼로그 배선은 구조 검사 중심**. ML Kit 배경제거 취소 3건은 처리 모형을 복제해.
- **현재 믿어도 되는 것:** 검사한 꾸미기 데이터와 기하 계산의 경계 조건.
- **아직 믿으면 안 되는 것:** 실제 터치·회전·Undo/Redo 전체 흐름, 사진 URI 영속성, 실제 배경제거 결과.
- **수동 확인 필요:** 예 — 추가·이동·회전·크기·삭제·되돌리기와 재진입/공유 결과.

근거: [낙서 테스트](../../app/src/test/java/com/postcardmemory/utils/DoodleStrokeTest.kt), [배경제거 replica](../../app/src/test/java/com/postcardmemory/ui/detail/StickerBackgroundRemovalCancellationTest.kt).

### 기능: 카메라 / 사진 처리

- **보호 수준:** 약함.
- **현재 보호하는 테스트:** `ProvisionalFileTest`가 임시 파일 실패 정리의 일부 공통 규칙을 보호해. 촬영·crop·EXIF·색 추출의 실제 경로 테스트는 기존 감사에서 확인되지 않았어.
- **실제 production 직접 검증:** 공통 임시 파일 helper 수준.
- **간접 검증:** 배경제거 취소 replica가 있지만 카메라나 ML Kit 자체 실행은 없어.
- **현재 믿어도 되는 것:** helper가 검사한 실패 정리 규칙만.
- **아직 믿으면 안 되는 것:** 촬영부터 crop·Bitmap 압축·DB 등록·URI 복사·EXIF 회전까지 전체 과정.
- **수동 확인 필요:** 예 — 테스트용 촬영, 회전 사진 crop, 취소, 결과 재진입과 사진 표시 확인.

근거: [임시 파일 테스트](../../app/src/test/java/com/postcardmemory/utils/ProvisionalFileTest.kt), [카메라 production](../../app/src/main/java/com/postcardmemory/ui/camera/CameraViewModel.kt).

### 기능: 앱 시작

- **보호 수준:** 약함.
- **현재 보호하는 테스트:** `AppIntroMessageLogicTest`, `AppIntroVisitPostmarkStructureTest`, 방문 파일·임시 파일 정리 helper 테스트.
- **실제 production 직접 검증:** 인트로 문구 선택과 방문/정리 helper.
- **간접 검증:** **앱 시작 시 방문 호출·인트로 표시 연결은 구조 검사가 주된 자동 방어선**이야.
- **현재 믿어도 되는 것:** 문구 선택과 개별 helper 규칙.
- **아직 믿으면 안 되는 것:** Application부터 Activity·인트로·갤러리로 이어지는 실제 시작과 DI(필요한 객체를 연결하는 설정).
- **수동 확인 필요:** 예 — 정상 시작·재실행·백그라운드 복귀와 인트로 중복 표시 확인.

근거: [인트로 구조 검사](../../app/src/test/java/com/postcardmemory/ui/intro/AppIntroVisitPostmarkStructureTest.kt), [앱 시작](../../app/src/main/java/com/postcardmemory/MainActivity.kt).

### 기능: navigation — 화면 이동

- **보호 수준:** 약함.
- **현재 보호하는 테스트:** 이탈 저장 replica와 달력 내부 탐색 단계의 순수 함수 검사. 실제 navigation 테스트는 없어.
- **실제 production 직접 검증:** 내부 탐색 상태 계산 일부만.
- **간접 검증:** replica에서 scope 취소를 화면 이동에 대응시켜.
- **현재 믿어도 되는 것:** 검사한 내부 단계 계산.
- **아직 믿으면 안 되는 것:** 실제 뒤로가기·이동 경로·화면 제거·빠른 연속 입력에서의 저장 순서.
- **수동 확인 필요:** 예 — 아이콘/시스템 뒤로가기, 갤러리↔상세↔카메라/우체통 이동 확인.

근거: [이탈 모형](../../app/src/test/java/com/postcardmemory/ui/detail/DetailScreenExitSaveGuaranteeTest.kt), [navigation production](../../app/src/main/java/com/postcardmemory/MainActivity.kt).

### 기능: ViewModel / lifecycle — 화면 상태가 살아 있는 시간

- **보호 수준:** 약함.
- **현재 보호하는 테스트:** 핵심 replica 25개, `ExitSaveTimeoutTest`, 실제 ViewModel 계측 4개.
- **실제 production 직접 검증:** 저장 timeout helper, 배경색·뒷면 일부 ViewModel 저장 시나리오가 계측으로 작성돼 있어.
- **간접 검증:** Android ViewModel 제거를 가짜 scope 취소로 치환한 모형. 실제 이탈 scope 주입·프로세스 종료는 검사하지 않아.
- **현재 믿어도 되는 것:** timeout helper가 기다림과 저장을 분리하는 규칙.
- **아직 믿으면 안 되는 것:** helper 검사를 실제 화면 이탈 전체의 보장으로 보는 것. 이탈 replica의 일부 설명·초안 flush 모형은 현재 production과 차이가 있어.
- **수동 확인 필요:** 예 — 편집 후 뒤로가기·회전·백그라운드 복귀·재진입. 강제 종료/저장 실패는 별도 안전한 테스트 환경의 후속 후보야.

근거: [실제 helper 테스트](../../app/src/test/java/com/postcardmemory/ui/detail/ExitSaveTimeoutTest.kt), [현재 이탈 저장](../../app/src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt).

### 기능: 실제 Compose interaction — 버튼·터치 조작

- **보호 수준:** 약함 (뒷면 표시 3건은 81일차부터 실제 통과, 나머지 편집 상호작용은 여전히 공백).
- **현재 보호하는 테스트:** 뒷면 Compose UI 3개(`PostcardBackRenderingTest`)와 여러 UI 구조 검사.
- **실제 production 직접 검증:** 뒷면 표시·캡처·글자 측정용 Compose를 실행하는 테스트가 있어. 80일차 emulator(API 37) 첫 실행에서는 3건 전부 `NoSuchMethodException: android.hardware.input.InputManager.getInstance`로 실패했어(Espresso가 `onIdle` 처리 중 쓰는 hidden API가 이 API 레벨과 안 맞는 test infrastructure 문제, production 문제 아님). **81일차에 `espresso-core`를 3.5.1→3.7.0, `androidx.test.ext:junit`을 1.1.5→1.3.0으로 두 테스트 dependency만 올려 이 hidden API 호출을 제거했고, 검증 전용 emulator(API 37)에서 3/3 통과를 확인했어.**
- **간접 검증:** **대부분의 편집 버튼·toolbar·picker·dialog는 구조 검사가 유일한 자동 방어선**이야.
- **현재 믿어도 되는 것:** 선언·호출 형태가 유지되는지, 그리고 뒷면 Compose 표시·캡처·글자 측정 3건은 API 37 emulator에서 실제로 통과한다는 것.
- **아직 믿으면 안 되는 것:** 구조 검사 통과가 실제 클릭·drag·포커스·키보드·접근성·화면 크기별 정상 조작을 증명한다는 해석. 뒷면 3건 외 나머지 편집 버튼·제스처는 여전히 자동 실행 검증이 없어.
- **수동 확인 필요:** 예 — 해당 변경의 실제 버튼과 gesture, 비활성 상태, 작은 화면·키보드 겹침 확인.
- **emulator 환경 메모:** draw/capture 계열 instrumentation은 emulator가 `mWakefulness=Asleep`이면 실제 draw pass가 없어 저장 timeout이 발생할 수 있어(production 문제 아님, `emulator / OS environment`). 실행 전 `adb shell dumpsys power`로 확인해.

근거: [Compose 계측](../../app/src/androidTest/java/com/postcardmemory/PostcardBackRenderingTest.kt), [구조 검사의 한계와 이유](../../app/src/test/java/com/postcardmemory/testsupport/StructureTestSource.kt).

## replica와 구조 테스트를 유지해서 읽는 법

핵심 DetailViewModel replica 25건은 삭제 대상이라는 뜻이 아니야. **설계 의도와 경합 규칙을 설명하는 데 가치가 있지만 실제 production 동작의 직접 증거는 아니야.** 배경색 계측 2건은 실제 production 검증 일부 존재로 따로 표시해. 뒷면 저장 계측은 다른 필드의 저장을 검사하므로 슬라이더 replica의 전체 대체가 아니야.

구조 테스트 148건도 역할을 나눠 읽어야 해. migration 등록, 공용 component 사용, 의존 관계처럼 형태가 계약이면 유효한 안전망이야. 버튼 반응·화면 갱신·레이어 결과를 대신 검사하면 간접 보호에 머물러. 직접 테스트와 구조 테스트를 묶어 볼 수는 있지만, 테스트 개수를 합쳐 보호가 강해졌다고 판단하지 않아.

## 자동 실행 여부

**80일차부터 바뀐 사실:** `feature/photo-sticker`에 push하거나 pull request를 열면 GitHub Actions(`.github/workflows/android-ci.yml`)가 다음을 자동 실행하고, 실제로 통과를 확인했어(2026-09-21, run 35563162247).

| 항목 | 자동 실행 | 근거 |
|---|---|---|
| JVM unit test 750개 (`testDebugUnitTest`) | 예 — GitHub Actions에서 실제 자동 실행 확인됨 | CI 성공 로그 |
| `assembleDebug` (앱 빌드) | 예 — 자동 실행 확인됨 | CI 성공 로그 |
| `assembleDebugAndroidTest` (Android 테스트 코드 컴파일) | 예 — 자동 실행 확인됨 | CI 성공 로그. **테스트 코드가 최신 소스 기준으로 컴파일된다는 뜻이지, 실제 Android 환경에서 실행됐다는 뜻이 아니야.** |
| instrumentation 13개가 CI(GitHub Actions)에서 자동 실행 | 아니오 | CI에는 emulator가 없어 `connectedDebugAndroidTest`를 넣지 않았어. 실제 실행은 로컬 검증 전용 emulator에서만 확인됐어(아래 참고). |
| lint | 아니오 | 오늘 범위 밖 |

79일차에는 이 표의 모든 항목이 "아니오"였어. 79일차 조사와 80일차 도입 과정은 [CI-AUDIT-79.md](CI-AUDIT-79.md), [CI-AUDIT-80.md](CI-AUDIT-80.md)를 확인해. **instrumentation의 "컴파일 자동검증됨"과 "CI에서 자동 실행됨"은 서로 다른 사실이니 혼동하면 안 돼.**

**같은 날 추가로 확인된 사실 — instrumentation 10건 최초 실제 실행(CI 아님, 로컬 emulator):** 80일차에 사용자가 Android Studio에서 테스트 전용 AVD `PostcardMemory_Test`(API 37)를 직접 부팅했고, `adb devices -l`로 실사용 기기가 없고 `emulator-5554` 하나만 연결된 것을 실행 전마다 재확인한 뒤 로컬 Gradle(`connectedDebugAndroidTest`)로 10건을 처음 실제 실행했어.

- **1차 결과: 5/10 성공.** `PostcardBackMigrationTest`(1/1), `PostcardBackSaveTest`(2/2), `PostcardBackgroundColorSaveRaceTest`(2/2)는 성공. `PostcardBackRenderingTest`(0/3)와 `PostcardFullMigrationChainTest`(0/2)는 실패.
- `PostcardFullMigrationChainTest` 실패는 **재현 가능한 실제 production Room migration 버그**였어(`message`/`futureMailDeliverAt`/`envelopeStyle` 세 컬럼의 migration SQL DEFAULT 선언이 entity/schema export와 어긋남). `PostcardDatabase.kt`의 관련 3개 migration SQL을 최소 수정(버전 증가·새 migration 없음, 기존 데이터 영향 없음)한 뒤 재실행해 **2/2 통과**로 확인했어. 상세 원인·diff는 [CI-AUDIT-80.md](CI-AUDIT-80.md)에 있어.
- `PostcardBackRenderingTest` 3건은 수정 전후 동일하게 실패해. 원인은 `NoSuchMethodException: android.hardware.input.InputManager.getInstance` — API 37이 최신 SDK라 현재 Espresso/androidx.test 버전과의 test infrastructure 호환 문제로 분류했고, production 문제가 아니라서 손대지 않았어.
- **최종(수정 후) 재실행: 7/10 성공, 3/10 실패(test infrastructure), 0 skipped.** 이 3건은 위 "실제 Compose interaction" 항목과 "Room database / migration" 항목의 등급·근거에 반영해야 할 최신 사실이었고(81일차에 해결, 아래 문단 참고), 각 항목에도 반영했어.
- 이 실행은 GitHub Actions CI가 아니라 **로컬**에서 사용자가 준비한 emulator 위에서 이뤄졌어 — 위 표의 "instrumentation이 CI에서 자동 실행"은 여전히 "아니오"인 게 맞아.

**81일차 추가 확인 — Espresso/API 37 수정 후 전량 재실행(CI 아님, 검증 전용 로컬 emulator):** `da78619`에서 `espresso-core`를 3.7.0으로, `androidx.test.ext:junit`을 1.3.0으로 올려 위 `PostcardBackRenderingTest` 3건 실패 원인이던 hidden API 호출을 제거했고, 같은 커밋에서 `PostcardDeletionOrchestrationTest` 3건(DB 삭제 실패 시 파일 미삭제·DB 행 유지, DB 삭제 성공 시 소유 파일 정리, 재호출 멱등성)을 추가했어. instrumentation 실행 전 `adb devices -l`로 검증 전용 `emulator-5554`만 연결된 걸 재확인했고, **13/13 전부 통과**했어 — `PostcardBackMigrationTest`(1/1), `PostcardFullMigrationChainTest`(2/2), `PostcardBackSaveTest`(2/2), `PostcardBackgroundColorSaveRaceTest`(2/2), `PostcardBackRenderingTest`(3/3), `PostcardDeletionOrchestrationTest`(3/3). 이 실행도 GitHub Actions CI가 아니라 로컬 검증 전용 emulator에서 이뤄졌고, `da78619`·`000e7ce` push 각각의 GitHub Actions run(`35701039918`, `35704566020`)은 JVM unit test·`assembleDebug`·`assembleDebugAndroidTest`(컴파일)만 성공을 확인했을 뿐 emulator instrumentation은 여전히 CI에 포함되지 않아.

## 마지막 요약

### 지금 테스트를 꽤 믿어도 되는 영역

- 초안·꾸미기 데이터의 저장 형식과 옛 데이터 읽기.
- 개별 파일의 덮어쓰기·실패 시 보존·소유 경계, DB 삭제 성공/실패에 따른 파일 정리 gate(81일차 instrumentation).
- 방문일·날짜·검색·달력·꾸미기 위치 같은 순수 계산.

이 신뢰는 해당 테스트를 실행해 통과했을 때의 검사 범위에 한정돼. push 자체가 자동 검사를 해 주지는 않아.

### 테스트 통과 후에도 수동 확인이 필요한 영역

- 상세 편집 저장 후 재진입과 실제 뒤로가기.
- 앞면·뒷면 preview와 저장/공유 이미지.
- 달력·우체통의 화면 갱신과 개봉.
- 스티커·테이프·낙서의 터치, 카메라 촬영·crop.

이는 오늘 재QA 요청이 아니라 향후 변경 시 확인할 지도야. 자동으로 재현 가능한 DB/파일 실패는 사용자 수동 QA로 떠넘기지 않아.

### 현재 자동검증 공백이 큰 영역

- 카메라·Android 사진/URI 처리 전체 과정.
- navigation·ViewModel 제거·프로세스 lifecycle.
- 실제 Compose 버튼·drag·키보드 상호작용.
- 앞면 최종 렌더링 비교, DB 실패와 파일 삭제가 연결된 전체 과정.
- instrumentation이 **CI에서 자동으로** 실행되는 것(여전히 없음). 실제 emulator 실행 자체는 80일차에 처음 확인했고(7/10 성공, 3/10은 API 37/Espresso 환경 문제), 81일차에 그 3건의 원인(Espresso hidden API)을 test dependency 갱신으로 해결하고 신규 삭제 테스트 3건을 더해 **13/13 통과**로 확정했어 — 위 "자동 실행 여부" 참고. push 시 JVM 테스트·빌드 자동 실행은 80일차에 해결됐어.

테스트 수와 실제 안전성은 부분적으로 일치해. **계산·직렬화·파일 helper에는 근거가 두껍지만, Android 화면으로 조립된 전체 앱과 자동 실행 보호까지 750개라는 숫자로 보장할 수는 없어.**
