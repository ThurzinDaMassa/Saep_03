import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Verificação direta das invariantes do banco por JDBC. */
public class VerificarBanco {
 public static void main(String[] args) throws Exception {
  Properties p=new Properties();try(var in=Files.newInputStream(Path.of(args[0]))){p.load(in);}
  String url="jdbc:mysql://"+p.getProperty("db.host")+":"+p.getProperty("db.port")+"/"+p.getProperty("db.name")+"?useSSL=false&allowPublicKeyRetrieval=true";
  try(var db=DriverManager.getConnection(url,p.getProperty("db.user"),p.getProperty("db.password",""));var st=db.createStatement()) {
   try(var rs=st.executeQuery("SELECT VERSION()")){rs.next();System.out.println("Servidor validado: "+rs.getString(1));}
   check(st,"Saldos conciliados com movimentações", "SELECT COUNT(*) FROM pecas p WHERE p.estoque <> COALESCE((SELECT SUM(CASE WHEN m.tipo='saida' THEN -m.quantidade ELSE m.quantidade END) FROM movimentacoes m WHERE m.peca_id=p.id),0)");
   check(st,"Estoque não negativo", "SELECT COUNT(*) FROM pecas WHERE estoque<0");
   check(st,"Senhas com PBKDF2", "SELECT COUNT(*) FROM usuarios WHERE senha_hash NOT LIKE '210000$%$%'");
   check(st,"Veículos sem ordens ativas duplicadas", "SELECT COUNT(*) FROM (SELECT veiculo_id FROM ordens WHERE status NOT IN ('entregue','cancelada') GROUP BY veiculo_id HAVING COUNT(*)>1) a");
   try(var rs=st.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()")){rs.next();if(rs.getInt(1)!=11)throw new AssertionError("Esperadas 11 tabelas");System.out.println("APROVADO: 11 tabelas relacionais");}
  }
 }
 static void check(Statement st,String label,String sql) throws Exception {
  try(var rs=st.executeQuery(sql)){rs.next();if(rs.getInt(1)!=0)throw new AssertionError(label+": "+rs.getInt(1)+" inconsistências");System.out.println("APROVADO: "+label);}
 }
}
