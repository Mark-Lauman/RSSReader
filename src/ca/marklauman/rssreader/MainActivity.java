package ca.marklauman.rssreader;

import ca.marklauman.rssreader.adapters.ItemAdapter;
import ca.marklauman.rssreader.database.Updater;
import ca.marklauman.rssreader.database.schema.Item;
import ca.marklauman.rssreader.settings.SettingsActivity;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends SherlockFragmentActivity
						  implements LoaderCallbacks<Cursor> {
	
	/** Listens to the rssdata {@link Updater}
	 *  so progress can be displayed.       */
	private UpdateListener updaterListener;
	
	/** Adapter for the item list. */
	private ItemAdapter adapt;
	/** Progress bar on the screen bottom */
	private ProgressBar prog_bar;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		IntentFilter filter = new IntentFilter(Updater.BROADCAST);
		filter.addCategory(Intent.CATEGORY_DEFAULT);
		updaterListener = new UpdateListener(this);
		registerReceiver(updaterListener, filter);
	}
	
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
	
	
	@Override
	public void onStart() {
		super.onStart();
		
		ListView list = (ListView) findViewById(R.id.list);
		adapt = new ItemAdapter(this);
		list.setAdapter(adapt);
		list.setOnItemClickListener(adapt);
		
		prog_bar = (ProgressBar) findViewById(R.id.progress);
		
		getSupportLoaderManager().restartLoader(1, null, this);
	}
	
	
	@Override
	public void onDestroy() {
		unregisterReceiver(updaterListener);
		super.onDestroy();
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		
        // Refresh button
		case R.id.action_refresh:
			String path = PreferenceManager.getDefaultSharedPreferences(this)
										   .getString("feed1", "");
			Intent refresh = new Intent(this, Updater.class);
			refresh.putExtra(Updater.PARAM_URL, path);
			refresh.putExtra(Updater.PARAM_CACHE, getCacheDir().getPath());
			startService(refresh);
			return true;
		
		// Settings Button
		case R.id.action_settings:
			Intent settings = new Intent(this, SettingsActivity.class);
			startActivity(settings);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		CursorLoader c = new CursorLoader(this);
		c.setUri(Item.URI);
		c.setProjection(new String[]{Item._ID,
									 Item._TITLE,
									 Item._BRIEF,
									 Item._URL,
									 "ifnull(" + Item._TIME
									 + ", " + Item._TIME_SAVE
									 + ") AS " + Item._TIME});
		c.setSortOrder(Item._TIME + " DESC, " + Item._TITLE);
		return c;
	}
	
	
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		adapt.changeCursor(data);
	}
	
	
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapt.changeCursor(null);
	}
	
	
	private class UpdateListener extends BroadcastReceiver {
		private Context mContext;
		
		public UpdateListener(Context c) {
			mContext = c;
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
}