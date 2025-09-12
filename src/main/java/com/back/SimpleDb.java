package com.back;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@RequiredArgsConstructor
@Setter
public class SimpleDb {
    private String host;
    private String username;
    private String password;
    private String dbName;

    private boolean devMode;

    // 스레드별 Connection 관리
    private ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    public SimpleDb(String host, String username, String password, String dbName) {

        //굳이 없어도 자동으로 드라이버를 로드하지만 호환성, 명시성 때문에 넣어주는게 좋다.
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // MySQL 드라이버 로드
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL Driver not found", e);
        }

        this.host = host;
        this.username = username;
        this.password = password;
        this.dbName = dbName;

        // DB 및 기본 테이블 보장
        ensureDatabaseExists();
        ensureArticleTableExists();
    }

    private String buildUrlWithoutDb() {
        return String.format(
                "jdbc:mysql://%s:3306/?serverTimezone=Asia/Seoul&characterEncoding=UTF-8",
                host
        );
    }

    private String buildUrlWithDb() {
        return String.format(
                "jdbc:mysql://%s:3306/%s?serverTimezone=Asia/Seoul&characterEncoding=UTF-8",
                host, dbName
        );
    }

    private void ensureDatabaseExists() {
        try (Connection conn = DriverManager.getConnection(buildUrlWithoutDb(), username, password);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName +
                    " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure database exists: " + dbName, e);
        }
    }

    private void ensureArticleTableExists() {
        try (Connection conn = DriverManager.getConnection(buildUrlWithDb(), username, password);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS article (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    createdDate DATETIME NOT NULL,
                    modifiedDate DATETIME NOT NULL,
                    title VARCHAR(255) NOT NULL,
                    body TEXT NOT NULL
                )
                """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure article table exists", e);
        }
    }


    /*
        스레드 A가 getConnection()을 부르면 A 전용 Connection이 생김
        스레드 B가 호출하면 B 전용 Connection이 따로 생김
        A, B 스레드는 서로의 커넥션을 건드리지 않음
     */
    public Connection getConnection() throws SQLException {
        Connection conn = connectionHolder.get();
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(buildUrlWithDb(), username, password);
            connectionHolder.set(conn);
        }
        return conn;
    }

    // Sql 빌더 생성
    public Sql genSql() {
        return new Sql(this);
    }

    public void run(String sql) {
    }

    public void run(String sql, String title, String body, boolean isBlind) {
    }

    public void startTransaction() {
    }

    public void rollback() {
    }

    public void commit() {
    }
    // 스레드별 Connection 닫기
    public void close() {
        try {
            Connection conn = connectionHolder.get();
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            connectionHolder.remove();
        }
    }
}
