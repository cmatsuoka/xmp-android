package org.helllabs.android.xmp.browser;

import java.util.List;

import org.helllabs.android.xmp.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PlaylistItemAdapter extends ArrayAdapter<PlaylistItem> {
    private final List<PlaylistItem> items;
    private final Context context;
    private final boolean useFilename;

    public PlaylistItemAdapter(final Context context, final int resource, final int textViewResId, final List<PlaylistItem> items, final boolean useFilename) {
    	super(context, resource, textViewResId, items);
    	this.items = items;
    	this.context = context;
    	this.useFilename = useFilename;
    }
    
    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
    	View view = convertView;
    	if (view == null) {
    		final LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    		view = inflater.inflate(R.layout.playlist_item, null);
    	}
    	final PlaylistItem info = items.get(position);
    	           
    	if (info != null) {                		
    		final TextView titleText = (TextView)view.findViewById(R.id.plist_title);
    		final TextView infoText = (TextView)view.findViewById(R.id.plist_info);
    		final ImageView image = (ImageView)view.findViewById(R.id.plist_image);
    		
   			titleText.setText(useFilename ? FileUtils.basename(info.filename) : info.name);
   			infoText.setText(info.comment);
   			
   			final Typeface typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
   			if (info.imageRes == R.drawable.folder || info.imageRes == R.drawable.parent) {
   				//tt.setTypeface(t, Typeface.ITALIC);
    			infoText.setTypeface(typeface, Typeface.ITALIC);
   			} else {
   				//tt.setTypeface(t, Typeface.NORMAL);
    			infoText.setTypeface(typeface, Typeface.NORMAL);  			
   			}

   			if (info.imageRes > 0) {
   				image.setImageResource(info.imageRes);
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
}
