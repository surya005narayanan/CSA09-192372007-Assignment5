import java.io.BufferedReader;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public class CampusMigrationTool {
    public static void inspectDatabase(Connection con) throws SQLException {
        DatabaseMetaData meta = con.getMetaData();
        System.out.println("=== Database Environment Metadata ===");
        System.out.println("Database Product : " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
        System.out.println("JDBC Driver      : " + meta.getDriverName() + " " + meta.getDriverVersion());
        System.out.print("Tables Present   : ");
        try (ResultSet tables = meta.getTables(con.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                System.out.print(tables.getString("TABLE_NAME") + " ");
            }
        }
        System.out.println("\n=====================================");
    }

    public static void migrateFileToDb(Connection con, Path csvFile) throws Exception {
        List<String[]> validRows = new ArrayList<>();
        List<String> errorReport = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(csvFile)) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                String[] parts = line.split(",");
                if (parts.length != 5) {
                    errorReport.add("Line " + lineNo + ": Invalid field count -> " + line);
                    continue;
                }
                try {
                    Integer.parseInt(parts[3].trim());
                    Double.parseDouble(parts[4].trim());
                    validRows.add(parts);
                } catch (NumberFormatException ex) {
                    errorReport.add("Line " + lineNo + ": Invalid numeric route/amount -> " + line);
                }
            }
        }

        int loaded = PassLoader.loadAll(con, validRows);
        System.out.println("Migration Complete. Loaded: " + loaded + " | Failed Validation: " + errorReport.size());
        errorReport.forEach(System.err::println);
    }
}
