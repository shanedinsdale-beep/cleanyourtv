package com.gathin.hdmihome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A Home screen whose only job is to put the TV back on the HDMI input the Google TV Streamer is
 * plugged into. Whenever the TV would show its own home screen (power on, Home on the TV's remote,
 * Back out of the HDMI picture), this shows "Switching to Google TV" for two seconds and opens that
 * input. If the input reports nothing connected, it waits and switches the moment the Streamer turns
 * on. Back or Menu during the countdown opens a small menu, so the TV is never locked up.
 */
public class MainActivity extends Activity {
    private static final long COUNTDOWN_MS = 2000;
    /** Back on the screen this soon after a switch means the TV's player closed at once: stop and show the menu. */
    private static final long BOUNCE_MS = 4000;
    private static final Pattern PORT_ONE = Pattern.compile("(?i)hdmi\\s*1(?!\\d)");
    private static final String PREFS = "hdmihome", KEY_TARGET = "target_input";

    private enum State { COUNTDOWN, WAITING, MENU }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable switchNow = this::switchToTarget;
    private TvInputManager tim;
    private SharedPreferences prefs;
    private TextView title, hint;
    private LinearLayout buttons;
    private State state = State.COUNTDOWN;
    /** True while the person is using the menu (or a screen opened from it): coming back shows the menu again. */
    private boolean inMenu;
    private long lastSwitchAt;
    private float density;

