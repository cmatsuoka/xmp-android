package org.helllabs.android.xmp.player;

import org.helllabs.android.xmp.Log;
import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.service.ModInterface;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.RemoteException;
import android.view.Surface;

@SuppressWarnings("PMD.ShortVariable")
public class ChannelViewer extends Viewer {
	private static final String TAG = "ChannelViewer"; 
	private final Paint scopePaint, scopeLinePaint, insPaint, meterPaint, numPaint, scopeMutePaint;
	private final int fontSize, fontHeight, fontWidth;
	private final int font2Size, font2Height, font2Width;
	private String[] insName, insNameTrim;		
	private final Rect rect = new Rect();
	private final byte[] buffer;
	private float[] bufferXY;
	private int[] holdKey;
	private String[] channelNumber;
	private ModInterface modPlayer;
	private int cols = 1;
	private int scopeWidth;
	private int scopeHeight;
	private int scopeLeft;
	private int volLeft;
	private int volWidth;
	private int panLeft;
	private int panWidth;
	private int[] keyRow = new int[64];
	
	@Override
	public void setup(final ModInterface modPlayer, final int[] modVars) {
		super.setup(modPlayer, modVars);

		final int chn = modVars[3];
		this.modPlayer = modPlayer;
		
		try {
			insName = modPlayer.getInstruments();
		} catch (RemoteException e) {
			Log.e(TAG, "Can't get instrument name");
		}

		holdKey = new int[chn];
		channelNumber = new String[chn];
		
		// This is much faster than String.format
		final char[] c = new char[2];
		for (int i = 0; i < chn; i++) {
			Util.to2d(c, i + 1);
			channelNumber[i] = new String(c);
		}
		
		scopeWidth = 8 * fontWidth;
		scopeHeight = 3 * fontHeight;
		scopeLeft = 2 * font2Width + 2 * fontWidth;
		volLeft = scopeLeft + scopeWidth + fontWidth * 2;
	}

