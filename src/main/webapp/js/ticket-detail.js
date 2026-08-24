/* ==========================================================================
   ticket-detail.js — powers ticket-detail.html
   ========================================================================== */

const SAMPLE_TICKET_DETAIL = {
    ticketId: 1042,
    ticketNumber: "TK-2026-1042",
    title: "Login button unresponsive on Safari",
    description: "Steps to reproduce:\n1. Open the login page in Safari 17 on macOS.\n2. Enter valid credentials.\n3. Click \"Sign in\".\n\nExpected: redirected to dashboard.\nActual: button shows the spinner indefinitely, no network request is sent. Works fine in Chrome and Firefox.",
    categoryName: "Bug",
    priority: "CRITICAL",
    status: "OPEN",
    creatorName: "Ama Boateng",
    assigneeName: null,
    createdAt: "2026-08-22T09:14:00",
    updatedAt: "2026-08-22T09:14:00",
    resolvedAt: null,
    history: [
        { type: "created", at: "2026-08-22T09:14:00", text: "Ama Boateng created this ticket" }
    ],
    comments: []
};

let currentTicketId = null;

$(function () {
    App.requireAuth();
    const layoutDeferred = App.loadLayout("tickets", "Ticket Detail");

    const params = new URLSearchParams(window.location.search);
    currentTicketId = params.get("id");

    if (!currentTicketId) {
        window.location.href = "tickets.html";
        return;
    }

    $("#statusUpdateForm").on("submit", submitStatusUpdate);
    $("#commentForm").on("submit", submitComment);

    loadTicket();
});

function loadTicket() {
    App.ApiClient.get(`api/tickets/${currentTicketId}`)
        .done((ticket) => renderTicket(ticket))
        .fail((xhr) => {
            if (xhr.status === 401) return;
            if (xhr.status === 404 && xhr.responseJSON) {
                // A real backend told us this ticket genuinely doesn't exist.
                $("#ticketDetailBody").html(`
          <div class="empty-state">
            <div class="empty-state-title">Ticket not found</div>
            <div class="empty-state-desc">It may have been removed, or the link is incorrect.</div>
            <a href="tickets.html" class="btn btn-outline btn-sm u-mt-lg">Back to tickets</a>
          </div>
        `);
                return;
            }
            // Backend not reachable — preview with sample data.
            App.showDemoBanner("#ticketDetailBody");
            renderTicket(Object.assign({}, SAMPLE_TICKET_DETAIL, { ticketId: currentTicketId }));
        });
}

function renderTicket(ticket) {
    $("#ticketNumberTag").text(ticket.ticketNumber);
    $("#ticketTitle").text(ticket.title);
    $("#ticketPriorityBadge").html(App.getPriorityBadge(ticket.priority));
    $("#ticketStatusBadge").html(App.getStatusBadge(ticket.status));
    $("#ticketDescription").text(ticket.description || "No description provided.");
    $("#pageTitle").text(ticket.ticketNumber);

    $("#metaCategory").text(ticket.categoryName || "\u2014");
    $("#metaCreatedBy").text(ticket.creatorName || "\u2014");
    $("#metaAssignedTo").text(ticket.assigneeName || "Unassigned");
    $("#metaCreatedAt").text(App.formatDate(ticket.createdAt));
    $("#metaUpdatedAt").text(App.formatDate(ticket.updatedAt));
    $("#metaResolvedAt").text(ticket.resolvedAt ? App.formatDate(ticket.resolvedAt) : "\u2014");

    $("#statusSelect").val(ticket.status);

    renderTimeline(ticket.history || [], ticket.comments || []);
}

function renderTimeline(history, comments) {
    const events = [
        ...history.map(h => ({ ...h, isComment: false })),
        ...comments.map(c => ({ at: c.createdAt, text: `${c.userName || "Someone"} commented: \u201c${c.text}\u201d`, isComment: true }))
    ].sort((a, b) => new Date(a.at) - new Date(b.at));

    const $timeline = $("#ticketTimeline").empty();

    if (events.length === 0) {
        $timeline.html('<li class="timeline-item"><div class="timeline-text cell-muted">No activity yet.</div></li>');
        return;
    }

    events.forEach((e) => {
        $timeline.append(`
      <li class="timeline-item ${e.isComment ? "is-comment" : ""}">
        <div class="timeline-time">${App.formatDate(e.at)}</div>
        <div class="timeline-text">${App.escapeHtml(e.text)}</div>
      </li>
    `);
    });
}

function submitStatusUpdate(e) {
    e.preventDefault();
    const newStatus = $("#statusSelect").val();
    const $btn = $("#updateStatusBtn");
    $btn.prop("disabled", true).find(".btn-spinner").show();

    App.ApiClient.put(`api/tickets/${currentTicketId}/status`, { status: newStatus })
        .done(() => {
            $("#ticketStatusBadge").html(App.getStatusBadge(newStatus));
            loadTicket();
        })
        .fail((xhr) => {
            if (App.isBackendUnavailable(xhr)) {
                // No backend — reflect the change locally so the flow is reviewable.
                $("#ticketStatusBadge").html(App.getStatusBadge(newStatus));
            }
        })
        .always(() => $btn.prop("disabled", false).find(".btn-spinner").hide());
}

function submitComment(e) {
    e.preventDefault();
    const text = $("#commentInput").val().trim();
    if (!text) return;

    const $btn = $("#addCommentBtn");
    $btn.prop("disabled", true);

    App.ApiClient.post(`api/tickets/${currentTicketId}/comments`, { text })
        .done(() => {
            $("#commentInput").val("");
            loadTicket();
        })
        .fail((xhr) => {
            if (App.isBackendUnavailable(xhr)) {
                const user = App.Session.get() || {};
                $("#ticketTimeline").append(`
          <li class="timeline-item is-comment">
            <div class="timeline-time">Just now</div>
            <div class="timeline-text">${App.escapeHtml(user.fullName || user.username || "You")} commented: \u201c${App.escapeHtml(text)}\u201d</div>
          </li>
        `);
                $("#commentInput").val("");
            }
        })
        .always(() => $btn.prop("disabled", false));
}
