package mldelaro.asu.edu.quadcoptercontroller;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import mldelaro.asu.edu.quadcoptercontroller.client.udp.UdpClient;

public class MainActivity extends AppCompatActivity {

    private UdpClient udpClient;
    private String runningTxMessage;
    private boolean didStartTcpConnection;
    private String runningTcpHostName;
    private String runningTcpHostPort;

    private Button btn_stopTcpConnect;
    private Button btn_startTcpConnect;
    private Button btn_txForward;
    private Button btn_txBackward;
    private Button btn_txLeft;
    private Button btn_txRight;
    private Button btn_txUp;
    private Button btn_txDown;
    private Button btn_txTest;
    private Button btn_txFStart;
    private Button btn_txStart;
    private Button btn_txStop;

    private EditText editTxt_txClientMessage;
    private EditText editTxt_rxHostMessage;
    private EditText editTxt_hostName;
    private EditText editTxt_hostPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        didStartTcpConnection = true;
        runningTxMessage = "{\"command\":\"S\"}";

        btn_stopTcpConnect = (Button) findViewById(R.id.btn_stopTcpConnect);
        btn_startTcpConnect = (Button) findViewById(R.id.btn_startTcpConnect);
        btn_txForward = (Button) findViewById(R.id.btn_txForward);
        btn_txBackward = (Button) findViewById(R.id.btn_txBackward);
        btn_txLeft = (Button) findViewById(R.id.btn_txLeft);
        btn_txRight = (Button) findViewById(R.id.btn_txRight);
        btn_txUp = (Button) findViewById(R.id.btn_txUp);
        btn_txDown = (Button) findViewById(R.id.btn_txDown);
        btn_txTest = (Button) findViewById(R.id.btn_txTest);
        btn_txFStart = (Button) findViewById(R.id.btn_txFStart);
        btn_txStart = (Button) findViewById(R.id.btn_txStart);
        btn_txStop = (Button) findViewById(R.id.btn_txStop);

        editTxt_txClientMessage = (EditText) findViewById(R.id.editTxt_txClientMessage);
        editTxt_rxHostMessage = (EditText) findViewById(R.id.editTxt_rxHostMessage);
        editTxt_hostName = (EditText) findViewById(R.id.editTxt_hostName);
        editTxt_hostPort = (EditText) findViewById(R.id.editTxt_hostPort);

        String[] perms = {"android.permission.INTERNET"};
        int permsRequestCode = 200;
        requestPermissions(perms, permsRequestCode);

        /* SET BUTTON LISTENERS */
        btn_startTcpConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runningTcpHostPort = editTxt_hostPort.getText().toString();
                runningTcpHostName = editTxt_hostName.getText().toString();

                if(udpClient == null) {

                    int udpHostPort = 0;
                    try {
                         udpHostPort = Integer.valueOf(runningTcpHostPort);
                    } catch (NumberFormatException ex) {
                        Toast toast = Toast.makeText(getApplicationContext(), "Failed to convert '" + runningTcpHostPort + "' to a number ", Toast.LENGTH_SHORT);
                        toast.show();
                        udpHostPort = -1;
                    }
                    if (Integer.valueOf(runningTcpHostPort) < 1025) {
                        Toast toast = Toast.makeText(getApplicationContext(), "Please bind to a port larger than 1024", Toast.LENGTH_SHORT);
                        toast.show();
                    } else if (Integer.valueOf(runningTcpHostPort) > 65534) {
                        Toast toast = Toast.makeText(getApplicationContext(), "Please bind to a port less than 65535", Toast.LENGTH_SHORT);
                        toast.show();
                    } else {
                        Log.d("mdlog-Main", "Starting UDP connection...");
                        runningTcpHostName = editTxt_hostName.getText().toString();
                        runningTcpHostPort = editTxt_hostPort.getText().toString();

                        Toast toast = Toast.makeText(getApplicationContext(), "Connecting to " + runningTcpHostName + ":" + runningTcpHostPort, Toast.LENGTH_SHORT);
                        toast.show();
                        new UdpConnectionTask().execute(runningTcpHostName, runningTcpHostPort);
                    }
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), "Connection already exists to " + runningTcpHostName + ":" + runningTcpHostPort, Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        btn_stopTcpConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTxt_txClientMessage.setText("---");
                if (udpClient != null) {
                    Toast toast = Toast.makeText(getApplicationContext(), "Stopping connection to " + runningTcpHostName + ":" + runningTcpHostPort, Toast.LENGTH_SHORT);
                    toast.show();
                    Log.d("mdlog-Main", "Stopping UDP connection...");
                    udpClient.stopClient();
                    udpClient = null;
                }
            }
        });

        btn_txForward.setOnTouchListener(getPressedListenerForValue("F"));
        btn_txBackward.setOnTouchListener(getPressedListenerForValue("B"));
        btn_txLeft.setOnTouchListener(getPressedListenerForValue("L"));
        btn_txRight.setOnTouchListener(getPressedListenerForValue("R"));
        btn_txUp.setOnTouchListener(getPressedListenerForValue("U"));
        btn_txDown.setOnTouchListener(getPressedListenerForValue("D"));
        btn_txTest.setOnTouchListener(getPressedListenerForValue("T")); //test
        btn_txFStart.setOnTouchListener(getPressedListenerForValue("Y")); //fstart
        btn_txStart.setOnTouchListener(getPressedListenerForValue("V")); //start
        btn_txStop.setOnTouchListener(getPressedListenerForValue("W")); //stop

        editTxt_txClientMessage.setText(runningTxMessage);
    }

    public View.OnTouchListener getPressedListenerForValue(final String tcpMessage) {
        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        runningTxMessage = "{\"command\":\"" + tcpMessage + "\"}";
                        editTxt_txClientMessage.setText(runningTxMessage);
                        break;
                    case MotionEvent.ACTION_UP:
                        runningTxMessage = "{\"command\":\"S\"}";
                        editTxt_txClientMessage.setText(runningTxMessage);
                        break;
                }
                if(udpClient != null) {
                    udpClient.sendMessage(runningTxMessage);
                }
                return false;
            }
        };
    }

    public class UdpConnectionTask extends AsyncTask<String, String, UdpClient> {
        @Override
        protected UdpClient doInBackground(String... params) {
            Log.d("mdlog-UdpConnectionTask", "Creating UDP Client...");
            udpClient = new UdpClient(params[0], Integer.parseInt(params[1]), new UdpClient.OnUdpMessageReceived() {
                @Override
                public void udpMessageReceived(String message) {
                    Log.d("mdlog-UdpConnectionTask", "Receiving UDP Message: " + message);
                    publishProgress(message);
                }
            });
            udpClient.udpClientDriver();
            Log.d("mdlog-UdpConnectionTask", "Did create UDP Client...");
            return null;
        }

        @Override
        protected void onProgressUpdate(String... params) {
            super.onProgressUpdate(params);
            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonParser jp = new JsonParser();
                JsonElement je = jp.parse(params[0].trim());
                String prettyJsonString = gson.toJson(je);
                editTxt_rxHostMessage.setText(prettyJsonString);
            } catch(Exception ex) {
                editTxt_rxHostMessage.setText(params[0]);
                Log.d(this.getClass().getCanonicalName(), "Received malformed json from host: " + params[0]);
            }
        }
    }

    private final int REQUEST_PERMISSION_INTERNET=200;
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_INTERNET:
                if (grantResults.length > 0
                        && grantResults[0] == this.getApplicationContext().getPackageManager().PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(MainActivity.this, "Internet Permissions Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }
}
