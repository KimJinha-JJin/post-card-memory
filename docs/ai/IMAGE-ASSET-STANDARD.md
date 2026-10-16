# IMAGE-ASSET-STANDARD — 이미지 자원 제작 표준 v1

작성: 2026-10-15, 104일차 3부. 코드 기준 `e8ab9c9` / `feature/photo-sticker`.

프리셋 개정: 2026-10-16. 사용자 제공 이미지 생성 테스트 결론을 반영했다. 아래 자산 수·크기·기존 외형 관찰은 104일차 조사 기록이며 최신 PNG 현황을 재집계한 값이 아니다. 이번 개정은 제작 지침만 바꾸며 기존 자산 교체를 승인하지 않는다. 105일차에 이 프리셋으로 교체된 21장의 현재 크기·좌표는 [IMAGE-ASSET-MAP](IMAGE-ASSET-MAP.md) 상단 105일차 절이 기준이다.

## 1. 역할과 적용 범위

[IMAGE-ASSET-MAP](IMAGE-ASSET-MAP.md)은 **현재 자산과 사용처 지도**, 이 문서는 **앞으로 생성·도입·교체할 자산의 제작 기준**이다. 현황을 이 문서에 중복 집계하지 않는다. 실제 적용 전 최신 코드·MAP을 재확인한다. 여기의 현재 크기는 영구 규격이나 일괄 리사이즈 승인이 아니다.

- **확정 규칙:** 용도에 맞는 해상도를 쓴다. 무조건 1254×1254로 시작하지도, 무조건 작게 만들지도 않는다.
- **권장 규칙:** 아래 자산군의 시각 방향·크기 산정 방식을 사용하되 실제 용도가 다르면 재분류한다.
- **추가 조사 필요(REVIEW):** 밀도·최대 확대·crop·공유 사용처·좌표 결합이 불명확하면 숫자를 확정하지 않는다. 안전한 스타일 초안과 미정 기술 항목을 분리하고, 명세 조사로 넘긴다.
- 생성 프롬프트 작성, 이미지 생성, 앱에 도입, 코드 변경은 별도 단계다. 프롬프트나 조사 결과를 앱 수정·기기 명령·commit/push 승인으로 해석하지 않는다. 실행 안전선은 [AGENTS.md](../../AGENTS.md) 5·6·8·9절을 따른다.

### 조사 근거

PNG 31개(추적 30, 보호 untracked 1)의 헤더·크기와 실제 사용 코드를 대조했다. `drawable-nodpi` 26개, `drawable` 5개이며 런처는 adaptive icon XML을 거친다. 관련 문서는 MAP, HANDOFF, DECISIONS와 workflow의 UI/UX 문법 표준이다.

대표 이미지 `editor_cutout_sticker`, `editor_cutout_stamp`, `editor_label_peek`, `quick_select_camera_hand`, `seal_stamp_hand`, `gallery_paper_tile`, `visit_calendar_newspaper_ribbon`, `postcard_app_icon_hand_envelope`를 직접 열어 시각 방향을 확인했다. 코드 근거는 아래 각 군의 파일·상수다. 앱 화면을 새로 실행하거나 실기기 QA를 한 것은 아니다.

104일차의 16장 최적화·기기 측정·QA는 MAP 9절과 HANDOFF의 **기존 기록**이다. 이 문서 작성 중 재측정하지 않았다. 특히 MAP의 103일차 표와 과거 후보 설명에는 Before 값이 남아 있으므로 최신 파일과 9절 정정을 함께 읽는다.

## 2. 공통 시각 언어

### 2.1 명시적 기본 프리셋 — 「실사 인쇄물 오림 질감」

제작 논리: **실제 대상/장면 → 한 장의 실제 사진 → 얇은 신문·잡지 지면에 인쇄 → 가위로 오림 → 한 장의 납작한 인쇄물 조각**. 새로 그린 빈티지 삽화에 신문 효과를 얹는 것이 아니다. 이미 지면에 인쇄되어 있던 실제 사진을 종이째 오린 모습이어야 한다. 이는 생성물의 시각적 제작 논리이며 실제 촬영·출판 이력을 사실로 주장하는 뜻은 아니다.

- 실제 사진 기반의 자연스러운 물체 입체감·명암을 유지하되 최종 자산은 한 장의 납작한 종이다. 종이 자체를 입체 공예나 3D 물체로 만들지 않는다.
- 흑백 또는 매우 낮은 채도, **중성 회색 중심**. Warm Paper UI 위에 쓴다는 이유로 그림까지 갈색·노란색으로 물들이지 않는다.
- 얇은 신문/잡지 인쇄용지, 미세하고 촘촘한 망점, 약한 잉크 농도 편차, 아주 미세한 번짐, 약한 인쇄 흐림. 하프톤은 가까이서 느껴질 정도이며 디자인의 주인공이 아니다.
- 실제 가위질처럼 작은 불규칙성이 있지만 UI용으로 정돈된 외곽. 종이 단면을 두꺼운 스티커 테두리로 만들지 않는다. 작은 화면에서는 질감보다 실루엣·동작이 먼저 읽혀야 한다.

