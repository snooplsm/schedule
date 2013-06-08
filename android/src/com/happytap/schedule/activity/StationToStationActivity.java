package com.happytap.schedule.activity;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import roboguice.inject.InjectView;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdRequest.ErrorCode;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.google.inject.Inject;
import com.googlecode.android.widgets.DateSlider.DateSlider;
import com.googlecode.android.widgets.DateSlider.DateSlider.OnDateSetListener;
import com.googlecode.android.widgets.DateSlider.DateTimeSlider;
import com.happytap.schedule.adapter.ScheduleAdapter;
import com.happytap.schedule.domain.Schedule;
import com.happytap.schedule.domain.StationToStation;
import com.happytap.schedule.domain.TrainStatus;
import com.happytap.schedule.provider.CurrentScheduleProvider;
import com.happytap.schedule.service.BillingService;
import com.happytap.schedule.service.BillingService.RequestPurchase;
import com.happytap.schedule.service.BillingService.RestoreTransactions;
import com.happytap.schedule.service.Consts;
import com.happytap.schedule.service.Consts.PurchaseState;
import com.happytap.schedule.service.Consts.ResponseCode;
import com.happytap.schedule.service.DepartureVision;
import com.happytap.schedule.service.DepartureVision.TrainStatusListener;
import com.happytap.schedule.service.PurchaseDatabase;
import com.happytap.schedule.service.PurchaseObserver;
import com.happytap.schedule.service.ResponseHandler;
import com.happytap.schedule.util.date.DateUtils;
import com.happytap.schedule.view.AdPopupView;
import com.happytap.schedule.view.FarePopupView;
import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;
import us.wmwm.njrail.R;

