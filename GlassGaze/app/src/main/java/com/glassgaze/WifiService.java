package com.glassgaze;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.glassgaze.R;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Locale;

public class WifiService extends Service {


    // Constants describing types of message. Different types of messages can be
    // passed and this identifies them.
    /**
     * Message type: register the activity's messenger for receiving responses
     * from Service. We assume only one activity can be registered at one time.
     */
    public static final int MESSAGE_TYPE_REGISTER = 1;
    /**
     * Message type: text sent Activity<->Service
     */
    public static final int MESSAGE_TYPE_TEXT = 2;

    /**
     * Messenger used for handling incoming messages.
     */
    public final Messenger mMessenger = new Messenger(new IncomingHandler());
    /**
     * Messenger on Activity side, used for sending messages back to Activity
     */
    Messenger mResponseMessenger = null;


    // Debugging
    private static final String TAG = "ConnectionService";
    private static final boolean D = true;

    public static TextToSpeech tts;

    // Member fields

    public ConnectThread mConnectThread;
    public ConnectedThread mConnectedThread;
    private byte[] digest;

    public int mState;
    // Constants that indicate the current connection state

    public static final int STATE_DISCONNECTED = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    public int mSubState;
    public static final int SUBSTATE_GENERAL_RECEIVE = 5;
    public static final int SUBSTATE_SENDPHOTO = 6;
    public static final int SUBSTATE_SENDJSON = 7;

    static final int HMGT = 0;
    static final int RGT = 1;

    public int backgroundColor= R.color.background_color;

    public String myIP="";
    public int myIP_int;


    public  Boolean mStop=false;
    public final IBinder mBinder = new MyBinder();
    public class GoConnect implements Runnable {
        @Override
        public void run() {


            //mStop==false

            if( Looper.myLooper() == Looper.getMainLooper())
                Log.e("InitService", "...................................ERROR! You are in the main thread" );
            else
                Log.d("InitService", "...................................Safe Separate thread");


            mSubState = SUBSTATE_GENERAL_RECEIVE;

            myIP=getMyIP();

            Log.d("MessengerService", "Service started");
            System.out.println("wifiService running");

            connect();

        }

    }

public String getMyIP(){

    WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
    WifiInfo wifiInfo = wm.getConnectionInfo();
    int ip = wifiInfo.getIpAddress();
    myIP_int=ip;

    String ipString = String.format(
            "%d.%d.%d.%d",
            (ip & 0xff),
            (ip >> 8 & 0xff),
            (ip >> 16 & 0xff),
            (ip >> 24 & 0xff));
   return ipString;

}
    @Override
    public void onDestroy(){
        super.onDestroy();
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null)  mConnectedThread = null;

