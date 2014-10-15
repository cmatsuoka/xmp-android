package org.helllabs.android.xmp.browser.playlist;

public class PlaylistItem implements Comparable<PlaylistItem> {
	public static final int TYPE_DIRECTORY = 1;
	public static final int TYPE_PLAYLIST = 2;
	public static final int TYPE_FILE = 3;
	
	private final int type;
	private final String name;
	private final String comment;
	private String filename;
	private int imageRes;

	public PlaylistItem(final int type, final String name, final String comment) {
		this.type = type;
		this.name = name;
		this.comment = comment;
	}
	
	public String toString() {
		return String.format("%s:%s:%s\n", filename, comment, name);
	}
	
	// Comparable
	
	public int compareTo(final PlaylistItem info) {
		return this.name.compareTo(info.name);
	}

	// Accessors
	
	public int getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
	
	public String getComment() {
		return comment;
	}
	
	public String getFilename() {
		return filename;
	}
	
	public void setFilename(final String filename) {
		this.filename = filename;
	}
	
	public int getImageRes() {
		return imageRes;
	}
	
	public void setImageRes(final int imageRes) {
		this.imageRes = imageRes;
	}
}
