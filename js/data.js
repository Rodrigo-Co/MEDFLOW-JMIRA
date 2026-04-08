// ============================================================
// MedFlow – Dados Mockados
// ============================================================

const mockAdmins = [
  { id: "a1", name: "Dr. Fernando Costa",  email: "fernando@neocore.com", phone: "(11) 99900-0001", cpf: "000.000.000-01", role: "admin", title: "Diretor do Hospital",          username: "fernando.costa" },
  { id: "a2", name: "Dra. Lucia Martins",  email: "lucia@neocore.com",    phone: "(11) 99900-0002", cpf: "000.000.000-02", role: "admin", title: "Diretora de Análises Clínicas", username: "lucia.martins"   },
  { id: "a3", name: "Roberto Almeida",     email: "roberto@neocore.com",  phone: "(11) 99900-0003", cpf: "000.000.000-03", role: "admin", title: "Diretor Administrativo",       username: "roberto.almeida" },
];

const mockDoctors = [
  { id: "d1", name: "Dr. Ricardo Mendes",       email: "ricardo@cardio.com", phone: "(11) 99800-1001", cpf: "111.222.333-44", role: "doctor", specialty: "Cardiologia",              crm: "CRM/SP 123456", username: "ricardo.mendes", attendances: 342, surgeries: 28, exams: 156 },
  { id: "d2", name: "Dra. Ana Beatriz Lima",    email: "ana@cardio.com",     phone: "(11) 99800-1002", cpf: "222.333.444-55", role: "doctor", specialty: "Cirurgia Cardiovascular", crm: "CRM/SP 654321", username: "ana.lima",       attendances: 218, surgeries: 67, exams: 98  },
  { id: "d3", name: "Dr. Carlos Eduardo Silva", email: "carlos@cardio.com",  phone: "(11) 99800-1003", cpf: "333.444.555-66", role: "doctor", specialty: "Hemodinâmica",            crm: "CRM/SP 789012", username: "carlos.silva",   attendances: 189, surgeries: 12, exams: 234 },
];

const mockPatients = [
  {
    id: "p1", name: "Maria da Silva", email: "maria@email.com", phone: "(11) 99700-2001", cpf: "444.555.666-77", role: "patient",
    bloodPressureHistory: [
      { date: "Jan", systolic: 150, diastolic: 95 }, { date: "Fev", systolic: 145, diastolic: 92 },
      { date: "Mar", systolic: 140, diastolic: 88 }, { date: "Abr", systolic: 135, diastolic: 85 },
      { date: "Mai", systolic: 130, diastolic: 82 }, { date: "Jun", systolic: 125, diastolic: 80 },
    ],
    heartRateHistory: [
      { date: "Jan", rate: 88 }, { date: "Fev", rate: 85 }, { date: "Mar", rate: 82 },
      { date: "Abr", rate: 78 }, { date: "Mai", rate: 75 }, { date: "Jun", rate: 72 },
    ],
    medications: ["Losartana 50mg", "Atenolol 25mg", "AAS 100mg"],
    conditions: ["Hipertensão Arterial", "Arritmia Cardíaca"],
  },
  {
    id: "p2", name: "Joao Carlos Pereira", email: "joao@email.com", phone: "(11) 99700-2002", cpf: "555.666.777-88", role: "patient",
    bloodPressureHistory: [
      { date: "Jan", systolic: 160, diastolic: 100 }, { date: "Fev", systolic: 155, diastolic: 96 },
      { date: "Mar", systolic: 148, diastolic: 92  }, { date: "Abr", systolic: 142, diastolic: 88 },
      { date: "Mai", systolic: 138, diastolic: 86  }, { date: "Jun", systolic: 132, diastolic: 84 },
    ],
    heartRateHistory: [
      { date: "Jan", rate: 95 }, { date: "Fev", rate: 90 }, { date: "Mar", rate: 86 },
      { date: "Abr", rate: 82 }, { date: "Mai", rate: 78 }, { date: "Jun", rate: 76 },
    ],
    medications: ["Enalapril 10mg", "Sinvastatina 20mg"],
    conditions: ["Insuficiência Cardíaca", "Colesterol Alto"],
  },
  {
    id: "p3", name: "Ana Paula Rodrigues", email: "ana.paula@email.com", phone: "(11) 99700-2003", cpf: "666.777.888-99", role: "patient",
    bloodPressureHistory: [
      { date: "Jan", systolic: 138, diastolic: 88 }, { date: "Fev", systolic: 135, diastolic: 86 },
      { date: "Mar", systolic: 130, diastolic: 84 }, { date: "Abr", systolic: 128, diastolic: 82 },
      { date: "Mai", systolic: 125, diastolic: 80 }, { date: "Jun", systolic: 122, diastolic: 78 },
    ],
    heartRateHistory: [
      { date: "Jan", rate: 82 }, { date: "Fev", rate: 80 }, { date: "Mar", rate: 78 },
      { date: "Abr", rate: 76 }, { date: "Mai", rate: 74 }, { date: "Jun", rate: 72 },
    ],
    medications: ["Captopril 25mg"],
    conditions: ["Pré-hipertensão"],
  },
];

