package org.helllabs.android.xmp;

import org.helllabs.android.xmp.PlayerCallback;

interface ModInterface {
	void play(in String[] files, boolean shuffle, boolean loopList);
	void add(in String[] files);
	void stop();
	void pause();
	void getInfo(out int[] values);
	void seek(in int seconds);
	int time();
	void getModVars(out int[] vars);
	String getModName();
	String getModType();
	void getChannelData(out int[] volumes, out int[] instruments, out int[] keys);
	void nextSong();
	void prevSong(); 
	boolean isPaused();
	boolean toggleLoop();
	String getFileName();
	String[] getInstruments();
	void getPatternRow(int pat, int row, out byte[] rowNotes, out byte[] rowInstruments);
	boolean deleteFile();
	
	void registerCallback(PlayerCallback cb);
	void unregisterCallback(PlayerCallback cb);
}