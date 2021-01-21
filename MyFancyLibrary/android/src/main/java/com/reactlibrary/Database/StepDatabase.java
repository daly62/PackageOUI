package com.reactlibrary.Database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Step.class}, version = 1, exportSchema = false)
public abstract class StepDatabase extends RoomDatabase {

	public abstract StepDao stepDao();
}