**물리 법칙:** 손+머그컵처럼 여러 요소가 있어도 실제 사람이 실제 컵을 든 **하나의 장면을 먼저 한 장의 사진으로 촬영**한 다음 전체를 한 종이에 인쇄해 한 조각으로 오린다. 별도 손 조각+컵 조각을 겹치는 페이퍼 콜라주는 금지한다. 실제 장면 내부의 자연스러운 가림·접촉·명암은 유지한다. quick select의 손 위 블록도 장면의 실제 물체로 취급하며, 블록 속 캐릭터/활자는 인쇄 내용일 수 있지만 별도 오림 레이어로 덧붙이지 않는다.

### 2.2 하위 프리셋과 적용 제외

- **캐릭터 인쇄물 오림:** 빠끔이·동글이 등은 실사 인간/동물로 바꾸지 않는다. 단순한 캐릭터 삽화 → 얇은 신문/잡지 종이에 인쇄 → 캐릭터 외곽을 가위로 오린 한 조각. 단순함·동글동글함·작은 점눈·천천히 손으로 그린 듯한 약간의 비정형성을 유지한다. 매끈한 디지털 벡터 대신 미세한 망점·약한 잉크 농도 편차·인쇄 종이 질감을 적용한다. 이는 허용된 캐릭터 삽화 예외이지 빈티지 선화·스케치로 재해석하라는 뜻이 아니다.
- **활자 인쇄물 오림:** 실제 신문/잡지의 활자 부분을 사각형이나 띠 형태로 종이째 잘라낸 한 조각. 실사 사물을 억지로 추가하지 않는다. 새 디지털 타이포그래피 카드·UI 패널처럼 만들지 않는다. 요청 문구의 정확한 철자와 작은 크기 가독성을 검수하며 생성 도구의 정확한 활자 재현을 보장하지 않는다.
- **자동 적용 제외:** 종이 배경 자체, 종이 섬유 타일, 마스킹테이프 재질, 도장용 단색 마스크, 사용자 사진, 데이터 저장용 이미지, 기능상 별도 재질 언어가 확정된 자원. 4절의 해당 규칙을 유지한다. 기존 앱 아이콘의 펜선 등 별도 언어를 이 프리셋으로 무단 교체하지 않는다.
- 캐릭터·활자 예외와 실제 사물이 함께 있는 자산은 어느 내용이 어디에 인쇄되어 있는지 먼저 정한다. 설명이 모순되면 관련 생성 규격을 REVIEW로 남긴다. 기존 앱의 런타임 합성 구조를 금지하거나 수정하는 규칙이 아니라 **한 생성 자산 내부의 질감·구성 규칙**이다.

### 2.3 공통 금지와 고정 프롬프트 문구

실사 및 두 하위 프리셋 모두에 공통 금지: 페이퍼 콜라주, 여러 종이 레이어 공예, 찢어진 종이 공예, 새로 그린 빈티지 삽화, 깨끗한 벡터 아이콘, 픽토그램 중심 표현, 연필 스케치·색연필·크레파스, 판화·에칭·목판화, 강한 크로스해칭, 과도한 점묘, 과장된 굵은 하프톤, 강한 세피아, 지나치게 노란 골동품 종이, 광택 있는 3D 렌더, 외곽 흰색/검정/빨강/노랑 halo. 캐릭터의 단순 삽화와 활자의 사각/띠 절단은 2.2절의 명시적 예외다. 적용 제외 자원에 이 금지를 전파하지 않는다.

각 완성형 프롬프트에는 아래에서 **알맞은 핵심 블록 하나 + 공통 인쇄 블록 + 공통 금지 블록**을 원문 그대로 넣는다. 매번 분위기 표현을 새로 요약하지 않는다. 핵심 블록 3개를 동시에 넣어 사진·캐릭터·활자 지시를 충돌시키지 않는다. 복합 실사 장면에는 실사 블록의 마지막 두 문장을 반드시 유지한다.

**실사 핵심 블록**

```text
This is NOT a newly drawn vintage illustration. It must look like a real photograph that had already been printed on thin newspaper or magazine paper, and that printed paper was then cut out with scissors along the subject's outline as one single flat paper piece. Preserve the natural volume, lighting, contact and occlusion of the photographed subject within the print. The entire subject must originate from one real photograph before printing. Do not assemble separate paper cutouts into a collage.
```

**캐릭터 핵심 블록 — 실사 블록 대신 사용**

