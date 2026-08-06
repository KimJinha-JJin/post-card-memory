# 저장 덮어쓰기 테스트 2건 실패 원인 조사 보고서

- 대작업: DetailScreen·DetailViewModel 구조 감사 및 단계적 안정화
- 종류: 제2차 후속 조사 (제1차~제10차 번호는 변경하지 않음)
- 실제 작업일: 2026-08-06
- 조사 방식: 코드 정독 + 원인 분리를 위한 최소 진단 테스트 1건 추가. 프로덕션 저장 로직은 수정하지 않음.

---

## 1. 최종 판정

**조사 완료**

두 실패는 서로 다른 클래스에서 발생했지만 **완전히 동일한 근본 원인**을 공유한다: `java.io.File.renameTo()`가 대상 파일이 이미 존재할 때 Windows에서 실패할 수 있다는, JDK가 공식 문서로 명시한 플랫폼 종속 동작이다. 코드 조사 결과 이 결론에 도달했으며, 원인의 "Windows에서 실패한다"는 부분은 사용자가 이미 재현한 실제 결과와 정확히 일치한다. "Android 실기기에서는 영향이 없다"는 부분은 POSIX `rename()` 표준 동작에 근거한 고신뢰 추정이며, 이 조사에서 Android 실기기로 직접 재현하지는 못했다 — 이 구분은 아래 각 절에서 명확히 표시한다.

---

## 2. Git 기준 상태

| 항목 | 값 |
|---|---|
| 브랜치 | `feature/photo-sticker` |
| 시작 HEAD | `2196dda` |
| 종료 HEAD | `2196dda` (이번 조사에서 프로덕션/기존 파일 커밋 없음, 신규 진단 테스트 1개는 아래 §변경 파일 참고) |
| origin/feature/photo-sticker | `2196dda` (fetch 후 일치) |
| 작업트리 | `?? .kotlin/`(기존 빌드 산출물), `?? app/src/test/.../FileRenameToOverwriteDiagnosticTest.kt`(신규 진단 테스트) — tracked 파일 변경 없음 |
| 변경 파일 | 신규 추가 1개: `app/src/test/java/com/postcardmemory/utils/FileRenameToOverwriteDiagnosticTest.kt`. 기존 파일 수정 0개 |

---

## 3. 실패 재현

사용자가 Android Studio에서 실행해 보고한 결과를 그대로 인용하며, 이 조사는 이를 실제 재현 결과로 채택한다(이 환경에는 `gradlew`/`gradle`/`java`가 전혀 없어 이번 조사에서 직접 재실행하지는 못했다 — §9 참고).

| 테스트 | 단독 실행 결과 | 실패 위치 | 실제값 / 기대값 |
|---|---|---|---|
| `ConfirmedEditStateStorageTest.writeTextAtomically_overwritesExistingConfirmedFile_onSuccess` | 5개 중 4 성공 / 1 실패 | `ConfirmedEditStateStorageTest.kt:54` | `assertTrue(saved)`에서 `saved == false` |
| `PostcardTemplateStorageTest.overwrite_sameId_replacesContent_otherTemplatesUntouched` | 7개 중 6 성공 / 1 실패 | `PostcardTemplateStorageTest.kt:91` | 기대값 "바뀐 이름", 실제값 "원래 이름" |

두 테스트 모두 순서 의존 없이 단독 실행에서도 동일하게 실패한다는 사용자 보고는 아래 §4·§5의 코드 흐름과 정확히 부합한다(테스트 간 상태 공유가 원인이 될 수 없는 구조 — `TemporaryFolder` Rule이 테스트마다 새 디렉터리를 만듦).

---

## 4. ConfirmedEditStateStorage 원인

### 저장 흐름 (`app/src/main/java/com/postcardmemory/utils/ConfirmedEditStateStorage.kt:15~38`)

