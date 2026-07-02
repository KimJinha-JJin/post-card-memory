$ErrorActionPreference = "Stop"

$expectedBranch = "feature/photo-sticker"
$currentBranch = (git branch --show-current).Trim()

if ($currentBranch -ne $expectedBranch) {
    throw "Run this script from branch: $expectedBranch"
}

$detailPath = "app/src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt"
$layoutPath = "app/src/main/java/com/postcardmemory/ui/components/PostcardLayoutPicker.kt"

# Restore the original layout picker as raw Git bytes.
git restore --source origin/main -- $layoutPath
if ($LASTEXITCODE -ne 0) {
    throw "Could not restore PostcardLayoutPicker.kt"
}

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$detailText = [System.IO.File]::ReadAllText($detailPath)
$detailText = $detailText.Replace("`r`n", "`n")

if ($detailText.Contains("val customizationPagerState = rememberPagerState")) {
    throw "The pager already appears to be applied. Stop and build the project."
}

$detailText = $detailText.Replace(
    "import androidx.compose.foundation.layout.size`n",
    "import androidx.compose.foundation.layout.size`nimport androidx.compose.foundation.layout.wrapContentHeight`n"
)

$detailText = $detailText.Replace(
    "import androidx.compose.foundation.rememberScrollState`n",
    "import androidx.compose.foundation.pager.HorizontalPager`nimport androidx.compose.foundation.pager.rememberPagerState`nimport androidx.compose.foundation.rememberScrollState`n"
)

$stateAnchor = "    val selectedLayout ="
$stateIndex = $detailText.IndexOf($stateAnchor)

if ($stateIndex -lt 0) {
    throw "Could not find the selectedLayout state anchor"
}

$pagerState = @'
    val customizationPagerState = rememberPagerState(
        pageCount = { 2 }
    )

'@
$pagerState = $pagerState.Replace("`r`n", "`n")

$detailText =
    $detailText.Substring(0, $stateIndex) +
    $pagerState +
    $detailText.Substring($stateIndex)

$postcardAnchor = "            postcard?.let { pc ->"
$postcardIndex = $detailText.IndexOf($postcardAnchor)

if ($postcardIndex -lt 0) {
    throw "Could not find the postcard content block"
}

$drawerStartMarker = "                DetailDrawer("
$drawerStartIndex = $detailText.IndexOf($drawerStartMarker, $postcardIndex)
$drawerEndMarker = "                if (`n                    dateFormatUpdateState"
$drawerEndIndex = $detailText.IndexOf($drawerEndMarker, $drawerStartIndex)

if ($drawerStartIndex -lt 0 -or $drawerEndIndex -lt 0) {
    throw "Could not find the existing four drawer block"
}

$drawersBlock = $detailText.Substring(
    $drawerStartIndex,
    $drawerEndIndex - $drawerStartIndex
)

$pagerStart = @'
                HorizontalPager(
                    state = customizationPagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    verticalAlignment = Alignment.Top
                ) { page ->
                    when (page) {
                        0 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {
'@
$pagerStart = $pagerStart.Replace("`r`n", "`n")

$pagerEnd = @'
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
                        Box(
                            modifier = Modifier
                                .size(
                                    if (
                                        customizationPagerState.currentPage ==
                                        pageIndex
                                    ) {
                                        11.dp
                                    } else {
                                        8.dp
                                    }
                                )
                                .background(
                                    color =
                                        if (
                                            customizationPagerState.currentPage ==
                                            pageIndex
                                        ) {
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

'@
$pagerEnd = $pagerEnd.Replace("`r`n", "`n")

$newPagerBlock =
    $pagerStart +
    "`n" +
    $drawersBlock +
    $pagerEnd

$detailText =
    $detailText.Substring(0, $drawerStartIndex) +
    $newPagerBlock +
    $detailText.Substring($drawerEndIndex)

[System.IO.File]::WriteAllText(
    $detailPath,
    $detailText,
    $utf8NoBom
)

Write-Host "DONE"
Write-Host "Page 1: all four customization drawers"
Write-Host "Page 2: sticker photo panel"
Write-Host "Two page dots: enabled"
