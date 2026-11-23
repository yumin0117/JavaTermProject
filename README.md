# 음악 플레이리스트 공유 시스템

Java 17 · Swing · Oracle Database 21c XE · JDBC · Maven

현재 상태: Java 17 빌드·기본 테스트 통과, SQL Developer 내보내기 완료. 실제 JDBC CRUD 검증과 최종 보고서는 DB 비밀번호·학년 입력 대기 중이다. PDF는 검토 초안으로 저장했다.

김유민 / 2601110285 / 인공지능소프트웨어과 / 데이터베이스실무 / 민경주 교수님

저장소: https://github.com/yumin0117/JavaTermProject

## 1. 구현 범위와 구조

회원가입·로그인·로그아웃, 개인 플레이리스트 CRUD, 곡/아티스트 검색, 수록곡 추가·제거·위아래 순서 변경, 공개 목록 검색, 좋아요 등록·취소를 구현한 데스크톱 프로그램이다. 실제 음원 재생·외부 음악 API·관리자 화면은 범위에 포함하지 않는다.

```text
JavaTermProject/
  pom.xml
  run.bat
  config/db.properties.example
  config/db.properties             # 실제 비밀번호, Git 제외
  sql/01_original_schema_and_seed.sql
  sql/02_add_sequence.sql
  sql/03_original_queries.sql
  sql/04_sqldeveloper_export.sql   # SQL Developer 실제 내보내기
  src/main/java/kr/ac/yumin/playlist/
    App.java
    config/Database.java
    security/PasswordHasher.java
    model/{Member,Playlist,Song,PlaylistDetail}.java
    dao/{Jdbc,MemberDao,PlaylistDao,SongDao}.java
    service/{AppException,AuthService,PlaylistService}.java
    ui/{Ui,AuthFrame,MainFrame,PlaylistEditor,DetailDialog}.java
  src/test/java/kr/ac/yumin/playlist/
    CoreTest.java
    OracleIntegrationTest.java
  docs/전체소스코드.md
  docs/테스트순서.md
  docs/screenshots/
  docs/기말보고서_검토초안_2601110285_김유민.pdf
```

## 2. pom.xml 전체 내용

[pom.xml](pom.xml)에서 전체 내용을 확인할 수 있다. Oracle JDBC 드라이버 ojdbc11 21.18.0.0을 포함한 실행 JAR를 Maven Shade로 만든다. Java 17 대상 컴파일을 명시한다.

## 3. SQL 실행 순서

### 현재 PC처럼 테이블과 예시 데이터가 이미 있는 경우

1. SQL Developer에서 `c__dbsoft` 접속을 연다. 접속 이름과 달리 실제 사용자 이름은 `c##dbsoft`이다.
2. Java 프로그램을 종료한 상태에서 `sql/02_add_sequence.sql`만 스크립트 실행(F5)한다.
3. 이 스크립트는 테이블과 기존 데이터를 변경하지 않는다. 시퀀스가 없는 경우에만 현재 최대 번호 다음 값으로 생성한다. 기존 시퀀스는 유지한다.
4. 프로그램은 생성 때마다 `SEQ_PLAYLIST.NEXTVAL`을 사용한다. 트랜잭션이 취소되어도 시퀀스 번호가 건너뛰는 것은 정상이다.

### 완전히 비어 있는 별도의 실습 스키마를 준비하는 경우

`sql/01_original_schema_and_seed.sql` → `COMMIT;` → `sql/02_add_sequence.sql` 순서로 실행한다. 원본 스크립트의 마지막 SELECT는 건수 조회용이다. 기존 DB에는 원본 생성/INSERT 파일을 재실행하지 않는다.

`sql/04_sqldeveloper_export.sql`은 교수님 제출 요구에 따라 SQL Developer의 데이터베이스 익스포트로 만든 파일이다. 원본 그대로 보관한 파일이므로 환경 고정 스키마·저장영역과 DDL 종결자 설정이 포함된다. 다른 빈 스키마에 복원하려면 `sql/05_restore_portable.sql`을 사용한다. 이 보조 파일은 원본 CREATE 정의와 실제 내보낸 INSERT를 외래키 순서로 배치했다. 원본 생성 파일과 중복 실행하지 않는다. 기존 계정과 데이터에 대한 DROP·초기화 작업은 필요하지 않다.

## 4. 외부 설정과 .gitignore

`config/db.properties.example`을 같은 폴더의 `db.properties`로 복사한다.

```properties
db.url=jdbc:oracle:thin:@localhost:1521:xe
db.username=c##dbsoft
db.password=여기에_실제_DB_비밀번호
```

설정 파일은 UTF-8이다. Java properties 문법이므로 비밀번호에 역슬래시가 있으면 `\\`로 작성한다. 설정 파일 경로는 프로젝트 실행 폴더 기준이며 필요하면 VM 옵션 `-Dplaylist.config=설정파일절대경로`로 지정할 수 있다. 실제 설정 파일은 `.gitignore`에서 제외하며 소스·로그·보고서에 비밀번호를 출력하지 않는다.

