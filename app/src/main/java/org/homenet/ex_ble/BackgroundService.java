package org.homenet.ex_ble;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by bbiggu on 2015. 12. 17..
 */
public class BackgroundService extends Service {
    // Debugging
    private final static String TAG = BackgroundService.class.getSimpleName();
    private static final boolean D = true;
    //*********************************************
    //******** Bluetooth LE Feature ***************
    //*********************************************
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    //
    NotificationManager mNotiManager;
    public static final int ONGOING_NOTIFICATION_ID = 1;
    public static final int BUTTON_NOTIFICATION_ID = 2;
    //** Action Constant
    public final static String ACTION_GATT_DISCOVERED = "kr.co.ftlab.bletest.ACTION_GATT_DISCOVERED";
    public final static String ACTION_GATT_CONNECTED = "kr.co.ftlab.bletest.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "kr.co.ftlab.bletest.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "kr.co.ftlab.bletest.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "kr.co.ftlab.bletest.ACTION_DATA_AVAILABLE";
    public final static String ACTION_DATA_AVAILABLE_CTRL = "kr.co.ftlab.bletest.ACTION_DATA_AVAILABLE_CTRL";
    public final static String ACTION_DATA_AVAILABLE_MEAS = "kr.co.ftlab.bletest.ACTION_DATA_AVAILABLE_MEAS";
    public final static String ACTION_DATA_AVAILABLE_LARGE = "kr.co.ftlab.bletest.ACTION_DATA_AVAILABLE_LARGE";
    public final static String EXTRA_DATA = "kr.co.ftlab.bletest.EXTRA_DATA";
    public final static String ACTION_GATT_CHARACTERISTIC_SET_NOTI_MEAS = "kr.co.ftlab.bletest.ACTION_GATT_CHARACTERISTIC_SET_NOTI_MEAS";
    public final static String ACTION_GATT_CHARACTERISTIC_SET_NOTI_LOG = "kr.co.ftlab.bletest.ACTION_GATT_CHARACTERISTIC_SET_NOTI_LOG";
    //** Member
    private MainActivity mCallerClone = null;
    private BluetoothManager mBluetoothManager;
    public BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    //
    public String mBluetoothDeviceName;
    public String mBluetoothDeviceAddress;
    public boolean mScanning;
    public int mConnectionState = STATE_DISCONNECTED;
    public static final long SCAN_PERIOD = 15000;	//10000;
    //
    public BluetoothGattCharacteristic mCtrlCharacteristic;
    public BluetoothGattCharacteristic mMeasCharacteristic;
    public BluetoothGattCharacteristic mButtonCharacteristic;

