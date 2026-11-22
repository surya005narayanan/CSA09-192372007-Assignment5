// Benchmark harness for Task B1.3 (Buffering) and B1.4 (Linear Search Growth)
import java.io.*;
import java.nio.file.*;

public class FileBenchmark {
    public static long countUnbuffered(Path p) throws IOException {
        long lines = 0;
        try (InputStream in = Files.newInputStream(p)) {
            int b;
            while ((b = in.read()) != -1) if (b == '\n') lines++;
        }
        return lines;
    }

    public static long countBuffered(Path p) throws IOException {
        long lines = 0;
        byte[] buf = new byte[65536];
        try (InputStream in = Files.newInputStream(p)) {
            int n;
            while ((n = in.read(buf)) != -1) {
                for (int i = 0; i < n; i++) if (buf[i] == '\n') lines++;
            }
        }
        return lines;
    }
}
