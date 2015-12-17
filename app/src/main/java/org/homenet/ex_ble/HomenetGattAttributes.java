package org.homenet.ex_ble;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created by bbiggu on 2015. 12. 17..
 */
public class HomenetGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();

    public static String GAP_SERVICE = "00001800-0000-1000-8000-00805f9b34fb";
    public static String GAP_DEVICE_NAME = "00002a00-0000-1000-8000-00805f9b34fb";
    public static String GAP_APPEARANCE = "00002a01-0000-1000-8000-00805f9b34fb";
    public static String GAP_PPCP = "00002a04-0000-1000-8000-00805f9b34fb";
    //
    public static String GA_SERVICE = "00001801-0000-1000-8000-00805f9b34fb";
    //
    public static String BATTERY_SERVICE = "0000180f-0000-1000-8000-00805f9b34fb";
    public static String BAS_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb";
    //
    public static String DEVICE_INFO_SERVICE = "0000180a-0000-1000-8000-00805f9b34fb";
    public static String DIS_MANUFACTURER = "00002a29-0000-1000-8000-00805f9b34fb";
    //
    public static String WP_SERVICE = "00001523-1212-efde-1523-785feabcd123";
    public static String WP_CTL = "00001524-1212-efde-1523-785feabcd123";
    public static String WP_MEAS = "00001525-1212-efde-1523-785feabcd123";
    public static String WP_LOG = "00001526-1212-efde-1523-785feabcd123";
    //
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    //** UUID
    public final static UUID UUID_WP_SERVICE = UUID.fromString(WP_SERVICE);
    public final static UUID UUID_WP_CTL = UUID.fromString(WP_CTL);
    public final static UUID UUID_WP_MEAS = UUID.fromString(BAS_LEVEL);
    public final static UUID UUID_WP_LOG = UUID.fromString(WP_LOG);


    static {
        // Sample Services.
        attributes.put(GAP_SERVICE, "Generic Access");
        attributes.put(GA_SERVICE, "Generic Attribute");
        attributes.put(BATTERY_SERVICE, "Battery Service");
        attributes.put(DEVICE_INFO_SERVICE, "Device Information Service");
        //******************************************************
        attributes.put(WP_SERVICE, "Wireless Probe Service");

        // Sample Characteristics.
        attributes.put(GAP_DEVICE_NAME, "Device Name");
        attributes.put(GAP_APPEARANCE, "Appearance");
        attributes.put(GAP_PPCP, "Peripheral Preferred Connection Parameters");
        attributes.put(BAS_LEVEL, "Battery Level");
        attributes.put(DIS_MANUFACTURER, "Manufacturer Name String");
        //******************************************************
        attributes.put(WP_CTL, "Wireless Probe Control");
        attributes.put(WP_MEAS, "Wireless Probe Measurement");
        attributes.put(WP_LOG, "Wireless Probe Log");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
