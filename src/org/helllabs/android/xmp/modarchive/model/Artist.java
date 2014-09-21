package org.helllabs.android.xmp.modarchive.model;

public class Artist {
	public static final String UNKNOWN = "unknown";
	private String alias;
	private long id;
	
	public String getAlias() {
		return alias;
	}
	
	public void setAlias(final String alias) {
		if (alias == null || alias.isEmpty()) {
			this.alias = Artist.UNKNOWN;
		} else {
			this.alias = alias;
		}
	}
	
	public long getId() {
		return id;
	}
	
	public void setId(final long id) {
		this.id = id;
	}
	
	@Override
	public String toString() {
		return alias;
	}
}
