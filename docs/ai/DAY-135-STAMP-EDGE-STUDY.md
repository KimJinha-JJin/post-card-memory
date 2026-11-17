# post-card-memory 135일차 — 우표 테두리 구조 조사 및 C안 중심 디자인 목업 결과

2026-11-17 · 패코(Codex) · 수동 표준 모드 · 앱 적용/디자인 최종 채택 전 정적 검토야.

## 지금 확인할 것

- [비교 HTML](../mockups/day135-stamp-edge-study/index.html)을 내려받거나 로컬에서 더블클릭하면 돼. 파일 하나에 사진이 내장돼서 서버·설치·인터넷 연결이 필요 없어. GitHub의 HTML 소스 화면 자체는 실행 화면이 아니야.
- 상단 C1/C2/C3 → 다섯 실제 크기 → 가장자리 6배·네 모서리 → 실제 설계값 순서야. 이전 A/B/개선 B/C는 하단 펼치기에 보존했어.
- [60·38dp 캡처](../mockups/day135-stamp-edge-study/c-small-cards.png), [가장자리·네 모서리 캡처](../mockups/day135-stamp-edge-study/c-edge-corners.png)를 같이 남겼어. 브라우저 주소·프로필·개인 경로가 없는 본문만 잘랐어.
- 사용자는 얕은 C 방향을 선호한다고 정했지만, C2/C3의 최종 채택이나 실제 앱 적용은 승인하지 않았어.

## C1/C2/C3의 차이

360dp 화면의 사진 한 변은 `(360−12×2−8×2)/3 = 106.667dp`야. 아래 값은 흰 선 중심선이 아니라 **바깥 윤곽 설계값**이야.

| 항목 | C1 · 기존 C 보존 | C2 · 정렬된 얕은 천공 | C3 · 더 좁고 얕은 천공 |
|---|---:|---:|---:|
| 한 변 안쪽 홈 수 | 16 | 15 | 16 |
| 홈 중심 간격 | 6.433dp | 6.766dp | 6.365dp |
| 홈 입구 폭 | 3.733dp | 3.247dp | 2.801dp |
| 홈 깊이 | 1.120dp | 1.015dp | 0.827dp |
| 평평한 종이 연결부 | 2.700dp | 3.518dp | 3.565dp |
| 입구 폭 / 간격 | 약 58% | 48% | 44% |
| 깊이 / 간격 | 약 17.4% | 15% | 13% |
| 모서리 | 변의 원호와 다른 반지름의 1/4 원 홈 | 같은 원의 교차·격자 정렬 | 같은 원의 교차·격자 정렬 |

C1은 기존 B의 홈 깊이만 60%로 줄인 원본이야. C2/C3는 깊이뿐 아니라 입구 폭·연결부·모서리 위상까지 함께 바꿨어. 모두 원호와 직선으로만 구성했고 무작위 찢김·섬유·애니메이션은 없어.

C2/C3의 계산은 HTML `shallowAligned`에 있어. 천공 간격을 p, 입구 반폭을 a, 깊이를 d라 하면 원의 반지름은 `R=(a²+d²)/(2d)`야. 원 중심을 종이 바깥으로 `o=R−d`만큼 옮겨 얕은 원호를 만들어. 같은 원의 격자를 모서리까지 잇고 `p=한 변/(분할 수−2o의 비율)`로 풀어서 첫 홈만 압축되지 않게 했어. 모서리의 작은 홈도 같은 원이 두 직선 변과 만난 부분이라, 큰 둥근 코너를 따로 붙이지 않아.

C1의 기존 사진 clip·inset 흰 선은 그대로야. C2/C3는 같은 바깥 윤곽의 안쪽 3dp만 흰 선으로 남겨. 세 안의 명목 흰 여백은 3dp, 그림자는 0, 사진·배경·사진 배율·정사각 비율은 같아. 다만 **C1과 새 후보는 흰 선 생성 방식도 다르므로 순수 수치 하나만 바꾼 비교는 아니야.** C2/C3끼리는 같은 방식으로 그려.

## 실제 크기와 시각적 판단

