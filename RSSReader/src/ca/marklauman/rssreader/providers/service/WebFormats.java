package ca.marklauman.rssreader.providers.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import ca.marklauman.rssreader.R;

import android.content.res.Resources;

public abstract class WebFormats {
	/** {@code true} if {@link #set(Resources)} has been
	 *  called.                                         */
	private static boolean set = false;
	
	/** Returns {@code true} only if the web formats have
	 *  been set with {@link #set(Resources)}.         */
	public static boolean isSet() {
		return set;
	}
	
	/** Takes all the web formats from their xml file
	 *  ({@code res/values/web_formats.xml}).
	 *  Until this function is called, all non-final variables
	 *  in this class are null.
	 *  @param res The application's {@link Resources}.
	 *  Used to get the xml contents.                       */
	public static void set(Resources res) {
		if(set) return;
		
		// if mod since format string
		HTTP.if_mod_since = new SimpleDateFormat(res.getString(R.string.http_mod_since), Locale.US);
		HTTP.if_mod_since.format(new Date());
		
		// setup the tag mapper
		int[] arr_ids = {R.array.rss_tag_item,
						 R.array.rss_tag_title,
						 R.array.rss_tag_link,
						 R.array.rss_tag_content,
						 R.array.rss_tag_time};
		int[] types = {RSS.Tag.TYPE_ITEM,
					   RSS.Tag.TYPE_TITLE,
					   RSS.Tag.TYPE_LINK,
					   RSS.Tag.TYPE_CONTENT,
					   RSS.Tag.TYPE_TIME};
		RSS.Tag.tag_mapper = new HashMap<String, Integer[]>();
		for(int i = 0; i < arr_ids.length; i++) {
			String[] names = res.getStringArray(arr_ids[i]);
			for(int priority = 0;
					priority < names.length;
					priority++) {
				RSS.Tag.tag_mapper.put(names[priority],
						new Integer[]{types[i], priority});
			}
		}
		
		// setup rss time formats
		RSS.sample_times = res.getStringArray(R.array.rss_sample_times);
		String[] t_formats = res.getStringArray(R.array.rss_timeformats);
		RSS.time_formats = new SimpleDateFormat[t_formats.length];
		for(int i = 0; i < RSS.time_formats.length; i++) {
			RSS.time_formats[i] = new SimpleDateFormat(t_formats[i], Locale.US);
		}
		
		set = true;
	}
	
	
	public static class HTTP {
		public static SimpleDateFormat if_mod_since;
	}
	
	
	public static class RSS {
		
		/** <p>A collection of formatters for translating internet
		 *  time strings to Unix time. The strings that form the
		 *  basis for these parsers can be found in
		 *  {@code res/values/web_formats.xml} under the name
		 *  {@code timeformats}.</p>                          */
	 	public static SimpleDateFormat[] time_formats;
		/** Some sample times for priming the
		 *  {@link rss_time_formats}. Comes from 
		 *  {@code res/values/web_formats.xml}.               */
		public static String[] sample_times;
		
		
		public static class Tag {
			/** The type of the tag (see constants) */
			public int type;
			/** The priority level of the tag. 0 is the highest
			 *  priority, and {@link Integer#MAX_VALUE} is the
			 *  lowest. Higher level tags should override lower
			 *  level ones (if you find a new date at a higher
			 *  priority level, it should be your new date to
			 *  display)                                    */
			public int priority;
			
			/** This tag may be safely ignored. */
			public static final int TYPE_UNIMPORTANT = 0;
			/** This tag contains an rss item. */
			public static final int TYPE_ITEM = 1;
			/** This tag is a title element.   */
			public static final int TYPE_TITLE = 2;
			/** This tag is a link element.    */
			public static final int TYPE_LINK = 3;
			/** This tag is a content element. */
			public static final int TYPE_CONTENT = 4;
			/** This tag is a time element.    */
			public static final int TYPE_TIME = 5;
			/** A list of all types except unimportant. */
			public static final int[] TYPES =
				{TYPE_ITEM, TYPE_TITLE, TYPE_LINK,
				 TYPE_CONTENT, TYPE_TIME};

			/** Maps each tag string to its type
			 *  and priority level.              */
			private static HashMap<String, Integer[]> tag_mapper;
			
			
			Tag() {
				type = TYPE_UNIMPORTANT;
				priority = Integer.MAX_VALUE;
			}
			
			Tag(int type, int priority) {
				this.type		= type;
				this.priority	= priority;
			}
			
			/** Determines if the passed tag name is an item tag
			 *  @param name The name to interpret.
			 *  @return {@code true} if it is an item, false if
			 *  it is not.                                     */
			public static boolean isItem(String name) {
				if(!WebFormats.isSet())
					throw new NullPointerException("WebFormats have not been set. Have you called WebFormats.setup()?");
				return fromName(name).type == TYPE_ITEM;
			}
			
			/** Turn a tag name into a {@link Tag} object.
			 *  @param name The name to parse.
			 *  @return A {@link Tag} representing the tag name. */
			public static Tag fromName(String name) {
				if(!WebFormats.isSet())
					throw new NullPointerException("WebFormats have not been set. Have you called WebFormats.setup()?");
				Integer[] res = tag_mapper.get(name);
				if(res == null)
					return new Tag(TYPE_UNIMPORTANT, Integer.MAX_VALUE);
				return new Tag(res[0], res[1]);
			}
		}
	}
}
