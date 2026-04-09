// ============================================================
// MedFlow – API Client + Estado Global
// ============================================================

const App = (() => {

  // ── Configuração ──────────────────────────────────────────
  const API_BASE = "http://localhost:8080/api";

  // ── Estado ────────────────────────────────────────────────
  let currentUser = null;
  const saved = sessionStorage.getItem("medflow_user");
  if (saved) try { currentUser = JSON.parse(saved); } catch (_) {}

  // ── HTTP Helper ───────────────────────────────────────────
  async function http(method, path, body) {
    const token = sessionStorage.getItem("medflow_token");
    const headers = { "Content-Type": "application/json" };
    if (token) headers["Authorization"] = "Bearer " + token;

    const res = await fetch(API_BASE + path, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });

    if (res.status === 401) { logout(); return; }
    if (res.status === 204)  return null;

    const data = await res.json();
    if (!res.ok) throw new Error(data.message || "Erro na requisição");
    return data;
  }

  const get    = (path)         => http("GET",    path);
  const post   = (path, body)   => http("POST",   path, body);
  const put    = (path, body)   => http("PUT",    path, body);
  const patch  = (path, body)   => http("PATCH",  path, body);
  const del    = (path)         => http("DELETE",  path);

  // ── Auth ──────────────────────────────────────────────────
  function getUser()   { return currentUser; }
  function getToken()  { return sessionStorage.getItem("medflow_token"); }

  function _saveSession(token, user) {
    sessionStorage.setItem("medflow_token", token);
    sessionStorage.setItem("medflow_user",  JSON.stringify(user));
    currentUser = user;
  }

  function logout() {
    currentUser = null;
    sessionStorage.removeItem("medflow_token");
    sessionStorage.removeItem("medflow_user");
    window.location.href = rootPath() + "index.html";
  }

  function requireAuth(expectedRole) {
    if (!currentUser || !getToken()) {
      window.location.href = rootPath() + "index.html";
      return false;
    }
    if (expectedRole && currentUser.role !== expectedRole) {
      window.location.href = rootPath() + "index.html";
      return false;
    }
    return true;
  }

  function rootPath() {
    const path = window.location.pathname;
    if (path.includes("/pages/admin/") || path.includes("/pages/doctor/") || path.includes("/pages/patient/"))
      return "../../";
    if (path.includes("/pages/")) return "../";
    return "";
  }

  // ── Login ─────────────────────────────────────────────────
  async function loginByPatientId(patientId) {
    const data = await post("/auth/patient/login", { patientId });
    if (data) { _saveSession(data.token, data.user); return data.user; }
    return null;
  }

  async function loginByUsername(username, role) {
    const data = await post("/auth/staff/login", { username, role });
    if (data) { _saveSession(data.token, data.user); return data.user; }
    return null;
  }

  async function listPatients() { return get("/auth/patients"); }
  async function listStaff(role){ return get("/auth/staff?role=" + role); }

  // ── Tickets ───────────────────────────────────────────────
  async function getMyTickets()           { return get("/tickets/my"); }
  async function getAllTickets(status)     { return get("/tickets" + (status ? "?status=" + status : "")); }
  async function createTicket(type, specialty, description) {
    return post("/tickets", { type, specialty, description });
  }
  async function acceptTicket(id)         { return patch("/tickets/" + id + "/accept"); }
  async function completeTicket(id)       { return patch("/tickets/" + id + "/complete"); }

  // ── Records ───────────────────────────────────────────────
  async function getAllRecords(q)         { return get("/records" + (q ? "?q=" + encodeURIComponent(q) : "")); }
  async function getMyRecords()           { return get("/records/my"); }
  async function getPatientRecords(pid)   { return get("/records/patient/" + pid); }
  async function formatNotes(rawNotes, type) {
    return post("/records/format", { rawNotes, type });
  }
  async function createRecord(patientId, type, rawNotes, formattedNotes, diagnosis) {
    return post("/records", { patientId, type, rawNotes, formattedNotes, diagnosis });
  }
  async function editRecord(recordId, type, rawNotes, formattedNotes, diagnosis) {
    return put("/records/" + recordId, { type, rawNotes, formattedNotes, diagnosis });
  }

  // ── Patients ──────────────────────────────────────────────
  async function getAllPatients()                          { return get("/patients"); }
  async function getPatient(id)                           { return get("/patients/" + id); }
  async function registerPatient(name, email, phone, cpf) {
    return post("/patients", { name, email, phone, cpf });
  }
  async function updateMyProfile(id, name, email, phone) {
    return put("/patients/" + id + "/profile", { name, email, phone });
  }

  // ── Doctors (admin) ───────────────────────────────────────
  async function getAllDoctors()       { return get("/admin/doctors"); }
  async function createDoctor(form)   { return post("/admin/doctors", form); }
  async function updateDoctor(id, form){ return put("/admin/doctors/" + id, form); }

  // ── Doctor self ───────────────────────────────────────────
  async function getDoctorProfile()       { return get("/doctor/profile"); }
  async function updateDoctorProfile(body){ return put("/doctor/profile", body); }
  async function submitChangeRequest(fieldName, oldValue, newValue) {
    return post("/doctor/change-request", { fieldName, oldValue, newValue });
  }

  // ── Admin ─────────────────────────────────────────────────
  async function getDashboard()                    { return get("/admin/dashboard"); }
  async function getRequests(status)               { return get("/admin/requests" + (status ? "?status=" + status : "")); }
  async function resolveRequest(id, approved)      { return patch("/admin/requests/" + id + "/resolve", { approved }); }
  async function getAuditLog(severity, role) {
    const params = new URLSearchParams();
    if (severity) params.set("severity", severity);
    if (role)     params.set("role", role);
    const qs = params.toString();
    return get("/admin/audit" + (qs ? "?" + qs : ""));
  }

  // ── Utilitários UI ────────────────────────────────────────
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
      : type === "cirurgia"
      ? `<span class="badge badge-teal">Cirurgia</span>`
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

  // Mostra spinner de carregamento em um elemento
  function showLoading(elementId) {
    const el = document.getElementById(elementId);
    if (el) el.innerHTML = `<div class="empty"><svg xmlns="http://www.w3.org/2000/svg" style="animation:spin 1s linear infinite" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a9 9 0 1 1-6.219-8.56"/></svg><p>Carregando...</p></div>
    <style>@keyframes spin{from{transform:rotate(0deg)}to{transform:rotate(360deg)}}</style>`;
  }

  function showError(elementId, msg) {
    const el = document.getElementById(elementId);
    if (el) el.innerHTML = `<div class="empty"><p style="color:var(--danger)">${msg}</p></div>`;
  }

  return {
    // auth
    getUser, getToken, logout, requireAuth, rootPath,
    loginByPatientId, loginByUsername, listPatients, listStaff,
    // tickets
    getMyTickets, getAllTickets, createTicket, acceptTicket, completeTicket,
    // records
    getAllRecords, getMyRecords, getPatientRecords,
    formatNotes, createRecord, editRecord,
    // patients
    getAllPatients, getPatient, registerPatient, updateMyProfile,
    // doctors
    getAllDoctors, createDoctor, updateDoctor,
    getDoctorProfile, updateDoctorProfile, submitChangeRequest,
    // admin
    getDashboard, getRequests, resolveRequest, getAuditLog,
    // ui helpers
    statusBadge, typeBadge, formatDate, toast, showLoading, showError,
  };
})();
