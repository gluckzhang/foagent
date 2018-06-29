package se.kth.chaos;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.utils.AddrUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class FOAgent {
    public static Map<String, FailureObliviousPoint> foPointsMap = new HashMap<String, FailureObliviousPoint>();
    public static MemcachedClient memcachedClient = null;

    public static void failureObliviousOrNot(String foPointKey, Throwable oriException) throws Throwable {
        FailureObliviousPoint foPoint = foPointsMap.getOrDefault(foPointKey, null);
        if (foPoint != null && foPoint.mode == "fo") {
            // failure oblivious mode is on, so swallow this exception
            System.out.println("INFO FOAgent failure oblivious mode is on, ignore the following exception");
            System.out.println(String.format("INFO FOAgent %s @ %s/%s", oriException.getClass().toString(), foPoint.className, foPoint.methodName));
        } else {
            throw oriException;
        }
    }

    public static String getMode(String tcIndexInfo, String memcachedHost, int memcachedPort) {
        String mode = null;

        try {
            if (memcachedClient == null) {
                MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(memcachedHost + ":" + memcachedPort));
                builder.setConnectionPoolSize(5);
                memcachedClient = builder.build();
            }
            mode = memcachedClient.get(tcIndexInfo, 1000);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (MemcachedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            mode = "UNKNOWN";
            String executedClassName = Thread.currentThread().getStackTrace()[3].getClassName();
            String executedMethodName = Thread.currentThread().getStackTrace()[3].getMethodName();
            System.out.println(String.format("INFO FOAgent getMode time out, %s @ %s", executedMethodName, executedClassName));
        }

        return mode;
    }

    public static void printInfo(String tcIndexInfo) {
        System.out.println(String.format("INFO FOAgent try catch index %s", tcIndexInfo));
    }

    public static void registerFailureObliviousPoint(FailureObliviousPoint foPoint, AgentArguments arguments) {
        if (!foPointsMap.containsKey(foPoint.key)) {
            foPointsMap.put(foPoint.key, foPoint);

            // register to a csv file
            File csvFile = new File(arguments.csvfilepath());
            try {
                PrintWriter out = null;
                if (csvFile.exists()) {
                    out = new PrintWriter(new FileWriter(csvFile, true));
                    out.println(String.format("%s,%s,%s,%s", foPoint.key, foPoint.className, foPoint.methodName, foPoint.mode));
                } else {
                    csvFile.createNewFile();
                    out = new PrintWriter(new FileWriter(csvFile));
                    out.println("key,className,methodName,mode");
                    out.println(String.format("%s,%s,%s,%s", foPoint.key, foPoint.className, foPoint.methodName, foPoint.mode));
                }
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void registerTrycatchToMemcached(String tcIndexInfo, String defaultMode, String memcachedHost, int memcachedPort) {
        try {
            if (memcachedClient == null) {
                MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(memcachedHost + ":" + memcachedPort));
                builder.setConnectionPoolSize(5);
                memcachedClient = builder.build();
            }
            memcachedClient.set(tcIndexInfo, 0, defaultMode);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (MemcachedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            System.out.println(String.format("INFO FOAgent registerTrycatchToMemcached time out (%s)", tcIndexInfo));
        }
    }
}