```text
targetFile, content 전달
→ dir(부모 디렉터리) 존재 확인/생성 (19~23줄)
→ tempFile = "{target 파일명}.tmp" 경로 계산 (25줄)
→ FileOutputStream(tempFile)로 새 내용 쓰기 + flush (28~31줄)
→ tempFile.renameTo(targetFile) 결과를 그대로 함수 반환값으로 사용 (33줄)
→ (IOException 발생 시) tempFile.delete() 후 false 반환 (34~37줄)
```

### 실패 테스트의 정확한 실행 순서 (`ConfirmedEditStateStorageTest.kt:45~56`)

1. `stateFile`(`3.txt`) 생성, `"old-confirmed-content"` 기록 — **대상 파일이 이미 존재**하는 상태를 의도적으로 만듦
2. `writeTextAtomically(stateFile, "new-confirmed-content")` 호출
3. 내부적으로 `3.txt.tmp`를 새로 만들어 `"new-confirmed-content"`를 정상적으로 씀(이 단계는 실패 원인이 아님 — §7에서 근거 제시)
4. `tempFile.renameTo(targetFile)` 호출 — **이때 `targetFile`(`3.txt`)이 이미 존재**
5. `assertTrue(saved)` 실패 → `saved`가 `false`

### 실패 단계

**파일 rename 실패** (작업지시서 7단계 분류 기준). 정확히 33줄의 `tempFile.renameTo(targetFile)` 호출이 `false`를 반환하는 지점이다.

### 근거

- `java.io.File.renameTo()`의 공식 Javadoc은 다음과 같이 명시한다(원문 요지): *"이 메서드의 동작 중 상당 부분은 본질적으로 플랫폼에 종속적이다. rename 연산은 파일시스템 간 이동을 지원하지 못할 수 있고, 원자적이지 않을 수 있으며, **대상 경로에 이미 파일이 존재하면 성공하지 못할 수 있다.**"* 이 마지막 문장이 정확히 이번 실패와 일치한다.
- 실무적으로 이 문서화된 동작은 **Windows에서는 대상 파일이 이미 존재하면 `renameTo()`가 `false`를 반환**하고, **POSIX 계열(Linux/macOS/Android)에서는 `rename(2)` 시스템콜이 대상을 원자적으로 교체**하는 방식으로 갈린다는 것이 수십 년간 알려진 JDK 특성이다. `java.nio.file.Files.move()`가 덮어쓰기를 위해 별도로 `StandardCopyOption.REPLACE_EXISTING` 옵션을 요구하도록 설계된 것 자체가, `File.renameTo()`가 "대상 존재 시 성공을 보장하지 않는다"는 것을 JDK 설계 차원에서 인정하고 있다는 방증이다.
- 이번 조사에서 추가한 최소 진단 테스트(`FileRenameToOverwriteDiagnosticTest.kt`)가 이 메커니즘을 저장 클래스 로직과 완전히 분리해 순수 `File.renameTo()` 하나만으로 직접 검증한다(§9에서 실행 방법과 기대 결과 설명).
- 시나리오 4 테스트(`writeTextAtomically_preservesExistingConfirmedFile_whenWriteFails`, 58~79줄)는 대상 파일이 이미 존재하는 상황에서도 **통과**한다 — 이 테스트는 `FileOutputStream` 단계에서 의도적으로 `IOException`을 발생시켜 `renameTo` 자체에 도달하지 않기 때문이다. 즉 이 저장 로직에서 "대상 파일이 이미 있는 상태로 정상적으로 `renameTo`까지 도달하는" 경로를 검증하는 테스트는 실패한 테스트 단 하나뿐이며, 다른 시나리오들은 이 경로를 우회하고 있었다는 사실도 원인 특정을 뒷받침한다.

---

## 5. PostcardTemplateStorage 원인

### 저장 흐름 (`app/src/main/java/com/postcardmemory/utils/PostcardTemplateStorage.kt:49~75`)

