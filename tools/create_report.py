from pathlib import Path
from xml.sax.saxutils import escape
from reportlab.platypus import SimpleDocTemplate,Paragraph,Spacer,Table,TableStyle,Image,PageBreak
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.pagesizes import A4
from reportlab.graphics.shapes import Drawing,Rect,Line,String,PolyLine
from reportlab.graphics import renderSVG
from PIL import Image as PIL
ROOT=Path(__file__).resolve().parents[1]
pdfmetrics.registerFont(TTFont('Korean',r'C:\Windows\Fonts\malgun.ttf'));pdfmetrics.registerFont(TTFont('KoreanBold',r'C:\Windows\Fonts\malgunbd.ttf'))
pdfmetrics.registerFontFamily('Korean',normal='Korean',bold='KoreanBold',italic='Korean',boldItalic='KoreanBold')
INK=colors.HexColor('#202127');ACCENT=colors.HexColor('#D73250');PALE=colors.HexColor('#F6F3F6');GRAY=colors.HexColor('#747582');W=A4[0]-100
styles={
 'body':ParagraphStyle('body',fontName='Korean',fontSize=10,leading=17.5,spaceAfter=11,textColor=INK,wordWrap='CJK'),
 'title':ParagraphStyle('title',fontName='KoreanBold',fontSize=23,leading=34,spaceAfter=22,textColor=INK,wordWrap='CJK'),
 'sub':ParagraphStyle('sub',fontName='KoreanBold',fontSize=12,leading=21,spaceBefore=10,spaceAfter=9,textColor=INK,wordWrap='CJK'),
 'small':ParagraphStyle('small',fontName='Korean',fontSize=8.5,leading=14,spaceAfter=10,textColor=GRAY,wordWrap='CJK')}
story=[]
def p(s,style='body'):story.append(Paragraph(s,styles[style]))
def heading(n,s):p(f'{n:02d}  {s}','title')
def page():story.append(PageBreak())
def table(rows,widths=None):
 t=Table([[Paragraph(escape(str(x)),styles['small']) for x in row] for row in rows],colWidths=widths or [W/len(rows[0])]*len(rows[0]));t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),PALE),('VALIGN',(0,0),(-1,-1),'TOP'),('LINEBELOW',(0,0),(-1,0),1,ACCENT),('LINEBELOW',(0,1),(-1,-1),.4,colors.HexColor('#E5E4EA')),('LEFTPADDING',(0,0),(-1,-1),10),('TOPPADDING',(0,0),(-1,-1),8),('BOTTOMPADDING',(0,0),(-1,-1),7)]));story.append(t);story.append(Spacer(1,10))
def image(name,height=350):
 path=ROOT/'docs/screenshots'/name
 with PIL.open(path) as im:w,h=im.size
 factor=min(W/w,height/h);story.append(Image(str(path),width=w*factor,height=h*factor));story.append(Spacer(1,13))
def footer(c,d):
 c.setStrokeColor(colors.HexColor('#E6E4EA'));c.line(50,44,A4[0]-50,44);c.setFont('Korean',8);c.setFillColor(GRAY);c.drawString(50,29,'muse | Java Swing · Oracle JDBC');c.drawRightString(A4[0]-50,29,str(d.page))
def cover(c,d):
 footer(c,d);c.setFillColor(INK);c.setFont('KoreanBold',15);c.drawString(60,740,'기말 프로젝트 보고서')
 c.setFont('Korean',9);c.setFillColor(GRAY);c.drawString(60,716,'아래 작성란은 직접 입력할 수 있도록 비워 두었습니다.')
 y=650
 for label,height in [('제목',80),('Git Repository 주소',50),('과목명',42),('담당교수명',42),('학과',42),('학년',42),('학번',42),('성명',42)]:
  c.setFont('KoreanBold',10);c.setFillColor(INK);c.drawString(62,y-15,label);c.setStrokeColor(colors.HexColor('#C6C3CD'));c.line(178,y-height+17,A4[0]-62,y-height+17);y-=height+12
