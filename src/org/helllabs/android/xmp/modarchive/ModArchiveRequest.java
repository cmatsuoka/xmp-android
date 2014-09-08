package org.helllabs.android.xmp.modarchive;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.helllabs.android.xmp.XmpApplication;
import org.helllabs.android.xmp.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

public class ModArchiveRequest implements Response.Listener<String>, Response.ErrorListener {
	private static final String SERVER = "http://api.modarchive.org";

	private static final String TAG = "ModArchiveRequest";

	private final String mKey;
	private final String mRequest;
	private OnResponseListener mOnResponseListener;

	public interface OnResponseListener {
		void onResponse(Module module);
	}


	public ModArchiveRequest(final String key, final String request) {
		Log.d(TAG, "request=" + request);
		mKey = key;
		mRequest = request;
	}

	public ModArchiveRequest setOnResponseListener(final OnResponseListener listener) {
		mOnResponseListener = listener;
		return this;
	}

	public void send() {
		final String url = SERVER + "/xml-tools.php?key=" + mKey + "&request=" + mRequest;
		final RequestQueue queue = XmpApplication.getInstance().getRequestQueue();

		final StringRequest jsObjRequest = new StringRequest(url, this, this);
		queue.add(jsObjRequest);
	}

	@Override
	public void onErrorResponse(final VolleyError error) {
		Log.e(TAG, "Volley error: " + error.getMessage());
		mOnResponseListener.onResponse(null);

	}

	@Override
	public void onResponse(final String result) {
		Log.i(TAG, "Volley: get response");
		mOnResponseListener.onResponse(xmlParse(result));
	}


	private Module xmlParse(final String result) {
		final Module module = new Module();

		try {
			final XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
			final XmlPullParser myparser = xmlFactoryObject.newPullParser();
			final InputStream stream = new ByteArrayInputStream(result.getBytes());
			myparser.setInput(stream, null);
			
			int event = myparser.getEventType();
			String text = "";
			while (event != XmlPullParser.END_DOCUMENT)	{
				switch (event){
				case XmlPullParser.START_TAG:
					break;
				case XmlPullParser.TEXT:
					text = myparser.getText();
					break;
				case XmlPullParser.END_TAG:
					final String name = myparser.getName();
					//Log.d(TAG, "name=" + name + " text=" + text);
					if (name.equals("filename")){
						module.setFilename(text);
					} else if (name.equals("format")) {
						module.setFormat(text);
					} else if (name.equals("url")) {
						module.setUrl(text);
					} else if (name.equals("bytes")) {
						module.setBytes(Integer.parseInt(text));
					} else if (name.equals("songtitle")) {
						module.setSongTitle(text);
					} else if (name.equals("title")) {
						module.setLicense(text);
					} else if (name.equals("instruments")) {
						module.setInstruments(text);
					}
					break;
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

		return module;
	}

}
