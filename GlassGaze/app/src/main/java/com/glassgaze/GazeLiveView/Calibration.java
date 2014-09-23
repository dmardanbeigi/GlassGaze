/**
 * Created by diako on 8/27/2014.
 */
package com.glassgaze.GazeLiveView;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
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

    private CameraView cameraView = null;

    public boolean waitForYesNo = false;
    private long total = 4000;
    private long tempTotal = 5000;



    int currentPhoto=0;
    int totalPhotosNeeded;

    FrameLayout preview;

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

            Toast.makeText(getApplicationContext(), "Stand in front of the calibration board", Toast.LENGTH_LONG).show(); //"M2
            mWifiService.Speek("Stand in front of the calibration board");
            startCountDownTimer();
            waitForYesNo = true;

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

                case MessageType.DATA_SENT_OK: {

                    //Toast.makeText(LiveViewActivity.this, "Photo was sent successfully", Toast.LENGTH_SHORT).show();
                }
                break;

                case MessageType.DIGEST_DID_NOT_MATCH: {

                    Toast.makeText(Calibration.this, "Photo not sent properly!", Toast.LENGTH_SHORT).show();


                    }

                break;
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
                        case MessageType.toGLASS_Calibrate_Scene:


                            int x0 = Utils.GetX(readBuf);
                            int y0 = Utils.GetY(readBuf);
                            if (x0 == -2 && y0 == -2) {



                                Done();
                            }
                            else if (x0 == -3 && y0 == -3) {

                                Cancel();


                            } else if (x0 == -4 && y0 == -4) {//look at the next target
                                int tmp = (currentPhoto + 1);
                                //look at the next target
                                mWifiService.Speek("keep your head still and Look at target number " + tmp);

                            } else if (x0 == -5 && y0 == -5) {//look at the next target

                                //look at the next target
                                mWifiService.Speek("Look at target number " + currentPhoto + " again!");

                            } else {//set current point and Take picture


                                totalPhotosNeeded = y0;
                                currentPhoto = x0;
                                cameraView.camera.takePicture(null, null, new PhotoHandler(getApplicationContext(), mHandler));


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

    // private final Handler mHandler = new Handler();
    // The Handler that gets information back
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {


            switch (msg.what) {

                case  MessageType.PHOTO_READY:
                    mWifiService.sendPhoto((byte[]) msg.obj);

                            RestartCameraView();
                    break;
            }
        }
    };

    private void startCountDownTimer() {
        CountDownTimer cdTimer = new CountDownTimer(tempTotal, 1000) {
            public void onTick(long millisUntilFinished) {
                //update total with the remaining time left
                tempTotal = millisUntilFinished;

            }

            public void onFinish() {

                if (waitForYesNo) {
                    mWifiService.Speek("Tap when you are ready");
                    tempTotal = 2 * total;//
                    startCountDownTimer();
                }

            }
        }.start();

    }


private void Done(){
 /*   Intent returnIntent = new Intent();
    returnIntent.putExtra("result",result);
    setResult(RESULT_OK,returnIntent);
    finish();*/

    finish();



    AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
    audio.playSoundEffect(Sounds.SUCCESS);

    setContentView(R.layout.menu_layout);
    ((ImageView)findViewById(R.id.icon)).setImageResource(R.drawable.ic_done_50);
    //((TextView)findViewById(R.id.label)).setText(getString(R.string.deleted_label));




            Intent returnIntent = new Intent();
            setResult(RESULT_OK, returnIntent);


}
    private void Cancel(){
        waitForYesNo=false;
        finish();


        AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        audio.playSoundEffect(Sounds.ERROR);


         Intent returnIntent = new Intent();
         setResult(RESULT_CANCELED, returnIntent);


    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // Ensure screen stays on during demo.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Turn on Gestures
        mGestureDetector = createGestureDetector(this);

        setContentView(R.layout.liveview_blank);

        try {
            // create a basic camera preview class that can be included in a View layout.
            cameraView = new CameraView(this);
        } catch (Exception e) {
            Log.e("............", "Error creating cameraView: " + e.getMessage());
        }
        try {
            preview = (FrameLayout) findViewById(R.id.camera_preview);
            preview.addView(cameraView);

            // Attach a callback for preview
            // CamCallback camCallback = new CamCallback();
            //cameraView.camera.setPreviewCallback(camCallback);

        } catch (Exception e) {
            Log.e(">>>>>>>>>>>>>>>>>>>", "Error adding cameraview to FrameLayout: " + e.getMessage());
        }


    }



    //...................................................
    @Override
    protected void onDestroy(){
        waitForYesNo=false;
        cameraView.releaseCamera();
        preview.removeAllViews();

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


    /**
     * We call this method after taking a picture to release the freezed camera view
     */
    private void RestartCameraView() {

        cameraView.CameraStart();

    }

    private void ShowCameraView(){

        //preview.setVisibility(View.VISIBLE);
        cameraView.setVisibility(View.VISIBLE);
    }


    private void HideCameraView(){

        preview.setVisibility(View.INVISIBLE);

        // I couldn't takepicture when the cameraview is invisible.
        //So I had to invisible the whole view instead!!!!!!!!!!!!!!!!!!!
        cameraView.setVisibility(View.INVISIBLE);
    }

    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);
        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.LONG_PRESS || gesture == Gesture.TAP) {
                    AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    audio.playSoundEffect(Sounds.TAP);

                    if (waitForYesNo) {

                        HideCameraView();

                        mWifiService.Speek("OK, Look at the first target");

                        mWifiService.write(MessageType.toHAYTHAM_SceneCalibrationReady);


                        waitForYesNo = false;
                        return true;
                    }
                }
               else if (gesture == Gesture.SWIPE_DOWN ) {

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
