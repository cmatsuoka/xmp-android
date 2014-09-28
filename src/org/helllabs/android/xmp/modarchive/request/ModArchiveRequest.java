package org.helllabs.android.xmp.modarchive.request;

import org.helllabs.android.xmp.XmpApplication;
import org.helllabs.android.xmp.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

public abstract class ModArchiveRequest<T> implements Response.Listener<String>, Response.ErrorListener {
	private static final String SERVER = "http://api.modarchive.org";

	private static final String TAG = "ModArchiveRequest";

	private final String mKey;
	private final String mRequest;
	private OnResponseListener<T> mOnResponseListener;

	public interface OnResponseListener<T> {
		void onResponse(T response);
		void onError(Throwable error);
	}

	public ModArchiveRequest(final String key, final String request) {
		Log.d(TAG, "request=" + request);
		mKey = key;
		mRequest = request;
	}

	public ModArchiveRequest<T> setOnResponseListener(final OnResponseListener<T> listener) {
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
		mOnResponseListener.onError(error);
	}

	@Override
	public void onResponse(final String result) {
		Log.i(TAG, "Volley: get response");
		mOnResponseListener.onResponse(xmlParse(result));
	}

	protected abstract T xmlParse(String result);

}
