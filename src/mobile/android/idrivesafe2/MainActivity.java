package mobile.android.idrivesafe2;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ImageView;

import mobile.android.idrivesafe2.service.*;

public class MainActivity extends FragmentActivity  {

	static final String LOGS_FILE = "iDriveSafeLogs";
	static final int DEFAULT_SAFE_SPEED_LIMIT = 50;
	static final String PROFILE_GENERAL = "GENERAL";
	static final String PROFILE_SILENT = "SILENT";
	static final String PREF_SPEED_LIMIT = "SAFESPEEDLIMIT";
	static final String PREF_PROFILE_SELECTED = "PROFILETOBESELECTED";
	static final String PREF_SEND_MSG = "SAFEMODESENDMSG";
	static final String PREF_MSG = "SAFEMODEMSG";
	static final String PREF_DECLINED_CALLS_COUNTER = "DECLINEDCALLSCOUNT";
	static final String PREF_REPLIED_TEXTS_COUNTER = "REPLIEDTEXTSCOUNT";
	static final String PREF_NEW_LOGS_COUNTER = "NEWLOGSCOUNT";
	
	static int safeSpeedLimit;
	static String profileToBeSelected;
	static Boolean safeModeSendMsg;
	static String safeModeMsg;
	
	private ViewPager pager;
	private WebView webVSignal;
	private ImageView imgVPageIcon;
	private ImageView imgVTitleArrowLeft;
	private ImageView imgVTitleArrowRight;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);
		webVSignal = (WebView) findViewById(R.id.webVSignal);
		imgVPageIcon = (ImageView) findViewById(R.id.imgVPageIcon);
		imgVTitleArrowLeft = (ImageView) findViewById(R.id.imgVTitleArrowLeft);
		imgVTitleArrowRight = (ImageView) findViewById(R.id.imgVTitleArrowRight);
		imgVTitleArrowLeft.setVisibility(View.GONE);
		imgVTitleArrowRight.setVisibility(View.VISIBLE);
		
		webVSignal.loadUrl("file:///android_asset/gifs/signal_animation.gif");
		webVSignal.setBackgroundColor(0x00000000);
		
		pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));
        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int position) {
				switch(position) {
	        	case 0:
	        		imgVPageIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_no_signal));
	        		webVSignal.setVisibility(View.GONE);
	        		imgVPageIcon.setVisibility(View.VISIBLE);
	        		imgVTitleArrowLeft.setVisibility(View.GONE);
	        		imgVTitleArrowRight.setVisibility(View.VISIBLE);
	        		break;
	        		
	        	case 1:	        		
	        		imgVPageIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_customize));
	        		webVSignal.setVisibility(View.GONE);
	        		imgVPageIcon.setVisibility(View.VISIBLE);
	        		imgVTitleArrowLeft.setVisibility(View.VISIBLE);
	        		imgVTitleArrowRight.setVisibility(View.VISIBLE);
	        		break;
	        		
	        	case 2:
	        		imgVPageIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_logs));
	        		webVSignal.setVisibility(View.GONE);
	        		imgVPageIcon.setVisibility(View.VISIBLE);
	        		imgVTitleArrowLeft.setVisibility(View.VISIBLE);
	        		imgVTitleArrowRight.setVisibility(View.VISIBLE);
	        		break;
	        		
	        	case 3:
	        		imgVPageIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_about));
	        		webVSignal.setVisibility(View.GONE);
	        		imgVPageIcon.setVisibility(View.VISIBLE);
	        		imgVTitleArrowLeft.setVisibility(View.VISIBLE);
	        		imgVTitleArrowRight.setVisibility(View.GONE);
	        		break;
	        		
	        	default:
	        		imgVPageIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_no_signal));
	        		webVSignal.setVisibility(View.GONE);
	        		imgVPageIcon.setVisibility(View.VISIBLE);
	        		imgVTitleArrowLeft.setVisibility(View.GONE);
	        		imgVTitleArrowRight.setVisibility(View.VISIBLE);
	        		break;
				}				
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub
				
			}
		});
        
        imgVTitleArrowLeft.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						imgVTitleArrowLeft.setAlpha(0.4f);
						break;
					case MotionEvent.ACTION_UP:
						imgVTitleArrowLeft.setAlpha(1.0f);
						pager.setCurrentItem(pager.getCurrentItem()-1);
				}
				return true;
			}
		});
        
        imgVTitleArrowRight.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					imgVTitleArrowRight.setAlpha(0.4f);
					break;
				case MotionEvent.ACTION_UP:
					imgVTitleArrowRight.setAlpha(1.0f);
					pager.setCurrentItem(pager.getCurrentItem()+1);
				}
				return true;
			}
		});
	}
	
	@Override
	public void onStop() {
	    super.onStop();
		if (!isMyServiceRunning(BackgroundLocationService.class)) {
			Intent startMyLocationService = new Intent(this, BackgroundLocationService.class);
			startService(startMyLocationService);
			//showToast("iDriveSafe Service started for background monitoring");
		}
	}
	
	@Override
	public void onStart() {
	    super.onStart();
	    if (isMyServiceRunning(BackgroundLocationService.class)) {
	    	stopService(new Intent(this, BackgroundLocationService.class));
	    	//showToast("iDriveSafe Background Service stopped since main application opened.");
	    }
	}
	
	private boolean isMyServiceRunning(Class<?> serviceClass) {
	    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (serviceClass.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	private class MyPagerAdapter extends FragmentPagerAdapter {
		
		public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int pos) {
            switch(pos) {
            	case 0:
            		retrievePreferences();
            		return new StatusFragment();            		
            	case 1:
            		retrievePreferences();
            		return new CustomizeFragment();            		
            	case 2:
            		return new LogsFragment();            		
            	case 3:
            		return new AboutFragment();            		
            	default:
            		retrievePreferences();
            		return new StatusFragment();
            }
        }

        @Override
        public int getCount() {
        	return 4;
        }
    }
	
	void retrievePreferences() {
		// Set defaults
		MainActivity.safeSpeedLimit = MainActivity.DEFAULT_SAFE_SPEED_LIMIT;
		MainActivity.profileToBeSelected = MainActivity.PROFILE_SILENT;
		MainActivity.safeModeSendMsg = true;
		MainActivity.safeModeMsg = getString(R.string.pref_auto_message_text);

		if (PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getString(
						MainActivity.PREF_SPEED_LIMIT, null) != null)
			MainActivity.safeSpeedLimit = Integer.parseInt(PreferenceManager
					.getDefaultSharedPreferences(
							getApplicationContext()).getString(
									MainActivity.PREF_SPEED_LIMIT, null));

		if (PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getString(
						MainActivity.PREF_PROFILE_SELECTED, null) != null)
			MainActivity.profileToBeSelected = PreferenceManager
					.getDefaultSharedPreferences(
							getApplicationContext()).getString(
									MainActivity.PREF_PROFILE_SELECTED, null);

		if (PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getString(MainActivity.PREF_SEND_MSG,
				null) != null) {
			if (PreferenceManager
					.getDefaultSharedPreferences(
							getApplicationContext())
					.getString(MainActivity.PREF_SEND_MSG, null).equalsIgnoreCase("true"))
				MainActivity.safeModeSendMsg = true;
			else
				MainActivity.safeModeSendMsg = false;
		}

		if (PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext())
				.getString(MainActivity.PREF_MSG, null) != null)
			MainActivity.safeModeMsg = PreferenceManager.getDefaultSharedPreferences(
					getApplicationContext()).getString(MainActivity.PREF_MSG,
					null);
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Since this is the start activity, so on press of back button, send application to background
		if(keyCode == KeyEvent.KEYCODE_BACK) {
			moveTaskToBack(true);
            return true;
        }
        return false;
    }
}
