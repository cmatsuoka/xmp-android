package org.helllabs.android.xmp.modarchive.response;

public class SoftErrorResponse extends ModArchiveResponse {
	
	private final String message;
	
	public SoftErrorResponse(final String message) {
		super();
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
