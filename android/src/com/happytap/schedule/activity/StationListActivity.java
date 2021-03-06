package com.happytap.schedule.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.happytap.jumper.JumpDialog;
import com.happytap.jumper.JumpListener;
import com.happytap.schedule.adapter.StationAdapter;
import com.happytap.schedule.database.StationDao;
import us.wmwm.njrail.R;

public class StationListActivity extends ScheduleActivity implements OnItemLongClickListener, OnItemClickListener, JumpListener {
	
	@Inject
	SharedPreferences preferences;
	
	String currentStopId;
	
	String currentStopName;
	
	@Inject
	Injector injector;
	
	@Inject
	StationDao stationDao;
	
	private static final int DIALOG_JUMP = 1;
	
	@Override
	protected Dialog onCreateDialog(int id) {
		if(id==DIALOG_JUMP) {
			JumpDialog d = new JumpDialog(this, this);
			d.setColorStateList(getResources().getColorStateList(R.drawable.color_selector));
			StationAdapter a = (StationAdapter)getListAdapter();			
			d.setEnabledCharacters(a.getFirstLetterOfItems());
			return d;
		}
		return super.onCreateDialog(id);
	}
	
	private void setListAdapter(ListAdapter adapter) {
		getListView().setAdapter(adapter);
	}
	
	private ListAdapter getListAdapter() {
		return getListView().getAdapter();
	}
	
	private ListView getListView() {
		return (ListView)findViewById(R.id.list);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.stations);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(getString(R.string.activity_title_stations));
		setListAdapter(new StationAdapter(this, stationDao));
		getListView().setOnItemLongClickListener(this);
		getListView().setOnItemClickListener(this);
		registerForContextMenu(getListView());
		if(getSharedPreferences("com.happytap.schedule_preferences", Context.MODE_PRIVATE).getBoolean("showJumper", true)) {
			showDialog(DIALOG_JUMP);
		}		
	}
	
	MenuItem favorite;
	MenuItem hide;
	MenuItem unfavorite;
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		if(!preferences.contains("f_"+currentStopId)) {
			favorite = menu.add("Add as Favorite");
			favorite.setVisible(false);
		} else {
			unfavorite = menu.add("Remove from Favorites");
			unfavorite.setVisible(false);
		}
		//hide = menu.add("Hide");
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if(item.equals(hide)) {
			preferences.edit().putString("h_"+currentStopId,currentStopId).commit();
			setListAdapter(injector.getInstance(StationAdapter.class));
		}
		if(item.equals(favorite)) {
			preferences.edit().putString("f_"+currentStopId,currentStopId).commit();
		}
		if(item.equals(unfavorite)) {
			preferences.edit().remove("f_"+currentStopId).commit();
		}
		return true;
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> adapter, View view, int position,
			long id) {
		getCurrentStopId(adapter,position);
		openContextMenu(adapter);		
		return true;
	}
	
	com.actionbarsherlock.view.MenuItem jumper;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		jumper = menu.add("Jumper");
		jumper.setIcon(R.drawable.ic_action_grid);
		jumper.setShowAsAction(com.actionbarsherlock.view.MenuItem.SHOW_AS_ACTION_ALWAYS);
		return super.onCreateOptionsMenu(menu);

	}
	
	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		if(item.equals(jumper)) {
			showDialog(DIALOG_JUMP);
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int index, long id) {
		setResultAndFinish(adapter,(TextView)view, index);
	}

	private void getCurrentStopId(AdapterView<?> adapter, int index) {
		Cursor cursor = (Cursor)adapter.getItemAtPosition(index);
		currentStopId = cursor.getString(cursor.getColumnIndex("_id"));
		currentStopName = cursor.getString(cursor.getColumnIndex("name"));
	}
	
	public static final String STOP_ID = "stop_id";
	public static final String STOP_NAME = "stop_name";
	
	private void setResultAndFinish(AdapterView<?> adapter, TextView view, int index) {
		getCurrentStopId(adapter,index);
		Intent intent = new Intent();
		intent.putExtra(STOP_ID, currentStopId);
		intent.putExtra(STOP_NAME, view.getText().toString());
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	public void onJump(Character c) {		
		StationAdapter a = (StationAdapter)getListAdapter();
		getListView().setSelection(a.findNearestPosition(c));
	}
	
}
