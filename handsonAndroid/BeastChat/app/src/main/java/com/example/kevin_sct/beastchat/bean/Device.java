package com.example.kevin_sct.beastchat.bean;

/**
 * Created by kevin_sct on 5/16/17.
 */

public class Device {
    private String deviceName;


    public int getBundlestate() {
        return bundlestate;
    }

    public void setBundlestate(int bundlestate) {
        this.bundlestate = bundlestate;
    }

    private int bundlestate;

    public Device(String deviceName, String deviceAddress, int bundlestate) {
        this.deviceAddress = deviceAddress;
        this.deviceName = deviceName;
        this.bundlestate = bundlestate;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    private String deviceAddress;
}
