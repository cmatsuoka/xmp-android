package org.helllabs.android.xmp.player;

import org.helllabs.android.xmp.service.ModInterface;
import org.helllabs.android.xmp.util.Log;

import android.content.Context;
import android.os.RemoteException;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;


public abstract class Viewer extends SurfaceView implements SurfaceHolder.Callback, View.OnClickListener {
	private static final String TAG = "Viewer";
	protected Context context;
	protected SurfaceHolder surfaceHolder;
	protected int canvasHeight, canvasWidth;
	protected int[] modVars;
	protected ModInterface modPlayer;
	protected boolean[] isMuted;
	protected int rotation;
	protected int screenSize;
	private final GestureDetector gestureDetector;

	// Touch tracking
	protected float posX, posY, velX, velY;
	protected boolean isDown;
	private int maxX, maxY;
	
	public static class Info {
		public int time;
		public int[] values = new int[7];	// order pattern row num_rows frame speed bpm
		public int[] volumes = new int[64];
		public int[] finalvols = new int[64];
		public int[] pans = new int[64];
		public int[] instruments = new int[64];
		public int[] keys = new int[64];
		public int[] periods = new int[64];
	};
	
	private void limitPosition() {
		if (posX > maxX - canvasWidth) {
			posX = maxX - canvasWidth;
		}
		if (posX < 0) {
			posX = 0;
		}

		if (posY > maxY - canvasHeight) {
			posY = maxY - canvasHeight;
		}
		if (posY < 0) {
			posY = 0;
		}
	}

	private class MyGestureDetector extends SimpleOnGestureListener {

		public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY) {
			synchronized (this) {
				posX += distanceX;
				posY += distanceY;

				limitPosition();

				velX = velY = 0;
			}
			return true;
		}

		public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
			velX = velocityX / 25;
			velY = velocityY / 25;
			return true;
		}

		public boolean onSingleTapUp(final MotionEvent e) {
			onClick((int)e.getX(), (int)e.getY());
			return true;
		}

		public void onLongPress(final MotionEvent e) {
			onLongClick((int)e.getX(), (int)e.getY());
		}

		public boolean onDown(final MotionEvent e) {
			velX = velY = 0;		// stop fling
			return true;
		}
	}

	protected void updateScroll() {		// Hmpf, reinventing the wheel instead of using Scroller
		posX -= velX;
		posY -= velY;

		limitPosition();

		velX *= 0.9;
		if (Math.abs(velX) < 0.5) {
			velX = 0;
		}

		velY *= 0.9;
		if (Math.abs(velY) < 0.5) {
			velY = 0;
		}
	}

	public Viewer(final Context context) {
		super(context);
		this.context = context;

		// register our interest in hearing about changes to our surface
		final SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		surfaceHolder = holder;

		posX = posY = 0;
		isDown = false;

		// Gesture detection
		gestureDetector = new GestureDetector(context, new MyGestureDetector());
		final View.OnTouchListener gestureListener = new View.OnTouchListener() {
			public boolean onTouch(final View v, final MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		};

		setOnClickListener(Viewer.this); 
		setOnTouchListener(gestureListener);

		final ScreenSizeHelper screenSizeHelper = new ScreenSizeHelper();
		screenSize = screenSizeHelper.getScreenSize(context);
	}


	@Override
	public void onClick(final View view) {
		// do nothing
	}

	protected void onClick(final int x, final int y) {
		final View parent = (View)getParent();
		if (parent != null) {
			parent.performClick();
		}
	}

	protected void onLongClick(final int x, final int y) {
		// do nothing
	}

	public void setRotation(final int val) {
		rotation = val;
	}

	public void update(final Info info, final boolean paused) {
		updateScroll();
	}

	public void setup(final ModInterface modPlayer, final int[] modVars) {
		Log.i(TAG, "Viewer setup");
		
		final int chn = modVars[3];
		this.modVars = modVars;
		this.modPlayer = modPlayer;

		isMuted = new boolean[chn];
		for (int i = 0; i < chn; i++) {
			try {
				isMuted[i] = modPlayer.mute(i, -1) == 1;
			} catch (RemoteException e) {
				Log.e(TAG, "Can't read channel mute status");
			}
		}

		posX = posY = 0;
	}

	public void setMaxX(final int x) {
		synchronized (this) {
			maxX = x;
		}
	}

	public void setMaxY(final int y) {		
		synchronized (this) {
			maxY = y;
		}
	}

	/* Callback invoked when the surface dimensions change. */
	public void setSurfaceSize(final int width, final int height) {
		// synchronized to make sure these all change atomically
		synchronized (surfaceHolder) {
			canvasWidth = width;
			canvasHeight = height;
		}
	}

	@Override
	public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
		setSurfaceSize(width, height);
	}

	@Override
	public void surfaceCreated(final SurfaceHolder holder) {		
		surfaceHolder = holder;
	}

	@Override
	public void surfaceDestroyed(final SurfaceHolder holder) {
		// do nothing
	}
}
