package org.helllabs.android.xmp.modarchive.request;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.helllabs.android.xmp.XmpApplication;
import org.helllabs.android.xmp.modarchive.response.HardErrorResponse;
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse;
import org.helllabs.android.xmp.modarchive.response.SoftErrorResponse;
import org.helllabs.android.xmp.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

public abstract class ModArchiveRequest implements Response.Listener<String>, Response.ErrorListener {

	private static final String TAG = "ModArchiveRequest";
	private static final String SERVER = "http://api.modarchive.org";

	public static final String ARTIST = "search_artist&query=";
	public static final String ARTIST_MODULES = "view_modules_by_artistid&query=";
	public static final String MODULE = "view_by_moduleid&query=";
	public static final String RANDOM = "random";
	public static final String FILENAME_OR_TITLE = "search&type=filename_or_songtitle&query=";

	private final String mKey;
	private final String mRequest;
	private OnResponseListener mOnResponseListener;

	public interface OnResponseListener {
		void onResponse(ModArchiveResponse response);
		void onSoftError(SoftErrorResponse response);
		void onHardError(HardErrorResponse response);
	}

	public ModArchiveRequest(final String key, final String request) {
		Log.d(TAG, "request=" + request);
		mKey = key;
		mRequest = request;
	}
	
	public ModArchiveRequest(final String key, final String request, final String parameter) throws UnsupportedEncodingException {
		this(key, request + URLEncoder.encode(parameter, "UTF-8"));
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
		//		} catch (UnsupportedEncodingException e) {
		//			mOnResponseListener.onError(new Throwable("Bad search string. "));
		//		}
	}

	@Override
	public void onErrorResponse(final VolleyError error) {
		Log.e(TAG, "Volley error: " + error.getMessage());
		mOnResponseListener.onHardError(new HardErrorResponse(error));
	}

	@Override
	public void onResponse(final String result) {
		Log.i(TAG, "Volley: get response " + result);
		final ModArchiveResponse response = xmlParse(result);
		if (response instanceof SoftErrorResponse) {
			mOnResponseListener.onSoftError((SoftErrorResponse)response);
		} else {
			mOnResponseListener.onResponse(response);
		}
	}

	protected abstract ModArchiveResponse xmlParse(String result);

}
