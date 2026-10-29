package com.limelight.ui.gamemenu;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

// EXTENSION DEVELOPMENT [EXT-FULL-KEYBOARD-KEYS] [NEW] BEGIN
/**
 * 虚拟全键盘顶栏「全键盘快捷键」的数据层（与「操作 › 自定义快捷键」彻底分开）。
 *
 * 背景：v5 之前这一栏直接读 QuickKeyStore 的全量数据并过滤默认组，导致
 * 「操作」面板新增的快捷键会同时出现在全键盘快捷键栏上，两处无法各自维护。
 * 现在全键盘快捷键有独立存储（见下方 PREF_* 常量，历史命名仍保留 combo 字样，
 * 是为了兼容已经写进用户设备的旧数据，不要改字符串值）。
 *
 * 升级迁移：首次运行时把 QuickKeyStore 里的“用户自建项”（不含 11 项默认组）
 * 复制一份过来，保证升级后顶栏胶囊内容与升级前完全一致；复制只做一次
 * （meta 标记 migrated_from_quick_keys），此后两处增删互不影响。
 */
public final class FullKeyboardKeyStore {

    // 历史命名：字符串值是已落盘的存储名，改动会导致用户数据丢失，只允许改常量名。
    public static final String PREF_FULL_KEYBOARD_LIST_NAME = "combo_axi_keyAssemble";
    public static final String PREF_FULL_KEYBOARD_LIST_KEY = "combo_assemble_key_";
    public static final String PREF_FULL_KEYBOARD_META_NAME = "combo_axi_keyAssemble_meta";

    // 导出文件里的 type 同样保持旧值，兼容此前导出的文件。
    public static final String EXPORT_TYPE = "axi_combo_keys";
    public static final int EXPORT_VERSION = 1;

    private static final String MIGRATED_KEY = "migrated_from_quick_keys";

    private FullKeyboardKeyStore() {
    }

    /** 首次升级：把「操作 › 自定义快捷键」里的用户项复制一份到全键盘快捷键（不含默认组）。 */
    public static void ensureMigrated(Context context) {
        SharedPreferences meta = context.getSharedPreferences(PREF_FULL_KEYBOARD_META_NAME, Activity.MODE_PRIVATE);
        if (meta.getBoolean(MIGRATED_KEY, false)) {
            return;
        }
        meta.edit().putBoolean(MIGRATED_KEY, true).apply();

        SharedPreferences pref = context.getSharedPreferences(PREF_FULL_KEYBOARD_LIST_NAME, Activity.MODE_PRIVATE);
        if (!pref.getAll().isEmpty()) {
            return; // 已有全键盘快捷键数据（例如刚导入过）：不覆盖
        }

        List<GameMenuQuickBean> source = QuickKeyStore.loadCustom(context);
        SharedPreferences.Editor editor = pref.edit();
        int sort = 1;
        for (GameMenuQuickBean bean : source) {
            if (bean == null || TextUtils.isEmpty(bean.getId()) || QuickKeyStore.isSeedItem(bean)) {
                continue;
            }
            bean.setSort(sort++);
            editor.putString(bean.getId(), new Gson().toJson(bean));
        }
        editor.apply();
    }

    /** 导入后同样视为已完成迁移，避免默认组或旧数据被再次复制进来。 */
    public static void markMigrated(Context context) {
        context.getSharedPreferences(PREF_FULL_KEYBOARD_META_NAME, Activity.MODE_PRIVATE).edit()
                .putBoolean(MIGRATED_KEY, true)
                .apply();
    }

    // ---------------------------------------------------------------- 读写

    public static List<GameMenuQuickBean> load(Context context) {
        ensureMigrated(context);
        List<GameMenuQuickBean> items = new ArrayList<>();
        SharedPreferences pref = context.getSharedPreferences(PREF_FULL_KEYBOARD_LIST_NAME, Activity.MODE_PRIVATE);
        Map<String, String> map = (Map<String, String>) pref.getAll();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            GameMenuQuickBean bean = fromJson(entry.getValue());
            if (bean == null) {
                continue;
            }
            if (TextUtils.isEmpty(bean.getId())) {
                bean.setId(entry.getKey());
            }
            items.add(bean);
        }
        sortItems(items);
        return items;
    }

    public static int nextSort(Context context) {
        int max = 0;
        for (GameMenuQuickBean bean : load(context)) {
            max = Math.max(max, bean.getSort());
        }
        return max + 1;
    }

    public static void save(Context context, GameMenuQuickBean bean) {
        if (bean == null || TextUtils.isEmpty(bean.getId())) {
            return;
        }
        if (bean.getSort() <= 0) {
            bean.setSort(nextSort(context));
        }
        context.getSharedPreferences(PREF_FULL_KEYBOARD_LIST_NAME, Activity.MODE_PRIVATE).edit()
                .putString(bean.getId(), new Gson().toJson(bean))
                .apply();
    }

    public static void remove(Context context, GameMenuQuickBean bean) {
        if (bean == null || TextUtils.isEmpty(bean.getId())) {
            return;
        }
        context.getSharedPreferences(PREF_FULL_KEYBOARD_LIST_NAME, Activity.MODE_PRIVATE).edit()
                .remove(bean.getId())
                .apply();
    }

    /** 导入语义：覆盖全部全键盘快捷键；导入后不再执行升级迁移。 */
    public static void replaceAll(Context context, List<GameMenuQuickBean> beans) {
        SharedPreferences pref = context.getSharedPreferences(PREF_FULL_KEYBOARD_LIST_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.clear();
        if (beans != null) {
            for (GameMenuQuickBean bean : beans) {
                if (bean == null || TextUtils.isEmpty(bean.getId())) {
                    continue;
                }
                editor.putString(bean.getId(), new Gson().toJson(bean));
            }
        }
        editor.apply();
        markMigrated(context);
    }

    public static String exportJson(Context context) {
        try {
            org.json.JSONObject root = new org.json.JSONObject();
            root.put("type", EXPORT_TYPE);
            root.put("version", EXPORT_VERSION);
            root.put("exportedAt", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                    java.util.Locale.US).format(new java.util.Date()));
            org.json.JSONArray data = new org.json.JSONArray();
            for (GameMenuQuickBean bean : load(context)) {
                data.put(QuickKeyStore.itemJson(bean));
            }
            root.put("data", data);
            return root.toString(2);
        } catch (Exception e) {
            return "";
        }
    }

    /** 解析全键盘快捷键导出文件；返回空列表表示没有有效条目。 */
    public static List<GameMenuQuickBean> parseItems(org.json.JSONArray data) {
        return QuickKeyStore.parseItems(data, PREF_FULL_KEYBOARD_LIST_KEY);
    }

    private static GameMenuQuickBean fromJson(String json) {
        try {
            return new Gson().fromJson(json, GameMenuQuickBean.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void sortItems(List<GameMenuQuickBean> items) {
        Collections.sort(items, new Comparator<GameMenuQuickBean>() {
            @Override
            public int compare(GameMenuQuickBean a, GameMenuQuickBean b) {
                if (a.getSort() != b.getSort()) {
                    return Integer.compare(a.getSort(), b.getSort());
                }
                String ida = a.getId() == null ? "" : a.getId();
                String idb = b.getId() == null ? "" : b.getId();
                return ida.compareTo(idb);
            }
        });
    }
}
// EXTENSION DEVELOPMENT [EXT-FULL-KEYBOARD-KEYS] [NEW] END
