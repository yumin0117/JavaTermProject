package kr.ac.yumin.playlist.config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

/** 접속 정보는 실행 폴더의 외부 파일에서만 읽는다. */
public final class Database {
    private Database() {}

    public static Path settingsPath() {
        return Path.of(System.getProperty("playlist.config", "config/db.properties"));
    }

    public static Properties settings() {
        Properties p = new Properties();
        try (Reader in = Files.newBufferedReader(settingsPath(), StandardCharsets.UTF_8)) {
            p.load(in);
        } catch (IOException ignored) {
        }
        return p;
    }

    public static void testAndSave(String url, String username, char[] password)
            throws SQLException, IOException {
        Properties p = new Properties();
        p.setProperty("db.url", url.strip());
        p.setProperty("db.username", username.strip());
        p.setProperty("db.password", new String(password));
        java.util.Arrays.fill(password, '\0');
        try (Connection c = connect(p)) {
            try (PreparedStatement statement = c.prepareStatement("SELECT 1 FROM DUAL");
                    ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("DB 연결을 확인할 수 없습니다.");
            }
        }
        Path path = settingsPath().toAbsolutePath();
        Files.createDirectories(path.getParent());
        try (Writer out = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            p.store(out, "Private Oracle connection settings. Never upload.");
        }
    }

    private static Connection connect(Properties p) throws SQLException {
        for (String key : new String[] {"db.url", "db.username", "db.password"})
            if (p.getProperty(key, "").isBlank())
                throw new SQLException("DB 설정의 URL, 사용자 이름, 비밀번호를 모두 입력해 주세요.");
        Properties info = new Properties();
        info.setProperty("user", p.getProperty("db.username"));
        info.setProperty("password", p.getProperty("db.password"));
        info.setProperty("oracle.net.CONNECT_TIMEOUT", "7000");
        info.setProperty("oracle.jdbc.ReadTimeout", "15000");
        return DriverManager.getConnection(p.getProperty("db.url"), info);
    }

    public static Connection open() throws SQLException {
        if (!Files.exists(settingsPath()))
            throw new SQLException("config/db.properties를 만들고 Oracle 접속 정보를 입력해 주세요.");
        return connect(settings());
    }
}
