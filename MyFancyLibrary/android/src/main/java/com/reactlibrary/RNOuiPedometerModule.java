package com.reactlibrary;


import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.OnBatchCompleteListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;


import com.google.gson.Gson;
import com.reactlibrary.Database.Step;
import com.reactlibrary.Database.StepRepository;
import com.reactlibrary.Impl.OuiStepServiceImpl;

import org.json.JSONArray;

import java.util.List;

public class RNOuiPedometerModule extends ReactContextBaseJavaModule {

	private OuiStepService mSensorService;
	private StepRepository stepRepository;


	public static final String REACT_CLASS = "RNOuiPedometer";
	private static ReactApplicationContext reactContext;

	public RNOuiPedometerModule(ReactApplicationContext reactContext) {
		super(reactContext);
		this.reactContext = reactContext;
		mSensorService = new OuiStepServiceImpl(reactContext);
		stepRepository = new StepRepository(reactContext);

	}

	@Override
	public String getName() {
		return REACT_CLASS;
	}

	@ReactMethod
	public void isAvailable(Promise promise, ReactApplicationContext reactContext) {
		promise.resolve(mSensorService.isStepCounterFeatureAvailable(reactContext.getPackageManager()));
	}


	@ReactMethod
	public void getProvider(Callback successCallback) {
		successCallback.invoke(mSensorService.getProvider());
		//successCallback.invoke(mSensorService.getProvider());
	}

	@ReactMethod
	public void authorizationStatus(Promise promise) {
		promise.resolve("authorized");
	}

	@ReactMethod
	public void getNumberOfSteps(Callback successCallback) {
		successCallback.invoke(mSensorService.getSteps());
	}

	@ReactMethod
	public void getNumberDailyByDates(String strDate, String endDate) {
		LiveData<List<Step>> list = stepRepository.fetchAllStepsByDates(Long.parseLong(strDate), Long.parseLong(endDate));

		new Handler(Looper.getMainLooper()).post(() -> {
			list.observeForever(steps -> {
				double numberOfSteps = 0;
				if (!reactContext.hasActiveCatalystInstance() || steps == null) {
					return;
				}
				if (steps.size() > 0)
					numberOfSteps = steps.get(0).getValue() - steps.get(steps.size() - 1).getValue();


				String jsonData = new Gson().toJson(numberOfSteps);
				WritableMap params = Arguments.createMap();
				// pass data instead of jsonData if data is a String
				params.putString("data", jsonData);
				reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
						.emit("onDataUpdate", params);

				//double numberOfSteps =   steps.get(0).getValue() -steps.get(steps.size()-1).getValue();
				//successCallback.invoke(numberOfSteps);

			});
		});


	}

	@ReactMethod
	public void startService() {
		//OuiStepServiceManager.startAutoUpdate(reactContext);
		this.reactContext.startService(new Intent(this.reactContext, OuiStepServiceImpl.class));
		//mSensorManager.startAutoUpdate(reactContext);
	}

	@ReactMethod
	public void stopService() {
		//OuiStepServiceManager.stopAutoUpdate(reactContext);
		this.reactContext.stopService(new Intent(this.reactContext, OuiStepServiceImpl.class));
		//	mSensorManager.stopAutoUpdate(reactContext);
	}


}
