package org.helllabs.android.xmp.service;


import org.helllabs.android.xmp.Log;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;


public class XmpPhoneStateListener extends PhoneStateListener {
	private static final String TAG = "PhoneStateListener";
	private final PlayerService service;
	
	public XmpPhoneStateListener(final PlayerService service) {
		super();
		this.service = service;
	}
	
	@Override
	public void onCallStateChanged(final int state, final String incomingNumber) {
		Log.i(TAG, "Call state changed: " + state);
		service.autoPause(state != TelephonyManager.CALL_STATE_IDLE);
	}
}



