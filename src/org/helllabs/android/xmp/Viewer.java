package org.helllabs.android.xmp;

import android.content.Context;
import android.util.Log;
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

    protected int deltaX, deltaY;
    protected int downX, downY;
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
					}
					break;
				case MotionEvent.ACTION_MOVE:
					synchronized (isDown) {
						if (isDown) {
							deltaX = (int)ev.getX() - downX;
							deltaY = (int)ev.getY() - downY;
						}
					}
					break;
				case MotionEvent.ACTION_UP:
					isDown = false;
					
					Log.i("asd", "x=" + deltaX + " y=" + deltaY);
					if (deltaX == 0 && deltaY == 0) {
						((View)getParent()).performClick();
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
	
	public abstract void update(Info info);
	
	public void setup(ModInterface modPlayer, int[] modVars) {
		this.modVars = modVars;
		this.modPlayer = modPlayer;
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
