# 원자적 저장 덮어쓰기 안정화 — 결과 보고서

- 대작업: DetailScreen·DetailViewModel 구조 감사 및 단계적 안정화
- 종류: 제2차 후속 수정 (renameTo() 기반 저장 덮어쓰기 구현의 플랫폼 독립적 교체)
- 실제 작업일: 2026-08-06
- 관련 선행 문서: `docs/storage-overwrite-test-failure-investigation-2026-08-06.md` (원인 조사)

---

## 1. 최종 판정

**완료** (단, §7 자동검증은 이 환경에 `gradlew`/`java`가 없어 직접 실행하지 못함 — `사용자 검증 대기`로 명시)

---

## 2. Git 기준 상태

| 항목 | 값 |
|---|---|
| 브랜치 | `feature/photo-sticker` |
| 시작 HEAD | `8273c78` (`Investigate storage overwrite test failures` 커밋, 이미 push됨) |
| 종료 HEAD | 이 보고서 작성 시점까지 신규 커밋 없음(작업 내용은 작업트리에만 존재, 아래 §변경 파일 참고) |
| origin HEAD | `8273c78` (fetch 후 일치, 시작 시점과 동일) |
| 작업트리 | `.kotlin/`(빌드 산출물), 프로덕션 3개 수정, 테스트 3개 수정, 신규 파일 2개, 삭제 1개 — 상세는 아래 표 |
| 조사 커밋 상태 | 선행 확인 완료 — `git log`에서 `8273c78 Investigate storage overwrite test failures`가 이미 HEAD이자 origin HEAD와 일치함을 확인. 별도 재커밋 불필요 |

### 변경 파일 (`git status --short` 기준)

```text
 M app/src/main/java/com/postcardmemory/utils/ConfirmedEditStateStorage.kt
 M app/src/main/java/com/postcardmemory/utils/PostcardDraftStorage.kt
 M app/src/main/java/com/postcardmemory/utils/PostcardTemplateStorage.kt
 M app/src/test/java/com/postcardmemory/utils/ConfirmedEditStateStorageTest.kt
D  app/src/test/java/com/postcardmemory/utils/FileRenameToOverwriteDiagnosticTest.kt
 M app/src/test/java/com/postcardmemory/utils/PostcardDraftStorageTest.kt
 M app/src/test/java/com/postcardmemory/utils/PostcardTemplateStorageTest.kt
?? app/src/main/java/com/postcardmemory/utils/AtomicFileReplace.kt
?? app/src/test/java/com/postcardmemory/utils/AtomicFileReplaceTest.kt
```

`git diff --stat` 기준 기존 파일 변경은 6개 파일에 걸쳐 **65줄 추가, 4줄 삭제**뿐이며, `DetailScreen.kt`/`DetailViewModel.kt`/UI/Room 관련 파일은 전혀 포함되지 않았다.

---

## 3. renameTo() 전수 조사

프로젝트 전체(`app/src`)에서 `renameTo(` 재검색 결과, 조사 보고서가 확인한 것과 정확히 동일한 **4곳**이 유일한 프로덕션 사용처였다(추가 발견 없음).

| 파일:줄 | 함수 | 대상 파일 | 덮어쓰기 가능 여부 | 이번 작업 포함 여부 |
|---|---|---|---|---|
| `ConfirmedEditStateStorage.kt:33` | `writeTextAtomically` | `sticker_states/`, `seal_states/`, `doodle_states/` 확정 파일 | 예(반복 편집 시 항상 재작성) | 포함 |
| `PostcardDraftStorage.kt:80` | `saveDraftAtomically` | `drafts/edit_state/` 초안 파일 | 예(자동저장이 같은 postcardId를 반복 저장) | 포함 |
| `PostcardTemplateStorage.kt:70` | `saveTemplateAtomically` | 사용자 템플릿 본문 `.txt` | 예(같은 id로 덮어쓰기 시나리오 존재) | 포함 |
| `PostcardTemplateStorage.kt:108` | `savePreviewAtomically` | 사용자 템플릿 미리보기 `.png` | 예(템플릿 덮어쓰기 시 미리보기도 같이 갱신) | 포함 |

