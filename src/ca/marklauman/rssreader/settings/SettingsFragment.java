package ca.marklauman.rssreader.settings;

import ca.marklauman.rssreader.R;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;

/** Fragment used for settings in android > 3.0.
 *  @author Mark Lauman                       */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsFragment extends PreferenceFragment {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        String category = getArguments().getString("category");
        if("feeds".equals(category))
        	addPreferencesFromResource(R.xml.pref_feeds);
	}
}
