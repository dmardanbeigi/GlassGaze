package com.glassgaze.SeperateThreadExamples;
import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

/**
 * Created by Diako on 7/30/2014.
 */

/*
inside the main activity Start the service by:
         Intent serviceIntent = new Intent( this, Wifi_initService.class);
         startService(serviceIntent);
And stop it by:
        stopWifiService(new Intent(ApiDemoActivity.this, Wifi_initService.class));


 */
public class Wifi_initService extends IntentService {
/*

 */
public Wifi_initService() {
    super("Wifi_initService");
}

    @Override
    protected void onHandleIntent(Intent intent) {

        //test
        int counter=0;
        while(counter<50000 && mStop==false){

            Log.d("InitService", "..................................." + counter++);
            if( Looper.myLooper() == Looper.getMainLooper())
                Log.e("InitService", "...................................ERROR" );
            else
                Log.d("InitService", "...................................Separate thread" );

        }

    }

Boolean mStop=false;

    private final IBinder mBinder = new MyBinder();





    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("InitService", "InitService STOPED!   :) :) ");

        mStop=true;
        stopSelf();
    }



    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    public class MyBinder extends Binder {
        Wifi_initService getService() {
            return Wifi_initService.this;
        }
    }
}
