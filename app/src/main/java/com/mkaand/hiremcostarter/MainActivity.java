package com.mkaand.hiremcostarter;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String PREFS = "prefs";
    private static final String DEFAULT_TARGET_PACKAGE = "com.superdtv";
    private static final int[] BOOT_DELAYS = {5, 10, 15, 20, 30, 45, 60};
    private static final int[] WAKE_DELAYS = {2, 5, 10, 15, 20, 30};

    private SharedPreferences prefs;
    private Switch masterSwitch;
    private Switch bootSwitch;
    private Switch wakeSwitch;
    private Button bootDelayButton;
    private Button wakeDelayButton;
    private EditText packageEdit;
    private TextView statusText;

    private int bootDelayIndex;
    private int wakeDelayIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        masterSwitch = findViewById(R.id.master_switch);
        bootSwitch = findViewById(R.id.boot_switch);
        wakeSwitch = findViewById(R.id.wake_switch);
        bootDelayButton = findViewById(R.id.boot_delay_button);
        wakeDelayButton = findViewById(R.id.wake_delay_button);
        packageEdit = findViewById(R.id.package_edit);
        statusText = findViewById(R.id.status_text);

        masterSwitch.setChecked(prefs.getBoolean("master_enabled", true));
        bootSwitch.setChecked(prefs.getBoolean("boot_enabled", true));
        wakeSwitch.setChecked(prefs.getBoolean("wake_enabled", true));
        packageEdit.setText(prefs.getString("target_package", DEFAULT_TARGET_PACKAGE));

        bootDelayIndex = indexOf(BOOT_DELAYS, prefs.getInt("boot_delay_seconds", 30));
        wakeDelayIndex = indexOf(WAKE_DELAYS, prefs.getInt("wake_delay_seconds", 5));
        updateDelayLabels();

        masterSwitch.setOnCheckedChangeListener((b, checked) ->
                prefs.edit().putBoolean("master_enabled", checked).apply());
        bootSwitch.setOnCheckedChangeListener((b, checked) ->
                prefs.edit().putBoolean("boot_enabled", checked).apply());
        wakeSwitch.setOnCheckedChangeListener((b, checked) ->
                prefs.edit().putBoolean("wake_enabled", checked).apply());

        bootDelayButton.setOnClickListener(v -> {
            bootDelayIndex = (bootDelayIndex + 1) % BOOT_DELAYS.length;
            prefs.edit().putInt("boot_delay_seconds", BOOT_DELAYS[bootDelayIndex]).apply();
            updateDelayLabels();
        });
        wakeDelayButton.setOnClickListener(v -> {
            wakeDelayIndex = (wakeDelayIndex + 1) % WAKE_DELAYS.length;
            prefs.edit().putInt("wake_delay_seconds", WAKE_DELAYS[wakeDelayIndex]).apply();
            updateDelayLabels();
        });

        findViewById(R.id.detect_button).setOnClickListener(v -> showDetectDialog());

        findViewById(R.id.accessibility_button).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        findViewById(R.id.overlay_button).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });

        findViewById(R.id.save_button).setOnClickListener(v -> {
            String pkg = packageEdit.getText().toString().trim();
            prefs.edit().putString("target_package", pkg).apply();
            Toast.makeText(this, R.string.saved_toast, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        statusText.setText(isAccessibilityServiceEnabled()
                ? R.string.accessibility_enabled
                : R.string.accessibility_disabled);
    }

    private int indexOf(int[] arr, int value) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == value) return i;
        }
        return 0;
    }

    private void updateDelayLabels() {
        bootDelayButton.setText(getString(R.string.boot_delay_format, BOOT_DELAYS[bootDelayIndex]));
        wakeDelayButton.setText(getString(R.string.wake_delay_format, WAKE_DELAYS[wakeDelayIndex]));
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        if (am == null) return false;
        List<AccessibilityServiceInfo> enabled =
                am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        String target = getPackageName() + "/" + StarterAccessibilityService.class.getName();
        for (AccessibilityServiceInfo info : enabled) {
            if (target.equals(info.getId())) return true;
        }
        return false;
    }

    private void showDetectDialog() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<String> matches = new ArrayList<>();
        for (ApplicationInfo info : apps) {
            String pkg = info.packageName;
            CharSequence label = info.loadLabel(pm);
            boolean nameMatch = pkg.toLowerCase().contains("hiremco")
                    || (label != null && label.toString().toLowerCase().contains("hiremco"));
            if (nameMatch && pm.getLaunchIntentForPackage(pkg) != null) {
                matches.add(pkg);
            }
        }
        if (matches.isEmpty()) {
            Toast.makeText(this, R.string.detect_none_found, Toast.LENGTH_LONG).show();
            return;
        }
        String[] items = matches.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle(R.string.detect_dialog_title)
                .setItems(items, (dialog, which) -> packageEdit.setText(items[which]))
                .show();
    }
}