story.append(Spacer(1,10));page();heading(1,'주제와 선정 이유')
p('음악 플레이리스트 공유 시스템 · muse','sub')
p('muse는 멜론 TOP100의 실제 곡 정보를 Oracle에 저장하고, 좋아하는 곡을 플레이리스트로 구성하여 공유하는 Java 데스크톱 프로그램이다. 로그인 화면 없이 개인 보관함으로 시작한다. 사용 권한이 있는 MP3 파일을 연결하면 무료 첫 60초, 과제용 모의 Plus 이용권으로 전체 길이를 재생한다.')
p('주제 선정 이유','sub')
p('음악을 취향과 상황에 따라 모으는 과정에는 회원, 곡, 아티스트, 플레이리스트, 좋아요 등 여러 관계가 생긴다. 이 관계를 정규화하고 JDBC CRUD로 연결하면 데이터 모델과 실제 프로그램의 관계를 이해하기 쉽다. 차트 수집 시점과 순위를 별도로 보존하고 결제 이력까지 확장하여 트랜잭션, 중복 방지, 권한 검사를 함께 학습한다.')
table([['영역','구현 내용'],['곡 데이터','2026.11.26 11:00 KST 멜론 TOP100 100곡의 순위·제목·가수·앨범·곡 번호·커버 URL'],['GUI','홈, 커버 카드, 검색, 순위 필터, 개인/공개/좋아요 목록, 고정 플레이어'],['JDBC CRUD','목록 생성·조회·수정·삭제, 곡 담기·제거·순서 변경, 좋아요와 취소'],['차트 갱신','공개 HTML 수집 → 100곡 검증 → Oracle 트랜잭션 저장 → 최신 스냅샷 조회'],['재생과 이용권','보유 MP3 연결, 실제 디코딩/출력, 무료 60초 제한, 모의 구매·취소와 DB 이력']],[105,W-105])
p('사용 환경','sub');p('IntelliJ IDEA 2025.3.3 / Java 17 / Maven / Swing + FlatLaf / Oracle XE / ojdbc11 / jsoup / Java Sound + MP3SPI. 초기 로그인 절차는 없지만 개인 보관함 인증과 DB의 소유권 검사는 유지한다.')
p('차트 정보 수집과 음원 재생은 별개다. 멜론의 상업 음원을 다운로드하거나 스트리밍 권한을 부여하지 않는다. 음원이 없는 곡은 “정보”로 표시하며 다른 곡의 녹음을 대신 재생하지 않는다.','small')

page();heading(2,'데이터베이스 E-R 설계')
p('기존 7개 테이블을 유지하고 음원·모의 결제·차트 스냅샷 관계를 추가한 총 11개 테이블이다. 연결 테이블로 곡-가수, 목록-곡, 회원-목록의 다대다 관계를 표현한다.','small')
d=Drawing(510,510)
nodes={'MEMBER':(190,440),'PLAYLIST_LIKE':(10,440),'MEMBERSHIP_ORDER':(380,440),'PLAYLIST':(190,320),'SONG_ARTIST':(10,200),'PLAYLIST_SONG':(190,200),'SONG':(190,80),'ARTIST':(10,80),'SONG_MEDIA':(380,80),'MELON_CHART':(380,320),'MELON_CHART_ENTRY':(380,200)}
sub={'MEMBER':'PK member_id','PLAYLIST_LIKE':'PK, FK member_id / playlist_id','MEMBERSHIP_ORDER':'PK order_id / FK member_id','PLAYLIST':'PK playlist_id / FK member_id','SONG_ARTIST':'PK, FK song_id / artist_id','PLAYLIST_SONG':'PK, FK playlist_id / song_id','SONG':'PK song_id','ARTIST':'PK artist_id','SONG_MEDIA':'PK, FK song_id','MELON_CHART':'PK chart_at','MELON_CHART_ENTRY':'PK chart_at / chart_rank; FK song_id'}
def edge(points,label,x,y):
 d.add(PolyLine(points,strokeColor=colors.HexColor('#A4A0AF'),strokeWidth=1));d.add(String(x,y,label,fontName='Korean',fontSize=8,fillColor=GRAY))
edge([130,465,190,465],'1 : N',145,472);edge([310,465,380,465],'1 : N',334,472)
edge([250,440,250,370],'1 : N',257,402);edge([70,440,70,345,190,345],'1 : N',85,352)
edge([250,320,250,250],'1 : N',257,282);edge([250,130,250,200],'1 : N',257,162)
edge([70,130,70,200],'1 : N',77,162);edge([190,105,155,105,155,225,130,225],'1 : N',135,170)
edge([310,105,380,105],'1 : 0..1',326,92);edge([440,320,440,250],'1 : N',447,282)
edge([310,115,345,115,345,225,380,225],'1 : N',348,162)
for name,(x,y) in nodes.items():
 fill=PALE if name in ('MEMBERSHIP_ORDER','SONG_MEDIA','MELON_CHART','MELON_CHART_ENTRY') else colors.white
 d.add(Rect(x,y,120,50,rx=6,ry=6,fillColor=fill,strokeColor=colors.HexColor('#BCB7C5'),strokeWidth=.8))
 d.add(String(x+6,y+31,name,fontName='KoreanBold',fontSize=8,fillColor=INK));d.add(String(x+6,y+13,sub[name],fontName='Korean',fontSize=5.9,fillColor=GRAY))
