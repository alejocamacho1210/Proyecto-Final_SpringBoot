const tokenKey = 'coworkhub_token';
const api = '/api';
const byId = id => document.getElementById(id);

function token() {
  return localStorage.getItem(tokenKey);
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers || {});
  headers.set('Accept', 'application/json');
  if (token()) headers.set('Authorization', `Bearer ${token()}`);
  if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');

  const response = await fetch(path, { ...options, headers });
  if (response.status === 401) {
    localStorage.removeItem(tokenKey);
    if (location.pathname !== '/login') location.replace('/login');
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      if (response.ok) throw new Error('El servidor devolvió una respuesta no válida.');
      data = { error: text };
    }
  }
  if (!response.ok) throw new Error(data?.error || 'La operación no pudo completarse.');
  return data;
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>'"]/g, character => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
  })[character]);
}

function toast(message, success = true) {
  const element = byId('toast');
  if (!element) return;
  element.textContent = message;
  element.classList.toggle('error-toast', !success);
  element.classList.add('show');
  clearTimeout(element.timeoutId);
  element.timeoutId = setTimeout(() => element.classList.remove('show'), 3200);
}

function localDate(date = new Date()) {
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 10);
}

function money(value) {
  return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 2 }).format(Number(value || 0));
}

const loginForm = byId('loginForm');
if (loginForm) {
  document.querySelectorAll('[data-user]').forEach(button => button.addEventListener('click', () => {
    byId('username').value = button.dataset.user;
    byId('password').value = 'coworkhub123';
    byId('password').focus();
  }));

  loginForm.addEventListener('submit', async event => {
    event.preventDefault();
    const error = byId('loginError');
    const submit = loginForm.querySelector('[type="submit"]');
    error.textContent = '';
    submit.disabled = true;
    try {
      const response = await fetch('/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({
          username: byId('username').value.trim(),
          password: byId('password').value
        })
      });
      const result = await response.json();
      if (!response.ok) throw new Error(result.error || 'Credenciales incorrectas.');
      localStorage.setItem(tokenKey, result.token);
      location.replace('/dashboard');
    } catch (failure) {
      error.textContent = failure.message || 'No se pudo iniciar sesión.';
    } finally {
      submit.disabled = false;
    }
  });

  const registerForm = byId('registerForm');
  const demoUsers = document.querySelector('.demo-users');
  const loginSwitch = byId('loginSwitch');
  const registerSwitch = byId('registerSwitch');

  byId('showRegister').addEventListener('click', async () => {
    loginForm.hidden = true;
    demoUsers.hidden = true;
    loginSwitch.hidden = true;
    registerForm.hidden = false;
    registerSwitch.hidden = false;
    byId('loginError').textContent = '';
    byId('registerDocumentNumber').focus();

    const select = byId('registerPlan');
    if (select.dataset.loaded === 'true') return;
    try {
      const response = await fetch('/auth/planes', { headers: { 'Accept': 'application/json' } });
      const plans = await response.json();
      if (!response.ok) throw new Error(plans.error || 'No se pudieron cargar los planes.');
      const placeholder = document.createElement('option');
      placeholder.value = '';
      placeholder.textContent = 'Selecciona un plan';
      placeholder.disabled = true;
      placeholder.selected = true;
      select.replaceChildren(placeholder);
      plans.forEach(plan => {
        const option = document.createElement('option');
        option.value = plan.id;
        option.textContent = `${plan.name} — ${plan.includedHoursPerMonth} h/mes`;
        select.append(option);
      });
      select.dataset.loaded = 'true';
    } catch (failure) {
      byId('registerError').textContent = failure.message || 'No se pudieron cargar los planes.';
    }
  });

  byId('showLogin').addEventListener('click', () => {
    registerForm.hidden = true;
    registerSwitch.hidden = true;
    loginForm.hidden = false;
    demoUsers.hidden = false;
    loginSwitch.hidden = false;
    byId('registerError').textContent = '';
    byId('username').focus();
  });

  registerForm.addEventListener('submit', async event => {
    event.preventDefault();
    const error = byId('registerError');
    const submit = registerForm.querySelector('[type="submit"]');
    error.textContent = '';
    submit.disabled = true;
    try {
      const response = await fetch('/auth/registro', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({
          documentNumber: byId('registerDocumentNumber').value.trim(),
          fullName: byId('registerFullName').value.trim(),
          email: byId('registerEmail').value.trim(),
          planId: Number(byId('registerPlan').value),
          username: byId('registerUsername').value.trim(),
          password: byId('registerPassword').value
        })
      });
      const result = await response.json();
      if (!response.ok) throw new Error(result.error || 'No se pudo crear la cuenta.');
      localStorage.setItem(tokenKey, result.token);
      location.replace('/dashboard');
    } catch (failure) {
      error.textContent = failure.message || 'No se pudo crear la cuenta.';
    } finally {
      submit.disabled = false;
    }
  });
}

