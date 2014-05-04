package ca.marklauman.rssreader.database;

import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import ca.marklauman.rssreader.database.schema.Item;

import android.content.ContentValues;

/** This class parses RSS files for you. Input is
 *  buffered, so you may use an unbuffered stream
 *  if you wish - the class will wrap it.
 *  @author Mark Lauman                        */
public final class RSSParser {
	
	/** Stream connected to the RSS file. */
	private ProgressStream str;
	/** Tool for parsing the rss file.
	 *  (RSS data is structured as XML) */
	private XmlPullParser parser;
	
	
	/** Standard constructor. Be sure to call
	 *  {@link #close()} when you are done with this
	 *  parser!
	 *  @param in An InputStream tied to the RSS data.
	 *  @param length The estimated length of the
	 *  stream in bytes (used to estimate progress
	 *  in {@link #getProgress()}).             */
	public RSSParser(InputStream in, long length) {
		setInput(in, length);
	}
	
	
	/** Closes the current {@link InputStream}
	 *  (if any) and sets the new stream
	 *  to the one provided.
	 *  @param in An InputStream tied to the RSS data.
	 *  @param length The estimated length of the
	 *  stream in bytes (used to estimate progress
	 *  in {@link #getProgress()}).             */
	public void setInput(InputStream in, long length) {
		close();
		str = new ProgressStream(in, length);
		// Xml.newPullParser() has bugs in 2.3 (see
		// http://android-developers.blogspot.ca/2011/12/watch-out-for-xmlpullparsernexttext.html
		// for details). We will used XmlPullParserFactory.
		try {
			parser = XmlPullParserFactory.newInstance().newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(str, null);
		} catch (XmlPullParserException e) {}
	}
	
	
	/** Read the next RSS item from the file.
	 *  @return A {@link ContentValues} object
	 *  representing the item. It follows the
	 *  structure set out in the {@link Item}
	 *  table for RSSData. If no items remain,
	 *  returns {@code null} instead.       */
	public ContentValues readItem() {
		if(str == null || parser == null)
			return null;
		
		/* If we are at the end of the file,
		 * return null.                   */
		try {
			if(parser.getEventType() == XmlPullParser.END_DOCUMENT)
				return null;
		} catch (XmlPullParserException e) {}
		
		/* Skip all tags until we reach an <item> tag,
		 * the end of the file, or an exception.    */
		try{ while(parser.next() != XmlPullParser.END_DOCUMENT
				    && !(parser.getEventType() == XmlPullParser.START_TAG
						 || RSSTag.isItem(parser.getName()))) {
		}
		} catch (Exception e) {
			// Exception = no more items found.
			return null;
		}
		
		/* If we are at the end of the file,
		 * return null.                   */
		try {
			if(parser.getEventType() == XmlPullParser.END_DOCUMENT)
				return null;
		} catch (XmlPullParserException e) {}
		
		// We are now at the start of an <item> tag.
		ContentValues res = new ContentValues();
		/* Loop until we reach an </item> tag,
		 * the end of the file, or an exception. */
		try{ while(parser.next() != XmlPullParser.END_DOCUMENT
				   && !(parser.next() == XmlPullParser.END_TAG
				   		&& RSSTag.isItem(parser.getName()))) {
			// ignore all events that aren't a start tag
			if(parser.getEventType() != XmlPullParser.START_TAG)
				continue;
			// interpret the start tag
			switch(RSSTag.type(parser.getName())) {
			case RSSTag.TYPE_TITLE:
				res.put(Item._TITLE, readTag());
				break;
			case RSSTag.TYPE_URL:
				res.put(Item._URL, readTag());
				break;
			case RSSTag.TYPE_CONTENT:
				res.put(Item._CONTENT, readTag());
				break;
			case RSSTag.TYPE_TIME:
				res.put(Item._TIME, RSSTag.parseTime(readTag()));
				break;
			}
		}} catch (Exception e) {
			/* on the first exception, return the
			 * partial data we have acquired. */
		}
		return res;
	}
	
	
	/** Read the contents of the current tag.
	 *  Advances {@link #parser} to the end of that
	 *  tag.
	 *  @return The contents of the tag.   */
	private String readTag() {
		String result = "";
		try {
			if (parser.next() == XmlPullParser.TEXT) {
				result += parser.getText().trim();
		        parser.nextTag();
		    }
		} catch (Exception e) {}
	    return result;
	}
	
	
	/** Get the amount of the stream that has been parsed.
	 *  @return How much has been parsed as a percent (range
	 *  of values is 0 - 100)                             */
	public int getProgress() {
		if(str == null) return 100;
		else return str.getProgress();
	}
	
	
	/** Closes this parser. The source stream is also
	 *  closed and all resources released.         */
	public void close() {
		parser = null;
		try{ str.close();
		} catch (Exception e) {}
		str = null;
	}
}