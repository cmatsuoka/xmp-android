package org.helllabs.android.xmp.modarchive.response;

public class HardErrorResponse extends ModArchiveResponse {
	
	private final Throwable error;
	
	public HardErrorResponse(final Throwable error) {
		super();
		this.error = error;
	}
	
	public Throwable getError() {
		return error;
	}
}
