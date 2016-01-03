package org.helllabs.android.xmp.browser.playlist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.preferences.Preferences;
import org.helllabs.android.xmp.util.FileUtils;
import org.helllabs.android.xmp.util.InfoCache;
import org.helllabs.android.xmp.util.Log;
import org.helllabs.android.xmp.util.Message;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Playlist {
	private static final String TAG = "Playlist";
	public static final String COMMENT_SUFFIX = ".comment";
	public static final String PLAYLIST_SUFFIX = ".playlist";
	private static final String OPTIONS_PREFIX = "options_";
	private static final String SHUFFLE_MODE = "_shuffleMode";
	private static final String LOOP_MODE = "_loopMode";
	private static final boolean DEFAULT_SHUFFLE_MODE = true;
	private static final boolean DEFAULT_LOOP_MODE = false;
	
	private final String mName;
	private String mComment;
	private boolean mListChanged;
	private boolean mCommentChanged;
	private boolean mShuffleMode;
	private boolean mLoopMode;
	private final List<PlaylistItem> mList;
	private final SharedPreferences mPrefs;

	@SuppressWarnings("serial")
	private static class ListFile extends File {
		public ListFile(final String name) {
			super(Preferences.DATA_DIR, name + PLAYLIST_SUFFIX);
		}
		
		public ListFile(final String name, final String suffix) {
			super(Preferences.DATA_DIR, name + PLAYLIST_SUFFIX + suffix);
		}
	}
	
	@SuppressWarnings("serial")
	private static class CommentFile extends File {
		public CommentFile(final String name) {
			super(Preferences.DATA_DIR, name + COMMENT_SUFFIX);
		}
		
		public CommentFile(final String name, final String suffix) {
			super(Preferences.DATA_DIR, name + COMMENT_SUFFIX + suffix);
		}
	}
	
	public Playlist(final Context context, final String name) throws IOException {
		mName = name;
		mList = new ArrayList<PlaylistItem>();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		final File file = new ListFile(name);
		if (file.exists()) {
			Log.i(TAG, "Read playlist " + name);
			final String comment = FileUtils.readFromFile(new CommentFile(name));
				
			// read list contents
			if (readList(name)) {
				mComment = comment;
				mShuffleMode = readShuffleModePref(name);
				mLoopMode = readLoopModePref(name);
			}
		} else {
			Log.i(TAG, "New playlist " + name);
			mShuffleMode = DEFAULT_SHUFFLE_MODE;
			mLoopMode = DEFAULT_LOOP_MODE;
			mListChanged = true;
			mCommentChanged = true;
		}
		
		if (mComment == null) {
			mComment = "";
		}
	}	
	
	/**
	 * Save the current playlist.
	 */
	public void commit() {
		Log.i(TAG, "Commit playlist " + mName);
		if (mListChanged) {
			writeList(mName);
			mListChanged = false;
		}
		if (mCommentChanged) {
			writeComment(mName);
			mCommentChanged = false;
		}
		
		boolean saveModes = false;
		if (mShuffleMode != readShuffleModePref(mName)) {
			saveModes = true;
		}
		if (mLoopMode != readLoopModePref(mName)) {
			saveModes = true;
		}
		if (saveModes) {
			final SharedPreferences.Editor editor = mPrefs.edit();
			editor.putBoolean(optionName(mName, SHUFFLE_MODE), mShuffleMode);
			editor.putBoolean(optionName(mName, LOOP_MODE), mLoopMode);
			editor.apply();
		}
	}
	
//	/**
//	 * Add a new item to the playlist.
//	 * 
//	 * @param item The item to be added
//	 */
//	public void add(final PlaylistItem item) {
//		mList.add(item);
//	}
//	
//	/**
//	 * Add new items to the playlist.
//	 * 
//	 * @param items The items to be added
//	 */
//	public void add(final PlaylistItem[] items) {
//		for (final PlaylistItem item : items) {
//			add(item);
//		}
//	}
	
	/**
	 * Remove an item from the playlist.
	 * 
	 * @param index The index of the item to be removed
	 */
	public void remove(final int index) {
		Log.i(TAG, "Remove item #" + index + ": " + mList.get(index).getName());
		mList.remove(index);
		mListChanged = true;
	}
	

	// Static utilities
	
	/**
	 * Rename a playlist.
	 * 
	 * @param context The context we're running in
	 * @param oldName The current name of the playlist
	 * @param newName The new name of the playlist
	 * 
	 * @return Whether the rename was successful
	 */
	public static boolean rename(final Context context, final String oldName, final String newName) {
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
		editor.putBoolean(optionName(newName, SHUFFLE_MODE), prefs.getBoolean(optionName(oldName, SHUFFLE_MODE), DEFAULT_SHUFFLE_MODE));
		editor.putBoolean(optionName(newName, LOOP_MODE), prefs.getBoolean(optionName(oldName, LOOP_MODE), DEFAULT_LOOP_MODE));
		editor.remove(optionName(oldName, SHUFFLE_MODE));
		editor.remove(optionName(oldName, LOOP_MODE));
		editor.apply();
		
		return true;
	}
	
	/**
	 * Delete the specified playlist.
	 * 
	 * @param context The context the playlist is being created in
	 * @param name The playlist name
	 */
	public static void delete(final Context context, final String name) {		
		(new ListFile(name)).delete();
		(new CommentFile(name)).delete();

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		final SharedPreferences.Editor editor = prefs.edit();
		editor.remove(optionName(name, SHUFFLE_MODE));
		editor.remove(optionName(name, LOOP_MODE));
		editor.apply();
	}
	
	/**
	 * Add an item to the specified playlist file.
	 * 
	 * @param context The context we're running in
	 * @param name The playlist name
	 * @param item The playlist item to add
	 */
	/*public static void addToList(final Context context, final String name, final PlaylistItem item) {
		try {
			FileUtils.writeToFile(new File(Preferences.DATA_DIR, name + PLAYLIST_SUFFIX), item.toString());
		} catch (IOException e) {
			Message.error(context, context.getString(R.string.error_write_to_playlist));
		}
	}*/
	
	/**
	 * Add a list of items to the specified playlist file.
	 * 
	 * @param context The context we're running in
	 * @param name The playlist name
	 * @param items The list of playlist items to add
	 */
	public static void addToList(final Activity activity, final String name, final List<PlaylistItem> items) {
		final String[] lines = new String[items.size()];
		
		int i = 0;
		for (final PlaylistItem item : items) {
			lines[i++] = item.toString();
		}
		try {
			FileUtils.writeToFile(new File(Preferences.DATA_DIR, name + PLAYLIST_SUFFIX), lines);
		} catch (IOException e) {
			Message.error(activity, activity.getString(R.string.error_write_to_playlist));
		}
	}
	
	/**
	 * Read comment from a playlist file.
	 * 
	 * @param context The context we're running in
	 * @param name The playlist name
	 * 
	 * @return The playlist comment
	 */
	public static String readComment(final Activity activity, final String name) {
		String comment = null;
		try {
			comment = FileUtils.readFromFile(new CommentFile(name));
		} catch (IOException e) {
			Message.error(activity, activity.getString(R.string.error_read_comment));
		}	    
	    if (comment == null || comment.trim().isEmpty()) {
	    	comment = activity.getString(R.string.no_comment);
	    }
		return comment;		
	}
	
	
	// Helper methods
	
	private boolean readList(final String name) {
		mList.clear();
		
		final File file = new ListFile(name);
		String line;
		int lineNum;
		
		final List<Integer> invalidList = new ArrayList<Integer>();
		
	    try {
	    	final BufferedReader reader = new BufferedReader(new FileReader(file), 512);
	    	lineNum = 0;
	    	while ((line = reader.readLine()) != null) {
	    		final String[] fields = line.split(":", 3);
	    		final String filename = fields[0];
	    		final String comment = fields.length > 1 ? fields[1] : "";
	    		final String title = fields.length > 2 ? fields[2] : "";
	    		if (InfoCache.fileExists(filename)) {
	    			final PlaylistItem item = new PlaylistItem(PlaylistItem.TYPE_FILE, title, comment);	// NOPMD
	    			item.setFile(new File(filename));	// NOPMD
	    			item.setImageRes(R.drawable.grabber);
	    			mList.add(item);
	    		} else {
	    			invalidList.add(lineNum);
	    		}
	    		lineNum++;
	    	}
	    	reader.close();
		    PlaylistUtils.renumberIds(mList);
	    } catch (IOException e) {
	    	Log.e(TAG, "Error reading playlist " + file.getPath());
	    	return false;
	    }		
		
	    if (!invalidList.isEmpty()) {
	    	final int[] array = new int[invalidList.size()];
	    	final Iterator<Integer> iterator = invalidList.iterator();
	    	for (int i = 0; i < array.length; i++) {
	    		array[i] = iterator.next().intValue();
	    	}
	    	
			try {
				FileUtils.removeLineFromFile(file, array);
			} catch (FileNotFoundException e) {
				Log.e(TAG, "Playlist file " + file.getPath() + " not found");
			} catch (IOException e) {
				Log.e(TAG, "I/O error removing invalid lines from " + file.getPath());
			}
		}
	    
	    return true;
	}
	
	private final void writeList(final String name) {
		Log.i(TAG, "Write list");
		final File file = new ListFile(name,  ".new");
		file.delete();
		
		try {
			final BufferedWriter out = new BufferedWriter(new FileWriter(file), 512);
			for (final PlaylistItem item : mList) {
				out.write(item.toString());
			}
			out.close();
			
			final File oldFile = new ListFile(name);
			oldFile.delete();
			file.renameTo(oldFile);
		} catch (IOException e) {
			Log.e(TAG, "Error writing playlist file " + file.getPath());
		}
	}

	private final void writeComment(final String name) {
		Log.i(TAG, "Write comment");
		final File file = new CommentFile(name,  ".new");
		file.delete();		
		try {
			FileUtils.writeToFile(file, mComment);
			final File oldFile = new CommentFile(name);
			oldFile.delete();
			file.renameTo(oldFile);
		} catch (IOException e) {
			Log.e(TAG, "Error writing comment file " + file.getPath());
		}
	}
		
	private static String optionName(final String name, final String option) {
		return OPTIONS_PREFIX + name + option;
	}
	
	private boolean readShuffleModePref(final String name) {
		return mPrefs.getBoolean(optionName(name, SHUFFLE_MODE), DEFAULT_SHUFFLE_MODE);
	}
	
	private boolean readLoopModePref(final String name) {
		return mPrefs.getBoolean(optionName(name, LOOP_MODE), DEFAULT_LOOP_MODE);
	}
	

	// Accessors
	
	public String getName() {
		return mName;
	}
	
	public String getComment() {
		return mComment;
	}
	
	public List<PlaylistItem> getList() {
		return mList;
	}
	
	public boolean isLoopMode() {
		return mLoopMode;
	}
	
	public boolean isShuffleMode() {
		return mShuffleMode;
	}
	
	public void setComment(final String comment) {
		mComment = comment;
	}
	
	public void setLoopMode(final boolean loopMode) {
		mLoopMode = loopMode;
	}
	
	public void setShuffleMode(final boolean shuffleMode) {
		mShuffleMode = shuffleMode;
	}
	
	public void setListChanged(final boolean listChanged) {
		mListChanged = listChanged;
	}
}
