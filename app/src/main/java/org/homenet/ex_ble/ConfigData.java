package org.homenet.ex_ble;

import java.io.Serializable;

/**
 * Created by bbiggu on 2015. 12. 17..
 */
public class ConfigData implements Serializable {
    private static final long serialVersionUID = 1L;
    // Central Setting
    public int ReconnectionTimeout = 120;	// def: 120s
    // Peripheral Setting
    public int AdvertisingTimeout = 120;	// def: 120s
    public int AdvertisingInterval = 1000;	// def: 1000ms
}
