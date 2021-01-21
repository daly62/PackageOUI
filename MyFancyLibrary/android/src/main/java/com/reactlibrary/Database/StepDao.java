package com.reactlibrary.Database;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.Date;
import java.util.List;

@Dao
public interface StepDao {

	@Insert
	Long insertStep(Step step);


	@Query("SELECT * FROM Step ORDER BY created_at desc")
	LiveData<List<Step>> fetchAllSteps();


	@Query("SELECT * FROM Step  WHERE created_at  Between :startDate and :endDate  ORDER BY created_at desc")
	LiveData<List<Step>> fetchAllStepsByDate(Long startDate, Long endDate);


	@Delete
	void deleteTask(Step step);
}
