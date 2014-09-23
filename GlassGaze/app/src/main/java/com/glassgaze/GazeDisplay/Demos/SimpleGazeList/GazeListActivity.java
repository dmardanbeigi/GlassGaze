/**
 * Created by diako on 8/27/2014.
 */
package com.glassgaze.GazeDisplay.Demos.SimpleGazeList;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
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

import java.io.Serializable;
import java.util.List;

public class GazeListActivity extends Activity {

    ListView myList;
    GazeListAdapter customAdapter;
    private Point gaze=new Point(0,0);
    PointerView_display mPointerViewDisplay;
    private GestureDetector mGestureDetector = null;

    static final int RGT = 1;
    static final int CALIB = 1;


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
            Toast.makeText(GazeListActivity.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mBounded = false;
            mWifiService = null;
        }




    };

private void init()
{
    mWifiService.GazeStream(RGT, true);

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


                            Intent i = new Intent(GazeListActivity.this, Calibration.class);
                            startActivityForResult(i,CALIB);


                        }

                        break;

                    case MessageType.toGLASS_test:
                        Toast.makeText(getApplicationContext(), "Test Msg from Haytham", Toast.LENGTH_SHORT).show();  //  C1S

                        break;
                    case MessageType.toGLASS_ERROR_NOTCalibrated:
                        mWifiService.Speek("Calibrate first!");
                        break;


                    case MessageType.toGLASS_GAZE_RGT:

                        //gaze.x = Utils.GetX(readBuf);
                        gaze.y = Utils.GetY(readBuf);

                        int item_height = 96;
                        if (myList.getSelectedItemPosition()!=0 &&(gaze.y > 0 && gaze.y < item_height)) {
                            myList.setSelection(0);
                            // myList.requestFocus();
                            // myList.clearChoices();
                            // myList.getSelectedView().setSelected(false);
                            myList.setItemChecked(0, true);
                        } else if (myList.getSelectedItemPosition()!=1 && myList.getCount() > 1 && (gaze.y > item_height && gaze.y < 2 * item_height)) {
                            myList.setSelection(1);

                            myList.setItemChecked(1, true);

                            int test=myList.getSelectedItemPosition();
                            myList.setSelection(1);

                            myList.setItemChecked(1, true);


                        } else if (myList.getSelectedItemPosition()!=2 && myList.getCount() > 2 && (gaze.y >2* item_height && gaze.y < 3 * item_height)) {
                            myList.setSelection(2);
                            myList.setItemChecked(2, true);
                        }else if (myList.getSelectedItemPosition()!=3 && myList.getCount() > 3 && (gaze.y >3* item_height && gaze.y < 4 * item_height)) {
                            myList.setSelection(3);
                            myList.setItemChecked(3, true);
                        }

                        //Log.i("gaze:", String.valueOf(gaze.y));

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

        if (requestCode == CALIB) {
            if(resultCode == RESULT_OK){
                //String result=data.getStringExtra("result");
            }
            if (resultCode == RESULT_CANCELED) {
                //Write your code if there's no result

            }
        }

    }//onActivityResult


    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // Ensure screen stays on during demo.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mGestureDetector = createGestureDetector(this);


        Intent intent = getIntent();
        Bundle args = intent.getBundleExtra("names");
        List<String> names = ( List<String>) args.getSerializable("ARRAYLIST");
        args = intent.getBundleExtra("IDs");
        List<String> IDs = ( List<String>) args.getSerializable("ARRAYLIST");

        setContentView(R.layout.gazelist_view);
         customAdapter = new GazeListAdapter(getApplicationContext(),names,IDs);
         myList = (ListView) findViewById(R.id.my_list);
        myList.setAdapter(customAdapter);

        myList.setOverScrollMode(View.OVER_SCROLL_NEVER);
        myList.setVerticalScrollBarEnabled(false);



        runOnUiThread(new Runnable() {
            public void run() {
                myList.requestFocus();
                myList.clearChoices();
               // myList.setSelection(0);
               // myList.getSelectedView().setSelected(false);

               // myList.clearFocus();
            }
        });

    }

    @Override
    public  void onAttachedToWindow() {
/*        super.onAttachedToWindow();
        //does not work
        try {
            myList.clearFocus();
            myList.clearChoices();
            myList.setSelection(-1);
            myList.invalidate();
        }
        catch (Exception e)
        {

        }*/


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

    public  int getItemHeightofListView(ListView listView, int item) {


        int listviewElementsheight = 0;
        // for listview total item height
        // items = mAdapter.getCount();



            View childView = customAdapter.getView(item, null, listView);
            childView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            listviewElementsheight= childView.getMeasuredHeight();


            //debug
           int top= childView.getTop();
           Log.d("top", String.valueOf( top));
           int btm= childView.getBottom();
        Log.d("btm", String.valueOf( btm));
           int totalH= childView.getHeight();
        Log.d("totalH", String.valueOf( totalH));


        return listviewElementsheight;

    }
    private void Done(int selected_indx){
        Intent returnIntent = new Intent();
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.playSoundEffect(Sounds.TAP);

        returnIntent.putExtra("result", String.valueOf(selected_indx));

        if (selected_indx!=-1){

            Bundle args = new Bundle();
            args.putSerializable("ARRAYLIST", (Serializable) customAdapter.list_names);
            returnIntent.putExtra("names", args);
        }

    setResult(RESULT_OK,returnIntent);

        finish();

    }
    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);
        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP) {


/*                    Toast.makeText(getApplicationContext(),
                            String.valueOf(getItemHeightofListView(myList,0)),
                            Toast.LENGTH_LONG).show();*/


                    Done(myList.getSelectedItemPosition());

                    return true;
                }
               else if (gesture == Gesture.TWO_TAP) {


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