        stopSelf();

    }

    @Override
    public void onCreate() {
        super.onCreate();


        // Even though the text-to-speech engine is only used in response to a menu action, we
        // initialize it when the application starts so that we avoid delays that could occur
        // if we waited until it was needed to start it up.
       tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // Do nothing.
            }
        });


    }

    /**
     * Stop all threads
     */
    public synchronized void stopWifiService() {

       // if (mConnectThread != null) mConnectThread.cancel();
       // if (mConnectedThread != null) { mConnectedThread.CloseSocket(); mConnectedThread = null;}



        //Log.d("Wifi_Service", "........................................Wifi_Service STOPED! ");


        //stopSelf();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        new Thread(new GoConnect()).start();

        return super.onStartCommand(intent, flags, startId);

    }


    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_TYPE_TEXT:
                    Bundle b = msg.getData();
                    if (b != null) {
                        Log.d("MessengerService",
                                "Service received message MESSAGE_TYPE_TEXT with: " + b.getCharSequence("data"));
                        sendToActivity("Who's there? You wrote: " + b.getCharSequence("data"));
                    } else {
                        Log.d("MessengerService", "Service received message MESSAGE_TYPE_TEXT with empty message");
                        sendToActivity("Who's there? Speak!");
                    }
                    break;
                case MESSAGE_TYPE_REGISTER:
                    Log.d("MessengerService", "Registered Activity's Messenger.");
                    mResponseMessenger = msg.replyTo;
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("MessengerService", "Binding messenger...");
        //return mMessenger.getBinder();
       return mBinder;
    }

    public class MyBinder extends Binder {
        public WifiService getService() {
            return WifiService.this;
        }
    }

    public static void Speek(String txt) {
        if (tts != null) {
            //if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            } else {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "helloID");
                tts.speak(txt, TextToSpeech.QUEUE_FLUSH, map);
            }
        }
    }

    /**
     * Sends message with text stored in bundle extra data ("data" key).
     *
     * @param text
     *            text to send
     */
    void sendToActivity(CharSequence text) {
        if (mResponseMessenger == null) {
            Log.d("MessengerService", "Cannot send message to activity - no activity registered to this service.");
        } else {
            Log.d("MessengerService", "Sending message to activity: " + text);
            Bundle data = new Bundle();
            data.putCharSequence("data", text);
            Message msg = Message.obtain(null, MESSAGE_TYPE_TEXT);
            msg.setData(data);
            try {
                mResponseMessenger.send(msg);
            } catch (RemoteException e) {
                // We always have to trap RemoteException (DeadObjectException
                // is thrown if the target Handler no longer exists)
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends message with text stored in bundle extra data ("data" key).
     *
     * @param msg
     *            msg to send
     */
    void sendToActivity(Message msg ) {
        if (mResponseMessenger == null) {
            Log.d("MessengerService", "Cannot send message to activity - no activity registered to this service.");
        } else {
            Log.d("MessengerService", "Sending message to activity" + msg.what);

            /*Bundle data = new Bundle();
            data.putCharSequence("data", text);
            Message msg = Message.obtain(null, MESSAGE_TYPE_TEXT);
            msg.setData(data);*/

            try {
                mResponseMessenger.send(msg);
            } catch (RemoteException e) {
                // We always have to trap RemoteException (DeadObjectException
                // is thrown if the target Handler no longer exists)
                  Log.d("MessengerService", "Cannot send message to activity -  activity NOT exist.");

                e.printStackTrace();
            }
        }
    }



    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
       // mHandler.obtainMessage(MessageType.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();

        Message msg = Message.obtain(null,MessageType.MESSAGE_STATE_CHANGE,state, -1);
        sendToActivity(msg);


    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }


    public synchronized void connect() {
        setState(STATE_CONNECTING);

        if (D) Log.d(TAG, "ConnectThread started forever!!");

        // Cancel any thread attempting to make a connection

        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        // Cancel any thread currently running a connection
        if (mConnectedThread != null)  mConnectedThread = null;

        // Start the thread to connect with the given device

        mConnectThread = new ConnectThread();
        mConnectThread.start();
    }


    public synchronized void connected(Socket mmSocket) {
        if (D) Log.d(TAG, "connected");
        Speek("Haytham Connected");

        if (mConnectThread != null) {
            // mConnectThread.cancel();
            mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            //mConnectedThread.cancel();
            mConnectedThread = null;}

        // Start the thread to manage the connection and perform transmissions



        setState(STATE_CONNECTED);


        //AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //audio.playSoundEffect(Sounds.SUCCESS);

        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();

    }


    public void GazeStream(int i,Boolean b){
        int mm=0;
        switch (i)
        {
            case HMGT:
                if (b) mm = MessageType.toHAYTHAM_StreamGaze_HMGT_START;
                else mm = MessageType.toHAYTHAM_StreamGaze_HMGT_STOP;


                break;

            case RGT:

                if (b) mm = MessageType.toHAYTHAM_StreamGaze_RGT_START;
                else mm = MessageType.toHAYTHAM_StreamGaze_RGT_STOP;


                break;

        }


        try {
            write(mm);
        }
        catch (Exception e){

        }
    }

    public void write(int msg) {
        if (mConnectedThread!=null)
        {
            // Create temporary object
            ConnectedThread r;
            // Synchronize a copy of the ConnectedThread
            synchronized (this) {
                if (mState != STATE_CONNECTED ) return;
                r = mConnectedThread;
            }
            // Perform the write unsynchronized

            r.write(msg);
        }
    }


    public void sendPhoto( byte[] payload)
    {
        if (mConnectedThread!=null) {
            // Create temporary object
            ConnectedThread r;
            // Synchronize a copy of the ConnectedThread
            synchronized (this) {
                if (mState != STATE_CONNECTED) return;
                r = mConnectedThread;
            }
            // Perform the write unsynchronized

            r.sendPhoto(payload);
        }
    }
 /*   public void sendJson( JSONObject json,int msg)
    {
        if (mConnectedThread!=null) {
            // Create temporary object
            ConnectedThread r;
            // Synchronize a copy of the ConnectedThread
            synchronized (this) {
                if (mState != STATE_CONNECTED) return;
                r = mConnectedThread;
            }
            // Perform the write unsynchronized


            r.sendJson(json, msg);
        }
    }*/
    public void sendJson( String json,int msg)
    {
        if (mConnectedThread!=null) {
            // Create temporary object
            ConnectedThread r;
            // Synchronize a copy of the ConnectedThread
            synchronized (this) {
                if (mState != STATE_CONNECTED) return;
                r = mConnectedThread;
            }
            // Perform the write unsynchronized


            r.sendJson(json, msg);
        }
    }

    private int FindHost() {
        int found = 0;
        DatagramSocket socket=null;
        try {
            String msg0 = "Hello Haytham!";
            byte[] byteArray = msg0.getBytes();// Utils.intToByteArray(1);
            if (socket == null || !socket.isBound())  {
                socket = new DatagramSocket(Constants.SERVER_PORT);
            }

            socket.setReuseAddress(true);
            DatagramPacket packet = new DatagramPacket(byteArray, byteArray.length, InetAddress.getByName("255.255.255.255"), Constants.SERVER_PORT);
            socket.setBroadcast(true);
            socket.send(packet);
            Log.e(TAG, "Broadcasting from " + myIP);


            socket.setSoTimeout(3000);
            byte[] receiveData = new byte[1000];
            DatagramPacket receivePacket;// = new DatagramPacket(receiveData, receiveData.length);
            String hostIP = "";

            while (!mStop) {
                receivePacket = new DatagramPacket(receiveData, receiveData.length);

                try {


                    socket.receive(receivePacket);
                    hostIP = new String(receiveData, 0, receivePacket.getLength());

                    // check received data...

                    Log.e(TAG, "receivePacket  getAddress" + receivePacket.getAddress());

                    if (!hostIP.startsWith("Hello Haytham!")) {
                        Log.e(TAG, "compare " + (receivePacket.getAddress() != InetAddress.getByName(myIP)));
                        break;
                    }

                } catch (SocketTimeoutException e) {
                    //resend
                    socket.send(packet);
                    continue;
                }


            }


            Constants.SERVER_IP = InetAddress.getByName(hostIP);
            found = 1;
            //if (socket.isConnected()) {
                socket.disconnect();
                socket.close();
            //}
        } catch (IOException e2) {
            Log.e(TAG, ".....                    NO UDP. Host NOT FOUND", e2);

            found = 0;
        }

        return found;

    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {

        private Socket tempSocket=null;
        Boolean searching;

        public ConnectThread() {

            setName("ConnectThread");
            searching=true;
            while (searching) {
                if (Constants.UDPBroadcasting) {
                    //Find the host in the network and get its IP
                     /*
                    int found = FindHost();

                    if (found == 1 ) {


                        try {
                            // mmSocket = new Socket();
                            // mmSocket.connect(new InetSocketAddress(Constants.SERVER_IP, Constants.SERVER_PORT), 5000);
                            //mmSocket.setSoTimeout(5000);


                            tempSocket = new Socket(Constants.SERVER_IP, Constants.SERVER_PORT); // connect to the server

                        } catch (IOException e) {
                            // Close the socket
                            cancel();
                            return;
                        }
                    }*/
                } else {


                    try {
                        tempSocket = new Socket();

                        tempSocket.connect(new InetSocketAddress(Constants.directServerIP, Constants.SERVER_PORT), 5000);
                        tempSocket.setSoTimeout(5000);

                    } catch (IOException e) {
                        // Close the socket
                        // cancel();
                        //return;
                    }


                }


                if (tempSocket.isConnected()) {
                    searching=false;
                    // Start the connected thread
                    connected(tempSocket);
                    cancel();
                    return;
                }
            }
        }

        public void cancel() {
            searching=false;
            try {

                tempSocket = null;
            } catch (Exception e) {
                Log.e(TAG, ".....................tempSocket ending failed", e);
            }

            // Reset the ConnectThread because we're done
            synchronized (this) {
                mConnectThread = null;
            }
        }




    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    public class ConnectedThread extends Thread {
        public Socket mmSocket;
        public OutputStream mmOutStream;
        public InputStream mmInStream;

        private DatagramSocket mmUDPSocket;
        private DatagramSocket mmUDPSocket_listening;


        public ConnectedThread(Socket socket) {
            setName("ConnectedThread");


            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;


            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the socket input and output streams
            try {
                mmSocket.setSoTimeout(0);
                //2. get Input and Output streams
                tmpOut = socket.getOutputStream();
                tmpOut.flush();
                tmpIn = socket.getInputStream();

            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

try {
    Thread.sleep(2000);
    WriteUDP("Hello Haytham!");
}catch (Exception ex)
{}
            //a seperate thread for receiving the gaze data via UDP.
            new  Thread(new Runnable() {
                public void run() {



                    byte[]  buffer = new byte[Constants.MSG_SIZE];
                    DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

                    while (mConnectedThread!=null) {

                        try {
                            //UDP
                            mmUDPSocket_listening = new DatagramSocket(Constants.SERVER_PORT_UDP_GAZE);
                            mmUDPSocket_listening.setSoTimeout(1000);
                            receivePacket = new DatagramPacket(buffer, buffer.length);
                            mmUDPSocket_listening.receive(receivePacket);
                            if (mSubState == SUBSTATE_GENERAL_RECEIVE && receivePacket.getLength()==12) {
                                // Send the obtained bytes to the UI Activity
                                // mHandler.obtainMessage(MessageType.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                                Message msg = Message.obtain(null, MessageType.MESSAGE_READ, receivePacket.getLength(), -1, buffer);
                                sendToActivity(msg);
                            }
                        } catch (IOException e) {

                               // Log.e(TAG, "UDP GAZE recieve ERROR!!!!!!!!!!!!!", e);


                        }finally {
                            if (mmUDPSocket_listening != null) {
                                mmUDPSocket_listening.disconnect();
                                mmUDPSocket_listening.close();
                            }
                        }
                    }


                }}).start();
        }

        public void run() {

            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[Constants.MSG_SIZE];

            // Keep listening to the InputStream while connected
            while (true) {//mStop==false) {
                int bytes = 0;
                try {


                    buffer = new byte[16];//digest size is 16 even though MSG size is 12
                    bytes = mmInStream.read(buffer, 0, buffer.length);


                    if (bytes == -1)
                        throw new IOException("***********************Server missing!!");

                } catch (IOException e) {
                    Log.d(TAG, "disconnected");
                    cancel();
                    break;
                }

                if (mSubState == SUBSTATE_GENERAL_RECEIVE && bytes==12) {



                    // Send the obtained bytes to the UI Activity
                    // mHandler.obtainMessage(MessageType.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    Message msg = Message.obtain(null, MessageType.MESSAGE_READ, bytes, -1, buffer);
                    sendToActivity(msg);

                } else if (mSubState == SUBSTATE_SENDPHOTO) {


                    if (bytes == 16 && Utils.digestMatch(digest, buffer)) {

                        Log.v(TAG, "Digest matched OK.  Data was received OK.");
                        //mHandler.sendEmptyMessage(MessageType.DATA_SENT_OK);
                        Message msg = Message.obtain(null, MessageType.DATA_SENT_OK);
                        sendToActivity(msg);

                    } else {
                        Log.e(TAG, "Digest did not match.  Might want to resend.");
                        //mHandler.sendEmptyMessage(MessageType.DIGEST_DID_NOT_MATCH);
                        Message msg = Message.obtain(null, MessageType.DIGEST_DID_NOT_MATCH);
                        sendToActivity(msg);

                    }
                    mSubState = SUBSTATE_GENERAL_RECEIVE;


                }
            }


        }


        public void write(int msg) {


            byte[] toHaythm = new byte[Constants.MSG_SIZE];
            byte[] msgArray = Utils.intToByteArray(msg);

            // 12 bytes msg format iiiixxxxyyyy
            java.lang.System.arraycopy(msgArray, 0, toHaythm, 0, msgArray.length);
            java.lang.System.arraycopy(msgArray, 0, toHaythm, 4, msgArray.length);
            java.lang.System.arraycopy(msgArray, 0, toHaythm, 8, msgArray.length);


            try {
                mmOutStream.write(toHaythm);
                mmOutStream.flush();
                // Share the sent message back to the UI Activity
//                mHandler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1, buffer)
//                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
                cancel();
            }

        }
        public void write(String msg) {

            try {

                byte[] buffer = new byte[Constants.MSG_SIZE];
                byte[] array = msg.getBytes();
                if (array.length > buffer.length || array.length < 1) {
//Not acceptable!! don't send
                } else {    //Converting all msgs to 9 bytes array
                    java.lang.System.arraycopy(array, 0, buffer, 0, array.length);
                    mmOutStream.write(buffer);
                    mmOutStream.flush();
                }


            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
                cancel();
            }

        }
        public void WriteUDP(String msg)
        {

            try {
                mmUDPSocket = new DatagramSocket(Constants.SERVER_PORT_UDP);

                //send your IP to Haytham
                byte[] sendData = msg.getBytes();
                InetAddress myIP_inet=InetAddress.getByName(Constants.directServerIP);

                mmUDPSocket.setReuseAddress(true);
                DatagramPacket packet = new DatagramPacket(sendData, sendData.length,myIP_inet, Constants.SERVER_PORT_UDP);

                mmUDPSocket.send(packet);


            } catch (IOException e) {
                Log.e(TAG, "*************************************************************** \n", e);
            }  finally {
                if (mmUDPSocket!=null) {
                    mmUDPSocket.disconnect();
                    mmUDPSocket.close();
                    //mmUDPSocket = null;
                }
            }

        }

/*
        public void write(byte[] buffer) {

            try {
                mmOutStream.write(buffer);
                mmOutStream.flush();
                // Share the sent message back to the UI Activity
//                mHandler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1, buffer)
//                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
                cancel();
            }

        }
*/






        public void sendPhoto( byte[] payload){


            Log.v(TAG, "Handle received data to send");
            mSubState=SUBSTATE_SENDPHOTO;
            try {
                //mHandler.sendEmptyMessage(MessageType.SENDING_DATA);
                Message msg = Message.obtain(null,MessageType.SENDING_DATA);
                sendToActivity(msg);

                write(MessageType.toHAYTHAM_HeadderComming);//tell the server to wait for the headder
                // Send the header control first
                mmOutStream.write(Constants.HEADER_MSB);
                mmOutStream.write(Constants.HEADER_LSB);

                // write size
                mmOutStream.write(Utils.intToByteArray(payload.length));

                // write digest
                digest = Utils.getDigest(payload);
                mmOutStream.write(digest);
                Log.v(TAG, "digest length" + digest.length);

                // now write the data
                mmOutStream.write(payload);
                mmOutStream.flush();

                Log.v(TAG, "Photo sent.  Waiting for return digest as confirmation");

          } catch (Exception e) {
                Log.e(TAG, e.toString());

                cancel();
            }

        }


       /* public void sendJson( JSONObject json,int msg){


            Log.v(TAG, "Handle received data to send");
            mSubState=SUBSTATE_SENDJSON;
            try {


                //json to byte array
                byte[] payload = json.toString().getBytes("utf-8");


                write(msg);//tell the server to wait for the headder

                // write size
                mmOutStream.write(Utils.intToByteArray(payload.length));



                // now write the data
                mmOutStream.write(payload);
                mmOutStream.flush();

            } catch (Exception e) {
                Log.e(TAG, e.toString());

                cancel();
            }
            mSubState=SUBSTATE_GENERAL_RECEIVE;
        }
*/
        public void sendJson( String json,int msg){


            Log.v(TAG, "Handle received data to send");
            mSubState=SUBSTATE_SENDJSON;
            try {

                //json to byte array
                byte[] payload = json.getBytes("utf-8");


                write(msg);//tell the server to wait for the headder

                // write size
                mmOutStream.write(Utils.intToByteArray(payload.length));



                // now write the data
                mmOutStream.write(payload);
                mmOutStream.flush();

            } catch (Exception e) {
                Log.e(TAG, e.toString());

                cancel();
            }
            mSubState=SUBSTATE_GENERAL_RECEIVE;
        }

        public void cancel() {
            Speek("Haytham disConnected");
            mSubState=SUBSTATE_GENERAL_RECEIVE;

            CloseSocket();

            // Reset the ConnectThread because we're done
            //  synchronized (this) {

               mConnectedThread = null;
            // }

          if(!mStop)  connect();
          else {
              stopSelf();
          }

        }
        private void CloseSocket(){
           try {
          mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "close() of connected socket failed", e);
        }
            try {
                mmSocket=null;
            } catch (Exception e) {
                Log.e(TAG, " connected socket null failed", e);
            }}
    }


}