수정 후 재검색(`app/src/main/java/com/postcardmemory` 대상)에서 `renameTo(` 사용처는 0건이다 — 4곳 모두 교체됨을 확인했다. (`AtomicFileReplace.kt`의 문서 주석에 "renameTo()"라는 문자열이 설명용으로 남아 있으나 실제 호출이 아니다.)

---

## 4. 공통 helper

| 항목 | 내용 |
|---|---|
| 파일 | `app/src/main/java/com/postcardmemory/utils/AtomicFileReplace.kt` (신규) |
| 함수 | `AtomicFileReplace.replace(tempFile: File, targetFile: File): Boolean` |
| 입력 | 이미 쓰기가 끝난 임시 파일과 최종 대상 파일(같은 디렉터리 전제) |
| 출력 | `Boolean` — 기존 4곳의 반환 계약(성공 시 `true`, 실패 시 `false`)과 동일한 의미로 그대로 사용 가능 |
| 원자 이동 | `Files.move(tempPath, targetPath, REPLACE_EXISTING, ATOMIC_MOVE)`를 우선 시도 |
| fallback | `AtomicMoveNotSupportedException`을 잡았을 때만 `Files.move(tempPath, targetPath, REPLACE_EXISTING)`(원자성 없이 교체)로 한 번 재시도 |
| 실패 처리 | 위 두 시도가 모두 `IOException`으로 실패하면 `false` 반환. 어떤 예외도 무조건 삼켜 성공으로 처리하지 않음(`AtomicMoveNotSupportedException`과 `IOException`만 각각의 위치에서 명시적으로 처리) |
| 대상 파일 선삭제 | 하지 않음 — `REPLACE_EXISTING` 옵션이 교체를 담당하므로 별도 삭제 코드가 없고, 실패 시 대상 파일은 항상 그대로 보존됨 |
| 임시 파일 처리 | 실패 시(`moved == false`)에만 `tempFile.delete()` 시도. 정리 성공 여부는 함수의 반환값에 전혀 영향을 주지 않음(정리 실패가 저장 성공으로 오인될 수 없는 구조) |
| minSdk 적합성 | `java.nio.file.Files`/`StandardCopyOption`/`AtomicMoveNotSupportedException`은 Android API 26부터 네이티브 지원되며, 이 프로젝트의 `minSdk = 26`(`app/build.gradle.kts:14`)과 정확히 일치해 별도 core library desugaring 설정 없이 사용 가능함을 확인했다 |
| 책임 범위 | 파일 교체 책임만 담당(직렬화·텍스트 생성·편집 상태·ViewModel·Repository·오류 메시지·백업 정책·Room 어느 것도 다루지 않음) — 작업지시서 §7 제한 범위를 그대로 준수 |

---

## 5. 수정한 Storage

### ConfirmedEditStateStorage
`writeTextAtomically`(15~38줄) 내부의 `tempFile.renameTo(targetFile)` 한 줄만 `AtomicFileReplace.replace(tempFile, targetFile)`로 교체했다. 임시 파일 생성 방식, 텍스트 내용, 반환값 계약, `IOException` 발생 시 처리, 확정 파일 경로, 호출부 계약은 전혀 변경하지 않았다.

### PostcardDraftStorage
`saveDraftAtomically`(59~85줄) 내부의 동일한 한 줄만 교체했다. 자동저장이 같은 `postcardId`로 반복 호출하는 경로(`DetailViewModel.persistDraftNow` → `PostcardDraftStorage.saveDraftAtomically`)는 이 파일을 전혀 건드리지 않았으므로 그대로 유지되며, 이제는 기존 초안 파일이 있어도 정상적으로 교체된다(§6·§8에서 검증).

### PostcardTemplateStorage
`saveTemplateAtomically`(49~75줄)와 `savePreviewAtomically`(83~113줄) 두 함수 각각의 `tempFile.renameTo(target)`을 `AtomicFileReplace.replace(tempFile, target)`로 교체했다. 두 함수의 의미(템플릿 본문 vs 미리보기 PNG)는 다르지만 "파일 교체" 책임만 공통화했고, 각 함수의 나머지 로직(직렬화, 비트맵 압축, 실패 시 처리)은 변경하지 않았다.

