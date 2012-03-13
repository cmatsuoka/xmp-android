package org.helllabs.android.xmp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

// http://developer.android.com/guide/topics/graphics/2d-graphics.html

public class PatternViewer extends SurfaceView implements SurfaceHolder.Callback {
	private Context context;
	private PatternThread thread = null;

	class PatternThread extends Thread {
        private SurfaceHolder surfaceHolder;        
        private int canvasHeight, canvasWidth;
        private Paint notePaint;
        private Boolean running = false;
        
        public PatternThread(SurfaceHolder surfaceHolder, Context c) {
        	this.surfaceHolder = surfaceHolder;
        	context = c;
        	
        	notePaint = new Paint();
        	notePaint.setARGB(255, 0, 255, 0);
        }

		/* Callback invoked when the surface dimensions change. */
		public void setSurfaceSize(int width, int height) {
			// synchronized to make sure these all change atomically
			synchronized (surfaceHolder) {
				Log.i("Xmp PatternViewer", "width=" + width + " height=" + height);
				canvasWidth = width;
				canvasHeight = height;
			}
		}
		
		@Override
        public void run() {
            while (running) {
            	//Log.i("asd", "is running");
                Canvas c = null;
                try {
                    c = surfaceHolder.lockCanvas(null);
                    synchronized (surfaceHolder) {
                       doDraw(c);
                    }
				} finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        surfaceHolder.unlockCanvasAndPost(c);
                    }
                }
                try {
					sleep(210);
				} catch (InterruptedException e) {

				}
            }
        }
		
		private void doDraw(Canvas canvas) {
			canvas.drawLine(10, 10, 20, 20, notePaint);
		}

		public void setRunning(boolean b) {
			running = b;
		}
	}


	public PatternViewer(Context context) {
		super(context);
		
		Log.d("Xmp PatternViewer", "PatternViewer constructor");
		
		// register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        thread = new PatternThread(holder, context);

		/*FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
		this.setLayoutParams(params);*/
	}

    // create thread only; it's started in surfaceCreated()

    
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		thread.setSurfaceSize(width, height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// start the thread here so that we don't busy-wait in run()
		// waiting for the surface to be created

		Log.d("Xmp PatternViewer", "surfaceCreated");
		if (thread == null || !thread.isAlive()) {
			thread =new PatternThread(holder, context);
		}
		thread.setRunning(true);
		thread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// we have to tell thread to shut down & wait for it to finish, or else
		// it might touch the Surface after we return and explode
		
		Log.d("Xmp PatternViewer", "surfaceDestroyed");
		
		boolean retry = true;
		thread.setRunning(false);
		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
	}
}
