package ca.marklauman.rssreader.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

/** An {@link EditTextPreference} that displays
 *  its current value as its summary. If the current
 *  value is the empty string or {@code null},
 *  displays the XML specified summary instead.
 * @author Mark Lauman                            */
public class ValueTextPreference extends EditTextPreference {
	private Context c;
	private CharSequence null_summary;
	
	public ValueTextPreference(Context context) {
		super(context);
		c = context;
		null_summary = getSummary();
		setSummary();
	}
	
	public ValueTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		c = context;
		null_summary = getSummary();
		setSummary();
	}
	
	public ValueTextPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		c = context;
		null_summary = getSummary();
		setSummary();
	}
	
	@Override
	protected void onDialogClosed (boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		setSummary();
	}
	
	/** Set the summary to the value of the preference. */
	public void setSummary() {
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(c);
		String val = p.getString(getKey(), "");
		val = val.trim();
		if("".equals(val))
			setSummary(null_summary);
		else setSummary(val);
	}
}