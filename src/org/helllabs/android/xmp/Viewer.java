package org.helllabs.android.xmp;

import android.content.Context;
import android.os.RemoteException;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public abstract class Viewer extends SurfaceView implements SurfaceHolder.Callback, View.OnClickListener {
	protected Context context;
	protected SurfaceHolder surfaceHolder;
	protected int canvasHeight, canvasWidth;
	protected int[] modVars;
	protected ModInterface modPlayer;
	protected boolean[] isMuted;
	private GestureDetector gestureDetector;
    View.OnTouchListener gestureListener;
	
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
    protected int deltaX, deltaY;
    protected int posX, posY;
    protected Boolean isDown;
    protected long downTime;
    
    private class MyGestureDetector extends SimpleOnGestureListener {
    	
    	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
    		deltaX -= (int)distanceX;
    		deltaY -= (int)distanceY;
    		return true;
    	}
    	
    	public boolean onSingleTapUp(MotionEvent e) {
    		onClick((int)e.getX(), (int)e.getY());
    		return true;
    	}
    	
    	public void onLongPress(MotionEvent e) {
    		onLongClick((int)e.getX(), (int)e.getY());
    	}
    }
    
	public Viewer(Context context) {
		super(context);
		this.context = context;

		// register our interest in hearing about changes to our surface
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		surfaceHolder = holder;
		
		posX = posY = 0;
		isDown = false;
		
        // Gesture detection
        gestureDetector = new GestureDetector(new MyGestureDetector());
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        };
        
        setOnClickListener(Viewer.this); 
        setOnTouchListener(gestureListener);
	}
	

	@Override
	public void onClick(View v) {
		
	}
	
	protected void onClick(int x, int y) {
		((View)getParent()).performClick();
	}
	
	protected void onLongClick(int x, int y) {
		// does nothing
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
				deltaX = 0;
			}

			if (bias < max) {
				bias = max;
				posX = max;
				deltaX = 0;
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
				deltaY = 0;
			}

			if (bias < max) {
				bias = max;
				posY = max;
				deltaY = 0;
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