---

## 6. 테스트 변경

### 기존 테스트(기대값 변경 없음, 모두 그대로 통과해야 함)
- `ConfirmedEditStateStorageTest.writeTextAtomically_overwritesExistingConfirmedFile_onSuccess` — 실패하던 테스트, 수정 후 통과 예상. 추가로 "덮어쓰기 후 temp 파일 잔존 없음" 검증 1줄 보강(§9단계, 부족한 지점만 추가)
- `PostcardTemplateStorageTest.overwrite_sameId_replacesContent_otherTemplatesUntouched` — 실패하던 테스트, 수정 후 통과 예상. 동일하게 "덮어쓰기 후 temp 파일 잔존 없음" 검증 보강
- 그 외 기존 테스트(신규 파일 저장, 실패 시 보존, 부모 디렉터리 생성, 삭제, 손상 파일 스킵 등)는 이번 변경으로 동작이 달라지지 않으므로 손대지 않았다

### 신규 테스트
| 파일 | 테스트 수 | 검증 내용 |
|---|---|---|
| `AtomicFileReplaceTest.kt`(신규) | 4개 | 대상 없을 때 이동 성공, **대상이 이미 있을 때 새 내용으로 덮어쓰기 성공**(이번 수정의 핵심 계약), 성공 후 temp 미잔존, temp가 없을 때 실패 반환 + 기존 대상 보존 |
| `PostcardDraftStorageTest.kt`(기존 파일에 2개 추가) | +2개 | `saveDraftAtomically_sameIdSavedTwice_secondSaveOverwritesFirst`(작업지시서 8단계가 요구한 "같은 postcardId 반복 저장" 시나리오 — 최초 저장 성공, 두 번째 저장 성공, 재로드 시 두 번째 값, temp 미잔존), `saveDraftAtomically_overwritingOnePostcard_otherPostcardDraftUntouched`(다른 postcardId 초안은 영향 없음) |

### 진단 테스트 처리 (10단계)
`FileRenameToOverwriteDiagnosticTest.kt`를 **삭제**했다. 이유:

- 이 테스트는 원시 `File.renameTo()`의 플랫폼 종속 동작을 관찰하기 위한 것으로, 저장 클래스 로직과 분리해 원인을 확정하는 데는 이미 목적을 다했다(원인 조사 보고서에 그 근거가 남아 있음)
- 이 테스트 자체는 원래도 OS별로 다른 assertion을 강제하지 않고 "성공하면 new-content, 실패하면 old-content(부분 손상 없음)"라는 플랫폼 독립적 불변식만 검증하도록 작성돼 있었으나, 지금은 **원시 `renameTo()`를 프로덕션에서 더 이상 아무 곳에서도 쓰지 않으므로** 이 테스트가 관찰하는 대상 자체가 프로덕션 코드와 무관해졌다
- 작업지시서 10단계의 "실제 저장 helper 회귀 테스트로 대체" 선택지에 따라, 이를 새로 만든 `AtomicFileReplace`의 계약을 직접 검증하는 `AtomicFileReplaceTest.kt`로 대체했다 — 이 새 테스트는 `Files.move(..., REPLACE_EXISTING)`을 사용하므로 Windows·Linux·Android 어디서나 동일한 성공 결과를 기대할 수 있어(§4 근거) "Windows에서만 통과" 같은 취약함이 없다

### 테스트 기대값 변경 여부
**없음.** 기존 두 실패 테스트의 assertion(`assertTrue(saved)`, `assertEquals("바뀐 이름", ...)`)은 한 글자도 바꾸지 않았다. 완화된 assertion, `@Ignore`, 삭제된 실패 테스트는 없다.

---

## 7. 자동검증

