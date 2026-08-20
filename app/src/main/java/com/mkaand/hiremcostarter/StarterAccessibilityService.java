package com.mkaand.hiremcostarter;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;

/**
 * Hiremco Starter v3.0 runtime logic.
 *
 * Architecture (unchanged from the original design):
 * - No manifest BOOT_COMPLETED receiver. Android re-binds enabled Accessibility
 *   services after every reboot; we detect "this is a new boot" by comparing the
 *   platform's Settings.Global.BOOT_COUNT against the last value we saw, so the
 *   boot-launch logic runs exactly once per reboot.
 * - A dynamically registered SCREEN_ON receiver drives the wake-launch path.
 * - Accessibility window-state events never trigger a launch by themselves; they
 *   only record which package is currently in the foreground.
 * - Master switch + independent boot/wake switches + independent delays, all
 *   read from SharedPreferences so MainActivity's settings screen controls them.
 */
public class StarterAccessibilityService extends AccessibilityService {

    private static final String PREFS = "prefs";
    private static final String DEFAULT_TARGET_PACKAGE = "com.superdtv";
    private static final String PREF_MASTER_ENABLED = "master_enabled";
    private static final String PREF_BOOT_ENABLED = "boot_enabled";
    private static final String PREF_WAKE_ENABLED = "wake_enabled";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean launchScheduled;
    private boolean receiverRegistered;
    private String lastPackage;
    private ScreenReceiver screenReceiver;

    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 50;
        setServiceInfo(info);

        if (!receiverRegistered) {
            screenReceiver = new ScreenReceiver(this);
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(screenReceiver, filter);
            }
            receiverRegistered = true;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int bootCount = Settings.Global.getInt(getContentResolver(), Settings.Global.BOOT_COUNT, -1);
        int previous = prefs.getInt("last_boot_count", -2);
        if (bootCount >= 0 && bootCount != previous) {
            // Commit synchronously before scheduling so a service restart cannot
            // interpret this same device boot as a new one.
            prefs.edit().putInt("last_boot_count", bootCount).commit();
            handleBoot();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event != null && event.getPackageName() != null) {
            lastPackage = String.valueOf(event.getPackageName());
        }
    }

    @Override
    public void onInterrupt() {
        // Required override; nothing to clean up.
    }

    @Override
    public boolean onUnbind(Intent intent) {
        cleanUp();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        cleanUp();
        super.onDestroy();
    }

    void handleBoot() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean(PREF_MASTER_ENABLED, true)) return;
        if (!prefs.getBoolean(PREF_BOOT_ENABLED, true)) return;

        int delaySeconds = prefs.getInt("boot_delay_seconds", 30);
        scheduleLaunch(delaySeconds, PREF_BOOT_ENABLED);
    }

    void handleWake() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean(PREF_MASTER_ENABLED, true)) return;
        if (!prefs.getBoolean(PREF_WAKE_ENABLED, true)) return;

        String target = prefs.getString("target_package", DEFAULT_TARGET_PACKAGE);
        if (!TextUtils.isEmpty(target) && target.equals(lastPackage)) {
            // Hiremco is already in the foreground; skip the launch.
            return;
        }

        int delaySeconds = prefs.getInt("wake_delay_seconds", 5);
        scheduleLaunch(delaySeconds, PREF_WAKE_ENABLED);
    }

    private void scheduleLaunch(int delaySeconds, String triggerPreference) {
        if (launchScheduled) return;
        launchScheduled = true;
        handler.postDelayed(() -> {
            launchScheduled = false;
            launchTarget(triggerPreference);
        }, delaySeconds * 1000L);
    }

    private void launchTarget(String triggerPreference) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        // Re-check at execution time so switching automation off also cancels
        // the effect of a launch that was already waiting through its delay.
        if (!prefs.getBoolean(PREF_MASTER_ENABLED, true)) return;
        if (!prefs.getBoolean(triggerPreference, true)) return;

        String target = prefs.getString("target_package", DEFAULT_TARGET_PACKAGE);
        if (TextUtils.isEmpty(target)) return;

        if (PREF_WAKE_ENABLED.equals(triggerPreference) && target.equals(lastPackage)) return;

        PackageManager pm = getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage(target);
        if (launch == null) return;
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(launch);
    }

    private void cleanUp() {
        handler.removeCallbacksAndMessages(null);
        launchScheduled = false;
        if (receiverRegistered && screenReceiver != null) {
            unregisterReceiver(screenReceiver);
            receiverRegistered = false;
        }
    }
}
