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

public class ChaosMonkey {
    public static Map<String, FailureObliviousPoint> foPointsMap = new HashMap<String, FailureObliviousPoint>();
    public static MemcachedClient memcachedClient = null;

    public static void failureObliviousOrNot(String className, String methodName, String key, Throwable oriException) throws Throwable {
        FailureObliviousPoint foPoint = foPointsMap.getOrDefault(key, null);
        if (foPoint != null && foPoint.mode == "fo") {

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
            System.out.println(String.format("INFO ChaosMachine getMode time out, %s @ %s", executedMethodName, executedClassName));
        }

        return mode;
    }

    public static void printInfo(String tcIndexInfo) {
        System.out.println(String.format("INFO ChaosMachine try catch index %s", tcIndexInfo));
    }

    public static void registerTrycatchInfo(AgentArguments arguments, String memcachedKey, String value) {
        // register to a csv file
        File csvFile = new File(arguments.csvfilepath());
        try {
            PrintWriter out = null;
            if (csvFile.exists()) {
                out = new PrintWriter(new FileWriter(csvFile, true));
                out.println(String.format("%s,%s,%s", memcachedKey, "no", value));
            } else {
                csvFile.createNewFile();
                out = new PrintWriter(new FileWriter(csvFile));
                out.println("tcIndex,methodName,className,isCovered,mode");
                out.println(String.format("%s,%s,%s", memcachedKey, "no", value));
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // register to memcached server
        // lots of timeout issues so we only do the file registeration first
        // then the controller will register all the info in memcached server
        /*
        try {
            MemcachedClient client = new XMemcachedClient(arguments.memcachedHost(), arguments.memcachedPort());
            client.set(memcachedKey, 0, value);
            client.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (MemcachedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            System.out.println(String.format("INFO ChaosMachine registerTrycatchInfo time out (%s)", memcachedKey));
        }
        */
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
            System.out.println(String.format("INFO ChaosMachine registerTrycatchToMemcached time out (%s)", tcIndexInfo));
        }
    }
}