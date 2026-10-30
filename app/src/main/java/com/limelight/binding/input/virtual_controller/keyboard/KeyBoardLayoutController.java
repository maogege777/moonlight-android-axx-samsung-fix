/**
 * Created by Karim Mreisi.
 */

package com.limelight.binding.input.virtual_controller.keyboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.Game;
import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.binding.input.ControllerHandler;
import com.limelight.binding.input.evdev.EvdevListener;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.gamemenu.GameKeyboardUpdateFragment;
import com.limelight.ui.gamemenu.FullKeyboardKeyStore;
import com.limelight.ui.gamemenu.bean.GameMenuQuickBean;
import com.limelight.ui.gamemenu.TouchPadView;

import java.util.ArrayList;
import java.util.List;

public class KeyBoardLayoutController {

    private final ControllerHandler controllerHandler;
    private final Context context;
    private FrameLayout frame_layout = null;
    private Vibrator vibrator;
    private LinearLayout keyboardView;
    private RadioGroup rg_keyboard;
    private TextView tvMode;

    /** 顶栏右侧的全键盘快捷键栏：自定义快捷键，横向滚动 + 末尾添加按钮 */
    private HorizontalScrollView hsvCombo;
    private LinearLayout llCombo;
    private PreferenceConfiguration prefConfig;

    // 本次触摸手势是否已触发按下震动（与 KeyBoardController 的规则一致）
    private boolean pressHapticFired;

    public KeyBoardLayoutController(final ControllerHandler controllerHandler, FrameLayout layout, final Context context,PreferenceConfiguration prefConfig) {
        this.controllerHandler = controllerHandler;
        this.frame_layout = layout;
        this.context = context;
        this.prefConfig=prefConfig;
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        this.keyboardView= (LinearLayout) LayoutInflater.from(context).inflate(R.layout.layout_axixi_keyboard,null);
        initKeyboard();
    }

    private List<String> keyList=new ArrayList<>();

    //组合键模式
    private boolean isCombination=false;
    /**
     * 「:」键的伪 tag（不是真实 keycode）：普通点击时自动带 Shift，输出 :；
     * 组合模式下勾选了 Shift 时反过来只发分号，输出 ;。
     */
    private static final String TAG_COLON = "1001";
    private boolean colonShiftSent;

    private void initKeyboard(){
        //整块拦截触摸：键盘范围内的空白、行间距不再漏给下层串流画面。
        //ViewGroup 派发顺序是子 View 先拿事件，所以按键按钮的点击不受影响。
        keyboardView.setOnTouchListener((v, event) -> true);

        hsvCombo = keyboardView.findViewById(R.id.hsv_combo);
        llCombo = keyboardView.findViewById(R.id.ll_combo);
        rebuildComboBar();

        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 处理按下事件
                        String tag=(String) v.getTag();
                        if(TextUtils.equals("hide",tag)){
                            return true;
                        }
                        //隐藏键之外，所有按键（含修饰键、鼠标键）按下都震动一次
                        notifyPressHapticOnDown();
                        if(!TextUtils.isEmpty(tag)&&tag.startsWith("mouse_")){
                            int code=1;
                            switch (tag){
                                case "mouse_1":
                                    code=1;
                                    break;
                                case "mouse_2":
                                    code=2;
                                    break;
                                case "mouse_3":
                                    code=3;
                                    break;
                            }
                            KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, code);
                            keyEvent.setSource(1);
                            sendKeyEvent(keyEvent);
                            keyDown(v);
                            return true;
                        }

                        if (TAG_COLON.equals(tag)) {
                            // 「:」键：普通点击自动带 Shift 输出 :；
                            // 组合模式里已勾选 Shift 时反过来，只发分号输出 ;
                            colonShiftSent = !(isCombination && keyList.contains("59"));
                            if (colonShiftSent) {
                                KeyEvent colonShiftDown = new KeyEvent(KeyEvent.ACTION_DOWN,
                                        KeyEvent.KEYCODE_SHIFT_LEFT);
                                colonShiftDown.setSource(0);
                                sendKeyEvent(colonShiftDown);
                            }
                            KeyEvent colonDown = new KeyEvent(KeyEvent.ACTION_DOWN,
                                    KeyEvent.KEYCODE_SEMICOLON);
                            colonDown.setSource(0);
                            sendKeyEvent(colonDown);
                            keyDown(v);
                            return true;
                        }

