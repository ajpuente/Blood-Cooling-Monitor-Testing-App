package com.capstone.bloodcoolingdevicesimulator;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter adapter = null;
    private AcceptThread acceptThread = null;
    private ConnectedThread connectedThread = null;

    private static final String TAG = "Simulator";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && !adapter.isEnabled()) {
            adapter.enable();
        }
        acceptThread = new AcceptThread();
        acceptThread.start();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (connectedThread != null && connectedThread.isAlive()) {
                    try {
                        byte[] byteString = generateRandomMessage().getBytes("UTF-8");
                        connectedThread.write(byteString);
                        Log.d(TAG, "Sent data: " + new String(byteString, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {

                    }

                } else {
                    if (!acceptThread.isAlive()) {
                        acceptThread = new AcceptThread();
                        acceptThread.start();
                    }
                    Log.d(TAG, "Device not connected");
                }

            }

            private String generateRandomMessage() {
                String stringResult = "";
                Random r = new Random();
                for(int i = 0; i < 6; i++) {
                    stringResult += Integer.toString(r.nextInt(101)) + ",";
                }
                return stringResult.substring(0, stringResult.lastIndexOf(","));
            }
        }, 0, 1000);
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket temp = null;
            try {
                temp = adapter.listenUsingRfcommWithServiceRecord(getString(R.string.bt_service_name),
                        UUID.fromString(getString(R.string.uuid)));
            } catch (IOException e) {

            }
            mmServerSocket = temp;
        }

        public void run() {
            BluetoothSocket socket = null;
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                if (socket != null) {
                    connectedThread = new ConnectedThread(socket);
                    connectedThread.start();
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {

                    }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {

            }
        }
    }

    private class ConnectedThread extends Thread {

        private final BluetoothSocket btSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.btSocket = socket;
            InputStream tempInput = null;
            OutputStream tempOutput = null;
            try {
                tempInput = socket.getInputStream();
                tempOutput = socket.getOutputStream();
            } catch (IOException e) {

            }
            this.inputStream = tempInput;
            this.outputStream = tempOutput;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(0, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {

            }
        }

        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) {

            }
        }
    }


    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] writeBuffer = (byte[]) msg.obj;
            int begin = msg.arg1;
            int end = msg.arg2;
            switch (msg.what) {
                case 0:
                    String message = new String(writeBuffer);
                    message = message.substring(begin, end);
                    break;
            }
        }
    };
}
