package oficina;

import com.sun.net.httpserver.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Servidor local Java 17+, JDBC e MySQL. Sem framework web. */
public class App {
    private static final Properties CFG = new Properties();
    private static final Path ROOT = locateRoot();
    private static final ConcurrentMap<String, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> CLOSED = Set.of("concluida", "entregue", "cancelada");
    private static final Map<String,Set<String>> TRANSITIONS = Map.of(
        "aberta", Set.of("em_execucao", "aguardando_pecas", "cancelada"),
        "em_execucao", Set.of("aguardando_pecas", "concluida", "cancelada"),
        "aguardando_pecas", Set.of("em_execucao", "cancelada"),
        "concluida", Set.of("entregue"), "entregue", Set.of(), "cancelada", Set.of());
    private record Session(long userId, String csrf, long expires) {}
    private static class Rule extends RuntimeException {
        final int code; Rule(int code, String message) { super(message); this.code=code; }
    }
    private static Path locateRoot() {
        String explicit = System.getProperty("torq.root");
        if (explicit != null) return Path.of(explicit).toAbsolutePath().normalize();
        Path current = Path.of(".").toAbsolutePath().normalize();
        for (Path candidate : List.of(current, current.resolve("TorqOficina"))) {
            if (Files.isDirectory(candidate.resolve("web")) && Files.isDirectory(candidate.resolve("banco"))) return candidate;
        }
        return current;
    }
    private static Path configurationPath() {
        String explicit = System.getProperty("torq.config");
        if (explicit != null) return ROOT.resolve(explicit).normalize();
        Path local = ROOT.resolve("config.local.properties");
        return Files.isRegularFile(local) ? local : ROOT.resolve("config.properties");
    }
    public static void main(String[] args) throws Exception {
        Path config = configurationPath();
        if (!Files.isRegularFile(config)) throw new FileNotFoundException("Configuração não encontrada: " + config + ". Ajuste o Working directory para a pasta TorqOficina ou informe -Dtorq.root=CAMINHO_DA_PASTA.");
        try (var in = Files.newInputStream(config)) { CFG.load(in); }
        System.out.println("Projeto: " + ROOT + " | Configuração: " + config.getFileName());
        System.out.println("Banco: " + CFG.getProperty("db.host", "127.0.0.1") + ":" + CFG.getProperty("db.port", "3306") + " / " + dbName());
        Class.forName("com.mysql.cj.jdbc.Driver");
        if (Arrays.asList(args).contains("--init")) { initialize(); return; }
        try (var db=connect()) { one(db,"SELECT COUNT(*) n FROM usuarios"); }
        int port=Integer.parseInt(CFG.getProperty("server.port","8080"));
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",port),0);
        server.createContext("/api/",App::api);
        server.createContext("/",App::staticFile);
        server.setExecutor(Executors.newFixedThreadPool(12));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
        server.start();
        System.out.println("Torq Oficina em http://localhost:"+port+" | Java + MySQL");
    }
    private static String dbName() {
        String n=CFG.getProperty("db.name","torq_oficina");
        if(!n.matches("[a-zA-Z0-9_]+")) throw new IllegalArgumentException("Nome de banco inválido.");
        return n;
    }
    private static Connection connection(boolean withDatabase) throws SQLException {
        String url="jdbc:mysql://"+CFG.getProperty("db.host","127.0.0.1")+":"+CFG.getProperty("db.port","3306")+"/"+
            (withDatabase?dbName():"")+"?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&connectionTimeZone=LOCAL";
        return DriverManager.getConnection(url,CFG.getProperty("db.user","root"),CFG.getProperty("db.password",""));
    }
    private static Connection connect() throws SQLException { return connection(true); }
    private static void initialize() throws Exception {
        try(var db=connection(false); var stmt=db.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS `"+dbName()+"` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
        try(var db=connect()) {
            String sql=Files.readString(ROOT.resolve("banco/02_estrutura.sql"),StandardCharsets.UTF_8).replaceAll("(?m)^--.*$","");
            for(String s:sql.split(";")) if(!s.isBlank()) try(var st=db.createStatement()){st.execute(s);}
            if(number(one(db,"SELECT COUNT(*) n FROM usuarios").get("n"))==0) {
                db.setAutoCommit(false);
                try { seed(db); db.commit(); } catch(Exception e){ db.rollback(); throw e; }
            }
        }
        System.out.println("Banco "+dbName()+" preparado. Dados existentes preservados.");
    }
    private static void seed(Connection db) throws Exception {
        insert(db,"INSERT INTO usuarios(nome,email,senha_hash,perfil) VALUES(?,?,?,?)","Administrador","admin@torq.local",hash("Oficina@123"),"administrador");
        insert(db,"INSERT INTO usuarios(nome,email,senha_hash,perfil) VALUES(?,?,?,?)","Atendimento","atendimento@torq.local",hash("Atende@123"),"atendente");
        insert(db,"INSERT INTO usuarios(nome,email,senha_hash,perfil) VALUES(?,?,?,?)","Mecânico","mecanico@torq.local",hash("Mecanico@123"),"mecanico");
        insert(db,"INSERT INTO clientes(nome,documento,telefone,email) VALUES(?,?,?,?)","Marina Oliveira","DEMO-001","(47) 99911-2020","marina@example.com");
        insert(db,"INSERT INTO clientes(nome,documento,telefone,email) VALUES(?,?,?,?)","Rafael Costa","DEMO-002","(47) 99922-3030","rafael@example.com");
        insert(db,"INSERT INTO clientes(nome,documento,telefone,email) VALUES(?,?,?,?)","Beatriz Souza","DEMO-003","(47) 99933-4040","beatriz@example.com");
        insert(db,"INSERT INTO veiculos(cliente_id,placa,marca,modelo,ano,km) VALUES(?,?,?,?,?,?)",1,"ABC1D23","Volkswagen","Gol",2019,68400);
        insert(db,"INSERT INTO veiculos(cliente_id,placa,marca,modelo,ano,km) VALUES(?,?,?,?,?,?)",2,"XYZ2E45","Chevrolet","Onix",2021,42500);
        insert(db,"INSERT INTO veiculos(cliente_id,placa,marca,modelo,ano,km) VALUES(?,?,?,?,?,?)",3,"DEF3F67","Fiat","Argo",2020,57300);
        insert(db,"INSERT INTO servicos(nome,descricao,preco) VALUES(?,?,?)","Troca de óleo","Substituição de óleo e filtro",new BigDecimal("90.00"));
        insert(db,"INSERT INTO servicos(nome,descricao,preco) VALUES(?,?,?)","Revisão de freios","Inspeção e manutenção do conjunto",new BigDecimal("180.00"));
        insert(db,"INSERT INTO servicos(nome,descricao,preco) VALUES(?,?,?)","Alinhamento","Ajuste da geometria da suspensão",new BigDecimal("120.00"));
        String[] names={"Óleo 5W30 1 litro","Filtro de óleo","Pastilhas de freio","Filtro de ar"};
        String[] prices={"42.90","35.00","165.00","48.00"}; int[] stocks={20,3,8,2}; int[] mins={5,4,3,3};
        for(int i=0;i<4;i++) {
            long id=insert(db,"INSERT INTO pecas(codigo,nome,preco,estoque,estoque_minimo) VALUES(?,?,?,?,?)","PC-00"+(i+1),names[i],new BigDecimal(prices[i]),stocks[i],mins[i]);
            movement(db,id,null,1,"entrada",stocks[i],stocks[i],"Saldo inicial de demonstração");
        }
        long o1=insert(db,"INSERT INTO ordens(veiculo_id,responsavel_id,relato,status) VALUES(?,?,?,?)",1,3,"Revisão preventiva e troca de óleo","em_execucao");
        history(db,o1,1,"Ordem aberta"); history(db,o1,3,"Status alterado para em_execucao");
        insert(db,"INSERT INTO ordem_servicos(ordem_id,servico_id,quantidade,preco_unitario) VALUES(?,?,?,?)",o1,1,1,new BigDecimal("90.00"));
        history(db,o1,3,"Serviço adicionado: Troca de óleo (1)");
        long o2=insert(db,"INSERT INTO ordens(veiculo_id,responsavel_id,relato,status) VALUES(?,?,?,?)",2,3,"Ruído ao frear. Aguardando inspeção.","aberta"); history(db,o2,1,"Ordem aberta");
        long o3=insert(db,"INSERT INTO ordens(veiculo_id,responsavel_id,relato,status) VALUES(?,?,?,?)",3,3,"Substituição do filtro de ar","aguardando_pecas"); history(db,o3,1,"Ordem aberta"); history(db,o3,3,"Status alterado para aguardando_pecas");
        audit(db,1,"Carga inicial de dados fictícios");
    }
    private static List<Map<String,Object>> query(Connection db,String sql,Object...args) throws SQLException {
        try(var st=db.prepareStatement(sql)) {
            bind(st,args); try(var rs=st.executeQuery()) {
                var list=new ArrayList<Map<String,Object>>(); var meta=rs.getMetaData();
                while(rs.next()) { var row=new LinkedHashMap<String,Object>(); for(int i=1;i<=meta.getColumnCount();i++) { Object v=rs.getObject(i); if(v instanceof java.sql.Timestamp || v instanceof java.sql.Date) v=v.toString(); row.put(meta.getColumnLabel(i),v); } list.add(row); }
                return list;
            }
        }
    }
    private static Map<String,Object> one(Connection db,String sql,Object...args) throws SQLException {
        var q=query(db,sql,args); if(q.isEmpty()) throw new Rule(404,"Registro não encontrado."); return q.get(0);
    }
    private static void bind(PreparedStatement st,Object...args) throws SQLException { for(int i=0;i<args.length;i++) st.setObject(i+1,args[i]); }
    private static long insert(Connection db,String sql,Object...args) throws SQLException {
        try(var st=db.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) { bind(st,args); st.executeUpdate(); try(var rs=st.getGeneratedKeys()){ if(rs.next()) return rs.getLong(1); return 0; } }
    }
    private static void update(Connection db,String sql,Object...args) throws SQLException { try(var st=db.prepareStatement(sql)){bind(st,args); st.executeUpdate();} }
    private static long number(Object value) { return ((Number)value).longValue(); }
    private static void history(Connection db,long order,long user,String event) throws SQLException { insert(db,"INSERT INTO historico_ordens(ordem_id,usuario_id,evento) VALUES(?,?,?)",order,user,event); }
    private static void audit(Connection db,long user,String event) throws SQLException { insert(db,"INSERT INTO auditoria(usuario_id,acao) VALUES(?,?)",user,event); }
    private static void movement(Connection db,long part,Long order,long user,String type,int qty,int balance,String reason) throws SQLException {
        insert(db,"INSERT INTO movimentacoes(peca_id,ordem_id,usuario_id,tipo,quantidade,saldo_apos,motivo) VALUES(?,?,?,?,?,?,?)",part,order,user,type,qty,balance,reason);
    }
    private static String token() { byte[] b=new byte[32]; RANDOM.nextBytes(b); return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
    private static String hash(String password) throws Exception {
        byte[] salt=new byte[16]; RANDOM.nextBytes(salt); return "210000$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(derive(password,salt,210000));
    }
    private static byte[] derive(String password,byte[] salt,int iterations) throws Exception {
        var spec=new PBEKeySpec(password.toCharArray(),salt,iterations,256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); } finally {spec.clearPassword();}
    }
    private static boolean verify(String password,String stored) throws Exception {
        var p=stored.split("\\$"); return MessageDigest.isEqual(Base64.getDecoder().decode(p[2]),derive(password,Base64.getDecoder().decode(p[1]),Integer.parseInt(p[0])));
    }
    private static Map<String,String> form(String body) {
        var data=new HashMap<String,String>(); if(body==null || body.isEmpty()) return data;
        for(String item:body.split("&")){var pair=item.split("=",2); data.put(URLDecoder.decode(pair[0],StandardCharsets.UTF_8),pair.length>1?URLDecoder.decode(pair[1],StandardCharsets.UTF_8):"");} return data;
    }
    private static String text(Map<String,String> f,String key,int max) {
        String v=f.getOrDefault(key,"").trim(); if(v.isEmpty() || v.length()>max) throw new Rule(400,"Preencha corretamente: "+key+" (até "+max+" caracteres)."); return v;
    }
    private static String optional(Map<String,String> f,String key,int max) { String v=f.getOrDefault(key,"").trim(); if(v.length()>max) throw new Rule(400,"Campo muito longo: "+key); return v; }
    private static int integer(Map<String,String> f,String key,int min,int max) {
        try {int v=Integer.parseInt(f.getOrDefault(key,"")); if(v<min || v>max) throw new NumberFormatException(); return v;} catch(NumberFormatException e){throw new Rule(400,"Valor inválido para "+key+".");}
    }
    private static long id(Map<String,String> f,String key) { return integer(f,key,1,Integer.MAX_VALUE); }
    private static BigDecimal money(Map<String,String> f,String key) {
        try {var v=new BigDecimal(f.getOrDefault(key,"").replace(",",".")); if(v.signum()<0 || v.scale()>2 || v.compareTo(new BigDecimal("99999999.99"))>0) throw new NumberFormatException(); return v.setScale(2);} catch(Exception e){throw new Rule(400,"Preço inválido. Use até duas casas decimais.");}
    }
    private static String email(Map<String,String> f,String key,boolean required) {
        String v=required?text(f,key,150):optional(f,key,150); if(!v.isEmpty()&&!v.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new Rule(400,"E-mail inválido."); return v.toLowerCase(Locale.ROOT);
    }
    private static void permit(Map<String,Object> user,String...roles) { if(!Set.of(roles).contains(user.get("perfil"))) throw new Rule(403,"Seu perfil não tem permissão para esta ação."); }
    private static Session session(HttpExchange x) {
        String cookie=x.getRequestHeaders().getFirst("Cookie"); if(cookie!=null) for(String c:cookie.split(";")){if(c.trim().startsWith("torq_session=")){var s=SESSIONS.get(c.trim().substring(13)); if(s!=null&&s.expires>System.currentTimeMillis())return s;}} throw new Rule(401,"Entre para continuar.");
    }
    private static void api(HttpExchange x) throws IOException {
        try {
            String path=x.getRequestURI().getPath().substring(5), method=x.getRequestMethod();
            if(!Set.of("GET","POST").contains(method)) throw new Rule(405,"Método não permitido.");
            byte[] bytes=x.getRequestBody().readNBytes(65537); if(bytes.length>65536) throw new Rule(413,"Dados muito grandes.");
            var f=form(new String(bytes,StandardCharsets.UTF_8));
            try(var db=connect()) {
                if(path.equals("login") && method.equals("POST")) {
                    var users=query(db,"SELECT * FROM usuarios WHERE email=? AND ativo=TRUE",email(f,"email",true));
                    if(users.isEmpty() || !verify(f.getOrDefault("senha",""),users.get(0).get("senha_hash").toString())) throw new Rule(401,"E-mail ou senha incorretos.");
                    var user=users.get(0); String sid=token(),csrf=token(); long uid=number(user.get("id"));
                    SESSIONS.entrySet().removeIf(e->e.getValue().expires<System.currentTimeMillis());
                    SESSIONS.put(sid,new Session(uid,csrf,System.currentTimeMillis()+8*60*60*1000));
                    x.getResponseHeaders().add("Set-Cookie","torq_session="+sid+"; HttpOnly; SameSite=Strict; Path=/; Max-Age=28800");
                    audit(db,uid,"Login realizado"); reply(x,200,Map.of("usuario",publicUser(user),"csrf",csrf)); return;
                }
                Session ses=session(x);
                var activeUsers=query(db,"SELECT * FROM usuarios WHERE id=? AND ativo=TRUE",ses.userId);
                if(activeUsers.isEmpty()) { SESSIONS.values().removeIf(s->s.userId==ses.userId); throw new Rule(401,"Seu acesso foi desativado. Entre com um usuário ativo."); }
                var user=activeUsers.get(0);
                if(method.equals("POST")&&!MessageDigest.isEqual(ses.csrf.getBytes(StandardCharsets.UTF_8),Objects.toString(x.getRequestHeaders().getFirst("X-CSRF-Token"),"").getBytes(StandardCharsets.UTF_8))) throw new Rule(403,"Sessão inválida. Atualize a página.");
                if(path.equals("logout")&&method.equals("POST")) {
                    SESSIONS.values().removeIf(s->s.equals(ses)); x.getResponseHeaders().add("Set-Cookie","torq_session=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0"); reply(x,200,Map.of("ok",true));return;
                }
                if(path.equals("me")&&method.equals("GET")){reply(x,200,Map.of("usuario",publicUser(user),"csrf",ses.csrf));return;}
                if(method.equals("GET")){reply(x,200,read(db,path,form(x.getRequestURI().getRawQuery()),user));return;}
                db.setAutoCommit(false);
                try { Object result=write(db,path,f,user); db.commit(); reply(x,200,result); }
                catch(Exception e){db.rollback();throw e;}
            }
        } catch(Rule e){reply(x,e.code,Map.of("erro",e.getMessage()));}
        catch(SQLException e) {
            System.err.println("SQL: "+e.getSQLState()+" "+e.getMessage());
            String state=Objects.toString(e.getSQLState(),"");
            if(state.startsWith("23")) reply(x,409,Map.of("erro","Registro duplicado ou referência inválida. Confira os dados."));
            else if(state.equals("40001")) reply(x,409,Map.of("erro","Outra operação está em andamento. Tente novamente."));
            else reply(x,500,Map.of("erro","Não foi possível acessar o banco. Confira a conexão no servidor."));
        } catch(Exception e){e.printStackTrace();reply(x,500,Map.of("erro","Falha interna ao processar a operação."));}
    }
    private static Map<String,Object> publicUser(Map<String,Object> u){return Map.of("id",u.get("id"),"nome",u.get("nome"),"email",u.get("email"),"perfil",u.get("perfil"));}
    private static final String ORDER_SELECT="SELECT o.*,v.placa,v.marca,v.modelo,c.nome cliente,u.nome responsavel, COALESCE((SELECT SUM(quantidade*preco_unitario) FROM ordem_servicos WHERE ordem_id=o.id),0)+COALESCE((SELECT SUM(quantidade*preco_unitario) FROM ordem_pecas WHERE ordem_id=o.id),0) total FROM ordens o JOIN veiculos v ON v.id=o.veiculo_id JOIN clientes c ON c.id=v.cliente_id JOIN usuarios u ON u.id=o.responsavel_id ";
    private static Object read(Connection db,String path,Map<String,String> params,Map<String,Object> user) throws SQLException {
        return switch(path) {
            case "clientes" -> query(db,"SELECT * FROM clientes ORDER BY nome");
            case "veiculos" -> query(db,"SELECT v.*,c.nome cliente FROM veiculos v JOIN clientes c ON c.id=v.cliente_id ORDER BY v.id DESC");
            case "servicos" -> query(db,"SELECT * FROM servicos ORDER BY nome");
            case "pecas" -> query(db,"SELECT *,estoque<=estoque_minimo alerta FROM pecas ORDER BY nome");
            case "usuarios" -> {permit(user,"administrador");yield query(db,"SELECT id,nome,email,perfil,ativo FROM usuarios ORDER BY nome");}
            case "responsaveis" -> query(db,"SELECT id,nome,perfil FROM usuarios WHERE ativo=TRUE ORDER BY nome");
            case "ordens" -> query(db,ORDER_SELECT+"ORDER BY o.id DESC");
            case "ordem" -> {
                long order=id(params,"id"); var detail=new LinkedHashMap<String,Object>();
                detail.put("ordem",one(db,ORDER_SELECT+"WHERE o.id=?",order));
                detail.put("servicos",query(db,"SELECT i.*,s.nome FROM ordem_servicos i JOIN servicos s ON s.id=i.servico_id WHERE i.ordem_id=?",order));
                detail.put("pecas",query(db,"SELECT i.*,p.nome,p.codigo FROM ordem_pecas i JOIN pecas p ON p.id=i.peca_id WHERE i.ordem_id=?",order));
                detail.put("historico",query(db,"SELECT h.*,u.nome usuario FROM historico_ordens h JOIN usuarios u ON u.id=h.usuario_id WHERE h.ordem_id=? ORDER BY h.id DESC",order));yield detail;
            }
            case "movimentacoes" -> query(db,"SELECT m.*,p.nome peca,p.codigo,u.nome usuario FROM movimentacoes m JOIN pecas p ON p.id=m.peca_id JOIN usuarios u ON u.id=m.usuario_id ORDER BY m.id DESC LIMIT 500");
            case "historico" -> query(db,"SELECT h.*,u.nome usuario,v.placa FROM historico_ordens h JOIN usuarios u ON u.id=h.usuario_id JOIN ordens o ON o.id=h.ordem_id JOIN veiculos v ON v.id=o.veiculo_id ORDER BY h.id DESC LIMIT 500");
            case "auditoria" -> {permit(user,"administrador");yield query(db,"SELECT a.*,u.nome usuario FROM auditoria a JOIN usuarios u ON u.id=a.usuario_id ORDER BY a.id DESC LIMIT 500");}
            case "dashboard" -> Map.of(
                "resumo",one(db,"SELECT (SELECT COUNT(*) FROM ordens WHERE status NOT IN ('entregue','cancelada')) atendimentos,(SELECT COUNT(*) FROM ordens WHERE status='em_execucao') em_execucao,(SELECT COUNT(*) FROM ordens WHERE status='concluida') prontas,(SELECT COUNT(*) FROM pecas WHERE estoque<=estoque_minimo) alertas"),
                "ordens",query(db,ORDER_SELECT+"WHERE o.status NOT IN ('entregue','cancelada') ORDER BY o.id DESC"),
                "alertas",query(db,"SELECT * FROM pecas WHERE estoque<=estoque_minimo ORDER BY estoque,nome"));
            default -> throw new Rule(404,"Consulta não encontrada.");
        };
    }
    private static Object write(Connection db,String path,Map<String,String> f,Map<String,Object> user) throws Exception {
        long uid=number(user.get("id")); long result=0; boolean edit=f.containsKey("id")&&!f.get("id").isBlank(); long row=edit?id(f,"id"):0;
        switch(path) {
            case "clientes" -> {
                permit(user,"administrador","atendente"); String nome=text(f,"nome",120),doc=text(f,"documento",20),tel=text(f,"telefone",25),mail=email(f,"email",false);
                if(edit){one(db,"SELECT id FROM clientes WHERE id=?",row);update(db,"UPDATE clientes SET nome=?,documento=?,telefone=?,email=? WHERE id=?",nome,doc,tel,mail,row);result=row;}
                else result=insert(db,"INSERT INTO clientes(nome,documento,telefone,email) VALUES(?,?,?,?)",nome,doc,tel,mail);
            }
            case "veiculos" -> {
                permit(user,"administrador","atendente"); long client=id(f,"cliente_id"); String plate=text(f,"placa",10).toUpperCase(Locale.ROOT).replace("-","").replace(" ","");
                if(!plate.matches("[A-Z]{3}[0-9][A-Z0-9][0-9]{2}")) throw new Rule(400,"Placa inválida. Exemplo: ABC1D23 ou ABC1234.");
                String brand=text(f,"marca",50),model=text(f,"modelo",60);int year=integer(f,"ano",1900,2100),km=integer(f,"km",0,Integer.MAX_VALUE);
                one(db,"SELECT id FROM clientes WHERE id=?",client);
                if(edit){one(db,"SELECT id FROM veiculos WHERE id=? FOR UPDATE",row);var old=one(db,"SELECT cliente_id FROM veiculos WHERE id=?",row);if(number(old.get("cliente_id"))!=client&&!query(db,"SELECT id FROM ordens WHERE veiculo_id=?",row).isEmpty())throw new Rule(409,"Veículo com histórico não pode trocar de cliente.");update(db,"UPDATE veiculos SET cliente_id=?,placa=?,marca=?,modelo=?,ano=?,km=? WHERE id=?",client,plate,brand,model,year,km,row);result=row;}
                else result=insert(db,"INSERT INTO veiculos(cliente_id,placa,marca,modelo,ano,km) VALUES(?,?,?,?,?,?)",client,plate,brand,model,year,km);
            }
            case "servicos" -> {
                permit(user,"administrador");String nome=text(f,"nome",100),desc=text(f,"descricao",255);var price=money(f,"preco");
                if(edit){one(db,"SELECT id FROM servicos WHERE id=?",row);update(db,"UPDATE servicos SET nome=?,descricao=?,preco=? WHERE id=?",nome,desc,price,row);result=row;}
                else result=insert(db,"INSERT INTO servicos(nome,descricao,preco) VALUES(?,?,?)",nome,desc,price);
            }
            case "pecas" -> {
                permit(user,"administrador");String code=text(f,"codigo",30).toUpperCase(Locale.ROOT),nome=text(f,"nome",100);var price=money(f,"preco");int min=integer(f,"estoque_minimo",0,1000000);
                if(edit){one(db,"SELECT id FROM pecas WHERE id=?",row);update(db,"UPDATE pecas SET codigo=?,nome=?,preco=?,estoque_minimo=? WHERE id=?",code,nome,price,min,row);result=row;}
                else {int stock=integer(f,"estoque",0,1000000);result=insert(db,"INSERT INTO pecas(codigo,nome,preco,estoque,estoque_minimo) VALUES(?,?,?,?,?)",code,nome,price,stock,min);if(stock>0)movement(db,result,null,uid,"entrada",stock,stock,"Saldo inicial do cadastro");}
            }
            case "usuarios" -> {
                permit(user,"administrador");String nome=text(f,"nome",100),mail=email(f,"email",true),role=text(f,"perfil",20);
                if(!Set.of("administrador","atendente","mecanico").contains(role)) throw new Rule(400,"Perfil inválido.");
                // Serializa mudanças de administradores para preservar ao menos um administrador ativo.
                query(db,"SELECT id FROM usuarios WHERE perfil='administrador' FOR UPDATE");
                if(edit){var target=one(db,"SELECT * FROM usuarios WHERE id=?",row);int active=integer(f,"ativo",0,1);if(row==uid&&(active==0||!role.equals("administrador")))throw new Rule(409,"Você não pode desativar ou rebaixar sua própria conta.");
                    if(target.get("perfil").equals("administrador")&&(active==0||!role.equals("administrador"))&&number(one(db,"SELECT COUNT(*) n FROM usuarios WHERE perfil='administrador' AND ativo=TRUE").get("n"))<=1)throw new Rule(409,"Mantenha um administrador ativo.");
                    update(db,"UPDATE usuarios SET nome=?,email=?,perfil=?,ativo=? WHERE id=?",nome,mail,role,active,row);result=row;
                    String pass=optional(f,"senha",128);if(!pass.isEmpty()){if(pass.length()<8)throw new Rule(400,"Senha deve ter ao menos 8 caracteres.");update(db,"UPDATE usuarios SET senha_hash=? WHERE id=?",hash(pass),row);SESSIONS.values().removeIf(s->s.userId==row);}
                }else{String pass=text(f,"senha",128);if(pass.length()<8)throw new Rule(400,"Senha deve ter ao menos 8 caracteres.");result=insert(db,"INSERT INTO usuarios(nome,email,senha_hash,perfil) VALUES(?,?,?,?)",nome,mail,hash(pass),role);}
            }
            case "ordens" -> {
                permit(user,"administrador","atendente");long vehicle=id(f,"veiculo_id"),responsible=id(f,"responsavel_id");
                one(db,"SELECT id FROM veiculos WHERE id=? FOR UPDATE",vehicle);one(db,"SELECT id FROM usuarios WHERE id=? AND ativo=TRUE",responsible);
                if(!query(db,"SELECT id FROM ordens WHERE veiculo_id=? AND status NOT IN ('entregue','cancelada')",vehicle).isEmpty())throw new Rule(409,"Este veículo já possui uma ordem em atendimento.");
                result=insert(db,"INSERT INTO ordens(veiculo_id,responsavel_id,relato) VALUES(?,?,?)",vehicle,responsible,text(f,"relato",1000));history(db,result,uid,"Ordem aberta");
            }
            case "ordem/dados" -> {
                permit(user,"administrador","atendente");
                long order=id(f,"ordem_id"),responsible=id(f,"responsavel_id");
                var current=one(db,"SELECT * FROM ordens WHERE id=? FOR UPDATE",order);
                if(CLOSED.contains(current.get("status")))throw new Rule(409,"Esta ordem está fechada para alterações.");
                var responsibleUser=one(db,"SELECT nome FROM usuarios WHERE id=? AND ativo=TRUE",responsible);
                String report=text(f,"relato",1000);
                update(db,"UPDATE ordens SET responsavel_id=?,relato=?,atualizada_em=CURRENT_TIMESTAMP WHERE id=?",responsible,report,order);
                history(db,order,uid,"Dados do atendimento atualizados. Responsável: "+responsibleUser.get("nome"));
                result=order;
            }
            case "ordem/status" -> {
                permit(user,"administrador","atendente","mecanico");long order=id(f,"ordem_id");var o=one(db,"SELECT * FROM ordens WHERE id=? FOR UPDATE",order);String before=o.get("status").toString(),next=text(f,"status",30);
                if(!TRANSITIONS.get(before).contains(next))throw new Rule(409,"Mudança de status não permitida.");
                if(next.equals("entregue"))permit(user,"administrador","atendente");
                if(next.equals("concluida")&&query(db,"SELECT id FROM ordem_servicos WHERE ordem_id=?",order).isEmpty())throw new Rule(409,"Registre ao menos um serviço antes de concluir.");
                if(next.equals("cancelada")) {
                    permit(user,"administrador","atendente");
                    for(var item:query(db,"SELECT * FROM ordem_pecas WHERE ordem_id=? ORDER BY peca_id",order)) {
                        long part=number(item.get("peca_id"));int qty=((Number)item.get("quantidade")).intValue();var p=one(db,"SELECT * FROM pecas WHERE id=? FOR UPDATE",part);int stock=((Number)p.get("estoque")).intValue()+qty;
                        update(db,"UPDATE pecas SET estoque=? WHERE id=?",stock,part);movement(db,part,order,uid,"devolucao",qty,stock,"Cancelamento da OS #"+order);
                    }
                }
                update(db,"UPDATE ordens SET status=?,atualizada_em=CURRENT_TIMESTAMP WHERE id=?",next,order);history(db,order,uid,"Status: "+before+" para "+next);result=order;
            }
            case "ordem/servico", "ordem/peca" -> {
                permit(user,"administrador","mecanico");long order=id(f,"ordem_id");var o=one(db,"SELECT * FROM ordens WHERE id=? FOR UPDATE",order);if(CLOSED.contains(o.get("status")))throw new Rule(409,"Esta ordem está fechada para alterações.");
                int qty=integer(f,"quantidade",1,10000);boolean part=path.endsWith("peca");long item=id(f,part?"peca_id":"servico_id");var data=one(db,"SELECT * FROM "+(part?"pecas":"servicos")+" WHERE id=? FOR UPDATE",item);
                if(part) {int stock=((Number)data.get("estoque")).intValue();if(stock<qty)throw new Rule(409,"Estoque insuficiente. Disponível: "+stock+".");stock-=qty;update(db,"UPDATE pecas SET estoque=? WHERE id=?",stock,item);movement(db,item,order,uid,"saida",qty,stock,"Consumo na OS #"+order);}
                result=insert(db,"INSERT INTO "+(part?"ordem_pecas(ordem_id,peca_id":"ordem_servicos(ordem_id,servico_id")+",quantidade,preco_unitario) VALUES(?,?,?,?)",order,item,qty,data.get("preco"));
                history(db,order,uid,(part?"Peça":"Serviço")+" adicionado: "+data.get("nome")+" ("+qty+")");update(db,"UPDATE ordens SET atualizada_em=CURRENT_TIMESTAMP WHERE id=?",order);
            }
            case "ordem/remover-servico", "ordem/remover-peca" -> {
                permit(user,"administrador","mecanico");boolean part=path.endsWith("peca");long order=id(f,"ordem_id"),item=id(f,"item_id");var o=one(db,"SELECT * FROM ordens WHERE id=? FOR UPDATE",order);if(CLOSED.contains(o.get("status")))throw new Rule(409,"Esta ordem está fechada para alterações.");
                String table=part?"ordem_pecas":"ordem_servicos";var data=one(db,"SELECT * FROM "+table+" WHERE id=? AND ordem_id=?",item,order);
                if(part) {long p=number(data.get("peca_id"));int qty=((Number)data.get("quantidade")).intValue();var stockRow=one(db,"SELECT estoque FROM pecas WHERE id=? FOR UPDATE",p);int stock=((Number)stockRow.get("estoque")).intValue()+qty;update(db,"UPDATE pecas SET estoque=? WHERE id=?",stock,p);movement(db,p,order,uid,"devolucao",qty,stock,"Remoção de item da OS #"+order);}
                update(db,"DELETE FROM "+table+" WHERE id=?",item);history(db,order,uid,(part?"Peça":"Serviço")+" removido da ordem, item #"+item);result=order;
            }
            case "estoque/movimentar" -> {
                permit(user,"administrador");long part=id(f,"peca_id");int qty=integer(f,"quantidade",1,1000000);String type=text(f,"tipo",20),reason=text(f,"motivo",255);if(!Set.of("entrada","saida").contains(type))throw new Rule(400,"Tipo de movimentação inválido.");
                var p=one(db,"SELECT * FROM pecas WHERE id=? FOR UPDATE",part);int stock=((Number)p.get("estoque")).intValue();if(type.equals("saida")&&qty>stock)throw new Rule(409,"Estoque insuficiente.");stock+=type.equals("entrada")?qty:-qty;if(stock>100000000)throw new Rule(400,"Saldo acima do limite.");update(db,"UPDATE pecas SET estoque=? WHERE id=?",stock,part);movement(db,part,null,uid,type,qty,stock,reason);result=part;
            }
            default -> throw new Rule(404,"Operação não encontrada.");
        }
        audit(db,uid,path+(edit?" alterado #":" registrado #")+result);return Map.of("ok",true,"id",result);
    }
    private static void headers(HttpExchange x) {
        x.getResponseHeaders().set("X-Content-Type-Options","nosniff");x.getResponseHeaders().set("X-Frame-Options","DENY");x.getResponseHeaders().set("Referrer-Policy","same-origin");
        x.getResponseHeaders().set("Content-Security-Policy","default-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
    }
    private static void reply(HttpExchange x,int code,Object data) throws IOException {
        headers(x);x.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");x.getResponseHeaders().set("Cache-Control","no-store");byte[] b=Json.stringify(data).getBytes(StandardCharsets.UTF_8);x.sendResponseHeaders(code,b.length);try(var out=x.getResponseBody()){out.write(b);}
    }
    private static void staticFile(HttpExchange x) throws IOException {
        headers(x); if(!x.getRequestMethod().equals("GET")){x.sendResponseHeaders(405,-1);x.close();return;}
        var base=ROOT.resolve("web").normalize();String request=x.getRequestURI().getPath();Path p=base.resolve(request.equals("/")?"index.html":request.substring(1)).normalize();
        if(!p.startsWith(base)||!Files.isRegularFile(p)){x.sendResponseHeaders(404,-1);x.close();return;}
        String name=p.toString();String type=name.endsWith(".html")?"text/html":name.endsWith(".css")?"text/css":name.endsWith(".js")?"application/javascript":name.endsWith(".svg")?"image/svg+xml":"application/octet-stream";
        x.getResponseHeaders().set("Content-Type",type+"; charset=utf-8");x.getResponseHeaders().set("Cache-Control","no-cache");byte[] b=Files.readAllBytes(p);x.sendResponseHeaders(200,b.length);try(var out=x.getResponseBody()){out.write(b);}
    }
}
