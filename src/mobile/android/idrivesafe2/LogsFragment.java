package mobile.android.idrivesafe2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class LogsFragment extends Fragment {

	private View view;
	private Handler mHandler;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.logs_view, container, false);
		mHandler = new Handler();
		changeFonts();
		// Inflate the layout for this fragment
		return view;
	}

	private void initialize() {
		final String logs = readLogs();
		
		mHandler.post(new Runnable() {
			
			@Override
			public void run() {
				TextView tvAllLogs = (TextView) view.findViewById(R.id.tvAllLogs);
				if (logs != null && logs.length() > 0)
					tvAllLogs.setText(logs.toString());
				else
					tvAllLogs.setText(getActivity().getString(R.string.no_logs));
				
				int declinedCallsCount = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getInt(MainActivity.PREF_DECLINED_CALLS_COUNTER, 0);
				int repliedTextsCount = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getInt(MainActivity.PREF_REPLIED_TEXTS_COUNTER, 0);
				
				TextView tvLifetimeDeclinedCallsCounter = (TextView) view.findViewById(R.id.tvLifetimeDeclinedCallsCounter);
				TextView tvLifetimeRepliedTextsCounter = (TextView) view.findViewById(R.id.tvLifetimeRepliedTextsCounter);
				tvLifetimeDeclinedCallsCounter.setText("0" + String.valueOf(declinedCallsCount));
				tvLifetimeRepliedTextsCounter.setText("0" + String.valueOf(repliedTextsCount));			
			}
		});
		
//		RelativeLayout rLayLogsLifetimeHeader = (RelativeLayout) view.findViewById(R.id.rLayLogsLifetimeHeader);
//		
//		if (declinedCallsCount == 0 && repliedTextsCount == 0)
//			rLayLogsLifetimeHeader.setVisibility(View.INVISIBLE);
//		else
//			rLayLogsLifetimeHeader.setVisibility(View.VISIBLE);
	}

	private String readLogs() {
		String eol = System.getProperty("line.separator");
		StringBuffer stringBuffer = null;
		try {
			BufferedReader inputReader = new BufferedReader(
					new InputStreamReader(getActivity().openFileInput(
							MainActivity.LOGS_FILE)));
			String inputString;
			stringBuffer = new StringBuffer();
			while ((inputString = inputReader.readLine()) != null) {
				//in chronological order, recent first
				stringBuffer.insert(0, inputString + eol + eol);
				//stringBuffer.append(inputString + eol + eol);
			}
			if (stringBuffer != null) {
				try {
					inputReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (stringBuffer != null)
			return stringBuffer.toString();
		else
			return "";
	}
	
	@Override
	public void onResume() {
	    super.onResume();
	    new Thread(new Runnable() {
			
			@Override
			public void run() {
				initialize();				
			}
		}).start();
	}
	
	private void changeFonts() {
		Typeface myTypeface2 = Typeface.createFromAsset(getActivity().getAssets(),"fonts/quarthin.ttf");
		Typeface myTypeface = Typeface.createFromAsset(getActivity().getAssets(),"fonts/KoshgarianRegular.ttf");
        
		TextView tvLifetimeDeclinedCalls = (TextView) view.findViewById(R.id.tvLifetimeDeclinedCalls);
		TextView tvLifetimeRepliedTexts = (TextView) view.findViewById(R.id.tvLifetimeRepliedTexts);
		TextView tvLifetimeDeclinedCallsCounter = (TextView) view.findViewById(R.id.tvLifetimeDeclinedCallsCounter);
		TextView tvLifetimeRepliedTextsCounter = (TextView) view.findViewById(R.id.tvLifetimeRepliedTextsCounter);
    
		tvLifetimeDeclinedCallsCounter.setTypeface(myTypeface2);
		tvLifetimeRepliedTextsCounter.setTypeface(myTypeface2);
		tvLifetimeDeclinedCalls.setTypeface(myTypeface);
		tvLifetimeRepliedTexts.setTypeface(myTypeface);
	}

}
