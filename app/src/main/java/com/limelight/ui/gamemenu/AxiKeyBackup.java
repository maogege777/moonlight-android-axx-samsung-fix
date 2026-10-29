package com.limelight.ui.gamemenu;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.limelight.R;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// EXTENSION DEVELOPMENT [EXT-KEY-BACKUP] [NEW] BEGIN
/**
 * 三处按键数据的统一备份层：
 *  - 自定义快捷键（QuickKeyStore，含默认组）
 *  - 组合键条（ComboKeyStore，独立存储）
 *  - 虚拟按键 5 个布局方案（files/axi_<方案值>.txt）
 *
 * 导出为单一文件，导入时可勾选要恢复的部分（见 {@link #SECTION_*}）。
 * 同时兼容两种旧文件：axi_quick_keys（旧快捷键导出）与裸 JSON 数组（旧“虚拟按键配置导出”，
 * 只会覆盖当前选中的那个方案）。
 *
 * 方案复制（虚拟按键 / 虚拟手柄共用）：把某个方案档位的配置文件覆盖到另一个档位，
 * 用于“在方案 1 调好布局，复制到方案 2/3”。文件名规则两处一致：axi_<方案值>.txt。
 */
public final class AxiKeyBackup {

    public static final String TYPE = "axi_key_backup";
    public static final int VERSION = 1;

    public static final int SECTION_QUICK_KEYS = 1;
    public static final int SECTION_COMBO_KEYS = 2;
    public static final int SECTION_LAYOUTS = 4;
    public static final int SECTION_GAMEPAD_LAYOUTS = 8;
    public static final int SECTION_ALL = SECTION_QUICK_KEYS | SECTION_COMBO_KEYS
            | SECTION_LAYOUTS | SECTION_GAMEPAD_LAYOUTS;

    private AxiKeyBackup() {
    }

    // ---------------------------------------------------------------- 导出

