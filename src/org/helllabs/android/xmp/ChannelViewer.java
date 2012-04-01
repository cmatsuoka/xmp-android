package org.helllabs.android.xmp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.RemoteException;

public class ChannelViewer extends Viewer {
	private Paint scopePaint, scopeLinePaint, insPaint, meterPaint, numPaint, scopeMutePaint;
	private int fontSize, fontHeight, fontWidth;
	private int font2Size, font2Height, font2Width;
	private String[] insName;		
	private Rect rect = new Rect();
	private byte[] buffer;
	private float[] bufferXY;
	private int[] holdKey;
	private String[] channelNumber;
	private ModInterface modPlayer;

	@Override
	public void setup(ModInterface modPlayer, int[] modVars) {
		super.setup(modPlayer, modVars);

		int chn = modVars[3];
		this.modPlayer = modPlayer;
		
		try {
			insName = modPlayer.getInstruments();
		} catch (RemoteException e) { }

		setMaxY((chn * 4 + 1) * fontHeight);

		holdKey = new int[chn];
		channelNumber = new String[chn];
		
		char[] c = new char[2];
		for (int i = 0; i < chn; i++) {
			Util.to2d(c, i + 1);
			channelNumber[i] = new String(c);
		}
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
	
	private int findScope(int x, int y) {
		final int chn = modVars[3];
		final int scopeWidth = 8 * fontWidth;
		final int scopeLeft = 2 * font2Width + 2 * fontWidth;
		
		if (x >= scopeLeft && x <= scopeLeft + scopeWidth) {
			int scopeNum = (y + (int)posY - fontHeight) / (4 * fontHeight);
			if (scopeNum >= chn) {
				scopeNum = -1;
			}
			return scopeNum;
		} else {
			return -1;
		}
	}
	
	@Override
	public void onClick(int x, int y) {

		// Check if clicked on scopes
		int n = findScope(x, y);

		if (n >= 0) {
			try {
				modPlayer.mute(n, isMuted[n] ? 0 : 1);
				isMuted[n] = !isMuted[n];
			} catch (RemoteException e) { }
		} else {
			super.onClick(x, y);
		}
	}
	
	@Override
	public void onLongClick(int x, int y) {
		int chn = modVars[3];

		// Check if clicked on scopes
		int n = findScope(x, y);

		// If the channel is solo, a long press unmute all channels,
		// otherwise solo this channel
		
		if (n >= 0) {
			int count = 0;
			for (int i = 0; i < chn; i++) {
				if (!isMuted[i])
					count++;
			}
			if (count == 1 && !isMuted[n]) {
				try {
					for (int i = 0; i < chn; i++) {
						modPlayer.mute(i, 0);
						isMuted[i] = false;
					}
				} catch (RemoteException e) { }
			} else {
				try {
					for (int i = 0; i < chn; i++) {
						modPlayer.mute(i, i != n ? 1 : 0);
						isMuted[i] = i != n;
					}
				} catch (RemoteException e) { }
			}
		} else {
			super.onLongClick(x, y);
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

		// Clear screen
		canvas.drawColor(Color.BLACK);

		for (int i = 0; i < chn; i++) {
			final int y = (i * 4 + 1) * fontHeight - (int)posY;
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
				int h = scopeHeight / 2;
				for (int j = 0; j < scopeWidth; j++) {
					bufferXY[j * 2] = scopeLeft + j;
					bufferXY[j * 2 + 1] = y + h + buffer[j] * h * finalvol / (64 * 180);
				}
				
				// Using drawPoints() instead of drawing each point saves a lot of CPU
				canvas.drawPoints(bufferXY, 0, scopeWidth << 1, scopeLinePaint);
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
			int panX = panLeft + pan * panWidth / 0x100;
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
		scopeLinePaint.setStrokeWidth(0);
		scopeLinePaint.setAntiAlias(false);
		
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
		bufferXY = new float[8 * fontWidth * 2];
	}
}
