package com.limelight.ui.gamemenu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/**
 * 虚拟手柄 / 虚拟按键 按钮右上角的「布局方案」角标。
 *
 * <p>视觉：实心圆角矩形底 + 实心数字，数字 1~5 对应方案 1~5；
 * 鼠标模式 5~8 用琥珀实心底 + 深色字区分。</p>
 *
 * <p>自绘保证 15dp 小尺寸下的居中与清晰度；文字一律使用填充（FILL）而非描边，
 * 避免小字号描边产生的发虚/重影。</p>
 *
 * <p>触摸说明：本控件不可点击、不可获得焦点，落在它范围内的触摸会继续下发给下层的 Button，
 * 因此角标不会在按钮右上角形成点击死角。</p>
 */
public class SchemeBadgeView extends View {

    /** 圆角半径（dp） */
    private static final float CORNER_RADIUS_DP = 3f;
    /** 字号相对控件高度的比例 */
    private static final float TEXT_SIZE_RATIO = 0.76f;
    /** 两个字符（鼠标模式简称）时的字号比例，避免撑出胶囊 */
    private static final float MULTI_TEXT_SIZE_RATIO = 0.58f;

    /** 未启用时的实心底色（近黑，压在深灰按钮上也清晰） */
    private static final int COLOR_BG_IDLE = 0xE6171719;
    /** 启用时的实心底色（深绿，与亮绿按钮形成层次而不糊成一片） */
    private static final int COLOR_BG_ACTIVE = 0xE6143A02;
    /** 鼠标变体模式（5~8）的琥珀底色 */
    private static final int COLOR_VARIANT = 0xFFE0A33A;
    /** 琥珀底上的深色字 */
    private static final int COLOR_VARIANT_TEXT = 0xFF241A08;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private String text = "1";
    private boolean active = true;
    /** 鼠标 5~8：琥珀实心底 + 深色实心字 */
    private boolean variant = false;

    public SchemeBadgeView(Context context) {
        this(context, null);
    }

    public SchemeBadgeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SchemeBadgeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        fillPaint.setStyle(Paint.Style.FILL);

        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);

        setClickable(false);
        setFocusable(false);

        applyColors();
    }

    private void applyColors() {
        fillPaint.setColor(variant ? COLOR_VARIANT : (active ? COLOR_BG_ACTIVE : COLOR_BG_IDLE));
    }

    /** 设置显示的数字（1~5），对应方案序号。 */
    public void setSchemeNumber(int number) {
        setBadgeText(String.valueOf(number));
    }

    /** 设置角标文字：布局方案用数字，鼠标模式用两个字简称。 */
    public void setBadgeText(String value) {
        String text = value == null ? "" : value;
        if (!text.equals(this.text)) {
            this.text = text;
            invalidate();
        }
    }

    /** 跟随对应虚拟手柄 / 虚拟按键的启用状态切换底色。 */
    public void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;
            applyColors();
            invalidate();
        }
    }

    /** 鼠标 5~8 的变体样式：琥珀实心底 + 深色实心字。 */
    public void setVariant(boolean variant) {
        if (this.variant != variant) {
            this.variant = variant;
            applyColors();
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final float w = getWidth();
        final float h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        final float radius = dp(CORNER_RADIUS_DP);
        rect.set(0, 0, w, h);
        canvas.drawRoundRect(rect, radius, radius, fillPaint);
        // 实心纯色文字：小尺寸下描边字容易发虚、看起来像重影
        textPaint.setColor(variant ? COLOR_VARIANT_TEXT : Color.WHITE);

        final boolean multiChar = text != null && text.length() > 1;
        textPaint.setTextSize(h * (multiChar ? MULTI_TEXT_SIZE_RATIO : TEXT_SIZE_RATIO));

        // 以控件高度垂直居中（补偿字体 ascent/descent 造成的上下不对称）
        final Paint.FontMetrics fm = textPaint.getFontMetrics();
        final float baseline = h / 2f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(text, w / 2f, baseline, textPaint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}