```text
Keep the character as a simple, rounded illustration with tiny dot eyes and slight slowly hand-drawn irregularity; do not turn it into a realistic person or animal. It must look like that character illustration had already been printed on thin newspaper or magazine paper, then cut out with scissors along its outline as one single flat paper piece. Preserve its existing identity. Do not assemble separate paper cutouts into a collage.
```

**활자 핵심 블록 — 실사 블록 대신 사용**

```text
It must look like an existing printed passage of newspaper or magazine type, cut out together with its thin paper as one single rectangular or strip-shaped piece. Preserve the requested wording exactly. Do not turn it into a newly designed digital typography card or add a photographic object merely to make it realistic. Do not assemble separate paper cutouts into a collage.
```

**공통 인쇄 블록**

```text
Use black and white or very low saturation, centered on neutral gray. Use thin printing paper, very fine and closely spaced halftone dots visible mainly on close inspection, subtle ink-density variation, minute ink bleed and mild print softness. Halftone must never dominate the design. Keep tiny scissor-cut irregularities along a tidy UI-ready edge. The result is one flat printed-paper piece, not a layered craft object.
```

**공통 금지 블록**

```text
No paper collage, layered paper crafts, torn-paper crafts, newly drawn vintage illustration, clean vector icon, pictogram-led rendering, pencil sketch, colored pencil, crayon, printmaking, etching, woodcut, heavy crosshatching, excessive stippling, coarse exaggerated halftone, strong sepia, deeply yellow antique paper, glossy 3D rendering, or white, black, red or yellow edge halos. The simple character illustration or printed-type treatment is allowed only when its corresponding preset is explicitly selected.
```

### 2.4 참조 이미지 해석

색·망점부터 모사하지 말고 `사진/삽화 여부 → 종이에 인쇄된 내용 → 외곽 오림/사각 영역 절단 → 한 조각/여러 겹`을 먼저 읽는다. 기본값은 한 장의 인쇄물 조각이다. 참조가 콜라주라면 대상·포즈만 참고할지 구분하고 레이어를 자동 복제하지 않는다. 참조의 단순 캐릭터를 실사화하지 않는다. 접근하지 못한 이미지는 본 것처럼 설명하지 않는다.

## 3. 크기를 정하는 방법

1. **실제 표시 영역**과 이미지의 **유효 그림 영역**을 구분한다. dp 상자 전체를 쓰는지, alpha bbox를 잘라 맞추는지 확인한다.
2. 대상 density를 기록해 `표시 px = dp × density`를 계산한다. MAP의 3x는 계산 가정, 450dpi/2.8125x는 당시 기기 측정이다. 이를 모든 지원 기기의 값으로 고정하지 않는다.
3. 최대 zoom·animation scale·회전 시 필요한 화면 영역·cover-crop·export 출력을 포함한다. 공유 자산은 가장 큰 실제 사용처를 기준으로 한다.
4. crop 뒤 사용되는 원본 영역이 필요한 표시 px를 충족하는지 본다. 예를 들어 사용하는 crop 폭이 전체 원본의 f라면 필요한 canvas 폭은 대략 `필요 crop px / f`다. alpha 자동 crop도 같은 원리로 유효 그림 픽셀을 따로 계산한다. canvas 전체를 그대로 표시하는 자산에는 alpha 여백을 이유로 배율을 이중 적용하지 않는다.
5. decode sample과 밀도 확대를 계산에 넣는다. 원본을 줄이고 기존 sample을 유지하면 이중 축소될 수 있다. 출력 PNG 바이트가 작아도 decode RAM이 작다는 뜻은 아니다.
6. **권장 여유:** 정적 오림은 최대 표시 px의 약 1.25~1.5배를 검토 시작점으로 삼을 수 있다. 이는 104일차 시계·quick select 사례에 가까운 여유이지 합격선·상한이 아니다. 섬세한 인쇄 결·큰 손·texture·export는 별도 판단한다. 이미 검증된 314² 기능줄도 이 비율로 다시 축소하지 않는다.
7. RAM 추정은 실제 decode 폭×높이×bytes/pixel(ARGB_8888 가정이면 4)이다. 동시 상주 수, 정적 캐시, 중간 Bitmap·GPU 메모리는 따로 본다. APK 영향은 실제 인코딩·패키징 전에는 추정 또는 REVIEW이며 픽셀 수만으로 정확한 용량을 약속하지 않는다.

**제작 목표 크기와 생성 도구의 실제 출력 크기를 구분한다.** 생성 도구가 정확한 px를 지원한다고 가정하지 않는다. 목표 px·비율을 프롬프트에 쓰고, 결과 헤더를 확인해 별도 승인된 자산 준비 단계에서 맞춘다. 이번 표준 작성은 생성·리사이즈를 실행하지 않는다.

## 4. 자산군별 기준

