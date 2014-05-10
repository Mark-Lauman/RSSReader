package ca.marklauman.rssreader.settings;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/** An {@link EditTextPreference} that displays
 *  its current value as its summary. If the current
 *  value is the empty string or {@code null},
 *  displays the XML specified summary instead.
 *  @author Mark Lauman                           */
public class ValueTextPreference extends EditTextPreference {
	
	public ValueTextPreference(Context context) {
		super(context);
	}
	
	public ValueTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public ValueTextPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	public CharSequence getSummary() {
		String text = getText();
		if(text == null || "".equals(text))
			return super.getSummary();
		else return text;
	}
	
	@Override
	public void setText(String text) {
		super.setText(text);
		notifyChanged();
	}
}