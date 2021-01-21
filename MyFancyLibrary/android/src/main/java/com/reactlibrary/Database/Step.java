package com.reactlibrary.Database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.io.Serializable;
import java.util.Date;

@Entity
public class Step implements Serializable {

	@PrimaryKey(autoGenerate = true)
	private int id;

	@ColumnInfo(name = "created_at")
	@TypeConverters({TimestampConverter.class})
	private Long createdAt;

	private double value;


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Long createdAt) {
		this.createdAt = createdAt;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}
}