Microsoft Edge에서 로컬 HTML을 직접 열어 사진/단색, 100% 배율, 넓은 창과 절반 폭의 창을 확인했어. 320/360/480은 기기나 브라우저 viewport를 측정한 값이 아니라 갤러리 크기 계산에 넣은 논리 화면 폭이야. 실제 Android 물리 크기·Android Path 출력 품질까지 검증한 것은 아니야.

| 비교 조건 | 사진 한 변 | C1 깊이 | C2 깊이 | C3 깊이 | 관찰 |
|---|---:|---:|---:|---:|---|
| 320dp 화면 | 93.333dp | .980dp | .888dp | .724dp | 홈이 남아. C2/C3의 모서리는 전체적으로 더 네모나게 읽혀. |
| 360dp 화면 | 106.667dp | 1.120dp | 1.015dp | .827dp | C2의 평평한 연결부가 보이고, C3는 더 조용해. |
| 480dp 화면 | 146.667dp | 1.540dp | 1.395dp | 1.138dp | 세 후보의 가장자리 차이를 비교하기 쉬워. |
| 작은 우표 | 60dp | .630dp | .571dp | .465dp | 윤곽이 약해지고 C2/C3 차이도 작아. |
| 캐릭터 손 사진 가정 | 38dp | .399dp | .362dp | .295dp | 셋 모두 거의 네모난 사진처럼 읽혀. 천공 정체성을 충분히 보장하지 못해. |

6배 확대에서는 원호와 평평한 연결부가 반복되고 네 모서리에 끊김·별도 큰 돌출이 보이지 않았어. C2는 평평한 부분과 얕은 홈의 균형을 탐색하는 후보, C3는 사진을 더 우선하는 비교 후보야. C3의 차이는 확대에서 보이지만 작은 카드에서 의미가 크다고 판단하지 않았어. 깊이를 키워 축소 한계를 억지로 해결하지 않았고 채택 결론도 내리지 않았어.

로컬 대한민국 500원 태극기 우표 WebP(331×295)를 직접 열어 봤어. 사진에서 둥근 홈 사이의 평평한 종이와 모서리 천공의 연결을 참고했어. 원본은 낮은 해상도의 시각 참고이며 치수·천공 규격 실측 자료가 아니야. 실제 우표의 가로세로 비율과 물리적 분리 결과까지 복제하지 않았어. 참고 파일은 원본 프로젝트에 두고 읽기만 했으며 HTML·앱 자원·Git에 편입하지 않았어.

## 기존 A/B/개선 B의 보존과 한계

A는 `PostcardRenderSpec.createPinkingPath`의 Float32 연산 순서를 JavaScript로 이식했어. Android 밀도 3.0 모형에서 사진 clip은 inset 0, 흰 선은 3dp/2 inset으로 그려. 임의로 새 톱니 디자인으로 재해석하지 않았어. Android 캡처와 픽셀 일치는 직접 검증하지 못했고, 안전한 대체 사진 구성·Canvas antialias·dp/px 모형의 차이가 있어.

기존 B는 반원 홈·별도 1/4 원 모서리야. 모서리 중심부터 첫 홈까지 약 .79배 일반 간격이라 경계에서 반복이 압축돼. 개선 B는 정렬된 격자를 시도했지만 38dp 안쪽 선 중첩 우려가 남아 있어. 이전 추가 선 보정은 자동 승인 검토가 2회 제한 초과로 거절했고, 사용자가 현재 상태 유지를 선택했어. 이번 C 작업에서 개선 B의 함수나 그리기 방식은 바꾸지 않았어. 그 보류 접근을 C의 채택 근거로 사용하지 않아.

C 후속 수정 직전 임시 스냅샷과 비교해 A/B/개선 B/C1의 그리기 명령은 4안×5크기×3 DPR(1,1.5,2), **60건 모두 같았어**. 내장 사진 바이트도 같았어. 이 검사는 DOM stub과 Path2D 기록을 쓰는 명령 보존 검사이며 브라우저 픽셀 회귀나 Android 테스트가 아니야. 새 C 15샘플의 유한 좌표·양수 연결부·얕은 깊이 조건도 확인했어.

