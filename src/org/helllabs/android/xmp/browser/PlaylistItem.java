package org.helllabs.android.xmp.browser;

public class PlaylistItem implements Comparable<PlaylistItem> {
	public String name;
	public String comment;
	public String filename;
	public int imageRes;
	
	public PlaylistItem(final String name, final String comment, final String filename) {
		this.name = name;
		this.comment = comment;
		this.filename = filename;
		this.imageRes = -1;
	}
	
	public PlaylistItem(final String name, final String comment, final int imageRes) {
		this.name = name;
		this.comment = comment;
		this.imageRes = imageRes;
	}
	
	public PlaylistItem(final String name, final String comment, final String filename, final int imageRes) {
		this.name = name;
		this.comment = comment;
		this.filename = filename;
		this.imageRes = imageRes;
	}
	
	public String toString() {
		return String.format("%s:%s:%s\n", filename, comment, name);
	}
	
	// Comparable
	
	public int compareTo(final PlaylistItem info) {
		return this.name.compareTo(info.name);
	}

}
