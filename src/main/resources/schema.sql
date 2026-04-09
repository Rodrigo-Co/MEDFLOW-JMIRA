-- ================================================================
--  MedFlow – Schema do Banco de Dados
--  Execute este script no Supabase SQL Editor (uma vez)
--  Supabase Dashboard → SQL Editor → New Query → Cole e execute
-- ================================================================

-- Extensão para UUID (opcional, usamos VARCHAR para compatibilidade com o frontend)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ---------------------------------------------------------------
--  USUÁRIOS (admins, médicos e pacientes em uma tabela)
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            VARCHAR(50)  PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) UNIQUE NOT NULL,
    phone         VARCHAR(30),
    cpf           VARCHAR(20)  UNIQUE,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('admin', 'doctor', 'patient')),
    password_hash VARCHAR(255) NOT NULL DEFAULT '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    -- admin
    title         VARCHAR(150),
    -- doctor
    specialty     VARCHAR(150),
    crm           VARCHAR(50),
    username      VARCHAR(100) UNIQUE,
    attendances   INTEGER DEFAULT 0,
    surgeries     INTEGER DEFAULT 0,
    exams         INTEGER DEFAULT 0,
    created_at    TIMESTAMPTZ DEFAULT NOW()
);

-- ---------------------------------------------------------------
--  HISTÓRICO DE PRESSÃO ARTERIAL
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS blood_pressure_history (
    id          SERIAL PRIMARY KEY,
    patient_id  VARCHAR(50) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date_label  VARCHAR(20) NOT NULL,
    systolic    INTEGER NOT NULL,
    diastolic   INTEGER NOT NULL
);

-- ---------------------------------------------------------------
--  HISTÓRICO DE FREQUÊNCIA CARDÍACA
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS heart_rate_history (
    id          SERIAL PRIMARY KEY,
    patient_id  VARCHAR(50) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date_label  VARCHAR(20) NOT NULL,
    rate        INTEGER NOT NULL
);

-- ---------------------------------------------------------------
--  MEDICAÇÕES DO PACIENTE
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS patient_medications (
    id          SERIAL PRIMARY KEY,
    patient_id  VARCHAR(50) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    medication  VARCHAR(255) NOT NULL
);

-- ---------------------------------------------------------------
--  CONDIÇÕES DO PACIENTE
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS patient_conditions (
    id             SERIAL PRIMARY KEY,
    patient_id     VARCHAR(50) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    condition_name VARCHAR(255) NOT NULL
);

