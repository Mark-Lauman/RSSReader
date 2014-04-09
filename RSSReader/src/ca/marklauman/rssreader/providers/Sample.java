package ca.marklauman.rssreader.providers;

import ca.marklauman.rssreader.providers.DBSchema.Feed;
import ca.marklauman.rssreader.providers.DBSchema.Folder;
import ca.marklauman.rssreader.providers.DBSchema.Item;
import android.content.ContentValues;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;


public abstract class Sample {
	
	public static void doNothing() {}
	
	/** Good for making LogCat work. */
	@Deprecated
	public static void throwError() {
		if(true)
			throw new NullPointerException("Hello Logcat!");
	}
	
	
	public static Cursor feedCursor() {
		String[] cols = {Feed._ID, Feed._NAME, Feed._URL};
		MatrixCursor cursor = new MatrixCursor(cols);
		String[] row = {"1", "The Escapist",
				"http://www.escapistmagazine.com/rss/news/"};
		cursor.addRow(row);
		row = new String[]{"2", "LoadingReadyRun",
				"http://feeds.feedburner.com/Loadingreadyrun"};
		cursor.addRow(row);
		return cursor;
	}
	
	
	
	public static void makefolders(ContextWrapper c) {
		msg(insertFolder(c, 1, "News"));
		msg(insertFolder(c, 2, "Comics"));
		msg(insertFolder(c, 3, "Video"));
	}
	
	private static String insertFolder(ContextWrapper c, long id, String name) {
		ContentValues values = new ContentValues();
		values.put(Folder._ID, id);
		values.put(Folder._NAME, name);
		Uri res = c.getContentResolver()
				   .insert(Folder.URI,
						   values);
		return res + "";
	}
	
	
	
	public static void makeLinkedFeeds(ContextWrapper c) {
		msg(insertLinkFeed(c, 1, 1,
						"The Escapist",
						"http://www.escapistmagazine.com/rss/news/"));
		msg(insertLinkFeed(c, 2, 3,
						"LoadingReadyRun",
						"http://feeds.feedburner.com/Loadingreadyrun"));
		msg(insertLinkFeed(c, 3, 2,
						"Gunnerkrigg court",
						"http://www.rsspect.com/rss/gunner.xml"));
		msg(insertLinkFeed(c, 4, 2,
						"The Trenches",
						"http://trenchescomic.com/feed"));
		msg(insertLinkFeed(c, 5, 2,
						"Table Titans",
						"http://www.tabletitans.com/feed"));
		msg(insertLinkFeed(c, 6, 2,
						"XKCD What If?",
						"http://what-if.xkcd.com/feed.atom"));
	}
	
	private static String insertLinkFeed(ContextWrapper c,
			long id, long folder,
			String name, String url) {
		ContentValues values = new ContentValues();
		values.put(Feed._ID, id);
		values.put(Feed._NAME, name);
		values.put(Feed._URL, url);
		values.put(Feed._FOLD_ID, folder);
		Uri uri = Feed.URI;
		return "" + c.getContentResolver()
					 .insert(uri,
							 values);
	}

	public static void msg(String msg) {
		Log.d("RSSReader.Sample", msg);
	}
	
	
	public static void makeFeedItems(ContextWrapper c) {
		// Escapist
		int feed = 1;
		msg(insertItem(c, feed, 1391263920000L,
				"Report: Remember Me Developer Dontnod Files for Bankruptcy",
				"<div style='float: left; width: 90px; height: 90px; margin: 0 5px 5px 0; display: block;'><img src='http://cdn.themis-media.com/media/global/images/library/deriv/672/672461.png' width='90' height='90'></div><p>Multiple French media outlets are reporting that Dontnod Entertainment has filed for \"redressement judiciaire,\" which is the French equivalent of bankruptcy.</p><p><a href=\"http://www.escapistmagazine.com/news/view/131863-Report-Remember-Me-Developer-Dontnod-Files-for-Bankruptcy?utm_source=rss&amp;utm_medium=rss&amp;utm_campaign=news\" title=\"\" target=\"_blank\">View Article</a></p>",
				"http://www.escapistmagazine.com/news/view/131863-Report-Remember-Me-Developer-Dontnod-Files-for-Bankruptcy?utm_source=rss&amp;utm_medium=rss&amp;utm_campaign=news"));
		msg(insertItem(c, feed, 1391263740000L,
				"Microsoft CEO Search May Be Nearing Conclusion",
				"<div style='float: left; width: 90px; height: 90px; margin: 0 5px 5px 0; display: block;'><img src='http://cdn.themis-media.com/media/global/images/library/deriv/672/672465.png' width='90' height='90'></div><p>Satya Nadella is the man everyone's talking about, even the people who don't remember him.</p>\n<p><a href=\"http://www.escapistmagazine.com/news/view/131866-Microsoft-CEO-Search-May-Be-Nearing-Conclusion?utm_source=rss&amp;utm_medium=rss&amp;utm_campaign=news\" title=\"\" target=\"_blank\">View Article</a></p>",
				"http://www.escapistmagazine.com/news/view/131866-Microsoft-CEO-Search-May-Be-Nearing-Conclusion?utm_source=rss&amp;utm_medium=rss&amp;utm_campaign=news"));
		
		// Loading Ready Run
		feed = 2;
		msg(insertItem(c, feed,
				"Twenty-Thirteen Teaser",
				"A little taste of the LRR Season 10 finale. Look it for it on Monday!&lt;img src=\"http://feeds.feedburner.com/~r/Loadingreadyrun/~4/TgXNv4SGTVk\" height=\"1\" width=\"1\"/&gt;",
				"http://feedproxy.google.com/~r/Loadingreadyrun/~3/TgXNv4SGTVk/Twenty-Thirteen-Teaser"));
		msg(insertItem(c, feed, 138792960000L,
				"The Black Santa Xmas Special",
				"Merry Christmas! We got you an extra long Feed Dump. With a side of stupidity.&lt;img src=\"http://feeds.feedburner.com/~r/Loadingreadyrun/~4/Sfcap8xFZWs\" height=\"1\" width=\"1\"/&gt;",
				"http://feedproxy.google.com/~r/Loadingreadyrun/~3/Sfcap8xFZWs/8623-The-Black-Santa-Xmas-Special"));
	}
	
	
	private static String insertItem(ContextWrapper c,
			long feed,
			String title, String desc, String link) {
		ContentValues values = new ContentValues();
		values.put(Item._FEED_ID, feed);
		values.put(Item._TITLE, title);
		values.put(Item._DESC, desc);
		values.put(Item._LINK, link);
		Uri uri = Item.URI;
		return "" + c.getContentResolver()
					 .insert(uri, values);
	}
	
	
	private static String insertItem(ContextWrapper c,
			long feed, long time,
			String title, String desc, String link) {
		ContentValues values = new ContentValues();
		values.put(Item._FEED_ID, feed);
		values.put(Item._TITLE, title);
		values.put(Item._DESC, desc);
		values.put(Item._LINK, link);
		values.put(Item._TIME, time);
		Uri uri = Item.URI;
		return "" + c.getContentResolver()
					 .insert(uri, values);
	}
}