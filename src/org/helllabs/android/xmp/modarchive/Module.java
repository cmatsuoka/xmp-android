package org.helllabs.android.xmp.modarchive;

import android.text.Html;

public class Module {
	private String filename;
	private String format;
	private String url;
	private int bytes;
	private String songTitle;
	private String license;
	private String instruments;
	
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
	
	public String getInstruments() {
		return instruments;
	}
	
	public void setInstruments(final String instruments) {
		this.instruments = Html.fromHtml(instruments).toString();
	}
	
	public String getLicense() {
		return license;
	}
	
	public void setLicense(final String license) {
		this.license = Html.fromHtml(license).toString();
	}
	
	public String getSongTitle() {
		return songTitle;
	}
	
	public void setSongTitle(final String songtitle) {
		this.songTitle = Html.fromHtml(songtitle).toString();
	}
	
	public String getUrl() {
		return url;
	}
	
	public void setUrl(final String url) {
		this.url = url;
	}
}
