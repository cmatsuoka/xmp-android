package org.helllabs.android.xmp.modarchive.response;

public class SoftErrorResponse extends ModArchiveResponse {
	
	private String message;
	
	public SoftErrorResponse(final String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
