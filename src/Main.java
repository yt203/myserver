package src;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;

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
        try (s; BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
             OutputStream out = s.getOutputStream()) {

            String line = in.readLine();
            if (line == null || !line.startsWith("GET ")) return;
            String uri = line.split(" ")[1];
            if (uri.equals("/")) uri = "/1mb.test";

            Path file = WWW.resolve(uri.substring(1));
            if (!Files.isRegularFile(file)) {
                byte[] body = "404 Not Found".getBytes();
                out.write(("HTTP/1.0 404\r\nContent-Length: " + body.length + "\r\n\r\n").getBytes());
                out.write(body); return;
            }

            byte[] body = Files.readAllBytes(file);
            out.write(("HTTP/1.0 200 OK\r\nContent-Length: " + body.length + "\r\n\r\n").getBytes());
            out.write(body);
        } catch (IOException ignore) {}
    }
}
