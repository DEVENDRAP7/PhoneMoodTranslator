package com.devendrap7.phonemoodtranslator.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.devendrap7.phonemoodtranslator.workers.UsageWorker;
import java.util.concurrent.TimeUnit;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Reschedule WorkManager after device boot
            PeriodicWorkRequest usageWorkRequest =
                    new PeriodicWorkRequest.Builder(
                            UsageWorker.class,
                            15,
                            TimeUnit.MINUTES)
                            .build();

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "MoodUsageMonitor",
                    ExistingPeriodicWorkPolicy.KEEP,
                    usageWorkRequest
            );
        }
    }
}