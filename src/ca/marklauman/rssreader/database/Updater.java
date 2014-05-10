package ca.marklauman.rssreader.database;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;

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

/** <p>Service used to update the database from the
 *  internet. Only the URLs specified in the
 *  ({@link #PARAM_URL} extra will be updated.</p>
 *  <p>The progress of this service may be tracked
 *  with {@link #BROADCAST}s. For more details,
 *  see all the variables whose names begin with
 *  MSG_</p>
 *  @author Mark Lauman                       */
public class Updater extends IntentService {
	
	/** When you launch an {@link Intent} calling
	 *  this service, use
	 *  {@link Intent#putExtra(String, String)}
	 *  with this name to pass the URL of the
	 *  feed to download.                    */
	public static final String PARAM_URL =
			"ca.marklauman.rssdata.url";
	
	/** {@link Intent}s broadcast by this service
	 *  will have this type of action.         */
	public static final String BROADCAST =
			"ca.marklauman.rssdata.update";
	
	/** This extra in a broadcast indicates the
	 *  current phase the updater is in. (Either
	 *  {@link #PHASE_DOWNLOAD} or
	 *  {@link #PHASE_PARSE}).  */
	public static final String MSG_PHASE =
			"ca.marklauman.rssdata.phase";
	/** The updater is downloading data. */
	public static final int PHASE_DOWNLOAD = 0;
	/** The updater is parsing the data. */
	public static final int PHASE_PARSE = 1;
	/** Number of phases. Phase index starts at 0
	 *  and ends at {@code PHASE_QTY - 1}      */
	public static final int PHASE_QTY = 2;
	
	/** This extra in a broadcast indicates the
	 *  progress of the current phase. It is passed
	 *  as an integer from 0 - 100 (inclusive).  */
	public static final String MSG_PROG =
			"ca.marklauman.rssdata.progress";
	
	/** This extra in a broadcast indicates the
	 *  URL being processed right now. The URL
	 *  is passed as a String.       */
	public static final String MSG_URL =
			"ca.marklauman.rssdata.url";
	
