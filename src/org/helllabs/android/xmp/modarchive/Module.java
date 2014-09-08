package org.helllabs.android.xmp.modarchive;

public class Module {
	private String filename;
	private String format;
	private String url;
	private int bytes;
	private String songtitle;
	private String license;
	private String artist;
	
	public String getArtist() {
		return artist;
	}
	
	public void setArtist(final String artist) {
		this.artist = artist;
	}
	
	public int getBytes() {
		return bytes;
	}
	
	public void setBytes(final int bytes) {
		this.bytes = bytes;
	}
	
	public String getFilename() {
		return filename;
	}
	
	public void setFilename(final String filename) {
		this.filename = filename;
	}
	
	public String getFormat() {
		return format;
	}
	
	public void setFormat(final String format) {
		this.format = format;
	}
	
	public String getLicense() {
		return license;
	}
	
	public void setLicense(final String license) {
		this.license = license;
	}
	
	public String getSongtitle() {
		return songtitle;
	}
	
	public void setSongtitle(final String songtitle) {
		this.songtitle = songtitle;
	}
	
	public String getUrl() {
		return url;
	}
	
	public void setUrl(final String url) {
		this.url = url;
	}
}
