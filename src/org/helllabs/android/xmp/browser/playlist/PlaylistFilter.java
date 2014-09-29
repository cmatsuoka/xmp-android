package org.helllabs.android.xmp.browser.playlist;

import java.io.File;
import java.io.FilenameFilter;

class PlaylistFilter implements FilenameFilter {
	public boolean accept(final File dir, final String name) {
		return name.endsWith(Playlist.PLAYLIST_SUFFIX);
	}
}