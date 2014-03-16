package org.helllabs.android.xmp.player;

import org.helllabs.android.xmp.ModInterface;

import android.content.Context;
import android.os.Build;
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
	protected int rotation;
	protected int screenSize;
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
	protected float posX, posY, velX, velY;
	protected Boolean isDown;
	private int maxX, maxY;

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

		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			synchronized (isDown) {
				posX += distanceX;
				posY += distanceY;

				limitPosition();

				velX = velY = 0;
			}
			return true;
		}

		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			velX = velocityX / 25;
			velY = velocityY / 25;
			return true;
		}

		public boolean onSingleTapUp(MotionEvent e) {
			onClick((int)e.getX(), (int)e.getY());
			return true;
		}

		public void onLongPress(MotionEvent e) {
			onLongClick((int)e.getX(), (int)e.getY());
		}

		public boolean onDown(MotionEvent e) {
			velX = velY = 0;		// stop fling
			return true;
		}
	}


	protected void updateScroll() {		// Hmpf, reinventing the wheel instead of using Scroller
		posX -= velX;
		posY -= velY;

		limitPosition();

		velX *= 0.9;
		if (Math.abs(velX) < 0.5)
			velX = 0;

		velY *= 0.9;
		if (Math.abs(velY) < 0.5)
			velY = 0;
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

		ScreenSizeHelper screenSizeHelper = new ScreenSizeHelper();
		screenSize = screenSizeHelper.getScreenSize(context);
	}


	@Override
	public void onClick(View v) {

	}

	protected void onClick(int x, int y) {
		View parent = (View)getParent();
		if (parent != null) {
			parent.performClick();
		}
	}

	protected void onLongClick(int x, int y) {
		// does nothing
	}

	public void setRotation(int n) {
		rotation = n;
	}

	public void update(Info info) {
		updateScroll();
	}

	public void setup(ModInterface modPlayer, int[] modVars) {
		final int chn = modVars[3];

		this.modVars = modVars;
		this.modPlayer = modPlayer;

		isMuted = new boolean[chn];
		for (int i = 0; i < chn; i++) {
			try {
				isMuted[i] = modPlayer.mute(i, -1) == 1;
			} catch (RemoteException e) { }
		}

		posX = posY = 0;
	}

	public void setMaxX(int x) {
		synchronized (isDown) {
			maxX = x;
		}
	}

	public void setMaxY(int y) {		
		synchronized (isDown) {
			maxY = y;
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
