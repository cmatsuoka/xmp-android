package org.helllabs.android.xmp;


public class Xmp {
	public class TestInfo {
		String name;
		String type;
		
		public ModInfo toModInfo(String filename) {
			ModInfo mi = new ModInfo();
			mi.filename = filename;
			mi.name = name;
			mi.type = type;
			return mi;
		}		
	}
	
	public static final int XMP_CTL_LOOP = 1 << 3;
	
	public native int init();
	public native int deinit();
	public native boolean testModule(String name, TestInfo info);
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
	public native int seek(long time);
	public native int time();
	public native int seek(int seconds);
	public native int getPlayTempo();
	public native int getPlayBpm();
	public native int getPlayPos();
	public native int getPlayPat();
	public native String getVersion();
	public native String getTitle();
	public native String[] getFormats();
	public native String[] getInstruments();
	public native void getChannelData(int[] volumes, int[] instruments, int[] keys);
	
	static {
		System.loadLibrary("xmp");
	}
}
