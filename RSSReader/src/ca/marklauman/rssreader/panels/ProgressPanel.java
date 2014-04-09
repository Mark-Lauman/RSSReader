package ca.marklauman.rssreader.panels;

import ca.marklauman.rssreader.R;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

/** A basic progress bar with a title and percentage bar.
 *  In this project, used inside {@link ItemPanel}.    */
public class ProgressPanel extends Fragment {

	private TextView title;
	private TextView perc;
	private ProgressBar bar;
	int col_title_norm;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View res = inflater.inflate(R.layout.panel_prog, container, false);
		title	= (TextView) res.findViewById(R.id.prog_title);
		col_title_norm = title.getCurrentTextColor();
		perc	= (TextView) res.findViewById(R.id.prog_perc);
		perc.setText("0%");
		bar		= (ProgressBar) res.findViewById(R.id.prog_bar);
		bar.setMax(100);
		bar.setProgress(0);
		return res;
	}
	
	public void setProgress(String title, int percent) {
		this.title.setTextColor(col_title_norm);
		this.title.setText(title);
		perc.setText(percent + "%");
		bar.setProgress(percent);
	}
	
	public static ProgressPanel getPanel(FragmentManager fm) {
		return (ProgressPanel) fm.findFragmentById(R.id.prog_panel);
	}
	
	public void showError(String msg) {
		title.setTextColor(Color.RED);
		title.setText(msg);
	}
}