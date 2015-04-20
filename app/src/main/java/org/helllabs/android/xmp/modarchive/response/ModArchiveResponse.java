package org.helllabs.android.xmp.modarchive.response;

import org.helllabs.android.xmp.modarchive.model.Sponsor;

public class ModArchiveResponse {
	
	private Sponsor sponsor;
	
	protected ModArchiveResponse() {
		// Do nothing
	}
	
	public Sponsor getSponsor() {
		return sponsor;
	}
	
	public void setSponsor(final Sponsor sponsor) {
		this.sponsor = sponsor;
	}
	
}
