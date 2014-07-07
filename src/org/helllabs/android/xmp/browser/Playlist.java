package org.helllabs.android.xmp.browser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.helllabs.android.xmp.preferences.Preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Playlist {
	private static final String COMMENT_SUFFIX = ".comment";
	private static final String PLAYLIST_SUFFIX = ".playlist";
	public static final String OPTIONS_PREFIX = "options_";
	private String mName;
	private String mComment;
	private List<String> mList = new ArrayList<String>();

	private static class ListFile extends File {
		public ListFile(final String name) {
			super(Preferences.DATA_DIR, name + PLAYLIST_SUFFIX);
		}
	}
	
	private static class CommentFile extends File {
		public CommentFile(final String name) {
			super(Preferences.DATA_DIR, name + COMMENT_SUFFIX);
		}
	}
	
	public Playlist(String name, String comment) {
		this.mName = name;
		this.mComment = comment;
	}	
	
	void commit() {
		
	}
	
	private static String optionName(final String name, final String option) {
		return PlaylistUtils.OPTIONS_PREFIX + name + option;
	}
	
	static boolean rename(final Context context, final String oldName, final String newName) {
		final File old1 = new ListFile(oldName);
		final File old2 = new CommentFile(oldName);
		final File new1 = new ListFile(newName);
		final File new2 = new CommentFile(newName);

		boolean error = false;
		
		if (!old1.renameTo(new1)) { 
			error = true;
		} else if (!old2.renameTo(new2)) {
			new1.renameTo(old1);
			error = true;
		}

		if (error) {
			return false;
		}
		
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		final SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(optionName(newName, "_shuffleMode"), prefs.getBoolean(optionName(oldName, "_shuffleMode"), true));
		editor.putBoolean(optionName(newName, "_loopMode"),	prefs.getBoolean(optionName(oldName, "_loopMode"), false));
		editor.remove(optionName(oldName, "_shuffleMode"));
		editor.remove(optionName(oldName, "_loopMode"));
		editor.commit();
		
		return true;
	}
	
	static void delete(Context context, final String name) {		
		(new ListFile(name)).delete();
		(new CommentFile(name)).delete();

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		final SharedPreferences.Editor editor = prefs.edit();
		editor.remove(optionName(name, "_shuffleMode"));
		editor.remove(optionName(name, "_loopMode"));
		editor.commit();
	}
	
	void add(final String line) {
		mList.add(line);
	}
	
	void add(final String[] lines) {
		for (final String line : lines) {
			add(line);
		}
	}
	
	void remove(int index) {
		mList.remove(index);
	}
	
	
	public String getName() {
		return mName;
	}
	
	public String getComment() {
		return mComment;
	}
	
	public void setComment(String comment) {
		this.mComment = comment;
	}
}
