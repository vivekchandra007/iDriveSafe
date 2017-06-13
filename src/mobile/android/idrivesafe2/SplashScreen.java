package mobile.android.idrivesafe2;

import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.WebView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SplashScreen extends Activity {

	static Boolean stopUpdates = false;
	
	private TextView tvLoadMessage;
	private WebView webVLoader;
	private LocationManager locationManager;
	private LocationListener locationListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash_screen);
		
		RelativeLayout rLayBackground = (RelativeLayout) findViewById(R.id.rLayBackground);
		rLayBackground.setOnClickListener(new RelativeLayout.OnClickListener() {
			@Override
			public void onClick(View v) {
				hideNavigationBar();
			}
		});	
		
		changeFonts();
		initialize();	
	}

	private void initialize() {
		stopUpdates = false;
		webVLoader = (WebView) findViewById(R.id.webVLoader);
		webVLoader.loadUrl("file:///android_asset/gifs/loader_blue.gif");
		webVLoader.setBackgroundColor(0x00000000);
		webVLoader.setVisibility(View.VISIBLE);
		tvLoadMessage.setVisibility(View.VISIBLE);
		tvLoadMessage.setText(getResources().getString(R.string.loading_location));
		
		// Acquire a reference to the system Location Manager
		locationManager = (LocationManager) getSystemService(
						Context.LOCATION_SERVICE);
		locationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				// Called when a new location is found by the network location
				// provider.
				if (!stopUpdates) {
					moveToNextActivity();
				} else {
					try {
						locationManager.removeUpdates(locationListener);
						stopUpdates = true;
						locationListener =null;
						locationListener = null;
					} catch (Exception e) {
						//don't do anything
					}
				}					
			}

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onProviderDisabled(String provider) {
			}
		};
		Random r = new Random();
		int delay = r.nextInt(3500 - 500) + 500;
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				// Do something after delay second(s)
				locationManager.requestLocationUpdates(
						LocationManager.NETWORK_PROVIDER, 0, 10, locationListener);
				locationManager.requestLocationUpdates(
						LocationManager.GPS_PROVIDER, 0, 10, locationListener);
			}
		}, delay);		
	}

	private void moveToNextActivity() {
		// Delay opening of Status activity by random milliseconds between 1500
		// and 3000
		locationManager.removeUpdates(locationListener);
		stopUpdates = true;
		locationListener =null;
		locationListener = null;
		webVLoader.setVisibility(View.INVISIBLE);
		tvLoadMessage.setVisibility(View.VISIBLE);
		tvLoadMessage.setText(getResources().getString(R.string.welcome_message));
		
		Random r = new Random();
		int delay = r.nextInt(1500 - 500) + 500;
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				// Do something after delay milliseconds
				Intent statusIntent = new Intent(getApplicationContext(),
						MainActivity.class);
				startActivity(statusIntent);
			}
		}, delay);
	}

	private void hideNavigationBar() {
		// Hide the navigation bar
		View decorView = getWindow().getDecorView();
		int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
		decorView.setSystemUiVisibility(uiOptions);
	}

	private void changeFonts() {
		Typeface myTypeface = Typeface.createFromAsset(getAssets(),
				"fonts/KoshgarianRegular.ttf");
		tvLoadMessage = (TextView) findViewById(R.id.tvLoadMessage);
		tvLoadMessage.setTypeface(myTypeface);
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		hideNavigationBar();
	}
}
