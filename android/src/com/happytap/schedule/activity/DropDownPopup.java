package com.happytap.schedule.activity;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.PopupWindow;

import com.actionbarsherlock.internal.widget.IcsListPopupWindow;

public class DropDownPopup extends IcsListPopupWindow {
	
    private int mDropDownWidth;
    private View anchor;
    
    public DropDownPopup(Context context, View anchor) {
        super(context);
        this.anchor = anchor;
        setAnchorView(anchor);
        //setModal(true);
        //setPromptPosition(position)
        //setPromptPosition(POSITION_PROMPT_ABOVE);
		setBackgroundDrawable(new BitmapDrawable());
		//set
        setOnItemClickListener(new OnItemClickListener() {
            @SuppressWarnings("rawtypes")
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                //IcsSpinner.this.setSelection(position);
                dismiss();
            }
        });
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
    }

    @Override
    public void show() {
//        final int spinnerPaddingLeft = anchor.getPaddingLeft();
//        if (mDropDownWidth == WRAP_CONTENT) {
//            final int spinnerWidth = anchor.getWidth();
//            final int spinnerPaddingRight = anchor.getPaddingRight();
//            setContentWidth(Math.max(
//                    anchor.measureContentWidth((SpinnerAdapter) mAdapter, anchor.getBackground()),
//                    spinnerWidth - spinnerPaddingLeft - spinnerPaddingRight));
//        } else if (mDropDownWidth == MATCH_PARENT) {
//            final int spinnerWidth = IcsSpinner.this.getWidth();
//            final int spinnerPaddingRight = IcsSpinner.this.getPaddingRight();
//            setContentWidth(spinnerWidth - spinnerPaddingLeft - spinnerPaddingRight);
//        } else {
//            setContentWidth(mDropDownWidth);
//        }
//        final Drawable background = getBackground();
//        int bgOffset = 0;
//        if (background != null) {
//            background.getPadding(mTempRect);
//            bgOffset = -mTempRect.left;
//        }
//        setHorizontalOffset(bgOffset + spinnerPaddingLeft);
    	//setInputMethodMode(/PopupWindow.IN)
        //setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        super.show();
        //getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        //setSelection(IcsSpinner.this.getSelectedItemPosition());
    }
}
