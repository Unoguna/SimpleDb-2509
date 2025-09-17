# MySQL/JDBC 유틸리티 클래스 SimpleDb 구현

## 🎯 과제 목표

- 순수 **JDBC**로 경량 DB 유틸리티(**SimpleDb**)를 구현한다.
- **멀티스레드 환경**(예: Spring WebMVC)에서 안전하게 동작하는 **커넥션 관리**를 설계한다.
- **트랜잭션(Commit/Rollback)**, **SQL 빌더**, **DTO/엔티티 매핑** 등 핵심 기능을 스스로 설계/구현한다.
- 제공된 **단위 테스트(SimpleDbTest)** 전 항목 `통과(✅ t001~t019)`를 최종 목표로 한다.

---

## 📦 개발 환경 & 의존성

- Java 21, Gradle
- mysql-connector-j: 9.3.0
- 테스트: JUnit 5, AssertJ
- 직렬화: Jackson (JSR-310 포함)

---

## 🧩 요구사항 정리

### A. 스레드·커넥션 관리

- `SimpleDb` **인스턴스 1개**를 여러 스레드에서 **동시에 공유**해도 안전해야 함.
- **각 스레드는 독립적인 Connection 1개**를 사용한다.
- `simpleDb.close()` 호출 전까지 **스레드별 Connection은 유지**되어야 함.
- 구현 힌트: `ThreadLocal<Connection>` 또는 `Map<ThreadId, Connection>` + 동기화 정책.

### B. SQL 빌더(`Sql`) 기능

- `append(...)`/`appendIn(...)`을 통해 **가변 파라미터 바인딩** 및 **IN 절**을 안전하게 생성.
- 주요 실행 메서드
    - `insert()` → 생성된 **Auto Increment PK** 반환
    - `update()`, `delete()` → 영향 행 수 반환
    - 단일 값 조회: `selectLong()`, `selectString()`, `selectBoolean()`, `selectDatetime()`
    - 다중/단일 행 조회: `selectRows()`, `selectRow()`
    - 매핑 조회: `selectRows(Class<T>)`, `selectRow(Class<T>)`
- LIKE / BETWEEN / ORDER BY FIELD / LIMIT 등 조합을 **문자열 안전성**(바인딩) 유지하며 구성

### C. 트랜잭션 API

- `startTransaction()` → autoCommit=false 설정 및 트랜잭션 시작
- `commit()` / `rollback()` → 현재 스레드의 Connection 트랜잭션 제어
- 트랜잭션 경계 간 일관성 보장 (동일 스레드 내 같은 Connection 재사용)

### D. 로깅/디버그

- `setDevMode(true)`일 때 **raw SQL & 바인딩 값**을 확인 가능한 수준의 로그 출력 권장

---

## ✅ 테스트 통과 기준(요약)

- **CRUD**: `t001 insert`, `t002 update`, `t003 delete`
- **조회**: `t004 selectRows`, `t005 selectRow`, `t006 NOW()`, `t007 selectLong`, `t008 selectString`, `t009~t011 selectBoolean`
- **쿼리 도우미**: `t012 LIKE`, `t013 appendIn`, `t014 ORDER BY FIELD`, `t015~t016 DTO 매핑(Article)`
- **동시성**: `t017 multi threading` (10개 스레드 동시 조회 성공 및 **스레드별 커넥션 사용** 확인)
- **트랜잭션**: `t018 rollback`, `t019 commit`

> 모든 테스트가 green이어야 제출 요건 충족으로 간주합니다.

---
## ✨ 새롭게 알게된 내용

### ThreadLocal
- [ThreadLocal](https://velog.io/@wkdrhrwjdgh/Java-ThreadLocal)

### Connection 인터페이스
- [Connection](https://velog.io/@wkdrhrwjdgh/Java-Connection-%EC%9D%B8%ED%84%B0%ED%8E%98%EC%9D%B4%EC%8A%A4)

---
## ✏️ 세부 구현

### 🎈 SimpleDb 

#### `Connection getConnection()`

- `ThreadLocal`에서 현재 스레드의 Connection을 가져온다.

- 없거나 닫혀 있으면 `DriverManager.getConnection(...)`으로 새로 생성해 `ThreadLocal`에 저장.

- 같은 스레드에서 여러 번 호출하면 동일한 Connection을 재사용한다.

#### `Sql genSql()`

- `Sql` 빌더(별도 클래스)를 생성하여 SQL을 조립하고 실행할 수 있게 한다.

#### `void run(String sql, Object... params)`

- `PreparedStatement` 기반으로 쿼리 실행 (INSERT/UPDATE/DELETE/DDL 등).

- `Object... params`를 받아 `?`에 순서대로 바인딩.

- 가변 인자이므로 파라미터를 넘기지 않아도 동작.

#### **트랜잭션**

- `startTransaction()` : conn.setAutoCommit(false) — 트랜잭션 시작

- `commit()` : conn.commit() 및 conn.setAutoCommit(true) — 정상종료

- `rollback()` : conn.rollback() 및 conn.setAutoCommit(true) — 취소

#### `close()`

- 현재 스레드에 저장된 `Connection`을 닫고 `ThreadLocal`에서 제거합니다.

- 커넥션 누수 방지.

---

### 🚀 SQL 빌더

#### `append(String sqlPart, Object... paramValues)`, `appendIn(String sqlPart, Object... paramValues)`

- StringBuilder을 통해 sql문 작성.
  
- `Object... paramValues`을 이용해 `?` 바인딩.

#### `executeUpdate()`

- **INSERT / UPDATE / DELETE / DDL**(CREATE, DROP, TRUNCATE…) 같이 👉 **결과가 “변경된 행 수”로 나타나는 쿼리**를 실행할 때 사용.

- 리턴값: `int` → 변경된 row 수

```java
PreparedStatement ps = conn.prepareStatement("UPDATE article SET title=? WHERE id=?");
ps.setString(1, "새 제목");
ps.setLong(2, 1);
int updatedRows = ps.executeUpdate();
```

➡️ `updatedRows` = 수정된 행 수

#### `executeQuery()`

- **SELECT** 같이 👉 **결과가 ResultSet으로 나오는 쿼리** 실행할 때 사용.

- 리턴값: `ResultSet`

```java
PreparedStatement ps = conn.prepareStatement("SELECT id FROM article WHERE id=?");
ps.setLong(1, 1);
ResultSet rs = ps.executeQuery();
```

---

### 🎨 로깅/디버그

-`getSql()`메서드를 사용할 때마다 로그 출력

```java
    public String getSql() {
        if (simpleDb.isDevMode()) {
            logDebugSql();
        }
        return sb.toString();
    }

    private void logDebugSql() {
        System.out.println("\n== Log ==");
        System.out.println("== Raw SQL ==");
        System.out.println(sb.toString());
        System.out.println("== Params ==");
        for (int i = 0; i < params.size(); i++) {
            System.out.printf("[%d] %s%n", i + 1, params.get(i));
        }
    }
```
