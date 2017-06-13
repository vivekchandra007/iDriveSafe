package mobile.android.idrivesafe2.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import mobile.android.idrivesafe2.service.*;

public class StartBackServiceAtBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, BackgroundLocationService.class);
            context.startService(serviceIntent);
        }
    }
}