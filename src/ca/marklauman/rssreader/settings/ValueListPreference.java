package ca.marklauman.rssreader.settings;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

/** A {@link ListPreference} that displays the
 *  current selection as its summary.
 *  @author Mark Lauman                     */
public class ValueListPreference extends ListPreference {
	
	public ValueListPreference(Context context) {
		super(context);
	}
	
	public ValueListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	public CharSequence getSummary() {
		return getEntry();
	}
	
	@Override
	public void setValue(final String value) {
		super.setValue(value);
		notifyChanged();
	}
}