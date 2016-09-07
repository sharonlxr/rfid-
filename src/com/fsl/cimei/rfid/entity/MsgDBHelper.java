package com.fsl.cimei.rfid.entity;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import com.freescale.api.Constants;

public class MsgDBHelper extends SQLiteOpenHelper {

	private static final String DB_Name = "msg2.db";
	private String tableName = "msg2";
	private String createTableSQL = "create table msg2 (_id integer primary key autoincrement, content text, time text, sender text, type text, mach text)";
	private String createTableUniqueIndex = "create unique index msg_uk_2 on msg2 (time, mach, content)";

	private SQLiteDatabase db;
	private SQLiteDatabase readableDb;

	public MsgDBHelper(Context context) {
		super(context, DB_Name, null, 1);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public void onConfigure(SQLiteDatabase db) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			super.onConfigure(db);
			// db.execSQL("PRAGMA journal_mode=OFF");
			this.setWriteAheadLoggingEnabled(false);
			db.execSQL("PRAGMA synchronous=OFF");
			db.execSQL("PRAGMA temp_store=MEMORY");
			db.execSQL("PRAGMA cache_size=20000");
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		this.db = db;
		db.execSQL(createTableSQL);
		db.execSQL(createTableUniqueIndex);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	public boolean insert(ContentValues values) {
		// SQLiteDatabase db = getWritableDatabase();
		if (db == null) {
			db = getWritableDatabase();
		}
		// long a = db.insert(tableName, null, values);
		long a = db.replace(tableName, null, values);
		// db.close();
		if (a == -1) {
			return false;
		} else {
			return true;
		}
	}

	public Cursor query() {
		if (null == readableDb) {
			readableDb = getReadableDatabase();// getWritableDatabase();
		}
		Cursor c = readableDb.query(tableName, null, null, null, null, null, null);
		return c;
	}

	public Cursor query(String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		if (null == readableDb) {
			readableDb = getReadableDatabase();// getWritableDatabase();
		}
		Cursor c = readableDb.query(tableName, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
		return c;
	}

	public void del(int id) {
		if (db == null) {
			db = getWritableDatabase();
		}
		db.delete(tableName, "_id=?", new String[] { String.valueOf(id) });
	}

	public void del(String machId) {
		if (db == null) {
			db = getWritableDatabase();
		}
		db.delete(tableName, "mach=?", new String[] { machId });
	}

	public int delHist(String date) {
		if (db == null) {
			db = getWritableDatabase();
		}
		return db.delete(tableName, "time<?", new String[] { date });
	}

	public void updateType(String machId, String oldType) {
		if (db == null) {
			db = getWritableDatabase();
		}
		ContentValues values = new ContentValues();
		values.put("type", Constants.TYPE_ERROR);
		db.update(tableName, values, "mach=? and type=?", new String[] { machId, oldType });
	}

	public void close() {
		if (db != null) {
			db.close();
		}
		if (readableDb != null) {
			readableDb.close();
		}
	}
	//
	// public void update() {
	// SQLiteDatabase db = getWritableDatabase();
	// db.execSQL("create unique index msg_uk_1 on msg (time, mach, content)");
	// }

}
