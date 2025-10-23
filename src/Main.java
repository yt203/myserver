package src;
import config.ServerConf;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Date;

public class Main {
    static ServerConf conf;                       // 全局配置

    public static void main(String[] args) throws Exception {
        conf = new ServerConf("config/server.conf");
        try (ServerSocket server = new ServerSocket(conf.port)) {
            System.out.println("Listening on " + conf.port);
            while (true) {
                Socket cli = server.accept();
                handle(cli);                    // 单线程阻塞版
            }
        }
    }

    /* 处理一次 HTTP/1.0 GET 请求 */
    static void handle(Socket cli) {
        try (BufferedReader in  = new BufferedReader(new InputStreamReader(cli.getInputStream()));
             PrintStream     out = new PrintStream(cli.getOutputStream())) {

            /* 1. 读请求行 */
            String reqLine = in.readLine();
            if (reqLine == null) return;
            System.out.println("[" + new Date() + "] " + reqLine);
            String[] parts = reqLine.split(" ");
            if (parts.length != 3 || !parts[0].equals("GET")) {
                sendError(400, "Bad Request", out);
                return;
            }
            String uri = parts[1];

            /* 2. 读头部，取 Host */
            String host = null, line;
            while ((line = in.readLine()) != null && line.length() > 0) {
                if (line.toLowerCase().startsWith("host:"))
                    host = line.substring(5).trim();
            }
            if (host == null) host = "zzh";          // 缺省到第一个虚拟主机

            /* 3. 映射本地文件 */
            String docRoot = conf.host2root.getOrDefault(host, conf.host2root.get("zzh"));
            File file = new File(docRoot, uri);
            if (!file.isFile() || uri.contains("..")) {   // 简单安全校验
                sendError(404, "Not Found", out);
                return;
            }

            /* 4. 发 200 + 文件 */
            byte[] body = Files.readAllBytes(file.toPath());
            out.printf("HTTP/1.0 200 OK\r\n");
            out.printf("Date: %s\r\n", new Date());
            out.printf("Server: myserver/0.1\r\n");
            out.printf("Content-Type: text/html\r\n");
            out.printf("Content-Length: %d\r\n", body.length);
            out.printf("\r\n");
            out.write(body);

        } catch (Exception e) { e.printStackTrace(); }
    }

    /* 统一错误响应 */
    static void sendError(int code, String msg, PrintStream out) {
        String body = "<h1>" + code + " " + msg + "</h1>";
        out.printf("HTTP/1.0 %d %s\r\n", code, msg);
        out.printf("Content-Type: text/html\r\n");
        out.printf("Content-Length: %d\r\n", body.length());
        out.printf("\r\n");
        out.print(body);
    }
}
