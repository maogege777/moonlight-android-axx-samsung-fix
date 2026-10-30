package com.limelight.ui.gamemenu;

import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.R;
import com.limelight.binding.input.virtual_controller.VirtualController;
import com.limelight.binding.input.virtual_controller.keyboard.KeyBoardController;
import com.limelight.binding.input.virtual_controller.keyboard.KeyboardVariantStore;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;
import com.limelight.utils.UiHelper;

import java.util.List;

import static com.limelight.binding.input.virtual_controller.keyboard.KeyBoardControllerConfigurationLoader.OSC_GAMEPAD_PREFERENCE;
import static com.limelight.binding.input.virtual_controller.keyboard.KeyBoardControllerConfigurationLoader.OSC_GAMEPAD_PREFERENCE_VALUE;
import static com.limelight.binding.input.virtual_controller.keyboard.KeyBoardControllerConfigurationLoader.OSC_PREFERENCE;
import static com.limelight.binding.input.virtual_controller.keyboard.KeyBoardControllerConfigurationLoader.OSC_PREFERENCE_VALUE;

/**
 * Description
 * Date: 2024-10-20
 * Time: 16:07
 */
public class GameMenuVirtualViewFragment extends BaseGameMenuDialog implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    @Override
    public int getLayoutRes() {
        return R.layout.dialog_game_menu_virtual_gamepad;
    }

    private ImageButton ibtn_back;
    private TextView tx_title;

    private String title;

    private Button btn_vibration;

    private Button btn_vibration_on_press;

    private Button btn_vibration_strength;

    private LinearLayout ll_ds_touchpad_row;

    private Button btn_vibration_gamepad;

    private Button btn_ds_touchpad;
    private boolean dsTouchpadSupported;
    private boolean dsTouchpadVisible;

    private SeekBar sb_adjust_keyboard_all;

    private SeekBar sb_height_keyboard_all;

    private SeekBar sb_adjust_virtual_gamepad;

    private SeekBar sb_gamepad_scale_factor;

    private TextView tx_gamepad_scale_factor;

    private TextView tx_adjust_keyboard_all;

    private TextView tx_height_keyboard_all;

    private TextView tx_adjust_virtual_gamepad;

    private RadioGroup rg_game_virtual_pad;

    private RadioGroup rg_game_virtual_pad_key;

    private RadioGroup rg_game_virtual_key_scheme;

    private RadioGroup rg_game_virtual_key_color;

    // EXTENSION DEVELOPMENT [EXT-KB-VARIANTS] [ADDED] BEGIN
    // 全键盘布局启停：四个 chip 与 KeyboardVariantStore.ALL 一一对应
    // （电脑全键盘 / 简化键盘 / 简化v2 / 数字键盘），顺序必须与 ALL 保持一致
    private static final int[] KB_VARIANT_CHIP_IDS = {
            R.id.kb_variant_full, R.id.kb_variant_simple,
            R.id.kb_variant_simplev2, R.id.kb_variant_digitpad};
    private static final int[] KB_VARIANT_LABEL_IDS = {
            R.id.tx_kb_variant_full, R.id.tx_kb_variant_simple,
            R.id.tx_kb_variant_simplev2, R.id.tx_kb_variant_digitpad};
    private static final int[] KB_VARIANT_BADGE_IDS = {
            R.id.tx_kb_variant_full_no, R.id.tx_kb_variant_simple_no,
            R.id.tx_kb_variant_simplev2_no, R.id.tx_kb_variant_digitpad_no};

    private final View[] kbVariantChips = new View[4];
    private final TextView[] kbVariantLabels = new TextView[4];
    private final TextView[] kbVariantBadges = new TextView[4];
    // EXTENSION DEVELOPMENT [EXT-KB-VARIANTS] [ADDED] END

    private KeyBoardController.ControllerMode gamePadMode;
    private KeyBoardController.ControllerMode gameKeyMode;

    private String[] keyValues;
    private String keyName;

    private String[] keyValuesGamePad;
    private String keyNameGamePad;

    private RadioGroup rg_game_virtual_game_scheme;
    @Override
    public void bindView(View v) {
        super.bindView(v);
        ibtn_back=v.findViewById(R.id.ibtn_back);
        tx_title=v.findViewById(R.id.tx_title);

        btn_vibration=v.findViewById(R.id.btn_vibration);
        btn_vibration_on_press=v.findViewById(R.id.btn_vibration_on_press);
        btn_vibration_strength=v.findViewById(R.id.btn_vibration_strength);
        ll_ds_touchpad_row=v.findViewById(R.id.ll_ds_touchpad_row);
        btn_vibration_gamepad=v.findViewById(R.id.btn_vibration_gamepad);
        btn_ds_touchpad=v.findViewById(R.id.btn_ds_touchpad);
        sb_adjust_keyboard_all=v.findViewById(R.id.sb_adjust_keyboard_all);
        sb_height_keyboard_all=v.findViewById(R.id.sb_height_keyboard_all);
        sb_adjust_virtual_gamepad=v.findViewById(R.id.sb_adjust_virtual_gamepad);

        tx_adjust_keyboard_all=v.findViewById(R.id.tx_adjust_keyboard_all);
        tx_height_keyboard_all=v.findViewById(R.id.tx_height_keyboard_all);
        tx_adjust_virtual_gamepad=v.findViewById(R.id.tx_adjust_virtual_gamepad);

        rg_game_virtual_key_scheme=v.findViewById(R.id.rg_game_virtual_key_scheme);
        rg_game_virtual_game_scheme=v.findViewById(R.id.rg_game_virtual_game_scheme);
        rg_game_virtual_pad=v.findViewById(R.id.rg_game_virtual_pad);
        rg_game_virtual_pad_key=v.findViewById(R.id.rg_game_virtual_key);

        rg_game_virtual_key_color=v.findViewById(R.id.rg_game_virtual_key_color);

        // EXTENSION DEVELOPMENT [EXT-KB-VARIANTS] [ADDED] BEGIN
        for (int i = 0; i < KB_VARIANT_CHIP_IDS.length; i++) {
            kbVariantChips[i] = v.findViewById(KB_VARIANT_CHIP_IDS[i]);
            kbVariantLabels[i] = v.findViewById(KB_VARIANT_LABEL_IDS[i]);
            kbVariantBadges[i] = v.findViewById(KB_VARIANT_BADGE_IDS[i]);
            if (kbVariantChips[i] != null) {
                kbVariantChips[i].setOnClickListener(this);
            }
        }
        renderKbVariants();
        // EXTENSION DEVELOPMENT [EXT-KB-VARIANTS] [ADDED] END

        sb_gamepad_scale_factor=v.findViewById(R.id.sb_gamepad_scale_factor);
        tx_gamepad_scale_factor=v.findViewById(R.id.tx_gamepad_scale_factor);

        if(!TextUtils.isEmpty(title)){
            tx_title.setText(title);
        }
        initViewData();
        initViewHeight();
        initViewAdjust();

        ibtn_back.setOnClickListener(this);
        btn_vibration.setOnClickListener(this);
        btn_vibration_on_press.setOnClickListener(this);
        btn_vibration_strength.setOnClickListener(this);
        btn_vibration_gamepad.setOnClickListener(this);
        btn_ds_touchpad.setOnClickListener(this);
        v.findViewById(R.id.btn_right).setOnClickListener(this);

        sb_adjust_keyboard_all.setOnSeekBarChangeListener(this);
        sb_height_keyboard_all.setOnSeekBarChangeListener(this);
        sb_adjust_virtual_gamepad.setOnSeekBarChangeListener(this);
        sb_gamepad_scale_factor.setOnSeekBarChangeListener(this);

        switch (gamePadMode){
            case Active:
                rg_game_virtual_pad.check(R.id.btn_game_virtual_nomall);
                break;
            case MoveButtons:
                rg_game_virtual_pad.check(R.id.btn_game_virtual_move);
                break;
        }

        switch (gameKeyMode){
            case Active:
                rg_game_virtual_pad_key.check(R.id.btn_game_virtual_key_nomall);
                break;
            case MoveButtons:
                rg_game_virtual_pad_key.check(R.id.btn_game_virtual_key_move);
                break;
        }

        rg_game_virtual_pad.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(onClick==null){
                    return;
                }
                if(checkedId==R.id.btn_game_virtual_nomall){
                    onClick.switchModeGamePad("正常模式", KeyBoardController.ControllerMode.Active);
                    return;
                }
                if(checkedId==R.id.btn_game_virtual_move){
                    onClick.switchModeGamePad("编辑模式", KeyBoardController.ControllerMode.MoveButtons);
                    Toast.makeText(getActivity(),"已进入编辑模式，关闭游戏菜单，进行操作！",Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });

        rg_game_virtual_pad_key.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(onClick==null){
                    return;
                }
                if(checkedId==R.id.btn_game_virtual_key_nomall){
                    onClick.switchModeGameKey("正常模式", KeyBoardController.ControllerMode.Active);
                    return;
                }
                if(checkedId==R.id.btn_game_virtual_key_move){
                    onClick.switchModeGameKey("编辑模式", KeyBoardController.ControllerMode.MoveButtons);
                    Toast.makeText(getActivity(),"已进入编辑模式，关闭游戏菜单，进行操作！",Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });

        keyValues=getResources().getStringArray(R.array.keyboard_axi_values);
        keyName = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(OSC_PREFERENCE, OSC_PREFERENCE_VALUE);

        for (int i = 0; i < keyValues.length; i++) {
            if(TextUtils.equals(keyName,keyValues[i])){
                switch (i){
                    case 0:
                        rg_game_virtual_key_scheme.check(R.id.btn_game_virtual_key_scheme_1);
                        break;
                    case 1:
                        rg_game_virtual_key_scheme.check(R.id.btn_game_virtual_key_scheme_2);
                        break;
                    case 2:
                        rg_game_virtual_key_scheme.check(R.id.btn_game_virtual_key_scheme_3);
                        break;
                    case 3:
                        rg_game_virtual_key_scheme.check(R.id.btn_game_virtual_key_scheme_4);
                        break;
                    case 4:
                        rg_game_virtual_key_scheme.check(R.id.btn_game_virtual_key_scheme_5);
                        break;
                }
                break;
            }
        }
        rg_game_virtual_key_scheme.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId==R.id.btn_game_virtual_key_scheme_1){
                    keyName=keyValues[0];
                }
                if(checkedId==R.id.btn_game_virtual_key_scheme_2){
                    keyName=keyValues[1];
                }
                if(checkedId==R.id.btn_game_virtual_key_scheme_3){
                    keyName=keyValues[2];
                }
                if(checkedId==R.id.btn_game_virtual_key_scheme_4){
                    keyName=keyValues[3];
                }
                if(checkedId==R.id.btn_game_virtual_key_scheme_5){
                    keyName=keyValues[4];
                }
                PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .edit()
                        .putString(OSC_PREFERENCE,keyName)
                        .commit();
            }
        });

        //虚拟手柄
        keyValuesGamePad=getResources().getStringArray(R.array.gamepad_axi_values);
        keyNameGamePad = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(OSC_GAMEPAD_PREFERENCE, OSC_GAMEPAD_PREFERENCE_VALUE);

        for (int i = 0; i < keyValuesGamePad.length; i++) {
            if(TextUtils.equals(keyNameGamePad,keyValuesGamePad[i])){
                switch (i){
                    case 0:
                        rg_game_virtual_game_scheme.check(R.id.btn_game_virtual_game_scheme_1);
                        break;
                    case 1:
                        rg_game_virtual_game_scheme.check(R.id.btn_game_virtual_game_scheme_2);
                        break;
                    case 2:
                        rg_game_virtual_game_scheme.check(R.id.btn_game_virtual_game_scheme_3);
                        break;
                    case 3:
                        rg_game_virtual_game_scheme.check(R.id.btn_game_virtual_game_scheme_4);
                        break;
                    case 4:
                        rg_game_virtual_game_scheme.check(R.id.btn_game_virtual_game_scheme_5);
                        break;
                }
                break;
            }
        }
        rg_game_virtual_game_scheme.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId==R.id.btn_game_virtual_game_scheme_1){
                    keyNameGamePad=keyValuesGamePad[0];
                }
                if(checkedId==R.id.btn_game_virtual_game_scheme_2){
                    keyNameGamePad=keyValuesGamePad[1];
                }
                if(checkedId==R.id.btn_game_virtual_game_scheme_3){
                    keyNameGamePad=keyValuesGamePad[2];
                }
                if(checkedId==R.id.btn_game_virtual_game_scheme_4){
                    keyNameGamePad=keyValuesGamePad[3];
                }
                if(checkedId==R.id.btn_game_virtual_game_scheme_5){
                    keyNameGamePad=keyValuesGamePad[4];
                }
                PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .edit()
                        .putString(OSC_GAMEPAD_PREFERENCE,keyNameGamePad)
                        .commit();
            }
        });


        switch (PreferenceConfiguration.readPreferences(getActivity()).virtualkeyViewNormalColor){
            case 0xF0000000:
                rg_game_virtual_key_color.check(R.id.btn_game_virtual_key_color_1);
                break;
            case 0xF0FFFFFF:
                rg_game_virtual_key_color.check(R.id.btn_game_virtual_key_color_2);
                break;
            case 0xFF888888:
                rg_game_virtual_key_color.check(R.id.btn_game_virtual_key_color_3);
                break;
        }

        rg_game_virtual_key_color.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int value=0xFF888888;
                if(checkedId==R.id.btn_game_virtual_key_color_1){
                    value=0xF0000000;
                }
                if(checkedId==R.id.btn_game_virtual_key_color_2){
                    value=0xF0FFFFFF;
                }
                PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .edit()
                        .putInt("virtual_key_view_normal_color",value)
                        .commit();
            }
        });