```text
template(같은 id로 재저장) 전달
→ templatesDir 존재 확인/생성 (53~57줄)
→ target = templateFile(filesDir, template.id) 계산 (59줄)
→ tempFile = "{template.id}.tmp" 계산 (60줄)
→ FileOutputStream(tempFile)로 template.serialize() 결과 쓰기 + flush (63~68줄)
→ tempFile.renameTo(target) 결과를 그대로 함수 반환값으로 사용 (70줄)
→ (IOException 발생 시) tempFile.delete() 후 false 반환 (71~74줄)
```

`loadAllTemplates`(123~137줄)는 캐시 없이 매번 `templatesDir` 안의 `.txt` 파일을 실제로 나열하고 각 파일을 새로 읽어 파싱한다 — "이전 캐시를 읽는다"는 가설은 코드상 성립하지 않는다(파일 목록도, 파일 내용도 매 호출마다 디스크에서 새로 읽음).

### 실패 테스트의 정확한 실행 순서 (`PostcardTemplateStorageTest.kt:77~93`)

1. `original`(`id="fixed-id"`, name="원래 이름") 저장 — **이 시점엔 `fixed-id.txt`가 없으므로 `renameTo` 성공**
2. `other`(`id="other-id"`) 저장 — 역시 신규 파일이라 성공
3. `renamed`(`original.copy(id는 그대로 "fixed-id"`, name="바뀐 이름")) 저장 — **이번엔 `fixed-id.txt`가 이미 존재** → `tempFile.renameTo(target)`이 실패 가능 지점. 이 호출의 반환값은 테스트가 **직접 확인하지 않는다**(83·86줄 어디에서도 `saved` 변수를 받지 않음)
4. `loadAllTemplates` 재조회 → `fixed-id`의 이름을 확인 → rename이 실패했으므로 디스크의 `fixed-id.txt`는 여전히 1단계에서 쓰인 "원래 이름" 그대로
5. `assertEquals("바뀐 이름", ...)` 실패 → 실제값 "원래 이름"

### 실패 단계

**파일 rename 실패**(4단계 세부와 동일 메커니즘). §4와 동일하게 70줄의 `tempFile.renameTo(target)`이 `false`를 반환하는 지점이며, 이 테스트는 반환값을 직접 검사하지 않으므로 증상이 "최종 파일 읽기 결과가 옛 내용"이라는 형태로 한 단계 늦게 드러난다.

### 근거

- `memory 객체 교체`(85줄 `original.copy(name = "바뀐 이름", ...)`)와 `직렬화`(65줄 `template.serialize()`)는 이 흐름에서 새 이름을 정확히 포함한 상태로 temp 파일에 쓰이고, 이 단계는 §4와 마찬가지로 `renameTo` 이전에 정상 완료된다 — `saveThenLoad_roundTrips`(49~61줄)와 `save_leavesNoTempFileBehind`(63~74줄) 두 테스트가 "신규 파일" 시나리오에서 직렬화·저장·재로드 전 과정이 정상 동작함을 별도로 입증하므로, 문제가 직렬화나 메모리 교체에 있다고 볼 근거가 없다.
- `corruptedTemplateFile_isSkipped_othersStillLoad`(96~109줄), `deleteTemplate_removesDataAndPreview_otherTemplateFilesUntouched`(121~144줄) 등 "다른 템플릿은 영향받지 않아야 한다"는 요구사항 자체는 실패 테스트와 무관하게 이미 별도로 검증되고 있다 — "다른 템플릿 유지" 부분(92줄, `other-id`가 여전히 "다른 템플릿"인지)은 실제로 통과했을 것으로 판단된다(실패 지점은 91줄, `fixed-id`의 이름 쪽).

---

## 6. 공통 원인 여부

### 공유하는 구현
**없음** — `ConfirmedEditStateStorage.writeTextAtomically`(33줄)와 `PostcardTemplateStorage.saveTemplateAtomically`(70줄)는 공통 함수를 호출하지 않는다. 각자 독립적으로 "temp 파일에 쓰고 `File.renameTo()`로 교체"하는 패턴을 **직접 구현**하고 있다.

