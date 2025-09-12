package com.back;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Sql {
    private final SimpleDb simpleDb;
    private final StringBuilder sb = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    public Sql(SimpleDb simpleDb) {
        this.simpleDb = simpleDb;
    }

    // 단순 문자열 추가
    public Sql append(String sqlPart) {
        if (sb.length() > 0) sb.append(" ");
        sb.append(sqlPart);
        return this;
    }

    // 파라미터 포함된 문자열 추가
    public Sql append(String sqlPart, Object param) {
        if (sb.length() > 0) sb.append(" ");
        sb.append(sqlPart);
        params.add(param);
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

    public List<Object> getParams() {
        return params;
    }


    // INSERT 실행 → 생성된 PK 반환
    public long insert() {
        try {
            Connection conn = simpleDb.getConnection();
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

    // UPDATE/DELETE 실행 → 영향받은 행 수 반환
    public int updateOrDelete() {
        try {
            Connection conn = simpleDb.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(getSql())) {
                bindParams(ps);
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 단일 값 조회
    public <T> T selectOne(Class<T> type) {
        try {
            Connection conn = simpleDb.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(getSql())) {
                bindParams(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Object value = rs.getObject(1);
                        return type.cast(value);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    // 파라미터 바인딩
    private void bindParams(PreparedStatement ps) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    public int update() {
        return 0;
    }

    public int delete() {
        return 0;
    }

    public List<Map<String, Object>> selectRows() {
        return List.of();
    }

    public List<Article> selectRows(Class<Article> articleClass){
        return null;
    }

    public Map<String, Object> selectRow() {
        return Map.of();
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

    public void append(String s, int i, int i1, int i2) {
    }

    public void append(String s, int i, int i1, int i2, int i3) {
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
