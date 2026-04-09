# MedFlow Backend – Neocore Hospital

Backend Java com Spring Boot + Supabase (PostgreSQL).

---

## ✅ Pré-requisitos

- Java 21+
- Maven 3.9+
- Conta no [Supabase](https://supabase.com) (gratuito)

---

## 🚀 Setup em 3 passos

### 1. Configure o Supabase

**a) Crie o banco de dados:**

1. Acesse [supabase.com](https://supabase.com) → New Project
2. Anote a **senha do banco** definida na criação
3. Vá em **SQL Editor → New Query**
4. Cole e execute todo o conteúdo de `src/main/resources/schema.sql`

**b) Obtenha as credenciais:**

1. Vá em **Settings → Database**
2. Copie o **Host** (formato: `db.XXXXXXXX.supabase.co`)
3. A porta é sempre `5432` e o banco é `postgres`

---

### 2. Configure o arquivo de propriedades

Abra `src/main/resources/application.properties` e preencha:

```properties
supabase.url=db.XXXXXXXXXXXXXXXX.supabase.co   # ← Cole aqui o Host do Supabase
supabase.port=5432
supabase.db=postgres
supabase.username=postgres
supabase.password=SUA_SENHA_AQUI               # ← Senha definida na criação do projeto

jwt.secret=medflow-super-secret-key-troque-isso-em-producao-123456789

cors.allowed-origins=http://localhost:3000,http://localhost:5500,http://127.0.0.1:5500
```

---

### 3. Rode o servidor

```bash
cd medflow-backend
mvn spring-boot:run
```

O servidor sobe em `http://localhost:8080`

---

## 🔌 Endpoints da API

### Autenticação

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET`  | `/api/auth/patients` | Lista pacientes (para tela de seleção) |
| `GET`  | `/api/auth/staff?role=doctor` | Lista médicos/admins |
| `POST` | `/api/auth/patient/login` | Login do paciente por ID |
| `POST` | `/api/auth/staff/login` | Login de médico/admin por username |
| `GET`  | `/api/auth/me` | Retorna usuário autenticado |

**Exemplo de login de paciente:**
```json
POST /api/auth/patient/login
{ "patientId": "p1" }
```

**Exemplo de login de médico:**
```json
POST /api/auth/staff/login
{ "username": "ricardo.mendes", "role": "doctor" }
```

**Resposta:**
```json
{
  "token": "eyJhbGciOiJS...",
  "user": { "id": "d1", "name": "Dr. Ricardo Mendes", "role": "doctor", ... }
}
```

> Use o token em todas as requisições protegidas:
> `Authorization: Bearer <token>`

---

### Prontuários (médico/admin)

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET`  | `/api/records?q=busca` | Lista todos (com busca opcional) |
| `GET`  | `/api/records/my` | Prontuários do médico logado |
| `GET`  | `/api/records/patient/{id}` | Prontuários de um paciente |
| `GET`  | `/api/records/{id}` | Detalhe de um prontuário |
| `POST` | `/api/records` | Criar prontuário |
| `PUT`  | `/api/records/{id}` | Editar prontuário (dono) |
| `POST` | `/api/records/format` | Formatar notas brutas (IA) |

---

### Chamados / Tickets

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET`  | `/api/tickets?status=aberto` | Lista chamados (médico/admin) |
| `GET`  | `/api/tickets/my` | Chamados do paciente logado |
| `POST` | `/api/tickets` | Paciente cria chamado |
| `PATCH`| `/api/tickets/{id}/accept` | Médico aceita chamado |
| `PATCH`| `/api/tickets/{id}/complete` | Médico conclui chamado |

---

### Pacientes

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET`  | `/api/patients` | Lista pacientes |
| `GET`  | `/api/patients/{id}` | Detalhe do paciente |
| `POST` | `/api/patients` | Cadastrar paciente (médico) |
| `PUT`  | `/api/patients/{id}/profile` | Atualizar perfil (próprio paciente) |

---

### Admin

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET`  | `/api/admin/dashboard` | Estatísticas gerais |
| `GET`  | `/api/admin/doctors` | Lista médicos |
| `POST` | `/api/admin/doctors` | Cadastrar médico |
| `PUT`  | `/api/admin/doctors/{id}` | Editar médico |
| `GET`  | `/api/admin/requests?status=pendente` | Solicitações de alteração |
| `PATCH`| `/api/admin/requests/{id}/resolve` | Aprovar/Rejeitar solicitação |
| `GET`  | `/api/admin/audit?severity=info&role=doctor` | Log de auditoria |

---

### Médico (self-service)

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET`  | `/api/doctor/profile` | Perfil do médico logado |
| `PUT`  | `/api/doctor/profile` | Atualizar perfil (gera change request p/ e-mail) |
| `POST` | `/api/doctor/change-request` | Solicitar alteração de dado |

---

## 🔗 Integrando com o Frontend

Para conectar o frontend estático ao backend, atualize o `js/app.js` substituindo
as chamadas aos arrays mockados por chamadas `fetch` à API.

**Exemplo – Login de paciente:**
```javascript
// Antes (mock)
const user = mockPatients.find(p => p.id === patientId);

// Depois (API)
const res  = await fetch("http://localhost:8080/api/auth/patient/login", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ patientId })
});
const { token, user } = await res.json();
sessionStorage.setItem("medflow_token", token);
```

---

## 🏗️ Estrutura do Projeto

```
medflow-backend/
├── pom.xml
└── src/main/
    ├── java/com/medflow/
    │   ├── MedFlowApplication.java
    │   ├── config/
    │   │   ├── SecurityConfig.java
    │   │   └── GlobalExceptionHandler.java
    │   ├── security/
    │   │   ├── JwtUtil.java
    │   │   └── JwtFilter.java
    │   ├── model/          (User, MedicalRecord, Ticket, ...)
    │   ├── repository/     (Spring Data JPA)
    │   ├── service/        (lógica de negócio)
    │   ├── controller/     (REST endpoints)
    │   └── dto/            (request/response objects)
    └── resources/
        ├── application.properties  ← ✏️ EDITE AQUI
        └── schema.sql              ← Execute no Supabase
```

---

## 🔒 Segurança

- Autenticação via **JWT** (Bearer Token)
- Autorização por papel: `ADMIN`, `DOCTOR`, `PATIENT`
- CORS configurado para o frontend local
- Senhas com **BCrypt** (hash padrão: `medflow123`)
- Sessão **stateless** (sem cookies)