    private final TvInputManager.TvInputCallback inputCallback = new TvInputManager.TvInputCallback() {
        @Override
        public void onInputStateChanged(String inputId, int newState) {
            TvInputInfo target = target();
            if (state == State.WAITING && target != null && target.getId().equals(inputId)
                    && newState != TvInputManager.INPUT_STATE_DISCONNECTED) {
                switchToTarget();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
        tim = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        title = text(34, Color.WHITE);
        hint = text(18, 0xFFAAAAAA);
        hint.setPadding(0, dp(10), 0, dp(28));
        buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.VERTICAL);
        column.addView(title);
        column.addView(hint);
        column.addView(buttons, new LinearLayout.LayoutParams(dp(420), LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(column, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        setContentView(root);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home pressed (from anywhere, including a screen opened from the menu): back to Google TV.
        inMenu = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tim != null) tim.registerCallback(inputCallback, handler);
        if (inMenu || SystemClock.elapsedRealtime() - lastSwitchAt < BOUNCE_MS) {
            showMenu();
        } else {
            startAutoSwitch();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(switchNow);
        if (tim != null) tim.unregisterCallback(inputCallback);
    }

    private void startAutoSwitch() {
        TvInputInfo target = target();
        if (target == null) {
            showMenu();
            return;
        }
        String label = label(target);
        buttons.removeAllViews();
        if (tim.getInputState(target.getId()) == TvInputManager.INPUT_STATE_DISCONNECTED) {
            state = State.WAITING;
            title.setText(R.string.waiting);
            hint.setText(getString(R.string.waiting_hint, label));
            return;
        }
        state = State.COUNTDOWN;
        title.setText(R.string.switching);
        hint.setText(getString(R.string.switching_hint, label));
        handler.removeCallbacks(switchNow);
        handler.postDelayed(switchNow, COUNTDOWN_MS);
    }

    private void switchToTarget() {
        handler.removeCallbacks(switchNow);
        TvInputInfo target = target();
        if (target == null) {
            showMenu();
            return;
        }
        inMenu = false;
        lastSwitchAt = SystemClock.elapsedRealtime();
        android.net.Uri uri = TvContract.buildChannelUriForPassthroughInput(target.getId());
        Intent typed = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "vnd.android.cursor.item/channel");
        Intent plain = new Intent(Intent.ACTION_VIEW, uri);
        if (!start(typed) && !start(plain)) {
            lastSwitchAt = 0;
            Toast.makeText(this, R.string.cant_open, Toast.LENGTH_LONG).show();
            showMenu();
        }
    }

    private void showMenu() {
        handler.removeCallbacks(switchNow);
        state = State.MENU;
        inMenu = true;
        TvInputInfo target = target();
        title.setText(R.string.menu_title);
        buttons.removeAllViews();
        if (target == null) {
            hint.setText(R.string.no_inputs);
        } else {
            String label = label(target);
            hint.setText(getString(R.string.menu_hint, label));
            addButton(getString(R.string.go_target, label), this::switchToTarget);
            addButton(getString(R.string.change_target), this::showPicker);
        }
        addButton(getString(R.string.stock_home), this::openStockHome);
        addButton(getString(R.string.settings), () -> start(new Intent(Settings.ACTION_SETTINGS)));
        buttons.getChildAt(0).requestFocus();
    }

    private void showPicker() {
        state = State.MENU;
        title.setText(R.string.pick_title);
        hint.setText("");
        buttons.removeAllViews();
        for (TvInputInfo info : hdmiInputs()) {
            final String id = info.getId();
            String row = label(info);
            if (tim.getInputState(id) == TvInputManager.INPUT_STATE_DISCONNECTED) row += "  (nothing connected)";
            addButton(row, () -> {
                prefs.edit().putString(KEY_TARGET, id).apply();
                switchToTarget();
            });
        }
        if (buttons.getChildCount() == 0) hint.setText(R.string.no_inputs);
        else buttons.getChildAt(0).requestFocus();
    }

    /** The TV's own launcher: any other Home app, preferring a real launcher over Settings' fallback. */
    private void openStockHome() {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo best = null;
        for (ResolveInfo ri : getPackageManager().queryIntentActivities(home, 0)) {
            if (ri.activityInfo.packageName.equals(getPackageName())) continue;
            if (best == null || ri.priority > best.priority) best = ri;
        }
        if (best == null) {
            Toast.makeText(this, R.string.cant_open, Toast.LENGTH_LONG).show();
            return;
        }
        start(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .setComponent(new ComponentName(best.activityInfo.packageName, best.activityInfo.name)));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event);
        int key = event.getKeyCode();
        boolean ok = key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER
                || key == KeyEvent.KEYCODE_NUMPAD_ENTER;
        if (state != State.MENU) {
            // OK goes now (also when waiting, in case the TV misreports the input as empty); anything
            // else stops the countdown and opens the menu.
            if (ok) switchToTarget();
            else showMenu();
            return true;
        }
        if (key == KeyEvent.KEYCODE_BACK) {
            inMenu = false;
            startAutoSwitch();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    /** The saved input, else the HDMI input labelled port 1, else the first HDMI input. */
    private TvInputInfo target() {
        List<TvInputInfo> hdmi = hdmiInputs();
        if (hdmi.isEmpty()) return null;
        String saved = prefs.getString(KEY_TARGET, null);
        for (TvInputInfo info : hdmi) if (info.getId().equals(saved)) return info;
        for (TvInputInfo info : hdmi) if (PORT_ONE.matcher(String.valueOf(info.loadLabel(this))).find()) return info;
        return hdmi.get(0);
    }

    /** HDMI ports themselves, not the CEC devices the TV lists under them, sorted by name. */
    private List<TvInputInfo> hdmiInputs() {
        List<TvInputInfo> out = new ArrayList<>();
        if (tim == null) return out;
        for (TvInputInfo info : tim.getTvInputList()) {
            if (info.getType() == TvInputInfo.TYPE_HDMI && info.getParentId() == null) out.add(info);
        }
        Collections.sort(out, (a, b) -> String.valueOf(a.loadLabel(this))
                .compareToIgnoreCase(String.valueOf(b.loadLabel(this))));
        return out;
    }

    private String label(TvInputInfo info) {
        String label = String.valueOf(info.loadLabel(this));
        CharSequence custom = info.loadCustomLabel(this);
        if (custom != null && custom.length() > 0 && !custom.toString().equals(label)) label = custom + " (" + label + ")";
        return label;
    }

    private boolean start(Intent intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    private void addButton(String text, Runnable action) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        b.setPadding(dp(24), 0, dp(24), 0);
        b.setTextColor(new ColorStateList(
                new int[][]{{android.R.attr.state_focused}, {}}, new int[]{Color.BLACK, Color.WHITE}));
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, pill(Color.WHITE));
        bg.addState(new int[]{}, pill(0x26FFFFFF));
        b.setBackground(bg);
        b.setStateListAnimator(null);
        b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        lp.bottomMargin = dp(12);
        buttons.addView(b, lp);
    }

    private GradientDrawable pill(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(28));
        return d;
    }

    private TextView text(int sp, int color) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private int dp(int v) {
        return Math.round(v * density);
    }
}
