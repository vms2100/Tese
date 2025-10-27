package API;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

public class Metrics {

    private static final Path CSV_PATH = Paths.get("api_metrics.csv");
    private static final AtomicBoolean HEADER_WRITTEN = new AtomicBoolean(false);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void log(String endpoint, String params, long dbTime, long parseTime, long totalTime,
                           int bytesSent, int statusCode, long memoriaKB, long cpuMicros) {
        try {
            boolean writeHeader = !HEADER_WRITTEN.getAndSet(true);
            Files.createDirectories(CSV_PATH.getParent() != null ? CSV_PATH.getParent() : Paths.get("."));

            try (BufferedWriter writer = Files.newBufferedWriter(CSV_PATH, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                if (writeHeader) {
                    writer.write("dia,endpoint,params,db_time_nanos,parser_time_nanos,total_time_nanos,bytes_sent,status,memoria_b,cpu_nanos");
                    writer.newLine();
                }

                String dia = LocalDate.now().format(DATE_FORMAT);
                params = sanitize(params);

                String row = String.format("%s,%s,%s,%d,%d,%d,%d,%d,%d,%d",
                        dia, endpoint, params, dbTime, parseTime, totalTime, bytesSent, statusCode, memoriaKB, cpuMicros);
                writer.write(row);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String sanitize(String input) {
        if (input == null) return "";
        return input.replaceAll("[\\n\\r,]", " ");
    }



}
