package ca.marklauman.rssreader.providers.service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashSet;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import ca.marklauman.rssreader.providers.DBSchema.Item;
import ca.marklauman.rssreader.providers.service.WebFormats.RSS;
import ca.marklauman.rssreader.providers.service.WebFormats.RSS.Tag;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.util.SparseIntArray;

public abstract class RSSParser {
	/** Standard {@link XmlPullParser} flag for the end of
	 *  the document.
	 *  (see {@link XmlPullParser#END_DOCUMENT})        */
	private static final int EVENT_END_DOC = XmlPullParser.END_DOCUMENT;
	/** Standard {@link XmlPullParser} flag for the start
	 *  of a tag.
	 *  (see {@link XmlPullParser#START_TAG})        */
	private static final int EVENT_TAG_ST = XmlPullParser.START_TAG;
	/** Standard {@link XmlPullParser} flag for the end
	 *  of a tag.
	 *  (see {@link XmlPullParser#END_TAG})     */
	private static final int EVENT_TAG_END = XmlPullParser.END_TAG;
	
	
	/** Listener interface for classes who wan to know
	 *  the progress of an {@link RSSParser}.
	 * @author Mark Lauman                           */
	public interface ParseRequester {
		/** Called when a new item has been read.
		 *  @param perc The percentage toward completion. */
		public void onParserUpdate(int perc);
		/** Return a ContentResolver instance for your
		 *  application's package.                  */
		public ContentResolver getContentResolver();
		/** Return the id of the folder being parsed. */
		public long getFoldId();
	}
	
	
	
	public static HashSet<Long> parse(ProgressStream in, ParseRequester req) {
		/* This function is mainly the setup. Others handle
		 * the parsing itself. This is mainly to provide a
		 * clean interface to external classes.
		 * Parsing begins in readFeed()                   */
		
		// WebFormats is required to do parsing.
		// Check to see if it is set now.
		if(!WebFormats.isSet())
			throw new NullPointerException("WebFormats have not been set. Have you called WebFormats.setup()?");
		
		req.onParserUpdate(0);
		
		// Xml.newPullParser() has bugs in 2.3 (see
		// http://android-developers.blogspot.ca/2011/12/watch-out-for-xmlpullparsernexttext.html
		// for details). We will used XmlPullParserFactory.
		XmlPullParser parser;
		try {
			parser = XmlPullParserFactory.newInstance().newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(in, null);
	        parser.nextTag();
	        return readFeed(parser, in, req);
		}
		catch (XmlPullParserException e) {}
		catch (IOException e) {}
		/* Only reached in the event of xml errors in the
		 * first 2 lines of the file.                  */
        return new HashSet<Long>();
	}
	
	
	
	private static HashSet<Long> readFeed(XmlPullParser parser, ProgressStream in,
								   ParseRequester req) {
		HashSet<Long> id_list = new HashSet<Long>();
		try {
			// continue until we reach the end of the doc
			while(parser.next() != EVENT_END_DOC) {
				// ignore all events that aren't a start tag
				if(parser.getEventType() != EVENT_TAG_ST) continue;
				
				if(Tag.isItem(parser.getName())) {
					long id = readItem(parser, in, req);
					if(id != 0) id_list.add(id);
					
					// notify the caller of progress
					req.onParserUpdate(in.getPercent());
				}
			}
		} catch(Exception e) {}
		req.onParserUpdate(100);
		return id_list;
	}
	
	
	private static long readItem(XmlPullParser parser, ProgressStream in,
								 ParseRequester req) {
		// The name of the item tag we are in
		String item_name = parser.getName();
		// The current priority level of each attribute
		SparseIntArray cur_priority
					= new SparseIntArray(Tag.TYPES.length);
		for(int type : Tag.TYPES) {
			cur_priority.put(type, Integer.MAX_VALUE);
		}
		
		ContentValues values = new ContentValues();
		values.put(Item._FEED_ID, req.getFoldId());
		
		// while we are still in the item tag
		try{while(!(parser.next() == EVENT_TAG_END
					&& parser.getName().equalsIgnoreCase(item_name))) {
			// ignore all events that aren't a start tag
			if(parser.getEventType() != EVENT_TAG_ST) continue;
			
			
			// interpret the tag
			Tag tag = Tag.fromName(parser.getName());
			if(tag.type == Tag.TYPE_UNIMPORTANT)
				skip(parser);
			else if(tag.priority < cur_priority.get(tag.type)) {
				// we are only interested in tags with a
				// lower priority number.
				// place the value into the tag
				if(tag.type == Tag.TYPE_TITLE)
					values.put(Item._TITLE, readText(parser));
				else if(tag.type == Tag.TYPE_LINK)
					values.put(Item._LINK, readText(parser));
				else if(tag.type == Tag.TYPE_CONTENT)
					values.put(Item._DESC, readText(parser));
				else if(tag.type == Tag.TYPE_TIME) {
					long time = readTime(parser);
					if(time != 0)
						values.put(Item._TIME, time);
				}
				// update the priority value
				cur_priority.put(tag.type, tag.priority);
			// the item is not high enough priority
			} else skip(parser);	
		}} catch(Exception e) {}
		
		// Insert the item, return its id number.
		Uri uri = req.getContentResolver()
					 .insert(Item.URI, values);
		if(uri == null) return 0;
		String res = uri.getLastPathSegment();
		if(res == null) return 0;
		try {
			return Long.parseLong(res);
		} catch(NumberFormatException e) {
			return 0;
		}
	}
	
	
	/** Skips this tag and all the stuff inside it.
	 *  @param parser The parser, positioned at the
	 *  start of the tag to skip                 */
	private static void skip(XmlPullParser parser) {
		try {
			// invalid start point, do nothing
			if (parser.getEventType() != XmlPullParser.START_TAG)
				return;
			int depth = 1;
			int event;
			while (depth != 0) {
				event = parser.next();
				if(event == EVENT_TAG_END) depth--;
				else if(event == EVENT_TAG_ST) depth++;
			}
		} catch(Exception e) {}
	}
	
	/** Reads one tag and returns the text inside it.
	 *  Formatting will <b>not</b> be removed. To do that
	 *  call {@link #stripHtml(String)}.
	 *  @param parser The parser linked to the feed. The
	 *  current position of the parser should be the start
	 *  of the tag you wish to read.
	 *  @return The contents of the tag.                */
	private static String readText(XmlPullParser parser) {
		String result = "";
		
		try {
			if (parser.next() == XmlPullParser.TEXT) {
				result += parser.getText().trim();
		        parser.nextTag();
		    }
		} catch (Exception e) {}
	    return result;
	}
	
	/** Interprets an internet datetime object. This function
	 *  heavily uses {@link #timeParsers} to achieve its
	 *  result.
	 *  @param parser The parser linked to the feed,
	 *  positioned at the start of the time tag.
	 *  @return The value of the time as the number of
	 *  seconds since the unix epoch.
	 *  <p><b>Warning:</b> in Java, unix time objects
	 *  are in milliseconds. In SQL they're in seconds.</p> */
	private static long readTime(XmlPullParser parser) {
		long res = 0;
		try {
			String str_time = readText(parser);
			for(SimpleDateFormat format : RSS.time_formats) {
				try {
					res = format.parse(str_time).getTime();
				} catch (Exception e) {}
				if(res != 0) return res;
			}
		} catch (Exception e) {}
		return res;
	}
	
	
}