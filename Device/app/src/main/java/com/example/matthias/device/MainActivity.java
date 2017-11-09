package com.example.matthias.device;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

// 메인 액티비티 클래스 선언부

public class MainActivity extends AppCompatActivity implements DataApi.DataListener,
        MessageApi.MessageListener, NodeApi.NodeListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, Serializable {

    private String TAG = "MainActivity"; // 무시
    private RadioGroup radioGroup; // 라디오버튼 그룹을 묶는 용도

    /*Google Play Service API 객체*/
    private GoogleApiClient         mGoogleApiClient;

    /* UI */
    private EditText editText;
    private TextView mTextView, watchText, raspText;
    private Button   button, sendButton, serverButton;
    private int saveState = 0;
    private boolean waitForUser = false, temperCheck = false;
    private StringBuilder saveAct, serverAddress;
    private RadioButton radioButton;
    private ImageView watchImage, raspImage;
    private TextView last_command_str, tempCheck;
    private long m_ExitCondition = 0L;
    private EditText serverAddr, serverPort, serverPassword;
    private View dialogView1, dialogView2;
    private int serverPortNumber = 0;
    private BufferedReader reader;
    private PrintWriter writer;
    private Socket sock;
    private boolean check = false, connecting = false;
    private String  tempStr;
    private boolean connectionCheck = true;
    private View dialogView;
    private EditText addrPart, portPart, passwdPart;

    private ImageView light0, light1;


    private TextView Cur_Acc_Text;
    private String Saved_pass;

    private TextView house_relay;

    boolean flag_up, flag_down, flag_left, flag_right;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* 라디오 버튼 연결 */
        radioButton = (RadioButton) findViewById(R.id.radio1);
        radioButton.setChecked(true);
        saveState = R.id.radio1;

        /* UI 연결 */
        saveAct = new StringBuilder("INITIALIZED");
        serverAddress = new StringBuilder("INITIALIZED_ADDRESS");

        last_command_str = (TextView) findViewById(R.id.command);
        last_command_str.setText("");

        watchImage= (ImageView) findViewById(R.id.watchImg);
        raspImage = (ImageView) findViewById(R.id.raspImg);

        watchText = (TextView) findViewById(R.id.watchConn);
        raspText = (TextView) findViewById(R.id.raspConn) ;

        Cur_Acc_Text = (TextView) findViewById(R.id.Cur_authorization);

        light0 = (ImageView) findViewById(R.id.light0);
        light1 = (ImageView) findViewById(R.id.light1);

        house_relay = (TextView) findViewById(R.id.house_relay);

        flag_up=flag_down= flag_left= flag_right = false;

        /* UI 연결 및 액션 리스너 설정 */
        serverButton = (Button) findViewById(R.id.serverSetting);
        serverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* 팝업창 속성 설정 및 인플레이트 */
                dialogView1 = (View) View.inflate(MainActivity.this, R.layout.dialog, null);
                AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);

                dlg.setTitle("Server Configuration");
                dlg.setView(dialogView1);

                /* 확인 버튼 클릭에 대한 리스너 설정 */
                dlg.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        /* UI 연결 */
                        serverAddr = (EditText) dialogView1.findViewById(R.id.serverAddr);
                        serverPort = (EditText) dialogView1.findViewById(R.id.serverPort);
                        serverPassword = (EditText) dialogView1.findViewById(R.id.password);

                        Saved_pass = serverPassword.getText().toString();

                        Log.d("test_serverconfig",Saved_pass);

                        /* 네트워크 설정 스레드 생성 */
                        Runnable r = new setUpNetworking();
                        Thread serverConnection = new Thread(r);

                        /* 이하 예외처리 */
                        if(connecting) {
                            Toast.makeText(MainActivity.this, "Just waiting for connection you required before...", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if(check == true) {
                            check = false;
                            raspImage.setImageResource(R.drawable.disconn);
                            raspText.setText("Disconnected");

                            try {
                                reader.close();
                                writer.close();
                                sock.close();
                                serverPortNumber = 0;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                raspImage.setImageResource(R.drawable.conn);
                                raspText.setText("Connecting...");
                            }
                        });

                        /* 네트워크 설정 스레드 실행 */
                        serverConnection.start();
                    }
                });

                dlg.setNegativeButton("Cancel", null);
                dlg.show();
            }
        });

        /* 명령 발송 UI 연결 */
        sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                waitForUser = true;
                decideOperation(saveAct.toString());
            }
        });

        /* 라디오버튼을 그룹으로 묶음 */
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int check) {
                if(check == R.id.radio1) {
                    sendButton.setEnabled(false);
                    saveState = R.id.radio1;
                }

                else {
                    sendButton.setEnabled(true);
                    saveState = R.id.radio2;
                }
            }
        });

        System.out.println("onCreate");

        /*Google Play Service 객체를 Wearable 설정으로 초기화*/
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();


        /* 온도와 습도를 확인하는 UI 설정 */
        button = (Button) findViewById(R.id.temperCheck);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(check) {
                    temperCheck = true;

                    /* 온도와 습도에 관한 내용을 얻어오는 스레드 생성 및 실행 */
                    Runnable r1 = new checkingTemp();
                    Thread t1 = new Thread(r1);

                    t1.start();

                    /* 팝업창 설정 및 인플레이트 */
                    dialogView2 = (View) View.inflate(MainActivity.this, R.layout.temperature, null);
                    AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);
                    dlg.setTitle("Circumstance information");
                    dlg.setView(dialogView2);
                    dlg.setCancelable(false);
                    dlg.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            temperCheck = false;
                        }
                    });
                    dlg.show();
                    tempCheck = (TextView) dialogView2.findViewById(R.id.circums);
                }
                else {
                    Toast.makeText(MainActivity.this, "You need to connect to server first!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /* 우측 상단의 메뉴 버튼 */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    /* 우측 상단의 메뉴 버튼에서 무언가가 눌렸을 경우 */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.register: { /* 등록하기를 눌렀을 경우 */
                /* 팝업창 설정 및 인플레이트 */
                dialogView = (View) View.inflate(MainActivity.this, R.layout.dialognew, null);
                AlertDialog.Builder dlg = new AlertDialog.Builder(MainActivity.this);

                dlg.setTitle("Register new device");
                dlg.setView(dialogView);

                /* 확인 버튼을 눌렀을 경우 리스너 실행 */
                dlg.setPositiveButton("Register", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        /* UI 연결 */
                        addrPart = (EditText) dialogView.findViewById(R.id.newServerAddr);
                        portPart = (EditText) dialogView.findViewById(R.id.newServerPort);
                        passwdPart = (EditText) dialogView.findViewById(R.id.newPassword);

                        /* 새로운 장치를 등록하는 스레드 생성 및 실행 */
                        Runnable r1 = new registerNewDevice();
                        Thread t1 = new Thread(r1);

                        t1.start();
                    }
                });
                dlg.setNegativeButton("Cancel", null);
                dlg.show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onStart() {
        super.onStart();

        System.out.println("onStart");

        /*Google Play Service 접속*/
        if(!mGoogleApiClient.isConnected()){
            mGoogleApiClient.connect();
            Log.d("test","mGoogleApiClient.connect();");
        }
    }

    // 안드로이드웨어와의 접속 종료
    @Override
    protected void onStop() {
        System.out.println("onStop");
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();

        watchText.setText("Disconnected");
        watchImage.setImageResource(R.drawable.disconn);

        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        System.out.println("onConnected");
        /*Google Play Service 접속 되었을 경우 호출*/
        /*Data 수신을 위한 리스너 설정*/
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);

        watchText.setText("Connected");
        watchImage.setImageResource(R.drawable.conn);
        Log.d("test","onConnected(Bundle bundle)");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        System.out.println("onConnectionFailed");
        /*Google Play Service 접속 실패했을 때 호출*/
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        Log.d("test","onConnectionFailed");
    }

    @Override
    public void onConnectionSuspended(int i) {
        System.out.println("onConnectionSuspended");
        /*Google Play Service 접속 일시정지 됐을 때 호출*/
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        Log.d("test","onConnectionSuspended");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        /*Google Play Service 데이터가 변경되면 호출*/

        // nothing
    }

    /* 웨어러블 기기에서 명령이 수신될 경우 호출되는 메소드 */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        /* 메시지를 수신함 */
        String args = messageEvent.getPath();

        Log.d("test", args + "fromwatch");

        /* 메시지에 관한 여러 설정들 */
        last_command_str.setText(args);

        saveAct.delete(0, 12);
        saveAct.replace(0, 12, args);
        decideOperation(args);
    }

    @Override
    public void onPeerConnected(Node node) {
        /*Wearable 페어링 되면 호출*/
    }

    @Override
    public void onPeerDisconnected(Node node) {
        /*Wearable 페어링 해제되면 호출*/
    }

    /* 수신된 메시지를 처리하기 위한 메소드 */
    public void decideOperation(String act) {
        /* 라디오 버튼 또는 전송 버튼 클릭에 대한 내용 */
        if((saveState == R.id.radio1 || waitForUser == true)) {
            if(waitForUser == true)
                waitForUser = false;

            /* 메시지 발송 */
            if(saveAct.toString().equals("LEFT") || saveAct.toString().equals("RIGHT") || saveAct.toString().equals("UP") || saveAct.toString().equals("DOWN")) {
                if(check == true && temperCheck == false) {

                    if(saveAct.toString().equals("LEFT") || flag_up){
                        writer.write("UP");
                        writer.flush();

                        flag_down=flag_left=flag_right=flag_up = false;
                    }
                    else if(saveAct.toString().equals("UP") || flag_left){
                        writer.write("UP");
                        writer.flush();

                        flag_down=flag_left=flag_right=flag_up = false;
                    }
                    else if(saveAct.toString().equals("DOWN") || flag_right){
                        writer.write("DOWN");
                        writer.flush();

                        flag_down=flag_left=flag_right=flag_up = false;
                    }
                    else if(saveAct.toString().equals("RIGHT") || flag_down){
                        writer.write("DOWN");
                        writer.flush();

                        flag_down=flag_left=flag_right=flag_up = false;
                    }
                    else if(saveAct.toString().equals("UP")){
                        flag_down=flag_left=flag_right=flag_up = false;
                        flag_up=true;
                    }
                    else if(saveAct.toString().equals("LEFT")){
                        flag_down=flag_left=flag_right=flag_up = false;
                        flag_left=true;
                    }
                    else if(saveAct.toString().equals("RIGHT")){
                        flag_down=flag_left=flag_right=flag_up = false;
                        flag_right=true;
                    }
                    else if(saveAct.toString().equals("DOWN")){
                        flag_down=flag_left=flag_right=flag_up = false;
                        flag_down=true;
                    }

                }

            }

            saveAct.delete(0, 12);
        }
    }

    /* 뒤로가기 버튼을 눌렀을 경우의 예외처리 */
    @Override
    public void onBackPressed() {
        if(m_ExitCondition != 0 && SystemClock.uptimeMillis() - m_ExitCondition < 3000) {
            connectionCheck = false;

            if(check) {
                writer.write("ENDUP");
                writer.flush();
            }

            finish();
        }
        else {
            Toast.makeText(this, "You can exit if you click again.", Toast.LENGTH_LONG).show();
            m_ExitCondition = SystemClock.uptimeMillis();
        }
    }

    /* 온도 및 습도를 확인하는 스레드 */
    public class checkingTemp implements Runnable {
        @Override
        public void run() {
            /* 해당 명령 발송 */
            writer.write("CIRCUMS");
            writer.flush();
            Log.d("testforcircums","asdf");
            try {
                /* 원하는 내용 수신 */
                while((tempStr = reader.readLine()) != null){
                    Log.d("test", tempStr);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if(tempStr.equals("1")){    // 집의 불이 켜졌다는 신호
                                light0.setVisibility(View.INVISIBLE);
                                light1.setVisibility(View.VISIBLE);
                                house_relay.setText("Lights ON");
                            }
                            else if(tempStr.equals("0")){ // 집의 불이 꺼져있다는 신호
                                light0.setVisibility(View.VISIBLE);
                                light1.setVisibility(View.INVISIBLE);
                                house_relay.setText("Lights OFF");
                            }
                            else if(tempStr.equals("password_ok")){ // 패스워드 입력, 인증완료
                                Cur_Acc_Text.setText("Connection State : OK");
                            }
                            else if(tempStr.equals("password_not")){ // 패스워드 입력, 인증실패
                                Cur_Acc_Text.setText("Connection State : FAILED");
                            }
                            else{ // 온습도 정보

                                tempCheck.setText(tempStr);
                            }

                        }
                    });
                };
            } catch (IOException ie) {

            }


        }
    }

    /* 네트워크 설정 스레드 */
    public class setUpNetworking implements Runnable {
        @Override
        public void run() {
            /* 아래 계속 예외 처리 */
            if(serverAddr.getText().toString().equals(serverAddress.toString()) && serverPort.getText().toString().equals(Integer.toString(serverPortNumber))) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "They are already connected! (Same address)", Toast.LENGTH_SHORT).show();
                        raspImage.setImageResource(R.drawable.disconn);
                        raspText.setText("Disconnected");
                    }
                });

                return;
            }

            serverAddress.delete(0, 20);
            serverAddress.replace(0, 20, serverAddr.getText().toString());

            try {
                serverPortNumber = Integer.parseInt(serverPort.getText().toString());
            } catch (Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "You entered a wrong port number! (Only number)", Toast.LENGTH_SHORT).show();
                        raspImage.setImageResource(R.drawable.disconn);
                        raspText.setText("Disconnected");
                    }
                });

                return;
            }

            try {
                int temp = Integer.parseInt(serverPassword.getText().toString());
            } catch (Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "You need to enter password!", Toast.LENGTH_SHORT).show();
                        raspImage.setImageResource(R.drawable.disconn);
                        raspText.setText("Disconnected");
                    }
                });

                return;
            }

            String[] ipCollect = serverAddress.toString().split("\\.");


            try {
                connecting = true;
                SocketAddress socketAddress = new InetSocketAddress(serverAddress.toString(), serverPortNumber);
                sock = new Socket();
                // sock.setSoTimeout(5000);
                sock.connect(socketAddress, 5000);

                InputStreamReader streamReader = new InputStreamReader(sock.getInputStream());
                reader = new BufferedReader(streamReader);
                writer = new PrintWriter(sock.getOutputStream());

                connecting = false;
                System.out.println("networking established");
                check = true;
                sock.setKeepAlive(true);

                // check password here

                String ciphertext = MakeSHA256(Saved_pass);

                Log.d("cipher_statart",Saved_pass);
                Log.d("cipher_statart",ciphertext);

                writer.write("Pass_:" + ciphertext);
                writer.flush();


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(check == true) {
                            raspImage.setImageResource(R.drawable.conn);
                            raspText.setText("Connected");
                        }
                    }
                });

            } catch (Exception ex) {
                System.out.println("networking not established or connection failed");
                connecting = false;

                try {
                    sock.close();
                } catch (IOException ie) {
                    ie.printStackTrace();
                }

                serverAddress.delete(0, 20);
                serverPortNumber = 0;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        raspImage.setImageResource(R.drawable.disconn);
                        raspText.setText("Disconnected");
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Runnable temp = new closeSocket();
        Thread tempt = new Thread(temp);

        tempt.start();
    }

    public class closeSocket implements Runnable {
        @Override
        public void run() {
            if(connectionCheck) {
                Log.d("TestFor", "is it?");
                try {
                    reader.close();
                    writer.close();
                    sock.close();
                } catch (IOException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }

    public class registerNewDevice implements Runnable {
        @Override
        public void run() {
            if(!Pattern.matches("([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})\\.([0-9]{1,3})", addrPart.getText().toString())) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "You entered a wrong ip form!", Toast.LENGTH_SHORT).show();
                    }
                });

                return;
            }

            try {
                if (Integer.parseInt(portPart.getText().toString()) > 65535) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Port number exceeds the maximum value! (over 65,536)", Toast.LENGTH_SHORT).show();
                        }
                    });

                    return;
                }
            } catch (Exception ie){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "You must enter a port number!", Toast.LENGTH_SHORT).show();
                    }
                });

                return;
            }

            try {
                if (passwdPart.getText().toString().length() < 4) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Password must be 4 characters!", Toast.LENGTH_SHORT).show();
                        }
                    });

                    return;
                }
            } catch (Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "You must enter a password!", Toast.LENGTH_SHORT).show();
                    }
                });

                return;
            }


            return;

        }
    }


    // for SHA256
    public String MakeSHA256(String str){
        String SHA = "";
        try{
            MessageDigest sh = MessageDigest.getInstance("SHA-256");
            sh.update(str.getBytes());
            byte byteData[] = sh.digest();
            StringBuffer sb = new StringBuffer();
            for(int i = 0 ; i < byteData.length ; i++){
                sb.append(Integer.toString((byteData[i]&0xff) + 0x100, 16).substring(1));
            }
            SHA = sb.toString();
        }catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
            SHA = null;
        }
        return SHA;

    }

}

