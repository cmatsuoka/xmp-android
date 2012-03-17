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
	private Paint scopePaint, scopeLinePaint, insPaint, meterPaint, numPaint, scopeMutePaint;
	private int fontSize, fontHeight, fontWidth;
	private int font2Size, font2Height, font2Width;
	private String[] insName;		
	private Rect rect = new Rect();
	private byte[] buffer;
	private int[] holdKey;
	private String[] channelNumber;
	private ModInterface modPlayer;

	@Override
	public void setup(ModInterface modPlayer, int[] modVars) {
		super.setup(modPlayer, modVars);

		int chn = modVars[3];
		int insNum = modVars[4];
		String[] instruments;
		this.modPlayer = modPlayer;

		insName = new String[insNum];
		
		try {
			instruments = modPlayer.getInstruments();
			for (int i = 0; i < insNum; i++) {
				insName[i] = new String(String.format("%02X %s", i + 1, instruments[i]));
			}
		} catch (RemoteException e) { }

		synchronized (isDown) {
			posY = 0;
		}

		holdKey = new int[chn];
		channelNumber = new String[chn];
		for (int i = 0; i < chn; i++) {
			channelNumber[i] = new String(String.format("%2d", i + 1));
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
	
	@Override
	public void onClick(int x, int y) {
		final int chn = modVars[3];
		final int scopeWidth = 8 * fontWidth;
		final int scopeLeft = 2 * font2Width + 2 * fontWidth;
		
		// Check if clicked on scopes
		if (x >= scopeLeft && x <= scopeLeft + scopeWidth) {
			int scopeNum = (y - posY - fontHeight) / (4 * fontHeight);
			if (scopeNum >= chn) {
				scopeNum = chn - 1;
			}
			Log.i("asd", "scopeNum=" + scopeNum);
			try {
				modPlayer.mute(scopeNum, isMuted[scopeNum] ? 0 : 1);
				isMuted[scopeNum] = !isMuted[scopeNum];
			} catch (RemoteException e) { }
			
		} else {
			super.onClick(x, y);
		}
	}

	private void doDraw(Canvas canvas, ModInterface modPlayer, Info info) {
		final int chn = modVars[3];
		final int insNum = modVars[4];
		final int scopeWidth = 8 * fontWidth;
		final int scopeHeight = 3 * fontHeight;
		final int scopeLeft = 2 * font2Width + 2 * fontWidth;
		final int volLeft = scopeLeft + scopeWidth + fontWidth * 2;
		final int volWidth = (canvasWidth - 6 * fontWidth - volLeft) / 2;
		final int panLeft = volLeft + volWidth + 4 * fontWidth;
		final int panWidth = volWidth;
		int biasY;

		biasY = updatePositionY(canvasHeight - (chn * 4 + 1) * fontHeight);

		// Clear screen
		canvas.drawColor(Color.BLACK);

		for (int i = 0; i < chn; i++) {
			final int y = biasY + (i * 4 + 1) * fontHeight;
			final int ins = isMuted[i] ? -1 : info.instruments[i];
			final int vol = isMuted[i] ? 0 : info.volumes[i];
			final int finalvol = info.finalvols[i];
			final int pan = info.pans[i];
			final int key = info.keys[i];
			final int period = info.periods[i];

			if (key >= 0) {
				holdKey[i] = key;
			}

			// Don't draw if not visible
			if (y < -scopeHeight || y > canvasHeight) {
				continue;
			}

			// Draw channel number
			canvas.drawText(channelNumber[i], 0, y + scopeHeight / 2 + font2Height / 2, numPaint);

			// Draw scopes
			rect.set(scopeLeft, y + 1, scopeLeft + scopeWidth, y + scopeHeight);
			if (isMuted[i]) {
				canvas.drawRect(rect, scopeMutePaint);
				canvas.drawText("MUTE", scopeLeft + 2 * fontWidth, y + fontHeight + fontSize, insPaint);
			} else {
				canvas.drawRect(rect, scopePaint);
	
				try {
					int trigger;
	
					// Be very careful here!
					// Our variables are latency-compensated but sample data is current
					// so caution is needed to avoid retrieving data using old variables
					// from a module with sample data from a newly loaded one.
	
					if (key >= 0) {
						trigger = 1;
					} else {
						trigger = 0;
					}
	
					modPlayer.getSampleData(trigger, ins, holdKey[i], period, i, scopeWidth, buffer);
	
				} catch (RemoteException e) { }
				for (int j = 0; j < scopeWidth; j++) {
					canvas.drawPoint(scopeLeft + j, y + scopeHeight / 2 + buffer[j] * finalvol / 64 * scopeHeight / 2 / 180, scopeLinePaint);
				}
			}

			// Draw instrument name
			if (ins >= 0 && ins < insNum) {
				canvas.drawText(insName[ins], volLeft, y + fontHeight, insPaint);
			}

			// Draw volumes
			int volX = volLeft + vol * volWidth / 0x40;
			int volY1 = y + 2 * fontHeight;
			int volY2 = y + 2 * fontHeight + fontHeight / 3;	
			rect.set(volLeft, volY1, volX, volY2);
			canvas.drawRect(rect, meterPaint);
			rect.set(volX + 1, volY1, volLeft + volWidth, volY2);
			canvas.drawRect(rect, scopePaint);

			// Draw pan
			int panX = panLeft + panWidth / 2 + pan * panWidth / 0x100;
			rect.set(panLeft, volY1, panLeft + panWidth, volY2);
			canvas.drawRect(rect, scopePaint);
			if (ins >= 0) {
				rect.set(panX, volY1, panX + fontWidth / 2, volY2);
				canvas.drawRect(rect, meterPaint);
			}
		}
	}

	public ChannelViewer(Context context) {
		super(context);

		fontSize = getResources().getDimensionPixelSize(R.dimen.channelview_font_size);
		font2Size = getResources().getDimensionPixelSize(R.dimen.channelview_channel_font_size);

		scopePaint = new Paint();
		scopePaint.setARGB(255, 40, 40, 40);

		scopeLinePaint = new Paint();
		scopeLinePaint.setARGB(255, 80, 160, 80);
		
		scopeMutePaint = new Paint();
		scopeMutePaint.setARGB(255, 60, 0, 0);
		
		meterPaint = new Paint();
		meterPaint.setARGB(255, 40, 80, 160);

		insPaint = new Paint();
		insPaint.setARGB(255, 140, 140, 160);
		insPaint.setTypeface(Typeface.MONOSPACE);
		insPaint.setTextSize(fontSize);
		insPaint.setAntiAlias(true);

		numPaint = new Paint();
		numPaint.setARGB(255, 220, 220, 220);
		numPaint.setTypeface(Typeface.MONOSPACE);
		numPaint.setTextSize(font2Size);
		numPaint.setAntiAlias(true);

		fontWidth = (int)insPaint.measureText("X");
		fontHeight = fontSize * 12 / 10;

		font2Width = (int)numPaint.measureText("X");
		font2Height = font2Size * 12 / 10;

		buffer = new byte[8 * fontWidth];
	}
}