-- ---------------------------------------------------------------
--  PRONTUÁRIOS MÉDICOS
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS medical_records (
    id              VARCHAR(50) PRIMARY KEY,
    patient_id      VARCHAR(50) REFERENCES users(id),
    patient_name    VARCHAR(255),
    doctor_id       VARCHAR(50) REFERENCES users(id),
    doctor_name     VARCHAR(255),
    record_date     DATE NOT NULL DEFAULT CURRENT_DATE,
    type            VARCHAR(20) NOT NULL CHECK (type IN ('consulta', 'cirurgia', 'exame')),
    raw_notes       TEXT,
    formatted_notes TEXT,
    diagnosis       VARCHAR(500),
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ---------------------------------------------------------------
--  HISTÓRICO DE EDIÇÕES DE PRONTUÁRIO
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS record_edit_history (
    id              VARCHAR(50) PRIMARY KEY,
    record_id       VARCHAR(50) NOT NULL REFERENCES medical_records(id) ON DELETE CASCADE,
    edited_by       VARCHAR(50),
    edited_by_name  VARCHAR(255),
    edit_timestamp  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    changes         TEXT
);

-- ---------------------------------------------------------------
--  CHAMADOS / TICKETS
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tickets (
    id           VARCHAR(50) PRIMARY KEY,
    patient_id   VARCHAR(50) REFERENCES users(id),
    patient_name VARCHAR(255),
    type         VARCHAR(20) NOT NULL CHECK (type IN ('consulta', 'exame')),
    specialty    VARCHAR(150),
    description  TEXT,
    status       VARCHAR(20) NOT NULL DEFAULT 'aberto'
                 CHECK (status IN ('aberto', 'em_andamento', 'concluido')),
    doctor_id    VARCHAR(50),
    doctor_name  VARCHAR(255),
    result       TEXT,
    created_at   DATE DEFAULT CURRENT_DATE,
    updated_at   DATE DEFAULT CURRENT_DATE
);

-- ---------------------------------------------------------------
--  SOLICITAÇÕES DE ALTERAÇÃO DE DADOS
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS data_change_requests (
    id               VARCHAR(50) PRIMARY KEY,
    requester_id     VARCHAR(50) REFERENCES users(id),
    requester_name   VARCHAR(255),
    requester_role   VARCHAR(20),
    field_name       VARCHAR(100),
    old_value        TEXT,
    new_value        TEXT,
    status           VARCHAR(20) DEFAULT 'pendente'
                     CHECK (status IN ('pendente', 'aprovado', 'rejeitado')),
    created_at       TIMESTAMPTZ DEFAULT NOW(),
    resolved_at      TIMESTAMPTZ,
    resolved_by      VARCHAR(50),
    resolved_by_name VARCHAR(255)
);

-- ---------------------------------------------------------------
--  LOG DE AUDITORIA
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_log (
    id        VARCHAR(50) PRIMARY KEY,
    action    VARCHAR(100) NOT NULL,
    severity  VARCHAR(20)  NOT NULL CHECK (severity IN ('info', 'warning', 'error')),
    details   TEXT,
    user_id   VARCHAR(50),
    user_name VARCHAR(255),
    user_role VARCHAR(20),
    log_timestamp TIMESTAMPTZ DEFAULT NOW()
);

-- ================================================================
--  DADOS INICIAIS (mesmos do mock do frontend)
--  Senha padrão para todos: medflow123
-- ================================================================

-- Admins
INSERT INTO users (id, name, email, phone, cpf, role, title, username) VALUES
('a1','Dr. Fernando Costa','fernando@neocore.com','(11) 99900-0001','000.000.000-01','admin','Diretor do Hospital','fernando.costa'),
('a2','Dra. Lucia Martins','lucia@neocore.com','(11) 99900-0002','000.000.000-02','admin','Diretora de Análises Clínicas','lucia.martins'),
('a3','Roberto Almeida','roberto@neocore.com','(11) 99900-0003','000.000.000-03','admin','Diretor Administrativo','roberto.almeida')
ON CONFLICT (id) DO NOTHING;

-- Médicos
INSERT INTO users (id, name, email, phone, cpf, role, specialty, crm, username, attendances, surgeries, exams) VALUES
('d1','Dr. Ricardo Mendes','ricardo@cardio.com','(11) 99800-1001','111.222.333-44','doctor','Cardiologia','CRM/SP 123456','ricardo.mendes',342,28,156),
('d2','Dra. Ana Beatriz Lima','ana@cardio.com','(11) 99800-1002','222.333.444-55','doctor','Cirurgia Cardiovascular','CRM/SP 654321','ana.lima',218,67,98),
('d3','Dr. Carlos Eduardo Silva','carlos@cardio.com','(11) 99800-1003','333.444.555-66','doctor','Hemodinâmica','CRM/SP 789012','carlos.silva',189,12,234)
ON CONFLICT (id) DO NOTHING;

-- Pacientes
INSERT INTO users (id, name, email, phone, cpf, role) VALUES
('p1','Maria da Silva','maria@email.com','(11) 99700-2001','444.555.666-77','patient'),
('p2','Joao Carlos Pereira','joao@email.com','(11) 99700-2002','555.666.777-88','patient'),
('p3','Ana Paula Rodrigues','ana.paula@email.com','(11) 99700-2003','666.777.888-99','patient')
ON CONFLICT (id) DO NOTHING;

-- Histórico de pressão – Maria
INSERT INTO blood_pressure_history (patient_id, date_label, systolic, diastolic) VALUES
('p1','Jan',150,95),('p1','Fev',145,92),('p1','Mar',140,88),
('p1','Abr',135,85),('p1','Mai',130,82),('p1','Jun',125,80)
ON CONFLICT DO NOTHING;

-- Histórico de pressão – João
INSERT INTO blood_pressure_history (patient_id, date_label, systolic, diastolic) VALUES
('p2','Jan',160,100),('p2','Fev',155,96),('p2','Mar',148,92),
('p2','Abr',142,88),('p2','Mai',138,86),('p2','Jun',132,84)
ON CONFLICT DO NOTHING;

-- Histórico de pressão – Ana Paula
INSERT INTO blood_pressure_history (patient_id, date_label, systolic, diastolic) VALUES
('p3','Jan',138,88),('p3','Fev',135,86),('p3','Mar',130,84),
('p3','Abr',128,82),('p3','Mai',125,80),('p3','Jun',122,78)
ON CONFLICT DO NOTHING;

-- Freq. cardíaca – Maria
INSERT INTO heart_rate_history (patient_id, date_label, rate) VALUES
('p1','Jan',88),('p1','Fev',85),('p1','Mar',82),
('p1','Abr',78),('p1','Mai',75),('p1','Jun',72)
ON CONFLICT DO NOTHING;

-- Freq. cardíaca – João
INSERT INTO heart_rate_history (patient_id, date_label, rate) VALUES
('p2','Jan',95),('p2','Fev',90),('p2','Mar',86),
('p2','Abr',82),('p2','Mai',78),('p2','Jun',76)
ON CONFLICT DO NOTHING;

-- Freq. cardíaca – Ana Paula
INSERT INTO heart_rate_history (patient_id, date_label, rate) VALUES
('p3','Jan',82),('p3','Fev',80),('p3','Mar',78),
('p3','Abr',76),('p3','Mai',74),('p3','Jun',72)
ON CONFLICT DO NOTHING;

-- Medicações
INSERT INTO patient_medications (patient_id, medication) VALUES
('p1','Losartana 50mg'),('p1','Atenolol 25mg'),('p1','AAS 100mg'),
('p2','Enalapril 10mg'),('p2','Sinvastatina 20mg'),
('p3','Captopril 25mg')
ON CONFLICT DO NOTHING;

-- Condições
INSERT INTO patient_conditions (patient_id, condition_name) VALUES
('p1','Hipertensão Arterial'),('p1','Arritmia Cardíaca'),
('p2','Insuficiência Cardíaca'),('p2','Colesterol Alto'),
('p3','Pré-hipertensão')
ON CONFLICT DO NOTHING;

-- Prontuários
INSERT INTO medical_records (id, patient_id, patient_name, doctor_id, doctor_name, record_date, type, raw_notes, formatted_notes, diagnosis) VALUES
('r1','p1','Maria da Silva','d1','Dr. Ricardo Mendes','2026-03-20','consulta',
 'paciente relata dor no peito ao esforço físico, pressão melhorou desde última consulta, manter medicação atual',
 'Paciente relata dor torácica ao esforço físico. Pressão arterial apresentou melhora significativa desde a última consulta. Conduta: manter medicação atual e agendar eletrocardiograma de controle.',
 'Angina estável em acompanhamento'),
('r2','p2','Joao Carlos Pereira','d1','Dr. Ricardo Mendes','2026-03-18','consulta',
 'paciente apresenta melhora no quadro de insuficiência cardíaca, ecocardiograma mostrou fração de ejeção de 45%',
 'Paciente apresenta melhora no quadro de insuficiência cardíaca congestiva. Ecocardiograma realizado demonstrou fração de ejeção de 45%. Conduta: manter esquema terapêutico vigente e reavaliar em 30 dias.',
 'Insuficiência cardíaca em melhora progressiva'),
('r3','p3','Ana Paula Rodrigues','d2','Dra. Ana Beatriz Lima','2026-03-15','cirurgia',
 'avaliação pré-operatória para angioplastia, exames dentro da normalidade, paciente apta para procedimento',
 'Avaliação pré-operatória para angioplastia coronariana. Exames laboratoriais e de imagem dentro dos parâmetros de normalidade. Paciente considerada apta para o procedimento cirúrgico.',
 'Pré-operatório de angioplastia - apta')
ON CONFLICT (id) DO NOTHING;

-- Tickets
INSERT INTO tickets (id, patient_id, patient_name, type, specialty, description, status, doctor_id, doctor_name, result, created_at, updated_at) VALUES
('t1','p1','Maria da Silva','consulta','Cardiologia','Sentindo dor no peito e falta de ar','em_andamento','d1','Dr. Ricardo Mendes',NULL,'2026-03-19','2026-03-20'),
('t2','p1','Maria da Silva','exame','Cardiologia','Eletrocardiograma de controle','concluido','d1','Dr. Ricardo Mendes','ECG sem alterações significativas','2026-03-10','2026-03-15'),
('t3','p2','Joao Carlos Pereira','exame','Cardiologia','Ecocardiograma de acompanhamento','aberto',NULL,NULL,NULL,'2026-03-21','2026-03-21'),
('t4','p3','Ana Paula Rodrigues','consulta','Cirurgia Cardiovascular','Retorno pós-operatório angioplastia','aberto',NULL,NULL,NULL,'2026-03-22','2026-03-22'),
('t5','p2','Joao Carlos Pereira','consulta','Cardiologia','Consulta de rotina para acompanhamento','em_andamento','d1','Dr. Ricardo Mendes',NULL,'2026-03-17','2026-03-18')
ON CONFLICT (id) DO NOTHING;

-- Solicitação de alteração
INSERT INTO data_change_requests (id, requester_id, requester_name, requester_role, field_name, old_value, new_value, status, created_at) VALUES
('dcr1','d3','Dr. Carlos Eduardo Silva','doctor','email','carlos@cardio.com','carlos.silva@neocore.com','pendente','2026-03-28T10:00:00Z')
ON CONFLICT (id) DO NOTHING;

-- Auditoria inicial
INSERT INTO audit_log (id, action, severity, details, user_id, user_name, user_role, log_timestamp) VALUES
('al1','LOGIN','info','Login realizado no sistema','d1','Dr. Ricardo Mendes','doctor','2026-03-28T09:00:00Z'),
('al2','RECORD_CREATE','info','Prontuário criado para Maria da Silva','d1','Dr. Ricardo Mendes','doctor','2026-03-28T09:15:00Z'),
('al3','REQUEST_CREATE','warning','Solicitação de alteração de email enviada','d3','Dr. Carlos Eduardo Silva','doctor','2026-03-28T10:00:00Z'),
('al4','LOGIN','info','Login realizado no sistema','a1','Dr. Fernando Costa','admin','2026-03-29T08:30:00Z')
ON CONFLICT (id) DO NOTHING;
