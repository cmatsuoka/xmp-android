package org.helllabs.android.xmp.modarchive.request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.helllabs.android.xmp.modarchive.model.Artist;
import org.helllabs.android.xmp.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class ArtistRequest extends ModArchiveRequest<List<Artist>> {

	private static final String TAG = "ArtistListRequest";

	public ArtistRequest(final String key, final String request) {
		super(key, request);
	}

	@Override
	protected List<Artist> xmlParse(final String result) {
		final List<Artist> artistList = new ArrayList<Artist>();
		Artist artist = null;

		try {
			final XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
			final XmlPullParser myparser = xmlFactoryObject.newPullParser();
			final InputStream stream = new ByteArrayInputStream(result.getBytes());
			myparser.setInput(stream, null);

			int event = myparser.getEventType();
			String text = "";
			while (event != XmlPullParser.END_DOCUMENT)	{
				switch (event){
				case XmlPullParser.START_TAG: {
					final String start = myparser.getName();
					if (start.equals("item")) {
						artist = new Artist(); // NOPMD
					}
					break;
				}
				case XmlPullParser.TEXT:
					text = myparser.getText();
					break;
				case XmlPullParser.END_TAG: {
					final String end = myparser.getName();
					if (end.equals("id")) {
						artist.setId(Long.parseLong(text));
					} else if (end.equals("alias")) {
						artist.setAlias(text);
					} else if (end.equals("item")) {
						artistList.add(artist);
					}
					break;
				}
				default:
					break;
				}		 
				event = myparser.next(); 					
			}
		} catch (XmlPullParserException e) {
			Log.e(TAG, "XmlPullParserException: " + e.getMessage());
			return null;
		} catch (IOException e) {
			Log.e(TAG, "IOException: " + e.getMessage());
			return null;
		}

		return artistList;
	}

}
