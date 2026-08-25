/* ==========================================================================
   app.js — shared across all authenticated pages, plus the login handler.
   Uses jQuery 3.6+ and plain ES6. No build step: loaded via <script> tag.
   ========================================================================== */

const App = (() => {
    "use strict";

    /** Demo/sample data used only as a fallback when the backend API is not
     *  reachable yet, so the frontend can be reviewed on its own. Every real
     *  request still goes to the API first — see ApiClient.request(). */
    const DEMO_MODE_BANNER_ID = "demoModeBanner";

    const CURRENT_USER_KEY = "tms_current_user";

    /* ---------------------------------------------------------------------
       Session helpers (sessionStorage mirrors the server-side HTTP session
       just enough for the UI to render a name/role instantly; the servlet's
       HttpSession remains the actual source of truth for auth).
       --------------------------------------------------------------------- */
    const Session = {
        get() {
            try {
                const raw = sessionStorage.getItem(CURRENT_USER_KEY);
                return raw ? JSON.parse(raw) : null;
            } catch (e) {
                return null;
            }
        },
        set(user) {
            sessionStorage.setItem(CURRENT_USER_KEY, JSON.stringify(user));
        },
        clear() {
            sessionStorage.removeItem(CURRENT_USER_KEY);
        }
    };

    /* ---------------------------------------------------------------------
       Minimal API client wrapping $.ajax with the global 401 handling the
       spec calls for (redirect to login on an expired/missing session).
       --------------------------------------------------------------------- */
    const ApiClient = {
        request(options) {
            return $.ajax(Object.assign({
                dataType: "json",
                contentType: "application/json"
            }, options)).fail((xhr) => {
                if (xhr.status === 401 && !location.pathname.endsWith("login.html")) {
                    Session.clear();
                    window.location.href = "login.html";
                }
            });
        },
        get(url, data) {
            return this.request({ url, method: "GET", data });
        },
        post(url, body) {
            return this.request({ url, method: "POST", data: JSON.stringify(body) });
        },
        put(url, body) {
            return this.request({ url, method: "PUT", data: JSON.stringify(body) });
        }
    };

    /* ---------------------------------------------------------------------
       Escaping / formatting helpers, reused by tickets.js and dashboard.js
       --------------------------------------------------------------------- */
    /** True when a failed AJAX call did not come from a real JSON API — i.e.
     *  there's no backend wired up yet (network error, or a plain-text/HTML
     *  404/501 from a static file server). A real backend always responds
     *  with JSON per spec §7.4, so this stays reliable once one exists. */
    function isBackendUnavailable(xhr) {
        return xhr.status === 0 || xhr.responseJSON === undefined;
    }

    function escapeHtml(text) {
        const div = document.createElement("div");
        div.textContent = text == null ? "" : String(text);
        return div.innerHTML;
    }

    function formatDate(dateString) {
        if (!dateString) return "\u2014";
        const d = new Date(dateString);
        if (isNaN(d.getTime())) return "\u2014";
        return d.toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" }) +
            " \u00b7 " + d.toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" });
    }

    function initials(name) {
        if (!name) return "?";
        const parts = name.trim().split(/\s+/);
        return ((parts[0]?.[0] || "") + (parts[1]?.[0] || "")).toUpperCase() || name[0].toUpperCase();
    }

    function getPriorityBadge(priority) {
        const p = (priority || "LOW").toUpperCase();
        const label = p.charAt(0) + p.slice(1).toLowerCase();
        return `<span class="badge badge-priority-${p.toLowerCase()}"><span class="status-dot"></span>${label}</span>`;
    }

    function getStatusBadge(status) {
        const s = (status || "OPEN").toUpperCase();
        const label = s.replace("_", " ").split(" ").map(w => w.charAt(0) + w.slice(1).toLowerCase()).join(" ");
        return `<span class="badge badge-status-${s.toLowerCase()}"><span class="status-dot"></span>${label}</span>`;
    }

    /* ---------------------------------------------------------------------
       Layout: sidebar/navbar/footer markup, generated inline (not fetched).

       Earlier version used $.load("components/sidebar.html") via AJAX. That
       only works when the app is served over http:// by a real server —
       opening the files directly (file://) makes the browser silently block
       that fetch, so the sidebar/navbar never appeared. Since the whole point
       of this stage is a frontend that works on its own, with zero
       dependencies (no server, no backend, no database), the shell markup
       is now built in JS and injected directly — no fetch involved.

       components/sidebar.html, navbar.html, footer.html are kept as the
       source-of-truth markup/reference (and match the project structure),
       but are no longer required at runtime.
       --------------------------------------------------------------------- */

    function sidebarMarkup() {
        return `
<aside class="app-sidebar" id="appSidebar">
  <div class="sidebar-brand">
    <span class="brand-mark">TMS</span>
    <span class="brand-name">Ticketing</span>
  </div>

  <nav class="sidebar-nav">
    <p class="nav-section-label">Workspace</p>
    <a href="dashboard.html" class="nav-link" data-page="dashboard">
      <svg class="nav-icon" viewBox="0 0 20 20" fill="none"><path d="M3 10.5 10 4l7 6.5M5 9v7h4v-4h2v4h4V9" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>
      <span>Dashboard</span>
    </a>
    <a href="tickets.html" class="nav-link" data-page="tickets">
      <svg class="nav-icon" viewBox="0 0 20 20" fill="none"><path d="M3 6a1 1 0 0 1 1-1h12a1 1 0 0 1 1 1v1.5a1.5 1.5 0 0 0 0 3V12a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-1.5a1.5 1.5 0 0 0 0-3V6Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/></svg>
      <span>Tickets</span>
    </a>
    <a href="tickets.html?new=1" class="nav-link" data-page="new-ticket">
      <svg class="nav-icon" viewBox="0 0 20 20" fill="none"><path d="M10 4v12M4 10h12" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>
      <span>New Ticket</span>
    </a>

    <p class="nav-section-label admin-only">Manage</p>
    <a href="tickets.html?filter=unassigned" class="nav-link admin-only" data-page="assign">
      <svg class="nav-icon" viewBox="0 0 20 20" fill="none"><circle cx="7.5" cy="7" r="2.6" stroke="currentColor" stroke-width="1.6"/><path d="M2.8 16c.6-2.6 2.4-4 4.7-4s4.1 1.4 4.7 4M13.5 9.5h4M15.5 7.5v4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>
      <span>Assign Tickets</span>
    </a>
    <a href="reports.html" class="nav-link admin-only" data-page="reports">
      <svg class="nav-icon" viewBox="0 0 20 20" fill="none"><path d="M4 16V9m4 7V4m4 12v-5m4 5V7" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>
      <span>Reports</span>
    </a>
  </nav>

  <div class="sidebar-footer">
    <div class="sidebar-user">
      <span class="user-avatar" id="sidebarUserAvatar">?</span>
      <div class="user-meta">
        <span class="user-name" id="sidebarUserName">&nbsp;</span>
        <span class="user-role" id="sidebarUserRole">&nbsp;</span>
      </div>
    </div>
    <button type="button" class="logout-btn" id="logoutBtn" title="Log out">
      <svg viewBox="0 0 20 20" fill="none"><path d="M8 4H5a1 1 0 0 0-1 1v10a1 1 0 0 0 1 1h3M13 13l4-3-4-3M17 10H8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>
    </button>
  </div>
</aside>
<div class="sidebar-scrim" id="sidebarScrim"></div>`;
    }

    function navbarMarkup() {
        return `
<header class="app-navbar">
  <button type="button" class="sidebar-toggle" id="sidebarToggle" aria-label="Toggle navigation">
    <svg viewBox="0 0 20 20" fill="none"><path d="M3 5h14M3 10h14M3 15h14" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>
  </button>

  <h1 class="page-title" id="pageTitle">Dashboard</h1>

  <div class="navbar-actions">
    <div class="user-chip">
      <span class="user-avatar user-avatar--sm" id="navUserAvatar">?</span>
      <span class="user-chip-name" id="navUserName">Loading&hellip;</span>
    </div>
  </div>
</header>`;
    }

    function footerMarkup() {
        return `
<footer class="app-footer">
  <span>Ticketing Management System</span>
  <span class="footer-dot">&middot;</span>
  <span>Internal Support Tool</span>
</footer>`;
    }

    function loadLayout(activePage, pageTitle) {
        $("#includeSidebar").html(sidebarMarkup());
        $("#includeNavbar").html(navbarMarkup());
        $("#includeFooter").html(footerMarkup());

        $(`.nav-link[data-page="${activePage}"]`).addClass("active");
        if (pageTitle) $("#pageTitle").text(pageTitle);

        renderUserChrome();
        applyRoleVisibility();

        $("#logoutBtn").on("click", logout);
        $("#sidebarToggle").on("click", toggleSidebar);
        $("#sidebarScrim").on("click", closeSidebar);
    }

    function toggleSidebar() {
        $("#appSidebar").toggleClass("is-open");
        $("#sidebarScrim").toggleClass("is-open");
    }
    function closeSidebar() {
        $("#appSidebar").removeClass("is-open");
        $("#sidebarScrim").removeClass("is-open");
    }

    function renderUserChrome() {
        const user = Session.get();
        if (!user) return;
        const initialsText = initials(user.fullName || user.username);
        $("#sidebarUserAvatar, #navUserAvatar").text(initialsText);
        $("#sidebarUserName").text(user.fullName || user.username);
        $("#sidebarUserRole").text((user.role || "").toLowerCase());
        $("#navUserName").text(user.fullName || user.username);
    }

    function applyRoleVisibility() {
        const user = Session.get();
        const role = (user && user.role) || "USER";
        // .admin-only: Admin sees it, nobody else (e.g. Reports, Assign Tickets).
        if (role !== "ADMIN") {
            $(".admin-only").hide();
        }
        // .staff-only: Admin and Agent see it, plain Users don't (e.g. status
        // updates on a ticket) — per spec §4.1, agents can update status too.
        if (role !== "ADMIN" && role !== "AGENT") {
            $(".staff-only").hide();
        }
    }

    function logout() {
        ApiClient.post("api/auth/logout", {}).always(() => {
            Session.clear();
            window.location.href = "login.html";
        });
    }

    /* ---------------------------------------------------------------------
       Guard: pages under the authenticated shell redirect to login if no
       client-side session record exists. (Server-side AuthenticationFilter
       is the real gate — this just avoids a flash of empty UI.)
       --------------------------------------------------------------------- */
    function requireAuth() {
        if (!Session.get()) {
            window.location.href = "login.html";
        }
    }

    function showDemoBanner(container) {
        if ($("#" + DEMO_MODE_BANNER_ID).length) return;
        const banner = $(`
      <div id="${DEMO_MODE_BANNER_ID}" class="alert alert-warning" role="status">
        <span>Showing sample data &mdash; the backend API isn't reachable yet, so this view is populated with placeholder tickets for preview purposes.</span>
      </div>
    `);
        $(container).prepend(banner);
    }

    /* ---------------------------------------------------------------------
       Login page wiring
       --------------------------------------------------------------------- */
    function initLoginPage() {
        // If a session already exists client-side, skip straight to dashboard.
        if (Session.get()) {
            window.location.href = "dashboard.html";
            return;
        }

        $("#loginForm").on("submit", function (e) {
            e.preventDefault();

            const $form = $(this);
            const $btn = $("#loginSubmitBtn");
            const $spinner = $("#loginSpinner");
            const $alert = $("#loginAlert");

            const username = $("#username").val().trim();
            const password = $("#password").val();

            $alert.hide().text("");

            if (!username || !password) {
                $alert.text("Enter your username and password.").show();
                return;
            }

            $btn.prop("disabled", true);
            $spinner.show();

            ApiClient.post("api/auth/login", { username, password })
                .done((user) => {
                    Session.set(user || { username });
                    window.location.href = "dashboard.html";
                })
                .fail((xhr) => {
                    // Real backend responded with a structured error — trust it.
                    if (xhr.responseJSON && xhr.responseJSON.message) {
                        $alert.text(xhr.responseJSON.message).show();
                        $btn.prop("disabled", false);
                        $spinner.hide();
                        return;
                    }

                    if (isBackendUnavailable(xhr)) {
                        // No backend wired up yet — sign in locally so every page can
                        // still be previewed. Role is inferred from the username so
                        // all three roles (admin/agent/user) are easy to demo.
                        const lower = username.toLowerCase();
                        const role = lower.includes("admin") ? "ADMIN" : lower.includes("agent") ? "AGENT" : "USER";
                        const fullName = username.charAt(0).toUpperCase() + username.slice(1);
                        Session.set({ username, fullName, role });
                        window.location.href = "dashboard.html";
                        return;
                    }

                    $alert.text("Invalid username or password.").show();
                    $btn.prop("disabled", false);
                    $spinner.hide();
                });
        });
    }

    return {
        Session, ApiClient, isBackendUnavailable,
        escapeHtml, formatDate, initials,
        getPriorityBadge, getStatusBadge,
        loadLayout, requireAuth, showDemoBanner,
        initLoginPage, logout
    };
})();

$(function () {
    // Global AJAX 401 handler (belt-and-suspenders alongside ApiClient).
    $(document).ajaxError((event, xhr) => {
        if (xhr.status === 401 && !location.pathname.endsWith("login.html")) {
            App.Session.clear();
            window.location.href = "login.html";
        }
    });
});
