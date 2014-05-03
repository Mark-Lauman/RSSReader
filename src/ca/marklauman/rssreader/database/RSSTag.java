package ca.marklauman.rssreader.database;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

/** Class defining the characteristics of an RSS tag.
 *  Helps to identify tags and their types.
 *  @author Mark Lauman                           */
public final class RSSTag {
	
	/** One return value of {@link #type(String)}.
	 *  Indicates an unknown tag.               */
	public static final int TYPE_UNKNOWN = 0;
	/** One return value of {@link #type(String)}.
	 *  Indicates an item tag.                  */
	public static final int TYPE_ITEM = 1;
	/** One return value of {@link #type(String)}.
	 *  Indicates a title tag.                  */
	public static final int TYPE_TITLE = 2;
	/** One return value of {@link #type(String)}.
	 *  Indicates a url tag.                    */
	public static final int TYPE_URL = 3;
	/** One return value of {@link #type(String)}.
	 *  Indicates a content tag.                */
	public static final int TYPE_CONTENT = 4;
	/** One return value of {@link #type(String)}.
	 *  Indicates a time tag.                   */
	public static final int TYPE_TIME = 5;
	
	/** Set which contains all known item tags. */
	private static final HashSet<String> item_tags;
	/** Maps RSS tags to their types. */
	private static final HashMap<String, Integer> type_map;
	/** Contains all known RSS time formats. */
	private static final SimpleDateFormat[] time_formats;
	/** Index of the last time format used
	 *  by {@link #parseTime(String)}. */
	private static int last_time;
	
	
	static {
		item_tags = new HashSet<String>();
		item_tags.add("item");
		item_tags.add("entry");
		
		type_map = new HashMap<String, Integer>();
		type_map.put("item", TYPE_ITEM);
		type_map.put("entry", TYPE_ITEM);
		
		type_map.put("title", TYPE_TITLE);
		
		type_map.put("link", TYPE_URL);
		
		type_map.put("description", TYPE_CONTENT);
		type_map.put("content", TYPE_CONTENT);
		
		type_map.put("pubDate", TYPE_TIME);
		type_map.put("dc:date", TYPE_TIME);
		type_map.put("updated", TYPE_TIME);
		type_map.put("published", TYPE_TIME);
		
		String[] formats = new String[]{"EEE, dd MMM yyyy kk:mm:ss ZZZ",
										"yyyy-MM-dd'T'kk:mm:ssZZZZZ",
										"yyyy-MM-dd'T'kk:mm:ss'Z'"};
		time_formats = new SimpleDateFormat[formats.length];
		for(int i=0; i<formats.length; i++)
			time_formats[i] = new SimpleDateFormat(formats[i], Locale.US);
		last_time = 0;
	}
	
	
	/** Do not instantiate. */
	RSSTag() {}
	
	
	/** Checks to see if a tag is
	 *  of {@link #TYPE_ITEM}.
	 *  @param tagname The name of the tag.
	 *  @return {@code true} if the tag is an item
	 *  tag. {@code false} otherwise.             */
	public static boolean isItem(String tagname) {
		return item_tags.contains(tagname);
	}
	
	
	/** Identify the type of the tag.
	 *  @param tagname The name of the tag.
	 *  @return The type of the tag (possible values
	 *  are static variables of this class
	 *  with TYPE in front of their names).       */
	public static int type(String tagname) {
		Integer res = type_map.get(tagname);
		if(res == null) return 0;
		return res;
	}
	
	
	/** Parse an RSS-formatted time.
	 *  @param time The time as seen in the RSS file.
	 *  @return The time in the UNIX time format
	 *  (ms since UNIX epoch).                     */
	public synchronized static long parseTime(String time) {
		// try last successful formatter first
		try { return time_formats[last_time].parse(time)
										    .getTime();
		} catch (ParseException e) {}
		
		/* if that fails, try them all until
		 * something works.               */
		for(last_time=0; last_time<time_formats.length; last_time++) {
			try { return time_formats[last_time].parse(time)
												.getTime();
			} catch (ParseException e) {}
		}
		
		// If nothing works, return 0
		return 0;
	}
}