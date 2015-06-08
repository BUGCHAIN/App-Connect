package app.bugchain.appconnect;

import android.os.Bundle;
import android.util.Log;

import com.facebook.FacebookBroadcastReceiver;

/**
 * Created by POSEIDON on 12/5/2558.
 */
public class AppConnectBroadcastRecevier extends FacebookBroadcastReceiver {

    @Override
    protected void onSuccessfulAppCall(String appCallId, String action, Bundle extras) {
        //super.onSuccessfulAppCall(appCallId, action, extras);
        Log.d("AppConnect",String.format("Photo uploaded by call " + appCallId + " succeeded"));
    }

    @Override
    protected void onFailedAppCall(String appCallId, String action, Bundle extras) {
        //super.onFailedAppCall(appCallId, action, extras);
        Log.d("AppConnect",String.format("Photo uploaded by call " + appCallId + " failed."));
    }
}
