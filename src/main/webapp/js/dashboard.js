/* ==========================================================================
   dashboard.js — powers dashboard.html. Renders different stat cards
   depending on role (Admin / Agent / User), per spec §4.2 Feature 3.
   ========================================================================== */

const SAMPLE_DASHBOARD = {
    ADMIN: {
        totalTickets: 128,
        openTickets: 34,
        resolvedToday: 9,
        overdueTickets: 6,
        workload: [
            { name: "Nana Adjei", count: 14 },
            { name: "Abena Sarpong", count: 11 },
            { name: "Kojo Mensah", count: 8 },
            { name: "Efua Boateng", count: 5 }
        ]
    },
    AGENT: { myAssigned: 12, overdue: 2, resolvedToday: 3 },
    USER: { myTotal: 7, myOpen: 3, myResolved: 4 }
};

$(function () {
    App.requireAuth();
    App.loadLayout("dashboard", "Dashboard");

    // Wait a tick for layout (and thus role info) to load before rendering.
    setTimeout(renderDashboard, 60);
});

function renderDashboard() {
    const user = App.Session.get() || {};
    const role = (user.role || "USER").toUpperCase();

    $("#welcomeName").text((user.fullName || user.username || "there").split(" ")[0]);

    App.ApiClient.get("api/dashboard", { role })
        .done((data) => buildStatCards(role, data))
        .fail((xhr) => {
            if (xhr.status === 401) return;
            if (!App.isBackendUnavailable(xhr)) return;
            App.showDemoBanner("#dashboardPageBody");
            buildStatCards(role, SAMPLE_DASHBOARD[role] || SAMPLE_DASHBOARD.USER);
        });
}

function buildStatCards(role, data) {
    const $grid = $("#statGrid").empty();
    data = data || {};

    const cards = {
        ADMIN: [
            { label: "Total Tickets", value: data.totalTickets, accent: "var(--primary-color)" },
            { label: "Open Tickets", value: data.openTickets, accent: "var(--status-open)" },
            { label: "Resolved Today", value: data.resolvedToday, accent: "var(--status-resolved)" },
            { label: "Overdue", value: data.overdueTickets, accent: "var(--priority-critical)" }
        ],
        AGENT: [
            { label: "My Assigned Tickets", value: data.myAssigned, accent: "var(--primary-color)" },
            { label: "Overdue", value: data.overdue, accent: "var(--priority-critical)" },
            { label: "Resolved Today", value: data.resolvedToday, accent: "var(--status-resolved)" }
        ],
        USER: [
            { label: "My Tickets", value: data.myTotal, accent: "var(--primary-color)" },
            { label: "Open", value: data.myOpen, accent: "var(--status-open)" },
            { label: "Resolved", value: data.myResolved, accent: "var(--status-resolved)" }
        ]
    };

    (cards[role] || cards.USER).forEach((c) => {
        $grid.append(`
      <div class="stat-card" style="--stat-accent:${c.accent}">
        <div class="stat-card-label"><span class="status-dot" style="color:${c.accent}"></span>${c.label}</div>
        <div class="stat-card-value">${c.value != null ? c.value : "\u2014"}</div>
      </div>
    `);
    });

    if (role === "ADMIN" && Array.isArray(data.workload)) {
        renderWorkloadChart(data.workload);
        $("#workloadCard").show();
    } else {
        $("#workloadCard").hide();
    }
}

function renderWorkloadChart(workload) {
    const $chart = $("#workloadChart").empty();
    const max = Math.max(1, ...workload.map(w => w.count));

    workload
        .sort((a, b) => b.count - a.count)
        .forEach((w) => {
            const pct = Math.round((w.count / max) * 100);
            $chart.append(`
        <div class="bar-row">
          <span class="bar-row-label" title="${App.escapeHtml(w.name)}">${App.escapeHtml(w.name)}</span>
          <span class="bar-track"><span class="bar-fill" style="width:${pct}%"></span></span>
          <span class="bar-row-value">${w.count}</span>
        </div>
      `);
        });
}
