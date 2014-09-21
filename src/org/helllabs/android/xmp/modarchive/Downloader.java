package org.helllabs.android.xmp.modarchive;

import java.io.File;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.util.Log;

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

/*
 * Based on the Groundy download example
 */
public class Downloader {

	protected static final String TAG = "Downloader";
	private final Context mContext;
	private ProgressDialog mProgressDialog;
	private TaskHandler mTaskHandler;

	private final Object mCallback = new Object() {
		@SuppressLint("NewApi")
		@OnProgress(DownloadTask.class)
		public void onProgress(@Param(Groundy.PROGRESS) final int progress) {
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
		public void onSuccess() {
			Log.d(TAG, "download success");
			Toast.makeText(mContext, R.string.file_downloaded, Toast.LENGTH_LONG).show();
			mProgressDialog.dismiss();
		}

		@OnFailure(DownloadTask.class)
		public void onFailure(@Param(Groundy.CRASH_MESSAGE) final String error) {
			Log.d(TAG, "download fail: " + error);
			Toast.makeText(mContext, error, Toast.LENGTH_LONG).show();
			mProgressDialog.dismiss();
		}
	};

	public static class DownloadTask extends GroundyTask {
		public static final String PARAM_URL = "org.helllabs.android.xmp.modarchive.URL";
		public static final String PARAM_PATH = "org.helllabs.android.xmp.modarchive.PATH";

		@Override
		protected TaskResult doInBackground() {
			try {
				final String url = getStringArg(PARAM_URL);
				final String path = getStringArg(PARAM_PATH);
				final String name = new File(url).getName();
				final int start = name.indexOf('#') + 1;
				final File dest = new File(path, name.substring(start));
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
		this.mContext = context;
	}

	public void download(final String url, final String path) {
		mProgressDialog = new ProgressDialog(mContext);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setCancelable(true);
		mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(final DialogInterface dialogInterface){
				if (mTaskHandler != null) {
					mTaskHandler.cancel(mContext, 0, new GroundyManager.SingleCancelListener() {
						@Override
						public void onCancelResult(final long id, final int result){
							Toast.makeText(mContext, R.string.download_cancelled, Toast.LENGTH_LONG).show();
						}
					});
				}
			}
		});
		mProgressDialog.show();

		mTaskHandler = Groundy.create(Downloader.DownloadTask.class)
				.callback(mCallback)
				.arg(DownloadTask.PARAM_URL, url)
				.arg(DownloadTask.PARAM_PATH, path)
				.queueUsing(mContext);
	}


}
