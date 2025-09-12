## 🚀 SQL 빌더
### 🔹`executeUpdate()`의 역할

- **INSERT / UPDATE / DELETE / DDL**(CREATE, DROP, TRUNCATE…) 같이 👉 **결과가 “변경된 행 수”로 나타나는 쿼리**를 실행할 때 사용.

- 리턴값: `int` → 변경된 row 수

예시:
```java
PreparedStatement ps = conn.prepareStatement("UPDATE article SET title=? WHERE id=?");
ps.setString(1, "새 제목");
ps.setLong(2, 1);
int updatedRows = ps.executeUpdate();
```

➡️ `updatedRows` = 수정된 행 수

### 🔹 `executeQuery()`의 역할

- **SELECT** 같이 👉 **결과가 ResultSet으로 나오는 쿼리** 실행할 때 사용.

- 리턴값: `ResultSet`

예시:
```java
PreparedStatement ps = conn.prepareStatement("SELECT id FROM article WHERE id=?");
ps.setLong(1, 1);
ResultSet rs = ps.executeQuery();
```
