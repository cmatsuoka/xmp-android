package org.helllabs.android.xmp.browser;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.helllabs.android.xmp.util.Log;

import java.io.File;
import java.util.Stack;

public class FilelistNavigation {

	private static final String TAG = "FilelistNavigation";
	private final Stack<ListState> mPathStack;
	private File mCurrentDir;

	/**
	 * To restore list position when traversing directories.
	 */
	private static class ListState {
		private final int index;
		private final int top;

		public ListState(final RecyclerView recyclerView) {
			final LinearLayoutManager layoutManager = (LinearLayoutManager)recyclerView.getLayoutManager();
			this.index = layoutManager.findFirstVisibleItemPosition();
			final View view = recyclerView.getChildAt(0);
			this.top = view == null ? 0 : view.getTop();
		}
		
		public void restoreState(final RecyclerView recyclerView) {
			final LinearLayoutManager layoutManager = (LinearLayoutManager)recyclerView.getLayoutManager();
			recyclerView.post(new Runnable() {
				@Override
				public void run() {
					layoutManager.scrollToPositionWithOffset(index, top);
				}
			});
		}
	}
	
	public FilelistNavigation() {
		mPathStack = new Stack<>();
	}
	
	/**
	 * Change to the specified directory.
	 * 
	 * @param file The name of the directory to change to.
	 * 
	 * @return True if current directory was changed.
	 */
	public boolean changeDirectory(File file) {
		if (file == null) {
			return false;
		}
		
		final boolean isDir = file.isDirectory();

		if (isDir) {
			if (file.getName().equals("..")) {
				file = file.getParentFile().getParentFile();
				if (file == null) {
					file = new File("/");
				}
			}
			mCurrentDir = file;
		}
		
		return isDir;
	}
	
	/**
	 * Save RecyclerView position.
	 * 
	 * @param recyclerView The RecyclerView whose position is to be saved.
	 */
	public void saveListPosition(final RecyclerView recyclerView) {
		mPathStack.push(new ListState(recyclerView));
	}
	
	/**
	 * Restore RecyclerView position.
	 * 
	 * @param recyclerView The RecyclerView whose position is to be restored.
	 */
	public void restoreListPosition(final RecyclerView recyclerView) {
		if (!mPathStack.isEmpty()) {
			final ListState state = mPathStack.pop();
			state.restoreState(recyclerView);
		}
	}
	
	/**
	 * Start filesystem navigation and clear previous history.
	 * 
	 * @param currentDir The directory to start navigation at.
	 */
	public void startNavigation(final File currentDir) {
		Log.d(TAG, "start navigation at " + currentDir.getPath());
		mCurrentDir = currentDir;
		mPathStack.clear();
	}
	
	/**
	 * Get the current directory.
	 * 
	 * @return The current directory pathname.
	 */
	public File getCurrentDir() {
		return mCurrentDir;
	}
	
	/**
	 * Navigate to the parent directory.
	 * 
	 * @return True if the current directory was changed.
	 */
	public boolean parentDir() {
		if (mCurrentDir == null) {
			return false;
		}
		
		final File parent = mCurrentDir.getParentFile();
		if (parent == null) {
			return false;
		}
		
		mCurrentDir = parent;
		return true;
	}
	
	/**
	 * Check whether we're at the file system root.
	 * 
	 * @return True if we're at the file system root.
	 */
	public boolean isAtTopDir() {
		return mPathStack.isEmpty();
	}
}