## 실제 앱의 테두리 생성 방식

기준은 PR #29 병합 `3665af30b5a3f6d5568cc94fd857c86d19807b4b`야. PR #29가 MERGED이고 `build-and-test`가 JVM·APK·instrumentation APK 컴파일 단계를 실제 성공한 것을 조회했어. 기존 HANDOFF의 JVM 1,289건·카드 6장 0픽셀 차이는 134일차 기록이며 이번에 재실행한 결과가 아니야.

공통 원본은 `app/src/main/java/com/postcardmemory/utils/PostcardRenderSpec.kt:776 createPinkingPath`야. PNG가 아니라 직선 Path로 삼각 톱니를 만들어.

- inset을 뺀 짧은 변×.032, 최소 2px가 톱니 깊이야.
- 모서리 사선 길이는 깊이×1.5야.
- 모서리를 뺀 변 길이 / (깊이×2.3)를 정수로 버리고 최소 8개의 톱니를 배치해. 실제 간격은 남는 변 길이/개수라 균등해.
- 각 톱니는 반 간격에서 안으로 들어갔다가 바깥선으로 돌아와. 네 모서리는 사선으로 이어.
- `ui/components/StampPhoto.kt:33 createPinkingPath`는 위 공통 Path를 `asComposePath()`로 감싸는 wrapper야. 134일차 이후 계산 복사본이 아니야.

## 전체 사용처와 변경 영향

아래 경로는 모두 `app/src/main/java/com/postcardmemory/` 아래야. 겉모양이 비슷한 것과 실제 호출을 구분했어.

| 기능 | 실제 호출 경로 | 크기·잘림·그림자 조건 |
|---|---|---|
| 월별 갤러리·검색 | `ui/gallery/GalleryScreen.kt GalleryMonthlyGridPage → GalleryMonthlyGridItem → components/StampCard.kt StampCardContent → StampPhoto` | 3열, 좌우12dp·간격8dp. 정사각 `ContentScale.Crop`, 흰 선3dp. 선택 overlay도 `PinkingPhotoShape`. |
| 연못 | 같은 `GalleryMonthlyGridItem → StampCard → StampCardContent` | 연잎 안쪽22dp(연잎 없으면8dp), 활성 scale1.03·elevation6. 부모 그림자는 CircleShape/둥근 사각형이라 우표 윤곽과 별개. |
| 흔들기 랜덤 결과 | `ui/gallery/GalleryRandomPostcardOverlay.kt GalleryRandomPostcardOverlay → StampPhoto` | `randomOverlayLayout`: 폭×.62/높이×.40/라벨 여유 중 최소. 흰 선3dp, 최종 크기 측정 뒤 scale. 그림자는 `PinkingPhotoShape`, elevation 최대10dp·clip=false. |
| 동글이·쫑쫑컵 무대 | `ui/gallery/GalleryPlayStage.kt GalleryPlayStageCard / DonggeuliCardContent → StampCardContent` | 카드폭92/84/76dp, 동글이 사진은 캐릭터 폭의.5라 약34–41dp. 손이 하단을 덮어. |
| 레이스카 | `ui/gallery/PixelPostcardRaceCar.kt PixelPostcardRaceCar → StampCardContent` | 사진 padding7dp, 부모 scale·회전 존재. |
| 레이스 관중·우승자 | `ui/gallery/PostcardRaceOverlay.kt PostcardRaceOverlay / RaceWinnerScoreboard → StampCardContent` | 관중은 무대 카드폭×.88에 padding5dp, 우승자는60dp. |
| 카메라 실시간 | `ui/camera/CameraScreen.kt CameraPreviewScreen → PinkingPhotoGuide` | 정사각 카메라 위 padding6dp, 주선4dp·halo8dp. 사진 자체를 톱니로 자르지 않고 선만 겹쳐. |
| 촬영 뒤 자르기 | 같은 파일 `CropEditorScreen → PinkingPhotoGuide` | 가로.86·정사각, 사진 zoom/offset, 주선4dp·halo9dp. |
| 상세·꾸미기 앞면 | `ui/detail/DetailScreen.kt PostcardPreviewContent → PostcardRenderSpec.drawBaseContent → drawStampPhoto` | 상위 폭×.8 정사각 미리보기. 2048 논리 좌표를 target 크기로 scale해. |
| 앞면 저장·공유 | `ui/detail/DetailViewModel.kt exportPostcardToGallery / sharePostcard → utils/PostcardImageExporter.kt exportToGallery / exportForSharing → renderFrontThenOutput → createPostcardBitmap → drawBaseContent → drawStampPhoto` | 2048² ARGB_8888, PNG 출력. 사진 밖에 엽서 배경이 있는 구조라 이미지 전체가 투명 우표 cutout인 것은 아니야. |
| 뒷면 저장·공유 | `DetailViewModel.exportBackPostcard → exportBackForSharing / exportBackToGallery` | 전달된 뒷면 bitmap 출력. 앞면 우표 Path 호출이 아니야. |