아래 “현재”는 코드·파일에서 확인한 사실, “권장”은 새 제작 시 기준이다. 공통 출력은 현행 로더와 맞는 PNG를 기본 후보로 한다. 투명 자산은 실제 alpha를 쓰고 체크무늬를 그림으로 넣지 않는다. 포맷 전환·리소스 폴더 이동은 별도 호환성 판단 없이 하지 않는다.

신규 오림의 질감은 2절 프리셋을 따른다. 아래 과거 자산 설명은 새로운 삽화·세피아·공예 콜라주 제작 지시가 아니다. 대상이 캐릭터/활자이거나 적용 제외 자원이면 해당 예외를 먼저 선택한다.

### A. 편집 상위 기능 아이콘 — editor_cutout_*

- **위치·용도:** `ui/detail/EditorBottomTabBar.kt`, DetailScreen의 사진·배경·글귀·스티커·테이프·도장·낙서 탐색.
- **현재:** 7장 모두 314×314 RGBA / `drawable-nodpi`. `EDITOR_CUTOUT_SAMPLE=1`, alpha>16 bbox 자동 crop, 비율 유지하며 32×24dp 상자에 fit. process 정적 캐시, 단일 기능줄 사용.
- **권장:** 같은 용도의 신규 자산은 1:1 canvas, 기존 314²를 비교 출발점으로 삼되 3절로 유효 그림 해상도를 확인한다. 중앙의 한 물체, 2절의 실사 인쇄물 오림(캐릭터/활자는 하위 프리셋), 가늘고 불규칙한 종이 단면. 외곽 잘림 없이 배치하고 큰 빈 여백을 만들지 않는다. 여백 비율을 임의의 고정 숫자로 강제하지 않는다.
- **crop·후처리:** loader의 bbox crop은 허용된 현행 동작. 숨은 alpha 점이 bbox를 넓히지 않게 검사한다. 생성 시 색을 완성하며 임의 tint는 기본값이 아니다. 좌표 상수 결합은 없지만 sample과 bbox에 결합한다.
- **우선도·QA:** 해상도보다 작은 화면 식별성 우선. 실제 32×24dp 이하에서 종류 구분·7개 간 시각 무게·alpha 가장자리·이중 sample 없음 확인. 사용자가 붙이는 확대 가능한 스티커 콘텐츠와 혼동하지 않는다.

### B. 빠끔 / 소형 캐릭터 — editor_label_peek

- **위치·용도:** `ui/detail/PaperLabelSubcategoryNav.kt`, 세부 선택줄 5탭 공용 얼굴.
- **현재:** 1254² RGBA, sample 4, 원본 기준 `(376,352)`에서 `720×656` crop을 /4하여 사용. 표시 높이 20dp. 정적 캐시. **COORDINATE-COUPLED: pixel crop + sample**.
- **권장:** 작은 눈·입과 둥근 실루엣, 낮은 대비의 종이·망점. 기존 교체는 1:1 canvas와 얼굴 위치·손·crop 경계를 유지한다. 완전히 새 캐릭터의 원본 크기는 crop 후 20dp 및 최대 이동/확대 기준으로 산정(REVIEW).
- **alpha·crop·후처리:** 외부 투명, 기존 여백·crop 임의 trim 금지. 색 필터를 새로 전제하지 않는다.
- **우선도·QA:** 표정과 선택 칸 위치가 먼저다. 얼굴 튀어나오는 높이·이동·라벨과 겹침·crop 잘림·5탭 공통 결과 확인.

### C. 손 연출 — 도장 / 핀셋 / 테이프 / 랜덤 엽서

- **위치:** `SealStampInteraction.kt`, `PhotoStickerPlaceInteraction.kt`, `MaskingTapePlaceInteraction.kt`, `ui/gallery/GalleryRandomPostcardOverlay.kt`.
- **현재:** 4장 1254² RGBA, 1:1, imageResource→drawImage. 도장·핀셋 300dp, 테이프 260dp. 도장은 진입 scale 최대 1.12도 고려한다. 랜덤 손은 엽서 변의 1.0~1.6배로 viewport에서 계산하므로 하나의 dp로 고정하지 않는다.
- **공유·상주:** 도장 손은 인트로와 편집 공용, 핀셋은 사진·글귀·라벨 공용. 연출 구성 중 decode되며 종료 즉시 RAM 회수까지 보장하지 않는다.
- **권장:** 손과 도구가 함께 촬영된 한 장의 사진을 인쇄·오린 질감과 접촉 실루엣을 우선한다(2절). 새로 그린 선묘나 손/도구 조각 합성은 쓰지 않는다. 1254²는 조사 당시 값이지 신규 기본값이 아니다. 최대 표시와 animation·공유 사용처로 산정하며 작은 기능줄 규격을 적용하지 않는다.
- **alpha·결합:** 투명 배경, anchor가 분율이어도 **COORDINATE-COUPLED: composition/anchor**다. 도장 `(0.459,0.431)`, 테이프 `(0.399,0.259)`, 랜덤 `(0.670,0.223)` 등 접촉점과 손목 방향을 유지한다. 동일 canvas의 비례 축소와 crop·재구도는 다르다. 색 후처리는 기본 없음.
- **QA:** 접촉 순간 도구·엽서 위치, 진입/퇴장·회전·잘림·최대 크기 화질, 공용 인트로/스티커 종류 확인. 신규 손의 정확한 크기·접촉점은 별도 조사.

