package ca.marklauman.rssreader;

import ca.marklauman.rssreader.adapters.ItemAdapter;
import ca.marklauman.rssreader.database.Updater;
import ca.marklauman.rssreader.database.schema.Item;
import ca.marklauman.rssreader.settings.SettingsActivity;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends SherlockFragmentActivity
						  implements LoaderCallbacks<Cursor> {
	
	/** Adapter for the item list */
	ItemAdapter adapt;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
	
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
	
	
	@Override
	public void onStart() {
		super.onStart();
		
		TextView txt = (TextView) findViewById(R.id.text);
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		txt.setText(pref.getString("feed1", ""));
		
		ListView list = (ListView) findViewById(R.id.list);
		adapt = new ItemAdapter(this);
		list.setAdapter(adapt);
		list.setOnItemClickListener(adapt);
		
		getSupportLoaderManager().restartLoader(1, null, this);
	}
	
	
	@Override
	public void onDestroy() {
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
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		CursorLoader c = new CursorLoader(this);
		c.setUri(Item.URI);
		c.setProjection(new String[]{Item._ID,
									 Item._TITLE,
									 Item._BRIEF,
									 Item._URL,
									 Item._TIME,
									 Item._TIME_SAVE});
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
}