package org.helllabs.android.xmp;



public final class Xmp {
	
	public static final int XMP_PLAYER_AMP = 0;			// Amplification factor
	public static final int XMP_PLAYER_MIX = 1;			// Stereo mixing
	public static final int XMP_PLAYER_INTERP = 2;		// Interpolation type
	public static final int XMP_PLAYER_DSP = 3;			// DSP effect flags

	public static final int XMP_INTERP_NEAREST = 0;		// Nearest neighbor		// NOPMD
	public static final int XMP_INTERP_LINEAR = 1;		// Linear (default)
	public static final int XMP_INTERP_SPLINE = 2;		// Cubic spline
	
	public static final int XMP_DSP_LOWPASS = 1 << 0;	// Lowpass filter effect
	
	public static final int XMP_FORMAT_MONO = 1 << 2;
	
	private Xmp() {
		
	}
	
	public static native int init();
	public static native int deinit();
	public static native boolean testModule(String name, ModInfo info);
	public static native int loadModule(String name);
	public static native int releaseModule();
	public static native int startPlayer(int start, int rate, int flags);
	public static native int endPlayer();
	public static native int playFrame();	
	public static native int getBuffer(short buffer[]);
	public static native int nextPosition();
	public static native int prevPosition();
	public static native int setPosition(int num);
	public static native int stopModule();
	public static native int restartModule();
	public static native int seek(int time);
	public static native int time();
	public static native int mute(int chn, int status);
	public static native void getInfo(int[] values);
	public static native void setPlayer(int parm, int val);
	public static native int getLoopCount();
	public static native void getModVars(int[] vars);
	public static native String getVersion();
	public static native String getModName();
	public static native String getModType();
	public static native String[] getFormats();
	public static native String[] getInstruments();
	public static native void getChannelData(int[] volumes, int[] finalvols, int[] pans, int[] instruments, int[] keys, int[] periods);
	public static native void getPatternRow(int pat, int row, byte[] rowNotes, byte[] rowInstruments);
	public static native void getSampleData(boolean trigger, int ins, int key, int period, int chn, int width, byte[] buffer);
	
	static {
		System.loadLibrary("xmp-jni");
	}
}
