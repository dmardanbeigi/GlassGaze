package com.glassgaze.SeperateThreadExamples;
import android.app.Service;
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
         Intent serviceIntent = new Intent( this, Wifi_Service2.class);
         startService(serviceIntent);
And stop it by:
        stopWifiService(new Intent(ApiDemoActivity.this, Wifi_Service2.class));

 */


public class Wifi_Service2 extends Service {

Boolean mStop=false;
    private final IBinder mBinder = new MyBinder();
    class Task implements Runnable {
        @Override
        public void run() {
            //test
            int counter=0;
            while(counter<50000 && mStop==false){
                Log.d("InitService", "..................................." + counter++);
                if( Looper.myLooper() == Looper.getMainLooper())
                    Log.e("InitService", "...................................ERROR" );
                else
                    Log.d("InitService", "...................................Separate thread");

            }

        }

    }

    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(new Task()).start();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("InitService", "Counting STOPED!   :) :) " );

        mStop=true;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    public class MyBinder extends Binder {
        Wifi_Service2 getService() {
            return Wifi_Service2.this;
        }
    }
}
