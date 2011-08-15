package com.happytap.schedule.database;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
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

	private boolean useExternalStorage() {
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		return mExternalStorageWriteable;
	}

	private File externalStorageDatabaseFolder() {
		File file = new File(Environment.getExternalStorageDirectory()
				+ "/Android/data/"
				+ context.getApplicationContext().getPackageName() + "/files/");
		return file;
	}

	@Override
	public synchronized SQLiteDatabase getReadableDatabase() {
		SharedPreferences prefs = context.getSharedPreferences("database_info",
				Context.MODE_PRIVATE);
		final File database;
		if (useExternalStorage()) {
			File folder = externalStorageDatabaseFolder();
			folder.mkdirs();
			database = new File(folder, NAME);
			context.getDatabasePath(NAME).delete();
		} else {
			database = context.getDatabasePath(NAME);
		}
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
		if (!useExternalStorage()) {
			return super.getReadableDatabase();
		} else {
			return SQLiteDatabase.openDatabase(database.getPath(), null,
					SQLiteDatabase.OPEN_READONLY);
		}
	}

	private void copyDatabaseTo(File database) throws IOException {
		// long start = System.currentTimeMillis();
		try {
			installMeter.onBeforeCopy();
		} catch (Exception e) {
			Log.e(getClass().getSimpleName(), "onBeforeCopy Exception", e);
		}

		Field[] fields = R.raw.class.getFields();
		long totalSize = 0;
		for (int i = 0; i < fields.length; i++) {
			if(!fields[i].getName().startsWith("database")) {
				continue;
			}
			try {
				InputStream in = context.getResources().openRawResource(
						fields[i].getInt(null));
				totalSize += in.available();
				in.close();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

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
			byte[] buffer = new byte[1024 * 5];
			long totalBytesCopied = 0;
			Arrays.sort(fields, new Comparator<Field>() {

				@Override
				public int compare(Field object1, Field object2) {
					return object1.getName().compareTo(object2.getName());
				}

			});
			for (int i = 0; i < fields.length; i++) {
				if(!fields[i].getName().startsWith("database")) {
					continue;
				}
				InputStream in = context.getResources().openRawResource(
						fields[i].getInt(null));

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
		SQLiteDatabase db = SQLiteDatabase.openDatabase(database.getPath(),
				null, SQLiteDatabase.OPEN_READWRITE);
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
		Cursor cal = db.rawQuery("select min(start), max(end) from calendar",
				null);
		if (cal.moveToNext()) {
			preferences.edit().putLong("minimumCalendarDate", cal.getLong(0))
					.putLong("maximumCalendarDate", cal.getLong(1)).putBoolean("usesCalendar", true).commit();
		} else {
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
						.putLong("maximumCalendarDate", c.getLong(1)).putBoolean("usesCalendar", false).commit();
			}
			c.close();
		}
		cal.close();

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