//        switch (PreferenceConfiguration.readPreferences(getActivity()).gamepad_skin){
//            case 0:
//                rg_game_pad_skin.check(R.id.btn_game_pad_skin_1);
//                break;
//            case 1:
//                rg_game_pad_skin.check(R.id.btn_game_pad_skin_2);
//                break;
//            case 2:
//                rg_game_pad_skin.check(R.id.btn_game_pad_skin_3);
//                break;
//        }
//
//        rg_game_pad_skin.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(RadioGroup group, int checkedId) {
//                int value=0;
//                if(checkedId==R.id.btn_game_pad_skin_2){
//                    value=1;
//                }
//                if(checkedId==R.id.btn_game_pad_skin_3){
//                    value=2;
//                }
//                if(prefConfig!=null){
//                    prefConfig.gamepad_skin=value;
//                }
//                PreferenceManager.getDefaultSharedPreferences(getActivity())
//                        .edit()
//                        .putInt("onscreen_game_pad_skin",value)
//                        .commit();
//                if(onClick!=null){
//                    onClick.click("刷新",0);
//                }
//            }
//        });
    }

    @Override
    public float getDimAmount() {
        return super.getDimAmount();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setGameKeyMode(KeyBoardController.ControllerMode gameKeyMode) {
        this.gameKeyMode = gameKeyMode;
    }

    public void setGamePadMode(KeyBoardController.ControllerMode gamePadMode) {
        this.gamePadMode = gamePadMode;
    }

    private void initViewData(){
        btn_vibration.setBackgroundResource(prefConfig.enableKeyboardVibrate?R.drawable.ic_game_menu_btn_green_selector:R.drawable.ic_game_menu_btn_selector);

        //“仅按下震动”是“按键震动”的子选项：主开关关闭时置灰，避免误以为它单独生效
        btn_vibration_on_press.setEnabled(prefConfig.enableKeyboardVibrate);
        btn_vibration_on_press.setAlpha(prefConfig.enableKeyboardVibrate ? 1f : 0.4f);
        btn_vibration_on_press.setBackgroundResource(
                prefConfig.enableKeyboardVibrate && prefConfig.enableKeyboardVibrateOnPressOnly
                        ? R.drawable.ic_game_menu_btn_green_selector
                        : R.drawable.ic_game_menu_btn_selector);

        btn_vibration_gamepad.setBackgroundResource(prefConfig.vibrateOsc?R.drawable.ic_game_menu_btn_green_selector:R.drawable.ic_game_menu_btn_selector);

        //DS 触控板单独一行（与上面每格同为 1/3 宽），不支持时整行 GONE，不再留空白
        ll_ds_touchpad_row.setVisibility(dsTouchpadSupported ? View.VISIBLE : View.GONE);
        btn_ds_touchpad.setBackgroundResource(dsTouchpadVisible ?
                R.drawable.ic_game_menu_btn_green_selector : R.drawable.ic_game_menu_btn_selector);

        //两行显示：标题 + 当前时长，右侧箭头提示可进入
        // EXTENSION DEVELOPMENT [EXT-VIBRATE-SYSTEM-DEFAULT] [MODIFIED] BEGIN
        // 0 = 系统默认，跟随手机自身的振动强度设置
        btn_vibration_strength.setText("震感强度\n"
                + (prefConfig.vibratePressMs == PreferenceConfiguration.VIBRATE_PRESS_SYSTEM_DEFAULT
                ? "系统默认" : prefConfig.vibratePressMs + "ms"));
        // EXTENSION DEVELOPMENT [EXT-VIBRATE-SYSTEM-DEFAULT] [MODIFIED] END
    }

    // EXTENSION DEVELOPMENT [EXT-KB-VARIANTS] [ADDED] BEGIN
    /**
     * 刷新全键盘布局启停按钮：点亮 = 启用（绿底 + 白字粗体 + 顺序角标），
     * 角标数字就是顶栏「布局 ▾」按钮每次点击的切换顺序。
     */
    private void renderKbVariants() {
        if (getActivity() == null) {
            return;
        }
        List<String> enabled = KeyboardVariantStore.getEnabled(getActivity());
        for (int i = 0; i < KB_VARIANT_CHIP_IDS.length; i++) {
            String variant = KeyboardVariantStore.ALL[i];
            int index = enabled.indexOf(variant);
            boolean on = index >= 0;

            if (kbVariantChips[i] != null) {
                kbVariantChips[i].setBackgroundResource(on
                        ? R.drawable.ic_game_menu_btn_green_selector
                        : R.drawable.ic_game_menu_btn_selector);
            }
            if (kbVariantLabels[i] != null) {
                kbVariantLabels[i].setTextColor(on ? 0xFFFFFFFF : 0xFF9AA0A6);
                kbVariantLabels[i].setTypeface(null, on ? Typeface.BOLD : Typeface.NORMAL);
            }
            if (kbVariantBadges[i] != null) {
                kbVariantBadges[i].setVisibility(on ? View.VISIBLE : View.GONE);
                if (on) {
                    kbVariantBadges[i].setText(String.valueOf(index + 1));
                }
            }
        }
    }
    // EXTENSION DEVELOPMENT [EXT-KB-VARIANTS] [ADDED] END

    private void initViewHeight(){
        sb_height_keyboard_all.setProgress(prefConfig.oscKeyboardHeight);
        tx_height_keyboard_all.setText("高度："+prefConfig.oscKeyboardHeight);

    }

    private void initViewAdjust(){
        sb_adjust_virtual_gamepad.setProgress(prefConfig.oscOpacity);
        tx_adjust_virtual_gamepad.setText("透明度："+prefConfig.oscOpacity+"%");

        sb_adjust_keyboard_all.setProgress(prefConfig.oscKeyboardOpacity);

        tx_adjust_keyboard_all.setText("透明度："+prefConfig.oscKeyboardOpacity+"%");

        sb_gamepad_scale_factor.setProgress(prefConfig.virtualGamePadScaleFactor);
        tx_gamepad_scale_factor.setText("缩放："+prefConfig.virtualGamePadScaleFactor+"%");
    }


    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.ibtn_back){
            dismiss();
            return;
        }

        // EXTENSION DEVELOPMENT [EXT-KB-VARIANTS] [ADDED] BEGIN
        for (int i = 0; i < KB_VARIANT_CHIP_IDS.length; i++) {
            if (v.getId() != KB_VARIANT_CHIP_IDS[i]) {
                continue;
            }
            // 至少保留一种：只剩一个时再点它会被拒绝，状态不变
            if (KeyboardVariantStore.toggle(getActivity(), KeyboardVariantStore.ALL[i]) == null) {
                Toast.makeText(getActivity(), "至少要保留一种键盘布局", Toast.LENGTH_SHORT).show();
                return;
            }
            renderKbVariants();
            return;
        }
        // EXTENSION DEVELOPMENT [EXT-KB-VARIANTS] [ADDED] END

        if(v.getId()==R.id.btn_right){
            if(onClick!=null){
                onClick.click("刷新",0);
            }
            return;
        }

        if(v.getId()==R.id.btn_vibration){
            prefConfig.enableKeyboardVibrate=!prefConfig.enableKeyboardVibrate;
            initViewData();
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putBoolean(PreferenceConfiguration.CHECKBOX_ENABLE_KEYBOARD_VIBRATE,prefConfig.enableKeyboardVibrate)
                    .commit();
            return;
        }
        if(v.getId()==R.id.btn_vibration_strength){
            GameVibrationStrengthFragment fragment=new GameVibrationStrengthFragment();
            fragment.setWidth(UiHelper.dpToPx(getActivity(),364));
            fragment.setValue(prefConfig.vibratePressMs);
            fragment.setOnClick(ms -> {
                prefConfig.vibratePressMs=ms;
                initViewData();
                PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .edit()
                        .putInt(PreferenceConfiguration.EDIT_VIBRATE_PRESS_MS,ms)
                        .commit();
            });
            fragment.show(getFragmentManager());
            return;
        }
        if(v.getId()==R.id.btn_vibration_on_press){
            if(!prefConfig.enableKeyboardVibrate){
                Toast.makeText(getActivity(), "请先开启按键震动", Toast.LENGTH_SHORT).show();
                return;
            }
            prefConfig.enableKeyboardVibrateOnPressOnly=!prefConfig.enableKeyboardVibrateOnPressOnly;
            initViewData();
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putBoolean(PreferenceConfiguration.CHECKBOX_VIBRATE_ON_PRESS_ONLY,prefConfig.enableKeyboardVibrateOnPressOnly)
                    .commit();
            return;
        }
        if(v.getId()==R.id.btn_vibration_gamepad){
            prefConfig.vibrateOsc=!prefConfig.vibrateOsc;
            initViewData();
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putBoolean(PreferenceConfiguration.VIBRATE_OSC_PREF_STRING,prefConfig.vibrateOsc)
                    .commit();
            return;
        }
        if(v.getId()==R.id.btn_ds_touchpad){
            if(onClick!=null){
                dsTouchpadVisible=onClick.toggleDsTouchpad();
                initViewData();
            }
            return;
        }
    }

    private PreferenceConfiguration prefConfig;

    public void setPrefConfig(PreferenceConfiguration prefConfig) {
        this.prefConfig = prefConfig;
    }

    public void setDsTouchpadState(boolean supported, boolean visible) {
        dsTouchpadSupported = supported;
        dsTouchpadVisible = supported && visible;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(seekBar==sb_height_keyboard_all){
            prefConfig.oscKeyboardHeight=progress;
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putInt("seekbar_keyboard_axi_height",progress)
                    .commit();
            initViewHeight();
        }
        if(seekBar==sb_adjust_keyboard_all){
            prefConfig.oscKeyboardOpacity=progress;
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putInt("seekbar_keyboard_axi_opacity",progress)
                    .commit();
            initViewAdjust();
        }
        if(seekBar==sb_adjust_virtual_gamepad){
            prefConfig.oscOpacity=progress;
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putInt(PreferenceConfiguration.OSC_OPACITY_PREF_STRING,progress)
                    .commit();
            initViewAdjust();
        }

        if(seekBar==sb_gamepad_scale_factor){
            prefConfig.virtualGamePadScaleFactor=progress;
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putInt("virtualGamePadScaleFactor",progress)
                    .commit();
            initViewAdjust();
        }

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private onClick onClick;

    public interface onClick{
        void click(String name,int index);
        void switchModeGamePad(String name, KeyBoardController.ControllerMode mode);
        void switchModeGameKey(String name, KeyBoardController.ControllerMode mode);
        boolean toggleDsTouchpad();
    }

    public void setOnClick(onClick onClick) {
        this.onClick = onClick;
    }
}
