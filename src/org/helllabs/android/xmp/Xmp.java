package org.helllabs.android.xmp;


public class Xmp {
	public static final int XMP_FORMAT_MONO = 1 << 2;
	
	public native int init();
	public native int deinit();
	public native static boolean testModule(String name, ModInfo info);
	public native int loadModule(String name);
	public native int releaseModule();
	public native int startPlayer(int start, int rate, int flags);
	public native int endPlayer();
	public native int playFrame();	
	public native int getBuffer(short buffer[]);
	public native int nextOrd();
	public native int prevOrd();
	public native int setOrd(int n);
	public native int stopModule();
	public native int restartModule();
	public native int incGvol();
	public native int decGvol();
	public native int seek(int time);
	public native int time();
	public native void getInfo(int[] values);
	public native void setMixerAmp(int amp);
	public native void setMixerMix(int mix);
	public native int getLoopCount();
	public native void getModVars(int[] vars);
	public native static String getVersion();
	public native String getModName();
	public native String getModType();
	public native String[] getFormats();
	public native String[] getInstruments();
	public native void getChannelData(int[] volumes, int[] instruments, int[] keys);
	
	static {
		System.loadLibrary("xmp");
	}
}