public class StationToStationActivity extends ScheduleActivity implements
		OnItemLongClickListener, OnItemClickListener, OnClickListener,
		OnMenuItemClickListener {

	@InjectView(android.R.id.list)
	ListView listView;

	ScheduleAdapter adapter;

	@InjectView(R.id.ad_layout)
	LinearLayout adLayout;

	@InjectView(R.id.ad_fodder)
	View adFodder;

	Handler mHandler = new Handler();;

	@Inject
	CurrentScheduleProvider scheduleProvider;

	private BillingService mBillingService;

	String departureText;

	String arrivalText;

	@Inject
	AlarmManager alarmManager;

	@Inject
	NotificationManager notifications;

	AdView adView;

	public static final String SCHEDULE = Schedule.class.getName();
	public static final String DEPARTURE_STATION = "departure_station";
	public static final String ARRIVAL_STATION = "arrival_station";
	public static final String DEPARTURE_ID = "departure_id";
	public static final String ALARM_TRIP_ID = "alarm_trip_id";
	public static final String ARRIVAL_ID = "arrival_id";
	public static final String DEPARTURE_DATE_START = "departure_date_start";
	public static final String DEPARTURE_DATE_END = "departure_date_end";

	private String departureStopId;
	private String arrivalStopId;

	private AsyncTask<Void, TrainStatus, Void> departureVisionTask;

	private boolean useDepartureVision() {
		Calendar start = Calendar.getInstance();
		start.setTime(schedule.start);
		return getSharedPreferences(
				getApplication().getPackageName() + "_preferences",
				Context.MODE_PRIVATE).getBoolean("useDepartureVision", true)
				&& DateUtils.isToday(start);

	}

	private boolean showAds() {
		if (getResources().getBoolean(R.bool.paidApp)) {
			return false;
		}
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				"showAds", true);
	}

	private void setShowAds(boolean show) {
		PreferenceManager.getDefaultSharedPreferences(this).edit()
				.putBoolean("showAds", show).commit();
	}

	boolean paused;

	private AsyncTask<Void, Integer, Void> last;

	protected void onResume() {
		super.onResume();
		paused = false;
		ScheduleAdapter adapter = (ScheduleAdapter)listView.getAdapter();
		if(adapter==null) {
			return;
		}
		adapter.setTripIdForAlarm(PreferenceManager
				.getDefaultSharedPreferences(StationToStationActivity.this)
				.getString("alarm", null));
		last = new AsyncTask<Void, Integer, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					while (!isCancelled()) {
						Calendar c = Calendar.getInstance();
						c.add(Calendar.MINUTE, 1);
						c.set(Calendar.SECOND, 0);
						long diff = c.getTimeInMillis()
								- System.currentTimeMillis();
						if (diff <= 0) {
							diff = 20000;
						}
						Thread.sleep(diff);
						publishProgress(1);
					}
				} catch (Exception e) {

				}
				return null;
			}

			@Override
			protected void onProgressUpdate(Integer... values) {
				ScheduleAdapter adapter = (ScheduleAdapter) listView.getAdapter();
				adapter.notifyDataSetChanged();
			}
		};
		adapter.notifyDataSetChanged();
		if (!useDepartureVision()) {
			return;
		}
		departureVisionTask = newDepartureVisionTask();
		departureVisionTask.execute();
	}

	private AsyncTask<Void, TrainStatus, Void> newDepartureVisionTask() {
		return new AsyncTask<Void, TrainStatus, Void>() {

			private DepartureVision vision;

			@Override
			protected Void doInBackground(Void... arg0) {

				vision = new DepartureVision();
				vision.addListener(new TrainStatusListener() {

					@Override
					public void onTrainStatus(TrainStatus status) {
						publishProgress(status);
					}

				});
				try {
					String stationId = departureStopId;
					String[] abbreviatedNames = getResources().getStringArray(
							R.array.abbreviated_names);
					String start = stationId + ",";
					String[] abbreviatedName = null;
					for (String name : abbreviatedNames) {
						if (name.startsWith(start)) {
							abbreviatedName = name.split(",");
							break;
						}
					}
					if (abbreviatedName != null && abbreviatedName.length > 1) {
						vision.startDepartures(schedule, abbreviatedName);
					}
				} catch (Exception e) {
				}
				return null;
			}

			protected void onProgressUpdate(TrainStatus... values) {
				ScheduleAdapter adapter = (ScheduleAdapter) listView
						.getAdapter();
				adapter.onStatus(values[0]);
			};

			protected void onCancelled() {
				if (vision != null) {
					vision.cancel();
				}
			}
		};
	}

	protected void onPause() {
		super.onPause();
		if (last != null) {
			last.cancel(false);
		}
		if (!useDepartureVision()) {
			return;
		}
		departureVisionTask.cancel(true);
	}

	MenuItem shareItem;

	MenuItem rate;

	MenuItem email;

	private boolean purchasedAdFree = false;

	private class SchedulePurchaseObserver extends PurchaseObserver {
		public SchedulePurchaseObserver(Handler handler) {
			super(StationToStationActivity.this, handler);
		}

		@Override
		public void onBillingSupported(boolean supported, String type) {
			// TODO Auto-generated method stub
			if (supported) {
				restoreDatabase();
			}
		}

		private void restoreDatabase() {
			mBillingService.restoreTransactions();
		}

		@Override
		public void onPurchaseStateChange(PurchaseState purchaseState,
				String itemId, int quantity, long purchaseTime,
				String developerPayload) {
			// TODO Auto-generated method stub
			if ("remove.ads".equals(itemId)) {
				if (purchaseState == PurchaseState.PURCHASED) {
					purchasedAdFree = true;
				}
				if (purchaseState == PurchaseState.CANCELED) {
					purchasedAdFree = false;
				}
				if (purchaseState == PurchaseState.REFUNDED) {
					purchasedAdFree = false;
				}
			}
			if (purchasedAdFree == false) {
				if ("remove.ads.subscription".equals(itemId)
						|| "remove_ads_monthly".equals(itemId)) {
					if (purchaseState == PurchaseState.PURCHASED) {
						purchasedAdFree = true;
					}
					if (purchaseState == PurchaseState.CANCELED) {
						purchasedAdFree = false;
					}
					if (purchaseState == PurchaseState.REFUNDED) {
						purchasedAdFree = false;
					}
				}
			}
			onPurchaseStateChanged();
		}

		@Override
		public void onRequestPurchaseResponse(RequestPurchase request,
				ResponseCode responseCode) {
			System.out.println("what, " + request + ", " + responseCode);
		}

		@Override
		public void onRestoreTransactionsResponse(RestoreTransactions request,
				ResponseCode responseCode) {
			System.out.println("foo " + request + ", " + responseCode);

		}
	}

	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// shareItem = menu.add("Share");
	// shareItem.setIcon(getResources().getDrawable(
	// android.R.drawable.ic_menu_share));
	// rate = menu.add("Rate");
	// rate.setIcon(R.drawable.ic_menu_star);
	// email = menu.add("Email us");
	// email.setIcon(android.R.drawable.ic_menu_send);
	// purchases = menu.add("Remove Ads");
	// return true;
	// }

	com.actionbarsherlock.view.MenuItem rates, emails, purchase, shares,
			reverse;

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		reverse = menu.add("Reverse").setIcon(R.drawable.ic_refresh);
		reverse.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		shares = menu.add("Share").setIcon(R.drawable.ic_share);
		shares.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		// ShareActionProvider actionProvider = new ShareActionProvider(this);

		// shares.setActionProvider(new ActionProvider(this) {
		//
		// @Override
		// public boolean hasSubMenu() {
		// return true;
		// }
		//
		// @Override
		// public View onCreateActionView() {
		// // TODO Auto-generated method stub
		// TypedValue outTypedValue = new TypedValue();
		// StationToStationActivity.this.getTheme().resolveAttribute(R.attr.actionModeShareDrawable,
		// outTypedValue, true);
		// Drawable drawable =
		// StationToStationActivity.this.getResources().getDrawable(outTypedValue.resourceId);
		// RelativeLayout l = new RelativeLayout(StationToStationActivity.this);
		// RelativeLayout.LayoutParams lp = new
		// RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
		// LayoutParams.WRAP_CONTENT);
		// ImageView v = new ImageView(StationToStationActivity.this);
		// lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		// lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		// v.setImageDrawable(drawable);
		// l.addView(v,lp);
		// return l;
		// }
		//
		// });
		// shares.
		// actionProvider.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
		// Note that you can set/change the intent any time,
		// say when the user has selected an image.
		// actionProvider.setShareIntent(share());
		rates = menu.add("Rate").setIcon(R.drawable.ic_star);
		rates.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		emails = menu.add("Email us for help").setIcon(R.drawable.ic_email);
		emails.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		purchase = menu.add("Purchases").setIcon(R.drawable.ic_action_dollar);
		purchase.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return super.onCreateOptionsMenu(menu);

	}

	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		if (item.equals(reverse)) {
			reverse();
		}
		if (item.equals(shares)) {
			startActivity(share());
		}
		if (item.equals(rates)) {
			rate();
		}
		if (item.equals(emails)) {
			email();
		}
		if (item.equals(purchase)) {
			showPayloadEditDialog();
		}
		return super.onOptionsItemSelected(item);
	}

	private void rate() {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(
				R.string.marketUrl, getPackageName())));
		startActivity(intent);
	}

	private void email() {
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("plain/text");
		i.putExtra(Intent.EXTRA_EMAIL,
				new String[] { "njrail-feedback@wmwm.us" });
		StringBuilder b = new StringBuilder("NJ Rail Feedback "
				+ getIntent().getStringExtra(DEPARTURE_STATION) + " : "
				+ getIntent().getStringExtra(ARRIVAL_STATION));
		try {
			b.append(" version:"
					+ getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		} catch (Exception e) {

		}
		i.putExtra(Intent.EXTRA_SUBJECT, b.toString());
		i.putExtra(Intent.EXTRA_TEXT, "");
		if (getPackageManager().resolveActivity(i, 0) != null) {
			startActivity(i);
		} else {
			Toast.makeText(
					this,
					"Sorry, you do not have an email client on your device.  Email us@wmwm.us",
					Toast.LENGTH_LONG).show();
		}

	}

	private Intent share() {
		Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		Resources r = getResources();
		String url = r.getString(R.string.application_url);
		String name = r.getString(R.string.app_name);
		String date = new SimpleDateFormat("MMMM dd, yyyy")
				.format(schedule.end);
		String depart = getIntent().getStringExtra(DEPARTURE_STATION);
		String arrive = getIntent().getStringExtra(ARRIVAL_STATION);
		shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "[" + name
				+ "] " + depart + " to " + arrive + " " + date);
		StringBuilder b = new StringBuilder();
		b.append(depart + " to " + arrive + " for " + date).append("\n\n");
		ScheduleAdapter adapter = (ScheduleAdapter) listView.getAdapter();
		DateFormat df = new SimpleDateFormat("MM/dd/yy");
		boolean tomorrow = false;
		for (int i = 0; i < adapter.getCount(); i++) {
			StationToStation sts = adapter.getItem(i);
			if (sts == null) {
				continue;
			}
			if (!DateUtils.isToday(sts.departTime)) {
				if (!tomorrow) {
					b.append('\n');
				}
				tomorrow = true;
			}
			b.append(ScheduleAdapter.time(sts));
			if (!DateUtils.isToday(sts.departTime)) {
				b.append(" (").append(df.format(sts.departTime.getTime()))
						.append(")");
			}
			b.append('\n');
		}
		shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, b.toString());
		return shareIntent;
		// startActivity(Intent.createChooser(shareIntent, "Share"));
	}

	private String mPayloadContents = null;

	private void showPayloadEditDialog() {
		// mBillingService.ch
		mBillingService.requestPurchase("remove_ads_monthly",
				Consts.ITEM_TYPE_SUBSCRIPTION, null);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (listView != null && listView.getAdapter() != null
				&& listView.getAdapter().getCount() > 0) {
			rates.setVisible(true);
			shares.setVisible(true);
			ScheduleAdapter adapter = (ScheduleAdapter) listView.getAdapter();
			if (clearAlarm != null) {
				if (adapter.getTripIdForAlarm() != null) {
					clearAlarm.setVisible(true);

				} else {
					clearAlarm.setVisible(true);

				}
			}
			if (purchase != null) {
				purchase.setVisible(showAds());
			}
		} else {

			shares.setVisible(false);
			rates.setVisible(false);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	// @Override
	// public boolean onPrepareOptionsMenu(Menu menu) {
	// if (listView != null && listView.getAdapter() != null
	// && listView.getAdapter().getCount() > 0) {
	// rate.setVisible(true);
	// shareItem.setVisible(true);
	// ScheduleAdapter adapter = (ScheduleAdapter) listView.getAdapter();
	// if (clearAlarm != null) {
	// if (adapter.getTripIdForAlarm() != null) {
	// clearAlarm.setVisible(true);
	// } else {
	// clearAlarm.setVisible(false);
	// }
	// }
	// purchases.setVisible(showAds());
	// } else {
	//
	// shareItem.setVisible(false);
	// rate.setVisible(false);
	// }
	// return true;
	// }

	// @Override
	// public boolean onMenuItemSelected(int featureId, MenuItem item) {
	// if (item.equals(shareItem)) {
	// Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
	// shareIntent.setType("text/plain");
	// Resources r = getResources();
	// String url = r.getString(R.string.application_url);
	// String name = r.getString(R.string.app_name);
	// String date = new SimpleDateFormat("MMMM dd, yyyy")
	// .format(schedule.end);
	// String depart = getIntent().getStringExtra(DEPARTURE_STATION);
	// String arrive = getIntent().getStringExtra(ARRIVAL_STATION);
	// shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "["
	// + name + "] " + depart + " to " + arrive + " " + date);
	// StringBuilder b = new StringBuilder();
	// b.append(depart + " to " + arrive + " for " + date).append("\n\n");
	// ScheduleAdapter adapter = (ScheduleAdapter) listView.getAdapter();
	// DateFormat df = new SimpleDateFormat("MM/dd/yy");
	// boolean tomorrow = false;
	// for (int i = 0; i < adapter.getCount(); i++) {
	// StationToStation sts = adapter.getItem(i);
	// if (!DateUtils.isToday(sts.departTime)) {
	// if (!tomorrow) {
	// b.append('\n');
	// }
	// tomorrow = true;
	// }
	// b.append(ScheduleAdapter.time(sts));
	// if (!DateUtils.isToday(sts.departTime)) {
	// b.append(" (").append(df.format(sts.departTime.getTime()))
	// .append(")");
	// }
	// b.append('\n');
	// }
	// shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,
	// b.toString());
	// startActivity(Intent.createChooser(shareIntent, "Share"));
	// return true;
	// }
	// if (item.equals(share)) {
	// Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
	// String depart = getIntent().getStringExtra(DEPARTURE_STATION);
	// String arrive = getIntent().getStringExtra(ARRIVAL_STATION);
	// shareIntent.setType("text/plain");
	// shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, depart
	// + " to " + arrive + " " + currentItemDescription);
	// shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, depart
	// + " to " + arrive + " " + currentItemDescription);
	// startActivity(Intent.createChooser(shareIntent, "Share"));
	// return true;
	// }
	// if (item.equals(alarmArrive)) {
	// showDialog(DIALOG_ARRIVE);
	// return true;
	// }
	// if (item.equals(alarmDepart)) {
	// showDialog(DIALOG_DEPART);
	// return true;
	// }
	// if (item.equals(rate)) {
	// Intent intent = new Intent(Intent.ACTION_VIEW,
	// Uri.parse(getString(R.string.marketUrl,getPackageName())));
	// startActivity(intent);
	// return true;
	// }
	// if (item.equals(email)) {
	// Intent i = new Intent(Intent.ACTION_SEND);
	// i.setType("plain/text");
	// i.putExtra(Intent.EXTRA_EMAIL,
	// new String[] { "njtransitrail-feedback@wmwm.us" });
	// StringBuilder b = new StringBuilder("NJTransit Rail Feedback "
	// + getIntent().getStringExtra(DEPARTURE_STATION) + " : "
	// + getIntent().getStringExtra(ARRIVAL_STATION));
	// try {
	// b.append(" version:"
	// + getPackageManager().getPackageInfo(getPackageName(),
	// 0).versionName);
	// } catch (Exception e) {
	//
	// }
	// i.putExtra(Intent.EXTRA_SUBJECT, b.toString());
	// i.putExtra(Intent.EXTRA_TEXT, "");
	// if(getPackageManager().resolveActivity(i, 0)!=null) {
	// startActivity(i);
	// } else {
	// Toast.makeText(this,
	// "Sorry, you do not have an email client on your device.  Email us@wmwm.us",
	// Toast.LENGTH_LONG).show();
	// }
	//
	// return true;
	// }
	// if (item.equals(clearAlarm)) {
	// getAdapter().setTripIdForAlarm(null);
	// String ns = Context.NOTIFICATION_SERVICE;
	// NotificationManager mNotificationManager = (NotificationManager)
	// getSystemService(ns);
	// mNotificationManager.cancel(1);
	// Intent intent = new Intent(StationToStationActivity.this,
	// AlarmActivity.class);
	// PendingIntent pi = PendingIntent.getActivity(
	// StationToStationActivity.this, 1, intent, 0);
	// alarmManager.cancel(pi);
	// adapter.notifyDataSetChanged();
	// }
	// if (item.equals(purchases)) {
	// showPayloadEditDialog();
	// }
	// return false;
	// }

	private void initializeOwnedItems() {
		new Thread(new Runnable() {
			public void run() {
				doInitializeOwnedItems();
			}
		}).start();
	}

	private void doInitializeOwnedItems() {
		Cursor cursor = mPurchaseDatabase.queryAllPurchasedItems();
		if (cursor == null) {
			return;
		}

		try {
			int productIdCol = cursor
					.getColumnIndexOrThrow(PurchaseDatabase.PURCHASED_PRODUCT_ID_COL);
			while (cursor.moveToNext()) {
				String productId = cursor.getString(productIdCol);
				if (productId.equals("remove.ads")
						|| productId.equals("remove.ads.subscription")) {
					purchasedAdFree = true;
				}
			}
		} finally {
			cursor.close();
		}
		onPurchaseStateChanged();
	}

	private void onPurchaseStateChanged() {
		boolean val = purchasedAdFree;
		setShowAds(!purchasedAdFree);
		if (!showAds()) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					adLayout.setVisibility(View.GONE);
				}

			});

		}
	}

	private PurchaseDatabase mPurchaseDatabase;
	private SchedulePurchaseObserver mPurchaseObserver;

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
		outState.putLong("start", schedule.start.getTime());
		outState.putString("departureText", departureText);
		outState.putString("arrivalText", arrivalText);
		outState.putString("departureId", departureStopId);
		outState.putString("arrivalId", arrivalStopId);
	}

	private long start;

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onRestoreInstanceState(savedInstanceState);
		start = savedInstanceState.getLong("start");
		departureText = savedInstanceState.getString("departureText");
		departureStopId = savedInstanceState.getString("departureId");
		arrivalText = savedInstanceState.getString("arrivalText");
		arrivalStopId = savedInstanceState.getString("arrivalId");
	}
	
	private Map<String,Double> fares;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// setTheme(android.R.style.Theme_Light_NoTitleBar);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.station_to_station);
		this.getSupportActionBar().setSubtitle(
				getIntent().getStringExtra(DEPARTURE_STATION) + " \nto\n"
						+ getIntent().getStringExtra(ARRIVAL_STATION));
		// getSupportActionBar().setDisplayShowHomeEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mBillingService = new BillingService();
		mBillingService.setContext(this);
		mPurchaseDatabase = new PurchaseDatabase(this);
		mPurchaseObserver = new SchedulePurchaseObserver(mHandler);
		ResponseHandler.register(mPurchaseObserver);
		adView = new AdView(this, AdSize.SMART_BANNER,
				getString(R.string.publisherId));
		fares = (HashMap<String,Double>)getIntent().getSerializableExtra(LoadScheduleActivity.FARE);
		// if(fare>=0) {
		// this.fare.setText("Fair: " + LoadScheduleActivity.df.format(fare));
		// this.fareContainer.setVisibility(View.VISIBLE);
		// } else {
		// this.fareContainer.setVisibility(View.GONE);
		// }
		if (showAds()) {
			AdRequest req = new AdRequest();
			final View orAd = getLayoutInflater()
					.inflate(R.layout.our_ad, null);
			orAd.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					AdPopupView p = new AdPopupView(v,null);
					p.show();
					//
					//showPayloadEditDialog();
				}
			});
			int rand = 1;
			if (rand == 1) {
				adLayout.addView(orAd);
			}
			adView.setGravity(Gravity.CENTER);
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			lp.gravity = Gravity.CENTER;//
			adLayout.addView(adView, lp);
			adView.loadAd(req);
			adView.setAdListener(new AdListener() {

				@Override
				public void onDismissScreen(Ad arg0) {

				}

				@Override
				public void onFailedToReceiveAd(Ad arg0, ErrorCode arg1) {

				}

				@Override
				public void onLeaveApplication(Ad arg0) {

				}

				@Override
				public void onPresentScreen(Ad arg0) {

				}

				@Override
				public void onReceiveAd(Ad arg0) {
					int index = adLayout.indexOfChild(orAd);
					if (index >= 0) {
						adLayout.removeViewAt(index);
					}
				}

			});
		}
		adFodder.setVisibility(View.GONE);

		schedule = scheduleProvider.get();
		if (schedule != null) {
			new Thread() {
				public void run() {
					Intent it = new Intent(StationToStationActivity.this,
							StationToStationActivity.class);
					it.putExtras(getIntent());
					it.putExtra("schedule", schedule);
					setIntent(it);
				};
			}.start();
		} else {
			schedule = (Schedule) getIntent().getSerializableExtra("schedule");
		}
		if (schedule == null) {
			Intent intent = new Intent(StationToStationActivity.this,
					LoadScheduleActivity.class);
			intent.putExtra(LoadScheduleActivity.DEPARTURE_STATION,
					departureText);
			intent.putExtra(LoadScheduleActivity.ARRIVAL_STATION, arrivalText);
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(start);
			intent.putExtra(LoadScheduleActivity.DEPARTURE_DATE_START, c);
			intent.putExtra(LoadScheduleActivity.DEPARTURE_ID, departureStopId);
			intent.putExtra(LoadScheduleActivity.ARRIVAL_ID, arrivalStopId);
			if (DateUtils.isToday(c)) {
				Calendar tom = Calendar.getInstance();
				tom.setTimeInMillis(c.getTimeInMillis());
				tom.add(Calendar.DAY_OF_YEAR, 1);
				intent.putExtra(LoadScheduleActivity.DEPARTURE_DATE_END, tom);
			} else {
				intent.putExtra(LoadScheduleActivity.DEPARTURE_DATE_END, c);
			}
			startActivity(intent);
			return;
		}
		listView.setAdapter(adapter = new ScheduleAdapter(this, schedule));
		if (getIntent().hasExtra(ALARM_TRIP_ID)) {
			adapter.setTripIdForAlarm(getIntent().getStringExtra(ALARM_TRIP_ID));
		}
		registerForContextMenu(listView);
		listView.setOnItemLongClickListener(this);
		listView.setOnItemClickListener(this);
		int index = adapter.findIndexOfCurrent();
		if (index > 1) {
			if (fares !=null && !fares.isEmpty()) {
				adapter.setFareAnchor(fares, index - 1);
				listView.setSelectionFromTop(index - 1, 0);
			} else {
				listView.setSelectionFromTop(index - 1, 0);
			}

		} else {
			if (fares !=null && !fares.isEmpty()) {
				adapter.setFareAnchor(fares, 0);
			}
		}

		departureStopId = getIntent().getStringExtra(DEPARTURE_ID);
		arrivalStopId = getIntent().getStringExtra(ARRIVAL_ID);
		departureText = getIntent().getStringExtra(DEPARTURE_STATION);
		arrivalText = getIntent().getStringExtra(ARRIVAL_STATION);

		SVG svg = SVGParser.getSVGFromResource(getResources(), R.raw.reload);

		mPurchaseDatabase.queryAllPurchasedItems();
		mBillingService.checkBillingSupported();
	}

	private static final int DIALOG_ARRIVE = 1;
	private static final int DIALOG_DEPART = 2;

	@Override
	protected void onStart() {
		super.onStart();
		ResponseHandler.register(mPurchaseObserver);
		initializeOwnedItems();
	}

	@Override
	protected void onStop() {
		super.onStop();
		ResponseHandler.unregister(mPurchaseObserver);
	}

	@Override
	protected Dialog onCreateDialog(final int id) {
		final StationToStation sts = getAdapter().getItem(currentItemPosition);
		Calendar alarm = id == DIALOG_DEPART ? sts.departTime : sts.arriveTime;
		final Calendar alarmTime = Calendar.getInstance();
		alarmTime.setTimeInMillis(alarm.getTimeInMillis());
		alarmTime.add(Calendar.MINUTE, -1);
		final DateTimeSlider tpd = new DateTimeSlider(this,
				new OnDateSetListener() {

					@Override
					public void onDateSet(DateSlider view, Calendar selectedDate) {
						doAlarm(id == DIALOG_DEPART ? AlarmActivity.TYPE_DEPART
								: AlarmActivity.TYPE_ARRIVE, selectedDate, sts);
						removeDialog(id);
					}

				}, alarmTime) {
			@Override
			protected void setTitle() {
				setTitle(id == DIALOG_DEPART ? ("Set depart alarm " + ScheduleAdapter.time
						.format(sts.departTime.getTime()))
						: ("Arrive alarm " + ScheduleAdapter.time
								.format(sts.arriveTime.getTime())));
			}

			public String getTodayText() {
				return "Revert";
			}

			public android.view.View.OnClickListener newTodayListener() {
				return new android.view.View.OnClickListener() {

					@Override
					public void onClick(View v) {
						setTime(alarmTime);

					}
				};

			}
		};
		tpd.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				removeDialog(id);
			}
		});
		return tpd;
	}

	private Schedule schedule;

	@Override
	protected void onDestroy() {
		super.onDestroy();
		adView.destroy();
		mPurchaseDatabase.close();
		mBillingService.unbind();
	}

	private int currentItemPosition;
	private String currentItemDescription;

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view,
			int position, long id) {
		ScheduleAdapter adapter = (ScheduleAdapter) listView.getAdapter();
		StationToStation sts = adapter.getItem(position);
		if (sts == null) {
			View fare = view.findViewById(R.id.fare);
			if (fare != null) {
				FarePopupView v = new FarePopupView(fare, null, fares,
						"Yeah");
				v.show();
			}
			return;
		}
		Intent intent = new Intent(this, TripActivity.class)
				.putExtra("tripId", sts.tripId)
				.putExtra("start", sts.departTime.getTimeInMillis())
				.putExtra("departId", departureStopId)
				.putExtra("arriveId", arrivalStopId);
		startActivityFromChild(this, intent, 0);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, View view,
			int position, long id) {
		// getCurrentStopId(adapter,position);
		if (adapter.getItem(position) == null) {
			return true;
		}
		currentItemPosition = position;
		currentItemDescription = ""
				+ ((TextView) view.findViewById(R.id.time)).getText();
		openContextMenu(adapterView);
		return true;
	}

	MenuItem alarmDepart;
	MenuItem alarmArrive;
	MenuItem share;
	MenuItem clearAlarm;
	MenuItem purchases;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.setHeaderTitle(currentItemDescription);
		alarmDepart = menu.add("Add Depart Alarm");
		alarmDepart.setOnMenuItemClickListener(this);
		alarmArrive = menu.add("Add Arrive Alarm");
		alarmArrive.setOnMenuItemClickListener(this);
		final StationToStation sts = getAdapter().getItem(currentItemPosition);
		if (sts.arriveTime.before(Calendar.getInstance())) {
			alarmArrive.setVisible(false);
		}
		if (sts.departTime.before(Calendar.getInstance())) {
			alarmDepart.setVisible(false);
		}
		share = menu.add("Share");
		share.setIcon(getResources().getDrawable(
				android.R.drawable.ic_menu_share));
		share.setOnMenuItemClickListener(this);
		clearAlarm = menu.add("Clear Alarm");
		clearAlarm.setIcon(R.drawable.ic_menu_alarms);
		clearAlarm.setOnMenuItemClickListener(this);
		if (adapter.getTripIdForAlarm() != null) {
			clearAlarm.setVisible(true);
			alarmDepart.setVisible(false);
			alarmArrive.setVisible(false);
		} else {
			clearAlarm.setVisible(false);
			alarmDepart.setVisible(true);
			alarmArrive.setVisible(true);
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	void doAlarm(final String type, final Calendar time,
			final StationToStation sts) {
		new AsyncTask<Void, Void, Void>() {

			protected void onPreExecute() {
				ScheduleAdapter adapter = (ScheduleAdapter) listView
						.getAdapter();
				PreferenceManager
						.getDefaultSharedPreferences(
								StationToStationActivity.this).edit()
						.putString("alarm", sts.tripId).commit();
				adapter.setTripIdForAlarm(sts.tripId);
				// listView.invalidateViews();
			};

			@Override
			protected Void doInBackground(Void... params) {
				time.clear(Calendar.SECOND);
				time.clear(Calendar.MILLISECOND);
				Intent intent = new Intent(StationToStationActivity.this,
						AlarmActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				intent.putExtra(AlarmActivity.TYPE, AlarmActivity.TYPE_ARRIVE);
				if (AlarmActivity.TYPE_ARRIVE.equals(type)) {
					intent.putExtra(AlarmActivity.TIME, time);
				} else {
					intent.putExtra(AlarmActivity.TIME, time);
				}
				PendingIntent pi = PendingIntent.getActivity(
						StationToStationActivity.this, 1, intent, 0);
				String ns = Context.NOTIFICATION_SERVICE;
				NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
				long diff = time.getTimeInMillis() - System.currentTimeMillis();
				int hours = (int) (diff / 3600000);
				int mins = (int) (diff % 3600000 / 60000);
				int seconds = (int) (diff % 60000 / 1000);
				String tickerText = "alarm goes off in ";
				if (hours > 0) {
					tickerText += String.valueOf(hours);
					tickerText += "h";
				}
				if (mins > 0) {
					tickerText += String.valueOf(mins);
					tickerText += "m";
				}
				if (seconds > 0) {
					tickerText += String.valueOf(seconds);
					tickerText += "s";
				}
				long when = System.currentTimeMillis();
				Notification notification = new Notification(
						R.drawable.stat_notify_alarm, tickerText, when);
				notification.flags = notification.flags
						| Notification.FLAG_ONGOING_EVENT;
				Context context = getApplicationContext();
				CharSequence contentTitle = getString(R.string.app_name);
				CharSequence contentText = "alarm "
						+ android.text.format.DateFormat.getTimeFormat(
								StationToStationActivity.this).format(
								time.getTime()) + " train #" + sts.blockId;
				Intent notificationIntent = new Intent(
						StationToStationActivity.this,
						StationToStationActivity.class);
				notificationIntent.putExtras(getIntent().getExtras());
				notificationIntent.putExtra("alarm", sts.tripId);
				PendingIntent contentIntent = PendingIntent
						.getActivity(StationToStationActivity.this, 0,
								notificationIntent, 0);

				notification.setLatestEventInfo(context, contentTitle,
						contentText, contentIntent);
				mNotificationManager.notify(1, notification);
				alarmManager.cancel(pi);
				alarmManager.set(AlarmManager.RTC_WAKEUP,
						time.getTimeInMillis(), pi);
				return null;
			}
		}.execute();

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		return false;
	}

	private ScheduleAdapter getAdapter() {
		return (ScheduleAdapter) listView.getAdapter();
	}

	private void reverse() {
		boolean departureVision = useDepartureVision();
		if (departureVision) {
			departureVisionTask.cancel(true);
		}
		if (schedule.transfers.length == 1) {
			String temp = departureText;
			departureText = arrivalText;
			arrivalText = temp;
			String temp2 = departureStopId;
			departureStopId = arrivalStopId;
			arrivalStopId = temp2;
			getSupportActionBar().setSubtitle(
					departureText + " to " + arrivalText);
			getAdapter().reverse();
			int index = getAdapter().findIndexOfCurrent();
			if (index > 0) {
				listView.setSelectionFromTop(index - 1, 0);
			}
			if (departureVision) {
				departureVisionTask = newDepartureVisionTask();
				departureVisionTask.execute();
			}
			return;
		}
		Intent intent = new Intent(StationToStationActivity.this,
				LoadScheduleActivity.class);
		intent.putExtra(LoadScheduleActivity.DEPARTURE_STATION, arrivalText);
		intent.putExtra(LoadScheduleActivity.ARRIVAL_STATION, departureText);
		Calendar c = Calendar.getInstance();
		c.setTime(schedule.start);
		intent.putExtra(LoadScheduleActivity.DEPARTURE_DATE_START, c);
		intent.putExtra(LoadScheduleActivity.DEPARTURE_ID, arrivalStopId);
		intent.putExtra(LoadScheduleActivity.ARRIVAL_ID, departureStopId);
		if (DateUtils.isToday(c)) {
			Calendar tom = Calendar.getInstance();
			tom.setTimeInMillis(c.getTimeInMillis());
			tom.add(Calendar.DAY_OF_YEAR, 1);
			intent.putExtra(LoadScheduleActivity.DEPARTURE_DATE_END, tom);
		} else {
			intent.putExtra(LoadScheduleActivity.DEPARTURE_DATE_END, c);
		}
		startActivity(intent);

	}

	@Override
	public void onClick(View arg0) {
		boolean departureVision = useDepartureVision();
		if (departureVision) {
			departureVisionTask.cancel(true);
		}
		if (schedule.transfers.length == 1) {
			String temp = departureText;
			departureText = arrivalText;
			arrivalText = temp;
			String temp2 = departureStopId;
			departureStopId = arrivalStopId;
			arrivalStopId = temp2;
			getAdapter().reverse();
			int index = getAdapter().findIndexOfCurrent();
			if (index > 0) {
				listView.setSelectionFromTop(index - 1, 0);
			}
			if (departureVision) {
				departureVisionTask = newDepartureVisionTask();
				departureVisionTask.execute();
			}
			return;
		}
		Intent intent = new Intent(StationToStationActivity.this,
				LoadScheduleActivity.class);
		intent.putExtra(LoadScheduleActivity.DEPARTURE_STATION, arrivalText);
		intent.putExtra(LoadScheduleActivity.ARRIVAL_STATION, departureText);
		Calendar c = Calendar.getInstance();
		c.setTime(schedule.start);
		intent.putExtra(LoadScheduleActivity.DEPARTURE_DATE_START, c);
		intent.putExtra(LoadScheduleActivity.DEPARTURE_ID, arrivalStopId);
		intent.putExtra(LoadScheduleActivity.ARRIVAL_ID, departureStopId);
		if (DateUtils.isToday(c)) {
			Calendar tom = Calendar.getInstance();
			tom.setTimeInMillis(c.getTimeInMillis());
			tom.add(Calendar.DAY_OF_YEAR, 1);
			intent.putExtra(LoadScheduleActivity.DEPARTURE_DATE_END, tom);
		} else {
			intent.putExtra(LoadScheduleActivity.DEPARTURE_DATE_END, c);
		}
		startActivity(intent);

	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		if (item.equals(alarmArrive)) {
			showDialog(DIALOG_ARRIVE);
			return true;
		}
		if (item.equals(alarmDepart)) {
			showDialog(DIALOG_DEPART);
			return true;
		}
		if (item.equals(share)) {
			Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
			String depart = getIntent().getStringExtra(DEPARTURE_STATION);
			String arrive = getIntent().getStringExtra(ARRIVAL_STATION);
			shareIntent.setType("text/plain");
			shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, depart
					+ " to " + arrive + " " + currentItemDescription);
			shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, depart
					+ " to " + arrive + " " + currentItemDescription);
			startActivity(Intent.createChooser(shareIntent, "Share"));
			return true;
		}
		if (item.equals(clearAlarm)) {
			PreferenceManager.getDefaultSharedPreferences(this).edit()
					.remove("alarm").commit();
			getAdapter().setTripIdForAlarm(null);
			String ns = Context.NOTIFICATION_SERVICE;
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
			mNotificationManager.cancel(1);
			Intent intent = new Intent(StationToStationActivity.this,
					AlarmActivity.class);
			PendingIntent pi = PendingIntent.getActivity(
					StationToStationActivity.this, 1, intent, 0);
			alarmManager.cancel(pi);
			adapter.notifyDataSetChanged();
		}
		return false;
	}
}
