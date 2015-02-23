/**
 * Created by diako on 8/27/2014.
 */
package com.glassgaze.GazeLiveView.Demos;


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
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.glassgaze.Constants;
import com.glassgaze.GazeDisplay.PointerView_display;
import com.glassgaze.GazeLiveView.CameraView;
import com.glassgaze.GazeLiveView.PhotoHandler;
import com.glassgaze.MessageType;
import com.glassgaze.R;
import com.glassgaze.Utils;
import com.glassgaze.WifiService;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;

public class SnapShot extends Activity{


    private int mState_SNAPSHOT;
    // Constants that indicate the current state within each card


    public static final int STATE_SNAPSHOT_LIVEVIEW= 1;
    public static final int STATE_SNAPSHOT_PhotoREADY = 2;

    private CameraView cameraView = null;
    private Boolean showPointer=true;

    private int backgroundColor= Color.BLACK;

    private GestureDetector mGestureDetector = null;

    static final int HMGT = 0;
    static final int RGT = 1;

    PointerView_display mPointerViewDisplay;

    FrameLayout preview;
    private boolean mVoiceMenuEnabled = true;
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
            Toast.makeText(SnapShot.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mBounded = false;
            mWifiService = null;
        }




    };

private void init()
{
    mWifiService.GazeStream(HMGT, true);



}
    //......................WIFI SERVICE


// protected abstract void setAdapter(CardScrollView view);




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

                Toast.makeText(SnapShot.this, "Photo not sent properly!", Toast.LENGTH_SHORT).show();


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


                        int x1 = Utils.GetX(readBuf);
                        int y1 = Utils.GetY(readBuf);

                        if (x1 == -1 && y1 == -1) //only show the text on the display
                        {
                            cameraView.releaseCamera();
                            preview.removeAllViews();

                            Intent i = new Intent(SnapShot.this, com.glassgaze.GazeLiveView.Calibration.class);
                            startActivityForResult(i, 1);


                        }

                        break;
                    case MessageType.toGLASS_test:
                        Toast.makeText(getApplicationContext(), "Test Msg from Haytham", Toast.LENGTH_SHORT).show();  //  C1S

                        break;
                    case MessageType.toGLASS_GAZE_HMGT:
                        int x = Utils.GetX(readBuf);
                        int y = Utils.GetY(readBuf);

                        //do something with x,y
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

                    cameraView.CameraStop();
                    mState_SNAPSHOT= STATE_SNAPSHOT_PhotoREADY;
                    getWindow().invalidatePanelMenu(WindowUtils.FEATURE_VOICE_COMMANDS);
                    break;
            }
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if(resultCode == RESULT_OK){

                //String result=data.getStringExtra("result");
            }
            if (resultCode == RESULT_CANCELED) {

                //Write your code if there's no result


            }

            new Handler().postDelayed(new Runnable()
            {
                public void run()
                {

                    SetupView();
                }
            }, 2000);



        }
    }//onActivityResult



private  void SetupView()
{
    mState_SNAPSHOT=STATE_SNAPSHOT_LIVEVIEW;

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

    } catch (Exception e) {
        Log.e(">>>>>>>>>>>>>>>>>>>", "Error adding cameraview to FrameLayout: " + e.getMessage());
    }
}


    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);


        // Requests a voice menu on this activity. As for any other window feature,
        // be sure to request this before setContentView() is called
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        // Ensure screen stays on during demo.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mGestureDetector = createGestureDetector(this);

     SetupView();



    }

    //...................................................
    @Override
    protected void onDestroy(){
        mWifiService. GazeStream(HMGT, false);
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
    //..................................VoiceMenu

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            switch (mState_SNAPSHOT)
            {
                case STATE_SNAPSHOT_LIVEVIEW:
                    getMenuInflater().inflate(R.menu.voice_menu_scene_snapshot1, menu);
                    break;
                case STATE_SNAPSHOT_PhotoREADY:
                    getMenuInflater().inflate(R.menu.voice_menu_scene_snapshot2, menu);
                    break;

            }


            return true;
        }
        // Good practice to pass through, for options menu.
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            // Dynamically decides between enabling/disabling voice menu.
            return mVoiceMenuEnabled;
        }
        // Good practice to pass through, for options menu.
        return super.onPreparePanel(featureId, view, menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            switch (item.getItemId()) {

                case R.id.menu_liveview_calibration_calibrate: {
                    mWifiService.GazeStream(HMGT, false);
                    mWifiService.Speek("Wait!");
                    mWifiService.write(MessageType.toHAYTHAM_Calibrate_Scene_4);
                } break;
                case R.id.menu_liveview_calibration_correctOffset:  {
                    mWifiService.GazeStream(HMGT, false);
                    mWifiService.Speek("Wait!");
                    mWifiService.write(MessageType.toHAYTHAM_Calibrate_Scene_Correct);

                } break;
                case R.id.menu_liveview_calibration_create:  {
                    mWifiService.GazeStream(HMGT, false);
                    mWifiService.Speek("Wait!");
                    mWifiService.write(MessageType.toHAYTHAM_Calibrate_Scene_Master);

                } break;

                case R.id.menu_snapshot:
                {

                    //save sample here
                    mWifiService.GazeStream(HMGT, false);
                    mWifiService.write(MessageType.toHAYTHAM_SnapshotComming);
                    cameraView.camera.takePicture(null, null, new PhotoHandler(getApplicationContext(), mHandler));

                } break;
                case R.id.menu_viewfinder:
                {
                cameraView.CameraStart();
                    mState_SNAPSHOT= STATE_SNAPSHOT_LIVEVIEW;
                    getWindow().invalidatePanelMenu(WindowUtils.FEATURE_VOICE_COMMANDS);
                } break;
                default: return true;  // No change.
            }

            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }





    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);
        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

               if (gesture == Gesture.TWO_TAP) {

                   // Plays sound.

                   am.playSoundEffect(Sounds.TAP);
                   // Toggles voice menu. Invalidates menu to flag change.
                   mVoiceMenuEnabled = !mVoiceMenuEnabled;
                   getWindow().invalidatePanelMenu(WindowUtils.FEATURE_VOICE_COMMANDS);


                   return true;
               }else if (gesture == Gesture.SWIPE_DOWN && mState_SNAPSHOT == STATE_SNAPSHOT_PhotoREADY) {
                   am.playSoundEffect(Sounds.DISMISSED);
                   cameraView.CameraStart();
                   mState_SNAPSHOT = STATE_SNAPSHOT_LIVEVIEW;
                   getWindow().invalidatePanelMenu(WindowUtils.FEATURE_VOICE_COMMANDS);
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
