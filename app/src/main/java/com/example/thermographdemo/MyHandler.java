package com.example.thermographdemo;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;

/**
 * Created by QLK on 20-4-22.
 */
public class MyHandler extends Handler {
    private static final String TAG = MyHandler.class.getSimpleName();

    private WeakReference<MainActivity> mReference;

    public MyHandler(MainActivity activity) {
        this.mReference = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        Log.d(TAG, "handleMessage: " + msg);

        MainActivity activity = mReference.get();
        switch (msg.what) {
            case MainActivity.ON_SERIAL_CONNECT:
                activity.onSerialConnect();
                break;
            case MainActivity.ON_SERIAL_CONNECT_ERR:
                activity.onSerialConnectError((Exception) msg.obj);
                break;
            case MainActivity.ON_SERIAL_READ:
                activity.onSerialRead((String) msg.obj);
                break;
            case MainActivity.ON_SERIAL_IO_ERR:
                activity.onSerialIoError((Exception) msg.obj);
                break;

        }
        super.handleMessage(msg);
    }
}