let mockRecords = [
  {
    id: "r1", patientId: "p1", patientName: "Maria da Silva", doctorId: "d1", doctorName: "Dr. Ricardo Mendes",
    date: "2026-03-20", type: "consulta",
    rawNotes: "paciente relata dor no peito ao esforço físico, pressão melhorou desde última consulta, manter medicação atual",
    formattedNotes: "Paciente relata dor torácica ao esforço físico. Pressão arterial apresentou melhora significativa desde a última consulta. Conduta: manter medicação atual e agendar eletrocardiograma de controle.",
    diagnosis: "Angina estável em acompanhamento", editHistory: [],
  },
  {
    id: "r2", patientId: "p2", patientName: "Joao Carlos Pereira", doctorId: "d1", doctorName: "Dr. Ricardo Mendes",
    date: "2026-03-18", type: "consulta",
    rawNotes: "paciente apresenta melhora no quadro de insuficiência cardíaca, ecocardiograma mostrou fração de ejeção de 45%",
    formattedNotes: "Paciente apresenta melhora no quadro de insuficiência cardíaca congestiva. Ecocardiograma realizado demonstrou fração de ejeção de 45%, evidenciando evolução positiva. Conduta: manter esquema terapêutico vigente e reavaliar em 30 dias.",
    diagnosis: "Insuficiência cardíaca em melhora progressiva",
    editHistory: [{ id: "eh1", editedBy: "d1", editedByName: "Dr. Ricardo Mendes", timestamp: "2026-03-19T14:30:00.000Z", changes: "Diagnóstico atualizado." }],
  },
  {
    id: "r3", patientId: "p3", patientName: "Ana Paula Rodrigues", doctorId: "d2", doctorName: "Dra. Ana Beatriz Lima",
    date: "2026-03-15", type: "cirurgia",
    rawNotes: "avaliação pré-operatória para angioplastia, exames dentro da normalidade, paciente apta para procedimento",
    formattedNotes: "Avaliação pré-operatória para angioplastia coronariana. Exames laboratoriais e de imagem dentro dos parâmetros de normalidade. Paciente considerada apta para o procedimento cirúrgico.",
    diagnosis: "Pré-operatório de angioplastia - apta", editHistory: [],
  },
];

let mockTickets = [
  { id: "t1", patientId: "p1", patientName: "Maria da Silva",      type: "consulta", specialty: "Cardiologia",              description: "Sentindo dor no peito e falta de ar",       status: "em_andamento", createdAt: "2026-03-19", updatedAt: "2026-03-20", doctorId: "d1", doctorName: "Dr. Ricardo Mendes" },
  { id: "t2", patientId: "p1", patientName: "Maria da Silva",      type: "exame",    specialty: "Cardiologia",              description: "Eletrocardiograma de controle",              status: "concluido",     createdAt: "2026-03-10", updatedAt: "2026-03-15", doctorId: "d1", doctorName: "Dr. Ricardo Mendes", result: "ECG sem alterações significativas" },
  { id: "t3", patientId: "p2", patientName: "Joao Carlos Pereira", type: "exame",    specialty: "Cardiologia",              description: "Ecocardiograma de acompanhamento",          status: "aberto",        createdAt: "2026-03-21", updatedAt: "2026-03-21" },
  { id: "t4", patientId: "p3", patientName: "Ana Paula Rodrigues", type: "consulta", specialty: "Cirurgia Cardiovascular",  description: "Retorno pós-operatório angioplastia",       status: "aberto",        createdAt: "2026-03-22", updatedAt: "2026-03-22" },
  { id: "t5", patientId: "p2", patientName: "Joao Carlos Pereira", type: "consulta", specialty: "Cardiologia",              description: "Consulta de rotina para acompanhamento",    status: "em_andamento",  createdAt: "2026-03-17", updatedAt: "2026-03-18", doctorId: "d1", doctorName: "Dr. Ricardo Mendes" },
];

let mockDataChangeRequests = [
  {
    id: "dcr1", requesterId: "d3", requesterName: "Dr. Carlos Eduardo Silva", requesterRole: "doctor",
    fieldName: "email", oldValue: "carlos@cardio.com", newValue: "carlos.silva@neocore.com",
    status: "pendente", createdAt: "2026-03-28T10:00:00.000Z",
  },
];

let auditLog = [];

const hospitalStats = {
  weekly:  { attendances: 87,   surgeries: 5,   exams: 134  },
  monthly: { attendances: 342,  surgeries: 18,  exams: 521  },
  yearly:  { attendances: 4102, surgeries: 207, exams: 6234 },
};

const monthlyTrend = [
  { month: "Out", atendimentos: 310, cirurgias: 15, exames: 480 },
  { month: "Nov", atendimentos: 328, cirurgias: 20, exames: 502 },
  { month: "Dez", atendimentos: 295, cirurgias: 14, exames: 460 },
  { month: "Jan", atendimentos: 335, cirurgias: 22, exames: 510 },
  { month: "Fev", atendimentos: 318, cirurgias: 16, exames: 498 },
  { month: "Mar", atendimentos: 342, cirurgias: 18, exames: 521 },
];
