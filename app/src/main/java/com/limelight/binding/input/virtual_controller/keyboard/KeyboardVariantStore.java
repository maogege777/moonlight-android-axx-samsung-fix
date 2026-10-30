package com.limelight.binding.input.virtual_controller.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * EXTENSION DEVELOPMENT [EXT-KB-VARIANTS]
 *
 * <p>全键盘布局（电脑全键盘 / 简化键盘 / 简化v2 / 数字键盘）的启用列表。</p>
 *
 * <p>存储的是一个有序字符串（如 {@code simple,digitpad}）：顺序即「点亮顺序」，
 * 也就是顶栏「布局 ▾」按钮每次点击的切换顺序；列表里没有的布局不参与循环。</p>
 *
 * <p>初始化规则：全新安装只启用 简化 + 数字；覆盖安装不写任何东西，读取时回落到
 * 旧版写死的切换顺序（简化 → 全键盘 → 数字），老用户行为不变。</p>
 */
public final class KeyboardVariantStore {

    public static final String PREF_KEY = "keyboard_axi_variants";

    public static final String VARIANT_FULL = "full";
    public static final String VARIANT_SIMPLE = "simple";
    /** 简化v2：以简化键盘为基础左右分离，鼠标/滚轮/触摸板放到中间（EXT-KB-SPLIT-V2）。 */
    public static final String VARIANT_SIMPLE_V2 = "simplev2";
    public static final String VARIANT_DIGITPAD = "digitpad";

    /** 界面展示顺序固定：电脑全键盘 / 简化键盘 / 简化v2 / 数字键盘。 */
    public static final String[] ALL = {
            VARIANT_FULL, VARIANT_SIMPLE, VARIANT_SIMPLE_V2, VARIANT_DIGITPAD};

    /** 覆盖安装（没有记录）时沿用旧版的切换顺序。 */
    private static final List<String> LEGACY_ORDER =
            Arrays.asList(VARIANT_SIMPLE, VARIANT_FULL, VARIANT_DIGITPAD);

    /** 全新安装的默认启用项与顺序。 */
    private static final List<String> FRESH_ORDER =
            Arrays.asList(VARIANT_SIMPLE, VARIANT_DIGITPAD);

    private KeyboardVariantStore() {
    }

    /** 已启用的布局，按切换顺序排列。 */
    public static List<String> getEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String raw = prefs.getString(PREF_KEY, null);
        if (TextUtils.isEmpty(raw)) {
            // 覆盖安装 / 老版本升级：不写 pref，按旧版写死的顺序回落
            return new ArrayList<>(LEGACY_ORDER);
        }
        List<String> result = new ArrayList<>();
        for (String part : raw.split(",")) {
            String id = part.trim();
            if (isVariant(id) && !result.contains(id)) {
                result.add(id);
            }
        }
        if (result.isEmpty()) {
            // 数据被写坏时兜底：至少留一种，避免顶栏按钮切到空布局
            result.add(VARIANT_SIMPLE);
        }
        return result;
    }

    public static boolean isEnabled(Context context, String id) {
        return getEnabled(context).contains(id);
    }

    public static void saveEnabled(Context context, List<String> ids) {
        List<String> clean = new ArrayList<>();
        for (String id : ids) {
            if (isVariant(id) && !clean.contains(id)) {
                clean.add(id);
            }
        }
        if (clean.isEmpty()) {
            clean.add(VARIANT_SIMPLE);
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(PREF_KEY, TextUtils.join(",", clean))
                .apply();
    }

    /**
     * 切换某个布局的启用状态：新启用的追加到末尾（点亮顺序即切换顺序）。
     *
     * @return 切换后的列表；若这次操作会把最后一个也关掉，返回 null 且不做任何改动
     */
    public static List<String> toggle(Context context, String id) {
        if (!isVariant(id)) {
            return getEnabled(context);
        }
        List<String> ids = getEnabled(context);
        if (ids.contains(id)) {
            if (ids.size() <= 1) {
                return null;
            }
            ids.remove(id);
        } else {
            ids.add(id);
        }
        saveEnabled(context, ids);
        return ids;
    }

    /** 全新安装：只启用 简化 + 数字；覆盖安装不动。可重复调用。 */
    public static void ensureInstalledDefault(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.contains(PREF_KEY)) {
            return;
        }
        if (!isFreshInstall(context)) {
            return;
        }
        saveEnabled(context, FRESH_ORDER);
    }

    /**
     * 是否全新安装：覆盖安装后 lastUpdateTime 一定晚于 firstInstallTime。
     * 比「prefs 里有没有别的键」更可靠——老用户可能还没动过任何设置。
     */
    private static boolean isFreshInstall(Context context) {
        try {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return info.firstInstallTime == info.lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isVariant(String id) {
        for (String variant : ALL) {
            if (variant.equals(id)) {
                return true;
            }
        }
        return false;
    }
}
