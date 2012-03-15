package org.helllabs.android.xmp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.RemoteException;
import android.util.Log;

public class ChannelViewer extends Viewer {
	private Paint scopePaint, scopeLinePaint, insPaint;
	private int fontSize, fontHeight, fontWidth;
	private String[] hexByte = new String[256];	
	private String[] instruments;
	private int[] channelIns;

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
		Rect rect;
		
		// Clear screen
		canvas.drawColor(Color.BLACK);

		// Scope
		for (int i = 0; i < chn; i++) {
			int y = (i * 4 + 1) * fontHeight;
			int ins = info.instruments[i];
			
			rect = new Rect(0, y + 1, 8 * fontWidth, y + 3 * fontHeight);
			canvas.drawRect(rect, scopePaint);

			if (ins >= 0) {
				channelIns[i] = ins;
			}
			
			int ci = channelIns[i];
			if (ci >= 0) {
				canvas.drawText(hexByte[ci + 1] + ":" + instruments[ci],
							9 * fontWidth, y + fontHeight, insPaint);
			}
		}
	}
	
	public ChannelViewer(Context context) {
		super(context);
		
		fontSize = getResources().getDimensionPixelSize(R.dimen.channelview_font_size);
		
		scopePaint = new Paint();
		scopePaint.setARGB(255, 40, 40, 40);
		
		scopeLinePaint = new Paint();
		scopeLinePaint.setARGB(255, 40, 160, 40);
		
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
