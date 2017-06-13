package mobile.android.idrivesafe2;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AboutFragment extends Fragment {

	private View view;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		// Inflate the layout for this fragment
		view = inflater.inflate(R.layout.about_view, container, false);
		changeFonts();
		return view;
	}
	
	private void changeFonts() {
		Typeface myTypeface = Typeface.createFromAsset(getActivity()
				.getAssets(), "fonts/KoshgarianRegular.ttf");

		TextView tvTagline = (TextView) view
				.findViewById(R.id.tvTagline);
		TextView tvDevelopedBy = (TextView) view
				.findViewById(R.id.tvDevelopedBy);
		tvTagline.setTypeface(myTypeface);
		tvDevelopedBy.setTypeface(myTypeface);
	}
}