## 5. Java 파일 전체 코드

[전체 소스 코드 문서](docs/전체소스코드.md)에 pom.xml, 추가 SQL, 설정 예시, .gitignore, 모든 Java 파일의 전체 코드가 파일 경로와 함께 수록되어 있다. 실제 구현 파일은 `src/main/java`에 있다.

## 6. IntelliJ IDEA 2025.3.3 실행

1. IntelliJ의 Open에서 이 폴더의 `pom.xml`을 선택하고 Maven 프로젝트로 연다.
2. File → Project Structure → Project SDK를 Java 17로 설정한다. 이 PC에 설치한 경로는 `C:\Users\AISW-510-106\.jdks\jdk-17.0.18+8`이다.
3. Settings → Build, Execution, Deployment → Build Tools → Maven → Runner의 JRE도 Project SDK(17)로 설정한다. Maven은 IntelliJ에 포함된 버전을 쓸 수 있다.
4. Maven 창에서 Reload All Maven Projects를 눌러 의존성을 받는다.
5. 위 SQL과 외부 설정을 확인한다.
6. `kr.ac.yumin.playlist.App.main()` 옆 초록색 실행 버튼을 누른다. Run Configuration의 Working directory는 이 `JavaTermProject` 폴더로 지정한다.
7. 처음에는 프로그램에서 회원가입한다. `user01`~`user03`의 `DEMO_HASH_NOT_FOR_AUTH_*`는 진짜 해시가 아니므로 로그인할 수 없다.

Maven Lifecycle → `package`를 실행하면 `target/JavaTermProject-1.0.0.jar`가 생성된다. Java 17 이상에서 프로젝트 폴더를 기준으로 `java -jar target/JavaTermProject-1.0.0.jar`로 실행할 수 있다. Windows에서는 JAVA_HOME 또는 PATH에 Java를 설정한 후 `run.bat`을 실행한다.

## 7. 테스트와 구현 설명

[사용자 테스트 순서](docs/테스트순서.md)에 회원가입부터 삭제까지 예상 결과가 있다.

```shell
mvn clean package          # 비밀번호/입력 검증 테스트 + 실행 JAR 생성
mvn -Poracle-test test     # 실제 Oracle 연동 테스트 (외부 설정 필요)
```

일반 테스트는 DB 없이 실행한다. Oracle 테스트는 임의 이름의 회원과 목록을 새로 생성하고, 자신이 생성한 데이터만 정리한다. 실행 전후 원본 행 전체를 비교해 보존 여부를 확인한다. 실패할 경우 DB 연결 문제와 테스트 실패를 구분하여 Maven 결과를 확인한다.

구조: Swing 화면 → 서비스(검증·권한·트랜잭션) → DAO(PreparedStatement) → Oracle. 서비스는 로그인 회원별로 생성한다. 비공개 조회 SQL과 쓰기 SQL에도 소유자/공개 조건이 포함되어 있다. 이 데스크톱 프로그램은 신뢰된 실습 DB 계정을 사용하는 구조로, DB 계정 자체를 공유받은 사람이 직접 실행하는 SQL의 접근까지 통제하는 서버 시스템은 아니다.

회원별 16바이트 salt와 PBKDF2WithHmacSHA256 600,000회 반복 결과를 저장한다. 평문 비밀번호는 저장하지 않는다. 좋아요 중복은 복합 PK와 부모 행 잠금으로 방지하며 자기 공개 목록 좋아요도 허용한다.

곡 순서 변경은 같은 Connection의 트랜잭션에서 PLAYLIST 부모 행을 `FOR UPDATE WAIT 5`로 잠근다. 사용하지 않는 양수 임시 번호를 찾아 3회 UPDATE로 교환한다. 0과 음수를 쓰지 않으며 실패 시 전체 롤백한다. 제거 후에는 남은 곡을 기존 순서의 오름차순으로 1부터 압축한다. NUMBER(6)의 범위를 넘는 번호는 사용하지 않는다. 같은 목록의 모든 앱 쓰기 작업이 같은 잠금 규칙을 따른다.

DB 조회와 PBKDF2 계산은 SwingWorker에서 실행하고, 화면은 EDT에서 갱신한다. 작업 중 중복 실행을 막고 연결 실패를 한글로 안내한다.

## 제출

공개 저장소 이름은 `JavaTermProject`이다. 제출 PDF와 SQL 내보내기 파일을 저장소에 포함하고 제출 커밋 메시지는 `★ 기말과제제출 ★`를 사용한다. 마감은 2026년 12월 10일(목) 자정 전이다. 실제 LMS 제출이 별도로 필요한지는 수업 공지를 확인한다.

참고: [Oracle JDBC 호환성](https://www.oracle.com/database/technologies/faq-jdbc.html), [Apache Maven](https://maven.apache.org/).
