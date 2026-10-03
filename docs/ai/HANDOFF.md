# HANDOFF — 92일차 선행 안전정비: 로컬 Android Emulator 폐기 + 실기기 instrumentation 안전 등급

확인일: 2026-10-03. 수동 표준 모드(92일차 선행 안전정비 수정지시서, 담당 Claude Code). 앱 기능·production·test 코드 변경 없이, 운영 규칙·workflow·테스트 지도에서 로컬 emulator 사용 경로를 없애고 실사용 기기를 기본 검증 환경으로 바꾸면서 사용자 데이터를 지키는 instrumentation 규칙을 정했어. **이 문서의 다음 후보는 실행 승인이 아니야.**

## 시작 상태

- 브랜치 `feature/photo-sticker`, 시작 HEAD `4a60314eb596cdd918a65585f207a8c66eda435c`("Make a forgotten decoration save result or directory fail loudly"), `git fetch` 후 origin 대비 `0/0`.
- 기존 미커밋 변경 `app/src/main/java/com/postcardmemory/utils/PostcardDeletionManager.kt` 1건: 정리 단계 주석 번호 `5.`→`4.`, `6~8.`→`5~7.` 2줄(코드 동작 무관). 이번 작업과 무관한 기존 변경으로 보고 수정·restore·stage·commit하지 않고 그대로 보존했어.
- 보호 untracked `.codex-config.candidate.toml`, `.kotlin/`와 ignore된 `.claude/settings.local.json` 보존.

## 현재 정책 (2026-10-03 사용자 결정)

- **로컬 Android Emulator 운영 폐기.** 사용자가 emulator SDK, hypervisor driver, `PostcardMemory_Test` AVD를 제거했고 Android SDK Platform-Tools(`adb`)는 유지했어. 저장소와 GitHub Actions 어디에도 emulator를 자동 생성·실행하는 스크립트는 없어(조사 확인). AVD 생성·부팅, `emulator -avd`, `avdmanager`, emulator 구성요소 재설치, emulator 기반 설치·instrumentation·메모리 측정·QA는 하지 않아. 사용자가 해당 작업에서 별도로 명시 승인할 때만 예외야.
- **로컬 검증 기본 환경 = 사용자가 연결한 실사용 기기.** 실제 엽서가 있는 보호 대상이라 읽기·관찰·비파괴 검증(`adb devices -l`, `dumpsys`, `meminfo`, log, 수동 QA)을 우선해.
- **instrumentation 20건 안전 등급**(현재 코드 기준 읽기 조사, 상세는 `TEST-COVERAGE-MAP.md`):
  - SAFE 7: `PhotoStickerEdgeStyleInstrumentedTest` 7
  - CONDITIONAL 6: `PostcardBackMigrationTest` 1, `PostcardFullMigrationChainTest` 2, `PostcardBackRenderingTest` 3
  - FORBIDDEN 7: `PostcardDeletionOrchestrationTest` 3, `PostcardBackSaveTest` 2, `PostcardBackgroundColorSaveRaceTest` 2
- SAFE 7건만 매번 serial 확인 → 클래스·데이터 영향 없음 명시 → 사용자 승인을 거쳐 `adb -s <serial> shell am instrument -w -r -e class ... com.postcardmemory.test/androidx.test.runner.AndroidJUnitRunner`로 실행할 수 있어. 나머지 13건은 실사용 기기에서 실행하지 않고 `실행 불가`로 기록해.
- Gradle `connected*` task(실행 후 uninstall 위험), `adb uninstall`, `pm clear`, 앱 데이터 초기화, 앱 삭제 후 재설치 우회는 금지야. `adb install -r`도 승인 후에만 하고 서명 충돌로 실패하면 STOP해.
- 원격 CI emulator는 현재 없고 후속 후보야.
- **이전 기록 읽는 법:** 아래 91일차 이전 섹션의 "검증 전용 emulator에서 실행" 같은 후속 지시와 emulator 실행 결과는 당시 기준 기록이야. 현재 실행 지시가 아니고, 위 정책이 우선해.

## 변경 내용

- 저장소 `AGENTS.md` 5절: 실기기 절대 금지 목록에 `connected*` 전체·갤러리·`filesDir` 삭제·재설치 우회를 명시, `adb install -r` 승인·서명 충돌 STOP 추가, emulator 우선·자동 실행·화면 깨우기·삭제 검증 emulator 우선 규칙을 제거하고 새 하위 절 "로컬 검증 환경과 instrumentation 등급"(emulator 폐기, 실기기 기본, SAFE/CONDITIONAL/FORBIDDEN, SAFE 실행 절차, 원격 CI 후보)을 추가. 8절: 검증 영역 `emulator instrumentation` → `실사용 기기 instrumentation(승인된 SAFE 테스트에 한함)`, 실패 분류 `emulator / OS environment` → `device / OS environment`.
- 저장소 `docs/ai/TEST-COVERAGE-MAP.md`: 안전 등급 절과 표 추가, migration·Compose 항목과 마지막 요약의 "다시 emulator에서 확인/실행 필요" 지시형 문구를 당시 기록 + 현재 정책으로 정리. 80~91일차 실행 기록은 그대로 보존. 테스트 수 변화 없음.
- 저장소 밖 workflow canonical source `~/plugins/post-card-memory-workflow`(Git 저장소 아님, 이번 commit에 포함되지 않음): `SKILL.md` #29~32, `references/work-order-template.md`, `references/claude-code-execution-rules.md`, `references/codex-execution-rules.md`, `references/handoff-template.md`, `write-project-handoff/SKILL.md`, `write-project-handoff/references/handoff-template.md`를 같은 정책으로 수정. 템플릿 검증 표의 `emulator instrumentation`/`AVD·API` 칸을 실사용 기기 SAFE 칸으로 바꿨어.
- Claude 메모리(저장소 밖): 91일차 메모리의 emulator 부팅~종료 절차를 지우고 폐기 사실만 남겼어. 새 정책 메모리를 추가하고 색인을 갱신했어.

## 독립 상태

| 구분 | 상태 | 근거 |
|---|---|---|
| 구현(문서·규칙) | 완료 | 위 변경 내용 |
| production / test Kotlin | 변경 없음 | diff 확인 |
| 로컬 자동검증(JVM) | 불필요 | 문서·규칙만 변경 |
| 실사용 기기 instrumentation | 미실행 | 이번 작업 범위에서 실행 금지 |
| 로컬 emulator | 미사용 | 폐기 |
| 실기기 감각 QA | 불필요 | 앱 동작 변화 없음 |
| 정합성 감사 | 수정 후 검색 재감사로 확인 | 남은 emulator 언급은 금지 문장·과거 기록·원격 CI 후보로 분류 |
| TEST-COVERAGE-MAP | 갱신 완료 | 테스트 수 변화 없음, 실행 가능 범위(보호 범위 설명) 변화 반영 |
| 실사용 기기 | 미접촉 | ADB·설치·실행 없음 |
| repository HANDOFF | 최신화 | 이 섹션 |
| commit / push / CI | 이 HANDOFF를 포함한 커밋으로 진행 | 결과는 최종 완료보고에서 확인 |

## 남은 위험과 후속 후보(승인된 작업 아님)

- `PostcardBackSaveTest`·`PostcardBackgroundColorSaveRaceTest`가 실제 기기에서 같은 id의 실제 엽서 초안·확정 상태 파일을 건드리는지 읽기 전용 조사로 확정하고, 필요하면 테스트 데이터 격리(별도 승인 필요, test 코드 변경).
- 원격 CI에서 격리된 instrumentation 환경 도입 검토(CONDITIONAL·FORBIDDEN 13건의 Android 실제 실행 경로).
- README 122행("검증 전용 emulator에서 수동으로 실행")과 `StructureTestSource.kt` 15~16행 주석은 이번 범위에서 빠졌어. 후속 정리 후보야.
- Codex 플러그인 캐시 사본 `~/.codex/plugins/cache/personal/post-card-memory-workflow/...`는 canonical source가 아니라서 수정하지 않았어. Codex가 이 캐시를 쓰면 옛 emulator 규칙이 보일 수 있으니 플러그인 재설치·동기화 여부를 확인해야 해.
- 92일차 메모리 실측은 실사용 기기의 읽기 전용 `meminfo`/profiler 기준으로 다시 설계해야 해.

---

# 이전 기록 — 91일차: 꾸미기 저장 누락 방지 보강 + 주석·README 최신화

확인일: 2026-10-02. 수동 표준 모드(91일차 작업지시서, 담당 Claude Code). 새 기능·UI·미감 변경 없이, 꾸미기 저장 결과나 디렉터리 하나를 빠뜨려도 조용히 지나가지 않도록 작은 안전장치를 넣고 오래된 주석과 README를 현재 사실에 맞췄어. **이 문서의 다음 후보는 실행 승인이 아니야.**

## 91일차 시작 상태

- 브랜치 `feature/photo-sticker`, 시작 HEAD `3b01f18adde0e76d6c0421c91a6cec06396e2955`("Record the 90-day structure and tech-debt audit"), `git fetch` 후 origin 대비 `0/0`, 추적 작업트리 clean, `git diff --check` 통과.
- Codex 실패 시도 뒤 저장소 상태: 추적 파일 변경 없음. untracked는 기존 보호 대상 `.codex-config.candidate.toml`, `.kotlin/`(errors log 2개)뿐이었고, `.claude/settings.local.json`은 전역 ignore로 존재. Codex가 남긴 예상 밖 변경은 발견되지 않았어.
- 90일차 HANDOFF 게이트: 90일차 구조·기술부채 감사 기록은 이미 `3b01f18`로 커밋·푸시돼 있어 별도 docs-only commit을 하지 않았어.

## 변경 내용

- **저장 성공 확인 누락 방지:** `shouldConfirmSaveSucceed`(`ui/detail/DetailViewModel.kt`)의 `doodlesSaved`·`textStickersSaved`·`maskingTapesSaved`·`labelStickersSaved` 기본값 `= true` 4개를 제거했어. production 호출부는 여섯 결과를 이름 있는 인자로 넘기고, `ConfirmSaveLogicTest` 11개 호출도 여섯 값을 모두 명시해. 판정은 여전히 여섯 개 AND라 저장 성공 조건·실패 처리·초안 삭제/보존 조건은 그대로야. 이제 결과 하나를 빠뜨리면 컴파일이 실패해.
- **꾸미기 디렉터리 기준 단일화:** 새 `utils/PostcardOwnedFileLayout.kt`에 `DecorationStateFile`(상태 파일 6종: `sticker_states`, `seal_states`, `doodle_states`, `text_sticker_states`, `masking_tape_states`, `label_sticker_states`)과 `PostcardAssetDirectory`(엽서별 자산 3종: `sticker_bgs`, `sticker_originals`, `masking_tape_photos`)를 두었어.
  - 저장·복원: `DetailViewModel`의 상태 파일 쓰기/읽기 12곳과 `sticker_bgs` 3곳이 enum 경로를 써.
  - 삭제: `cleanupPostcardOwnedAssets`가 두 enum을 순회해(삭제 순서·결과 이름 `stickerState`·`confirmedStickerBackgrounds` 등 불변).
  - 고아 진단: `OrphanFileDiagnostics.scan`이 같은 enum을 순회하고, 초안 경로는 `PostcardDraftStorage`의 상수(`private` → `internal`, 값 불변)를 써. category 순서·type·reason 문자열 불변.
  - `PhotoStickerImageStorage`·`MaskingTapePhotoStorage`의 private 디렉터리 상수도 enum 값을 참조해.
  - **기존 경로 문자열은 한 글자도 바꾸지 않았어.** 경로 계산 결과(`File(filesDir, "<dir>/<id>.txt")`, `File(filesDir, "<dir>/<id>")`)도 기존과 같아. 파일 이동·rename·데이터 migration 없음.
- **새 보호 테스트:** `DecorationDirectoryContractTest` 3건 — 디렉터리 이름을 production 상수가 아닌 리터럴로 독립 고정, 저장 경로 함수로 만든 파일을 production 삭제가 전부 지우고 다른 엽서는 보존하는지, production 진단이 전부 찾고 엽서가 있으면 0건인지 실제 파일 I/O로 확인. 삭제 목록에서 마지막 종류를 빼는 일시 변형을 넣었을 때 이 테스트가 실패하는 것을 확인한 뒤 원복했어.
- **주석 건강검진:** `app/src` 전체에서 TODO/FIXME/HACK·emulator·schema/version 숫자·개수 표현을 검색해 현재 코드와 대조했어. TODO/FIXME는 0건. 고친 오래된 주석:
  - `testsupport/StructureTestSource.kt`: "emulator는 아직 준비돼 있지 않다" → 90일차부터 검증 전용 emulator 수동 실행이 가능하지만 CI는 계측 테스트를 컴파일만 한다는 현재 사실.
  - `ui/detail/SaveResultAlertDialog.kt`: "다이얼로그 7종" → 현재 DetailScreen 호출부 4곳(56·58일차에 줄어든 사실 반영).
  - `DetailViewModel.kt`: `ConfirmSaveState`·`saveEditsAndClearDraft` 설명의 "스티커·도장" 2종 표현 → 꾸미기 여섯 종. `shouldConfirmSaveSucceed`에는 기본값을 두지 않는 이유를 적었어.
  - `PostcardDeletionManager.kt`·`OrphanFileDiagnostics.kt`: "새 디렉터리 추가 시 여기도 넣어야" 경고 → 공용 목록을 순회하고 계약 테스트가 보호한다는 설명. 저장 실패·초안 보존·`ExitSaveScope`·`onCleared`·삭제 순서·파일 소유권 주석은 건드리지 않았어.
  - 갤러리 퀵셀렉트의 "특별한 갤러리 3종" 주석은 88일차 변경을 함께 적은 이력 주석이라 유지했어.
- **README:** "과거 설명 보존" 상태였던 앱 소개를 현재 코드 기준으로 다시 썼어 — 2열 그리드·5페이지 편집·Neo-Brutalism 색표 같은 옛 설명을 걷어내고, 앞면/뒷면 편지·작성 기록, 편집 탭 7개와 꾸미기 종류(사진 스티커 오림 5종 포함), 초안·확정 저장·공유·내보내기, 월별(3열)/기억 밀도 갤러리, 흔들어서 한 장, 퀵셀렉트, 방문 기록·달력, 미래 우체통, 기술 스택(Room schema 19, migration 1→19), 검증 표(CI는 `assembleDebugAndroidTest`로 계측 APK를 컴파일만 하고 실행하지 않음을 구분), 권한(CAMERA, VIBRATE)을 정리했어.

## 최종 검증과 독립 상태

| 구분 | 상태 | 근거 |
|---|---|---|
| 구현 | 완료 | 위 변경 내용 |
| 관련 JVM 우선 실행 | 86/86 통과 | `ConfirmSaveLogicTest` 15, `ConfirmSaveHistoryClearStructureTest` 3, `DecorationDirectoryContractTest` 3, `PostcardDeletionManagerTest` 12, `OrphanFileDiagnosticsTest` 9, `PostcardDraftStorageTest` 29, `ConfirmedEditStateStorageTest` 5, `AppFileOwnershipTest` 10 |
| 로컬 JVM 전체 | **906/906 통과** | XML 95개, 실패·오류·skip 0. `@Test` 906개, 테스트 파일 94 + helper 1 |
| assembleDebug | 성공 | 앱 APK 빌드 |
| assembleDebugAndroidTest | 성공 | 계측 테스트 APK **컴파일**일 뿐 실행 아님 |
| emulator instrumentation | `PostcardDeletionOrchestrationTest` 3/3, 전체 **20/20** 통과 | 검증 전용 `PostcardMemory_Test`(`emulator-5554`, sdk_gphone16k_x86_64)만 연결 확인 후 APK 설치·실행. boot 직후 첫 시도 2회는 emulator lowmemorykiller가 test process를 죽여 status 없이 종료 → 환경 문제로 분류, test emulator만 재부팅 후 같은 APK로 통과. 실행 후 emulator 종료 |
| 실기기 QA | 불필요 | UI·미감·사용자 동작 변화 없음, 저장 판정 논리·경로 문자열 불변, 경로 일치는 자동 테스트와 계측으로 확인 |
| TEST-COVERAGE-MAP | 갱신 완료 | 테스트 3건 추가, 디렉터리 계약 보호 범위·한계 기록 |
| Room schema / migration / serialization / 꾸미기 파일 형식 / 디렉터리 이름 / 사용자 데이터 | 변경 없음 | diff 확인 |
| 실사용 기기 | 미접촉 | 연결·ADB·설치·계측 없음 |
| repository HANDOFF | 최신화 | 이 섹션 |
| commit / push / CI | 이 HANDOFF를 포함한 91일차 커밋으로 진행 | 결과는 최종 완료보고에서 확인 |

