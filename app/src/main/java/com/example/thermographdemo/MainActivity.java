package com.example.thermographdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.ArrayMap;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ca.hss.heatmaplib.HeatMap;

/**
 * Created by QLK on 20-4-21.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    public static final int SERVER_MODE = 0x10;
    public static final int CLIENT_MODE = 0x11;

    public static final int ON_SERIAL_CONNECT = 0x31;
    public static final int ON_SERIAL_CONNECT_ERR = 0x32;
    public static final int ON_SERIAL_READ = 0x34;
    public static final int ON_SERIAL_IO_ERR = 0x35;

    public static final int WARNING_OVER_HEATED = 34;

    private BluetoothAdapter bluetoothAdapter;

    private ArrayAdapter<BluetoothDevice> mArrayAdapter;
    private List<BluetoothDevice> mList = new ArrayList<>();

    @BindView(R.id.heatmap)
    HeatMap mHeatMap;
    @BindView(R.id.et_msg)
    EditText et_msg;
    @BindView(R.id.tv_msg)
    TextView tv_msg;
    @BindView(R.id.tv_max)
    TextView tv_max;
    @BindView(R.id.tv_min)
    TextView tv_min;
    @BindView(R.id.tv_status)
    TextView tv_status;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String NAME = "BluServer";

    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private final Handler handler = new MyHandler(this);


    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                String deviceName = device.getName();
//                String deviceHardwareAddress = device.getAddress(); // MAC address

                mArrayAdapter.insert(device, 0);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction())) {
                Log.d(TAG, "Blue tooth discovery  start");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG, "Blue tooth discovery  over");
            }
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mArrayAdapter = new DeviceAdapter(this, mList);

        tv_msg.setMovementMethod(ScrollingMovementMethod.getInstance());

        // 注册回调：当找到一个蓝牙设备
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(receiver, filter);

        // 注册回调：当结束搜索蓝牙设备
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(receiver, filter);

        // 注册回调：当开始搜索蓝牙设备
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        this.registerReceiver(receiver, filter);

        mHeatMap.setRadius(88);

        DisplayMetrics out = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(out);

        Log.d(TAG, "onCreate width: " + out.widthPixels);
        Log.d(TAG, "onCreate height: " + out.heightPixels);

        mHeatMap.getLayoutParams().width = out.widthPixels;
        mHeatMap.getLayoutParams().height = (int) (out.widthPixels / (32 / 24.0f));

        mHeatMap.setMaxDrawingWidth(256);
        mHeatMap.setMaxDrawingHeight(192);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void setHeatMapData(float[] arr) {

        float min = Data.getMin(arr);
        float max = Data.getMax(arr);

        tv_min.setText(String.valueOf(min));
        tv_max.setText(String.valueOf(max));

        if (max > WARNING_OVER_HEATED) {
            tv_status.setText(R.string.over_heated);
            tv_status.setTextColor(getResources().getColor(R.color.colorOverHeated));
            Toast.makeText(this, getString(R.string.over_heated), Toast.LENGTH_SHORT).show();
        } else {
            tv_status.setText(R.string.normal_heated);
            tv_status.setTextColor(getResources().getColor(R.color.colorNormal));
        }

        mHeatMap.setMinimum(min);
        mHeatMap.setMaximum(max);

//        Map<Float, Integer> colors = new ArrayMap<>();
////        colors.put(0.0f, 0xFF00008b);
////        colors.put(0.2f, 0xFF6495ED);
////        colors.put(0.4f, 0xFF013220);
////        colors.put(0.6f, 0xFF7FFF00);
////        colors.put(0.8f, 0xFFF8DE7E);
//
//        colors.put(0.0f, 0xff000000);
//        colors.put(0.2f, 0xFF0019FF);
//        colors.put(0.4f, 0xff0000ff);
//
//        colors.put(0.6f, 0xFF000eff);
//
//        colors.put(0.65f, 0xFF00E635);//green
//        colors.put(0.70f, 0xFFFEF504);//
//        colors.put(1.0f, 0xFFFF0305);//


        Map<Float, Integer> colors2 = new ArrayMap<>();
        //build a color gradient in HSV from red at the center to blue at the outside
        for (int i = 0; i < max; i++) {
            float stop = ((float) i) / max;
            int color = doGradient(i, min, max, 0xff0000ff, 0xffff0000);
            colors2.put(stop, color);
        }

        mHeatMap.setColorStops(colors2);

        AsyncTask.execute(() -> {
            //drawNewMap
            mHeatMap.clearData();
            Map<Integer, ArrayList<Float>> map = Data.get(arr);
            for (int y = 0; y < map.size(); y++) {
                ArrayList<Float> list = map.get(y);
                Collections.reverse(list);//x坐标反转

                for (int x = 0; x < list.size(); x++) {
                    HeatMap.DataPoint point = new HeatMap.DataPoint(x / (float) list.size(), y / (float) map.size(), list.get(x));
                    mHeatMap.addData(point);

//                Log.d(TAG, "onCreate: " + list.get(x));
                }
            }
            mHeatMap.forceRefreshOnWorkerThread();
            runOnUiThread(() -> mHeatMap.invalidate());
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bluetooth_settings:
                Intent intent = new Intent();
                intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(intent);
                break;
            case R.id.show_devices:
                startDiscovery();
                break;
            case R.id.start_server:
                startListener();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    int sendCount = 0;

    @OnClick(R.id.btn_send)
    void sendMsg() {
        Log.d(TAG, "sendMsg: ");
        if (mConnectedThread != null) {
            String msg = et_msg.getText().toString();
            if (TextUtils.isEmpty(msg)) {
                msg = Data.testArr[sendCount % 5];
                sendCount++;
            }
            Log.d(TAG, "sendMsg: " + msg);

            mConnectedThread.write(msg.getBytes());
        }
    }

    @OnClick(R.id.tv_clear)
    void clear() {
        tv_msg.setText("");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        Log.d(TAG, "onActivityResult: " + requestCode + ", " + resultCode + ", " + data);
        switch (requestCode) {
            case SERVER_MODE://服务器模式
                if (resultCode == RESULT_OK) {
                    startListener();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "蓝牙未开启", Toast.LENGTH_SHORT).show();
                }
                break;
            case CLIENT_MODE://客户端模式
                if (resultCode == Activity.RESULT_OK) {
                    startDiscovery();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "蓝牙未开启", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    private void startDiscovery() {
        if (bluetoothAdapter == null) {
            Toast.makeText(getBaseContext(), "不支持蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, CLIENT_MODE);
            return;
        }

        mArrayAdapter.clear();
        mArrayAdapter.addAll(bluetoothAdapter.getBondedDevices());

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Bluetooth devices");
        builder.setAdapter(mArrayAdapter, (dialog, which) -> {
            BluetoothDevice device = mArrayAdapter.getItem(which);
            Log.d(TAG, "OnItemClick: " + device.getName() + ":" + device.getAddress());

            mConnectThread = new ConnectThread(device);
            mConnectThread.start();
            dialog.dismiss();
        });
        builder.create().show();
    }

    void startListener() {
        if (bluetoothAdapter == null) {
            Toast.makeText(getBaseContext(), "不支持蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }

        //判断是否可被搜索
        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 5 * 60);
            startActivityForResult(intent, SERVER_MODE);
            return;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
        }
        if (mConnectThread != null) {
            mConnectThread.cancel();
        }

        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
    }

    public void onSerialConnect() {
        tv_msg.append("Connect successful\n");

    }

    public void onSerialConnectError(Exception e) {
        tv_msg.append(e.getMessage() + "\n");
    }

    public void onSerialRead(String data) {
        try {
            tv_msg.append(data + "\n");

            String[] arr = data.split(" ");
            Log.d(TAG, "onSerialRead split len: " + arr.length);

            if (768 != arr.length) {
                return;
            }

            float[] fArr = Data.convert(arr);

//            for (int i = 0; i < arr.length; i++) {
//                fArr[i] = Float.parseFloat(arr[i]);
//            }

            setHeatMapData(fArr);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void onSerialIoError(Exception e) {
        tv_msg.append(e.getMessage() + "\n");
    }


    private class AcceptThread extends Thread {

        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();

                    mConnectedThread = new ConnectedThread(socket);
                    mConnectedThread.start();

                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
//                    manageMyConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                handler.sendEmptyMessage(ON_SERIAL_CONNECT);

                mConnectedThread = new ConnectedThread(mmSocket);
                mConnectedThread.start();

            } catch (Exception e) {
                e.printStackTrace();
                Message message = handler.obtainMessage(ON_SERIAL_CONNECT_ERR, e);
                handler.sendMessage(message);

                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
//            manageMyConnectedSocket(mmSocket);
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    // 客户端与服务器建立连接成功后，用ConnectedThread收发数据
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream input = null;
            OutputStream output = null;

            try {
                input = socket.getInputStream();
                output = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.inputStream = input;
            this.outputStream = output;
        }

        public void run() {
            StringBuilder recvText = new StringBuilder();
            byte[] buff = new byte[1024];
            int bytes;

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream));

            while (true) {
                try {
                    String str = reader.readLine();
                    Message message = handler.obtainMessage(ON_SERIAL_READ, str);
                    handler.sendMessage(message);
//                    bytes = inputStream.read(buff);
//                    String str = new String(buff, "ISO-8859-1");
//                    str = str.substring(0, bytes);
//
//                    Log.d(TAG, "run: " + str);
//
//
//
//                    // 收到数据，单片机发送上来的数据以"#"结束，这样手机知道一条数据发送结束
//                    //Log.e("read", str);
//                    if (!str.endsWith("\r\n")) {
//                        recvText.append(str);
//                        continue;
//                    }
//                    recvText.append(str.substring(0, str.length() - 1)); // 去除'#'
//

//                    Message message = handler.obtainMessage(ON_SERIAL_READ, recvText.toString());
//                    handler.sendMessage(message);


                    recvText.replace(0, recvText.length(), "");
                } catch (IOException e) {
                    e.printStackTrace();
                    Message msg = handler.obtainMessage(ON_SERIAL_IO_ERR, e);
                    handler.sendMessage(msg);
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private static int doGradient(double value, double min, double max, int min_color, int max_color) {
        if (value >= max) {
            return max_color;
        }
        if (value <= min) {
            return min_color;
        }
        float[] hsvmin = new float[3];
        float[] hsvmax = new float[3];
        float frac = (float) ((value - min) / (max - min));
        Color.RGBToHSV(Color.red(min_color), Color.green(min_color), Color.blue(min_color), hsvmin);
        Color.RGBToHSV(Color.red(max_color), Color.green(max_color), Color.blue(max_color), hsvmax);
        float[] retval = new float[3];
        for (int i = 0; i < 3; i++) {
            retval[i] = interpolate(hsvmin[i], hsvmax[i], frac);
        }
        return Color.HSVToColor(retval);
    }

    private static float interpolate(float a, float b, float proportion) {
        return (a + ((b - a) * proportion));
    }
}
