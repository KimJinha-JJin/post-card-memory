$ErrorActionPreference = "Stop"

$expectedBranch = "feature/photo-sticker"
$currentBranch = (git branch --show-current).Trim()

if ($currentBranch -ne $expectedBranch) {
    throw "현재 브랜치가 '$currentBranch'입니다. '$expectedBranch' 브랜치에서 실행해 주세요."
}

git fetch origin

$detailPath = "app/src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt"
$layoutPath = "app/src/main/java/com/postcardmemory/ui/components/PostcardLayoutPicker.kt"
$temporaryWorkflowPath = ".github/workflows/apply-photo-drawer-fix.yml"
$scriptPath = "tools/apply_customization_pager_fix.ps1"

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

$layoutLines = git show "origin/main:$layoutPath"
if ($LASTEXITCODE -ne 0) {
    throw "origin/main에서 PostcardLayoutPicker.kt를 읽지 못했습니다."
}
[System.IO.File]::WriteAllText(
    $layoutPath,
    (($layoutLines -join "`n") + "`n"),
    $utf8NoBom
)

$detailText = [System.IO.File]::ReadAllText($detailPath)
$detailText = $detailText.Replace("`r`n", "`n")

if (-not $detailText.Contains("import androidx.compose.foundation.pager.HorizontalPager")) {
    $detailText = $detailText.Replace(
        "import androidx.compose.foundation.layout.size`n",
        "import androidx.compose.foundation.layout.size`nimport androidx.compose.foundation.layout.wrapContentHeight`n"
    )

    $detailText = $detailText.Replace(
        "import androidx.compose.foundation.rememberScrollState`n",
        "import androidx.compose.foundation.pager.HorizontalPager`nimport androidx.compose.foundation.pager.rememberPagerState`nimport androidx.compose.foundation.rememberScrollState`n"
    )
}

$drawerStateBlock = @'
    var openedDrawerName by rememberSaveable {
        mutableStateOf(
            DetailDrawerSection.LAYOUT.name
        )
    }
'@

$pagerStateBlock = @'
    var openedDrawerName by rememberSaveable {
        mutableStateOf(
            DetailDrawerSection.LAYOUT.name
        )
    }

    val customizationPagerState = rememberPagerState(
        pageCount = { 2 }
    )
'@

if (-not $detailText.Contains("val customizationPagerState = rememberPagerState")) {
    if (-not $detailText.Contains($drawerStateBlock)) {
        throw "DetailScreen.kt에서 서랍 상태 블록을 찾지 못했습니다. 파일을 변경하지 않습니다."
    }

    $detailText = $detailText.Replace(
        $drawerStateBlock,
        $pagerStateBlock
    )
}

