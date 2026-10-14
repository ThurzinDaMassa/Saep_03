package oficina.service;

import static oficina.dao.JdbcDao.insert;
import static oficina.dao.JdbcDao.number;
import static oficina.dao.JdbcDao.one;
import static oficina.dao.JdbcDao.query;
import static oficina.dao.JdbcDao.update;
import static oficina.dao.RegistroDao.movement;
import static oficina.security.SenhaService.hash;
import static oficina.validation.Validacao.email;
import static oficina.validation.Validacao.id;
import static oficina.validation.Validacao.integer;
import static oficina.validation.Validacao.optional;
import static oficina.validation.Validacao.permit;
import static oficina.validation.Validacao.text;

import oficina.exception.RegraNegocioException;
import oficina.model.Cliente;
import oficina.model.Peca;
import oficina.model.Servico;
import oficina.model.Usuario;
import oficina.model.Veiculo;
import oficina.security.SessaoService;

import java.sql.Connection;
import java.util.Map;
import java.util.Set;

/** Regras para clientes, veículos, serviços, peças e usuários. */
public final class CadastroService {

    private final SessaoService sessoes;

    public CadastroService(SessaoService sessoes) {
        this.sessoes = sessoes;
    }

    public long salvar(Connection db, String path, Map<String, String> f, Usuario user)
            throws Exception {
        long uid = user.id();
        long result = 0;
        boolean edit = f.containsKey("id") && !f.get("id").isBlank();
        long row = edit ? id(f, "id") : 0;
        switch (path) {
            case "clientes" -> {
                permit(user, "administrador", "atendente");
                Cliente cliente = Cliente.fromForm(f);
                String nome = cliente.nome(),
                        doc = cliente.documento(),
                        tel = cliente.telefone(),
                        mail = cliente.email();
                if (edit) {
                    one(db, "SELECT id FROM clientes WHERE id=?", row);
                    update(
                            db,
                            "UPDATE clientes SET nome=?,documento=?,telefone=?,email=? WHERE id=?",
                            nome,
                            doc,
                            tel,
                            mail,
                            row);
                    result = row;
                } else
                    result =
                            insert(
                                    db,
                                    "INSERT INTO clientes(nome,documento,telefone,email)"
                                            + " VALUES(?,?,?,?)",
                                    nome,
                                    doc,
                                    tel,
                                    mail);
            }
            case "veiculos" -> {
                permit(user, "administrador", "atendente");
                Veiculo veiculo = Veiculo.fromForm(f);
                long client = veiculo.clienteId();
                String plate = veiculo.placa(), brand = veiculo.marca(), model = veiculo.modelo();
                int year = veiculo.ano(), km = veiculo.km();
                one(db, "SELECT id FROM clientes WHERE id=?", client);
                if (edit) {
                    one(db, "SELECT id FROM veiculos WHERE id=? FOR UPDATE", row);
                    var old = one(db, "SELECT cliente_id FROM veiculos WHERE id=?", row);
                    if (number(old.get("cliente_id")) != client
                            && !query(db, "SELECT id FROM ordens WHERE veiculo_id=?", row)
                                    .isEmpty())
                        throw new RegraNegocioException(
                                409, "Veículo com histórico não pode trocar de cliente.");
                    update(
                            db,
                            "UPDATE veiculos SET cliente_id=?,placa=?,marca=?,modelo=?,ano=?,km=?"
                                    + " WHERE id=?",
                            client,
                            plate,
                            brand,
                            model,
                            year,
                            km,
                            row);
                    result = row;
                } else
                    result =
                            insert(
                                    db,
                                    "INSERT INTO veiculos(cliente_id,placa,marca,modelo,ano,km)"
                                            + " VALUES(?,?,?,?,?,?)",
                                    client,
                                    plate,
                                    brand,
                                    model,
                                    year,
                                    km);
            }
            case "servicos" -> {
                permit(user, "administrador");
                Servico servico = Servico.fromForm(f);
                String nome = servico.nome(), desc = servico.descricao();
                var price = servico.preco();
                if (edit) {
                    one(db, "SELECT id FROM servicos WHERE id=?", row);
                    update(
                            db,
                            "UPDATE servicos SET nome=?,descricao=?,preco=? WHERE id=?",
                            nome,
                            desc,
                            price,
                            row);
                    result = row;
                } else
                    result =
                            insert(
                                    db,
                                    "INSERT INTO servicos(nome,descricao,preco) VALUES(?,?,?)",
                                    nome,
                                    desc,
                                    price);
            }
            case "pecas" -> {
                permit(user, "administrador");
                Peca peca = Peca.fromForm(f);
                String code = peca.codigo(), nome = peca.nome();
                var price = peca.preco();
                int min = peca.estoqueMinimo();
                if (edit) {
                    one(db, "SELECT id FROM pecas WHERE id=?", row);
                    update(
                            db,
                            "UPDATE pecas SET codigo=?,nome=?,preco=?,estoque_minimo=? WHERE id=?",
                            code,
                            nome,
                            price,
                            min,
                            row);
                    result = row;
                } else {
                    int stock = integer(f, "estoque", 0, 1000000);
                    result =
                            insert(
                                    db,
                                    "INSERT INTO pecas(codigo,nome,preco,estoque,estoque_minimo)"
                                            + " VALUES(?,?,?,?,?)",
                                    code,
                                    nome,
                                    price,
                                    stock,
                                    min);
                    if (stock > 0)
                        movement(
                                db,
                                result,
                                null,
                                uid,
                                "entrada",
                                stock,
                                stock,
                                "Saldo inicial do cadastro");
                }
            }
            case "usuarios" -> {
                permit(user, "administrador");
                String nome = text(f, "nome", 100),
                        mail = email(f, "email", true),
                        role = text(f, "perfil", 20);
                if (!Set.of("administrador", "atendente", "mecanico").contains(role))
                    throw new RegraNegocioException(400, "Perfil inválido.");
                // Serializa mudanças de administradores para preservar ao menos um administrador
                // ativo.
                query(db, "SELECT id FROM usuarios WHERE perfil='administrador' FOR UPDATE");
                if (edit) {
                    var target = one(db, "SELECT * FROM usuarios WHERE id=?", row);
                    int active = integer(f, "ativo", 0, 1);
                    if (row == uid && (active == 0 || !role.equals("administrador")))
                        throw new RegraNegocioException(
                                409, "Você não pode desativar ou rebaixar sua própria conta.");
                    if (target.get("perfil").equals("administrador")
                            && (active == 0 || !role.equals("administrador"))
                            && number(
                                            one(
                                                            db,
                                                            "SELECT COUNT(*) n FROM usuarios WHERE"
                                                                    + " perfil='administrador' AND"
                                                                    + " ativo=TRUE")
                                                    .get("n"))
                                    <= 1)
                        throw new RegraNegocioException(409, "Mantenha um administrador ativo.");
                    update(
                            db,
                            "UPDATE usuarios SET nome=?,email=?,perfil=?,ativo=? WHERE id=?",
                            nome,
                            mail,
                            role,
                            active,
                            row);
                    result = row;
                    String pass = optional(f, "senha", 128);
                    if (!pass.isEmpty()) {
                        if (pass.length() < 8)
                            throw new RegraNegocioException(
                                    400, "Senha deve ter ao menos 8 caracteres.");
                        update(db, "UPDATE usuarios SET senha_hash=? WHERE id=?", hash(pass), row);
                        sessoes.invalidarUsuario(row);
                    }
                } else {
                    String pass = text(f, "senha", 128);
                    if (pass.length() < 8)
                        throw new RegraNegocioException(
                                400, "Senha deve ter ao menos 8 caracteres.");
                    result =
                            insert(
                                    db,
                                    "INSERT INTO usuarios(nome,email,senha_hash,perfil)"
                                            + " VALUES(?,?,?,?)",
                                    nome,
                                    mail,
                                    hash(pass),
                                    role);
                }
            }
            default -> throw new RegraNegocioException(404, "Cadastro não encontrado.");
        }
        return result;
    }
}
