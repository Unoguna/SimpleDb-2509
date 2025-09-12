package com.back;

import lombok.Getter;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class Sql {
    private final SimpleDb simpleDb;
    private final StringBuilder sb = new StringBuilder();
    @Getter
    private final List<Object> params = new ArrayList<>();

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    public Sql append(String sqlPart, Object... paramValues) {
        if (!sb.isEmpty()) sb.append(" ");
        sb.append(sqlPart);
        params.addAll(Arrays.asList(paramValues));
        return this;
    }

    // IN절 지원: appendIn("id IN", List.of(1,2,3))
    public Sql appendIn(String prefix, List<?> inParams) {
        if (inParams == null || inParams.isEmpty()) {
            sb.append(prefix).append(" (NULL)"); // 항상 false
            return this;
        }
        sb.append(prefix).append(" (");
        for (int i = 0; i < inParams.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("?");
            params.add(inParams.get(i));
        }
        sb.append(")");
        return this;
    }

    public String getSql() {
        return sb.toString();
    }


    // INSERT 실행 → 생성된 PK 반환
    public long insert() {
        try {
            Connection conn = simpleDb.getConnection();

            //RETURN_GENERATED_KEYS 옵션을 주면, INSERT 실행 후 DB가 생성한 AUTO_INCREMENT 값을 JDBC가 꺼낼 수 있게 된다.
            try (PreparedStatement ps = conn.prepareStatement(getSql(), Statement.RETURN_GENERATED_KEYS)) {
                bindParams(ps);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    // 파라미터 바인딩
    private void bindParams(PreparedStatement ps) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    public int update() {
        try {
            Connection conn = simpleDb.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(getSql())) {
                bindParams(ps);
                return ps.executeUpdate(); // 영향을 받은 행 수 반환
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int delete() {
        try {
            Connection conn = simpleDb.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(getSql())) {
                bindParams(ps);
                return ps.executeUpdate(); // 영향을 받은 행 수 반환
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Object>> selectRows() {
        List<Map<String, Object>> rows = new ArrayList<>();

        try {
            Connection conn = simpleDb.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(getSql())) {
                bindParams(ps);

                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();

                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = meta.getColumnLabel(i);
                            Object value = rs.getObject(i);
                            row.put(columnName, value);
                        }
                        rows.add(row);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return rows;
    }

    public List<Article> selectRows(Class<Article> articleClass){
        return null;
    }

    public Map<String, Object> selectRow() {
        try {
            Connection conn = simpleDb.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(getSql())) {
                bindParams(ps);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null; // 결과가 없으면 null

                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();

                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = meta.getColumnLabel(i); // 컬럼명
                        Object value = rs.getObject(i);            // 값
                        row.put(columnName, value);
                    }
                    return row;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Article selectRow(Class<Article> articleClass){
        return null;
    }

    public LocalDateTime selectDatetime() {
        try {
            Connection conn = simpleDb.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(getSql())) {
                bindParams(ps);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Timestamp ts = rs.getTimestamp(1);
                        if (ts != null) {
                            return ts.toLocalDateTime();
                        }

                        /*
                        * NOTE:
                        * - 아래의 코드처럼 getObject(..., LocalDateTime.class)를 쓰면 Timestamp 없이 바로 LocalDateTime을 얻을 수 있음
                        * - 그러나 구버전 JDBC 드라이버는 이를 지원하지 않을 수 있음
                        * - 또한 Timestamp는 DATE/TIME/DATETIME/TIMESTAMP 모두 커버 가능
                        * => 호환성과 안정성을 위해 Timestamp를 거쳐 변환하는 방식을 사용
                        */
                        // return rs.getObject(1, LocalDateTime.class);
                    }
                    throw new IllegalStateException("쿼리 결과가 없습니다: " + getSql());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Long selectLong() {
        try {
            Connection conn = simpleDb.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(getSql())) {
                bindParams(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Object value = rs.getObject(1);
                        if (value == null) return null;
                        if (value instanceof Number num) {
                            return num.longValue();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public String selectString() {
        try {
            Connection conn = simpleDb.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(getSql())) {
                bindParams(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Object value = rs.getObject(1);
                        if (value == null) return null;
                        if (value instanceof String str) {
                            return str;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public Boolean selectBoolean() {
        try {
            Connection conn = simpleDb.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(getSql())) {
                bindParams(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Object value = rs.getObject(1);
                        if (value == null) return null;
                        if (value instanceof Boolean bool) {
                            return bool;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public Sql append(String s, int i, int i1) {
        return null;
    }

    public void appendIn(String s, int i, int i1, int i2) {
    }

    public Sql appendIn(String s, Long[] ids) {
        return null;
    }

    public List<Long> selectLongs() {
        return List.of();
    }

    public Sql appendIn(String s, String 새_제목, String 새_내용) {
        return null;
    }

}
