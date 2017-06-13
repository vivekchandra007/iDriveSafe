package mobile.android.idrivesafe2;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import com.android.internal.telephony.ITelephony;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewPager;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class StatusFragment extends Fragment {

	private static final int LOW_BATTERY_LEVEL = 9;

	private static boolean blnDemoMode;
	private static boolean blnMaxSpeedReached;
	public static String lastIncomingNumber;
	public static int lastIncomingCallTime;

	private View view;
	private WebView webVSignal;
	private ImageView imgVPageIcon;
	private TextView tvStatus;
	private TextView tvSpeedDigit1;
	private TextView tvSpeedDigit2;
	private TextView tvSpeedDigit3;
	private TextView tvLocation;
	private TextView tvSafeMode;
	private RelativeLayout rLaySafeModeOuterRing;
	private RelativeLayout rLayLogsOuterRing;
	private RelativeLayout rLayDemoOuterRing;
	private TextView tvDemo;
	private BroadcastReceiver ringerModeChangeReceiver;

	private int speed;
	private Location recentLocation;
	private int hibernateMultiplier;
	private Boolean stopUpdates;
	private int idleCount;
	private LocationManager locationManager;
	private LocationListener locationListener;
	private Boolean blnSafeMode;
	private Boolean blnSafeModeOverridden;
	private TelephonyManager incomingCallManager;
	private PhoneStateListener incomingCallListener;
	private String profileStatus;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		view = inflater.inflate(R.layout.status_view, container, false);
		initialize();
		setupListeners();
		// trackSpeed();
		return view;
	}

	private void initialize() {
		webVSignal = (WebView) getActivity().findViewById(R.id.webVSignal);
		imgVPageIcon = (ImageView) getActivity()
				.findViewById(R.id.imgVPageIcon);
		tvStatus = (TextView) view.findViewById(R.id.tvStatus);
		tvSpeedDigit1 = (TextView) view.findViewById(R.id.tvSpeedDigit1);
		tvSpeedDigit2 = (TextView) view.findViewById(R.id.tvSpeedDigit2);
		tvSpeedDigit3 = (TextView) view.findViewById(R.id.tvSpeedDigit3);
		tvLocation = (TextView) view.findViewById(R.id.tvLocation);
		rLaySafeModeOuterRing = (RelativeLayout) view
				.findViewById(R.id.rLaySafeModeOuterRing);
		rLaySafeModeOuterRing.setVisibility(View.GONE);
		tvSafeMode = (TextView) view.findViewById(R.id.tvSafeMode);
		rLayLogsOuterRing = (RelativeLayout) view
				.findViewById(R.id.rLayLogsOuterRing);
		rLayDemoOuterRing = (RelativeLayout) view
				.findViewById(R.id.rLayResetToDefaultsOuterRing);
		tvDemo = (TextView) view.findViewById(R.id.tvDemo);
		changeFonts();

		speed = 0;
		hibernateMultiplier = 1;
		blnDemoMode = false;
		blnSafeMode = false;
		blnSafeModeOverridden = false;
		rLaySafeModeOuterRing.setBackground(getActivity().getResources().getDrawable(
				R.drawable.outer_ring_glow_red));
		tvSafeMode.setTextColor(getActivity().getResources().getColor(R.color.theme_red));
		tvSafeMode.setText(getActivity().getString(R.string.safe_mode_active));
		assignSpeed();
		tvLocation.setText("");
		tvStatus.setText("");
		// Acquire a reference to the system Location Manager
		locationManager = (LocationManager) getActivity().getSystemService(
				Context.LOCATION_SERVICE);
		recentLocation = locationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (recentLocation == null)
			recentLocation = locationManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (recentLocation != null) {
			setLocality(recentLocation);
		}

		GregorianCalendar date = new GregorianCalendar();
		lastIncomingCallTime = date.get(Calendar.HOUR) * 100
				+ date.get(Calendar.MINUTE);
		lastIncomingNumber = "";
		profileStatus = getCurrentProfile();
		ringerModeChangeReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				profileStatus = getCurrentProfile();
				if (profileStatus
						.equalsIgnoreCase(MainActivity.PROFILE_GENERAL))
					DontlistenForIncomingCalls();
			}
		};
	}

	private void setupListeners() {
		locationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				// Called when a new location is found by the network location
				// provider.
				setSpeed(location);
			}

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onProviderDisabled(String provider) {
			}
		};

		rLaySafeModeOuterRing.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!blnSafeModeOverridden) {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							getActivity());
					builder.setMessage(
							getActivity().getString(
									R.string.safe_mode_override_message))
							.setPositiveButton("Proceed",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											blnSafeModeOverridden = true;
											rLaySafeModeOuterRing
													.setBackground(getActivity().getResources()
															.getDrawable(
																	R.drawable.outer_ring_glow_blue));
											tvSafeMode
													.setTextColor(getActivity().getResources()
															.getColor(
																	R.color.theme_blue));
											tvSafeMode
													.setText(getActivity()
															.getString(
																	R.string.safe_mode_overridden));
										}
									})
							.setNegativeButton("Cancel",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											// User cancelled the dialog
											return;
										}
									}).setCancelable(false).create().show();
				} else {
					blnSafeModeOverridden = false;
					rLaySafeModeOuterRing.setBackground(getActivity().getResources()
							.getDrawable(R.drawable.outer_ring_glow_red));
					tvSafeMode.setTextColor(getActivity().getResources().getColor(
							R.color.theme_red));
					tvSafeMode.setText(getActivity().getString(
							R.string.safe_mode_active));
				}
			}
		});

		rLayDemoOuterRing.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					rLayDemoOuterRing.setAlpha(0.4f);
					return true;
				case MotionEvent.ACTION_UP:
					rLayDemoOuterRing.setAlpha(1.0f);
					if (!blnDemoMode) {
						blnDemoMode = true;
						tvDemo.setText(getActivity().getString(R.string.stop));
						tvDemo.setTextColor(getActivity().getResources()
								.getColor(R.color.theme_red));
						rLayDemoOuterRing.setBackground(getActivity()
								.getResources().getDrawable(
										R.drawable.button_back_active_small));
						dontTrackSpeed();
						testingSpeed();
					} else {
						blnDemoMode = false;
						tvDemo.setText(getActivity().getString(R.string.demo));
						tvDemo.setTextColor(getActivity().getResources()
								.getColor(R.color.theme_blue));
						rLayDemoOuterRing.setBackground(getActivity()
								.getResources().getDrawable(
										R.drawable.button_back_small));
						initialize();
						trackSpeed();
					}
					return true;
				}
				return false;
			}
		});

		rLayLogsOuterRing.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					rLayLogsOuterRing.setAlpha(0.4f);
					return true;
				case MotionEvent.ACTION_UP:
					rLayLogsOuterRing.setAlpha(1.0f);
					blnDemoMode = false;
					TextView tvRecentLogsCount = (TextView) view
							.findViewById(R.id.tvRecentLogsCount);
					PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().putInt(MainActivity.PREF_NEW_LOGS_COUNTER, 0).commit();	
					tvRecentLogsCount.setText("0");
					tvRecentLogsCount.setVisibility(View.GONE);
					// Move user to Logs page
					ViewPager pager = (ViewPager) getActivity().findViewById(
							R.id.pager);
					pager.setCurrentItem(2);
					return true;
				}
				return false;
			}
		});
	}

	private void trackSpeed() {
		blnDemoMode = false;
		tvDemo.setText(getActivity().getString(R.string.demo));
		tvDemo.setTextColor(getActivity().getResources().getColor(
				R.color.theme_blue));
		rLayDemoOuterRing.setBackground(getActivity().getResources()
				.getDrawable(R.drawable.button_back_small));
		webVSignal.setVisibility(View.VISIBLE);
		imgVPageIcon.setVisibility(View.GONE);

		Boolean lowBattery = false;
		if (getBatteryLevel() > LOW_BATTERY_LEVEL)
			lowBattery = false;
		else
			lowBattery = true;

		if (!lowBattery) {
			tvStatus.setText(getActivity().getResources().getString(R.string.active));
			// Register the listener with the Location Manager to receive
			// location updates
			locationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
			locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 0, 0, locationListener);
			stopUpdates = false;
			idleCount = 0;
		} else {
			// low battery, so halt all location updates and save battery
			hibernateMode();
			tvStatus.setText(getActivity().getResources().getString(R.string.low_battery));
		}
	}

	private void dontTrackSpeed() {
		webVSignal.setVisibility(View.GONE);
		imgVPageIcon.setVisibility(View.VISIBLE);
		tvStatus.setText(getActivity().getResources().getString(R.string.inactive));
		speed = 0;
		assignSpeed();
		locationManager.removeUpdates(locationListener);
		stopUpdates = true;

		makeProfileNormal();
		DontlistenForIncomingCalls();
	}

	private void setSpeed(Location location) {
		if (stopUpdates)
			return;

//		if (recentLocation == null || tvLocation.getText().toString().isEmpty()
//				|| location.distanceTo(recentLocation) > 1000)
//			setLocality(location);
		setLocality(location);
		
		if (location.hasSpeed()) {
    		// take speed from location's speed attribute
    		speed = (int) (location.getSpeed() * 3.6);
		} else {
			// calculate speed from distance traveled in time (s=d/t)
			if (recentLocation != null) {
				int localSpeed = (int) ((recentLocation.distanceTo(location) * 1000 / (location
						.getTime() - recentLocation.getTime())) * 3.6);
				if (Math.abs(localSpeed - speed) <= 111) {
					// accept only normal speed changes, discard abnormal acceleration
					speed = localSpeed;
				}
			} else
				speed = 0;
		}		
		
		if (speed > 0) {
			// user is moving
			assignSpeed();
			if (speed <= MainActivity.safeSpeedLimit) {
				blnSafeMode = false;
				rLaySafeModeOuterRing.setVisibility(View.GONE);
				makeProfileNormal();
				DontlistenForIncomingCalls();
			} else {
				// user crossed safe speed
				rLaySafeModeOuterRing.setVisibility(View.VISIBLE);
				if (!blnSafeModeOverridden) {
					blnSafeMode = true;
					if (MainActivity.profileToBeSelected
							.equalsIgnoreCase(MainActivity.PROFILE_SILENT)) {
						makeProfileSilent();
						listenForIncomingCalls();
					}
				} else {
					if (blnSafeMode) {
						blnSafeMode = false;
						makeProfileNormal();
						DontlistenForIncomingCalls();
					}
				}
			}
		} else {
			// user is not moving
			idleCount++;
			if (probablityOfUserMoving() < 0.5) {
				// go to hibernate mode
				hibernateMode();
			}
		}
		
		recentLocation = location;
	}

	private void hibernateMode() {
		dontTrackSpeed();
		if (hibernateMultiplier++ >= 10)
			hibernateMultiplier = 1;
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				// Do something after every 1 minute = 60 sec
				trackSpeed();
			}
		}, 60000 * hibernateMultiplier);
	}

	private void setLocality(Location location) {
		Geocoder geocoder;
		List<Address> addresses;
		geocoder = new Geocoder(getActivity().getApplicationContext(),
				Locale.getDefault());
		try {
			addresses = geocoder.getFromLocation(location.getLatitude(),
					location.getLongitude(), 1);
			if (!addresses.isEmpty()) {
				tvLocation.setText(addresses.get(0).getSubLocality());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getLocality(Location location) {
		String city, locality;
		Geocoder geocoder;
		List<Address> addresses;
		geocoder = new Geocoder(getActivity().getApplicationContext(),
				Locale.getDefault());
		try {
			addresses = geocoder.getFromLocation(location.getLatitude(),
					location.getLongitude(), 1);
			if (!addresses.isEmpty()) {
				city = addresses.get(0).getLocality();
				locality = addresses.get(0).getSubLocality();
				if (locality != "" && locality != null && city != ""
						&& city != null)
					return locality + ", " + city;
				else if (locality == "" || locality == null) {
					if (city != "" && city != null) {
						return city;
					}
				} else {
					return locality;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private double probablityOfUserMoving() {
		double userMoving = 0.0;

		// Check whether connected to car dock
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
		Intent dockStatus = getActivity().getApplicationContext()
				.registerReceiver(null, ifilter);
		int dockState = (dockStatus == null ? Intent.EXTRA_DOCK_STATE_UNDOCKED
				: dockStatus.getIntExtra(Intent.EXTRA_DOCK_STATE, -1));
		boolean isDocked = dockState != Intent.EXTRA_DOCK_STATE_UNDOCKED;
		boolean isCar = dockState == Intent.EXTRA_DOCK_STATE_CAR;
		if (isDocked && isCar)
			userMoving = 1;
		else
			userMoving = 0.0;

		// Check whether moving through accelerometer

		userMoving = userMoving - (idleCount * 0.1);
		return userMoving;

	}

	private float getBatteryLevel() {
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = getActivity().registerReceiver(null, ifilter);
		int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		// Error checking that probably isn't needed but I added just in case.
		if (level == -1 || scale == -1) {
			return 50.0f;
		}

		return ((float) level / (float) scale) * 100.0f;
	}

	@Override
	public void onDestroy() {
		dontTrackSpeed();
		super.onDestroy();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (ringerModeChangeReceiver.isOrderedBroadcast()) {
			getActivity().unregisterReceiver(ringerModeChangeReceiver);
		}
		if (!blnSafeMode) {
			// blnDemoMode = false;
			dontTrackSpeed();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!ringerModeChangeReceiver.isOrderedBroadcast()) {
			IntentFilter intentFilterRingerMode = new IntentFilter(
					AudioManager.RINGER_MODE_CHANGED_ACTION);
			getActivity().registerReceiver(ringerModeChangeReceiver,
					intentFilterRingerMode);
		}
		recentLogCheck(PreferenceManager.getDefaultSharedPreferences(
				getActivity().getApplicationContext()).getInt(
				MainActivity.PREF_NEW_LOGS_COUNTER, 0));
		// initialize();
		if (!blnSafeMode)
			trackSpeed();
	}

	@Override
	public void onStop() {
		super.onStop();
		blnDemoMode = false;
		dontTrackSpeed();
	}

	@Override
	public void onStart() {
		super.onStart();
		trackSpeed();
	}

	private void assignSpeed() {
		int digit3 = speed % 10;
		int digit2 = (speed / 10) % 10;
		int digit1 = (speed / 100) % 10;
		
		tvSpeedDigit1.setText(String.valueOf(digit1));
		tvSpeedDigit2.setText(String.valueOf(digit2));
		tvSpeedDigit3.setText(String.valueOf(digit3));

		setSpeedDigitsColor(digit1, digit2, digit3);
	}

	private void setSpeedDigitsColor(int digit1, int digit2, int digit3) {
		int speedDigitColor, zeroDigitColor;
		if (blnSafeMode && !blnSafeModeOverridden) {
			zeroDigitColor = getActivity().getResources().getColor(
					R.color.theme_red_inactive);
			speedDigitColor = getActivity().getResources().getColor(
					R.color.theme_red);
		} else {
			zeroDigitColor = getActivity().getResources().getColor(
					R.color.theme_blue_inactive);
			speedDigitColor = getActivity().getResources().getColor(
					R.color.theme_blue);
		}

		if (digit1 == 0) {
			tvSpeedDigit1.setShadowLayer(3, 6, 6, zeroDigitColor);
			tvSpeedDigit1.setTextColor(zeroDigitColor);
		} else {
			tvSpeedDigit1.setShadowLayer(3, 6, 6, speedDigitColor);
			tvSpeedDigit1.setTextColor(speedDigitColor);
		}

		if (digit1 == 0 && digit2 == 0) {
			tvSpeedDigit2.setShadowLayer(3, 6, 6, zeroDigitColor);
			tvSpeedDigit2.setTextColor(zeroDigitColor);
		} else {
			tvSpeedDigit2.setShadowLayer(3, 6, 6, speedDigitColor);
			tvSpeedDigit2.setTextColor(speedDigitColor);
		}

		if (digit1 == 0 && digit2 == 0 && digit3 == 0) {
			tvSpeedDigit3.setShadowLayer(3, 6, 6, zeroDigitColor);
			tvSpeedDigit3.setTextColor(zeroDigitColor);
		} else {
			tvSpeedDigit3.setShadowLayer(3, 6, 6, speedDigitColor);
			tvSpeedDigit3.setTextColor(speedDigitColor);
		}
	}

	protected void testingSpeed() {
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				// Do something after delay milliseconds
				if (!blnDemoMode) {
					initialize();
					return;
				}

				webVSignal.setVisibility(View.VISIBLE);
				imgVPageIcon.setVisibility(View.GONE);
				tvStatus.setText(getActivity().getResources().getString(R.string.active));

				Random r = new Random();
				int random = r.nextInt(13);
				if ((speed - random) < (random * 4.33))
					blnMaxSpeedReached = false;

				if ((speed + random) > ((random + 1) * 15.38))
					blnMaxSpeedReached = true;

				if (!blnMaxSpeedReached)
					speed = speed + random;
				else
					speed = speed - random;

				if (speed > 200)
					speed = 200;
				
				assignSpeed();
				
				if (speed <= MainActivity.safeSpeedLimit) {
					blnSafeMode = false;
					rLaySafeModeOuterRing.setVisibility(View.GONE);
				} else {
					rLaySafeModeOuterRing.setVisibility(View.VISIBLE);
					if (!blnSafeModeOverridden) {
						blnSafeMode = true;
						if (MainActivity.profileToBeSelected
								.equalsIgnoreCase(MainActivity.PROFILE_SILENT)) {
							makeProfileSilent();
							listenForIncomingCalls();
						}
					} else {
						if (blnSafeMode) {
							blnSafeMode = false;
							makeProfileNormal();
							DontlistenForIncomingCalls();
						}
					}
				}		

				// again call same function recursively to generate random
				// speeds for demo purpose.
				testingSpeed();
			}
		}, 500);
	}

	private void makeProfileSilent() {
		String profileStatus = getCurrentProfile();
		if (profileStatus.equalsIgnoreCase("silent"))
			return;

		AudioManager mAudioManager = (AudioManager) getActivity()
				.getSystemService(Context.AUDIO_SERVICE);
		mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
		mAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
	}

	private void makeProfileNormal() {
		String profileStatus = getCurrentProfile();
		if (profileStatus.equalsIgnoreCase("normal"))
			return;

		AudioManager mAudioManager = (AudioManager) getActivity()
				.getSystemService(Context.AUDIO_SERVICE);
		mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
	}

	private String getCurrentProfile() {
		AudioManager mAudioManager = (AudioManager) getActivity()
				.getSystemService(Context.AUDIO_SERVICE);
		int ringermode = mAudioManager.getRingerMode();
		if (ringermode == AudioManager.RINGER_MODE_SILENT
				|| ringermode == AudioManager.RINGER_MODE_VIBRATE)
			return "silent";
		else
			return "normal";
	}

	private void listenForIncomingCalls() {
		if (!ringerModeChangeReceiver.isOrderedBroadcast()) {
			IntentFilter intentFilterRingerMode = new IntentFilter(
					AudioManager.RINGER_MODE_CHANGED_ACTION);
			getActivity().registerReceiver(ringerModeChangeReceiver,
					intentFilterRingerMode);
		}

		incomingCallManager = (TelephonyManager) getActivity()
				.getSystemService(Context.TELEPHONY_SERVICE);
		incomingCallListener = new PhoneStateListener() {
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
				profileStatus = getCurrentProfile();
				switch (state) {
				case TelephonyManager.CALL_STATE_RINGING:
					if (profileStatus.equalsIgnoreCase("normal")) {
						DontlistenForIncomingCalls();
						break;
					}
					// called when someone is ringing to this phone
					Class c = null;
					try {
						c = Class.forName(incomingCallManager.getClass()
								.getName());
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
					Method m = null;
					try {
						m = c.getDeclaredMethod("getITelephony");
					} catch (SecurityException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					}
					m.setAccessible(true);
					ITelephony telephonyService = null;
					try {
						telephonyService = (ITelephony) m
								.invoke(incomingCallManager);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
					// Bundle bundle = intent.getExtras();
					telephonyService.endCall();

					String newLogStatement = getActivity().getResources().getString(
							R.string.missed_call);
					String name = getContactName(incomingNumber);
					if (!name.equalsIgnoreCase(""))
						newLogStatement = name + " (" + incomingNumber + ") "
								+ newLogStatement;
					else
						newLogStatement = incomingNumber + " "
								+ newLogStatement;

					boolean callAlreadyRejected = false;
					GregorianCalendar date = new GregorianCalendar();
					int currentTime = date.get(Calendar.HOUR) * 100
							+ date.get(Calendar.MINUTE);
					if (lastIncomingNumber.equalsIgnoreCase(incomingNumber)
							&& (currentTime - lastIncomingCallTime) < 9)
						callAlreadyRejected = true;
					else {
						callAlreadyRejected = false;
						lastIncomingCallTime = currentTime;
						lastIncomingNumber = incomingNumber;
					}

					if (MainActivity.safeModeSendMsg && !callAlreadyRejected)
						newLogStatement = sendSMS(incomingNumber, name,
								newLogStatement);

					if (!callAlreadyRejected) {
						// log only once for a call from a number within 9
						// minutes
						int declinedCallsCount = PreferenceManager
								.getDefaultSharedPreferences(
										getActivity().getApplicationContext())
								.getInt(MainActivity.PREF_DECLINED_CALLS_COUNTER,
										0);
						declinedCallsCount++;
						PreferenceManager
								.getDefaultSharedPreferences(
										getActivity().getApplicationContext())
								.edit()
								.putInt(MainActivity.PREF_DECLINED_CALLS_COUNTER,
										declinedCallsCount).commit();

						displayNotification(003, newLogStatement);
						logIncident(newLogStatement);
					}

					break;
				}
			}
		};

		incomingCallManager.listen(incomingCallListener,
				PhoneStateListener.LISTEN_CALL_STATE);
	}

	private void DontlistenForIncomingCalls() {
		if (ringerModeChangeReceiver.isOrderedBroadcast())
			getActivity().unregisterReceiver(ringerModeChangeReceiver);

		if (incomingCallManager != null && incomingCallListener != null)
			incomingCallManager.listen(incomingCallListener,
					PhoneStateListener.LISTEN_NONE);

		incomingCallManager = null;
		incomingCallListener = null;
	}

	private String getContactName(String incomingNumber) {
		try {
			String name = "";
			// define the columns I want the query to return
			String[] projection = new String[] {
					ContactsContract.PhoneLookup.DISPLAY_NAME,
					ContactsContract.PhoneLookup._ID };
			// encode the phone number and build the filter URI
			Uri contactUri = Uri.withAppendedPath(
					ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
					Uri.encode(incomingNumber));
			// query
			Cursor cursor = getActivity().getContentResolver().query(
					contactUri, projection, null, null, null);
			if (cursor.moveToFirst())
				name = cursor
						.getString(cursor
								.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
			cursor.close();
			return name;
		} catch (Exception e) {
			return "";
		}
	}

	private String sendSMS(String incomingNumber, String name,
			String newLogStatement) {
		String messageText = MainActivity.safeModeMsg;
		try {
			int i = -1;
			i = MainActivity.safeModeMsg.indexOf("Hi");
			if (i == -1)
				i = MainActivity.safeModeMsg.indexOf("Hey");

			if (i != -1 && !name.equalsIgnoreCase(""))
				messageText = MainActivity.safeModeMsg.substring(0, i + 2)
						+ " "
						+ name
						+ " "
						+ MainActivity.safeModeMsg.substring(i + 3,
								MainActivity.safeModeMsg.length());

			messageText = messageText
					+ getActivity().getString(R.string.msg_suffix);

		} catch (Exception e) {
			e.printStackTrace();
			return newLogStatement;
		}
		try {
			SmsManager sms = SmsManager.getDefault();
			sms.sendTextMessage(incomingNumber, null, messageText, null, null);

			int repliedTextsCount = PreferenceManager
					.getDefaultSharedPreferences(
							getActivity().getApplicationContext()).getInt(
							MainActivity.PREF_REPLIED_TEXTS_COUNTER, 0);
			repliedTextsCount++;
			PreferenceManager
					.getDefaultSharedPreferences(
							getActivity().getApplicationContext())
					.edit()
					.putInt(MainActivity.PREF_REPLIED_TEXTS_COUNTER,
							repliedTextsCount).commit();

			ContentValues values = new ContentValues();
			values.put("address", incomingNumber);
			values.put("body", messageText);
			getActivity().getContentResolver().insert(
					Uri.parse("content://sms/sent"), values);
		} catch (Exception e) {
			e.printStackTrace();
			return newLogStatement;
		}

		return newLogStatement + getString(R.string.missed_call_but_replied);
	}

	private void logIncident(String newLogStatement) {
		GregorianCalendar date = new GregorianCalendar();
		newLogStatement = String.valueOf(date.get(Calendar.DAY_OF_MONTH)) + "/"
				+ String.valueOf(date.get(Calendar.MONTH) + 1) + "/"
				+ String.valueOf(date.get(Calendar.YEAR)) + " "
				+ String.valueOf(date.get(Calendar.HOUR_OF_DAY)) + ":"
				+ String.valueOf(date.get(Calendar.MINUTE)) + ":"
				+ String.valueOf(date.get(Calendar.SECOND)) + " - "
				+ newLogStatement;

		String locality = getLocality(recentLocation);
		if (locality != null)
			newLogStatement = newLogStatement + " @ " + locality;

		newLogStatement = newLogStatement
				+ System.getProperty("line.separator");

		BufferedWriter writer = null;
		try {
			FileOutputStream openFileOutput = getActivity().openFileOutput(
					MainActivity.LOGS_FILE, Context.MODE_APPEND);
			openFileOutput.write(newLogStatement.getBytes());
			int recentLogsCount = PreferenceManager
					.getDefaultSharedPreferences(
							getActivity().getApplicationContext()).getInt(
							MainActivity.PREF_NEW_LOGS_COUNTER, 0);
			recentLogsCount++;
			PreferenceManager
					.getDefaultSharedPreferences(
							getActivity().getApplicationContext())
					.edit()
					.putInt(MainActivity.PREF_NEW_LOGS_COUNTER, recentLogsCount)
					.commit();
			recentLogCheck(recentLogsCount);
		} catch (Exception e) {
			// throw new RuntimeException(e);
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}
	}

	private void recentLogCheck(int recentLogsCount) {
		TextView tvRecentLogsCount = (TextView) view
				.findViewById(R.id.tvRecentLogsCount);
		if (recentLogsCount > 0) {
			tvRecentLogsCount.setText(String.valueOf(recentLogsCount));
			tvRecentLogsCount.setVisibility(View.VISIBLE);
		} else {
			tvRecentLogsCount.setText("0");
			tvRecentLogsCount.setVisibility(View.GONE);
		}
	}

	private void displayNotification(int notificationId, String message) {
		String contentInfo = "";

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				getActivity()).setSmallIcon(R.drawable.ic_notification)
				// notification icon
				.setContentTitle("iDriveSafe")
				// title for notification
				.setContentText(message)
				// message for notification
				.setContentInfo(contentInfo)
				.setAutoCancel(true)
				// clear notification after click
				.setStyle(
						new NotificationCompat.BigTextStyle().bigText(message));

		final Intent notificationIntent = new Intent(getActivity(),
				MainActivity.class);
		notificationIntent.setAction(Intent.ACTION_MAIN);
		notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		PendingIntent pi = PendingIntent.getActivity(getActivity(), 0,
				notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(pi);
		NotificationManager mNotificationManager = (NotificationManager) getActivity()
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(notificationId, mBuilder.build());
	}

	private void changeFonts() {
		Typeface myTypeface2 = Typeface.createFromAsset(getActivity()
				.getAssets(), "fonts/quarthin.ttf");
		Typeface myTypeface = Typeface.createFromAsset(getActivity()
				.getAssets(), "fonts/KoshgarianRegular.ttf");
		tvStatus.setTypeface(myTypeface);
		tvSpeedDigit1.setTypeface(myTypeface2);
		tvSpeedDigit2.setTypeface(myTypeface2);
		tvSpeedDigit3.setTypeface(myTypeface2);
		tvLocation.setTypeface(myTypeface);
		tvSafeMode.setTypeface(myTypeface);
		tvDemo.setTypeface(myTypeface);
		TextView tvLogs = (TextView) view.findViewById(R.id.tvLogs);
		tvLogs.setTypeface(myTypeface);
	}
}