	@Override
	public void update(Info info) {
		super.update(info);
		
		Canvas canvas = null;

		try {
			canvas = surfaceHolder.lockCanvas(null);
			synchronized (surfaceHolder) {
				doDraw(canvas, modPlayer, info);
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
	
	private int findScope(final int x, final int y) {		
		final int chn = modVars[3];
		final int scopeWidth = 8 * fontWidth;
		int scopeLeft = 2 * font2Width + 2 * fontWidth;
		
		if (x >= scopeLeft && x <= scopeLeft + scopeWidth) {
			int scopeNum = (y + (int)posY - fontHeight) / (4 * fontHeight);
			if (cols > 1) {
				if (scopeNum >= ((chn + 1) / cols)) {
					scopeNum = -1;
				}
			} else {
				if (scopeNum >= chn) {
					scopeNum = -1;
				}
			}
			return scopeNum;
		} else if (cols <= 1) {
			return -1;
		}
		
		// Two column layout
		scopeLeft += canvasWidth / cols;
		
		if (x >= scopeLeft && x <= scopeLeft + scopeWidth) {
			int scopeNum = (y + (int)posY - fontHeight) / (4 * fontHeight) + (chn + 1) / cols;
			if (scopeNum >= chn) {
				scopeNum = -1;
			}	
			return scopeNum;
		} else {
			return -1;
		}
	}
	
	@Override
	public void onClick(final int x, final int y) {

		// Check if clicked on scopes
		final int n = findScope(x, y);

		if (n >= 0) {
			try {
				modPlayer.mute(n, isMuted[n] ? 0 : 1);
				isMuted[n] ^= true;
			} catch (RemoteException e) {
				Log.e(TAG, "Can't mute channel " + n);
			}
		} else {
			super.onClick(x, y);
		}
	}
	
	@Override
	public void onLongClick(final int x, final int y) {
		final int chn = modVars[3];

		// Check if clicked on scopes
		final int n = findScope(x, y);

		// If the channel is solo, a long press unmute all channels,
		// otherwise solo this channel
		
		if (n >= 0) {
			int count = 0;
			for (int i = 0; i < chn; i++) {
				if (!isMuted[i]) {
					count++;
				}
			}
			if (count == 1 && !isMuted[n]) {
				try {
					for (int i = 0; i < chn; i++) {
						modPlayer.mute(i, 0);
						isMuted[i] = false;
					}
				} catch (RemoteException e) {
					Log.e(TAG, "Can't mute channels");
				}
			} else {
				try {
					for (int i = 0; i < chn; i++) {
						modPlayer.mute(i, i != n ? 1 : 0);
						isMuted[i] = i != n;
					}
				} catch (RemoteException e) {
					Log.e(TAG, "Can't unmute channel " + n);
				}
			}
		} else {
			super.onLongClick(x, y);
		}
	}

	@Override
	public void setRotation(final int n) {
		super.setRotation(n);
		
		// Should use canvasWidth but it's not updated yet
		final int width = ((Activity)context).getWindowManager().getDefaultDisplay().getWidth();
		
		// Use two columns in large screen or normal screen with 800 or more pixels in
		// landscape or xlarge screen in any orientation
		switch (screenSize) {
		case Configuration.SCREENLAYOUT_SIZE_NORMAL:
			if (width < 800) {
				cols = 1;
				break;
			}
			/* fall-though */
		case Configuration.SCREENLAYOUT_SIZE_LARGE:
			if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
				cols = 1;
			} else {
				cols = 2;
			}
			break;
		case Configuration.SCREENLAYOUT_SIZE_XLARGE:
			cols = 2;
			break;
		default:
			cols = 1;
		}
		
		final int chn = modVars[3];
		if (cols == 1) {
			setMaxY((chn * 4 + 1) * fontHeight);
		} else {
			setMaxY(((chn + 1) / cols * 4 + 1) * fontHeight);
		}
		
		volWidth = (width / cols - 5 * fontWidth - volLeft) / 2;
		panLeft = volLeft + volWidth + 3 * fontWidth;
		panWidth = volWidth;
		final int textWidth = 2 * volWidth / fontWidth + 3;
		
		final int num = insName.length;
		insNameTrim = new String[num];
		
		for (int i = 0; i < num; i++) {
			if (insName[i].length() > textWidth) {
				insNameTrim[i] = insName[i].substring(0, textWidth);
			} else {
				insNameTrim[i] = insName[i];
			}
		}
	}
	
	private void doDraw(final Canvas canvas, final ModInterface modPlayer, final Info info) {
		final int chn = modVars[3];
		final int insNum = modVars[4];
		final int row = info.values[2];
		
		// Clear screen
		canvas.drawColor(Color.BLACK);

		for (int i = 0; i < chn; i++) {
			final int num = (chn + 1) / cols;
			final int icol = i % num;
			final int x = (i / num) * canvasWidth / 2;
			final int y = (icol * 4 + 1) * fontHeight - (int)posY;
			final int ins = isMuted[i] ? -1 : info.instruments[i];
			final int vol = isMuted[i] ? 0 : info.volumes[i];
			final int finalvol = info.finalvols[i];
			final int pan = info.pans[i];
			int key = info.keys[i];
			final int period = info.periods[i];

			if (key >= 0) {
				holdKey[i] = key;
				
				if (keyRow[i] == row) {
					key = -1;
				} else {
					keyRow[i] = row;
				}
			}

			// Don't draw if not visible
			if (y < -scopeHeight || y > canvasHeight) {
				continue;
			}

			// Draw channel number
			canvas.drawText(channelNumber[i], x, y + (scopeHeight + font2Height) / 2, numPaint);

			// Draw scopes
			rect.set(x + scopeLeft, y + 1, x + scopeLeft + scopeWidth, y + scopeHeight);
			if (isMuted[i]) {
				canvas.drawRect(rect, scopeMutePaint);
				canvas.drawText("MUTE", x + scopeLeft + 2 * fontWidth, y + fontHeight + fontSize, insPaint);
			} else {
				canvas.drawRect(rect, scopePaint);
	
				try {	
					// Be very careful here!
					// Our variables are latency-compensated but sample data is current
					// so caution is needed to avoid retrieving data using old variables
					// from a module with sample data from a newly loaded one.

					modPlayer.getSampleData(key >= 0, ins, holdKey[i], period, i, scopeWidth, buffer);
	
				} catch (RemoteException e) {
					// fail silently
				}
				
				final int h = scopeHeight / 2;
				for (int j = 0; j < scopeWidth; j++) {
					bufferXY[j * 2] = x + scopeLeft + j;
					bufferXY[j * 2 + 1] = y + h + buffer[j] * h * finalvol / (64 * 180);
				}
				
				// Using drawPoints() instead of drawing each point saves a lot of CPU
				canvas.drawPoints(bufferXY, 0, scopeWidth << 1, scopeLinePaint);
			}

			// Draw instrument name
			if (ins >= 0 && ins < insNum) {
				canvas.drawText(insNameTrim[ins], x + volLeft, y + fontHeight, insPaint);
			}

			// Draw volumes
			final int volX = volLeft + vol * volWidth / 0x40;
			final int volY1 = y + 2 * fontHeight;
			final int volY2 = y + 2 * fontHeight + fontHeight / 3;	
			rect.set(x + volLeft, volY1, x + volX, volY2);
			canvas.drawRect(rect, meterPaint);
			rect.set(x + volX + 1, volY1, x + volLeft + volWidth, volY2);
			canvas.drawRect(rect, scopePaint);

			// Draw pan
			final int panX = panLeft + pan * panWidth / 0x100;
			rect.set(x + panLeft, volY1, x + panLeft + panWidth, volY2);
			canvas.drawRect(rect, scopePaint);
			if (ins >= 0) {
				rect.set(x + panX, volY1, x + panX + fontWidth / 2, volY2);
				canvas.drawRect(rect, meterPaint);
			}
		}
	}

	public ChannelViewer(final Context context) {
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
