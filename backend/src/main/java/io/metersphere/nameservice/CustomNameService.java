package io.metersphere.nameservice;

import io.metersphere.commons.utils.LogUtil;
import sun.net.spi.nameservice.NameService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CustomNameService implements NameService {

    private static Map<String, InetAddress> mapping = new ConcurrentHashMap<String, InetAddress>();

    @Override
    public InetAddress[] lookupAllHostAddr(String host) throws UnknownHostException {
        if (host == null || host.trim() == "") {
            throw new UnknownHostException();
        }
        InetAddress ip = (InetAddress) mapping.get(host);
        if (ip == null) {
            throw new UnknownHostException();
        }
        return new InetAddress[]{ip};
    }

    @Override
    public String getHostByAddr(byte[] bytes) throws UnknownHostException {
        return null;
    }

    public static void load(Map<String, String> hostMap) {
        if (hostMap == null || hostMap.size() == 0) {
            return;
        }
        for (String host : hostMap.keySet()) {
            String ip = hostMap.get(host);
            try {
                InetAddress addr = InetAddress.getByName(ip);
                mapping.put(host.trim(), addr);
            } catch (UnknownHostException e) {
                LogUtil.error(e.getMessage());
            }
        }
        LogUtil.info("Load hosts mapping success");
    }

    public static void add(String host, String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            mapping.put(host.trim(), addr);
        } catch (UnknownHostException e) {
            LogUtil.error(e.getMessage());
        }
    }

    public static void clear() {
        mapping.clear();
        LogUtil.info("Clean hosts mapping success");
    }

}