### D. quick select 손 — quick_select_*_hand

- **위치·용도:** `GalleryQuickSelectFan.kt`의 geometry와 `GalleryScreen.kt`의 `GalleryQuickSelectHand`. 5개 바로가기, 갤러리 안 사용.
- **현재:** 5장 720² RGBA, 180×180dp canvas, 닫힌 묶음에서도 구성. 1:1. `QUICK_SELECT_HAND_SOURCE_PX=1254f`는 **논리 좌표**, Bitmap 해상도가 아니다.
- **권장:** 같은 손·블록 위치와 구도를 유지한 인쇄 그림. 동일 사용 조건이면 현재 720²가 비교 기준이며 4x에서 180dp=720px다. 새 화면·확대 조건에는 재산정한다. 장당 약 2.07MB(720²×4)라는 decode 추정과 5장 상주를 함께 고려한다.
- **alpha·crop·후처리:** 여백도 geometry 일부다. trim·손목 끝 위치·블록 위치 변경 금지(별도 재측정 없이는). 원본 색을 유지하고 활성 테두리를 그림에 굽지 않는다. **COORDINATE-COUPLED: logical + hit + animation**.
- **QA:** 5개 블록 읽힘, 닫힘/펼침, 접촉 영역·활성 테두리·각도·가장자리. 작은 픽셀 결보다 블록 기호가 먼저 읽혀야 한다.

### E. 종이 / 배경 texture — 타일·달력 종이·라벨 띠를 분리

- **공통 권장:** 낮은 대비의 따뜻한 생활 종이. 그림자·텍스트·장식 캐릭터를 굽지 않는다. 텍스트 가독성과 넓은 면의 반복 품질은 높은 우선도다. 범용 크기 하나로 합치지 않는다.
- **타일:** `GalleryScreen.kt` / `DetailScreen.kt`, `gallery_paper_tile` 1254² RGB 불투명, 1:1. 360dp 반복 shader, 갤러리·편집 바닥 공용. 원본 decode·상주 가능. 양끝 연결이 중요하며 임의 crop/alpha 여백 금지. 최대 타일 표시 px와 섬유 스케일로 산정. 예를 들어 3x라면 한 타일 1080px지만 전 기기 보장값은 아니다. 색 변경은 공유 화면 전체 영향 검토. QA: 이음새·섬유 크기·글 대비·편집 바닥 연속성.
- **달력 종이:** `VisitCalendarDrawer.kt`, 1122×1402 RGB 불투명, 현재 약 4:5(정확한 비율은 1122:1402). 월 페이지 cover-crop, 연/월 picker 조각 crop 3곳 공용. 닫힌 drawer에서도 상주한 기존 측정 기록이 있다. 새 크기는 페이지 최대 영역과 crop 배율로 산정(REVIEW), 억지 정사각화 금지. 여백 없이 종이 자체를 채우고 부분 crop에도 결이 성립해야 한다. 색 후처리 변경은 해당 3곳 확인. QA: 페이지·picker 모두 결/글 가독성, 잘린 부분에 장식·글 없음.
- **라벨 띠:** `gallery_date_paper_strip` 2172×724 RGBA, 3:1 canvas. `GalleryScreen.kt` / `PaperLabelSubcategoryNav.kt` 공용. 원본 `(89,172)/2011×379` crop과 선택줄 sample 4가 결합. 월 라벨은 내용에 따라 폭이 달라지고 선택줄 종이 높이는 25dp다. 갈색·회색 필터와 뒤집기가 있으므로 색을 중복으로 진하게 굽지 않는다. 새 크기 REVIEW, alpha·고정 crop·여백 유지. **COORDINATE-COUPLED: pixel crop + sample + shared**. QA: 두 화면·5탭, 긴 월 문구·선택/미선택·뒤집힘.
- **보호:** `postcard_paper_fiber_tile.png`는 미사용 보호 untracked다. 새 표준 도입을 이유로 채택·이동·삭제하지 않는다.

### F. 달력 소형 장식 — visit_calendar_newspaper_ribbon

