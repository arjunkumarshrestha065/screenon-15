package com.arjun.signage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {

            Intent launchIntent =
                    new Intent(context, MainActivity.class);

            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            try {
                context.startActivity(launchIntent);
            } catch (Exception ignored) {
                // Some Android boxes require the app to be selected
                // as the default Home application.
            }
        }
    }
}