## 남은 위험과 후속 후보(승인된 작업 아님)

- 디렉터리 계약 테스트는 DetailViewModel이 실제로 enum 경로로 저장한다는 사실까지는 증명하지 못해(Robolectric 없음). 새 꾸미기 종류가 enum을 거치지 않고 다른 경로에 저장되면 잡지 못해 — 7번째 종류 기획 시 공통 틀과 함께 검토.
- `DetailViewModel`의 초기 로드·"원래대로"·확정 저장 후 이력 초기화의 여섯 종 평행 목록은 여전히 기억 의존이야(구조 테스트 `ConfirmSaveHistoryClearStructureTest`만 존재).
- `ConfirmSaveLogicTest`의 "…AllSaved" 4건은 기본값 제거 후 모두 여섯 값 true로 같은 입력이 됐어. 이름은 여전히 사실이지만 중복 정리는 후속 후보.
- `GalleryViewMode.DETAIL_LIST`는 enum 선언 외 참조가 없어 보여(README에서 "자세히 보기" 설명을 뺀 근거). 저장된 화면 상태 복원(`valueOf`) 호환까지 확인한 뒤 dead code 여부를 판단할 후보.
- `PostcardDeletionManager.kt` 정리 단계 주석 번호가 3 → 5로 건너뛰는 오래된 표기는 의미 영향이 없어 그대로 뒀어.
- 90일차 후보(백업 정책, release 로그, FileProvider 여분 root, quick select 손 해상도, 실제 heap 측정)는 그대로 남아 있고, 92일차 메모리 실측과 섞지 않았어. 90일차 "코드 클린데이 후보" 1~3번은 이번에 처리했어.

---

# 이전 기록 — 90일차 건강검진: instrumentation 청산 + 첫 보안 감사 + bitmap 메모리 감사

확인일: 2026-10-01. 수동 표준 모드(90일차 장기 작업지시서). 신규 기능과 앱 동작 변경 없이, 미실행 instrumentation을 검증 전용 emulator에서 실제로 실행하고 release/debug 보안 표면과 bitmap 상주 구조를 감사했어. production·test·이미지 자산은 수정하지 않았고 이 HANDOFF와 TEST-COVERAGE-MAP만 실제 결과로 갱신했어. **이 문서의 다음 후보는 실행 승인이 아니야.**

## 90일차 시작 상태

- 브랜치 `feature/photo-sticker`, 시작 HEAD `5e1abc861b88fc5a365acb16a3b222b663a3f3bf`, origin 대비 `0/0`, 추적 작업트리 clean.
- 보호 untracked `.codex-config.candidate.toml`, `.kotlin/`을 수정·삭제·이동·stage하지 않았어. 기존 `.claude/settings.local.json`도 보존했어.
- JVM 903개/소스 테스트 파일 93개(+helper 1개), 기존 결과 903/903. instrumentation 20개/7파일. 최근 CI run `36713844171` 성공.
- Room schema 19, migration 1→19 연속 등록, custom text serialization과 `filesDir/visits/visit_record.txt` + `visits/history/<epochDay>.visit` 방문 기록 형식, dependency 구조를 확인했어.

## 1단계 — instrumentation 실제 실행

- 검증 전용 `PostcardMemory_Test` AVD만 연결했어: `emulator-5554`, API 37, x86_64, 16KB page system image, 1080×2400, 420dpi. 실사용 기기는 연결하거나 조작하지 않았어.
- `assembleDebug`와 `assembleDebugAndroidTest` 성공 후 main/test APK를 이 emulator에만 설치했어.
- `PhotoStickerEdgeStyleInstrumentedTest` 7건을 명시 실행해 **7/7 통과**, 이어 전체 instrumentation을 **20/20 통과**했어.
- boot 직후 첫 대상 실행은 test process가 test status 전 종료됐어. 같은 시각 emulator에서 Play Store install session 충돌과 UWB HAL 재시작이 반복돼 환경 문제로 분류했고, test emulator만 재부팅해 session을 비운 뒤 같은 APK·테스트를 재실행하자 전부 통과했어. production/test 수정은 없었어.

## 2~3단계 — 첫 보안 감사와 수정 판단

- 앱 소스·설정 126개와 생성된 release/debug manifest를 정적 감사했어. manifest component/exported/permission, 내부 저장소와 cache, FileProvider/URI, 경로 입력, backup, release log, secret, 네트워크, 외부 Intent와 존재하는 선택 기능을 추적했어.
- 결과: 높음 0, 중간 1, 낮음 2, 정보성 3.
- **중간 — 기본 백업 범위 미지정:** `allowBackup=true`이고 `dataExtractionRules`/`fullBackupContent`가 없어 내부 Room·사진·초안·방문 기록이 플랫폼 기본 백업/기기 이전 대상이 될 수 있어. 앱 샌드박스와 계정 보호는 있지만 백업 계정·복원 환경 노출 시 개인 데이터가 함께 노출될 가능성이 있어. 제외하면 복원·기기 이전에서 데이터가 사라질 수 있으므로 제품 정책 결정 전 수정 STOP.
- **낮음 — release 진단 로그:** `PostCardMemoryApp`, `DetailViewModel`, `GalleryViewModel`의 `Log.w` 9곳에 내부 경로·ID·예외 메시지가 일부 남을 수 있어. 일반 앱이 logcat을 읽을 수 있는 구조는 아니고 사용자 본문을 직접 기록하지 않아 즉시 수정하지 않았어.
- **낮음 — FileProvider 여분 root:** provider는 `exported=false`이고 개별 URI grant가 필요하지만, 현재 호출 경로가 쓰지 않는 `files-path/postcards/`가 URI 생성 범위에 포함돼 있어. 현재 외부 grant 호출은 없고 과거 URI 호환을 깨뜨릴 가능성이 있어 보류했어.
- release의 필수 외부 진입은 launcher `MainActivity`뿐이야. provider/service는 비공개이고, 외부 표시된 profile installer receiver는 시스템급 `DUMP` permission으로 보호돼. `root-path`·`external-path`·전체 저장소 권한·deep link·WebView·동적 코드·secret/credential은 없었어.
- CAMERA는 카메라 기능, VIBRATE는 햅틱에 사용해. INTERNET·ACCESS_NETWORK_STATE는 ML Kit 계열 전이 dependency가 병합하지만 앱 소스에는 endpoint/HTTP 호출이 없고 release cleartext HTTP는 플랫폼 기본값으로 차단돼.
- production/test 수정과 보안 테스트 추가는 없음. 근거 없는 수정이나 테스트 수 늘리기를 피했어.

## 4단계 — bitmap 메모리 건강검진

- quick select 손 5장은 각각 1254×1254 `drawable-nodpi`이고 ARGB_8888 예상 decode 합계가 31,450,320B(약 30.0MiB)야. 표시 크기는 180dp, 검증 emulator 420dpi에서는 약 473px인데 메뉴를 닫아도 손 묶음이 composition에 남아 5장 모두 상주할 수 있어.
- `gallery_paper_tile.png`는 1254×1254, 예상 6,290,064B(약 6.0MiB). 360dp 반복 타일로 갤러리 화면 수명 동안 한 장만 유지되고 빈 화면/일반 pager 분기는 서로 배타적이야.
- `visit_calendar_paper.png`는 1122×1402, 예상 6,292,176B(약 6.0MiB). 월 이동 장들이 하나를 공유하지만 drawer가 닫혀도 drawer content composition에 남을 수 있어.
- 세 자산군 동시 상주 예상은 44,032,560B(약 42.0MiB)야. 실제 heap profiler 측정이 아니라 픽셀×4 상한 추정이야.
- 가장 큰 후보는 quick select야. 640px 가정 시 약 22.2MiB(74%), 720px 가정 시 약 20.1MiB(67%)를 줄일 수 있지만 손 디테일·알파 경계 비교 QA가 필요해. 이미지 파일과 두 종이의 해상도·밝기·질감은 수정하지 않았고, 달력 조건부 composition도 첫 열림/넘김 감각을 바꿀 수 있어 보류했어.

## 최종 검증과 구조 상태

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| production/test 구현 | 변경 없음 | 감사 결과상 즉시 수리보다 정책·호환 판단이 먼저 |
| 로컬 JVM | 903/903 통과(94 XML, 실패·오류·skip 0) | 최종 task 성공, 코드 불변으로 test task는 up-to-date |
| assembleDebug / assembleDebugAndroidTest | 성공 | 최종 task 성공, 산출물 up-to-date |
| emulator instrumentation | 대상 7/7, 전체 20/20, 최종 대상 7/7 통과 | 검증 전용 emulator만 사용 |
| 실기기 QA | 불필요 | 앱 코드·UI·이미지·동작 변경 없음 |
| TEST-COVERAGE-MAP | 갱신 완료 | 테스트 수 변화 없음, instrumentation 실제 실행 상태 변경 |
| Room / migration / serialization / visit history / draft / photo format / dependency | 변경 없음 | 읽기 전용 감사 |
| 실사용 기기 | 미조작 | 설치·실행·ADB·instrumentation 없음 |

## 남은 위험과 91일차 후보(승인된 작업 아님)

- 백업 보존 정책을 사용자와 먼저 결정한 뒤 민감 경로 제외/백업 유지/전체 비활성 중 하나를 별도 작업으로 설계.
- release 로그 최소화와 미사용 FileProvider root 제거는 과거 호환·진단 필요성을 먼저 확인한 뒤 방어 심화 작업으로 검토.
- quick select 손 640px/720px 후보를 별도 파일로 비교해 실기기 화질 승인을 받은 뒤에만 원본 교체 검토.
- Android Studio profiler로 갤러리 진입·drawer 열기/닫기 전후 실제 Java/native/graphics heap을 측정해 42.0MiB 추정을 검증.
- CI는 instrumentation을 실행하지 않으므로 원격 CI에서 격리된 instrumentation 환경 도입 검토(2026-10-03 갱신: 로컬 emulator는 폐기됐으니 로컬 emulator 재구성이 아니라 원격 CI 후보로 읽어).

---

# 이전 기록 — 90일차 추가 감사 — 구조·기술부채 / 장기 유지보수

확인일: 2026-10-01. 수동 표준 모드. 위 90일차 건강검진을 기록한 HEAD `7fe29bf874461c46a3313d3667c0c1132abcfc06` "Record the 90-day health and security audit" 상태를 대상으로 한 **읽기 전용 후속 감사**야. 보안·bitmap 메모리는 위 건강검진에서 이미 봤으므로 반복하지 않고 장기 유지보수 관점만 봤어. production·test·Gradle/dependency·이미지 자산·Room/migration/serialization은 수정하지 않았고, 이 HANDOFF 섹션만 추가했어. 아래 수치는 감사 시점 `wc -l`·`grep` 실측이며, 상태 개수 등은 근사값이야. **이 섹션의 모든 후보는 승인된 작업이 아니라 관찰·후속 후보야.**

## 전체 판단

- 현재 구조 상태: **관리 필요**. 다만 **구조적 한계 접근 중은 아니야.**
- 강한 영역: 저장 안전성, Room schema/migration(schema 19 JSON 보존, 1→19 연결 계측 테스트), 원자 저장(`AtomicFileReplace`), 삭제 흐름(DB 삭제 성공 → 앱 소유 파일 정리, `isInsideDirectory` guard), 초안 파일 기반 프로세스 종료 복구.
- 현재 구조 비용은 주로 **편집 화면(`DetailScreen`/`DetailViewModel`)과 꾸미기 종류 확장 경로**에 집중돼 있어. 데이터 손상 위험보다 변경 시 수고 형태의 비용이야.
- 지금은 대규모 리팩터링보다 관찰과 작은 예방 조치가 적절해.

## 주요 hotspot

### DetailScreen

- `ui/detail/DetailScreen.kt`: 약 7,173줄(2026-08-08 기준 5,666줄 → 제3~9차 UI 조각 분리 이후에도 약 1,500줄 증가).
- `DetailScreen()` 단일 composable(`:1407` ~ 파일 끝): 약 5,770줄.
- 함수 안 `remember` 상태 약 78개, Effect(`LaunchedEffect`/`DisposableEffect`/`SideEffect`) 약 21개, `viewModel.` 참조 약 234개, gesture/`pointerInput` 계열 약 44곳.
- 2026-09-01 이후 이 파일을 건드린 커밋 13개로 변경 빈도가 가장 높은 파일이야. 새 상호작용은 별도 파일(`*PlaceInteraction.kt`, `SealStampInteraction.kt`)로 빼고 있지만 연결 코드는 계속 이 함수에 쌓여.
- 지금 대규모 분리는 회귀 위험(제스처·저장 연결, 소스 텍스트 구조 테스트 동반 수정)이 더 크므로 즉시 리팩터링하지 않아.
- 새 편집 탭이나 새 꾸미기 기능을 추가할 때 그 탭 단위로 작은 분리를 검토해.

### DetailViewModel

- `ui/detail/DetailViewModel.kt`: 약 4,718줄.
- 꾸미기 6종(사진 스티커·도장·낙서·텍스트·마스킹테이프·라벨)마다 목록·선택 id·canUndo/canRedo·확정 baseline·`persist*EditState`·`clear*History`·`filesDir/<종류>_states/<id>.txt` 경로가 평행 구조로 존재해. 여기에 Room 필드 즉시 저장 Job 19개, 초안 자동저장, 배경 제거, export/share가 같은 클래스에 있어.
- 7번째 꾸미기 종류 추가 시 이 파일만 최소 8~10군데를 고쳐야 해 유지보수 비용이 크게 증가할 가능성이 있어. 확정 저장 코드 주석도 "여섯 개여야 한다 — 하나라도 빠지면 …"이라고 기억 의존 계약을 경고해.
- 현재는 공통화하지 않아. 7번째 종류가 실제로 기획될 때 공통 틀을 검토해.

### 화면 / exporter 이중 렌더러

- 마스킹테이프 무늬와 도장 모양 일부가 Compose 화면(`ui/components/MaskingTapeShapes.kt`, `SealShapes.kt`, DrawScope)과 Canvas exporter(`utils/PostcardImageExporter.kt`의 `drawTape*Overlay`, `drawCirclePostmarkOverlay` 등)에 별도 구현돼 있어. 상수만 공유하고, 예를 들어 하트 path cubic 수식은 두 파일에 복사돼 있어. 사진·라벨·도장 잉크처럼 `PostcardRenderSpec`/`LabelStickerRenderer`/`SealInkWearRenderer`로 이미 공통화된 부분도 있어.
- 한쪽만 고치면 화면과 저장·공유 결과가 달라질 잠재 위험이 있고(`AGENTS.md` 13절 불변값), 좌표 테스트는 있지만 그림 일치 테스트는 없어.
- 지금 전체 통합하지 않아. 해당 모양을 실제 수정하는 시점에 그 종류만 공통화를 검토해.

## 숨은 결합

