package com.reactlibrary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.reactlibrary.Impl.OuiStepServiceImpl;


public abstract class BootCompletedReceiver extends BroadcastReceiver {

	private static final String TAG = "BootCompletedReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {

		Log.d(TAG, "onReceive, action : " + intent.getAction());
		//Check if we need to start on boot completed
		if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {

			context.startService(new Intent(context, OuiStepServiceImpl.class));
			//getSensorManagerImpl().startStepSensorService(context, intent);
			return;
		}

	}

}
