package com.happytap.schedule.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.njtransit.rail.R;

/**
 * A shape that resembles a triangle. The user can form an equalateral triangle
 * if they make height and width the same size. The triangle can be rotated to
 * point up or down. Currently no need for 360degs of rotation.
 * 
 * 
 */
public class Triangle extends View {

	private Paint paint;
	
	private Paint paintStroke;

	private int rotate = 0;
	
	private Path path;
	
	private int borderColor = Color.TRANSPARENT;
	private int color = Color.BLACK;

	public Triangle(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		if(Build.VERSION.SDK_INT>Build.VERSION_CODES.HONEYCOMB) {
			setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		}
	}

	public Triangle(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if(attrs!=null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Triangle,
	                defStyle, 0);
					color = a.getColor(R.styleable.Triangle_color, Color.BLACK);
					borderColor = a.getColor(R.styleable.Triangle_borderColor, Color.TRANSPARENT);
					rotate = a.getInt(R.styleable.Triangle_rotate, 0);
			a.recycle();
		}
	}

	public Triangle(Context context) {
		this(context,null,0);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		if (paint == null) {
			paint = new Paint();
			paint.setAntiAlias(true);
			paint.setStyle(Paint.Style.FILL_AND_STROKE);
			paint.setColor(color);
			paintStroke = new Paint();
			paintStroke.setAntiAlias(true);
			paintStroke.setStyle(Paint.Style.STROKE);
			paintStroke.setColor(borderColor);
			paintStroke.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
			path = new Path();
		}
		int height = getHeight();
		int width = getWidth();
		int topXA = width / 2;
		path.moveTo(topXA, 0);
		path.lineTo(0, height);
		path.lineTo(width, height);
		path.lineTo(topXA, 0);
		path.close();
		if (rotate%360!=0) {
			canvas.rotate(rotate, width / 2, height / 2);
		}
		canvas.drawPath(path, paint);
		path.reset();
		path.moveTo(0, height);
		canvas.drawLine(topXA, 0, 0, height, paintStroke);
		canvas.drawLine(topXA, 0, width, height, paintStroke);
		path.reset();
	}

}
