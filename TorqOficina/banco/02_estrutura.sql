-- Execute no banco escolhido. Estrutura compatível com MySQL 8.
CREATE TABLE IF NOT EXISTS usuarios (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 nome VARCHAR(100) NOT NULL,
 email VARCHAR(150) NOT NULL UNIQUE,
 senha_hash VARCHAR(255) NOT NULL,
 perfil ENUM('administrador','atendente','mecanico') NOT NULL,
 ativo BOOLEAN NOT NULL DEFAULT TRUE,
 criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS clientes (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 nome VARCHAR(120) NOT NULL,
 documento VARCHAR(20) NOT NULL UNIQUE,
 telefone VARCHAR(25) NOT NULL,
 email VARCHAR(150),
 criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS veiculos (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 cliente_id BIGINT NOT NULL,
 placa VARCHAR(7) NOT NULL UNIQUE,
 marca VARCHAR(50) NOT NULL,
 modelo VARCHAR(60) NOT NULL,
 ano INT NOT NULL,
 km INT NOT NULL DEFAULT 0,
 CONSTRAINT fk_veiculo_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
 CONSTRAINT ck_veiculo_ano CHECK (ano BETWEEN 1900 AND 2100),
 CONSTRAINT ck_veiculo_km CHECK (km >= 0)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS servicos (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 nome VARCHAR(100) NOT NULL,
 descricao VARCHAR(255) NOT NULL,
 preco DECIMAL(10,2) NOT NULL,
 CONSTRAINT ck_servico_preco CHECK (preco >= 0)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS pecas (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 codigo VARCHAR(30) NOT NULL UNIQUE,
 nome VARCHAR(100) NOT NULL,
 preco DECIMAL(10,2) NOT NULL,
 estoque INT NOT NULL DEFAULT 0,
 estoque_minimo INT NOT NULL DEFAULT 0,
 CONSTRAINT ck_peca_preco CHECK (preco >= 0),
 CONSTRAINT ck_peca_estoque CHECK (estoque >= 0),
 CONSTRAINT ck_peca_minimo CHECK (estoque_minimo >= 0)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS ordens (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 veiculo_id BIGINT NOT NULL,
 responsavel_id BIGINT NOT NULL,
 relato VARCHAR(1000) NOT NULL,
 status ENUM('aberta','em_execucao','aguardando_pecas','concluida','entregue','cancelada') NOT NULL DEFAULT 'aberta',
 aberta_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 atualizada_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_ordem_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos(id),
 CONSTRAINT fk_ordem_usuario FOREIGN KEY (responsavel_id) REFERENCES usuarios(id),
 INDEX idx_ordem_status (status), INDEX idx_ordem_veiculo (veiculo_id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS ordem_servicos (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 ordem_id BIGINT NOT NULL,
 servico_id BIGINT NOT NULL,
 quantidade INT NOT NULL,
 preco_unitario DECIMAL(10,2) NOT NULL,
 CONSTRAINT fk_os_ordem FOREIGN KEY (ordem_id) REFERENCES ordens(id),
 CONSTRAINT fk_os_servico FOREIGN KEY (servico_id) REFERENCES servicos(id),
 CONSTRAINT ck_os_qtd CHECK (quantidade > 0),
 CONSTRAINT ck_os_preco CHECK (preco_unitario >= 0)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS ordem_pecas (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 ordem_id BIGINT NOT NULL,
 peca_id BIGINT NOT NULL,
 quantidade INT NOT NULL,
 preco_unitario DECIMAL(10,2) NOT NULL,
 CONSTRAINT fk_op_ordem FOREIGN KEY (ordem_id) REFERENCES ordens(id),
 CONSTRAINT fk_op_peca FOREIGN KEY (peca_id) REFERENCES pecas(id),
 CONSTRAINT ck_op_qtd CHECK (quantidade > 0),
 CONSTRAINT ck_op_preco CHECK (preco_unitario >= 0)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS movimentacoes (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 peca_id BIGINT NOT NULL,
 ordem_id BIGINT NULL,
 usuario_id BIGINT NOT NULL,
 tipo ENUM('entrada','saida','devolucao') NOT NULL,
 quantidade INT NOT NULL,
 saldo_apos INT NOT NULL,
 motivo VARCHAR(255) NOT NULL,
 criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_mov_peca FOREIGN KEY (peca_id) REFERENCES pecas(id),
 CONSTRAINT fk_mov_ordem FOREIGN KEY (ordem_id) REFERENCES ordens(id),
 CONSTRAINT fk_mov_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
 CONSTRAINT ck_mov_qtd CHECK (quantidade > 0),
 CONSTRAINT ck_mov_saldo CHECK (saldo_apos >= 0)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS historico_ordens (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 ordem_id BIGINT NOT NULL,
 usuario_id BIGINT NOT NULL,
 evento VARCHAR(255) NOT NULL,
 criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_hist_ordem FOREIGN KEY (ordem_id) REFERENCES ordens(id),
 CONSTRAINT fk_hist_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS auditoria (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 usuario_id BIGINT NOT NULL,
 acao VARCHAR(255) NOT NULL,
 criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
) ENGINE=InnoDB;
