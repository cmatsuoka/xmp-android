package org.helllabs.android.xmp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.RemoteException;

public class InstrumentViewer extends Viewer {
	private Paint[] insPaint, barPaint;
	private int fontSize, fontHeight, fontWidth;
	private String[] insName;
	private Rect rect = new Rect();
	
	@Override
	public void setup(ModInterface modPlayer, int[] modVars) {
		super.setup(modPlayer, modVars);
		
		int insNum = modVars[4];
		
		try {
			insName = modPlayer.getInstruments();
		} catch (RemoteException e) { }
		
		setMaxY(insNum * fontHeight + fontHeight / 2);
	}
	
	@Override
	public void setRotation(int n) {
		super.setRotation(n);
	}

	@Override
	public void update(Info info) {
		super.update(info);
		
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
		final int chn = modVars[3];
		final int ins = modVars[4];
		
		// Clear screen
		canvas.drawColor(Color.BLACK);

		for (int i = 0; i < ins; i++) {
			final int y = (i + 1) * fontHeight - (int)posY;
			final int width = (canvasWidth - 3 * fontWidth) / chn;
			int maxVol;
			
			// Don't draw if not visible
			if (y < 0 || y > canvasHeight + fontHeight) {
				continue;
			}
			
			maxVol = 0;
			for (int j = 0; j < chn; j++) {
				
				if (isMuted[j])
					continue;
				
				if (info.instruments[j] == i) {
					final int x = 3 * fontWidth + width * j;
					int vol = info.volumes[j] / 8;
					if (vol > 7) {
						vol = 7;
					}
					rect.set(x, y - fontSize + 1, x + width * 8 / 10, y + 1);
					canvas.drawRect(rect, barPaint[vol]);
					if (vol > maxVol) {
						maxVol = vol;
					}
				}
			}
			
			canvas.drawText(insName[i], 0, y, insPaint[maxVol]);
		}
	}
	
	public InstrumentViewer(Context context) {
		super(context);
		
		fontSize = getResources().getDimensionPixelSize(R.dimen.instrumentview_font_size);
		
		insPaint = new Paint[8];
		for (int i = 0; i < 8; i++) {
			int val = 120 + 10 * i;
			insPaint[i] = new Paint();
			insPaint[i].setARGB(255, val, val, val);
			insPaint[i].setTypeface(Typeface.MONOSPACE);
			insPaint[i].setTextSize(fontSize);
			insPaint[i].setAntiAlias(true);
		}
		
		barPaint = new Paint[8];
		for (int i = 0; i < 8; i++) {
			int val = 15 * i;
			barPaint[i] = new Paint();
			barPaint[i].setARGB(255, val /4, val / 2, val);
		}

		fontWidth = (int)insPaint[0].measureText("X");
		fontHeight = fontSize * 14 / 10;
	}
}
