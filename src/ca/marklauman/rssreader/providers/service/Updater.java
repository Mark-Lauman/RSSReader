package ca.marklauman.rssreader.providers.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;

import ca.marklauman.rssreader.R;
import ca.marklauman.rssreader.providers.DBService;
import ca.marklauman.rssreader.providers.DBSchema.Feed;
import ca.marklauman.rssreader.providers.service.RSSParser.ParseRequester;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Updater implements ParseRequester {

	/** Number of phases per feed */
	private static final int NUM_PHASES = 2;
	
	
	/** String prefix for an in-progress download.    */
	private String down_prefix = null;
	/** String prefix for an in-progress parse.       */
	private String parse_prefix = null;
	/** String for the cleanup step.                  */
	private String down_clean = null;
	/** Error message for when the device is offline. */
	private String err_offline = null;
	/** Error message for when the url is invalid.    */
	private String err_url = null;
	/** Error message for when the connection is bad. */
	private String err_conn = null;
	/** Error message for no cache file.              */
	private String err_file = null;
	/** Error message for bad write to cache.         */
	private String err_write = null;
	
	private DBService service;
	/** The connectivity manager.
	 *  (for testing if we're online)  */
	private ConnectivityManager connMgr = null;
	
	/** Total number of feeds in this download operation */
	int num_feeds;
	/** Number of feeds already downloaded in this operation. */
	int done_feeds;
	/** ID value of the current feed. */
	long id;
	/** Name of the current feed (for display) */
	String feed_name;
	/** Url of the current feed */
	String feed_url;
	

	/** Default Constructor. Sets up basic environment
	 *  variables, such as output strings, etc.
	 *  @param service The service that will call this
	 *  updater. Progress updates will be sent here. */
	public Updater(DBService service) {
		this.service = service;
		Resources r = service.getResources();
		
		// Setup the web formats if needed
		if(!WebFormats.isSet())
			WebFormats.set(r);
		
		// Setup the output strings
		down_prefix = r.getString(R.string.download_prefix) + " ";
		parse_prefix = r.getString(R.string.download_parse_prefix) + " ";
		down_clean = r.getString(R.string.download_clean);
		err_offline = r.getString(R.string.download_offline);
		err_url = r.getString(R.string.download_bad_url) + " ";
		err_conn = r.getString(R.string.download_bad_conn) + " ";
		err_file = r.getString(R.string.download_bad_file);
		err_write = r.getString(R.string.download_bad_write);
	}
	
	
	
	/** Update the indicated feeds from the internet.
	 * @param feed_ids The ids of all the feeds to update. */
	public void update(long... feed_ids) {
		
		// select the provided feeds from the database
		String sel_str;
		String[] sel_ids;
		if(feed_ids != null && feed_ids.length > 0) {
			sel_str = "";
			sel_ids = new String[feed_ids.length];
			for(int i=0; i < feed_ids.length; i++) {
				sel_str += " or " + Feed._ID + "=?";
				sel_ids[i] = "" + feed_ids[i];
			}
			sel_str = sel_str.substring(4);
		} else {
			sel_ids = null;
			sel_str = null;
		}
		Cursor feeds = service.getContentResolver()
							  .query(Feed.URI, null, sel_str,
									  sel_ids, null);
		if(feeds == null || !feeds.moveToFirst()) {
			service.sendDone();
			return;
		}
		
		
		if(!isOnline()) {
			service.sendError(DBService.ERR_OFFLINE,
							  err_offline);
			service.sendDone();
			return;
		}
		
		// setup loop variables
		int col_id = feeds.getColumnIndex(Feed._ID);
		int col_name = feeds.getColumnIndex(Feed._NAME);
		int col_url = feeds.getColumnIndex(Feed._URL);
		num_feeds = feeds.getCount();
		HashSet<Long> ids = new HashSet<Long>();
		
		// Choose the cache location
		File cache_dir = service.getExternalCacheDir();
		File cache_file = new File(cache_dir.getPath()
									+ "tmp.rss");
		ProgressStream cache_str = null;
		
		// The main processing loop
		feeds.moveToPosition(-1);
		
		for(done_feeds = 0;
				isOnline() && feeds.moveToNext();
				done_feeds++) {
			try {
				// get feed info
				id = feeds.getLong(col_id);
				feed_name = feeds.getString(col_name);
				feed_url = feeds.getString(col_url);
				
				// download the feed to the cache file
				URL url = new URL(feed_url);
				cache_str = download(url, cache_file, feed_name);
				if(cache_str == null) continue;
				
				// parse the cache file, insert data to database
				ids.addAll(RSSParser.parse(cache_str, this));
			} catch (MalformedURLException e) {
				service.sendError(DBService.ERR_URL, err_url + feed_name);
			} finally {
				// close the connection
				try { if(cache_str != null) cache_str.close();
				} catch (IOException e) {}
			}
		}
		
		if(!isOnline()) {
			done_feeds = num_feeds - 1;
			service.sendError(DBService.ERR_OFFLINE, err_offline);
		}
		
		// Cleanup
		sendPhaseProgress(down_clean, 1, 0);
		cache_file.delete();
		feeds.close();
		// TODO: Purge old feed items
	}
	
	
	/** Checks to see if the device is connected to the
	 *  internet.
	  * @return {@code true} if the internet is available. */
	private boolean isOnline() {
		if(connMgr == null)
			connMgr = (ConnectivityManager) service.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		return networkInfo != null && networkInfo.isConnected();
	}
	
	
	@Override
	public void onParserUpdate(int perc) {
		sendPhaseProgress(parse_prefix + feed_name, 1, perc);
	}
	
	
	/** Send out a progress update for this phase of the
	 *  current feed.
	 *  @param desc A description of the current phase.
	 *  @param phase The phase for this progress update.
	 *  @param perc The progress of that phase as a percent. */
	private void sendPhaseProgress(String desc, int phase, int perc) {
		int res = (done_feeds * NUM_PHASES * 100 // complete feeds
				   + phase * 100				 // complete phases
				   + perc)						 // current progress
				   / (NUM_PHASES * num_feeds);	 // max value / 100
		service.sendProgress(desc, res);
	}
	
	@Override
	public long getFoldId() {
		return id;
	}

	@Override
	public ContentResolver getContentResolver() {
		return service.getContentResolver();
	}
	
	
	/** Functionally identical to a call to
	 *  {@link #download(URL, File, String, long)}
	 *  where {@code ifModSince = 0}.
	 *  @param source The source url to download.
	 *  @param dest The destination file for the feed.
	 *  @param name The name of the current feed.
	 *  @return A {@link ProgressStream} linked to the
	 *  destination file, or {@code null} if the
	 *  file was not created. (feed up-to-date,
	 *  bad connection, etc).                       */
	public ProgressStream download(URL source, File dest, String feed_name) {
		return download(source, dest, feed_name, 0);
	}
	
	
	/** Downloads the feed at the given url to a
	 *  chache file.
	 *  @param source The source url to download.
	 *  @param dest The destination file for the feed.
	 *  @param name The name of the current feed.
	 *  @param ifModSince Do not download the feed if
	 *  it has not changed since this time.
	 *  (Unix timecode format - ms since Unix epoch)
	 *  @return A {@link ProgressStream} linked to the
	 *  destination file, or {@code null} if the
	 *  file was not created. (feed up-to-date,
	 *  bad connection, etc).                       */
	public ProgressStream download(URL source, File dest, String feed_name, long ifModSince) {
		if(!isOnline()) {
			service.sendError(DBService.ERR_OFFLINE, err_offline);
			return null;
		}
		// variable setup
		String desc = down_prefix + " " + feed_name;
		BufferedOutputStream out = null;
		HttpURLConnection conn = null;
		ProgressStream in = null;
		ProgressStream res;
		
		sendPhaseProgress(desc, 0, 0);
		
		try {
			// Link to the cache file (out)
			FileOutputStream str = new FileOutputStream(dest);
			out = new BufferedOutputStream(str);
			
			// Link to the webserver (in)
			conn = (HttpURLConnection) source.openConnection();
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			conn.connect();
			in = new ProgressStream(conn.getInputStream(),
									conn.getContentLength());
			
			// Download the data
			try {
				int prog = 0;
				for(int dat = in.read(); 0 <= dat; dat = in.read()) {
					out.write(dat);
					// progress of the load
					sendPhaseProgress(desc, 0, prog);
				}
				out.flush();
			} catch(IOException e) {
				// Error writing to file
				// (file may still contain data)
				service.sendError(DBService.ERR_WRITE, err_write);
			}
			
			
			FileInputStream in_str;
			try {
				in_str = new FileInputStream(dest);
				res = new ProgressStream(in_str,
		  				dest.length());
			} catch (FileNotFoundException e) {
				// If the file doesn't exist anymore, then
				// another error will have triggered.
				res = null;
			}
			
		// Handle errors that will create no cache file
		} catch (FileNotFoundException e) {
			// Cannot access the cache file
			service.sendError(DBService.ERR_FILE, err_file);
			res = null;
		} catch (IOException e) {
			// Error reaching webserver
			service.sendError(DBService.ERR_CONN, err_conn + feed_name);
			res = null;
		} finally {
			// close the connections
			try { if(out != null) out.close();
			} catch (IOException e) {}
			try { if(in != null) in.close();
			} catch (IOException e) {}
			if(conn != null) conn.disconnect();
			// send 100% progress
			sendPhaseProgress(desc, 0, 100);
		}
		
		return res;
	}
}