1. **꾸미기 상태 파일 디렉터리 목록 3중 정의:** 같은 `<종류>_states`·`sticker_bgs`·`sticker_originals`·`masking_tape_photos` 등 디렉터리 목록이 `DetailViewModel`(쓰기), `PostcardDeletionManager`(삭제), `OrphanFileDiagnostics`(진단) 세 곳에 별도 문자열 목록으로 존재해. 세 곳 모두 "새 디렉터리 추가 시 여기도 넣어야" 주석이 있고 목록별 테스트는 있지만, 세 목록이 서로 같은지 확인하는 테스트는 없어. 삭제 쪽 누락은 컴파일·테스트를 통과한 채 삭제할 때마다 고아 파일을 남길 수 있어.
2. **`shouldConfirmSaveSucceed`(`DetailViewModel.kt:101`):** 6종 중 4종 인자의 기본값이 `= true`야. 새 꾸미기 종류 추가 시 이 판정에 넘기는 것을 잊어도 컴파일되고, 그 종류의 실제 저장 실패가 전체 성공으로 처리돼 초안이 삭제될 수 있어. 미래 데이터 손실 함정 후보야(현재 6종은 모두 명시적으로 전달돼 현재 결함은 아님).
3. **초안 형식(`PostcardEditDraft.kt`):** 위치 기반 meta index(`meta[14]` 등)와 "앞 종류 개수 합" offset 구조야. v1~v5 하위 호환 테스트는 강하지만 종류 추가마다 offset 계산이 길어지고, 순서 실수 시 `parsePostcardEditDraft`가 null을 반환해 초안이 통째로 버려질 수 있어.
4. **꾸미기 종류 간 겹침 순서:** 종류 사이의 layer 순서는 데이터에 저장되지 않고 화면 코드와 exporter 코드의 그리기 순서에 각각 암묵적으로 존재해. 한쪽만 바뀌면 미리보기와 저장 이미지의 layer 순서가 달라질 수 있어(코드 구조로 판단, 실제 불일치 재현은 미실행).

참고(기록용): 레이아웃 × 사진 배치 컬럼(`stamp/polaroid/tapedFilm × offset/zoom/scale`)도 새 레이아웃 추가 시 Entity·Migration·DAO·Repository·저장 Job·화면 분기(`DetailScreen` 약 9곳)를 함께 늘리는 곱셈 구조야. 경로 자체는 잘 보호돼 있어 아래 안정 영역으로 분류해.

## 안정적인 영역 — 현재 건드리지 않는 편이 좋음

- Room schema / migration chain
- 평면 `Postcard` Entity와 DAO/Repository 1:1 경로
- 기존 텍스트 serialization 형식(바꾸면 기존 사용자 파일 호환 위험이 이득보다 큼)
- 필드별 저장 Job 구조(장황하지만 경합 테스트가 붙어 있음)
- `ExitSaveScope` / 초안 승격 / `onCleared` 저장 흐름
- 삭제 순서와 앱 소유 파일 guard
- 갤러리 계열 기능 분리 구조(`GalleryScreen.kt` 1,545 → 3,267 → 2,182줄로 실제 분리된 이력)
- 방문 기록 저장 구조

## 테스트 관점

- JVM 903개 / instrumentation 20개라는 숫자보다 무엇을 보호하는지가 중요해.
- migration, 직렬화, 초안, 원자 저장, 삭제, 순수 계산(좌표·오림 모양·달력·상호작용 session)은 production 직접 검증으로 강해.
- 구조 테스트 159개는 실제 동작이 아니라 소스 형태를 보호하는 한계가 있고, `DetailScreen` 분리 시 테스트 동반 수정 비용을 만들어.
- replica 테스트(핵심 DetailViewModel replica 25개)는 실제 production ViewModel 직접 검증이 아닌 영역이 존재해(`TEST-COVERAGE-MAP.md`에 이미 명시).
- 화면과 exporter의 시각 결과 일치 검증은 현재 공백이야.
- `app/src/test/.../testsupport/StructureTestSource.kt`의 "emulator는 아직 준비돼 있지 않다" 취지 주석은 90일차 검증 전용 emulator 20/20 실제 실행 결과 기준으로 이제 오래된 설명이야.

## 다음 코드 클린데이 후보(승인된 작업 아님, 실행하지 않음)

1. 쓰기·삭제·진단의 꾸미기 디렉터리 목록이 서로 동일한지 확인하는 보호 테스트 1건.
2. `shouldConfirmSaveSucceed`의 위험한 `= true` 기본값 제거 검토.
3. `StructureTestSource`의 emulator 관련 오래된 주석 갱신.

## 장기 후보(승인된 작업 아님 — 100일 이후 또는 실제 기능 확장 시점의 관찰·후속 후보)

- `DetailScreen` 탭 단위 점진적 분리
- 꾸미기 공통 상태/저장 틀
- 화면/exporter renderer 공통화(그림 비교 검증 마련 선행)
- 초안 위치 기반 형식의 장기 확장 전략

## 최종 평가

> 90일 동안 기능이 지속적으로 추가된 것에 비해 프로젝트는 구조적으로 잘 버티고 있다. 현재 가장 큰 비용은 데이터 손상 위험보다는 편집 기능을 변경할 때 발생하는 유지보수 비용이다. 지금은 대규모 리팩터링보다 다음 기능 확장 시점에 작은 단위로 분리하는 편이 안전하다.

## 이 감사의 독립 상태

| 구분 | 상태 | 근거 |
|---|---|---|
| production/test/Gradle/자산/Room/serialization | 변경 없음 | 읽기 전용 감사 |
| 로컬 JVM / instrumentation / CI | 미실행 | 코드 불변, 위 건강검진 결과(903/903, 20/20) 유지 |
| 실기기 QA | 불필요 | 앱 동작 변경 없음 |
| TEST-COVERAGE-MAP | 변경 없음 | 테스트 수·범위 변화 없음 |
| repository HANDOFF | 이 섹션 추가 | 문서-only |
| commit / push | 미실행(사용자 승인 대기) | |

---

# 이전 기록 — 90일차(89일차 추가 작업): 방문 달력 종이 한 장 + 위로 넘기는 월 이동

확인일: 2026-09-30. 수동 표준 모드(89일차 추가 작업지시서: 구조 조사 → 자산 규격 → 자산 게이트 → 정적 형태 → 넘김 → QA 보정). 사이드바 방문 달력의 제목·장식·요일·날짜 grid·방문 표시·카오모지를 사용자 제공 종이 한 장 위에 묶고, 이전/다음 달 이동을 벽걸이 달력처럼 윗변을 축으로 위로 넘기는 넘김으로 바꿨어. 로컬 자동검증과 사용자 실기기 QA(정적 형태 → 넘김 → 다음 달 반투명 겹침 보정 → 하단 가로선 제거, 전부 통과)를 마쳤어. **commit·push는 사용자 승인 대기**야. **이 문서의 다음 후보는 실행 승인이 아니야.**

## 현재 상태 빠른 확인

- 브랜치: `feature/photo-sticker`, 시작·현재 HEAD `926b280` "Lay the main gallery on a cardboard paper sheet"(CI run `36698372064` 성공), origin `0/0`
- 미커밋 변경: `ui/gallery/VisitCalendarDrawer.kt` 수정, `VisitCalendarTest.kt` 수정 / 새 파일 `res/drawable-nodpi/visit_calendar_paper.png`, `VisitCalendarPaperPageStructureTest.kt` / 이 HANDOFF·TEST-COVERAGE-MAP
- 보호 untracked `.codex-config.candidate.toml`, `.kotlin/` 보존

## 자산

