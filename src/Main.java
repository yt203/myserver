package src;
import config.ServerConf;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class Main {
    static ServerConf conf;
    static final SimpleDateFormat rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
    static { rfc1123.setTimeZone(TimeZone.getTimeZone("GMT")); }

    /* ④ 线程池（给心跳打印用） */
    static final ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    public static void main(String[] args) throws Exception {
        conf = new ServerConf("config/server.conf");
        try (ServerSocket server = new ServerSocket(conf.port)) {
            System.out.println("Listening on " + conf.port);
            while (true) {
                Socket cli = server.accept();
                pool.execute(() -> handle(cli));
            }
        }
    }

    static void handle(Socket cli) {
        try (BufferedReader in  = new BufferedReader(new InputStreamReader(cli.getInputStream()));
             PrintStream     out = new PrintStream(cli.getOutputStream())) {

            String reqLine = in.readLine();
            if (reqLine == null) return;
            String[] parts = reqLine.split(" ");
            if (parts.length != 3 || !parts[0].equals("GET")) {
                sendError(400, "Bad Request", out);  return;
            }
            String uri = parts[1];

            String host = null, line;
            while ((line = in.readLine()) != null && line.length() > 0) {
                if (line.toLowerCase().startsWith("host:")) host = line.substring(5).trim();
            }
            if (host == null) host = "zzh";

            String docRoot = conf.host2root.getOrDefault(host, conf.host2root.get("zzh"));
            File file = new File(docRoot, uri);
            if (uri.contains("..")) { sendError(400, "Bad Request", out); return; }

            /* ===== d. 心跳监控 5pt ===== */
            if (uri.equals("/heartbeat")) {
                // ④ 打印活跃线程数 & 队列长度
                System.out.printf("[heartbeat] active=%d  queue=%d%n",
                                  pool.getActiveCount(), pool.getQueue().size());
                // ② 固定响应
                out.print("HTTP/1.0 200 OK\r\n");
                out.print("Content-Length: 2\r\n");
                out.print("\r\n");
                out.print("OK");
                return;
            }

            if (uri.startsWith("/cgi-bin/")) { doCGI(file, uri, host, out, in); return; }
            if (!file.isFile()) { sendError(404, "Not Found", out); return; }

            long lastMod = file.lastModified();
            String ifMod = null;
            // （略去 If-Modified-Since 解析，同之前代码）
            byte[] body = Files.readAllBytes(file.toPath());
            out.printf("HTTP/1.0 200 OK\r\n");
            out.printf("Date: %s\r\n", rfc1123.format(new Date()));
            out.printf("Server: myserver/0.1\r\n");
            out.printf("Last-Modified: %s\r\n", rfc1123.format(new Date(lastMod)));
            out.printf("Content-Type: text/html\r\n");
            out.printf("Content-Length: %d\r\n", body.length);
            out.printf("\r\n");
            out.write(body);

        } catch (Exception e) { e.printStackTrace(); }
    }

    /* 以下 CGI / error / 304 方法保持原样，直接复制即可 */
    static void doCGI(File file, String uri, String host, PrintStream out,
                      BufferedReader in) throws IOException {
        if (!file.exists() || !Files.isExecutable(file.toPath())) {
            sendError(404, "CGI Not Found or Not Executable", out);  return;
        }
        ProcessBuilder pb = new ProcessBuilder(file.getAbsolutePath());
        Map<String,String> env = pb.environment();
        env.put("REQUEST_METHOD", "GET");
        env.put("QUERY_STRING",     uri.contains("?") ? uri.substring(uri.indexOf('?')+1) : "");
        env.put("CONTENT_LENGTH",   "");
        env.put("REMOTE_ADDR",      "127.0.0.1");
        env.put("SERVER_NAME",      host);
        env.put("SCRIPT_NAME",      uri.split("\\?")[0]);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        out.print("HTTP/1.0 200 OK\r\n\r\n");
        try (InputStream cin = p.getInputStream()) { cin.transferTo(out); }
    }

    static void sendError(int code, String msg, PrintStream out) {
        String body = "<h1>" + code + " " + msg + "</h1>";
        out.printf("HTTP/1.0 %d %s\r\n", code, msg);
        out.printf("Content-Type: text/html\r\n");
        out.printf("Content-Length: %d\r\n", body.length());
        out.printf("\r\n");
        out.print(body);
    }
}
