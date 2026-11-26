import kr.ac.yumin.playlist.config.Database;
import java.sql.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Reproducible public music facts only. Never exports members, payments or favorites. */
public class ExportLibrary {
    private static String literal(Object value) {
        if(value==null)return "NULL";
        if(value instanceof java.sql.Date d)return "DATE '"+d.toLocalDate()+"'";
        if(value instanceof Timestamp t)return "TIMESTAMP '"+t.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSSSSSSSS"))+"'";
        if(value instanceof Number)return value.toString();
        return "'"+value.toString().replace("'","''")+"'";
    }
    private static void emit(Connection c,StringBuilder sql,String table,String where,String... keys)throws Exception {
        try(var p=c.prepareStatement("SELECT * FROM "+table+(where.isBlank()?"":" WHERE "+where)+" ORDER BY "+String.join(",",keys));var r=p.executeQuery()) {
            var m=r.getMetaData();List<String> names=new ArrayList<>();for(int i=1;i<=m.getColumnCount();i++)names.add(m.getColumnName(i));
            Set<String> keySet=Set.of(keys);int count=0;
            while(r.next()) {
                List<String> values=new ArrayList<>(),select=new ArrayList<>(),on=new ArrayList<>(),updates=new ArrayList<>();
                for(int i=1;i<=names.size();i++) {Object value=switch(m.getColumnType(i)){case Types.DATE->r.getDate(i);case Types.TIMESTAMP,Types.TIMESTAMP_WITH_TIMEZONE->r.getTimestamp(i);case Types.NUMERIC,Types.DECIMAL->r.getBigDecimal(i);default->r.getString(i);};String n=names.get(i-1),v=literal(value);values.add("x."+n);select.add(v+" "+n);if(keySet.contains(n))on.add("d."+n+"=x."+n);else updates.add("d."+n+"=x."+n);}
                sql.append("MERGE INTO ").append(table).append(" d USING(SELECT ").append(String.join(",",select)).append(" FROM DUAL) x ON(").append(String.join(" AND ",on)).append(") ");
                if(!updates.isEmpty())sql.append("WHEN MATCHED THEN UPDATE SET ").append(String.join(",",updates)).append(' ');
                sql.append("WHEN NOT MATCHED THEN INSERT(").append(String.join(",",names)).append(") VALUES(").append(String.join(",",values)).append(");\n");count++;
            }
            System.out.println(table+": "+count);
        }
    }
    public static void main(String[] args)throws Exception {
        StringBuilder out=new StringBuilder("-- Public music facts snapshot. Run 01,02,06,08,09,10 first.\n-- No user accounts, credentials, orders, favorites or commercial audio.\nSET DEFINE OFF;\n");
        try(Connection c=Database.open()) {
            c.setAutoCommit(false);try(var s=c.createStatement()){s.execute("SET TRANSACTION READ ONLY");}
            emit(c,out,"ARTIST","artist_id>=2000000000","ARTIST_ID");
            emit(c,out,"SONG","song_id>=1000000000","SONG_ID");
            emit(c,out,"SONG_ARTIST","song_id>=1000000000","SONG_ID","ARTIST_ID");
            emit(c,out,"MUSIC_CATALOG","","SONG_ID");
            emit(c,out,"ALBUM_INFO","","ALBUM_ID");
            emit(c,out,"SONG_DETAIL","","SONG_ID");
            emit(c,out,"SONG_CREDIT","","SONG_ID","CREDIT_ROLE","CREDIT_ORDER");
            c.commit();
        }
        out.append("COMMIT;\n");Path target=Path.of("sql/11_library_facts_snapshot.sql");Files.writeString(target,out,StandardCharsets.UTF_8);System.out.println(target);
    }
}
