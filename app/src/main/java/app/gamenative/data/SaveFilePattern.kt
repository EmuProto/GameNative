package app.gamenative.data

import app.gamenative.enums.PathType
import app.gamenative.utils.SteamUtils
import kotlinx.serialization.Serializable

@Serializable
data class SaveFilePattern(
    val root: PathType,
    val path: String,
    val pattern: String,
) {
    val prefix: String
        get() = "%${root.name}%$path"
            .replace("{64BitSteamID}", SteamUtils.getSteamId64().toString())
            .replace("{Steam3AccountID}", SteamUtils.getSteam3AccountId().toString())

    val substitutedPath: String
        get() = path
            .replace("{64BitSteamID}", SteamUtils.getSteamId64().toString())
            .replace("{Steam3AccountID}", SteamUtils.getSteam3AccountId().toString())
}
