package config;

import java.io.*;
import java.util.*;

public class ServerConf {
    public int port;
    public final Map<String, String> host2root = new HashMap<>();

    public ServerConf(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (line.startsWith("listen")) {
                port = Integer.parseInt(line.split("\\s+")[1]);
            } else if (line.startsWith("<VirtualHost")) {
                String host = line.split("\\s+")[1].replace(">", "");
                while (!(line = br.readLine().trim()).startsWith("</VirtualHost>")) {
                    if (line.startsWith("DocumentRoot")) {
                        String root = line.split("\\s+")[1];
                        host2root.put(host, root);
                    }
                }
            }
        }
        br.close();
    }
}
