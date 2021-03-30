package com.reactlibrary.Database;

import android.content.Context;
import android.os.AsyncTask;


import androidx.lifecycle.LiveData;
import androidx.room.Room;

import java.util.Date;
import java.util.List;

public class StepRepository {


	private String DB_NAME = "db_steps";

	private StepDatabase stepDatabase;

	public StepRepository(Context context) {
		stepDatabase = Room.databaseBuilder(context, StepDatabase.class, DB_NAME).build();
	}

	public void insertStep(Long timeStamp, double value,String type) {

		Step step = new Step();
		step.setCreatedAt(timeStamp);
		step.setValue(value);
		step.setType(type);
		insertStep(step);
	}


	public void insertStep(final Step step) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... voids) {
				stepDatabase.stepDao().insertStep(step);
				return null;
			}
		}.execute();
	}

	public LiveData<List<Step>> getSteps() {
		return stepDatabase.stepDao().fetchAllSteps();
	}

	public LiveData<List<Step>> fetchAllStepsByDates(Long strDate, Long endDate){
		return stepDatabase.stepDao().fetchAllStepsByDate(strDate,endDate);
	}

}
