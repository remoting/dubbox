package com.alibaba.dubbo.monitor.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.monitor.MonitorService;

import java.io.Serializable;

/**
 * Created by yong on 15/12/6.
 */
public class LogModel implements Serializable {
    private String server;
    private String application;
    private String side;
    private String service;
    private String host;
    private int port;

    public LogModel(URL url) {
        this.server = url.getAddress();
        this.host = url.getHost();
        this.port = url.getPort();
        this.side = url.getParameter(Constants.SIDE_KEY);
        this.application = url.getParameter(MonitorService.APPLICATION);
        this.service = url.getParameter(MonitorService.INTERFACE);
    }

    public String getServer() {
        return server;
    }

    public String getApplication() {
        return application;
    }

    public String getSide() {
        return side;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getHost(){
        return this.host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((side == null) ? 0 : side.hashCode());
        result = prime * result + ((application == null) ? 0 : application.hashCode());
        result = prime * result + ((server == null) ? 0 : server.hashCode());
        result = prime * result + ((service == null) ? 0 : service.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LogModel other = (LogModel) obj;

        if (side == null) {
            if (other.side != null)
                return false;
        } else if (!side.equals(other.side))
            return false;

        if (application == null) {
            if (other.application != null)
                return false;
        } else if (!application.equals(other.application))
            return false;


        if (server == null) {
            if (other.server != null)
                return false;
        } else if (!server.equals(other.server))
            return false;

        if (service == null) {
            if (other.service != null)
                return false;
        } else if (!service.equals(other.service))
            return false;

        return true;
    }
}