### 실제로는 같은 패턴이 4곳에 중복 구현됨
코드 전체에서 `tempFile.renameTo(...)` 패턴을 검색한 결과, 다음 4개 지점이 전부 동일한 패턴을 각자 손으로 구현하고 있다.

| 파일:줄 | 대상 |
|---|---|
| `ConfirmedEditStateStorage.kt:33` | 스티커/도장/낙서 확정 상태 파일 |
| `PostcardDraftStorage.kt:80` | 편집 초안(draft) 파일 |
| `PostcardTemplateStorage.kt:70` | 사용자 템플릿 본문 파일 |
| `PostcardTemplateStorage.kt:108` | 사용자 템플릿 미리보기 PNG |

`ConfirmedEditStateStorage.kt` 상단 주석은 "`PostcardDraftStorage`의 검증된 원자적 저장 패턴과 동일한 방식"이라고 명시하고 있어, 이 패턴이 `PostcardDraftStorage`를 원형으로 삼아 손으로 복제된 것임을 코드 자체가 증언한다.

### 동일 증상의 이유
두 실패는 **서로 다른 코드를 실행하지만 동일한 JDK 플랫폼 동작(§4 근거)에 부딪혀 우연이 아니라 필연적으로 같은 증상을 낸다.** 작업지시서 6단계의 분류로 답하면: "공통 구현이 있는가?"는 아니오이지만, "공통 원인이 있는가?"는 그렇다 — 공통 원인은 함수 재사용이 아니라 **동일한 안전하지 않은 패턴의 중복 구현**이다.

### PostcardDraftStorage는 왜 실패하지 않았는가
`PostcardDraftStorage.kt:80`도 동일한 `tempFile.renameTo(target)`를 쓰지만, `PostcardDraftStorageTest.kt`를 전수 확인한 결과 **"같은 `postcardId`로 두 번 저장해 기존 draft 파일을 덮어쓰는" 시나리오를 검증하는 테스트가 존재하지 않는다**(매 테스트가 서로 다른 `postcardId`만 사용). 즉 `PostcardDraftStorage`가 안전해서 통과한 것이 아니라, **이 취약한 경로를 실행하는 테스트 자체가 없어서 우연히 드러나지 않았을 뿐**이다. 이는 §8·§11에서 다시 다룬다.

---

## 7. Windows 환경 관련 분석

| 항목 | 확인 결과 |
|---|---|
| 관련 API | `java.io.File.renameTo()` (4곳 전부 동일) — `Files.move`, `StandardCopyOption.REPLACE_EXISTING`, 명시적 선삭제(delete-then-rename) 방식은 어디에도 쓰이지 않음 |
| 최종 파일이 이미 존재할 때 `renameTo`가 실패할 수 있는가 | 그렇다 — Javadoc에 명시된 플랫폼 종속 동작이며, Windows에서 이 케이스가 `false`를 반환한다는 것은 광범위하게 알려진 JDK 특성이다(§4 근거) |
| 실패 반환값을 확인하는가 | **확인한다** — 4곳 모두 `renameTo()`의 결과를 함수의 최종 반환값으로 그대로 사용한다(무시하지 않음). 문제는 반환값 확인 누락이 아니라, 실패 시 재시도(예: 선삭제 후 재시도)나 `Files.move` 폴백이 전혀 없다는 것이다 |
| 파일 스트림이 닫히기 전에 이동을 시도하는가 | 아니다 — `FileOutputStream(tempFile).use { ... }`의 `use` 블록이 끝나야 스트림이 닫히고, `renameTo` 호출은 그 `use` 블록 바깥(닫힌 뒤)에서 실행된다. 스트림 미종료로 인한 파일 잠금 문제는 아니다 |
| Windows에서 열려 있는 파일을 교체하려 하는가 | 이번 재현 시나리오에서는 아니다 — 테스트는 `stateFile.readText()`/`loadAllTemplates()` 호출 전에 명시적으로 파일을 닫으며, `target` 파일 자체를 열어둔 채로 rename을 시도하는 코드 경로는 확인되지 않았다. 다만 실제 안티바이러스나 인덱싱 서비스가 파일을 순간적으로 여는 경우는 이 조사 범위 밖의 변수로 남는다(추정) |
| 임시 파일과 최종 파일이 같은 디렉터리에 있는가 | 그렇다 — 4곳 전부 `File(dir, "...tmp")` 형태로 대상과 동일한 디렉터리에 temp 파일을 만든다. 같은 볼륨/디렉터리이므로 "다른 파일시스템 간 이동" 문제는 배제된다 |

