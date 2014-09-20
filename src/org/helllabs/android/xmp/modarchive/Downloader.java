package org.helllabs.android.xmp.modarchive;

import java.io.File;

import org.helllabs.android.xmp.R;

import com.telly.groundy.Groundy;
import com.telly.groundy.GroundyManager;
import com.telly.groundy.GroundyTask;
import com.telly.groundy.TaskHandler;
import com.telly.groundy.TaskResult;
import com.telly.groundy.annotations.OnFailure;
import com.telly.groundy.annotations.OnProgress;
import com.telly.groundy.annotations.OnSuccess;
import com.telly.groundy.annotations.Param;
import com.telly.groundy.util.DownloadUtils;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.widget.Toast;

public class Downloader {

	private final Context context;
	private ProgressDialog mProgressDialog;
	private TaskHandler mTaskHandler;


	//a callback can be any kind of object :)
	private final Object mCallback = new Object() {
		@SuppressLint("NewApi")
		@OnProgress(DownloadTask.class)
		public void onNiceProgress(@Param(Groundy.PROGRESS) final int progress) {
			if (progress == Groundy.NO_SIZE_AVAILABLE) {
				mProgressDialog.setIndeterminate(true);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
					mProgressDialog.setProgressNumberFormat(null);
					mProgressDialog.setProgressPercentFormat(null);
				}
				return;
			}
			mProgressDialog.setProgress(progress);
		}

		@OnSuccess(DownloadTask.class)
		public void onBeautifulSuccess() {
			Toast.makeText(context, R.string.file_downloaded, Toast.LENGTH_LONG).show();
			mProgressDialog.dismiss();
		}

		@OnFailure(DownloadTask.class)
		public void onTragedy(@Param(Groundy.CRASH_MESSAGE) final String error) {
			Toast.makeText(context, error, Toast.LENGTH_LONG).show();
			mProgressDialog.dismiss();
		}
	};

	public class DownloadTask extends GroundyTask {
		public static final String PARAM_URL = "org.helllabs.android.xmp.modarchive.URL";

		@Override
		protected TaskResult doInBackground() {
			try {
				final String url = getStringArg(PARAM_URL);
				final File dest = new File(getContext().getFilesDir(), new File(url).getName());
				DownloadUtils.downloadFile(getContext(), url, dest,
						DownloadUtils.getDownloadListenerForTask(this), new DownloadUtils.DownloadCancelListener(){
					@Override
					public boolean shouldCancelDownload() {
						return isQuitting();
					}
				});

				if (isQuitting()) {
					return cancelled();
				}
				return succeeded();
			} catch (Exception e) {
				return failed();
			}
		}
	}


	public Downloader(final Context context) {
		this.context = context;
	}

	public void download(final String url) {
		mProgressDialog = new ProgressDialog(context);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setCancelable(true);
		mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(final DialogInterface dialogInterface){
				if (mTaskHandler != null) {
					mTaskHandler.cancel(context, 0, new GroundyManager.SingleCancelListener() {
						@Override
						public void onCancelResult(final long id, final int result){
							Toast.makeText(context, R.string.download_cancelled, Toast.LENGTH_LONG).show();
						}
					});
				}
			}
		});
		mProgressDialog.show();

		mTaskHandler = Groundy.create(DownloadTask.class)
				.callback(mCallback)
				.arg(DownloadTask.PARAM_URL, url)
				.queueUsing(context);
	}


}
