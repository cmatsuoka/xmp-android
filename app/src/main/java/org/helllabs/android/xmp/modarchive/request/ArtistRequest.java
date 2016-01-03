package org.helllabs.android.xmp.modarchive.request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.helllabs.android.xmp.modarchive.model.Artist;
import org.helllabs.android.xmp.modarchive.model.Sponsor;
import org.helllabs.android.xmp.modarchive.response.ArtistResponse;
import org.helllabs.android.xmp.modarchive.response.HardErrorResponse;
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse;
import org.helllabs.android.xmp.modarchive.response.SoftErrorResponse;
import org.helllabs.android.xmp.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class ArtistRequest extends ModArchiveRequest {

	private static final String TAG = "ArtistListRequest";

	public ArtistRequest(final String key, final String request) {
		super(key, request);
	}
	
	public ArtistRequest(final String key, final String request, final String parameter) throws UnsupportedEncodingException {
		super(key, request, parameter);
	}
	
	public ArtistRequest(final String key, final String request, final long parameter) throws UnsupportedEncodingException {
		this(key, request, String.valueOf(parameter));
	}

	@Override
	protected ModArchiveResponse xmlParse(final String result) {
		final ArtistResponse artistList = new ArtistResponse();
		Artist artist = null;
		Sponsor sponsor = null;

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
					} else if (start.equals("sponsor")) {
						sponsor = new Sponsor();	// NOPMD
					}
					break;
				}
				case XmlPullParser.TEXT:
					text = myparser.getText().trim();
					break;
				case XmlPullParser.END_TAG: {
					final String end = myparser.getName();
					if (sponsor != null) {
						switch (end) {
							case "text":
								sponsor.setName(text);
								break;
							case "link":
								sponsor.setLink(text);
								break;
							case "sponsor":
								artistList.setSponsor(sponsor);
								sponsor = null;    // NOPMD
								break;
						}
					} else {
						switch (end) {
							case "error":
								return new SoftErrorResponse(text);
							case "id":
								artist.setId(Long.parseLong(text));
								break;
							case "alias":
								artist.setAlias(text);
								break;
							case "item":
								artistList.add(artist);
								break;
						}
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
			return new HardErrorResponse(e);
		} catch (IOException e) {
			Log.e(TAG, "IOException: " + e.getMessage());
			return new HardErrorResponse(e);
		}

		return artistList;
	}

}
