package org.helllabs.android.xmp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.helllabs.android.xmp.Watchdog.onTimeoutListener;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;


public class ModService extends Service {
	final Xmp xmp = new Xmp();
	AudioTrack audio;
	Thread playThread;
	SharedPreferences prefs;
	Watchdog watchdog;
	int minSize;
	int sampleRate, sampleFormat;
	boolean stereo;
	boolean interpolate;
	Notifier notifier;
	boolean stopPlaying = false;
	static boolean isPlaying = false;
	boolean restartList;
	boolean returnToPrev;
	boolean paused;
	boolean looped;
	Boolean updateData = false;
	String fileName;			// currently playing file
	String currentTitle;
	QueueManager queue;
    final RemoteCallbackList<PlayerCallback> callbacks =
		new RemoteCallbackList<PlayerCallback>();
    boolean autoPaused = false;		// paused on phone call
    XmpPhoneStateListener listener;
    TelephonyManager tm;
    
    // for media buttons
    private AudioManager audioManager;
    private ComponentName remoteControlResponder;
    private static Method registerMediaButtonEventReceiver;
    private static Method unregisterMediaButtonEventReceiver;

	static {
		initializeRemoteControlRegistrationMethods();
	}
    
    @Override
	public void onCreate() {
    	super.onCreate();
    	
    	Log.i("Xmp ModService", "Create service");
    	
   		prefs = PreferenceManager.getDefaultSharedPreferences(this);
   		
   		int bufferMs = prefs.getInt(Settings.PREF_BUFFER_MS, 500);
   		sampleRate = Integer.parseInt(prefs.getString(Settings.PREF_SAMPLING_RATE, "44100"));
   		sampleFormat = 0;
   		
   		stereo = prefs.getBoolean(Settings.PREF_STEREO, true);
   		if (!stereo) {
   			sampleFormat |= Xmp.XMP_FORMAT_MONO;
   		}
   		
   		int bufferSize = (sampleRate * (stereo ? 2 : 1) * 2 * bufferMs / 1000) & ~0x3;
	
   		int channelConfig = stereo ?
   				AudioFormat.CHANNEL_CONFIGURATION_STEREO :
   				AudioFormat.CHANNEL_CONFIGURATION_MONO;
   		
		minSize = AudioTrack.getMinBufferSize(
				sampleRate,
				channelConfig,
				AudioFormat.ENCODING_PCM_16BIT);

		audio = new AudioTrack(
				AudioManager.STREAM_MUSIC, sampleRate,
				channelConfig,
				AudioFormat.ENCODING_PCM_16BIT,
				minSize < bufferSize ? bufferSize : minSize,
				AudioTrack.MODE_STREAM);

		xmp.init();

		isPlaying = false;
		paused = false;
		
		notifier = new Notifier();
		listener = new XmpPhoneStateListener(this);
		
		tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		tm.listen(listener, XmpPhoneStateListener.LISTEN_CALL_STATE);
		
		audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		remoteControlResponder = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());
		registerRemoteControl();
		
