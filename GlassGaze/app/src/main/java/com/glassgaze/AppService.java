package com.glassgaze;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

public class AppService extends Service {

    private static final String TAG = "AppService";
    private static final String LIVE_CARD_ID = "HelloGlass";
    private AppDrawer mCallback;
    private LiveCard mLiveCard;

    public class LocalBinder extends Binder {
        public AppService getService() {
            return AppService.this;
        }
    }
    private final IBinder mBinder = new LocalBinder();


    public AppService()
    {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onServiceStart();

        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent)
    {
        // ????
        onServiceStart();
        return mBinder;
    }

    @Override
    public void onDestroy()
    {
        // ???
        onServiceStop();

        super.onDestroy();
    }


    // Service state handlers.
    // ....

    private boolean onServiceStart()
    {

        // TBD:
        // Publish live card...
        // ....
        publishCard(this);
        // ....

        //currentState = STATE_NORMAL;
        return true;
    }

    private boolean onServicePause()
    {
        Log.d("AppService","onServicePause() called.");
        return true;
    }
    private boolean onServiceResume()
    {
        Log.d("AppService","onServiceResume() called.");
        return true;
    }

    private boolean onServiceStop()
    {
        Log.d("AppService","onServiceStop() called.");

        stopService(new Intent(AppService.this, WifiService.class));

        // TBD:
        // Unpublish livecard here
        // .....
        unpublishCard(this);
        // ...

        android.os.Process.killProcess(android.os.Process.myPid());

        return true;
    }


    // For live cards...

    private void publishCard(Context context)
    {


        if (mLiveCard == null) {
            mLiveCard = new LiveCard(this, LIVE_CARD_ID);
            mCallback = new AppDrawer(this);
            mLiveCard.setDirectRenderingEnabled(true).getSurfaceHolder().addCallback(mCallback);

            Intent menuIntent = new Intent(this, ApiDemoActivity.class);

            // start wifi service here so that it can be used from other applications
            Intent intent= new Intent(this, WifiService.class);
            context.startService(intent);

            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
            mLiveCard.attach(this);
            mLiveCard.publish(PublishMode.REVEAL);
        } else {
            mLiveCard.navigate();
        }


    }

    private void unpublishCard(Context context)
    {

        if (mLiveCard != null && mLiveCard.isPublished()) {
            if (mCallback != null) {
                mLiveCard.getSurfaceHolder().removeCallback(mCallback);
            }
            mLiveCard.unpublish();
            mLiveCard = null;
        }

    }


}
