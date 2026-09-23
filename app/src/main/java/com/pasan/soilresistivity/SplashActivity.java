package com.pasan.soilresistivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable openApp;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        FrameLayout background = new FrameLayout(this);
        background.setBackgroundColor(Color.rgb(11, 74, 45));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.app_icon);
        icon.setAlpha(0f);
        icon.setScaleX(0.65f);
        icon.setScaleY(0.65f);
        content.addView(icon, new LinearLayout.LayoutParams(dp(128), dp(128)));

        TextView title = label("Soil Resistivity", 28, Color.WHITE);
        title.setTypeface(null, 1);
        title.setAlpha(0f);
        title.setTranslationY(dp(18));
        content.addView(title);

        TextView subtitle = label("VES Survey & Layer Analysis", 15, Color.rgb(210, 239, 222));
        subtitle.setAlpha(0f);
        content.addView(subtitle);

        background.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(background);

        icon.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(800)
                .setInterpolator(new AccelerateDecelerateInterpolator()).start();
        title.animate().alpha(1f).translationY(0f).setStartDelay(350).setDuration(600).start();
        subtitle.animate().alpha(1f).setStartDelay(650).setDuration(500).start();

        openApp = () -> background.animate().alpha(0f).setDuration(250).withEndAction(() -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            overridePendingTransition(0, 0);
        }).start();
        handler.postDelayed(openApp, 1750);
    }

    @Override
    protected void onDestroy() {
        if (openApp != null) handler.removeCallbacks(openApp);
        super.onDestroy();
    }

    private TextView label(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);
        view.setPadding(8, 8, 8, 8);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