### 재현 근거
이 절의 결론은 사용자가 실제로 재현한 두 실패(§3)와, 그 실패가 정확히 "대상 파일이 이미 존재하는 상태에서의 `renameTo` 호출"이라는 동일한 코드 지점에서 발생한다는 코드 추적(§4·§5) 결과에 근거한다. 추가로 이 조사에서 `FileRenameToOverwriteDiagnosticTest.kt`를 작성해 이 메커니즘을 저장 클래스 로직과 분리해 직접 검증할 수 있게 했다 — 이 진단 테스트를 사용자가 실행하면 콘솔에 `os.name`과 `renameTo()`의 실제 반환값이 출력되어, "Windows에서 대상 존재 시 `renameTo`가 실패한다"는 이번 판정을 자신의 환경에서 직접 눈으로 재확인할 수 있다(§9 실행 안내).

---

## 8. 앱 실사용 영향

| 질문 | 답 | 근거/확실성 |
|---|---|---|
| 실제 앱(Android)에서도 같은 증상이 발생할 수 있는가 | **가능성 낮음(고신뢰 추정, 이번 조사에서 실기기로 직접 확인하지는 못함)** | Android는 Linux 커널 기반이며 `File.renameTo()`가 POSIX `rename(2)`에 매핑된다. POSIX `rename()`은 대상이 이미 존재해도 원자적으로 교체하도록 표준이 보장한다. 이것이 "temp 파일에 쓰고 rename"이라는 패턴이 Android 개발에서 널리 권장되는 이유이기도 하다(Windows와 달리 Android/Linux에서는 이 패턴이 실제로 원자적이다) |
| 기존 confirmed 상태 갱신이 실패할 수 있는가 | Windows 개발 PC의 단위 테스트 환경에서는 실패가 확인됨. 실기기에서는 위 추정에 따라 가능성 낮음 | `persistStickerEditState`(`DetailViewModel.kt:932~982`)가 `ConfirmedEditStateStorage.writeTextAtomically`의 반환값을 그대로 `Boolean`으로 전파하며 확인함(973~975줄) |
| 기존 템플릿 수정이 저장되지 않을 수 있는가 | 위와 동일 | `overwriteUserTemplateWithCurrentStyle`(`DetailViewModel.kt:2200~2278`)이 정확히 "같은 templateId로 재저장"하는 코드 경로이며, `saved`가 `false`면 `TemplateManageState.Error("템플릿을 덮어쓰지 못했어. 기존 템플릿은 그대로야.")`로 명확히 실패를 알린다(2243~2258줄) |
| 앱이 저장 실패를 성공으로 오인하는가 | **아니다 — 확인된 사실** | 4곳 모두 `renameTo()`의 `Boolean` 반환값을 함수 반환값으로 그대로 쓰고, 호출부(`persistStickerEditState`, `persistDraftNow`, `overwriteUserTemplateWithCurrentStyle` 등)가 전부 이 값을 확인해 실패 시 명시적으로 에러 상태를 설정한다. "저장 성공으로 표시되지만 실제로는 갱신되지 않는" 치명적 패턴은 발견되지 않았다 |
| 기존 파일이 보존되는 안전한 실패인가 | **그렇다 — 확인된 사실** | `renameTo()`가 `false`를 반환해도 `target`(기존 파일)은 애초에 건드려지지 않는다 — 4곳 어디에도 rename 전에 기존 파일을 미리 삭제하는 코드가 없다. `_userTemplates`/`_photoStickers` 등 인메모리 상태도 저장 성공 확인 후에만 갱신되므로 화면-디스크 불일치도 발생하지 않는다 |
| 새 내용이 유실되는가 | 디스크에 반영되지는 않지만, 실패가 명확히 보고되므로 사용자가 재시도할 수 있다. 앱이 즉시 강제 종료되는 경우가 아니라면 인메모리 편집 상태(`_photoStickers` 등)에는 그대로 남아 있다 | `persistDraftNow`는 실패 시 `_draftSaveStatus.value = DraftSaveStatus.Failed`만 반영하고 `_photoStickers` 등은 되돌리지 않는다(834줄) |
| 임시 파일이 남는가 | 그렇다(이번 조사에서 확인) | `renameTo()`가 `false`를 반환하는 경로는 `IOException`이 아니므로 `catch` 블록의 `tempFile.delete()`가 실행되지 않는다 — 4곳 전부 이 경로에서 `.tmp` 파일이 정리되지 않는다. 단, temp 파일명이 고정이라 재시도해도 누적되지 않고 매번 덮어써진다(§12 기록) |
| 반복 저장 시 상태가 악화되는가 | 아니다 | 매 시도가 같은 지점에서 같은 방식으로 "안전하게" 실패할 뿐, 추가 손상이나 파일 개수 증가는 없다 |
| Windows JVM 단위 테스트 환경에만 국한된 문제인가 | **매우 높은 확률로 그렇다(추정)**, 단 아래 남은 위험도 함께 표기 | Android 실기기·에뮬레이터로 직접 재현/미재현을 확인하지 못했고, 파일시스템 종류(예: 일부 특수한 벤더 파일시스템, SAF 경유 경로 등)에 따른 예외 가능성은 이론상 완전히 배제하지 못한다 |

