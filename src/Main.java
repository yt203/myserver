package src;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.nio.channels.*;

/** 阻塞式 Thread-Per-Connection，线程池 200 */
public class Main {
    private static final int PORT = 2345;
    private static final int THREADS = 200;
    private static final Path WWW = Paths.get("www");

    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(PORT);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        System.out.println("BlockingServer listen " + PORT + " pool=" + THREADS);

        while (true) {
            Socket s = ss.accept();
            pool.execute(() -> handle(s));
        }
    }

    private static void handle(Socket s) {
        try (s;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(s.getInputStream()));
             OutputStream raw = s.getOutputStream()) {

            String line = in.readLine();
            if (line == null || !line.startsWith("GET ")) return;
            String uri = line.split(" ")[1];
            if (uri.equals("/")) uri = "/1mb.test";

            while ((line = in.readLine()) != null && !line.isEmpty()) {}

            Path file = WWW.resolve(uri.substring(1));
            if (!Files.isRegularFile(file)) {
                byte[] body = "404 Not Found".getBytes();
                raw.write(("HTTP/1.0 404\r\nContent-Length: " +
                        body.length + "\r\n\r\n").getBytes());
                raw.write(body);
                return;
            }

            String mode = System.getProperty("mode", "0");
            long len = Files.size(file);
            String hdr = "HTTP/1.0 200 OK\r\nContent-Length: " +
                         len + "\r\n\r\n";
            raw.write(hdr.getBytes());

            /* ---------- 零拷贝 / 传统 分支 ---------- */
            try (FileChannel fc = "0".equals(mode) ?
                    FileChannel.open(file, StandardOpenOption.READ) : null) {
                if ("0".equals(mode)) {              // 零拷贝
                    try (WritableByteChannel sockCh =
                                 Channels.newChannel(raw)) {
                        fc.transferTo(0, len, sockCh);
                    }
                } else {                             // 传统
                    Files.copy(file, raw);
                }
            }
        } catch (IOException ignore) {}
        System.out.println("Request line=[" + line + "]");
    }
}
