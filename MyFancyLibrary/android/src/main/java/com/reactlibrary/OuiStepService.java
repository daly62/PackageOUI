package com.reactlibrary;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;


import com.reactlibrary.Database.Step;
import com.reactlibrary.Database.StepRepository;

import java.util.Date;
import java.util.List;
import java.util.Set;

public abstract class OuiStepService extends Service implements SensorEventListener {

	private static final String TAG = "OuiStepService";
	private static final int SCREEN_OFF_RECEIVER_DELAY = 5000;

	private static final int SERVICE_ID = 123;

	private static final String CHANNEL_ID = "1234";

	protected Context mContext;

	private PowerManager.WakeLock mWakeLock;

	private static OuiStepService mInstance;

	public static OuiStepService getInstance() {
		return mInstance;
	}

	private Sensor senAccelerometer;
	private Sensor senStepCounter;

	private double MagnitudePrevious = 0;
	private Integer stepCount = 0;

	private StepRepository stepRepository;

	private static final int ACCEL_RING_SIZE = 50;
	private static final int VEL_RING_SIZE = 10;

	// change this threshold according to your sensitivity preferences
	private static final float STEP_THRESHOLD = 60f;

	private static final int STEP_DELAY_NS = 250000000;

	private int accelRingCounter = 0;
	private float[] accelRingX = new float[ACCEL_RING_SIZE];
	private float[] accelRingY = new float[ACCEL_RING_SIZE];
	private float[] accelRingZ = new float[ACCEL_RING_SIZE];
	private int velRingCounter = 0;
	private float[] velRing = new float[VEL_RING_SIZE];
	private long lastStepTimeNs = 0;
	private float oldVelocityEstimate = 0;


	public OuiStepService() {
	}

	public OuiStepService(Context context) {
		this.mContext = context;
	}

