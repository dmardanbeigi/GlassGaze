/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.glassgaze;

import com.glassgaze.EXPERIMENT.Activity_CalibEX;
import com.glassgaze.GazeDisplay.Demos.metaio.SplashActivity;
import com.github.barcodeeye.scan.CaptureActivity;


import com.glassgaze.GazeDisplay.DisplayActivity;
import com.glassgaze.GazeLiveView.LiveViewActivity;
import com.google.android.glass.app.Card;
import com.google.android.glass.media.Sounds;

import com.glassgaze.card.CardAdapter;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Messenger;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.List;


import android.os.RemoteException;
import android.os.Message;
import android.widget.Toast;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;


public class ApiDemoActivity extends Activity  {

    private AppService appService;

    private static final String TAG = ApiDemoActivity.class.getSimpleName();

    // Index of the cards.
    static final int DISPLAY = 0;
    static final int LIVEVIEW = 1;
    static final int EXPERIMENT = 2;


    static final int EXIT = 3;



    private CardScrollView mCardScroller;
    private CardScrollView mView;

    private CountDownTimer cdTimer;
    private long total = 4000;

//.......................WIFI SERVICE

    /**
     * Messenger used for receiving responses from service.
     * Activity target published for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Messenger used for communicating with service.
     */
    Messenger mService = null;

    private WifiService mWifiService;
    private boolean mBounded;


   // private final Handler mHandler = new Handler();
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {


                case  MessageType.PHOTO_READY:

                    mWifiService.sendPhoto((byte[]) msg.obj);

                    break;

            }
        }
    };



    /**
     * Activity Handler of incoming messages from service.
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
                    break;
                }



                case MessageType.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {

                        case WifiService.STATE_CONNECTED:

                            //mTitle.setText(R.string.title_connected);

                            break;
                        case WifiService.STATE_DISCONNECTED:


                            //Stop;

                            // android.os.Process.killProcess(android.os.Process.myPid());

                            //mTitle.setText(R.string.title_connected);

                            break;
                        case WifiService.STATE_CONNECTING:


                            // mTitle.setText(R.string.title_connecting);
                            break;



                    }
                    break;


                case MessageType.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
                case MessageType.MESSAGE_READ:

                    byte[] readBuf = (byte[]) msg.obj;

                    switch (Integer.valueOf(readBuf[0])) {
                        case MessageType.toGLASS_test:

                            Toast.makeText(getApplicationContext(), "Test Msg from Haytham", Toast.LENGTH_SHORT).show();  //  C1S

                            break;

                        default:
                            super.handleMessage(msg);
                    }

                    break;
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {

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



        }


        public void onServiceDisconnected(ComponentName className) {
            Toast.makeText(ApiDemoActivity.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mBounded = false;
            mWifiService = null;
        }




    };

    //......................WIFI SERVICE

    // Visible for testing.
    CardScrollView getScroller() {
        return mCardScroller;
    }


    /**
     * Create list of API demo cards.
     */
    private List<Card> createCards(Context context) {
        ArrayList<Card> cards = new ArrayList<Card>();


        cards.add(DISPLAY, new Card(context)
                .addImage(getImageResource(DISPLAY))
                .setImageLayout(Card.ImageLayout.LEFT)
                .setText(R.string.text_DISPLAY));

        cards.add(LIVEVIEW, new Card(context)
                .addImage(getImageResource(LIVEVIEW))
                .setImageLayout(Card.ImageLayout.LEFT)
                .setText(R.string.text_LIVEVIEW));

        cards.add(EXPERIMENT, new Card(context).setText("Calibration Experiment"));

        cards.add(EXIT, new Card(context).setText(R.string.text_EXIT));


  /*     cards.add(EXIT, new Card(context)
                .addImage(getImageResource(-1))
                .setImageLayout(Card.ImageLayout.LEFT)
                .setText(R.string.text_EXIT));
*/
        return cards;
    }


    /** Returns current image resource. */
    public static int getImageResource(int i) {
        switch (i) {
            case DISPLAY:  return R.drawable.icon_displaygaze;
            case LIVEVIEW:  return R.drawable.icon_liveview;

            default: return 0;
        }
    }



    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);





        // Ensure screen stays on during demo.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        CardScrollAdapter mAdapter = new CardAdapter(createCards(this));
        mCardScroller = new CardScrollView(this);
        mCardScroller.setAdapter(mAdapter);
        setContentView(mCardScroller);
        setCardScrollerListener();

        if (Constants.QRcode_Scan ) {
            Constants.QRcode_Scan=false;//Only for the first time
            startActivity(new Intent(ApiDemoActivity.this, CaptureActivity.class));
        }





    }
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
        bindService(intent, mConnection,                Context.BIND_AUTO_CREATE);

        super.onStart();
    }

    @Override
    protected void onResume() {
        mCardScroller.activate();
        Intent intent= new Intent(this, WifiService.class);
        bindService(intent, mConnection,                 Context.BIND_AUTO_CREATE);
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
        mCardScroller.deactivate();
        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
        super.onPause();
    }

    /**
     * Different type of activities can be shown, when tapped on a card.
     */
    private void setCardScrollerListener() {
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Clicked view at position " + position + ", row-id " + id);
                int soundEffect = Sounds.TAP;
                switch (position) {


                    case DISPLAY:
                        startActivity(new Intent(ApiDemoActivity.this, DisplayActivity.class));




                        break;
                    case LIVEVIEW:
                        startActivity(new Intent(ApiDemoActivity.this, LiveViewActivity.class));
                        break;

                    case EXPERIMENT:
                        startActivity(new Intent(ApiDemoActivity.this, Activity_CalibEX.class));
                        break;



                    case EXIT:
                        mWifiService.tts.shutdown();
                        mWifiService.tts = null;


                        mWifiService.mStop=true;
                       // mWifiService.stopWifiService();

                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            if(mBounded) {
                                unbindService(mConnection);
                                mBounded = false;
                            }
                           // String str = "CLIENT>>> TERMINATE";
                            //byte[] array = str.getBytes();
                            //mWifiService.write(array);


                            stopService(new Intent(ApiDemoActivity.this, AppService.class));




                        }
                    });


                        finish();

                        break;




                    default:
                        soundEffect = Sounds.ERROR;
                        Log.d(TAG, "Don't show anything");
                }

                // Play sound.
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(soundEffect);
            }
        });
    }




}
