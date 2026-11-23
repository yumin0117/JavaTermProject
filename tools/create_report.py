from pathlib import Path
from xml.sax.saxutils import escape
import json
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, PageBreak, KeepTogether
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
ROOT=Path(__file__).resolve().parents[1]
pdfmetrics.registerFont(TTFont('Malgun',r'C:\Windows\Fonts\malgun.ttf'))
pdfmetrics.registerFont(TTFont('MalgunBold',r'C:\Windows\Fonts\malgunbd.ttf'))
pdfmetrics.registerFontFamily('Malgun',normal='Malgun',bold='MalgunBold',italic='Malgun',boldItalic='MalgunBold')
blue=colors.HexColor('#245BB1'); ink=colors.HexColor('#182235'); pale=colors.HexColor('#EEF3FB')
styles=getSampleStyleSheet()
styles.add(ParagraphStyle(name='KBody',fontName='Malgun',fontSize=10.1,leading=17.6,textColor=ink,spaceAfter=10,wordWrap='CJK'))
styles.add(ParagraphStyle(name='KTitle',fontName='MalgunBold',fontSize=28,leading=42,textColor=ink,spaceAfter=24,wordWrap='CJK'))
styles.add(ParagraphStyle(name='KH1',fontName='MalgunBold',fontSize=21,leading=31,textColor=blue,spaceAfter=20,wordWrap='CJK'))
styles.add(ParagraphStyle(name='KH2',fontName='MalgunBold',fontSize=12,leading=20,textColor=ink,spaceBefore=11,spaceAfter=9,wordWrap='CJK'))
styles.add(ParagraphStyle(name='KSmall',fontName='Malgun',fontSize=8.6,leading=14,textColor=colors.HexColor('#4B586B'),spaceAfter=10,wordWrap='CJK'))
WIDTH=A4[0]-100
story=[]
def para(s,style='KBody'):
    return Paragraph(s,styles[style])
def p(s,style='KBody'):story.append(para(s,style))
def heading(n,title):p(f'{n:02d}  {title}','KH1')
def img(name,maxheight=340):
    path=ROOT/name
    if path.exists():
        from PIL import Image as PIL
        with PIL.open(path) as source:w,h=source.size
        scale=min(WIDTH/w,maxheight/h);story.append(Image(str(path),width=w*scale,height=h*scale));story.append(Spacer(1,10))
def table(rows,widths=None):
    data=[[para(escape(str(x)),'KSmall') for x in row] for row in rows]
    t=Table(data,colWidths=widths or [WIDTH/len(rows[0])]*len(rows[0]),hAlign='LEFT')
    t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),pale),('VALIGN',(0,0),(-1,-1),'TOP'),('LINEBELOW',(0,0),(-1,0),1,blue),('LINEBELOW',(0,1),(-1,-1),.4,colors.HexColor('#DDE3EB')),('LEFTPADDING',(0,0),(-1,-1),10),('RIGHTPADDING',(0,0),(-1,-1),10),('TOPPADDING',(0,0),(-1,-1),8),('BOTTOMPADDING',(0,0),(-1,-1),5)]))
    story.append(t);story.append(Spacer(1,12))
