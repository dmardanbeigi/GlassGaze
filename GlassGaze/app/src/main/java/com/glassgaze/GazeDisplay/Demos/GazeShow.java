/**
 * Created by diako on 8/27/2014.
 */
package com.glassgaze.GazeDisplay.Demos;


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
import com.glassgaze.GazeDisplay.Calibration;
import com.glassgaze.GazeDisplay.PointerView_display;
import com.glassgaze.MessageType;
import com.glassgaze.R;
import com.glassgaze.Utils;
import com.glassgaze.WifiService;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;

public class GazeShow extends Activity{


    private Boolean showPointer=true;

    private GestureDetector mGestureDetector = null;

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
            Toast.makeText(GazeShow.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mBounded = false;
            mWifiService = null;
        }




    };

private void init()
{
    mWifiService.GazeStream(RGT,true);
    mPointerViewDisplay = (PointerView_display) findViewById(R.id.pointerView_display);
    mPointerViewDisplay.setBackgroundColor(mWifiService.backgroundColor);

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
                    case MessageType.toGLASS_LetsCorrectOffset:

                        mWifiService.GazeStream(RGT, false);
                        mWifiService.write(MessageType.toHAYTHAM_Calibrate_Display_Correct);


                        break;
                    case MessageType.toGLASS_Calibrate_Display:


                        int x0 = Utils.GetX(readBuf);
                        int y0 = Utils.GetY(readBuf);

                        if ((x0==-1 && y0==-1)||(x0==-3 && y0==-3) )//calibrate or correct offset
                        {


                            Intent i = new Intent(GazeShow.this, Calibration.class);
                            startActivityForResult(i, 1);


                        }

                        break;

                    case MessageType.toGLASS_test:
                        Toast.makeText(getApplicationContext(), "Test Msg from Haytham", Toast.LENGTH_SHORT).show();  //  C1S

                        break;
                    case MessageType.toGLASS_ERROR_NOTCalibrated:
                        mWifiService.Speek("Calibrate first!");
                        break;


                    case MessageType.toGLASS_GAZE_RGT:

                        int x = Utils.GetX(readBuf);
                        int y = Utils.GetY(readBuf);

                        if( showPointer )
                        {

                            mPointerViewDisplay.GazeEvent(x, y, 5);
                            mPointerViewDisplay.postInvalidate();

                        }
                        // if(cardSelected && mCardScroller.getSelectedItemId()==APP?) DO SOMETHING ELSE!;


                        break;

                    default:
                        super.handleMessage(msg);
                }

                break;
        }
    }
}


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        if (requestCode == 1) {
            if(resultCode == RESULT_OK){

                //String result=data.getStringExtra("result");
            }
            if (resultCode == RESULT_CANCELED) {

                //Write your code if there's no result
               // audio.playSoundEffect(Sounds.ERROR);

            }
        }
    }//onActivityResult






    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // Requests a voice menu on this activity. As for any other window feature,
        // be sure to request this before setContentView() is called
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        // Ensure screen stays on during demo.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mGestureDetector = createGestureDetector(this);

        setContentView(R.layout.display_blank);

    }

    //...................................................
    @Override
    protected void onDestroy(){
        mWifiService. GazeStream(RGT, false);
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
            getMenuInflater().inflate(R.menu.voice_menu_display_gazeshow, menu);
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
                case R.id.menu_display_calibration_calibrate: {
                    mWifiService.GazeStream(RGT, false);
                    mWifiService.Speek("Wait!");
                    mWifiService.write(MessageType.toHAYTHAM_Calibrate_Display);
                } break;
                case R.id.menu_display_calibration_correctOffset:  {
                    mWifiService.GazeStream(RGT, false);
                    mWifiService.Speek("Wait!");
                    mWifiService.write(MessageType.toHAYTHAM_Calibrate_Display_Correct);

                } break;

                case R.id.menu_calibration_reuse:  {
                    mWifiService.GazeStream(RGT, false);
                    mWifiService.Speek("Wait!");
                    mWifiService.write(MessageType.toHAYTHAM_Calibrate_ReUse);

                } break;
                case R.id.menu_color_white:
                    int backgroundColor = Color.BLACK;
                {
                    backgroundColor =Color.WHITE;
                    mWifiService.backgroundColor= backgroundColor;
                    mPointerViewDisplay.setBackgroundColor(mWifiService.backgroundColor);
                }  break;
                case R.id.menu_color_gray:  {
                    backgroundColor =Color.GRAY;
                    mWifiService.backgroundColor= backgroundColor;
                    mPointerViewDisplay.setBackgroundColor(mWifiService.backgroundColor);
                } break;
                case R.id.menu_color_blue:  {
                    backgroundColor =Color.BLUE;
                    mWifiService.backgroundColor= backgroundColor;
                    mPointerViewDisplay.setBackgroundColor(mWifiService.backgroundColor);
                }  break;
                case R.id.menu_color_black:  {
                    backgroundColor =Color.BLACK;
                    mWifiService.backgroundColor= backgroundColor;
                    mPointerViewDisplay.setBackgroundColor(mWifiService.backgroundColor);
                }  break;

                case R.id.menu_pointer_show: showPointer=true;  break;
                case R.id.menu_pointer_hide: showPointer=false; mPointerViewDisplay.hideFingerTrace(0,true);  break;
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
               if (gesture == Gesture.TWO_TAP) {

                   // Plays sound.
                   AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                   am.playSoundEffect(Sounds.TAP);
                   // Toggles voice menu. Invalidates menu to flag change.
                   mVoiceMenuEnabled = !mVoiceMenuEnabled;
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
