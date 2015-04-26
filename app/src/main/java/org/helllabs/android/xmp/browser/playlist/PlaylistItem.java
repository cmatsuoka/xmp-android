package org.helllabs.android.xmp.browser.playlist;

import java.io.File;

import org.helllabs.android.xmp.R;

public class PlaylistItem implements Comparable<PlaylistItem> {
	public static final int TYPE_DIRECTORY = 1;
	public static final int TYPE_PLAYLIST = 2;
	public static final int TYPE_FILE = 3;
	public static final int TYPE_SPECIAL = 4;

	private int id;
	private final int type;
	private final String name;
	private final String comment;
	private File file;
	private int imageRes;

	public PlaylistItem(final int type, final String name, final String comment) {
		this.type = type;
		this.name = name;
		this.comment = comment;

		switch (type) {
		case TYPE_DIRECTORY:
			imageRes = R.drawable.folder;
			break;
		case TYPE_PLAYLIST:
			imageRes = R.drawable.list;
			break;
		case TYPE_FILE:
			imageRes = R.drawable.file;
			break;
		default:
			imageRes = -1;
			break;
		}
	}
	
	public String toString() {
		return String.format("%s:%s:%s\n", file.getPath(), comment, name);
	}
	
	// Comparable
	
	public int compareTo(final PlaylistItem info) {
		final boolean d1 = this.file.isDirectory();
		final boolean d2 = info.file.isDirectory();
		
		if (d1 ^ d2) {
			return d1 ? -1 : 1;
		} else {
			return this.name.compareTo(info.name);
		}
	}

	// Accessors

	public int getId() {
		return id;
	}

	public void setId(final int id) {
		this.id = id;
	}
	
	public int getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
	
	public String getComment() {
		return comment;
	}
	
	public File getFile() {
		return file;
	}
	
	public void setFile(final File file) {
		this.file = file;
	}

	public int getImageRes() {
		return imageRes;
	}
	
	public void setImageRes(final int imageRes) {
		this.imageRes = imageRes;
	}
}
