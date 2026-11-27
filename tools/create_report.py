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
p('muse는 실제 곡과 앨범 정보를 Oracle에 저장하고, 좋아하는 곡을 플레이리스트로 구성하여 공유하는 Java 데스크톱 프로그램이다. 로그인 화면 없이 개인 보관함으로 시작한다. 공식 영상을 앱 중앙에서 재생한다. 무료 등급은 60초에서 멈추고 Oracle에 저장한 과제용 모의 Plus 등급은 전체 길이를 재생한다. 실제 청구액은 0원이다.')
p('주제 선정 이유','sub')
p('음악을 취향과 상황에 따라 모으는 과정에는 회원, 곡, 아티스트, 플레이리스트, 좋아요 등 여러 관계가 생긴다. 이 관계를 정규화하고 JDBC CRUD로 연결하면 데이터 모델과 실제 프로그램의 관계를 이해하기 쉽다. 차트 수집 시점과 순위를 별도로 보존하고 결제 이력까지 확장하여 트랜잭션, 중복 방지, 권한 검사를 함께 학습한다.')
table([['영역','구현 내용'],['곡 데이터','실제 150곡의 제목·가수·앨범·커버, 141앨범의 발매 정보와 1,116개 크레딧'],['GUI','홈, 커버 카드, 검색, 순위 필터, 개인/공개 목록, 고정 플레이어'],['JDBC CRUD','목록 생성·조회·수정·삭제, 곡 담기·제거·순서 변경, 좋아요와 취소'],['곡 추가와 별','최신곡/곡 번호 수집 → DB 보관함 유지, 회원별 별 저장과 해제'],['재생과 이용권','공식 임베드 영상과 실제 MP3 출력, 공식 영상의 무료 60초 제한, 모의 구매·취소와 DB 이력']],[105,W-105])
p('사용 환경','sub');p('IntelliJ IDEA 2025.3.3 / Java 17 / Maven / Swing + FlatLaf / Oracle XE / ojdbc11 / jsoup / Java Sound + MP3SPI / JCEF Chromium / YouTube IFrame API. 초기 로그인 절차는 없지만 개인 보관함 인증과 DB의 소유권 검사는 유지한다.')
p('차트 정보 수집과 음원 재생은 별개다. 멜론의 상업 음원을 다운로드하거나 스트리밍 권한을 부여하지 않는다. 곡별 공식 영상 ID와 확인 상태를 DB에 보관한다. 미연결/재생 제한 곡은 구분하여 표시하고 다른 곡으로 대체하지 않는다.','small')

page();heading(2,'데이터베이스 E-R 설계')
p('기존 7개 테이블에 음원·모의 결제·차트 관계를 추가한 기본 11개 관계다. 다음 페이지에서 앨범·즐겨찾기·영상 6개 테이블을 확장해 최종 17개를 구성한다. 연결 테이블로 곡-가수, 목록-곡, 회원-목록의 다대다 관계를 표현한다.','small')
d=Drawing(510,510)
nodes={'MEMBER':(190,440),'PLAYLIST_LIKE':(10,440),'MEMBERSHIP_ORDER':(380,440),'PLAYLIST':(190,320),'SONG_ARTIST':(10,200),'PLAYLIST_SONG':(190,200),'SONG':(190,80),'ARTIST':(10,80),'SONG_MEDIA':(380,80),'MELON_CHART':(380,320),'MELON_CHART_ENTRY':(380,200)}
sub={'MEMBER':'PK member_id','PLAYLIST_LIKE':'PK, FK member_id / playlist_id','MEMBERSHIP_ORDER':'PK order_id / FK member_id','PLAYLIST':'PK playlist_id / FK member_id','SONG_ARTIST':'PK, FK song_id / artist_id','PLAYLIST_SONG':'PK, FK playlist_id / song_id','SONG':'PK song_id','ARTIST':'PK artist_id','SONG_MEDIA':'PK, FK song_id','MELON_CHART':'PK chart_at','MELON_CHART_ENTRY':'PK chart_at / chart_rank; FK song_id'}
def edge(points,label,x,y):
 d.add(PolyLine(points,strokeColor=colors.HexColor('#A4A0AF'),strokeWidth=1));d.add(String(x,y,label,fontName='Korean',fontSize=8,fillColor=GRAY))
