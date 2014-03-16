package org.helllabs.android.xmp.browser;

public class PlaylistInfo implements Comparable<PlaylistInfo> {
	public final String name;
	public String comment;
	public String filename;
	public int imageRes;
	
	public PlaylistInfo(final String name, final String comment, final String filename) {
		this.name = name;
		this.comment = comment;
		this.filename = filename;
		this.imageRes = -1;
	}
	
	public PlaylistInfo(final String name, final String comment, final int imageRes) {
		this.name = name;
		this.comment = comment;
		this.imageRes = imageRes;
	}
	
	public PlaylistInfo(final String name, final String comment, final String filename, final int imageRes) {
		this.name = name;
		this.comment = comment;
		this.filename = filename;
		this.imageRes = imageRes;
	}
	
	// Comparable
	
	public int compareTo(final PlaylistInfo info) {
		return this.name.compareTo(info.name);
	}

}