- **위치·현재:** `VisitCalendarDrawer.kt`, 좌우 리본이 하나의 bitmap 공유. 192×144 RGBA, 4:3 canvas, 표시 24×18dp. 그림 자체는 여백 때문에 더 작다. 갤러리 상주 기록.
- **권장:** 작은 신문 인쇄 리본의 실루엣, 실제 표시에서 뭉치지 않는 매듭·날개. 동일 역할은 현재 크기와 비교하되 새 장식은 유효 그림 px로 재산정한다. alpha 여백을 제거하면 화면상의 크기가 바뀌므로 그대로 trim하지 않는다.
- **crop·후처리·결합:** 원본 전체 표시, 비율 상수 `1448f/1086f`와 구도가 결합한다. 임의 crop·색 필터 추가 없음.
- **QA:** 좌우 균형·작은 화면 식별성·텍스트 간섭·4:3 보존. 큰 달력 장식까지 192×144를 강제하지 않는다.

### G. 도장 silhouette — seal_dog_paw / heart / pigeon_track / star

- **위치:** `ui/components/SealShapes.kt`와 `utils/PostcardImageExporter.kt`, 화면·저장/공유 공용.
- **현재:** RGBA PNG, 288~480px 범위의 서로 다른 비율, `drawable/`. 화면 tint와 exporter SRC_IN에 쓰인다. 일반 drawable의 밀도 확대 decode 가능성을 포함해 계산한다. exporter는 사용 후 recycle한다. 모든 화면 경로의 실제 상주량은 REVIEW.
- **권장:** alpha 실루엣에 읽히는 모양, 배경 투명. 종이 테두리·그림자·하프톤 결을 임의로 넣어 기존 잉크 renderer와 이중 처리하지 않는다. 고유 비율을 보존하고 과도한 빈 여백을 피한다. tint는 의도된 후처리다.
- **크기·crop:** 작은 패널 52~72dp만으로 원본을 정하지 않는다. 엽서 위 최대 크기·zoom·export px를 조사한 뒤 결정(REVIEW). 밀도 폴더 이동·crop도 결과 변경 가능성이 있다.
- **QA:** 최소 크기의 모양 식별, 여러 tint·잉크 결손, preview/export·옛 엽서 재내보내기 동일성. 자산 교체는 저장 필드를 안 바꿔도 기존 엽서의 재출력 모양을 바꿀 수 있다.

### H. 앱 아이콘 — postcard_app_icon_hand_envelope

- **위치·현재:** `mipmap-anydpi-v26/ic_launcher*.xml`→drawable-nodpi 전경. 1254² RGB 불투명, 1:1. 앱이 아닌 런처에서 사용, 일반/둥근 XML 공용.
- **권장:** 따뜻한 종이와 손·봉투의 단순 거친 펜선. 일반 기능줄의 투명 오림과 다르다. alpha 제거/추가를 임의 기본값으로 삼지 않는다.
- **크기·여백·crop:** 실제 adaptive mask·safe area·지원 밀도·런처 변형을 조사해 결정(REVIEW). MAP의 108dp/432px 설명만으로 교체 규격을 확정하지 않는다. 마스크 안 핵심 실루엣을 보존하고 모서리 crop을 검증한다. 색 후처리는 기본 없음.
- **QA:** 작은 런처·둥근/기타 마스크·불투명 배경·기존 브랜드 식별. 구 `ic_launcher_foreground` 미참조는 삭제 승인이 아니다.

### I. 갤러리 장면 장식 — 레트로 시계 3장

- **위치·현재:** `GalleryRetroClock.kt`. 몸체 720×394→168×92dp, 왼손 480×360→112×84dp, 커피손 450×600→105.28×140.373dp(위 108dp viewport). RGBA, painterResource, 갤러리 상주 가능, 다른 화면 공유 없음.
- **권장:** 인쇄된 물체의 비율·종이 외곽을 유지한다. 몸체는 현재 720:394, 왼손 4:3, 커피손 3:4 비율을 기준으로 3절 산정. 기존 숫자창·손끝·김 위치는 dp geometry 결합이다. 과장된 입체 장난감으로 만들지 않는다.
- **alpha·crop·후처리:** 기존 canvas와 장면 안 crop을 유지하며 파일을 임의 trim하지 않는다. 색 후처리 없음이 기본.
- **QA:** 숫자창과 실제 숫자 정렬·손끝·커피 김·viewport 잘림·작은 화면 가독성. 픽셀 결보다 장면 정렬 우선.

### J. 기타 신규 이미지

- 먼저 역할을 정한다: 탐색 아이콘 / 캔버스 콘텐츠 / 연출 / 배경 / 시스템 아이콘. 위 군과 실제 소비 방식이 같을 때만 재사용한다.
- 위치·스타일·PNG/다른 포맷·alpha·비율·crop·후처리·공유·상주·좌표 결합·최대 크기·QA를 조사해 명세에 쓴다. 미확인은 모두 REVIEW.
- 확대·export되는 캔버스 스티커에는 작은 탐색 아이콘 규격을 적용하지 않는다. 기본 경로 후보는 `app/src/main/res/drawable-nodpi/`, 파일명은 역할을 드러내는 소문자 snake_case지만 충돌·로더 확인 전 확정하지 않는다.