edge([130,465,190,465],'N : 1',145,472);edge([310,465,380,465],'1 : N',334,472)
edge([250,440,250,370],'1 : N',257,402);edge([70,440,70,345,190,345],'N : 1',85,352)
edge([250,320,250,250],'1 : N',257,282);edge([250,130,250,200],'1 : N',257,162)
edge([70,130,70,200],'1 : N',77,162);edge([190,105,155,105,155,225,130,225],'1 : N',135,170)
edge([310,105,380,105],'1 : 0..1',326,92);edge([440,320,440,250],'1 : N',447,282)
edge([310,115,345,115,345,225,380,225],'1 : N',348,162)
for name,(x,y) in nodes.items():
 fill=PALE if name in ('MEMBERSHIP_ORDER','SONG_MEDIA','MELON_CHART','MELON_CHART_ENTRY') else colors.white
 d.add(Rect(x,y,120,50,rx=6,ry=6,fillColor=fill,strokeColor=colors.HexColor('#BCB7C5'),strokeWidth=.8))
 d.add(String(x+6,y+31,name,fontName='KoreanBold',fontSize=8,fillColor=INK));d.add(String(x+6,y+13,sub[name],fontName='Korean',fontSize=5.9,fillColor=GRAY))
d.scale(W/510,W/510);d.width=W;d.height=510*W/510;story.append(d)
(ROOT/'docs/design').mkdir(exist_ok=True);renderSVG.drawToFile(d,str(ROOT/'docs/design/er-core.svg'))
p('SONG_MEDIA: 음원 경로와 출처. MEMBERSHIP_ORDER: 회원별 결제·만료·취소 이력. MELON_CHART: 차트 기준 시각과 출처. MELON_CHART_ENTRY: 차트 시각별 1~100위, SONG 외래키와 멜론 곡 번호. 동일 곡은 차트가 바뀌어도 SONG 기본키를 유지한다.','small')

page();heading(3,'앨범·별 즐겨찾기 E-R 확장')
p('이 페이지의 6개 신규 테이블을 앞의 11개 테이블에 연결하여 최종 17개 테이블을 구성했다. SONG과 MEMBER는 앞 그림에 있는 테이블이며 연결을 설명하기 위해 다시 표시했다.')
d2=Drawing(510,420)
extension={'SONG_VIDEO':(380,340),'SONG':(10,220),'MUSIC_CATALOG':(200,340),'SONG_DETAIL':(200,220),'ALBUM_INFO':(380,220),'SONG_CREDIT':(10,80),'SONG_FAVORITE':(200,80),'MEMBER':(380,80)}
labels={'SONG_VIDEO':'PK, FK song_id / video metadata','SONG':'existing PK song_id','MEMBER':'existing PK member_id','MUSIC_CATALOG':'PK, FK song_id','SONG_DETAIL':'PK, FK song_id / FK album_id','ALBUM_INFO':'PK album_id','SONG_CREDIT':'PK song_id / role / order','SONG_FAVORITE':'PK, FK member_id / song_id'}
def extedge(points,label,x,y):
 d2.add(PolyLine(points,strokeColor=colors.HexColor('#A4A0AF'),strokeWidth=1));d2.add(String(x,y,label,fontName='Korean',fontSize=8,fillColor=GRAY))
extedge([70,270,70,365,200,365],'1 : 0..1',100,372)
extedge([130,255,160,255,160,410,440,410,440,390],'1 : 0..1',330,417)
extedge([130,245,200,245],'1 : 0..1',143,252)
extedge([320,245,380,245],'N : 1',339,252)
extedge([70,220,70,130],'1 : N',77,172)
extedge([70,220,70,165,260,165,260,130],'1 : N',179,173)
extedge([320,105,380,105],'N : 1',339,112)
for name,(x,y) in extension.items():
 d2.add(Rect(x,y,120,50,rx=6,ry=6,fillColor=colors.white if name in ('SONG','MEMBER') else PALE,strokeColor=colors.HexColor('#BCB7C5'),strokeWidth=.8))
 d2.add(String(x+6,y+31,name,fontName='KoreanBold',fontSize=8,fillColor=INK));d2.add(String(x+6,y+13,labels[name],fontName='Korean',fontSize=5.9,fillColor=GRAY))
