// ============================================================
// MedFlow – data.js
// Os dados agora vêm do backend (Supabase).
// Este arquivo existe apenas para compatibilidade de imports
// em páginas que ainda referenciam scripts antigos.
// ============================================================

// Objetos vazios – o App.js real faz as chamadas à API
const mockPatients = [];
const mockDoctors  = [];
const mockAdmins   = [];
let mockRecords    = [];
let mockTickets    = [];
let mockDataChangeRequests = [];
let auditLog       = [];
const hospitalStats = { weekly:{}, monthly:{}, yearly:{} };
const monthlyTrend  = [];