| 검증 | 결과 |
|---|---|
| Kotlin 컴파일 | **실행하지 못함** — 이 환경에 `gradlew`/`gradlew.bat`도, 전역 `gradle`/`java`도 없음(재확인 완료). 대신 변경된 4개 프로덕션 파일과 2개 신규 테스트 파일 전체를 다시 읽어 구문(import, 예외 처리 순서, 패키지 일치, 함수 시그니처)을 수동으로 재검토했다 |
| 기존 실패 테스트 2건 | **실행하지 못함**(위와 동일 사유). 코드 추적 결과 두 테스트 모두 실패 지점이던 `renameTo()` 호출이 `AtomicFileReplace.replace()`로 교체됐고, 새 구현은 대상 존재 여부와 무관하게 `Files.move(..., REPLACE_EXISTING)`으로 성공하므로 통과할 것으로 판단하나 **실행으로 확인하지 않았다** |
| 관련 저장 테스트 | 실행하지 못함(위와 동일) |
| 전체 단위 테스트 | 실행하지 못함(위와 동일). 제2차 사용자 실행 기준 292개에서, 이번에 삭제 1개(`FileRenameToOverwriteDiagnosticTest`, 1개 테스트) + 신규 6개(`AtomicFileReplaceTest` 4개, `PostcardDraftStorageTest` +2개)로 순증 5개, 전체 297개 예상(실행 확인 전까지는 추정) |
| 경고 | 해당 없음(컴파일을 실행하지 못해 경고 여부도 확인 불가) |

**이 항목은 완료 조건 중 "기존 실패 테스트 2건 통과", "관련 저장 테스트 전체 통과", "전체 단위 테스트 실패 0건", "Kotlin 컴파일 성공"을 자동으로는 충족하지 못했다는 뜻이다.** 코드 검토로는 통과를 예상하지만 단정하지 않는다. **사용자 검증 대기**: Android Studio에서 Kotlin 컴파일과 다음 4개 테스트 클래스 실행 확인 필요 — `ConfirmedEditStateStorageTest`, `PostcardTemplateStorageTest`, `PostcardDraftStorageTest`, `AtomicFileReplaceTest`. 이어서 전체 단위 테스트도 실행해 실패 0건을 확인해야 한다.

---

## 8. 실사용 영향

이번 수정은 **호출부(파일 교체 책임의 반환값을 소비하는 쪽)를 전혀 건드리지 않았으므로**, 실패 시 기존 파일 보존·저장 실패 안내 흐름 등 기존 계약은 코드상 그대로 유지된다. 이번 수정으로 달라지는 것은 오직 "덮어쓰기가 실제로 성공하는 빈도"뿐이다(Windows에서는 실패하던 것이 이제 성공).

| 항목 | 확인 결과 |
|---|---|
| 자동저장 | `DetailViewModel.persistDraftNow()`(762~835줄)는 `PostcardDraftStorage.saveDraftAtomically`의 반환값을 그대로 `saved`로 받아 `_draftSaveStatus`를 갱신하는 구조를 그대로 유지 — 이번 수정으로 "같은 postcardId 반복 자동저장"이 (Windows에서도) 정상적으로 최종 파일까지 반영됨 |
| confirmed edit state | `DetailViewModel.persistStickerEditState`(932~982줄) 등은 `ConfirmedEditStateStorage.writeTextAtomically`의 반환값을 그대로 확인하는 구조를 그대로 유지 |
| 템플릿 덮어쓰기 | `DetailViewModel.overwriteUserTemplateWithCurrentStyle`(2200~2278줄)은 `saved == false`일 때 `"템플릿을 덮어쓰지 못했어. 기존 템플릿은 그대로야."` 에러를 보여주는 구조를 그대로 유지 — 이제 이 실패 경로 자체가 Windows에서도 더 이상 불필요하게 발생하지 않음 |
| 미리보기 교체 | `savePreviewAtomically` 호출부(`overwriteUserTemplateWithCurrentStyle`, `saveCurrentStyleAsNewTemplate`)도 반환값 소비 방식 변경 없음 |
| 실패 시 기존 파일 유지 | **강화됨** — `AtomicFileReplace`는 대상 파일을 먼저 삭제하지 않으므로, 실패 시 기존 파일이 보존된다는 계약은 이전보다 더 명시적으로 보장된다(§4) |
| UI 동작 | 변경 없음 — `DetailScreen.kt`/`DetailViewModel.kt`의 다이얼로그·상태 처리 코드를 전혀 수정하지 않았으므로 대규모 실기기 테스트는 요구되지 않는다(작업지시서 12단계 판단과 일치) |

