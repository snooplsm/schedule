package com.slidingmenu.lib;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;

public class CustomViewBehind extends CustomViewAbove {

	private static final String TAG = "CustomViewBehind";

	public CustomViewBehind(Context context) {
		this(context, null);
	}

	public CustomViewBehind(Context context, AttributeSet attrs) {
		super(context, attrs, false);
	}

	public int getDestScrollX() {
		if (isMenuOpen()) {
			return getBehindWidth();
		} else {
			return 0;
		}
	}

	public int getChildLeft(int i) {
		return 0;
	}

	public int getChildRight(int i) {
		return getChildLeft(i) + getChildWidth(i);
	}

	public boolean isMenuOpen() {
		return getScrollX() == 0;
	}

	public int getCustomWidth() {
		int i = isMenuOpen() ? 0 : 1;
		return getChildWidth(i);
	}

	public int getChildWidth(int i) {
		if (i <= 0) {
			return getBehindWidth();
		} else {
			return getChildAt(i).getMeasuredWidth();
		}
	}

	public int getBehindWidth() {
		ViewGroup.LayoutParams params = getLayoutParams();
		return params.width;
	}

	@Override
	public void setContent(View v) {
		super.setMenu(v);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		System.out.println("behind onInterceptTouchEvent");
		int action = ev.getAction();
		if (action == MotionEvent.ACTION_DOWN) {
			mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
			mLastX = ev.getX();
			if (mVelocityTracker == null) {
				mVelocityTracker = VelocityTracker.obtain();
			}
			mVelocityTracker.addMovement(ev);
		} else if (action == MotionEvent.ACTION_MOVE) {
			mVelocityTracker.addMovement(ev);
			mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
			int initialVelocity = (int) VelocityTrackerCompat.getXVelocity(
					mVelocityTracker, mActivePointerId);
			int initialYVelocity = (int) VelocityTrackerCompat.getYVelocity(mVelocityTracker, mActivePointerId);
			System.out.println(initialVelocity + ","+initialYVelocity);
			System.out.println("whaasssttt");
			if(Math.abs(initialVelocity)>100) {
				if(Math.abs(initialYVelocity) < 100) {
					System.out.println("intercepting");
					return true;
				}
			}
			// Scroll to follow the motion event

		}	
		return false;
	}

	float mLastX;

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		return mViewAbove.onTouchEvent(ev);
	}

	CustomViewAbove mViewAbove;
	
	public void setCustomViewAbove(CustomViewAbove mViewAbove) {
		this.mViewAbove = mViewAbove;
	}

}
