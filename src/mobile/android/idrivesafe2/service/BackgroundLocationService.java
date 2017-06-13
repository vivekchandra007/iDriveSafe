package mobile.android.idrivesafe2.service;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import com.android.internal.telephony.ITelephony;
import mobile.android.idrivesafe2.*;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;

/**
 * BackgroundLocationService used for determining user location & speed in background and helping user drive safe
*/
public class BackgroundLocationService extends Service {
		
	static final String LOGS_FILE = "iDriveSafeLogs";
	static final int LOW_BATTERY_LEVEL = 9;
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
	static String lastIncomingNumber;
	static int lastIncomingCallTime;
	
	IBinder mBinder = new LocalBinder();
	
	int speed;
	private Location recentLocation;
	private int hibernateMultiplier;
	private Boolean stopUpdates;
	private int idleCount;
	private Boolean lowBattery;
	private String newLogStatement;
	
	private LocationManager locationManager;
	private LocationListener locationListener;	
	private TelephonyManager incomingCallManager;
	private PhoneStateListener  incomingCallListener;
	private String profileStatus;
	private BroadcastReceiver ringerModeChangeReceiver;
	
    public class LocalBinder extends Binder {
    	public BackgroundLocationService getServerInstance() {
    		return BackgroundLocationService.this;
    	}
    }
    
    @Override
	public void onCreate() {
        super.onCreate();
        initialize();		
        //displayNotification(001, "Tracker bot created.");
    }
    
    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        final Handler handler = new Handler();
		handler.post(new Runnable() {
		  @Override
		  public void run() {
			  setupLocationParameters();
		      trackSpeed();
		  }
		});
        