`PinkingPhotoShape.createOutline`는 inset0 사진 clip, `StampPhoto` 흰 선은 inset=선굵기/2로 별도 계산돼. 카메라 가이드는 max(주선,halo)/2 inset이야. `outlineColor` RGB는 흰색으로 강제되고 alpha만 이어받아.

`PostcardRenderSpec.drawStampPhoto:1125`는 18 논리px 흰 선·inset9·그림자(+22,+26), ARGB(85,18,12,28)를 써. STAMP 사진은 기본1000×scale(.7..1.3), 중심(1024,802.5)이야. 공통 Path를 바꿔도 선·그림자·배율이 같아지는 것은 아니야. POLAROID/TAPED_FILM/LETTER 레이아웃은 별도 분기야.

`ui/gallery/GalleryCardPressFeel.kt GalleryCardPhotoShadowShape.createOutline:173`은 사진 영역 사각형 그림자야. 공통 우표 Path를 바꿔도 이 그림자는 그대로야. 랜덤 결과 그림자는 반대로 공통 Path를 따라 바뀌어.

## 이미지 자원과 독립 유사 구현

- 현재 우표 사진 프레임용 PNG/WebP/Vector Drawable은 발견하지 못했어. `res` 자원은 PNG33개/XML6개이며 우표 프레임 vector나 WebP는 없어.
- `drawable-nodpi/editor_cutout_stamp.png`: `DetailScreen`의 도장 탭 삽화. 종이 우표 프레임이 아니야.
- `seal_stamp_hand.png`: `SealStampInteraction.kt SealStampHandOverlay`의 기존 도장 손. 새 펀칭 연출의 자원으로 승인된 것이 아니야.
- `gallery_paper_tile.png`: 갤러리·꾸미기 배경 질감. `gallery_date_paper_strip.png`: 월 헤더·종이 라벨 navigation 자원. `visit_calendar_paper.png`, `visit_calendar_newspaper_ribbon.png`: 달력/우체통 종이·신문 리본. 천공 Path와 독립이야.
- `PostcardLayoutPicker`는 우표 텍스트 목록이고 별도 천공 preview가 없어. `SealShapes`의 원형 소인·물결 취소선은 잉크 표현이야.
- `PhotoStickerEdgeStyle.kt photoStickerPaperSpec / scissorPaperOutline`는 스티커 가위질 종이 외곽으로 화면·출력이 spec을 공유하지만 우표 Path는 호출하지 않아. 달력의 테이프 찢긴 끝도 별도 계산이야.
- 미래 우체통 봉투·소인·색 덩어리 모형은 우표 사진 프레임과 독립이야. 이번 Kotlin 검색에서 별도 앱 백업 ZIP exporter는 발견하지 못했지만 OS 자동 백업까지 없다는 결론은 아니야.
- 목업 사진은 기존 `editor_cutout_photo.png` 카메라 오림 사진만 내장했어. 운영 자원을 수정하지 않았고 개인 사진을 쓰지 않았어.

## 실제 앱 적용 방법과 예상 위험

