package com.happytap.schedule.activity;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import roboguice.RoboGuice;
import roboguice.inject.InjectView;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.BounceInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.inject.Inject;
import com.googlecode.android.widgets.DateSlider.DateSlider;
import com.googlecode.android.widgets.DateSlider.DateSlider.OnDateSetListener;
import com.googlecode.android.widgets.DateSlider.DefaultDateSlider;
import com.happytap.schedule.database.DatabaseHelper;
import com.happytap.schedule.database.DatabaseHelper.InstallDatabaseMeter;
import com.happytap.schedule.database.PreferencesDao;
import com.happytap.schedule.provider.PreferencesDatabaseProvider;
import com.happytap.schedule.provider.SQLiteDatabaseProvider;
import com.happytap.schedule.service.ScheduleService;
import com.happytap.schedule.service.ThreadHelper;
import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;
import us.wmwm.njrail.R;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.SlidingMenu.OnCloseListener;
import com.slidingmenu.lib.SlidingMenu.OnOpenListener;
import com.slidingmenu.lib.app.SlidingActivityBase;
import com.slidingmenu.lib.app.SlidingActivityHelper;

@SuppressLint("NewApi")
public class SplashScreenActivity extends ScheduleActivity implements
		SlidingActivityBase, OnOpenListener, OnCloseListener {

	private static final int CHANGE_DATE_DIALOG = 1970;
	private static final int RETRY = 1;

	public static SimpleDateFormat df = new SimpleDateFormat("MM/dd/yy");
	public static SimpleDateFormat LONG_DATE = new SimpleDateFormat(
			"MMMM d, yyyy");

	private static final int TOAST = 1;

	MenuItem about;

	@InjectView(R.id.arrival)
	protected RelativeLayout arrival;

	private String arrivalStopId;

	@InjectView(R.id.arrivalText)
	private TextView arrivalText;

	@InjectView(R.id.container)
	private LinearLayout container;

	@InjectView(R.id.scheduleEnd)
	private TextView scheduleEnd;

	private OnClickListener clickStationListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(SplashScreenActivity.this,
					StationListActivity.class);
			SplashScreenActivity.this.startActivityForResult(intent, v.getId());
		}

	};

	@Inject
	private DatabaseHelper databaseHelper;
	

	MenuItem departAt;
	@InjectView(R.id.departure)
	protected RelativeLayout departure;
	@InjectView(R.id.departureDate)
	TextView departureDateText;;

	private String departureStopId;

	@InjectView(R.id.departureText)
	private TextView departureText;

	LinkedHashSet<Character> enabledCharacters;

	@InjectView(R.id.get_schedule)
	protected View getSchedule;

	private OnClickListener getScheduleClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (departureStopId != null && arrivalStopId != null) {

			} else {
				Toast.makeText(SplashScreenActivity.this, "choose stations",
						Toast.LENGTH_SHORT).show();
				return;
			}
			Intent intent = new Intent(SplashScreenActivity.this,
					LoadScheduleActivity.class);
			intent.putExtra(LoadScheduleActivity.DEPARTURE_STATION,
					departureText.getText());
			intent.putExtra(LoadScheduleActivity.ARRIVAL_STATION,
					arrivalText.getText());
			intent.putExtra(LoadScheduleActivity.DEPARTURE_DATE_START,
					userDefinedDate);
			intent.putExtra(LoadScheduleActivity.DEPARTURE_ID, departureStopId);
			intent.putExtra(LoadScheduleActivity.ARRIVAL_ID, arrivalStopId);
			if (DateUtils.isToday(userDefinedDate.getTimeInMillis())) {
				Calendar tom = Calendar.getInstance();
				tom.setTimeInMillis(userDefinedDate.getTimeInMillis());
				tom.add(Calendar.DAY_OF_YEAR, 1);
				intent.putExtra(LoadScheduleActivity.DEPARTURE_DATE_END, tom);
			} else {
				intent.putExtra(LoadScheduleActivity.DEPARTURE_DATE_END,
						userDefinedDate);
			}
			startActivity(intent);
		}

	};

	private boolean canShowScheduleExpiration() {
		return getSharedPreferences(
				getApplication().getPackageName() + "_preferences",
				Context.MODE_PRIVATE).getBoolean("showScheduleExpiration",
				false);
	}

	private AsyncTask<Void, Float, Void> loadingTask = newLoadingTask();

	private AsyncTask<Void, Float, Void> newLoadingTask() {
		return new AsyncTask<Void, Float, Void>() {

			boolean error = false;

			@Override
			protected Void doInBackground(Void... params) {
				error = false;
				databaseHelper.setInstallMeter(new InstallDatabaseMeter() {

					@Override
					public void onBeforeCopy() {
						// TODO Auto-generated method stub

					}

					@Override
					public void onFinishedCopying() {

					}

					@Override
					public void onPercentCopied(long copySize, float percent,
							long totalBytesCopied) {
						publishProgress(percent);
					}

					@Override
					public void onSizeToBeCopiedCalculated(long copySize) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onError(File database, Exception e) {
						error = true;

						try {
							SharedPreferences prefs = getApplicationContext()
									.getSharedPreferences("database_info",
											Context.MODE_PRIVATE);
							prefs.edit().clear().commit();
							for(File file : database.getParentFile().listFiles()) {
								file.delete();
							}
						} catch (Exception ex) {

						}
					}

				});
				try {
					databaseHelper.getReadableDatabase();
					provider.get();
					preferencesProvider.get();
				} catch (Exception e) {
					error = true;
				}

				return null;
			}

			private DecimalFormat df = new DecimalFormat("0.0%");

			@Override
			protected void onProgressUpdate(Float... values) {
				percentage.setText(df.format(values[0]));
			}

			protected void onPostExecute(Void result) {
				if (!error) {
					splashContainer.setVisibility(View.GONE);
					getSupportActionBar().show();
					history.setAdapter(adapter = new PreferencesAdapter());
				} else {
					percentage
							.setText("Something has gone horribly wrong.  Is your disk full?  Uninstall and reinstall.");
					showDialog(RETRY);
				}
			};
		};
	}

	private PreferencesAdapter adapter;
	public View findViewById(int id) {
		View v = super.findViewById(id);
		if (v != null)
			return v;
		return mHelper.findViewById(id);
	}

	public void setContentView(int id) {
		setContentView(getLayoutInflater().inflate(id, null));
	}

	public void setContentView(View v) {
		setContentView(v, new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
	}

	public void setContentView(View v, LayoutParams params) {
		super.setContentView(v, params);
		mHelper.registerAboveContentView(v, params);
	}

	public void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mHelper.onPostCreate(savedInstanceState);
	}

	private OnLongClickListener longClickStationListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			openContextMenu(v);
			return true;
		}

	};

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			try {
				Message msg = Message.obtain(null,
						ScheduleService.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
				msg = Message.obtain(null, ScheduleService.CHECK_FOR_UPGRADE);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			mService = null;
		}
	};

	boolean mIsBound;

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			final Object obj = msg.obj;
			int what = msg.what;
			// if(what==ScheduleService.FOUND_SCHEDULE) {
			// new Thread() {
			// public void run() {
			// Intent intent = new Intent(LoadScheduleActivity.this,
			// StationToStationActivity.class);
			// intent.putExtra(DEPARTURE_STATION,
			// getIntent().getStringExtra(StationToStationActivity.DEPARTURE_STATION));
			// intent.putExtra(ARRIVAL_STATION,
			// getIntent().getStringExtra(StationToStationActivity.ARRIVAL_STATION));
			// intent.putExtra(DEPARTURE_ID,
			// getIntent().getStringExtra(StationToStationActivity.DEPARTURE_ID));
			// intent.putExtra(ARRIVAL_ID,
			// getIntent().getStringExtra(StationToStationActivity.ARRIVAL_ID));
			// intent.putExtra(StationToStationActivity.SCHEDULE,
			// (Schedule)obj);
			// startActivity(intent);
			// finish();
			// };
			// }.start();
			//
			// }
		}
	}

	final Messenger mMessenger = new Messenger(new IncomingHandler());

	private Messenger mService = null;

	@Inject
	private PreferencesDao preferencesDao;

	@Inject
	SharedPreferences preferences;
	MenuItem preferencesItem;
	@Inject
	private PreferencesDatabaseProvider preferencesProvider;

	@Inject
	private SQLiteDatabaseProvider provider;

	@InjectView(R.id.reverse)
	private ImageView reverse;

	@InjectView(R.id.percentage)
	private TextView percentage;

	@InjectView(R.id.favs)
	private ImageView favs;

	private OnClickListener reverseListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			View[] views = { departureText, arrivalText };
			for (View vi : views) {
				try {
					if (vi.getAnimation() != null) {
						// vi.getAnimation().cancel();
					}
				} catch (Exception e) {

				}
			}
			final AlphaAnimation dAnimation = new AlphaAnimation(1, 0);
			dAnimation.setZAdjustment(Animation.ZORDER_TOP);
			dAnimation.setDuration(200);
			dAnimation.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationEnd(Animation animation) {
					CharSequence temp = arrivalText.getText();
					if (departureStopId != null) {
						arrivalText.setText(departureText.getText());
					} else {
						arrivalText.setText(getString(R.string.arrival_text));
					}
					if (arrivalStopId != null) {
						departureText.setText(temp);
					} else {
						departureText
								.setText(getString(R.string.departure_text));
					}
					temp = arrivalStopId;
					SplashScreenActivity.this.arrivalStopId = departureStopId;
					if (temp != null) {
						SplashScreenActivity.this.departureStopId = temp
								.toString();
					} else {
						SplashScreenActivity.this.departureStopId = null;
					}
					AlphaAnimation bAnimation = new AlphaAnimation(0, 1);
					bAnimation.setDuration(500);
					TranslateAnimation _tAnim = new TranslateAnimation(0, 0,
							-200, 0);
					_tAnim.setInterpolator(new BounceInterpolator());
					_tAnim.setDuration(500);
					AnimationSet set = new AnimationSet(false);
					set.addAnimation(bAnimation);
					set.addAnimation(_tAnim);

					departureText.setVisibility(View.VISIBLE);
					arrivalText.setVisibility(View.VISIBLE);
					departureText.startAnimation(set);
					arrivalText.startAnimation(set);
				}

				public void onAnimationRepeat(Animation animation) {
				};

				public void onAnimationStart(Animation animation) {

				};
			});
			// aAnimation.setDuration(550);
			// TranslateAnimation translate1 = new
			// TranslateAnimation(0,0,0,d.top-a.top);
			// translate1.setDuration(5000);
			// EXECUTE
			departureText.startAnimation(dAnimation);
			arrivalText.startAnimation(dAnimation);
		}

	};

	private OnClickListener favsListener = new OnClickListener() {
		public void onClick(View arg0) {
			toggle();
		};
	};

	MenuItem share;;

	@InjectView(R.id.splash)
	private View splashContainer;

	@InjectView(R.id.loading)
	private ImageView splashImage;

	private Calendar userDefinedDate;

	private void displayDate() {
		if (DateUtils.isToday(userDefinedDate.getTimeInMillis())) {
			departureDateText.setText("for Today");
		} else {
			departureDateText.setText("for "
					+ df.format(userDefinedDate.getTime()));
		}
	}

	void doBindService() {
		boolean bound = bindService(new Intent(SplashScreenActivity.this,
				ScheduleService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	void doUnbindService() {
		if (mIsBound) {
			if (mService != null) {
				try {
					Message msg = Message.obtain(null,
							ScheduleService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
				}
			}

			unbindService(mConnection);
			mIsBound = false;
		}
	}

	protected void onActivityResult(int requestCode, int resultCode,
			android.content.Intent data) {
		if (resultCode == RESULT_OK) {
			String stopId = data.getStringExtra(StationListActivity.STOP_ID);
			String name = data.getStringExtra(StationListActivity.STOP_NAME);

			TextView text;

			if (requestCode == R.id.arrival) {
				text = arrivalText;
				arrivalStopId = stopId;
				preferences.edit().putString("lastArrivalStopId", stopId)
						.putString("lastArrivalStopText", name).commit();
			} else {
				text = departureText;
				departureStopId = stopId;
				preferences.edit().putString("lastDepartureStopId", stopId)
						.putString("lastDepartureStopText", name).commit();
			}
			text.setText(name);
		}

	}

	public int getVersion() {
		try {
			PackageInfo info = getApplicationContext()
					.getPackageManager()
					.getPackageInfo(getApplicationContext().getPackageName(), 0);
			return info.versionCode;
		} catch (NameNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private void showScheduleEnd(boolean toast) {
		Long min = preferences.getLong("minimumCalendarDate", 0);
		Long max = preferences.getLong("maximumCalendarDate", 0);
		SharedPreferences prefs = getApplicationContext().getSharedPreferences(
				"database_info", Context.MODE_PRIVATE);
		int lastVersion = prefs.getInt(DatabaseHelper.DATABASE_VERSION, -1);
		int version = getVersion();
		if (lastVersion == version && min != 0 && max != 0) {
			long diff = max - System.currentTimeMillis();
			if (diff <= 0) {
				if (toast)
					Toast.makeText(
							SplashScreenActivity.this,
							"Bro, your schedule is out of date, download an update!",
							Toast.LENGTH_LONG).show();
			} else {
				long days = diff / 86400000;
				if (toast)
					// Toast.makeText(
					// SplashScreenActivity.this,
					// String.format(
					// "schedule is good for %s days or until changed",
					// days), Toast.LENGTH_LONG).show();
					if (canShowScheduleExpiration()) {
						scheduleEnd.setText("valid til "
								+ LONG_DATE.format(new Date(max)));
						scheduleEnd.setVisibility(View.VISIBLE);
					} else {
						scheduleEnd.setVisibility(View.GONE);
					}
			}
		} else {
			scheduleEnd.setVisibility(View.GONE);
		}
	}

	private SlidingActivityHelper mHelper;

	private View behind;

	private ListView history;
	
	private View controls;
	
	private View trash;
	private View cancel;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHelper = new SlidingActivityHelper(this);
		mHelper.onCreate(savedInstanceState);
		getSupportActionBar().hide();
		setContentView(R.layout.main);
		getSupportActionBar().setDisplayUseLogoEnabled(false);
		getSupportActionBar().setDisplayShowHomeEnabled(false);

		SlidingMenu menu = mHelper.getSlidingMenu();
		menu.setTouchModeBehind(SlidingMenu.TOUCHMODE_FULLSCREEN);
		menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		menu.setShadowDrawable(R.drawable.defaultshadow);
		menu.setBehindOffset((int) getResources().getDimension(
				R.dimen.abs__action_button_min_width));
		// menu.setShadowWidth(1);
		menu.setBehindScrollScale(.25f);
		setSlidingActionBarEnabled(true);
		setContentView(R.layout.main);
		setBehindContentView(behind = LayoutInflater.from(this).inflate(
				R.layout.history_list, null));
		menu.setOnOpenListener(this);
		menu.setOnCloseListener(this);
		history = (ListView) behind.findViewById(R.id.list);
		controls = behind.findViewById(R.id.controls);
		trash = behind.findViewById(R.id.trash);
		cancel = behind.findViewById(R.id.cancel);
		history.setOnItemClickListener(new OnItemClickListener() {
			
			@Override
			public void onItemClick(AdapterView<?> parent, final View view,
					int position, long id) {
				// TODO Auto-generated method stub
				toggle();
				ThreadHelper.getScheduler().schedule(new Runnable() {
					@Override
					public void run() {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {								
								History h = ((Holder) view.getTag()).history;
								departureStopId=h.fromId;
								arrivalStopId=h.toId;
								departureText.setText(h.departName);
								arrivalText.setText(h.arriveName);
								Intent intent = new Intent(SplashScreenActivity.this,
										LoadScheduleActivity.class);
								intent.putExtra(LoadScheduleActivity.DEPARTURE_STATION,h.departName
										);
								intent.putExtra(LoadScheduleActivity.ARRIVAL_STATION,
										h.arriveName);
								intent.putExtra(LoadScheduleActivity.DEPARTURE_DATE_START,
										Calendar.getInstance());
								intent.putExtra(LoadScheduleActivity.DEPARTURE_ID, h.fromId);
								intent.putExtra(LoadScheduleActivity.ARRIVAL_ID, h.toId);
								Calendar tom = Calendar.getInstance();
								tom.setTimeInMillis(userDefinedDate.getTimeInMillis());
								tom.add(Calendar.DAY_OF_YEAR, 1);
								intent.putExtra(LoadScheduleActivity.DEPARTURE_DATE_END, tom);
								startActivity(intent);
							}
						});
					}
				},400,TimeUnit.MILLISECONDS);
				
			}
		});
		View v = LayoutInflater.from(this).inflate(R.layout.history_header, null);
		Spinner sp = (Spinner) v.findViewById(R.id.spinner);
		final Spinner more = (Spinner) v.findViewById(R.id.more_overflow);
		more.setAdapter(new SimpleAdapter(Arrays.asList(new String[]{"","Edit"})) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				ImageView v =new ImageView(parent.getContext()); 
				v.setImageResource(R.drawable.abs__ic_menu_moreoverflow_normal_holo_dark);
				return v;
			}
			
			@Override
			public View getDropDownView(int position, View convertView,
					ViewGroup parent) {
				if(position==0) {
					View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.empty, null);
					//v.setVisibility(View.GONE);
					return v;
				}
				View v = super.getDropDownView(position, convertView, parent);
				
				return v;
			}
		});
		more.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				if(position==1) {
					adapter.edit();
					controls.setVisibility(View.VISIBLE);
					trash.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							adapter.removeCheckedItems();
							controls.setVisibility(View.GONE);
							more.setSelection(0);
						}
					});
					cancel.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							adapter.checked.clear();
							adapter.view();
							controls.setVisibility(View.GONE);
							more.setSelection(0);
						}
					});
				}
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				
			}
		});
		

		sp.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				String sort = (String)parent.getItemAtPosition(position);
				adapter.setSort(sort);
				PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putString("sort", sort).commit();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
			
		});
		String sort = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("sort", null);
		final List<String> choices = Arrays.asList(new String[]{"Frequency", "Most Recent", "Name"});
		sp.setAdapter(new SimpleAdapter(choices));
