// ============================================================
// MedFlow – Sidebar dinâmica
// ============================================================

function renderSidebar(activePath) {
  const user = App.getUser();
  if (!user) return;

  const root = App.rootPath();

  const menus = {
    admin: [
      { label: "Dashboard",        icon: "grid",           path: root + "pages/admin/dashboard.html" },
      { label: "Gerenciar Médicos",icon: "user-cog",       path: root + "pages/admin/doctors.html"   },
      { label: "Solicitações",     icon: "git-pull-request",path: root + "pages/admin/requests.html" },
      { label: "Log de Auditoria", icon: "scroll",         path: root + "pages/admin/audit.html"     },
    ],
    doctor: [
      { label: "Dashboard",         icon: "grid",         path: root + "pages/doctor/dashboard.html" },
      { label: "Prontuários",       icon: "file-text",    path: root + "pages/doctor/records.html"   },
      { label: "Chamados",          icon: "clipboard",    path: root + "pages/doctor/tickets.html"   },
      { label: "Cadastrar Paciente",icon: "user-plus",    path: root + "pages/doctor/register.html"  },
      { label: "Meu Perfil",        icon: "user-circle",  path: root + "pages/doctor/profile.html"   },
    ],
    patient: [
      { label: "Dashboard",        icon: "grid",         path: root + "pages/patient/dashboard.html" },
      { label: "Meus Chamados",    icon: "clipboard",    path: root + "pages/patient/tickets.html"   },
      { label: "Abrir Solicitação",icon: "calendar-plus",path: root + "pages/patient/new-ticket.html"},
      { label: "Resultados",       icon: "activity",     path: root + "pages/patient/results.html"   },
      { label: "Meu Perfil",       icon: "user-circle",  path: root + "pages/patient/profile.html"   },
    ],
  };

  const items = menus[user.role] || [];
  const roleLabel = user.role === "admin"
    ? (user.title || "Administrador")
    : user.role === "doctor" ? "Médico" : "Paciente";

  const pendingCount = user.role === "admin"
    ? mockDataChangeRequests.filter(r => r.status === "pendente").length
    : 0;

  const itemsHtml = items.map(item => {
    const isActive = window.location.pathname.endsWith(item.path.replace(/^.*\//, "")) ||
                     window.location.href.includes(item.path.replace(root, ""));
    const badge = item.label === "Solicitações" && pendingCount > 0
      ? `<span class="sidebar-badge">${pendingCount}</span>` : "";
    return `
      <a href="${item.path}" class="sidebar-item ${isActive ? "active" : ""}">
        <i data-lucide="${item.icon}" class="sidebar-icon"></i>
        <span class="sidebar-label">${item.label}</span>
        ${badge}
      </a>`;
  }).join("");

  const html = `
    <button class="mobile-menu-btn" id="mobileMenuBtn">
      <i data-lucide="menu"></i>
    </button>

    <div class="sidebar-overlay" id="sidebarOverlay"></div>

    <aside class="sidebar" id="sidebar">
      <div class="sidebar-header">
        <div class="sidebar-logo">
          <div class="sidebar-logo-icon">
            <i data-lucide="heart-pulse"></i>
          </div>
          <span class="sidebar-logo-text">MedFlow</span>
        </div>
        <button class="sidebar-collapse-btn" id="collapseBtn">
          <i data-lucide="chevron-left"></i>
        </button>
      </div>

      <nav class="sidebar-nav">${itemsHtml}</nav>

      <div class="sidebar-footer">
        <div class="sidebar-user">
          <div class="sidebar-avatar">${user.name.charAt(0).toUpperCase()}</div>
          <div class="sidebar-user-info">
            <p class="sidebar-user-name">${user.name}</p>
            <p class="sidebar-user-role">${roleLabel}</p>
          </div>
        </div>
        <button class="sidebar-logout" onclick="App.logout()" title="Sair">
          <i data-lucide="log-out"></i>
        </button>
      </div>
    </aside>`;

  document.getElementById("sidebarMount").innerHTML = html;

  // Mobile toggle
  document.getElementById("mobileMenuBtn").addEventListener("click", () => {
    document.getElementById("sidebar").classList.toggle("mobile-open");
    document.getElementById("sidebarOverlay").classList.toggle("active");
  });
  document.getElementById("sidebarOverlay").addEventListener("click", () => {
    document.getElementById("sidebar").classList.remove("mobile-open");
    document.getElementById("sidebarOverlay").classList.remove("active");
  });

  // Desktop collapse
  document.getElementById("collapseBtn").addEventListener("click", () => {
    const sidebar = document.getElementById("sidebar");
    sidebar.classList.toggle("collapsed");
    document.getElementById("collapseBtn").querySelector("[data-lucide]")
      .setAttribute("data-lucide", sidebar.classList.contains("collapsed") ? "chevron-right" : "chevron-left");
    lucide.createIcons();
  });

  lucide.createIcons();
}