function startDashboard() {
  const state = {
    profile: null,
    locations: [],
    rooms: [],
    equipment: [],
    services: [],
    plans: [],
    members: [],
    bookings: [],
    availabilityRooms: [],
    editing: null,
    serviceBookingId: null
  };
  const isAdmin = () => state.profile?.rol === 'ADMIN';
  const isStaff = () => ['ADMIN', 'RECEPTIONIST'].includes(state.profile?.rol);

  async function loadProfile() {
    state.profile = await request('/auth/perfil');
    byId('profileName').textContent = state.profile.usuario;
    byId('profileRole').textContent = ({
      ADMIN: 'Administrador',
      RECEPTIONIST: 'Recepción',
      MEMBER: 'Miembro'
    })[state.profile.rol] || state.profile.rol;
    byId('profileInitial').textContent = state.profile.usuario.charAt(0).toUpperCase();
    document.querySelectorAll('.staff-only').forEach(element => { element.hidden = !isStaff(); });
    document.querySelectorAll('.admin-only').forEach(element => { element.hidden = !isAdmin(); });
    byId('consumptionPanel').hidden = !state.profile.miembroId;
    byId('bookingMemberField').hidden = !isStaff();
    byId('bookingMember').required = isStaff();
  }

  function setView(name, button) {
    document.querySelectorAll('.nav-item').forEach(item => {
      item.classList.toggle('active', item === button);
    });
    document.querySelectorAll('.view').forEach(view => view.classList.remove('active'));
    byId(`${name}View`).classList.add('active');
    byId('viewTitle').textContent = button?.textContent.trim() || ({
      overview: 'Resumen', rooms: 'Salas y disponibilidad', bookings: 'Reservas',
      members: 'Miembros', catalogs: 'Catálogos'
    })[name];
    closeSidebar();
  }

  function closeSidebar() {
    byId('sidebar').classList.remove('open');
    byId('sidebarBackdrop').hidden = true;
    byId('sidebarToggle').setAttribute('aria-expanded', 'false');
  }

  function renderCatalogRows(items, type) {
    return items.map(item => {
      let detail = '';
      if (type === 'location') detail = `${item.city} · ${item.openingTime}–${item.closingTime}`;
      if (type === 'room') {
        const equipmentNames = (item.equipment || []).map(value => value.name).join(', ');
        detail = `${item.locationName} · ${item.capacity} personas · ${money(item.hourlyRate)}/h${equipmentNames ? ` · ${equipmentNames}` : ''}`;
      }
      if (type === 'equipment') detail = 'Equipamiento disponible';
      if (type === 'service') detail = money(item.unitPrice);
      if (type === 'plan') detail = `${item.includedHoursPerMonth} h/mes · ${item.overageDiscount}% descuento · ${item.maxActiveBookings} reservas`;
      const buttons = isAdmin()
        ? `<div class="row-actions"><button class="text-button" data-edit-catalog="${type}" data-id="${item.id}" type="button">Editar</button><button class="danger-button" data-delete-catalog="${type}" data-id="${item.id}" type="button">Eliminar</button></div>`
        : '';
      return `<div class="manage-row"><div><b>${escapeHtml(item.name)}</b><small>${escapeHtml(detail)}</small></div>${buttons}</div>`;
    }).join('') || '<p class="empty-state">Todavía no hay elementos en este catálogo.</p>';
  }

  function renderCatalogs() {
    byId('locationsList').innerHTML = renderCatalogRows(state.locations, 'location');
    byId('catalogRoomsList').innerHTML = renderCatalogRows(state.rooms, 'room');
    byId('equipmentList').innerHTML = renderCatalogRows(state.equipment, 'equipment');
    byId('servicesList').innerHTML = renderCatalogRows(state.services, 'service');
    byId('plansList').innerHTML = renderCatalogRows(state.plans, 'plan');

    byId('locationFilter').innerHTML = state.locations.map(item =>
      `<option value="${item.id}">${escapeHtml(item.name)} — ${escapeHtml(item.city)}</option>`
    ).join('');
    byId('equipmentFilter').innerHTML = state.equipment.map(item =>
      `<option value="${item.id}">${escapeHtml(item.name)}</option>`
    ).join('');
    byId('bookingRoom').innerHTML = state.rooms.filter(room => room.active).map(room =>
      `<option value="${room.id}">${escapeHtml(room.name)} · ${escapeHtml(room.locationName)}</option>`
    ).join('');
    byId('bookingServices').innerHTML = state.services.map(service =>
      `<label class="service-option"><input type="checkbox" data-service-id="${service.id}"><span>${escapeHtml(service.name)}<small>${money(service.unitPrice)}</small></span><input class="service-quantity" type="number" min="1" value="1" aria-label="Cantidad de ${escapeHtml(service.name)}" required disabled></label>`
    ).join('') || '<p class="empty-state">No hay servicios adicionales disponibles.</p>';
    byId('additionalServiceId').innerHTML = state.services.map(service =>
      `<option value="${service.id}">${escapeHtml(service.name)} — ${money(service.unitPrice)}</option>`
    ).join('');

    renderRooms(state.rooms, false);
  }

  async function loadCatalogs() {
    const [locations, rooms, equipment, services, plans] = await Promise.all([
      request(`${api}/sedes`),
      request(`${api}/salas`),
      request(`${api}/equipamientos`),
      request(`${api}/servicios-adicionales`),
      request(`${api}/planes`)
    ]);
    Object.assign(state, { locations, rooms, equipment, services, plans });
    renderCatalogs();
  }

  function renderRooms(rooms, availability = false) {
    byId('roomsGrid').innerHTML = rooms.map(room => {
      const active = availability || room.active;
      const equipmentNames = (room.equipment || []).map(item => item.name).join(', ');
      return `<article class="room-card">
        <div class="room-card-heading"><span class="room-icon">⌂</span><span class="status ${active ? 'status-active' : 'status-inactive'}">${active ? 'Disponible' : 'Inactiva'}</span></div>
        <h3>${escapeHtml(room.name)}</h3>
        <p>${escapeHtml(String(room.type || '').replaceAll('_', ' '))} · ${room.capacity} asistentes</p>
        <p>${escapeHtml(room.locationName || state.locations.find(location => location.id === room.locationId)?.name || 'Sede')}</p>
        ${equipmentNames ? `<p class="room-equipment">${escapeHtml(equipmentNames)}</p>` : ''}
        <div class="room-card-footer"><b>${money(room.hourlyRate)}<small>/ hora</small></b>${active ? `<button class="secondary small" type="button" data-book-room="${room.id}">Reservar</button>` : ''}</div>
      </article>`;
    }).join('') || '<p class="empty-state">No se encontraron salas para estos filtros.</p>';
  }

  async function loadMembers(filters = {}) {
    if (!isStaff()) return;
    state.members = await request(`${api}/miembros`);
    const activeMembers = state.members.filter(member => member.status === 'ACTIVE');
    byId('bookingMember').innerHTML = activeMembers.map(member =>
      `<option value="${member.id}">${escapeHtml(member.fullName)}</option>`
    ).join('');
    const nameFilter = filters.nombre?.toLocaleLowerCase('es') || '';
    const filteredMembers = state.members.filter(member =>
      (!nameFilter || member.fullName.toLocaleLowerCase('es').includes(nameFilter)) &&
      (!filters.estado || member.status === filters.estado)
    );
    renderMembers(filteredMembers);
  }

  function renderMembers(members) {
    byId('membersTable').innerHTML = members.length ? `<table><thead><tr>
      <th>Miembro</th><th>Documento</th><th>Plan</th><th>Cuenta</th><th>Estado</th><th>Acciones</th>
      </tr></thead><tbody>${members.map(member => `<tr>
        <td><b>${escapeHtml(member.fullName)}</b><br><small>${escapeHtml(member.email)}</small></td>
        <td>${escapeHtml(member.documentNumber)}</td>
        <td>${escapeHtml(member.planName)}</td>
        <td>${escapeHtml(member.username || 'Sin cuenta')}</td>
        <td><span class="status ${member.status === 'ACTIVE' ? 'status-active' : 'status-inactive'}">${member.status === 'ACTIVE' ? 'Activo' : 'Suspendido'}</span></td>
        <td><div class="row-actions">
          <button class="text-button" data-edit-member="${member.id}" type="button">Editar</button>
          <button class="text-button" data-member-status="${member.id}" data-next-status="${member.status === 'ACTIVE' ? 'suspender' : 'activar'}" type="button">${member.status === 'ACTIVE' ? 'Suspender' : 'Activar'}</button>
          <button class="danger-button" data-delete-member="${member.id}" type="button">Eliminar</button>
        </div></td>
      </tr>`).join('')}</tbody></table>` : '<p class="empty-state">No hay miembros registrados.</p>';
  }

  async function loadBookings() {
    const params = new URLSearchParams();
    if (state.profile.rol === 'MEMBER') params.set('miembroId', state.profile.miembroId);
    state.bookings = await request(`${api}/reservas${params.toString() ? `?${params}` : ''}`);
    renderBookings(state.bookings);
  }

  function bookingActions(booking) {
    const staffActions = [];
    if (isStaff() && booking.status === 'PENDING') {
      staffActions.push(`<button class="text-button" data-booking-action="confirmar" data-id="${booking.id}" type="button">Confirmar</button>`);
    }
    if (isStaff() && booking.status === 'CONFIRMED') {
      staffActions.push(`<button class="text-button" data-booking-action="completar" data-id="${booking.id}" type="button">Completar</button>`);
    }
    const mayCancel = ['PENDING', 'CONFIRMED'].includes(booking.status);
    if (mayCancel) {
      staffActions.push(`<button class="danger-button" data-booking-action="cancelar" data-id="${booking.id}" type="button">Cancelar</button>`);
    }
    if (booking.status === 'PENDING') {
      staffActions.push(`<button class="text-button" data-edit-booking="${booking.id}" type="button">Editar</button>`);
      staffActions.push(`<button class="text-button" data-booking-services="${booking.id}" type="button">Servicios</button>`);
    }
    return `<div class="row-actions">${staffActions.join('') || '<span class="muted">—</span>'}</div>`;
  }

  function bookingTable(bookings, compact = false) {
    const visible = compact ? bookings.slice(0, 5) : bookings;
    return `<table><thead><tr><th>Reserva</th><th>Sala</th><th>Miembro</th><th>Horario</th><th>Total</th><th>Estado</th>${compact ? '' : '<th>Acciones</th>'}</tr></thead><tbody>
      ${visible.map(booking => {
        const services = (booking.items || []).map(item => `${item.quantity}× ${escapeHtml(item.serviceName)}`).join(', ');
        const dateTime = `${booking.startTime.slice(0, 10)} · ${booking.startTime.slice(11, 16)}–${booking.endTime.slice(11, 16)}`;
        return `<tr>
          <td><b>#${booking.id}</b>${services ? `<small class="table-subline">${services}</small>` : ''}</td>
          <td>${escapeHtml(booking.roomName)}</td><td>${escapeHtml(booking.memberName)}</td>
          <td>${escapeHtml(dateTime)}</td><td>${money(booking.totalCost)}</td>
          <td><span class="status status-${booking.status.toLowerCase()}">${bookingStatus(booking.status)}</span></td>
          ${compact ? '' : `<td>${bookingActions(booking)}</td>`}
        </tr>`;
      }).join('') || `<tr><td colspan="${compact ? 6 : 7}" class="empty-cell">No hay reservas para mostrar.</td></tr>`}
    </tbody></table>`;
  }

  function bookingStatus(status) {
    return ({ PENDING: 'Pendiente', CONFIRMED: 'Confirmada', COMPLETED: 'Completada', CANCELLED: 'Cancelada' })[status] || status;
  }

  function renderBookings(bookings) {
    const status = byId('bookingStatus').value;
    const date = byId('bookingDateFilter').value;
    const filtered = bookings.filter(booking =>
      (!status || booking.status === status) &&
      (!date || booking.startTime.slice(0, 10) === date)
    );
    byId('recentBookings').innerHTML = bookingTable(bookings, true);
    byId('bookingsTable').innerHTML = bookingTable(filtered);
  }

  async function loadConsumption() {
    if (!state.profile.miembroId) return;
    const month = byId('consumptionMonth').value;
    const result = await request(`${api}/miembros/${state.profile.miembroId}/consumo?mes=${month}`);
    const percentage = result.horasIncluidas
      ? Math.min(100, Number(result.horasUsadas) / result.horasIncluidas * 100)
      : (Number(result.horasUsadas) ? 100 : 0);
    byId('consumption').innerHTML = `
      <div class="consumption-numbers"><b>${result.horasUsadas} h</b><span>usadas de ${result.horasIncluidas} incluidas</span></div>
      <div class="meter" role="progressbar" aria-label="Horas consumidas" aria-valuenow="${percentage}" aria-valuemin="0" aria-valuemax="100"><span style="width:${percentage}%"></span></div>
      <div class="consumption-breakdown"><span><b>${result.horasRestantes}</b> restantes</span><span><b>${result.horasExcedentes}</b> excedentes</span></div>`;
  }

  function renderStats() {
    const stats = state.profile.miembroId
      ? [['Sedes', state.locations.length], ['Salas', state.rooms.filter(room => room.active).length], ['Mis reservas', state.bookings.length], ['Servicios', state.services.length]]
      : [['Sedes', state.locations.length], ['Salas', state.rooms.length], ['Reservas', state.bookings.length], ['Miembros', state.members.length]];
    byId('stats').innerHTML = stats.map(([label, count]) =>
      `<article class="stat"><span>${label}</span><strong>${count}</strong></article>`
    ).join('');
  }

  const schemas = {
    location: [
      { name: 'name', label: 'Nombre', type: 'text', required: true },
      { name: 'city', label: 'Ciudad', type: 'text', required: true },
      { name: 'address', label: 'Dirección', type: 'text', required: true },
      { name: 'openingTime', label: 'Hora de apertura', type: 'time', required: true },
      { name: 'closingTime', label: 'Hora de cierre', type: 'time', required: true }
    ],
    room: [
      { name: 'name', label: 'Nombre', type: 'text', required: true },
      { name: 'type', label: 'Tipo', type: 'select', required: true, options: [['MEETING_ROOM', 'Sala de reuniones'], ['PRIVATE_OFFICE', 'Oficina privada'], ['DESK', 'Escritorio']] },
      { name: 'capacity', label: 'Capacidad', type: 'number', min: 1, required: true },
      { name: 'hourlyRate', label: 'Tarifa por hora', type: 'number', min: 0, step: '0.01', required: true },
      { name: 'active', label: 'Activa', type: 'checkbox' },
      { name: 'locationId', label: 'Sede', type: 'select', required: true, options: 'locations' },
      { name: 'equipmentIds', label: 'Equipamiento', type: 'multiselect', options: 'equipment' }
    ],
    equipment: [{ name: 'name', label: 'Nombre', type: 'text', required: true }],
    service: [
      { name: 'name', label: 'Nombre', type: 'text', required: true },
      { name: 'unitPrice', label: 'Precio unitario', type: 'number', min: 0, step: '0.01', required: true }
    ],
    plan: [
      { name: 'name', label: 'Nombre', type: 'text', required: true },
      { name: 'includedHoursPerMonth', label: 'Horas incluidas al mes', type: 'number', min: 0, required: true },
      { name: 'overageDiscount', label: 'Descuento por excedente (%)', type: 'number', min: 0, max: 100, required: true },
      { name: 'maxActiveBookings', label: 'Máximo de reservas activas', type: 'number', min: 1, required: true }
    ],
    booking: [
      { name: 'salaId', label: 'Sala', type: 'select', required: true, options: 'activeRooms' },
      { name: 'inicio', label: 'Inicio', type: 'datetime-local', required: true },
      { name: 'fin', label: 'Fin', type: 'datetime-local', required: true },
      { name: 'asistentes', label: 'Asistentes', type: 'number', min: 1, required: true }
    ],
    memberCreate: [
      { name: 'documentNumber', label: 'Documento', type: 'text', required: true },
      { name: 'fullName', label: 'Nombre completo', type: 'text', required: true },
      { name: 'email', label: 'Correo electrónico', type: 'email', required: true },
      { name: 'planId', label: 'Plan', type: 'select', required: true, options: 'plans' },
      { name: 'username', label: 'Usuario de acceso', type: 'text', required: true },
      { name: 'password', label: 'Contraseña inicial', type: 'password', minLength: 8, required: true }
    ],
    memberEdit: [
      { name: 'fullName', label: 'Nombre completo', type: 'text', required: true },
      { name: 'email', label: 'Correo electrónico', type: 'email', required: true },
      { name: 'planId', label: 'Plan', type: 'select', required: true, options: 'plans' }
    ]
  };

  function fieldOptions(field) {
    if (Array.isArray(field.options)) return field.options;
    if (field.options === 'activeRooms') {
      return state.rooms.filter(room => room.active).map(room => [String(room.id), `${room.name} · ${room.locationName}`]);
    }
    const source = state[field.options] || [];
    return source.map(item => [String(item.id), item.name]);
  }

  function renderEditorField(field, value) {
    const id = `editor-${field.name}`;
    const required = field.required ? 'required' : '';
    if (field.type === 'checkbox') {
      return `<label class="check-field"><input id="${id}" name="${field.name}" type="checkbox" ${value ? 'checked' : ''}><span>${field.label}</span></label>`;
    }
    if (field.type === 'select' || field.type === 'multiselect') {
      const multiple = field.type === 'multiselect';
      const selectedValues = multiple ? (value || []).map(String) : [String(value ?? '')];
      const options = fieldOptions(field).map(([optionValue, label]) =>
        `<option value="${escapeHtml(optionValue)}" ${selectedValues.includes(String(optionValue)) ? 'selected' : ''}>${escapeHtml(label)}</option>`
      ).join('');
      return `<label>${field.label}<select id="${id}" name="${field.name}" ${multiple ? 'multiple size="4"' : ''} ${required}>${options}</select></label>`;
    }
    const inputValue = field.type === 'time' && value
      ? String(value).slice(0, 5)
      : field.type === 'datetime-local' && value ? String(value).slice(0, 16) : value;
    const attributes = [
      field.min !== undefined ? `min="${field.min}"` : '',
      field.max !== undefined ? `max="${field.max}"` : '',
      field.step ? `step="${field.step}"` : '',
      field.minLength ? `minlength="${field.minLength}"` : '',
      required
    ].filter(Boolean).join(' ');
    return `<label>${field.label}<input id="${id}" name="${field.name}" type="${field.type}" value="${escapeHtml(inputValue ?? '')}" ${attributes}></label>`;
  }

  function openEditor(type, entity = null) {
    const memberCreation = type === 'member' && !entity;
    const schemaKey = type === 'member' ? (memberCreation ? 'memberCreate' : 'memberEdit') : type;
    const fields = schemas[schemaKey];
    state.editing = { type, id: entity?.id ?? null, schemaKey };
    byId('editorKicker').textContent = memberCreation ? 'Nuevo cliente' : entity ? 'Editar información' : 'Nuevo elemento';
    byId('editorTitle').textContent = memberCreation ? 'Registrar miembro' : entity ? 'Editar elemento' : ({
      location: 'Agregar sede', room: 'Agregar sala', equipment: 'Agregar equipamiento',
      service: 'Agregar servicio', plan: 'Agregar plan', member: 'Registrar miembro'
    })[type];
    byId('editorForm').innerHTML = fields.map(field => {
      let value = entity?.[field.name];
      if (field.name === 'equipmentIds' && entity) value = entity.equipment?.map(item => item.id) || [];
      if (field.name === 'active' && !entity) value = true;
      return renderEditorField(field, value);
    }).join('') + `<div class="form-actions"><button class="primary" type="submit">${entity ? 'Guardar cambios' : 'Crear'}</button><button class="secondary" type="button" data-close-modal>Cancelar</button></div>`;
    byId('editorModal').hidden = false;
    byId('editorForm').querySelector('input,select')?.focus();
  }

  async function saveEditor(event) {
    event.preventDefault();
    if (!state.editing) return;
    const { type, id, schemaKey } = state.editing;
    const fields = schemas[schemaKey];
    const body = {};
    for (const field of fields) {
      const input = byId(`editor-${field.name}`);
      if (field.type === 'checkbox') body[field.name] = input.checked;
      else if (field.type === 'multiselect') body[field.name] = Array.from(input.selectedOptions, option => Number(option.value));
      else if (field.type === 'number') body[field.name] = Number(input.value);
      else if (field.type === 'select' && ['locationId', 'planId', 'salaId'].includes(field.name)) body[field.name] = Number(input.value);
      else body[field.name] = input.value.trim();
    }
    if (type === 'member' && id) delete body.documentNumber;
    const endpoint = type === 'member' ? `${api}/miembros` : type === 'booking' ? `${api}/reservas` : `${api}/${({
      location: 'sedes', room: 'salas', equipment: 'equipamientos',
      service: 'servicios-adicionales', plan: 'planes'
    })[type]}`;
    try {
      await request(id ? `${endpoint}/${id}` : endpoint, {
        method: id ? 'PUT' : 'POST',
        body: JSON.stringify(body)
      });
      byId('editorModal').hidden = true;
      state.editing = null;
      if (type === 'member') await loadMembers();
      else if (type === 'booking') await loadBookings();
      else await loadCatalogs();
      renderStats();
      toast(type === 'booking' ? 'Reserva actualizada.' : id ? 'Cambios guardados.' : 'Elemento creado.');
    } catch (failure) {
      toast(failure.message, false);
    }
  }

  async function deleteCatalog(type, id) {
    const labels = { location: 'sede', room: 'sala', equipment: 'equipamiento', service: 'servicio', plan: 'plan' };
    if (!confirm(`¿Eliminar este ${labels[type]}? Si tiene elementos asociados, no se podrá eliminar.`)) return;
    const endpoint = ({
      location: 'sedes', room: 'salas', equipment: 'equipamientos',
      service: 'servicios-adicionales', plan: 'planes'
    })[type];
    try {
      await request(`${api}/${endpoint}/${id}`, { method: 'DELETE' });
      await loadCatalogs();
      renderStats();
      toast('Elemento eliminado.');
    } catch (failure) {
      toast(failure.message, false);
    }
  }

  async function updateMemberStatus(id, action) {
    try {
      await request(`${api}/miembros/${id}/${action}`, { method: 'PATCH' });
      await loadMembers();
      toast(action === 'activar' ? 'Miembro activado.' : 'Miembro suspendido.');
    } catch (failure) {
      toast(failure.message, false);
    }
  }

  async function deleteMember(id) {
    if (!confirm('¿Eliminar este miembro? Esta acción no se puede deshacer.')) return;
    try {
      await request(`${api}/miembros/${id}`, { method: 'DELETE' });
      await loadMembers();
      renderStats();
      toast('Miembro eliminado.');
    } catch (failure) {
      toast(failure.message, false);
    }
  }

  function openServiceManager(booking) {
    state.serviceBookingId = booking.id;
    byId('serviceModalTitle').textContent = `Servicios · reserva #${booking.id}`;
    byId('bookingServiceItems').innerHTML = booking.items?.length
      ? booking.items.map(item => `<div class="manage-row"><div><b>${escapeHtml(item.serviceName)}</b><small>${item.quantity} × ${money(item.appliedUnitPrice)}</small></div><button class="danger-button" data-remove-booking-service="${item.serviceId}" type="button">Quitar</button></div>`).join('')
      : '<p class="empty-state">Esta reserva todavía no tiene servicios adicionales.</p>';
    byId('serviceModal').hidden = false;
  }

  function openBookingEditor(booking) {
    openEditor('booking', {
      ...booking,
      salaId: booking.roomId,
      inicio: booking.startTime,
      fin: booking.endTime,
      asistentes: booking.attendees
    });
    byId('editorKicker').textContent = `Reserva #${booking.id} · pendiente`;
    byId('editorTitle').textContent = 'Modificar reserva';
  }

  async function performBookingAction(action, id) {
    const booking = state.bookings.find(item => item.id === Number(id));
    if (!booking) return;
    if (action === 'cancelar' && !confirm(`¿Cancelar la reserva #${id}? La tarifa depende de cuánto falte para el inicio.`)) return;
    try {
      const result = await request(`${api}/reservas/${id}/${action}`, { method: 'POST' });
      await loadBookings();
      renderStats();
      if (action === 'cancelar') toast(`Reserva cancelada. Cargo: ${money(result.cancellationFee)}.`);
      else toast(action === 'confirmar' ? 'Reserva confirmada.' : 'Reserva completada.');
    } catch (failure) {
      toast(failure.message, false);
    }
  }

  async function addBookingService(event) {
    event.preventDefault();
    if (!state.serviceBookingId) return;
    try {
      await request(`${api}/reservas/${state.serviceBookingId}/servicios`, {
        method: 'POST',
        body: JSON.stringify({
          servicioId: Number(byId('additionalServiceId').value),
          cantidad: Number(byId('additionalServiceQuantity').value)
        })
      });
      const booking = await request(`${api}/reservas/${state.serviceBookingId}`);
      openServiceManager(booking);
      await loadBookings();
      toast('Servicio agregado.');
    } catch (failure) {
      toast(failure.message, false);
    }
  }

  async function removeBookingService(serviceId) {
    if (!state.serviceBookingId) return;
    try {
      const booking = await request(`${api}/reservas/${state.serviceBookingId}/servicios/${serviceId}`, { method: 'DELETE' });
      openServiceManager(booking);
      await loadBookings();
      toast('Servicio retirado.');
    } catch (failure) {
      toast(failure.message, false);
    }
  }

  function showBookingForm(roomId = null, availableDate = null) {
    byId('bookingFormPanel').hidden = false;
    if (roomId) byId('bookingRoom').value = String(roomId);
    if (availableDate) {
      const date = availableDate;
      byId('bookingStart').value = `${date}T${byId('availabilityStart').value}`;
      byId('bookingEnd').value = `${date}T${byId('availabilityEnd').value}`;
    }
    setView('bookings', document.querySelector('[data-view="bookings"]'));
    byId('bookingRoom').focus();
  }

  async function createBooking(event) {
    event.preventDefault();
    const selectedServices = Array.from(byId('bookingServices').querySelectorAll('[data-service-id]:checked'))
      .map(checkbox => ({
        servicioId: Number(checkbox.dataset.serviceId),
        cantidad: Number(checkbox.closest('.service-option').querySelector('.service-quantity').value)
      }));
    const payload = {
      salaId: Number(byId('bookingRoom').value),
      miembroId: isStaff() ? Number(byId('bookingMember').value) : Number(state.profile.miembroId),
      inicio: byId('bookingStart').value,
      fin: byId('bookingEnd').value,
      asistentes: Number(byId('bookingAttendees').value),
      servicios: selectedServices
    };
    try {
      await request(`${api}/reservas`, { method: 'POST', body: JSON.stringify(payload) });
      byId('bookingForm').reset();
      byId('bookingFormPanel').hidden = true;
      await loadBookings();
      renderStats();
      toast('Reserva creada correctamente.');
    } catch (failure) {
      toast(failure.message, false);
    }
  }

  async function searchAvailability(event) {
    event.preventDefault();
    const date = byId('availabilityDate').value;
    const start = byId('availabilityStart').value;
    const end = byId('availabilityEnd').value;
    if (!date || !start || !end || start >= end) {
      toast('Selecciona una fecha y un horario válido.', false);
      return;
    }
    const params = new URLSearchParams({
      sedeId: byId('locationFilter').value,
      fecha: date,
      horaInicio: start,
      horaFin: end,
      capacidadMinima: byId('minimumCapacity').value || '1'
    });
    Array.from(byId('equipmentFilter').selectedOptions).forEach(option => params.append('equipamientoIds', option.value));
    try {
      state.availabilityRooms = await request(`${api}/disponibilidad?${params}`);
      renderRooms(state.availabilityRooms, true);
    } catch (failure) {
      toast(failure.message, false);
    }
  }

  document.querySelectorAll('.nav-item').forEach(button => button.addEventListener('click', () => setView(button.dataset.view, button)));
  document.querySelectorAll('[data-open-view]').forEach(button => button.addEventListener('click', () => {
    setView(button.dataset.openView, document.querySelector(`[data-view="${button.dataset.openView}"]`));
  }));
  byId('sidebarToggle').addEventListener('click', () => {
    const isOpen = byId('sidebar').classList.toggle('open');
    byId('sidebarBackdrop').hidden = !isOpen;
    byId('sidebarToggle').setAttribute('aria-expanded', String(isOpen));
  });
  byId('sidebarBackdrop').addEventListener('click', closeSidebar);
  byId('logoutButton').addEventListener('click', () => {
    localStorage.removeItem(tokenKey);
    location.replace('/login');
  });
  byId('memberFilter').addEventListener('submit', async event => {
    event.preventDefault();
    try {
      await loadMembers({ nombre: byId('memberName').value.trim(), estado: byId('memberStatus').value });
    } catch (failure) {
      toast(failure.message, false);
    }
  });
  byId('clearMemberFilter').addEventListener('click', async () => {
    byId('memberFilter').reset();
    try { await loadMembers(); } catch (failure) { toast(failure.message, false); }
  });
  byId('createMemberButton').addEventListener('click', () => openEditor('member'));
  byId('openBookingForm').addEventListener('click', () => showBookingForm());
  byId('closeBookingForm').addEventListener('click', () => { byId('bookingFormPanel').hidden = true; });
  byId('cancelBookingForm').addEventListener('click', () => { byId('bookingFormPanel').hidden = true; });
  byId('bookingForm').addEventListener('submit', createBooking);
  byId('availabilityForm').addEventListener('submit', searchAvailability);
  byId('bookingFilter').addEventListener('input', () => renderBookings(state.bookings));
  byId('bookingFilter').addEventListener('reset', () => setTimeout(() => renderBookings(state.bookings)));
  byId('consumptionMonth').value = new Date().toISOString().slice(0, 7);
  byId('consumptionMonth').addEventListener('change', () => loadConsumption().catch(failure => toast(failure.message, false)));
  byId('editorForm').addEventListener('submit', saveEditor);
  byId('addBookingServiceForm').addEventListener('submit', addBookingService);
  byId('bookingServices').addEventListener('change', event => {
    if (event.target.matches('[data-service-id]')) {
      event.target.closest('.service-option').querySelector('.service-quantity').disabled = !event.target.checked;
    }
  });
  byId('roomsGrid').addEventListener('click', event => {
    const button = event.target.closest('[data-book-room]');
    if (button) showBookingForm(Number(button.dataset.bookRoom), byId('availabilityDate').value);
  });
  document.addEventListener('click', event => {
    const createCatalogButton = event.target.closest('[data-create-catalog]');
    const editCatalogButton = event.target.closest('[data-edit-catalog]');
    const deleteCatalogButton = event.target.closest('[data-delete-catalog]');
    const editMemberButton = event.target.closest('[data-edit-member]');
    const statusButton = event.target.closest('[data-member-status]');
    const deleteMemberButton = event.target.closest('[data-delete-member]');
    const bookingActionButton = event.target.closest('[data-booking-action]');
    const editBookingButton = event.target.closest('[data-edit-booking]');
    const serviceButton = event.target.closest('[data-booking-services]');
    const removeServiceButton = event.target.closest('[data-remove-booking-service]');

    if (createCatalogButton) openEditor(createCatalogButton.dataset.createCatalog);
    else if (editCatalogButton) {
      const type = editCatalogButton.dataset.editCatalog;
      const collection = ({ location: state.locations, room: state.rooms, equipment: state.equipment, service: state.services, plan: state.plans })[type];
      openEditor(type, collection.find(item => item.id === Number(editCatalogButton.dataset.id)));
    } else if (deleteCatalogButton) deleteCatalog(deleteCatalogButton.dataset.deleteCatalog, Number(deleteCatalogButton.dataset.id));
    else if (editMemberButton) openEditor('member', state.members.find(item => item.id === Number(editMemberButton.dataset.editMember)));
    else if (statusButton) updateMemberStatus(Number(statusButton.dataset.memberStatus), statusButton.dataset.nextStatus);
    else if (deleteMemberButton) deleteMember(Number(deleteMemberButton.dataset.deleteMember));
    else if (bookingActionButton) performBookingAction(bookingActionButton.dataset.bookingAction, bookingActionButton.dataset.id);
    else if (editBookingButton) {
      const booking = state.bookings.find(item => item.id === Number(editBookingButton.dataset.editBooking));
      if (booking) openBookingEditor(booking);
    }
    else if (serviceButton) {
      const booking = state.bookings.find(item => item.id === Number(serviceButton.dataset.bookingServices));
      if (booking) openServiceManager(booking);
    } else if (removeServiceButton) removeBookingService(Number(removeServiceButton.dataset.removeBookingService));
    else if (event.target.closest('[data-close-modal]')) {
      byId('editorModal').hidden = true;
      byId('serviceModal').hidden = true;
    }
  });

  async function initialize() {
    await loadProfile();
    const date = new Date();
    date.setDate(date.getDate() + 7);
    byId('availabilityDate').value = localDate(date);
    if (state.profile.miembroId) {
      const now = new Date();
      byId('consumptionMonth').value = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    }
    const loaders = [loadCatalogs(), loadBookings()];
    if (isStaff()) loaders.push(loadMembers());
    if (state.profile.miembroId) loaders.push(loadConsumption());
    const results = await Promise.allSettled(loaders);
    const failures = results.filter(result => result.status === 'rejected').map(result => result.reason.message);
    renderStats();
    if (failures.length) toast(failures.join(' · '), false);
  }

  initialize().catch(failure => toast(failure.message, false));
}

if (document.querySelector('.workspace')) {
  if (token()) startDashboard();
  else location.replace('/login');
}
