package ca.marklauman.tools;

import android.util.Log;

public interface Debug {
	/** Print a message to {@link Log#d(String, String)}.
	 * @param msg The message to print.                */
	public void msg(String msg);
}
