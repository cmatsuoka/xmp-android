package org.helllabs.android.xmp.player;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.util.Log;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;


public class Sidebar {
	private static final String TAG = "Sidebar";
	private final PlayerActivity activity;
	private final TextView numPatText;
	private final TextView numInsText;
	private final TextView numSmpText;
	private final TextView numChnText;
	private final ImageButton allSequencesButton;
	private final RadioGroup seqGroup;
	private final RadioGroup.OnCheckedChangeListener seqGroupListener;

	public Sidebar(final PlayerActivity activity) {
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
		allSequencesButton = (ImageButton)activity.findViewById(R.id.sidebar_allseqs_button);

		allSequencesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				allSequencesButton.setImageResource(activity.toggleAllSequences() ? R.drawable.sub_on : R.drawable.sub_off);
			}
		});
		allSequencesButton.setImageResource(activity.getAllSequences() ? R.drawable.sub_on : R.drawable.sub_off);
		
		seqGroup = (RadioGroup)activity.findViewById(R.id.sidebar_sequences);
		seqGroupListener = new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final RadioGroup group, final int checkedId) {
				Log.e(TAG, "Selection changed to sequence " + checkedId);
				activity.playNewSequence(checkedId);
			}	
		};
		seqGroup.setOnCheckedChangeListener(seqGroupListener);
	}

	public void setDetails(final int numPat, final int numIns, final int numSmp, final int numChn, final boolean allSequences) {
		numPatText.setText(Integer.toString(numPat));
		numInsText.setText(Integer.toString(numIns));
		numSmpText.setText(Integer.toString(numSmp));
		numChnText.setText(Integer.toString(numChn));
		allSequencesButton.setImageResource(allSequences ? R.drawable.sub_on : R.drawable.sub_off);
	}

	public void clearSequences() {
		seqGroup.removeAllViews();
	}

	public void addSequence(final int num, final int duration) {
		//final RadioButton button = new RadioButton(activity);
		// Can't get it styled this way, see http://stackoverflow.com/questions/3142067/android-set-style-in-code
		
		final RadioButton button = (RadioButton)activity.getLayoutInflater().inflate(R.layout.sequence_item, null);
		
		final String text = num == 0 ? "main song" : "subsong " + Integer.toString(num);
		button.setText(String.format("%2d:%02d (%s)", duration / 60000, (duration / 1000) % 60, text));
		button.setId(num);
		seqGroup.addView(button, num, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	}
	
	public void selectSequence(final int num) {
		
		seqGroup.setOnCheckedChangeListener(null);
		
		Log.i(TAG, "Select sequence " + num);
		seqGroup.check(-1);		// force redraw
		seqGroup.check(num);
		
		seqGroup.setOnCheckedChangeListener(seqGroupListener);
		
	
	}
	
//	private void commentClick() {
//		final String comment = Xmp.getComment();
//		if (comment != null) {
//			final Intent intent = new Intent(activity, CommentActivity.class);
//			intent.putExtra("comment", comment);
//			activity.startActivity(intent);		
//		}
//		
//	}
}
