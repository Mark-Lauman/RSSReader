package ca.marklauman.rssreader;

import ca.marklauman.rssreader.database.Updater;
import ca.marklauman.rssreader.panel.item.ItemFragment;
import ca.marklauman.rssreader.settings.SettingsActivity;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.ActionMode.Callback;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.Loader;
import android.widget.ProgressBar;

public class MainActivity extends SherlockFragmentActivity {
	
	/** ID for the {@link Loader} associated with
	 *  the item panel. */
	public static final int LOADER_ID_ITEMS = 1;
	
	/** The current context bar in use (null if
	 *  the regular action bar is in use instead) */
	private ActionMode contextBar = null;
	/** Handler for displaying messages
	 *  sent by {@link Updater}.     */
	private DatabaseUI dbHandler;
	/** The ListFragment containing the RSS items. */
	private ItemFragment item_frag;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// if no settings, apply the default ones
		PreferenceManager.setDefaultValues(this, R.xml.pref_feeds, false);
		
		// set up the database handler
		ProgressBar bar = (ProgressBar) findViewById(R.id.progress);
		dbHandler = new DatabaseUI(this, bar);
		IntentFilter filter = new IntentFilter(Updater.BROADCAST);
		filter.addCategory(Intent.CATEGORY_DEFAULT);
		registerReceiver(dbHandler, filter);
		
		// set up the item fragment
		item_frag = (ItemFragment) getSupportFragmentManager().findFragmentById(R.id.list);
	}
	
	
	@Override
	public void onStart() {
		super.onStart();
		// restart the loaders
		getSupportLoaderManager().restartLoader(LOADER_ID_ITEMS, null, item_frag);
	}
	
	
	@Override
	public void onDestroy() {
		unregisterReceiver(dbHandler);
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
	public ActionMode startActionMode(Callback callback) {
		if(contextBar != null)
			contextBar.finish();
		contextBar = getSherlock().startActionMode(callback);
		return contextBar;
	}
	
	
	/** Removes all local instances of the current
	 *  {@link ActionMode}, allowing it to be
	 *  garbage collected.
	 *  The {@code ActionMode} is not closed -
	 *  users may call {@link ActionMode#finish()}
	 *  to do that.
	 *  @return The {@code ActionMode} formerly
	 *  tied to this activity, or {@code null}
	 *  if no mode was active
	 *  (the standard action bar was displayed). */
	public ActionMode clearActionMode() {
		ActionMode mode = contextBar;
		contextBar = null;
		return mode;
	}
}