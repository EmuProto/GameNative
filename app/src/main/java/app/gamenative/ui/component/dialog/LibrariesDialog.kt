package app.gamenative.ui.component.dialog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable

@Composable
fun LibrariesDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
) {
    MessageDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        onConfirmClick = onDismissRequest,
        confirmBtnText = "Close",
        icon = Icons.Default.Info,
        title = "Libraries Used",
        message = """
                Pluvia - github.com/oxters168/Pluvia
                JavaSteam - github.com/Longi94/JavaSteam
                Winlator & Vortek - github.com/brunodev85/winlator
                Winlator Cmod - github.com/coffincolors/winlator
                Wrapper - https://github.com/leegao/bionic-vulkan-wrapper & https://github.com/pipetto-crypto/
                Ubuntu RootFs - releases.ubuntu.com/focal
                Wine - winehq.org
                Box86/Box64 - box86.org
                Mesa (Turnip/Zink/VirGL) - mesa3d.org
                DXVK - github.com/doitsujin/dxvk
                VKD3D - gitlab.winehq.org/wine/vkd3d
                D8VK - github.com/AlpyneDreams/d8vk
                CNC DDraw - github.com/FunkyFr3sh/cnc-ddraw
                Goldberg - gitlab.com/Mr_Goldberg/goldberg_emulator
        """.trimIndent(),
    )
}
