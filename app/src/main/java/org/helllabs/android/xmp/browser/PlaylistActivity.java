package org.helllabs.android.xmp.browser;

import java.io.IOException;
import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.browser.playlist.Playlist;
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter;
import org.helllabs.android.xmp.preferences.Preferences;
import org.helllabs.android.xmp.util.Log;
import org.helllabs.android.xmp.util.Message;

import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.ItemShadowDecorator;
import com.h6ah4i.android.widget.advrecyclerview.decoration.SimpleListDividerDecorator;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;


public class PlaylistActivity extends BasePlaylistActivity implements PlaylistAdapter.OnItemClickListener {
	private static final String TAG = "PlaylistActivity";
	private Playlist mPlaylist;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mWrappedAdapter;
    private RecyclerViewDragDropManager mRecyclerViewDragDropManager;

	// List reorder

	/*private final TouchListView.DropListener onDrop = new TouchListView.DropListener() {
		@Override
		public void drop(final int from, final int to) {
			final PlaylistItem item = playlistAdapter.getItem(from);
			playlistAdapter.remove(item);
			playlistAdapter.insert(item, to);
			playlist.setListChanged(true);
		}
	};

	private final TouchListView.RemoveListener onRemove = new TouchListView.RemoveListener() {
		@Override
		public void remove(final int which) {
			playlistAdapter.remove(playlistAdapter.getItem(which));
		}
	};	*/

	@Override
	public void onCreate(final Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.playlist);

		final Bundle extras = getIntent().getExtras();
		if (extras == null) {
			return;
		}

		setTitle(R.string.browser_playlist_title);

		final String name = extras.getString("name");
		final boolean useFilename = mPrefs.getBoolean(Preferences.USE_FILENAME, false);

		try {
			mPlaylist = new Playlist(this, name);
		} catch (IOException e) {
			Log.e(TAG, "Can't read playlist " + name);
		}

		mRecyclerView = (RecyclerView) findViewById(R.id.plist_list);

		// layout manager
		final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
		layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

		// drag & drop manager
		mRecyclerViewDragDropManager = new RecyclerViewDragDropManager();
		mRecyclerViewDragDropManager.setDraggingItemShadowDrawable((NinePatchDrawable) getResources().getDrawable(R.drawable.material_shadow_z3));

		// adapter
		mPlaylistAdapter = new PlaylistAdapter(this, mPlaylist, useFilename);
		mWrappedAdapter = mRecyclerViewDragDropManager.createWrappedAdapter(mPlaylistAdapter);

		final GeneralItemAnimator animator = new RefactoredDefaultItemAnimator();

		mRecyclerView.setLayoutManager(layoutManager);
		mRecyclerView.setAdapter(mWrappedAdapter);
		mRecyclerView.setItemAnimator(animator);

		// additional decorations
		//noinspection StatementWithEmptyBody
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			// Lollipop or later has native drop shadow feature. ItemShadowDecorator is not required.
		} else {
			mRecyclerView.addItemDecoration(new ItemShadowDecorator((NinePatchDrawable) getResources().getDrawable(R.drawable.material_shadow_z1)));
		}
		mRecyclerView.addItemDecoration(new SimpleListDividerDecorator(getResources().getDrawable(R.drawable.list_divider), true));

		mRecyclerViewDragDropManager.attachRecyclerView(mRecyclerView);

		mPlaylistAdapter.setOnItemClickListener(this);

		final TextView curListName = (TextView) findViewById(R.id.current_list_name);
		final TextView curListDesc = (TextView) findViewById(R.id.current_list_description);

		curListName.setText(name);
		curListDesc.setText(mPlaylist.getComment());
		registerForContextMenu(mRecyclerView);

		setupButtons();
	}

	@Override
	public void onPause() {
		super.onPause();

		mRecyclerViewDragDropManager.cancelDrag();

		try {
			mPlaylist.commit();
		} catch (IOException e) {
			Message.toast(this, getString(R.string.error_write_to_playlist));
		}
	}

	@Override
	public void onDestroy() {
		if (mRecyclerViewDragDropManager != null) {
			mRecyclerViewDragDropManager.release();
			mRecyclerViewDragDropManager = null;
		}

		if (mRecyclerView != null) {
			mRecyclerView.setItemAnimator(null);
			mRecyclerView.setAdapter(null);
			mRecyclerView = null;
		}

		if (mWrappedAdapter != null) {
			WrapperAdapterUtils.releaseAll(mWrappedAdapter);
			mWrappedAdapter = null;
		}
		mPlaylistAdapter = null;

		super.onDestroy();
	}

	@Override
	protected void setShuffleMode(final boolean shuffleMode) {
		mPlaylist.setShuffleMode(shuffleMode);
	}

	@Override
	protected void setLoopMode(final boolean loopMode) {
		mPlaylist.setLoopMode(loopMode);
	}

	@Override
	protected boolean isShuffleMode() {
		return mPlaylist.isShuffleMode();
	}

	@Override
	protected boolean isLoopMode() {
		return mPlaylist.isLoopMode();
	}

	@Override
	protected List<String> getAllFiles() {
		return mPlaylistAdapter.getFilenameList();
	}

	@Override
	public void update() {
		mPlaylistAdapter.notifyDataSetChanged();
	}

	// Playlist context menu

	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View view, final ContextMenuInfo menuInfo) {

		final int mode = Integer.parseInt(mPrefs.getString(Preferences.PLAYLIST_MODE, "1"));

		menu.setHeaderTitle("Edit playlist");
		menu.add(Menu.NONE, 0, 0, "Remove from playlist");
		menu.add(Menu.NONE, 1, 1, "Add to play queue");
		menu.add(Menu.NONE, 2, 2, "Add all to play queue");
		if (mode != 2) {
			menu.add(Menu.NONE, 3, 3, "Play this module");
		}
		if (mode != 1) {
			menu.add(Menu.NONE, 4, 4, "Play all starting here");
		}
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		final int itemId = item.getItemId();

		switch (itemId) {
		case 0:										// Remove from playlist
			mPlaylist.remove(info.position);
			try {
				mPlaylist.commit();
			} catch (IOException e) {
				Message.toast(this, getString(R.string.error_write_to_playlist));
			}
			update();
			break;
		case 1:										// Add to play queue
			addToQueue(mPlaylistAdapter.getFilename(info.position));
			break;
		case 2:										// Add all to play queue
			addToQueue(mPlaylistAdapter.getFilenameList());
			break;
		case 3:										// Play only this module
			playModule(mPlaylistAdapter.getFilename(info.position));
			break;
		case 4:										// Play all starting here
			playModule(mPlaylistAdapter.getFilenameList(), info.position);
			break;
		}

		return true;
	}
}
