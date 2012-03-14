package org.helllabs.android.xmp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.Log;
import android.view.SurfaceHolder;

// http://developer.android.com/guide/topics/graphics/2d-graphics.html

public class PatternViewer extends Viewer implements SurfaceHolder.Callback {
	private Context context;
	private static final byte noteMap[] = new byte[32 * 256];
	private SurfaceHolder surfaceHolder;        
	private int canvasHeight, canvasWidth;
	private Paint headerPaint, headerTextPaint, notePaint, insPaint, barPaint;
	private int fontSize;
	private float fontWidth;
	private char[] charBuf = new char[2];
	private StringBuffer stringBuf = new StringBuffer(3);
	
	final static String[] notes = {
		"C ", "C#", "D ", "D#", "E ", "F ", "F#", "G ", "G#", "A ", "A#", "B "
	};


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
				doDraw(c, info);
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

	private void doDraw(Canvas canvas, Info info) {
		int lines = canvasHeight / fontSize;
		int barY = (lines / 2 + 1) * fontSize;
		int channels = (int)(canvasWidth / fontWidth / 6);
		Rect rect;
		
		// Clear screen
		canvas.drawColor(Color.BLACK);

		// Header
		rect = new Rect(0, 0, canvasWidth - 1, fontSize);
		canvas.drawRect(rect, headerPaint);
		for (int i = 0; i < channels; i++) {
			Util.toString(i, charBuf);
			stringBuf.delete(0, stringBuf.length());
			stringBuf.append(charBuf);
			canvas.drawText(stringBuf.toString(), (i * 6 + 1) * fontWidth, fontSize, headerTextPaint);
		}
		
		// Current line bar
		rect = new Rect(0, barY - fontSize, canvasWidth - 1, barY);
		canvas.drawRect(rect, barPaint);
		
		// Pattern data
		for (int i = 1; i < lines; i++) {
			int note = 60;
			int ins = 0;
			int y = (i + 1) * fontSize;
			
			for (int j = 0; j < channels; j++) {
				Util.toString(note / 12, charBuf);
				stringBuf.delete(0, stringBuf.length());
				stringBuf.append(notes[note % 12]).append(charBuf[1]);
				canvas.drawText(stringBuf.toString(), (j * 6) * fontWidth, y, notePaint);
				if (ins == 0) {
					canvas.drawText("--", (j * 6 + 3) * fontWidth, y, insPaint);
				} else {
					Util.toHex(ins, charBuf);
					stringBuf.delete(0, stringBuf.length());
					stringBuf.append(charBuf);
					canvas.drawText(stringBuf.toString(), (j * 6 + 3) * fontWidth, y, insPaint);
				}
				//canvas.drawText("C#205 D 301 ----- ---23 A 3-- G#202 ----- ===-- ---01 C#702", 0, (i + 1) * fontSize, notePaint);
			}
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

		fontSize = getResources().getDimensionPixelSize(R.dimen.patternview_font_size);

		notePaint = new Paint();
		notePaint.setARGB(255, 140, 140, 160);
		notePaint.setTypeface(Typeface.MONOSPACE);
		notePaint.setTextSize(fontSize);
		
		insPaint = new Paint();
		insPaint.setARGB(255, 140, 80, 80);
		insPaint.setTypeface(Typeface.MONOSPACE);
		insPaint.setTextSize(fontSize);
		
		headerTextPaint = new Paint();
		headerTextPaint.setARGB(255, 220, 220, 220);
		headerTextPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
		headerTextPaint.setTextSize(fontSize);
		
		headerPaint = new Paint();
		headerPaint.setARGB(255, 140, 140, 220);
		
		barPaint = new Paint();
		barPaint.setARGB(255, 40, 40, 40);
		
		fontWidth = notePaint.measureText("X");
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