d2.scale(W/510,W/510);d2.width=W;d2.height=420*W/510;story.append(d2)
renderSVG.drawToFile(d2,str(ROOT/'docs/design/er-extensions.svg'))
table([['테이블','역할'],['MUSIC_CATALOG','전체 보관함. 추가 수집곡도 유지하고 같은 곡의 중복 등록 방지'],['ALBUM_INFO / SONG_DETAIL','앨범 사실과 곡-앨범 외래키. 발매일·장르·발매사·기획사·출처'],['SONG_CREDIT','곡별 작사/작곡/편곡을 역할과 순번으로 나누어 저장'],['SONG_FAVORITE','회원-곡 복합 기본키. 회원별 별 저장/해제와 재실행 후 유지'],['SONG_VIDEO','공식 영상 ID·채널·종류·확인 상태와 시각. SONG 외래키']],[160,W-160])
p('공개 음악 정보는 150곡, 141개 앨범, 150개 곡-앨범 연결, 1,116개 크레딧 행이다. 개인 즐겨찾기는 공개 데이터 재현 SQL에서 제외한다.','small')

page();heading(4,'Java와 JDBC 구현')
table([['구성','책임'],['MainFrame / AlbumDialog','음악 앱·앨범 정보·별 즐겨찾기, SwingWorker로 DB 작업 분리'],['LocalProfileService','이 PC의 개인 회원 생성·재사용, PBKDF2 인증'],['CatalogService / AlbumService','최신곡·개별 곡 추가, 앨범·발매일·작사/작곡 수집과 DB 캐시'],['DAO / PlaylistService','PreparedStatement, 자원 종료, 소유권·공개 범위·CRUD·순서 행 잠금'],['MusicLibraryService','보관함과 음원 LEFT JOIN, 재생 전 DB의 파일·이용권 조회'],['LocalAudioService / AudioPlayer','보유 MP3 연결과 길이 확인, 실제 PCM 출력, 60초 프레임 제한'],['MembershipService','DB 시각으로 만료 확인, UUID로 중복 결제 방지, 구매·취소 이력 보존'],['OnlineVideoPanel / Service','앱 중앙 표준 임베드 재생, DB 영상·등급 조회, 무료 60초/Plus 전체']],[155,W-155])
p('데이터 정확성과 기존 자료 보존','sub')
p('기존 예시 30건을 삭제하지 않는다. 차트 곡 ID는 1,000,000,000 + 멜론 곡 번호, 가수 ID는 2,000,000,000 + 멜론 가수 번호로 매핑한다. 원문에 없는 재생 길이·장르·발매일은 임의 생성하지 않는다. 길이는 NULL을 허용하도록 추가 SQL에서 변경하고 기존 양수 CHECK는 유지한다. MP3를 연결하면 확인한 길이를 DB에 기록한다.')
p('트랜잭션과 접근 제어','sub')
p('소유자만 목록을 변경할 수 있고 다른 회원의 비공개 목록은 번호를 알아도 조회할 수 없다. 순서 교환은 부모 행 잠금과 사용하지 않는 양수 임시 순서로 처리하며 오류 시 롤백한다. 차트는 100곡 검증을 통과한 결과만 저장한다. 모의 결제는 회원 행 잠금과 주문 UUID로 중복을 막고 취소 기록을 남긴다.')
p('DB 접속 정보·개인 인증 정보·사용자 연결 음원은 공개 Git에 포함하지 않는다. 결제 성공 여부와 플레이리스트는 로컬 JSON이 아니라 Oracle에서 조회한다.','small')

