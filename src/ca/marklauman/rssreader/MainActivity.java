package ca.marklauman.rssreader;

import ca.marklauman.rssreader.adapters.ItemAdapter;
import ca.marklauman.rssreader.database.Item;
import ca.marklauman.rssreader.settings.SettingsActivity;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.widget.ListView;

public class MainActivity extends SherlockFragmentActivity
						  implements LoaderCallbacks<Cursor> {
	
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
		ListView list = (ListView) findViewById(R.id.list);
		adapt = new ItemAdapter(this);
		list.setAdapter(adapt);
		
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
			ContentValues values = new ContentValues();
			values.put(Item._TITLE, "Report: Remember Me Developer Dontnod Files for Bankruptcy");
			values.put(Item._CONTENT, "<div style='float: left; width: 90px; height: 90px; margin: 0 5px 5px 0; display: block;'><img src='http://cdn.themis-media.com/media/global/images/library/deriv/672/672461.png' width='90' height='90'></div><p>Multiple French media outlets are reporting that Dontnod Entertainment has filed for \"redressement judiciaire,\" which is the French equivalent of bankruptcy.</p>\n<p><a href=\"http://www.escapistmagazine.com/news/view/131863-Report-Remember-Me-Developer-Dontnod-Files-for-Bankruptcy?utm_source=rss&amp;utm_medium=rss&amp;utm_campaign=news\" title=\"\" target=\"_blank\">View Article</a></p>");
			values.put(Item._TIME, 1391177520000L);
			values.put(Item._URL, "http://www.escapistmagazine.com/news/view/131863-Report-Remember-Me-Developer-Dontnod-Files-for-Bankruptcy?utm_source=rss&amp;utm_medium=rss&amp;utm_campaign=news");
			getContentResolver().insert(Item.URI, values);
			return true;
		
		// Settings Button
		case R.id.action_settings:
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
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