d.scale(W/510,W/510);d.width=W;d.height=510*W/510;story.append(d)
(ROOT/'docs/design').mkdir(exist_ok=True);renderSVG.drawToFile(d,str(ROOT/'docs/design/er-final.svg'))
p('SONG_MEDIA: 음원 경로와 출처. MEMBERSHIP_ORDER: 회원별 결제·만료·취소 이력. MELON_CHART: 차트 기준 시각과 출처. MELON_CHART_ENTRY: 차트 시각별 1~100위, SONG 외래키와 멜론 곡 번호. 동일 곡은 차트가 바뀌어도 SONG 기본키를 유지한다.','small')

page();heading(3,'Java와 JDBC 구현')
table([['구성','책임'],['MainFrame / FlatLaf','음악 앱 화면과 선언식 테마, SwingWorker로 DB 작업 분리'],['LocalProfileService','이 PC의 개인 회원 생성·재사용, PBKDF2 인증'],['MelonChartService','jsoup으로 공개 차트 수집·검증, 하나의 트랜잭션으로 스냅샷 저장'],['DAO / PlaylistService','PreparedStatement, 자원 종료, 소유권·공개 범위·CRUD·순서 행 잠금'],['MusicLibraryService','최신 차트와 음원 LEFT JOIN, 재생 전 DB의 파일·이용권 조회'],['LocalAudioService / AudioPlayer','보유 MP3 연결과 길이 확인, 실제 PCM 출력, 60초 프레임 제한'],['MembershipService','DB 시각으로 만료 확인, UUID로 중복 결제 방지, 구매·취소 이력 보존']],[155,W-155])
p('데이터 정확성과 기존 자료 보존','sub')
p('기존 예시 30건을 삭제하지 않는다. 차트 곡 ID는 1,000,000,000 + 멜론 곡 번호, 가수 ID는 2,000,000,000 + 멜론 가수 번호로 매핑한다. 원문에 없는 재생 길이·장르·발매일은 임의 생성하지 않는다. 길이는 NULL을 허용하도록 추가 SQL에서 변경하고 기존 양수 CHECK는 유지한다. MP3를 연결하면 확인한 길이를 DB에 기록한다.')
p('트랜잭션과 접근 제어','sub')
p('소유자만 목록을 변경할 수 있고 다른 회원의 비공개 목록은 번호를 알아도 조회할 수 없다. 순서 교환은 부모 행 잠금과 사용하지 않는 양수 임시 순서로 처리하며 오류 시 롤백한다. 차트는 100곡 검증을 통과한 결과만 저장한다. 모의 결제는 회원 행 잠금과 주문 UUID로 중복을 막고 취소 기록을 남긴다.')
p('DB 접속 정보·개인 인증 정보·사용자 연결 음원은 공개 Git에 포함하지 않는다. 결제 성공 여부와 플레이리스트는 로컬 JSON이 아니라 Oracle에서 조회한다.','small')

page();heading(4,'로그인 없이 열리는 TOP100 홈');image('13_home_live.png',370)
p('실제 Oracle에 연결한 홈 화면이다. 멜론 차트 기준 시각, 상위 4곡의 실제 제목·가수·커버를 표시한다. 왼쪽에서 곡과 개인/공개/좋아요 목록으로 이동하며 하단 플레이어는 화면을 바꾸어도 유지된다.')
p('색상·글꼴·버튼·표·입력란은 theme/muse.properties에서 관리한다. 차트 커버는 원문 URL에서 비동기로 가져오며 실패하면 앱의 자체 그래픽을 표시한다. 홈의 곡 목록에는 최신 TOP100만 나타난다.','small')

page();heading(5,'실제 곡 검색과 목록 담기');image('14_library_live.png',290);image('16_playlist_live.png',260)
p('상단은 멜론 TOP100 100곡 조회와 필터 화면이다. 제목·가수·앨범으로 검색하며 TOP 10/50과 음원 연결 여부로 필터링한다. 하단은 실제 곡을 새 플레이리스트에 담은 상세 화면이다. 음원 미연결 곡도 검색·담기·정렬할 수 있다.','small')

