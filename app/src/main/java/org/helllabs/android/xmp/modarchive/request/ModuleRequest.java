package org.helllabs.android.xmp.modarchive.request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import org.helllabs.android.xmp.modarchive.model.Artist;
import org.helllabs.android.xmp.modarchive.model.Module;
import org.helllabs.android.xmp.modarchive.model.Sponsor;
import org.helllabs.android.xmp.modarchive.response.HardErrorResponse;
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse;
import org.helllabs.android.xmp.modarchive.response.ModuleResponse;
import org.helllabs.android.xmp.modarchive.response.SoftErrorResponse;
import org.helllabs.android.xmp.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class ModuleRequest extends ModArchiveRequest {

	private static final String TAG = "ModuleRequest";
	private static final String[] UNSUPPORTED = {
		"AHX", "HVL", "MO3"
	};

	public ModuleRequest(final String key, final String request) {
		super(key, request);
	}
	
	public ModuleRequest(final String key, final String request, final String parameter) throws UnsupportedEncodingException {
		super(key, request, parameter);
	}
	
	public ModuleRequest(final String key, final String request, final long parameter) throws UnsupportedEncodingException {
		this(key, request, String.valueOf(parameter));
	}

	@Override
	protected ModArchiveResponse xmlParse(final String result) {
		final ModuleResponse moduleList = new ModuleResponse();
		Module module = null;
		Sponsor sponsor = null;
		boolean inArtistInfo = false;
		final List<String> unsupported = Arrays.asList(UNSUPPORTED);

		try {
			final XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
			final XmlPullParser myparser = xmlFactoryObject.newPullParser();
			final InputStream stream = new ByteArrayInputStream(result.getBytes());
			myparser.setInput(stream, null);

			int event = myparser.getEventType();
			String text = "";
			while (event != XmlPullParser.END_DOCUMENT)	{
				switch (event){		// NOPMD
				case XmlPullParser.START_TAG:
					final String start = myparser.getName();
					switch (start) {
						case "module":
							module = new Module(); // NOPMD
							break;
						case "artist_info":
							inArtistInfo = true;
							break;
						case "sponsor":
							sponsor = new Sponsor();    // NOPMD
							break;
					}
					break;
				case XmlPullParser.TEXT:
					text = myparser.getText().trim();
					break;
				case XmlPullParser.END_TAG:
					final String end = myparser.getName();
					//Log.d(TAG, "name=" + name + " text=" + text);
					if (sponsor != null) {
						switch (end) {
							case "text":
								sponsor.setName(text);
								break;
							case "link":
								sponsor.setLink(text);
								break;
							case "sponsor":
								moduleList.setSponsor(sponsor);
								sponsor = null; // NOPMD
								break;
						}
					} else if (end.equals("error")) {
						return new SoftErrorResponse(text);
					} else if (end.equals("artist_info")) {
							inArtistInfo = false;
					} else if (module != null) {
						switch (end) {
							case "filename":
								module.setFilename(text);
								break;
							case "format":
								module.setFormat(text);
								break;
							case "url":
								module.setUrl(text);
								break;
							case "bytes":
								module.setBytes(Integer.parseInt(text));
								break;
							case "songtitle":
								module.setSongTitle(text);
								break;
							case "alias":
								// Use non-guessed artist if available
								if (module.getArtist().equals(Artist.UNKNOWN)) {
									module.setArtist(text);
								}
								break;
							case "title":
								module.setLicense(text);
								break;
							case "description":
								module.setLicenseDescription(text);
								break;
							case "legalurl":
								module.setLegalUrl(text);
								break;
							case "instruments":
								module.setInstruments(text);
								break;
							case "id":
								if (!inArtistInfo) {
									module.setId(Long.parseLong(text));
								}
								break;
							case "module":
								if (!unsupported.contains(module.getFormat())) {
									moduleList.add(module);
								}
								break;
						}
					}
					break;
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

		return moduleList;
	}
}
