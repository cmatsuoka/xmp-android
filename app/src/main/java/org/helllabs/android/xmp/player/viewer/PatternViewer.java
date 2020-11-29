package org.helllabs.android.xmp.player.viewer;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.player.Util;
import org.helllabs.android.xmp.service.ModInterface;
import org.helllabs.android.xmp.util.Log;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.RemoteException;


// http://developer.android.com/guide/topics/graphics/2d-graphics.html

public class PatternViewer extends Viewer {
	private static final String TAG = "PatternViewer";
	private static final int MAX_NOTES = 120;
	private static final int RIGHT_BUMP = 20; // Provide a slight margin for row numbers
	private final Paint headerPaint, headerTextPaint, notePaint, insPaint;
	private final Paint barPaint, muteNotePaint, muteInsPaint;
	private final int fontSize, fontHeight, fontWidth;
	private final String[] allNotes = new String[MAX_NOTES];
	private String[] hexByte = new String[0];
	private final String[] instrumentHexByte = new String[256];
	private final byte[] rowNotes = new byte[64];
	private final byte[] rowInstruments = new byte[64];
	private int oldRow, oldOrd, oldPosX;
	private final Rect rect = new Rect(); 

	private final static String[] NOTES = {
		"C ", "C#", "D ", "D#", "E ", "F ", "F#", "G ", "G#", "A ", "A#", "B "
	};

	public PatternViewer(final Context context) {
		super(context);

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

		muteNotePaint = new Paint();
		muteNotePaint.setARGB(255, 60, 60, 60);
		muteNotePaint.setTypeface(Typeface.MONOSPACE);
		muteNotePaint.setTextSize(fontSize);
		muteNotePaint.setAntiAlias(true);

		muteInsPaint = new Paint();
		muteInsPaint.setARGB(255, 80, 40, 40);
		muteInsPaint.setTypeface(Typeface.MONOSPACE);
		muteInsPaint.setTextSize(fontSize);
		muteInsPaint.setAntiAlias(true);

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

		for (int i = 0; i < MAX_NOTES; i++) {
			allNotes[i] = NOTES[i % 12] + (i / 12);
		}

		final char[] c = new char[2];
		for (int i = 0; i < 256; i++) {
			Util.to02X(c, i);
			instrumentHexByte[i] = new String(c);
		}
	}
	
	@Override
	public void setup(final ModInterface modPlayer, final int[] modVars) {
		super.setup(modPlayer, modVars);

		oldRow = -1;
		oldOrd = -1;
		oldPosX = -1;

		final int chn = modVars[3];
		setMaxX((chn * 6 + /*2*/ 3) * fontWidth);
	}

	//@Override
	//public void setRotation(final int val) {
	// 	super.setRotation(val);
	//}

	@Override
	public void update(final Info info, final boolean paused) {
		super.update(info, paused);

		final int row = info.values[2];
		final int ord = info.values[0];

		if (oldRow == row && oldOrd == ord && oldPosX == (int)posX) {
			return;
		}

		final int numRows = info.values[3];
		Canvas canvas = null;

		if (numRows != 0) {		// Skip first invalid infos
			oldRow = row;
			oldOrd = ord;
			oldPosX = (int)posX;
		}

		try {
			canvas = surfaceHolder.lockCanvas(null);
			if (canvas != null) {
				synchronized (surfaceHolder) {
					doDraw(canvas, modPlayer, info);
				}
			}
		} finally {
			// do this in a finally so that if an exception is thrown
			// during the above, we don't leave the Surface in an
			// inconsistent state
			if (canvas != null) {
				surfaceHolder.unlockCanvasAndPost(canvas);
			}
		}
	}

	private void doDraw(final Canvas canvas, final ModInterface modPlayer, final Info info) {
		final int lines = canvasHeight / fontHeight;
		final int barLine = lines / 2 + 1;
		final int barY = barLine * fontHeight;
		final int row = info.values[2];
		final int pat = info.values[1];
		final int chn = modVars[3];
		final int numRows = info.values[3];

		// Get the number of rows dynamically
		// Side effect of https://github.com/cmatsuoka/xmp-android/pull/15
		if(numRows > 0 && hexByte.length != numRows) {
			Log.d(TAG, "Resizing numRows to " + numRows);
			hexByte = new String[numRows];
			final char[] c = new char[3];
			for(int i = 0; i < numRows; i++) {
				if(i < 256) {
					Util.to02X(c, i);
				} else {
					Util.to03X(c, i);
				}
				hexByte[i] = new String(c);
			}
		}

		// Clear screen
		canvas.drawColor(Color.BLACK);

		// Header
		rect.set(0, 0, canvasWidth - 1, fontHeight - 1);
		canvas.drawRect(rect, headerPaint);
		for (int i = 0; i < chn; i++) {
			final int adj = (i + 1) < 10 ? 1 : 0;
			final int x = (3 + i * 6 + 1 + adj) * fontWidth - (int)posX + RIGHT_BUMP;
			if (x > -2 * fontWidth && x < canvasWidth) {
				canvas.drawText(Integer.toString(i + 1), x, fontSize, headerTextPaint);
			}
		}

		// Current line bar
		rect.set(0, barY - fontHeight + 10, canvasWidth, barY + 10);
		canvas.drawRect(rect, barPaint);

		// Pattern data
		for (int i = 1; i < lines; i++) {
			final int lineInPattern = i + row - barLine + 1; 
			final int y = (i + 1) * fontHeight;
			Paint paint;
			Paint paint2;
			int x;

			if (lineInPattern < 0 || lineInPattern >= numRows) {
				continue;
			}

			// Row number
			if (posX > -2 * fontWidth) {
				canvas.drawText(hexByte[lineInPattern], -posX, y, headerTextPaint);
			}

			for (int j = 0; j < chn; j++) {	
				try {

					// Be very careful here!
					// Our variables are latency-compensated but pattern data is current
					// so caution is needed to avoid retrieving data using old variables
					// from a module with pattern data from a newly loaded one.

					modPlayer.getPatternRow(pat, lineInPattern, rowNotes, rowInstruments);
				} catch (RemoteException e) {
					// fail silenty
				}

			x = (3 + j * 6) * fontWidth - (int)posX + RIGHT_BUMP;

				if (x < -6 * fontWidth || x > canvasWidth) {
					continue;
				}

				if (isMuted[j]) {
					paint = muteNotePaint;
					paint2 = muteInsPaint;
				} else {
					paint = notePaint;
					paint2 = insPaint;
				}

				final byte note = rowNotes[j];
				if (note < 0) {
					canvas.drawText("===", x, y, paint);
				} else if (note > MAX_NOTES) {
					canvas.drawText(">>>", x, y, paint);
				} else if (note > 0) {
					canvas.drawText(allNotes[note - 1], x, y, paint);
				} else {
					canvas.drawText("---", x, y, paint);
				}

				x = (3 + j * 6 + 3) * fontWidth - (int)posX + RIGHT_BUMP;
				if (rowInstruments[j] > 0) {
					canvas.drawText(instrumentHexByte[rowInstruments[j]], x, y, paint2);
				} else {
					canvas.drawText("--", x, y, paint2);
				}
			}
		}
	}
}
