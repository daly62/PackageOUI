package com.reactlibrary;


import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
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



import com.google.gson.Gson;
import com.reactlibrary.Database.Step;
import com.reactlibrary.Database.StepRepository;
import com.reactlibrary.Impl.OuiStepServiceImpl;



import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

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
	public void getNumberDailyByDates(String strDate, String endDate,Promise promise) {
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
				promise.resolve(params);

				//double numberOfSteps =   steps.get(0).getValue() -steps.get(steps.size()-1).getValue();
				//successCallback.invoke(numberOfSteps);

			});
		});


	}


	@RequiresApi(api = Build.VERSION_CODES.N)
	@ReactMethod
	public void getListDailyByDates(String strDate, String endDate, Promise promise) {
		LiveData<List<Step>> list = stepRepository.fetchAllStepsByDates(Long.parseLong(strDate), Long.parseLong(endDate));
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		Date date1 = new Date(Long.parseLong(strDate));
		Date date2 = new Date(Long.parseLong(endDate));

		date1.setHours(0);
		date1.setMinutes(0);
		date1.setSeconds(0);

		date2.setHours(0);
		date2.setMinutes(0);
		date2.setSeconds(0);


		List<Date> datesInRange = getDatesBetween(date1, date2);


		new Handler(Looper.getMainLooper()).post(() -> {
			WritableArray data = Arguments.createArray();
			list.observeForever(steps -> {
				if (!reactContext.hasActiveCatalystInstance() || steps == null) {
					promise.resolve(null);
					return;
				}
				//if (steps.size() > 0) {

				for (Date date : datesInRange) {

					double stepNumber = getNumberStep(date,steps);

					WritableMap dateMap = Arguments.createMap();
					dateMap.putString("date", convertDate(date));
					dateMap.putDouble("value", stepNumber);

					data.pushMap(dateMap);

				}
				promise.resolve(data);

			});
		});


	}

	@ReactMethod
	public void getListByDates(String strDate, String endDate, Promise promise) {
		LiveData<List<Step>> list = stepRepository.fetchAllStepsByDates(Long.parseLong(strDate), Long.parseLong(endDate));

		new Handler(Looper.getMainLooper()).post(() -> {
			WritableArray data = Arguments.createArray();
			list.observeForever(steps -> {
				if (!reactContext.hasActiveCatalystInstance() || steps == null) {
					promise.resolve(null);
					return;
				}

				if (steps.size() > 0) {

					for (Step step : steps) {

						WritableMap stepMap = Arguments.createMap();
						stepMap.putDouble("value", step.getValue());
						stepMap.putDouble("timeStamp", step.getCreatedAt());

						data.pushMap(stepMap);
					}
				}
				promise.resolve(data);
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

	public static List<Date> getDatesBetween(
			Date startDate, Date endDate) {

		List<Date> datesInRange = new ArrayList<>();
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(startDate);

		Calendar endCalendar = new GregorianCalendar();
		endCalendar.setTime(endDate);

		while (calendar.before(endCalendar)) {
			Date result = calendar.getTime();
			datesInRange.add(result);
			calendar.add(Calendar.DATE, 1);
		}
		return datesInRange;
	}

	public static String convertDate(Date date)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		sdf.setTimeZone(TimeZone.getDefault());
		String text = sdf.format(date);
		return text;
	}




	@RequiresApi(api = Build.VERSION_CODES.N)
	public static double getNumberStep(
			Date day, List<Step> steps) {
		double numberSteps = 0;
		List<Step> mFinalList = new ArrayList<>();

		for (Step step : steps) {
			Long time = step.getCreatedAt();
			Long dayTime = day.getTime();
			if (time > dayTime && time < dayTime + 60 * 60 * 1000 * 24) {
				mFinalList.add(step);
			}
		}
		if (mFinalList.size() > 0) {
			mFinalList.sort(new Comparator<Step>() {
				@Override
				public int compare(Step o1, Step o2) {
					return (int) (o1.getValue() - o2.getValue());
				}
			});

			numberSteps = Math.abs(mFinalList.get(0).getValue() - mFinalList.get(mFinalList.size() - 1).getValue());

		}
		return numberSteps;
	}



}
