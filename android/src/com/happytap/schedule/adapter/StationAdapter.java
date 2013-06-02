package com.happytap.schedule.adapter;

import java.util.LinkedHashSet;

import roboguice.inject.ContextSingleton;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.google.inject.Inject;
import com.happytap.schedule.database.StationDao;
import us.wmwm.njrail.R;

public class StationAdapter extends SimpleCursorAdapter {
	
	public StationAdapter(final Context context, StationDao stationDao) {
		super(context, android.R.layout.simple_list_item_1, stationDao.getStations(), new String[] {StationDao.NAME}, new int[] {android.R.id.text1});
		setViewBinder(new ViewBinder() {

			@Override
			public boolean setViewValue(View arg0, Cursor cursor, int position) {
				TextView v = (TextView)arg0;
				v.setText(makePretty(cursor.getString(position)));
				v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
				
				v.setTextColor(context.getResources().getColor(R.color.station_name));
				v.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.station));
				v.setPadding(v.getPaddingLeft()+10, v.getPaddingTop(), v.getPaddingRight()+10, v.getPaddingBottom());
				v.setTypeface(null,Typeface.BOLD);
				return true;
			}
			
		});
	}
	
	public static String makePretty(String str) {
		char lastChar=' ';
		StringBuilder sb = new StringBuilder(str);
		int whitespaceDist = 0;
		for(int i = 0; i < sb.length();i++) {
			char nowChar = sb.charAt(i);
			if(nowChar!=' ' && nowChar!='.') {
				whitespaceDist+=1;
			} else {
				if(whitespaceDist==2) {
					char b = sb.charAt(i-1);
					char a = sb.charAt(i-2);
					sb.setCharAt(i-1, Character.toUpperCase(b));
					sb.setCharAt(i-2, Character.toUpperCase(a));
				}
			}
			if(lastChar==' ' || lastChar=='/') {
				sb.setCharAt(i, Character.toUpperCase(nowChar));
			} else {
				sb.setCharAt(i, Character.toLowerCase(nowChar));
			}
			lastChar = nowChar;
		}
		return sb.toString();
	}

	
	public LinkedHashSet<Character> getFirstLetterOfItems() {
		LinkedHashSet<Character> c = new LinkedHashSet<Character>();
		int pos = getCursor().getColumnIndex(StationDao.NAME);
		for(int i = 0; i < getCount(); i++) {
			Cursor cursor = (Cursor)getItem(i);
			String name = cursor.getString(pos);
			c.add(name.charAt(0));
		}
		return c;		
	}
	
	public int findNearestPosition(Character c) {
		int pos = getCursor().getColumnIndex(StationDao.NAME);
		for(int i = 0; i < getCount(); i++) {
			Cursor cursor = (Cursor)getItem(i);
			String name = cursor.getString(pos);
			int cn = name.charAt(0);
			int cc = c.charValue();	
			if(cn==cc) {
				return i;
			}
			if(cn>cc) {
				return i;
			}
		}
		return 0;
	}

	
}
