package com.winlator.fexcore;

import app.gamenative.R;

import android.content.Context;
import com.winlator.container.ContainerManager;
import com.winlator.container.Shortcut;

import android.media.Image;
import android.provider.ContactsContract;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.winlator.container.Container;
import com.winlator.contents.ContentProfile;
import com.winlator.contents.ContentsManager;
import com.winlator.core.AppUtils;
import com.winlator.core.DefaultVersion;
import com.winlator.core.FileUtils;
import com.winlator.xenvironment.ImageFs;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public final class FEXCoreManager {

    private static File configFile;
    private static List<String> tsoPresets;
    private static List<String> x87modePresets;
    private static List<String> multiblockValues;
    private static ImageFs imageFS;


    FEXCoreManager() {
    }

    private static String presetFromTSOValues(String tsoEnabled, String vectorTSOEnabled, String memcpySetTSOEnabled, String halfbarrierTSOEnabled) {
        String ret;

        if (tsoEnabled.equals("1")) {
            if (halfbarrierTSOEnabled.equals("1"))
                ret = "Slow";
            else if(vectorTSOEnabled.equals("1") && memcpySetTSOEnabled.equals("1"))
                ret = "Slowest";
            else
                ret = "Fast";
        }
        else
            ret = "Fastest";

        return ret;
    }

    public static void writeToConfigFile(Context context, String containerId, String tsoPreset, String mblockValue, String x87ModePreset) {
        String tsoEnabled = "";
        String X87ReducedPrecisionValue = "" ;
        String vectorTSOEnabled = "";
        String multiblockValue = "";
        String memcpysetTSOEnabled = "";
        String halfbarrierTSOEnabled = "";

        switch (tsoPreset) {
            case "Fastest":
                tsoEnabled = "0";
                vectorTSOEnabled = "0";
                memcpysetTSOEnabled = "0";
                halfbarrierTSOEnabled = "0";
                break;
            case "Fast":
                tsoEnabled = "1";
                vectorTSOEnabled = "0";
                memcpysetTSOEnabled = "0";
                halfbarrierTSOEnabled = "0";
                break;
            case "Slow":
                tsoEnabled = "1";
                vectorTSOEnabled = "0";
                memcpysetTSOEnabled = "0";
                halfbarrierTSOEnabled = "1";
                break;
            case "Slowest":
                tsoEnabled = "1";
                vectorTSOEnabled = "1";
                memcpysetTSOEnabled = "1";
                halfbarrierTSOEnabled = "0";
                break;
            default:
                break;
        }

        switch(x87ModePreset) {
            case "Fast":
                X87ReducedPrecisionValue = "1";
                break;
            case "Slow":
                X87ReducedPrecisionValue = "0";
                break;
        }

        switch (mblockValue) {
            case "Enabled":
                multiblockValue = "1";
                break;
            case "Disabled":
                multiblockValue = "0";
                break;
        }

        try {
            JSONObject config = new JSONObject();
            JSONObject opts = new JSONObject()
                    .put("Multiblock", multiblockValue)
                    .put("TSOEnabled", tsoEnabled)
                    .put("VectorTSOEnabled", vectorTSOEnabled)
                    .put("MemcpySetTSOEnabled", memcpysetTSOEnabled)
                    .put("HalfBarrierTSOEnabled", halfbarrierTSOEnabled)
                    .put("X87ReducedPrecision", X87ReducedPrecisionValue);
            config.put("Config", opts);
            String json = config.toString();
            ImageFs imageFs = ImageFs.find(context);
            configFile = new File(imageFs.home_path + "-" + containerId + "/.fex-emu/Config.json");
            FileUtils.writeString(configFile, json);
        }
        catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeToConfigFile(File configFile, String tsoPreset, String mblockValue, String x87ModePreset) {
        String tsoEnabled = "";
        String X87ReducedPrecisionValue = "" ;
        String vectorTSOEnabled = "";
        String multiblockValue = "";
        String memcpysetTSOEnabled = "";
        String halfbarrierTSOEnabled = "";

        switch (tsoPreset) {
            case "Fastest":
                tsoEnabled = "0";
                vectorTSOEnabled = "0";
                memcpysetTSOEnabled = "0";
                halfbarrierTSOEnabled = "0";
                break;
            case "Fast":
                tsoEnabled = "1";
                vectorTSOEnabled = "0";
                memcpysetTSOEnabled = "0";
                halfbarrierTSOEnabled = "0";
                break;
            case "Slow":
                tsoEnabled = "1";
                vectorTSOEnabled = "0";
                memcpysetTSOEnabled = "0";
                halfbarrierTSOEnabled = "1";
                break;
            case "Slowest":
                tsoEnabled = "1";
                vectorTSOEnabled = "1";
                memcpysetTSOEnabled = "1";
                halfbarrierTSOEnabled = "0";
                break;
            default:
                break;
        }

        switch(x87ModePreset) {
            case "Fast":
                X87ReducedPrecisionValue = "1";
                break;
            case "Slow":
                X87ReducedPrecisionValue = "0";
                break;
        }

        switch (mblockValue) {
            case "Enabled":
                multiblockValue = "1";
                break;
            case "Disabled":
                multiblockValue = "0";
                break;
        }

        try {
            JSONObject config = new JSONObject();
            JSONObject opts = new JSONObject()
                    .put("Multiblock", multiblockValue)
                    .put("TSOEnabled", tsoEnabled)
                    .put("VectorTSOEnabled", vectorTSOEnabled)
                    .put("MemcpySetTSOEnabled", memcpysetTSOEnabled)
                    .put("HalfBarrierTSOEnabled", halfbarrierTSOEnabled)
                    .put("X87ReducedPrecision", X87ReducedPrecisionValue);
            config.put("Config", opts);
            String json = config.toString();
            FileUtils.writeString(configFile, json);
        }
        catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setFromConfigFile(Spinner tsoModeSpinner, Spinner x87modeSpinner, Spinner multiBlockSpinner) {
        try {
            JSONObject jobj = new JSONObject(FileUtils.readString(configFile));
            JSONObject config = jobj.getJSONObject("Config");
            String tsoPreset = presetFromTSOValues(config.getString("TSOEnabled"), config.getString("VectorTSOEnabled"), config.getString("MemcpySetTSOEnabled"), config.getString("HalfBarrierTSOEnabled"));
            selectSpinnerItemByValue(tsoModeSpinner, tsoPresets, tsoPreset);
            String x87mode = (config.getString("X87ReducedPrecision").equals("1")) ? "Fast" : "Slow";
            selectSpinnerItemByValue(x87modeSpinner, x87modePresets, x87mode);
            String multiBlockValue = (config.getString("Multiblock").equals("1")) ? "Enabled" : "Disabled";
            selectSpinnerItemByValue(multiBlockSpinner, multiblockValues, multiBlockValue);
        }
        catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setFromDefaults(Spinner tsoModeSpinner, Spinner x87modeSpinner, Spinner multiBlockSpinner) {
        selectSpinnerItemByValue(tsoModeSpinner, tsoPresets, "Fast");
        selectSpinnerItemByValue(x87modeSpinner, x87modePresets, "Fast");
        selectSpinnerItemByValue(multiBlockSpinner, multiblockValues, "Disabled");
    }

    private static void selectSpinnerItemByValue(Spinner spnr, List<String> values, String value) {
        int position = values.indexOf(value);
        spnr.setSelection(position);
    }

    public static void loadFEXCoreSettings(Context ctx, Container container, Spinner fexcoreTSOSpinner, Spinner fexcoreMultiblockSpinner, Spinner fexcoreX87ModeSpinner) {
        File imageFsRoot = new File(ctx.getFilesDir(), "imagefs");
        imageFS = ImageFs.find(imageFsRoot);
        ContainerManager containerManager = new ContainerManager(ctx);

        tsoPresets = new ArrayList<>(Arrays.asList(ctx.getResources().getStringArray(R.array.fexcore_preset_entries)));
        x87modePresets = new ArrayList<>(Arrays.asList(ctx.getResources().getStringArray(R.array.x87mode_preset_entries)));
        multiblockValues = new ArrayList<>(Arrays.asList(ctx.getResources().getStringArray(R.array.multiblock_values)));

        fexcoreTSOSpinner.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, tsoPresets));
        fexcoreMultiblockSpinner.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, multiblockValues));
        fexcoreX87ModeSpinner.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, x87modePresets));

        if (container != null)
            configFile = new File(imageFS.home_path + "-" + container.id + "/.fex-emu/Config.json");

        if (configFile != null && configFile.exists())
            setFromConfigFile(fexcoreTSOSpinner, fexcoreX87ModeSpinner, fexcoreMultiblockSpinner);
        else
            setFromDefaults(fexcoreTSOSpinner, fexcoreX87ModeSpinner, fexcoreMultiblockSpinner);
    }

    public static void saveFEXCoreSpinners(Container container, Spinner fexcoreTSOSpinner, Spinner fexcoreMultiblockSpinner, Spinner fexcoreX87ModeSpinner) {
        String preset = (String)fexcoreTSOSpinner.getSelectedItem();
        String multiBlockValue = (String)fexcoreMultiblockSpinner.getSelectedItem();
        String x87ReducedPrecisionValue = (String)fexcoreX87ModeSpinner.getSelectedItem();
        if (!configFile.exists())
            configFile.getParentFile().mkdirs();
        writeToConfigFile(null, container.id, preset, multiBlockValue, x87ReducedPrecisionValue);
    }

    public static void loadFEXCoreVersion(Context context, ContentsManager contentsManager, Spinner spinner, Container container) {
        String[] originalItems = context.getResources().getStringArray(R.array.fexcore_version_entries);
        List<String> itemList = new ArrayList<>(Arrays.asList(originalItems));
        for (ContentProfile profile : contentsManager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_FEXCORE)) {
            String entryName = ContentsManager.getEntryName(profile);
            int firstDashIndex = entryName.indexOf('-');
            itemList.add(entryName.substring(firstDashIndex + 1));
        }
        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, itemList));
        if (container != null)
            AppUtils.setSpinnerSelectionFromValue(spinner, container.getFEXCoreVersion());
        else
            AppUtils.setSpinnerSelectionFromValue(spinner, DefaultVersion.FEXCORE);
    }

    public static void loadFEXCoreVersion(Context context, ContentsManager contentsManager, Spinner spinner, Shortcut shortcut) {
        String[] originalItems = context.getResources().getStringArray(R.array.fexcore_version_entries);
        List<String> itemList = new ArrayList<>(Arrays.asList(originalItems));
        for (ContentProfile profile : contentsManager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_FEXCORE)) {
            String entryName = ContentsManager.getEntryName(profile);
            int firstDashIndex = entryName.indexOf('-');
            itemList.add(entryName.substring(firstDashIndex + 1));
        }
        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, itemList));
        AppUtils.setSpinnerSelectionFromValue(spinner, shortcut.getExtra("fexcoreVersion", shortcut.container.getFEXCoreVersion()));
    }

    public static void createAppConfigFiles(Context ctx) {
        String[] programsName = {"winhandler.exe"};
        for (String programName : programsName) {
            File configFile = new File(ctx.getFilesDir(), "imagefs/home/xuser/.fex-emu/AppConfig/" + programName + ".json");
            if (!configFile.exists()) {
                switch (programName) {
                    case "winhandler.exe":
                        writeToConfigFile(configFile, "Fastest", "Disabled", "Fast");
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private static String parseConfigFile() {
        try {
            JSONObject jobj = new JSONObject(FileUtils.readString(configFile));
            JSONObject config = jobj.getJSONObject("Config");
            String tsoPreset = presetFromTSOValues(config.getString("TSOEnabled"), config.getString("VectorTSOEnabled"), config.getString("MemcpySetTSOEnabled"), config.getString("HalfBarrierTSOEnabled"));
            String x87mode = (config.getString("X87ReducedPrecision").equals("1")) ? "Fast" : "Slow";
            String multiBlockValue = (config.getString("Multiblock").equals("1")) ? "Enabled" : "Disabled";
            return String.format("TSOMode %s, x87Mode %s, MultiBlock %s", tsoPreset, x87mode, multiBlockValue);
        } catch (JSONException e) {
            // Return a default or error string if parsing fails
            return "TSOMode Fast, x87Mode Fast, MultiBlock Disabled";
        }
    }

    public static String printFEXCoreSettings(Context ctx, Container container) {
        File imageFsRoot = new File(ctx.getFilesDir(), "imagefs");
        imageFS = ImageFs.find(imageFsRoot);
        configFile = new File(imageFS.home_path + "-" + container.id + "/.fex-emu/Config.json");
        return parseConfigFile();
    }

    /**
     * Read FEXCore settings for the given container from .fex-emu/Config.json.
     * Returns an array of 3 strings: [TSO preset, x87 mode, multiblock value].
     * Defaults to ["Fast", "Fast", "Disabled"] if file missing or parse error.
     */
    public static String[] readFEXCoreSettings(Context ctx, Container container) {
        try {
            File imageFsRoot = new File(ctx.getFilesDir(), "imagefs");
            imageFS = ImageFs.find(imageFsRoot);
            File cfg = new File(imageFS.home_path + "-" + container.id + "/.fex-emu/Config.json");
            if (!cfg.exists()) {
                return new String[]{"Fast", "Fast", "Disabled"};
            }
            JSONObject jobj = new JSONObject(FileUtils.readString(cfg));
            JSONObject config = jobj.getJSONObject("Config");
            String tsoPreset = presetFromTSOValues(
                    config.getString("TSOEnabled"),
                    config.getString("VectorTSOEnabled"),
                    config.getString("MemcpySetTSOEnabled"),
                    config.getString("HalfBarrierTSOEnabled")
            );
            String x87mode = (config.getString("X87ReducedPrecision").equals("1")) ? "Fast" : "Slow";
            String multiBlockValue = (config.getString("Multiblock").equals("1")) ? "Enabled" : "Disabled";
            return new String[]{tsoPreset, x87mode, multiBlockValue};
        } catch (Throwable t) {
            return new String[]{"Fast", "Fast", "Disabled"};
        }
    }
}