def page():story.append(PageBreak())
complete=(ROOT/'docs/screenshots/08_deleted.png').exists()
grade='미확인'
meta=ROOT/'docs/report-metadata.json'
if meta.exists():grade=json.loads(meta.read_text(encoding='utf-8')).get('grade',grade)
final=complete and grade!='미확인'
filename=('기말보고서' if final else '기말보고서_검토초안')+'_2601110285_김유민.pdf'
story.append(Spacer(1,44));p('데이터베이스실무 · JDBC Programming','KH2');story.append(Spacer(1,20))
p('음악 플레이리스트<br/>공유 시스템','KTitle');p('Java Swing과 Oracle JDBC를 활용한 데스크톱 프로젝트','KBody')
story.append(Spacer(1,35))
table([['구분','내용'],['과목명','데이터베이스실무'],['담당교수','민경주 교수님'],['학과','인공지능소프트웨어과'],['학년',grade],['학번','2601110285'],['성명','김유민'],['Git Repository','https://github.com/yumin0117/JavaTermProject']], [105,WIDTH-105])
story.append(Spacer(1,18));p('작성일 2026년 11월 23일 · 제출기한 2026년 12월 10일(목) 자정 전','KSmall')
if not final:p('검토 초안: '+('학년 확인이 필요합니다. ' if grade=='미확인' else '')+('Oracle JDBC 연결 테스트와 CRUD 실행 캡처는 DB 비밀번호 입력 후 보완합니다.' if not complete else ''),'KSmall')
page();heading(1,'주제와 선정 이유')
p('프로젝트 주제','KH2');p('회원이 음악 정보를 검색하여 자신만의 플레이리스트를 만들고, 공개 목록을 다른 회원과 공유하며 좋아요로 취향을 표현하는 시스템이다. 실제 음원 재생을 구현하는 대신 음악 정보와 플레이리스트 데이터를 관리하는 데 집중한다.')
p('주제 선정 이유','KH2');p('음악을 상황이나 기분에 따라 모아 듣는 경험을 데이터베이스 구조로 표현할 수 있다. 회원과 플레이리스트의 일대다 관계, 곡과 아티스트 및 플레이리스트의 다대다 관계를 함께 다루므로 E-R 설계와 관계형 데이터베이스의 학습 내용을 적용하기에 적합하다. 또한 곡 추가 순서와 좋아요 중복 같은 실제 처리 규칙을 통해 제약조건과 트랜잭션의 역할을 확인할 수 있다.')
p('핵심 기능','KH2')
table([['영역','구현 기능'],['회원','회원가입, 아이디 중복 검사, PBKDF2 해시 인증, 로그인·로그아웃'],['개인 목록 CRUD','생성(Create), 조회(Read), 제목·설명·공개 여부 수정(Update), 삭제(Delete)'],['곡 관리','곡 제목·아티스트 검색, 수록곡 추가·제거, 위·아래 순서 변경'],['공유','공개 목록 제목 검색, 상세 조회, 좋아요 등록·취소'],['데이터 보호','소유자 검사, 비공개 조회 제한, 트랜잭션과 행 잠금, 기존 데이터 보존']],[100,WIDTH-100])
p('개발 환경','KH2');p('IntelliJ IDEA 2025.3.3 / Java 17 (Microsoft OpenJDK 17.0.18) / Swing / Oracle Database 21c XE / JDBC ojdbc11 21.18.0.0 / Maven. 별도 웹 서버와 Spring Boot는 사용하지 않는다.')
page();heading(2,'데이터베이스 설계')
p('E-R Diagram','KH2');img('docs/design/er-diagram.png',355)
p('기존 데이터베이스 설계 보고서의 E-R Diagram을 사용하였다. MEMBER가 PLAYLIST를 생성하고, SONG과 ARTIST는 참여 관계를 가진다. PLAYLIST의 곡 포함 관계에는 added_at과 play_order를, 좋아요 관계에는 liked_at을 둔다.','KSmall')
table([['개념 관계','관계형 구현'],['MEMBER 1 : N PLAYLIST','PLAYLIST.member_id 외래키'],['SONG M : N ARTIST','SONG_ARTIST(song_id, artist_id) 복합 기본키'],['PLAYLIST M : N SONG','PLAYLIST_SONG(playlist_id, song_id) 복합 기본키'],['MEMBER M : N PLAYLIST 좋아요','PLAYLIST_LIKE(member_id, playlist_id) 복합 기본키']],[205,WIDTH-205])
page();heading(3,'테이블과 처리 규칙')
table([['테이블','주요 데이터와 제약조건'],['MEMBER','member_id PK, password_hash, nickname, email'],['ARTIST','artist_id PK, artist_name, country'],['SONG','song_id PK, title, album_title, genre, release_date, duration_seconds > 0'],['PLAYLIST','playlist_id PK, title, description, created_at, is_public Y/N, member_id FK'],['SONG_ARTIST','song_id + artist_id 복합 PK와 각각의 FK'],['PLAYLIST_SONG','playlist_id + song_id 복합 PK, (playlist_id, play_order) UNIQUE, play_order ≥ 1'],['PLAYLIST_LIKE','member_id + playlist_id 복합 PK, liked_at']],[118,WIDTH-118])
p('기존 SQL에서 보장되는 것과 Java에서 추가한 것','KH2');p('기본키·외래키·공개 값 범위·재생순서 유일성과 양수 조건·삭제 연쇄는 기존 CREATE TABLE의 제약조건으로 보장한다. 로그인 회원의 소유권, 비공개 목록 접근 제한, 공개 목록에만 좋아요 등록, 검색과 순서 교환은 Java 서비스와 조건부 SQL로 구현하였다.')
p('기존 데이터와 번호 생성','KH2');p('기존 예시 데이터는 회원 3건, 아티스트 4건, 곡 5건, 플레이리스트 3건, 곡·아티스트 6건, 수록곡 6건, 좋아요 3건으로 총 30건이다. 기존 데이터를 초기화하지 않는다. 목록 번호는 별도 추가한 SEQ_PLAYLIST의 NEXTVAL로 생성한다. MAX(id)+1은 최초 시퀀스 시작값 산정에만 사용한다.')
p('공개 범위와 삭제','KH2');p('본인만 목록과 수록곡을 변경할 수 있다. 비공개 목록은 번호를 직접 지정해도 다른 회원이 조회할 수 없다. 목록 삭제 시 수록곡 연결과 좋아요는 ON DELETE CASCADE로 삭제되며 회원·곡·아티스트 원본은 유지된다.')
page();heading(4,'Java 구현 구조')
table([['계층 / 파일','역할'],['App','EDT에서 UI 초기화 및 로그인 화면 실행'],['Database','외부 UTF-8 properties 설정을 읽고 Oracle Connection 생성'],['PasswordHasher','16바이트 salt와 PBKDF2WithHmacSHA256 600,000회 해시·검증'],['model','Member, Playlist, Song, PlaylistDetail로 조회 결과 전달'],['DAO / Jdbc','PreparedStatement로 값 전달, try-with-resources로 JDBC 자원 종료'],['AuthService / PlaylistService','입력 검증, 인증, 소유권 확인, 단일 Connection 트랜잭션'],['Swing 화면','회원가입·로그인·목록·상세·곡 검색 및 관리, SwingWorker로 DB 작업 실행']],[160,WIDTH-160])
p('안전한 순서 교환','KH2');p('같은 목록의 쓰기 작업은 PLAYLIST 부모 행을 FOR UPDATE WAIT 5로 잠근다. 교환할 두 곡의 기존 순서를 읽은 뒤, 사용하지 않는 양수 임시 순서를 찾아 A → 임시, B → A의 기존 순서, A → B의 기존 순서로 이동한다. 0과 음수를 사용하지 않으며 NUMBER(6) 범위를 확인한다. 중간 오류는 전체 롤백한다. 곡 제거 후에는 남은 곡을 순서대로 1부터 정리한다.')
p('인증과 화면 응답성','KH2');p('DB 예시 데이터의 DEMO_HASH_NOT_FOR_AUTH 값은 인증에 사용하지 않는다. 로그인 테스트는 새로 가입한 계정으로 수행한다. 접속 비밀번호는 Git에서 제외한 config/db.properties에만 둔다. 암호화와 DB 호출은 SwingWorker의 작업 스레드에서 처리하고, 화면 갱신은 EDT에서 수행한다.')
page();heading(5,'주요 실행 화면')
p('회원 로그인','KH2');img('docs/screenshots/01_login.png',280);p('회원 아이디와 비밀번호를 입력하여 로그인한다. 처음 사용하는 회원은 새 계정 만들기로 이동한다. 아래 캡처는 실행 중인 Swing 화면의 클라이언트 영역을 저장한 것이다.','KSmall')
p('회원가입','KH2');img('docs/screenshots/02_signup.png',185);p('필수값과 컬럼 길이, 아이디 중복, 비밀번호 확인 일치를 검사한다. 가입 성공 후 로그인 화면에서 새 계정을 사용한다.','KSmall')
if complete:
    page();heading(6,'목록과 곡 관리 화면')
    p('내 플레이리스트','KH2');img('docs/screenshots/03_my_playlists.png',255);p('로그인한 회원이 만든 목록만 표시한다. 생성·조회·수정·삭제를 수행할 수 있으며 생성 기본값은 비공개이다.','KSmall')
    p('곡 검색과 수록곡 관리','KH2');img('docs/screenshots/04_song_search.png',300);p('네온으로 검색해도 별빛 산책의 참여 아티스트 루나·네온을 모두 표시한다. 선택한 곡을 목록의 마지막에 추가하며 중복은 차단한다.','KSmall')
    page();heading(7,'순서 변경과 공유 화면')
    p('재생순서 변경','KH2');img('docs/screenshots/05_reordered.png',290);p('여름의 끝을 위로 이동한 결과이다. 양수 임시 순서와 하나의 트랜잭션으로 유일성 제약조건을 유지한다.','KSmall')
    p('공개 목록과 좋아요','KH2');img('docs/screenshots/06_public_likes.png',255);p('공개된 목록과 좋아요 수, 본인이 누른 상태를 표시한다. 본인이 작성한 공개 목록에도 좋아요를 누를 수 있다.','KSmall')
    page();heading(8,'조회 권한과 삭제 결과')
    p('다른 회원의 공개 목록 상세','KH2');img('docs/screenshots/07_other_public.png',275);p('공개 목록은 읽을 수 있으나 소유자가 아니므로 곡 관리 기능이 표시되지 않는다. 서비스와 SQL에서도 권한을 다시 확인한다.','KSmall')
    p('플레이리스트 삭제 후','KH2');img('docs/screenshots/08_deleted.png',250);p('테스트 목록을 삭제한 뒤 내 목록에서 사라진 결과이다. 캡처용 임시 회원도 검증 종료 후 정리하고 기존 예시 데이터는 유지한다.','KSmall')
