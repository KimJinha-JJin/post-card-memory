# HANDOFF — 79일차 후속: 테스트 안전망 품질 보강

확인일: 2026-09-20(같은 날, 79일차 본 작업 직후). 수동 표준 모드. "아직 80일차가 아니다"라고 명시한 79일차 후속 작업지시서에 따라, 79일차에 새로 추가한 instrumentation 4건의 assertion/실행 가능성을 감사하고, migration fixture를 강화했다. **새 기능·UI·navigation 없음. Room schema/DB version/DAO 계약 변경 없음. 새 dependency 없음. production 코드 변경 없음**(오늘도 androidTest/test 파일만 수정).

## 시작 Git 실측 (79일차 본 작업 보고값을 다시 확인)

```yaml
branch: feature/photo-sticker
HEAD: 6b9a7da (동일, 오늘도 커밋 없었음)
ahead/behind: 0/0
staged: 없음
tracked: 78일차부터 이어진 31파일 + docs/ai/HANDOFF.md(79일차 기록) — 오늘 손대지 않음
untracked: .codex-config.candidate.toml, .kotlin/(기존, 보호) + PostcardBackgroundColorSaveRaceTest.kt, PostcardFullMigrationChainTest.kt(79일차 본 작업에서 신규)
```

79일차 본 작업 보고값과 정확히 일치했다.

## §2 숫자 정정 — replica는 25건 그대로다

79일차 HANDOFF에 "나머지 22건"이라는 표현이 있었는데 산수가 틀렸다(`BackgroundColorSaveRaceTest`에서 겹치는 건 2건뿐이라 나머지는 9건, 총 23건이어야 함). **더 중요한 사실**: replica는 삭제되거나 대체된 적이 없다 — 25건 전부 오늘도 그대로 있고, `PostcardBackgroundColorSaveRaceTest`의 2건은 **추가 production coverage**일 뿐이다. 정확한 표현으로 위 79일차 섹션의 해당 문단을 고쳤다: "나머지 23건(`BackgroundColorSaveRaceTest` 나머지 9, `DetailScreenExitSaveGuaranteeTest` 8, `DetailScreenExitSaveLossTest` 3, `StyleSaveRaceTest` 3)".

## §4 신규 instrumentation 4건 코드 감사 — 진짜 문제 하나 발견

`PostcardBackgroundColorSaveRaceTest.kt`, `PostcardFullMigrationChainTest.kt`를 감사 체크리스트(이름=assertion 일치, production 실제 호출, Fake가 핵심 로직을 대신하지 않는지, cleanup 보장, wall-clock 의존 여부, assertion 강도)로 다시 읽었다.

### 발견: `failedColorSave_doesNotRollbackNewerColor`는 실제로 "실패"가 아니라 "취소" 경로를 탄다

production `DetailViewModel.updateBackgroundColor`를 다시 읽으니(§6), 매 호출이 시작할 때

```kotlin
backgroundColorSaveJob?.cancel()
backgroundColorSaveJob = viewModelScope.launch { ... }
```

를 실행한다 — **이전 저장 Job 자체를 취소**한다. 내가 처음 쓴 테스트는 "오래된 저장이 `release.await()`에서 멈춰 있다가, 풀려나면 IOException을 던진다"는 구조였는데, 실제로는 두 번째 `updateBackgroundColor` 호출이 실행되는 순간 첫 번째 Job이 `release.complete()`를 기다리다 **그 자리에서 취소**돼 버린다 — 내가 주입하려던 IOException에 도달하기 전에 `catch (CancellationException) { throw exception }` 경로로 빠진다. 즉 테스트 이름과 실제로 실행되는 코드 경로가 어긋나 있었다(§4 체크리스트 첫 항목이 정확히 겨냥한 문제).

**비교로 확인한 사실**: 같은 종류의 `StyleSaveRaceTest`(JVM replica) 쪽 `FakeViewModel.saveFieldA`는 이미 `fieldASaveJob?.cancel()`을 포함하고 있어 production과 일치하고, 그래서 그 파일은 "취소는 실패가 아니다" 시나리오(`cancelledIndividualSave_doesNotRecordError_doesNotRollbackNewerValue`)를 따로 두고 있다. 반면 `BackgroundColorSaveRaceTest`(JVM replica)의 `FakeViewModel.updateBackgroundColor`에는 이 취소 로직이 없다 — 그래서 그 replica의 `failedColorSave_doesNotRollbackNewerColor`/`staleColorSave_afterCommit_doesNotRewriteNewerColor`는 "두 저장이 서로 독립적으로 동시에 진행되다 하나가 실패한다"는, production에서 두 번의 실제 `updateBackgroundColor` 호출만으로는 재현되지 않는 구조를 가정하고 있다. **이건 반드시 틀린 테스트는 아니다** — Mutex+재읽기 메커니즘 자체가 (취소 로직이 나중에 제거되더라도) 여전히 옳게 동작하는지를 보는, 더 방어적인 "메커니즘 단위" 테스트로 볼 수 있다. 다만 **instrumentation은 실제로 도달 가능한 호출 경로만 주장해야 하므로** 이 불일치를 instrumentation 쪽에서 그대로 반복하지 않았다.

### 조치: instrumentation 테스트를 실제 경로에 맞게 다시 씀 (production은 건드리지 않음)

§6 지침대로 "production이 테스트 가정과 다르면 테스트를 현실에 맞춰 고친다"를 따랐다 — production 동작은 올바르고(취소가 stale write를 막는 것 자체가 안전 장치), 잘못된 건 내 테스트의 가정이었다.

- `failedColorSave_doesNotRollbackNewerColor` → **`staleInFlightColorSave_isCancelledAndNeverReachesTheDatabase`**로 다시 작성. 이름 그대로: 아직 커밋되지 않은 오래된 저장이 새 저장으로 인해 취소되고, **실제로 DB에 쓰인 색 목록**(`actuallyWrittenColors`)이 최신 색 하나뿐임을 직접 확인한다 — "예외 없이 끝났다"가 아니라 진짜 결과값을 본다(§5 요구사항).
- `staleNeverReleases`는 절대 `complete()`되지 않는 Deferred다 — 만약 취소가 실제로 안 된다면 production의 `PENDING_STYLE_SAVE_TIMEOUT_MS`(2초, `DetailViewModel.kt:72`) 안에 `awaitPendingStyleSaves()`가 타임아웃하고 이후 assertion이 실패로 드러난다. 고정 `delay()`로 경합을 만들지 않았다(§5).
- `afterFailure_nextColorSaveSucceedsNormally`는 두 호출을 **순차적으로**(첫 호출이 `awaitPendingStyleSaves()`로 완전히 끝난 뒤 두 번째 호출) 실행해서 취소 이슈가 없다 — 감사 결과 그대로 둬도 되는 테스트로 확인했다.
- 두 테스트 모두 `db.close()`/`vm.viewModelScope.cancel()`을 finally에서 보장하고, in-memory Room이라 임시 파일이 남지 않는다.
- **production bug는 발견하지 못했다** — production의 cancel-on-relaunch 자체는 의도된 동작이고 올바르게 안전하다. §7의 수정 조건("실제 잘못된 동작이 명확")에 해당하는 사례가 없어 production은 전혀 건드리지 않았다.

### migration 테스트(§8~11) 강화

- **v2→v3**: 기존엔 행 1개만 확인했다. 이제 **행 2개**(하나는 `location` NULL)를 넣고, 표 재생성 후 `getAllPostcards().first().size == 2`(row count 보존), 그리고 최종 새 행 insert 후 `size == 3`까지 확인한다. PK(`id`)·핵심 텍스트/시간/위치·backfill 값(`4294966263`/`null`) 검증은 기존 그대로 유지.
- **v14→v15**: 기존엔 폐기값(`'AIRY'`) 정규화 하나만 봤다. 이제 같은 fixture에 **현재도 유효한 값(`'POLAROID'`, id=2)을 함께 심어서**, `UPDATE ... WHERE layoutStyle NOT IN (...)`가 폐기값만 정확히 골라내고 유효값은 그대로 두는지 함께 확인한다 — 조건이 너무 넓어지는 회귀(유효값까지 덮어씀)와 너무 좁아지는 회귀(폐기값을 놓침) 둘 다 이 한 테스트가 잡는다.
- fixture 근거는 여전히 초기 커밋 원본 소스 + production Migration 객체 자체뿐, 상상한 값 없음(§12).
- 두 테스트 모두 최종적으로 `MIGRATION_18_19`까지 실제로 열어 v19 도달과 최신 DAO read를 확인한다(§11) — v2→v3 테스트는 추가로 최신 DAO write(`updatePostcardBackMessage`)와 DB 재오픈 후 재확인까지 포함.

### 컴파일/실행

```yaml
assembleDebugAndroidTest: BUILD SUCCESSFUL (수정 후 재검증)
실제 실행: 미실행(아래 emulator 조사 참고)
```

## §13~14 emulator 환경 한 단계 더 확인 — 여전히 STOP

```yaml
system-images 디렉터리: 없음
cmdline-tools(avdmanager/sdkmanager): 설치 안 됨
설치된 것: platform-tools, platforms, build-tools, emulator 바이너리(이미지 없이는 실행 불가)
```

AVD를 만들려면 system image 다운로드 + cmdline-tools 설치가 먼저 필요하다 — §14의 STOP 조건("system image 다운로드 필요")에 정확히 해당해 오늘도 emulator 생성을 진행하지 않았다. instrumentation 10건은 오늘도 **미실행**.

## §18 replica ↔ production coverage mapping

| replica 파일 | 총 건수 | production 직접 연결 coverage |
|---|---:|---|
| `BackgroundColorSaveRaceTest` | 11 | 2건(`PostcardBackgroundColorSaveRaceTest`) — stale-저장 취소, 실패 후 재저장 |
| `StyleSaveRaceTest` | 3 | 없음 |
| `DetailScreenExitSaveGuaranteeTest` | 8 | 없음(`PostcardBackSaveTest`가 다른 필드=back message/postscript로 exit-save 보장의 일부를 이미 실증하지만, 이 replica가 겨냥하는 슬라이더류 필드 자체는 아님) |
| `DetailScreenExitSaveLossTest` | 3 | 없음(위와 동일) |

## §19 다음 production 전환 후보 — 이번엔 추가하지 않음

우선순위 1위 `StyleSaveRace`(개별 슬라이더 저장, 예: `saveStampPhotoScale`)는 `PostcardBackgroundColorSaveRaceTest`와 거의 같은 fixture(같은 `styleWriteMutex`, 같은 gated-DAO 패턴)로 재사용 가능해 보였지만, §20("emulator 실행도 못 한 상태에서 테스트를 더 만들지 않는다")에 따라 **오늘은 추가하지 않았다** — 이미 추가한 4건도 아직 실행 검증이 안 된 상태라 안전망을 더 쌓기보다 지금 있는 것부터 확실히 하는 쪽을 택했다.

## §21 낮은 비용 약점 — 1건 강화

- `VisitRecordTest.parseVisitRecord_toleratesTrailingNewline`의 `assertNotNull(parseVisitRecord(...))`를 `assertEquals(record, parseVisitRecord(...))`로 강화 — null 여부만이 아니라 날짜/누적일/연속일 값 자체가 훼손 없이 파싱되는지 확인한다. 관련 JVM test로 재검증(통과).
- `ExitSaveTimeoutTest`/`DayBoundaryTest`의 wall-clock 의존은 79일차 본 작업에서 이미 조사해 여유 폭이 충분(8배 이상, 또는 하한만 검사)하다고 판단했고 오늘 다시 봐도 같은 결론 — 수정하지 않았다.
- "예외 없음만 확인하는 테스트 8건"은 78일차부터 이월된 카탈로그 항목인데 오늘 위치를 다시 특정하지 못했다 — 새로 찾는 작업은 후속으로 남긴다.

## 79일차 후속 최종 자동 검증

```yaml
JVM unit test: 750 tests / 81 files / failures 0 / errors 0 / skipped 0
assembleDebug: BUILD SUCCESSFUL
assembleDebugAndroidTest: BUILD SUCCESSFUL (수정된 4건 포함 10건 컴파일 재확인)
connectedDebugAndroidTest: 미실행(emulator 없음, §13~14)
git diff --check: 통과(기존 LF→CRLF 안내만)
```

## 사용자 확인 / Git

- production 변경 0건, 사용자 눈에 보이는 변화 없음 — 실기기 QA 불필요.
- **미검증**: 오늘 고친 instrumentation 4건이 실제로(특히 취소 타이밍) 통과하는지는 여전히 에뮬레이터가 있어야 확인 가능. 새로 강화한 migration 2건의 row-count/유효값-보존 assertion도 마찬가지.
- commit: 미실행·미승인
- push: 미실행·미승인
- 종료 HEAD/upstream: `6b9a7da`, ahead/behind `0/0`(오늘도 커밋 없음)
- 78일차 tracked 31파일 + 기존 untracked 2개는 오늘도 보존.

## 후속 후보 (승인된 작업 아님)

1. emulator 준비 후 instrumentation 10건 전체 실행 — 특히 오늘 고친 `staleInFlightColorSave_isCancelledAndNeverReachesTheDatabase`가 실제로 2초 타임아웃 안에 통과하는지가 최우선 확인 대상.
2. `BackgroundColorSaveRaceTest`(JVM replica)에 `fieldASaveJob?.cancel()`과 대응하는 취소 로직이 빠져 있다는 점을 문서화하거나, `StyleSaveRaceTest`처럼 취소 시나리오를 별도로 추가하는 안 검토(오늘은 손대지 않음 — replica 자체 수정은 이번 감사 범위 밖).
3. §19에서 미룬 `StyleSaveRace` production coverage 추가(instrumentation 10건 실제 실행 확인 후).
4. "예외 없음만 확인하는 테스트 8건" 재식별.

---

# HANDOFF — 79일차 테스트 실효성 보강 (replica 우선순위 전환 + migration chain)

확인일: 2026-09-20. 수동 표준 모드. 사용자 제공 "79일차 테스트 실효성 보강 및 안정성 2차" 작업지시서에 따라 DetailViewModel replica 25건 분류, Room migration 1→18 공백 조사, instrumentation 6건 실행 가능성 확인을 수행했어. **새 기능·새 UI·navigation 없음. Room schema/DB version/DAO 계약 변경 없음. 새 dependency 없음.** production 코드는 전혀 건드리지 않았고, 오늘 변경은 전부 androidTest 신규 테스트 파일 2개 추가다.

## 시작 Git 상태 (실측)

```yaml
branch: feature/photo-sticker
HEAD: 6b9a7da0955362d08833388b46c405db511ecb1e
upstream: origin/feature/photo-sticker
ahead/behind: 0/0
staged: 없음
working tree: 78일차부터 이어진 tracked 변경 31파일(위 78일차 섹션 그대로, 오늘 손대지 않음) + 기존 untracked .codex-config.candidate.toml, .kotlin/
```

78일차 마지막 참고값과 정확히 일치했다. 기존 tracked/untracked는 읽기만 하고 수정하지 않았다.

## 기준선 (작업 전 재확인)

```yaml
JVM unit test: 750 tests / 81 files / failures 0 / errors 0 / skipped 0
assembleDebug: BUILD SUCCESSFUL
assembleDebugAndroidTest: BUILD SUCCESSFUL
```

78일차 HANDOFF 값과 동일 — 신규 변경과 섞일 기존 실패는 없었다.

## emulator / 실기기 확인

```yaml
adb devices: 빈 목록(연결된 기기 없음)
emulator -list-avds: AVD 없음(에뮬레이터 자체가 구성돼 있지 않음)
```

§17 기준대로 새 AVD 생성·이미지 다운로드는 오늘 범위 밖으로 두고, instrumentation 6건 + 오늘 추가한 4건 모두 **미실행**으로 기록한다(연결된 기기는 없었고, 있었어도 실사용 기기 조작은 금지). 대체 검증은 `assembleDebugAndroidTest` 컴파일 성공.

## DetailViewModel replica 25건 분류

4개 파일(`BackgroundColorSaveRaceTest` 11 / `DetailScreenExitSaveGuaranteeTest` 8 / `DetailScreenExitSaveLossTest` 3 / `StyleSaveRaceTest` 3) 전수를 읽었다. **25건 전부 자기 KDoc이 이미 정확하게 명시하고 있었다** — `DetailViewModel`은 `android.content.Context`(`@ApplicationContext` Hilt 주입), `android.graphics.Bitmap`, `android.util.Log`를 직접 쓰고, `app/build.gradle.kts`에 `testOptions.unitTests.isReturnDefaultValues`가 설정돼 있지 않아 순수 JVM `src/test`에서 인스턴스화하면 `Log.d` 등에서 즉시 "not mocked"로 실패한다. 즉 **25건 모두 분류 C**(JVM 불가, instrumentation에서만 production 직접 호출 가능)이고, A/B로 "바로 전환 가능"한 항목은 0건이었다 — 원인이 각 파일 이름이 아니라 `DetailViewModel` 생성자 자체의 Context 의존이라 전부 같은 결론이다.

다만 `PostcardBackSaveTest`(기존 instrumentation)가 이미 증명하듯 `DetailViewModel(repository, deletionManager, context, scope)`를 **androidTest에서는** Hilt 없이 직접 생성할 수 있고, 실제 Room in-memory DB + `object : PostcardDao by dao { override suspend fun ... }` 델리게이트로 개별 DAO 메서드만 실패/지연시키는 패턴이 이미 이 저장소에 정착돼 있다. §6 우선순위(최신 저장 우선 > 화면 이탈 생존 > 실패 rollback > background color race > style race)에 따라 **가장 위험한 항목 하나(배경색 저장 경합)만** 오늘 이 패턴으로 실제 production 테스트를 추가했다.

### 추가: `PostcardBackgroundColorSaveRaceTest`(androidTest, 신규 2건)

- `failedColorSave_doesNotRollbackNewerColor`: 실제 `DetailViewModel.updateBackgroundColor`를 실제 Room + 첫 호출만 실패하는 gated DAO로 두 번 연속 호출해, 늦게 실패한 저장이 그사이 커밋된 최신 색을 되돌리지 않는지 확인.
- `afterFailure_nextColorSaveSucceedsNormally`: 실패 후 다음 정상 저장이 그대로 커밋되는지 확인.
- `BackgroundColorSaveRaceTest`(JVM replica) 11건 중 이 두 시나리오와 겹치는 부분의 production 연결 대응이다. `saveBackgroundImagePath`가 겨냥하는 경로 컬럼 경합(replica 3건)은 **오늘 그 값을 쓰는 실제 UI 호출자가 없어**(replica 자체 주석이 이미 명시) 실제 호출 경로를 꾸며내지 않고 미전환으로 남겼다.
- **실행 결과**: 미실행(에뮬레이터 없음). `assembleDebugAndroidTest`로 컴파일만 검증했다 — 타이밍 로직이 실제로 통과하는지는 에뮬레이터가 생기기 전까지 미확인이다.
- **replica는 삭제하지 않았다.** 새 instrumentation 테스트가 실제로 통과하는 것을 확인하지 못한 상태에서 유일한 안전망(JVM replica)을 줄이면 순간적으로 안전망이 비는 위험이 있다 — §8 "가능하면 새 테스트의 실효성을 실증한다" 조건(mutation으로 실패 재현)도 실행 없이는 만족할 수 없었다.
- **[79일차 후속 정정] replica는 25건 그대로 유지했다(삭제 0, 대체 제거 0).** 위 2건은 기존 replica를 지우거나 대체한 게 아니라 **별도 production coverage를 추가**한 것이다. 나머지 23건(`BackgroundColorSaveRaceTest` 나머지 9, `DetailScreenExitSaveGuaranteeTest` 8, `DetailScreenExitSaveLossTest` 3, `StyleSaveRaceTest` 3)은 오늘 손대지 않고 분류 C로 남겼다 — 각각의 KDoc이 이미 "Context/Room/Hilt 제약, StyleSaveRaceTest와 동일" 계열의 정확한 설명을 갖고 있어 문서 수정도 하지 않았다.

## Room migration 1→18 공백 조사

`app/schemas/`에는 `18.json`/`19.json`만 있다(`exportSchema=true`는 2026-08-31 "Add migration baseline" 커밋에서 처음 켜졌다 — 그 전 버전은 애초에 schema 자산이 생성된 적이 없다, 삭제된 게 아니다). v1~v17은 상상으로 채우지 않고 **초기 커밋(`8d95bc35`, 2026-06-27)의 `Postcard.kt`/`PostcardDatabase.kt` 원본**에서 v1 schema(`id INTEGER PK AUTOINCREMENT, imagePath TEXT NOT NULL, title TEXT NOT NULL, capturedAt INTEGER NOT NULL, location TEXT`)를 그대로 확인했다. 이후 v1→v18 사이 19개 Migration을 전부 읽고 분류했다:

```yaml
1→2: ADD COLUMN(message). 저위험.
2→3: 표 재생성(CREATE postcards_new → INSERT ... SELECT → DROP → RENAME) +
     backgroundColorArgb/backgroundImagePath 하드코딩 backfill(4294966263 / NULL).
     이번 조사에서 찾은 유일한 구조 변경 + 값 backfill 구간 → 최우선 위험.
3→4 ~ 13→14: 전부 ADD COLUMN + DEFAULT. 저위험(기존 행에도 소급 적용).
14→15: UPDATE로 layoutStyle 폐기값(예: 옛 AIRY/MAGAZINE)을 'STAMP'로 정규화.
       열 추가가 아니라 기존 값을 실제로 바꾸는 유일한 구간 → 두 번째 위험.
15→16 ~ 18→19: ADD COLUMN + DEFAULT, KDoc에 "기존 행 소급 적용" 이미 명시. 저위험.
```

### 추가: `PostcardFullMigrationChainTest`(androidTest, 신규 2건)

새 fixture나 새 dependency 없이(room-testing 미사용, `androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory` + `SupportSQLiteOpenHelper`는 이미 Room runtime의 전이 의존성) production `MIGRATION_x_y` 객체 자체를 fixture로 재사용했다.

- `migrateFromVersion1PreservesLegacyDataThroughFullChain`: 손으로 만든 v1 raw table에 옛 행 1건을 넣고 `Room.databaseBuilder(...).addMigrations(1_2 ... 18_19)`로 전체 체인을 실행 → `imagePath`/`title`/`capturedAt`/`location` 원본 보존, 2→3의 하드코딩 backfill값(`backgroundColorArgb=4294966263`, `backgroundImagePath=null`) 확인, 3→18 사이 모든 ADD COLUMN 기본값이 옛 행에 소급 적용됨을 확인, 이후 최신 DAO write(`updatePostcardBackMessage`)와 새 행 insert, DB 재오픈 후 재확인까지 검증.
- `migration14to15NormalizesLegacyLayoutStyleValue`: `MIGRATION_1_2`~`MIGRATION_13_14`를 실제 코드로 순차 실행해 v14 상태를 만든 뒤 폐기값 `'AIRY'`를 직접 심고, `MIGRATION_14_15`부터 열어 `layoutStyle`이 `'STAMP'`로 정규화되는지 확인.
- **실행 결과**: 미실행(에뮬레이터 없음). `assembleDebugAndroidTest` 컴파일 성공으로 API 사용(`FrameworkSQLiteOpenHelperFactory`, `SupportSQLiteOpenHelper.Configuration/Callback`, 각 `MIGRATION_x_y.migrate()` 직접 호출)까지는 타입 수준에서 검증했지만, 마이그레이션 SQL이 실제 SQLite에서 그대로 실행되는지는 미확인이다.
- **남은 공백**: v2 재생성 직후(`postcards_new`) 상태 자체를 별도로 들여다보는 테스트는 아니다 — 최종 v19 결과값만 확인한다. 더 세분화된 중간 상태 검증은 후속 후보로 남긴다.

## Instrumentation 현황 (갱신)

```yaml
기존: 6 tests / 3 files (PostcardBackMigrationTest 1, PostcardBackRenderingTest 3, PostcardBackSaveTest 2)
오늘 추가: 4 tests / 2 files (PostcardBackgroundColorSaveRaceTest 2, PostcardFullMigrationChainTest 2)
최종: 10 tests / 5 files
실제 실행: 미실행(에뮬레이터 없음, adb devices 빈 목록)
대체 검증: assembleDebugAndroidTest 성공(10건 전부 컴파일 확인)
```

## lifecycle/navigation, Robolectric, 구조 테스트, flaky 후보

- **lifecycle/navigation 공백**: 78일차 조사와 달라진 사실 없음(Compose BOM/Robolectric 구성 미변경) — 78일차 섹션의 판정을 그대로 유지한다. 오늘 별도 재조사하지 않았다.
- **Robolectric**: 오늘도 도입하지 않음(§20, 새 dependency는 STOP 대상). 필요성 판단도 78일차와 동일.
- **구조 테스트 148건**: 오늘 손대지 않음(§21 "메인 아님", 시간을 replica/migration에 우선 배분). 분류·개수 변화 없음.
- **flaky 후보 조사**: `ExitSaveTimeoutTest`(`saveSlowerThanTheTimeout...`, `saveSurvivesEvenWhen...`)는 timeout 50ms vs 실제 delay 400ms(8배 여유), `oldShape...`는 withTimeoutOrNull 50ms vs delay 400ms(8배 여유) — 실제 wall-clock 기반이지만 여유 폭이 넓어 낮은 위험으로 판단해 수정하지 않았다. `DayBoundaryTest.ticksKeepComingOnceEachBoundary`의 `elapsed >= 90L`은 하한만 검사해 느린 환경일수록 오히려 통과하기 쉬운 구조라 안전하다고 판단, 수정하지 않았다.
- **약한 assertion 후보**: 오늘 조사하지 않음(§25 최저 우선순위, 시간 배분상 제외).

## 79일차 최종 자동 검증

```yaml
JVM unit test: 750 tests / 81 files / failures 0 / errors 0 / skipped 0 (변화 없음 — 오늘 변경은 androidTest만)
assembleDebug: BUILD SUCCESSFUL
assembleDebugAndroidTest: BUILD SUCCESSFUL (신규 4건 포함 10건 컴파일 확인)
instrumentation 실제 실행: 미실행(에뮬레이터 없음)
git diff --check: 통과(오류 없음, 기존 LF→CRLF 안내만)
```

## 사용자 확인 / Git

- **자동 검증으로 충분(오늘 변경분)**: 오늘 production 코드 변경이 없고, 추가한 androidTest 4건은 컴파일 검증만 가능한 상태로 정직하게 "미실행"으로 남겼다. 사용자 눈에 보이는 앱 동작 변화가 없어 실기기 QA를 만들지 않았다.
- **미검증·위험**: 새로 추가한 4개 instrumentation 테스트가 실제로 통과하는지는 에뮬레이터가 준비되기 전까지 확인할 수 없다 — 컴파일 성공은 타입 수준 검증일 뿐 타이밍/SQL 실행 검증이 아니다.
- commit: 미실행·미승인
- push: 미실행·미승인
- 종료 기준 HEAD/upstream: `6b9a7da`, ahead/behind `0/0`(오늘 커밋 없음이라 78일차와 동일)
- 78일차부터 이어진 tracked 변경 31파일과 기존 untracked `.codex-config.candidate.toml`, `.kotlin/`는 오늘도 그대로 보존했다.

