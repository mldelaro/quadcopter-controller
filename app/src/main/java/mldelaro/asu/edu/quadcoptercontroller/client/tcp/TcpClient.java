package mldelaro.asu.edu.quadcoptercontroller.client.tcp;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * Created by mldof on 5/10/2017.
 */

public class TcpClient {
    private String hostIp;
    private int hostPort;
    private boolean isListening;
    private OnTcpMessageReceived tcpMessageListener;
    private String rxTcpMessage;
    private String runningTxTcpMessage;

    private TcpClient(){};

    public TcpClient(String hostIp, int hostPort, OnTcpMessageReceived listener) {
        this.hostIp = hostIp;
        this.hostPort = hostPort;
        isListening = false;
        tcpMessageListener = listener;
        runningTxTcpMessage = "{\"command\":\"S\"}";
        rxTcpMessage = "";
        Log.d("mldlog-TcpClientDriver", "Initializing tcp client driver: " + hostIp + ":" + hostPort);
    }

    public void sendMessage(String message) {
        Log.d("mldlog-TcpClientDriver", "Setting txMessage: " + runningTxTcpMessage);
        runningTxTcpMessage = message;
    }

    public void stopClient() {
        Log.d("mldlog-TcpClientDriver", "Stopping tcp connection: " + runningTxTcpMessage);
        isListening = false;
        if(runningTxTcpMessage != null) {
            runningTxTcpMessage = null;
        }
        if(tcpMessageListener != null) {
            tcpMessageListener = null;
        }
    }

    public void tcpClientDriver() {
        Log.d("mldlog-TcpClientDriver", "Started tcp client driver...");
        isListening = true;
        try {
            InetAddress hostAddress = InetAddress.getByName(hostIp);
            Socket tcpSocket = new Socket(hostAddress, hostPort);
            PrintWriter tcpTxBuffer = new PrintWriter(tcpSocket.getOutputStream());
            BufferedReader tcpRxBuffer = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            tcpSocket.setSoTimeout(5);
            Log.d("mldlog-TcpClientDriver", "Opened tcp socket...");
            while(isListening) {
                //TX phase
                if(tcpTxBuffer != null) {
                    tcpTxBuffer.print(runningTxTcpMessage);
                    tcpTxBuffer.flush();
                } else {
                    Log.d("mdlog-TcpClientDriver", "Null buffer when sending message: " + runningTxTcpMessage);
                }

                //RX Phase
                rxTcpMessage = tcpRxBuffer.readLine();
                rxTcpMessage.trim();
                Log.d("mdlog-TcpClientDriver", "Receiving message: " + rxTcpMessage);
                tcpMessageListener.tcpMessageReceived(rxTcpMessage);
            }
            tcpSocket.close();
            Log.d("mldlog-TcpClientDriver", "Received Response: " + rxTcpMessage);
        } catch (UnknownHostException ex) {
            Log.e(this.getClass().getCanonicalName(), "Failed to get host address", ex);
        } catch (IOException ex) {
            Log.e(this.getClass().getCanonicalName(), "Failed to open socket to host", ex);
        }
    }

    public interface OnTcpMessageReceived {
        public void tcpMessageReceived(String message);
    }
}