page();heading(5,'로그인 없이 열리는 음악 홈');image('13_home_live.png',370)
p('실제 Oracle에 연결한 홈 화면이다. 실제 곡 제목·가수·앨범 커버를 표시한다. 커버를 누르면 앨범 상세가 열린다. 왼쪽에서 곡과 개인/공개 목록으로 이동하며 하단 플레이어는 화면을 바꾸어도 유지된다.')
p('색상·글꼴·버튼·표·입력란은 theme/muse.properties에서 관리한다. 차트 커버는 원문 URL에서 비동기로 가져오며 실패하면 앱의 자체 그래픽을 표시한다. 화면의 TOP100 문구를 제거하고 추가 수집곡까지 보관함에 표시한다.','small')

page();heading(6,'실제 곡 검색과 목록 담기');image('14_library_live.png',290);image('16_playlist_live.png',260)
p('상단은 음악 보관함 150곡 조회와 필터 화면이다. 제목·가수·앨범으로 검색하며 즐겨찾기로 필터링하고 재생 확인/제한 상태를 구분한다. 하단은 실제 곡을 새 플레이리스트에 담은 상세 화면이다. 음원 미연결 곡도 검색·담기·정렬할 수 있다.','small')

page();heading(7,'플레이리스트 CRUD와 순서 변경');image('15_my_playlist_live.png',270);image('17_reorder_live.png',255)
p('생성한 목록의 제목·설명·공개 여부를 수정하고 곡을 추가·제거·이동할 수 있다. 위·아래 이동은 실제 DB의 play_order를 변경한다. 공개 목록의 좋아요와 취소도 저장된다. 삭제 시 목록 연결과 좋아요를 정리하며 곡·가수 원본은 보존한다.','small')

page();heading(8,'무료 1분과 모의 Plus 결제');image('18_free_pass_live.png',260);image('19_paid_pass_live.png',260)
p('영상과 연결한 음원은 무료 상태에서 첫 60초까지 재생한다. Plus 30일 이용권은 표시 금액 4,900원의 과제용 모의 결제이며 실제 청구액은 0원이다. 구매 후 Oracle의 결제 이력과 유효기간을 읽어 전체 길이 재생을 허용한다. 멜론 스트리밍 권한은 포함하지 않는다.','small')

page();heading(9,'앱 안의 뮤비와 등급별 재생');image('28_in_app_mv_live.jpg',350);image('21_cancelled_pass_live.png',205)
p('재생 버튼을 누르면 새 창 없이 앱 중앙에서 표준 YouTube 임베드 영상을 재생한다. 오른쪽 추천 목록·댓글 영역이 없다. 무료 등급은 60초에서 멈추며 끄는 체크박스가 없다. Oracle 모의 구매 후 62초 이후 재생과 취소 후 제한 복귀를 확인했다. 137곡의 영상 메타데이터 중 119곡의 실제 임베드 재생을 확인했다. 미연결 13곡과 제한/미확인 18곡은 구분한다.','small')
p('결제 취소는 CANCELLED 이력으로 유지된다. 다른 유효 주문이 없으면 무료 상태로 돌아가며 기존 재생을 중지하고 권한을 다시 조회한다.','small')

page();heading(10,'앨범 커버로 보는 발매·크레딧');image('25_album_info_live.png',475)
p('하단 플레이어의 NEON 커버를 클릭하여 실제로 열린 창이다. 앨범 제목·아티스트·발매일·유형·장르·발매사·기획사와 선택한 곡의 작사·작곡·편곡을 표시한다. 곡마다 크레딧이 다르므로 앨범 정보와 선택 곡 크레딧을 구분한다.')
p('정보는 원본 곡/앨범 페이지에서 필요한 사실만 추출하여 DB에 저장한다. 다음 열기에는 DB 캐시를 사용하고 정보 새로고침으로 갱신할 수 있다. 원문에 없는 값은 임의 생성하지 않고 “제공된 정보 없음”으로 표시한다. 가사와 상업 음원은 수집하지 않았다.','small')

