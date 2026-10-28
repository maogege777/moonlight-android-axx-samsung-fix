package com.limelight.ui.gamemenu;

import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.R;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.BaseFragmentDialog.BaseGameMenuDialog;

/**
 * 震感强度面板：预设 10/15/20/25/30ms，也允许输入 8~100 的自定义值。
 * 结构与“码率”面板保持一致。
 */
public class GameVibrationStrengthFragment extends BaseGameMenuDialog implements View.OnClickListener {

    private ImageButton ibtn_back;
    private EditText edt_press_ms;

    private int value = 10;

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_game_menu_vibration_strength;
    }

    @Override
    public void bindView(View v) {
        super.bindView(v);
        ibtn_back = v.findViewById(R.id.ibtn_back);
        edt_press_ms = v.findViewById(R.id.edt_press_ms);

        edt_press_ms.setText(String.valueOf(value));
        edt_press_ms.setSelection(edt_press_ms.getText().length());

        ibtn_back.setOnClickListener(this);
        v.findViewById(R.id.btn_right).setOnClickListener(this);

        // 常用档位数量与 dialog_game_menu_vibration_strength.xml 中的 tag 0..4 保持一致
        for (int i = 0; i < 5; i++) {
            TextView textView = v.findViewWithTag("" + i);
            if (textView == null) {
                continue;
            }
            textView.setOnClickListener(v1 -> {
                String txt = textView.getText().toString().trim().replace("ms", "");
                try {
                    commit(Integer.parseInt(txt));
                } catch (NumberFormatException ignored) {
                }
            });
        }
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ibtn_back) {
            dismiss();
            return;
        }

        if (v.getId() == R.id.btn_right) {
            String text = edt_press_ms.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(getActivity(), "震动时长不能为空！", Toast.LENGTH_SHORT).show();
                return;
            }
            int ms;
            try {
                ms = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), "请输入 8~100 的整数", Toast.LENGTH_SHORT).show();
                return;
            }
            commit(ms);
        }
    }

    private void commit(int ms) {
        if (ms < PreferenceConfiguration.MIN_VIBRATE_PRESS_MS
                || ms > PreferenceConfiguration.MAX_VIBRATE_PRESS_MS) {
            Toast.makeText(getActivity(),
                    "震动时长需在 " + PreferenceConfiguration.MIN_VIBRATE_PRESS_MS
                            + "~" + PreferenceConfiguration.MAX_VIBRATE_PRESS_MS + "ms 之间",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        dismiss();
        if (onClick != null) {
            onClick.click(ms);
        }
    }

    private onClick onClick;

    public interface onClick {
        void click(int ms);
    }

    public void setOnClick(onClick onClick) {
        this.onClick = onClick;
    }
}