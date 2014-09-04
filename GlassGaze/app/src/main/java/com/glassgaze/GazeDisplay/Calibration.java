/**
 * Created by diako on 8/27/2014.
 */
package com.glassgaze.GazeDisplay;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.glassgaze.Constants;
import com.glassgaze.MessageType;
import com.glassgaze.R;
import com.glassgaze.Utils;
import com.glassgaze.WifiService;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

public class Calibration extends Activity {

    private GestureDetector mGestureDetector = null;
    PointerView_display mPointerViewDisplay;
    static final int HMGT = 0;
    static final int RGT = 1;



    /**
     * Messenger used for receiving responses from service.
     * Activity target published for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    //.......................WIFI SERVICE


    /**
     * Messenger used for communicating with service.
     */
    Messenger mService = null;
    private WifiService mWifiService;
    private boolean mBounded;



    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,IBinder binder) {

            Log.d("MessengerActivity", "Connected to service. Registering our Messenger in the Service...");

            WifiService.MyBinder b = (WifiService.MyBinder) binder;
            mWifiService = b.getService();
            mService =  mWifiService.mMessenger;
            //mService = new Messenger(binder);
            mBounded = true;

            // Register our messenger also on Service side:
            Message msg = Message.obtain(null, WifiService.MESSAGE_TYPE_REGISTER);
            msg.replyTo = mMessenger;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                // We always have to trap RemoteException (DeadObjectException
                // is thrown if the target Handler no longer exists)
                e.printStackTrace();
            }


init();

        }
        public void onServiceDisconnected(ComponentName className) {
            Toast.makeText(Calibration.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mBounded = false;
            mWifiService = null;
        }
    };
private void init()
{
    if (mWifiService.mState==mWifiService.STATE_CONNECTED) {
        mWifiService.Speek("OK, Follow the white circle");

        mPointerViewDisplay.GazeEvent(320, 180, 1);
        mWifiService.write(MessageType.toHAYTHAM_READY);
        mWifiService. GazeStream(RGT,false);
        mWifiService. GazeStream(HMGT,false);
    }
}
    //......................WIFI SERVICE


    /**
     * Activity Handler of incoming messages
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {


            switch (msg.what) {

                case WifiService.MESSAGE_TYPE_TEXT: {
                    Bundle b = msg.getData();
                    CharSequence text = null;
                    if (b != null) {
                        text = b.getCharSequence("data");
                    } else {
                        text = "Service responded with empty message";
                    }
                    Log.d("MessengerActivity", "Response: " + text);
                }
                break;


                case MessageType.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {

                        case WifiService.STATE_CONNECTED:

                            //mTitle.setText(R.string.title_connected);

                            break;
                        case WifiService.STATE_DISCONNECTED:


                            break;
                        case WifiService.STATE_CONNECTING:

                            Cancel();
                            // mTitle.setText(R.string.title_connecting);
                            break;
                    }
                    break;


                case MessageType.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),Toast.LENGTH_SHORT).show();
                    break;

                case MessageType.MESSAGE_READ:

                    byte[] readBuf = (byte[]) msg.obj;

                    switch (Utils.GetIndicator(readBuf)) {
                        case MessageType.toGLASS_Calibrate_Display:


                            int x0 = Utils.GetX(readBuf);
                            int y0 = Utils.GetY(readBuf);

                           if ((x0==-2 && y0==-2)||(x0==-3 && y0==-4))
                            {
                               // Toast.makeText(getApplicationContext(), "Calibration finished!", Toast.LENGTH_SHORT).show(); //"M1

                                mPointerViewDisplay.hideFingerTrace(1);

                                Done();


                            }
                           else
                            {
                                mPointerViewDisplay.GazeEvent(x0, y0, 1);
                                mWifiService.write(MessageType.toHAYTHAM_READY);
                            }


                            break;

                        case MessageType.toGLASS_test:
                            Toast.makeText(getApplicationContext(), "Test Msg from Haytham", Toast.LENGTH_SHORT).show();  //  C1S

                            break;
                        case MessageType.toGLASS_ERROR_NOTCalibrated: {
                            mWifiService.Speek("Calibrate first!");
                            Cancel();
                        }
                            break;

                        default:
                            super.handleMessage(msg);
                    }

                    break;
            }
        }
    }


private void Done(){
 /*   Intent returnIntent = new Intent();
    returnIntent.putExtra("result",result);
    setResult(RESULT_OK,returnIntent);
    finish();*/



    Intent returnIntent = new Intent();
    setResult(RESULT_OK, returnIntent);

    AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
    audio.playSoundEffect(Sounds.SUCCESS);

    setContentView(R.layout.menu_layout);
    ((ImageView)findViewById(R.id.icon)).setImageResource(R.drawable.ic_done_50);
    //((TextView)findViewById(R.id.label)).setText(getString(R.string.deleted_label));



    new Handler().postDelayed(new Runnable()
    {
        public void run()
        {
            finish();
        }
    }, 1000);





}
    private void Cancel(){

        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);

        AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        audio.playSoundEffect(Sounds.ERROR);

        finish();
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // Ensure screen stays on during demo.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Turn on Gestures
        mGestureDetector = createGestureDetector(this);

        setContentView(R.layout.display_blank);
        mPointerViewDisplay = (PointerView_display) findViewById(R.id.pointerView_display);
        mPointerViewDisplay.setBackgroundColor(Color.BLACK);




    }



    //...................................................
    @Override
    protected void onDestroy(){

        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }


        super.onDestroy();
    }

    @Override
    protected void onStart() {
        Intent intent= new Intent(this, WifiService.class);
        bindService(intent, mConnection,Context.BIND_AUTO_CREATE);

        super.onStart();


    }

    @Override
    protected void onResume() {

        Intent intent= new Intent(this, WifiService.class);
        bindService(intent, mConnection,Context.BIND_AUTO_CREATE);
        super.onResume();


    }

    @Override
    protected void onStop() {
        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
        super.onStop();

    }
    @Override
    protected void onPause() {

        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
        super.onPause();
    }
    //.....................................................


    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);
        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.SWIPE_DOWN ) {
                    Cancel();

                    return true;
                }
                return false;
            }
        });
        return gestureDetector;
    }


    @Override
    public boolean onGenericMotionEvent(MotionEvent event)
    {
        if (mGestureDetector != null)
        {
            return mGestureDetector.onMotionEvent(event);
        }

        return false;
    }

}
