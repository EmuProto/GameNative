package app.gamenative.ui.screen.settings

import android.content.res.Configuration
import android.os.Environment
import android.os.storage.StorageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.gamenative.PrefManager
import app.gamenative.enums.AppTheme
import app.gamenative.ui.component.dialog.SingleChoiceDialog
import app.gamenative.ui.theme.settingsTileColorsAlt
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsSwitch
import com.materialkolor.PaletteStyle
import kotlinx.serialization.json.Json
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import app.gamenative.ui.component.settings.SettingsListDropdown
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.R
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import app.gamenative.utils.IconSwitcher
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSlider
import kotlin.math.roundToInt

@Composable
fun SettingsGroupInterface(
    appTheme: AppTheme,
    paletteStyle: PaletteStyle,
    onAppTheme: (AppTheme) -> Unit,
    onPaletteStyle: (PaletteStyle) -> Unit,
) {
    val context = LocalContext.current

    var openWebLinks by rememberSaveable { mutableStateOf(PrefManager.openWebLinksExternally) }

    var openAppThemeDialog by rememberSaveable { mutableStateOf(false) }
    var openAppPaletteDialog by rememberSaveable { mutableStateOf(false) }

    var openStartScreenDialog by rememberSaveable { mutableStateOf(false) }
    var startScreenOption by rememberSaveable(openStartScreenDialog) { mutableStateOf(PrefManager.startScreen) }

    // Load Steam regions from assets
    val steamRegionsMap: Map<Int, String> = remember {
        val jsonString = context.assets.open("steam_regions.json").bufferedReader().use { it.readText() }
        Json.decodeFromString<Map<String, String>>(jsonString).mapKeys { it.key.toInt() }
    }
    val steamRegionsList = remember {
        // Always put 'Automatic' (id 0) first, then sort the rest alphabetically
        val entries = steamRegionsMap.toList()
        val (autoEntries, otherEntries) = entries.partition { it.first == 0 }
        autoEntries + otherEntries.sortedBy { it.second }
    }
    var openRegionDialog by rememberSaveable { mutableStateOf(false) }
    var selectedRegionIndex by rememberSaveable { mutableStateOf(
        steamRegionsList.indexOfFirst { it.first == PrefManager.cellId }.takeIf { it >= 0 } ?: 0
    ) }

    SettingsGroup(title = { Text(text = "Interface") }) {
        SettingsSwitch(
            colors = settingsTileColorsAlt(),
            title = { Text(text = "Open web links externally") },
            subtitle = { Text(text = "Links open with your main web browser") },
            state = openWebLinks,
            onCheckedChange = {
                openWebLinks = it
                PrefManager.openWebLinksExternally = it
            },
        )

        // Unified visual icon picker (affects app and notification icons)
        var selectedVariant by rememberSaveable { mutableStateOf(if (PrefManager.useAltLauncherIcon || PrefManager.useAltNotificationIcon) 1 else 0) }
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(text = "Icon style")
            Spacer(modifier = Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconVariantCard(
                    label = "Default",
                    launcherIconRes = R.mipmap.ic_launcher,
                    notificationIconRes = R.drawable.ic_notification,
                    selected = selectedVariant == 0,
                    onClick = {
                        selectedVariant = 0
                        PrefManager.useAltLauncherIcon = false
                        PrefManager.useAltNotificationIcon = false
                        IconSwitcher.applyLauncherIcon(context, false)
                    },
                )
                IconVariantCard(
                    label = "Alternate",
                    launcherIconRes = R.mipmap.ic_launcher_alt,
                    notificationIconRes = R.drawable.ic_notification_alt,
                    selected = selectedVariant == 1,
                    onClick = {
                        selectedVariant = 1
                        PrefManager.useAltLauncherIcon = true
                        PrefManager.useAltNotificationIcon = true
                        IconSwitcher.applyLauncherIcon(context, true)
                    },
                )
            }
        }
    }

    // Downloads settings
    SettingsGroup(title = { Text(text = "Downloads") }) {
        var wifiOnlyDownload by rememberSaveable { mutableStateOf(PrefManager.downloadOnWifiOnly) }
        SettingsSwitch(
            colors = settingsTileColorsAlt(),
            title = { Text(text = "Download only over Wi-Fi") },
            subtitle = { Text(text = "Prevent downloads on cellular data") },
            state = wifiOnlyDownload,
            onCheckedChange = {
                wifiOnlyDownload = it
                PrefManager.downloadOnWifiOnly = it
            },
        )
        val ctx = LocalContext.current
        val sm = ctx.getSystemService(StorageManager::class.java)

        // All writable volumes: primary first, then every SD / USB
        val dirs = remember {
            ctx.getExternalFilesDirs(null)
                .filterNotNull()
                .filter { Environment.getExternalStorageState(it) == Environment.MEDIA_MOUNTED }
                .filter { sm.getStorageVolume(it)?.isPrimary != true }
        }

        // Labels the user sees
        val labels = remember(dirs) {
            dirs.map { dir ->
                sm.getStorageVolume(dir)?.getDescription(ctx) ?: dir.name
            }
        }
        var useExternalStorage by rememberSaveable { mutableStateOf(PrefManager.useExternalStorage) }
        SettingsSwitch(
            colors = settingsTileColorsAlt(),
            enabled  = dirs.isNotEmpty(),
            title = { Text(text = "Write to external storage") },
            subtitle = {
                if (dirs.isEmpty())
                    Text("No external storage detected")
                else
                    Text("Save games to external storage")
            },
            state = useExternalStorage,
            onCheckedChange = {
                useExternalStorage = it
                PrefManager.useExternalStorage = it
                if (it && dirs.isNotEmpty()) {
                    PrefManager.externalStoragePath = dirs[0].absolutePath
                }
            },
        )
        if (useExternalStorage) {
            // Currently selected item
            var selectedIndex by rememberSaveable {
                mutableStateOf(
                    dirs.indexOfFirst { it.absolutePath == PrefManager.externalStoragePath }
                        .takeIf { it >= 0 } ?: 0
                )
            }
            SettingsListDropdown(
                title = { Text(text = "Storage volume") },
                items = labels,
                value = selectedIndex,
                onItemSelected = { idx ->
                    selectedIndex = idx
                    PrefManager.externalStoragePath = dirs[idx].absolutePath
                },
                colors = settingsTileColorsAlt()
            )
        }
        // Steam download server selection
        SettingsMenuLink(
            colors = settingsTileColorsAlt(),
            title = { Text(text = "Steam Download Server") },
            subtitle = { Text(text = steamRegionsList.getOrNull(selectedRegionIndex)?.second ?: "Default") },
            onClick = { openRegionDialog = true }
        )
    }

    // Steam Download Server choice dialog
    SingleChoiceDialog(
        openDialog = openRegionDialog,
        icon = Icons.Default.Map,
        iconDescription = "Steam Download Server",
        title = "Steam Download Server",
        items = steamRegionsList.map { it.second },
        currentItem = selectedRegionIndex,
        onSelected = { index ->
            selectedRegionIndex = index
            val selectedId = steamRegionsList[index].first
            PrefManager.cellId = selectedId
            PrefManager.cellIdManuallySet = selectedId != 0
        },
        onDismiss = { openRegionDialog = false }
    )
}


@Composable
private fun IconVariantCard(
    label: String,
    launcherIconRes: Int,
    notificationIconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val border = if (selected) BorderStroke(2.dp, Color(0xFF4F46E5)) else BorderStroke(1.dp, Color(0x33404040))
    Card(
        modifier = Modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        border = border,
        colors = CardDefaults.cardColors(),
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.BottomEnd) {
                AndroidView(
                    modifier = Modifier.matchParentSize(),
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            setImageResource(launcherIconRes)
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                    },
                )
                Image(
                    painter = painterResource(id = notificationIconRes),
                    contentDescription = "$label notification icon",
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = label)
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_SettingsScreen() {
    val context = LocalContext.current
    PrefManager.init(context)
    PluviaTheme {
        SettingsGroupInterface (
            appTheme = AppTheme.DAY,
            paletteStyle = PaletteStyle.TonalSpot,
            onAppTheme = { },
            onPaletteStyle = { },
        )
    }
}
