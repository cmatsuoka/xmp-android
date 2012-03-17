package org.helllabs.android.xmp;

import android.content.Context;
import android.os.RemoteException;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public abstract class Viewer extends SurfaceView implements SurfaceHolder.Callback {
	protected Context context;
	protected SurfaceHolder surfaceHolder;
	protected int canvasHeight, canvasWidth;
	protected int[] modVars;
	protected ModInterface modPlayer;
	protected boolean[] isMuted;
	
    public class Info {
    	int time;
    	int[] values = new int[7];	// order pattern row num_rows frame speed bpm
    	int[] volumes = new int[64];
    	int[] finalvols = new int[64];
    	int[] pans = new int[64];
    	int[] instruments = new int[64];
    	int[] keys = new int[64];
    	int[] periods = new int[64];
    };

    // Touch tracking
    private int downX, downY;
    private int currentX, currentY;
    private boolean moved;
    protected int deltaX, deltaY;
    protected int posX, posY;
    protected Boolean isDown;
    
	public Viewer(Context context) {
		super(context);
		this.context = context;

		// register our interest in hearing about changes to our surface
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		surfaceHolder = holder;
		
		posX = posY = 0;
		isDown = false;
		moved = false;
	
		setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent ev) {
				int action = ev.getAction();
				
				switch (action) {
				case MotionEvent.ACTION_DOWN:
					synchronized (isDown) {
						isDown = true;
						downX = (int)ev.getX();
						downY = (int)ev.getY();
						deltaX = deltaY = 0;
						moved = false;
					}
					break;
				case MotionEvent.ACTION_MOVE:
					synchronized (isDown) {
						if (isDown) {
							currentX = (int)ev.getX();
							currentY = (int)ev.getY();
							deltaX = currentX - downX;
							deltaY = currentY - downY;
							moved = true;
						}
					}
					break;
				case MotionEvent.ACTION_UP:
					isDown = false;
					if (!moved) {
						onClick((int)ev.getX(), (int)ev.getY());
					}
					
					synchronized (isDown) {
						posX += deltaX;
						posY += deltaY;
						deltaX = deltaY = 0;
					}
					break;
				}
				
				return true;
			}
			 
		 });
	}
	
	protected void onClick(int x, int y) {
		((View)getParent()).performClick();
	}
	
	public abstract void update(Info info);
	
	protected int updatePositionX(int max) {
		int bias;

		synchronized (isDown) {
			bias = deltaX + posX;

			if (max > 0) {
				max = 0;
			}

			if (bias > 0) {
				bias = 0;
				posX = 0;
				downX = currentX;
			}

			if (bias < max) {
				bias = max;
				posX = max;
				downX = currentX;
			}
		}

		return bias;
	}
	
	protected int updatePositionY(int max) {
		int bias;

		synchronized (isDown) {
			bias = deltaY + posY;

			if (max > 0) {
				max = 0;
			}

			if (bias > 0) {
				bias = 0;
				posY = 0;
				downY = currentY;
			}

			if (bias < max) {
				bias = max;
				posY = max;
				downY = currentY;
			}
		}

		return bias;
	}
	
	public void setup(ModInterface modPlayer, int[] modVars) {
		final int chn = modVars[3];
		
		this.modVars = modVars;
		this.modPlayer = modPlayer;
				
		isMuted = new boolean[chn];
		for (int i = 0; i < chn; i++) {
			try {
				isMuted[i] = modPlayer.mute(i, 2) == 1;
			} catch (RemoteException e) { }
		}
	}
	
	/* Callback invoked when the surface dimensions change. */
	public void setSurfaceSize(int width, int height) {
		// synchronized to make sure these all change atomically
		synchronized (surfaceHolder) {
			canvasWidth = width;
			canvasHeight = height;
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		setSurfaceSize(width, height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {		
		surfaceHolder = holder;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

	}
}
