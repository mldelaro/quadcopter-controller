package mldelaro.asu.edu.quadcoptercontroller.client.udp;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * Created by mldof on 5/10/2017.
 */

public class UdpClient {
    private String hostIp;
    private int hostPort;
    private boolean isListening;
    private OnUdpMessageReceived udpMessageListener;
    private String rxUdpMessage;
    private String runningTxUdpMessage;

    private UdpClient(){};

    public UdpClient(String hostIp, int hostPort, OnUdpMessageReceived listener) {
        this.hostIp = hostIp;
        this.hostPort = hostPort;
        isListening = false;
        udpMessageListener = listener;
        runningTxUdpMessage = "{\"command\":\"S\"}";
        rxUdpMessage = "";
        Log.d("mldlog-UdpClientDriver", "Initializing udp client driver: " + hostIp + ":" + hostPort);
    }

    public void sendMessage(String message) {
        Log.d("mldlog-UdpClientDriver", "Setting txMessage: " + runningTxUdpMessage);
        runningTxUdpMessage = message;
    }

    public void stopClient() {
        Log.d("mldlog-UdpClientDriver", "Stopping udp connection: " + runningTxUdpMessage);
        isListening = false;
        if(runningTxUdpMessage != null) {
            runningTxUdpMessage = null;
        }
        if(udpMessageListener != null) {
            udpMessageListener = null;
        }
    }

    public void udpClientDriver() {
        Log.d("mldlog-UdpClientDriver", "Started udp client driver...");
        isListening = true;
        try {
            InetAddress hostAddress = InetAddress.getByName(hostIp);
            byte[] txUdpMessage = runningTxUdpMessage.getBytes();
            DatagramSocket txUdpSocket = new DatagramSocket(hostPort);
            int i = 0;
            txUdpSocket.setSoTimeout(5);
            Log.d("mldlog-UdpClientDriver", "Opened udp socket...");
            while(isListening) {
                //TX phase
                DatagramPacket txUdpPacket = new DatagramPacket(txUdpMessage, txUdpMessage.length, hostAddress, hostPort);
                i++;
                txUdpMessage = runningTxUdpMessage.getBytes();
                Log.d("mdlog-UdpClientDriver", "Sending message: " + runningTxUdpMessage);

                try {
                    txUdpSocket.send(txUdpPacket);
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (SocketTimeoutException ex) {

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                //RX Phase
                byte[] rxData = new byte[512];
                DatagramPacket rxUdpPacket = new DatagramPacket(rxData, 512);

                try {
                    txUdpSocket.receive(rxUdpPacket);
                    rxUdpMessage = new String(rxUdpPacket.getData());
                    rxUdpMessage.trim();
                    Log.d("mdlog-UdpClientDriver", "Receiving message: " + rxUdpMessage);
                    udpMessageListener.udpMessageReceived(rxUdpMessage);
                } catch (SocketTimeoutException ex) {

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            txUdpSocket.close();
            Log.d("mldlog-UdpClientDriver", "Received Response: " + rxUdpMessage);
        } catch (UnknownHostException ex) {
            Log.e(this.getClass().getCanonicalName(), "Failed to get host address", ex);
        } catch (IOException ex) {
            Log.e(this.getClass().getCanonicalName(), "Failed to open socket to host", ex);
        }
    }

    public interface OnUdpMessageReceived {
        public void udpMessageReceived(String message);
    }
}
