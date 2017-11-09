package com.example.matthias.device;

import java.io.Serializable;

/**
 * Created by Matthias on 2017-08-09.
 */

public class DeviceInfo implements Serializable {
    private String serverIP;
    private String serverPort;
    private String serverPasswd;

    public void setServerIP(String IP) {
        this.serverIP = IP;
    }

    public void setServerPort(String port) {
        this.serverPort = port;
    }

    public void setServerPasswd(String passwd) {
        this.serverPasswd = passwd;
    }

    public String getServerIP() {
        return this.serverIP;
    }

    public String getServerPort() {
        return this.serverPort;
    }

    public String getServerPasswd() {
        return this.serverPasswd;
    }
}
