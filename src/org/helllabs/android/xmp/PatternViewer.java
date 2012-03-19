package org.helllabs.android.xmp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.RemoteException;

// http://developer.android.com/guide/topics/graphics/2d-graphics.html

public class PatternViewer extends Viewer {
	private Paint headerPaint, headerTextPaint, notePaint, insPaint;
	private Paint barPaint, muteNotePaint, muteInsPaint;
	private int fontSize, fontHeight, fontWidth;
	private String[] allNotes = new String[120];
	private String[] hexByte = new String[256];
	private byte[] rowNotes = new byte[64];
	private byte[] rowInstruments = new byte[64];
	private int oldRow, oldOrd, oldPosX;
	private Rect rect = new Rect(); 
	
	private final static String[] notes = {
		"C ", "C#", "D ", "D#", "E ", "F ", "F#", "G ", "G#", "A ", "A#", "B "
	};
	
	@Override
	public void setup(ModInterface modPlayer, int[] modVars) {
		super.setup(modPlayer, modVars);
		
		oldRow = -1;
		oldOrd = -1;
		oldPosX = -1;
		
		synchronized (isDown) {
			int chn = modVars[3];
			posX = 0;
			maxX = (chn * 6 + 2) * fontWidth;
		}
	}

	@Override
	public void update(Info info) {
		int row = info.values[2];
		int ord = info.values[0];
		int numRows = info.values[3];
		
		Canvas c = null;
		
		if (oldRow == row && oldOrd == ord && oldPosX == (int)posX) {
			return;
		}
		
		if (numRows != 0) {		// Skip first invalid infos
			oldRow = row;
			oldOrd = ord;
			oldPosX = (int)posX;
		}
		
		try {
			c = surfaceHolder.lockCanvas(null);
			synchronized (surfaceHolder) {
				doDraw(c, modPlayer, info);
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

	private void doDraw(Canvas canvas, ModInterface modPlayer, Info info) {
		final int lines = canvasHeight / fontHeight;
		final int barLine = lines / 2 + 1;
		final int barY = barLine * fontHeight;
		final int row = info.values[2];
		final int pat = info.values[1];
		final int chn = modVars[3];
		final int numRows = info.values[3];

		// Clear screen
		canvas.drawColor(Color.BLACK);

		// Header
		rect.set(0, 0, canvasWidth - 1, fontHeight - 1);
		canvas.drawRect(rect, headerPaint);
		for (int i = 0; i < chn; i++) {
			int adj = (i + 1) < 10 ? 1 : 0;
			canvas.drawText(Integer.toString(i + 1), (3 + i * 6 + 1 + adj) * fontWidth - posX, fontSize, headerTextPaint);
		}
		
		// Current line bar
		rect.set(0, barY - fontHeight + 1, canvasWidth - 1, barY);
		canvas.drawRect(rect, barPaint);
		
		// Pattern data
		for (int i = 1; i < lines; i++) {
			final int lineInPattern = i + row - barLine + 1; 
			final int y = (i + 1) * fontHeight;
			Paint paint, paint2;
			int x;
			
			if (lineInPattern < 0 || lineInPattern >= numRows)
				continue;

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
				} catch (RemoteException e) { }
					
				x = (3 + j * 6) * fontWidth - (int)posX;
				
				if (x > canvasWidth || x < -6 * fontWidth) {
					continue;
				}
				
				if (isMuted[j]) {
					paint = muteNotePaint;
					paint2 = muteInsPaint;
				} else {
					paint = notePaint;
					paint2 = insPaint;
				}
				
				if (rowNotes[j] > 0x80) {
					canvas.drawText("===", x, y, paint);
				} else if (rowNotes[j] > 0) {
					canvas.drawText(allNotes[rowNotes[j] - 1], x, y, paint);
				} else {
					canvas.drawText("---", x, y, paint);
				}
				
				x = (3 + j * 6 + 3) * fontWidth - (int)posX;
				if (rowInstruments[j] > 0) {
					canvas.drawText(hexByte[rowInstruments[j]], x, y, paint2);
				} else {
					canvas.drawText("--", x, y, paint2);
				}
			}
		}
	}

	public PatternViewer(Context context) {
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
		
		for (int i = 0; i < 120; i++) {
			allNotes[i] = new String(notes[i % 12] + (i / 12));
		}
		for (int i = 0; i < 256; i++) {
			hexByte[i] = new String(String.format("%02X", i));
		}
	}
}