$startMarker = @'
                DetailDrawer(
                    title = "레이아웃 꾸미기"
'@

$endMarker = @'
                if (
                    dateFormatUpdateState
'@

$startIndex = $detailText.IndexOf($startMarker)
$endIndex = $detailText.IndexOf($endMarker, $startIndex)

if ($startIndex -lt 0 -or $endIndex -lt 0) {
    throw "DetailScreen.kt에서 기존 네 개 꾸미기 서랍 구간을 찾지 못했습니다. 파일을 변경하지 않습니다."
}

$newPagerBlock = @'
                HorizontalPager(
                    state = customizationPagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    beyondViewportPageCount = 1,
                    verticalAlignment = Alignment.Top
                ) { page ->
                    when (page) {
                        0 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {
                                DetailDrawer(
                                    title = "레이아웃 꾸미기",
                                    summary = selectedLayout.label,
                                    expanded =
                                        openedDrawerName ==
                                                DetailDrawerSection
                                                    .LAYOUT
                                                    .name,
                                    enabled = controlsEnabled,
                                    onClick = {
                                        openedDrawerName =
                                            if (
                                                openedDrawerName ==
                                                DetailDrawerSection
                                                    .LAYOUT
                                                    .name
                                            ) {
                                                ""
                                            } else {
                                                DetailDrawerSection
                                                    .LAYOUT
                                                    .name
                                            }
                                    },
                                    modifier =
                                        Modifier.fillMaxWidth(0.92f)
                                ) {
                                    PostcardLayoutPicker(
                                        selectedLayout =
                                            selectedLayout,
                                        onLayoutSelected = { layout ->
                                            viewModel.updateLayoutStyle(
                                                layout.name
                                            )
                                        },
                                        enabled = controlsEnabled,
                                        modifier =
                                            Modifier.fillMaxWidth()
                                    )
                                }

                                Spacer(
                                    modifier = Modifier.height(14.dp)
                                )

                                DetailDrawer(
                                    title = "배경 꾸미기",
                                    summary = selectedPattern.label,
                                    expanded =
                                        openedDrawerName ==
                                                DetailDrawerSection
                                                    .BACKGROUND
                                                    .name,
                                    enabled = controlsEnabled,
                                    onClick = {
                                        openedDrawerName =
                                            if (
                                                openedDrawerName ==
                                                DetailDrawerSection
                                                    .BACKGROUND
                                                    .name
                                            ) {
                                                ""
                                            } else {
                                                DetailDrawerSection
                                                    .BACKGROUND
                                                    .name
                                            }
                                    },
                                    modifier =
                                        Modifier.fillMaxWidth(0.92f)
                                ) {
                                    PostcardBackgroundPicker(
                                        selectedColorArgb =
                                            pc.backgroundColorArgb,
                                        hasBackgroundImage = false,
                                        enabled = controlsEnabled,
                                        onColorSelected = { colorArgb ->
                                            viewModel.updateBackgroundColor(
                                                colorArgb
                                            )
                                        },
                                        onPickImage = {},
                                        onRemoveImage = {},
                                        selectedPattern = selectedPattern,
                                        onPatternSelected = { pattern ->
                                            viewModel
                                                .updateBackgroundPattern(
                                                    pattern.name
                                                )
                                        },
                                        modifier =
                                            Modifier.fillMaxWidth()
                                    )
                                }

                                Spacer(
                                    modifier = Modifier.height(14.dp)
                                )

                                DetailDrawer(
                                    title = "글귀 꾸미기",
                                    summary = selectedFont.label,
                                    expanded =
                                        openedDrawerName ==
                                                DetailDrawerSection
                                                    .TEXT
                                                    .name,
                                    enabled = controlsEnabled,
                                    onClick = {
                                        openedDrawerName =
                                            if (
                                                openedDrawerName ==
                                                DetailDrawerSection
                                                    .TEXT
                                                    .name
                                            ) {
                                                ""
                                            } else {
                                                DetailDrawerSection
                                                    .TEXT
                                                    .name
                                            }
                                    },
                                    modifier =
                                        Modifier.fillMaxWidth(0.92f)
                                ) {
                                    PostcardFontPicker(
                                        selectedFont = selectedFont,
                                        onFontSelected = { font ->
                                            viewModel.updateMessageFont(
                                                font.name
                                            )
                                        },
                                        enabled = controlsEnabled,
                                        modifier =
                                            Modifier.fillMaxWidth()
                                    )
                                }

                                Spacer(
                                    modifier = Modifier.height(14.dp)
                                )

                                DetailDrawer(
                                    title = "날짜 꾸미기",
                                    summary = selectedDateFormat.label,
                                    expanded =
                                        openedDrawerName ==
                                                DetailDrawerSection
                                                    .DATE
                                                    .name,
                                    enabled = controlsEnabled,
                                    onClick = {
                                        openedDrawerName =
                                            if (
                                                openedDrawerName ==
                                                DetailDrawerSection
                                                    .DATE
                                                    .name
                                            ) {
                                                ""
                                            } else {
                                                DetailDrawerSection
                                                    .DATE
                                                    .name
                                            }
                                    },
                                    modifier =
                                        Modifier.fillMaxWidth(0.92f)
                                ) {
                                    PostcardDateFormatPicker(
                                        selectedFormat =
                                            selectedDateFormat,
                                        onFormatSelected = { dateFormat ->
                                            viewModel.updateDateFormat(
                                                dateFormat.name
                                            )
                                        },
                                        enabled = controlsEnabled,
                                        modifier =
                                            Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        else -> {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                PhotoStickerPickerPanel(
                                    enabled = controlsEnabled,
                                    modifier =
                                        Modifier.fillMaxWidth(0.92f)
                                )
                            }
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    repeat(2) { pageIndex ->
                        val selectedPage =
                            customizationPagerState.currentPage ==
                                    pageIndex

                        Box(
                            modifier = Modifier
                                .size(
                                    if (selectedPage) {
                                        11.dp
                                    } else {
                                        8.dp
                                    }
                                )
                                .background(
                                    color =
                                        if (selectedPage) {
                                            BrutalDeepViolet
                                        } else {
                                            BrutalLavender
                                        },
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = BrutalBlack,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Text(
                    text =
                        if (
                            customizationPagerState.currentPage == 0
                        ) {
                            "기본 꾸미기"
                        } else {
                            "스티커 사진 꾸미기"
                        },
                    color = BrutalDeepViolet,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp)
                )

'@

$detailText =
    $detailText.Substring(0, $startIndex) +
    $newPagerBlock +
    $detailText.Substring($endIndex)

[System.IO.File]::WriteAllText(
    $detailPath,
    $detailText,
    $utf8NoBom
)

if (Test-Path $temporaryWorkflowPath) {
    Remove-Item $temporaryWorkflowPath -Force
}

if (Test-Path $scriptPath) {
    Remove-Item $scriptPath -Force
}

git add $detailPath $layoutPath $temporaryWorkflowPath $scriptPath

git commit -m "Group customization drawers into swipe pager"
git push origin $expectedBranch

Write-Host "완료: 첫 페이지에 네 개 꾸미기 서랍, 두 번째 페이지에 스티커 사진, 하단 점 표시를 적용했습니다."