## 5. 좌표 결합 자산 — COORDINATE-COUPLED ASSET

도입 명세에 `결합 유형 / 소비 파일·상수 / 보존 조건 / 재검증`을 기록한다. 이름에 px가 들어 있다고 실제 픽셀 결합이라고 단정하지 않는다.

| 유형 | 반드시 확인할 것 | 현행 예시 |
|---|---|---|
| 실제 pixel crop | Bitmap width/height를 읽는 곳, srcOffset/srcSize 범위, sample 전후 정수 나눗셈 | 월 라벨 띠, 빠끔 얼굴 |
| 논리 좌표 | 기준 단위→dp/분율 변환, Bitmap 크기를 직접 사용하는지 | quick select 1254f→180dp |
| 구도·anchor | trim·canvas·접촉점·손목 방향·비율 | 도장/핀셋/테이프/랜덤 손 |
| hit·animation·장면 | 터치, 테두리, transformOrigin, 회전·최대 scale, 숫자창/김 좌표 | quick select, 시계 |

**104일차 사례:** quick select PNG는 1254²에서 720²로 줄었지만 `QUICK_SELECT_HAND_SOURCE_PX=1254f`와 블록 좌표는 유지됐다. 화면은 180dp와 분율로 계산하며 Bitmap 해상도를 기준 단위로 쓰지 않기 때문이다. 같은 1:1 canvas 전체를 비례 축소했고 구도·여백을 바꾸지 않았다는 조건이 핵심이다. 이를 “좌표 결합 그림도 자유롭게 crop 가능”이라는 선례로 쓰지 않는다.

반대로 라벨 띠와 빠끔 얼굴은 고정 src crop/sample이 있어 이미지 단독 축소만으로 안전하다고 볼 수 없다. 변경할 경우 두 화면과 로더까지 조사하고 별도 승인 범위에서 적용한다. 코드 변경이 필요한데 이미지 제작만 승인됐다면 관련 도입을 STOP한다.

## 6. 재사용 템플릿 A — 완성형 이미지 생성 프롬프트

작성자는 아래 항목을 **실제 값과 자연스러운 문장으로 채워** 사용자에게 한 번에 복사 가능한 프롬프트를 준다. 괄호 양식을 그대로 넘기지 않는다. 기술 조건이 미확정이면 해당 규격 확정은 멈추고 7절로 전환한다. 스타일만 원하는 경우에는 `앱 도입 규격 미확정인 시안`임을 명시한다.

**자산 하나당 독립 프롬프트 하나.** 여러 자산 요청은 각각 별도 프롬프트로 나누며 contact sheet나 한 장에 여러 자산으로 묶지 않는다. 손+컵처럼 한 장면을 이루는 구성요소는 하나의 자산이다. 각 프롬프트에 해당 핵심·공통 인쇄·금지 블록 전문과 자산별 조건을 다시 넣어, 새 ChatGPT 이미지 채팅에 단독 복사해도 완결되게 한다. “위와 같은 질감”, 프리셋 이름만 쓰기, 다른 프롬프트 참조로 생략하기는 금지한다. 적용 제외 자원에는 해당 자산군의 별도 지침을 넣는다.

```text
post-card-memory의 [사용 화면/기능]에 쓸 [대상] 이미지 한 장을 만들어줘.
용도와 자산군: [탐색/콘텐츠/연출/배경, 해당 군]. 실제 위치: [화면·컴포넌트].
출력: [형식], 목표 도입 크기 [W×H px], 비율 [W:H].
크기 근거: [표시 dp, density 조건, 유효 그림/crop, 최대 확대, 공유 최대 사용처].
도구가 정확한 픽셀 출력을 지원하지 않으면 비율과 구도를 보존하고 실제 출력 크기를 알려줘.
배경: [실제 alpha/불투명 종이]. [여백·canvas 가장자리·손목 등 보존 조건].
프리셋: [실사 / 캐릭터 / 활자 / 적용 제외와 근거].
스타일: [2.3절에서 선택한 핵심 블록 + 공통 인쇄 블록 전문. 적용 제외면 별도 규칙].
포즈·방향: [대상별 자세, 시선/손목/접촉 방향, canvas 내 배치].
필수: [한눈에 읽힐 실루엣, 접촉 방향, 포함할 물체].
금지: [적용 프리셋의 공통 금지 블록 전문 + 자산별 불필요한 글자·로고·UI 배경·장식].
작은 화면: [실제 표시 크기에서 남아야 하는 특징].
후처리: [tint/회색화/갈색 필터/샘플링/없음]을 전제로 [주의사항].
코드 결합: [없음으로 확인/유형과 anchor·crop·비율 보존 조건].
QA 기준: [alpha·실루엣·최소/최대 표시·공유처·preview/export 중 해당 항목].
이미지는 후보이며 앱 파일 교체나 코드 수정은 하지 않아.
```