                        if(isCombination){
                            switch (tag){
                                case "113"://ctrl
                                case "114"://ctrl right
                                case "117"://win
                                case "57"://alt
                                case "58"://alt right
                                case "59"://shift
                                    if(!keyList.contains(tag)){
                                        keyList.add(tag);
                                        rememberKeyBg(v);
                                        v.setBackgroundResource(R.drawable.bg_ax_keyboard_button_press);
                                    }else{
                                        keyList.remove(tag);
                                        keyUp(v);
                                    }
                                    return true;
                            }
                            if(!keyList.isEmpty()){
                                for (String t:keyList) {
                                    KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, Integer.parseInt(t));
                                    keyEvent.setSource(0);
                                    sendKeyEvent(keyEvent);
                                }
                            }
                        }
                        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, Integer.parseInt(tag));
                        keyEvent.setSource(0);
                        sendKeyEvent(keyEvent);
                        keyDown(v);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // 处理释放事件
                        resetPressHapticGesture();
                        String tag2=(String) v.getTag();
                        if(TextUtils.equals("hide",tag2)){
//                            hide();
                            return true;
                        }
                        if(!TextUtils.isEmpty(tag2)&&tag2.startsWith("mouse_")){
                            int code=1;
                            switch (tag2){
                                case "mouse_1":
                                    code=1;
                                    break;
                                case "mouse_2":
                                    code=2;
                                    break;
                                case "mouse_3":
                                    code=3;
                                    break;
                            }
                            KeyEvent keyUP = new KeyEvent(KeyEvent.ACTION_UP, code);
                            keyUP.setSource(1);
                            sendKeyEvent(keyUP);
                            keyUp(v);
                            return true;
                        }
                        if (TAG_COLON.equals(tag2)) {
                            KeyEvent colonUp = new KeyEvent(KeyEvent.ACTION_UP,
                                    KeyEvent.KEYCODE_SEMICOLON);
                            colonUp.setSource(0);
                            sendKeyEvent(colonUp);
                            if (colonShiftSent) {
                                KeyEvent colonShiftUp = new KeyEvent(KeyEvent.ACTION_UP,
                                        KeyEvent.KEYCODE_SHIFT_LEFT);
                                colonShiftUp.setSource(0);
                                sendKeyEvent(colonShiftUp);
                            }
                            colonShiftSent = false;
                            keyUp(v);
                            return true;
                        }
                        if(isCombination){
                            switch (tag2) {
                                case "113"://ctrl
                                case "114"://ctrl right
                                case "117"://win
                                case "57"://alt
                                case "58"://alt right
                                case "59"://shift
                                    return true;
                            }
                            // Release the main key before its modifiers.
                            sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, Integer.parseInt(tag2)));
                            if(!keyList.isEmpty()){
                                for (int i = keyList.size() - 1; i >= 0; i--) {
                                    String t = keyList.get(i);
                                    KeyEvent keyEvent2 = new KeyEvent(KeyEvent.ACTION_UP, Integer.parseInt(t));
                                    keyEvent2.setSource(0);
                                    sendKeyEvent(keyEvent2);
                                    View modView = keyboardView.findViewById(R.id.lv_keyboard).findViewWithTag(t);
                                    if (modView != null) keyUp(modView);
                                    modView = keyboardView.findViewById(R.id.lv_keyboard_digitpad).findViewWithTag(t);
                                    if (modView != null) keyUp(modView);
                                    modView = keyboardView.findViewById(R.id.lv_keyboard_simple).findViewWithTag(t);
                                    if (modView != null) keyUp(modView);
                                }
                                keyList.clear();
                            }
                            keyUp(v);
                            return true;
                        }
                        KeyEvent keyUP = new KeyEvent(KeyEvent.ACTION_UP, Integer.parseInt(tag2));
                        keyUP.setSource(0);
                        sendKeyEvent(keyUP);
                        keyUp(v);
                        return true;
                }
                return false;
            }
        };
        // 递归绑定：全键盘 + 数字键盘一次绑完（数字键盘的跨行回车多了一层容器）
        bindKeys(keyboardView, touchListener);

        CheckBox btn_combination=keyboardView.findViewById(R.id.btn_combination);
        attachTopBarHaptic(btn_combination);
        btn_combination.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isCombination=isChecked;
                LimeLog.info("axi-2-"+isCombination);
                resetView();
            }
        });
        //组合键模式
        if(prefConfig.keyboard_axi_combination){
            btn_combination.setChecked(true);
            isCombination=true;
        }

        keyboardView.findViewById(R.id.iv_down).setOnClickListener(v -> hide());
        attachTopBarHaptic(keyboardView.findViewById(R.id.iv_down));

        keyboardView.findViewById(R.id.iv_game_menu).setOnClickListener(v -> Game.instance.showGameMenu());
        attachTopBarHaptic(keyboardView.findViewById(R.id.iv_game_menu));

        rg_keyboard = keyboardView.findViewById(R.id.rg_keyboard);
        tvMode = keyboardView.findViewById(R.id.btn_keyboard_mode);

        // 顶栏「布局 ▾」：在菜单里点亮的布局之间按点亮顺序循环（EXT-KB-VARIANTS）
        tvMode.setOnClickListener(v -> cycleMode());
        attachTopBarHaptic(tvMode);