**데이터 손상 위험 없음, 저장 성공 오판 없음** — 이 두 가지가 이번 조사의 가장 중요한 결론이다. 실패하더라도 기존 데이터는 항상 보존되고, 실패는 항상 실패로 정직하게 보고된다.

---

## 9. 최종 원인 판정

**판정 C — 플랫폼별 구현 차이** (부분적으로 판정 A의 성격도 있어 아래에 함께 명시)

- Windows JVM 단위 테스트 환경에서는 `File.renameTo()`가 대상 파일 존재 시 실패하는 것이 원인이며, 이는 테스트 기대값의 오류가 아니라 **`ConfirmedEditStateStorage`와 `PostcardTemplateStorage`(그리고 동일 패턴을 쓰는 `PostcardDraftStorage`)가 플랫폼 독립적이지 않은 `File.renameTo()`만으로 원자적 덮어쓰기를 구현했다는 실제 코드 설계의 약점**이다.
- Android 실기기에서는 POSIX `rename()` 의미론에 따라 정상 동작할 가능성이 높다고 판단하지만, 이는 이번 조사에서 실기기로 직접 검증하지 못한 고신뢰 추정이다. 순수한 "판정 C"라면 "Android는 정상, 테스트 환경만 문제"로 끝나야 하지만, **이 코드가 문서화되지 않은 플랫폼 종속 동작에 의존하고 있다는 사실 자체는 Android에서도 잠재적 위험 요소로 남는다** — 예를 들어 특정 Android 파일시스템/스토리지 벤더 구현이나 향후 Android 버전의 파일 접근 방식 변화(Scoped Storage 확장 등)에서 동일한 취약점이 드러나지 않는다는 보장은 없다. 따라서 "테스트만 고치면 끝"이 아니라 "프로덕션 코드가 플랫폼에 안전하지 않은 API에 의존하고 있다"는 사실은 별도로 남긴다(§10).
- 테스트 기대값(`assertTrue(saved)`, `"바뀐 이름"`)은 **정상**이다 — 둘 다 제품이 실제로 보장해야 하는 동작(기존 확정 파일을 새 내용으로 덮어쓸 수 있어야 한다, 같은 ID로 재저장하면 교체돼야 한다)을 정확히 표현하고 있으며 임의로 완화해서는 안 된다.

