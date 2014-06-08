package org.helllabs.android.xmp.service;

public class Watchdog implements Runnable {
	private int timer;
	private boolean running;
	private Thread thread;
	private OnTimeoutListener listener;
	private final int timeout;
	
	public Watchdog(final int timeout) {
		this.timeout = timeout; 
	}
	
	public interface OnTimeoutListener {
		void onTimeout();
	}
	
	public void setOnTimeoutListener(final OnTimeoutListener listener) {
		this.listener = listener;
	}
	
	public void run() {
		while (running) {
			if (--timer <= 0) {
				listener.onTimeout();
				break;
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {	}
		}
	}
	
	public void start() {
		running = true;
		refresh();
		thread = new Thread(this);
		thread.start();
	}
	
	public void stop() {
		running = false;
		try {
			thread.join();
		} catch (InterruptedException e) { }
	}
	
	public void refresh() {
		timer = timeout;
	}
}
