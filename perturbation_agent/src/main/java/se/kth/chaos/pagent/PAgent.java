package se.kth.chaos.pagent;

import com.opencsv.CSVReader;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PAgent {
    public static Map<String, PerturbationPoint> perturbationPointsMap = new HashMap<String, PerturbationPoint>();
    public static boolean monitoringThread = false;

    public static int pOneArrayReading(int readingIndex, String perturbationPointKey) {
        PerturbationPoint perturbationPoint = perturbationPointsMap.getOrDefault(perturbationPointKey, null);
        int result = readingIndex;

        if (perturbationPoint != null && perturbationPoint.mode.equals("array_pone") && perturbationPoint.perturbationCountdown > 0) {
            if (shouldActivate(perturbationPoint.chanceOfFailure)) {
                System.out.println("INFO PAgent array_pone perturbation activated in "
                        + perturbationPoint.className + "/" + perturbationPoint.methodName
                        + ", countDown: " + perturbationPoint.perturbationCountdown);

                perturbationPoint.perturbationCountdown = perturbationPoint.perturbationCountdown - 1;
                result = result + 1;
            }
        }

        return result;
    }

    public static void perturbationOrNot(String perturbationPointKey, Throwable oriException) throws Throwable {
        PerturbationPoint perturbationPoint = perturbationPointsMap.getOrDefault(perturbationPointKey, null);
        if (perturbationPoint != null && perturbationPoint.mode.equals("fo")) {
            // perturbation mode is on, so ...
            System.out.println("INFO PAgent perturbation mode is on, ...");
            System.out.println(String.format("INFO PAgent %s @ %s/%s", oriException.getClass().toString(), perturbationPoint.className, perturbationPoint.methodName));
        } else {
            throw oriException;
        }
    }

    public static boolean shouldActivate(double chanceOfFailure) {
        Random random = new Random();
        return random.nextDouble() < chanceOfFailure;
    }

    public static void registerPerturbationPoint(PerturbationPoint perturbationPoint, AgentArguments arguments) {
        if (!perturbationPointsMap.containsKey(perturbationPoint.key)) {
            if (monitoringThread == false) {
                monitoringCsvFile(arguments.csvfilepath());
                monitoringThread = true;
            }
            // since monitoring thread is on, adding perturbationPoints might be executed twice
            perturbationPointsMap.put(perturbationPoint.key, perturbationPoint);

            // register to a csv file
            File csvFile = new File(arguments.csvfilepath());
            try {
                PrintWriter out = null;
                if (csvFile.exists()) {
                    out = new PrintWriter(new FileWriter(csvFile, true));
                    out.println(String.format("%s,%s,%s,%s,%s", perturbationPoint.key, perturbationPoint.className,
                            perturbationPoint.methodName, perturbationPoint.indexNumber, perturbationPoint.mode));
                } else {
                    csvFile.createNewFile();
                    out = new PrintWriter(new FileWriter(csvFile));
                    out.println("key,className,methodName,indexNumber,mode");
                    out.println(String.format("%s,%s,%s,%s,%s", perturbationPoint.key, perturbationPoint.className,
                            perturbationPoint.methodName, perturbationPoint.indexNumber, perturbationPoint.mode));
                }
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void monitoringCsvFile(String filepath) {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        exec.execute(new Runnable(){
            public void run() {
                Long lastModified = 0L;
                while (true) {
                    File file = new File(filepath);
                    if (file.exists() && file.lastModified() > lastModified) {
                        updateModesByFile(filepath);
                        lastModified = file.lastModified();
                        System.out.println("INFO PAgent csv file was updated, update the perturbationPointsMap now");
                    }

                    try {
                        Thread.currentThread().sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public static void updateModesByFile(String filepath) {
        CSVReader reader = null;
        List<String[]> foPoints = null;

        try {
            reader = new CSVReader(new FileReader(filepath));
            foPoints = reader.readAll();
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, String> kv = new HashMap<String, String>();
        for (int i = 1; i < foPoints.size(); i++) {
            String[] line = foPoints.get(i);
            PerturbationPoint foPoint = perturbationPointsMap.get(line[0]);
            if (foPoint != null) {
                foPoint.mode = line[3];
                perturbationPointsMap.put(line[0], foPoint);
            }
        }
    }

    public static void printLog(String log) {
        System.err.println(log);
    }
}