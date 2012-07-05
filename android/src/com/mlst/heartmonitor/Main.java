package com.mlst.heartmonitor;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class Main extends Activity implements Runnable {

    private static final String TAG = "Main";
    private static final String ACTION_USB_PERMISSION = "com.mlst.helloarduino.Main.action.USB_PERMISSION";
    private ImageView img;
    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private UsbAccessory mAccessory;
    private boolean mPermissionRequestPending;
    private Handler handler;
    private TextView label;
    private MediaPlayer beat;
    private GraphView graph;

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.main);
	
	 beat = MediaPlayer.create(this, R.raw.beat);
	 graph = (GraphView) findViewById(R.id.graph);

	img = (ImageView) findViewById(R.id.indicator);
	label = (TextView) findViewById(R.id.label);
	mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
	mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
	IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
	filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
	registerReceiver(mUsbReceiver, filter);

	handler = new Handler() {
	    @Override
	    public void handleMessage(Message msg) {
		if (msg.what == 0) {
		    HeartMonitorMessage hm = (HeartMonitorMessage) msg.obj;
		    if (hm.beat) {
			pumpHeart();
		    } else {
			label.setText(String.valueOf(hm.rate));
		    }
		    
		    

		}
	    }
	};
    }

    @Override
    protected void onPause() {
	unregisterReceiver(mUsbReceiver);
	super.onPause();
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

	@Override
	public void onReceive(Context context, Intent intent) {
	    String action = intent.getAction();

	    if (ACTION_USB_PERMISSION.equals(action)) {
		synchronized (this) {
		    UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
		    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
			openAccessory(accessory);
		    } else {
			Log.d(TAG, "permission denied for accessory " + accessory);
		    }

		    mPermissionRequestPending = false;
		}
	    } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
		UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
		if (accessory != null && accessory.equals(mAccessory)) {
		    closeAccessory();
		}
	    }
	}
    };
    private ParcelFileDescriptor fileDescriptor;
    private FileInputStream input;
    private FileOutputStream output;

    protected void openAccessory(UsbAccessory accessory) {
	fileDescriptor = mUsbManager.openAccessory(accessory);
	if (fileDescriptor != null) {
	    mAccessory = accessory;
	    FileDescriptor fd = fileDescriptor.getFileDescriptor();
	    input = new FileInputStream(fd);
	    output = new FileOutputStream(fd);

	    Thread t = new Thread(this, "Heart Monitor");
	    t.start();

	    Log.d(TAG, "accessory opened");
	} else {
	    Log.d(TAG, "accessory open fail");
	}

    }

    protected void closeAccessory() {
	try {
	    if (fileDescriptor != null) {
		fileDescriptor.close();
	    }
	} catch (IOException e) {
	} finally {
	    fileDescriptor = null;
	    mAccessory = null;
	}

    }

    @Override
    protected void onResume() {
	Intent intent = getIntent();
	if (input != null && output != null) {
	    return;
	}

	UsbAccessory[] accessories = mUsbManager.getAccessoryList();
	UsbAccessory accessory = (accessories == null ? null : accessories[0]);
	if (accessory != null) {
	    if (mUsbManager.hasPermission(accessory)) {
		openAccessory(accessory);
	    } else {
		synchronized (mUsbReceiver) {
		    if (!mPermissionRequestPending) {
			mUsbManager.requestPermission(accessory, mPermissionIntent);
			mPermissionRequestPending = true;
		    }
		}
	    }
	} else {
	    Log.d(TAG, "mAccessory is null");
	}
	super.onResume();
    }

    private void pumpHeart() {
	img.animate().scaleXBy(0.2f).scaleYBy(0.2f).setDuration(50).setListener(scaleUpListener);
	playBeat();
	
	graph.setDrawPulse(true);
    }

    private AnimatorListener scaleDownListener = new AnimatorListener() {

	@Override
	public void onAnimationStart(Animator animation) {
	    // TODO Auto-generated method stub

	}

	@Override
	public void onAnimationRepeat(Animator animation) {
	    // TODO Auto-generated method stub

	}

	@Override
	public void onAnimationEnd(Animator animation) {
	    // img.animate().scaleXBy(0.2f).scaleYBy(0.2f).setDuration(100).setListener(scaleUpListener);
	}

	@Override
	public void onAnimationCancel(Animator animation) {
	    // TODO Auto-generated method stub

	}
    };

    private AnimatorListener scaleUpListener = new AnimatorListener() {

	@Override
	public void onAnimationStart(Animator animation) {
	    // TODO Auto-generated method stub

	}

	@Override
	public void onAnimationRepeat(Animator animation) {
	    // TODO Auto-generated method stub

	}

	@Override
	public void onAnimationEnd(Animator animation) {
	    img.animate().scaleXBy(-0.2f).scaleYBy(-0.2f).setDuration(50).setListener(scaleDownListener);

	}

	@Override
	public void onAnimationCancel(Animator animation) {
	    // TODO Auto-generated method stub

	}
    };

    @Override
    public void run() {
	byte[] buffer = new byte[16384];
	int i;
	int retvalue = 0;

	while (retvalue >= 0) {
	    try {
		retvalue = input.read(buffer);
	    } catch (IOException e) {
		e.printStackTrace();
		break;
	    }

	    i = 0;
	    Message m = Message.obtain(handler, 0);
	    byte command = buffer[0];
	    byte value = buffer[1];

	    HeartMonitorMessage msg = new HeartMonitorMessage();

	    if (command == 0) {
		msg.beat = true;
	    } else {
		msg.beat = false;
	    }

	    msg.rate = value & 0xFF;

	    m.what = 0;
	    m.obj = msg;
	    handler.sendMessage(m);
	}

    }

    class HeartMonitorMessage {
	public boolean beat;
	public int rate;
    }

    private void playBeat() {
	if(!beat.isPlaying()){
	    beat.start();
	}else{
	    beat.stop();
	    beat.release();
	    beat = MediaPlayer.create(this, R.raw.beat);
	    beat.start();
	}
    }
}