page();heading(9 if complete else 6,'SQL 내보내기와 검증')
img('docs/screenshots/09_sql_export.png',310);p('SQL Developer → 도구 → 데이터베이스 익스포트로 원본 DB의 DDL과 데이터를 내보냈다. 파일: sql/04_sqldeveloper_export.sql. 테이블 7개와 INSERT 30건이 포함되어 있고 한글 데이터도 보존되어 있다. 내보내기는 DB를 변경하지 않는다.','KSmall')
table([['검증 항목','결과'],['Java 17 Maven 빌드','성공: Java 소스 컴파일과 실행 JAR 생성'],['기본 자동 테스트','3개 통과: 해시 검증·salt, DEMO/잘못된 해시 거부, 입력 길이'],['SQL Developer 실제 접속·내보내기','성공: 기존 7개 테이블, 예시 데이터 30건'],['Oracle 시퀀스 추가 SQL','SQL Developer에서 PL/SQL 실행 완료'],['Java 프로그램의 실제 JDBC CRUD','통합 테스트 통과 및 실제 DB 화면 캡처 완료' if complete else '대기: 실제 DB 비밀번호가 입력되지 않아 실행하지 않음']],[210,WIDTH-210])
if not complete:p('Java 빌드 성공과 SQL Developer 접속 성공은 Java 프로그램의 Oracle JDBC 연결 성공을 의미하지 않는다. 비밀번호 입력 후 회원가입·로그인, CRUD, 권한, 좋아요, 순서 교환, 다중 연결 충돌, 롤백 및 원본 데이터 보존을 OracleIntegrationTest로 검증하고 실제 주요 화면을 추가해야 한다.','KSmall')
page();heading(10 if complete else 7,'실행 방법과 제출 파일')
p('IntelliJ 실행 절차','KH2');p('① JavaTermProject/pom.xml을 Maven 프로젝트로 연다.<br/>② Project SDK와 Maven Runner JRE를 Java 17로 지정한다.<br/>③ 기존 DB에서는 02_add_sequence.sql만 최초 실행한다.<br/>④ config/db.properties.example을 복사해 실제 접속 정보를 입력한다.<br/>⑤ App.main()을 프로젝트 폴더 기준으로 실행한다.<br/>⑥ 프로그램에서 새로 회원가입한 후 로그인한다.')
p('사용자 확인 순서','KH2');p('회원가입 → 로그인 → 비공개 목록 생성 → 제목·설명 수정 → 곡 검색·추가 → 위/아래 순서 변경 → 곡 제거와 순서 정리 → 공개 전환 → 다른 회원으로 조회·좋아요 → 소유 계정으로 삭제 → 로그아웃 순서로 확인한다. 삭제 전 확인창에서 취소하면 목록이 유지되어야 한다.')
table([['제출물','저장소 내 경로'],['Java 프로젝트','pom.xml, src/main/java, src/test/java, run.bat'],['보고서 PDF','docs/'+filename],['SQL Developer 내보내기','sql/04_sqldeveloper_export.sql'],['추가 SQL','sql/02_add_sequence.sql'],['전체 코드와 실행 안내','docs/전체소스코드.md, README.md, docs/테스트순서.md']],[155,WIDTH-155])
p('참고 자료','KH2');p('기존 음악 플레이리스트 DB 설계 보고서, CreateTable_c__dbsoft.sql, Select&amp;Join SQL, 요구사항 분석 명세서와 테이블 명세서를 기준으로 구현하였다. Oracle JDBC 호환성: https://www.oracle.com/database/technologies/faq-jdbc.html','KSmall')
if not final:p('이 파일은 최종 제출본이 아닌 검토 초안이다. 학년과 JDBC 검증·주요 화면 캡처를 보완한 뒤 최종 PDF와 SQL을 공개 저장소에 올리고 “★ 기말과제제출 ★” 커밋으로 제출해야 한다.','KSmall')
def footer(canvas,doc):
    canvas.setStrokeColor(colors.HexColor('#DCE2EC'));canvas.line(50,43,A4[0]-50,43)
    canvas.setFont('Malgun',8);canvas.setFillColor(colors.HexColor('#657085'));canvas.drawString(50,29,'음악 플레이리스트 공유 시스템 | 2601110285 김유민');canvas.drawRightString(A4[0]-50,29,str(doc.page))
out=ROOT/'docs'/filename
doc=SimpleDocTemplate(str(out),pagesize=A4,rightMargin=50,leftMargin=50,topMargin=48,bottomMargin=60,title='음악 플레이리스트 공유 시스템 - 기말 프로젝트',author='김유민')
doc.build(story,onFirstPage=footer,onLaterPages=footer)
print(out)
