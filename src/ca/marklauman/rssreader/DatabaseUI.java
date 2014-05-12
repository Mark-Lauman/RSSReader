package ca.marklauman.rssreader;

import ca.marklauman.rssreader.database.Updater;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

/** Class devoted to displaying the
 *  {@link RSSData} {@link Updater}'s messages
 *  in the UI.
 *  @author Mark Lauman                          */
public class DatabaseUI extends BroadcastReceiver {
	/** Current application context.
	 *  Only needed for Toast messages.
	 *  TODO: move messages to a display bar
	 *  instead.                          */
	private Context mContext;
	/** Progress bar on the screen bottom */
	private ProgressBar prog_bar;
	
	
	public DatabaseUI(Context c, ProgressBar bar) {
		mContext = c;
		prog_bar = bar;
	}
	
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle extras = intent.getExtras();
		String msg;
		
		switch(extras.getInt(Updater.MSG_ERR)) {
		case Updater.ERR_OFFLINE:
			msg = mContext.getString(R.string.err_offline);
			Toast.makeText(mContext, msg, Toast.LENGTH_LONG)
				 .show();
			prog_bar.setVisibility(View.GONE);
			return;
		case Updater.ERR_URL:
			msg = mContext.getString(R.string.err_url);
			String url = extras.getString(Updater.MSG_URL);
			Toast.makeText(mContext, msg + " " + url, Toast.LENGTH_LONG)
				 .show();
			prog_bar.setVisibility(View.GONE);
			return;
		case Updater.ERR_CONN:
			msg = mContext.getString(R.string.err_connection);
			Toast.makeText(mContext, msg, Toast.LENGTH_LONG)
				 .show();
			prog_bar.setVisibility(View.GONE);
			return;
		case Updater.ERR_NONE:
			break;
		default:
			msg = mContext.getString(R.string.err_internal);
			Toast.makeText(mContext, msg, Toast.LENGTH_LONG)
			 	 .show();
			prog_bar.setVisibility(View.GONE);
			return;
		}
		
		int phase = extras.getInt(Updater.MSG_PHASE);
		int prog = extras.getInt(Updater.MSG_PROG);
		prog_bar.setVisibility(View.VISIBLE);
		if(phase == Updater.PHASE_DOWNLOAD)
			prog_bar.setProgress(prog / 2);
		else {
			if(prog == 100) prog_bar.setVisibility(View.GONE);
			else prog_bar.setProgress(prog / 2 + 50);
		}
	}
}