기술 수치·현재 사실·디자인 제안을 구분한다. 특정 도구의 지원 여부를 검증하지 않고 “정확한 출력·alpha가 보장된다”고 말하지 않는다. 참고 이미지 접근이 없으면 본 것처럼 설명하지 않는다.

## 7. 재사용 템플릿 B — 개발 에이전트용 이미지 명세 조사

Claude Code·Codex·다른 coding agent 모두에게 전달할 수 있는 일반 지시다. 에이전트 이름을 지정할 필요가 없다.

```text
post-card-memory에서 [화면/기능/새 그림의 역할]에 필요한 이미지 명세만 조사해줘.
목표는 이미지 생성이나 코드 수정이 아니라 제작 가능한 규격과 근거를 정리하는 거야.

현재 AGENTS.md, branch/HEAD/working tree, HANDOFF의 현재 위험,
docs/ai/IMAGE-ASSET-STANDARD.md와 IMAGE-ASSET-MAP.md를 먼저 확인해.
현재 세션에 task/progress tracking UI가 있으면 조사→명세→검토를 표시하고 갱신해.
지원이 없으면 그 사실을 짧게 밝히고 진행 상태로 대신해. Task는 Agent/Subagent가 아니야.

해당 production 코드·resource·XML·호출 경로를 읽기 전용으로 추적해.
각 값에 파일·심볼과 확정/권장/REVIEW를 붙여 아래를 보고해:
- 사용 화면/기능, 모든 공유 사용처, 참조 이미지와 자산군
- 실제 표시 dp, density 근거와 필요 px(실측과 가정 분리)
- 최대 확대/animation/회전/viewport, export 필요 px
- 권장 도입 W×H와 산정식, aspect ratio, alpha와 유효 그림/여백
- resource load, density scaling, sample/decode, 정적 캐시·동시 상주
- crop 좌표, 실제 pixel/논리 단위, hit area·anchor·animation geometry 결합
- 좌표 의존 자산의 정확한 배치 사양: canvas 내 bbox·접촉점·보존할 여백과 방향, 좌표 단위와 허용 오차의 근거(미확인은 REVIEW)
- 관련 상수와 보존 조건, 임의 trim/리사이즈 위험
- 권장 파일명·경로(기존 이름 충돌 및 보호 파일 확인)
- 예상 decode RAM과 동시 사용 영향, APK 영향(측정 아닌 추정은 표시)
- 최소/최대 표시 QA, 공유처·preview/export 회귀 항목
- 확정 규격, 권장 선택과 이유, REVIEW 및 재개에 필요한 사실

production/test/설정 수정, 이미지 생성·추가·변경·삭제·이동,
리팩터링, build/test/기기 명령, 설치·초기화, commit/push는 하지 마.
기존 사용자 변경과 보호 untracked를 보존해.
확인할 수 없는 값은 추측하지 말고 REVIEW로 남겨.
제품 역할이 여러 가지면 차이를 보고하고 규격을 임의로 하나로 합치지 마.
결과는 대화에 명세로 반환하고 저장소 파일은 바꾸지 마.
```

반환된 명세의 HEAD·역할·수치 근거가 현재 요청과 맞는지 확인한 뒤 6절 프롬프트로 구성한다. “갤러리 손”만으로 quick select/랜덤/시계 손 중 하나를 골라 720px를 확정하지 않는다.

## 8. 도입·교체 시 확인과 REVIEW

- 이미지 후보: 실제 크기·비율·alpha, 투명 외곽의 검은/흰 halo, 고립된 점, 작은 표시 가독성 확인.
- 앱 도입이 따로 승인됐으면: load/sample/crop/좌표·공유처·최대 표시를 다시 대조하고 필요한 QA를 수행한다. 자동검사와 사람의 미감 확인을 구분한다. MAP의 해당 행을 최신화하고 결과는 HANDOFF에 남긴다.
- 이미지 변경이 테스트 수·의미·보호 범위에 영향을 준 경우만 기존 TEST-COVERAGE-MAP 규칙을 적용한다. 새 포맷이나 최적화를 이 표준으로 자동 승인하지 않는다.
- **REVIEW:** 지원 기기 최대 density/화면폭, 새 자산의 최대 zoom/export, 신규 손의 접촉 geometry, 앱 아이콘 mask 안전영역, 도장 실루엣의 실제 decode 상주량, 새로운 생성 도구의 정확한 출력 크기/alpha 지원. 각각 해당 요청에서만 조사하며 이번에 기기 실행·GPU 원인 분석으로 확대하지 않는다.
- MAP의 과거 후보에 남은 quick select “px 좌표” 표현 등은 최신 정정과 코드가 우선이다. 이 표준에서 과거 결과를 새 실행 승인으로 사용하지 않는다.
