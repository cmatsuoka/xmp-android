package org.helllabs.android.xmp.modarchive.response;

public class HardErrorResponse extends ModArchiveResponse {
	
	private Throwable error;
	
	public HardErrorResponse(final Throwable error) {
		this.error = error;
	}
	
	public Throwable getError() {
		return error;
	}
}