page();heading(11,'별 즐겨찾기 저장과 해제');image('23_favorite_library_live.png',270);image('26_album_unfavorite_live.png',240)
p('하단 플레이어·곡 목록·앨범 창의 별을 누르면 SONG_FAVORITE에 저장하고 다시 누르면 해제한다. 왼쪽 즐겨찾기 곡에서 모아 볼 수 있으며 프로그램을 다시 켜도 DB의 별 상태를 읽는다. 다른 회원의 즐겨찾기에는 영향을 주지 않는다.','small')
p('상단은 별표한 곡을 모아 보는 화면이고, 하단은 앨범 창에서 즐겨찾기를 해제한 모습이다. 홈의 곡 수 문구와 좋아요한 목록 메뉴, 곡 가져오기 및 연결 설정 버튼을 제거하여 음악 탐색과 개인 보관함에 집중하도록 정리했다.','small')

page();heading(12,'검증 결과와 실행 방법')
table([['검증','결과'],['Java 17 Maven package','실행 JAR 생성 성공, 실제 Oracle 포함 20 tests / failures 0 / errors 0'],['CoreTest 3개','해시·salt·DEMO 인증 거부·입력 검증'],['PlaybackTest 4개','무료·유료·만료/취소 정책, CC0 MP3 4개 실제 디코딩'],['OracleIntegrationTest 1개','CRUD·접근 제어·좋아요·순서·동시 변경·롤백·원본 보존'],['MusicOracleTest 1개','자동 보관함 재사용, 보관함 조회, 중복 결제·취소·재구매'],['MelonChartTest 2개','실제 수집 스냅샷 파싱, 부분 차트·중복 순위 거부'],['LocalAudioOracleTest 1개','실제 MP3 연결·길이 확인·음원 없는 곡 재생 거부'],['AlbumFactsTest 2개','실제 발매일·다중 작사/작곡·필수 정보·최신곡 파싱'],['FavoriteOracleTest 3개','별 저장/해제·회원 분리·중복 방지, 상세 갱신 롤백, 재수집 중복 방지'],['OnlineVideoTest / OracleTest 3개','영상 주소 검증, 150곡 DB 연결·외래키·저장 ID 조회']],[155,W-155])
p('IntelliJ 실행','sub')
p('① pom.xml을 Maven 프로젝트로 열고 SDK를 Java 17로 설정한다.<br/>② 빈 스키마에서만 원본 SQL 01을 실행한다. 기존 DB에서는 재실행하지 않는다.<br/>③ 추가 SQL 02 → 06 → 08 → 09 → 10 → 11 → 13 → 14를 SQL Developer에서 F5로 실행한다.<br/>④ 접속 설정 후 App.main()을 실행한다. 빌드 후에는 run.bat으로 실행할 수 있다.')
p('제출 자료와 출처','sub')
p('공개 저장소: https://github.com/yumin0117/JavaTermProject<br/>보고서 PDF·E-R SVG·주요 화면·Java 소스·SQL Developer 최종 17개 테이블 DDL 내보내기 15·음악 정보 재현 SQL 11를 포함한다. 표지는 직접 작성하도록 항목 위치만 마련했다. 제출 커밋명은 “★ 기말과제제출 ★”이다.','small')
p('차트: https://www.melon.com/chart/index.htm (2026.11.26 11:00 KST)<br/>파서: https://jsoup.org / 테마: https://www.formdev.com/flatlaf/<br/>오디오 검증: https://archive.org/details/musopen-chopin (CC0 1.0)<br/>영상 API: https://developers.google.com/youtube/iframe_api_reference','small')
out=ROOT/'docs/기말보고서_2601110285_김유민.pdf'
doc=SimpleDocTemplate(str(out),pagesize=A4,rightMargin=50,leftMargin=50,topMargin=48,bottomMargin=60,title='muse - 음악 플레이리스트 공유 시스템',author='')
doc.build(story,onFirstPage=cover,onLaterPages=footer);print(out)
import xml.etree.ElementTree as ET
ns='http://www.w3.org/2000/svg';ET.register_namespace('',ns)
combined=ET.Element('{'+ns+'}svg',width='510',height='970',viewBox='0 0 510 970')
for name,y in [('er-core.svg',0),('er-extensions.svg',515)]:
 group=ET.parse(ROOT/'docs/design'/name).getroot();group.set('x','0');group.set('y',str(y));combined.append(group)
ET.ElementTree(combined).write(ROOT/'docs/design/er-final.svg',encoding='utf-8',xml_declaration=True)
