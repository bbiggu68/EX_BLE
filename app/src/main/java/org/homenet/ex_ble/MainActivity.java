package org.homenet.ex_ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class MainActivity extends Activity implements CompoundButton.OnCheckedChangeListener {
    // Debugging
    private final static String TAG = MainActivity.class.getSimpleName();
    private static final boolean D = true;
    // 상수
    private static final int REQUEST_ENABLE_BT = 1;
    final static int ACT_SETUP = 0;
    //
    private BackgroundService mBluetoothLeService;
    public Handler mHandler;
    ConfigData mCfgClass;
    //
    //
    private TextView mLblDeviceName;
    private TextView mLblDeviceAddress;
    private TextView mLblConnectionState;
    private TextView mMeasDataField;
    private TextView mLargeDataField;
    //
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BackgroundService.LocalBinder) service).getService();
            if (!mBluetoothLeService.bleInitialize(MainActivity.this)) {
                if(D) lg.o("Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            if (mBluetoothLeService.mConnectionState == BackgroundService.STATE_DISCONNECTED) {
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
            }
            if (mBluetoothLeService.mConnectionState == BackgroundService.STATE_CONNECTED) {
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            }
            if(D) lg.o("mServiceConnection:onServiceConnected()");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            if(D) lg.o("mServiceConnection:onServiceDisconnected()");
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BackgroundService.ACTION_GATT_DISCOVERED.equals(action)) {
                mLblDeviceName.setText(mBluetoothLeService.mBluetoothDeviceName);
                mLblDeviceAddress.setText(mBluetoothLeService.mBluetoothDeviceAddress);
            } else if (BackgroundService.ACTION_GATT_CONNECTED.equals(action)) {
                updateConnectionState(R.string.connected);
                mBluetoothLeService.bleDiscoverServices();
            } else if (BackgroundService.ACTION_GATT_DISCONNECTED.equals(action)) {
                updateConnectionState(R.string.disconnected);
                clearUI();
            } else if (BackgroundService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                discoverGattServices(mBluetoothLeService.bleGetSupportedGattServices());
            } else if (BackgroundService.ACTION_GATT_CHARACTERISTIC_SET_NOTI_MEAS.equals(action)) {
                setNotifyMeasurementCharacteristic();
            } else if (BackgroundService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(0, intent.getStringExtra(BackgroundService.EXTRA_DATA));
            } else if (BackgroundService.ACTION_DATA_AVAILABLE_MEAS.equals(action)) {
                displayData(1, intent.getStringExtra(BackgroundService.EXTRA_DATA));
            } else if (BackgroundService.ACTION_DATA_AVAILABLE_CTRL.equals(action)) {
                displayData(2, intent.getStringExtra(BackgroundService.EXTRA_DATA));
            } else if (BackgroundService.ACTION_DATA_AVAILABLE_LARGE.equals(action)) {
                displayData(3, intent.getStringExtra(BackgroundService.EXTRA_DATA));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 로그 유틸리티 초기화
        TextLog.init(this);
        TextLog.mAppendTime = true;
        TextLog.mReverseReport = true;
        TextLog.mTag = TAG;
        TextLog.mMaxFileSize = 500;
        startService();
        //
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // BLE 지원 체크
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            if(D) lg.o("BLE Not Supported!!!");
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        //
        mHandler = new Handler();
        mCfgClass = new ConfigData();
        // Sets up UI references.
        mLblDeviceName = (TextView) findViewById(R.id.device_name);
        mLblDeviceAddress = (TextView) findViewById(R.id.device_address);
        mLblConnectionState = (TextView) findViewById(R.id.connection_state);
        mMeasDataField = (TextView) findViewById(R.id.data_meas);
        mLargeDataField = (TextView) findViewById(R.id.data_large);
        //
        mLblDeviceName.setText("N/A");
        mLblDeviceAddress.setText("N/A");
        //**************************
        Switch sw = (Switch)findViewById(R.id.switchLED2);
        sw.setOnCheckedChangeListener(this);
        sw = (Switch)findViewById(R.id.switchLED3);
        sw.setOnCheckedChangeListener(this);
        sw = (Switch)findViewById(R.id.switchLED4);
        sw.setOnCheckedChangeListener(this);
        sw = (Switch)findViewById(R.id.switchLED5);
        sw.setOnCheckedChangeListener(this);
        sw = (Switch)findViewById(R.id.switchLED6);
        sw.setOnCheckedChangeListener(this);
        //
        Intent gattServiceIntent = new Intent(this, BackgroundService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBluetoothLeService.bleScanLeDevice(false);
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
        stopService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        if(D) TextLog.addMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingActivity.class);
            intent.putExtra("ConfigClass", mCfgClass);
            startActivityForResult(intent, ACT_SETUP);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onCheckedChanged(CompoundButton v, boolean isChecked) {
        //byte[] val = new byte[1];
        //int val = 0;
        byte[] val = new byte[20];
        for (int i=0;i<20;i++) val[i]=(byte)0;

        switch (v.getId()) {
            case R.id.switchLED2:
                if (isChecked) {
                    //val[0] = 0x02;
                    val[0] = (byte)2;
                    //val = 0x02;
                }
                else {
                    //val[0] = 0x42;
                    val[0] = (byte)66;
                    //val = 0x42;
                }
                break;
            case R.id.switchLED3:
                if (isChecked) {
                    //val[0] = 0x03;
                    val[0] = (byte)3;
                    //val = 0x03;
                }
                else {
                    //val[0] = 0x43;
                    val[0] = (byte)67;
                    //val = 0x43;
                }
                break;
            case R.id.switchLED4:
                if (isChecked) {
                    //val[0] = 0x04;
                    val[0] = (byte)4;
                    //val = 0x04;
                }
                else {
                    //val[0] = 0x44;
                    val[0] = (byte)68;
                    //val = 0x44;
                }
                break;
            case R.id.switchLED5:
                if (isChecked) {
                    //val[0] = 0x05;
                    val[0] = (byte)5;
                    //val = 0x05;
                }
                else {
                    //val[0] = 0x45;
                    val[0] = (byte)69;
                    //val = 0x45;
                }
                break;
            case R.id.switchLED6:
                if (isChecked) {
                    //val[0] = 0x06;
                    val[0] = (byte)6;
                    //val = 0x06;
                }
                else {
                    //val[0] = 0x46;
                    val[0] = (byte)70;
                    //val = 0x46;
                }
                break;
        }
        //mCtrlCharacteristic.setValue(val, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        mBluetoothLeService.mCtrlCharacteristic.setValue(val);
        //if(D) lg.o(ret.toString());
        mBluetoothLeService.bleWriteCharacteristic(mBluetoothLeService.mCtrlCharacteristic);
    }

    public void mOnClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan:
                if (!mBluetoothLeService.mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                mBluetoothLeService.bleScanLeDevice(true);
                break;
            case R.id.btn_connectgatt_false:
                mBluetoothLeService.bleConnectGatt(mBluetoothLeService.mBluetoothDeviceAddress, false);
                break;
            case R.id.btn_connectgatt_true:
                mBluetoothLeService.bleConnectGatt(mBluetoothLeService.mBluetoothDeviceAddress, true);
                break;
            case R.id.btn_disconnect:
                mBluetoothLeService.bleDisconnect();
                break;
            case R.id.btn_connect_known:
                mBluetoothLeService.bleConnect(mBluetoothLeService.mBluetoothDeviceAddress);
                break;
            case R.id.btn_close:
                mBluetoothLeService.bleClose();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //
        switch (requestCode) {
            case ACT_SETUP:
                if (resultCode == RESULT_OK) {
                    mCfgClass = (ConfigData)data.getSerializableExtra("ConfigClass");
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_CANCELED) {
                    finish();
                    return;
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void discoverGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                if (HomenetGattAttributes.UUID_WP_MEAS.equals(gattCharacteristic.getUuid())) {
                    mBluetoothLeService.mMeasCharacteristic = gattCharacteristic;
                    //setNotifyMeasurementCharacteristic();
                }
                if (HomenetGattAttributes.UUID_WP_CTL.equals(gattCharacteristic.getUuid())) {
                    mBluetoothLeService.mCtrlCharacteristic = gattCharacteristic;
                }
                if (HomenetGattAttributes.UUID_WP_LOG.equals(gattCharacteristic.getUuid())) {
                    mBluetoothLeService.mButtonCharacteristic = gattCharacteristic;
                    setNotifyLargeDataCharacteristic();
                }
            }
        }
    }

    private void setNotifyLargeDataCharacteristic() {
        mBluetoothLeService.bleSetCharacteristicNotification(mBluetoothLeService.mButtonCharacteristic, true);
    }

    private void setNotifyMeasurementCharacteristic() {
        mBluetoothLeService.bleSetCharacteristicNotification(mBluetoothLeService.mMeasCharacteristic, true);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLblConnectionState.setText(resourceId);
                if (resourceId == R.string.disconnected) {
                    mLblDeviceName.setText("N/A");
                    mLblDeviceAddress.setText("N/A");
                } else {
                    mLblDeviceName.setText(mBluetoothLeService.mBluetoothDeviceName);
                    mLblDeviceAddress.setText(mBluetoothLeService.mBluetoothDeviceAddress);
                }
            }
        });
    }

    private void displayData(int selchara, String data) {
        if (data != null) {
            if (selchara == 0)
                return;
            else if (selchara == 1)
                mMeasDataField.setText(data);
            else if (selchara == 2)
                return;
            else if (selchara == 3)
                mLargeDataField.setText(data);
        }
    }

    private void clearUI() {
        mMeasDataField.setText(R.string.no_data);
        mLargeDataField.setText(R.string.no_data);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BackgroundService.ACTION_GATT_DISCOVERED);
        intentFilter.addAction(BackgroundService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BackgroundService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BackgroundService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BackgroundService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BackgroundService.ACTION_DATA_AVAILABLE_MEAS);
        intentFilter.addAction(BackgroundService.ACTION_DATA_AVAILABLE_CTRL);
        intentFilter.addAction(BackgroundService.ACTION_DATA_AVAILABLE_LARGE);
        intentFilter.addAction(BackgroundService.ACTION_GATT_CHARACTERISTIC_SET_NOTI_MEAS);
        intentFilter.addAction(BackgroundService.ACTION_GATT_CHARACTERISTIC_SET_NOTI_LOG);
        return intentFilter;
    }

    private void startService() {
        Intent intent;
        Toast.makeText(getApplication(), "Service-Start", Toast.LENGTH_SHORT).show();
        intent = new Intent(this, BackgroundService.class);
        startService(intent);
    }

    private void stopService() {
        Intent intent;
        intent = new Intent(this, BackgroundService.class);
        stopService(intent);
    }
}