---

## 10. 권장 수정 방향

**이번 조사에서는 수정하지 않았다.** 아래는 사용자 승인을 전제로 한 권장안이다.

### 수정 대상 (권장, 승인 필요)
- `ConfirmedEditStateStorage.kt:33`, `PostcardDraftStorage.kt:80`, `PostcardTemplateStorage.kt:70`, `PostcardTemplateStorage.kt:108` — 4곳의 `tempFile.renameTo(target)` 호출을, 대상이 이미 존재해도 플랫폼에 관계없이 원자적으로 교체되는 방식(예: `Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)`)으로 교체하는 것을 권장한다. 4곳이 동일 패턴을 손으로 복제하고 있으므로(§6), 수정 시 공통 헬퍼로 통합할지 4곳을 각각 고칠지는 별도 판단이 필요하나 — **이 조사는 원인 확정이 목적이므로 통합 여부까지 이번에 결정하지 않는다.**

### 수정하지 않을 영역
- 테스트 기대값(`writeTextAtomically_overwritesExistingConfirmedFile_onSuccess`, `overwrite_sameId_replacesContent_otherTemplatesUntouched`) — 정상이므로 완화하지 않는다
- 저장 실패 시 에러 처리·롤백·기존 파일 보존 로직 — 이미 안전하게 동작하므로(§8) 변경 불필요
- `PostcardDraftStorage`의 자동저장 흐름 자체(`persistDraftNow` 등) — 이번 원인과 같은 API를 쓰지만, 별도 결함이 발견된 것은 아니므로 renameTo 교체 이상의 변경은 불필요
- Room, Migration, DetailScreen/DetailViewModel 구조, 다이얼로그 UI — 이번 조사 범위 밖

### 필요한 테스트 (수정 시)
- 현재 실패 중인 2개 테스트가 수정 후 통과해야 함
- `PostcardDraftStorageTest`에 "같은 `postcardId`로 두 번 저장해 기존 draft를 덮어쓴다" 테스트가 **현재 없음** — §6에서 확인한 커버리지 공백이므로, 수정 시 이 테스트도 함께 추가할 것을 권장(그래야 자동저장 경로의 동일 취약점이 향후에도 조용히 통과하지 않는다)
- `PostcardTemplateStorage.savePreviewAtomically`(108줄)도 같은 패턴이므로 "기존 미리보기 PNG 덮어쓰기" 테스트 보강 권장
- 새 진단 테스트(`FileRenameToOverwriteDiagnosticTest`)는 수정 후에도 그대로 두면 향후 회귀 감지에 유용

### 예상 위험도
**낮음** — 수정 대상이 "임시 파일→최종 파일 교체" 한 줄씩 4곳으로 국한되고, 실패 시 기존 파일 보존이라는 안전 계약은 그대로 유지하면서 성공 케이스의 범위만 넓히는 변경이라 회귀 위험이 작다. 다만 Room이나 UI를 건드리지 않아도 **파일 I/O 핵심 경로**이므로, 실제 수정 시에는 제1차 감사가 지정한 "먼저 건드리면 안 되는 영역"(파일 교체·삭제) 원칙에 따라 별도 작업지시서와 충분한 테스트로 진행해야 한다.

---

## 11. 발견했지만 수정하지 않은 사항