### 권장 최소 실기기 검증 (작업지시서 12단계 권고)
- 같은 엽서를 연속 두 번 저장(스티커/도장 편집 후 완료 저장을 두 번 연달아)
- 기존 템플릿을 같은 ID로 덮어쓰기(`overwriteUserTemplateWithCurrentStyle` 경로)
- 앱을 재진입해 두 번째 내용이 유지되는지 확인

이 셋은 이 조사·수정의 핵심 가설(대상 파일이 이미 있을 때의 덮어쓰기)을 실제 기기에서 직접 확인하는 최소 시나리오이며, **사용자 검증 대기** 항목으로 남긴다.

---

## 9. 발견했지만 수정하지 않은 사항

| # | 사항 | 위치 | 영향 | 별도 작업 필요 여부 |
|---|---|---|---|---|
| 1 | `PostcardTemplateStorage.savePreviewAtomically`(미리보기 PNG 저장)를 직접 검증하는 자동 테스트는 여전히 없음(Bitmap/Context 의존이라 이 프로젝트의 순수 JUnit 환경에서 애초에 실행 불가, 테스트 파일 상단 주석에 기존부터 명시) | `PostcardTemplateStorageTest.kt:15~19` | 낮음 — 코드 경로는 `saveTemplateAtomically`와 동일한 `AtomicFileReplace`를 쓰므로 회귀 위험은 낮으나, 직접 검증하는 자동 테스트는 이번에도 추가하지 못함(인프라 제약, 작업지시서 §9 "이미 충분한 테스트가 있다면 중복 테스트를 만들지 않는다"의 반대 상황이지만 인프라가 없어 추가 불가) | 아니오 — Robolectric 등 인프라 도입이 필요한 별도 범위 |
| 2 | `ConfirmedEditStateStorage`에 "다른 postcardId 파일은 영향 없음"을 직접 검증하는 테스트를 추가하지 않음 | `ConfirmedEditStateStorageTest.kt` | 낮음 — 각 저장 호출이 서로 다른 target 파일을 독립적으로 다루는 구조상 구조적으로 보장되며, `PostcardDraftStorageTest`에 동등한 시나리오를 이미 추가했으므로(§6) 중복 추가를 피함 | 아니오 |

치명적 결함(저장 계약 변경 필요, 예상하지 못한 플랫폼 문제)에 해당하는 사항은 발견되지 않아 작업을 중단하지 않았다.

---

## 10. 제3차 진행 여부

**사용자 실기기 검증 후 진행 권장.**

원인은 확정됐고(선행 조사 보고서) 수정도 최소 범위로 적용했지만, 이 환경에서 컴파일·테스트를 직접 실행하지 못했으므로(§7) 다음 두 가지가 확인되기 전까지는 제3차(UI 분리)를 시작하지 않는 것을 권장한다.

1. Android Studio에서 Kotlin 컴파일 성공 + 관련 테스트 통과 + 전체 테스트 실패 0건 확인
2. §8에서 권장한 최소 실기기 검증(연속 두 번 저장, 템플릿 같은 ID 덮어쓰기, 재진입 후 유지) 확인

두 확인이 끝나면 이번 저장 안정화 작업은 완료로 확정되며, 제3차는 이 저장 버그와 무관한 별개 작업이므로 곧바로 시작할 수 있다.

---

## 11. 진행 위치

```text
실제 작업일: 2026-08-06
대작업 진행 위치: 제2차 완료
후속 조사: 완료
후속 저장 안정화: 완료(자동검증은 사용자 검증 대기)
다음 시작 위치: 사용자 검증 후 제3차 1단계
```