    @Override
    public void onCreate() {
        super.onCreate();
        //TextLog.mTag = TAG;
        //Toast.makeText(this, "BackgroundService:onCreate()", Toast.LENGTH_SHORT).show();
        if(D) lg.o("BackgroundService:onCreate()");

        //
        mNotiManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.mipmap.ic_launcher, "서비스 실행됨", System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this,  "Background Service", "Foreground로 실행됨", pendingIntent);
        startForeground(ONGOING_NOTIFICATION_ID, notification);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //bleClose();
        //bleDisconnect();
        if(D) lg.o("BackgroundService:onDestroy()");
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if(D) lg.o("BackgroundService:onStartCommand()");
        return START_STICKY;
    }


    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    intentAction = ACTION_GATT_CONNECTED;
                    mConnectionState = STATE_CONNECTED;
                    broadcastUpdate(intentAction);
                    Log.i(TAG, "Connected to GATT server.");
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
	            	/*
	            	 * For Nexus 7 (2013)
	            	 *
	            	if (mConfirmDisconnection) {
	            		bleClose(); // BluetoothGatt.close(); BluetoothGatt = null;
	            	}
	            	*/
                    intentAction = ACTION_GATT_DISCONNECTED;
                    mConnectionState = STATE_DISCONNECTED;
                    Log.i(TAG, "Disconnected from GATT server.");
                    broadcastUpdate(intentAction);
                }
            } else {
                Log.d(TAG, "onConnectionStateChange staus!=GATT_SUCCESS");
                if(D) lg.o("onConnectionStateChange staus!=GATT_SUCCESS");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (HomenetGattAttributes.UUID_WP_CTL.equals(characteristic.getUuid()))
                    broadcastUpdate(ACTION_DATA_AVAILABLE_CTRL, characteristic);
                if (HomenetGattAttributes.UUID_WP_MEAS.equals(characteristic.getUuid()))
                    broadcastUpdate(ACTION_DATA_AVAILABLE_MEAS, characteristic);
                if (HomenetGattAttributes.UUID_WP_LOG.equals(characteristic.getUuid()))
                    broadcastUpdate(ACTION_DATA_AVAILABLE_LARGE, characteristic);
            }
        }

        @Override
        public void  onCharacteristicWrite (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                //if (UUID_WP_MEAS.equals(characteristic.getUuid()))
                //	broadcastUpdate(ACTION_DATA_AVAILABLE_MEAS, characteristic);
                //if (UUID_WP_CTL.equals(characteristic.getUuid()))
                //	broadcastUpdate(ACTION_DATA_AVAILABLE_POWER, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            if (HomenetGattAttributes.UUID_WP_CTL.equals(characteristic.getUuid()))
                broadcastUpdate(ACTION_DATA_AVAILABLE_CTRL, characteristic);
            if (HomenetGattAttributes.UUID_WP_MEAS.equals(characteristic.getUuid()))
                broadcastUpdate(ACTION_DATA_AVAILABLE_MEAS, characteristic);
            if (HomenetGattAttributes.UUID_WP_LOG.equals(characteristic.getUuid()))
                broadcastUpdate(ACTION_DATA_AVAILABLE_LARGE, characteristic);
        }

        @Override
        public void onDescriptorWrite (BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Wrote Descriptor to GATT server.");
                if (!HomenetGattAttributes.UUID_WP_MEAS.equals(descriptor.getCharacteristic().getUuid()))  {
                    broadcastUpdate(ACTION_GATT_CHARACTERISTIC_SET_NOTI_MEAS);
                }
            }
        }
    };

    public class LocalBinder extends Binder {
        BackgroundService getService() {
            return BackgroundService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if(D) lg.o("BackgroundService:onBind()");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        //bleClose();
        //bleDisconnect();
        if(D) lg.o("BackgroundService:onUnbind()");
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public boolean bleInitialize(MainActivity caller) {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        mCallerClone = caller;

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    public void bleScanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mCallerClone.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            List<UUID> retList = parseUuids(scanRecord);
            if (retList.contains(HomenetGattAttributes.UUID_WP_SERVICE)) {
                mCallerClone.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothDeviceName = device.getName();
                        mBluetoothDeviceAddress = device.getAddress();
                        if (mScanning) {
                            mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            mScanning = false;
                        }
                        broadcastUpdate(ACTION_GATT_DISCOVERED);
                    }
                });
            }
        }
    };

    public boolean bleConnectGatt(final String address, boolean autoConnect) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        //final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, autoConnect, mGattCallback);
        //mBluetoothGatt = device.connectGatt(this, true, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mBluetoothDeviceName = device.getName();
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public boolean bleConnect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public boolean bleDiscoverServices() {
        return mBluetoothGatt.discoverServices();
    }

    public void bleDisconnect() {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return;
        }
        mBluetoothGatt.disconnect();

    }

    public void bleClose() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void bleReadCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void bleWriteCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return;
        }
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public void bleSetCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to WP_LOG, WP_MEAS. For ENABLE_NOTIFICATION_VALUE Descripter.
        if ((HomenetGattAttributes.UUID_WP_LOG.equals(characteristic.getUuid())) ||
                (HomenetGattAttributes.UUID_WP_MEAS.equals(characteristic.getUuid()))) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(HomenetGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    public List<BluetoothGattService> bleGetSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();


        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            //********************************************************************
            //********************************************************************
            if(D) lg.o(stringBuilder.toString());
            if (ACTION_DATA_AVAILABLE_LARGE.equals(action)) {
                Notification notification = new Notification(R.mipmap.ic_launcher, stringBuilder.toString() + "번 버튼 눌림", System.currentTimeMillis());
                notification.defaults |= Notification.DEFAULT_ALL;
                notification.flags |= Notification.FLAG_INSISTENT | Notification.FLAG_AUTO_CANCEL;

                Intent notificationIntent = new Intent(this, MainActivity.class);
                notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
                notification.setLatestEventInfo(this,  "Background Service", stringBuilder.toString() + "번 버튼 눌림", pendingIntent);
                mNotiManager.notify(BUTTON_NOTIFICATION_ID, notification);
            }

            //intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            intent.putExtra(EXTRA_DATA, stringBuilder.toString());
        }

        sendBroadcast(intent);
    }

    private List<UUID> parseUuids(byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();

        ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0) break;

            byte type = buffer.get();
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (length >= 2) {
                        uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    break;

                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;

                default:
                    buffer.position(buffer.position() + length - 1);
                    break;
            }
        }
        return uuids;
    }
}
