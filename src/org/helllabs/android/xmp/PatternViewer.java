package org.helllabs.android.xmp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.Log;
import android.view.SurfaceHolder;

// http://developer.android.com/guide/topics/graphics/2d-graphics.html

public class PatternViewer extends Viewer implements SurfaceHolder.Callback {
	private Context context;
	private static final byte noteMap[] = new byte[32 * 256];

	//class PatternThread extends Thread {
	private SurfaceHolder surfaceHolder;        
	private int canvasHeight, canvasWidth;
	private Paint notePaint, barPaint;
	private int fontSize;


	/* Callback invoked when the surface dimensions change. */
	public void setSurfaceSize(int width, int height) {
		// synchronized to make sure these all change atomically
		synchronized (surfaceHolder) {
			Log.i("Xmp PatternViewer", "width=" + width + " height=" + height);
			canvasWidth = width;
			canvasHeight = height;
		}
	}

	@Override
	public void update(Info info) {
		super.update(info);
		
		Canvas c = null;
		try {
			c = surfaceHolder.lockCanvas(null);
			synchronized (surfaceHolder) {
				doDraw(c);
			}
		} finally {
			// do this in a finally so that if an exception is thrown
			// during the above, we don't leave the Surface in an
			// inconsistent state
			if (c != null) {
				surfaceHolder.unlockCanvasAndPost(c);
			}
		}
	}


	private void doDraw(Canvas canvas) {
		int lines = canvasHeight / fontSize;
		int barY = (lines / 2) * fontSize;
		Rect rect;

		//canvas.drawLine(10, 10, 20, 20, notePaint);
		rect = new Rect(0, barY - fontSize, canvasWidth - 1, barY);

		canvas.drawRect(rect, barPaint);
		for (int i = 0; i < lines; i++) {
			canvas.drawText("C#2 D 3 --- --- A 3 G#2 --- === --- C#7", 0, (i + 1) * fontSize, notePaint);
		}
	}


	public PatternViewer(Context context) {
		super(context);

		Log.d("Xmp PatternViewer", "PatternViewer constructor");

		// register our interest in hearing about changes to our surface
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		this.surfaceHolder = surfaceHolder;
		this.context = context;

		fontSize = 10;

		notePaint = new Paint();
		notePaint.setARGB(255, 140, 140, 160);
		notePaint.setTypeface(Typeface.MONOSPACE);
		notePaint.setTextSize(fontSize);

		barPaint = new Paint();
		barPaint.setARGB(255, 40, 40, 40);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		setSurfaceSize(width, height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d("Xmp PatternViewer", "surfaceCreated");		
		surfaceHolder = holder;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d("Xmp PatternViewer", "surfaceDestroyed");
	}
}
