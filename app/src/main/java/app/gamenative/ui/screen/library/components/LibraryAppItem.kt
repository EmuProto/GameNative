package app.gamenative.ui.screen.library.components

import android.content.res.Configuration
import app.gamenative.data.GameSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face4
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.gamenative.PrefManager
import app.gamenative.data.LibraryItem
import app.gamenative.service.DownloadService
import app.gamenative.service.SteamService
import app.gamenative.ui.enums.PaneType
import app.gamenative.ui.internal.fakeAppInfo
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.util.ListItemImage

@Composable
internal fun AppItem(
    modifier: Modifier = Modifier,
    appInfo: LibraryItem,
    onClick: () -> Unit,
    paneType: PaneType = PaneType.LIST,
    onFocus: () -> Unit = {},
) {
    var hideText by remember { mutableStateOf(true) }
    var alpha by remember { mutableFloatStateOf(1f) }

    // True when selected, e.g. with controller
    var isFocused by remember { mutableStateOf(false) }

    // Border is used to highlight selected card
    val border = if (isFocused) {
        androidx.compose.foundation.BorderStroke(
            width = 3.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                )
            )
        )
    } else {
        androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        )
    }

    // Modern card-style item with gradient hover effect
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (isFocused) {
                    onFocus()
                }
            }
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = border,
    ) {
        val outerPadding = if (paneType == PaneType.LIST) {
            // Padding to make text easy to read
            16.dp
        } else {
            0.dp
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(outerPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Game icon
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp)),
            ) {
                if (paneType == PaneType.LIST) {
                    ListItemImage(
                        modifier = Modifier.size(56.dp),
                        imageModifier = Modifier.clip(RoundedCornerShape(10.dp)),
                        image = { appInfo.clientIconUrl }
                    )
                } else {
                    val aspectRatio = if (paneType == PaneType.GRID_CAPSULE) { 2/3f } else { 460/215f }
                    val imageUrl = if (paneType == PaneType.GRID_CAPSULE) {
                        "https://shared.steamstatic.com/store_item_assets/steam/apps/" + appInfo.gameId + "/library_600x900.jpg"
                    } else {
                        "https://shared.steamstatic.com/store_item_assets/steam/apps/" + appInfo.gameId + "/header.jpg"
                    }

                    ListItemImage(
                        modifier = Modifier.aspectRatio(aspectRatio),
                        imageModifier = Modifier.clip(RoundedCornerShape(3.dp)).alpha(alpha),
                        image = { imageUrl },
                        onFailure = {
                            hideText = false
                            alpha = 0.1f
                        }
                    )

                    // Only display text if the image loading has failed
                    if (! hideText) {
                        GameInfoBlock(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp),
                            appInfo = appInfo,
                        )
                    } else {
                        val isInstalled = remember(appInfo.appId) {
                            SteamService.isAppInstalled(appInfo.gameId)
                        }
                        // Cute floating icons for install status/family share
                        if (isInstalled || appInfo.isShared) {
                            Row(
                                modifier = Modifier
                                    .align(alignment = Alignment.BottomEnd)
                                    .padding(4.dp) // Padding from the outer card
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                    .height(24.dp)
                                    .padding(2.dp) // Padding for inner icons
                                    .alpha(0.9f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (isInstalled) {
                                    Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onSurface)
                                }
                                if (appInfo.isShared) {
                                    Icon(Icons.Filled.Face4, null, tint = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }
                    }
                }
            }

            if (paneType == PaneType.LIST) {
                GameInfoBlock(
                    modifier = Modifier.weight(1f),
                    appInfo = appInfo,
                )

                // Play/Open button
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        text = "Open",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
internal fun GameInfoBlock(
    modifier: Modifier,
    appInfo: LibraryItem,
) {
    // For text displayed in list view, or as override if image loading fails

    // Determine download and install state
    val downloadInfo = remember(appInfo.appId) { SteamService.getAppDownloadInfo(appInfo.gameId) }
    val downloadProgress = remember(downloadInfo) { downloadInfo?.getProgress() ?: 0f }
    val isDownloading = downloadInfo != null && downloadProgress < 1f
    val isInstalled = remember(appInfo.appId) {
        SteamService.isAppInstalled(appInfo.gameId)
    }

    var appSizeOnDisk by remember { mutableStateOf("") }

    var hideText by remember { mutableStateOf(true) }
    var alpha = remember(Int) {1f}

    LaunchedEffect(Unit) {
        if (isInstalled) {
            appSizeOnDisk = "..."
            DownloadService.getSizeOnDiskDisplay(appInfo.gameId) {  appSizeOnDisk = it }
        }
    }

    // Game info
    Column(
        modifier = modifier,
    ) {
        Text(
            text = appInfo.name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Column(
            modifier = Modifier.padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Status indicator: Installing / Installed / Not installed
            val statusText = when {
                isDownloading -> "Installing"
                isInstalled -> "Installed"
                else -> "Not installed"
            }
            val statusColor = when {
                isDownloading || isInstalled -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color = statusColor, shape = CircleShape)
                )
                // Status text
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor
                )
                // Download percentage when installing
                if (isDownloading) {
                    Text(
                        text = "${(downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor
                    )
                }
            }

            // Game size on its own line for installed games
            if (isInstalled) {
                Text(
                    text = "$appSizeOnDisk",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Family share indicator on its own line if needed
            if (appInfo.isShared) {
                Text(
                    text = "Family Shared",
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

/***********
 * PREVIEW *
 ***********/

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_AppItem() {
    PrefManager.init(LocalContext.current)
    PluviaTheme {
        Surface {
            LazyColumn(
                modifier = Modifier.padding(16.dp)
            ) {
                items(
                    items = List(5) { idx ->
                        val item = fakeAppInfo(idx)
                        LibraryItem(
                            index = idx,
                            appId = "${GameSource.STEAM.name}_${item.id}",
                            name = item.name,
                            iconHash = item.iconHash,
                            isShared = idx % 2 == 0,
                        )
                    },
                    itemContent = {
                        AppItem(appInfo = it, onClick = {})
                    },
                )
            }
        }
    }
}

@Preview(device = "spec:width=1920px,height=1080px,dpi=440") // Odin2 Mini
@Composable
private fun Preview_AppItemGrid() {
    PrefManager.init(LocalContext.current)
    PluviaTheme {
        Surface {
            Column {
                val appInfoList = List(4) { idx ->
                    val item = fakeAppInfo(idx)
                    LibraryItem(
                        index = idx,
                        appId = "${GameSource.STEAM.name}_${item.id}",
                        name = item.name,
                        iconHash = item.iconHash,
                        isShared = idx % 2 == 0,
                        gameSource = GameSource.STEAM,
                    )
                }

                // Hero
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 72.dp
                    ),
                ) {
                    items(items = appInfoList, key = { it.index }) { item ->
                        AppItem(
                            appInfo = item,
                            onClick = { },
                            paneType = PaneType.GRID_HERO,
                        )
                    }
                }

                // Capsule
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 72.dp
                    ),
                ) {
                    items(items = appInfoList, key = { it.index }) { item ->
                        AppItem(
                            appInfo = item,
                            onClick = { },
                            paneType = PaneType.GRID_CAPSULE,
                        )
                    }
                }
            }
        }
    }
}
