package ca.marklauman.rssreader.providers;

import ca.marklauman.rssreader.R;
import ca.marklauman.rssreader.providers.service.Updater;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class DBService extends IntentService {
	
	/** <p>Extra flag that indicates what type of
	 *  request the {@link DBService} should process.
	 *  In return broadcasts, indicates the nature of
	 *  the current operation.</p>
	 *  <p>When starting this service with an Intent,
	 *  you <b>must</b> specify this extra with
	 *  {@link Intent#putExtra(String, int)} for the
	 *  service to do anything. Valid values are
	 *  provided by this class, and start with
	 *  "{@code REQ_}".</p> */
	public static final String EXT_REQ = "request";
	/** A request to update feeds from the internet.
	 *  If {@link #EXT_FEEDS} is unspecified, all
	 *  feeds are refreshed. If it is specified, only
	 *  those feeds are refreshed.                 */
	public static final int REQ_REFRESH = 1;
	/** A request to delete 1 or more items. */
	public static final int REQ_DEL = 2;
	/** A request to do an undo operation    */
	public static final int REQ_UNDO = 3;
	
	/** Extra flag indicating what feeds this
	 *  operation should affect.
	 *  Passed in as an array of {@link long}s
	 *  indicating the ids of the folders.    */
	public static final String EXT_FEEDS = "feeds";
	
	/** {@link Intent}s broadcast by this service
	 *  will have this action specified.       */
	public static final String BROADCAST_ACT =
			"ca.marklauman.rssdata.progress";
	
	
	public static final String PROG_DESC =
			"description";
	public static final String PROG_PERC =
			"percentage";
	public static final String PROG_ERR =
			"error";
	
	/** Error thrown when the device is offline */
	public static final int ERR_OFFLINE = 1;
	/** Error thrown if the url for this feed is invalid */
	public static final int ERR_URL = 2;
	/** Error thrown if there is a problem connecting to
	 *  the server in question.                       */
	public static final int ERR_CONN = 3;
	/** Error thrown if there is a problem opening the
	 *  cache file.                                 */
	public static final int ERR_FILE = 4;
	/** Error thrown if there was a problem writing the
	 *  feed to the cache file                       */
	public static final int ERR_WRITE = 5;
	
	
	/** Indicates if this operation is done or not.
	 *  becomes {@code true} on the first call to
	 *  {@link #sendDone()}.                            */
	private boolean prog_done;
	/** The percentage posted in the last progress update.
	 *  Used to prune out duplicate broadcasts.         */
	private int prog_perc;
	/** The description posted in the last progress update.
	 *  Used to prune out duplicate broadcasts.         */
	private String prog_desc;
	
	
	public DBService() {
		super("DatabaseService");
	}
	
	
	@Override
	protected void onHandleIntent(Intent intent) {
		prog_done = false;
		
		prog_perc = -1;
		prog_desc = "";
		
		switch(intent.getExtras().getInt(EXT_REQ)) {
		case REQ_REFRESH:
			Updater update = new Updater(this);
			update.update(null);
			break;
		}
		
		sendDone();
	}
	
	
	/** Send out a message with a progress update
	 *  (Note that this is for overall progress, not the
	 *  progress of a particular phase or segment).
	 *  @param desc A status description.
	 *  @param perc The total progress as a percent.     */
	public void sendProgress(String desc, int perc) {
		if(prog_done) return;
		// prune duplicate broadcasts.
		if(prog_perc == perc
				&& prog_desc.equals(desc)) return;
		prog_perc = perc;
		prog_desc = desc;
		
		// Send the broadcast
		Intent broadcast = new Intent();
		broadcast.setAction(BROADCAST_ACT);
		broadcast.addCategory(Intent.CATEGORY_DEFAULT);
		broadcast.putExtra(PROG_DESC, desc);
		broadcast.putExtra(PROG_PERC, perc);
		sendBroadcast(broadcast);
	}
	
	
	/** Send out a progress message, saying there was a
	 *  problem. Also pauses execution for 2 seconds after
	 *  the message is sent (to allow the user to see the
	 *  error uninterrupted).
	 *  @param type The type of the error (see static vars).
	 *  @param desc A description to clarify the error. */
	public void sendError(int type, String desc) {
		if(prog_done) return;
		Intent broadcast = new Intent();
		broadcast.setAction(BROADCAST_ACT);
		broadcast.addCategory(Intent.CATEGORY_DEFAULT);
		broadcast.putExtra(PROG_ERR, type);
		broadcast.putExtra(PROG_DESC, desc);
		sendBroadcast(broadcast);
		SystemClock.sleep(2000);
	}
	
	/** Send out a progress message, saying the current
	 *  operation is complete. No further messages will
	 *  be sent after this one.                      */
	public void sendDone() {
		String msg_prog_done = getResources().getString(R.string.download_done);
		sendProgress(msg_prog_done, 100);
		prog_done = true;
	}
	
	
	/** Do a refresh request on the database. Handles
	 *  creating the intent and launching the service.
	 *  @param c The context making the request.
	 *  @param feed_ids The ids of feeds to refresh.
	 *  If {@code null}, refreshes all feeds.       */
	public static void refresh(Context c, long... feed_ids) {
		Intent dbIntent = new Intent(c, DBService.class);
		dbIntent.putExtra(EXT_REQ, REQ_REFRESH);
		dbIntent.putExtra(EXT_FEEDS, feed_ids);
		c.startService(dbIntent);
	}
}