		watchdog = new Watchdog(10);
 		watchdog.setOnTimeoutListener(new onTimeoutListener() {
			public void onTimeout() {
				Log.e("Xmp ModService", "Stopped by watchdog");
		    	stopSelf();
			}
		});
 		watchdog.start();
    }

    @Override
	public void onDestroy() {
    	unregisterRemoteControl();
    	watchdog.stop();
    	notifier.cancel();
    	end();
    }

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	private void checkMediaButtons() {
		int key = RemoteControlReceiver.keyCode;
		
		if (key > 0) {
			switch (key) {
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				Log.i("Xmp ModService", "Handle KEYCODE_MEDIA_NEXT");
				xmp.stopModule();
				stopPlaying = false;
				paused = false;
				break;
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				Log.i("Xmp ModService", "Handle KEYCODE_MEDIA_PREVIOUS");
				if (xmp.time() > 2000) {
					xmp.seek(0);
				} else {
					xmp.stopModule();
					returnToPrev = true;
					stopPlaying = false;
				}
				paused = false;
				break;
			case KeyEvent.KEYCODE_MEDIA_STOP:
				Log.i("Xmp ModService", "Handle KEYCODE_MEDIA_STOP");
		    	xmp.stopModule();
		    	paused = false;
		    	stopPlaying = true;
		    	break;
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				Log.i("Xmp ModService", "Handle KEYCODE_MEDIA_PLAY_PAUSE");
				paused = !paused;
				break;
			}
			
			RemoteControlReceiver.keyCode = -1;
		}
	}
	
	private class PlayRunnable implements Runnable {
    	public void run() {
    		do {
    			fileName = queue.getFilename();		// Used in reconnection
    			
    			if (!InfoCache.testModule(fileName)) {
    				Log.w("Xmp ModService", fileName + ": unrecognized format");
    				continue;
    			}
    			
	    		Log.i("Xmp ModService", "Load " + fileName);
	       		if (xmp.loadModule(fileName) < 0) {
	       			Log.e("Xmp ModService", "Error loading " + fileName);
	       			continue;
	       		}

	       		notifier.notification(xmp.getModName(), queue.index());
		       		    	
	        	final int numClients = callbacks.beginBroadcast();
	        	for (int j = 0; j < numClients; j++) {
	        		try {
	    				callbacks.getBroadcastItem(j).newModCallback(
	    							fileName, xmp.getInstruments());
	    			} catch (RemoteException e) { }
	        	}
	        	callbacks.finishBroadcast();

	        	String volBoost = prefs.getString(Settings.PREF_VOL_BOOST, "1");

	       		audio.play();
	       		xmp.startPlayer(0, sampleRate, sampleFormat);
	        	xmp.setMixerAmp(Integer.parseInt(volBoost));
	        	xmp.setMixerMix(prefs.getInt(Settings.PREF_PAN_SEPARATION, 70));
	       		updateData = true;
	    			    		
	    		short buffer[] = new short[minSize];
	    		
	    		int count, loopCount = 0;
	       		while (xmp.playFrame() == 0) {
	       			count = xmp.getLoopCount();
	       			if (!looped && count != loopCount)
	       				break;
	       			loopCount = count;
	       			
	       			int size = xmp.getBuffer(buffer);
	       			audio.write(buffer, 0, size);
	       			
	       			while (paused) {
	       				audio.pause();
	       				watchdog.refresh();
	       				try {
							Thread.sleep(500);
							checkMediaButtons();
						} catch (InterruptedException e) {
							break;
						}
	       			}
	       			audio.play();
	       			
	       			watchdog.refresh();
	       			checkMediaButtons();
	       		}

	       		xmp.endPlayer();     		
	       		xmp.releaseModule();
       		
	       		audio.stop();
	       		
	       		if (restartList) {
	       			queue.restart();
	       			restartList = false;
	       			continue;
	       		}
	       		
	       		if (returnToPrev) {
	       			queue.previous();
	       			returnToPrev = false;
	       			continue;
	       		}
    		} while (!stopPlaying && queue.next());

    		synchronized (updateData) {
    			updateData = false;		// stop getChannelData update
    		}
    		watchdog.stop();
    		notifier.cancel();
        	end();
        	stopSelf();
    	}
    }

	protected void end() {    	
		Log.i("Xmp ModService", "End service");
	    final int numClients = callbacks.beginBroadcast();
	    for (int i = 0; i < numClients; i++) {
	    	try {
				callbacks.getBroadcastItem(i).endPlayCallback();
			} catch (RemoteException e) { }
	    }	    
	    callbacks.finishBroadcast();

	    isPlaying = false;
    	xmp.stopModule();
    	paused = false;

    	if (playThread != null && playThread.isAlive()) {
    		try {
    			playThread.join();
    		} catch (InterruptedException e) { }
    	}
    	
    	xmp.deinit();
    	audio.release();
    }
	
	private class Notifier {
	    private NotificationManager nm;
	    PendingIntent contentIntent;
	    private static final int NOTIFY_ID = R.layout.player;
		String title;
		int index;

		public Notifier() {
			nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
	    	contentIntent = PendingIntent.getActivity(ModService.this, 0,
					new Intent(ModService.this, Player.class), 0);
		}
		
		private String message() {
			return queue.size() > 1 ?
				String.format("%s (%d/%d)", title, index, queue.size()) :
				title;
		}
		
		public void cancel() {
			nm.cancel(NOTIFY_ID);
		}
		
		public void notification() {
			notification(null, null);
		}
		
		public void notification(String title, int index) {
			this.title = title;
			this.index = index + 1;			
			notification(message(), message());
		}
		
		public void notification(String ticker) {
			notification(ticker, message());
		}
		
		public void notification(String ticker, String latest) {
	        Notification notification = new Notification(
	        		R.drawable.notification, ticker, System.currentTimeMillis());
	        notification.setLatestEventInfo(ModService.this, getText(R.string.app_name),
	        		latest, contentIntent);
	        notification.flags |= Notification.FLAG_ONGOING_EVENT;	        
	        nm.notify(NOTIFY_ID, notification);				
		}
	}

	private final ModInterface.Stub binder = new ModInterface.Stub() {
		public void play(String[] files, boolean shuffle, boolean loopList) {	
			notifier.notification();
			queue = new QueueManager(files, shuffle, loopList);
			returnToPrev = false;
			stopPlaying = false;
			paused = false;

			if (isPlaying) {
				Log.i("Xmp ModService", "Use existing player thread");
				restartList = true;
				nextSong();
			} else {
				Log.i("Xmp ModService", "Start player thread");
				restartList = false;
		   		playThread = new Thread(new PlayRunnable());
		   		playThread.start();
			}
			isPlaying = true;
		}
		
		public void add(String[] files) {	
			queue.add(files);
			notifier.notification("Added to play queue");			
		}
	    
	    public void stop() {
	    	xmp.stopModule();
	    	paused = false;
	    	stopPlaying = true;
	    }
	    
	    public void pause() {
	    	paused = !paused;
	    }
	    
	    public int time() {
	    	return xmp.time() / 100;
	    }
	
		public void seek(int seconds) {
			xmp.seek(seconds);
		}
		
		public int getPlaySpeed() {
			return xmp.getPlaySpeed();
		}
		
		public int getPlayBpm() {
			return xmp.getPlayBpm();
		}
		
		public int getPlayPos() {
			return xmp.getPlayPos();
		}
		
		public int getPlayPat() {
			return xmp.getPlayPat();
		}
		
		public void getModVars(int[] vars) {
			xmp.getModVars(vars);
		}
		
		public String getModName() {
			return xmp.getModName();
		}
		
		public String getModType() {
			return xmp.getModType();
		}
		
		public void getChannelData(int[] volumes, int[] instruments, int[] keys) {
			synchronized (updateData) {
				if (updateData) {
					xmp.getChannelData(volumes, instruments, keys);
				}
			}
		}
		
		public void nextSong() {
			xmp.stopModule();
			stopPlaying = false;
			paused = false;
		}
		
		public void prevSong() {
			xmp.stopModule();
			returnToPrev = true;
			stopPlaying = false;
			paused = false;
		}

		public boolean toggleLoop() throws RemoteException {
			looped = !looped;
			return looped;
		}
		
		public boolean isPaused() {
			return paused;
		}
		
		// for Reconnection
		
		public String getFileName() {
			return fileName;
		}
		
		public String[] getInstruments() {
			return xmp.getInstruments();
		}
		
		
		// File management
		
		public boolean deleteFile() {
			Log.i("Xmp ModService", "Delete file " + fileName);
			return InfoCache.delete(fileName);
		}

		
		// Callback
		
		public void registerCallback(PlayerCallback cb) {
        	if (cb != null)
            	callbacks.register(cb);
        }
        
        public void unregisterCallback(PlayerCallback cb) {
            if (cb != null)
            	callbacks.unregister(cb);
        }
	};
	
	
	// for Telephony
	
	public boolean autoPause(boolean pause) {
		Log.i("Xmp ModService", "Auto pause changed to " + pause + ", previously " + autoPaused);
		if (pause) {
			paused = autoPaused = true;
		} else {
			if (autoPaused) {
				paused = autoPaused = false;
			}
		}	
		
		return autoPaused;
	}
	
	// for media buttons
	// see http://android-developers.blogspot.com/2010/06/allowing-applications-to-play-nicer.html
	
	private static void initializeRemoteControlRegistrationMethods() {
		try {
			if (registerMediaButtonEventReceiver == null) {
				registerMediaButtonEventReceiver = AudioManager.class
						.getMethod("registerMediaButtonEventReceiver",
								new Class[] { ComponentName.class });
			}
			if (unregisterMediaButtonEventReceiver == null) {
				unregisterMediaButtonEventReceiver = AudioManager.class
						.getMethod("unregisterMediaButtonEventReceiver",
								new Class[] { ComponentName.class });
			}
			/* success, this device will take advantage of better remote */
			/* control event handling */
		} catch (NoSuchMethodException nsme) {
			/* failure, still using the legacy behavior, but this app */
			/* is future-proof! */
		}
	}

	private void registerRemoteControl() {
		try {
			if (registerMediaButtonEventReceiver == null) {
				return;
			}
			registerMediaButtonEventReceiver.invoke(audioManager, remoteControlResponder);
		} catch (InvocationTargetException ite) {
			/* unpack original exception when possible */
			Throwable cause = ite.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			} else if (cause instanceof Error) {
				throw (Error) cause;
			} else {
				/* unexpected checked exception; wrap and re-throw */
				throw new RuntimeException(ite);
			}
		} catch (IllegalAccessException ie) {
			Log.e("Xmp ModService", "Unexpected " + ie);
		}
	}

	private void unregisterRemoteControl() {
		try {
			if (unregisterMediaButtonEventReceiver == null) {
				return;
			}
			unregisterMediaButtonEventReceiver.invoke(audioManager,	remoteControlResponder);
		} catch (InvocationTargetException ite) {
			/* unpack original exception when possible */
			Throwable cause = ite.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			} else if (cause instanceof Error) {
				throw (Error) cause;
			} else {
				/* unexpected checked exception; wrap and re-throw */
				throw new RuntimeException(ite);
			}
		} catch (IllegalAccessException ie) {
			Log.e("Xmp ModService", "Unexpected " + ie);
		}
	}
}
