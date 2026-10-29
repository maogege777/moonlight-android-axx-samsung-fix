/**
 * Created by Karim Mreisi.
 */

package com.limelight.binding.input.virtual_controller.keyboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Vibrator;
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
import com.limelight.ui.gamemenu.QuickKeyStore;
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

    /** 顶栏右侧的组合键条：自定义快捷键，横向滚动 + 末尾添加按钮 */
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

        rg_keyboard=keyboardView.findViewById(R.id.rg_keyboard);
        // 模式切换按钮按下即震（重复点同一个也有反馈，所以挂在触摸层而非状态变化）
        attachTopBarHaptic(keyboardView.findViewById(R.id.rbt_keyboard_1));
        attachTopBarHaptic(keyboardView.findViewById(R.id.rbt_keyboard_2));
        rg_keyboard.check(R.id.rbt_keyboard_1);

        rg_keyboard.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(R.id.rbt_keyboard_1==checkedId){
                    resetView();
                    keyboardView.findViewById(R.id.lv_keyboard).setVisibility(View.VISIBLE);
                    keyboardView.findViewById(R.id.lv_keyboard_digitpad).setVisibility(View.GONE);
                    return;
                }
                if(R.id.rbt_keyboard_2==checkedId){
                    resetView();
                    keyboardView.findViewById(R.id.lv_keyboard).setVisibility(View.GONE);
                    keyboardView.findViewById(R.id.lv_keyboard_digitpad).setVisibility(View.VISIBLE);
                    return;
                }
            }
        });

        // 两块触摸板都按 1:1 位移，和数字键盘手感一致
        bindTouchPad(keyboardView.findViewById(R.id.fv_keyboard_touch), 1f);
        bindTouchPad(keyboardView.findViewById(R.id.fv_keyboard_touch_mini), 1f);

        keyboardView.findViewById(R.id.btn_soft_phone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Game.instance.hasWindowFocus()) {
                    new Handler().postDelayed(() -> Game.instance.toggleKeyboard(),10);
                    return;
                }
                Game.instance.toggleKeyboard();
            }
        });
        attachTopBarHaptic(keyboardView.findViewById(R.id.btn_soft_phone));

        keyboardView.findViewById(R.id.mouse_up).setOnTouchListener(new View.OnTouchListener() {
            private Runnable repeater = new Runnable() {
                @Override
                public void run() {
                    Game.instance.mouseHighResScroll(true);
                    keyboardView.findViewById(R.id.mouse_up).postDelayed(this, 100);
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        vibrateTap();
                        keyboardView.findViewById(R.id.mouse_up).post(repeater);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        keyboardView.findViewById(R.id.mouse_up).removeCallbacks(repeater);
                        return true;
                }
                return false;
            }
        });

        keyboardView.findViewById(R.id.mouse_down).setOnTouchListener(new View.OnTouchListener() {
            private Runnable repeater = new Runnable() {
                @Override
                public void run() {
                    Game.instance.mouseHighResScroll(false);
                    keyboardView.findViewById(R.id.mouse_down).postDelayed(this, 100);
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        vibrateTap();
                        keyboardView.findViewById(R.id.mouse_down).post(repeater);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        keyboardView.findViewById(R.id.mouse_down).removeCallbacks(repeater);
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
        v.setBackgroundResource(R.drawable.bg_ax_keyboard_button_confirm);
    }

    private void keyUp(View v) {
        Object bg = (v == null) ? null : v.getTag(R.id.kb_key_bg);
        if (bg instanceof Drawable) {
            v.setBackground((Drawable) bg);
        } else if (v != null) {
            v.setBackgroundResource(R.drawable.bg_ax_keyboard_button);
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
        }
    }

    public void hide() {
        keyboardView.setVisibility(View.GONE);
    }

    public void show() {
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
//        DisplayMetrics screen = context.getResources().getDisplayMetrics();
//        (int)(screen.heightPixels/0.4)
        int height=prefConfig.oscKeyboardHeight;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity= Gravity.BOTTOM;
//        params.leftMargin = 20 + buttonSize;
//        params.topMargin = 15;

        LinearLayout.LayoutParams params1=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dip2px(context,height));
        keyboardView.findViewById(R.id.lv_keyboard).setLayoutParams(params1);
        keyboardView.findViewById(R.id.lv_keyboard_digitpad).setLayoutParams(params1);


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
        vibrator.vibrate(getPressHapticMs());
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
        vibrator.vibrate(getPressHapticMs());
    }

    private boolean canVibrate() {
        return vibrator != null && vibrator.hasVibrator() && prefConfig.enableKeyboardVibrate;
    }

    private int getPressHapticMs() {
        return Math.max(PreferenceConfiguration.MIN_VIBRATE_PRESS_MS,
                Math.min(PreferenceConfiguration.MAX_VIBRATE_PRESS_MS, prefConfig.vibratePressMs));
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

    // ---------------------------------------------------------------- 顶栏组合键条

    /**
     * 重建顶栏右侧的组合键条：读自定义快捷键（跳过默认组），末尾固定一个「+」。
     * 与「操作」面板、设置页的导入导出共用 QuickKeyStore，因此自动被导出/导入覆盖。
     */
    private void rebuildComboBar() {
        if (llCombo == null) {
            return;
        }
        llCombo.removeAllViews();
        llCombo.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        List<GameMenuQuickBean> items = QuickKeyStore.loadCustom(context);
        for (GameMenuQuickBean bean : items) {
            if (QuickKeyStore.isSeedItem(bean)) {
                continue;
            }
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
                .setTitle("删除组合键")
                .setMessage("确定删除「" + comboLabel(bean) + "」？")
                .setPositiveButton("删除", (dialog, which) -> {
                    QuickKeyStore.removeCustom(context, bean);
                    rebuildComboBar();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 打开现有编辑器：新增或编辑自定义组合键，保存后刷新组合键条。 */
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
        fragment.setTitle(existing == null ? "添加组合键" : "编辑组合键");
        fragment.setKeyFrom(1);
        fragment.setExisting(existing);
        fragment.setOnClick(bean -> {
            if (bean != null) {
                QuickKeyStore.saveCustom(context, bean);
                rebuildComboBar();
            }
        });
        fragment.show(activity.getFragmentManager());
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
