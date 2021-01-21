package com.reactlibrary.Impl;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.reactlibrary.OuiStepService;
import com.reactlibrary.R;

import java.util.HashSet;
import java.util.Set;

public class OuiStepServiceImpl extends OuiStepService {


	private static final String TAG = "OuiStepServiceImpl";
	private SharedPreferences mSharedPreferences;
	private static final String STEPS_PREFERENCE_KEY = "STEPS_PREFERENCE_KEY";
	private static final String STEPS_DAILY_PREFERENCE_KEY = "STEPS_DAILY_PREFERENCE_KEY";
	private static final String PROVIDER_PREFERENCE_KEY = "PROVIDER_PREFERENCE_KEY";
	private static final String ACCELEROMETRE_STEPS_PREFERENCE_KEY = "ACCELEROMETRE_STEPS_PREFERENCE_KEY";
	private static final String ZERO_STEPS_PREFERENCE_KEY = "ZERO_STEPS_PREFERENCE_KEY";

	public OuiStepServiceImpl(){

	}
	public OuiStepServiceImpl(Context context) {
		super(context);
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public int getRawSteps() {
		initSharedPreferences();
		return mSharedPreferences.getInt(STEPS_PREFERENCE_KEY, 0);
	}


	@Override
	public int getAccelerometreSteps() {
		initSharedPreferences();
		return mSharedPreferences.getInt(ACCELEROMETRE_STEPS_PREFERENCE_KEY, 0);
	}

	@Override
	public void storeAccelerometreSteps(int steps) {
		Log.d(TAG, "storeSteps called ==> Steps:" + steps);
		initSharedPreferences();
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putInt(ACCELEROMETRE_STEPS_PREFERENCE_KEY, steps);
		editor.apply();
	}

	@Override
	public void storeRawSteps(int steps) {
		Log.d(TAG, "storeSteps called ==> Steps:" + steps);
		initSharedPreferences();
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putInt(STEPS_PREFERENCE_KEY, steps);
		editor.apply();
	}


	@Override
	public void storeProvider(String provider) {
		initSharedPreferences();
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putString(PROVIDER_PREFERENCE_KEY, provider);
		editor.apply();

	}

	@Override
	public String getProvider() {
		initSharedPreferences();
		return mSharedPreferences.getString(PROVIDER_PREFERENCE_KEY, "none");


	}

	@Override
	public int getZeroSteps() {
		initSharedPreferences();
		return mSharedPreferences.getInt(ZERO_STEPS_PREFERENCE_KEY, 0);
	}

	@Override
	public void storeZeroSteps() {
		Log.d(TAG, "storeZeroSteps called ==> Steps:" + getRawSteps());
		initSharedPreferences();
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putInt(ZERO_STEPS_PREFERENCE_KEY, getRawSteps());
		editor.apply();
	}

	@Override
	public Class getNotificationLaunchClass() {
		return OuiStepService.class;
	}


	@Override
	public String getNotificationContentTitle() {
		return "OuiSpot";
	}

	@Override
	public String getNotificationContentText() {
		return "Counting your steps";
	}

	private void initSharedPreferences() {
		if (mSharedPreferences == null) {
			mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		}
	}
}
