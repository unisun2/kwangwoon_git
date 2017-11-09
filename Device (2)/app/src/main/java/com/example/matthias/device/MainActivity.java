package com.example.matthias.device;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Image;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends Activity implements DataApi.DataListener, SensorEventListener,
        MessageApi.MessageListener, NodeApi.NodeListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    boolean left = false, right = false, up = false, down = false;
    private String TAG = "MainActivity";
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ImageView leftImage, rightImage, upImage, downImage;

    /*Google Play Service API 객체*/
    private GoogleApiClient         mGoogleApiClient;

    public TextView mTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);

                leftImage = (ImageView) stub.findViewById(R.id.left);
                rightImage = (ImageView) stub.findViewById(R.id.right);
                upImage = (ImageView) stub.findViewById(R.id.up);
                downImage = (ImageView) stub.findViewById(R.id.down);

            }
        });

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        System.out.println("onCreate");

        /*Google Play Service 객체를 Wearable 설정으로 초기화*/
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    public void onClickButton(View v) {
        String data = "What's your name?";
        System.out.println("connected?");
        Send(data);
        mTextView.setText("changed?");
    }

    protected void onStart() {
        super.onStart();

        System.out.println("onStart");

        /*Google Play Service 접속*/
        //if(!mGoogleApiClient.isConnected()){
        mGoogleApiClient.connect();
        //}
    }

    @Override
    protected void onStop() {
        System.out.println("onStop");
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();

        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        System.out.println("onConnected");
        /*Google Play Service 접속 되었을 경우 호출*/
        /*Data 수신을 위한 리스너 설정*/
        Log.d(TAG, "onConnected");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        System.out.println("onConnectionFailed");
        /*Google Play Service 접속 실패했을 때 호출*/
        Log.d(TAG, "onConnectionFailed");
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.NodeApi.removeListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        System.out.println("onConnectedSuspended");
        /*Google Play Service 접속 일시정지 됐을 때 호출*/
        Log.d(TAG, "onConnectionSuspended");
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.NodeApi.removeListener(mGoogleApiClient, this);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        System.out.println("onMessageReceived");

        mTextView.setText(messageEvent.getPath());
        /*메시지가 수신되면 호출*/
    }

    @Override
    public void onPeerConnected(Node node) {
        /*Wearable 페어링 되면 호출*/
    }

    @Override
    public void onPeerDisconnected(Node node) {
        /*Wearable 페어링 해제되면 호출*/
    }

    @Override
    public void onDataChanged(DataEventBuffer deb) {
        // nothing
    }

    public void Send(String sdata) {
        byte[] length = {100, 100};
        System.out.println("Send");
        Wearable.MessageApi.sendMessage(mGoogleApiClient, "TEST", sdata, length);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Log.d("test", "" + event.values[1]);

        if (event.values[0] >= 8) {
            left = true;
            leftImage.setVisibility(View.VISIBLE);
        }

        if (event.values[0] < 5 && left == true) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            left = false;
            // vibrator.vibrate(100);

            Log.d("test", "LEFT");
            Send("LEFT");

            leftImage.setVisibility(View.INVISIBLE);
        }

        if (event.values[0] <= -8) {
            right = true;
            rightImage.setVisibility(View.VISIBLE);
        }

        if (event.values[0] > -5 && right == true) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            right = false;
            // vibrator.vibrate(100);

            Log.d("test", "RIGHT");
            Send("RIGHT");

            rightImage.setVisibility(View.INVISIBLE);
        }

        if (event.values[1] <= -8) {
            up = true;
            upImage.setVisibility(View.VISIBLE);
        }

        if (event.values[1] > -5 && up == true) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            up = false;
            // vibrator.vibrate(100);

            Log.d("test", "UP");
            Send("UP");

            upImage.setVisibility(View.INVISIBLE);
        }

        if (event.values[1] >= 8) {
            down = true;
            downImage.setVisibility(View.VISIBLE);
        }

        if (event.values[1] < 5 && down == true) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            down = false;
            // vibrator.vibrate(100);

            Log.d("test", "DOWN");
            Send("DOWN");

            downImage.setVisibility(View.INVISIBLE);
        }
    }
}