//		more.setAdapter(new SimpleAdapter(Arrays.asList("Edit")) {
//			@Override
//			public View getView(int position, View convertView, ViewGroup parent) {
//				View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.more_spinner, null);
//				return v;
//			}
//		});
		for(int i = 0; i < choices.size(); i++) {
			String s = choices.get(i);
			if(s.equals(sort)) {
				sp.setSelection(i);
			}
		}
		history.addHeaderView(v, null, false);
		SVG svg = SVGParser.getSVGFromResource(getResources(), R.raw.newjersey);
		if (Build.VERSION.SDK_INT >= 11) {
			splashImage.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
			reverse.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
			favs.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		}

		splashImage.setImageDrawable(svg.createPictureDrawable());

		svg = SVGParser.getSVGFromResource(getResources(), R.raw.reload);
		reverse.setImageDrawable(svg.createPictureDrawable());

		svg = SVGParser.getSVGFromResource(getResources(), R.raw.star);

		favs.setImageDrawable(svg.createPictureDrawable());
		fixReverseFavs();
		arrival.setOnClickListener(clickStationListener);
		departure.setOnClickListener(clickStationListener);
		// departure.setClipChildren(false);
		// arrival.setClipChildren(false);
		// container.setClipChildren(false);
		reverse.setOnClickListener(reverseListener);
		favs.setOnClickListener(favsListener);
		getSchedule.setOnClickListener(getScheduleClickListener);
		userDefinedDate = Calendar.getInstance();

		if (savedInstanceState == null) {
			arrivalStopId = preferences.getString("lastArrivalStopId", null);
			departureStopId = preferences
					.getString("lastDepartureStopId", null);
			arrivalText.setText(preferences.getString("lastArrivalStopText",
					getString(R.string.arrival_text)));
			departureText.setText(preferences
					.getString("lastDepartureStopText",
							getString(R.string.departure_text)));
		}
		if (preferences.getBoolean("checkForUpdates", true)) {
			// doBindService();
		}
		showScheduleEnd(true);
		onActivityResult(0, 0, null);
	}

	private void fixReverseFavs() {
		if (getSharedPreferences(
				getApplication().getPackageName() + "_preferences",
				Context.MODE_PRIVATE).getBoolean("showFavs", true)) {
			favs.setVisibility(View.VISIBLE);
			((LinearLayout.LayoutParams) reverse.getLayoutParams()).gravity = Gravity.NO_GRAVITY;
			((LinearLayout.LayoutParams) reverse.getLayoutParams()).weight = 0;
		} else {
			favs.setVisibility(View.GONE);
			((LinearLayout.LayoutParams) reverse.getLayoutParams()).weight = 1;
			((LinearLayout.LayoutParams) reverse.getLayoutParams()).gravity = Gravity.CENTER_HORIZONTAL;
		}
		favs.invalidate();
		reverse.invalidate();
		reverse.getParent().requestLayout();
	}

	public void onCreateContextMenu(android.view.ContextMenu menu, View v,
			android.view.ContextMenu.ContextMenuInfo menuInfo) {
		PreferencesDao dao = RoboGuice.getInjector(SplashScreenActivity.this)
				.getInstance(PreferencesDao.class);

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == CHANGE_DATE_DIALOG) {
			return new DefaultDateSlider(this, new OnDateSetListener() {

				@Override
				public void onDateSet(DateSlider view, Calendar selectedDate) {
					userDefinedDate = selectedDate;
					displayDate();
				}

			}, userDefinedDate);
		}
		if (id == RETRY) {
			AlertDialog d = new AlertDialog.Builder(this)
					.setTitle("Error Copying Database")
					.setMessage(
							"Please ensure you have roughly 13mb of disk available.")
					.setCancelable(true)
					.setPositiveButton("Retry",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									loadingTask = newLoadingTask();
									loadingTask.execute();
									dismissDialog(RETRY);
								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dismissDialog(RETRY);
								}
							}).create();
			return d;
		}
		return super.onCreateDialog(id);
	}

	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// departAt = menu.add("Depart on");
	// departAt.setIcon(android.R.drawable.ic_menu_month);
	// share = menu.add("Share");
	// share.setIcon(android.R.drawable.ic_menu_share);
	// preferencesItem = menu.add("Preferences");
	// preferencesItem.setIcon(android.R.drawable.ic_menu_preferences);
	// about = menu.add("About");
	// about.setIcon(android.R.drawable.ic_menu_info_details);
	// return super.onCreateOptionsMenu(menu);
	// }

	private com.actionbarsherlock.view.MenuItem depart, prefs, abt;

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		depart = menu.add("Depart on").setIcon(R.drawable.ic_time);
		depart.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		prefs = menu.add("Preferences").setIcon(R.drawable.ic_settings);
		prefs.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		abt = menu.add("About").setIcon(R.drawable.ic_about);
		abt.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		if (item.equals(depart)) {
			showDialog(CHANGE_DATE_DIALOG);
		}
		if (item.equals(prefs)) {
			Intent intent = new Intent(this, SchedulePreferenceActivity.class);
			startActivityForResult(intent, 0);
		}
		if (item.equals(abt)) {
			Intent intent = new Intent(this, AboutActivity.class);
			startActivity(intent);
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
		loadingTask.cancel(false);
	}

	//
	// @Override
	// public boolean onOptionsItemSelected(MenuItem item) {
	// if (item.equals(departAt)) {
	// showDialog(CHANGE_DATE_DIALOG);
	// }
	// if (item.equals(share)) {
	// Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
	// shareIntent.setType("text/plain");
	// Resources r = getResources();
	// String url = r.getString(R.string.application_url);
	// String name = r.getString(R.string.app_name);
	// shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, name
	// + " " + url);
	// shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Get "
	// + name + " for Android @ " + url);
	// startActivity(Intent.createChooser(shareIntent, "Share"));
	// }
	// if (item.equals(preferencesItem)) {
	// Intent intent = new Intent(this, SchedulePreferenceActivity.class);
	// startActivityForResult(intent, 0);
	// }
	// if (item.equals(about)) {
	// Intent intent = new Intent(this, AboutActivity.class);
	// startActivity(intent);
	// }
	// return true;
	// }

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (savedInstanceState != null) {
			String departureStopId = savedInstanceState
					.getString("departureStopId");
			if (departureStopId != null) {
				this.departureStopId = departureStopId;
				departureText.setText(savedInstanceState
						.getString("departureStopName"));
			}
			String arrivalStopId = savedInstanceState
					.getString("arrivalStopId");
			if (arrivalStopId != null) {
				this.arrivalStopId = arrivalStopId;
				arrivalText.setText(savedInstanceState
						.getString("arrivalStopName"));
			}
		} else {

		}
	}

	protected void onResume() {
		super.onResume();
		if (loadingTask.getStatus() == Status.PENDING) {
			loadingTask.execute();
		}
		displayDate();
		showScheduleEnd(false);
		fixReverseFavs();

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("departureStopId", departureStopId);
		outState.putString("arrivalStopId", arrivalStopId);
		outState.putString("departureStopName", departureText.getText()
				.toString());
		outState.putString("arrivalStopName", arrivalText.getText().toString());
	}

	public void setBehindContentView(int id) {
		setBehindContentView(getLayoutInflater().inflate(id, null));
	}

	public void setBehindContentView(View v) {
		setBehindContentView(v, new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
	}

	public void setBehindContentView(View v, LayoutParams params) {
		mHelper.setBehindContentView(v, params);
	}

	public SlidingMenu getSlidingMenu() {
		return mHelper.getSlidingMenu();
	}

	public void toggle() {
		mHelper.toggle();
	}

	public void showAbove() {
		mHelper.showAbove();
	}

	public void showBehind() {
		mHelper.showBehind();
	}

	public void setSlidingActionBarEnabled(boolean b) {
		mHelper.setSlidingActionBarEnabled(b);
	}

	private class PreferencesAdapter extends BaseAdapter {

		public PreferencesAdapter() {			
		}
		@Override
		public int getCount() {
			Cursor c = null;
			try {
				c = preferencesProvider.get().rawQuery("select count(*) from history", null);
				c.moveToFirst();
				return c.getInt(0);
			} catch (Exception e) {
				return 0;
			} finally {
				try {
					c.close();
				} catch (Exception e) {
					
				}
			}			
		}

		public void removeCheckedItems() {
			for(Iterator<History> his= checked.iterator(); his.hasNext();) {
				History h = his.next();
				preferencesProvider.get().delete("history", "depart_id=? and arrive_id=?", new String[]{h.fromId,h.toId});
				his.remove();
			}
			view();
			notifyDataSetChanged();
		}

		private boolean edit = false;
		public void edit() {
			
			edit = true;
			notifyDataSetChanged();
		}
		
		public void view() {
			edit = false;
			notifyDataSetChanged();
		}

		private String orderBy = "occurrences";
		
		public void setSort(String itemAtPosition) {
			if(itemAtPosition.toLowerCase().equals("frequency")) {
				orderBy = "occurrences desc";
			} else if(itemAtPosition.toLowerCase().startsWith("most")){
				orderBy = "last_updated desc";
			} else {
				orderBy = "depart_name asc, arrive_name asc";
			}			
			notifyDataSetChanged();
		}

		@Override
		public Object getItem(int position) {
			Cursor c = preferencesProvider.get().rawQuery("select * from history order by " + orderBy + "  limit " + position + ",1", null);
			History h = new History();
			c.moveToFirst();
			h.fromId = c.getString(c.getColumnIndex("depart_id"));
			h.toId = c.getString(c.getColumnIndex("arrive_id"));
			h.departName= c.getString(c.getColumnIndex("depart_name"));
			h.arriveName = c.getString(c.getColumnIndex("arrive_name"));
			h.count = c.getInt(c.getColumnIndex("occurrences"));
			c.close();
			return h;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}
		
		@Override
		public int getItemViewType(int position) {
			if(edit) {
				return 1;
			}
			return 0;
		}
		
		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Holder holder;
			if(convertView==null) {
				if(edit) {
					convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.history_view_edit, null);
					
				} else {
					convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.history_view, null);
				}
				convertView.setTag(holder = new Holder());
				holder.from = (TextView) convertView.findViewById(R.id.from);
				holder.to = (TextView) convertView.findViewById(R.id.to);
				holder.occurrence = (TextView) convertView.findViewById(R.id.occurrence);
				holder.check = (CheckBox) convertView.findViewById(R.id.check);
			} else {
				holder = (Holder) convertView.getTag();
			}
			History history = (History) getItem(position);
			holder.from.setText(history.departName + " ↝");
			holder.to.setText(history.arriveName);		
			holder.occurrence.setText(String.valueOf(history.count));
			holder.history = history;
			if(edit) {
				convertView.setOnClickListener(editListener);
				holder.check.setTag(holder);
				holder.check.setOnCheckedChangeListener(onCheckedChangeListener);
				if(checked.contains(holder.history)) {
					holder.check.setChecked(true);
				} else {
					holder.check.setChecked(false);
				}
			}
			return convertView;
		}
		
		Set<History> checked = new HashSet<History>();
		
		private OnClickListener editListener = new OnClickListener() {
			public void onClick(View v) {
				Holder h = (Holder)v.getTag();
				h.check.toggle();
				if(h.check.isChecked()) {
					checked.add(h.history);
				} else {
					checked.remove(h.history);
				}
			};
		};
		
		private OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				Holder h = (Holder) buttonView.getTag();
				if(isChecked) {
					checked.add(h.history);
				} else {
					checked.remove(h.history);
				}
			}
		};
				

	};
	
	public boolean onMenuItemSelected(int featureId, com.actionbarsherlock.view.MenuItem item) {
		if(item.getItemId()==android.R.id.home) {
			toggle();
			return true;
		} else {
			return super.onMenuItemSelected(featureId, item);
		}
	};
	
	@Override
	public void onClose() {
		// TODO Auto-generated method stub
		getSupportActionBar().setDisplayUseLogoEnabled(false);
		getSupportActionBar().setDisplayShowHomeEnabled(false);
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);
	}
	
	@Override
	public void onOpen() {
		getSupportActionBar().setDisplayUseLogoEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	private class Holder {
		public CheckBox check;
		TextView from;
		TextView to;
		TextView occurrence;
		History history;
	}
	private class History {
		String toId;
		String fromId;
		String departName;
		String arriveName;
		int count;
	}
	
	private static class SimpleAdapter extends BaseAdapter {
		
		private List<String> choices;
		
		public SimpleAdapter(List<String> choices) {
			this.choices = choices;
		}
		
		@Override
		public int getCount() {
			return choices.size();
		}

		@Override
		public String getItem(int position) {
			return choices.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.spinner_item, null);
			TextView t = (TextView) convertView.findViewById(android.R.id.text1);
			t.setText(getItem(position));
			return convertView;
		}

		@Override
		public View getDropDownView(int position, View convertView,
				ViewGroup parent) {
			convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.spinner_item_dropdown, null);
			TextView t = (TextView) convertView.findViewById(android.R.id.text1);
			t.setText(getItem(position));
			return convertView;
		}
	}
}
