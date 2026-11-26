from pathlib import Path
root=Path(__file__).resolve().parents[1]
names=['pom.xml','sql/02_add_sequence.sql','sql/06_music_and_membership.sql','sql/08_melon_chart.sql','sql/09_melon_snapshot.sql','sql/10_catalog_album_favorites.sql','sql/11_library_facts_snapshot.sql','src/main/resources/theme/muse.properties','config/db.properties.example','.gitignore','run.bat']+[p.relative_to(root).as_posix() for p in sorted((root/'src').rglob('*.java'))]
parts=['# 파일별 전체 소스 코드\n\n실제 구현 파일의 UTF-8 원문입니다. DB 비밀번호는 포함하지 않습니다.\n']
for name in names:
    lang='java' if name.endswith('.java') else 'xml' if name.endswith('.xml') else 'sql' if name.endswith('.sql') else 'text'
    parts.append('## '+name+'\n\n```'+lang+'\n'+(root/name).read_text(encoding='utf-8').rstrip()+'\n```\n')
(root/'docs/전체소스코드.md').write_text('\n'.join(parts),encoding='utf-8')
