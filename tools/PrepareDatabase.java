import kr.ac.yumin.playlist.config.Database;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
public class PrepareDatabase {
    public static void main(String[] args)throws Exception{
        try(Connection c=Database.open()){
            System.out.println("Oracle JDBC connection confirmed: "+c.getMetaData().getUserName());
            for(String file:args){
                StringBuilder sql=new StringBuilder();boolean block=false;
                for(String line:Files.readAllLines(Path.of(file),StandardCharsets.UTF_8)){
                    String t=line.strip();if(t.isEmpty()||t.startsWith("--")||t.startsWith("SET "))continue;
                    if(t.equals("DECLARE")||t.equals("BEGIN"))block=true;
                    if(t.equals("/")){execute(c,sql.toString());sql.setLength(0);block=false;continue;}
                    sql.append(line).append('\n');
                    if(!block&&t.endsWith(";")){String statement=sql.toString().strip();execute(c,statement.substring(0,statement.length()-1));sql.setLength(0);}
                }
                if(!sql.toString().isBlank())throw new IllegalArgumentException("Unterminated SQL: "+file);
                System.out.println("Applied: "+file);
            }
        }
    }
    private static void execute(Connection c,String sql)throws SQLException{try(PreparedStatement p=c.prepareStatement(sql)){p.execute();}}
}
