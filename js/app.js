// ============================================================
// MedFlow – Estado global (Auth + Data)
// ============================================================

const App = (() => {
  // ── Estado ──────────────────────────────────────────────
  let currentUser = null;

  // Carrega sessão salva
  const saved = sessionStorage.getItem("medflow_user");
  if (saved) try { currentUser = JSON.parse(saved); } catch (_) {}

  // ── Auth ─────────────────────────────────────────────────
  function login(user) {
    currentUser = user;
    sessionStorage.setItem("medflow_user", JSON.stringify(user));
  }

  function logout() {
    currentUser = null;
    sessionStorage.removeItem("medflow_user");
    window.location.href = rootPath() + "index.html";
  }

  function getUser() { return currentUser; }

  function requireAuth(expectedRole) {
    if (!currentUser) { window.location.href = rootPath() + "index.html"; return false; }
    if (expectedRole && currentUser.role !== expectedRole) {
      window.location.href = rootPath() + "index.html"; return false;
    }
    return true;
  }

  // Descobre o caminho raiz relativo ao arquivo atual
  function rootPath() {
    const depth = window.location.pathname.split("/").filter(Boolean).length;
    // pages/admin/xxx.html -> depth=3 on server, but we work with file paths
    // Detect by checking if we're in a subfolder
    const path = window.location.pathname;
    if (path.includes("/pages/admin/") || path.includes("/pages/doctor/") || path.includes("/pages/patient/")) {
      return "../../";
    }
    if (path.includes("/pages/")) return "../";
    return "";
  }

  // ── Login helpers ─────────────────────────────────────────
  function loginByEmail(email) {
    const user = mockPatients.find(p => p.email.toLowerCase() === email.toLowerCase());
    if (user) { login(user); return user; }
    return null;
  }

  function loginByUsername(username) {
    const user = mockDoctors.find(d => d.username.toLowerCase() === username.toLowerCase());
    if (user) { login(user); return user; }
    const admin = mockAdmins.find(a => a.username.toLowerCase() === username.toLowerCase());
    if (admin) { login(admin); return admin; }
    return null;
  }

  // ── Tickets ───────────────────────────────────────────────
  function acceptTicket(id) {
    const t = mockTickets.find(t => t.id === id);
    if (!t) return;
    t.status = "em_andamento";
    t.doctorId = currentUser.id;
    t.doctorName = currentUser.name;
    t.updatedAt = new Date().toISOString().split("T")[0];
  }

  function completeTicket(id) {
    const t = mockTickets.find(t => t.id === id);
    if (!t) return;
    t.status = "concluido";
    t.updatedAt = new Date().toISOString().split("T")[0];
    t.result = "Avaliação realizada com sucesso";
  }

  function createTicket(type, specialty, description) {
    const id = "t" + Date.now();
    mockTickets.push({
      id, patientId: currentUser.id, patientName: currentUser.name,
      type, specialty, description, status: "aberto",
      createdAt: new Date().toISOString().split("T")[0],
      updatedAt: new Date().toISOString().split("T")[0],
    });
  }

  // ── Records ───────────────────────────────────────────────
  function createRecord(patientId, type, rawNotes, formattedNotes, diagnosis) {
    const patient = mockPatients.find(p => p.id === patientId);
    if (!patient) return null;
    const rec = {
      id: "r" + Date.now(), patientId: patient.id, patientName: patient.name,
      doctorId: currentUser.id, doctorName: currentUser.name,
      date: new Date().toISOString().split("T")[0],
      type, rawNotes, formattedNotes, diagnosis, editHistory: [],
    };
    mockRecords.unshift(rec);
    addAuditEntry("RECORD_CREATE", "info", `Prontuário criado para ${patient.name}`);
    return rec;
  }

  function editRecord(recordId, rawNotes, formattedNotes, diagnosis, type) {
    const rec = mockRecords.find(r => r.id === recordId);
    if (!rec) return;
    rec.editHistory.push({
      id: "eh" + Date.now(), editedBy: currentUser.id, editedByName: currentUser.name,
      timestamp: new Date().toISOString(), changes: "Prontuário editado.",
    });
    rec.rawNotes = rawNotes;
    rec.formattedNotes = formattedNotes;
    rec.diagnosis = diagnosis;
    rec.type = type;
    addAuditEntry("RECORD_EDIT", "warning", `Prontuário de ${rec.patientName} editado`);
  }

  // ── Doctors ───────────────────────────────────────────────
  function addDoctor(form) {
    const username = form.name
      .replace(/^(Dr\.|Dra\.)\s*/i, "").trim().toLowerCase()
      .normalize("NFD").replace(/[\u0300-\u036f]/g, "")
      .split(/\s+/).filter(Boolean).slice(0, 2).join(".");
    const doc = {
      id: "d" + Date.now(), role: "doctor",
      name: form.name, email: form.email, phone: form.phone, cpf: form.cpf,
      specialty: form.specialty, crm: form.crm, username,
      attendances: 0, surgeries: 0, exams: 0,
    };
    mockDoctors.push(doc);
    addAuditEntry("DOCTOR_CREATE", "info", `Médico ${doc.name} cadastrado`);
    return doc;
  }

  function updateDoctor(id, form) {
    const doc = mockDoctors.find(d => d.id === id);
    if (!doc) return;
    Object.assign(doc, form);
    addAuditEntry("DOCTOR_UPDATE", "warning", `Dados do médico ${doc.name} atualizados`);
  }

  // ── Change Requests ───────────────────────────────────────
  function resolveChangeRequest(requestId, approved) {
    const req = mockDataChangeRequests.find(r => r.id === requestId);
    if (!req) return;
    req.status = approved ? "aprovado" : "rejeitado";
    req.resolvedAt = new Date().toISOString();
    req.resolvedBy = currentUser.id;
    req.resolvedByName = currentUser.name;
    addAuditEntry("REQUEST_RESOLVE", "warning",
      `${approved ? "Aprovou" : "Rejeitou"} solicitação de ${req.requesterName}`);
  }

  function submitChangeRequest(fieldName, oldValue, newValue) {
    mockDataChangeRequests.push({
      id: "dcr" + Date.now(), requesterId: currentUser.id, requesterName: currentUser.name,
      requesterRole: "doctor", fieldName, oldValue, newValue,
      status: "pendente", createdAt: new Date().toISOString(),
    });
    addAuditEntry("REQUEST_CREATE", "info", `Solicitação de alteração de ${fieldName} enviada`);
  }

  // ── Audit Log ─────────────────────────────────────────────
  function addAuditEntry(action, severity, details) {
    auditLog.unshift({
      id: "al" + Date.now(), action, severity, details,
      userId: currentUser?.id || "system", userName: currentUser?.name || "Sistema",
      userRole: currentUser?.role || "system",
      timestamp: new Date().toISOString(),
    });
  }

  // ── AI Format (simulado) ──────────────────────────────────
  function simulateAIFormatting(raw, type) {
    let text = raw.trim();
    if (!text) return "";
    text = text.replace(/(^|[.!?]\s+)([a-z])/g, (_, p, c) => p + c.toUpperCase());
    if (text[0]) text = text[0].toUpperCase() + text.slice(1);
    const replacements = {
      "pressao": "pressão arterial", "coracao": "miocárdio",
      "dor no peito": "dor torácica (precordialgia)", "batimento": "frequência cardíaca",
      "remedio": "fármaco", "melhorou": "apresentou melhora clínica significativa",
      "piorou": "apresentou piora do quadro clínico", "exame de sangue": "hemograma completo",
    };
    Object.entries(replacements).forEach(([k, v]) => {
      text = text.replace(new RegExp(k, "gi"), v);
    });
    if (!text.endsWith(".")) text += ".";
    const typeLabel = type === "cirurgia" ? "RELATÓRIO CIRÚRGICO" : "RELATÓRIO DE CONSULTA";
    const conduta = type === "cirurgia"
      ? "Conduta pós-operatória: monitorar sinais vitais, avaliar evolução e agendar retorno."
      : "Conduta: avaliar evolução do quadro e reavaliar em próxima consulta.";
    return `${typeLabel} - ${new Date().toLocaleDateString("pt-BR")}\n\n${text}\n\n${conduta}`;
  }

  // ── Utilitários ───────────────────────────────────────────
  function statusBadge(status) {
    const map = {
      aberto:       { cls: "badge-warning", label: "Aberto" },
      em_andamento: { cls: "badge-info",    label: "Em Andamento" },
      concluido:    { cls: "badge-success", label: "Concluído" },
      pendente:     { cls: "badge-warning", label: "Pendente" },
      aprovado:     { cls: "badge-success", label: "Aprovado" },
      rejeitado:    { cls: "badge-danger",  label: "Rejeitado" },
    };
    const s = map[status] || { cls: "", label: status };
    return `<span class="badge ${s.cls}">${s.label}</span>`;
  }

  function typeBadge(type) {
    return type === "consulta"
      ? `<span class="badge badge-purple">Consulta</span>`
      : `<span class="badge badge-teal">Exame</span>`;
  }

  function formatDate(iso) {
    if (!iso) return "-";
    try { return new Date(iso).toLocaleDateString("pt-BR"); } catch { return iso; }
  }

  function toast(msg, type = "success") {
    const el = document.createElement("div");
    el.className = `toast toast-${type}`;
    el.textContent = msg;
    document.body.appendChild(el);
    requestAnimationFrame(() => el.classList.add("show"));
    setTimeout(() => { el.classList.remove("show"); setTimeout(() => el.remove(), 300); }, 2800);
  }

  return {
    login, logout, getUser, requireAuth, rootPath,
    loginByEmail, loginByUsername,
    acceptTicket, completeTicket, createTicket,
    createRecord, editRecord,
    addDoctor, updateDoctor,
    resolveChangeRequest, submitChangeRequest,
    addAuditEntry,
    simulateAIFormatting,
    statusBadge, typeBadge, formatDate, toast,
  };
})();