## 후속 후보 (승인된 작업 아님)

1. 안전한 emulator 구성 후 오늘 추가한 4건 포함 instrumentation 10건 전체 실제 실행.
2. `DetailScreenExitSaveGuaranteeTest`(8)·`DetailScreenExitSaveLossTest`(3)·`StyleSaveRaceTest`(3)·`BackgroundColorSaveRaceTest` 나머지 9건도 같은 gated-DAO instrumentation 패턴으로 production 연결 테스트 추가 검토(실행 검증 가능해진 뒤).
3. `saveBackgroundImagePath` 경로 컬럼 경합은 실제 UI 호출자가 생기기 전까지 production instrumentation 전환 보류.
4. Migration v2→v3 표 재생성 직후의 중간 상태(`postcards_new`) 자체를 별도로 검증하는 세분화 테스트.
5. (78일차 이월) `createComposeRule` v2 전환, Robolectric 도입 여부 결정, lifecycle/navigation 41건 구조 테스트 실제 행동 테스트 전환.

---

# HANDOFF — 78일차 패코 잔여 안정성 마감

확인일: 2026-09-19. 수동 표준 모드. 직전 78일차 미커밋 working tree를 그대로 이어받아 방문 기록 읽기 실패 표시, instrumentation 현황, 구조 테스트 분류, UI 테스트 기반, 파일 소유권 판정 중복, 휴면 코드 문서를 현재 저장소 기준으로 재검증했어. 새 기능·새 UI·navigation·Room/Migration·dependency 변경은 없다.

## 시작 Git 상태 (실측)

```yaml
branch: feature/photo-sticker
HEAD: 6b9a7da0955362d08833388b46c405db511ecb1e
upstream: origin/feature/photo-sticker
ahead/behind: 0/0
staged: 없음
working tree: 직전 78일차 tracked 변경 20파일 + 기존 untracked .claude/, .codex-config.candidate.toml, .kotlin/
```

기존 tracked 변경은 사용자 작업으로 보존하고 그 위에서 이어갔다. 기존 untracked는 읽거나 수정·삭제·stage하지 않았다.

## 방문 기록 read 실패 표시

- **기존 동작**: `VisitRecordStorage.readStoredRecord()`는 파일 없음/읽기 실패/손상을 구분해 읽기 실패 때 디스크를 보존했지만, `recordTodayVisit()` 반환값은 `recordVisit(null, today)`가 만든 총 1회의 임시 기록이었다. `MainActivity`가 이 값을 Intro와 Gallery 달력으로 전달해, 같은 실행에서 `1번째 방문`과 `오늘까지 1번 만났어요~!`가 거짓으로 보일 수 있었다.
- **수정**: `TodayVisit.record`를 nullable로 바꾸고 `StoredVisitRecord.Unreadable`이면 `record=null`, `isFirstVisitToday=false`를 반환한다. 기존 UI 두 곳은 이미 null일 때 숫자·소인을 비우므로 별도 UI 문법을 만들지 않았다. 파일 없음과 읽었지만 손상된 파일의 기존 “1회로 새로 시작” 정책은 유지했다.
- **복구**: 다음 프로세스 실행에서 정상 read가 되면 보존된 누적값을 이어 방문을 기록한다. 같은 프로세스에서 재시도하지 않는 기존 “프로세스당 한 번” 정책도 유지했다.
- **테스트**: 실제 파일에 누적 2회를 저장한 뒤 `IOException`을 주입해 `record == null`, 기존 파일 내용 보존, 다음 정상 read 때 총 3회/연속 3회 복구를 검증했다. Intro/Gallery 소비 경로의 nullable 전달도 compile로 확인했다.

## Instrumentation 현황

```yaml
시작: 7 tests / 4 files
삭제: 1 test / 1 file  # Android Studio 기본 package-name ExampleInstrumentedTest
최종: 6 tests / 3 files
runner: androidx.test.runner.AndroidJUnitRunner
실행: 미실행
대체 검증: :app:assembleDebugAndroidTest 성공
```

| 파일 | 수 | 보호 대상 | 필요한 환경 / 판정 |
|---|---:|---|---|
| `PostcardBackMigrationTest` | 1 | 실제 v18 schema에서 v19 migration, 구 데이터 보존·신규 쓰기 | Android SQLite/Room 필요. 고유 DB를 만들고 finally에서 삭제하므로 emulator 가능, 실사용 기기 불필요 |
| `PostcardBackRenderingTest` | 3 | 공용 뒷면 compositor PNG, read-only UI semantics, 장문 fitting | Compose UI·Android graphics/MediaStore 필요. emulator 가능. 검증에 쓰지 않고 cache에 남기던 `day66-back-qa.png` 생성은 제거 |
| `PostcardBackSaveTest` | 2 | 지연 저장 실패 경합, 추신 저장 실패 rollback | Android Main dispatcher + Room in-memory 필요. emulator 가능, 실사용 기기 불필요 |

연결된 대상은 emulator가 아니라 실사용 `SM-S936N` 한 대뿐이었다. 설치/테스트 APK lifecycle과 실제 앱 데이터 위험 때문에 계측 실행은 강행하지 않았다. 세 파일 모두 androidTest APK 컴파일은 성공했다. `PostcardBackRenderingTest`의 기존 `createComposeRule` v1 API에는 deprecation 경고가 있으나 현재 실패는 아니며, v2는 coroutine 스케줄 의미가 달라 별도 테스트 이관 후보로 남긴다.

## 구조 테스트 재집계

production 소스 텍스트를 읽어 단언하는 메서드를 다시 셌다. 순수 행동 테스트와 구조 테스트가 한 파일에 섞인 `FutureMailOpeningGuardTest`, `FutureMailTimeBoundaryTest`, `VisitDayBoundaryDefinitionTest`는 메서드별로 분리했고, 직접 `File(...).readText()`를 쓰는 파일과 `AppIntroVisitPostmarkStructureTest`도 포함했다.

```yaml
이번 세션 시작/최종: 148 tests / 28 files
현재 분류:
  실제 요구사항 보호: 60
  구현 모양 고정: 47
  행동 테스트 대체 가능: 0
  UI 하네스 필요: 41
합계: 148
```

직전 미커밋 배치에서 시작 149건 중 저가치 4건을 제거하고 행동 테스트 4건으로 교체했으며, 자정 경계의 구조 불변식 3건을 추가해 148건이 된 산식도 현재 diff와 일치한다. 이번 이어받기 시점에는 새 dependency 없이 순수 production API로 더 바꿀 “행동 대체 가능” 항목이 0건이라 추가 삭제를 만들지 않았다. 대신 8개 KDoc의 틀린 “Compose UI 테스트 인프라 없음” 표현을 “`src/test` JVM에 Robolectric 없음”으로 바로잡았다.

## Compose UI / lifecycle 기반

```yaml
Compose BOM: 2026.04.01
androidTest: androidx.ui.test.junit4 + androidx.ui.test.manifest + Espresso 3.5.1 + AndroidX JUnit 1.1.5
기존 Compose UI test: PostcardBackRenderingTest 3건
JVM unit: JUnit 4.13.2
Robolectric: 없음
```

따라서 instrumentation 쪽 렌더링·semantics·단순 상태 표시는 가능하지만 현재 안전한 emulator가 없어 실행하지 못한다. ViewModel 제거, coroutine cancellation, process lifetime 같은 lifecycle의 실제 자동 검증은 여전히 Robolectric 추가 또는 더 복잡한 instrumentation fixture가 필요하다. 이번 STOP 범위라 dependency나 테스트 architecture는 바꾸지 않았다.

## 파일 소유권 판정

- `PhotoStickerImageStorage`, `MaskingTapePhotoStorage`, `DetailViewModel`은 이미 `isInsideDirectory(root, file)`를 공유했다.
- `PostcardImageStorage.deleteIfOwnedByApp`와 `PostcardDeletionManager.cleanupPostcardOwnedAssets`만 canonical path + separator 경계라는 완전히 같은 정책을 로컬로 복제하고 있었다. 두 곳을 기존 helper로 통합했다.
- 허용 root는 각 호출부가 그대로 넘기고, 정규화 실패 시 false, root 자체 false, sibling prefix 거부 정책도 동일하다. 삭제 대상·순서·참조 판정은 바꾸지 않았다.
- `OrphanFileDiagnostics`의 canonical path 사용은 삭제 소유권이 아니라 참조 집합 비교라 의미가 달라 공용화하지 않았다.
- `AppFileOwnershipTest`, `PostcardImageStorageTest`, `PostcardDeletionManagerTest`를 함께 실행해 내부/외부/형제 prefix/`..` 경계를 재검증했다.

## 휴면 코드 / 예제 테스트

- 템플릿 기능: **휴면 유지**. 화면 진입점 없음, 사용자 템플릿 파일 가능성과 완결된 데이터 계층은 유지. 현재 KDoc이 이 상태·보존 이유·UI 복원 금지를 정확히 설명하므로 추가 production 변경 없음.
- `OrphanFileDiagnostics`: **읽기 전용 개발 진단 도구 / 휴면 유지**. production 호출 0, 삭제·이동 없음이 KDoc에 명확해 추가 변경 없음.
- `ExampleUnitTest`: 삭제 상태 유지 확인.
- `ExampleInstrumentedTest`: package name만 비교하는 Android Studio 기본 예제로 제품 회귀를 보호하지 않아 삭제.

## 이전 안정성 수정 8건 회귀

초안 read 실패 보존, 화면 이탈 저장 scope, 미래 우체통 그룹 단일 개봉/실패 후 재시도, 표시 월 방문 load, provisional 사진 실패 cleanup, 미래 우체통 날짜 경계, 파일 경로 경계, visit record read 실패 보존 코드를 현재 tree에서 다시 확인했다. 관련 12개 JVM test class를 묶어 실행했고 모두 통과했다. 실제 Room transaction rollback과 Activity/ViewModel 파괴 lifecycle은 위 환경 공백 때문에 구조/순수 로직 검증까지만 완료했다.

## 최종 자동 검증

```yaml
시작 unit test: 750 / 81 files
unit 삭제/추가: 0 / 0
최종 unit test: 750 / 81 files
전체 unit XML: 750 tests, failures 0, errors 0, skipped 0
:app:assembleDebug: BUILD SUCCESSFUL
:app:assembleDebugAndroidTest: BUILD SUCCESSFUL
instrumentation 실행: 미실행 (emulator 없음, 연결 대상은 실사용 기기)
git diff --check: 통과 (오류 없음, 기존 LF→CRLF 안내만)
```

첫 sandbox 내부 Gradle 시도들은 `foojay-resolver` plugin artifact를 해석하지 못해 코드 실행 전 실패했다. 동일 명령을 승인된 로컬 Gradle/JBR 환경에서 다시 실행해 성공했으므로 코드 실패와 분리한다.

## 사용자 확인 / Git

- **자동 검증으로 충분**: 일시적인 방문 파일 읽기 실패는 정상 사용으로 의도적으로 만들기 어렵고 production 파일 조작은 금지라 실제 파일 기반 JVM 실패 주입으로 검증했다. 정상 read 경로는 기존 동작과 계산을 바꾸지 않았고 전체 unit/build가 통과했으므로 이번 잔여 작업 자체에 필수 실기기 QA는 없다. instrumentation 6건은 사용자 실기기가 아니라 안전한 emulator가 준비되면 실행해야 한다.
- commit: 미실행·미승인
- push: 미실행·미승인
- 종료 전 기준 HEAD/upstream: `6b9a7da`, ahead/behind `0/0`
- 기존 untracked `.claude/`, `.codex-config.candidate.toml`, `.kotlin/` 보존

## 후속 후보 (승인된 작업 아님)

1. 안전한 emulator 구성 후 instrumentation 6건 실행.
2. `createComposeRule` v2 전환 영향 조사와 deterministic synchronization 보강.
3. Robolectric 도입 여부 결정 후 lifecycle/Compose 구조 테스트 41건의 실제 행동 테스트 전환 검토(새 dependency라 별도 승인 필요).

---

# HANDOFF — 78일차 최종 정비: 코드베이스 호적등본

> 아래는 이번 잔여 작업 직전 독립 작업 단위의 당시 기록이야. 당시의 “후속 후보·미수정·7건” 표기는 맨 위 최신 섹션에서 해결·재집계된 항목이 있으므로, 현재 상태 판단에는 위 최신 섹션을 우선한다.

확인일: 2026-09-19. 수동 표준 모드. 사용자 제공 "78일차 최종 정비 작업지시서(코드베이스 호적등본)"에 따라 방문 정의 확정 · 템플릿 존폐 판정 · 구조 테스트 재집계와 저위험 교체 · UI 테스트 공백 실측 · 잔여 코드 호적 분류를 수행했어. **새 기능·UI·navigation 없음, Room schema/Migration 변경 없음, 새 dependency 없음.**

> 이 세션에서도 내장 Task 도구(`TaskCreate` 등)가 제공되지 않아 진행 안내로 대체했음.

## 시작 Git 상태 (실측)

branch `feature/photo-sticker`, 시작 HEAD `6b9a7da`, `git fetch` 후 local == origin (0/0), tracked working tree clean. 기존 무관 untracked(`.codex-config.candidate.toml`, `.kotlin/`)는 건드리지 않음.

## 직전 안정성 7건 회귀 확인 (§33)

P0-1 초안 읽기 실패 보존 / P0-2 이탈 저장 lifetime(`ExitSaveScopeModule`·`ExitSaveTimeout`) / P1-3 묶음 개봉(`openFutureMailGroup`·`FutureMailOpeningGuard`) / P1-4 과거 월 방문 로딩(`visitedDaysForMonth`) / P1-5 고아 파일(`ProvisionalFile`·`writeOrDeletePartialFile`) / P2-6 자정 경계(`DayBoundary`) / P3-7 파일 소유권(`AppFileOwnership`) — **7건 모두 현재 HEAD에 그대로 살아 있음. 회귀 없음.** 재설계하지 않았음.

## 방문 정의 — canonical 확정

**방문 = 그 날짜에 앱을 새로 연 기록.** 기존 제품 문법 그대로이며 이번에 바꾸지 않았다. 정의를 `VisitRecord.kt` 맨 위에 한 번만 명문화했고, 세부 세 가지를 못박았다.

- 하루에 여러 번 열어도 1회 (같은 날 재실행은 디스크 쓰기 0회)
- 판정은 **프로세스당 한 번** (`MainActivity`의 `AppIntroState.todayVisit == null` holder)
- **앱을 켜 둔 채 자정을 넘기는 것만으로는 새 방문이 생기지 않는다.** 화면 표시(달력 "오늘", 우체통 D-day)가 자정에 갱신되는 것은 **기존 기록을 보여주는 일**이지 만드는 일이 아니다.

`recordTodayVisit`의 정의·계산식은 손대지 않았다.

### 정합성 감사 결과 (§5)

| 소비자 | 쓰는 값 | 정의 일치 |
|---|---|---|
| 인트로 소인 "N번째 방문" | `VisitRecord.totalVisitDays` | 일치 |
| 인트로 milestone / 33일차 이스터에그 | `totalVisitDays` | 일치 |
| 달력 "오늘까지 N번 만났어요~!" | `totalVisitDays` | 일치 |
| 달력 날짜 칠하기 | `VisitHistoryStorage` marker 파일 | **세는 대상이 다름(의도)** |
| `currentStreakDays` | 저장만 하고 **어떤 화면에도 노출 안 함** | 의도된 제품 결정 |

달력에 칠해진 칸 수 < "N번 만났어요"의 N일 수 있다. marker 저장소는 도입 이후 관측한 날짜만 갖고 있고 `totalVisitDays`는 그 전부터 누적됐기 때문이다. **버그가 아니며** 이제 `VisitHistoryStorage` KDoc에 그 이유가 적혀 있다.

`currentStreakDays` KDoc의 "이번 작업에서 저장만 하고"라는 시점 의존 표현을 시점 무관 문장으로 고쳤다.

### 새로 발견한 실제 버그 — 방문 기록 읽기 실패 시 누적 방문일 소실 (수정함)

**기존 위험**: `VisitRecordStorage.load()`가 `runCatching { readText() }.getOrNull()`로 **"파일 없음"과 "읽지 못함"을 구분하지 않았다.** 파일 잠금·저장소 일시 오류로 한 번만 읽기에 실패해도 `recordVisit(null, today)`가 새 기록(총 1일)을 만들고 **그대로 저장돼 사용자의 누적 방문일이 영구히 사라졌다.** P0-1(초안)과 정확히 같은 계열의 결함이다.

**수정** (§38 저위험·영향 명확 기준으로 이번 범위에서 처리):

- `StoredVisitRecord` sealed interface로 세 경우를 분리 — `Absent`(파일 없음) / `Unreadable`(읽기 실패) / `Present(record?)`(읽었음, null이면 손상)
- 읽기 실패면 **저장하지 않고** `isFirstVisitToday = false`. 다음 실행에서 정상적으로 읽히면 원래 숫자가 그대로 돌아온다
- **손상(파싱 실패) 정책은 기존 그대로** 오늘이 첫 방문으로 재시작. 보존 범위를 넓히지 않았다
- 공개 API 시그니처·`recordVisit` 계산식·저장 형식 전부 무변경

**알려진 한계(의도, 미수정)**: 읽기에 실패한 그 실행에 한해 인트로 소인이 "1번째 방문"으로 보인다. 숨기려면 `recordTodayVisit`가 nullable을 반환하고 `MainActivity`가 그걸 처리해야 하는데, 그건 표시 방식에 대한 제품 결정이라 이번 범위 밖 → 후속 후보. **디스크는 손대지 않으므로 데이터 손실은 없다.**

**회귀 테스트**: `VisitRecordStorageTest` 15 → 21건. 일시적 실패는 `readRecordText = { throw IOException(...) }`로 **실제 주입**(production 기본값을 쓰는 테스트 seam), 그리고 seam 없이 기록 파일 자리를 디렉터리로 만들어 `readText`가 진짜 실패하게 하는 검증도 하나 넣었다.

**실패 가능성 실증**: `Unreadable` 반환을 수정 전 동작(`Present(null)`)으로 임시 되돌려 실행 → 새 테스트 **5건이 정확히 실패**, 손상 재시작 테스트(`..._soTheFixDidNotWidenPreservation`)는 그대로 통과. 이후 복구하고 재검증.

### 자정 경계 회귀 테스트 (§8)

`VisitDayBoundaryDefinitionTest` 신설 (4건). 보장의 출처를 먼저 분명히 했다 — 저장소 자체는 자정을 넘겨 **다시 호출되면** 새 방문을 만드는 게 맞고(`recordTodayVisit_justBeforeAndAfterLocalMidnight_countsAsTwoDays`가 이미 고정), 따라서 "자정만 통과했을 때 새 방문 없음"은 **자정 신호를 받는 쪽이 방문 기록 API를 부르지 않는다**는 사실에서만 나온다. 그 사실을 실제 소스에서 검사한다.

- 자정 신호 소비자 목록(`FutureMailboxViewModel`, `DetailScreen`, `GalleryScreen`, `VisitCalendarDrawer`)이 실제와 맞는지 먼저 확인 → 목록이 낡아 검사가 조용히 비는 것을 막음
- 그 4개 파일 어디에도 `recordTodayVisit(` / `VisitHistoryStorage.recordDate(`가 없음
- 유일한 기록 지점 `MainActivity`는 자정 신호를 쓰지 않고 프로세스당 1회 holder로 가름
- (행동) 같은 날 자정 직전에 다시 열어도 기록이 늘지 않음

**기기 날짜는 바꾸지 않았다.**

## 템플릿 기능 — **휴면(dormant)** 판정

| 항목 | 실측 결과 |
|---|---|
| 마지막 UI 진입점 | **2026-08-28 `7b3edd9`** "Simplify photo editing UI into layout and edit panels"에서 제거. 커밋 메시지가 "Template data ... unchanged"라고 **명시** |
| production 호출 | `PostcardTemplateSection` / `PostcardTemplateStorage` / `BuiltInTemplates` / `applyTemplateStyle` / `toTemplateStyle` 모두 **호출부 0** |
| Room 영향 | **전용 Entity·컬럼 없음.** `updatePostcardTemplateStyle`은 엽서가 원래 가진 스타일 컬럼들을 갱신할 뿐 → schema/Migration과 무관 |
| 기존 사용자 데이터 | **있을 수 있음.** 2026-07-24 ~ 08-28 사이 저장한 템플릿이 기기 `filesDir/postcard_templates/`에 남아 있고, 지우는 경로도 없다 |
| 보호 중인 테스트 | 27건 (`PostcardTemplateTest` 15 / `PostcardTemplateStorageTest` 7 / `BuiltInTemplatesTest` 5) |

→ **§11.C 휴면 기능**. 삭제하지 않는다. 기능 계층(모델·직렬화·내장 템플릿·파일 저장소·미리보기)이 완결된 채 화면만 떼어낸 상태이고, 코드를 지우면 기기에 남은 사용자 템플릿 파일을 되살릴 방법이 사라진다. 판정 근거와 되살리기 조건을 `PostcardTemplate.kt` 맨 위에 한 번 적고 나머지 3개 파일에는 한 줄 포인터만 뒀다. **UI는 새로 붙이지 않았다(§12).**

## 구조 테스트 — 실제 재집계와 4분류

과거 보고값 "129건 / 24파일"을 재사용하지 않고 다시 셌다. 판정 기준: **production 소스 텍스트를 읽어 단언하는 테스트**(공용 helper 경유 + `File("src/main/...")` 직접 읽기 양쪽 포함).

```yaml
구조 테스트 시작: 149건 / 27파일   # 전체 741건 중. 과거 "129/24"와 다름 — 직접 읽기 방식 파일이 집계에서 빠져 있었음
  실제 요구사항 보호:   57
  구현 모양 고정:       48
  행동 테스트 대체 가능: 3
  UI 하네스 필요:       41
삭제: 4
행동 테스트로 교체: 4건 제거 → 행동 4건 추가 + 대체 불가 영역의 새 구조 테스트 3건 추가
최종 구조 테스트: 148건 / 28파일
```

4분류 기준과 대표 예:

1. **실제 요구사항 보호 (57)** — Migration 등록 연속성, 저장 경합 mutex, 확정 저장 시 undo 히스토리 정리, 미리보기/exporter 레이어 순서 일치, 봉인 엽서가 내용을 넘겨받지 않음, 소인이 총 방문일만 보여주고 연속일 압박 표현을 쓰지 않음, 방문 판정 1회/프로세스. §17에 따라 유지.
2. **구현 모양 고정 (48)** — `declares...ExactlyOnce`, `noLongerDeclares...`, `hasExactly{N}CallSites`, `takesExpectedCoreParameters`. 리팩토링만 해도 깨진다. 다만 호출부 개수 검사는 "실수로 호출부를 지움"이라는 실제 회귀를 잡으므로 대체 수단 없이 삭제하지 않았다.
3. **행동 테스트 대체 가능 (3)** — 전부 이번에 교체함(아래).
4. **UI 하네스 필요 (41)** — 렌더링·제스처·애니메이션·접근성 터치 타깃처럼 실제 화면 없이는 확인 불가.

### 실제로 교체한 4건

| 제거 | 이유 | 대체 |
|---|---|---|
| `StickerEditModeToolbarStructureTest.stickerEditModeEnum_visibilityWidenedButMembersUnchanged` | **사실상 항상 참이었음** — 4000줄짜리 `DetailScreen.kt` 전체에 "Move"/"Scale"/"Rotate"가 있는지만 봄 | **신설** `StickerEditModeTest` 1건: `StickerEditMode.entries`를 직접 읽어 구성·순서 고정. 멤버를 지우면 컴파일이 깨지고, `private`으로 좁히면 테스트 소스가 컴파일되지 않음 |
| `OverlayFallbackSizeStructureTest.componentFile_declaresComputeFallbackOverlaySizeExactlyTwice` | 선언 개수만 셈 | **추가** `PostcardOverlayExportLogicTest` 3건: 직사각형 오버로드(마스킹테이프용)의 실제 계산 검증. 그동안 이 오버로드는 **행동 검증이 하나도 없었다** |
| `OverlayFallbackSizeStructureTest.componentFile_keepsExactSignatureAndTypes` | 파라미터 문자열 검사 | 같은 위 — 이름 붙인 인자로 호출하므로 시그니처가 **컴파일로** 고정됨 |
| `StickerPositionCalculationsStructureTest.componentFile_keepsExactSignatureAndTypes` | 파라미터 문자열 검사 | 기존 `StickerPositionCalculationsTest` 4건 + `PostcardOverlayExportLogicTest`가 이미 같은 함수를 이름 붙인 인자로 호출 중 → 중복 |

§36의 질문("이 테스트가 없으면 어떤 실제 회귀를 놓치는가?")에 네 건 모두 답이 "함수 이름/줄 모양이 바뀌는 것"뿐이었다.

### 구조 테스트 helper 감사 (§19)

`testsupport/StructureTestSource.kt`는 건강함 — 경로 후보 2개(모듈 루트/저장소 루트), 못 찾으면 cwd와 후보를 붙여 **즉시 실패**(조용한 통과 없음), 중복 helper 없음. 로직은 건드리지 않았고 KDoc의 사실만 고쳤다(아래).

## UI 자동 테스트 공백 — 실측 결과가 기존 통념과 달랐음

여러 테스트 KDoc이 "이 프로젝트는 Compose UI 테스트 인프라(androidx.compose.ui:ui-test)를 쓰지 않는다"고 적고 있었는데 **사실이 아니다.**

```yaml
androidx.compose.ui:ui-test-junit4:   이미 있음 (androidTestImplementation)
androidx.compose.ui:ui-test-manifest: 이미 있음 (debugImplementation)
실제 사용:                            PostcardBackRenderingTest가 createComposeRule/onNodeWithText 사용 중
JVM unit test 쪽 하네스(Robolectric):  없음  <-- 진짜 공백은 여기
새 dependency 필요 여부:               Compose UI test는 불필요 / Robolectric 도입은 별도 승인 필요
```

정확한 상태: **없는 것은 `src/test`(JVM) 쪽 하네스다.** `src/androidTest`에서는 Composable 렌더링도 ViewModel 인스턴스화도 이미 된다. 다만 instrumented 실행이 필요하고, 실사용 기기 계측은 `AGENTS.md` 5절로 금지, emulator는 미구성 → **지금 당장 확인할 수 있는 유일한 수단이 소스 텍스트**라서 구조 테스트가 존재한다.

이 정확한 사실을 공용 helper `StructureTestSource.kt` KDoc에 한 번 적고, 아티팩트 이름까지 대며 틀리게 단언하던 `SaveErrorDialogStructureTest` KDoc을 고쳤다. **남은 정리**: 다른 약 8개 테스트 파일이 여전히 "Compose UI 테스트 인프라가 없는 프로젝트 관례"라는 느슨한 표현을 쓴다 — 틀린 아티팩트 이름을 대지는 않지만 정확하지 않음 → 후속 후보(주석-only).

