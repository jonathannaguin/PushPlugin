package com.plugin.gcm;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMConstants;

@SuppressLint("NewApi")
public class GCMIntentService extends GCMBaseIntentService {

	public static final int NOTIFICATION_ID = 237;
	private static final String TAG = "GCMIntentService";
	private static final String FIELD_MESSAGE = "alert";
	private static final String FIELD_COUNT = "msgcnt";
	private static final String FIELD_SOUND = "sound";

	public GCMIntentService() {
		super("GCMIntentService");
	}

	@Override
	public void onRegistered(Context context, String regId) {

		Log.v(TAG, "onRegistered: " + regId);

		JSONObject json;

		try {
			json = new JSONObject().put("event", "registered");
			json.put("regid", regId);

			// Send this JSON data to the JavaScript application above EVENT
			// should be set to the msg type
			// In this case this is the registration ID
			PushPlugin.sendJavascript(json);

		} catch (JSONException e) {
			// No message to the user is sent, JSON failed
			Log.e(TAG, "onRegistered: JSON exception");
		}
	}

	@Override
	public void onUnregistered(Context context, String regId) {
		Log.d(TAG, "onUnregistered - regId: " + regId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {

		// Extract the payload from the message
		Bundle extras = intent.getExtras();
		if (extras != null)
		{
			PushPlugin.sendExtras(extras);

			// Send a notification if there is a message
			if (extras.containsKey(FIELD_MESSAGE) && extras.getString(FIELD_MESSAGE).length() != 0) {
				createNotification(context, extras);
			}
		}
	}

	public void createNotification(Context context, Bundle extras) {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		String appName = getAppName(this);

		Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("pushBundle", extras);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		NotificationCompat.Builder mBuilder =
			new NotificationCompat.Builder(context)
				.setSmallIcon(context.getApplicationInfo().icon)
				.setWhen(System.currentTimeMillis())
				.setContentTitle(appName)
				.setTicker(appName)
				.setContentIntent(contentIntent);
		
		if (extras.containsKey(FIELD_SOUND) && extras.getString(FIELD_SOUND).length() > 0){
			Uri uri = Uri.parse("android.resource://" + getPackageName() +  "/raw/"+ removeExtension(extras.getString(FIELD_SOUND)));
			mBuilder.setSound(uri);			
		}

		String message = extras.getString(FIELD_MESSAGE);
		if (message != null) {
			mBuilder.setContentText(message);
		} else {
			mBuilder.setContentText("<missing message content>");
		}

		String msgcnt = extras.getString(FIELD_COUNT);
		if (msgcnt != null) {
			mBuilder.setNumber(Integer.parseInt(msgcnt));
		}
		
		mNotificationManager.notify((String) appName, NOTIFICATION_ID, mBuilder.build());
	}
	
	public static void cancelNotification(Context context)
	{
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel((String)getAppName(context), NOTIFICATION_ID);	
	}
	
	private static String getAppName(Context context)
	{
		CharSequence appName = 
				context
					.getPackageManager()
					.getApplicationLabel(context.getApplicationInfo());
		
		return (String)appName;
	}
	
	private static String removeExtension(String s) {

	    String separator = System.getProperty("file.separator");
	    String filename;

	    // Remove the path upto the filename.
	    int lastSeparatorIndex = s.lastIndexOf(separator);
	    if (lastSeparatorIndex == -1) {
	        filename = s;
	    } else {
	        filename = s.substring(lastSeparatorIndex + 1);
	    }

	    // Remove the extension.
	    int extensionIndex = filename.lastIndexOf(".");
	    if (extensionIndex == -1)
	        return filename;

	    return filename.substring(0, extensionIndex);
	}
	
	@Override
	public void onError(Context context, String errorId) {
		Log.e(TAG, "onError - errorId: " + errorId);

		if (errorId == GCMConstants.ERROR_PHONE_REGISTRATION_ERROR) {

			// We simulate a successful operation with an empty registration ID

			JSONObject json = null;
			try {
				json = new JSONObject().put("event", "registered");
				PushPlugin.sendJavascript(json);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

}
