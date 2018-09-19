package se.kth.chaos.pagent.examples;

public class ArrayPOneOnTTorrent {
    public static void main(String[] args) {
        Process process = null;
        String rootPath = "ttorrent_evaluation_1.5/array_pone";
        String javaagentPath = System.getProperty("user.dir") + "/target/foagent-perturbation-jar-with-dependencies.jar";
        String endingPattern = "BitTorrent client signing off";
        String threadName = "ttorrent-1.5-client.jar";
        String osName = System.getProperty("os.name");

        
    }
}