        return START_STICKY;
    }
    
    private void initialize() {
    	retrievePreferences();
    	speed = 0;
		hibernateMultiplier = 1;
		lowBattery = false;
		
		GregorianCalendar date = new GregorianCalendar();
		lastIncomingCallTime = date.get(Calendar.HOUR) * 100 + date.get(Calendar.MINUTE);
		lastIncomingNumber = "";
		profileStatus = getCurrentProfile();
		ringerModeChangeReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
            	profileStatus = getCurrentProfile();
            	if (profileStatus.equalsIgnoreCase(PROFILE_GENERAL))
            		DontlistenForIncomingCalls();
            }
        };        
	}
    
    private void trackSpeed() {
		if (getBatteryLevel() > LOW_BATTERY_LEVEL)
			lowBattery = false;
		else 
			lowBattery = true;
		
		if(!lowBattery) {
			//displayNotification(001, getResources().getString(R.string.active));
    		stopUpdates = false;
    		idleCount = 0;
			// Register the listener with the Location Manager to receive location updates
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 100, locationListener);
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 100, locationListener);
		} else {
			//low battery, so halt all location updates and save battery
			hibernateMode();
			//displayNotification(001, getResources().getString(R.string.low_battery));
		}
	}
    
    private void dontTrackSpeed() {
    	speed = 0;
		locationManager.removeUpdates(locationListener);
		//displayNotification(001, getResources().getString(R.string.inactive));
		stopUpdates= true;
		
		makeProfileNormal();
		DontlistenForIncomingCalls();
	}
    
    private void setSpeed(Location location) {  
    	if (stopUpdates)
    		return;
    	
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
			// displayNotification(001, Integer.toString(speed) + " km/h");
			if (speed <= safeSpeedLimit) {
				// blnSafeMode = false;
				makeProfileNormal();
				DontlistenForIncomingCalls();
			} else {
				// user crossed safe speed
				// blnSafeMode = true;
				if (profileToBeSelected.equalsIgnoreCase(PROFILE_SILENT)) {
					makeProfileSilent();
					listenForIncomingCalls();
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
			  //Do something after 2X minutes
			  setupLocationParameters();
			  trackSpeed();
		  }
		}, 120000*hibernateMultiplier);
	}
    
	private void setupLocationParameters() {
		// Acquire a reference to the system Location Manager
		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);
		recentLocation = locationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (recentLocation == null)
			recentLocation = locationManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (recentLocation != null && recentLocation.hasSpeed()) {
			speed = (int) (recentLocation.getSpeed() * 3.6);
			//displayNotification(001, Integer.toString(speed) + " km/h");
		}

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
	}
    
    private void displayNotification(int notificationId, String message) {
    	String contentInfo = "";
    	if (recentLocation != null) {
    		String locality = getLocality(recentLocation);
    		if (!lowBattery && locality != null && notificationId != 003)
        		message =message + " at " + locality;
    	}   	
		
		NotificationCompat.Builder mBuilder =   new NotificationCompat.Builder(this)
        	.setSmallIcon(R.drawable.ic_notification) // notification icon
        	.setContentTitle("iDriveSafe") // title for notification
        	.setContentText(message) // message for notification
        	.setContentInfo(contentInfo)
        	.setAutoCancel(true) // clear notification after click
        	.setStyle(new NotificationCompat.BigTextStyle().bigText(message)); 
		
		final Intent notificationIntent = new Intent(this, MainActivity.class);
		notificationIntent.setAction(Intent.ACTION_MAIN);
		notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		PendingIntent pi = PendingIntent.getActivity(this, 0, notificationIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(pi);
		NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(notificationId, mBuilder.build());
    }
    
    private double probablityOfUserMoving() {
    	double userMoving = 0.0;
    	
    	//Check whether connected to car dock
    	IntentFilter ifilter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
    	Intent dockStatus = getApplicationContext().registerReceiver(null, ifilter);
    	int dockState = (dockStatus == null ?
                Intent.EXTRA_DOCK_STATE_UNDOCKED :
                dockStatus.getIntExtra(Intent.EXTRA_DOCK_STATE, -1));
        boolean isDocked = dockState != Intent.EXTRA_DOCK_STATE_UNDOCKED;
        boolean isCar = dockState == Intent.EXTRA_DOCK_STATE_CAR;
        if (isDocked && isCar)
        	userMoving = 0.75;
        else
        	userMoving = 0.0;
        
        //Check whether moving through accelerometer   
        
        userMoving = userMoving - (idleCount * 0.1);
        return userMoving;
        	
    }
    
    private String getLocality(Location location) {
		String city, locality;
    	Geocoder geocoder;
		List<Address> addresses;
		geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
		try {
			addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
			if (!addresses.isEmpty()) {
				city = addresses.get(0).getLocality();
	    		locality = addresses.get(0).getSubLocality();
	    		if (locality != "" && locality != null && city != "" && city != null)
	    			return locality + ", " + city;
	    		else if (locality == "" || locality == null) {
	    			if (city != "" && city != null) {
	    				return city;
	    			}
	    		}
	    		else {
	    			return locality;
	    		}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
    
    private float getBatteryLevel() {
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = registerReceiver(null, ifilter);
	    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
	    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

	    // Error checking that probably isn't needed but I added just in case.
	    if(level == -1 || scale == -1) {
	        return 50.0f;
	    }

	    return ((float)level / (float)scale) * 100.0f; 
	}
    
    private void makeProfileSilent() {
		String profileStatus = getCurrentProfile();
		if (profileStatus.equalsIgnoreCase("silent"))
			return;
		
		AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
		mAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
	}
	
	private void makeProfileNormal() {
		String profileStatus = getCurrentProfile();
		if (profileStatus.equalsIgnoreCase("normal"))
			return;
		
		AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);		
	}
		
	private String getCurrentProfile() {
		AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		int ringermode = mAudioManager.getRingerMode();
        if (ringermode == AudioManager.RINGER_MODE_SILENT || ringermode == AudioManager.RINGER_MODE_VIBRATE)
            return "silent";
        else
        	return "normal";
	}
	
	private void listenForIncomingCalls() {
		if (!ringerModeChangeReceiver.isOrderedBroadcast()) {
			IntentFilter intentFilterRingerMode = new IntentFilter(
					AudioManager.RINGER_MODE_CHANGED_ACTION);
			registerReceiver(ringerModeChangeReceiver, intentFilterRingerMode);
		}
		
		incomingCallManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);		
		incomingCallListener = new PhoneStateListener () {
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
	                	Class c=null;
	                	try {
	                		c = Class.forName(incomingCallManager.getClass().getName());
	                	} catch (ClassNotFoundException e) {
	                		e.printStackTrace();
	                	}
	                	Method m=null;
	                	try {
	                		m = c.getDeclaredMethod("getITelephony");
	                	} catch (SecurityException e) {
	                		e.printStackTrace();
	                	} catch (NoSuchMethodException e) {
	                		e.printStackTrace();
	                	}
	                		m.setAccessible(true);
	                		ITelephony telephonyService=null;
	                		try {
	                			telephonyService = (ITelephony) m.invoke(incomingCallManager);
	                		} catch (IllegalArgumentException e) {
	                			e.printStackTrace();
	                		} catch (IllegalAccessException e) {
	                			e.printStackTrace();
	                		} catch (InvocationTargetException e) {
	                			e.printStackTrace();
	                		}
	                		//Bundle bundle = intent.getExtras();
	                		telephonyService.endCall();
	                		
	                			                		
	                		newLogStatement = getResources().getString(R.string.missed_call);
	                		String name = getContactName(incomingNumber);
	                		if (!name.equalsIgnoreCase(""))
	                			newLogStatement = name + " (" + incomingNumber + ") " + newLogStatement;
	                		else
	                			newLogStatement = incomingNumber + " " + newLogStatement;
	                		
	                		boolean callAlreadyRejected = false;
	                		GregorianCalendar date = new GregorianCalendar();
	                		int currentTime = date.get(Calendar.HOUR) * 100 + date.get(Calendar.MINUTE);
	                		if(lastIncomingNumber.equalsIgnoreCase(incomingNumber) && (currentTime - lastIncomingCallTime) < 9)
	                			callAlreadyRejected = true;
	                		else {
	                			callAlreadyRejected = false;
	                			lastIncomingCallTime = currentTime;
	                			lastIncomingNumber = incomingNumber;
	                		}
	                		
	                		if (safeModeSendMsg && !callAlreadyRejected)
	                			newLogStatement = sendSMS(incomingNumber, name, newLogStatement);
	                		
	                		if (!callAlreadyRejected) {
	                			// log only once for a call from a number within 9 minutes
	                			int declinedCallsCount = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(PREF_DECLINED_CALLS_COUNTER, 0);
		            			declinedCallsCount++;
		            			PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putInt(PREF_DECLINED_CALLS_COUNTER, declinedCallsCount).commit();
		            			
	                			displayNotification(003, newLogStatement);
	                			logIncident(newLogStatement);
	                		}
	                		
	                		break;
	            }
	        }	        
	    };
	    
	    incomingCallManager.listen(incomingCallListener, PhoneStateListener.LISTEN_CALL_STATE);
	}
	
	private void DontlistenForIncomingCalls() {
		if (ringerModeChangeReceiver.isOrderedBroadcast()) {
			unregisterReceiver(ringerModeChangeReceiver);
		}
		if (incomingCallManager != null && incomingCallListener != null )
			incomingCallManager.listen(incomingCallListener, PhoneStateListener.LISTEN_NONE);
		
	    incomingCallManager = null;
	    incomingCallListener = null;
	}
	
	private String sendSMS(String incomingNumber, String name, String newLogStatement) {
		String messageText = safeModeMsg;
		try {
			int i = -1;
			i = safeModeMsg.indexOf("Hi");
			if(i==-1)
				i = safeModeMsg.indexOf("Hey");
			
			if(i != -1 && !name.equalsIgnoreCase(""))
				messageText = safeModeMsg.substring(0, i+2) + " " + name + " " + safeModeMsg.substring(i+3, safeModeMsg.length());
			
			messageText = messageText + getString(R.string.msg_suffix);
			
		} catch (Exception e) {
			e.printStackTrace();
			return newLogStatement;
		}
		
		try {
			SmsManager sms = SmsManager.getDefault();
			sms.sendTextMessage(incomingNumber, null, messageText, null, null);		
			
			int repliedTextsCount = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(PREF_REPLIED_TEXTS_COUNTER, 0);
			repliedTextsCount++;
			PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putInt(PREF_REPLIED_TEXTS_COUNTER, repliedTextsCount).commit();
						
			ContentValues values = new ContentValues();             
		    values.put("address", incomingNumber); 		              
		    values.put("body", messageText); 		              
		    getContentResolver().insert(Uri.parse("content://sms/sent"), values);
		} catch (Exception e) {
			e.printStackTrace();
			return newLogStatement;
		}
		
		return newLogStatement + " " + getString(R.string.missed_call_but_replied);
	}
	
	private String getContactName(String incomingNumber) {
		try {
			// define the columns I want the query to return
			String name = "";
			String[] projection = new String[] {
					ContactsContract.PhoneLookup.DISPLAY_NAME,
					ContactsContract.PhoneLookup._ID };
			// encode the phone number and build the filter URI
			Uri contactUri = Uri.withAppendedPath(
					ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
					Uri.encode(incomingNumber));
			// query
			Cursor cursor = getContentResolver().query(contactUri, projection,
					null, null, null);
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
    	
    	newLogStatement = newLogStatement + System.getProperty("line.separator");
    	
		BufferedWriter writer = null;
		try {
			FileOutputStream openFileOutput = openFileOutput(LOGS_FILE,
					Context.MODE_APPEND);
			openFileOutput.write(newLogStatement.getBytes());
			int recentLogsCount = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(PREF_NEW_LOGS_COUNTER, 0);
			recentLogsCount++;
			PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putInt(PREF_NEW_LOGS_COUNTER, recentLogsCount).commit();
		} catch (Exception e) {
			//throw new RuntimeException(e);
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
	
	private void retrievePreferences() {
		// Set defaults
		safeSpeedLimit = DEFAULT_SAFE_SPEED_LIMIT;
		profileToBeSelected = PROFILE_SILENT;
		safeModeSendMsg = true;
		safeModeMsg = getString(R.string.pref_auto_message_text);

		if (PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getString(
						PREF_SPEED_LIMIT, null) != null)
			safeSpeedLimit = Integer.parseInt(PreferenceManager
					.getDefaultSharedPreferences(
							getApplicationContext()).getString(
									PREF_SPEED_LIMIT, null));

		if (PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getString(
						PREF_PROFILE_SELECTED, null) != null)
			profileToBeSelected = PreferenceManager
					.getDefaultSharedPreferences(
							getApplicationContext()).getString(
									PREF_PROFILE_SELECTED, null);

		if (PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getString(PREF_SEND_MSG,
				null) != null) {
			if (PreferenceManager
					.getDefaultSharedPreferences(
							getApplicationContext())
					.getString(PREF_SEND_MSG, null).equalsIgnoreCase("true"))
				safeModeSendMsg = true;
			else
				safeModeSendMsg = false;
		}

		if (PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext())
				.getString(PREF_MSG, null) != null)
			safeModeMsg = PreferenceManager.getDefaultSharedPreferences(
					getApplicationContext()).getString(PREF_MSG,
					null);
	}    
    
    @Override
    public IBinder onBind(Intent intent) {
    	//return mBinder;
    	return null;
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	dontTrackSpeed();
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    	notificationManager.cancel(001);
        locationListener = null;
        locationManager = null;
    }
}