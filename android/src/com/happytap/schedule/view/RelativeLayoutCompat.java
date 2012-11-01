package com.happytap.schedule.view;

import android.content.Context;
import android.util.AttributeSet;

public class RelativeLayoutCompat extends android.widget.RelativeLayout {

	public interface OnLayoutChangeListener {
		/**
		 * Called when the focus state of a view has changed.
		 * 
		 * @param v
		 *            The view whose state has changed.
		 * @param left
		 *            The new value of the view's left property.
		 * @param top
		 *            The new value of the view's top property.
		 * @param right
		 *            The new value of the view's right property.
		 * @param bottom
		 *            The new value of the view's bottom property.
		 */
		void onLayoutChange(int left, int top, int right, int bottom);
	}

	private OnLayoutChangeListener layoutChangeListener;

	public RelativeLayoutCompat(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public RelativeLayoutCompat(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public RelativeLayoutCompat(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public void setOnLayoutChangeListener(
			OnLayoutChangeListener layoutChangeListener) {
		this.layoutChangeListener = layoutChangeListener;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
		super.onLayout(changed, l, t, r, b);
		if (changed && layoutChangeListener != null) {
			layoutChangeListener.onLayoutChange(l, t, r, b);
		}
	}
}
