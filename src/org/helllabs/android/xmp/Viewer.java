package org.helllabs.android.xmp;

import android.content.Context;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;

public abstract class Viewer extends SurfaceView {
	
    public class Info {
    	int time;
    	int[] values = new int[7];	// order pattern row num_rows frame speed bpm
    	int[] volumes = new int[64];
    	int[] instruments = new int[64];
    	int[] keys = new int[64];
    };

    protected int deltaX, deltaY;
    protected int downX, downY;
    protected int posX, posY;
    protected Boolean isDown;
    
	public Viewer(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		
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
	

	public void update(ModInterface modPlayer, Info info) {
		
	}
	
	public void setup(int[] modVars) {
		
	}
}
