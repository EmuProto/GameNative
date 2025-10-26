package com.winlator.xenvironment;

import static com.winlator.core.FileUtils.chmod;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import app.gamenative.R;
import app.gamenative.enums.Marker;
import app.gamenative.service.SteamService;
import app.gamenative.utils.ContainerUtils;
import app.gamenative.utils.MarkerUtils;

// import com.winlator.MainActivity;
// import com.winlator.R;
// import com.winlator.SettingsFragment;
import com.winlator.PrefManager;
import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
// import com.winlator.core.DownloadProgressDialog;
import com.winlator.contents.ContentsManager;
import com.winlator.core.Callback;
import com.winlator.core.DefaultVersion;
import com.winlator.core.FileUtils;
// import com.winlator.core.PreloaderDialog;
import com.winlator.core.TarCompressorUtils;
import com.winlator.core.WineInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ImageFsInstaller {
    public static final byte LATEST_VERSION = 24;

    private static void resetContainerImgVersions(Context context) {
        ContainerManager manager = new ContainerManager(context);
        for (Container container : manager.getContainers()) {
            String imgVersion = container.getExtra("imgVersion");
            String wineVersion = container.getWineVersion();
            if (!imgVersion.isEmpty() && WineInfo.isMainWineVersion(wineVersion) && Short.parseShort(imgVersion) <= 5) {
                container.putExtra("wineprefixNeedsUpdate", "t");
            }

            container.putExtra("imgVersion", null);
            container.putExtra("dxwrapper", null);
            container.saveData();
        }
    }

    public static void installWineFromAssets(final Context context, AssetManager assetManager) {
        String[] versions = context.getResources().getStringArray(R.array.bionic_wine_entries);
        File rootDir = ImageFs.find(context).getRootDir();
        for (String version : versions) {
            File outFile = new File(rootDir, "/opt/" + version);
            outFile.mkdirs();
            TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, assetManager, version + ".txz", outFile);
        }
    }


    private static Future<Boolean> installFromAssetsFuture(final Context context, AssetManager assetManager, String containerVariant, Callback<Integer> onProgress) {
        // AppUtils.keepScreenOn(context);
        ImageFs imageFs = ImageFs.find(context);
        final File rootDir = imageFs.getRootDir();

        PrefManager.init(context);
        PrefManager.putString("current_box64_version", "");

        // final DownloadProgressDialog dialog = new DownloadProgressDialog(context);
        // dialog.show(R.string.installing_system_files);
        return Executors.newSingleThreadExecutor().submit(() -> {
            clearRootDir(rootDir);
            final byte compressionRatio = 22;
            String imagefsFile = containerVariant.equals(Container.GLIBC) ? "imagefs_gamenative.txz" : "imagefs_bionic.txz";
            File downloaded = new File(imageFs.getFilesDir(), imagefsFile);
            final long contentLength = (long)(FileUtils.getSize(downloaded) * (100.0f / compressionRatio));
            AtomicLong totalSizeRef = new AtomicLong();
            Log.d("Extraction", "extracting " + imagefsFile);

            boolean success = TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, downloaded, rootDir, (file, size) -> {
                if (size > 0) {
                    long totalSize = totalSizeRef.addAndGet(size);
                    if (onProgress != null) {
                        final int progress = (int)(((float)totalSize / contentLength) * 100);
                        onProgress.call(progress);
                    }
                }
                return file;
            });

            if (success) {
                Log.d("ImageFsInstaller", "Successfully installed system files");
                ContainerManager containerManager = new ContainerManager(context);

                installWineFromAssets(context, assetManager);
                installGuestLibs(context);
                imageFs.createImgVersionFile(LATEST_VERSION);
                resetContainerImgVersions(context);

                // Clear Steam DLL markers for all games
                clearSteamDllMarkers(context, containerManager);
            }
            else {
                Log.e("ImageFsInstaller", "Failed to install system files");
                // AppUtils.showToast(context, R.string.unable_to_install_system_files);
            }
            return success;
            // dialog.closeOnUiThread();
        });
    }

    private static void installGuestLibs(Context ctx) {
        final String ASSET_TAR = "redirect.tzst";          // ➊  add this to assets/
        File imagefs = new File(ctx.getFilesDir(), "imagefs");
        // ➋  Unpack straight into imagefs, preserving relative paths.
        try (InputStream in  = ctx.getAssets().open(ASSET_TAR)) {
            TarCompressorUtils.extract(
                    TarCompressorUtils.Type.ZSTD,      // you said .tzst
                    in, imagefs);                      // helper already exists in the project
        } catch (IOException e) {
            Log.e("ImageFsInstaller", "redirect deploy failed", e);
            return;
        }

        // ➌  Make sure the new libs are world-readable / executable
        chmod(new File(imagefs, "usr/lib/libredirect.so"));
        chmod(new File(imagefs, "usr/lib/libredirect-bionic.so"));

        final String EXTRAS_TAR = "extras.tzst";          // ➊  add this to assets/
        // ➋  Unpack straight into imagefs, preserving relative paths.
        try (InputStream in  = ctx.getAssets().open(EXTRAS_TAR)) {
            TarCompressorUtils.extract(
                    TarCompressorUtils.Type.ZSTD,      // you said .tzst
                    in, imagefs);                      // helper already exists in the project
        } catch (IOException e) {
            Log.e("ImageFsInstaller", "extras deploy failed", e);
            return;
        }

        // ➌  Make sure the new libs are world-readable / executable
        chmod(new File(imagefs, "generate_interfaces_file.exe"));
        chmod(new File(imagefs, "Steamless/Steamless.CLI.exe"));
        chmod(new File(imagefs, "opt/mono-gecko-offline/wine-mono-9.0.0-x86.msi"));
    }

    private static void chmod(File f) { if (f.exists()) FileUtils.chmod(f, 0755);}

    public static Future<Boolean> installIfNeededFuture(final Context context, AssetManager assetManager) {
        return installIfNeededFuture(context, assetManager, null, null);
    }
    public static Future<Boolean> installIfNeededFuture(final Context context, AssetManager assetManager, Container container, Callback<Integer> onProgress) {
        ImageFs imageFs = ImageFs.find(context);
        if (!imageFs.isValid() || imageFs.getVersion() < LATEST_VERSION || !imageFs.getVariant().equals(container.getContainerVariant())) {
            Log.d("ImageFsInstaller", "Installing image from assets");
            return installFromAssetsFuture(context, assetManager, container.getContainerVariant(), onProgress);
        } else {
            Log.d("ImageFsInstaller", "Image FS already valid and at latest version");
            return Executors.newSingleThreadExecutor().submit(() -> true);
        }
    }

    private static void clearOptDir(File optDir) {
        File[] files = optDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().equals("installed-wine")) continue;
                FileUtils.delete(file);
            }
        }
    }

    private static void clearRootDir(File rootDir) {
        if (rootDir.isDirectory()) {
            File[] files = rootDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        String name = file.getName();
                        if (name.equals("home")) {
                            continue;
                        }
                    }
                    FileUtils.delete(file);
                }
            }
        }
        else rootDir.mkdirs();
    }

    public static void generateCompactContainerPattern(final Context context, AssetManager assetManager) {
        // AppUtils.keepScreenOn(context);
        // PreloaderDialog preloaderDialog = new PreloaderDialog(context);
        // preloaderDialog.show(R.string.loading);
        Executors.newSingleThreadExecutor().execute(() -> {
            File[] srcFiles, dstFiles;
            File rootDir = ImageFs.find(context).getRootDir();
            File wineSystem32Dir = new File(rootDir, "/opt/wine/lib/wine/x86_64-windows");
            File wineSysWoW64Dir = new File(rootDir, "/opt/wine/lib/wine/i386-windows");

            File containerPatternDir = new File(context.getCacheDir(), "container_pattern_gamenative");
            FileUtils.delete(containerPatternDir);
            TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, assetManager, "container_pattern_gamenative.tzst", containerPatternDir);

            File containerSystem32Dir = new File(containerPatternDir, ".wine/drive_c/windows/system32");
            File containerSysWoW64Dir = new File(containerPatternDir, ".wine/drive_c/windows/syswow64");

            dstFiles = containerSystem32Dir.listFiles();
            srcFiles = wineSystem32Dir.listFiles();

            ArrayList<String> system32Files = new ArrayList<>();
            ArrayList<String> syswow64Files = new ArrayList<>();

            for (File dstFile : dstFiles) {
                for (File srcFile : srcFiles) {
                    if (dstFile.getName().equals(srcFile.getName())) {
                        if (FileUtils.contentEquals(srcFile, dstFile)) system32Files.add(srcFile.getName());
                        break;
                    }
                }
            }

            dstFiles = containerSysWoW64Dir.listFiles();
            srcFiles = wineSysWoW64Dir.listFiles();

            for (File dstFile : dstFiles) {
                for (File srcFile : srcFiles) {
                    if (dstFile.getName().equals(srcFile.getName())) {
                        if (FileUtils.contentEquals(srcFile, dstFile)) syswow64Files.add(srcFile.getName());
                        break;
                    }
                }
            }

            try {
                JSONObject data = new JSONObject();

                JSONArray system32JSONArray = new JSONArray();
                for (String name : system32Files) {
                    FileUtils.delete(new File(containerSystem32Dir, name));
                    system32JSONArray.put(name);
                }
                data.put("system32", system32JSONArray);

                JSONArray syswow64JSONArray = new JSONArray();
                for (String name : syswow64Files) {
                    FileUtils.delete(new File(containerSysWoW64Dir, name));
                    syswow64JSONArray.put(name);
                }
                data.put("syswow64", syswow64JSONArray);

                FileUtils.writeString(new File(context.getCacheDir(), "common_dlls.json"), data.toString());

                File outputFile = new File(context.getCacheDir(), "container_pattern_gamenative.tzst");
                FileUtils.delete(outputFile);
                TarCompressorUtils.compress(TarCompressorUtils.Type.ZSTD, new File(containerPatternDir, ".wine"), outputFile, 22);

                FileUtils.delete(containerPatternDir);
                // preloaderDialog.closeOnUiThread();
            }
            catch (JSONException e) {
                Log.e("ImageFsInstaller", "Failed to read JSON data: " + e);
            }
        });
    }

    /**
     * Clears Steam DLL markers for all containers by scanning each mapped drive path.
     * Relies only on container drive mappings; does not call into SteamService.
     */
    private static void clearSteamDllMarkers(Context context, ContainerManager containerManager) {
        try {
            for (Container container : containerManager.getContainers()) {
                try {
                    int gameId = ContainerUtils.INSTANCE.extractGameIdFromContainerId(container.id);
                    String mappedPath = SteamService.Companion.getAppDirPath(gameId);
                    MarkerUtils.INSTANCE.removeMarker(mappedPath, Marker.STEAM_DLL_REPLACED);
                    MarkerUtils.INSTANCE.removeMarker(mappedPath, Marker.STEAM_DLL_RESTORED);
                    Log.i("ImageFsInstaller", "Cleared markers for container: " + container.getName() + " (ID: " + container.id + ")");
                } catch (Exception e) {
                    Log.w("ImageFsInstaller", "Failed to clear markers for container ID " + container.id + ": " + e.getMessage());
                }
            }
            Log.i("ImageFsInstaller", "Finished clearing Steam DLL markers for all containers");
        } catch (Exception e) {
            Log.e("ImageFsInstaller", "Error clearing Steam DLL markers: " + e.getMessage());
        }
    }
}