page();heading(6,'플레이리스트 CRUD와 순서 변경');image('15_my_playlist_live.png',270);image('17_reorder_live.png',255)
p('생성한 목록의 제목·설명·공개 여부를 수정하고 곡을 추가·제거·이동할 수 있다. 위·아래 이동은 실제 DB의 play_order를 변경한다. 공개 목록의 좋아요와 취소도 저장된다. 삭제 시 목록 연결과 좋아요를 정리하며 곡·가수 원본은 보존한다.','small')

page();heading(7,'무료 1분과 모의 Plus 결제');image('18_free_pass_live.png',260);image('19_paid_pass_live.png',260)
p('연결한 음원은 무료 상태에서 첫 60초까지 재생한다. Plus 30일 이용권은 표시 금액 4,900원의 과제용 모의 결제이며 실제 청구액은 0원이다. 구매 후 Oracle의 결제 이력과 유효기간을 읽어 전체 길이 재생을 허용한다. 멜론 스트리밍 권한은 포함하지 않는다.','small')

page();heading(8,'곡 정보·음원 연결과 결제 취소');image('20_chart_selection_live.png',290);image('21_cancelled_pass_live.png',250)
p('차트 곡을 선택해도 음원 파일이 없으면 재생이 시작되지 않는다. 더블클릭 후 해당 곡의 보유 MP3를 연결하거나 멜론 곡 페이지를 열 수 있다. 실제 재생 엔진은 CC0 검증 음원으로 60초 정지와 유료 61초 이후 출력을 확인했다. 위 차트 화면이 상업 음원의 전체 재생을 의미하지 않는다.','small')
p('결제 취소는 CANCELLED 이력으로 유지된다. 다른 유효 주문이 없으면 무료 상태로 돌아가며 기존 재생을 중지하고 권한을 다시 조회한다.','small')

page();heading(9,'검증 결과와 실행 방법')
table([['검증','결과'],['Java 17 Maven package','실행 JAR 생성 성공, 실제 Oracle 포함 12 tests / failures 0 / errors 0'],['CoreTest 3개','해시·salt·DEMO 인증 거부·입력 검증'],['PlaybackTest 4개','무료·유료·만료/취소 정책, CC0 MP3 4개 실제 디코딩'],['OracleIntegrationTest 1개','CRUD·접근 제어·좋아요·순서·동시 변경·롤백·원본 보존'],['MusicOracleTest 1개','자동 보관함 재사용, TOP100 100곡, 중복 결제·취소·재구매'],['MelonChartTest 2개','실제 수집 스냅샷 파싱, 부분 차트·중복 순위 거부'],['LocalAudioOracleTest 1개','실제 MP3 연결·길이 확인·DB 경로 저장·음원 없는 곡 재생 거부']],[155,W-155])
p('IntelliJ 실행','sub')
p('① pom.xml을 Maven 프로젝트로 열고 SDK를 Java 17로 설정한다.<br/>② 빈 스키마에서만 원본 SQL 01을 실행한다. 기존 DB에서는 재실행하지 않는다.<br/>③ 추가 SQL 02 → 06 → 08 → 09를 SQL Developer에서 F5로 실행한다.<br/>④ 접속 설정 후 App.main()을 실행한다. 빌드 후에는 run.bat으로 실행할 수 있다.<br/>⑤ 상단 차트 갱신 버튼으로 수집 결과를 DB에 다시 저장할 수 있다.')
p('제출 자료와 출처','sub')
p('공개 저장소: https://github.com/yumin0117/JavaTermProject<br/>보고서 PDF·E-R SVG·주요 화면·Java 소스·SQL Developer 내보내기 07·차트 재현 SQL 09를 포함한다. 표지는 직접 작성하도록 항목 위치만 마련했다. 제출 커밋명은 “★ 기말과제제출 ★”이다.','small')
p('차트: https://www.melon.com/chart/index.htm (2026.11.26 11:00 KST)<br/>파서: https://jsoup.org / 테마: https://www.formdev.com/flatlaf/<br/>오디오 검증: https://archive.org/details/musopen-chopin (CC0 1.0)','small')
out=ROOT/'docs/기말보고서_2601110285_김유민.pdf'
doc=SimpleDocTemplate(str(out),pagesize=A4,rightMargin=50,leftMargin=50,topMargin=48,bottomMargin=60,title='muse - 음악 플레이리스트 공유 시스템',author='')
doc.build(story,onFirstPage=cover,onLaterPages=footer);print(out)
