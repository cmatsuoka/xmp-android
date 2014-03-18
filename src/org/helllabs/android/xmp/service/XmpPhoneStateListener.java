package org.helllabs.android.xmp.service;


import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;


public class XmpPhoneStateListener extends PhoneStateListener {
	private final PlayerService service;
	
	public XmpPhoneStateListener(final PlayerService service) {
		super();
		this.service = service;
	}
	
	@Override
	public void onCallStateChanged(final int state, final String incomingNumber) {
		Log.i("Xmp Listener", "Call state changed: " + state);
		service.autoPause(state != TelephonyManager.CALL_STATE_IDLE);
	}
}



