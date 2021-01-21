package com.reactlibrary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

public abstract class OuiStepReceiver<T extends OuiStepService> extends BroadcastReceiver {
	private static final String TAG = "OuiStepReceiver";


	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction() == Intent.ACTION_BOOT_COMPLETED) {
			Log.d(TAG, "Starting Receiver");
			Intent updateService = new Intent(context, getServiceClass());
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				context.startForegroundService(updateService);
				return;
			}
			context.startService(new Intent(updateService));
		}
	}

	public abstract Class<T> getServiceClass();
}