    public static String exportAll(Context context) {
        try {
            JSONObject root = new JSONObject();
            root.put("type", TYPE);
            root.put("version", VERSION);
            root.put("exportedAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    .format(new Date()));

            String quickJson = QuickKeyStore.exportJson(context);
            root.put("quickKeys", new JSONObject(quickJson));

            String comboJson = ComboKeyStore.exportJson(context);
            root.put("comboKeys", new JSONObject(comboJson));

            JSONObject layouts = new JSONObject();
            for (String value : schemeValues(context, false)) {
                String raw = readLayout(context, value);
                if (TextUtils.isEmpty(raw)) {
                    continue; // 该方案还没保存过：保持默认，不写入备份
                }
                try {
                    layouts.put(value, new JSONArray(raw));
                } catch (Exception ignored) {
                    // 文件损坏就跳过，避免整份备份失败
                }
            }
            root.put("keyboardLayouts", layouts);
            root.put("keyboardScheme", currentScheme(context, false));

            JSONObject gamepadLayouts = new JSONObject();
            for (String value : schemeValues(context, true)) {
                String raw = readLayout(context, value);
                if (TextUtils.isEmpty(raw)) {
                    continue;
                }
                try {
                    gamepadLayouts.put(value, new JSONArray(raw));
                } catch (Exception ignored) {
                }
            }
            root.put("gamepadLayouts", gamepadLayouts);
            root.put("gamepadScheme", currentScheme(context, true));

            return root.toString(2);
        } catch (Exception e) {
            return "";
        }
    }

    // ---------------------------------------------------------------- 解析

    public static final class BackupData {
        public int sections;
        public List<GameMenuQuickBean> quickKeys = new ArrayList<>();
        public List<GameMenuQuickBean> comboKeys = new ArrayList<>();
        /** 方案值 → 布局 JSON（原样保存，还原时直接落盘）。 */
        public final Map<String, String> layouts = new LinkedHashMap<>();
        public final Map<String, String> gamepadLayouts = new LinkedHashMap<>();
        public String keyboardScheme;
        public String gamepadScheme;
    }

    /**
     * 解析备份文件；返回 null 表示无法识别。
     * 能识别：本应用新的合并备份、旧的快捷键导出、旧的裸布局数组。
     */
    public static BackupData parse(Context context, String json) {
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        String trimmed = json.trim();
        BackupData data = new BackupData();

        // 旧版「虚拟按键配置导出」：文件本身就是布局数组，只覆盖当前方案
        if (trimmed.startsWith("[")) {
            data.layouts.put(currentScheme(context, false), trimmed);
            data.sections = SECTION_LAYOUTS;
            return data;
        }

        try {
            JSONObject root = new JSONObject(trimmed);
            String type = root.optString("type", "");

            if (TYPE.equals(type)) {
                JSONObject quick = root.optJSONObject("quickKeys");
                if (quick != null) {
                    QuickKeyStore.ImportResult parsed = QuickKeyStore.parseImport(quick.toString());
                    if (parsed != null) {
                        data.quickKeys = parsed.custom;
                        data.sections |= SECTION_QUICK_KEYS;
                    }
                }
                JSONObject combo = root.optJSONObject("comboKeys");
                if (combo != null) {
                    data.comboKeys = QuickKeyStore.parseItems(combo.optJSONArray("data"),
                            ComboKeyStore.PREF_COMBO_LIST_KEY);
                    data.sections |= SECTION_COMBO_KEYS;
                }
                JSONObject layouts = root.optJSONObject("keyboardLayouts");
                if (layouts != null) {
                    Iterator<String> keys = layouts.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        JSONArray array = layouts.optJSONArray(key);
                        if (array != null) {
                            data.layouts.put(key, array.toString());
                        }
                    }
                    if (!data.layouts.isEmpty()) {
                        data.sections |= SECTION_LAYOUTS;
                    }
                }
                data.keyboardScheme = root.optString("keyboardScheme", null);

                JSONObject gamepadLayouts = root.optJSONObject("gamepadLayouts");
                if (gamepadLayouts != null) {
                    Iterator<String> keys = gamepadLayouts.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        JSONArray array = gamepadLayouts.optJSONArray(key);
                        if (array != null) {
                            data.gamepadLayouts.put(key, array.toString());
                        }
                    }
                    if (!data.gamepadLayouts.isEmpty()) {
                        data.sections |= SECTION_GAMEPAD_LAYOUTS;
                    }
                }
                data.gamepadScheme = root.optString("gamepadScheme", null);
                return data.sections == 0 ? null : data;
            }

            if (QuickKeyStore.EXPORT_TYPE.equals(type)) {
                QuickKeyStore.ImportResult parsed = QuickKeyStore.parseImport(trimmed);
                if (parsed == null) {
                    return null;
                }
                data.quickKeys = parsed.custom;
                data.sections = SECTION_QUICK_KEYS;
                return data;
            }

            if (ComboKeyStore.EXPORT_TYPE.equals(type)) {
                data.comboKeys = QuickKeyStore.parseItems(root.optJSONArray("data"),
                        ComboKeyStore.PREF_COMBO_LIST_KEY);
                data.sections = SECTION_COMBO_KEYS;
                return data;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- 还原

    /** 按位掩码还原所选部分；导入语义为整体覆盖。 */
    public static void restore(Context context, BackupData data, int sections) {
        if (data == null || sections == 0) {
            return;
        }
        if ((sections & SECTION_QUICK_KEYS) != 0) {
            QuickKeyStore.replaceAllCustom(context, data.quickKeys);
        }
        if ((sections & SECTION_COMBO_KEYS) != 0) {
            ComboKeyStore.replaceAll(context, data.comboKeys);
        }
        if ((sections & SECTION_LAYOUTS) != 0) {
            restoreLayouts(context, data.layouts, false, data.keyboardScheme);
        }
        if ((sections & SECTION_GAMEPAD_LAYOUTS) != 0) {
            restoreLayouts(context, data.gamepadLayouts, true, data.gamepadScheme);
        }
    }

    private static void restoreLayouts(Context context, Map<String, String> layouts,
                                       boolean gamePad, String scheme) {
        List<String> known = java.util.Arrays.asList(schemeValues(context, gamePad));
        for (Map.Entry<String, String> entry : layouts.entrySet()) {
            if (!known.contains(entry.getKey())) {
                continue; // 未知方案名（来自其它版本）不落盘
            }
            writeLayout(context, entry.getKey(), entry.getValue());
        }
        if (!TextUtils.isEmpty(scheme) && known.contains(scheme)) {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putString(gamePad ? KEY_GAMEPAD_SCHEME : KEY_KEYBOARD_SCHEME, scheme)
                    .apply();
        }
    }

    // ---------------------------------------------------------------- 方案复制

    public static final String KEY_KEYBOARD_SCHEME = "keyboard_axi_list";
    public static final String KEY_GAMEPAD_SCHEME = "gamepad_axi_list";
    public static final String DEFAULT_KEYBOARD_SCHEME = "OSC_Keyboard";
    public static final String DEFAULT_GAMEPAD_SCHEME = "gamePad";

    public static String[] schemeNames(Context context, boolean gamePad) {
        return context.getResources().getStringArray(
                gamePad ? R.array.gamepad_axi_names : R.array.keyboard_axi_names);
    }

    public static String[] schemeValues(Context context, boolean gamePad) {
        return context.getResources().getStringArray(
                gamePad ? R.array.gamepad_axi_values : R.array.keyboard_axi_values);
    }

    public static String currentScheme(Context context, boolean gamePad) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
                gamePad ? KEY_GAMEPAD_SCHEME : KEY_KEYBOARD_SCHEME,
                gamePad ? DEFAULT_GAMEPAD_SCHEME : DEFAULT_KEYBOARD_SCHEME);
    }

    public static String schemeFile(String value) {
        return "axi_" + value + ".txt";
    }

    public static boolean hasLayout(Context context, String value) {
        return layoutFile(context, value).exists();
    }

    /**
     * 把源方案覆盖到目标方案。
     * 源方案没有保存过文件时（即当前是默认布局），目标方案的文件会被删除，也就是一起回到默认布局。
     *
     * @return 是否写入成功
     */
    public static boolean copyLayout(Context context, String sourceValue, String targetValue) {
        if (TextUtils.isEmpty(sourceValue) || TextUtils.isEmpty(targetValue)
                || sourceValue.equals(targetValue)) {
            return false;
        }
        String raw = readLayout(context, sourceValue);
        if (TextUtils.isEmpty(raw)) {
            return deleteLayout(context, targetValue);
        }
        return writeLayout(context, targetValue, raw);
    }

    // ---------------------------------------------------------------- 文件

    private static File layoutFile(Context context, String value) {
        return new File(context.getFilesDir(), schemeFile(value));
    }

    private static String readLayout(Context context, String value) {
        File file = layoutFile(context, value);
        if (!file.exists()) {
            return null;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static boolean writeLayout(Context context, String value, String json) {
        if (TextUtils.isEmpty(value) || TextUtils.isEmpty(json)) {
            return false;
        }
        OutputStreamWriter writer = null;
        try {
            File file = layoutFile(context, value);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
            writer.write(json);
            writer.flush();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static boolean deleteLayout(Context context, String value) {
        File file = layoutFile(context, value);
        if (!file.exists()) {
            return true;
        }
        return file.delete();
    }
}
// EXTENSION DEVELOPMENT [EXT-KEY-BACKUP] [NEW] END