	/** This extra in a broadcast indicates any
	 *  errors which have occurred. (one of
	 *  {@link #ERR_NONE}, {@link #ERR_PARAMS},
	 *  {@link #ERR_OFFLINE}, {@link #ERR_URL},
	 *  {@link #ERR_ACCESS} or {@link #ERR_CONN}. */
	public static final String MSG_ERR =
			"ca.marklauman.rssdata.error";
	/** Everything is fine. No error has occurred. */
	public static final int ERR_NONE = 0;
	/** Bad input parameters. */
	public static final int ERR_PARAMS = 0;
	/** There is no internet connection. */
	public static final int ERR_OFFLINE = 2;
	/** The url passed was invalid */
	public static final int ERR_URL = 3;
	/** Updater does not have write access
	 *  to the cache directory.         */
	public static final int ERR_ACCESS = 4;
	/** Connection lost mid-op
	 *  (to cache file or to web). */
	public static final int ERR_CONN = 5;
	
	
	/** The last progress value sent with
	 *  {@link #sendProgress(Bundle)}. */
	private int last_prog;
	
	
	/** Standard constructor called by the system.
	 *  Do <b>not</b> call this directly. Instead
	 *  launch this service with an {@link Intent}. */
	public Updater() {
		/* The name passed to super() will become
		 * the name of this thread for the system.
		 * The visible name of this service is set
		 * in the Android manifest.             */
		super("ca.marklauman.rssdata.Updater");
	}
	
	
	@Override
	protected void onHandleIntent(Intent intent) {
		/* Current status:
		 * Parameters unchecked, download phase, 0% */
		Bundle status = new Bundle();
		status.putInt(MSG_PHASE, PHASE_DOWNLOAD);
		status.putInt(MSG_PROG, 0);
		status.putInt(MSG_ERR, ERR_PARAMS);
		last_prog = 0;
		
		// Get the url to load
		if(intent == null) {
			// bad params
			transmit(status);	return;
		}
		Bundle extras = intent.getExtras();
		if(extras == null) {
			// bad params
			transmit(status);	return;
		}
		String url = extras.getString(PARAM_URL);
		status.putString(MSG_URL, url);
		if(url == null)  {
			// bad params
			transmit(status);	return;
		}
		
		// Check if we're online
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		status.putInt(MSG_ERR, ERR_OFFLINE);
		if(connMgr == null) {
			// Offline - quit
			transmit(status);	return;
		}
		NetworkInfo net_info = connMgr.getActiveNetworkInfo();
		if(net_info == null) {
			// Offline - quit
			transmit(status);	return;
		}
		if(net_info.isConnected()) {
			switch(net_info.getType()) {
			case ConnectivityManager.TYPE_ETHERNET:
			case ConnectivityManager.TYPE_WIFI:
				// Online - proceed to next step
				status.putInt(MSG_ERR, ERR_NONE);
				transmit(status);
				break;
			default:
				// Offline - quit
				transmit(status);	return;
			}
		} else {
			// Offline - quit
			transmit(status);	return;
		}
		
		// We are online, on wifi or ethernet
		// Setup variables for download phase
		String cache_dir = getCacheDir().getPath() + "/";
		File cache_file = new File(cache_dir + "tmp.rss");
		HttpURLConnection conn = null;
		ProgressStream in = null;
		BufferedOutputStream out = null;
		
		// Download the feed.
		try {
			out = new BufferedOutputStream(new FileOutputStream(cache_file));
			URL web_url = new URL(url);
			conn = (HttpURLConnection) web_url.openConnection();
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			conn.connect();
			in = new ProgressStream(conn.getInputStream(),
									conn.getContentLength());
			// the actual transfer
			int prog = 0;
			for(int dat=in.read(); 0<=dat; dat=in.read()) {
				out.write(dat);
				status.putInt(MSG_PROG, prog);
				sendProgress(status);
			}
		} catch (MalformedURLException e1) {
			status.putInt(MSG_ERR, ERR_URL);
		} catch (FileNotFoundException e) {
			// cannot access cache directory
			status.putInt(MSG_ERR, ERR_ACCESS);
		} catch (IOException e) {
			// bad connection of some kind
			status.putInt(MSG_ERR, ERR_CONN);
		} finally {
			// close all connections
			try{ if(in != null) in.close(); }
			catch (IOException e) {}
			try{ if(out != null) {
				out.flush();
				out.close();
			}} catch (IOException e) {}
			if(conn != null) conn.disconnect();
		}
		// close this phase
		status.putInt(MSG_PROG, 100);
		transmit(status);
		if(status.getInt(MSG_ERR) != ERR_NONE)
			return;
		
		// Parse the feed
		status.putInt(MSG_PHASE, PHASE_PARSE);
		status.putInt(MSG_PROG, 0);
		sendProgress(status);
		RSSParser parser = null;
		ContentResolver cr = getContentResolver();
		try {
			parser = new RSSParser(new FileInputStream(cache_file),
								   cache_file.length());
			ContentValues rss_item = parser.readItem();
			while(rss_item != null) {
				cr.insert(Item.URI, rss_item);
				rss_item = parser.readItem();
				status.putInt(MSG_PROG, parser.getProgress());
				sendProgress(status);
			}
		} catch (FileNotFoundException e) {}
		finally {
			if(parser != null) parser.close();
		}
		
		// Delete the raw feed
		cache_file.delete();
		
		// Clear old feed items from the db
		long forget = Long.parseLong(
				  PreferenceManager.getDefaultSharedPreferences(this)
								   .getString("forget", "0"));
		if(forget != 0) {
			long now = Calendar.getInstance()
							   .getTimeInMillis();
			long old = now - forget;
			cr.delete(Item.URI,
					  Item._TIME_INSERT + " < ?",
					  new String[]{"" + old});
		}
		
		// Close up
		status.putInt(MSG_PROG, 100);
		sendProgress(status);
	}
	
	
	/** Transmit a message to anyone listening.
	 *  @param msg The message to send (passed
	 *  as the extras of the intent)        */
	private void transmit(Bundle msg) {
		if(msg == null) return;
		Intent broadcast = new Intent();
		broadcast.setAction(BROADCAST);
		broadcast.addCategory(Intent.CATEGORY_DEFAULT);
		broadcast.putExtras(msg);
		sendBroadcast(broadcast);
	}
	
	
	/** This is similar to {@link #transmit(Bundle)}
	 *  except it checks the {@code #MSG_PROG}
	 *  parameter to make sure the last transmission
	 *  wasn't the same. This helps to cut down on
	 *  redundant broadcasts.
	 *  @param msg The message to send (passed
	 *  as the extras of the intent)        */
	private void sendProgress(Bundle msg) {
		if(msg == null) return;
		int prog = msg.getInt(MSG_PROG);
		if(prog == last_prog) return;
		last_prog = prog;
		transmit(msg);
	}
}