### UI 자동화가 없어 놓치는 영역 (§21)

실제 렌더링 결과, 클릭/제스처, navigation, lifecycle, recomposition, semantics, 접근성 터치 타깃, 화면을 띄운 채 자정 통과. **unit test로 이미 충분히 보호되는 영역과는 구분됨** — 순수 계산(오프셋·크기·진행률·날짜 경계), 직렬화/파싱, 파일 저장소 원자성·소유권, Flow 조립 규칙은 전부 행동 테스트가 덮고 있다.

## instrumentation 현황 (§24)

7건 / 4파일. 실기기에서 실행하지 않았고(정적 감사만), 실행에는 emulator가 필요하다.

| 파일 | 건수 | 검증 대상 | 보호 가치 |
|---|---|---|---|
| `PostcardBackMigrationTest` | 1 | 실제 v18 DB에서 migration 후 보존 + 신규 쓰기 | **높음** (사용자 데이터) |
| `PostcardBackRenderingTest` | 3 | 공유 PNG 풀해상도 합성, 빈 뒷면에 placeholder 없음, 긴 본문 측정 | 높음 (미리보기/내보내기 일치) |
| `PostcardBackSaveTest` | 2 | 저장 실패가 최신 텍스트를 되돌리지 못함, 실패 후 영구 텍스트 복원 | **높음** (저장 경합) |
| `ExampleInstrumentedTest` | 1 | `packageName == "com.postcardmemory"` | 낮음 (템플릿 잔재) |

## ExampleUnitTest 최종 재판 (§26·§27) — **사망 확정, 삭제**

`assertEquals(4, 2 + 2)`. 앱 코드 호출 0, `.github` 없음(CI 없음 → smoke 역할 없음), 문서 참조는 HANDOFF의 "유지" 판정 기록뿐(참조가 아니라 판정문), 750건의 다른 테스트가 테스트 인프라를 훨씬 강하게 증명함. 1·2차 감사에서 "삭제 이득 0"으로 유지했으나 이번 지시의 "문화재 특별대우 없이" 기준에 따라 **삭제**.

`ExampleInstrumentedTest`(androidTest)는 §26이 지목한 대상이 아니라 그대로 뒀다 → 후속 후보.

## 잔여 코드 호적 분류 (§28~32)

| 대상 | 판정 | 근거 / 조치 |
|---|---|---|
| `OrphanFileDiagnostics` | **휴면** | production 호출 0이지만 완결된 읽기 전용 진단 계층 + 테스트 다수. UI 연결은 의도적 비범위. 삭제 안 함 |
| 템플릿 6파일 | **휴면** | 위 참조. KDoc으로 이유 기록 |
| `EditorOutlineButton` | **사망 → 삭제** | production 호출 **0**. 참조하던 구조 테스트 2곳은 모두 `assertFalse(...)`(= 쓰지 말 것) 또는 KDoc. KDoc이 "낙서 등 아직 옮기지 않은 화면은 계속 쓴다"고 **거짓 주장**하고 있었는데, 낙서는 55일차에 이미 이전됨. 함수 + 전용 import 5개 삭제 |
| `VisitRecord.currentStreakDays` | **호환성/의도된 보류** | 저장은 되지만 화면 노출 0. 압박 표현 회피가 이유. KDoc에 시점 무관 문장으로 명시 |
| `vibrateGalleryFab` / `vibrateVisitCalendarTodayReturn` / `vibrateIntroPostmark` | **현역 3개** | 각 파일 `private`, 강도·길이가 서로 다름. 통합은 후속 후보(강도 차이 보존 확인 필요) |

## 변경 파일과 이유

**production (9)**

- `utils/VisitRecordStorage.kt` — 읽기 실패와 손상 분리(데이터 보존). **이번 유일한 동작 변경**
- `utils/VisitRecord.kt` — canonical 정의 명문화, 시점 의존 표현 제거
- `utils/VisitHistoryStorage.kt` — `totalVisitDays`와 세는 대상이 다른 이유 명시
- `ui/gallery/VisitCalendarDrawer.kt` — 달력 월 로딩이 읽기 전용임을 명시(표시 != 기록 생성)
- `ui/detail/PostcardTemplate.kt` — 휴면 판정 근거 전문
- `ui/detail/BuiltInTemplates.kt` / `ui/detail/PostcardTemplateRow.kt` / `utils/PostcardTemplateStorage.kt` — 휴면 포인터 한 줄
- `ui/components/EditorSharedControls.kt` — `EditorOutlineButton` 삭제 + 거짓 KDoc 수정

**test (10)**

- `utils/VisitRecordStorageTest.kt` (15→21), `utils/VisitDayBoundaryDefinitionTest.kt` (신설 4), `ui/detail/StickerEditModeTest.kt` (신설 1), `ui/detail/PostcardOverlayExportLogicTest.kt` (+3)
- `ui/detail/OverlayFallbackSizeStructureTest.kt` (5→3), `ui/detail/StickerPositionCalculationsStructureTest.kt` (5→4), `ui/detail/StickerEditModeToolbarStructureTest.kt` (10→9)
- `testsupport/StructureTestSource.kt` / `ui/detail/SaveErrorDialogStructureTest.kt` — KDoc 사실 정정
- `ExampleUnitTest.kt` — 삭제

## 테스트 수 변화

```yaml
시작 unit test: 741건 / 80파일
삭제:  5    # ExampleUnitTest 1 + 저가치 구조 테스트 4
추가: 14    # 방문 읽기 실패 6, 자정 경계 정의 4, 직사각형 fallback 3, StickerEditMode 1
최종 unit test: 750건 / 81파일   # 소스 @Test 수와 runner 실행 수가 정확히 일치
구조 테스트: 149 -> 148
행동 테스트: 592 -> 602
```

## 자동 검증 (오늘 실제 실행)

- `assembleDebug` — **BUILD SUCCESSFUL**, `app/build/outputs/apk/debug/app-debug.apk` 생성
- 전체 unit test — **750건, 실패 0 / 오류 0 / 건너뜀 0 / 81 클래스**
- `compileDebugKotlin` — 성공 (dead code 삭제 직후 단독 확인, 미사용 import 정리 포함)
- `git diff --check` — 통과
- 항목별로 `수정 → 관련 테스트 → 컴파일` 순서를 지켰고, gradle 실행 중에는 소스를 편집하지 않았음

## 실기기 QA 필요성

**필요 (최소 smoke 1건)**. 이번 변경 대부분은 주석·테스트·dead code지만 `VisitRecordStorage`는 **production 동작 변경**이라 §44의 "테스트-only" 예외에 해당하지 않는다.

- 대상 build: 이번 `assembleDebug` 산출 APK
- 조작: 앱 실행 → 인트로 소인에 "N번째 방문" 표시 확인 → 갤러리 좌측 방문 달력 열어 "오늘까지 N번 만났어요~!"와 오늘 칸 표시 확인 → 앱 종료 후 재실행해 **같은 날 두 번째 실행에서 숫자가 늘지 않는지** 확인
- 기대: 숫자가 이전과 동일하게 이어지고, 같은 날 재실행에 증가 없음
- 자동 검증 한계: 정상 경로의 숫자 연속성은 단위 테스트로 덮여 있으나, 실제 기기의 기존 `visit_record.txt`가 새 코드로도 그대로 읽히는지는 그 파일이 있는 기기에서만 확인된다

## 미검증 / 남은 위험

- **자연적인 자정 통과** — 앱을 켜 둔 채 날짜가 바뀔 때 달력 "오늘"과 우체통 D-day가 실제로 갱신되는지. 기기 날짜 변경 금지(§25)라 자연 발생을 기다려야 함. 재개 조건: 앱을 켜 둔 채 자정을 넘긴 사용자 관찰
- **묶음 개봉 부분 실패 rollback** (78일차 후속에서 이월) — Room in-memory DB가 필요한 instrumented 영역, 실기기 계측 금지
- **instrumentation 7건 전부 미실행** — emulator 미구성. 정적 감사만 수행
- **읽기 실패 실행의 인트로 표시** — 그 실행에 한해 "1번째 방문"으로 보임(디스크는 무사). 의도적 미수정
- 다른 약 8개 테스트 파일의 "Compose UI 테스트 인프라가 없다"는 느슨한 표현

## 이번 범위에서 STOP한 것 (§41)

Room schema/migration, 데이터 변환, 새 dependency(Robolectric 포함), 새 navigation/UI, 템플릿 화면 복구, Compose test framework 신규 도입, 대규모 architecture 변경 — 전부 손대지 않음. 조사만 하고 구현하지 않았음.

## Git 상태

commit **미실행(사용자 승인 대기)**, push **미실행**. tracked 변경 19개(수정 16 / 신설 2 / 삭제 1), 기존 무관 untracked 2개는 그대로.

## 후속 후보 (승인된 작업 아님)

1. **Robolectric 도입 검토** — `src/test`에서 Composable/ViewModel을 다룰 수 있게 되면 "구현 모양 고정" 48건 중 상당수를 행동 테스트로 교체 가능. **새 dependency라 별도 승인 필요**
2. emulator 구성 → instrumentation 7건 + 묶음 개봉 rollback 실제 검증
3. 방문 기록 읽기 실패 시 인트로 숫자를 숨길지 결정(제품 표시 결정 + `recordTodayVisit` nullable화)
4. `ExampleInstrumentedTest` 존폐 판정
5. 나머지 테스트 파일의 "Compose UI 테스트 인프라" 표현 정정(주석-only)
6. `vibrate*` 3종 통합(강도·길이 차이 보존 확인 필요)
7. (78일차 후속에서 이월) `awaitPendingStyleSaves()`의 style-save Job 19개 scope 이전, `isInsideDirectory` 잔여 2곳 통합, `promoteDraftStickerBackgrounds`의 낡은 주석 수정
8. 템플릿 기능 되살리기 여부 — **제품 결정**. 되살린다면 화면만 붙이면 됨

---

# HANDOFF — 78일차 후속: 안정성 보강 (데이터 보존 · 실패 복구 · 시간 경계)

확인일: 2026-09-19. 수동 표준 모드. 사용자 제공 "78일차 안정성 보강 수정지시서"(P0 2건 / P1 3건 / P2 1건 / P3 1건)에 따라 항목별로 `조사 → 최소 수정 → 회귀 테스트 → 컴파일 확인`을 하나씩 끝내고 마지막에 전체 검증했어. **새 기능·UI·디자인 변경 없음, Room schema/Migration 변경 없음, 새 dependency 없음.** 실기기 smoke QA 대기, commit·push 미실행(사용자 승인 대기).

## 시작 Git 상태 (실측)

branch `feature/photo-sticker`, 시작 HEAD `a0352b6`, `git fetch` 후 local == origin (0/0), tracked working tree clean. 기존 무관 untracked(`.codex-config.candidate.toml`, `.kotlin/`)는 건드리지 않음. 감사 보고서의 값과 실제가 일치했음.

> 이 세션에서 내장 Task 도구(`TaskCreate` 등)가 제공되지 않아 진행 안내로 대체했음(`CLAUDE.md` 내장 Task 운영 절의 기능 부재 규정 적용).

## P0-1 — 초안 읽기 실패를 "손상"으로 오인해 삭제하던 문제

**기존 위험**: `PostcardDraftStorage.loadDraft`가 `runCatching { file.readText() }.getOrNull()`이 null이면 곧바로 초안 파일과 `draft_sticker_bgs/<postcardId>/`(누끼 PNG 폴더)를 **지웠다**. 파일 잠금·저장소 일시 오류 같은 일시적 I/O 실패와 실제 손상을 구분하지 못해, 한 번 읽기에 실패하면 사용자의 편집 초안이 영구 삭제됐다.

**수정**: 세 경우를 분리했다.

- 파일 없음 → 초안 없음(null), 아무것도 지우지 않음 (기존과 동일)
- **읽기 실패(예외) → 아무것도 지우지 않고 이번 진입만 복원을 건너뜀(null)**. 내용을 한 글자도 못 봤으므로 손상 판정 자체가 불가능하다는 근거를 KDoc에 남김
- 내용을 읽었는데 파싱 실패 → **기존 격리(삭제) 정책 그대로 유지**. 잘못된 UTF-8은 `readText`가 던지지 않고 U+FFFD로 치환하고, 잘린 파일도 파싱에서 걸리므로 실제 손상은 전부 이쪽 경로로 들어온다

`invalidateDraftFile` fallback(빈 내용 덮어쓰기 → 다음 loadDraft가 파싱 실패로 정리)도 파싱 경로라 그대로 동작한다.

**회귀 테스트**: `PostcardDraftStorageTest` 24 → 29건. 일시적 실패는 `loadDraft(filesDir, id) { throw IOException(...) }`로 **실제 주입**(production 기본값을 쓰는 테스트 seam 파라미터 추가). seam 없이 production 기본 경로만 쓰는 검증도 하나 넣었다(초안 경로를 디렉터리로 만들어 `readText`가 실제로 실패하게 함).

**실패 가능성 실증(§33)**: 수정 전 동작(`file.delete()` + `deleteRecursively()`)으로 되돌려 실행 → 새 테스트 **4건이 정확히 실패**, 손상 초안 격리 테스트는 그대로 통과. 이후 수정본 복구하고 TEMP 잔재 0건 확인 후 전체 재검증.

## P0-2 — 화면 이탈 상한 시간이 "대기"가 아니라 "저장"을 취소하던 문제

**조사 결과(§8: timeout 시 무엇이 취소되는가)** — `awaitPendingStyleSaves()` 안의 세 덩어리는 성질이 전부 달랐다.

| 대상 | 실행 위치 | 상한 시간이 취소하는 것 |
|---|---|---|
| style-save Job 19개 | `viewModelScope.launch` | **join(대기)만**. Job은 안 죽는다 |
| 초안 flush(`persistDraftNow`) | 대기 coroutine에서 **직접 suspend 호출** | **저장 자체**가 취소됨 ← 실제 버그 |
| cleanup sweep 2개 | 대기 coroutine에서 직접 호출, **상한 없음** | 아무것도 안 끊음 → navigation 무한 대기 가능 |

즉 KDoc이 약속한 "navigation이 무기한 멈추지 않도록 상한을 둔다"는 sweep 두 개에는 애초에 적용되지 않았고, 반대로 초안 flush에는 과하게 적용돼 있었다. 게다가 flush 직전 `draftAutosaveJob?.cancel()`을 이미 했기 때문에 재시도할 주체도 없어 **마지막 편집이 그대로 유실**됐다.

**수정** (숫자를 늘리는 대신 생명주기를 분리):

1. `di/ExitSaveScopeModule.kt` — `@ExitSaveScope`로 한정한 `@Singleton CoroutineScope(SupervisorJob() + Dispatchers.Default)`. **이탈 직전 마지막 저장 전용**이며 남용 금지를 KDoc에 명시. `GlobalScope` 아님, 새 dependency 아님
2. `ui/detail/ExitSaveTimeout.kt` — `launchSaveAndAwaitWithUiTimeout(saveScope, timeoutMillis, save)`. 저장을 `saveScope`에서 띄우고 `join()`만 상한으로 끊는다. 반환값 false는 "실패"가 아니라 "아직 진행 중이라 기다리지 않고 돌아간다"는 뜻
3. `awaitPendingStyleSaves()` — 초안 flush를 이 함수로 교체. cleanup sweep 두 개는 **상한 안으로** 넣음(이미 아무도 참조하지 않는 파일 삭제라 끊겨도 데이터 손실이 없고 다음 기회에 다시 후보가 됨)

**회귀 테스트**: `ExitSaveTimeoutTest` 4건. production 함수를 직접 호출한다(replica 아님). 상한 초과 시 대기는 끝나지만 저장은 완료되는 것, 대기하던 coroutine을 실제로 취소해도 저장이 살아남는 것을 검증. **수정 전 구조(`withTimeoutOrNull { save() }`)를 그대로 재현한 테스트를 함께 두어** 그 형태에서는 저장이 취소돼 영구 유실된다는 사실을 실행 가능한 형태로 못박음.

**남은 부분(미해소, 의도적)**: style-save Job 19개는 여전히 `viewModelScope`에 있어, 상한을 넘기면 navigation 직후 ViewModelStore.clear()로 취소된다. 19개를 한꺼번에 다른 scope로 옮기는 것은 각 Job의 취소 의미(덮어쓰기 저장의 선행 취소 등)까지 재검토해야 해서 이번 최소 수정 범위를 넘는다 → **후속 후보**.

## P1-3 — 미래 엽서 묶음 부분 개봉과 재시도 차단

**기존 위험**: `openArrivedGroup`이 `postcardIds.forEach { repository.openFutureMail(id) }`로 id마다 따로 열었고, 개봉 중 표시 해제(`_openingDeliverAtMillis -= ...`)가 루프 뒤 일반 문장이었다. 중간에 예외가 나면 (a) 앞의 몇 장만 열린 상태로 남고 (b) 표시가 해제되지 않아 **같은 묶음을 다시 시도할 수 없었다**.

**수정**:

- `PostcardDao.openFutureMail(id)` → `openFutureMailGroup(ids: List<Long>)`로 교체. `WHERE id IN (:ids)` **단일 UPDATE**라 SQLite가 문장 단위로 원자적으로 처리한다 — transaction 블록이나 schema 변경 없이 묶음 원자성 확보. 단일 id 경로는 `listOf(id)`와 동치라 남겨두지 않음(같은 일을 하는 두 경로 제거)
- `ui/futuremail/FutureMailOpeningGuard.kt` 신설 — `beginOrSkip(key)`(연타 차단, coroutine 띄우기 전 동기 호출)와 `releasingAfter(key) { ... }`(**finally에서 반드시 해제**, 예외는 그대로 재전파). 해제를 호출부의 성실함에 맡기지 않고 함수 안에 가둠. UI 스레드/IO 스레드 양쪽 접근이라 `MutableStateFlow.update`로 원자 갱신

**테스트**: `FutureMailOpeningGuardTest` 6건(성공/예외/취소 해제, 실패 후 재시도 가능, 다른 그룹 간섭 없음) + `FutureMailGroupOpenAtomicityStructureTest` 2건(id별 루프 재도입 차단, DAO 단일 UPDATE).

**미검증(명시)**: "하나 실패 → 전체 rollback"의 실제 DB 동작은 Room in-memory DB가 필요해 instrumented test 영역이고, 실기기 계측은 `AGENTS.md` 5절로 금지 → **미실행으로 기록**. 근거는 SQLite 단일 문장 원자성과 위 구조 테스트.

## P1-4 — 방문 달력이 앱 시작 월의 기록만 들고 있던 문제

**기존 동작**: `MainActivity`가 `VisitHistoryStorage.loadMonth(filesDir, YearMonth.now())`로 **현재 월 하나만** 읽어 `visitedEpochDays`로 내려줬다. 달력은 어느 달로든 이동할 수 있는데 집합은 그대로여서, 8월에 실제로 방문한 기록이 디스크에 있어도 8월로 이동하면 빈 달로 보였다.

**수정**:

- `MonthlyVisitCalendar`에 `LaunchedEffect(displayedMonth, todayYearMonth)`를 두어 **표시 월이 바뀌면 그 달의 marker만** `VisitHistoryStorage.loadMonth`로 읽는다(IO dispatcher). 한 달은 marker 파일 최대 31개 stat이라 이동할 때마다 읽어도 싸다
- 같은 drawer 세션에서 왔다 갔다 할 때 표시가 깜빡이지 않도록 읽어온 달만 `mutableStateMapOf`에 기억한다 — **세션 한정 memo이지 영구 캐시 architecture가 아니다**(§19)
- 어느 집합을 쓸지는 순수 함수 `visitedDaysForMonth(month, currentMonth, currentMonthVisitedDays, loadedByMonth)`로 분리. 아직 못 읽은 달은 **빈 집합**을 준다 — 다른 달 집합을 흘려보내지 않는다

**유지한 것**: `displayedMonth`/`pickerYear`의 key 없는 `rememberSaveable`(탐색 위치 보존), today marker·색·햅틱·swipe·오늘 복귀 의미, `recordTodayVisit`의 정의 — 전부 손대지 않음.

**테스트**: `VisitCalendarMonthLoadingTest` 8건. 규칙 판정 4건 + 실제 marker 파일을 `TemporaryFolder`에 만들고 `loadMonth`로 읽어 9월→8월→9월 왕복을 확인하는 2건 + 과거 월 grid에 오늘이 포함되지 않아 today 표시와 방문 표시가 섞이지 않음 2건.

## P1-5 — 이미지 파일 생성 후 DB 커밋 실패 시 고아 파일

**기존 위험 두 가지**:

- `ImageUtils.cropToStampRatio`: `compress`가 false를 돌려주거나 도중에 예외가 나면 0바이트/반쯤 쓰인 JPEG가 `postcards/`에 남았다
- `CameraViewModel.saveCroppedPhoto`: 파일 생성 성공 후 `insertPostcard` 실패나 coroutine 취소 시 생성 파일이 남았다. 게다가 `catch (exception: Exception)`이 `CancellationException`까지 삼켰고, 정리 `finally`의 `withContext(Dispatchers.IO)`는 취소된 coroutine에서 즉시 다시 취소돼 **촬영 임시 파일조차 지우지 못했다**

**수정**:

- `ImageUtils.writeOrDeletePartialFile(outputFile, write)` — 쓰기 실패(false 반환 또는 예외) 시 출력 파일을 지우고 예외를 재전파. `internal`이라 Bitmap 없이 순수 JUnit에서 검증 가능
- `utils/ProvisionalFile.kt`의 `withProvisionalFile(produce, commit)` — **커밋 전까지 파일은 임시 소유**라는 규칙을 한 곳에 못박음. commit 실패·취소 시 `NonCancellable`에서 삭제. `produce` 자체가 실패하면 만든 게 없으므로 아무것도 안 지운다
- `saveCroppedPhoto`가 이 helper를 사용. `CancellationException`을 별도로 잡아 **에러 화면을 띄우지 않고 재전파**하고, 촬영 임시 파일 정리도 `NonCancellable + Dispatchers.IO`로 옮겨 취소 시에도 실제로 지워지게 함

**사용자 원본 보호**: 지우는 대상은 앱이 이번 호출에서 만든 `filesDir/postcards/postcard_*.jpg`와 앱 자신의 촬영 임시 파일 `filesDir/postcards_temp/temp_*.jpg`뿐. 외부 갤러리 URI·사용자 원본은 이 경로에 들어오지 않는다.

**테스트**: `ProvisionalFileTest` 9건. 커밋 성공 시 유지 / 실패 시 삭제+재전파 / 취소 시 삭제 / **실제 Job 취소**로도 삭제 / produce 실패 시 남의 파일 안 건드림 / compress false·예외 시 부분 파일 제거 / 정상 쓰기 시 내용 보존.

## P2-6 — 미래 우체통 자정 경계

**§26 먼저 확인한 것 — `deliverAtMillis`의 의미**: 발송 시 `materialDatePickerUtcMillisToLocalStartOfDay`로 변환하고 그룹화 시 `startOfDayMillis`로 재정규화하므로 **로컬 타임존 자정**이다. 그리고 도착(`isFutureMailArrived`)·D-day(`daysUntilFutureMail`)·진행률(`futureMailProgressPercent`)이 **전부 자정 기준 날짜 비교**이고 시:분을 보는 판정은 프로젝트에 없다. → **갱신 주기는 "자정마다 한 번"이면 충분**하고, 시각 단위 ticker는 불필요하다(테스트로도 고정: 같은 날 안에서는 시각이 바뀌어도 그룹 결과가 완전히 동일).

**수정**:

- `utils/DayBoundary.kt` 신설. 78일차에 만든 순수 함수 `millisUntilNextMidnight`를 `ui/gallery/GalleryCurrentDate.kt`에서 여기로 **옮기고**(중복 구현을 만들지 않기 위해), 같은 함수를 쓰는 `dayBoundaryTicks(): Flow<Unit>`를 추가. 구독 즉시 1회 방출 후 자정마다 1회 — polling 아님. `rememberTodayDate()`(Compose)와 `dayBoundaryTicks()`(Flow)가 **같은 경계 함수를 공유**하므로 화면과 ViewModel의 날짜가 갈라지지 않는다
- `FutureMailboxViewModel.groups` — Room Flow 단독 `map` → `combine(Room Flow, dayBoundaryTicks())`. DB가 그대로여도 자정이 지나면 다시 판정한다. `WhileSubscribed(5000)` 안이라 화면을 안 보면 ticker도 멈춘다
- `FutureMailGroup`에 `daysLeft: Long` 추가, `buildFutureMailGroups`가 `arrived`/`progressPercent`와 **같은 nowMillis 하나로** 계산. `FutureMailboxScreen`은 화면에서 시각을 다시 읽지 않고 이 값을 그대로 쓴다(§27의 "D-day / progress 동일 기준")
- `DetailScreen`의 봉인된 미래 엽서 화면 — `val now = remember { System.currentTimeMillis() }` → `rememberTodayDate()`를 key로 삼아 날짜가 바뀔 때만 시각을 다시 읽음

**테스트**: `DayBoundaryTest` 4건(즉시 방출, 경계마다 반복 방출, 정확히 자정일 때 하루치 대기, 두 소비자가 같은 경계 함수 공유) + `FutureMailTimeBoundaryTest` 9건(같은 날 안에서 시각 무관 / 자정 통과 시 arrived 전환·daysLeft 감소·progress 전진 / daysLeft와 arrived가 같은 now 기반 / 도착 후 0 이하 + 100% / ViewModel·화면·상세화면이 실제로 새 기준을 쓰는지).

## P3-7 — 파일 소유권 prefix 판정

**조사 결과** — `startsWith`를 쓰는 7곳 중 **이미 안전한 2곳**과 **경계 확인이 없는 5개 지점**으로 갈렸다.

| 위치 | 상태 |
|---|---|
| `PostcardImageStorage.deleteIfOwnedByApp` | 👻 이미 안전 (`canonicalPath + File.separator`) — 손대지 않음 |
| `PostcardDeletionManager.cleanupPostcardOwnedAssets` | 👻 이미 안전 (동일) — 손대지 않음 |
| `PhotoStickerImageStorage.deleteOriginalIfUnreferenced` | ⚠️ 수정 |
| `MaskingTapePhotoStorage.deleteIfUnreferenced` | ⚠️ 수정 |
| `DetailViewModel.deleteStickerCacheFile` (cache/persist 2곳) | ⚠️ 수정 |
| `DetailViewModel.persistStickerBackground`의 캐시 원본 삭제 가드 | ⚠️ 수정 (canonical화도 안 하고 있었음) |

**수정**: `utils/AppFileOwnership.kt`의 `isInsideDirectory(root, file)` 하나로 통합. canonical 변환 후 `filePath[rootPath.length] == File.separatorChar`까지 확인한다. root 그 자체는 false(디렉터리는 자산 파일이 아니다), canonical 변환 실패 시 안전한 쪽인 false, 파일 존재 여부는 보지 않음(삭제 가드는 존재 확인보다 먼저 소유권을 물어야 함).

