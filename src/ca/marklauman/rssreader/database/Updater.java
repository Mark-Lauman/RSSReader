package ca.marklauman.rssreader.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import ca.marklauman.rssreader.database.schema.Item;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class Updater extends IntentService {
	
	/** When you launch an {@link Intent} calling
	 *  this service, use
	 *  {@link Intent#putExtra(String, String)}
	 *  with this name to pass the url of the
	 *  feed to download.                    */
	public static final String PARAM_URL = "url";
	
	public Updater() {
		/* The name passed to super will become
		 * the name of this thread (but not its label
		 * - that's set in the manifest            */
		super("ca.marklauman.rssreader.database.Updater");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// Get the url to load
		if(intent == null) return;
		Bundle extras = intent.getExtras();
		if(extras == null) return;
		String url = extras.getString(PARAM_URL);
		if(url == null) return;
		
		// Check if we're online
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if(connMgr == null) {
			Log.d("Updater", "No Connection");
			return;
		}
		NetworkInfo net_info = connMgr.getActiveNetworkInfo();
		if(net_info == null) {
			Log.d("Updater", "No Connection");
			return;
		}
		if(net_info.isConnected()) {
			switch(net_info.getType()) {
			case ConnectivityManager.TYPE_ETHERNET:
			case ConnectivityManager.TYPE_WIFI:
				Log.d("Updater", "LocalNet available!");
				break;
			default:
				Log.d("Updater", "RemoteNet available!");
			}
		} else
			Log.d("Updater", "No Connection");
		
		// Download the feed
		
		// Parse the feed
		// TODO: This is a placeholder implementation
		String path = PreferenceManager.getDefaultSharedPreferences(this)
				   .getString("feed1", "");
		RSSParser parser = null;
		try {
			File file = new File(path);
			parser = new RSSParser(new FileInputStream(file),
											 file.length());
			ContentResolver cr = getContentResolver();
			ContentValues rss_item = parser.readItem();
			while(rss_item != null) {
				cr.insert(Item.URI, rss_item);
				rss_item = parser.readItem();
			}
		} catch (FileNotFoundException e) {}
		finally {
			if(parser != null) parser.close();
		}
	}
}