package oficina.database;

import static oficina.dao.JdbcDao.insert;
import static oficina.dao.RegistroDao.audit;
import static oficina.dao.RegistroDao.history;
import static oficina.dao.RegistroDao.movement;
import static oficina.security.SenhaService.hash;

import java.math.BigDecimal;
import java.sql.Connection;

/** Popula um banco vazio com contas e registros fictícios para apresentação. */
public final class DadosDemonstracao {
    public static void seed(Connection db) throws Exception {
        insert(
                db,
                "INSERT INTO usuarios(nome,email,senha_hash,perfil) VALUES(?,?,?,?)",
                "Administrador",
                "admin@torq.local",
                hash("Oficina@123"),
                "administrador");
        insert(
                db,
                "INSERT INTO usuarios(nome,email,senha_hash,perfil) VALUES(?,?,?,?)",
                "Atendimento",
                "atendimento@torq.local",
                hash("Atende@123"),
                "atendente");
        insert(
                db,
                "INSERT INTO usuarios(nome,email,senha_hash,perfil) VALUES(?,?,?,?)",
                "Mecânico",
                "mecanico@torq.local",
                hash("Mecanico@123"),
                "mecanico");
        insert(
                db,
                "INSERT INTO clientes(nome,documento,telefone,email) VALUES(?,?,?,?)",
                "Marina Oliveira",
                "DEMO-001",
                "(47) 99911-2020",
                "marina@example.com");
        insert(
                db,
                "INSERT INTO clientes(nome,documento,telefone,email) VALUES(?,?,?,?)",
                "Rafael Costa",
                "DEMO-002",
                "(47) 99922-3030",
                "rafael@example.com");
        insert(
                db,
                "INSERT INTO clientes(nome,documento,telefone,email) VALUES(?,?,?,?)",
                "Beatriz Souza",
                "DEMO-003",
                "(47) 99933-4040",
                "beatriz@example.com");
        insert(
                db,
                "INSERT INTO veiculos(cliente_id,placa,marca,modelo,ano,km) VALUES(?,?,?,?,?,?)",
                1,
                "ABC1D23",
                "Volkswagen",
                "Gol",
                2019,
                68400);
        insert(
                db,
                "INSERT INTO veiculos(cliente_id,placa,marca,modelo,ano,km) VALUES(?,?,?,?,?,?)",
                2,
                "XYZ2E45",
                "Chevrolet",
                "Onix",
                2021,
                42500);
        insert(
                db,
                "INSERT INTO veiculos(cliente_id,placa,marca,modelo,ano,km) VALUES(?,?,?,?,?,?)",
                3,
                "DEF3F67",
                "Fiat",
                "Argo",
                2020,
                57300);
        insert(
                db,
                "INSERT INTO servicos(nome,descricao,preco) VALUES(?,?,?)",
                "Troca de óleo",
                "Substituição de óleo e filtro",
                new BigDecimal("90.00"));
        insert(
                db,
                "INSERT INTO servicos(nome,descricao,preco) VALUES(?,?,?)",
                "Revisão de freios",
                "Inspeção e manutenção do conjunto",
                new BigDecimal("180.00"));
        insert(
                db,
                "INSERT INTO servicos(nome,descricao,preco) VALUES(?,?,?)",
                "Alinhamento",
                "Ajuste da geometria da suspensão",
                new BigDecimal("120.00"));
        String[] names = {
            "Óleo 5W30 1 litro", "Filtro de óleo", "Pastilhas de freio", "Filtro de ar"
        };
        String[] prices = {"42.90", "35.00", "165.00", "48.00"};
        int[] stocks = {20, 3, 8, 2};
        int[] mins = {5, 4, 3, 3};
        for (int i = 0; i < 4; i++) {
            long id =
                    insert(
                            db,
                            "INSERT INTO pecas(codigo,nome,preco,estoque,estoque_minimo)"
                                    + " VALUES(?,?,?,?,?)",
                            "PC-00" + (i + 1),
                            names[i],
                            new BigDecimal(prices[i]),
                            stocks[i],
                            mins[i]);
            movement(
                    db,
                    id,
                    null,
                    1,
                    "entrada",
                    stocks[i],
                    stocks[i],
                    "Saldo inicial de demonstração");
        }
        long o1 =
                insert(
                        db,
                        "INSERT INTO ordens(veiculo_id,responsavel_id,relato,status)"
                                + " VALUES(?,?,?,?)",
                        1,
                        3,
                        "Revisão preventiva e troca de óleo",
                        "em_execucao");
        history(db, o1, 1, "Ordem aberta");
        history(db, o1, 3, "Status alterado para em_execucao");
        insert(
                db,
                "INSERT INTO ordem_servicos(ordem_id,servico_id,quantidade,preco_unitario)"
                        + " VALUES(?,?,?,?)",
                o1,
                1,
                1,
                new BigDecimal("90.00"));
        history(db, o1, 3, "Serviço adicionado: Troca de óleo (1)");
        long o2 =
                insert(
                        db,
                        "INSERT INTO ordens(veiculo_id,responsavel_id,relato,status)"
                                + " VALUES(?,?,?,?)",
                        2,
                        3,
                        "Ruído ao frear. Aguardando inspeção.",
                        "aberta");
        history(db, o2, 1, "Ordem aberta");
        long o3 =
                insert(
                        db,
                        "INSERT INTO ordens(veiculo_id,responsavel_id,relato,status)"
                                + " VALUES(?,?,?,?)",
                        3,
                        3,
                        "Substituição do filtro de ar",
                        "aguardando_pecas");
        history(db, o3, 1, "Ordem aberta");
        history(db, o3, 3, "Status alterado para aguardando_pecas");
        audit(db, 1, "Carga inicial de dados fictícios");
    }
}