**테스트**: `AppFileOwnershipTest` 10건 — 하위 파일/중첩 하위 허용, root 자체 거부, 이름만 비슷한 sibling 디렉터리·sibling 파일 거부, `../`로 빠져나가는 경로 거부, 나갔다 다시 들어오는 경로 허용, 무관 경로 거부, 부모 거부, 아직 없는 파일도 경로만으로 판정.

**의도적으로 안 한 것**: 이미 올바른 2곳은 건드리지 않았다. 같은 규칙의 구현이 두 벌 남았지만, `PostcardDeletionManager`는 삭제 경로 한복판이라 동작이 옳은 코드를 이번 안정성 수정에 섞지 않았다 → **후속 후보**(78일차 후속 후보 #4와 동일 항목).

## 위험도 재평가 (§36)

| 항목 | 감사 등급 | 실측 후 | 근거 |
|---|---|---|---|
| P0-1 | 높음 | **높음 유지** | 일시적 I/O 한 번에 사용자 초안 + 누끼 파일이 영구 삭제. 실증으로 4건 실패 확인 |
| P0-2 | 높음 | **높음 유지, 단 원인이 다름** | "2초가 짧다"가 아니라 상한과 저장의 생명주기 결합이 원인. 숫자를 키웠으면 안 고쳐졌다 |
| P1-3 | 중간 | **중간 유지** | 부분 개봉 + 재시도 영구 차단. 다만 개봉 자체가 멱등이라 데이터 손상은 아니고 상태 불일치 |
| P1-4 | 중간 | **중간 → 낮음~중간** | 데이터는 안전하고 **표시만** 누락. 다만 사용자 눈에는 "내 방문 기록이 사라졌다"로 보여 체감은 큼 |
| P1-5 | 중간 | **중간 유지 + 발견 1건 추가** | 고아 파일 외에, 취소 시 촬영 임시 파일도 안 지워지고 있었음(`CancellationException`을 일반 오류로 삼킴) |
| P2-6 | 중간 | **중간 유지** | 날짜 단위 갱신으로 충분함을 확인해 수정 범위가 줄었음 |
| P3-7 | 낮음 | **낮음 유지** | 실제로 `sticker_originals_backup` 같은 형제 디렉터리를 만드는 코드는 현재 없음. 방어적 수정 |

## 추가 발견 (이번에 고치지 않음)

- `DetailViewModel.promoteDraftStickerBackgrounds`의 주석이 "onCleared()가 화면 이탈 시 cacheDir/photo_stickers를 정리하므로"라고 하는데, `onCleared()`는 더 이상 캐시를 정리하지 않는다(같은 파일의 onCleared KDoc이 그 변경을 설명함). **주석-코드 불일치 1건** — 동작에는 영향 없어 이번 안정성 수정에 섞지 않음
- `awaitPendingStyleSaves()`의 style-save Job 19개는 상한 초과 시 여전히 ViewModel 소멸과 함께 취소됨(P0-2 항목 참조)

## 의도적으로 건드리지 않은 영역 (§34)

템플릿 기능 존폐, `ExampleUnitTest`, 방문 정의 A/B/C 결정(`recordTodayVisit` 무변경), `OrphanFileDiagnostics` UI 연결, 새 저장공간 관리 UI, 대규모 architecture 변경, 새 dependency, Room schema/Migration.

## 변경 파일

production 수정 12 + 신설 6:

| 파일 | 이유 |
|---|---|
| `utils/PostcardDraftStorage.kt` | P0-1 읽기 실패와 손상 분리 |
| `di/ExitSaveScopeModule.kt` (신설) | P0-2 이탈 저장 전용 scope |
| `ui/detail/ExitSaveTimeout.kt` (신설) | P0-2 대기/저장 생명주기 분리 |
| `ui/detail/DetailViewModel.kt` | P0-2 scope 주입·flush 교체·sweep 상한, P3-7 소유권 판정 3곳 |
| `data/PostcardDao.kt` / `data/PostcardRepository.kt` | P1-3 묶음 단일 UPDATE |
| `ui/futuremail/FutureMailOpeningGuard.kt` (신설) | P1-3 finally 보장 가드 |
| `ui/futuremail/FutureMailboxViewModel.kt` | P1-3 가드·묶음 개봉, P2-6 자정 combine |
| `ui/gallery/VisitCalendarDrawer.kt` | P1-4 표시 월 로딩 + 선택 규칙 |
| `utils/ImageUtils.kt` | P1-5 부분 출력 파일 제거 |
| `utils/ProvisionalFile.kt` (신설) | P1-5 커밋 전 임시 소유 규칙 |
| `ui/camera/CameraViewModel.kt` | P1-5 provisional 정리 + 취소 처리 |
| `utils/DayBoundary.kt` (신설) | P2-6 자정 경계 공유(순수 함수 이동 + tick Flow) |
| `ui/gallery/GalleryCurrentDate.kt` | P2-6 옮긴 함수 재사용 |
| `ui/futuremail/FutureMailLogic.kt` | P2-6 `daysLeft`를 그룹에 포함 |
| `ui/futuremail/FutureMailboxScreen.kt` | P2-6 화면에서 시각 재조회 제거 |
| `ui/detail/DetailScreen.kt` | P2-6 봉인 화면 날짜 기준 |
| `utils/AppFileOwnership.kt` (신설) | P3-7 경계 확인 소유권 판정 |
| `utils/PhotoStickerImageStorage.kt` / `utils/MaskingTapePhotoStorage.kt` | P3-7 적용 |

테스트 신설 7파일 / 수정 2파일(`PostcardDraftStorageTest` +5, `VisitCalendarTest` import), androidTest 1파일(`PostcardBackSaveTest` 생성자 인자 추가 — 계측 테스트라 **실행하지 않음**).

## 자동 검증 (오늘 실제 실행)

- `assembleDebug`: **BUILD SUCCESSFUL**, `app-debug.apk` 생성 확인
- `testDebugUnitTest`: **741건 / 실패 0 / 에러 0 / skip 0**, 80 클래스 (test-results XML 직접 집계)
- 소스 `@Test` 수 **741 = runner 741** 정확히 일치 (79 파일 / 80 클래스 — `FutureMailOpeningGuardTest.kt` 한 파일에 클래스 2개)
- 기준선 685 → **741 (+56)**
- `git diff --check`: clean (새 파일 포함 확인. `ProvisionalFileTest` EOF 빈 줄 1건 발견 후 수정)
- 경고: 기존 18건 그대로, 새로 추가된 경고 없음
- **P0-1 실패 실증**: 수정 전 동작 복원 → 새 테스트 4건 실패 확인 → 수정본 복구 → TEMP 잔재 0건 → 전체 재검증 741/0

## 실기기 QA — **통과** (사용자 "실기기 확인완료!")

자동 검증으로 확인할 수 없는 것: 실제 Room DB 위에서의 묶음 개봉, 실제 filesDir의 marker 파일 읽기, 실제 navigation/ViewModel 소멸 타이밍, CameraX 촬영 경로. 최소 확인 항목:

1. 상세 편집(스티커/도장 등) 후 **즉시 뒤로가기** → 다시 들어갔을 때 마지막 수정이 남아 있는가
2. 미래 우체통에서 같은 날짜 묶음 **열어보기** → 전부 갤러리로 돌아오는가, 메시지가 1회만 뜨는가
3. 방문 달력에서 **과거 월로 이동** → 그 달에 실제로 방문한 날에 marker가 보이는가, 다시 현재 월로 오면 정상인가
4. 새 사진 촬영·저장 → 정상 생성/표시되는가

**기기 날짜 변경 금지** — 시간 경계는 자동 테스트로만 검증했다.

사용자가 위 4개 항목을 실기기에서 확인하고 "실기기 확인완료!"로 통과 보고했다(2026-09-19).

## 미검증 / 남은 위험

- **자정 자연 통과 시 실제 갱신**(방문 달력·기억밀도·미래 우체통·봉인 상세) — 기기 날짜 조작 금지라 자연 발생 확인만 가능. 재개 조건: 앱을 켜 둔 채 실제 자정을 넘긴 뒤 각 화면 확인
- **묶음 개봉의 DB 원자성** — instrumented test 필요, 실기기 보호 원칙으로 미실행
- **style-save Job 19개의 ViewModel 소멸 시 취소** — P0-2에서 초안 flush만 해결, 나머지는 미해소
- `androidTest` 7건 — 이번에도 미실행(실기기 보호)

## Git 상태

사용자가 실기기 QA 통과("실기기 확인완료!") 후 commit·push를 요청해 실행했다.

- commit: **완료** — `3300edb` "Harden draft, exit-save, group-open, calendar, image and path safety" (31 파일, +1798 / −128). `git add .`/`-A` 없이 31개 파일을 명시 stage했고, stage 직후 unstaged diff가 비어 있음을 확인해 staged 내용이 작업트리와 정확히 일치함을 검증했다.
- push: **완료** — `origin/feature/photo-sticker` (`a0352b6..3300edb`). push 후 `git fetch` → `git rev-list --left-right --count` 결과 `0 0`(local == origin).
- 기존 무관 untracked(`.codex-config.candidate.toml`, `.kotlin/`)는 stage하지 않았고 그대로 남아 있다.
- 이 Git 절 갱신 자체는 78일차와 동일한 관례에 따라 별도 후속 commit으로 기록한다.

## 후속 후보 (승인된 작업 아님)

1. `awaitPendingStyleSaves()`의 style-save Job 19개를 이탈 후에도 완료되는 scope로 옮길지 — 각 Job의 취소 의미 재검토 필요
2. `isInsideDirectory`를 이미 올바른 2곳(`deleteIfOwnedByApp`, `cleanupPostcardOwnedAssets`)까지 통합할지
3. `promoteDraftStickerBackgrounds`의 onCleared 관련 주석-코드 불일치 1건 정리
4. (78일차에서 이월) 방문 정의 A/B/C 결정, 템플릿 기능 존폐, `OrphanFileDiagnostics` 읽기 전용 연결, `vibrate*` 3종 통합, Compose UI 테스트 하네스(새 dependency 필요)

---

# HANDOFF — 78일차: 코드 클린 데이 + 자정 갱신 버그 + 장례식 + 무효 테스트 수리 + 고아 파일 조사

확인일: 2026-09-19. 수동 표준 모드. 사용자 제공 78일차 "코드 클린 데이" 지시서에 따라 피코(Claude Code)가 실측→기준선→감사→저위험 정리→검증 순으로 진행했어. **새 기능·새 UI·디자인 변경 없음.** 이어서 사용자가 "78일차 후속 작업지시서"(1차 감사에서 발견한 자정 갱신 버그 수정 + 2차 클리닝)를 붙여넣어 같은 세션에서 계속 진행했어. 1차 결과는 되돌리지 않고 그대로 보존했고, 후속에서 **실제 동작 결함 1건을 수정**했어. 그 뒤 세 번째 지시서("시간 경계 버그 감사 + 죽은 코드의 장례식")로 시간 경계 staleness 2건을 마저 판정하고 저장소 전체 죽은 코드 감사를 했어. 마지막으로 네 번째 지시서("무효 테스트 수리 + 방문 정의 조사 + 잔여 코드 판정")로 **통과하지만 아무것도 지키지 않던 테스트를 실제로 실패할 수 있는 테스트로 고쳤어**. 마지막으로 다섯 번째 지시서("고아 이미지 파일 조사")로 이미지 파일의 수명을 끝까지 추적했고, **조사 결과 수정할 것이 없다는 결론**이 나왔어(근거는 아래). 실기기 smoke QA 대기, commit·push 미실행.

## 시작 Git 상태 (실측)

branch `feature/photo-sticker`, 시작 HEAD `f104cad`, local == origin (ahead 0 / behind 0), tracked working tree clean. **77일차 세션의 PC 강제종료로 유실된 변경은 없음** — 77일차 작업은 `fdbc1b1`+`f104cad` 두 커밋에 전부 반영돼 있고 working tree에 미커밋 잔여물 없음. 기존 무관 untracked(`.codex-config.candidate.toml`, `.kotlin/`)는 건드리지 않음.

## 기준선 (오늘 실제 실행, 과거 값 재사용 아님)

- `compileDebugKotlin` + `testDebugUnitTest`: **BUILD SUCCESSFUL**
- **677건 전부 통과 / 실패 0 / 에러 0 / skip 0** (test-results XML 직접 집계, 72 클래스)
- 경고 18건 전부 기존 것: Room Migration `db` 파라미터명 13건(`PostcardDatabase.kt`), deprecated API 5건(`CameraScreen.kt` 4, `DetailScreen.kt` 1). 최근 변경 파일엔 경고 0건.

## 리팩토링 사전 감사 결과

**dead code: 최근 변경 영역엔 없음.** 77일차 추가 심볼 25개 전부 실사용, unused import 0건(`getValue`/`setValue`·alias import는 전부 필요), 주석처리된 옛 구현 0건, TODO/FIXME 0건, 옛 58% 폭 제한 코드 잔재 0건(주석에만 남아 있었음 — 아래에서 수정).

### 실제로 정리한 것 (A: 저위험, 동작 보존 확인)

production 3파일:

1. `GalleryScreen.kt` — **주석-코드 불일치 수정**. "시계 폭은 GalleryRetroClock 내부에서 58%로 제한"이라 적혀 있었지만 v3에서 그 제한을 완전히 제거했음. 현재 실제 동작(`IntrinsicSize.Min`으로 바디가 글자 폭에 맞춰 닫힘)을 설명하도록 고침. 코드 변경 없음.
2. `GalleryRetroClock.kt` — `RETRO_CLOCK_CUP_SIZE` 선언이 `GalleryRetroClock`의 KDoc과 함수 선언 **사이에 끼어 있어** 시계 전체를 설명하는 KDoc이 엉뚱하게 그 상수에 붙어 있었음. 상수를 KDoc 위로 옮김(값·순서 의존성 없음).
3. `GalleryRetroClock.kt` — 12시간제 변환식(`hour % 12`, 0→12)이 `retroClockTimeTextFor`와 `retroClockAccessibilityDescriptionFor`에 **완전 중복**돼 화면 표시와 접근성 설명이 갈라질 수 있었음. `retroClockHour12()` private 순수 함수로 통합.
4. `GalleryRetroClock.kt` — LCD 패널색만 인라인 `InkSecondary.copy(alpha = 0.10f)`로 남아 같은 파일의 색 명명 규칙(`RetroClockSegmentOffColor` 등)에서 이탈해 있었음. `RetroClockPanelColor`로 명명(값 동일).
5. `VisitCalendarDrawer.kt` — MONTH_PICKER/YEAR_PICKER의 "오늘 복귀 링크"가 스타일·동작까지 **완전히 같은 사본 2개**였음. `VisitCalendarPickerTodayReturnLink` 공용 composable로 추출. **CALENDAR 레벨의 "오늘"(표시 월 자체를 되돌리고 햅틱까지 울림)은 의미가 다른 동작이라 의도적으로 합치지 않았고, 그 이유를 KDoc에 남김**(11절 원칙 보존).
6. `VisitCalendarDrawer.kt` — 요일→주말색 매핑이 `visitDateColor(date)`와 상단 요일 머리글의 인라인 `when (dow)` **두 곳에 중복**. `visitDayOfWeekColor(dayOfWeek)`로 통합하고 `visitDateColor`가 이를 위임하게 함. 요일 머리글 `listOf(...)`도 매 recomposition 재생성 대신 파일 상수 `VISIT_CALENDAR_WEEKDAY_HEADERS`로.

테스트 16파일(신규 1 + 수정 15):

7. `private fun readSource(candidates: List<String>)`가 **바이트 단위로 동일한 사본 15개**로 복제돼 있었음. `testsupport/StructureTestSource.kt`의 `readStructureTestSource()` 하나로 통합하고, 불필요해진 `import java.io.File`도 각 파일에서 제거. **-207 / +43줄**, assertion은 단 하나도 바꾸지 않음.

### 의도적으로 보류한 것 (B: 중위험)

- `vibrateGalleryFab` / `vibrateVisitCalendarTodayReturn` / `vibrateIntroPostmark` 3종 통합 — 각 호출부가 의도적으로 다른 강도(10ms/90, 14ms/120, 24ms/175)로 튜닝됐고 주석에 근거가 남아 있음. 3파일 교차 수정이라 오늘 범위 밖.
- `extractBalancedCall` ×4, `readSource` 변형 ~9개 — 시그니처가 서로 달라 호출부 의미까지 확인해야 함.

### 오늘 금지로 분류한 것 (C: 고위험)

- **구조 테스트 129건 / 24파일이 production 소스 텍스트를 읽어 검증** — 전체 677건의 19%. 지시서 27절이 말한 "implementation detail 결합"의 최대 사례이고, 리팩토링의 실질적 브레이크다. 근본 원인은 Compose UI 테스트 하네스 부재이고 해결하려면 새 dependency가 필요 → **STOP, 후속 후보로만 기록**.
- `DetailScreen.kt`(6422줄) / `DetailViewModel.kt`(4529줄) — 제1~10차 감사로 2026-08-08 공식 종료된 영역. 재분해하지 않음.
- Room Migration `db` 파라미터명 경고 13건 — AGENTS 5절 보호 대상. 손대지 않음.
- `CameraScreen.kt` deprecated API 경고 4건(`ArrowBack`, `LocalLifecycleOwner`, `rememberTransformableState`) — 범위 밖 + UI 영향 가능.

## Compose 상태 / recomposition 구조 감사

- **시계 ticker는 건강함**: `now` state가 `GalleryRetroClock` 내부 `remember`에만 있고 그 composable 안에서만 읽히므로, 매초 무효화되는 건 시계 scope뿐이고 `GalleryScreen` 전체는 재구성되지 않음. `LaunchedEffect`가 lifecycle-aware recomposer 위에서 돌아 백그라운드에서 자동 정지. 별도 조치 불필요.
- **달력 상태 소유권도 건강함**: `displayedMonth`(선택)와 `pickerYear`(보기 창)는 의도적으로 분리돼 있고(9절 원칙), `decadeStart`·`todayYearMonth`·`isCurrentMonth`는 저장하지 않고 파생 계산만 함 — 중복 state 없음. `navLevel`은 `VisitCalendarDrawer`가 단일 소유하고 hoisting으로 내려줌.
- **발견(오늘 수정 안 함)**: `MonthlyVisitCalendar`의 `today: LocalDate = remember { LocalDate.now() }`는 key 없는 `remember`라 composition이 살아 있는 동안 갱신되지 않음. 앱을 켜 둔 채 자정을 넘기면 **옆의 시계는 새 날짜로 넘어가는데 달력의 "오늘" marker/색은 어제에 머문다**. 영향은 작지만 실재하는 동작 차이이고, 고치려면 갱신 방식에 제품 판단이 필요해 후속 후보로만 남김.

## 테스트 인벤토리 (오늘 실제 집계)

- **전체 실행 건수 677 / 실패 0 / 에러 0 / skip 0**, 테스트 클래스 72개
- **소스 `@Test` 수 677 = runner 실행 677건으로 정확히 일치** — parameterized test·`@RunWith`·`@Ignore` 전무라 1:1이며, 지시서 46절이 우려한 두 숫자의 괴리는 이 저장소에 존재하지 않음
- source set: `test`(unit) 677건 / `androidTest`(instrumented) 7건 4파일 — **androidTest는 실기기 보호 원칙(AGENTS 5절)상 실행하지 않았고, 미실행으로 기록**
- 기능 영역별(패키지 기준): `ui/detail` 330건(36파일) / `utils` 152건(15) / `ui/gallery` 103건(10) / `ui/intro` 37건(2) / `ui/components` 31건(5) / `ui/futuremail` 19건(1) / `data` 4건(2) / 루트 1건(1)
- 가장 큰 파일: `PostcardOverlayExportLogicTest` 52건, `PostcardEditDraftTest` 29, `VisitCalendarTest` 28, `MaskingTapeItemTest` 25, `PostcardDraftStorageTest` 24
- 가장 테스트가 많은 영역: `ui/detail`(전체의 49%) — 프로젝트에서 가장 복잡한 영역이라 자연스러운 분포로 판단

### 테스트 품질 감사

- **implementation detail 결합**: 위 C항목의 구조 테스트 129건. 통과한다고 좋은 테스트는 아니지만, 이 프로젝트에선 Compose UI 하네스의 대체재 역할을 하고 있어 일괄 삭제 대상이 아님. 후속에서 하네스 도입을 검토할 때 함께 정리할 후보.
- **fixture/helper 중복**: `readSource` 15개 사본 → 정리 완료. `extractBalancedCall` 4개는 보류.
- **obsolete 후보**: `ExampleUnitTest.addition_isCorrect`(`assertEquals(4, 2 + 2)`)는 Android Studio 템플릿 잔재로 이 앱의 요구사항을 전혀 보호하지 않음. 다만 삭제 이득이 사실상 0이고 30절의 "애매하면 유지" 기준에 따라 **유지**하고 기록만 남김.
- **경계값 공백(후속 후보)**: 시계의 "다음 초 경계까지 delay" 계산(`1000 - nano/1_000_000`)이 `LaunchedEffect` 안에 인라인이라 순수 함수로 검증 불가. 현재 로직에 실제 결함은 없어 오늘 손대지 않음.
- 테스트 이름은 전반적으로 `대상_조건_기대` 형태로 읽히며, 일괄 rename이 필요한 모호 사례는 없었음.

## 문서-코드 정합성 감사 (36절)

- 77일차 HANDOFF의 기술 내용은 현재 코드와 일치. 기록된 "677건"도 오늘 재실행 결과와 일치.
- 유일한 불일치는 `GalleryScreen.kt`의 58% 주석이었고 위에서 수정함.
- `docs/ai/mockups/gallery-retro-clock-mockup.html`: 같은 폴더에 74~77일차 목업 6개가 커밋돼 유지되는 **레퍼런스 컬렉션**이고, 7세그먼트 글리프의 설계 근거 자료라 일회성 찌꺼기로 볼 근거 없음 → **유지, 조치 없음**(34절 판단).

## 자동 검증 (정리 후)

- `compileDebugKotlin`: **BUILD SUCCESSFUL**(1차 후 1회, 자정 수정 후 2회). 장례식 뒤에는 resource merge·packaging까지 포함한 **`assembleDebug` 전체 빌드로 재검증 — BUILD SUCCESSFUL**, 내가 만진 파일에서 신규 경고 0건.
- 중간에 자정 ticker를 공용 파일로 옮기는 스크립트가 `MonthlyVisitCalendar` 선언을 한 줄 중복시켜 컴파일이 1회 깨졌고(`assembleDebug` 실패), 중복 줄을 제거해 복구한 뒤 다시 전체 빌드로 확인했다.
- 전체 unit test: 장례식 후에도 **683건 전부 통과 / 실패 0 / 에러 0**, 클래스 72개(삭제한 production 코드에 걸린 테스트가 없었다는 확인). 1차 정리 직후 **677건 전부 통과**(기준선과 완전히 동일 — 테스트를 지우지도 늘리지도 않음). 자정 버그 수정 후 회귀 테스트 6건을 더해 **최종 683건 전부 통과 / 실패 0 / 에러 0**, 클래스 72개. 소스 `@Test` 683 = runner 683으로 여전히 일치.
- `git diff --check`: 통과(기존 LF→CRLF 경고만)
- 전체 diff 재검토 완료: production 변경은 색상값·크기·간격·문자열이 **전부 동일**하고 순수 추출·이동·주석뿐. 테스트 변경은 assertion 무변경.
- **미검증**: (1) 실제 화면 렌더링 결과 동일성 — Compose UI 테스트 하네스가 없어 자동 확인 불가. (2) **실제 자정 통과 시의 화면 갱신** — 순수 함수(대기 시간 계산)와 판정 로직은 테스트로 덮었지만, `LaunchedEffect`가 실기기에서 실제로 자정에 깨어 화면을 다시 그리는지는 자동 검증 불가. 지시서 13절에 따라 **기기 날짜를 강제로 바꾸지 않았고**, 자연스럽게 자정을 넘길 때 확인할 항목으로 남긴다.

## 후속 — 자정 갱신 버그 수정 (실제 동작 결함)

1차 감사에서 발견만 하고 남겨뒀던 항목을 사용자 지시로 이어서 수정했어.

**증상**: 앱을 켜 둔 채 자정을 넘기면 같은 화면 안에서 현재 날짜 기준이 갈라졌다. 레트로 시계는 매초 `LocalDateTime.now()`를 다시 읽어 새 날짜로 넘어가는데, 방문 달력의 "오늘" marker·오늘 채움색·picker의 현재 월/연도 판정·오늘 복귀 링크는 전날에 머물렀다.

**원인**: `MonthlyVisitCalendar(today: LocalDate = remember { LocalDate.now() })` — key 없는 `remember`라 composition이 살아 있는 동안 한 번 잡은 값이 갱신되지 않았다. 호출부는 `VisitCalendarDrawer` 한 곳뿐이고 `today`를 넘기지 않아, 이 기본값이 곧 전체 동작이었다.

**수정 방식**: 기본값을 `rememberTodayDate()`로 교체.

- 시계처럼 매초 깨우지 않고 **다음 자정까지 한 번만 기다린다**(하루 1회). 고빈도 polling 없음.
- 대기 시간은 순수 함수 `millisUntilNextMidnight(now: LocalDateTime)`로 분리 — 정확히 자정이어도 0이 아닌 하루치를 돌려줘 바쁜 루프를 막는다. **덕분에 새 dependency나 시간 주입(Clock/time provider) 없이 단위 테스트가 가능해졌다.**
- 깨어날 때마다 실제 현재 시각을 다시 읽으므로 기기 절전으로 타이머가 늦게 깨도 그 시점의 올바른 날짜로 스스로 맞춰진다.
- `LaunchedEffect`는 Activity의 lifecycle-aware recomposer 위에서 돌아 별도 lifecycle 처리 불필요.
- **시계의 1초 ticker와 상태를 합치지 않았다** — 갱신 주기와 UI 생명주기가 달라, 중복 제거를 이유로 강제 통합하지 않는다는 지시서 5·6절 판단.

**자정 이후 바뀌는 것**: 오늘 marker, 오늘 채움색(`VisitFillColorToday`), MONTH_PICKER/YEAR_PICKER의 현재 월·연도 강조, 오늘 복귀 링크 노출 조건과 문구, CALENDAR 레벨 "오늘" 버튼의 목표 월.

**자정 이후 바뀌지 않는 것**: 사용자가 보던 월(`displayedMonth`), picker 위치(`pickerYear`), `navLevel`, 방문 기록, 색 팔레트, 햅틱, 월 이동 애니메이션, swipe, 오늘 복귀 동작의 의미. 이 둘은 `displayedMonth`/`pickerYear`가 **key 없는 `rememberSaveable`**이라 구조적으로 보장되며, 미래에 누가 key를 추가해 원칙을 깨지 않도록 그 이유를 코드 주석으로 남겼다.

### 2차 클리닝 (이번 수정에 닿은 범위만)

닿은 코드를 다시 훑었고 **새로 생긴 중복은 없었다**: 중복 `LocalDate.now()` 없음(`rememberTodayDate` 내부의 초기값·갱신 2회가 전부), 중복 current date 계산 없음(`todayYearMonth`를 한 번 계산해 전부 재사용), 불필요한 state 없음, magic number 없음. 실제로 추가한 정리는 위의 `rememberSaveable` 의도 주석 1건.

### 추가한 테스트 (+6, 677 → 683)

`VisitCalendarTest`에 자정 회귀 방지 6건:

- `millisUntilNextMidnight` 카운트다운(23:59 / 밀리초 단위 / 정오)
- **정확히 자정일 때 0이 아닌 하루치**를 돌려주는지(바쁜 루프 방지) + 월말·윤년 경계에서 항상 양수
- 월 경계(9/30 23:59:59)·연 경계(12/31 23:59:59) 카운트다운
- 날짜가 하루 넘어가면 현재 월 판정이 함께 넘어가는지(9/30 → 10/1)
- 새해 자정에 현재 연도 판정이 넘어가는지(2026/12/31 → 2027/1/1)
- 넘어간 날짜가 picker 창 밖으로 나가면 오늘 복귀 링크 조건이 켜지는지(2026년 picker의 버퍼 끝 2027/4/30 → 5/1)

> 테스트 작성 중 내 예상이 틀린 게 하나 있었어: 2026년 MONTH_PICKER는 다음 해 1~4월을 버퍼로 보여주므로 2027년 1월은 아직 **창 안**이라 새해 자정만으로는 링크가 뜨지 않아. 실제 경계(2027/4/30 → 5/1)로 고쳐서 검증했어.

### 시간 주입 판단 (지시서 11절)

`Clock`/time provider 도입은 **하지 않았다.** 대기 시간 계산을 순수 함수로 떼어내는 것만으로 이번 결함의 회귀 보호가 가능했고, 앱 전역 time abstraction·모든 `LocalDate.now()` 교체·DI 확장은 오늘 범위를 넘는다(STOP 기준). `LocalDate.now()` 자체를 주입 가능하게 만드는 건 후속 후보로만 남김.

### 같은 부류로 새로 발견한 것 (오늘 수정 안 함)

- `GalleryScreen.kt`의 `GalleryDensityPage`: `val year = remember { YearMonth.now().year }` — 같은 staleness 패턴이고 경계는 **연말**이다. 다만 기억밀도 화면은 이번 수정이 닿지 않았고 UI freeze 대상이라, 연 경계에서 어떻게 동작해야 하는지도 제품 판단이 필요해 후속 후보로만 기록.
- `MainActivity.kt`: `VisitHistoryStorage.loadMonth(..., YearMonth.now())`가 `LaunchedEffect(Unit)`으로 프로세스당 1회만 실행 — 앱을 켜 둔 채 **달을 넘기면** 새 달의 방문 집합이 로드되지 않을 수 있다. 방문 기록 데이터 흐름이라 별도 조사·승인이 필요(AGENTS 5절 데이터 보호 영역).

## 추가 — 시간 경계 감사 2건 판정

### GalleryDensityPage 기준 연도 → **실제 버그, 수정함**

- **판정 근거**: 이 화면에는 연도 선택 UI가 전혀 없다(좌우 pager + 점 indicator뿐, indicator에는 contentDescription도 없음). 따라서 `year`는 "사용자가 고른 연도"가 아니라 순수하게 "지금 몇 년인가"다 — 지시서 3절이 경계한 두 의미의 혼동이 애초에 성립하지 않는다.
- **증상**: `remember { YearMonth.now().year }`가 key 없는 remember라, 앱을 켜 둔 채 연말 자정을 넘기면 제목("2026년")과 12칸 화단이 지난해에 머문다. 프로세스를 다시 시작하면 정상으로 돌아오므로, 지금 동작은 설계가 아니라 사고다.
- **조치**: 달력과 같은 기준인 `rememberTodayDate().year`로 교체. 디자인·레이아웃·색은 전혀 건드리지 않았다.
- **제품적 귀결(보고 대상)**: 연말 자정을 넘기면 화단이 새해 기준으로 바뀌어 한동안 비어 보인다. 프로세스 재시작 시 어차피 그렇게 동작하므로 기존 동작과 일치하지만, "지난해 화단을 계속 보고 싶다"가 의도라면 연도 선택 UI가 필요한 별개 기능이 된다.

### MainActivity `loadMonth(YearMonth.now())` → **수정하지 않음 (제품 판단 필요 + 데이터 영역)**

조사해 보니 이건 단순 staleness가 아니라 "방문"의 정의 문제였다.

- `visitedEpochDays`는 **현재 월 하나만** 담는다. 그래서 달력에서 다른 달로 넘어가면 원래부터 방문 채움이 안 보인다 — 자정과 무관한 기존 설계 경계다.
- 앱을 켜 둔 채 월 경계를 넘겨도 사용자가 보던 달(`displayedMonth`)은 그대로라 화면은 여전히 정확하다. 어긋나는 건 새 달로 이동했을 때뿐이다.
- 더 근본적으로 `recordTodayVisit`도 `AppIntroState.todayVisit != null`이면 건너뛰어 **프로세스당 한 번만** 실행된다(코드 주석에도 "프로세스 시작마다 한 번만"이라 명시). 즉 앱을 켜 둔 채 자정을 넘기면 **새 날의 방문 자체가 기록되지 않는다.**
- 따라서 이건 "방문 = 앱을 실행한 단위인가, 날짜 단위인가"라는 제품 정의 문제이고, 손대면 `filesDir/visits` 기록과 `totalVisitDays` 집계에 직접 영향을 준다. AGENTS 5절(사용자 데이터 보호) + 지시서 25절 STOP에 해당해 **조사 결과만 남기고 수정하지 않음.**

## 죽은 코드의 장례식

저장소 전체를 ui/data/utils/resources/tests로 나눠 감사했다. 이름 기반 스캔 두 벌을 교차 검증한 뒤, 후보마다 정의 위치·production 참조·test 참조·resource 참조·git history를 개별 확인했다.

### ⚰️ 사망 확정 (삭제함)

| 대상 | 삭제 이유 |
|---|---|
| `GalleryComingSoonPage` (GalleryScreen.kt, 27줄) | "미구현 보기" placeholder인데 76일차에 보기가 6종→2종으로 줄며 둘 다 구현됨 — 미구현 상태 자체가 사라짐. 호출부는 `7d2f476`에서 제거됐고 함수만 남았다. |
| `GalleryDetailList` (GalleryScreen.kt, 82줄) | 76일차에 삭제된 "세부 기록 보기"의 본체. 호출 0. 구조 테스트는 오히려 `"세부 기록 보기"` 문자열의 **부재**를 검증 중이라 방향이 일치한다. |
| `PostcardDetailRow.kt` (파일 전체, 97줄) | 유일한 실제 호출부가 위 `GalleryDetailList`였다. 남은 참조는 `FutureMailLogic`의 KDoc 링크 하나뿐이라 그 링크도 정리. |
| `import ... lazy.items as lazyColumnItems` | 위 삭제로 유일 사용처 소멸. |
| `PondController.clearRipples()` | 호출 0. `reset()`이 `ripples.clear()` + `lastImpulse = null`로 상위 집합이고 실제로 쓰인다. |
| `GalleryViewModel.isDeleting` (public StateFlow 1줄) | 구독자 0. 삭제 재진입을 막는 내부 가드 `_isDeleting`은 그대로 유지했다(데이터 안전 장치라 손대지 않음). |
| `PostcardImageStorage.copyToAppStorage` + private `getFileExtension` (108줄) | URI를 원본 확장자 그대로 복사하던 옛 사진 반입 경로. `7b3edd9`(사진 편집 UI 단순화)에서 마지막 호출부가 사라졌고, 지금은 `ImageUtils`가 정사각형으로 크롭해 같은 `filesDir/postcards/`에 저장한다. 메서드 참조(`::`)·test 모두 0. **이미 저장된 사용자 파일은 이 함수와 무관하게 그대로 남는다.** |
| 위 삭제로 죽은 import 4개 | `Uri`/`IOException`/`UUID`(PostcardImageStorage), `LazyListState`(GalleryScreen). |

### 🩺 생사불명 (보류)

| 대상 | 보류 이유 |
|---|---|
| **템플릿 기능 전체** — `PostcardTemplateRow.kt`(341줄), `BuiltInTemplates.kt`, `PostcardTemplateStorage`, `Repository/Dao.updatePostcardTemplateStyle` | **어떤 화면에서도 진입점이 없다**(`DetailScreen.kt`에 "Template" 문자열 0건). 그런데 DAO·Repository·저장소·내장 템플릿 데이터·테스트(`PostcardTemplateTest`, `BuiltInTemplatesTest`, `PostcardTemplateStorageTest`)는 전부 살아 있고 Room 컬럼까지 얽혀 있다. 이건 "남은 찌꺼기"가 아니라 **UI가 연결되지 않은 기능 한 벌**이라, 미완성인지 의도적으로 내려둔 것인지 사용자 판단 없이는 묻을 수 없다. 25절 STOP(DB schema·사용 여부 판단 불가). |
| `GalleryPageFormat.label` | `GalleryComingSoonPage`를 묻으면 독자가 0이 된다. 다만 76일차가 이 enum 파일을 직접 고쳐 쓰면서도 `label`은 남겼고("월별 보기"/"기억 밀도 보기"), 저장되는 enum의 생성자를 바꾸는 일이라 보류. 점 indicator에 contentDescription이 없는 접근성 공백을 메울 때 자연스럽게 쓰일 자리이기도 하다. |
| `textStickerColors` (Color.kt) | 코드 참조 0. 글자색이 고정 팔레트에서 커스텀 컬러 피커로 옮겨가며 남은 것으로 보이나, 같은 파일 `sealSelectableInkColors`의 주석이 "흰색 제외" 규칙의 근거로 이 목록을 인용하고 있어(팔레트 설계 의도 기록) 지우려면 그 문서화를 어디로 옮길지 함께 정해야 한다. |
| `ExampleUnitTest.addition_isCorrect` | 12절 재판정 결과도 1차와 같다 — 제품을 전혀 보호하지 않는 템플릿 테스트가 맞지만, 삭제 이득이 사실상 0이고 "단독 1건 때문에 별도 구조 변경은 하지 않는다"는 12절 단서에 맞춰 유지. |

### 👻 유령처럼 보였으나 현역 (건드리지 않음)

| 대상 | 실제 역할 |
|---|---|
| `PondImpulse.sequence` | **아무도 값을 읽지 않는데 지우면 기능이 깨진다.** `lastImpulse`가 `mutableStateOf`(기본 structural equality)라, 이 값이 없으면 같은 카드를 같은 자리에서 다시 발사할 때 모든 필드가 같아 "변경 없음"으로 무시된다. 발사마다 증가하는 이 값이 두 충격을 구분해 구독자에게 전달되게 한다. → **그 이유를 KDoc에 명시해 다음 청소 때 오해받지 않게 했다.** |
| `provideDatabase`, `providePostcardDao` | Hilt `@Provides` — 생성 코드가 호출. |
| `onImageSaved`(CameraX), `createOutline`(Shape), `isSelectableDate`(SelectableDates), `onSensorChanged`/`onAccuracyChanged`(SensorEventListener) | 전부 프레임워크 인터페이스 `override`. 이름으로 호출되지 않을 뿐 현역. |
| 모든 `@Test` 함수 | JUnit이 reflection으로 호출 — 이름 기반 스캔에서 대량으로 "미사용"으로 잡히지만 전부 현역. |
| resource 11개 전부 | `ic_camera_button`·seal 4종·launcher 아이콘·`file_paths`·`app_name` 모두 Kotlin/XML/Manifest에서 실제 참조 확인. **미사용 resource 0건.** |

### ⚠️ 죽은 코드인 줄 알았는데 테스트 구멍이었던 것 (삭제하지 않음, 보고)

`BackgroundColorSaveRaceTest`의 `FakeFileSystem.delete()`가 **한 번도 호출되지 않는다.** 그런데 이 함수가 `deletedFiles`에 값을 넣는 유일한 경로이고, 테스트 4곳이 `assertTrue(vm.files.deletedFiles.isEmpty())`로 단언한다 — 즉 **그 4개 단언은 무조건 참이라 절대 실패할 수 없다.** 배경색 저장 경합 중 배경 이미지 파일이 지워지지 않는지를 지키는 척하지만, production이 파일을 다 지워도 통과한다.

77일차 "막스 33일차" 사건과 같은 부류(테스트가 요구사항이 아니라 자기 자신을 보호)이고, 하필 데이터 안전 영역이다. 죽은 코드로 묻으면 4개 단언이 빈 껍데기인 채 남으므로 **삭제하지 않고 그대로 뒀다.** fake를 실제 삭제 경로에 연결하는 건 새 테스트 작성이고, 연결했을 때 실패한다면 production 결함 조사로 이어지므로 별도 작업으로 남긴다.

### 삭제 요약

- production 코드: **약 316줄 삭제**(파일 1개 제거 포함), 정리된 import 5개, KDoc 링크 1개 정리
- test 코드 삭제: **0줄** (삭제 기준을 확실히 만족하는 대상이 없었음)
- resource 삭제: **0개** (전부 현역)
- 주석 처리된 옛 구현·TODO·FIXME: **0건**(main 전체 재확인)

## BackgroundColorSaveRaceTest 수리 — 무효 단언 실효화

### 실제 원인

`FakeFileSystem.delete()`가 어디서도 호출되지 않았다. 이 함수가 `deletedFiles`를 채우는 **유일한** 경로였으므로 `deletedFiles`는 영원히 빈 리스트였고, `assertTrue(deletedFiles.isEmpty())` 4건은 무조건 참이었다. 같은 이유로 `existingFiles`에서 제거하는 경로도 없어 `assertTrue(files.exists(...))` 5건도 항상 참이었다 — **FakeFileSystem 전체가 죽은 계측이었다.**

원인을 production까지 따라가 보니 더 근본적인 사실이 나왔다: **`PostcardImageStorage.deleteIfOwnedByApp`는 production 호출부가 0개다.** 즉 지금 앱에는 배경 이미지 파일을 지우는 경로가 아예 없고, replica는 그 "삭제 없음"을 하드코딩해 재현하고 있었다. 그래서 production에 삭제가 생기더라도 replica는 그대로라 테스트가 잡아주지 못하는 구조였다.

### 수정 방식

production `updateBackgroundColor`의 주석이 위험 지점을 이미 정확히 명명하고 있다 — *"호출 당시 캡처한 경로만 보고 지우면 그 사이 다시 참조된 파일을 지울 수 있다."* 이 문장이 지키려는 사고를 replica가 실제로 재현하도록 연결했다.

`FakeViewModel.saveBackgroundImagePath`(경로 컬럼에 쓰는 저장의 대역)가 **교체 성공 후 옛 파일을 정리**하게 했다 — 실제 `deleteIfOwnedByApp`가 맡기로 한 바로 그 역할이다. 정리 규칙은 캡처한 경로를 그대로 지우지 않고 **Mutex 안에서 커밋된 상태가 아직 그 경로를 참조하는지 다시 확인**한 뒤에만 지운다.

- `deletedFiles`에 테스트가 직접 값을 넣지 않는다. 삭제는 저장 흐름이 실행돼야만 일어난다.
- assertion만 바꾸거나 무조건 통과하는 조건으로 교체하지 않았다.

### 무효 단언 4건 처리

| 위치 | 수정 전 | 수정 후 |
|---|---|---|
| 1 `colorSave_preservesExistingImagePathAndFile` | `deletedFiles.isEmpty()` (항상 참) | 그대로 두되, 이제 **삭제가 일어날 수 있는 계측** 위에서의 단언이라 실제 의미를 가진다 |
| 2 `staleColorSave_doesNotWipeNewerImagePath` | 〃 | 〃 (교체 대상이 null이라 삭제가 없는 게 맞다) |
| 3 `failedColorSave_doesNotRollbackNewerImagePath` | 〃 (사실과 다름) | **`assertEquals(listOf(PATH_OLD), deletedFiles)` + `assertFalse(PATH_NEW in deletedFiles)`** — 교체된 옛 파일 정리는 정상 동작이고, 지켜야 할 건 최신 파일이 안 지워지는 것 |
| 6 `cancelledColorSave_...` | 〃 | 그대로(경로 저장이 없는 시나리오라 삭제 0이 맞다) |

### 추가한 테스트 2건 (9 → 11)

- `replacingBackgroundImage_deletesOnlyTheReplacedFile` — **계측 자체가 살아 있음을 보증한다.** 교체 시 옛 파일이 실제로 삭제되고 기록된다. 이 테스트가 없으면 다른 테스트의 "지워지지 않았다"는 다시 공허해진다.
- `staleImageReplacement_doesNotDeleteAFileTheCommittedStateStillReferences` — production 주석이 경고하는 사고를 그대로 재현한다. PATH_OLD→PATH_NEW 교체가 멈춰 있는 사이 사용자가 PATH_OLD로 되돌리고 먼저 커밋되면, 뒤늦게 커밋한 교체가 캡처해 둔 PATH_OLD를 지우면 안 된다.

### 실패 가능성 실증 (지시서 5·7절)

구조 분석만으로 끝내지 않고 **실제로 실패하는지 확인했다.** replica의 정리 규칙만 일시적으로 순진한 버전(`replacedPath != committedPath` 검사 제거 = production 주석이 경고하는 바로 그 형태)으로 바꿔 실행했더니:

```text
tests=11 failures=1
실패: staleImageReplacement_doesNotDeleteAFileTheCommittedStateStillReferences
```

즉 잘못된 삭제가 발생하면 테스트가 실제로 깨진다. 확인 후 즉시 원복했고(`TEMP` 잔재 0건), production 코드는 이 실증 과정에서 **전혀 건드리지 않았다.**

### FakeFileSystem 주변 추가 감사 (지시서 6·20절)

같은 파일 안의 계측을 주석 제외 토큰 집계로 전수 확인했다. `delete`·`exists`·`existingFiles`·`deletedFiles`·`writeLog`·`errors`·`afterWrite`가 **모두 실제로 구동**된다(`writeLog`는 이미 test 9가 단언 중이었다). 남은 무효 계측 없음. 프로젝트 전체 재감사는 하지 않았다.

## 방문 정의 조사 (수정 없음)

### 현재 production이 가장 가까운 정의: **A — "방문 = 앱을 (새로) 연 날"**

코드와 주석이 일관되게 "**연다**"는 말을 쓴다. 추정이 아니라 문서의 실제 표현이다.

- `recordVisit` KDoc: "같은 날 **다시 열면**", "하루에 여러 번 **열어도**", "어제에 이어 **열면**"
- `TodayVisit` KDoc: "**이번 실행이** 오늘의 첫 방문", "소인은 앱을 **열 때마다** 찍히지만"
- `AppIntroScreen` KDoc: "소인은 앱을 **열 때마다** 찍히지만 진동은 이때만"
- `MainActivity`: "방문 판정은 **프로세스당 정확히 한 번만** 하면 되므로(하루 1회 판정 자체는 저장된 날짜가 보장한다)"

### 구조상 드러난 틈

마지막 주석이 핵심이다. "프로세스당 한 번"이 옳으려면 **"새 날 = 새 프로세스"**가 성립해야 하는데, 앱을 켜 둔 채 자정을 넘기면 그 전제가 깨진다. 그 날은 방문 판정이 아예 다시 실행되지 않는다.

정의 A를 엄격히 적용하면 "사용자가 앱을 새로 연 적이 없으니 새 방문이 아니다"가 맞다. 다만 그 날 하루 종일 앱을 써도 기록되지 않고, `currentStreakDays`가 끊길 수 있다(day1에 열어두고 day2를 넘긴 뒤 day3에 재실행 → 간격 2일로 판정돼 연속이 1로 리셋). 이건 정의의 문제이지 계산 버그가 아니다.

**중요**: 78일차의 달력 자정 갱신 수정 때문에 이 틈이 **눈에 보이게 됐다**. 이전에는 달력이 어제를 "오늘"로 표시해 어긋남이 가려졌지만, 지금은 자정 직후 달력이 새 날을 "오늘"로 정확히 표시하면서 그 칸에 방문 표시가 없다. 동작이 나빠진 게 아니라 원래 있던 틈이 정직하게 드러난 것이다.

### 가능한 정의와 영향

| | A. 앱을 새로 연 날 (현재) | B. 그 날짜에 앱이 실행 상태였던 날 | C. 그 날짜에 실제 사용자 활동이 있었던 날 |
|---|---|---|---|
| 자정 통과 | 새 방문 아님 | 자정 순간 새 방문 | resume/조작 시 새 방문 |
| `totalVisitDays` | 현행 유지 | 켜두기만 해도 증가 | 실제 사용일만 증가 |
| `currentStreakDays` | 켜둔 날은 끊길 수 있음 | 끊김 사라짐 | 사용한 날 기준 유지 |
| `visit_record.txt` | 변화 없음 | 자정 타이머가 기록 추가 | foreground 진입에 기록 추가 |
| history marker | 〃 | 새 날 marker 추가 | 〃 |
| Intro 소인·진동 | 다음 실행 때 | 인트로를 안 봤는데 방문만 쌓임(소인과 불일치) | 〃 |
| 33일차 이스터에그 | 실행한 날만 카운트 | 도달이 빨라짐 | 중간 |
| 방문 달력 | 현행 | 자정 직후 칸이 채워짐 | 조작 시 채워짐 |

B는 "인트로를 보지 않았는데 방문이 쌓인다"는 소인 개념과의 충돌이, C는 "활동"의 정의(resume? 탭? 저장?)가 새로 필요하다는 비용이 있다.

### 이번 작업에서 방문 동작 수정 여부 → **수정 안 함**

`recordTodayVisit`·`visitedEpochDays`·history marker·`totalVisitDays` 모두 그대로다. 사용자 결정 후 별도 작업.

## 잔여 생사불명 코드 최종 판정

### `GalleryPageFormat.label` → ⚰️ **사망 확정, 삭제**

- production 직접 참조 0(유일 독자였던 `GalleryComingSoonPage`를 장례식에서 제거), test 참조 0, resource 참조 0
- **저장값 영향 없음을 구조로 확인**: `PageFormatSaver`가 `save = { it.name }` / `restore = valueOf(saved)`로 **enum 상수 이름**만 저장한다. 생성자 인자는 직렬화에 전혀 관여하지 않으므로 `label` 제거는 기존 저장값 복원에 무영향이다.
- 다른 `.label` 호출부들은 전부 다른 enum(pattern/layout/style/type)이라 무관.
- 조치: `label` 파라미터와 두 상수의 문자열 인자 제거.

### `textStickerColors` → ⚰️ **사망 확정, 삭제**

- 코드 참조 0. 글자색은 고정 팔레트가 아니라 `PostcardCustomColorPicker`(자유 색상 선택)로 옮겨갔다 — `TextStickerDetailScreen`이 `PostcardCustomColorPicker`를 직접 쓰고, 고정 팔레트로 남은 건 외곽선용 `textStickerOutlineColors`뿐이다.
- 16절 기준 적용: 주석이 값의 존재 이유는 아니다. runtime 역할 0이므로 삭제하고, 이 목록을 인용하던 `sealSelectableInkColors` 주석에서 "흰색 제외" 규칙 자체는 남기고 인용만 걷어냈다.

### 덤으로 드러난 것 — `pastelColors` ⚰️ 삭제

`textStickerColors`를 지우고 연쇄를 확인하다 발견했다. 엽서 배경색 고정 팔레트인데 참조 0이고, 배경색도 `PostcardCustomColorPicker`로 옮겨간 상태다. **장례식 1차에서 놓친 이유가 교훈적이다** — 이름 기반 스캔이 `textStickerColors` 주석 안의 `pastelColors`라는 글자를 참조로 세어 살아 있는 것처럼 보였다. 이번엔 주석을 제거한 뒤 집계해 잡았다. `Color.kt` 전체를 같은 방식으로 재집계했고 다른 0-참조 값은 없다.

## 이번 회차 검증

- `assembleDebug`(resource merge·packaging 포함) + `testDebugUnitTest`: **BUILD SUCCESSFUL**, 신규 경고 0
- 관련 테스트 `BackgroundColorSaveRaceTest`: **11건 전부 통과**(9 → +2)
- 전체 unit test: **685건 전부 통과 / 실패 0 / 에러 0**, 클래스 72, 소스 `@Test` 685 = runner 685
- `git diff --check`: 통과
- 실패 가능성 실증: 정리 규칙을 일시 약화 → 1건 실패 확인 → 원복

## 고아 이미지 파일 조사 (production 변경 없음)

`deleteIfOwnedByApp` 호출부 0이 실제로 고아 파일을 만드는지 끝까지 추적했다. **결론: 앞으로 새 고아 파일이 생기는 경로는 없다.** 그 함수가 안 불리는 이유는 정리가 빠져서가 아니라 **그 함수가 맡던 기능 자체가 앱에서 사라졌기 때문**이다.

### 이미지 종류별 수명 지도

| 이미지 | 저장 | 교체 | 제거 | 파일 수명 | 판정 |
|---|---|---|---|---|---|
| **중심 사진** `imagePath` | 엽서 생성 시 `CameraViewModel` → `ImageUtils` → `filesDir/postcards/` | **기능 없음** — `imagePath`를 UPDATE하는 Dao/Repository 쿼리가 **아예 존재하지 않는다**(전 소스 확인) | 개별 제거 없음 | 엽서 삭제 시 `PostcardDeletionManager`가 정리 | **정상** |
| **배경 이미지** `backgroundImagePath` | **non-null로 설정하는 호출부가 없다** — 실사용에서 항상 null | 해당 없음 | 해당 없음 | 파일이 애초에 생기지 않음 | **정상(해당 없음)** |
| **사진 스티커 원본** | `PhotoStickerImageStorage.copyToStickerOriginalStorage`(DetailViewModel 2곳) | 스티커 교체·삭제 시 `deleteOriginalIfUnreferenced`(DetailViewModel 2곳) | 〃 | 참조 확인 후 삭제 | **정상** |
| **마스킹테이프 사진** | `MaskingTapePhotoStorage.copyToMaskingTapePhotoStorage` | `deleteIfUnreferenced`(DetailViewModel 2곳) | 〃 | 참조 확인 후 삭제 | **정상** |
| **누끼 디렉터리** `sticker_bgs/<id>/`, `draft_sticker_bgs/<id>/`, `sticker_originals/<id>/` | DetailViewModel | — | — | 엽서 삭제 시 `PostcardDeletionManager`가 id별 디렉터리 재귀 삭제 | **정상** |
| **임시 파일** `postcards_temp/` | 내보내기·편집 중간 산출물 | — | 앱 시작 시 `PostcardTempCleanup.cleanup`(`PostCardMemoryApp`에서 호출) | 자동 정리 | **정상** |

### 경합 안전성 (지시서 12절) — 이미 구현돼 있다

수리한 테스트가 경고하는 규칙(*삭제 직전에 현재 상태가 그 파일을 다시 참조하는지 확인*)이 살아 있는 삭제 경로에 **전부 이미 들어가 있다**:

- `PhotoStickerImageStorage.deleteOriginalIfUnreferenced`: ① `scheme == "file"` 확인 → ② 경로가 `sticker_originals/` 하위인지(소유권) → ③ `remainingStickers`에 같은 원본을 쓰는 스티커가 남아 있지 않은지(참조) → 셋 다 통과해야 삭제
- `MaskingTapePhotoStorage.deleteIfUnreferenced`: 같은 3단 구조
- `PostcardDeletionManager.cleanupPostcardOwnedAssets`: DB에 저장된 경로도 실제 filesDir 하위인지 확인한 뒤에만 삭제하고, 아니면 `failedAssets`에 사유와 함께 남기고 건드리지 않음

즉 과잉 삭제 위험도 현재 구조에서는 보이지 않는다.

### `deleteIfOwnedByApp` 최종 판정 → **B. 과거 구조의 잔재이며 현재는 다른 삭제 경로가 존재**

- production 호출 수: **0**(메서드 참조 `::`도 0). test 참조 7건.
- 원래 역할: 중심 사진 교체 성공 후 이전 파일 정리(KDoc에 그렇게 적혀 있다).
- 사라진 이유: 중심 사진 교체 기능이 `ee5749d`에서 추가됐다가 **`7b3edd9`("Simplify photo editing UI into layout and edit panels")에서 제거**되면서 호출부가 같이 사라졌다. 지금은 `imagePath`를 바꾸는 경로 자체가 없다.
- 현재 실제 역할: 없음. 다만 같은 "앱 소유 파일만 삭제" 판정을 `deleteOriginalIfUnreferenced`·`deleteIfUnreferenced`·`cleanupPostcardOwnedAssets`가 **각자 따로 구현**하고 있다(공용화 후보이지 이번 범위 아님).
- **조치: 삭제하지 않음.** 지시서 26절의 삭제 조건 중 "테스트 참조 없음"을 만족하지 않는다(전용 테스트 7건 존재). 기능이 되살아나면 그대로 쓸 수 있는, 검증된 안전 헬퍼다.

### `OrphanFileDiagnostics` 최종 판정 → **개발 진단 도구 / 미연결**

- 호출 위치: **production 0건.** 테스트(`OrphanFileDiagnosticsTest`)에서만 9회 호출.
- 검사 대상: `postcards/`(중심 사진), `postcard_backgrounds/`(배경), `sticker_bgs/`·`sticker_originals/`·`masking_tape_photos/`·`draft_sticker_bgs/`(id별 디렉터리), 꾸미기 상태 파일 디렉터리들.
- 비교 기준: Room의 `imagePath`/`backgroundImagePath` 집합과 살아 있는 postcardId 집합.
- 실제 역할: **진단만 한다.** 클래스 KDoc에 "파일을 지우거나 옮기지 않는다 — 삭제는 이 도구가 하지 않는 별도 작업이다"라고 명시돼 있고 구현도 목록만 만든다. 사용자에게 노출되는 화면·로그 출력도 없다.
- **조치: 삭제하지 않음.** dead code가 아니라 "아직 아무 데도 연결하지 않은 진단 도구"다. 연결 여부는 제품 판단이고, 16·22절이 금지한 전역 청소기/저장공간 화면과 얽히므로 이번 범위 밖.

### 고아 파일 발생 가능성 정리

| 시나리오 | 판정 |
|---|---|
| A. 배경 이미지 교체 | **해당 없음** — 배경 이미지를 설정하는 경로 자체가 없다 |
| B. 배경 이미지 제거 | **해당 없음** — 〃 |
| C. 중심 사진 교체 | **해당 없음** — 교체 기능이 없다(`imagePath` UPDATE 쿼리 부재) |
| D. 중심 사진 제거 | **해당 없음** — 개별 제거 없음 |
| E. 엽서 삭제 | **정상** — `PostcardDeletionManager`가 중심/배경/누끼/원본/상태 파일까지 정리하고, 실패는 `failedAssets`로 보고 |
| F. 저장 실패·취소·경합 | **정상** — 실패해도 기존 파일을 지우지 않는다(저장 성공 전 삭제 없음). 경합은 위 3단 확인으로 보호 |
| G. 같은 이미지 재선택 | **정상** — 참조가 남아 있으면 `stillReferenced`로 삭제 안 함 |
| H. 저장 중 앱 종료 | **정상** — 임시 파일은 `postcards_temp/`에 남고 다음 실행 시 `PostcardTempCleanup`이 정리 |

**과잉 삭제 위험: 현재 구조에서 발견되지 않음.**

### 기존 고아 파일 (이미 생겼을 가능성) → **처리하지 않음**

`postcard_backgrounds/` 디렉터리는 **어떤 production 코드도 더 이상 쓰지 않는다**(쓰는 곳은 `OrphanFileDiagnostics`의 스캔 대상 지정과 테스트뿐). 과거 배경 이미지 기능이 살아 있던 시절 실기기에 파일이 남아 있을 수 있고, 중심 사진 교체 기능이 있던 시기에 `postcards/`에 교체된 옛 파일이 남았을 수도 있다.

- 17절대로 "앞으로 생기지 않게"와 "이미 생긴 것"을 분리했다. 앞으로는 생기지 않는 것이 확인됐다.
- 기존 파일은 **왜 존재하는지 확실히 모르기 때문에 광역 청소하지 않는다**(16절). 실기기에서 삭제 실험도 하지 않았다.
- 필요하다면 `OrphanFileDiagnostics`를 디버그 경로에 한 번 연결해 **읽기 전용으로 목록만** 확인하는 것이 다음 단계 후보다(삭제 기능 신설은 별개 사안).

### 이번 회차 변경

**production 코드 변경 0줄. 테스트 변경 0줄. 삭제한 dead code 0건.** 지시서 14절의 수정 조건("고아 파일이 생기는 것이 명백한 경우")이 성립하지 않아 조사·보고로 끝냈다. 26절의 추가 dead code 삭제 조건도 `deleteIfOwnedByApp`(테스트 참조 있음)·`OrphanFileDiagnostics`(진단 도구) 모두 만족하지 않았다.

검증은 트리 무변경 상태에서 재확인: `assembleDebug` + `testDebugUnitTest` **BUILD SUCCESSFUL, 685건 전부 통과**, `git diff --check` 통과.

## 실기기 smoke QA

**통과** — 사용자 "실기기 확인완료!".

변경이 UI 렌더링 경로(달력 요일 머리글 색, picker 복귀 링크, 시계 패널색·커피잔 크기 상수, 달력 `today` 공급 방식, 기억밀도 기준 연도)에 닿았지만 Compose UI 테스트 하네스가 없어 자동 확인이 불가능해, **기존 동작 보존만** 최소 확인을 요청했다: 갤러리 시계 표시·초 갱신·7세그, 방문 달력 오늘 marker와 요일 머리글 색, MONTH/YEAR_PICKER marker, 다른 연도 탐색 시 복귀 링크 동작(보기 창만 이동·선택 유지·햅틱 없음), 기억밀도 화면의 "2026년" 표시.

기기 날짜 조작이나 전체 앱 자연 QA 반복은 요구하지 않았다.

**자연 발생으로 남긴 미검증**: 실제 자정을 넘길 때 달력·기억밀도가 실제로 갱신되는지. 순수 함수(대기 시간 계산)와 판정 로직은 테스트로 덮었지만 `LaunchedEffect`가 실기기에서 실제로 자정에 깨는지는 자동 검증 불가이고, 기기 날짜를 강제로 바꾸지 않는다는 원칙에 따라 자연 확인 대상으로 남긴다.

## Git

**commit·push 완료.** 실기기 smoke QA 통과("실기기 확인완료!") 후 사용자가 "커밋하고 푸시해줘"로 명시적으로 요청함.

- 시작 HEAD `f104cad` → 종료 HEAD **`3945caf`** (30 files, +778 / −607)
- `git push origin feature/photo-sticker` 완료, `git fetch` 후 재확인 시 local == origin (ahead 0 / behind 0)
- 관련 파일만 명시적으로 stage(`git add .`/`-A` 미사용). staged 내용과 작업트리가 정확히 일치함을 commit 전에 대조 확인
- tracked working tree clean. 기존 무관 untracked(`.codex-config.candidate.toml`, `.kotlin/`)는 그대로 두었고 stage하지 않음

## 남은 위험

- 실기기에서 달력·시계의 시각적 동일성 미확인(위 QA로 해소 예정)
- 실제 자정 통과 시 달력 갱신은 자연 발생 확인 대상(기기 날짜 조작 안 함)
- 앱을 켜 둔 채 자정을 넘기면 **새 날의 방문 자체가 기록되지 않음**(제품 정의 문제, 미수정)
- 템플릿 기능 한 벌이 UI 미연결 상태로 존재 — 미완성/보류 여부 미확정
- 실기기에 **과거 기능이 남긴 고아 파일**이 있을 수 있음(`postcard_backgrounds/`, 교체 시절의 `postcards/`) — 앞으로 생기지는 않지만 기존 파일은 미확인·미처리
- `OrphanFileDiagnostics`가 만들어져 있으나 아무 데도 연결돼 있지 않아, 실제 기기 상태를 확인할 수단이 현재 없음
- 방문 정의가 미확정이라 자정 이후 방문 기록·연속 방문 끊김이 남아 있음

## 다음 작업 후보 (승인된 실행 아님)

1. **"방문"의 정의 확정**(위 A/B/C 표) 후 자정 이후 방문 기록·월 집합 재로드 작업
2. **템플릿 기능을 살릴지 묻을지 결정** — UI 미연결 상태의 기능 한 벌(341줄 UI + 저장소 + DAO + Room 컬럼 + 테스트)
3. `OrphanFileDiagnostics`를 디버그 경로에 **읽기 전용으로만** 연결해 실기기의 기존 고아 파일 실태 확인(삭제 기능 신설은 별개 사안)
4. 앱 소유 파일 판정("filesDir 하위인가")이 `deleteIfOwnedByApp`·`deleteOriginalIfUnreferenced`·`deleteIfUnreferenced`·`cleanupPostcardOwnedAssets` 네 곳에 각자 구현돼 있음 — 공용화 검토
4. `vibrate*` 3종 공용화 검토
5. Compose UI 테스트 하네스 도입 검토 + 구조 테스트 129건 정리(새 dependency 필요 → 별도 승인 사안)

---

# HANDOFF — 77일차 추가: 달력 현재연도 색상 + 레트로 탁상시계

확인일: 2026-09-18. 수동 표준 모드. 77일차 본작업(`baef7a0`) 이후 사용자가 "77일차 추가 수정 작업지시서"(A. 현재 연도에 해당하는 달력 항목 색상 보강, B. 메인 갤러리 상단 레트로 디지털 탁상시계 추가)를 붙여넣어 피코(Claude Code)가 구현했어. **세션 도중 PC 강제종료로 중단됨** — 새 세션에서 branch/HEAD/git status/git diff/HANDOFF를 먼저 확인하고, 크래시난 세션의 로그 파일을 직접 읽어 이 작업이 실제로 지시받은 범위였는지(특히 A항목이 사용자 기억에 없다고 한 부분) 대조 검증한 뒤 이어서 진행했어. **실기기 QA 전부 통과(A·B 및 시계 후속 폴리싱 2건 포함), 사용자가 "커밋하고 푸시해줘"로 명시적으로 요청.**

## 크래시 정황

이전 세션(`4372dc55-5a9a-479a-b94f-a317ff0a7317.jsonl`, 종료 20:33)은 A·B 구현을 마치고 compile(BUILD SUCCESSFUL)·전체 unit test(677건 전부 통과)·`git diff --check`까지 자동 검증을 끝낸 직후, 시계 v2(폭 축소판) 실기기 QA를 요청하는 `AskUserQuestion` 시점에 끊겼어(로그상 `"User rejected tool use"`/`"Request interrupted by user for tool use"`). working tree에는 그 시점까지의 변경이 그대로 남아 있었고 git lock·gradle daemon 잔존 등 셸 중단 흔적은 없었어 — 파일 내용은 완결돼 있었고 미완성 코드는 없었음.

## 작업 A — 방문 달력 현재 연도 색상

- `VisitCalendarDrawer.kt`에 `VisitCalendarMonthCellEmphasis`/`VisitCalendarYearCellEmphasis` enum과 순수 판정 함수(`visitCalendarMonthCellEmphasisFor`/`visitCalendarYearCellEmphasisFor`) 추가.
- 우선순위: 선택 > 실제 현재 월(연도) > 현재 연도 소속 월 > 다음 해(YEAR_PICKER는 decade 밖) > 일반. 선택과 실제 현재가 같으면 marker 겹침 없이 기존 `visitCalendarShowsCurrentMarker` 원칙 유지.
- 새 색상 팔레트 없이 기존 오늘 색을 재사용: `VisitCalendarCurrentYearMonthTintColor = VisitFillColorToday.copy(alpha = 0.5f)`, 실제 현재 월/연도는 `VisitFillColorToday` 그대로.
- 연한 원(marker)=정확한 위치, 색=현재 연도 소속 시간대로 의미를 분리(코드 주석에 명시).
- `VisitCalendarTest.kt`에 emphasis 우선순위 테스트 4건 추가(선택 우선, 다음 해 버퍼 칸에 실제 현재 월이 겹칠 때의 우선순위 포함).

## 작업 B — 메인 갤러리 레트로 탁상시계

- `GalleryRetroClock.kt` 신규: `GalleryRetroClock` composable을 `GalleryScreen.kt` 타이틀 Row 아래 별도 줄에 연결.
- 시간: hh:mm(큰 7세그먼트, Canvas `Path`로 직접 그림, 새 폰트 의존성 없음) / ss(작은 7세그먼트) / AM·PM(절제된 보조 텍스트) 한 가로선 정렬. 날짜는 `2026 SEP 18 FRI` 형식 한 줄, 영문 약어는 화면용이고 접근성 설명(`retroClockAccessibilityDescriptionFor`)은 자연스러운 한국어로 별도 제공.
- 1초 갱신은 `GalleryRetroClock` 내부 `remember`+`LaunchedEffect`(다음 초 경계까지 delay)로만 처리 — 갤러리 화면 전체 재구성 없음.
- 커피잔은 기존 아이콘과 같은 `ImageVector.Builder` stroke 패턴으로 신규 제작, 장식 없음(몸체+손잡이+김 두 줄만).
- **후속 폴리싱 1 (폭)**: 초기 v1이 배너처럼 화면을 거의 다 먹는다는 피드백 → v2로 화면 폭의 58% 고정 비율 적용. 이후 "그래도 억지로 당긴 판넬처럼 보인다"는 추가 피드백을 받아 **v3**에서 고정 비율을 완전히 제거하고 `IntrinsicSize.Min`으로 내부 hh:mm/ss/PM·날짜 중 더 넓은 줄의 실제 글자 폭에만 바디가 맞춰 닫히게 함(안쪽 `HorizontalDivider`의 기본 `fillMaxWidth()`가 상위 화면 폭까지 다시 늘어나는 것을 이 방식으로 차단). 가로 정렬은 `Alignment.CenterHorizontally`로 통일.
- **후속 폴리싱 2 (상하 위치)**: 콘텐츠가 시각적으로 위로 치우쳐 보인다는 피드백 → 바디 전체 높이(외곽 크림 프레임 4/4dp + 내부 LCD 패널 7/7dp, 합계 22dp)는 그대로 두고 상/하 배분만 프레임 6/2dp·패널 13/1dp로 재분배해 콘텐츠를 8dp 아래로 이동. 요소 사이 `Spacer(4dp/3dp)` 간격, 가로 정렬, 바디 크기, 커피잔은 전혀 안 건드림.

## 자동 검증

- `compileDebugKotlin`: 크래시 직전 1회, 이번 세션에서 v3(폭)·상하 위치 수정 각 1회씩 총 2회 추가 재실행 — 전부 `BUILD SUCCESSFUL`, 신규 경고 없음(기존 Migration/deprecated 경고만).
- 전체 unit test: 크래시 직전 677건 전부 통과 확인(신규 `GalleryRetroClockTest` 13건 포함). 이후 수정은 레이아웃 modifier·padding 값만 바꾼 것이라 순수 함수·로직 변경 없음 — 전체 재실행하지 않음(변경 영향에 비례한 검증).
- `git diff --check`: 크래시 직전 통과(기존 CRLF 경고만).

## 실기기 QA

전부 통과. A(달력 현재연도 색상)·B(시계 기본형) QA와, 시계 후속 폴리싱 2건(폭 비율, 상하 중심)에 대해 각각 실기기로 직접 확인받음("실기기 확인완료!").

## Git

실기기 QA 전부 통과 후 사용자가 "커밋하고 푸시해줘"로 명시적으로 요청함.

## 다음 행동

**없음.** 77일차 추가 작업(A·B, 시계 후속 폴리싱 2건) 전체 완료.

---

# HANDOFF — 77일차: 방문 달력 폴리싱 + Intro 막스 33일차 조건 복구

확인일: 2026-09-18. 수동 표준 모드. 사용자 제공 77일차 지시서에 따라 피코(Claude Code)가 조사→구현→자동 검증→실기기 QA 순으로 진행했어. 지시서 범위(달력 현재 위치 표시·swipe 손맛, Intro 막스 문구 버그)를 끝낸 뒤, 같은 세션에서 사용자가 실기기로 확인하다가 작은 후속 요청("다른 월/연도 탐색 중엔 오늘 marker가 안 보이니 상단에 복귀 링크를 달아달라")을 추가로 반영했어. **실기기 QA 전부 통과. commit·push는 아직 요청받지 않아 미실행.**

## 시작 상태

branch `feature/photo-sticker`, 시작 HEAD `7e7b77f`(local==origin, tracked clean, 기존 무관 untracked `.codex-config.candidate.toml`/`.kotlin/`만 존재 — 76일차 종료 참고값과 정확히 일치). 76일차(갤러리 축소+기억밀도 새싹형)는 이번 작업에서 전혀 건드리지 않았어.

## 방문 달력 — 현재 월/연도 marker

- 75일차에 이미 있던 `displayedMonth`(선택/표시 중인 월) 강조와 완전히 분리된 "실제 오늘" 판정을 추가: `isCurrentMonthCell(year, month, today)`/`isCurrentYearCell(year, today)` 순수 함수.
- 표시: `VisitCalendarCurrentPeriodMarker`(26dp 원, `InkSecondary.copy(alpha=0.16f)`) — 새 색 안 만들고 기존 색을 한 번 더 낮춰 재사용. 성취처럼 안 보이게 아주 연하게.
- 겹침 방지: `visitCalendarShowsCurrentMarker(isCurrentPeriod, isSelected)` — 선택된 칸과 같으면 marker를 그리지 않음(강조 이중 적용 금지 지시 반영).
- `VisitCalendarMonthPicker`/`VisitCalendarYearPicker`가 `today: YearMonth` 파라미터를 새로 받음, `MonthlyVisitCalendar`에서 `todayYearMonth`로 계산해 전달.

## 방문 달력 — 후속: 오늘이 화면 밖일 때 헤더 복귀 링크 (사용자 실기기 QA 중 추가 요청)

- 문제: 위 marker는 오늘이 지금 보이는 4×4 창 안에 있을 때만 보임. 다른 연도로 탐색해 오늘이 창 밖으로 나가면 "오늘이 어딘지" 알 방법이 없었음 — 사용자가 실기기로 직접 확인하다 발견.
- 사용자에게 "안내 문구만" vs "탭하면 오늘로 복귀" 두 방식을 물어봤고, 후자로 확정.
- 순수 판정 함수 추가: `isCurrentMonthVisibleInMonthPicker(pickerYear, today)`, `isCurrentYearVisibleInYearPicker(decadeStart, today)` — 지금 보이는 4×4 grid 범위 안에 오늘이 있는지.
- 창 밖일 때만 헤더(`VisitCalendarPickerHeaderRow`) 바로 아래에 "오늘 2026년 9월 →" / "오늘 2026년 →" 같은 조용한 텍스트 링크를 추가로 보여줌. 탭하면 `pickerYear = todayYearMonth.year`로 이동 — **보기 창만 오늘이 포함되게 재배치**할 뿐, `navLevel`이나 실제 선택(`displayedMonth`)은 바꾸지 않음(CALENDAR 레벨의 "오늘" 버튼과 다른 동작이라 헷갈리지 않게 의도적으로 구분).
- 오늘이 이미 창 안에 보이면(예: 그냥 열었을 때 흔한 케이스) 이 링크는 안 뜨고 기존 marker만으로 충분 — 중복 표시 없음.

## 방문 달력 — MONTH_PICKER/YEAR_PICKER 수직 swipe

- 새 gesture 엔진 없이 표준 `detectVerticalDragGestures` + `android.view.ViewConfiguration.get(context).scaledPagingTouchSlop`(Android가 "페이지 넘기기"에 표준으로 쓰는 threshold, 임의 px 값 아님) 사용.
- 방향 판정은 순수 함수 `visitCalendarSwipeStepFor(accumulatedDrag, thresholdPx)`로 분리(위=NEXT, 아래=PREVIOUS, 미만은 NONE) — 단위 테스트로 직접 검증 가능.
- **버튼과 완전히 같은 handler 공유**: MONTH_PICKER/YEAR_PICKER 분기에서 `onStepUp`/`onStepDown` 람다를 한 번만 만들어 `VisitCalendarPickerHeaderRow`(▲▼ 버튼)와 `rememberVisitCalendarPickerSwipeModifier`(swipe) 양쪽에 그대로 전달 — 별도 swipe 전용 경로 없음.
- **애니메이션도 완전히 재사용**: swipe가 바꾸는 state(`pickerYear`/`decadeStart`)가 기존 `AnimatedContent`+`visitCalendarPickerStepTransition()`(75일차부터 있던 수직 슬라이드) 그대로를 트리거 — 새 transition 안 만듦.
- gesture 범위는 MONTH_PICKER/YEAR_PICKER의 `AnimatedContent` 영역에만 한정(CALENDAR 레벨은 그대로 버튼만). drawer 전체를 감싸는 `verticalScroll`과의 충돌 가능성을 조사 단계에서 짚었고, `change.consume()`으로 소비해 우선권을 가져가도록 구현 — 실기기 QA로 충돌 없음 확인.

## 방문 달력 — 오늘 복귀 햅틱

- `vibrateVisitCalendarTodayReturn`(14ms/amplitude 120) 추가. 프로젝트 표준 패턴(`Vibrator.vibrate(VibrationEffect.createOneShot)`, `LocalHapticFeedback` 무반응 확인 전력 재사용 — 갤러리 `vibrateGalleryFab`, 인트로 `vibrateIntroPostmark`와 동일 판단).
- 강도는 인트로 방문 소인의 "통"(24ms/175)보다 가볍고 갤러리 최소 탭(10ms/90)보다 살짝 무겁게 — "톡" 목표.
- 반복 탭 방지 로직 불필요: "오늘" 텍스트 자체가 기존부터 `!isCurrentMonth`일 때만 렌더링되므로(이미 76일차 이전부터 있던 gate), 클릭이 발생하는 시점은 항상 실제 이동일 때뿐.

## Intro — 막스 베르스타펜 33일차 버그 원인·수정

- **원인**: `INTRO_MILESTONE_MESSAGES[33]` 확정 조건 자체는 이미 정확했지만, 같은 문자열("뚜뚜뚜두 막스 베르스타펜")이 무작위 이스터에그 풀 `INTRO_SECRET_MESSAGES`(3% 확률)에도 동시에 들어있었음 — 33일차가 아닌 날에도 약 1%(3%×1/3) 확률로 같은 문구가 새어나왔음(실제 발견 당시 6일차에 노출).
- **수정**: `INTRO_MAX_MILESTONE_MESSAGE` 상수로 분리해 milestone map에서만 참조하고, `INTRO_SECRET_MESSAGES`에서는 완전히 제거(남은 무작위 풀은 "챗지피티야 고마워"/"비개발자가 만들었어요" 2개). 방문 데이터·Room·streak 계산은 전혀 안 건드림 — 순수 문구 풀 구성만 수정.
- 기존에 이 버그를 놓쳤던 이유: `AppIntroMessageLogicTest.secretMessages_matchFixedSpec`가 막스 문구가 풀 안에 있는 걸 "정상"으로 고정 검증하고 있었음 — 이번에 그 테스트도 함께 수정.

## 0/6/32/33/34 경계 테스트

`selectIntroMessage_atNonMilestoneVisitDays_neverReturnsMaxVerstappenMessageEvenByRandomRoll`(0/6/32/34, 각 2만 회 롤) + `selectIntroMessage_at33rdVisit_alwaysReturnsExactlyTheMaxMilestoneMessageConstant`(1천 회) 신규 추가, 기존 32/33/34 milestone 비적용 테스트와 함께 전부 통과.

## 자동 검증

- `compileDebugKotlin`: BUILD SUCCESSFUL(2회 재실행, marker/swipe 라운드 + 복귀 링크 라운드), 신규 경고 없음(기존 Migration/deprecated 경고만).
- `VisitCalendarTest` 24건(marker 판정 2개, 겹침 방지 1개, swipe 방향 1개, 창 안/밖 판정 2개 = 신규 6건), `AppIntroMessageLogicTest` 18건(신규 2건) 전부 통과.
- 전체 unit test: 660건 전부 통과, 실패·에러 0건(test-results XML 직접 집계로 재확인).
- `git diff --check`: 통과(기존 LF→CRLF 경고만).
- **미검증(Compose UI 테스트 하네스 없음, 75일차와 동일한 이유)**: 실제 swipe 제스처 손맛, drawer scroll과의 실제 충돌 여부, marker·복귀 링크 실기기 가독성, 햅틱 강도 — 전부 실기기 QA로 확인함(아래).

## 실기기 QA

2라운드 전부 통과. 1라운드(marker+swipe+햅틱+Intro): "월/연도 marker 잘 보이고 swipe도 자연스러워", "햅틱은 톡 정도로 가볍고, 막스 문구도 안 떴어". 2라운드(오늘 복귀 링크 후속): "잘 돌아가". MONTH_PICKER/YEAR_PICKER marker 가독성, swipe 방향·손맛, 기존 ▲▼ 버튼과의 일관성, 오늘 복귀 햅틱 강도, 33일 아닌 날 막스 문구 미노출, 창 밖일 때 복귀 링크 동작까지 전부 확인됨.

## 자연 QA로 남긴 항목

실제 총 방문일이 33일에 도달하는 날 막스 문구가 실제로 뜨는지는 자동 테스트(33일차 강제 파라미터 통과)로만 확인했고, 실사용 33일차 자연 노출은 아직 미확인 — 방문일이 자연스럽게 33에 도달할 때까지 향후 확인 대상으로 남김(지시서 20절에 명시된 정책과 동일).

## Git

실기기 QA 2라운드 전부 통과 후 사용자가 "커밋하고 푸시해줘"로 명시적으로 요청함. commit `fdbc1b1`(5 files changed), push 완료. 종료 HEAD `fdbc1b1`, local == origin/feature/photo-sticker, tracked working tree clean(기존 무관 untracked `.codex-config.candidate.toml`/`.kotlin/`만 남음, `git fetch` 후 재확인 완료).

## 다음 행동

**없음.** 77일차(달력 폴리싱·오늘 복귀 링크 후속·Intro 막스 버그 수정) 전체 완료, commit·push까지 끝남. 유일하게 남은 항목은 "자연 QA로 남긴 항목"의 실제 33일차 자연 노출 확인뿐 — 방문일이 자연스럽게 33에 도달할 때 다음 세션에서 확인하면 됨.

---

# HANDOFF — 76일차 후속: 기억밀도 카오모지 → 새싹형 전환

확인일: 2026-09-17. 수동 표준 모드. 76일차 본작업(commit `7d2f476`/`515ada5`) 이후 같은 세션에서 이어진 후속 지시서("카오모지형 → 새싹형 전환")를 피코(Claude Code)가 직접 구현했어. HTML 목업 먼저 만들어 확인받고(사용자 요청으로 편자→♥ 모양 수정, 하트 진하기, 물뿌리개 아이콘 추가까지 목업 단계에서 반복), 그 다음 production에 반영 → 실기기 QA → QA 피드백 2건(물뿌리개 제거, 구분선 통합, 얼굴 색 되돌림) 반영까지 끝냈어. **실기기 QA 통과, 사용자가 "커밋하고 푸시 부탁해"로 명시적으로 요청 — 이 HANDOFF 커밋에 포함. 사용자가 이 작업으로 76일차 전체를 마무리한다고 확인함(추가 예정 작업 없음).**

## 최종 상태

기억밀도(`GalleryDensityPage`)의 월별 표현이 카오모지 3단계(•_•/˙ᵕ˙/ᵔᴗᵔ)에서 "새싹형"으로 완전히 바뀌었어.

- **폐기 이유**: 카오모지 글리프마다 시각적 무게중심이 달라서 같은 기준선에 정렬해도 얼굴이 위/아래로 떠 보이는 문제가 있었고, "위로 갈수록 높은 수치"라는 그래프 문법과 표정이 충돌할 수 있다는 게 사용자 판단이었어.
- **새 구조(위→아래)**: overflow "+" 표시 → 수치만큼 자란 얇은 줄기(`MEMORY_DENSITY_STEM_WIDTH` 3dp, 기존 막대 14dp보다 훨씬 얇음) → 줄기 위 고정 모양 하트(♥, 처음엔 SVG 잎 두 장이었으나 사용자가 실기기에서 "말굽 편자 같다"고 해서 ♥ 텍스트로 교체) → **전체 폭으로 이어진 구분선 하나**(원래 달마다 22dp 개별 구분선이었는데 실기기 QA에서 "___" 끊김이 지적돼 통합) → 고정된 무표정 얼굴 "•_•"(색은 항상 `InkSecondary` 고정 — 하트 진하기와 공유해보자는 사용자 요청이 있었지만 실제로 보고 "원래대로 돌려달라"고 되돌림) → 월 숫자.
- **하트 진하기**: `memoryDensityHeartAlpha(level) = 0.4 + (level/6)*0.6`. 줄기가 높을수록(레벨 1→6) 하트가 옅음→진한 sunset-gold로 진해짐. 이 로직은 하트에만 남아있고 얼굴에는 적용 안 됨(사용자가 명시적으로 되돌림).
- **막대 단위 규칙은 그대로 유지**: 1칸=엽서 2장, 최대 6칸(12장), 13장부터 6칸 유지+"+".
- **물뿌리개 아이콘**: "2026년" 옆에 커스텀 `ImageVector`(`WateringCanIcon`, `PondDrawerIcon`과 같은 `ImageVector.Builder` 패턴)로 한 번 추가했으나, 실기기 QA에서 "빼줘"라는 요청으로 완전히 제거함(정의 자체도 삭제, dead code 안 남김).
- **함수 분리**: 기존 `GalleryMemoryDensityBar` 하나였던 걸 `GalleryMemoryDensityStem`(overflow+줄기+하트, 접근성 contentDescription 유지)과 `GalleryMemoryDensityFoot`(얼굴+월 숫자, `clearAndSetSemantics`로 중복 안내 방지)으로 나눴어 — 구분선을 전체 폭 하나로 통합하려면 Row 두 개로 쪼개야 했기 때문.
- **삭제한 것**: `memoryDensityKaomoji` 함수 완전 삭제(소스에 흔적 없음), 관련 카오모지 경계값 테스트 삭제.
- **기억밀도 데이터 기준·연도 처리·터치 상호작용 없음 등 76일차 본작업 원칙은 전혀 안 바뀜.**

## 목업 반복 과정(참고용, `docs/ai/mockups/memory-density-mockup.html`)

1차: 카오모지 폐기 + 하단 고정 얼굴 + 줄기 + 새싹(SVG 잎 두 장) → 사용자가 "말굽 편자 같다"고 지적 → SVG를 걷어내고 텍스트 "♥"로 교체 → 승인.
2차 요청: 하트 진하기를 높이에 비례하게, "2026년" 옆에 물뿌리개 아이콘 추가 → 둘 다 구현(물뿌리개는 초반에 자물쇠처럼 보여 물방울 2개를 추가해 재작성).
production 반영 후 실기기 QA → 물뿌리개 제거, 구분선 통합, 얼굴 색 되돌림 3가지 피드백 → production·테스트·목업 모두 동일하게 반영하고 재검증.

목업은 Chrome headless(`--headless=new`)로 매 단계 직접 캡처해서 확인했고, 검증용 스크린샷·임시 프로필은 매번 삭제해 저장소에 남기지 않았어. (세션 중 사용자가 "임시 파일은 저장소 내부 안전한 경로를 쓰고 작업 후 지워라"고 요청해 이후로는 `/tmp` 대신 `docs/ai/mockups/` 안에 밑줄 접두사 임시 파일을 만들었다가 확인 후 즉시 삭제하는 방식으로 전환했어.)

## 자동 검증

- `compileDebugKotlin`: 매 반영 라운드마다 재실행, 최종 `BUILD SUCCESSFUL`, 신규 경고 없음.
- gallery 패키지 관련 테스트: 최종 `GalleryMemoryDensityStructureTest` 7건, `GalleryMemoryDensityTest` 13건 전부 통과(카오모지 테스트 삭제, 하트 진하기 경계값·단조증가·물뿌리개 부재·구분선 단일화·얼굴 고정색 확인 테스트로 교체).
- 전체 unit test: 652건 전부 통과, 실패·에러 0건(XML 결과 직접 집계로 재확인, fork 보고를 그대로 신뢰하지 않음).
- `git diff --check`: 통과(기존 LF→CRLF 경고만).

## 실기기 QA

2라운드 진행. 1라운드: 전체적으로 만족("아주 완벽해 내가 원했던 거 그대로야" — 이건 76일차 본작업 ViewMode 축소 건). 새싹형 자체는 이번 후속 작업 QA에서 3가지 피드백(물뿌리개 제거/구분선 통합/얼굴 색 되돌림)을 받았고 전부 반영 후 사용자가 "좋아 커밋하고 푸시 부탁해"로 최종 승인.

## Git

commit·push는 사용자가 명시적으로 요청("좋아 커밋하고 푸시 부탁해~"). commit `06af4fe`(5 files changed), push 완료. 종료 HEAD `06af4fe`, local == origin/feature/photo-sticker, tracked working tree clean(기존 무관 untracked `.codex-config.candidate.toml`/`.kotlin/`만 남음).

## 다음 행동

**없음.** 사용자가 이 작업으로 76일차(본작업+후속 새싹형 전환)를 완전히 마무리한다고 명시했어. 77일차 예약 범위(방문 달력 폴리싱, Intro 총 방문 33일차 조건 복구)는 여전히 미착수 상태로 남아있고, 다음 세션은 새 지시서로 시작하면 돼.

---

# HANDOFF — 76일차: 갤러리 보기 체계 축소 + 기억밀도 재정의

확인일: 2026-09-17. 수동 표준 모드. 사용자 제공 76일차 지시서에 따라 피코(Claude Code)가 조사→production 구현(fork 위임 후 직접 재검증)→실기기 QA→commit·push 순으로 진행했어. 공용 작업판은 활성화하지 않았어. **실기기 QA 통과("아주 완벽해 내가 원했던 거 그대로야"), 사용자가 commit·push를 명시적으로 요청 — 이 HANDOFF 커밋에 포함.**

## 시작 상태

branch `feature/photo-sticker`, 시작 HEAD `e6659da`(local==origin, tracked clean, 기존 무관 untracked `.codex-config.candidate.toml`/`.kotlin/`만 존재 — 실측이 인수인계 참고값과 일치). 직전 HANDOFF는 75일차 방문 달력 작업으로 오늘 작업과 무관했어.

## 기존 ViewMode 구조(조사 결과)

- enum은 `GalleryPageFormat`(GalleryPageFormat.kt) — THREE_COLUMN/MONTHLY/TIMELINE/CALENDAR/STAMP/DENSITY 6종, default THREE_COLUMN. (참고: `GalleryViewMode.kt`의 COMPACT_GRID/DETAIL_LIST는 어디서도 참조 안 되는 완전 무관 dead code라 이번에도 건드리지 않았어.)
- 저장 방식은 SharedPreferences/DataStore가 아니라 `GalleryScreen.kt` 내부 `rememberSaveable`+커스텀 Saver뿐 — 앱 완전 재시작 시 항상 기본값으로 리셋됨(회전 등 프로세스 재생성에만 살아남음).
- 조사 중 지시서에 없던 위험 2가지를 발견해 사용자에게 확인받았어: ①"퐁(연못) 놀이 모드"가 3단 보기에만 렌더링 경로가 있었음(→월별 보기로 이식 승인), ②기억밀도로 들어가는 별도 진입점이 없었음(→기존 pager+점 인디케이터를 그대로 재사용해 월별/기억밀도 2페이지 좌우 스와이프로 승인).

## 삭제한 보기 / 월별 보기 default화

- 3단 보기, 캘린더 보기(갤러리 ViewMode만 — 방문 달력과 무관), 우표 보기, 타임라인 보기 삭제. enum에서 4개 항목 제거, 전용 composable(`GalleryThreeColumnPage`, `GalleryCalendarPage`, `GalleryStampPage`+`GalleryStampGridItem`, `GalleryTimelinePage`+`GalleryTimelineEntry`) 삭제.
- 공용 자산은 보존: `PinkingPhotoShape`/`StampCardContent`/`StampCard`(다른 화면에서도 재사용), `calendarCellsFor`(방문 달력 `VisitCalendarDrawer.kt`가 재사용 중 — 삭제했으면 방문 달력이 깨졌을 부분, diff에서 직접 확인).
- `activePageFormats`를 토글 가능한 `rememberSaveable Set`에서 `setOf(MONTHLY, DENSITY)` 고정값으로 단순화(더 이상 아무도 켤 수 없는 토글 로직을 남기지 않음). `currentPageFormat` 기본값 MONTHLY.
- 우측 상단 체크박스 기반 "보기 형식 관리" 드롭다운 UI 전체 제거(`GalleryPageFormatMenuItem` 포함). 기존 `HorizontalPager`+`GalleryPageIndicator`(2페이지 이상일 때 자동 표시)가 그대로 월별↔기억밀도 스와이프를 보여줌 — 새 UI 문법 추가 없음.
- 검색 아이콘 노출 조건과 검색 강제 종료 `LaunchedEffect`를 THREE_COLUMN→MONTHLY로 이관(정렬은 원래 MONTHLY도 `sortAffectsOrder=true`라 이관 불필요).
- 퐁 모드: `isPondModeOn`/`pondController`와 탭/드래그 파문 제스처를 `GalleryMonthlyGridPage`로 이식. 평소엔 가벼운 `StampCardContent`, 퐁 모드 켜졌을 때만 물리 연출 붙은 `StampCard`로 전환(`StampCard.kt`에 `dateLabelOverride` 파라미터 추가해 월 헤더와 날짜 중복 안 되게 "일"만 표기).
- legacy fallback: `PageFormatSaver`가 삭제된 이름(THREE_COLUMN/CALENDAR/STAMP/TIMELINE)이나 알 수 없는 값을 만나면 MONTHLY로 자동 복귀(`getOrDefault(GalleryPageFormat.MONTHLY)`). SharedPreferences/DataStore 자체가 없어 별도 마이그레이션 불필요.

## 기억밀도 재정의

- 데이터 source: `postcard.capturedAt`(방문 기록 아님), 지정 연도(기본 현재 연도) 1~12월만 — 연도 이동 picker는 기존에 없던 기능이라 새로 안 만듦.
- 막대 단위: 1칸=엽서 2장, 최대 6칸(12장), 13장부터 6칸 유지+위에 "+" 표시(`memoryDensityMonthsForYear`/`memoryDensityBarLevel`/`memoryDensityHasOverflow`).
- 카오모지 3단계: 0~4장 •_•, 5~8장 ˙ᵕ˙, 9장 이상 ᵔᴗᵔ(`memoryDensityKaomoji`) — 슬픈/우는 표정 없음.
- 레이아웃: 12개월을 `weight(1f)` 균등폭 Row로 한 화면 배치, 가로 스크롤 없음 — 실기기 QA로 뭉개짐 없이 확인됨. 월 표기는 숫자만("1"~"12").
- 기존에 있던 "점 탭하면 그 달 사진이 아래 펼쳐지는" 기능은 제거함 — "dashboard 아닌 순수 조회 화면" 재정의와 새 막대그래프 디자인이 양립하지 않아서 없앴고, 실기기 QA 통과로 사실상 승인됨(사용자에게 명시적으로 짚었고 이견 없었음).

## 자동 검증

- 내가 직접 재실행(fork의 보고를 그대로 믿지 않고 diff·테스트 결과를 재검증): `:app:testDebugUnitTest`(gallery 패키지 9개 파일, VisitCalendarTest 18건 포함 전부 통과) + `:app:compileDebugKotlin` `BUILD SUCCESSFUL`.
- 전체 unit test 재실행: `BUILD SUCCESSFUL`, test-results XML 직접 집계 **647건 전부 통과, 실패·에러 0건**.
- `git diff --check` 통과(기존 LF→CRLF 경고만).
- 기억밀도 막대/카오모지 경계값(0~13+ 전 구간), legacy ViewMode fallback 최소 테스트 신규 추가.
- 삭제한 ViewMode 전용 테스트 3개 파일(`GalleryCalendarStructureTest`/`GalleryStampStructureTest`/`GalleryTimelineStructureTest`) 삭제, `GalleryViewSelectionStructureTest`는 새 selector 없는 구조에 맞게 전면 재작성.

## 실기기 QA

통과. 사용자 확인: "아주 완벽해 내가 원했던 거 그대로야". 12개월 배치·월 숫자 표기·카오모지 가독성·퐁 모드 이관 모두 문제 없음.

## 추가 산출물 — 기억밀도 목업

며칠치 실데이터만으로는 밀도 체감이 어렵다는 사용자 요청으로 `docs/ai/mockups/memory-density-mockup.html` 추가(74~75일차와 같은 패턴 — 앱 밖 독립 HTML, production 미반영). 실제 앱과 동일한 막대/overflow/카오모지 공식을 그대로 재현했고, 월별 숫자를 직접 입력해 실시간으로 바꿔보거나 프리셋(조용한 해/보통인 해/풍성한 해/폭발적인 달 포함/기복이 큰 해)으로 비교 가능. Chrome headless로 직접 캡처해 12개월 전체 배치·overflow 표시·카오모지 단계를 확인했고, 그 과정에서 flex 자식 최소 너비 문제(입력창 때문에 12번째 달까지 안 잘리는지 확인 안 되던 버그)를 발견해 `min-width: 0` 추가로 수정했어. 검증용 스크린샷과 임시 Chrome 프로필은 작업 후 삭제.

## 남은 위험 / 다음 행동

- 지시서 26절(기억밀도 월 탭→월별 보기 이동)은 오늘 범위 아님, 77일차 이후 후보.
- 77일차 예약 범위(방문 달력 폴리싱, Intro 총 방문 33일차 조건 복구)는 이번 작업에서 건드리지 않음.
- 실사용 데이터가 쌓이면 12개월 배치·카오모지 가독성을 다시 한 번 확인하면 좋음(현재는 QA 시점 데이터량 기준 확인).

## Git

commit·push는 사용자가 목업 확인 후 명시적으로 요청("이대로 커밋 푸시하자")했어. commit `7d2f476`(14 files changed), push 완료. 종료 HEAD `7d2f476`, local == origin/feature/photo-sticker, tracked working tree clean(기존 무관 untracked `.codex-config.candidate.toml`/`.kotlin/`만 남음).

---

# HANDOFF — 75일차 추가: 방문 달력 계층형 월/연도 탐색

확인일: 2026-09-16. 수동 표준 모드. 75일차 기본 작업(월 이동/오늘 복귀/애니메이션/오늘 색) commit·push(`f687145`) 이후, 사용자가 챗지피티에게 받은 "75일차 추가 수정지시서"(계층형 월/연도 탐색)를 피코(Claude Code)가 직접 구현했어. 이어서 실기기 QA 중 나온 사용자 추가 요청(▼ 버튼 가시성, ▲▼ 애니메이션, 4×4 grid, 달력 6주 고정)까지 같은 흐름에서 반영했어. 공용 작업판은 활성화하지 않았어. **실기기 QA 통과, 사용자가 commit·push를 명시적으로 요청 — 이 HANDOFF 커밋에 포함.**

## 계층형 탐색 최종 상태

시작 HEAD `f687145`(clean, 기존 untracked 2개만). CALENDAR → MONTH_PICKER → YEAR_PICKER 3단 탐색기 + 실기기 QA 2라운드 반영까지 구현·자동 검증 완료.

- navigation level: `internal enum class VisitCalendarNavLevel { CALENDAR, MONTH_PICKER, YEAR_PICKER }`. `VisitCalendarDrawer`(부모)에서 `rememberSaveable`로 보유(hoist) — `BackHandler`와 drawer 재오픈 리셋이 이 값을 봐야 해서 `MonthlyVisitCalendar` 밖으로 끌어올렸어. `MonthlyVisitCalendar`는 `navLevel`/`onNavLevelChange`를 파라미터로만 받음.
- 표시 연도 state: `pickerYear: Int`를 `displayedMonth`와 분리된 별도 `rememberSaveable`로 둠. MONTH_PICKER/YEAR_PICKER 안에서 연도만 훑어보다가 선택 없이 뒤로 가도 실제 달력(`displayedMonth`)은 건드리지 않음 — 월/연도를 실제로 선택(탭)하는 순간에만 `displayedMonth`에 반영.
- **MONTH_PICKER grid 구조(QA 후 확정): 4열×4행(16칸)** — `monthPickerGridCells(pickerYear)`가 pickerYear의 1~12월(라벨 "1"~"12", "월" 접미사 제거) + 다음 해 1~4월을 반환. 다음 해 4칸은 `VisitCalendarAdjacentPeriodColor`(`InkSecondary` alpha 0.4, 새 회색 토큰 추가 안 함)로 낮춰 구분.
- **YEAR_PICKER grid 구조(QA 후 확정): 4열×4행(16칸)** — `yearPickerGridYears(decadeStart)`가 decade 앞 2년 + 실제 10년 + 뒤 4년을 반환(예: decadeStart=2020 → 2018~2033). 앞/뒤 6칸은 같은 낮춘 색으로 구분. 사용자가 확인한 Windows 작업표시줄 캘린더의 연도 grid 배치와 동일. (최초엔 5×2였다가 이 요청으로 4×4로 교체.)
- 연도 ▲▼ 동작: MONTH_PICKER 헤더 우측 ▲=`pickerYear++`(다음 연도), ▼=`pickerYear--`(이전 연도).
- decade ▲▼ 동작: YEAR_PICKER 헤더 우측 ▲=`pickerYear += 10`, ▼=`pickerYear -= 10`. decade 자체 state 없이 `pickerYear`에서 매번 계산.
- decade 계산 방식: `internal fun decadeStartFor(year: Int) = (year / 10) * 10`. 순수 함수, 경계값(1999/2000/2009/2010/2029/2030) 테스트 완료.
- 월 선택 동작: MONTH_PICKER에서 월(다음 해 4칸 포함) 탭 → `displayedMonth = YearMonth.of(선택한 year, month)`, `navLevel = CALENDAR`로 즉시 복귀. history marker·visit count·streak 변경 없음(순수 표시 state만).
- 연도 선택 동작: YEAR_PICKER에서 연도(앞뒤 6칸 포함) 탭 → `pickerYear = year`, `navLevel = MONTH_PICKER`로 한 단계 내려옴.
- 현재 선택값 표시: 강조 기준은 "오늘"이 아니라 "지금 표시 중인 연·월"(실제 `displayedMonth`와 정확히 일치하는 칸만). 강조 방식은 텍스트 weight(Medium)+색(InkPrimary) 하나만 사용 — 방문 라벨보다 약함.
- 계층 전환 animation(CALENDAR↔MONTH_PICKER↔YEAR_PICKER): `visitCalendarHierarchyTransition()` — `fadeIn + scaleIn(0.97f)` / `fadeOut + scaleOut(0.97f)`, 방향성 없이 대칭 처리, 170ms. `SizeTransform`도 같은 spec으로 덮어써 기본 spring 제거.
- **▲▼ 스텝 애니메이션(실기기 QA 추가 요청, 최초엔 애니메이션 없이 즉시 전환이라 "밑밑하다"는 피드백):** `visitCalendarPickerStepTransition()` 추가 — 월 이동과 같은 원리(state 비교가 방향 결정)로 `slideInVertically`/`slideOutVertically` 사용. ▲(다음 연도/decade)는 위로, ▼(이전)는 아래로 스르륵. MONTH_PICKER는 `pickerYear`로, YEAR_PICKER는 `decadeStart`로 각각 `AnimatedContent` 트리거. `MONTH_TRANSITION_DURATION_MS`(200ms) 재사용 — 월 이동과 같은 느낌을 원한 요청이라 새 duration을 만들지 않음.
- 계층 전환 duration: 170ms. 스텝(▲▼) 전환 duration: 200ms(월 이동과 동일).
- **CALENDAR 날짜 grid 높이(실기기 QA 추가 요청): 항상 6주(42칸) 고정.** `visitCalendarPaddedCells(month)`가 실제 달 뒤에 빈 칸(null)만 채워 42칸으로 맞춤 — 실제 날짜를 만들어내지 않음. 지시서 원문은 "5주 기준"이라고 했지만, 31일짜리 달이 금/토에 시작하면 실제로 6주가 필요해 5주로 고정하면 그 달만 여전히 튀어나온다고 판단해 6주로 구현하고 사용자에게 설명함(승인됨, 실기기 QA 통과). 주차 구분선은 실제 주(real week)까지만 그리고 패딩 행 사이에는 넣지 않음(`realWeekCount` 계산).
- drawer 재오픈 시 mode: `LaunchedEffect(drawerState.isOpen)`으로 drawer가 열릴 때마다 `navLevel = CALENDAR`로 리셋. `displayedMonth`는 기존 74·75일차 동작 그대로 유지.
- 오늘 복귀 시 mode: "오늘" 버튼은 CALENDAR 레벨에서만 보임(변경 없음) — 이미 CALENDAR로 돌아온 상태에서만 누를 수 있음.
- back 동작: `VisitCalendarDrawer`의 `BackHandler` — `navLevel == CALENDAR`면 drawer를 닫고, 아니면 `visitCalendarNavLevelOnBack(navLevel)`로 한 단계만 내려옴(YEAR_PICKER→MONTH_PICKER→CALENDAR).
- 미래 연도 탐색 정책: 75일차 기본 작업의 "제한 없음" 정책 그대로.
- 방문 셀 스타일(2dp, `#16A7A1`/`#117E7A`, 자동 대비)은 이번 작업에서 전혀 건드리지 않음.
- **QA 라운드 1 수정**: picker 헤더 Row에 `height(24.dp)`로 고정했다가 ▲▼ 아이콘 두 개(18+18=36dp)가 그 안에 다 못 들어가 ▼가 잘려서 작게 보이던 버그. 고정 높이를 제거하고 아이콘을 20dp로, 둘 사이 2dp 간격을 추가해 수정.
- UI 미감: Material DatePicker·card·pill·border·shadow·gradient 없음. 텍스트 중심 grid + 기존 `clickable` ripple만 사용. ▲▼는 20dp 아이콘(월/연도 이동 화살표와 동일 크기로 QA 후 통일).

### 자동 검증

- `:app:testDebugUnitTest --tests VisitCalendarTest :app:compileDebugKotlin`: 매 수정 라운드마다 재실행, 최종 `BUILD SUCCESSFUL`. `VisitCalendarTest` 11→18건(decade 계산·back 단계 이동·강조 로직·4×4 grid 생성·6주 패딩 신규 7건) 전부 통과. `compileDebugKotlin`은 기존 경고만, 신규 경고 없음.
- `git diff --check` 통과(기존 CRLF 경고만).
- **미검증**: 실제 탭 → navLevel 전환, ▲▼ 빠른 반복 입력, 각 애니메이션 자체, drawer 재오픈 시 리셋은 Compose UI 테스트 하네스가 이 저장소에 없어(새 테스트 의존성 추가는 미승인 범위) JVM 단위 테스트로 확인 불가 — **실기기 QA 2라운드로 확인 완료**(제목 터치, 4×4 월/연도 grid, ▲▼ 방향·가시성, 계층 전환, drawer 재오픈, back, ▲▼ 슬라이드 애니메이션, 6주 고정 높이 모두 통과).

### 사용자 승인

2026-09-16, 실기기 QA 2라운드(▼ 가시성 수정 확인, ▲▼ 애니메이션·4×4 grid·6주 높이 확인) 통과 후 사용자가 "커밋하고 푸시하자"로 commit·push를 명시적으로 요청함.

### 다음 행동

1. 실기기 QA 통과, 사용자 승인 완료 — commit·push 진행.
2. 다음 작업은 없음. 필요하면 사용자가 새 지시서로 시작.

---

# HANDOFF — 75일차 방문 달력 월 탐색·오늘 복귀·전환 애니메이션·오늘 방문 색

확인일: 2026-09-16. 수동 표준 모드. 사용자 제공 75일차 지시서에 따라 피코(Claude Code)가 직접 구현했어. 공용 작업판은 활성화하지 않았어.

## 75일차 현재 상태

시작 HEAD는 `86baedf`(74일차 장식선 production 반영까지 포함, 지시서 참고값 `a113dd7`보다 한 커밋 앞섰음 — 실제 상태로 확인 후 진행). 이전/다음 달 이동, 오늘 복귀, 월 전환 슬라이드 애니메이션은 구현·자동 검증까지 끝났어. **오늘 방문 색상만 사용자 목업 승인 대기**야 — 승인 전에는 production에 최종 반영하지 않아.

- 표시 월 state: `MonthlyVisitCalendar` 안에 `displayedMonth`를 `rememberSaveable`로 둬(기존 `GalleryScreen.kt`의 `visibleMonth` 패턴과 동일한 `YearMonth` 문자열 Saver 재사용, 이 파일 안에 따로 둠). 이전/다음 달은 `YearMonth.minusMonths(1)`/`plusMonths(1)`만 호출 — `visitedEpochDays`/`totalVisitDays`는 절대 만지지 않음.
- 이전/다음 달 UI: 이 드로어에 이미 있던 닫기 버튼과 같은 문법(`IconButton` + `InkSecondary` 20dp 아이콘) 재사용. `Icons.AutoMirrored.Filled.KeyboardArrowLeft/Right` — `GalleryScreen.kt`의 `GalleryCalendarPage` 월 이동과 같은 아이콘·색상 선례.
- 미래 달 정책: **제한 없음**. `GalleryCalendarPage`도 미래 달 제한이 없는 기존 선례라 그대로 따름 — 빈 달만 보일 뿐 데이터 위험이 없고, 제한을 넣으면 state만 복잡해짐.
- 오늘 복귀: "다녀간 날들" 캡션 줄 끝에 작은 텍스트 `오늘`, 현재 월일 때는 숨김(`isCurrentMonth` 조건). 방문 데이터에는 손대지 않고 `displayedMonth = YearMonth.from(today)`만 실행.
- 애니메이션: Compose `AnimatedContent` 2곳(월 제목 텍스트, 날짜 grid) — 둘 다 같은 `displayedMonth`로 동시에 트리거되어 함께 슬라이드. 방향은 `targetState > initialState`(YearMonth 비교)로만 결정 — 별도 "방향" state 없음(state가 진짜 값, animation은 표현). `slideInHorizontally`/`slideOutHorizontally` + `tween(200ms, FastOutSlowInEasing)`, `SizeTransform`도 같은 duration으로 덮어써 기본 spring bounce를 제거. `clipToBounds()`로 슬라이드 중 드로어 폭 밖으로 새는 것 방지.
- animation duration: **200ms** (150~250ms 검토 범위 중 다른 화면 전환보다 유독 느리지 않은 중간값).
- 오늘 방문 색: 함수 `visitDayFillColor(date, today)` / `visitDayFillContrastColor(date, today)` 추가. `visited`가 이미 true인 날짜에만 호출되므로(기존 gate 유지) 오늘+미방문·미래는 애초에 라벨 자체가 없음. 오늘+방문만 `VisitFillColorToday`, 그 외 방문은 기존 `VisitFillColor(#16A7A1)` 그대로. 대비도 같은 `labelStickerTextColorArgbFor` 재사용(`VisitFillContrastColorToday`).
- **오늘 방문 색 최종 HEX: `#117E7A`(후보 B, "또렷하게") 확정.** 아티팩트(https://claude.ai/artifact/5v44EFArEciGTaB8FacmCE)와 로컬 목업(`docs/ai/mockups/today-color-artifact.html`, `docs/ai/mockups/visit-calendar-today-color.html`)으로 A(#13908B)/B(#117E7A)/C(#0E6C68) 세 후보를 제시했고 사용자가 B를 선택했어(2026-09-16). `VisitFillColorToday`/`VisitFillTodayArgb`에 반영 후 재검증 완료. 세 후보 모두 luminance 계산상 기존과 같은 "밝은 글자" 쪽으로 나와 자동 대비 로직이 뒤집히지 않음을 Node로 확인했음.
- 2dp 라벨 모서리, 3dp 내부 여백, 카오모지 4종, deterministic selector, 토요일/일요일 무디게 낮춘 잉크색, 주차 구분선, 상하단 장식선, "다녀간 날들"/"오늘까지 N번 만났어요~!"/봉투 카운터, drawer 구조, 달력 크기 모두 그대로 유지.
- 공휴일 STOP 유지, 회귀 보호(방문 기록/Intro/Gallery) 모두 준수. 실제 `filesDir`·`visit_record.txt`·history marker·total/streak 접근 없음.

### 자동 검증

- `:app:testDebugUnitTest --tests VisitCalendarTest --tests VisitHistoryStorageTest :app:compileDebugKotlin`: `BUILD SUCCESSFUL`. `VisitCalendarTest` 8→11건(오늘 방문 색 결정 로직 신규 3건 포함) 전부 통과, `VisitHistoryStorageTest` 8건 통과. `compileDebugKotlin`은 기존 경고(Migration `db` 파라미터명, deprecated API)만 있고 신규 경고 없음. B 색상 확정 후 동일 테스트·compile 재실행해 재통과 확인(11건·8건 그대로 통과).
- `git diff --check` 통과(기존 LF→CRLF 경고만, 신규 오류 없음).
- 전체 unit test는 국소 UI 변경에 비례해 실행하지 않았어(기존 74일차 관례와 동일).
- 실기기 설치·실행·삭제·초기화·계측 테스트·사용자 데이터 조작 없음.
- 월 이동 자체의 연도 경계(12월↔1월)는 java.time `YearMonth.minusMonths/plusMonths`에 위임하고 자체 계산을 하지 않음 — 기존 `sharedCalendarHandlesLeapYearsMonthLengthsAndYearBoundary` 테스트가 `calendarCellsFor`로 이미 확인.
- **미검증**: 실제 클릭 → `displayedMonth` state 전환·애니메이션 방향·빠른 연속 클릭·drawer 재오픈 후 유지는 Compose UI 테스트 하네스(Robolectric 등)가 이 저장소에 없어 JVM 단위 테스트로 확인 불가 — 실기기 QA로만 확인 가능. 새 테스트 의존성 추가는 미승인 범위라 시도하지 않았어.

### 다음 행동

1. 실기기 QA(75일차 지시서 28절: 월 이동, 오늘 복귀, 애니메이션, 오늘 방문 색 `#117E7A`) — 대기 중.
2. QA 통과 후 사용자 승인 시에만 commit, 이후 별도 승인 시에만 push.

---

# HANDOFF — 74일차 방문 달력 폴리싱

확인일: 2026-09-15. 수동 표준 모드. 사용자 제공 74일차 지시서에 따라 Codex가 직접 구현했어. 공용 작업판은 활성화하지 않았어.

## 현재 상태

방문 라벨 모서리는 **6dp → 2dp**로 바꿨고 실기기 QA와 이전 commit·push까지 끝났어. 사용자가 승인한 전체 폭 장식선을 실제 방문 달력에도 최소 반영했어. 청록색·라벨 크기·3dp 여백·날짜·카오모지 위치·대비와 방문 저장 구조는 그대로야. 공휴일은 지원 연도 결정 지점에서 STOP했어.

| 구분 | 상태 |
|---|---|
| 구현 | 2dp 완료. 장식선 목업 승인 및 production 최소 반영 완료. 공휴일 미구현·정책 판단 대기 |
| 자동 검증 | 관련 테스트 16건·Kotlin compile·diff check 통과 |
| 사용자 QA | 2dp 실기기 완료. 장식선 목업 승인 완료, production 실기기 QA 대기 |
| commit | 2dp 작업 완료 — `49654c2`, HANDOFF `a113dd7`. 현재 장식선 변경은 미커밋 |
| push | 2dp 작업 완료 — 원격 `a113dd7`. 현재 장식선 변경은 미푸시 |
| 전체 완료 | 2dp 폴리싱 완료. 장식선은 production 반영 후 실기기 QA 대기. 공휴일 구현은 사용자 정책 판단 대기 |

73일차 원문은 [방문 달력 최종 인수인계](archive/HANDOFF-2026-09-14-visit-calendar-final.md)에 byte 동일하게 보존했어(SHA256 일치 확인). 과거 사고·실패·폐기 접근도 원문 그대로야. archive 안 상대 링크는 원래 HANDOFF 위치 기준이야. 원문에 QA 완료 표와 과거 QA 대기 문장이 혼재해 있어. 73일차 QA 완료는 현재 사용자 지시서와 원문 최종 표를 기준으로 봐. 이번 QA와 혼동하지 않아.

## Git 시작·종료 상태

- 브랜치: `feature/photo-sticker`.
- 시작 HEAD: `141d6cba7a25db88ed80678efbc8b1c896a87814`. 2dp·목업 commit: `49654c2` (`Polish visit calendar label corners`).
- `49654c2` push 후 local과 origin의 `feature/photo-sticker`가 동일한 것을 확인했어. 이 HANDOFF 결과 갱신은 별도 기록 commit으로 이어서 반영해.
- 시작 tracked 변경 없음. 기존 untracked `.claude/`, `.codex-config.candidate.toml`, `.kotlin/` 모두 보존. 지시서보다 `.claude/`가 추가로 있었어.
- `49654c2` 포함 파일: `VisitCalendarDrawer.kt`, `docs/ai/archive/HANDOFF-2026-09-14-visit-calendar-final.md`, `docs/ai/mockups/visit-calendar-2dp.html`, `docs/ai/mockups/visit-calendar-2dp.png`. 현재 HANDOFF 갱신은 별도 기록 commit 대상이야.
- 최근 관련 커밋: `141d6cb` 인수인계, `02574f1` 방문 달력 본체.

## UI/UX 문법·수정 범위

- 역할: 행동 없는 방문 기록 표시. 선택·속성 조절·객체 행동·위험 행동·완료/저장 UI는 해당 없어.
- 근거: `VisitCalendarDrawer.kt`의 현재 월 종이 달력, 73일차 QA와 현재 지시서에서 승인·유지 확인.
- 진입: Gallery 좌측 drawer 그대로. 토큰 `PaperSurface`, `InkSecondary`, `SealInkNavy`, `SealInkRed` 재사용 유지.
- 기존 variant 범위: 역할·정보 계층·상호작용·container·위치는 같고 명시 승인된 반경만 변경. 신규 문법·외부 디자인 레퍼런스·전역 shape 변경은 없어.
- 실제 production diff는 `.background(VisitFillColor, RoundedCornerShape(6.dp))`의 `6`을 `2`로 바꾼 한 줄이야.
- `#16A7A1`, 자동 대비, 카오모지 4종과 날짜 기반 selector, 현재 월만 표시, 크기·spacing은 그대로야.
- 경미한 조정 1회. 핵심 해결 실패·해결책 역전·범위 확대 없음.
- 이번 세션에 최신 실기기 스크린샷은 제공되지 않았어. 새 미감을 실제로 봤다고 주장하지 않아.

## 공휴일 조사와 STOP

`app/src`와 `gradle`의 holiday/공휴일/대체공휴일/KoreanCalendar/lunar 검색에서 기존 STOP 주석만 발견했어. 날짜 helper·방문 기록은 날짜 포맷·요일·epochDay를 다루며 국내 공휴일 데이터가 없어. build 파일과 version catalog에도 공휴일 제공 의존성이 없어.

| 방식 | 정확도·지원 범위 | 대체·임시공휴일 | 새 dependency·네트워크 | 유지보수·복잡도 |
|---|---|---|---|---|
| 공식 자료 기반 정적 연도별 목록 | 대조한 연도와 발표 시점까지. 2026 월력요항 확인 | 공식 대체공휴일 포함 가능. 이후 임시 지정은 데이터 업데이트 필요 | 둘 다 불필요 | 구현 단순, 매년 및 변경 발표 때 목록 대조 필요 |
| 기존 dependency / 플랫폼 API | 한국 공휴일 판정 지원 확인 못함. 날짜/역법 계산과 휴일 정책은 별개 | 자체 계산으로 임시 지정까지 보장 불가 | 현재 구성만으로 정확한 정책 데이터 확보 불가 | 자체 음력·대체 규칙 계산은 복잡하고 유지 비용 큼 |
| 한국천문연구원 특일 정보 API | 공식 날짜·공공기관 휴일 여부·명칭 제공. 전체 지원 연도는 페이지에 미명시 | 실제 응답의 수록·최신성 검증 필요 | 인증키·앱 네트워크 필요, 새 라이브러리 필요 여부와 별개로 API 연결은 STOP 대상 | 장애·캐시·키·갱신 관리 추가 |

출처(2026-09-15 조회):

- [우주항공청 2026년 월력요항](https://www.kasa.go.kr/prog/bbsArticle/BBSMSTR_000000000010/view.do?bbsId=BBSMSTR_000000000010&nttId=B000000001860Pe2zT3): 매년 달력 제작 기준 발표, 대체공휴일 포함. 2025-06-30 발표라 이후 정책 변경까지 검증됐다는 뜻은 아니야.
- [한국천문연구원 특일 정보](https://www.data.go.kr/data/15012690/openapi.do): REST/XML, 인증키 필요. 공공기관 휴일 여부·명칭 제공, 무료·이용허락범위 제한 없음 표시. 실제 API 호출이나 키 발급은 하지 않았어.
- [Android ICU Calendar](https://developer.android.com/reference/android/icu/util/Calendar): 날짜 필드와 역법 계산 문서. 한국 공휴일 정책을 정확히 반환하는 내장 기능은 확인하지 못했어.

**권장:** 공식 자료를 대조한 정적 연도별 목록. 단 몇 년까지 지원할지와 연도별 갱신 부담은 제품 선택이므로, 사용자 지시서 13절의 지원 연도 STOP 경계에 따라 공휴일 구현만 멈췄어. 2026 우선 지원 또는 다음 해 포함 등은 미승인 후보야. 지원 연도 미확정, 현재 앱의 공휴일·대체공휴일·임시공휴일 지원 없음. 임시공휴일 추정, 새 dependency·API·권한·Room 변경 없음.

재개 조건: 지원 연도와 유지 방침 결정 → 해당 연도 최신 공식 목록 및 이후 변경 발표 대조 → 최소 날짜 판정과 색상 분리 구현. 정책을 자체 계산하거나 미래 정확성을 약속하지 않아.

현재 색상은 방문 채움+자동 대비가 요일색보다 우선하고 토요일 navy·일요일 red·평일 기본색이야. 추후 구현은 `방문 채움+자동 대비 > 공휴일/대체공휴일 red > 토요일 navy > 평일 기본색`과 일요일 red를 지켜. 토요일 공휴일·방문 공휴일·12월→1월 테스트가 필요해. 이번 공휴일 테스트는 로직 미구현으로 미실행이야.

## 귀여운 요소 검토

- 방문이 드문 달은 기존 문구와 카오모지로 충분한 여백을 유지해.
- 5~7일 연속은 라벨·얼굴이 이미 반복되므로 추가 반복 장식 필요성이 낮아.
- 방문이 많은 달은 청록 면적이 커져 여백 보존이 중요해.
- 공휴일 red가 늘어나는 상황은 navy/red/teal 정보가 늘어 작은 낙서도 시선을 분산할 수 있어.
- 현재 판단은 **추가 없음 권장**. 작은 소인·월별 낙서도 해결할 문제가 뚜렷하지 않아 후보로 채택하지 않았어.
- 최초에는 목업을 만들지 않았고, 이후 사용자 추가 지시로 아래 비교 목업을 제작했어. production 장식·애니메이션 추가는 없어.

### 74일차 추가 지시 — 비교 목업 완료

- 사용자 확인(2026-09-15): 목업과 실기기에서 2dp 디자인이 마음에 든다고 확인했고, 이어서 commit·push를 명시적으로 요청했어. **2dp 실기기 QA 통과와 Git 반영 승인 완료**로 기록해. 공휴일 방식·지원 연도나 추가 장식 승인으로 확대하지 않아.

- 방식: 앱 밖 독립 [HTML 목업](mockups/visit-calendar-2dp.html), [브라우저 캡처](mockups/visit-calendar-2dp.png). 로컬 HTML을 브라우저로 열면 돼. 앱 route·debug screen·dependency는 추가하지 않았어.
- 2026년 10월 한 달 전체를 3개 나란히 비교해. A는 10/1~7 연속 7일(주말 포함), B는 2·9·21일 3회, C는 16일 방문 샘플이야. 하단 횟수도 샘플 수치야.
- 기본 2dp, 이전 6dp로 비교 가능. 공휴일 가정을 끄면 현재 앱처럼 주말색만 보여. 목업에서만 10/3 개천절(토), 10/5 대체공휴일(월), 10/9 한글날(금)을 사용해. 위 공식 월력요항에서 확인한 샘플이며 연간 공휴일 production 구현은 아니야.
- B에서 토요일+공휴일 red와 대체공휴일 red, 방문한 공휴일 teal을 동시에 확인할 수 있어. A에서는 주말·공휴일에 방문 라벨이 우선해.
- 앱 토큰 색상, 304px 패널/256px 달력, 34px 셀, 3px 안쪽 여백, 글자 크기를 dp/sp 수치에 대응했어. 날짜 기반 epochDay mod 4와 4종 카오모지를 재현했어. Android 폰트·실제 dp 밀도와 브라우저 CSS 렌더링 차이는 남아 있어.
- 자동 점검: Node에서 실제 HTML script 실행해 3개 달력·총 26개 라벨·공휴일 전환 시 방문 라벨 수 유지·2/6px 전환 통과. 최초 점검 명령의 PowerShell 인용 오류는 stdin 전달로 수정했어(앱 코드 오류 아님).
- Chrome headless 캡처를 직접 열어 3개 월 전체, 라벨·색상·텍스트 배치를 확인했어. 첫 제한 환경 실행의 GPU 실패 후 독립 임시 프로필과 허용된 권한으로 캡처 성공. 사용자 브라우저 프로필은 사용하지 않았어.
- 내 시각 판단: A 연속 7일이 작은 종이 흔적이라는 의도를 가장 잘 보여줘. 라벨 사이 여백이 유지돼 한 덩어리로 붙지 않고, C의 16일도 뭉치지 않아 보여. 이는 브라우저 목업 의견이며 사용자 QA 승인은 아니야.
- 실제 filesDir·visit_record·방문 횟수·streak 접근 없음. 이번 추가 작업에서 production 수정 없음. 기존 2dp 한 줄 변경만 그대로 남아 있어.
- 추가 산출물은 `docs/ai/mockups/`의 HTML·PNG와 이 기록이야. HTML 정적 점검 및 `git diff --check` 통과. 앱 코드 변경이 없어 Gradle 재실행은 불필요, 직전 관련 16건·compile 통과 기록 유지. 실기기 2dp QA와 `49654c2` commit·push까지 완료했어.

### 74일차 추가 장식선 목업 승인 및 production 최소 구현

- 사용자 지정 조합을 같은 HTML 목업에 추가했어: 상단 `────── ✦ ──────`, 하단 `⋆｡────────｡⋆`.
- 첫 목업 확인 후 사용자가 양옆 선이 중간에서 끊겨 보인다고 피드백했어. 상·하단 선을 주차 구분선과 같은 달력 전체 폭으로 늘리고, 중앙 `✦`와 양끝 `⋆｡`·`｡⋆`만 작은 문자로 남겼어.
- 두 장식선은 기존 상·하단 구분선 자리를 대신해 달력 높이를 거의 늘리지 않아. 선 두께는 주차 구분선과 같은 0.5px이고, 본문 잉크보다 훨씬 낮은 대비(`InkSecondary` 계열을 약 29%로 표현)와 9px 장식 문자 크기를 사용했어.
- `장식선 보기`를 켜고 끌 수 있어 같은 3종 전체 달력에서 기존 가로선과 직접 비교 가능해. 2dp·공휴일 가정 비교 기능도 유지했어.
- Chrome headless로 선 길이 수정 후 PNG를 다시 만들고 직접 확인했어. 양쪽 여백까지 선이 이어져 기존보다 덜 끊겨 보이며, 상단 장식은 요일·날짜보다 뒤로 물러나고 하단 장식은 방문 횟수 문구보다도 조용하게 보여.
- HTML script 실행 점검: 상단 장식 3개, 하단 장식 3개, 전체 폭 선 조각 9개, 끄면 기존 구분선 6개, 방문 라벨 26개, 2px 반경 유지 모두 통과. `git diff --check` 통과.
- 사용자 확인(2026-09-15): 늘린 선 길이가 정확히 원한 지점이고 완벽하다고 명시했어. 이 승인으로 정적 프로토타입 단계를 통과했어.
- production은 `VisitCalendarDrawer.kt` 안에서 기존 상·하단 구분선 자리를 전용 장식 composable로 교체했어. 상단은 전체 폭 선 사이 중앙 `✦`, 하단은 양끝 `⋆｡`·`｡⋆` 사이 전체 폭 선이야. 선은 0.5dp, `InkSecondary` 29% 색, 문자는 9sp로 목업과 맞췄어.
- 장식 Row를 기존 구분선과 여백의 총높이에 맞추고 footer 위 여백을 조정해 달력 전체 밀도와 위치를 유지했어. 장식은 `clearAndSetSemantics`로 접근성 읽기에서 제외했어.
- 실제 방문 데이터·filesDir·visit_record·streak 접근 없음. route·dependency·저장 구조 변경 없음. 현재 변경은 `VisitCalendarDrawer.kt`, `docs/ai/mockups/visit-calendar-2dp.html`, 갱신된 PNG와 이 HANDOFF야.
- 상태: 목업 승인과 production 최소 구현 완료, production 실기기 QA 대기. 현재 변경은 commit·push하지 않았어.

### 74일차 하단 장식선 대비·문자 후보 목업

- 사용자 추가 지시(2026-09-15): 현재 `⋆｡────────｡⋆` 문자열은 유지하되 색 대비를 소폭 높인 안을 먼저 확인하고, 존재감이 부족하면 `୨୧────────୨୧`와 `୨୧ ──────── ୨୧` 후보를 비교하기로 했어. 대비를 높여도 별무늬 가독성이 약해 사용자가 `୨୧ ──────── ୨୧`를 선택했어.
- 같은 HTML 목업에 `하단 장식` 선택을 추가했어. 기본 선택은 기존 문자열 + 대비 상향(약 39%)이고, 기존 대비(약 29%)와 두 `୨୧` 후보를 같은 2026년 10월 A/B/C 전체 달력에서 비교할 수 있어.
- 상단 `✦`와 선 길이, 달력 크기, 2dp 라벨, 공휴일 가정은 유지했어. 실제 앱 코드·route·dependency·filesDir·visit_record·streak는 건드리지 않았어.
- Chrome headless로 기본 대비 상향 상태의 PNG를 갱신하고 3개 달력 전체를 직접 확인했어. 문자열은 양끝에 남고 선은 기존 승인 길이를 유지하며, 대비 상향안도 방문 라벨보다 먼저 시선을 끌 정도는 아니었어. 후보 선택별 최종 채택은 아직 사용자 확인 대기야.
- 사용자가 선택한 `୨୧ ──────── ୨୧`를 production 하단 장식에 반영했고, 하단만 대비를 39%로 올렸어. 상단 `✦`는 기존 29% 대비를 유지해 시선 우선순위를 보존했어. 목업 기본 선택도 선택안으로 맞췄어.
- 자동 검증: 단일 Gradle 프로세스로 `VisitCalendarTest`·`VisitHistoryStorageTest` 관련 테스트 통과, `:app:compileDebugKotlin` 성공. 병렬 실행 시 Kotlin incremental cache 잠금 환경 오류가 있었지만 중단 후 단일 실행으로 재검증했어.
- 상태: 선택안 production 반영 및 자동 검증 완료, 실기기 QA 대기. 현재 변경은 commit·push하지 않았어.

## 자동 검증·실행 환경

- 기존 로컬 Gradle 9.4.1 및 Android Studio JBR 사용. 저장소에 실행 wrapper가 없어 기존 로컬 `gradle.bat` 사용.
- 첫 offline 실행은 `settings.gradle.kts:8`의 기존 `foojay-resolver-convention:0.10.0`을 찾지 못해 설정 단계에서 실패. 앱 코드 오류가 아니야. build 설정을 수정하지 않았어.
- 기존 캐시·네트워크 접근 권한으로 재실행. `:app:testDebugUnitTest --tests com.postcardmemory.ui.gallery.VisitCalendarTest --tests com.postcardmemory.utils.VisitHistoryStorageTest :app:compileDebugKotlin --console=plain`.
- 장식선 production 반영 후 재검증 결과: `BUILD SUCCESSFUL in 2m 32s`. `VisitCalendarTest` 8건, `VisitHistoryStorageTest` 8건, 실패·에러 0을 XML 결과로 확인했고 `compileDebugKotlin`도 성공했어. 기존 코드의 deprecated API 등 warning만 있었어.
- 최종 `git diff --check` 통과. production diff는 장식선 전용 composable과 기존 구분선 교체 및 밀도 보정으로 한정했어. 기존 LF→CRLF 경고는 있어.
- 전체 unit test는 국소 시각 변경에 비례해 실행하지 않았어. 새 장식을 그대로 되풀이하는 테스트도 추가하지 않았어.
- 실기기 설치·실행·삭제·초기화·계측 테스트·사용자 데이터 조작은 실행하지 않았어.

## 실기기 QA·남은 위험·다음 행동

**2dp 실기기 QA는 완료했고, 새 장식선 production 실기기 QA는 대기 중이야.** 사용자가 목업의 전체 폭 선 길이와 인상을 승인한 뒤 실제 코드에 반영했어.

- 확인 대상은 `141d6cb` 위 2dp 작업트리였고, 확인된 결과를 `49654c2`에 commit했어.
- Gallery 좌측 메뉴 → 방문 달력. 여러 방문 라벨을 함께 보고 종이 조각 느낌인지, 너무 직각이거나 답답한지 확인해. 닫고 다시 열어 날짜·카오모지·횟수 유지도 확인해.
- 5~7일 연속 기록이 없다면 기록·기기 날짜를 조작하지 않고 자연적으로 쌓인 뒤 확인해. 그 조건은 미검증으로 남겨.
- 장식선 QA는 Gallery 좌측 메뉴에서 방문 달력을 열어 상·하단 선이 목업처럼 양옆까지 충분히 이어지는지, 본문·방문 라벨보다 먼저 보이지 않는지, 전체 높이와 footer 간격이 자연스러운지 확인하면 돼. 공휴일 QA는 미구현이라 해당 없어.
- history marker와 `visit_record.txt`, total/streak·하루 중복 방지, Intro·Gallery 기타 기능, Room·Migration·저장·공유 경로는 변경하지 않았어.
- 기존 위험 유지: history와 요약 저장은 단일 트랜잭션이 아니며 history 실패 시 그날 라벨이 없을 수 있어. 자정 너머 프로세스를 유지할 때 방문 집계 범위도 기존 그대로야. 이번에 이를 해결하거나 실기기에서 검증했다고 주장하지 않아.
- 과거 데이터 사고·복구 범위와 Intro 자연 도달 미검증은 archive 기록을 유지해. 코드 커밋을 사용자 데이터 백업으로 해석하지 않아.
- 다음 행동: 사용자가 장식선 production 실기기 화면을 확인해. 통과 후 별도 명시 요청이 있으면 현재 4개 tracked 파일만 commit·push할 수 있어. 공휴일은 지원 연도·유지 방침 결정 후 별도 재개 가능해.
- 전용 task checklist 도구가 없어 진행 메시지와 이 문서로 단계를 기록했어. Goal 대체나 하위 agent 위임 없음.
