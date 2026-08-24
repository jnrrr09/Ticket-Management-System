/* ==========================================================================
   tickets.js — powers tickets.html (list, filter, search, paginate, create)
   ========================================================================== */

let currentPage = 1;
let itemsPerPage = 10;
let totalTickets = 0;
let lastKnownTickets = [];

const SAMPLE_TICKETS = [
    { ticketId: 1042, ticketNumber: "TK-2026-1042", title: "Login button unresponsive on Safari", categoryName: "Bug", priority: "CRITICAL", status: "OPEN", creatorName: "Ama Boateng", assigneeName: null, createdAt: "2026-08-22T09:14:00" },
    { ticketId: 1041, ticketNumber: "TK-2026-1041", title: "Add dark mode to dashboard", categoryName: "Feature Request", priority: "LOW", status: "OPEN", creatorName: "Kwame Owusu", assigneeName: "Nana Adjei", createdAt: "2026-08-21T15:02:00" },
    { ticketId: 1040, ticketNumber: "TK-2026-1040", title: "CSV export missing 'Assigned To' column", categoryName: "Bug", priority: "MEDIUM", status: "IN_PROGRESS", creatorName: "Efua Mensah", assigneeName: "Nana Adjei", createdAt: "2026-08-21T11:47:00" },
    { ticketId: 1039, ticketNumber: "TK-2026-1039", title: "Password reset email not arriving", categoryName: "Technical Issue", priority: "HIGH", status: "IN_PROGRESS", creatorName: "Yaw Darko", assigneeName: "Abena Sarpong", createdAt: "2026-08-20T08:30:00" },
    { ticketId: 1038, ticketNumber: "TK-2026-1038", title: "Clarify SSO rollout timeline", categoryName: "Support", priority: "LOW", status: "RESOLVED", creatorName: "Ama Boateng", assigneeName: "Abena Sarpong", createdAt: "2026-08-19T14:10:00" },
    { ticketId: 1037, ticketNumber: "TK-2026-1037", title: "Dashboard chart overlaps sidebar on tablet", categoryName: "Bug", priority: "MEDIUM", status: "CLOSED", creatorName: "Kwabena Asante", assigneeName: "Nana Adjei", createdAt: "2026-08-18T10:05:00" },
    { ticketId: 1036, ticketNumber: "TK-2026-1036", title: "Request bulk ticket assignment", categoryName: "Feature Request", priority: "MEDIUM", status: "OPEN", creatorName: "Efua Mensah", assigneeName: null, createdAt: "2026-08-17T16:40:00" }
];

$(function () {
    App.requireAuth();
    App.loadLayout("tickets", "Tickets");

    $("#statusFilter, #priorityFilter").on("change", () => loadTickets(1));
    $("#searchInput").on("keyup", filterVisibleRows);
    $("#newTicketBtn").on("click", openNewTicketModal);
    $("#newTicketForm").on("submit", submitNewTicket);
    $("#closeNewTicketModal, #cancelNewTicketBtn, #newTicketScrim").on("click", closeNewTicketModal);

    const params = new URLSearchParams(window.location.search);
    if (params.get("new") === "1") openNewTicketModal();
    if (params.get("filter") === "unassigned") $("#statusFilter").val("OPEN");

    loadTickets(1);
});

function loadTickets(page) {
    currentPage = page;
    const status = $("#statusFilter").val();
    const priority = $("#priorityFilter").val();
    const offset = (currentPage - 1) * itemsPerPage;

    renderSkeletonRows();

    App.ApiClient.get("api/tickets", { status, priority, offset, limit: itemsPerPage })
        .done((response) => {
            const tickets = Array.isArray(response) ? response : (response.tickets || []);
            totalTickets = Array.isArray(response) ? tickets.length : (response.total ?? tickets.length);
            lastKnownTickets = tickets;
            renderTickets(tickets);
            renderPagination();
        })
        .fail((xhr) => {
            if (xhr.status === 401) return; // handled globally, redirecting
            if (!App.isBackendUnavailable(xhr)) return; // real API error, nothing more to do
            // Backend not available yet — fall back to sample data for preview.
            App.showDemoBanner("#ticketsPageBody");
            let tickets = SAMPLE_TICKETS.filter(t =>
                (!status || t.status === status) && (!priority || t.priority === priority)
            );
            totalTickets = tickets.length;
            lastKnownTickets = tickets.slice(offset, offset + itemsPerPage);
            renderTickets(lastKnownTickets);
            renderPagination();
        });
}

function renderSkeletonRows() {
    const $tbody = $("#ticketsTableBody").empty();
    for (let i = 0; i < 5; i++) {
        $tbody.append(`
      <tr>
        <td colspan="8"><div class="skeleton" style="height:18px;width:${70 + Math.random()*25}%"></div></td>
      </tr>
    `);
    }
}

