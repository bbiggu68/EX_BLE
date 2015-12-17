package org.homenet.ex_ble;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;

/**
 * Created by bbiggu on 2015. 12. 17..
 */
public class SettingActivity extends Activity {
    // Debugging
    private final static String TAG = SettingActivity.class.getSimpleName();
    private static final boolean D = true;
    //
    ConfigData mCfgClass;
    //
    private SeekBar sbReconnectionTimeout;
    private int iReconnectionTimeout = 0;	// 디폴트: 타임아웃없음.
    private SeekBar sbAdvertisingTimeout;
    private int iAdvertisingTimeout = 0;	// 디폴트: 120s
    private SeekBar sbAdvertisingInterval;
    private int iAdvertisingInterval = 0;	// 디폴트: 20ms
    private EditText edtReconnectionTimeout;
    private EditText edtAdvertisingTimeout;
    private EditText edtAdvertisingInterval;

    //
    private View.OnFocusChangeListener edtListner = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean focus) {
            switch (v.getId()) {
                case R.id.edtReconnectionTimeout:
                    iReconnectionTimeout = Integer.parseInt(edtReconnectionTimeout.getText().toString());
                    sbReconnectionTimeout.setProgress(iReconnectionTimeout); // 디폴트: 타임아웃없음.
                    break;
                case R.id.edtAdvertisingTimeout:
                    iAdvertisingTimeout = Integer.parseInt(edtAdvertisingTimeout.getText().toString());
                    sbAdvertisingTimeout.setProgress(iAdvertisingTimeout - 120); // 디폴트: 120s
                    break;
                case R.id.edtAdvertisingInterval:
                    iAdvertisingInterval = Integer.parseInt(edtAdvertisingInterval.getText().toString());
                    sbAdvertisingInterval.setProgress(iAdvertisingInterval - 20); // 디폴트: 20ms
                    break;
            }
        }
    };

    private SeekBar.OnSeekBarChangeListener sbListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            updateValue4SeekBar(seekBar.getId(), progress);
        }
    };


    private void getIntentData() {
        Intent intent = getIntent();
        mCfgClass = (ConfigData)intent.getSerializableExtra("ConfigClass");
    }
    private void setIntentData() {
        Intent intent = new Intent();
        intent.putExtra("ConfigClass", mCfgClass);
        setResult(RESULT_OK, intent);
    }

    private void initUI() {
        //
        iReconnectionTimeout = mCfgClass.ReconnectionTimeout;
        iAdvertisingTimeout = mCfgClass.AdvertisingTimeout;
        iAdvertisingInterval = mCfgClass.AdvertisingInterval;
        //
        sbReconnectionTimeout = (SeekBar) findViewById(R.id.skbReconnectionTimeout);
        sbAdvertisingTimeout = (SeekBar) findViewById(R.id.skbAdvertisingTimeout);
        sbAdvertisingInterval = (SeekBar) findViewById(R.id.skbAdvertisingInterval);
        edtReconnectionTimeout = (EditText) findViewById(R.id.edtReconnectionTimeout);
        edtAdvertisingTimeout = (EditText) findViewById(R.id.edtAdvertisingTimeout);
        edtAdvertisingInterval = (EditText) findViewById(R.id.edtAdvertisingInterval);
        // 리스너 설정
        sbReconnectionTimeout.setOnSeekBarChangeListener(sbListener);
        sbAdvertisingTimeout.setOnSeekBarChangeListener(sbListener);
        sbAdvertisingInterval.setOnSeekBarChangeListener(sbListener);
        edtReconnectionTimeout.setOnFocusChangeListener(edtListner);
        edtAdvertisingTimeout.setOnFocusChangeListener(edtListner);
        edtAdvertisingInterval.setOnFocusChangeListener(edtListner);
        //
        sbReconnectionTimeout.setProgress(iReconnectionTimeout);
        sbAdvertisingTimeout.setProgress(iAdvertisingTimeout);
        sbAdvertisingInterval.setProgress(iAdvertisingInterval);
    }
    private void updateValue4SeekBar(int selectedSeekBar, int value) {
        EditText editTemp = null;

        switch(selectedSeekBar) {
            case R.id.skbReconnectionTimeout:
                editTemp =(EditText)findViewById(R.id.edtReconnectionTimeout);
                iReconnectionTimeout = value;
                editTemp.setText(String.format("%d", value)); // 디폴트: 타임아웃없음.
                break;
            case R.id.skbAdvertisingTimeout:
                editTemp =(EditText)findViewById(R.id.edtAdvertisingTimeout);
                iAdvertisingTimeout = value;
                editTemp.setText(String.format("%d", value + 120)); // 디폴트: 120s
                break;
            case R.id.skbAdvertisingInterval:
                editTemp =(EditText)findViewById(R.id.edtAdvertisingInterval);
                iAdvertisingInterval = value;
                editTemp.setText(String.format("%d", value + 20)); // 디폴트: 20ms
                break;
        }
    }

    private void updateUI() {

    }

    private void saveConfig() {
        mCfgClass.ReconnectionTimeout = iReconnectionTimeout;
        mCfgClass.AdvertisingTimeout = iAdvertisingTimeout;
        mCfgClass.AdvertisingInterval = iAdvertisingInterval;
    }

    public void mOnClick(View v) {
        switch (v.getId()) {
            case R.id.btnSetupOK:
                saveConfig();
                setIntentData();
                finish();
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        getIntentData();
        initUI();
        updateUI();
        //
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }
}
