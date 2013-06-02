package com.happytap.schedule.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.PictureDrawable;
import android.graphics.drawable.StateListDrawable;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;
import us.wmwm.njrail.R;

public class SwitchDrawable extends StateListDrawable {

	private PictureDrawable a,c;
	
	public SwitchDrawable(Resources resources) {
		InputStream is = resources.openRawResource(R.raw.reload);
		BufferedReader r = null;
		
		StringBuilder b = new StringBuilder();
		try {
			r = new BufferedReader(new InputStreamReader(is));
			String line = null;
			while((line = r.readLine())!=null){
				b.append(line+"\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				r.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		String val = "#"+Integer.toHexString(resources.getColor(R.color.get_stations)).substring(2);
		String xml = b.toString().replaceAll("#b3b3b3", val);
		SVG svg = SVGParser.getSVGFromResource(resources, R.raw.reload);
		SVG selected = SVGParser.getSVGFromString(xml);
		a = svg.createPictureDrawable();
		c = selected.createPictureDrawable();
		addState(new int[] {}, a);
		addState(new int[] { android.R.attr.state_focused },
				c);
		addState(new int[] { android.R.attr.state_selected },
				c);
		addState(new int[] { android.R.attr.state_pressed },
				c);
	}

	@Override
	public void setBounds(int left, int top, int right, int bottom) {
		// TODO Auto-generated method stub
		super.setBounds(left, top, right, bottom);
		a.setBounds(left, top, right, bottom);
		c.setBounds(left, top, right, bottom);
		System.out.println("setBounds(l,t,r,b)");
	}

	@Override
	public void setBounds(Rect bounds) {
		// TODO Auto-generated method stub
		super.setBounds(bounds);
		a.setBounds(bounds);
		c.setBounds(bounds);
		System.out.println("setBounds"+bounds);
	}
	
	
}