function renderTickets(tickets) {
    const $tbody = $("#ticketsTableBody").empty();

    if (!tickets || tickets.length === 0) {
        $("#ticketsEmptyState").show();
        $("#ticketsTableWrap").hide();
        return;
    }
    $("#ticketsEmptyState").hide();
    $("#ticketsTableWrap").show();

    tickets.forEach((ticket) => {
        const row = `
      <tr data-ticket-id="${ticket.ticketId}">
        <td><a class="ticket-id-link" href="ticket-detail.html?id=${ticket.ticketId}">${App.escapeHtml(ticket.ticketNumber)}</a></td>
        <td class="ticket-title-cell" title="${App.escapeHtml(ticket.title)}">${App.escapeHtml(ticket.title)}</td>
        <td class="cell-muted">${App.escapeHtml(ticket.categoryName || "\u2014")}</td>
        <td>${App.getPriorityBadge(ticket.priority)}</td>
        <td>${App.getStatusBadge(ticket.status)}</td>
        <td class="cell-muted">${App.escapeHtml(ticket.creatorName || "\u2014")}</td>
        <td class="cell-muted">${App.escapeHtml(ticket.assigneeName || "Unassigned")}</td>
        <td class="cell-muted">${App.formatDate(ticket.createdAt)}</td>
        <td><a class="btn btn-outline btn-sm" href="ticket-detail.html?id=${ticket.ticketId}">View</a></td>
      </tr>
    `;
        $tbody.append(row);
    });

    filterVisibleRows();
}

/** Client-side search over the currently loaded page, per spec §8.3
 *  ("Keyup listener on search box filters visible rows client-side"). */
function filterVisibleRows() {
    const query = $("#searchInput").val().trim().toLowerCase();
    $("#ticketsTableBody tr").each(function () {
        const text = $(this).text().toLowerCase();
        $(this).toggle(!query || text.includes(query));
    });
}

function renderPagination() {
    const totalPages = Math.max(1, Math.ceil(totalTickets / itemsPerPage));
    const start = totalTickets === 0 ? 0 : (currentPage - 1) * itemsPerPage + 1;
    const end = Math.min(currentPage * itemsPerPage, totalTickets);

    $("#paginationInfo").text(`Showing ${start}\u2013${end} of ${totalTickets} tickets`);

    const $controls = $("#paginationControls").empty();

    $controls.append(`<button class="page-btn" id="prevPageBtn" ${currentPage <= 1 ? "disabled" : ""} aria-label="Previous page">&lsaquo;</button>`);

    const windowSize = 5;
    let start_p = Math.max(1, currentPage - Math.floor(windowSize / 2));
    let end_p = Math.min(totalPages, start_p + windowSize - 1);
    start_p = Math.max(1, end_p - windowSize + 1);

    for (let p = start_p; p <= end_p; p++) {
        $controls.append(`<button class="page-btn ${p === currentPage ? "active" : ""}" data-page="${p}">${p}</button>`);
    }

    $controls.append(`<button class="page-btn" id="nextPageBtn" ${currentPage >= totalPages ? "disabled" : ""} aria-label="Next page">&rsaquo;</button>`);

    $("#prevPageBtn").on("click", () => loadTickets(currentPage - 1));
    $("#nextPageBtn").on("click", () => loadTickets(currentPage + 1));
    $controls.find("button[data-page]").on("click", function () {
        loadTickets(Number($(this).data("page")));
    });
}

/* ---------------------------------------------------------------------
   New ticket modal
   --------------------------------------------------------------------- */
function openNewTicketModal() {
    $("#newTicketAlert").hide();
    $("#newTicketForm")[0].reset();
    $("#newTicketModal").addClass("is-open");
    $("#ticketTitleInput").trigger("focus");
}
function closeNewTicketModal() {
    $("#newTicketModal").removeClass("is-open");
}

function submitNewTicket(e) {
    e.preventDefault();
    const $btn = $("#createTicketBtn");
    const $spinner = $("#createTicketSpinner");
    const $alert = $("#newTicketAlert").hide();

    const payload = {
        title: $("#ticketTitleInput").val().trim(),
        description: $("#ticketDescriptionInput").val().trim(),
        categoryId: $("#ticketCategoryInput").val(),
        priority: $("#ticketPriorityInput").val()
    };

    if (!payload.title || !payload.description) {
        $alert.text("Title and description are required.").show();
        return;
    }

    $btn.prop("disabled", true);
    $spinner.show();

    App.ApiClient.post("api/tickets", payload)
        .done(() => {
            closeNewTicketModal();
            loadTickets(1);
        })
        .fail((xhr) => {
            if (App.isBackendUnavailable(xhr)) {
                // No backend yet — reflect it optimistically so the flow is reviewable.
                SAMPLE_TICKETS.unshift({
                    ticketId: Date.now(),
                    ticketNumber: "TK-2026-" + Math.floor(1000 + Math.random() * 8999),
                    title: payload.title,
                    categoryName: $("#ticketCategoryInput option:selected").text(),
                    priority: payload.priority,
                    status: "OPEN",
                    creatorName: (App.Session.get() || {}).fullName || "You",
                    assigneeName: null,
                    createdAt: new Date().toISOString()
                });
                closeNewTicketModal();
                loadTickets(1);
                return;
            }
            const message = (xhr.responseJSON && xhr.responseJSON.message) || "Couldn't create the ticket. Try again.";
            $alert.text(message).show();
        })
        .always(() => {
            $btn.prop("disabled", false);
            $spinner.hide();
        });
}
