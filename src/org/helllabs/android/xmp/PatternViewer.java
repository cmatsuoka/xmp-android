package org.helllabs.android.xmp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.RemoteException;
import android.util.Log;
import android.view.SurfaceHolder;

// http://developer.android.com/guide/topics/graphics/2d-graphics.html

public class PatternViewer extends Viewer implements SurfaceHolder.Callback {
	private Context context;
	//private static final byte noteMap[] = new byte[32 * 256];
	private SurfaceHolder surfaceHolder;        
	private int canvasHeight, canvasWidth;
	private Paint headerPaint, headerTextPaint, notePaint, insPaint, barPaint;
	private int fontSize, fontHeight, fontWidth;
	private String[] allNotes = new String[120];
	private String[] hexByte = new String[256];
	private byte[] rowNotes = new byte[64];
	private byte[] rowInstruments = new byte[64];
	private int oldRow, oldOrd;
	
	private final static String[] notes = {
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
	public void update(ModInterface modPlayer, int[] modVars, Info info) {
		super.update(modPlayer, modVars, info);
		int row = info.values[2];
		int ord = info.values[0];
		
		Canvas c = null;
		
		if (oldRow == row && oldOrd == ord) {
			return;
		}
		
		oldRow = row;
		oldOrd = ord;		
		
		try {
			c = surfaceHolder.lockCanvas(null);
			synchronized (surfaceHolder) {
				doDraw(c, modPlayer, modVars, info);
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

	private void doDraw(Canvas canvas, ModInterface modPlayer, int[] modVars, Info info) {
		int lines = canvasHeight / fontHeight;
		int barLine = lines / 2 + 1;
		int barY = barLine * fontHeight;
		int channels = (int)((canvasWidth / fontWidth - 3) / 6);
		int row = info.values[2];
		int pat = info.values[1];
		int chn = modVars[3];
		int numRows = info.values[3];
		Rect rect;
		
		if (channels > chn) {
			channels = chn;
		}

		// Clear screen
		canvas.drawColor(Color.BLACK);

		// Header
		rect = new Rect(0, 0, canvasWidth - 1, fontHeight - 1);
		canvas.drawRect(rect, headerPaint);
		for (int i = 0; i < channels; i++) {
			int adj = i < 10 ? 1 : 0;
			canvas.drawText(Integer.toString(i), (3 + i * 6 + 1 + adj) * fontWidth, fontSize, headerTextPaint);
		}
		
		// Current line bar
		rect = new Rect(0, barY - fontHeight + 1, canvasWidth - 1, barY);
		canvas.drawRect(rect, barPaint);
		
		// Pattern data
		for (int i = 1; i < lines; i++) {
			int lineInPattern = i + row - barLine + 1; 
			int y = (i + 1) * fontHeight;

			for (int j = 0; j < channels; j++) {
				if (lineInPattern < 0 || lineInPattern >= numRows)
					continue;
				
				try {
					modPlayer.getPatternRow(pat, lineInPattern, rowNotes, rowInstruments);
				} catch (RemoteException e) { }
				
				canvas.drawText(hexByte[lineInPattern], 0, y, headerTextPaint);
				
				if (rowNotes[j] > 0x80) {
					canvas.drawText("===", (3 + j * 6) * fontWidth, y, notePaint);
				} else if (rowNotes[j] > 0) {
					canvas.drawText(allNotes[rowNotes[j] - 1], (3 + j * 6) * fontWidth, y, notePaint);
				} else {
					canvas.drawText("---", (3 + j * 6) * fontWidth, y, notePaint);
				}
				if (rowInstruments[j] > 0) {
					canvas.drawText(hexByte[rowInstruments[j]], (3 + j * 6 + 3) * fontWidth, y, insPaint);
				} else {
					canvas.drawText("--", (3 + j * 6 + 3) * fontWidth, y, insPaint);
				}
			}
		}
	}


	public PatternViewer(Context context) {
		super(context);

		Log.d("Xmp PatternViewer", "PatternViewer constructor");

		// register our interest in hearing about changes to our surface
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		this.surfaceHolder = holder;
		this.context = context;

		fontSize = getResources().getDimensionPixelSize(R.dimen.patternview_font_size);

		notePaint = new Paint();
		notePaint.setARGB(255, 140, 140, 160);
		notePaint.setTypeface(Typeface.MONOSPACE);
		notePaint.setTextSize(fontSize);
		notePaint.setAntiAlias(true);
		
		insPaint = new Paint();
		insPaint.setARGB(255, 160, 80, 80);
		insPaint.setTypeface(Typeface.MONOSPACE);
		insPaint.setTextSize(fontSize);
		insPaint.setAntiAlias(true);
		
		headerTextPaint = new Paint();
		headerTextPaint.setARGB(255, 220, 220, 220);
		headerTextPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
		headerTextPaint.setTextSize(fontSize);
		headerTextPaint.setAntiAlias(true);
		
		headerPaint = new Paint();
		headerPaint.setARGB(255, 140, 140, 220);
		
		barPaint = new Paint();
		barPaint.setARGB(255, 40, 40, 40);
		
		fontWidth = (int)notePaint.measureText("X");
		fontHeight = fontSize * 12 / 10;
		
		for (int i = 0; i < 120; i++) {
			allNotes[i] = new String(notes[i % 12] + (i / 12));
		}
		for (int i = 0; i < 256; i++) {
			hexByte[i] = new String(String.format("%02X", i));
		}
		
		oldRow = -1;
		oldOrd = -1;
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
