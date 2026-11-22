import java.nio.file.*;
import java.sql.*;

public class AdversarialTest {
    public static boolean unsafeLogin(Connection con, String id, String pwd) throws SQLException {
        String sql = "SELECT * FROM users WHERE id='" + id + "' AND pwd='" + pwd + "'";
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next();
        }
    }

    public static boolean safeLogin(Connection con, String id, String pwd) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ? AND pwd = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, pwd);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // 1. SQL Injection Test
        try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/test_db", "root", "password")) {
            String attackId = "admin' OR '1'='1";
            String attackPwd = "wrong_password";

            System.out.println("Unsafe Login Result: " + unsafeLogin(con, attackId, attackPwd)); // TRUE (Compromised!)
            System.out.println("Safe Login Result:   " + safeLogin(con, attackId, attackPwd));   // FALSE (Blocked!)
        }

        // 2. Adversarial Damaged Bus-Pass File Test
        Path badFile = Files.createTempFile("bad_passes", ".csv");
        Files.writeString(badFile,
            "P2001,192311010,ValidUser,5,3000.0\n" +
            "P2002,ONLY_TWO_FIELDS\n" +                 // Wrong field count
            "\n" +                                      // Empty line
            "P2003,192311011,BadAmount,5,NOT_A_NUMBER\n"// Non-numeric amount
        );
        PassStore store = new PassStore(badFile);
        System.out.println("Valid record found: " + (store.find("P2001") != null));
        System.out.println("Malformed fields skipped: " + (store.find("P2002") == null));
        System.out.println("Route 5 valid list size: " + store.listByRoute(5).size());
        Files.delete(badFile);
    }
}
