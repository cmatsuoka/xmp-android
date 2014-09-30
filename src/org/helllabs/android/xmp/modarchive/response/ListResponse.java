package org.helllabs.android.xmp.modarchive.response;

import java.util.ArrayList;
import java.util.List;

public abstract class ListResponse<T> extends ModArchiveResponse {
	
	private List<T> list = new ArrayList<T>();

	public void add(final T item) {
		list.add(item);
	}
	
	public List<T> getList() {
		return list;
	}
	
	public boolean isEmpty() {
		return list.isEmpty();
	}
	
	public T get(final int location) {
		return list.get(location);
	}
}