rg_keyboard.setOnCheckedChangeListener((group, checkedId) -> onModeChanged(checkedId));
        // 首次显示取「已启用」列表的第一个（新装与老用户此时都是简化键盘）
        ensureEnabledMode();
        // 两块触摸板都按 1:1 位移，和数字键盘手感一致
        bindTouchPad(keyboardView.findViewById(R.id.fv_keyboard_touch), 1f);
        bindTouchPad(keyboardView.findViewById(R.id.fv_keyboard_touch_mini), 1f);


        bindTouchPad(keyboardView.findViewById(R.id.fv_keyboard_touch_simple_r), 1f);




        bindScrollButton(keyboardView.findViewById(R.id.mouse_up), true);
        bindScrollButton(keyboardView.findViewById(R.id.mouse_down), false);
        bindScrollButton(keyboardView.findViewById(R.id.simple_scroll_up), true);
        bindScrollButton(keyboardView.findViewById(R.id.simple_scroll_down), false);
    }

    /** 滚轮键：按下连发（100ms 一次），抬手停止，按下时震一次。 */
    private void bindScrollButton(View btn, final boolean up) {
        if (btn == null) {
            return;
        }
        btn.setOnTouchListener(new View.OnTouchListener() {
            private final Runnable repeater = new Runnable() {
                @Override
                public void run() {
                    if (Game.instance != null) {
                        Game.instance.mouseHighResScroll(up);
                    }
                    btn.postDelayed(this, 100);
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        vibrateTap();
                        btn.post(repeater);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        btn.removeCallbacks(repeater);
                        return true;
                }
                return false;
            }
        });
    }
    // ---------------------------------------------------------------- 键面配色 / 按键绑定

    /**
     * 记下按键的原始背景（分组配色），只记一次。
     * 按下态和组合键高亮之后靠它还原，否则彩色键按一次就被刷回默认灰。
     */
    private void rememberKeyBg(View v) {
        if (v != null && v.getTag(R.id.kb_key_bg) == null) {
            v.setTag(R.id.kb_key_bg, v.getBackground());
        }
    }

    private void keyDown(View v) {
        rememberKeyBg(v);
        v.setBackgroundResource(confirmRes(v));
        setSplitEnterPressed(v, true);
    }

    /**
     * 按下态背景：大回车的上下两段各有与自身形状匹配的确认色（接缝侧直角、无边距），
     * 其余按键仍用统一的矩形确认背景。
     */
    private int confirmRes(View v) {
        int id = v == null ? View.NO_ID : v.getId();
        if (id == R.id.kb_enter_top) {
            return R.drawable.bg_ax_key_enter_top_confirm;
        }
        if (id == R.id.kb_enter_bottom) {
            return R.drawable.bg_ax_key_enter_bottom_confirm;
        }
        return R.drawable.bg_ax_keyboard_button_confirm;
    }

    private void keyUp(View v) {
        restoreKeyBg(v);
        setSplitEnterPressed(v, false);
    }

    private void restoreKeyBg(View v) {
        Object bg = (v == null) ? null : v.getTag(R.id.kb_key_bg);
        if (bg instanceof Drawable) {
            v.setBackground((Drawable) bg);
        } else if (v != null) {
            v.setBackgroundResource(R.drawable.bg_ax_keyboard_button);
        }
    }

    /**
     * 简化键盘的大回车由上下两段（tag 66）拼成 L 形：按下 / 抬起时两段同步变色，
     * 否则看起来像是两个各自独立的按键。
     */
    private void setSplitEnterPressed(View v, boolean pressed) {
        if (keyboardView == null || !(v.getTag() instanceof String)
                || !"66".equals(v.getTag())) {
            return;
        }
        View simple = keyboardView.findViewById(R.id.lv_keyboard_simple);
        if (!(simple instanceof ViewGroup)) {
            return;
        }
        List<View> parts = new ArrayList<>();
        collectTagged((ViewGroup) simple, "66", parts);
        if (parts.size() < 2) {
            return; // 只有一段（例如数字键盘）时不需要同步
        }
        for (View part : parts) {
            if (part == v) {
                continue;
            }
            if (pressed) {
                rememberKeyBg(part);
                part.setBackgroundResource(confirmRes(part));
            } else {
                restoreKeyBg(part);
            }
        }
    }

    private void collectTagged(ViewGroup group, String tag, List<View> out) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof ViewGroup) {
                collectTagged((ViewGroup) child, tag, out);
            } else if (tag.equals(child.getTag())) {
                out.add(child);
            }
        }
    }

    /**
     * 递归把所有按键绑到同一个触摸监听。
     * 数字键盘的「跨行回车 / 双格 0」多了一层容器，原来按「行 -> 键」两层写死的遍历会漏绑。
     * 触摸板与滚轮自带监听，跳过；顶栏按钮没有 String tag，不会被误绑。
     */
    private void bindKeys(ViewGroup group, View.OnTouchListener listener) {
        if (group == null) {
            return;
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TouchPadView) {
                continue;
            }
            if (child.getId() == R.id.mouse_up || child.getId() == R.id.mouse_down) {
                continue;
            }
            if (child instanceof ViewGroup) {
                bindKeys((ViewGroup) child, listener);
            } else if (child.getTag() instanceof String) {
                child.setOnTouchListener(listener);
            }
        }
    }

    /**
     * 触摸板：拖动移动光标、轻点左键、双指轻点右键。
     * mini 触摸板只有 60x32dp，位移要放大，否则划满全宽也走不了多远。
     */
    private void bindTouchPad(TouchPadView pad, final float sensitivity) {
        if (pad == null) {
            return;
        }
        pad.setLabel("触摸板");
        pad.setOnHapticListener(this::vibrateTap);
        pad.setViewLister(new TouchPadView.TouchPadViewLister() {
            @Override
            public void sendMouseMove(float dx, float dy) {
                if (Game.instance == null) {
                    return;
                }
                Game.instance.mouseMove(Math.round(dx * sensitivity), Math.round(dy * sensitivity));
            }

            @Override
            public void sendMouseLeft(boolean down) {
                if (Game.instance == null) {
                    return;
                }
                Game.instance.mouseButtonEvent(EvdevListener.BUTTON_LEFT, down);
            }

            @Override
            public void sendMouseRight(boolean down) {
                if (Game.instance == null) {
                    return;
                }
                Game.instance.mouseButtonEvent(EvdevListener.BUTTON_RIGHT, down);
            }

            @Override
            public void sendMouseScroll(float distance) {
                // 双指滚动暂不接管：垂直滚动用滚轮键
            }
        });
    }
    // ---------------------------------------------------------------- 键盘模式

    // EXTENSION DEVELOPMENT [EXT-KB-VARIANTS] [ADDED] BEGIN
    /** 布局标识与隐藏 RadioGroup 里按钮 id 的对照（顺序与 KeyboardVariantStore.ALL 一致）。 */
    private static final String[] MODE_VARIANTS = {
            KeyboardVariantStore.VARIANT_FULL,
            KeyboardVariantStore.VARIANT_SIMPLE,
            KeyboardVariantStore.VARIANT_DIGITPAD};
    private static final int[] MODE_VIEW_IDS = {
            R.id.rbt_keyboard_1, R.id.rbt_keyboard_3, R.id.rbt_keyboard_2};

    /**
     * 顶栏「布局 ▾」：只在「已启用」的布局里循环，顺序 = 菜单里点亮的顺序。
     * 启停与顺序来源：菜单 › 虚拟手柄与虚拟按键 › 全键盘。
     */
    private void cycleMode() {
        List<String> enabled = KeyboardVariantStore.getEnabled(context);
        if (enabled.size() <= 1) {
            Toast.makeText(context, "只启用了一种布局", Toast.LENGTH_SHORT).show();
            return;
        }
        int pos = enabled.indexOf(variantOfViewId(rg_keyboard.getCheckedRadioButtonId()));
        int next = pos < 0 ? 0 : (pos + 1) % enabled.size();
        rg_keyboard.check(viewIdOfVariant(enabled.get(next)));
    }

    /** 当前布局被关掉时自动切到第 1 个已启用布局（菜单「更新配置」后生效）。 */
    private void ensureEnabledMode() {
        if (rg_keyboard == null) {
            return;
        }
        List<String> enabled = KeyboardVariantStore.getEnabled(context);
        int checked = rg_keyboard.getCheckedRadioButtonId();
        if (checked != -1 && enabled.contains(variantOfViewId(checked))) {
            return;
        }
        rg_keyboard.check(viewIdOfVariant(enabled.get(0)));
    }

    private static String variantOfViewId(int viewId) {
        for (int i = 0; i < MODE_VARIANTS.length; i++) {
            if (MODE_VIEW_IDS[i] == viewId) {
                return MODE_VARIANTS[i];
            }
        }
        return KeyboardVariantStore.VARIANT_SIMPLE;
    }

    private static int viewIdOfVariant(String variant) {
        for (int i = 0; i < MODE_VARIANTS.length; i++) {
            if (MODE_VARIANTS[i].equals(variant)) {
                return MODE_VIEW_IDS[i];
            }
        }
        return R.id.rbt_keyboard_3;
    }
    // EXTENSION DEVELOPMENT [EXT-KB-VARIANTS] [ADDED] END

    /** 模式切换：谁能见、根容器要不要透明、位置气泡要不要显示，全在这里。 */
    private void onModeChanged(int checkedId) {
        resetView();

        View full = keyboardView.findViewById(R.id.lv_keyboard);
        View digit = keyboardView.findViewById(R.id.lv_keyboard_digitpad);
        View simple = keyboardView.findViewById(R.id.lv_keyboard_simple);

        boolean isSimple = (checkedId == R.id.rbt_keyboard_3);

        full.setVisibility(checkedId == R.id.rbt_keyboard_1 ? View.VISIBLE : View.GONE);
        digit.setVisibility(checkedId == R.id.rbt_keyboard_2 ? View.VISIBLE : View.GONE);
        simple.setVisibility(isSimple ? View.VISIBLE : View.GONE);

        // 三种模式共用同一块深色底板：顶栏、触摸板、键块连成一体，不再分区块
        keyboardView.setBackgroundColor(0xFF212121);


        if (tvMode != null) {
            tvMode.setText("布局 · " + modeName(checkedId) + " ▾");
        }

    }

    private String modeName(int checkedId) {
        if (checkedId == R.id.rbt_keyboard_2) {
            return "数字键盘";
        }
        if (checkedId == R.id.rbt_keyboard_3) {
            return "简化键盘";
        }
        return "电脑全键盘";
    }

    private void resetView(){
        keyList.clear();
//        isCombination=false;
        String[] keys={"113","114","117","57","58","59"};
        for (String key:keys) {
            View v = keyboardView.findViewById(R.id.lv_keyboard).findViewWithTag(key);
            if (v != null) {
                keyUp(v);
            }
            v = keyboardView.findViewById(R.id.lv_keyboard_digitpad).findViewWithTag(key);
            if (v != null) {
                keyUp(v);
            }
            v = keyboardView.findViewById(R.id.lv_keyboard_simple).findViewWithTag(key);
            if (v != null) {
                keyUp(v);
            }
        }
    }

    public void hide() {
        keyboardView.setVisibility(View.GONE);
    }

    public void show() {
        // 菜单里关掉的布局不应该再被显示：切到第 1 个已启用布局
        ensureEnabledMode();
        keyboardView.setVisibility(View.VISIBLE);
    }

    public boolean isVisible() {
        return keyboardView.getVisibility() == View.VISIBLE;
    }

    public void switchShowHide() {
        if (keyboardView.getVisibility() == View.VISIBLE) {
            hide();
        } else {
            show();
        }
        resetView();
    }

    public void refreshLayout() {
        frame_layout.removeView(keyboardView);
        // 布局启停可能刚在菜单里改过，先纠正当前布局再重算尺寸
        ensureEnabledMode();
//        DisplayMetrics screen = context.getResources().getDisplayMetrics();
//        (int)(screen.heightPixels/0.4)
        int height=prefConfig.oscKeyboardHeight;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity= Gravity.BOTTOM;
//        params.leftMargin = 20 + buttonSize;
//        params.topMargin = 15;

        // 全键盘保持原高度；简化键盘与数字键盘各矮一个 F 行（约 33dp）
        int fullH = dip2px(context, height);
        int shortH = Math.max(dip2px(context, 110), fullH - dip2px(context, 33));
        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, fullH);
        keyboardView.findViewById(R.id.lv_keyboard).setLayoutParams(params1);
        keyboardView.findViewById(R.id.lv_keyboard_digitpad).setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, shortH));

        keyboardView.findViewById(R.id.lv_keyboard_simple).setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, shortH));

        // 简化键盘宽度：手机（<=800dp）保持现状；屏幕更宽时，多出来的部分按比例分给两侧
        //   鼠标列 = 64 + 超出 x 5%      触摸板 = 42 + 超出 x 10%
        float density = context.getResources().getDisplayMetrics().density;
        int screenW = context.getResources().getDisplayMetrics().widthPixels;
        int screenDp = Math.round(screenW / density);
        int excess = Math.min(480, Math.max(0, screenDp - 800));
        int mouseW = dip2px(context, 64f + excess * 0.14f);
        int padW = dip2px(context, 42f + excess * 0.19f);
        int simpleWidth = screenW - mouseW - padW - dip2px(context, 6);

        View simpleKeys = keyboardView.findViewById(R.id.ll_simple_keys);
        if (simpleKeys != null) {
            simpleKeys.setLayoutParams(new LinearLayout.LayoutParams(simpleWidth, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        View simpleMouse = keyboardView.findViewById(R.id.ll_simple_mouse);
        if (simpleMouse != null) {
            LinearLayout.LayoutParams mlp = (LinearLayout.LayoutParams) simpleMouse.getLayoutParams();
            mlp.width = mouseW;
            simpleMouse.setLayoutParams(mlp);
        }


        keyboardView.setAlpha(prefConfig.oscKeyboardOpacity/100f);
        frame_layout.addView(keyboardView,params);

    }

    public int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public void sendKeyEvent(KeyEvent keyEvent) {
        if (Game.instance == null || !Game.instance.connected) {
            return;
        }
        //1-鼠标 0-按键 2-摇杆 3-十字键
        if (keyEvent.getSource() == 1) {
            Game.instance.mouseButtonEvent(keyEvent.getKeyCode(), KeyEvent.ACTION_DOWN == keyEvent.getAction());
        } else {
            Game.instance.keyboardEvent(keyEvent.getAction() == KeyEvent.ACTION_DOWN,
                    (short) keyEvent.getKeyCode());
        }
    }

    /**
     * 全键盘的按下震动：与虚拟按键、虚拟手柄共用同一组设置
     * （按键震动开关 / 仅按下震动 / 震感强度），规则保持一致。
     */
    private void notifyPressHapticOnDown() {
        if (!canVibrate()) {
            return;
        }
        //新手势，重置组合键模式的去重标记
        pressHapticFired = false;
        performPressHaptic();
    }

    private void performPressHaptic() {
        if (!canVibrate()) {
            return;
        }
        if (prefConfig.enableKeyboardVibrateOnPressOnly) {
            if (pressHapticFired) {
                return;
            }
            pressHapticFired = true;
        }
        PreferenceConfiguration.vibratePress(vibrator, prefConfig.vibratePressMs);
    }

    private void resetPressHapticGesture() {
        pressHapticFired = false;
    }

    /**
     * 独立的一次点击反馈（顶栏按钮、组合键胶囊）。
     *
     * <p>不能复用 {@link #performPressHaptic()}：那个方法带「一次手势只震一次」的去重标记，
     * 而该标记只由键盘按键的 ACTION_UP 重置。顶栏按钮和胶囊没有对应的抬手重置，
     * 第一次点击把标记置 true 后就再也清不掉，导致后续点击被去重挡掉
     * （表现为「必须先点一下键盘按键才有震动」）。</p>
     *
     * <p>这些 View 每次 ACTION_DOWN 本身就是一次独立点击，直接震即可。</p>
     */
    private void vibrateTap() {
        if (!canVibrate()) {
            return;
        }
        PreferenceConfiguration.vibratePress(vibrator, prefConfig.vibratePressMs);
    }

    private boolean canVibrate() {
        return vibrator != null && vibrator.hasVibrator() && prefConfig.enableKeyboardVibrate;
    }

    /**
     * 顶栏按钮（模式切换 / 组合键模式 / 手机软键盘 / 收起 / 菜单）的点击反馈。
     * 这些 View 不是键盘按键，没有走 ACTION_DOWN 的震动入口，单独补一次。
     * 用 OnTouchListener 而不是 setOnClickListener：前者不覆盖原有 click 监听。
     */
    private void attachTopBarHaptic(View view) {
        if (view == null) {
            return;
        }
        view.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                vibrateTap();
            }
            return false;
        });
    }

    // ---------------------------------------------------------------- 顶栏全键盘快捷键

    /**
     * 重建顶栏右侧的全键盘快捷键栏：读「全键盘快捷键」自己的数据（FullKeyboardKeyStore，
     * 与「操作 › 自定义快捷键」互不影响），末尾固定一个「+」。
     * 导出 / 导入走设置页的统一备份（AxiKeyBackup），全键盘快捷键作为其中一段。
     */
    private void rebuildComboBar() {
        if (llCombo == null) {
            return;
        }
        llCombo.removeAllViews();
        llCombo.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        List<GameMenuQuickBean> items = FullKeyboardKeyStore.load(context);
        for (GameMenuQuickBean bean : items) {
            llCombo.addView(createComboChip(bean));
        }
        llCombo.addView(createComboAddButton());
    }

    private View createComboChip(final GameMenuQuickBean bean) {
        TextView chip = new TextView(context);
        chip.setText(comboLabel(bean));
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        chip.setTextColor(0xFFE6E6E6);
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setPadding(dp(8), 0, dp(8), 0);
        chip.setBackground(comboBackground(0xFF3A3A3C, 0xFF3A3A3C));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(24));
        lp.rightMargin = dp(5);
        chip.setLayoutParams(lp);
        chip.setOnClickListener(v -> sendCombo(bean));
        chip.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                vibrateTap();
            }
            return false;
        });
        chip.setOnLongClickListener(v -> {
            showComboMenu(bean);
            return true;
        });
        return chip;
    }

    private View createComboAddButton() {
        TextView add = new TextView(context);
        add.setText("+");
        add.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        add.setTextColor(0xFFF2F2F7);
        add.setGravity(Gravity.CENTER);
        add.setBackground(comboBackground(0xFF3A3A3C, 0xFF3A3A3C));
        add.setLayoutParams(new LinearLayout.LayoutParams(dp(28), dp(24)));
        add.setOnClickListener(v -> openComboEditor(null));
        add.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                vibrateTap();
            }
            return false;
        });
        return add;
    }

    private GradientDrawable comboBackground(int fill, int stroke) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(fill);
        bg.setStroke(dp(1), stroke);
        return bg;
    }

    /** 胶囊显示用户输入的名称；名称留空时回落到组合键文本（desc），再退到键码。 */
    private String comboLabel(GameMenuQuickBean bean) {
        if (bean == null) {
            return "";
        }
        if (!TextUtils.isEmpty(bean.getName())) {
            return bean.getName();
        }
        if (!TextUtils.isEmpty(bean.getDesc())) {
            return bean.getDesc();
        }
        return bean.getCodes() == null ? "" : bean.getCodes();
    }

    /** 发送组合键：依次按下所有键，25ms 后逆序抬起（与「操作」面板一致）。 */
    private void sendCombo(GameMenuQuickBean bean) {
        if (bean == null || Game.instance == null) {
            return;
        }
        final String codes = bean.getCodes();
        if (TextUtils.isEmpty(codes)) {
            Toast.makeText(context, "该组合键没有可发送的按键", Toast.LENGTH_SHORT).show();
            return;
        }
        final String[] keys = codes.split(",");
        for (String key : keys) {
            try {
                Game.instance.keyboardEvent(true, Short.parseShort(key.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        new Handler().postDelayed(() -> {
            for (int i = keys.length - 1; i >= 0; i--) {
                try {
                    Game.instance.keyboardEvent(false, Short.parseShort(keys[i].trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }, 25);
    }

    /** 长按胶囊：编辑 / 删除。 */
    private void showComboMenu(final GameMenuQuickBean bean) {
        if (bean == null || !(context instanceof Activity)) {
            return;
        }
        new AlertDialog.Builder(context)
                .setTitle(comboLabel(bean))
                .setItems(new String[]{"编辑", "删除"}, (dialog, which) -> {
                    if (which == 0) {
                        openComboEditor(bean);
                    } else {
                        confirmDeleteCombo(bean);
                    }
                })
                .show();
    }

    private void confirmDeleteCombo(final GameMenuQuickBean bean) {
        new AlertDialog.Builder(context)
                .setTitle("删除全键盘快捷键")
                .setMessage("确定删除「" + comboLabel(bean) + "」？")
                .setPositiveButton("删除", (dialog, which) -> {
                    FullKeyboardKeyStore.remove(context, bean);
                    rebuildComboBar();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 打开全键盘快捷键编辑器：新增或编辑，保存后刷新顶栏。 */
    private void openComboEditor(GameMenuQuickBean existing) {
        if (!(context instanceof Activity)) {
            return;
        }
        final Activity activity = (Activity) context;
        GameKeyboardUpdateFragment fragment = new GameKeyboardUpdateFragment();
        int width = activity.getResources().getDisplayMetrics().widthPixels;
        if (activity.getResources().getDisplayMetrics().heightPixels > width) {
            width = (activity.getResources().getDisplayMetrics().heightPixels * 2) / 3;
        }
        fragment.setWidth(width);
        fragment.setDimAmount(0.8f);
        fragment.setTitle(existing == null ? "添加全键盘快捷键" : "编辑全键盘快捷键");
        fragment.setKeyFrom(1);
        // 全键盘快捷键只显示文字气泡，不需要图标选择；底部提示说明保存后如何编辑/删除。
        fragment.setShowIconPicker(false);
        fragment.setHintText("保存后长按气泡可以编辑和删除");
        fragment.setExisting(existing);
        fragment.setOnClick(bean -> {
            if (bean != null) {
                FullKeyboardKeyStore.save(context, bean);
                rebuildComboBar();
            }
        });
        fragment.show(activity.getFragmentManager());
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
