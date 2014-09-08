package org.helllabs.android.xmp.modarchive;

import java.util.HashMap;
import java.util.Map;

import org.helllabs.android.xmp.XmpApplication;
import org.helllabs.android.xmp.util.Log;
import org.json.JSONObject;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

public class ModArchiveRequest implements Response.Listener<JSONObject>, Response.ErrorListener {
	private static final String SERVER = "http://api.modarchive.org";

	private static final String TAG = "ModArchiveRequest";
	
	private final String mKey;
	private final String mRequest;
	private OnResponseListener mOnResponseListener;
	
	public interface OnResponseListener {
		void onResponse(JSONObject json);
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

		final JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url, null, this, this) {
			protected Map<String, String> getParams() throws AuthFailureError {
				Map<String, String> params = new HashMap<String, String>();
				return params;
			};
		};
		
		queue.add(jsObjRequest);
	}

	@Override
	public void onErrorResponse(VolleyError error) {
		mOnResponseListener.onResponse(null);
		
	}

	@Override
	public void onResponse(JSONObject json) {
		mOnResponseListener.onResponse(json);
	}

}