- `app/src/main/res/drawable-nodpi/visit_calendar_paper.png` — 사용자가 생성형 이미지로 만든 한 장 종이(타일 아님), 수정 없이 사용. 1122×1402(정확히 4:5) PNG 8bit RGB, 알파·ICC 없음(→ sRGB), 2,431,538 B, `caBX` 청크 포함. decode 약 6.3MB.
- 실측: 평균 #E9D4BA(L\* 85.9, b\* 15.7), 밝기 표준편차 5.06, 잔결 4.65, 가장 어두운 8×8 영역 201, 날짜색 #5B5046 대비 평균 약 5.4:1·최저 약 4.7:1, 5px 이상 섬유 점 81개, 3×4 구역 균일(±1), 비네팅·격자 성분 없음. 산출했던 규격(평균 #F4EDE1, L\* 92~95, 섬유 점 15개 이하)보다 어둡고 누렇지만 **사용자가 의도적으로 선택**했어.
- 경과: 1차(이 파일) 게이트 불통과 판정 → 2차(평균 #F6EFE6, 편차 2.03)는 폰에서 거의 무지로 보임 → 사용자가 "뒷배경이 너무 연하다"며 1차를 다시 채택. 2차는 저장소에 없음.

## 기존 구조(바꾸기 전)

- `VisitCalendarDrawer.kt` 한 파일: `ModalNavigationDrawer` → `ModalDrawerSheet`(304dp, `PaperSurface`) → `Column(verticalScroll, padding 24dp)` → `MonthlyVisitCalendar`. 월별↔선택판은 `AnimatedContent(navLevel)`(CALENDAR/MONTH_PICKER/YEAR_PICKER, fade+scale 170ms).
- 월 이동은 제목과 날짜 grid가 **각자 다른** `AnimatedContent(displayedMonth)`로 가로 슬라이드(200ms)했고, 요일·상단 장식·"다녀간 날들" 줄은 고정. 달력 자체 배경은 없었어(drawer 단색). 월 이동 gesture는 없음(◀ ▶·"오늘"·월 선택기만).

## 앱에 달라진 점과 구조

- `VisitCalendarMonthPage(month, …)`: 종이(`drawWithCache` + `drawImage`, 큰 쪽 배율 하나로 가운데 crop, 늘림·타일 없음) 위에 제목(48dp, 탭 → 월 선택)·상단 장식·요일·`VisitCalendarMonthGrid`. 안에서는 파라미터 `month`만 읽어 나가는 장이 다음 달로 바뀌지 않아. 윤곽선은 QA에서 "카드처럼 보임"으로 제거.
- 종이 bitmap은 `MonthlyVisitCalendar`에서 한 번만 로드해 모든 장이 공유(달이 바뀔 때마다 decode 없음).
- "다녀간 날들 / 오늘" 줄은 종이 위(밖)로 옮겨 고정, ◀ ▶는 종이 제목 줄 위에 겹쳐 고정(가운데 빈 곳 터치는 제목으로 내려감).
- 날짜 grid는 구분선이 없는 주 자리에도 같은 높이(4.5dp)를 비워 어느 달이든 장 높이가 같아(넘길 때 아래 장이 삐져나오지 않음). 4·5주 달의 빈 패딩 행 아래 위치가 최대 9dp 내려감.
- 월 이동: `updateTransition(displayedMonth)` + `Transition.AnimatedContent`, `visitCalendarPageTurnTransition`(Enter None, Exit `KeepUntilTransitionsFinished`, `targetContentZIndex = visitCalendarPageStackZIndex(month)` = 앞선 달이 항상 위, `SizeTransform(clip = false)`). 표현은 장 전체에 거는 `visitCalendarPageTurnModifier(forward)`(윗변 `TransformOrigin(0.5f, 0f)` + `rotationX`, cameraDistance 14, 380ms).
  - 다음 달: 위의 현재 장이 FastOutLinearIn으로 불투명한 채 들리다 빨라져 90°에서 사라지고(흐려짐은 마지막 10%), 들리는 장에만 옅은 그림자(최대 0.12). 아래 다음 장은 움직이지 않음. 처음엔 두 방향이 FastOutSlowIn·흐려짐 시작 0.45를 공유해 현재 장이 약 70% 시간 반투명하게 떠 있어 "두 달이 crossfade"처럼 보였고(QA), 다음 달 값만 분리해 해결.
  - 이전 달(QA에서 자연스럽다고 한 기준점, 보정 없이 유지): 이전 장이 80° 들린 채 FastOutSlowIn으로 내려와 덮고(흐려짐 시작 0.45), 덮이는 아래 장에 그림자.
- 하단 `VisitCalendarBottomOrnament`의 폭 전체 가로선 제거(종이 밑변과 이중 경계로 사이드바 구획선처럼 보임), 양끝 `୨୧`만 유지. 월/연도 선택판 하단도 같이 바뀜(사용자 승인).
- 월/연도 선택 단계 전환(fade+scale)·선택판 세로 슬라이드·방문 데이터 읽기(`visitedDaysForMonth`)·카오모지·오늘 표시·햅틱·drawer 폭/색/닫기·뒤로가기 변경 없음. Room·migration·serialization·dependency·방문 기록 형식 변경 없음.

## 검증과 남은 상태

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| 구현 | 완료 | |
| 로컬 JVM | 통과 903/903 (94 XML) | 신규 `VisitCalendarPaperPageStructureTest` 7건, `VisitCalendarTest` +3건 |
| assembleDebug / assembleDebugAndroidTest | 성공 | 최종 코드로 재실행 |
| emulator instrumentation | 미실행 | androidTest 변경 없음, 기존 미실행 7건 유지 |
| 실기기 QA | 완료 | 정적 형태(윤곽선 제거 후) 통과, 다음 달 넘김 보정 후 "좋아졌어", 이전 달·연타·오늘 복귀·선택 단계 전환·방문 표시 정상, 하단 가로선 제거 "좋아" |
| TEST-COVERAGE-MAP | 갱신 완료 | 893→903, 파일 92→93, 구조 152→159 |
| git diff --check / LF | 깨끗 / LF 유지 | |
| commit / push / CI | 미실행(사용자 승인 대기) | |

## 남은 위험

- 종이가 산출 규격보다 어둡고 섬유 점이 많아(최저 대비 약 4.7:1) 밝기가 낮은 기기·야외에서 날짜 가독성이 떨어질 수 있음 — 사용자 선택이라 유지.
- 상주 메모리: 달력 종이 약 6.3MB 추가(drawer 내용은 닫혀 있어도 composition에 있음). 메인 종이 6.3MB + 퀵 셀렉트 약 31MB와 합산.
- 넘김 중 ◀ ▶ 아주 빠른 연타는 방향이 섞여 보일 수 있음(QA 연타는 정상). 들어오는 달 방문 기록을 아직 못 읽었으면 넘긴 뒤 민트가 늦게 찍힐 수 있음(기존과 같은 구조).
- 가로 화면·작은 화면·다른 밀도 기기의 넘김 원근·종이 crop 미확인.

## 다음 후보(승인된 작업 아님)

- 이 변경의 commit·push·CI 확인(사용자 승인 필요)
- `PhotoStickerEdgeStyleInstrumentedTest` 7건 검증 전용 emulator 실행, 종이·퀵 셀렉트 PNG 메모리 최적화

---

# 이전 기록 — 89일차: 메인 갤러리 카드보드 종이 배경 + 불투명 시계 header

확인일: 2026-09-30. 수동 표준 모드(89일차 작업지시서). 새 기능 없이, 메인 갤러리의 구분선 아래 본문에만 사용자가 만든 종이 질감 타일을 깔고 시계·커피 header를 불투명하게 만들었어. 로컬 자동검증과 사용자 실기기 QA(이미지 교체 1회 후 "대만족", 이음매 선 없음·기존 기능 전부 정상)를 마쳤고, 작업지시서 27절에 따라 commit·push·CI 확인까지 진행해(실제 commit hash·CI는 완료보고·`git log`·GitHub Actions로 확인). **이 문서의 다음 후보는 실행 승인이 아니야.**

## 현재 상태 빠른 확인

- 브랜치: `feature/photo-sticker`, 시작 HEAD `e4045ab` "Record the quick select long-press removal as a product decision"(CI run `36549115333` 성공), origin `0/0`, 추적 파일 clean
- 커밋 대상: `ui/gallery/GalleryScreen.kt` 수정 / 새 파일 `res/drawable-nodpi/gallery_paper_tile.png`, `GalleryPaperBackgroundStructureTest.kt` / `GalleryMonthlyGridStructureTest.kt` 수정 / 이 HANDOFF·TEST-COVERAGE-MAP
- 보호 untracked `.codex-config.candidate.toml`, `.kotlin/` 보존

## 자산

- `app/src/main/res/drawable-nodpi/gallery_paper_tile.png` — 사용자가 생성형 이미지로 만든 seamless 타일, 수정 없이 사용. 1254×1254 PNG 8bit RGB(알파 없음, ICC 없음 → sRGB), 2,668,364 B, `caBX`(C2PA 출처 메타데이터 추정) 청크 포함.
- 실측(최종본): 평균 #EFE1CD(Hue 35°, 채도 14%), 밝기 표준편차 6.9/255, 156px 블록 평균 차 2.4, 내부 반복 없음, 좌우 이음매 연속. 위아래 이음매는 맨 아래 줄(224.5)과 맨 위 줄(229.9)이 전체 줄 중 가장 어둡고/밝은 1px 밝기 단차가 있었지만 실기기에서 선이 보이지 않음(QA 확인).
- 1차 이미지(표준편차 4.5, 평균 #F1E7D5)는 폰에서 거의 안 보여 사용자가 같은 파일명으로 교체함(1차는 커밋되지 않음).

## 기존 구조(바꾸기 전)

- `GalleryScreen` = `VisitCalendarDrawer` → `Box` → `Scaffold(containerColor = GalleryPaperWhite)`. 상태바는 `MainNavHost`의 `safeDrawingPadding`이 처리해 화면 밖.
- 기본 topBar `Column`에는 배경이 없고 제목 `Row`만 `GalleryPaperWhite` → **시계·커피 줄은 실제로 투명**했어. M3 Scaffold는 본문 다음에 topBar를 그리고, 월별 `LazyVerticalGrid`가 화면 전체 크기(contentPadding으로 header만큼 비움)라 위로 스크롤한 엽서가 시계 줄 뒤로 비치는 구조였어. 같은 색이라 눈에 안 띄었던 것.
- 월별·기억 밀도·검색 빈 상태·엽서 0장 화면이 각자 전체 화면 `GalleryPaperWhite`를 칠했어. 검색·선택 topBar는 Row 배경과 1dp 구분선이 이미 불투명.

## 앱에 달라진 점과 구조

- 기본 topBar `Column(modifier = Modifier.background(GalleryPaperWhite))` — header 전체 불투명. 스크롤한 엽서·글씨·그림자는 header 뒤로 가려짐(z-order로 해결, 별도 clip 없음). 구분선·시계·커피·김 animation은 그대로.
- `GalleryPaperBackground`(private composable): `ImageBitmap.imageResource` 1회 → `drawWithCache`에서 `ImageShader(Repeated, Repeated)` + `setLocalMatrix(setScale(s, s))`로 타일 한 장을 `GALLERY_PAPER_TILE_SIZE = 360.dp`로 같은 배율 축소 반복. 먼저 `GalleryPaperWhite`를 칠하고 타일을 `GALLERY_PAPER_TILE_ALPHA = 1f`로 겹침(0.5에서는 안 보여 QA 뒤 1로 올림). 늘림·crop 없음.
- 배치: pager `Box` 첫 자식으로 `padding(top = paddingValues.calculateTopPadding())` → 구분선 바로 아래부터, pager 밖이라 스크롤·보기 전환에 고정. 엽서 0장 빈 상태는 `matchParentSize`로 같은 composable 사용(두 곳은 서로 배타 분기라 동시에 한 레이어).
- 월별·기억 밀도·검색 빈 상태 페이지의 전체 배경 제거(카드·칩·월 헤더 자체 색은 유지).
- 양떼목장·쫑쫑컵(`SheepRanchStage`)은 사용자 결정으로 종이 없이 그대로. 퀵 셀렉트·흔들어서 한 장·drawer·popup·callback·navigation 변경 없음.
- Room·migration·serialization·dependency 변경 없음.

## 검증과 남은 상태

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| 구현 | 완료 | |
| 로컬 JVM | 통과 893/893 (93 XML) | 신규 `GalleryPaperBackgroundStructureTest` 5건, `GalleryMonthlyGridStructureTest` 배경 개수 1→0 |
| assembleDebug / assembleDebugAndroidTest | 성공 | 최종 alpha 1f 코드로 재실행 |
| emulator instrumentation | 미실행 | androidTest 변경 없음, 이 셸에 adb 명령 없음, 기존 미실행 7건 유지 |
| 실기기 QA | 완료 | 질감 보임·대만족, 가로 이음매 선 없음, header 가림·검색·다중 선택·퀵 셀렉트·흔들어서 한 장·연못 정상 |
| TEST-COVERAGE-MAP | 갱신 완료 | 888→893, 파일 91→92, 구조 147→152 |
| git diff --check / LF | 깨끗 / LF 유지 | |
| commit / push / CI | 작업지시서 27절에 따라 진행 | 결과는 완료보고·`git log`·GitHub Actions |

## 남은 위험

- 종이 타일 상주 메모리 약 6.3MB(1254²×4). 퀵 셀렉트 손 5장 약 31MB와 합쳐 저사양 기기 부담 가능 — 문제 시 축소 디코드가 후보.
- 위아래 이음매 1px 밝기 단차: 시험한 기기에서는 안 보였지만 다른 밀도·밝기 설정 기기에서는 미확인.
- 가로 화면·태블릿·아주 작은 화면의 종이 모습 미확인(타일 반복이라 늘림은 없음).
- 연못 파문이 종이 위에서 그려지는 모습은 QA에서 정상이라고 했지만 세부 미감은 따로 보지 않았어.

## 다음 후보(승인된 작업 아님)

- 90일차: 사이드바 방문 달력 종이 질감 + 이전/다음 달 이동 시 달력 한 장이 넘어가는 페이지 물성(월별↔연도별 전환 slide는 유지 예정)
- `PhotoStickerEdgeStyleInstrumentedTest` 7건 검증 전용 emulator 실행, 퀵 셀렉트 PNG 메모리 최적화

---

# 이전 기록 — 88일차: 갤러리 퀵 셀렉트 미감 정비(신문 오림 손 부채)

확인일: 2026-09-29. 수동 표준 모드(88일차 작업지시서). 기능은 더하거나 빼지 않고, 갤러리 우측 하단 + 클러스터의 표현만 사용자가 준비한 신문 오림 손 5장이 부채처럼 펼쳐지며 기능을 내미는 모습으로 바꿨어. 로컬 자동검증과 사용자 실기기 QA 2회(1차 보정 요청 → 2차 통과)를 마쳤고, 작업지시서 34절에 따라 commit·push·CI 확인까지 진행해(실제 commit hash·CI는 완료보고·`git log`·GitHub Actions로 확인). **이 문서의 다음 후보는 실행 승인이 아니야.**

## 현재 상태 빠른 확인

- 브랜치: `feature/photo-sticker`, 시작 HEAD `6621e12` "Let cutout stickers reach the postcard edge by their visible part"(CI run `36400626851` 성공), origin `0/0`
- 커밋 대상: `ui/gallery/GalleryScreen.kt` 수정 / 새 파일 `ui/gallery/GalleryQuickSelectFan.kt`, `GalleryQuickSelectFanTest.kt`, 손 PNG 5장 / `GalleryViewSelectionStructureTest.kt`, `AppIntroVisitPostmarkStructureTest.kt` 수정 / 이 HANDOFF·TEST-COVERAGE-MAP
- 보호 untracked `.codex-config.candidate.toml`, `.kotlin/` 보존

## 기존 퀵 셀렉트 구조(바꾸기 전)

- `GalleryScreen.kt`의 `GalleryFabCluster`: 오른쪽 아래 + 버튼 → 큰 원 3개(카메라·미래 우체통·"특별한 갤러리" 묶음 토글) + 작은 원 3개(연못·양떼목장·쫑쫑컵), 실행 버튼 5 + 묶음 토글 1 = 6개. 68일차 롱프레스 드래그 바로 실행, 탭/드래그 공용 `dispatchDragTarget`.
- 실행: 카메라 → `navigate("camera")`, 미래 우체통 → `navigate("futureMailbox")`, 놀이 3종 → `onPlayModeSelected`(같은 모드 재선택 시 NONE, 선택 해제·메뉴 닫힘).
- 노출: 다중 선택 중에만 숨김(`visible = !selectionMode`). 검색·놀이 모드 중에는 보임(놀이 모드 출구). 방문 달력 drawer 아래, 흔들어서 한 장 overlay 아래. 뒤로가기(`BackHandler(enabled = fabMenuExpanded)`)·20% 딤 바깥 탭으로 닫힘. 흔들기 인식 시 강제로 닫힘.

## 앱에 달라진 점

- 닫힌 상태: 오른쪽 아래 끝에 손끝·블록 일부만 삐져나온 작은 손 묶음(0.8배, 손끼리 1.2°씩 어긋남). 그 자리 60×64dp가 손잡이(탭 = 열기/닫기, 접근성 "바로가기 열기/닫기").
- 열기: 묶음째 쑤욱(130ms) → 아래로 5° 젖힘(60ms) → 촤라락 부채(170ms, 손별 약 14ms 어긋남). 닫기는 역방향(모임 120ms → 빠짐 110ms). 도중 반대 요청은 그 자리에서 방향만 바꿔.
- 손 매핑(아래→위): 카메라 손 = 카메라, 편지 손 = 미래 우체통, 강물 손 = 엽서의 연못, 양 손 = 양떼목장, 체커 손 = 엽서 쫑쫑컵. 기능 callback·navigation·노출 조건·뒤로가기·바깥 탭(딤)은 그대로.
- 켜져 있는 놀이 모드의 손은 블록 테두리에 옅은 금색(SunsetGold) 선. 누르면 0.96배 짧은 눌림 + 확정 진동(22ms/160), 손잡이 탭은 가벼운 진동(10ms/90).
- **사용자 승인으로 빠진 것:** "특별한 갤러리" 묶음 단계(다섯 손이 한 단계), 68일차 롱프레스 드래그 바로 실행과 그 진동, 옛 원형 버튼·선택 링·물방울 pulse. 닫힌 위치는 오른쪽 아래 유지(권장안 승인). 롱프레스 드래그는 사용자가 불필요하다고 봐서 애초에 지시하지 않은 것으로, 완료 뒤 사용자 요청으로 [DECISIONS](DECISIONS.md) 2026-09-29 항목에 제품 결정으로 기록함(복원 대상 아님).
- 저장 데이터·Room·migration·serialization·dependency 변경 없음.

## 자산

- `app/src/main/res/drawable-nodpi/quick_select_{camera,letter,sheep,checker,river}_hand.png` — 사용자 제공 원본 그대로(crop·보정 없음). 모두 1254×1254 32bpp ARGB 투명 PNG, 손목은 오른쪽 가장자리에서 들어옴(우측 가장자리 불투명 행: camera 631~909, letter 696~992, sheep 619~920, checker 617~933, river 670~929).
- 블록 경계(원본 px, 눈으로 실측): camera 243,405–692,718 / letter 113,432–590,750 / river 243,403–692,713 / sheep 280,377–723,722 / checker 162,413–653,725. `GalleryQuickSelectItem`에 상수로 둠.
- 기존 손 오버레이와 같은 `ImageBitmap.imageResource` + `FilterQuality.Medium`(원본 해상도).

## 구조와 결정

- `GalleryQuickSelectFan.kt`(순수): `GalleryQuickSelectPhase`(CLOSED/OPENING/OPEN/CLOSING, `onRequest`·`onAnimationFinished`·`onSelect`), `GalleryQuickSelectItem`(슬롯·블록 좌표), 부채 geometry.
- 부채: 화면 오른쪽 밖 가상 pivot 하나. 각 손은 블록 중심이 pivot에서 반지름 440dp 위에 오고 손 가로축이 반지름과 나란하게 같은 각도로 돈다(`graphicsLayer` translation + 블록 중심 transformOrigin + rotationZ). 이웃 각도 9°(작은 화면은 7.6°까지 clamp) → 슬롯 각도 −18°/−9°/0°/+9°/+18°, 이웃 블록 중심 간격 약 69dp. 가운데 손 블록 중심 = 오른쪽 끝에서 92dp, 맨 아래 블록 중심 = 바닥에서 64dp. 손 표시 크기 180dp.
- 닫힌 묶음: 전부 맨 아래 슬롯 각도 근처, 반지름 −72dp(오른쪽으로 물러남), 0.8배, 닫힌 자리만 24dp 더 아래(`quickSelectAnimatedBlockCenter`).
- 겹침 순서: 위 손부터 그려 아래 손이 위 손의 손바닥을 덮음(블록은 안 가림). 터치 영역은 블록 + 좌우 6dp, 높이 48dp(이웃 손과 겹치지 않는 한계 `r_in·tan(Δ/2)`로 제한). 회전된 layer 안이라 터치 판정도 회전을 따라감. 손잡이는 손들보다 먼저 둬 겹치는 곳은 손이 입력을 받음.
- 입력 보호: 기능은 OPEN에서만 실행, 탭 즉시 CLOSING으로 넘어가 연타해도 callback 1회. CLOSED에서는 손 터치 영역·접근성 노드 자체가 없음. 열기/닫기 연타는 같은 방향이면 재시작하지 않음.
- 연출 값(`Animatable` 3개: slide·tilt·spread)은 `graphicsLayer` 안에서만 읽음.

## 실기기 QA 결과

- 1차: 기능·닫기·연타·속도 정상. 요청 = "닫힌 손잡이 조금 더 아래", "손 사이 간격이 빡빡하고 겹치는 부분이 어색" → 반지름 400→440dp, 이웃 각도 8→9°(블록 간격 약 56→69dp, 기울기 ±16→±18°), 닫힌 자리만 24dp 하강.
- 2차: 손잡이 위치·간격·기능 전부 "좋아/전부 정상".

## 검증과 남은 상태

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| 구현 | 완료 | |
| 로컬 JVM | 통과 888/888 (92 XML) | 신규 `GalleryQuickSelectFanTest` 14건, 구조 테스트 12→11 |
| assembleDebug / assembleDebugAndroidTest | 성공 | |
| emulator instrumentation | 미실행 | androidTest 변경 없음, 연결된 emulator 없음(`adb devices -l` 빈 목록), 기존 미실행 7건 유지 |
| 실기기 QA | 완료(2차 통과) | 위 결과 |
| TEST-COVERAGE-MAP | 갱신 완료 | 875→888, 파일 90→91, 구조 148→147 |
| Room / migration / serialization / dependency | 변경 없음 | |
| commit / push / CI | 작업지시서 34절에 따라 진행 | 결과는 완료보고·`git log`·GitHub Actions |

## 남은 위험

- 손 PNG 5장을 원본 해상도로 상주 로드(장당 약 6.3MB, 합 약 31MB). 저사양 기기 메모리 압박 가능성 — 문제 시 표시 크기에 맞춘 축소 디코드가 후보(질감 손실 확인 필요).
- 블록 좌표는 눈 실측이라 수 px 오차 가능(터치 영역·금색 테두리 위치). QA에서 문제는 없었어.
- 롱프레스 드래그 바로 실행을 쓰던 사용 습관은 사라짐(승인된 제거).
- 가로 화면·아주 작은 화면(높이 약 400dp 미만)에서는 각도가 7.6°까지만 줄어 맨 위 손이 잘릴 수 있어(실기기 미확인).

## 다음 후보(승인된 작업 아님)

- 퀵 셀렉트 미감 추가 보정(사용자 요청 시)
- `PhotoStickerEdgeStyleInstrumentedTest` 7건 검증 전용 emulator 실행
- 사진 복사 중 고아 파일, 기본 모양 사진 스티커 회전 문제(88일차 범위 밖으로 유지)

---

# 이전 기록 — 87일차 후속: 누끼 스티커 이동 경계 수정

확인일: 2026-09-28. 수동 표준 모드. 마스킹테이프 쓸어 붙이기(`b9cd53b`, CI run `36398477330` 성공)를 마감한 직후 사용자가 제보한 버그를 고쳤어: "스티커 배경 제거 후 크기를 조절하니 원본 사진 크기가 테두리처럼 작용해서 원하는 위치까지 이동 못 함". 사용자 선택은 "보이는 부분 기준". 로컬 자동검증과 사용자 실기기 QA를 마쳤고, 사용자 요청("커밋하고 푸시해줘")으로 commit·push해(실제 commit hash·CI는 `git log`·GitHub Actions로 확인). **이 문서의 다음 후보는 실행 승인이 아니야.**

## 현재 상태 빠른 확인

- 브랜치: `feature/photo-sticker`, 시작 HEAD `b9cd53b` "Place masking tapes with a pressing hand sweep", origin `0/0`
- 커밋 대상: `DetailScreen.kt`, `DetailViewModel.kt`, `PhotoStickerPlaceInteraction.kt`, `utils/PostcardImageExporter.kt` 수정 / 새 파일 `PhotoStickerCutoutBounds.kt`, `PhotoStickerCutoutBoundsTest.kt` / 이 HANDOFF·TEST-COVERAGE-MAP
- 보호 untracked `.codex-config.candidate.toml`, `.kotlin/` 보존

## 원인

사진 스티커는 늘 정사각형 칸(`STICKER_BASE_SIZE × scale`) 안에 그려지고, 누끼는 그 칸 안에 Fit으로 놓인 투명 PNG야. 끌기·크기·조준·저장된 자리 재보정·저장본(exporter)이 모두 `clampStickerOffset`(칸 전체가 엽서 안)을 써서, 투명 여백이 먼저 엽서 가장자리에 걸려 보이는 부분을 가장자리까지 옮길 수 없었어. 키울수록 칸이 커져 더 심해져.

## 앱에 달라진 점

- 누끼 스티커는 보이는 부분(사람·물건)의 테두리가 엽서 가장자리에 닿을 때까지 옮길 수 있어. 투명 여백은 엽서 밖으로 나가고(엽서 경계에서 잘려 안 보임), 보이는 부분은 잘리지 않아. 끌기·두 손가락 확대/회전·크기 핸들·회전 모드/핸들·조준 미리보기 모두 같은 규칙이야.
- 기본 모양(누끼 아닌) 스티커는 예전과 똑같이 칸 전체가 엽서 안.
- 저장·공유 이미지도 화면과 같은 자리에 그려(누끼 칸이 캔버스 밖으로 나가도 당기지 않음). 저장 형식·Room은 그대로 — offset이 음수이거나 엽서보다 클 수 있게 됐을 뿐 같은 필드야.

## 구조와 결정

- 새 파일 `PhotoStickerCutoutBounds.kt`: `cutoutStickerVisibleExtent`(불투명 표본점을 뒤집기→회전해 중심 기준 보이는 범위, 표본 칸 반 대각선만큼 여유), `clampPhotoStickerOffset`(기본 = `clampStickerOffset`, 누끼+표본점 = 보이는 부분만 엽서 안, 누끼+표본점 없음 = 느슨한 규칙: 칸 중심이 엽서에서 한 변의 0.75배 이상 벗어나지 않게).
- 두 규칙의 역할: **정확한 규칙은 사용자 제스처(끌기·크기·회전·조준)에서만**, **느슨한 규칙은 저장된 자리 재보정 LaunchedEffect와 exporter에서** 써. 보이는 부분은 늘 회전한 칸 안(중심에서 한 변의 약 0.72배 이내)이라 정확한 규칙으로 놓은 자리는 느슨한 규칙에서 절대 당겨지지 않아(테스트로 고정) → 표본점을 줄여 읽은 결과가 조금 달라도 다시 열 때 스티커가 움직이지 않고, 화면 = 저장본이야.
- 표본점: 조준 중 배경제거 결과는 이미 계산한 값을 붙일 때 `rememberStickerCutoutSilhouette`로 넘겨. 그 밖의 누끼(기존 `배경제거` 버튼, 다시 연 엽서)는 `loadStickerCutoutSilhouette`가 그림을 긴 변 512px 이하로 줄여 읽어 계산해(uri당 한 번, 읽기만, 실패 = 빈 목록 → 느슨한 규칙). 화면 전용 메모리 값이라 저장하지 않아.
- 조준: `clampPhotoStickerAimCenter`에 누끼 표본점·각도를 넘기면 같은 규칙. `원본복원` 토글 때 칸 기준으로 다시 가둬(`toggleAimBackground(postcardSize, base)`, 기본값이면 예전 동작).
- 두 손가락 확대·회전 중(`Move` 모드 멀티터치)에는 예전처럼 offset을 그대로 두되, 누끼만 새 크기·각도로 정확한 규칙을 적용해.
- exporter: `createStickerOverlayForExport`는 누끼일 때 느슨한 규칙과 정규화 범위 -1~2, `PostcardImageExporter.drawStickerOverlay`는 누끼일 때 칸을 캔버스 안으로 당기지 않아.

## 검증과 남은 상태

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| 구현 | 완료 | |
| 로컬 JVM | 통과 875/875 (91 XML) | 신규 `PhotoStickerCutoutBoundsTest` 9건 |
| assembleDebug | 성공 | |
| emulator instrumentation | 미실행 | androidTest 변경 없음, 기존 미실행 7건 유지 |
| 실기기 QA | 완료 — 사용자가 "QA 완료" 보고, 보정 요청 없음(항목별 세부 결과는 받지 않음) | 아래 체크리스트 |
| TEST-COVERAGE-MAP | 갱신 완료 | 866→875, 파일 89→90 |
| Room / migration / serialization / dependency | 변경 없음 | |
| commit / push / CI | 사용자 요청으로 진행 | 결과는 완료보고·`git log`·GitHub Actions에서 확인 |

## 실기기 QA 체크리스트 (누끼 스티커 경계 — 사용자 QA 완료, 기록용)

1. 스티커를 붙이고 `배경제거` → 크게 키운 뒤 끌어서, 보이는 부분이 엽서 네 가장자리에 딱 닿을 때까지 가는지(투명 여백 때문에 멈추지 않는지), 보이는 부분이 잘리지 않는지
2. 누끼 스티커를 돌리거나(회전 모드·핸들·두 손가락) 뒤집은 뒤에도 가장자리까지 가고 잘리지 않는지
3. 조준 중 `배경제거` 후 가장자리로 조준 → 붙이기 → 조준한 자리 그대로인지, `원본복원`하면 칸이 엽서 안으로 들어오는지
4. 엽서를 나갔다 다시 열어도 가장자리에 붙인 누끼 스티커가 그 자리 그대로인지
5. 저장·공유 이미지에서 가장자리 누끼 스티커가 화면과 같은 자리에 있는지
6. 기본 모양 스티커(폴라로이드 등 포함)는 예전처럼 칸 전체가 엽서 안에서 멈추는지

## 남은 위험

- 누끼 스티커를 가장자리에 붙인 뒤 `원본복원`하면 칸 전체가 보이므로 재보정 규칙이 칸을 엽서 안으로 당겨(자리 이동). 의도된 동작이지만 QA에서 어색한지 확인 필요.
- 표본점은 48×48 격자라 보이는 테두리와 엽서 가장자리 사이에 최대 한 변의 약 1.5% 틈이 남을 수 있어(잘리지 않는 쪽을 택함).
- 표본점을 읽기 전 아주 짧은 순간 끌면 느슨한 규칙이 적용돼 보이는 부분이 엽서 밖으로 나갈 수 있어(읽기가 끝난 뒤 다시 끌면 맞음).
- exporter의 캔버스 밖 그리기는 JVM 테스트로 검사하지 못했어(`Uri` 필요) — QA 5번.

---

# 이전 기록 — 87일차 후속: 마스킹테이프 쓸어 붙이기

확인일: 2026-09-28. 수동 표준 모드. 87일차 작업지시서의 선택 목표였던 마스킹테이프 밀착 연출을 사용자 선택("조준 후 붙이기", "밀착 효과 살짝 넣기")으로 구현했어. 첫 QA 전 사용자 요청으로 "추가하면 세부 편집창이 바로 뜨고, 조준 중 `취소 | 편집 | 붙이기`" 흐름을 더했어(사용자 선택: 팝업 + 하단 편집 버튼). 로컬 자동검증과 사용자 실기기 QA를 마쳤고, 사용자 요청("커밋하고 푸시해줘")으로 commit·push해(실제 commit hash·CI는 `git log`·GitHub Actions로 확인). **이 문서의 다음 후보는 실행 승인이 아니야.**

## 현재 상태 빠른 확인

- 브랜치: `feature/photo-sticker`, HEAD `ec66fce` "Place label and text stickers with the tweezer hand"(CI run `36394271108` 성공), origin `0/0`
- 커밋 대상: `DetailScreen.kt`, `DetailViewModel.kt`, `MaskingTapeDetailScreen.kt` 수정 / 새 파일 `MaskingTapePlaceInteraction.kt`, `MaskingTapePlaceSessionTest.kt`, 이제 쓰는 자산 `tape_press_hand.png` / 이 HANDOFF·TEST-COVERAGE-MAP
- 보호 untracked `.codex-config.candidate.toml`, `.kotlin/` 보존

## 앱에 달라진 점

- 테이프 `+ 추가`(기본 디자인·커스텀·사진 세 방식 모두)에서 고르면 바로 붙지 않고, 붙인 테이프 `편집`과 같은 세부 편집창(가장자리·길이·굵기·회전, 창 안 미리보기가 즉시 바뀜)이 곧바로 떠. `저장`하면 엽서 위 +와 반투명 미리보기에 그 모양·길이·각도가 반영되고, `취소`는 값만 버려(추가는 계속). 탭·드래그로 자리, 두 손가락으로도 각도를 바꿀 수 있어. 패널은 안내 한 줄과 `취소 | 편집 | 붙이기`만 보여 — `편집`은 창을 다시 열고(현재 조준 각도에서 시작), `취소`는 추가 자체를 취소해.
- `붙이기` → 신문 오림 손이 화면 아래에서 올라와 테이프 한쪽 끝에 손가락을 대고(이 순간 실제 테이프 생성·"톡" 진동), 테이프 긴 축을 따라 반대쪽 끝까지 한 번 쓸고 빠져. 왕복·문지르기·흔들림·반동 없음.
- 밀착 연출: 손가락이 닿은 뒤 손 앞쪽(아직 안 눌린 부분)만 종이색 막(32%)으로 살짝 옅게 덮이고, 손이 지나간 자리부터 제 색이 돼. 쓸기가 끝나면 60ms에 걸쳐 사라져. 연출 overlay 안에서만 그리므로 저장·공유 이미지와 무관해.
- 추가 1번 = undo 1건, 붙인 뒤 선택 상태. 저장 형식·Room·serialization은 그대로(기존 offset·rotationDegrees만 채움). 복제·편집·삭제·끌기는 그대로.

## 구조와 결정

- 새 파일 `MaskingTapePlaceInteraction.kt`: `MaskingTapePlaceSession`(Idle→Aiming→Placing(placed)→Idle, 초안을 돌려주는 `startAiming`/`cancelAiming`/`abandon`, 편집창 값을 받는 `editAim` — 범위 제한 후 새 크기로 자리 재보정), 순수 계산 `maskingTapeSweep`·`maskingTapeUnpressedRange`·`maskingTapeSizePx`, `MaskingTapeAimLayer`, `MaskingTapePressHandOverlay`. 조준 +·진동은 스티커 것(`StickerPlaceCrosshair`·`vibratePhotoStickerContact`)을 그대로 써.
- 좌표: 조준 중심 = 엽서 미리보기 좌상단 기준 px, 붙인 테이프 끌기와 같은 `clampStickerOffset` 규칙(회전 전 사각형이 엽서 안)으로 가둬서 조준 = 착지. 크기는 붙인 테이프 Box와 같은 `132dp×scale×lengthScale`, `40dp×scale×thicknessScale`을 `roundToPx`. 이를 위해 `MASKING_TAPE_BASE_WIDTH/HEIGHT`를 private → internal로만 바꿨어(값 그대로).
- 쓸기 경로: 긴 축 방향 = 테이프 각도의 `(cos, sin)`(화면 가로 아님). 손이 뒤집히지 않도록 각도를 (-90°, 90°]로 접어서 늘 대체로 왼쪽→오른쪽(세로면 위→아래)으로 쓸고, 손 이미지도 그 각도만큼 돌려. 양 끝에서 테이프 굵기의 절반만큼 안쪽이 시작·끝점(짧으면 중심으로 모임).
- 손 anchor: `tape_press_hand.png`(1254×1254) 손가락 부분(y 90~500) 불투명 영역 가운데 약 (500, 325) → `MASKING_TAPE_HAND_ANCHOR_X/Y = 0.399/0.259`, 손 크기 260dp. 진입 320 → 누름 60 → 쓸기(길이 dp×2.6ms, 360~760ms, FastOutSlowIn) → 뗌 60 → 퇴장 220ms. 진입·퇴장 모양(아래에서 원근 +12%, 기울기 4°)은 핀셋 손과 같아.
- 사진 테이프: `addPhotoMaskingTape` → `importMaskingTapePhoto`(복사만, 콜백) + `addPlacedMaskingTape`. 예전에는 복사 전에 undo를 기록해 실패해도 빈 undo가 남았는데 이제 실제 추가 때만 생겨. 조준하다 버린 사진(취소·교체·화면 이탈)은 기존 `deleteMaskingTapePhotoIfUnreferenced`(현재 테이프·undo/redo가 참조하면 남김)로 정리해. 복사를 기다리는 사이 조준할 수 없게 되면(탭 이동·크게보기·뒷면·엽서 크기 미측정) 예전처럼 가운데에 바로 붙여.
- 조준 편집창: 기존 private `MaskingTapeEditDialog`를 그대로 재사용(패널 안에서 새 조준 id가 생기면 자동으로 열림). 새 dialog·새 slider는 만들지 않았어.
- 입력 잠금·취소: overlay가 모든 pointer 소비, 패널 `enabled && !isPlacing`, 조준 중 undo/redo 비활성. 테이프 탭 이탈·크게보기·뒷면이면 조준 취소, 뒤로가기는 조준만 취소하고 손 연출 중엔 흘려보냄.

## 검증과 남은 상태

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| 구현 | 완료 | |
| 로컬 JVM | 통과 866/866 (90 XML) | 신규 `MaskingTapePlaceSessionTest` 16건 |
| assembleDebug / assembleDebugAndroidTest | 성공 | |
| emulator instrumentation | 미실행 | androidTest 변경 없음, `PhotoStickerEdgeStyleInstrumentedTest` 7건은 여전히 미실행 |
| 실기기 QA | 완료 — 사용자가 "QA 완료" 보고, 보정 요청 없음(항목별 세부 결과는 받지 않음) | 아래 체크리스트 |
| TEST-COVERAGE-MAP | 갱신 완료 | 850→866, 파일 88→89 |
| Room / migration / serialization / dependency | 변경 없음 | |
| commit / push / CI | 사용자 요청으로 진행 | 결과는 완료보고·`git log`·GitHub Actions에서 확인 |

## 실기기 QA 체크리스트 (마스킹테이프 — 사용자 QA 완료, 기록용)

1. 기본 디자인·커스텀·사진 테이프 각각 `+ 추가` → 세부 편집창이 바로 뜨는지, `저장` 후 엽서 위 미리보기에 길이·굵기·각도·가장자리가 반영되는지(`취소`면 기본값 그대로 조준), 탭·드래그·두 손가락 회전, 하단 `취소 | 편집 | 붙이기`와 `편집`으로 다시 열기(돌린 각도에서 시작)가 되는지
2. 붙이기: 손가락이 테이프 끝에 닿는 순간 테이프가 생기고 "톡" 진동, 손이 긴 축을 따라 한 번만 쓸고 빠지는지(돌린 테이프도 축을 따라가는지), 조준한 자리·각도에 어긋남 없이 붙는지
3. 밀착 막이 "살짝"으로 보이는지(너무 진하거나 안 보이지 않는지), 손 크기·손가락 닿는 위치·쓸기 속도가 어색하지 않은지
4. undo 한 번에 방금 테이프만 사라지고 redo로 돌아오는지, 조준 중 취소·뒤로가기·탭 이동 시 생성 안 되는지
5. 사진 테이프 조준을 취소한 뒤에도 다른 사진 테이프·복제·저장이 정상인지
6. 스티커 붙이기·도장 회귀 없음

## 남은 위험

- 밀착 막은 화면 최상단 overlay에 그려서, 테이프 위에 겹친 스티커·도장이 있으면 쓸기 동안(최대 약 0.8초) 그 부분도 살짝 옅게 덮여 보일 수 있어.
- 조준 미리보기는 스티커 조준처럼 맨 위에 보이지만, 붙은 테이프는 원래 순서대로 스티커 아래에 그려져.
- 사진 테이프 복사가 끝나기 전에 화면 자체를 떠나면, 늦게 온 콜백이 사라진 화면의 조준을 시작해 그 복사본이 고아 파일로 남을 수 있어(사진 스티커와 같은 위험, 읽기 전용 `OrphanFileDiagnostics` 대상).
- 손 anchor·크기·쓸기 속도는 이미지 측정값이라 실기기 QA로 보정할 수 있어.

---

# 이전 기록 — 87일차 후속: 라벨·텍스트 스티커 핀셋 붙이기

확인일: 2026-09-28. 수동 표준 모드. 사진 스티커 핀셋 붙이기(`c8a2cf5`, CI run `36392041882` 성공)에 이어, 사용자가 고른 순서대로 라벨 스티커와 텍스트 스티커에 같은 조준·핀셋 붙이기 문법을 옮겼어(텍스트는 처음에 조준만 넣었다가 사용자 요청으로 핀셋 손을 추가). 로컬 자동검증과 사용자 실기기 QA를 마쳤고, 사용자 요청으로 commit·push해(실제 commit hash·CI는 `git log`·GitHub Actions로 확인). **이 문서의 다음 후보는 실행 승인이 아니야.**

## 현재 상태 빠른 확인

- 브랜치: `feature/photo-sticker`, HEAD `c8a2cf5` "Place photo stickers with a tweezer hand interaction", origin `0/0`
- 작업트리(미커밋): `DetailScreen.kt`, `LabelStickerDetailScreen.kt`, `TextStickerDetailScreen.kt`, `PhotoStickerPlaceInteraction.kt` 수정 / 새 파일 `LabelStickerPlaceInteraction.kt`, `TextStickerPlaceInteraction.kt`, `LabelStickerPlaceSessionTest.kt`, `TextStickerPlaceSessionTest.kt` / 이 HANDOFF·TEST-COVERAGE-MAP
- 보호 untracked `.codex-config.candidate.toml`, `.kotlin/` 보존. `tape_press_hand.png`는 미사용이라 여전히 untracked

## 앱에 달라진 점

- **라벨 스티커**: 문구·테이프를 고르고 확인하면 바로 붙지 않고 엽서 위 +와 반투명 미리보기로 자리·각도를 정해(붙인 라벨 편집처럼 크기 조절은 없음). `취소 | 붙이기` → 사진 스티커와 같은 핀셋 손이 라벨을 집어 와 톡 놓아. 핀셋은 회전한 라벨에서 화면 아래를 향한 변의 가운데를 물어.
- **텍스트 스티커**: 같은 조준(자리·크기 0.5~3배·각도)과 `취소 | 붙이기` → 핀셋 손이 외곽선 있는 글자 스티커를 집어 와 톡 놓아. 핀셋은 회전한 글자 영역(측정한 사각형)에서 화면 아래를 향한 변의 가운데를 물어 — 글자 모양이 아니라 사각형 기준이라 글자 사이 빈 곳을 무는 것처럼 보일 수 있어(QA로 판단).
- 둘 다 추가 1번 = undo 1건, 붙인 뒤 선택 상태. 저장 형식·Room은 그대로(기존 offset·scale·rotationDegrees만 채움).

## 구조와 결정

- 공용화한 최소 조각(`PhotoStickerPlaceInteraction.kt`): `clampStickerAimCenter`(직사각형판 조준 가두기), `rectStickerGripOffset`, `StickerPlaceCrosshair`, `StickerTweezerHandOverlay`(들고 갈 그림과 집는 자리만 받는 핀셋 손). 사진 스티커는 기존 동작 그대로 이 조각을 쓰도록 나눴어(상수·타이밍 변경 없음).
- 라벨: `LabelStickerPlaceSession`(Idle→Aiming→Placing(placed)→Idle). 크기는 `LabelStickerContent`와 같은 `LabelStickerRenderer`로 계산(`labelStickerSizePx`)해 조준 = 착지. 파일이 없어 정리할 것도 없어.
- 텍스트: `TextStickerPlaceSession`(Idle→Aiming→Placing(placed)→Idle). 크기는 글자 모양 측정이라 미리보기가 `onSizeChanged`로 잰 실제 크기를 쓰고, `붙이기` 때 마지막 크기로 자리를 한 번 더 가둬 고정해서 손 목표 = 착지야. 핀셋 손·집는 자리 계산은 라벨과 같은 `StickerTweezerHandOverlay`·`rectStickerGripOffset`.
- 조준 자동 취소: 각 하위 탭 이탈·크게보기·뒷면. 뒤로가기는 조준 취소, 손 연출 중엔 흘려보냄. 손 연출 중 각 패널 비활성. 엽서 크기를 아직 모르면 예전처럼 가운데에 바로 붙여.

## 검증과 남은 상태

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| 구현 | 라벨·텍스트 완료 | |
| 로컬 JVM | 통과 850/850 (89 XML) | 신규 라벨 10건·텍스트 7건 |
| assembleDebug / assembleDebugAndroidTest | 성공 | |
| emulator instrumentation | 미실행 | 연결 기기·emulator 없음(앞 확인 기준) |
| 실기기 QA | 완료 — 사용자가 "QA 완료" 보고, 보정 요청 없음(항목별 세부 결과는 받지 않음) | 아래 체크리스트 |
| TEST-COVERAGE-MAP | 갱신 완료 | 833→850, 파일 86→88 |
| Room / migration / serialization / dependency | 변경 없음 | |
| commit / push / CI | 사용자 요청("커밋하고 푸시해줘")으로 진행 | 결과는 완료보고·`git log`·GitHub Actions에서 확인 |

## 실기기 QA 체크리스트 (라벨·텍스트 — 사용자 QA 완료, 기록용)

1. 라벨 추가 → 조준 미리보기가 실제 라벨과 같은 모양·크기로 가운데에 뜨고, 탭·드래그·두 손가락 회전이 되는지(크기는 안 바뀜)
2. 라벨 붙이기: 핀셋이 라벨 아래 변(많이 돌리면 옆 변)을 문 것처럼 보이는지, 도착 순간 어긋남 없이 조준한 자리·각도에 놓이는지, "톡" 진동
3. 텍스트 추가 → 조준 미리보기에서 자리·크기·각도 조절, 붙이기 → 핀셋이 글자 스티커를 자연스럽게 무는지(글자 사이 빈 곳을 무는 것처럼 보이지 않는지), 조준한 자리에 놓이는지(엽서 가장자리에서 키운 뒤 붙여도 밖으로 안 나가는지)
4. 라벨·텍스트 각각 undo 한 번에 방금 것만 사라지고 redo로 돌아오는지, 조준 중 취소·뒤로가기·탭 이동 시 생성 안 되는지
5. 사진 스티커 붙이기·도장·흔들기 회귀 없음

## 남은 위험

- 텍스트 미리보기는 첫 프레임에 크기를 아직 몰라 아주 잠깐 가운데 기준이 어긋나 보일 수 있어(측정 직후 바로 맞음).
- 라벨 크기 계산은 화면 `LabelStickerContent`와 같은 렌더러 함수지만 px→dp→px 반올림으로 1px 차이가 날 수 있어.
- 마스킹 테이프 밀착 연출은 여전히 미착수(별도 승인 필요).

---

# 이전 기록 — 87일차 꾸미기 손 인터랙션(사진 스티커 핀셋 붙이기)

확인일: 2026-09-28. 수동 표준 모드(공용 작업판 비활성). 87일차 작업지시서의 필수 목표인 사진 스티커 핀셋 붙이기(조준 중 배경제거 포함)를 구현하고 로컬 자동검증과 사용자 실기기 QA까지 마쳤어. commit·push는 작업지시서 40·41절의 마감 절차로 수행해(실제 commit hash는 `git log`로 확인). 선택 목표인 마스킹 테이프 밀착 연출은 미착수 — 사용자가 다음 순서로 "사진 스티커 QA → 라벨 스티커 핀셋 붙이기 → 텍스트 스티커(조준만)"를 골랐어. **이 문서의 다음 후보는 실행 승인이 아니야.**

## 현재 상태 빠른 확인

- 브랜치: `feature/photo-sticker`
- 시작 HEAD: `dac9688` "Record final workflow closeout state", origin ahead·behind `0/0`, tracked clean
- 현재 작업트리: 아래 변경 파일이 **미커밋**. 보호 untracked `.codex-config.candidate.toml`, `.kotlin/` 보존
- 자산: 사용자가 넣은 `app/src/main/res/drawable-nodpi/sticker_tweezer_hand.png`, `tape_press_hand.png` (둘 다 1254×1254 RGBA PNG, 투명 배경). 첫 조사 때는 둘 다 없어서 자산 게이트 D로 조사만 하고, 사용자가 추가한 뒤 게이트 A로 진행했어.

## 앱에 달라진 점

- 사진 스티커를 갤러리·카메라·파일에서 고르면 바로 붙지 않고, 엽서 위 십자(+)와 반투명 미리보기로 자리·크기(0.5~2.5배)·각도를 먼저 정해. 패널은 안내 한 줄과 `취소 | 배경제거 | 붙이기`만 보여 줘(도장 찍기 조준과 같은 문법).
- 사용자 QA 첫 피드백("미리보기에 배경 제거가 안 돼서 불편")으로, 조준 중 `배경제거`/`원본복원` 토글을 추가했어(사용자가 선택지 중 "조준 중 누끼 토글"을 승인). 켜면 미리보기가 누끼 모양으로 바뀌고, 붙이면 처음부터 누끼 스티커로 생겨(undo 1건 그대로). 처리 중(`처리중...`)에는 토글·붙이기가 잠겨.
- `붙이기`를 누르면 신문 오림 핀셋 손이 스티커를 집은 채 화면 아래에서 올라와 조준한 자리에 놓고 빠져. 스티커는 손과 한 덩어리로 움직이고, 도착 순간 조준한 자리·크기·각도와 정확히 같아.
- 기존 엽서 데이터·저장 형식은 그대로야. 새 스티커는 기존 필드(offset·scale·rotationDegrees)만 채워 생성돼.

## 구조와 결정

- 새 파일 `ui/detail/PhotoStickerPlaceInteraction.kt`: `PhotoStickerPlaceSession`(Idle→Aiming→Placing(placed)→Idle), `PhotoStickerAimLayer`, `PhotoStickerTweezerHandOverlay`, `vibratePhotoStickerContact`. 도장(`SealStampInteraction.kt`)과 같은 모양이지만 코드는 공유하지 않아(공통 framework화 안 함).
- 좌표: 조준 중심 = 엽서 미리보기 좌상단 기준 px. 조준 중심을 붙인 스티커의 `clampStickerOffset`과 같은 규칙으로 가둬서 조준 = 착지. overlay 목표점은 도장과 같은 `postcardPositionInRoot - detailRootPositionInRoot + center`.
- 핀셋 anchor: 이미지에서 벌어진 집게 끝 약 (345,49)·(384,31) 바로 아래, 두 집게 사이 (372,56) → `PHOTO_STICKER_TWEEZER_ANCHOR_X/Y = 0.297/0.045`. 집는 점 = 스티커 중심에서 화면 아래로 `side/2 - 2dp`. 손·스티커는 이 점을 기준으로 함께 이동·기울기(4°)·원근 크기(+12%) 변환을 받아.
- 조준 중 배경제거: `DetailViewModel.removeAimStickerBackground` — 기존 `removeStickerBackground`와 같은 ML Kit·`saveStickerForegroundBitmap`(cache/photo_stickers/)을 쓰지만 undo·`stickerBackgroundRemovalState`는 건드리지 않아(아직 스티커가 아니라서). 결과는 기존 버튼이 만든 스티커와 같은 필드 모양(removedBgUri 보관, 켜졌을 때만 displayedUri)으로 채워. 조준이 취소·교체·출발한 뒤 늦게 온 결과는 세션이 돌려주고 바로 정리해.
- 누끼 핀셋 자리: 결과 그림을 48×48 격자로 훑어 alpha ≥128 표본점을 뽑고(Fit 배치 기준), 회전을 반영해 **화면에서 가장 아래 불투명 점**을 2dp 안쪽으로 물어. 기본 모양은 예전처럼 사각형 아래 변 가운데.
- 실제 스티커 생성 시점: 핀셋이 닿는 프레임 한 번(`takeContactPlacement` → `addPlacedPhotoSticker`). undo 1건 기록 + 추가 + 선택이 한 번에 일어나. 닿기 전·취소 후에는 생성 0건.
- 표현 중복 방지: 조준 미리보기는 닿는 순간 사라져. 들고 온 스티커는 실제 스티커가 확실히 그려지도록 같은 자리에 약 110ms(누름 40 + 멈춤 70) 겹친 뒤 손이 떼어질 때 사라져. 같은 자리·같은 그림이라 겹침은 보이지 않는 설계야(실기기 확인 필요).
- 타이밍: 진입 320 → 누름 40 → 멈춤 70 → 뗌 50 → 퇴장 220ms(도장 300/50/90/60/220 리듬). 안착 누름은 손만(1.2%), 스티커 크기는 변하지 않아(bounce 없음).
- 햅틱: 닿는 순간 한 번, 도장과 같은 Vibrator oneShot 방식에 14ms·세기 120("톡", 도장 28ms·200보다 약함).
- 입력 잠금: overlay가 모든 pointer를 소비, 스티커 패널 `enabled && !isPlacing`, 조준 중 undo/redo 비활성. 뒤로가기는 조준 중엔 조준만 취소, 손이 움직이는 동안은 흘려보냄(도장과 동일).
- 조준 자동 취소: 스티커 탭·사진 하위탭 이탈, 크게보기, 뒷면 전환.
- ViewModel: `addCameraPhotoSticker`/`addGalleryPhotoSticker` → `importCameraPhotoStickerOriginal`/`importGalleryPhotoStickerOriginal`(복사만 하고 콜백으로 원본 URI 전달) + `addPlacedPhotoSticker`. 예전에는 복사 전에 undo를 기록해서 복사가 실패해도 빈 undo가 남았는데, 이제 undo는 실제 추가 때만 생겨.
- 복사를 기다리는 사이 조준할 수 없는 상태(엽서 크기 미측정, 다른 탭, 크게보기·뒷면)가 되면 고른 사진을 잃지 않도록 예전처럼 가운데에 바로 붙여.
- 조준에 쓰다 버린 파일(원본·조준 중 배경제거 결과: 취소, 새 사진으로 교체, 화면 이탈, 늦은 결과)은 기존 `deleteStickerCacheUri` + `deleteStickerOriginalIfUnreferenced`로 정리해. 두 함수 모두 자기 폴더(`cache/photo_stickers/`·`sticker_bgs/` / `sticker_originals/`) 안의 `file://`이면서 현재 스티커·undo/redo 어디에서도 참조하지 않을 때만 지우고, 다른 폴더 파일과 SAF(파일에서 추가) URI는 건드리지 않아. 원본은 복사마다 새 UUID, 누끼 결과는 생성 시각 파일명이야.

## 검증과 남은 상태

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| 구현 | 사진 스티커 완료 / 마스킹 테이프 미착수 | 테이프는 스티커 미감 QA 게이트 대기 |
| 로컬 JVM | 통과 833/833 (87 XML, 실패·오류·skip 0) | 신규 `PhotoStickerPlaceSessionTest` 20건 포함(조준 중 배경제거 8건 추가분 포함) |
| assembleDebug / assembleDebugAndroidTest | 성공 | androidTest 소스 변경 없음 |
| emulator instrumentation | 미실행 | `adb devices -l` 결과 연결 기기·emulator 0개 |
| `PhotoStickerEdgeStyleInstrumentedTest` 7건 | 미실행 유지 | 위와 같음. 검증 전용 emulator에서만 후속 실행 |
| 실기기 QA | 완료 — 1차 피드백(누끼 미리보기) 반영 후 사용자가 "QA완료" 보고, 추가 보정 요청 없음(항목별 세부 결과는 받지 않음) | 핀셋 위치·크기·속도 미감, 조준 중 배경제거, 누끼 스티커 집는 자리, 조준 gesture, 진동, undo/redo, 도장·흔들기 회귀 |
| TEST-COVERAGE-MAP | 갱신 완료 | 813→833, 파일 85→86, 87일차 보호 범위 추가 |
| Room / migration / serialization / dependency | 변경 없음 | Entity·DAO·Migration·`PhotoStickerItem.serialize`·Gradle 미수정 |
| commit / push / CI | 작업지시서 40·41절 승인 범위로 진행 | 결과는 완료보고·`git log`·GitHub Actions에서 확인 |

## 작업 중 사고와 처리

- Python으로 파일을 다시 쓰는 과정에서 `DetailScreen.kt`, `PhotoStickerPlaceInteraction.kt`의 줄바꿈이 LF→CRLF로 바뀌었어. 그 때문에 LF 빈 줄 두 개로 선언 끝을 찾는 기존 구조 테스트 `BackgroundColorPickerEnabledStructureTest` 1건이 실패했어(분류: test infrastructure / 작업자 실수, production 아님). 두 파일을 원래 LF로 되돌린 뒤 당시 전체 825/825 통과를 확인했어. 테스트 조건은 바꾸지 않았어.

## 범위 밖 발견(수정 안 함)

- 조준 중 배경제거 계산을 테스트하다 `photoStickerGripOffset`이 집는 점을 중심 아래로만 제한(`max(0, …)`)하던 문제를 찾아 제거했어 — 누끼 그림이 모두 중심 위에 있으면 허공을 집게 되기 때문이야(아직 커밋 전 코드라 사용자 영향 없음).
- 기본 모양 사진 스티커는 화면에서 `fillMaxSize → clip(16dp 둥근 사각) → graphicsLayer(rotationZ)` 순서로 그려져, 코드 순서상 회전해도 둥근 틀은 돌지 않고 안의 사진만 도는 것으로 보여. 실제로 그렇게 보이는지, exporter와 일치하는지는 **미확인**이야. 핀셋 미리보기·들고 온 스티커는 같은 순서로 그려서 실제 스티커와 같게 맞췄고, 집는 거리도 이 전제(틀은 회전하지 않음)로 계산했어. 이 순서를 나중에 바꾸면 `photoStickerGripDistance`도 다시 봐야 해.

## 남은 위험과 재개 조건

1. 다음 작업(사용자 선택): 라벨 스티커에 같은 조준·핀셋 붙이기를 옮기고, 텍스트 스티커는 조준만(손 연출 없음) 넣어. 다른 꾸미기 영역으로의 문법 이전이라 각 영역 실기기 QA가 필요해. 텍스트·라벨은 크기가 글자에 따라 달라 조준 미리보기가 자기 크기를 재야 해.
2. 화면 이탈 시 원본 정리는 `viewModelScope`에서 실행돼. ViewModel이 같은 순간 정리되면 원본 파일 1개가 남을 수 있어(고아 파일 — 사용자 데이터 손상 아님, 기존 `OrphanFileDiagnostics` 진단 대상).
3. 사진 복사가 끝나기 전에 화면을 떠나면 복사 완료 콜백이 사라진 화면의 세션에 도착해 원본 1개가 남을 수 있어(위와 같은 고아 파일 위험).
4. 마스킹 테이프 밀착 연출: 사용자가 라벨·텍스트를 먼저 골라 보류. 별도 승인으로 진행해. 손 asset `tape_press_hand.png`는 저장소 폴더에만 두고 아직 commit하지 않았어(미사용 resource). 설계 메모 — 테이프 rotation은 degree·y-down·시계방향이라 긴 축 방향 = `(cos θrad, sin θrad)`, 긴 축 길이 = `MASKING_TAPE_BASE_WIDTH(132dp) × scale × lengthScale`, 손 asset `tape_press_hand.png`.
5. `PhotoStickerEdgeStyleInstrumentedTest` 7건은 검증 전용 emulator에서만 실행해.

## 실기기 QA 체크리스트 (사용자 QA 완료 — 기록용)

1. 사진 스티커 → 추가 → 갤러리/카메라/파일 각각: 조준 미리보기가 엽서 가운데에 뜨고, 탭·드래그·두 손가락 확대·회전이 되는지
2. 조준 중 `배경제거` → `처리중...` 뒤 미리보기가 누끼로 바뀌는지, `원본복원`이 바로 되는지, 누끼로 붙인 스티커가 편집 줄에서 `원본복원`으로 보이는지
3. 붙이기: 핀셋 끝이 스티커 아래 변(누끼면 그림의 가장 아래 끝)을 문 것처럼 보이는지, 손 크기·속도가 과하지 않은지, 도착 순간 순간이동·어긋남이 없는지, "톡" 진동
4. 붙인 뒤 스티커가 조준한 자리·크기·각도 그대로인지, 바로 선택돼 편집(모양·누끼·복제)이 되는지
5. undo 한 번에 방금 스티커만 사라지고 redo로 돌아오는지
6. 붙이기 연타 → 1개만. 손이 움직이는 동안 다른 조작·뒤로가기 무반응
7. 조준 중 취소·뒤로가기·탭 이동 → 스티커 생성 안 됨
8. 도장 찍기·흔들어서 한 장 기존 동작 회귀 없음

---

# 이전 기록 — 86일차 workflow 마감 구조 정비

확인일: 2026-09-27. 수동 표준 모드에서도 저장소 상태가 바뀐 개발 작업은 최종 완료보고 전에 `docs/ai/HANDOFF.md`를 최신화하도록 workflow를 보강했어. 이번 작업은 운영 문서와 canonical workflow plugin만 다뤘고 앱 production·test·Room·Gradle·CI YAML은 수정하지 않았어. **이 문서의 다음 후보는 실행 승인이 아니야.**

## 현재 상태 빠른 확인

- 브랜치: `feature/photo-sticker`
- 시작 HEAD: `1941308` "Recover 82-84 day handoff records from git and CI"
- 시작 origin 관계: ahead·behind `0/0`
- 시작 작업트리: tracked 변경 없음. 보호 untracked `.codex-config.candidate.toml`, `.kotlin/` 보존
- 현재 repository 변경: `AGENTS.md`, `CLAUDE.md`, `docs/ai/HANDOFF.md` 문서-only
- canonical workflow source: `C:/Users/estel/plugins/post-card-memory-workflow`
- 설치 캐시: `C:/Users/estel/.codex/plugins/cache/personal/post-card-memory-workflow/0.1.0+codex.20260927080957`
- source/cache: 전체 18개 파일 SHA-256 일치, 차이 0건
- plugin 내부 Markdown 링크: 14개 파일, 깨진 링크 0건
- plugin 재설치 명령: 성공 메시지와 새 캐시 생성을 확인했어. 직후 첫 조회에서는 일시적으로 `installed: false`, `enabled: false`가 보였지만 최종 재조회에서 새 버전 `installed: true`, `enabled: true`를 확인했어. 현재 세션은 이전 설치본을 이미 로드한 상태라 새 세션 실제 로딩만 아직 미확인이야.

## 이번에 고친 마감 구조

- `완료보고`: 작업자가 사용자에게 보내는 이번 실행 결과
- `다음 작업용 인수인계서`: ChatGPT·사용자가 다음 작업을 설계하거나 새 대화에 전달하는 별도 문서
- `repository HANDOFF`: 저장소의 `docs/ai/HANDOFF.md`. 실제 Git 상태·미검증·위험·재개 조건을 남기는 공용 교대 장부

수동 모드의 `CURRENT_TASK.md`·`WORK_CONTEXT.md`·`STATUS.md` 면제는 유지하지만 repository HANDOFF 마감까지 면제하지 않도록 분리했어. 기본 마감 순서는 아래와 같아.

```text
구현
→ 자동 검증
→ 필요한 사용자 QA
→ 테스트 변화가 있을 때 TEST-COVERAGE-MAP 갱신
→ repository HANDOFF 갱신
→ 전체 diff 확인
→ 명시적으로 승인된 commit / push
→ push했다면 CI 확인
→ 최종 완료보고
```

- 다음 작업 제안, 자동 후속 제안, `/clear` 권장은 repository HANDOFF 최신화 뒤에만 가능해.
- 날짜형 기록에 공백이 보이면 Git·CI·기존 기록으로 확인되는 사실만 복구하고, 나머지는 `미확인` 또는 `복구 필요`로 남겨.
- TEST-COVERAGE-MAP은 테스트 수·파일·의미·보호 범위가 실제로 바뀐 작업에서만 같은 작업일에 갱신해. 이번 작업은 production/test 변화가 없어 변경하지 않았어.
- commit·push는 기존 명시 승인 규칙을 그대로 따라. 이번 86일차 지시서에는 문서 commit·push·CI 확인이 명시적으로 포함돼 있어 현재 마감 단계에서 수행해.

## 검증과 남은 상태

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| workflow 정합성 | 확인 — 과거의 수동 모드 HANDOFF 면제 충돌 0건 | 저장소·canonical source 전체 검색 |
| source/cache | 확인 — 18/18 SHA-256 일치 | 새 설치 캐시와 canonical 비교 |
| plugin 설치 장부 | 확인 — installed / enabled 모두 true | `codex plugin list --available --json` 최종 재조회 |
| 내부 링크 | 확인 — 깨진 링크 0건 | Markdown 상대 링크 검사 |
| 공식 skill/plugin validator | 실행 불가 | 번들 Python에 `PyYAML`이 없어 import 단계에서 중단. 별도 dependency 설치는 범위 밖 |
| 앱 자동검증 | 불필요 | 운영 문서-only, 앱·테스트·Gradle 변경 없음 |
| 실기기 QA | 불필요 | 앱 동작 변경 없음 |
| TEST-COVERAGE-MAP | 변경 없음 | test 수·파일·의미·보호 범위 변화 없음 |
| 신규 instrumentation 7건 | 미실행 유지 | 85일차부터 compile만 확인. 이번 workflow 작업 범위 밖 |
| workflow commit / push / CI | 완료 — `257331e` push, CI 성공 | run `36305445591`: JVM unit tests·debug APK·instrumentation test compile 모두 success |

## 남은 항목과 재개 조건

1. 새 Codex 세션에서 `0.1.0+codex.20260927080957` 실제 로딩을 확인해. 현재 세션은 시작 시 이전 설치본을 로드했으므로 새 버전 로딩 근거가 될 수 없어.
2. `PhotoStickerEdgeStyleInstrumentedTest` 신규 7건은 검증 전용 emulator에서만 후속 실행해. 실사용 물리 기기에는 실행하지 않아.
3. 82~84일차 미확인 기록 3건은 아래 이전 기록의 상태를 유지해. 근거 없이 채우지 않아.

---

# 이전 기록 — 85일차 사진 스티커 오림 스타일 완료

확인일: 2026-09-26. 수동 표준 모드(공용 작업판 비활성). 85일차 작업지시서로 승인된 사진 스티커 오림 스타일 5종(기본·폴라로이드·가위 오림·찢은 종이·잡지 오림)은 구현·사용자 실기기 QA·commit·push·CI까지 끝났어. **이 문서의 다음 후보는 실행 승인이 아니야.**

이전 HANDOFF 원문(81일차 1~3단계 + 80일차 상세)은 [archive/HANDOFF-through-2026-09-22.md](archive/HANDOFF-through-2026-09-22.md)에 그대로 보존했어(git blob hash 일치 확인).

## 현재 상태 빠른 확인

- 브랜치: `feature/photo-sticker`
- 85일차 기능 commit: `c632887` "Add paper edge styles for photo stickers" (시작 HEAD `fdcabce`)
- origin: push 완료, 작업 종료 시 ahead·behind `0/0`
- 작업트리: tracked 변경 없음. 보호 untracked `.codex-config.candidate.toml`, `.kotlin/`만 존재하고 보존했어.
- 이 HANDOFF 갱신과 archive 추가는 별도 문서 commit으로 올라가. 실제 최신 HEAD는 `git log`로 확인해.

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| 구현 | 완료 — 5개 스타일 | `c632887` |
| 로컬 자동검증 | 실행 — JVM 813/813 통과, `assembleDebug`·`assembleDebugAndroidTest` 통과 | 로컬 결과 XML 86개 합산 |
| emulator instrumentation | **미실행** — 신규 7건은 컴파일만 확인 | 작업 중 `adb devices -l`에 실사용 기기만 연결, 검증 전용 emulator 없음. 실기기 자동 계측 금지 규칙 |
| GitHub Actions CI | 실행 — 성공 | run `36229495520` (HEAD `c632887`): JVM unit tests·Assemble debug APK·Compile instrumentation tests 모두 success |
| 실기기 감각 QA | 완료 | 사용자가 스타일별 확인. 찢은 종이·잡지 오림은 피드백 반영 후 재확인 |
| commit | 완료 | `c632887`, 12개 파일 명시 stage |
| push | 완료 | `fdcabce..c632887` |

## 앱에 달라진 점

- 사진 스티커를 선택하면 오림 선택 줄 **기본 · 폴라로이드 · 가위 오림 · 찢은 종이 · 잡지 오림**이 생겨. 각 타일은 그 스티커 사진을 실제 렌더러로 작게 그려 보여줘.
- **기존 엽서·스티커는 모두 "기본"으로 읽혀서 모습이 그대로야.** 기본은 새 디자인이 아니라 기존 렌더링 경로 그 자체야.
- 누끼 상태에서는 선택 줄이 비활성화돼. 저장된 스타일은 지우지 않고 렌더링에서만 쉬게 해서, 원본복원하면 원래 스타일이 돌아와.
- 사진 필터(세피아·채도·밝기 등)는 만들지 않았어.

## 사용자 QA로 확정된 형태

- **폴라로이드:** 위·좌·우 4.5%, 아래 13%의 흰 여백. 옅은 가장자리 선만 있고 그림자·자동 문구는 없어.
- **가위 오림:** 흰 종이 여백에, 네 변을 각각 1~2번 꺾인 곧은 가위질로 자른 외곽. 지그재그는 없어.
- **찢은 종이:** **네 변 모두 찢김.** 종이 외곽은 완만하게 일렁이고, 사진 인쇄층은 그보다 안쪽에서 얕게 찢겨 흰 단면 띠가 드러나.
  - 처음 한두 변만 찢던 방식은 사용자가 "윗부분만 찢어진 느낌이라 어색하다"고 해서 폐기했어.
- **잡지 오림:** 사진 둘레가 흰 종이가 아니라 **인쇄색 띠**야. 옅은 인쇄색 바탕 위에 망점을 올리고, 잉크 4색 중 하나를 seed로 골라.
  - 칼 단면에만 얇은 흰 선이 있고, 외곽은 거의 곧아. 사진 위에는 6% 먹 망점만 얹었어.
  - 처음 "흰 여백 + 희미한 망점"은 사용자가 "가위 오림과 차이를 모르겠다"고 해서 폐기했어.

## 지켜야 할 구조 규칙

- **저장:** Room이 아니라 `filesDir/sticker_states/<id>.txt`(편집 중 draft 포함)에 저장돼. `PhotoStickerItem` 탭 구분 줄의 11·12번 자리가 `edgeStyle`, `edgeSeed`야.
  - Room·schema(19)·migration 변경은 0건이야.
  - 필드 없음·알 수 없는 style·깨진 seed는 예외 없이 DEFAULT / seed 0으로 읽혀. `valueOf()`를 쓰면 `runCatching` 전체가 실패해서 스티커가 사라지니까 쓰지 않았어.
- **seed:** 처음 스타일을 적용할 때 한 번만 정해(0 = 미지정).
  - 복제는 `copy()` 흐름이라 style·seed가 같이 복사돼.
  - 이동·회전·재구성·재실행 중에는 다시 만들지 않아.
- **모양 생성:** 고정 알고리즘 `StableEdgeRandom`(SplitMix64)을 써.
  - ⚠ **생성 코드를 바꾸면 이미 저장된 스티커 모양이 바뀌어.** 가위 오림·찢은 종이 출력은 golden 테스트로 고정돼 있어.
- **preview / export:** canonical source는 `PhotoStickerPaperSpec`이야.
  - Compose preview(`PhotoStickerPaperModifier`)와 Android Canvas export(`PostcardImageExporter.drawPaperStyledSticker`)가 각자 이 spec으로 도형을 만들어.
  - 망점은 공통 `PhotoStickerHalftoneRenderer` 하나를 둘 다 써.
  - 대칭 처리도 같아: 종이는 대칭을 되돌리고, 사진은 거울상 창 안에 그려.
- **undo/redo:** 기존 `recordStickerSnapshotForUndo()` 경로를 그대로 써. 전용 history는 없어.

## 변경 파일 (`c632887`)

- **신규 production:**
  - `ui/detail/PhotoStickerEdgeStyle.kt`
  - `ui/detail/PhotoStickerPaperModifier.kt`
  - `ui/detail/PhotoStickerEdgeStyleRow.kt`
  - `utils/PhotoStickerHalftoneRenderer.kt`
- **수정 production:**
  - `PhotoStickerItem.kt`
  - `DetailViewModel.kt`
  - `DetailScreen.kt`
  - `PhotoStickerDetailScreen.kt`
  - `PostcardImageExporter.kt`
- **테스트:**
  - `PhotoStickerEdgeStyleTest.kt` (JVM 29건)
  - `PhotoStickerEdgeStyleInstrumentedTest.kt` (7건, 미실행)
- **문서:** `docs/ai/TEST-COVERAGE-MAP.md` (85일차 숫자·보호 범위 갱신)
- **변경 없음:** asset, dependency, Gradle, workflow, CI YAML

## 검증 세부

- **JVM:** 784 → 813 (+29). 테스트 파일 85 + helper 1, 결과 XML 86.
- **instrumentation:** 13 → 20개 / 7파일. 기존 13건은 80~81일차 통과 기록 그대로고, **신규 7건은 미실행**이야.
  - 신규 7건이 다루는 것: `PhotoStickerItem` 직렬화 왕복, 옛 8·11필드 형식 fallback, 알 수 없는 style, 복제 seed 유지, 누끼 중 스타일 보존.
  - Uri 때문에 JVM에서는 만들 수 없어서 instrumentation으로 작성했어.
- **중간 실패 1건:** `StickerItemFlatBoxRemovalStructureTest`(타일 호출 수 2개 고정)와 충돌했어.
  - 테스트 조건은 바꾸지 않고, 선택 줄을 별도 파일로 분리해서 해결했어.
- **리팩터링 검증:** 잡지 오림 작업 중 가위 오림 생성 코드를 파라미터화했어. 그 전에 golden 값을 뽑아 두고, 리팩터링 후 출력이 동일한 걸 확인했어.
- **의도적 조건 변경:** 잡지 오림 재설계로 여백 망점 강도 상한 테스트를 0.3 → 0.6으로 바꿨어. 사진 위 망점 상한 0.08은 유지했어.

## 알려진 위험·미검증

1. **신규 instrumentation 7건 미실행.**
   - 재개 조건: 검증 전용 emulator만 연결된 상태(`adb devices -l`로 확인)에서 실행.
2. **기존 DEFAULT 스티커의 preview/export 차이**는 기록만 하고 범위 밖으로 뒀어. 고치면 기존 엽서 모습이 바뀌어서 손대지 않았어.
   - export에만 상시 검은 테두리가 있어.
   - preview는 clip이 회전보다 바깥이라 회전 시 자르는 틀이 안 돌 가능성이 있어.
3. **preview와 export의 그림 일치**는 공통 spec까지만 자동 검사해. 실제 그림 일치는 사용자 QA로만 확인했어.
4. **스타일 변경 undo/redo**는 전용 자동 테스트가 없어. 기존 경로 재사용과 사용자 QA로 확인했어.

## 기록 공백 (발견사항)

- 이 파일은 81일차 3단계(`000e7ce`) 이후 82~84일차 동안 갱신되지 않았어.
- 사용자 지시에 따라 그 기간 내용은 추정으로 역작성하지 않았어.
- 확인 가능한 사실은 git의 commit 목록뿐이야(`000e7ce..fdcabce`):
  - `3b7dfb3` Update test coverage map after emulator validation
  - `d833c2a` Make postcard stamps feel ink-pressed
  - `0a19187` Add newspaper-hand stamping interaction for seals
  - `af59a93` Update test coverage map after seal interaction tests
  - `a659f89` Keep test coverage map current with test changes
  - `d2a875f` Add shake-to-draw random postcard overlay
  - `fdcabce` Animate subtle steam above gallery clock
- 각 날짜의 QA·위험 상세가 필요하면 해당 일자의 완료보고·인수인계서를 근거로 따로 정리해야 해.
- 83~84일차 테스트 수 변화는 `docs/ai/TEST-COVERAGE-MAP.md`의 일자별 "추가 확인" 문단에 기록돼 있어.

## 범위 밖으로 남긴 항목 (85일차 지시서 기준, 수정 안 함)

- 랜덤 엽서 overlay의 왼쪽 끝 스와이프
- 삭제 중 랜덤 후보 포함 가능성
- 연못 모드 흔들기 판정
- 시스템 애니메이션 줄이기
- 도장·달력·흔들어서 한 장·커피 김 추가 수정
- 갤러리 구조 변경

## 다음 후보 (승인된 작업 아님)

1. 검증 전용 emulator에서 `PhotoStickerEdgeStyleInstrumentedTest` 7건 실행.
2. 82~84일차 기록 공백을 실제 완료보고 근거로 정리할지 결정.
3. 기존 DEFAULT 스티커의 preview/export 차이를 정리할지 제품 판단 (기존 엽서 모습이 바뀌므로 신중).

## 재개 시 확인

- `git status`, 최신 HEAD, origin 0/0
- 보호 untracked 2개가 그대로인지
- instrumentation을 실행한다면 먼저 `adb devices -l`로 실사용 기기가 없는지 확인

---

## 82~84일차 기록 복구 (2026-09-26 복구, 이력)

위 "기록 공백" 절의 82~84일차를 실제 근거로만 복구한 이력이야. 현재 상태나 실행 승인이 아니야. 근거 표기:
- **[git]** commit 메시지·변경 파일
- **[CI]** GitHub Actions run 조회 결과
- **[지도@커밋]** 그 커밋 시점의 `docs/ai/TEST-COVERAGE-MAP.md`
- **[세션 메모]** 당시 Claude Code 세션이 남긴 로컬 메모. 저장소 밖 기록이라 보조 근거로만 써

근거를 찾지 못한 항목은 **미확인**으로 남겼어.

### 82일차 — 2026-09-23 · TEST-COVERAGE-MAP 최신화 (docs-only)

- **commit:** `3b7dfb3` "Update test coverage map after emulator validation" [git]
  - 변경 파일: `docs/ai/TEST-COVERAGE-MAP.md` 1개(+37/−34)
  - 커밋 메시지에 "Docs-only, no production/test/dependency/CI changes"라고 명시돼 있어.
- **반영 내용** [git·지도@3b7dfb3]:
  - 80~81일차 emulator 결과를 지도에 반영했어. instrumentation 총수 10→13개/6파일(`PostcardDeletionOrchestrationTest` 3건 추가).
  - Espresso/API 37 수정 후 `PostcardBackRenderingTest` 3/3, 전체 13/13 통과.
  - emulator가 `mWakefulness=Asleep`이면 draw pass가 없어 저장 timeout이 날 수 있다는 환경 메모를 넣었어. 분류는 `emulator / OS environment`야.
  - 로컬 emulator 실제 실행과 CI의 Android test APK 컴파일(`assembleDebugAndroidTest`)을 구분해 기록했어.
- **JVM 750** [지도@3b7dfb3]: 지도에는 "81일차에 실제로 재실행해 750/750 통과를 확인했고, 82일차에는 재검증 없이 그 결과를 그대로 썼어"라고 적혀 있어. 82일차에 로컬 JVM을 다시 실행했다는 기록은 없어.
- **CI:** run `35805774124` success (head `3b7dfb3`) [CI]
  - JVM unit tests, Assemble debug APK, Compile instrumentation tests 모두 success.
- **당시 남은 항목** [지도@3b7dfb3]:
  - instrumentation은 여전히 CI에서 자동 실행되지 않아(emulator 없음). 실제 실행은 로컬 검증 전용 emulator에서만 확인됐어.
  - 그 밖의 경고·후속 항목은 기록에서 찾지 못했어 → 미확인.

### 83일차 — 2026-09-24 · 도장 잉크 질감 · 도장 찍기 · 지도 · 규칙

**1차 — 도장 잉크 질감** `d833c2a` "Make postcard stamps feel ink-pressed" [git]
- **기능:** 기존 seal id에서 결정론적 잉크 결손 지도를 만들어(FNV-1a seed, SplitMix64 value noise). 누름 성격은 세 가지야(WELL/MEDIUM/LIGHT).
- **렌더러:** 공용 `SealInkWearRenderer`를 화면(`SealPreviewContent`)과 `PostcardImageExporter`가 같이 써.
- **저장:** 새 저장 필드 없음, Room·직렬화 변경 없음.
- **예외:** UI 아이콘(패널 타일·다이얼로그·인트로 소인)은 질감 없이 유지해.
- **테스트:** `SealInkWearTest` 5건 + `PostcardOverlayExportLogicTest` seed 연결 1건 → JVM 750→756 [지도@af59a93].
- **CI:** run `35971142738` success — 세 단계 모두 success [CI].
- **QA** [세션 메모]:
  - 1차 실기기 QA에서 "너무 약함" 피드백 → 강도와 단계별 성격 차이를 반영했어.
  - 2차 QA에서 승인됐어.
  - "점 무리 + radial 얼룩" 접근은 먼지·빛 번짐처럼 보여서 폐기했어.

**2차 — 신문 오림 손 도장 찍기** `0a19187` "Add newspaper-hand stamping interaction for seals" [git]
- **흐름:** 도장 추가 → 조준 단계(같은 id라 잉크 결손이 같은 반투명 미리보기 + 작은 +, 탭·드래그·핀치·회전) → "도장 찍기" → 신문 오림 손이 아래에서 올라와 찍어.
- **생성:** 손이 닿는 프레임에 도장이 한 번 생성되고, undo 1건과 햅틱 1회가 함께 일어나.
- **범위:** 손은 일시 overlay라 저장·복원·export에 들어가지 않아.
- **asset:** `res/drawable-nodpi/seal_stamp_hand.png`(약 1.8MB) 추가.
- **테스트:** `SealStampSessionTest` 10건 → JVM 756→766 [git·지도@af59a93].
- **이 시점 검증** [지도@af59a93·CI]:
  - 로컬 JVM 766/766.
  - CI run `35977829337` success — JVM unit tests, Assemble debug APK, Compile instrumentation tests 모두 success.
- **QA** [세션 메모]: 손목 끝의 직선 잘림은 사용자가 "신문지 오린 것 같아 좋음"으로 승인했어.

**3차 — TEST-COVERAGE-MAP 갱신** `af59a93` "Update test coverage map after seal interaction tests" [git]
- **변경 파일:** 지도 1개(+26/−24). 지도 기준 HEAD는 `0a19187`.
- **수치:** JVM 766, 테스트 파일 82 + helper 1, XML 83. instrumentation 13개/6파일 그대로(83일차 androidTest 변경 없음) [지도@af59a93].
- **보호 범위 반영:**
  - 같은 id의 잉크 결정성
  - preview/export seed 연결
  - 조준→찍기 순수 상태 전이
- **구분해서 적은 것:** Compose pointer 입력, 신문지 손 animation, 진동(haptic), 실제 undo 연결은 실행 검증이 아니라고 따로 적었어.
- **CI:** run `35981166343` success — 세 단계 모두 success [CI].

**4차 — 공통 규칙 반영** `a659f89` "Keep test coverage map current with test changes" [git]
- **변경 파일:** `AGENTS.md` 1개(+9). 8절에 "TEST-COVERAGE-MAP 조건부 마감" 절을 추가했어(갱신 조건, 권장 마감 순서, 숫자 구분, 보호 범위 표기, 완료보고 기록).
- **성격:** docs/rules-only. production·test 변경 없음.
- **CI:** 생성됐어 — run `35984597452` success, 세 단계 모두 success [CI].
- **workflow source/cache 정합성: 미확인.**
  - 당시 SHA-256 일치 결과와 링크 검사 결과는 저장소, commit 메시지, 세션 메모 어디에서도 찾지 못했어.
  - 참고로 2026-09-26에 읽기 전용으로 관찰한 사실은 이것뿐이야: 저장소 밖 workflow source(`post-card-memory-workflow` plugin)의 version marker가 `0.1.0+codex.20260924094513`이고, `test-coverage-map-maintenance.md` 등에 TEST-COVERAGE-MAP 규칙이 들어 있어.
  - 이 관찰은 당시 검증 결과를 대신하지 않아.

**84일차로 넘긴 맥락** — "흔들어서 랜덤 엽서 + 커피잔 김 애니메이션"을 84일차로 넘겼다는 83일차 당시 기록은 저장소·commit·세션 메모에서 찾지 못했어 → **미확인**. 확인된 사실은 다음 날 두 작업이 실제로 수행됐다는 것(84일차 commit)뿐이야.

### 84일차 — 2026-09-25 · 흔들어서 한 장 · 커피잔 김

**1차 — 흔들어서 랜덤 엽서** `d2a875f` "Add shake-to-draw random postcard overlay" [git]
- **흔들기 감지:** 순수 `GalleryShakeDetector`(중력 제거, 창 안의 방향 전환 2회, cooldown). 센서 리스너는 갤러리 back stack entry lifecycle을 따라서, 상세 화면과 백그라운드에서는 멈춰.
- **흔들기가 꺼지는 경우:** play mode, 선택 모드, 삭제 확인, 방문 달력, overlay가 열려 있을 때.
- **랜덤 선택:** 균등 랜덤이고, 후보는 현재 검색 결과(`filterPostcardsForSearch(postcards, searchQuery)`) 범위 안이야 [git diff].
- **미래편지 제외:** 새 코드가 아니야. 갤러리 목록 자체가 `PostcardDao`의 `futureMailState = 'NONE'` 쿼리에서 오기 때문에 발송된 미래편지는 원래 후보에 없어 [현재 코드 대조].
- **overlay:** 흐려진 갤러리 위에 엽서를 띄우고, 신문 오림 손이 엽서 왼쪽 아래 모서리를 집어 올려.
  - 날짜 문구는 월·일 0 채움 없음, 자정 경계는 zone을 따라가(`storyLabel_*` 테스트).
  - 엽서를 탭하면 기존 상세 화면이 열리고, 바깥 탭이나 뒤로가기로 닫혀.
- **asset:** `res/drawable-nodpi/gallery_pick_hand.png`(약 1.4MB) 추가.
- **테스트:** `GalleryShakeDetectorTest` 11건 + `GalleryRandomPostcardOverlayTest` 7건 → JVM 766→784. 같은 commit에서 지도도 갱신했어 [git·지도@fdcabce].
- **QA** [세션 메모]:
  - 흔들면 상세로 바로 이동하던 방식은 "갑작스럽다"는 QA로 overlay 방식이 됐어.
  - 손 asset은 사용자가 직접 준비했어(지시서상 준비 전 STOP).
  - 손 위치는 사용자 요청으로 가운데가 아니라 왼쪽 아래 모서리를 집게 했어.
  - 연못 모드의 옛 흔들기 판정과 겹치지 않게, 새 판정은 playMode NONE에서만 켜.

**2차 — 커피잔 김 애니메이션** `fdcabce` "Animate subtle steam above gallery clock" [git]
- **동작:** 컵은 그대로 두고 김 두 줄만 좌우로 약 1dp 흔들려. 주기는 2.8초와 3.4초라 두 줄이 같이 움직이지 않아.
- **유지된 것:** 김의 모양·색·alpha는 기존 정적 아이콘과 같아.
- **변경 파일:** `GalleryRetroClock.kt` 1개. 테스트 변경은 없어(시각 전용) [git·지도@fdcabce].

**검증** [CI·지도@fdcabce]:
- `d2a875f`와 `fdcabce`는 커밋 시각이 같고(2026-09-25 21:16:13 +0900) 함께 push됐어. 그래서 CI run은 head `fdcabce` 하나야 — run `36133974480` success, 세 단계 모두 success. `d2a875f` 단독 CI run은 없어.
- 로컬 JVM 784/784.
- instrumentation은 13개/6파일 그대로(84일차 androidTest 변경 없음).
