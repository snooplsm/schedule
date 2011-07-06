package com.happytap.schedule.database;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.njtransit.rail.R;

@Singleton
public class DatabaseHelper extends SQLiteOpenHelper {

	private Context context;
	private InstallDatabaseMeter installMeter;

	@Inject
	SharedPreferences preferences;

	public interface InstallDatabaseMeter {

		void onBeforeCopy();

		void onPercentCopied(long copySize, float percent, long totalBytesCopied);

		void onSizeToBeCopiedCalculated(long copySize);

		void onFinishedCopying();

	}

	private static final String NAME = "database.sqlite";
	private static final int VERSION = 1;
	private static final String DATABASE_VERSION = "database_version";

	@Override
	public synchronized SQLiteDatabase getReadableDatabase() {
		SharedPreferences prefs = context.getSharedPreferences("database_info",
				Context.MODE_PRIVATE);
		File database = context.getDatabasePath(NAME);
		int lastVersion = prefs.getInt(DATABASE_VERSION, -1);
		int version = getVersion();
		if (lastVersion < version) {
			database.delete();
			try {
				copyDatabaseTo(database);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return super.getReadableDatabase();
	}

	private void copyDatabaseTo(File database) throws IOException {
		// long start = System.currentTimeMillis();
		try {
			installMeter.onBeforeCopy();
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(), "onBeforeCopy Exception", e);
		}
		List<String> partions = new ArrayList<String>();
		final String[] files = context.getAssets().list("database");
		for (String f : files) {
			if (f.startsWith("database.sqlite_")) {
				partions.add(f);
			}
		}
		Collections.sort(partions);
		long totalSize = partions.size() * 51200;

		try {
			installMeter.onSizeToBeCopiedCalculated(totalSize);
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(),
					"onSizeToBeCopiedCalculated Exception", e);
		}
		OutputStream out = null;
		try {
			if (!database.exists()) {
				database.getParentFile().mkdirs();
				database.createNewFile();
			}
			out = new FileOutputStream(database);
			byte[] buffer = new byte[1024];
			long totalBytesCopied = 0;
			for (String partition : partions) {
				final InputStream in = context.getAssets().open(
						"database/" + partition);
				int read;
				while ((read = in.read(buffer)) > 0) {
					out.write(buffer);
					totalBytesCopied += read;
				}
				in.close();
				try {
					float percent = totalBytesCopied / (float) totalSize;
					percent = Math.min(1, percent);
					installMeter.onPercentCopied(totalSize, percent,
							totalBytesCopied);
				} catch (Exception e) {
					Log.e(getClass().getSimpleName(),
							"onPercentCopied Exception", e);
				}
			}
			SharedPreferences prefs = context.getSharedPreferences(
					"database_info", Context.MODE_PRIVATE);
			prefs.edit().putInt(DATABASE_VERSION, getVersion()).commit();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (out != null) {
				out.flush();
				out.close();
			}
		}
		SQLiteDatabase db = super.getWritableDatabase();
		String[] replacements = context.getResources().getStringArray(
				R.array.replacement_names);
		db.beginTransaction();
		for (int i = 0; i < replacements.length; i++) {
			String[] idToName = replacements[i].split(",");
			ContentValues cv = new ContentValues();
			cv.put("name", idToName[1]);
			db.update("stops", cv, "id=?", new String[] { idToName[0] });
		}
		db.setTransactionSuccessful();
		db.endTransaction();
		Cursor c = db
				.rawQuery(
						"select min(calendar_date), max(calendar_date) from calendar_dates",
						null);
		if (c.moveToNext()) {
			Calendar min = Calendar.getInstance();
			min.setTimeInMillis(c.getLong(0));
			Calendar max = Calendar.getInstance();
			max.setTimeInMillis(c.getLong(1));
			preferences.edit().putLong("minimumCalendarDate", c.getLong(0))
			.putLong("maximumCalendarDate", c.getLong(1)).commit();
		}

		try {
			installMeter.onFinishedCopying();
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(), "onFinishedCopying exception", e);
		}
	}

	public int getVersion() {
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
			return info.versionCode;
		} catch (NameNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Inject
	public DatabaseHelper(Context context) {
		super(context, NAME, null, VERSION);
		this.context = context;
	}

	public InstallDatabaseMeter getInstallMeter() {
		return installMeter;
	}

	public void setInstallMeter(InstallDatabaseMeter installMeter) {
		this.installMeter = installMeter;
	}

	@Override
	public void onCreate(SQLiteDatabase arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub

	}

}