| 방법 | 장점 | 예상 위험·검토할 것 |
|---|---|---|
| 코드 윤곽 | 크기별로 선명하고 134일차 공통 Path 경로를 활용할 수 있어. | inset·corner·최소px·카메라 halo와 Compose/Android Canvas antialias를 함께 확인해야 해. |
| PNG/WebP 프레임 | 고정된 종이 표면을 표현하기 쉬워. | 비율·늘이기·모서리 왜곡, alpha halo, 화면과 exporter의 다른 scale/filter, 디코딩 메모리. 크기별 이미지가 늘면 유지보수도 늘어. |
| 코드 윤곽+질감 | 윤곽은 공유하고 표면만 자원으로 표현할 수 있어. | 질감 tiling·밀도·alpha·색·내보내기 scale를 맞춰야 해. 질감으로 실제 천공을 대신하면 안 돼. |

성능 저하는 측정하지 않았어. 예를 들어 추가2048² ARGB bitmap 한 장은 약16MiB의 픽셀 버퍼를 요구할 수 있지만, 실제 추가 할당·캐시·프레임 저하는 구현·측정 전에는 확정할 수 없어. 지금 HTML의 비용을 Android 앱 성능으로 환산하지 않아.

후속 승인 시 예상 범위는 공통 Path와 필요한 Compose clip/선·exporter 선/그림자 검토, 카메라 halo, 독립 집기 그림자 정책, 동글이 손 가림·레이스 작은 사진 검증이야. 기존 공통 구조를 재사용할 후보지만 HTML을 Kotlin에 그대로 복사하는 적용은 아니야. Room·저장 형식 변경을 요구하는 디자인은 아니며 이번에는 앱 파일을 전혀 수정하지 않았어.

## 검증·Git·재개 조건

- 로컬 자동검증: **실행** — HTML JS 문법, 기준 60건 명령 보존, 15샘플 기하 조건, 링크·자원·diff·민감정보 훅 검사. 임시 검증 스크립트는 앱 테스트에 추가하지 않았어.
- 브라우저 시각 검증: **실행** — Edge의 로컬 HTML 사진/단색, 다섯 크기, 6배 원호·네 모서리, 넓은/좁은 창. 마지막에는 관찰 한계 설명만 추가했고 윤곽 수치와 그리기는 바꾸지 않았어.
- 실사용 기기 instrumentation: **불필요**. 실기기 감각 QA: **불필요**(정적 목업·문서만). 앱 설치·emulator·connected task·로컬 전체 Android 빌드는 실행하지 않았어.
- GitHub Actions CI: HTML·PNG는 현행 `ci-verification-scope.sh`의 Markdown 전용 예외가 아니어서 PR 자동 전체 검증 대상이야. workflow·Ruleset을 바꾸거나 CI를 수동 재실행하지 않아. 실제 최종 PR/CI 결과는 대화 완료보고에 남겨.
- TEST-COVERAGE-MAP: **변경 없음**. Kotlin·앱 테스트 수·보호 범위 무변경이야.
- 기준 main·origin/main `3665af3`, 기존 브랜치 `design/day135-stamp-edge-study` 재사용. 조사 시 기존 원격 브랜치/PR은 없었어. 작성 시점에는 commit 전이며 같은 목적 PR 한 개로 마감해. main 병합은 별도 승인 대기야.
- 원본 프로젝트의 미커밋 HANDOFF·개인정보 보류 자료·새 참고 WebP와 기존 미추적 파일을 보존했어. 개발용 worktree의 목업/보고/HANDOFF/보존 이력만 Git 대상이야.
- 다음 판단은 C1/C2/C3의 선호와 38dp 약화를 수용할지 여부야. 이 문서는 추가 구현·제품 확정 승인이 아니야.

향후 펀칭 애니메이션을 검토하려면 먼저 정적 윤곽·정사각/직사각 비율·기준 좌표·종이/사진 mask·손/도구/종이 레이어·전환 시점·저장/출력 동일성·reduced motion 정책을 별도 승인해야 해. 이번에는 전제만 기록했고 애니메이션·효과음·터치·전환·꾸미기 진입을 만들지 않았어.