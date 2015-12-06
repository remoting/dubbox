/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.monitor.dubbo;

import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.monitor.Monitor;
import com.alibaba.dubbo.monitor.MonitorService;
import com.alibaba.dubbo.rpc.Invoker;

/**
 * DubboMonitor
 * 
 * @author william.liangf
 */
public class DubboMonitor implements Monitor {
    
    private static final Logger logger = LoggerFactory.getLogger(DubboMonitor.class);
    
    private static final int LENGTH = 10;
    
    // 定时任务执行器
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3, new NamedThreadFactory("DubboMonitorSendTimer", true));

    // 统计信息收集定时器
    private final ScheduledFuture<?> sendFuture;
    
    private final Invoker<MonitorService> monitorInvoker;

    private final MonitorService monitorService;

    private final long monitorInterval;
    
    private final ConcurrentMap<LogModel, List<String>> statisticsMap = new ConcurrentHashMap<LogModel, List<String>>();

    public DubboMonitor(Invoker<MonitorService> monitorInvoker, MonitorService monitorService) {
        this.monitorInvoker = monitorInvoker;
        this.monitorService = monitorService;
        this.monitorInterval = monitorInvoker.getUrl().getPositiveParameter("interval", 60000);
        // 启动统计信息收集定时器
        sendFuture = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                // 收集统计信息
                try {
                    send();
                } catch (Throwable t) { // 防御性容错
                    logger.error("Unexpected error occur at send statistic, cause: " + t.getMessage(), t);
                }
            }
        }, monitorInterval, monitorInterval, TimeUnit.MILLISECONDS);
    }
    
    public void send() {
        if (logger.isInfoEnabled()) {
            logger.info("Send statistics to monitor " + getUrl());
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        for (Map.Entry<LogModel, List<String>> entry : statisticsMap.entrySet()) {
            // 获取已统计数据
            LogModel statistics = entry.getKey();
            URL url = new URL(Constants.COUNT_PROTOCOL, statistics.getHost(), statistics.getPort(), statistics.getSide());
            List<String> reference = entry.getValue();
            StringBuffer sb = new StringBuffer();
            for (int i=0;i<reference.size();i++){
                sb.append(reference.get(i)+";");
            }
            url = url.addParameter(statistics.getService(), sb.toString());
            // 减掉已统计数据
            reference.clear();
            monitorService.collect(url);
        }
    }
    
    public void collect(URL url) {
        // 读写统计变量
        String method = url.getParameter(MonitorService.METHOD);
        String success = url.getParameter(MonitorService.SUCCESS);
        String elapsed = url.getParameter(MonitorService.ELAPSED);
        String start = url.getParameter(MonitorService.TIMESTAMP);
        String side = url.getParameter(Constants.SIDE_KEY);
        String remote = "";
        if (Constants.CONSUMER_SIDE.equals(side)) {
            remote = url.getParameter(MonitorService.PROVIDER);
        }else{
            remote = url.getParameter(MonitorService.CONSUMER);
        }
        // 初始化原子引用
        LogModel log = new LogModel(url);
        List<String> reference = statisticsMap.get(log);
        if (reference == null) {
            statisticsMap.putIfAbsent(log, new ArrayList<String>());
            reference = statisticsMap.get(log);
        }
        // CompareAndSet并发加入统计数据

        reference.add(method+","+success+","+elapsed+","+start+","+remote);
        statisticsMap.replace(log,reference);
    }

	public List<URL> lookup(URL query) {
		return monitorService.lookup(query);
	}

    public URL getUrl() {
        return monitorInvoker.getUrl();
    }

    public boolean isAvailable() {
        return monitorInvoker.isAvailable();
    }

    public void destroy() {
        try {
            sendFuture.cancel(true);
        } catch (Throwable t) {
            logger.error("Unexpected error occur at cancel sender timer, cause: " + t.getMessage(), t);
        }
        monitorInvoker.destroy();
    }

}