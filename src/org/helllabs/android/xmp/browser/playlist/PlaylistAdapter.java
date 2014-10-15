package org.helllabs.android.xmp.browser.playlist;

import java.util.ArrayList;
import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.util.FileUtils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PlaylistAdapter extends ArrayAdapter<PlaylistItem> {
    private final List<PlaylistItem> items;
    private final Context context;
    private final boolean useFilename;

    public PlaylistAdapter(final Context context, final int resource, final int textViewResId, final List<PlaylistItem> items, final boolean useFilename) {
    	super(context, resource, textViewResId, items);
    	this.items = items;
    	this.context = context;
    	this.useFilename = useFilename;
    }
    
    @SuppressLint("InflateParams")
	@Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
    	View view = convertView;
    	if (view == null) {
    		final LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    		view = inflater.inflate(R.layout.playlist_item, null);
    	}
    	final PlaylistItem info = getItem(position);
    	           
    	if (info != null) {                		
    		final TextView titleText = (TextView)view.findViewById(R.id.plist_title);
    		final TextView infoText = (TextView)view.findViewById(R.id.plist_info);
    		final ImageView image = (ImageView)view.findViewById(R.id.plist_image);
    		
   			titleText.setText(useFilename ? FileUtils.basename(info.getFilename()) : info.getName());
   			infoText.setText(info.getComment());
   			
   			final Typeface typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
   			final int imageRes = info.getImageRes();
   			final int type = info.getType();
   			
   			if (type == PlaylistItem.TYPE_DIRECTORY) {
    			infoText.setTypeface(typeface, Typeface.ITALIC);
   			} else {
    			infoText.setTypeface(typeface, Typeface.NORMAL);  			
   			}

   			if (imageRes > 0) {
   				image.setImageResource(imageRes);
   				image.setVisibility(View.VISIBLE);
   			} else {
   				image.setVisibility(View.GONE);
   			}
     	}
            
    	return view;
    }
    
    public List<PlaylistItem> getItems() {
		return items;
	}
    
    public String getFilename(final int location) {
    	return items.get(location).getFilename();
    }
    
    public List<String> getFilenameList() {
    	final List<String> list = new ArrayList<String>();
    	for (final PlaylistItem item : items) {
    		list.add(item.getFilename());
    	}
    	return list;
    }
    
    public List<String> getFilteredFilenameList() {
    	final List<String> list = new ArrayList<String>();
    	for (final PlaylistItem item : items) {
    		if (item.getType() == PlaylistItem.TYPE_FILE) {
    			list.add(item.getFilename());
    		}
    	}
    	return list;
    }
    
    public List<String> getFilenameList(final int location) {
    	final List<PlaylistItem> subItems = items.subList(location, items.size());
    	final List<String> list = new ArrayList<String>();
    	for (final PlaylistItem item : subItems) {
    		list.add(item.getFilename());
    	}
    	return list;
    }
    
    @TargetApi(11)
    public void addList(final List<PlaylistItem> list) {
    	if (Build.VERSION.SDK_INT >= 11) {
    		addAll(list);
    	} else {
    		for (final PlaylistItem item : list) {
    			add(item);
    		}
    	}
    }
}