| # | 사항 | 위치 | 영향 | 별도 작업 필요 여부 |
|---|---|---|---|---|
| 1 | `PostcardDraftStorage`가 동일한 `renameTo` 취약점을 가지고 있으나, 이를 검증하는 "같은 postcardId 재저장" 테스트가 없어 드러나지 않고 있음 | `PostcardDraftStorage.kt:80`, `PostcardDraftStorageTest.kt` 전체 | 자동저장(`persistDraftNow`)이 반복적으로 이 경로를 타므로, 4곳 중 실사용 빈도가 가장 높은 지점 | 예 — §10의 테스트 보강 권장에 포함 |
| 2 | `PostcardTemplateStorage.savePreviewAtomically`(108줄)도 동일 패턴이나 이번 두 실패 목록에는 없었음(전용 오버라이드 테스트가 Bitmap/Context 의존이라 이 프로젝트의 순수 JUnit 환경에서 애초에 실행되지 않음, `PostcardTemplateStorageTest.kt` 15~19줄 주석) | `PostcardTemplateStorage.kt:108` | 미리보기 PNG 교체 실패 가능성(템플릿 본문보다 영향 작음) | 아니오, §10에 기록만 |
| 3 | `renameTo()` 실패 시(IOException이 아닌 단순 false 반환 시) `.tmp` 임시 파일이 정리되지 않고 남음(4곳 공통) | 4곳 전부 | 낮음 — 디스크에 파일 1개가 남을 뿐이며 다음 저장 시도가 덮어씀, 누적되지 않음(§8) | 아니오, 수정 시 자연히 함께 해결 권장 |

치명적 결함(실제 사용자 데이터 손상, 저장 성공 오판, 반복적 유실 등 작업지시서 §13 기준)에 해당하는 사항은 발견되지 않아 조사를 중단하지 않았다.

---

## 12. 제3차 진행 여부

**추가 조사 필요 없음 — 수정 승인만 있으면 진행 가능. 단, 아래 선택지 중 사용자 판단이 필요하다.**

이번 조사로 원인은 확정됐으나(§9), 제3차(UI 분리)는 이 저장 버그와 직접 관련이 없는 작업이다. 두 갈래 진행 방식이 가능하다.

- **선택지 1**: 이번 발견을 별도 작업지시서로 먼저 수정(§10 권장안)한 뒤 제3차 진행
- **선택지 2**: 이 버그는 Windows 테스트 환경에 국한될 가능성이 높고(§8) 데이터 손상 위험이 없다고 판단해, 수정을 별도 백로그로 남기고 제3차(UI 분리)를 먼저 진행

이 조사 자체는 "판정만 하고 수정은 승인 전까지 하지 않는다"는 작업지시서 원칙에 따라 어느 쪽도 미리 선택하지 않는다.

---

## 13. 진행 위치

```text
실제 작업일: 2026-08-06
대작업 진행 위치: 제2차 완료
후속 조사: 저장 덮어쓰기 테스트 2건 원인 조사 완료
제3차 상태: 조사 결과 확정 전 시작 보류 (원인 확정됨, 수정 여부·순서는 §12 사용자 판단 대기)
```

---

## 부록: 자동검증 한계 (재확인)

이 환경에는 `gradlew`/`gradlew.bat`가 없고 시스템에 전역 `gradle`/`java`도 설치돼 있지 않아(제2차 보고서에서도 동일하게 확인됨), 이번 조사에서 추가한 `FileRenameToOverwriteDiagnosticTest`를 포함한 어떤 테스트도 이 환경에서 직접 실행하지 못했다. 이 보고서의 결론은 (1) 사용자가 이미 재현해 전달한 두 실패의 정확한 위치·값, (2) 관련 프로덕션·테스트 코드의 정독, (3) `File.renameTo()`에 대한 JDK 공식 문서 및 POSIX/Windows 플랫폼 차이라는 잘 확립된 사실에 근거한다. **사용자 검증 대기**: Android Studio에서 `FileRenameToOverwriteDiagnosticTest`를 실행해 콘솔에 출력되는 `renameTo()` 반환값과 `os.name`을 확인하면, 이번 판정을 최종적으로 확증할 수 있다.