	BroadcastReceiver mScreenOffBroadcastReceiver = new BroadcastReceiver() {

		//When Event is published, onReceive method is called
		@Override
		public void onReceive(final Context context, Intent intent) {
			Log.i(TAG, "onReceive(" + intent + ")");
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF) || intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				Log.d(TAG, "SensorStepScreenOffReceiver triggered, registering StepSensor again");
				restartListener();
				stepRepository = new StepRepository(getApplicationContext());

				Runnable runnable = new Runnable() {
					public void run() {
						Log.d(TAG, "SensorStepScreenOffReceiver Runnable executes");
						restartListener();
						stepRepository = new StepRepository(getApplicationContext());
					}
				};

				new Handler().postDelayed(runnable, SCREEN_OFF_RECEIVER_DELAY);
			}
		}
	};

	@SuppressLint("InvalidWakeLockTag")
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "SensorStepService onCreate");

		stepRepository = new StepRepository(getApplicationContext());

		PowerManager manager =
				(PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		mWakeLock.acquire();

		registerReceiver(mScreenOffBroadcastReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
		registerReceiver(mScreenOffBroadcastReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
	}

	private String createNotificationChannel(String channelId, String channelName) {
		// Create the NotificationChannel, but only on API 26+ because
		// the NotificationChannel class is new and not in the support library
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			int importance = NotificationManager.IMPORTANCE_NONE;
			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName, importance);
			// Register the channel with the system; you can't change the importance
			// or other notification behaviors after this
			NotificationManager notificationManager = getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(channel);

			return channelId;
		} else
			return "";
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "SensorStepService onStartCommand service");

		this.mContext = getApplicationContext();
		mInstance = this;

		//Start as a foreground service to keep running
		if (mContext != null) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				startMyOwnForeground();
			else
				startForeground(SERVICE_ID, getNotification(mContext));
			Log.d(TAG, "SensorStepService startForeground service");
		}

		registerSensorStep();


		if (mWakeLock != null) {
			mWakeLock.acquire();
			Log.d(TAG, "Acquired Partial WakeLock service");
		}

		return Service.START_STICKY;
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	private void startMyOwnForeground() {
		String NOTIFICATION_CHANNEL_ID = "com.testpedo";
		String channelName = "My Background Service";
		NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
		chan.setLightColor(Color.BLUE);
		chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		assert manager != null;
		manager.createNotificationChannel(chan);

		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
		Notification notification = notificationBuilder.setOngoing(true)
				.setContentTitle("App is running in background")
				.setPriority(NotificationManager.IMPORTANCE_MIN)
				.setCategory(Notification.CATEGORY_SERVICE)
				.build();
		startForeground(124, notification);
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "SensorStepService onDestroy");
		unregisterReceiver(mScreenOffBroadcastReceiver);
		unregisterSensorStep();
		if (mWakeLock != null) {
			mWakeLock.release();
		}
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		Sensor sensor = event.sensor;

		switch (sensor.getType()) {
			case Sensor.TYPE_STEP_COUNTER:
				if (event.values[0] > Integer.MAX_VALUE) {
					Log.d(TAG, "Sensor: probably not a real value: " + event.values[0]);
					return;
				} else {
					int steps = (int) event.values[0];
					if (steps > 0) {
						Log.d(TAG, "Sensor: from registering " + event.values[0]);

						storeRawSteps(steps);

						stepRepository.insertStep(new Date().getTime(), event.values[0]);
						Log.d(TAG, "insertStep called ==> timestamp:" + event.timestamp);

						//Store the number of zero steps if none yet
						if (getZeroSteps() == 0) {
							storeZeroSteps();
						}
						storeProvider("STEP_COUNTER");

					}
				}
				break;
			case Sensor.TYPE_ACCELEROMETER:
				storeProvider("ACCELEROMETER");
				updateAccel(
						event.timestamp, event.values[0], event.values[1], event.values[2]);
				//countStep(event.values[0], event.values[1], event.values[2]);

				break;
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	public void updateAccel(long timeNs, float x, float y, float z) {
		float[] currentAccel = new float[3];
		currentAccel[0] = x;
		currentAccel[1] = y;
		currentAccel[2] = z;

		// First step is to update our guess of where the global z vector is.
		accelRingCounter++;
		accelRingX[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[0];
		accelRingY[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[1];
		accelRingZ[accelRingCounter % ACCEL_RING_SIZE] = currentAccel[2];

		float[] worldZ = new float[3];
		worldZ[0] = SensorFilter.sum(accelRingX) / Math.min(accelRingCounter, ACCEL_RING_SIZE);
		worldZ[1] = SensorFilter.sum(accelRingY) / Math.min(accelRingCounter, ACCEL_RING_SIZE);
		worldZ[2] = SensorFilter.sum(accelRingZ) / Math.min(accelRingCounter, ACCEL_RING_SIZE);

		float normalization_factor = SensorFilter.norm(worldZ);

		worldZ[0] = worldZ[0] / normalization_factor;
		worldZ[1] = worldZ[1] / normalization_factor;
		worldZ[2] = worldZ[2] / normalization_factor;

		float currentZ = SensorFilter.dot(worldZ, currentAccel) - normalization_factor;
		velRingCounter++;
		velRing[velRingCounter % VEL_RING_SIZE] = currentZ;

		float velocityEstimate = SensorFilter.sum(velRing);

		if (velocityEstimate > STEP_THRESHOLD && oldVelocityEstimate <= STEP_THRESHOLD
				&& (timeNs - lastStepTimeNs > STEP_DELAY_NS)) {
			stepCount++;
			storeAccelerometreSteps(stepCount);

			lastStepTimeNs = timeNs;
		}
		oldVelocityEstimate = velocityEstimate;
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	public void registerSensorStep() {
		if (mContext != null) {
			Log.d(TAG, "Register sensor listener");
			SensorManager sm = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);

			if (isStepCounterFeatureAvailable(mContext.getPackageManager())) {
				senStepCounter = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
				sm.registerListener(this, senStepCounter, SensorManager.SENSOR_DELAY_FASTEST);

			} else {
				senAccelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
				sm.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	public void unregisterSensorStep() {
		if (mContext != null) {
			Log.d(TAG, "Unregister sensor listener");
			SensorManager sm = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
			sm.unregisterListener(this);
			stopForeground(true);
		}
	}

	public void restartListener() {
		OuiStepService sensorStepService = OuiStepService.getInstance();
		if (sensorStepService != null) {
			sensorStepService.unregisterSensorStep();
			sensorStepService.registerSensorStep();
			mWakeLock.acquire();
		}
	}

	private Notification getNotification(Context context) {
		Intent notificationIntent = new Intent(context, getNotificationLaunchClass());
		PendingIntent contentIntent = PendingIntent.getActivity(context,
				0, notificationIntent,
				PendingIntent.FLAG_CANCEL_CURRENT);

		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
						.setContentTitle(getNotificationContentTitle())
						.setContentText(getNotificationContentText())
						.setContentIntent(contentIntent);

		return mBuilder.build();
	}


	public int getSteps() {
		int steps = 0;

		if (getProvider().equals("STEP_COUNTER")) {

			Log.d(TAG, "getSteps called ==> Steps:" + getRawSteps() + " getZeroSteps:" + getZeroSteps());
			steps = getRawSteps() - getZeroSteps();

			if (steps < 0) {
				storeZeroSteps();
				return 0;
			}

		} else if (getProvider().equals("ACCELEROMETER")) {

			steps = getAccelerometreSteps();
		}

		return steps;
	}

	public double getDailySteps() {
		int steps = 0;

		LiveData<List<Step>> stepList = null;
		if (stepRepository != null)
			stepList = stepRepository.getSteps();

		if (getProvider().equals("STEP_COUNTER")) {


			Log.d(TAG, "getSteps called ==> Steps:" + getRawSteps() + " getZeroSteps:" + getZeroSteps());
			steps = getRawSteps() - getZeroSteps();

			if (steps < 0) {
				storeZeroSteps();
				return 0;
			}

		}
		if (stepList != null) {
			Log.d(TAG, "Steps: "+stepList.getValue().get(0).getValue());
			return stepList.getValue().get(0).getValue();
		}
		else return 0;
	}


	public static boolean isStepCounterFeatureAvailable(PackageManager pm) {   // Require at least Android KitKat
		int currentApiVersion = (int) Build.VERSION.SDK_INT;
		// Check that the device supports the step counter and detector sensors
		return currentApiVersion >= 19 && pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER) && pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR);
	}

	public abstract int getRawSteps();

	public abstract int getAccelerometreSteps();

	public abstract void storeAccelerometreSteps(int steps);

	public abstract void storeRawSteps(int steps);


	public abstract void storeProvider(String provider);

	public abstract String getProvider();

	public abstract int getZeroSteps();

	public abstract void storeZeroSteps();

	public abstract Class getNotificationLaunchClass();


	public abstract String getNotificationContentTitle();

	public abstract String getNotificationContentText();


}
