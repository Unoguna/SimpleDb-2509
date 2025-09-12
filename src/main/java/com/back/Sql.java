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
        return null;
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
        return null;
    }

    public Long selectLong() {
        return 0L;
    }

    public String selectString() {
        return "";
    }

    public Boolean selectBoolean() {
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
