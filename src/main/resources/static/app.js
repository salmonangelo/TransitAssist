/**
 * TransitAssist — Accessible Transit Assistance Booking System
 * Modern Vanilla JavaScript Frontend (No external frameworks)
 */
'use strict';

// Global Application State
const state = {
    users: [],
    helpers: [],
    requests: [],
    activeSection: 'dashboard',
    activeModal: null,
    selectedRequest: null
};

// ==========================================================================
// Utility & Helper Functions
// ==========================================================================

function esc(value) {
    if (value === null || value === undefined) return '';
    return String(value).replace(/[&<>"']/g, ch => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[ch]));
}

function $(id) {
    return document.getElementById(id);
}

function todayIso(offsetDays = 0) {
    const d = new Date();
    d.setDate(d.getDate() + offsetDays);
    const pad = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function formatTime(timeStr) {
    if (!timeStr) return '--:--';
    return String(timeStr).substring(0, 5);
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    if (parts.length !== 3) return dateStr;
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const month = months[parseInt(parts[1], 10) - 1] || parts[1];
    return `${parts[2]} ${month} ${parts[0]}`;
}

function formatDateTime(isoStr) {
    if (!isoStr) return '-';
    const d = new Date(isoStr);
    if (isNaN(d.getTime())) return isoStr;
    return d.toLocaleString([], { dateStyle: 'short', timeStyle: 'short' });
}

// ==========================================================================
// Toast Notification Center
// ==========================================================================

function showToast(message, type = 'info', title = null) {
    const container = $('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    let iconSvg = '';
    let defaultTitle = '';

    if (type === 'success') {
        defaultTitle = title || 'Success';
        iconSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>';
    } else if (type === 'error') {
        defaultTitle = title || 'Error';
        iconSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>';
    } else if (type === 'warning') {
        defaultTitle = title || 'Warning';
        iconSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>';
    } else {
        defaultTitle = title || 'Notification';
        iconSvg = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>';
    }

    toast.innerHTML = `
        <div class="toast-icon">${iconSvg}</div>
        <div class="toast-content">
            <div class="toast-title">${esc(defaultTitle)}</div>
            <div class="toast-msg">${esc(message)}</div>
        </div>
        <button class="toast-close" aria-label="Close notification">&times;</button>
    `;

    const closeBtn = toast.querySelector('.toast-close');
    closeBtn.onclick = () => removeToast(toast);

    container.appendChild(toast);

    // Auto dismiss after 4.5 seconds
    setTimeout(() => {
        removeToast(toast);
    }, 4500);
}

function removeToast(toast) {
    if (!toast || toast.classList.contains('hiding')) return;
    toast.classList.add('hiding');
    setTimeout(() => {
        if (toast.parentNode) {
            toast.parentNode.removeChild(toast);
        }
    }, 250);
}

// ==========================================================================
// REST API Client
// ==========================================================================

function formatError(body, status) {
    if (!body) {
        if (status === 409) {
            return 'Helper is already assigned to another request at the requested time (Conflict).';
        }
        return `Request failed with HTTP status ${status}.`;
    }

    if (body.errors && typeof body.errors === 'object') {
        const errorList = Object.values(body.errors).join(', ');
        return `${body.message || 'Validation failed'}: ${errorList}`;
    }

    if (body.message) {
        return body.message;
    }

    return `Server error (${status}).`;
}

async function api(path, options = {}) {
    const settings = Object.assign({
        headers: { 'Content-Type': 'application/json' }
    }, options);

    let response;
    try {
        response = await fetch('/api' + path, settings);
    } catch (netErr) {
        throw new Error('Cannot connect to Spring Boot server. Please verify the application is running.');
    }

    let body = null;
    if (response.status !== 204) {
        try {
            body = await response.json();
        } catch (parseErr) {
            body = null;
        }
    }

    if (!response.ok) {
        throw new Error(formatError(body, response.status));
    }

    return body;
}

// ==========================================================================
// Status & Badge Formatters
// ==========================================================================

function statusBadge(status) {
    const s = String(status || '').toUpperCase();
    let symbol = '';
    if (s === 'REQUESTED') symbol = '&#x23F1;';
    else if (s === 'ASSIGNED') symbol = '&#x25CF;';
    else if (s === 'COMPLETED') symbol = '&#x2713;';
    else if (s === 'CANCELLED') symbol = '&#x2715;';

    return `<span class="status-pill ${esc(s)}">${symbol} ${esc(s)}</span>`;
}

function assistanceTypeBadge(type) {
    const t = String(type || '').toUpperCase();
    let icon = '&#x267F;';
    let label = 'Wheelchair';

    if (t === 'ESCORT') {
        icon = '&#x1F441;';
        label = 'Escort';
    } else if (t === 'MOBILITY_SUPPORT') {
        icon = '&#x1F9AF;';
        label = 'Mobility Support';
    }

    return `<span class="type-pill ${esc(t)}">${icon} ${esc(label)}</span>`;
}

function roleBadge(role) {
    const r = String(role || 'USER').toUpperCase();
    return `<span class="role-pill ${esc(r)}">${esc(r)}</span>`;
}

function yesNoBadge(flag) {
    return flag
        ? `<span class="status-pill COMPLETED">&#x2713; Active</span>`
        : `<span class="status-pill CANCELLED">&#x2715; Inactive</span>`;
}

function availabilityBadge(flag) {
    return flag
        ? `<span class="status-pill COMPLETED">&#x25CF; Available</span>`
        : `<span class="status-pill REQUESTED">&#x25CB; Unavailable</span>`;
}

// ==========================================================================
// Data Fetching & Sync
// ==========================================================================

async function loadAll() {
    try {
        const [users, helpers, requests] = await Promise.all([
            api('/users'),
            api('/helpers'),
            api('/assistance-requests')
        ]);

        state.users = users || [];
        state.helpers = helpers || [];
        state.requests = requests || [];

        renderAll();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

function renderAll() {
    renderStats();
    renderDashboardActivity();
    renderRequests();
    renderHelpers();
    renderUsers();
    renderSelects();
    updatePendingBadge();
}

function updatePendingBadge() {
    const pendingCount = state.requests.filter(r => r.status === 'REQUESTED').length;
    const badge = $('nav-pending-badge');
    if (badge) {
        badge.textContent = pendingCount;
        badge.style.display = pendingCount > 0 ? 'inline-block' : 'none';
    }
}

// ==========================================================================
// 1. Dashboard View
// ==========================================================================

function renderStats() {
    const all = state.requests;
    const count = s => all.filter(r => r.status === s).length;

    const totalUsers = state.users.length;
    const activeHelpers = state.helpers.filter(h => h.active).length;
    const availHelpers = state.helpers.filter(h => h.active && h.available).length;
    const requestedCount = count('REQUESTED');
    const assignedCount = count('ASSIGNED');
    const completedCount = count('COMPLETED');
    const cancelledCount = count('CANCELLED');

    // Update Dashboard KPI Values
    if ($('stat-users')) $('stat-users').textContent = totalUsers;
    if ($('stat-helpers')) $('stat-helpers').textContent = activeHelpers;
    if ($('stat-available-helpers')) $('stat-available-helpers').textContent = `● ${availHelpers} Available`;
    if ($('stat-requested')) $('stat-requested').textContent = requestedCount;
    if ($('stat-assigned')) $('stat-assigned').textContent = assignedCount;
    if ($('stat-completed')) $('stat-completed').textContent = completedCount;
    if ($('stat-cancelled')) $('stat-cancelled').textContent = cancelledCount;

    // Helper Section KPIs
    if ($('stat-helpers-total')) $('stat-helpers-total').textContent = state.helpers.length;
    if ($('stat-helpers-avail')) $('stat-helpers-avail').textContent = availHelpers;
    if ($('stat-helpers-active')) $('stat-helpers-active').textContent = activeHelpers;
    if ($('stat-helpers-inactive')) $('stat-helpers-inactive').textContent = state.helpers.length - activeHelpers;

    // User Section KPIs
    if ($('stat-users-total')) $('stat-users-total').textContent = totalUsers;
    if ($('stat-users-students')) $('stat-users-students').textContent = state.users.filter(u => u.role === 'USER').length;
    if ($('stat-users-staff')) $('stat-users-staff').textContent = state.users.filter(u => u.role === 'STAFF').length;
    if ($('stat-users-admins')) $('stat-users-admins').textContent = state.users.filter(u => u.role === 'ADMIN').length;

    // Visual Multi-segment Progress Bar
    const totalRequests = all.length || 1;
    const reqPct = Math.round((requestedCount / totalRequests) * 100);
    const assignPct = Math.round((assignedCount / totalRequests) * 100);
    const compPct = Math.round((completedCount / totalRequests) * 100);
    const cancPct = Math.round((cancelledCount / totalRequests) * 100);

    const barsContainer = $('status-bars-container');
    if (barsContainer) {
        barsContainer.innerHTML = `
            <div class="progress-track-multi">
                <div class="progress-bar-segment REQUESTED" style="width: ${reqPct}%" title="Requested: ${requestedCount} (${reqPct}%)"></div>
                <div class="progress-bar-segment ASSIGNED" style="width: ${assignPct}%" title="Assigned: ${assignedCount} (${assignPct}%)"></div>
                <div class="progress-bar-segment COMPLETED" style="width: ${compPct}%" title="Completed: ${completedCount} (${compPct}%)"></div>
                <div class="progress-bar-segment CANCELLED" style="width: ${cancPct}%" title="Cancelled: ${cancelledCount} (${cancPct}%)"></div>
            </div>
        `;
    }

    const legendContainer = $('status-chart-legend');
    if (legendContainer) {
        legendContainer.innerHTML = `
            <div class="legend-item">
                <span class="legend-dot" style="background-color: var(--warning);"></span>
                <span>Requested</span>
                <span class="legend-val">${requestedCount} (${reqPct}%)</span>
            </div>
            <div class="legend-item">
                <span class="legend-dot" style="background-color: var(--primary);"></span>
                <span>Assigned</span>
                <span class="legend-val">${assignedCount} (${assignPct}%)</span>
            </div>
            <div class="legend-item">
                <span class="legend-dot" style="background-color: var(--success);"></span>
                <span>Completed</span>
                <span class="legend-val">${completedCount} (${compPct}%)</span>
            </div>
            <div class="legend-item">
                <span class="legend-dot" style="background-color: #94a3b8;"></span>
                <span>Cancelled</span>
                <span class="legend-val">${cancelledCount} (${cancPct}%)</span>
            </div>
        `;
    }

    // Assistance Type Breakdown List
    const typeCount = t => all.filter(r => r.assistanceType === t).length;
    const wheelchairCount = typeCount('WHEELCHAIR');
    const escortCount = typeCount('ESCORT');
    const mobilityCount = typeCount('MOBILITY_SUPPORT');

    const typesContainer = $('types-breakdown-list');
    if (typesContainer) {
        typesContainer.innerHTML = `
            <div class="type-stat-row">
                <div class="type-stat-label">
                    <span class="type-icon primary" style="width: 28px; height: 28px; font-size: 0.8rem;">&#x267F;</span>
                    <span>Wheelchair Assistance</span>
                </div>
                <div class="type-stat-count">${wheelchairCount} bookings</div>
            </div>
            <div class="type-stat-row">
                <div class="type-stat-label">
                    <span class="type-icon info" style="width: 28px; height: 28px; font-size: 0.8rem;">&#x1F441;</span>
                    <span>Escort & Guiding Support</span>
                </div>
                <div class="type-stat-count">${escortCount} bookings</div>
            </div>
            <div class="type-stat-row">
                <div class="type-stat-label">
                    <span class="type-icon success" style="width: 28px; height: 28px; font-size: 0.8rem;">&#x1F9AF;</span>
                    <span>Mobility Aid Support</span>
                </div>
                <div class="type-stat-count">${mobilityCount} bookings</div>
            </div>
        `;
    }
}

function renderDashboardActivity() {
    const tbody = $('dash-recent-table-body');
    if (!tbody) return;

    // Show upcoming or recent 6 requests
    const recent = [...state.requests].slice(0, 6);

    if (recent.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">No recent activity found.</td></tr>';
        return;
    }

    tbody.innerHTML = recent.map(r => `
        <tr>
            <td><span class="id-pill">#${r.id}</span></td>
            <td>
                <div class="cell-requester">
                    <div class="avatar" style="width: 28px; height: 28px; font-size: 0.75rem;">${esc(getInitials(r.userName))}</div>
                    <div>
                        <span class="requester-name">${esc(r.userName)}</span>
                    </div>
                </div>
            </td>
            <td>
                <div class="cell-route">
                    <span>${esc(r.pickupPoint)}</span>
                    <span class="route-arrow">&rarr;</span>
                    <span>${esc(r.destination)}</span>
                </div>
            </td>
            <td>
                <div class="cell-time">
                    <span class="time-val">${formatTime(r.tripTime)}</span>
                    <span class="date-val">${formatDate(r.tripDate)}</span>
                </div>
            </td>
            <td>${assistanceTypeBadge(r.assistanceType)}</td>
            <td>
                ${r.helperName ? `<span class="text-normal font-medium">&#x1F464; ${esc(r.helperName)}</span>` : '<span class="text-muted font-normal"><em>Unassigned</em></span>'}
            </td>
            <td>${statusBadge(r.status)}</td>
            <td class="text-right">
                <button class="btn btn-outline btn-xs" onclick="openRequestDetailsModal(${r.id})">
                    Details
                </button>
            </td>
        </tr>
    `).join('');
}

// ==========================================================================
// 2. Requests View & Filters
// ==========================================================================

function renderRequests() {
    const tbody = $('requests-body');
    const emptyState = $('requests-empty-state');
    if (!tbody) return;

    const searchTerm = ($('requests-search') ? $('requests-search').value : '').toLowerCase().trim();
    const statusFilter = ($('status-filter') ? $('status-filter').value : '');
    const typeFilter = ($('type-filter') ? $('type-filter').value : '');
    const dateFilter = ($('date-filter') ? $('date-filter').value : '');

    let filtered = state.requests.filter(r => {
        if (statusFilter && r.status !== statusFilter) return false;
        if (typeFilter && r.assistanceType !== typeFilter) return false;
        if (dateFilter && r.tripDate !== dateFilter) return false;
        if (searchTerm) {
            const matchUser = (r.userName || '').toLowerCase().includes(searchTerm);
            const matchHelper = (r.helperName || '').toLowerCase().includes(searchTerm);
            const matchPickup = (r.pickupPoint || '').toLowerCase().includes(searchTerm);
            const matchDest = (r.destination || '').toLowerCase().includes(searchTerm);
            const matchNotes = (r.additionalNotes || '').toLowerCase().includes(searchTerm);
            const matchId = String(r.id) === searchTerm;
            if (!matchUser && !matchHelper && !matchPickup && !matchDest && !matchNotes && !matchId) {
                return false;
            }
        }
        return true;
    });

    if (filtered.length === 0) {
        tbody.innerHTML = '';
        if (emptyState) emptyState.classList.remove('hidden');
        return;
    }

    if (emptyState) emptyState.classList.add('hidden');

    tbody.innerHTML = filtered.map(r => {
        const isRequested = r.status === 'REQUESTED';
        const isAssigned = r.status === 'ASSIGNED';
        const isCompleted = r.status === 'COMPLETED';
        const isCancelled = r.status === 'CANCELLED';

        // Build valid action buttons according to status rules
        let actionButtons = `
            <button class="btn btn-outline btn-xs" onclick="openRequestDetailsModal(${r.id})" title="View full details and timeline">
                Details
            </button>
        `;

        if (isRequested || isAssigned) {
            actionButtons += `
                <button class="btn btn-primary btn-xs" onclick="openRequestDetailsModal(${r.id}, true)" title="Assign helper">
                    ${isAssigned ? 'Reassign' : 'Assign'}
                </button>
            `;
        }

        if (isAssigned) {
            actionButtons += `
                <button class="btn btn-success btn-xs" onclick="handleCompleteRequest(${r.id})" title="Mark trip completed">
                    &#x2713; Complete
                </button>
            `;
        }

        if (isRequested || isAssigned) {
            actionButtons += `
                <button class="btn btn-danger btn-xs" onclick="handleCancelRequest(${r.id})" title="Cancel request">
                    &#x2715; Cancel
                </button>
            `;
        }

        return `
            <tr>
                <td><span class="id-pill">#${r.id}</span></td>
                <td>
                    <div class="cell-requester">
                        <div class="avatar" style="width: 32px; height: 32px; font-size: 0.8rem;">${esc(getInitials(r.userName))}</div>
                        <div>
                            <span class="requester-name">${esc(r.userName)}</span>
                            <span class="requester-meta">User ID: #${r.userId}</span>
                        </div>
                    </div>
                </td>
                <td>
                    <div class="cell-route">
                        <span>${esc(r.pickupPoint)}</span>
                        <span class="route-arrow">&rarr;</span>
                        <span>${esc(r.destination)}</span>
                    </div>
                </td>
                <td>
                    <div class="cell-time">
                        <span class="time-val">${formatTime(r.tripTime)}</span>
                        <span class="date-val">${formatDate(r.tripDate)}</span>
                    </div>
                </td>
                <td>${assistanceTypeBadge(r.assistanceType)}</td>
                <td>
                    ${r.helperName 
                        ? `<span class="text-normal font-medium">&#x1F464; ${esc(r.helperName)}</span>` 
                        : '<span class="status-pill CANCELLED" style="font-size: 0.7rem;">Unassigned</span>'}
                </td>
                <td>${statusBadge(r.status)}</td>
                <td class="text-right">
                    <div class="actions-cell">
                        ${actionButtons}
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function filterRequestsByStatus(status) {
    showSection('requests');
    const select = $('status-filter');
    if (select) {
        select.value = status;
        renderRequests();
    }
}

// ==========================================================================
// 3. Request Details Modal & Status Stepper
// ==========================================================================

function openRequestDetailsModal(requestId, focusAssign = false) {
    const req = state.requests.find(r => r.id === requestId);
    if (!req) return;

    state.selectedRequest = req;

    // Header & Tags
    $('modal-req-id-badge').textContent = `Request #${req.id}`;
    $('modal-req-user').textContent = `${req.userName} (User ID #${req.userId})`;
    $('modal-req-type').innerHTML = assistanceTypeBadge(req.assistanceType);
    $('modal-req-route').textContent = `${req.pickupPoint}  →  ${req.destination}`;
    $('modal-req-schedule').textContent = `${formatDate(req.tripDate)} at ${formatTime(req.tripTime)}`;
    $('modal-req-helper').innerHTML = req.helperName 
        ? `<strong>&#x1F464; ${esc(req.helperName)}</strong> (Helper ID #${req.helperId})` 
        : '<em class="text-muted">None assigned yet</em>';
    $('modal-req-status-badge').innerHTML = statusBadge(req.status);
    $('modal-req-notes').textContent = req.additionalNotes ? req.additionalNotes : 'No additional accessibility notes provided.';
    $('modal-req-created').textContent = formatDateTime(req.createdAt);
    $('modal-req-updated').textContent = formatDateTime(req.updatedAt);

    // Visual Stepper Progress Bar
    const s1 = $('step-node-1');
    const s2 = $('step-node-2');
    const s3 = $('step-node-3');
    const l1 = $('step-line-1');
    const l2 = $('step-line-2');
    const cancelBanner = $('cancelled-alert');

    // Reset classes
    [s1, s2, s3].forEach(node => node.className = 'step-node');
    [l1, l2].forEach(line => line.className = 'step-line');
    cancelBanner.classList.add('hidden');

    if (req.status === 'REQUESTED') {
        s1.classList.add('active');
    } else if (req.status === 'ASSIGNED') {
        s1.classList.add('completed');
        l1.classList.add('filled');
        s2.classList.add('active');
    } else if (req.status === 'COMPLETED') {
        s1.classList.add('completed');
        l1.classList.add('filled');
        s2.classList.add('completed');
        l2.classList.add('filled');
        s3.classList.add('completed');
    } else if (req.status === 'CANCELLED') {
        s1.classList.add('completed');
        cancelBanner.classList.remove('hidden');
    }

    // Modal Helper Assign Dropdown
    const helperSelect = $('modal-helper-select');
    const assignSection = $('modal-assign-section');
    const isTerminal = req.status === 'COMPLETED' || req.status === 'CANCELLED';

    if (isTerminal) {
        assignSection.style.display = 'none';
    } else {
        assignSection.style.display = 'block';
        helperSelect.innerHTML = helperOptions(req.helperId);
    }

    // Action button inside modal footer
    const footer = $('modal-details-actions');
    let footerButtons = `<button type="button" class="btn btn-outline" onclick="closeActiveModal()">Close</button>`;

    if (req.status === 'ASSIGNED') {
        footerButtons += `
            <button type="button" class="btn btn-success" onclick="handleCompleteRequest(${req.id})">&#x2713; Mark Completed</button>
        `;
    }

    if (req.status === 'REQUESTED' || req.status === 'ASSIGNED') {
        footerButtons += `
            <button type="button" class="btn btn-danger" onclick="handleCancelRequest(${req.id})">&#x2715; Cancel Request</button>
        `;
    }

    footer.innerHTML = footerButtons;

    openModal('modal-request-details');

    if (focusAssign && !isTerminal) {
        helperSelect.focus();
    }
}

// Modal Assign Button Trigger
$('modal-assign-btn').addEventListener('click', async () => {
    if (!state.selectedRequest) return;
    const reqId = state.selectedRequest.id;
    const helperId = $('modal-helper-select').value;
    if (!helperId) {
        showToast('Please select a helper to assign.', 'warning');
        return;
    }

    const btn = $('modal-assign-btn');
    const originalText = btn.textContent;
    btn.disabled = true;
    btn.textContent = 'Assigning...';

    try {
        await api(`/assistance-requests/${reqId}/assign/${helperId}`, { method: 'POST' });
        showToast(`Helper assigned to Request #${reqId} successfully!`, 'success');
        closeActiveModal();
        await loadAll();
    } catch (err) {
        showToast(err.message, 'error', 'Assignment Failed');
    } finally {
        btn.disabled = false;
        btn.textContent = originalText;
    }
});

// ==========================================================================
// 4. Action Handlers (Complete, Cancel, Deactivate)
// ==========================================================================

async function handleCompleteRequest(requestId) {
    try {
        await api(`/assistance-requests/${requestId}/complete`, { method: 'POST' });
        showToast(`Request #${requestId} has been marked as COMPLETED.`, 'success');
        closeActiveModal();
        await loadAll();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function handleCancelRequest(requestId) {
    if (!confirm(`Are you sure you want to cancel Assistance Request #${requestId}?`)) {
        return;
    }

    try {
        await api(`/assistance-requests/${requestId}/cancel`, { method: 'POST' });
        showToast(`Request #${requestId} was cancelled.`, 'info');
        closeActiveModal();
        await loadAll();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function handleDeactivateHelper(helperId) {
    const helper = state.helpers.find(h => h.id === helperId);
    const name = helper ? helper.name : `#${helperId}`;

    if (!confirm(`Deactivate helper "${name}"? They will no longer be available for dispatch assignments.`)) {
        return;
    }

    try {
        await api(`/helpers/${helperId}`, { method: 'DELETE' });
        showToast(`Helper "${name}" deactivated.`, 'info');
        await loadAll();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function handleDeactivateUser(userId) {
    const user = state.users.find(u => u.id === userId);
    const name = user ? user.name : `#${userId}`;

    if (!confirm(`Deactivate user account for "${name}"?`)) {
        return;
    }

    try {
        await api(`/users/${userId}`, { method: 'DELETE' });
        showToast(`User account "${name}" deactivated.`, 'info');
        await loadAll();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==========================================================================
// 5. Helpers Section & Modal
// ==========================================================================

function renderHelpers() {
    const tbody = $('helpers-body');
    if (!tbody) return;

    const searchTerm = ($('helpers-search') ? $('helpers-search').value : '').toLowerCase().trim();

    let filtered = state.helpers.filter(h => {
        if (searchTerm) {
            const matchName = (h.name || '').toLowerCase().includes(searchTerm);
            const matchEmail = (h.email || '').toLowerCase().includes(searchTerm);
            const matchPhone = (h.phone || '').toLowerCase().includes(searchTerm);
            const matchSkills = (h.assistanceSkills || []).join(' ').toLowerCase().includes(searchTerm);
            if (!matchName && !matchEmail && !matchPhone && !matchSkills) return false;
        }
        return true;
    });

    if (filtered.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">No helpers found matching search.</td></tr>';
        return;
    }

    tbody.innerHTML = filtered.map(h => {
        const skillsBadges = (h.assistanceSkills || [])
            .map(s => assistanceTypeBadge(s))
            .join(' ');

        return `
            <tr>
                <td><span class="id-pill">#${h.id}</span></td>
                <td>
                    <div class="cell-requester">
                        <div class="avatar" style="width: 32px; height: 32px; font-size: 0.8rem; background: linear-gradient(135deg, #10b981 0%, #059669 100%);">
                            ${esc(getInitials(h.name))}
                        </div>
                        <div>
                            <span class="requester-name">${esc(h.name)}</span>
                            <span class="requester-meta">Joined ${formatDate(String(h.createdAt || '').substring(0, 10))}</span>
                        </div>
                    </div>
                </td>
                <td>
                    <div>
                        <div>${esc(h.email)}</div>
                        <div class="text-muted text-xs">${h.phone ? esc(h.phone) : 'No phone listed'}</div>
                    </div>
                </td>
                <td>${skillsBadges || '<span class="text-muted">None</span>'}</td>
                <td>${availabilityBadge(h.available)}</td>
                <td>${yesNoBadge(h.active)}</td>
                <td class="text-right">
                    <div class="actions-cell">
                        <button class="btn btn-outline btn-xs" onclick="inspectHelperWorkload(${h.id})" title="View helper workload timeline">
                            Workload
                        </button>
                        ${h.active ? `
                            <button class="btn btn-danger btn-xs" onclick="handleDeactivateHelper(${h.id})" title="Deactivate helper">
                                Deactivate
                            </button>
                        ` : ''}
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function openAddHelperModal() {
    $('helper-form').reset();
    openModal('modal-add-helper');
}

$('helper-form').addEventListener('submit', async event => {
    event.preventDefault();
    const skills = Array.from(document.querySelectorAll('input[name="skill"]:checked')).map(c => c.value);

    if (skills.length === 0) {
        showToast('Please select at least one certified assistance skill.', 'warning');
        return;
    }

    const payload = {
        name: $('helper-name').value.trim(),
        email: $('helper-email').value.trim(),
        phone: $('helper-phone').value.trim() || null,
        assistanceSkills: skills,
        available: true
    };

    const btn = $('save-helper-btn');
    btn.disabled = true;
    btn.textContent = 'Saving...';

    try {
        await api('/helpers', { method: 'POST', body: JSON.stringify(payload) });
        showToast(`Helper "${payload.name}" registered successfully!`, 'success');
        closeActiveModal();
        await loadAll();
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Save Helper';
    }
});

// ==========================================================================
// 6. Workload Section & Visual Timeline
// ==========================================================================

function inspectHelperWorkload(helperId) {
    showSection('workload');
    $('wl-helper').value = helperId;
    fetchWorkloadSchedule();
}

function setWorkloadDate(offsetDays) {
    $('wl-date').value = todayIso(offsetDays);
    document.querySelectorAll('.quick-dates-bar button').forEach((b, idx) => {
        b.classList.toggle('active-date', idx === offsetDays);
    });
    fetchWorkloadSchedule();
}

async function fetchWorkloadSchedule() {
    const helperId = $('wl-helper').value;
    const dateStr = $('wl-date').value;

    if (!helperId || !dateStr) return;

    const btn = $('wl-fetch-btn');
    if (btn) {
        btn.disabled = true;
        btn.textContent = 'Loading...';
    }

    try {
        const data = await api(`/helpers/${helperId}/workload?date=${dateStr}`);
        renderWorkloadResult(data);
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.textContent = 'View Schedule';
        }
    }
}

function renderWorkloadResult(data) {
    $('wl-summary-helper-name').textContent = `${data.helperName} Schedule`;
    $('wl-summary-date-desc').textContent = `Schedule for ${formatDate(data.date)}`;
    $('wl-helper-avatar').textContent = getInitials(data.helperName);

    $('wl-total-count').textContent = data.totalRequests;
    $('wl-assigned-count').textContent = data.assignedCount;
    $('wl-completed-count').textContent = data.completedCount;

    // Build Visual Hourly Timeline Slots
    const timelineGrid = $('timeline-slots-grid');
    const bookings = data.requests || [];

    if (bookings.length === 0) {
        timelineGrid.innerHTML = `
            <div class="card p-3 text-muted text-center" style="grid-column: 1 / -1; background-color: #f8fafc;">
                No bookings scheduled for ${data.helperName} on ${formatDate(data.date)}. This helper is fully available.
            </div>
        `;
    } else {
        timelineGrid.innerHTML = bookings.map(r => `
            <div class="timeline-slot-card ${r.status}">
                <div class="slot-time">&#x23F0; ${formatTime(r.tripTime)} &ndash; ${formatEndTime(r.tripTime)} (60 min)</div>
                <div class="slot-route">${esc(r.pickupPoint)} &rarr; ${esc(r.destination)}</div>
                <div class="slot-meta">Requester: ${esc(r.userName)}</div>
                <div class="mt-2">${statusBadge(r.status)}</div>
            </div>
        `).join('');
    }

    // Workload Detailed Table
    const tbody = $('wl-body');
    if (bookings.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">No requests found for this day.</td></tr>';
    } else {
        tbody.innerHTML = bookings.map(r => `
            <tr>
                <td><span class="id-pill">#${r.id}</span></td>
                <td><span class="font-medium">${formatTime(r.tripTime)}</span></td>
                <td>${esc(r.userName)}</td>
                <td>${esc(r.pickupPoint)} &rarr; ${esc(r.destination)}</td>
                <td>${assistanceTypeBadge(r.assistanceType)}</td>
                <td>${statusBadge(r.status)}</td>
            </tr>
        `).join('');
    }
}

function formatEndTime(timeStr) {
    if (!timeStr) return '';
    const parts = timeStr.split(':');
    let h = parseInt(parts[0], 10) + 1;
    return `${String(h).padStart(2, '0')}:${parts[1] || '00'}`;
}

$('workload-form').addEventListener('submit', event => {
    event.preventDefault();
    fetchWorkloadSchedule();
});

// ==========================================================================
// 7. Users Section & Modal
// ==========================================================================

function renderUsers() {
    const tbody = $('users-body');
    if (!tbody) return;

    const searchTerm = ($('users-search') ? $('users-search').value : '').toLowerCase().trim();

    let filtered = state.users.filter(u => {
        if (searchTerm) {
            const matchName = (u.name || '').toLowerCase().includes(searchTerm);
            const matchEmail = (u.email || '').toLowerCase().includes(searchTerm);
            const matchPhone = (u.phone || '').toLowerCase().includes(searchTerm);
            const matchRole = (u.role || '').toLowerCase().includes(searchTerm);
            if (!matchName && !matchEmail && !matchPhone && !matchRole) return false;
        }
        return true;
    });

    if (filtered.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">No campus users found.</td></tr>';
        return;
    }

    tbody.innerHTML = filtered.map(u => `
        <tr>
            <td><span class="id-pill">#${u.id}</span></td>
            <td>
                <div class="cell-requester">
                    <div class="avatar" style="width: 32px; height: 32px; font-size: 0.8rem;">
                        ${esc(getInitials(u.name))}
                    </div>
                    <div>
                        <span class="requester-name">${esc(u.name)}</span>
                        <span class="requester-meta">Registered ${formatDate(String(u.createdAt || '').substring(0, 10))}</span>
                    </div>
                </div>
            </td>
            <td>${esc(u.email)}</td>
            <td>${u.phone ? esc(u.phone) : '<span class="text-muted text-xs">None</span>'}</td>
            <td>${roleBadge(u.role)}</td>
            <td>${yesNoBadge(u.active)}</td>
            <td class="text-right">
                ${u.active ? `
                    <button class="btn btn-outline btn-xs" onclick="handleDeactivateUser(${u.id})">
                        Deactivate
                    </button>
                ` : ''}
            </td>
        </tr>
    `).join('');
}

function openAddUserModal() {
    $('user-form').reset();
    openModal('modal-add-user');
}

$('user-form').addEventListener('submit', async event => {
    event.preventDefault();
    const payload = {
        name: $('user-name').value.trim(),
        email: $('user-email').value.trim(),
        phone: $('user-phone').value.trim() || null,
        role: $('user-role').value
    };

    const btn = $('save-user-btn');
    btn.disabled = true;
    btn.textContent = 'Creating...';

    try {
        await api('/users', { method: 'POST', body: JSON.stringify(payload) });
        showToast(`User account "${payload.name}" created!`, 'success');
        closeActiveModal();
        await loadAll();
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Create User';
    }
});

// ==========================================================================
// 8. New Request Form & Location Presets
// ==========================================================================

function setQuickLocation(pickup, dest) {
    $('req-pickup').value = pickup;
    $('req-destination').value = dest;
    showToast(`Route set: ${pickup} → ${dest}`, 'info');
}

// Assistance Type Radio Cards Toggle
document.querySelectorAll('.type-radio-card').forEach(card => {
    card.addEventListener('click', () => {
        document.querySelectorAll('.type-radio-card').forEach(c => c.classList.remove('active'));
        card.classList.add('active');
        const radio = card.querySelector('input[type="radio"]');
        if (radio) radio.checked = true;
    });
});

$('request-form').addEventListener('submit', async event => {
    event.preventDefault();

    const checkedRadio = document.querySelector('input[name="assistanceType"]:checked');
    const assistanceType = checkedRadio ? checkedRadio.value : 'WHEELCHAIR';

    const payload = {
        userId: Number($('req-user').value),
        pickupPoint: $('req-pickup').value.trim(),
        destination: $('req-destination').value.trim(),
        tripDate: $('req-date').value,
        tripTime: $('req-time').value,
        assistanceType: assistanceType,
        additionalNotes: $('req-notes').value.trim() || null
    };

    if (!payload.userId) {
        showToast('Please select a requester user.', 'warning');
        return;
    }
    if (!payload.pickupPoint || !payload.destination) {
        showToast('Please specify pickup point and destination.', 'warning');
        return;
    }
    if (!payload.tripDate || !payload.tripTime) {
        showToast('Please select trip date and time.', 'warning');
        return;
    }

    const btn = $('submit-request-btn');
    btn.disabled = true;
    btn.textContent = 'Booking Transit Assistance...';

    try {
        const created = await api('/assistance-requests', { method: 'POST', body: JSON.stringify(payload) });
        showToast(`Request #${created.id} submitted successfully! Status: REQUESTED`, 'success', 'Booking Confirmed');
        
        $('request-form').reset();
        $('req-date').value = todayIso();
        $('req-time').value = '10:00';
        
        showSection('requests');
        await loadAll();
    } catch (err) {
        showToast(err.message, 'error', 'Booking Failed');
    } finally {
        btn.disabled = false;
        btn.innerHTML = `
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
            Confirm &amp; Book Assistance
        `;
    }
});

// ==========================================================================
// 9. Select Options Helpers
// ==========================================================================

function helperOptions(selectedId) {
    let options = '<option value="">-- Choose available helper --</option>';
    options += state.helpers.map(h => {
        const flags = (!h.active ? ' (inactive)' : (!h.available ? ' (unavailable)' : ''));
        const isSelected = h.id === selectedId ? ' selected' : '';
        const skills = (h.assistanceSkills || []).join(', ');
        return `<option value="${h.id}"${isSelected}>${esc(h.name)} [${esc(skills)}]${flags}</option>`;
    }).join('');
    return options;
}

function renderSelects() {
    const userSelect = $('req-user');
    if (userSelect) {
        const previousUser = userSelect.value;
        const activeUsers = state.users.filter(u => u.active);
        userSelect.innerHTML = activeUsers.map(u => 
            `<option value="${u.id}">${esc(u.name)} (${esc(u.role)} &bull; ${esc(u.email)})</option>`
        ).join('');
        if (previousUser) {
            userSelect.value = previousUser;
        }
    }

    const helperSelect = $('wl-helper');
    if (helperSelect) {
        const prevHelper = helperSelect.value;
        helperSelect.innerHTML = state.helpers.map(h => 
            `<option value="${h.id}">${esc(h.name)} (${(h.assistanceSkills || []).join(', ')})</option>`
        ).join('');
        if (prevHelper) {
            helperSelect.value = prevHelper;
        }
    }
}

function getInitials(name) {
    if (!name) return 'TA';
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 2) {
        return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
}

// ==========================================================================
// 10. Navigation & Modal Controller
// ==========================================================================

const sectionTitles = {
    'dashboard': { title: 'Dashboard', sub: 'Real-time overview of campus accessibility transit requests' },
    'requests': { title: 'Assistance Requests', sub: 'Manage, assign helpers, and track trip lifecycles' },
    'new-request': { title: 'Book Assistance', sub: 'Create a new campus transit assistance request' },
    'helpers': { title: 'Helper Roster', sub: 'Manage transit assistants, availability, and skills' },
    'workload': { title: 'Workload Schedule', sub: 'Inspect daily helper assignments and booking slots' },
    'users': { title: 'Campus Users & Staff', sub: 'Manage registered students, staff, and coordinators' },
    'api-docs': { title: 'API Reference', sub: 'Backend REST API endpoints and swagger specification' }
};

function showSection(name) {
    state.activeSection = name;

    // Toggle panels
    document.querySelectorAll('.panel-section').forEach(p => {
        p.classList.toggle('active', p.id === name);
    });

    // Toggle nav active state
    document.querySelectorAll('#sidebar-nav .nav-item').forEach(b => {
        b.classList.toggle('active', b.dataset.section === name);
    });

    // Update Topbar Title
    const meta = sectionTitles[name] || { title: 'TransitAssist', sub: 'Campus Accessibility' };
    if ($('page-title')) $('page-title').textContent = meta.title;
    if ($('page-subtitle')) $('page-subtitle').textContent = meta.sub;

    // Close mobile sidebar
    const sidebar = $('sidebar');
    if (sidebar) sidebar.classList.remove('open');

    // Auto trigger section refresh if needed
    if (name === 'workload') {
        fetchWorkloadSchedule();
    }
}

$('sidebar-nav').addEventListener('click', event => {
    const button = event.target.closest('button[data-section]');
    if (button) {
        showSection(button.dataset.section);
    }
});

// Mobile Sidebar Drawer
$('menu-toggle-btn').addEventListener('click', () => {
    $('sidebar').classList.add('open');
});

$('sidebar-close-btn').addEventListener('click', () => {
    $('sidebar').classList.remove('open');
});

// Modal Management
function openModal(dialogId) {
    const backdrop = $('modal-backdrop');
    const dialog = $(dialogId);
    if (!backdrop || !dialog) return;

    backdrop.classList.add('show');
    dialog.classList.add('show');
    state.activeModal = dialogId;
}

function closeActiveModal() {
    const backdrop = $('modal-backdrop');
    if (backdrop) backdrop.classList.remove('show');

    if (state.activeModal) {
        const dialog = $(state.activeModal);
        if (dialog) dialog.classList.remove('show');
    }
    state.activeModal = null;
    state.selectedRequest = null;
}

// Global Keyboard Listener (Escape to close modals)
document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && state.activeModal) {
        closeActiveModal();
    }
});

// Real-time Global Search Listener
$('global-search').addEventListener('input', event => {
    const val = event.target.value.trim();
    if (val && state.activeSection !== 'requests') {
        showSection('requests');
    }
    const reqSearch = $('requests-search');
    if (reqSearch) {
        reqSearch.value = val;
        renderRequests();
    }
});

// Local Search and Filter Listeners
if ($('requests-search')) $('requests-search').addEventListener('input', renderRequests);
if ($('status-filter')) $('status-filter').addEventListener('change', renderRequests);
if ($('type-filter')) $('type-filter').addEventListener('change', renderRequests);
if ($('date-filter')) $('date-filter').addEventListener('change', renderRequests);
if ($('clear-filters-btn')) $('clear-filters-btn').addEventListener('click', () => {
    if ($('requests-search')) $('requests-search').value = '';
    if ($('status-filter')) $('status-filter').value = '';
    if ($('type-filter')) $('type-filter').value = '';
    if ($('date-filter')) $('date-filter').value = '';
    renderRequests();
});
if ($('refresh-requests-btn')) $('refresh-requests-btn').addEventListener('click', loadAll);
if ($('helpers-search')) $('helpers-search').addEventListener('input', renderHelpers);
if ($('users-search')) $('users-search').addEventListener('input', renderUsers);

// Live Clock in Sidebar
function updateLiveClock() {
    const clock = $('sidebar-live-clock');
    if (clock) {
        const now = new Date();
        clock.textContent = now.toLocaleTimeString();
    }
}
setInterval(updateLiveClock, 1000);
updateLiveClock();

// ==========================================================================
// Initialization
// ==========================================================================

$('req-date').min = todayIso();
$('req-date').value = todayIso();
$('req-time').value = '10:00';
$('wl-date').value = todayIso(1); // default workload view to tomorrow

// Initial Load
loadAll();
