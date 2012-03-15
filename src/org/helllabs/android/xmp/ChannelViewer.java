package org.helllabs.android.xmp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.RemoteException;

public class ChannelViewer extends Viewer {
	private Paint scopePaint, scopeLinePaint, insPaint, meterPaint;
	private int fontSize, fontHeight, fontWidth;
	private String[] hexByte = new String[256];	
	private String[] instruments;
	private int[] channelIns;
	private Rect rect = new Rect();

	@Override
	public void setup(ModInterface modPlayer, int[] modVars) {
		super.setup(modPlayer, modVars);
		int chn = modVars[3];
		
		try {
			instruments = modPlayer.getInstruments();
		} catch (RemoteException e) { }
		
		channelIns = new int[chn];
		for (int i = 0; i < chn; i++) {
			channelIns[i] = -1;
		}
	}

	@Override
	public void update(Info info) {
		Canvas c = null;
		
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
		int chn = modVars[3];
		int volBase = modVars[6];
		
		// Clear screen
		canvas.drawColor(Color.BLACK);

		// Scope
		for (int i = 0; i < chn; i++) {
			int y = (i * 4 + 1) * fontHeight;
			int ins = info.instruments[i];
			int vol = info.volumes[i];
			int pan = info.pans[i];
			
			rect.set(0, y + 1, 8 * fontWidth, y + 3 * fontHeight);
			canvas.drawRect(rect, scopePaint);

			if (ins >= 0) {
				channelIns[i] = ins;
			}
			
			int ci = channelIns[i];
			if (ci >= 0) {
				canvas.drawText(hexByte[ci + 1] + ":" + instruments[ci],
							9 * fontWidth, y + fontHeight, insPaint);
			}
			
			// Draw volumes
			int volLeft = 9 * fontWidth;
			int volWidth = (canvasWidth - 6 * fontWidth - volLeft) / 2;
			int volX = volLeft + vol * volWidth / volBase;
			int volY1 = y + 2 * fontHeight;
			int volY2 = y + 2 * fontHeight + fontHeight / 3;
			rect.set(volLeft, volY1, volX, volY2);
			canvas.drawRect(rect, meterPaint);
			rect.set(volX + 1, volY1, volLeft + volWidth, volY2);
			canvas.drawRect(rect, scopePaint);

			// Draw pan
			int panLeft = volLeft + volWidth + 4 * fontWidth;
			int panWidth = volWidth; 
			int panX = panLeft + pan * panWidth / 0x100;
			int panY1 = volY1;
			int panY2 = volY2;
			rect.set(panLeft, panY1, panLeft + panWidth, panY2);
			canvas.drawRect(rect, scopePaint);
			rect.set(panX, panY1, panX + fontWidth / 2, panY2);
			canvas.drawRect(rect, meterPaint);
		}
	}
	
	public ChannelViewer(Context context) {
		super(context);
		
		fontSize = getResources().getDimensionPixelSize(R.dimen.channelview_font_size);
		
		scopePaint = new Paint();
		scopePaint.setARGB(255, 40, 40, 40);
		
		scopeLinePaint = new Paint();
		scopeLinePaint.setARGB(255, 40, 160, 40);
		
		meterPaint = new Paint();
		meterPaint.setARGB(255, 40, 80, 160);
		
		insPaint = new Paint();
		insPaint.setARGB(255, 140, 140, 160);
		insPaint.setTypeface(Typeface.MONOSPACE);
		insPaint.setTextSize(fontSize);
		insPaint.setAntiAlias(true);
		
		fontWidth = (int)insPaint.measureText("X");
		fontHeight = fontSize * 12 / 10;
		
		for (int i = 0; i < 256; i++) {
			hexByte[i] = new String(String.format("%02X", i));
		}
	}
}
