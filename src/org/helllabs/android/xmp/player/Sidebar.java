package org.helllabs.android.xmp.player;

import org.helllabs.android.xmp.R;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class Sidebar {
	private final Activity activity;
	private final TextView numPatText;
	private final TextView numInsText;
	private final TextView numSmpText;
	private final TextView numChnText;
	private final RadioGroup seqGroup;
	
	public Sidebar(final Activity activity) {
		this.activity = activity;
		
		final LinearLayout contentView = (LinearLayout)activity.findViewById(R.id.content_view);
		activity.getLayoutInflater().inflate(R.layout.player, contentView, true);

		final LinearLayout sidebarView = (LinearLayout)activity.findViewById(R.id.sidebar_view);
		activity.getLayoutInflater().inflate(R.layout.player_sidebar, sidebarView, true);
		//sidebarView.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
		
		numPatText = (TextView)activity.findViewById(R.id.sidebar_num_pat);
		numInsText = (TextView)activity.findViewById(R.id.sidebar_num_ins);
		numSmpText = (TextView)activity.findViewById(R.id.sidebar_num_smp);
		numChnText = (TextView)activity.findViewById(R.id.sidebar_num_chn);
		
		seqGroup = (RadioGroup)activity.findViewById(R.id.sidebar_sequences);
	}
	
	public void setDetails(final int numPat, final int numIns, final int numSmp, final int numChn) {
		numPatText.setText(Integer.toString(numPat));
		numInsText.setText(Integer.toString(numIns));
		numSmpText.setText(Integer.toString(numSmp));
		numChnText.setText(Integer.toString(numChn));
	}
	
	public void clearSequences() {
		seqGroup.removeAllViews();
	}
	
	public void addSequence(final int num, final int duration) {
		final RadioButton button = new RadioButton(activity);
		final String text = num == 0 ? "main" : Integer.toString(num);
		button.setText(String.format("%s (%d:%02d)", text, duration / 60000, (duration / 1000) % 60));
		seqGroup.addView(button, num, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	}
	
	public void selectSequence(int num) {
		final RadioButton button = (RadioButton)seqGroup.getChildAt(num);
		button.toggle();
		//seqGroup.check(num);
	}


}
