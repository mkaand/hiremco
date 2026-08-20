package com.mkaand.hiremcostarter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Fires on ACTION_SCREEN_ON; wake-launch logic lives in the service. */
public class ScreenReceiver extends BroadcastReceiver {

    private final StarterAccessibilityService service;

    public ScreenReceiver(StarterAccessibilityService service) {
        this.service = service;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
            service.handleWake();
        }
    }
}
