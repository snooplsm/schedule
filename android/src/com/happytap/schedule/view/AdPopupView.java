package com.happytap.schedule.view;

import junit.framework.Assert;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.happytap.schedule.view.RelativeLayoutCompat.OnLayoutChangeListener;
import com.njtransit.rail.R;

public class AdPopupView {

	private static final String EXCEPTION_INVALID_CONSTRUCTOR_PARAMS = 
		"The given view anchor must not be null!";
	
	private View _downArrow;
	private View _upArrow;
	private ListView list;
	private RelativeLayoutCompat _root;
	private PopupWindow _window;
	private View _anchor;
	private Rect frame;
	
	public AdPopupView(View anchor, Rect frame) {
		if (anchor == null)
			throw new IllegalArgumentException(EXCEPTION_INVALID_CONSTRUCTOR_PARAMS);
		
		this._anchor = anchor;
		this.frame = frame;
		System.out.println(frame);
		
		// Initialize layout and view references.
		LayoutInflater inflater = LayoutInflater.from(anchor.getContext());
		this._root = (RelativeLayoutCompat) inflater.inflate(com.njtransit.rail.R.layout.view_quick_action_ad, null);
		this.initializeViewReferences(this._root);
		//_blurb.setText(blurb);
		// Configure popup window for the quick action. It should have no background,
		// wrap its content, and close when touch events are dispatched outside of its 
		// content region (achieved by setting focusable).
		
		this._window = new PopupWindow(anchor.getContext());
		this._window.setContentView(this._root);
		this._window.setBackgroundDrawable(new BitmapDrawable());
		this._window.setFocusable(true);
		if(frame!=null) {
			this._window.setWidth(frame.width());
		} else {
			this._window.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
		}
		this._window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
	}

	/**
	 * Dismisses this quick action menu if showing. If not showing,
	 * this method does nothing.
	 */
	protected void dismiss() {
		this._window.dismiss();
	}
	
	/**
	 * Gets the anchor {@link View} for this quick action menu.
	 * @return Anchor {@link View} for this quick action menu.
	 */
	protected View getAnchor() {
		return this._anchor;
	}
	
	/**
	 * Initializes view references for this quick action from the given
	 * root {@link View} of this menu.
	 * @param root Root {@link View} of this menu.
	 */
	private void initializeViewReferences(View root) {
		Assert.assertTrue(root != null);
		
		this._downArrow = 
			 root.findViewById(R.id.QuickActionMenu_downArrow);
		this._upArrow = 
			 root.findViewById(R.id.QuickActionMenu_upArrow);
		//this.list = (ListView) root.findViewById(R.id.list);
		
//		this._itemsContainer = 
//			(LinearLayout) root.findViewById(R.id.QuickActionMenu_itemsContainer);
		//_downArrow.setColorFilter(root.getContext().getResources().getColor(R.color.ClassicQuickActionMenu_background));
//		_upArrow.setColorFilter(root.getContext().getResources().getColor(R.color.ClassicQuickActionMenu_background));
	}
	
	/**
	 * Shows this quick action menu.
	 */
	public void show() {
		
		// Generate a Rect for the coorindate information of the anchor.
		final Rect anchorRect = new Rect();
		_anchor.getGlobalVisibleRect(anchorRect);
		
		this._root.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		this._root.measure(MeasureSpec.makeMeasureSpec(LayoutParams.MATCH_PARENT, MeasureSpec.UNSPECIFIED), 
						   MeasureSpec.makeMeasureSpec(LayoutParams.WRAP_CONTENT, MeasureSpec.UNSPECIFIED));
		int windowContentsHeight = this._root.getMeasuredHeight();
		final int windowContentsWidth = this._root.getMeasuredWidth();
		WindowManager manager = 
				(WindowManager) this._root.getContext().getSystemService(Context.WINDOW_SERVICE);
		final int screenWidth;
		if(frame==null) {
			screenWidth = manager.getDefaultDisplay().getWidth();
		} else {
			screenWidth = frame.width();
		}
		this._root.setOnLayoutChangeListener(new OnLayoutChangeListener() {
			@Override
			public void onLayoutChange(int left, int top, int right,
					int bottom) {
				System.out.println("onLayoutChange... " + top + " " + bottom);
				int windowContentsHeight = bottom - top;
				boolean isOffscreen = isOffscreen(windowContentsHeight, anchorRect);
				showArrow(! isOffscreen, screenWidth - anchorRect.centerX());
				int x = getXLocation(isOffscreen, windowContentsWidth, anchorRect);
				int y = getYLocation(isOffscreen, windowContentsHeight, anchorRect);
				if(frame!=null) {
					x = frame.left + x;
					y = frame.top + y;
				}
				_window.update(x, y, -1, -1);
				
			}
		});
		
		// Get the total screen height.
		
		boolean isOffscreen = isOffscreen(windowContentsHeight, anchorRect);
		
		this.showArrow(! isOffscreen, screenWidth - anchorRect.centerX());
		
		this._root.measure(MeasureSpec.makeMeasureSpec(LayoutParams.MATCH_PARENT, MeasureSpec.UNSPECIFIED), 
				   MeasureSpec.makeMeasureSpec(LayoutParams.WRAP_CONTENT, MeasureSpec.UNSPECIFIED));
		windowContentsHeight = this._root.getMeasuredHeight();
		System.out.println(windowContentsHeight);
		System.out.println(isOffscreen);

		int x = getXLocation(isOffscreen, windowContentsWidth, anchorRect);
		int y = getYLocation(isOffscreen, windowContentsHeight, anchorRect);
		this._window.showAtLocation(this._root, Gravity.NO_GRAVITY, x, y);
		
	}
	
	protected boolean isOffscreen(int windowContentsHeight, Rect anchorRect) {
		return windowContentsHeight + anchorRect.height()/2  > anchorRect.top;
	}
	
	protected int getXLocation(boolean isOffscreen, int windowContentsWidth, Rect anchorRect) {
		return anchorRect.right;
	}
	
	protected int getYLocation(boolean isOffscreen, int windowContentsHeight, Rect anchorRect) {
		if(isOffscreen) {
			return anchorRect.bottom;
		}
		return (anchorRect.top - windowContentsHeight);
	}
	
	private void showArrow(boolean showOnTop, int rightMargin) {
		final View showArrow = (showOnTop) ? this._downArrow : this._upArrow;
        final View hideArrow = (showOnTop) ? this._upArrow : this._downArrow;

        showArrow.setVisibility(View.VISIBLE);
        
        int measuredWidth = showArrow.getWidth();
        int adjustedRightMargin = rightMargin - (measuredWidth / 2);
        
        ViewGroup.MarginLayoutParams param = 
        	(ViewGroup.MarginLayoutParams) showArrow.getLayoutParams();
        param.rightMargin = adjustedRightMargin;
      
        hideArrow.setVisibility(View.INVISIBLE);
	}
	
}