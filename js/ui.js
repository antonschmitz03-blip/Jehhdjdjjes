// DOM wiring: toolbar, catalog, properties panel, modals, status/hints.
'use strict';

const UI = (() => {
  const PRESET_COLORS = ['#9bb3c9', '#c2a987', '#8d9b87', '#b6a4c9', '#aac4cb', '#a8a39b', '#d8a5a0', '#e0c089', '#7f9b73', '#3a3a3a'];
  const TOOL_HINTS = {
    select: '',
    room: 'Click to place wall corners · type a number (e.g. 240 or 8ft) for an exact length · Enter/click start to finish · Esc cancel · Backspace undo last point',
    door: 'Click a wall to add a door, then drag to position it',
    window: 'Click a wall to add a window, then drag to position it',
  };
  let hintTimer = null;
  let catalogDrag = null;

  function qs(id) { return document.getElementById(id); }
  function escapeHtml(s) { return String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }

  // ---------------- Catalog ----------------
  function renderCatalog(filter) {
    const list = qs('catalogList');
    list.innerHTML = '';
    const items = Catalog.allItems();
    const f = (filter || '').toLowerCase().trim();
    for (const cat of Catalog.CATEGORY_ORDER) {
      const catItems = items.filter(i => i.cat === cat && (!f || i.label.toLowerCase().includes(f)));
      if (!catItems.length) continue;
      const title = document.createElement('div');
      title.className = 'cat-group-title';
      title.textContent = cat;
      list.appendChild(title);
      catItems.forEach(item => list.appendChild(buildCatalogRow(item)));
    }
    if (!list.children.length) {
      const p = document.createElement('p'); p.className = 'muted'; p.textContent = 'No matches.';
      list.appendChild(p);
    }
  }

  function renderCatalogIcon(canvasEl, item) {
    const size = 34;
    const dpr = window.devicePixelRatio || 1;
    canvasEl.width = size * dpr; canvasEl.height = size * dpr;
    canvasEl.style.width = size + 'px'; canvasEl.style.height = size + 'px';
    const ctx = canvasEl.getContext('2d');
    ctx.scale(dpr, dpr);
    const pad = 5;
    const scale = Math.min((size - pad * 2) / item.w, (size - pad * 2) / item.d);
    const w = item.w * scale, h = item.d * scale;
    ctx.save();
    ctx.translate(size / 2, size / 2);
    ctx.beginPath();
    if (item.kind === 'round') ctx.ellipse(0, 0, w / 2, h / 2, 0, 0, Math.PI * 2);
    else if (ctx.roundRect) ctx.roundRect(-w / 2, -h / 2, w, h, Math.min(w, h) * 0.12);
    else ctx.rect(-w / 2, -h / 2, w, h);
    ctx.fillStyle = item.color; ctx.fill();
    ctx.lineWidth = 1; ctx.strokeStyle = 'rgba(0,0,0,0.3)'; ctx.stroke();
    Catalog.drawGlyph(ctx, item.kind, w, h, 'rgba(0,0,0,0.35)');
    ctx.restore();
  }

  function buildCatalogRow(item) {
    const row = document.createElement('div');
    row.className = 'cat-item';
    const canvasEl = document.createElement('canvas');
    renderCatalogIcon(canvasEl, item);
    row.appendChild(canvasEl);
    const info = document.createElement('div');
    info.className = 'info';
    const name = document.createElement('div'); name.className = 'name'; name.textContent = item.label;
    const dims = document.createElement('div'); dims.className = 'dims';
    dims.textContent = `${Math.round(item.w)} × ${Math.round(item.d)} cm`;
    info.appendChild(name); info.appendChild(dims);
    row.appendChild(info);
    if (item.cat === 'Custom') {
      const del = document.createElement('button');
      del.className = 'del-btn'; del.textContent = '✕'; del.title = 'Remove from catalog';
      del.addEventListener('click', (e) => { e.stopPropagation(); Catalog.deleteCustomItem(item.id); renderCatalog(qs('catalogSearch').value); });
      row.appendChild(del);
    }
    row.addEventListener('pointerdown', (e) => startCatalogDrag(e, item, row));
    return row;
  }

  function startCatalogDrag(e, item, rowEl) {
    if (e.target.closest('.del-btn')) return;
    e.preventDefault();
    const startX = e.clientX, startY = e.clientY;
    rowEl.setPointerCapture(e.pointerId);
    const ghost = document.createElement('div');
    Object.assign(ghost.style, {
      position: 'fixed', left: '0', top: '0', pointerEvents: 'none', zIndex: 999,
      padding: '4px 9px', background: '#3b56d9', color: '#fff', borderRadius: '6px',
      fontSize: '11px', boxShadow: '0 4px 12px rgba(0,0,0,0.25)',
      transform: `translate(${e.clientX + 10}px, ${e.clientY + 10}px)`
    });
    ghost.textContent = item.label;
    document.body.appendChild(ghost);
    catalogDrag = { moved: false };

    function move(ev) {
      const dx = ev.clientX - startX, dy = ev.clientY - startY;
      if (Math.hypot(dx, dy) > 5) catalogDrag.moved = true;
      ghost.style.transform = `translate(${ev.clientX + 10}px, ${ev.clientY + 10}px)`;
    }
    function up(ev) {
      try { rowEl.releasePointerCapture(e.pointerId); } catch (err) {}
      rowEl.removeEventListener('pointermove', move);
      rowEl.removeEventListener('pointerup', up);
      document.body.removeChild(ghost);
      const canvasEl = qs('canvas');
      const rect = canvasEl.getBoundingClientRect();
      const overCanvas = ev.clientX >= rect.left && ev.clientX <= rect.right && ev.clientY >= rect.top && ev.clientY <= rect.bottom;
      let worldPos;
      if (catalogDrag && catalogDrag.moved && overCanvas) {
        worldPos = Render.screenToWorld(Store.state.view, { x: ev.clientX - rect.left, y: ev.clientY - rect.top });
      } else if (catalogDrag && !catalogDrag.moved) {
        worldPos = Render.screenToWorld(Store.state.view, { x: rect.width / 2, y: rect.height / 2 });
      } else {
        catalogDrag = null;
        return;
      }
      if (Store.state.settings.gridSnap) {
        worldPos = { x: Geo.snapToStep(worldPos.x, Store.state.settings.gridSize), y: Geo.snapToStep(worldPos.y, Store.state.settings.gridSize) };
      }
      const added = Store.addFurniture({
        catalogId: item.cat === 'Custom' ? null : item.id, label: item.label, kind: item.kind,
        w: item.w, d: item.d, x: worldPos.x, y: worldPos.y, rotation: 0, color: item.color
      });
      Store.setSelection('furniture', [added.id]);
      Store.pushHistory();
      setTool('select');
      catalogDrag = null;
    }
    rowEl.addEventListener('pointermove', move);
    rowEl.addEventListener('pointerup', up);
  }

  // ---------------- Properties panel ----------------
  function makeField(labelText, inputEl) {
    const div = document.createElement('div'); div.className = 'field';
    const label = document.createElement('label'); label.textContent = labelText;
    div.appendChild(label); div.appendChild(inputEl);
    return div;
  }
  function makeTextInput(id, value) {
    const inp = document.createElement('input'); inp.type = 'text'; inp.id = id; inp.value = value;
    return inp;
  }
  function bindLengthInput(input, setter, getter, units) {
    function commit() {
      const v = Geo.parseLength(input.value, units);
      if (v != null && v > 0) { setter(v); Store.pushHistory(); }
      input.value = Geo.formatLength(getter(), units);
    }
    input.addEventListener('change', commit);
    input.addEventListener('keydown', e => { if (e.key === 'Enter') { commit(); input.blur(); } });
  }

  function renderProps() {
    const sel = Store.state.selection;
    const root = qs('propsContent');
    root.innerHTML = '';
    if (!sel.type || !sel.ids.length) {
      root.innerHTML = '<p class="muted">Select a room, wall, door/window or furniture item to edit its properties.</p>';
      return;
    }
    if (sel.type === 'furniture') {
      if (sel.ids.length === 1) renderFurnitureProps(root, Store.getFurniture(sel.ids[0]));
      else renderMultiFurnitureProps(root, sel.ids);
    } else if (sel.type === 'room') {
      renderRoomProps(root, Store.getRoom(sel.ids[0]));
    } else if (sel.type === 'opening') {
      renderOpeningProps(root, Store.getOpening(sel.ids[0]));
    }
  }

  function renderFurnitureProps(root, item) {
    if (!item) return;
    const units = Store.state.settings.units;

    const labelInput = makeTextInput('propLabel', item.label);
    labelInput.addEventListener('change', () => { item.label = labelInput.value || item.label; Store.pushHistory(); requestRender(); });
    root.appendChild(makeField('Name', labelInput));

    const row1 = document.createElement('div'); row1.className = 'field-row';
    const wInput = makeTextInput('propW', Geo.formatLength(item.w, units));
    const dInput = makeTextInput('propD', Geo.formatLength(item.d, units));
    bindLengthInput(wInput, v => { item.w = v; requestRender(); }, () => item.w, units);
    bindLengthInput(dInput, v => { item.d = v; requestRender(); }, () => item.d, units);
    row1.appendChild(makeField('Width', wInput));
    row1.appendChild(makeField('Depth', dInput));
    root.appendChild(row1);

    const row2 = document.createElement('div'); row2.className = 'field-row';
    const xInput = makeTextInput('propX', Geo.formatLength(item.x, units));
    const yInput = makeTextInput('propY', Geo.formatLength(item.y, units));
    bindLengthInput(xInput, v => { item.x = v; requestRender(); }, () => item.x, units);
    bindLengthInput(yInput, v => { item.y = v; requestRender(); }, () => item.y, units);
    row2.appendChild(makeField('X', xInput));
    row2.appendChild(makeField('Y', yInput));
    root.appendChild(row2);

    const rotInput = document.createElement('input');
    rotInput.type = 'number'; rotInput.id = 'propRot'; rotInput.value = Math.round(item.rotation); rotInput.step = 1;
    rotInput.addEventListener('change', () => { item.rotation = Geo.normalizeDeg(parseFloat(rotInput.value) || 0); requestRender(); Store.pushHistory(); });
    root.appendChild(makeField('Rotation (°)', rotInput));

    const swatches = document.createElement('div'); swatches.className = 'color-swatches';
    PRESET_COLORS.forEach(c => {
      const sw = document.createElement('div');
      sw.className = 'color-swatch' + (item.color === c ? ' selected' : '');
      sw.style.background = c;
      sw.addEventListener('click', () => { item.color = c; requestRender(); Store.pushHistory(); renderProps(); });
      swatches.appendChild(sw);
    });
    root.appendChild(makeField('Color', swatches));

    const dims = document.createElement('p'); dims.className = 'muted';
    dims.textContent = `Footprint: ${Math.round(item.w)} × ${Math.round(item.d)} cm`;
    root.appendChild(dims);

    const btnRow = document.createElement('div'); btnRow.className = 'btn-row';
    const dupBtn = document.createElement('button'); dupBtn.className = 'text-btn'; dupBtn.textContent = 'Duplicate';
    dupBtn.addEventListener('click', () => Input.duplicateSelection());
    const delBtn = document.createElement('button'); delBtn.className = 'text-btn danger'; delBtn.textContent = 'Delete';
    delBtn.addEventListener('click', () => Input.deleteSelection());
    btnRow.appendChild(dupBtn); btnRow.appendChild(delBtn);
    root.appendChild(btnRow);
  }

  function renderMultiFurnitureProps(root, ids) {
    const p = document.createElement('p'); p.className = 'muted'; p.textContent = `${ids.length} items selected`;
    root.appendChild(p);
    const btnRow = document.createElement('div'); btnRow.className = 'btn-row';
    const dupBtn = document.createElement('button'); dupBtn.className = 'text-btn'; dupBtn.textContent = 'Duplicate';
    dupBtn.addEventListener('click', () => Input.duplicateSelection());
    const delBtn = document.createElement('button'); delBtn.className = 'text-btn danger'; delBtn.textContent = 'Delete';
    delBtn.addEventListener('click', () => Input.deleteSelection());
    btnRow.appendChild(dupBtn); btnRow.appendChild(delBtn);
    root.appendChild(btnRow);
  }

  function renderRoomProps(root, room) {
    if (!room) return;
    const units = Store.state.settings.units;

    const nameInput = makeTextInput('propRoomName', room.name);
    nameInput.addEventListener('change', () => { room.name = nameInput.value || room.name; Store.pushHistory(); renderRoomList(); });
    root.appendChild(makeField('Name', nameInput));

    const thickInput = makeTextInput('propThickness', Geo.formatLength(room.thickness, units));
    bindLengthInput(thickInput, v => { room.thickness = Math.max(2, v); requestRender(); }, () => room.thickness, units);
    root.appendChild(makeField('Wall thickness', thickInput));

    const wallsTitle = document.createElement('h3'); wallsTitle.textContent = 'Wall lengths'; wallsTitle.style.marginTop = '12px';
    root.appendChild(wallsTitle);
    const n = room.points.length;
    for (let i = 0; i < (n >= 3 ? n : n - 1); i++) {
      const a = room.points[i], b = room.points[(i + 1) % n];
      const len = Geo.dist(a, b);
      const inp = makeTextInput('propWallLen_' + i, Geo.formatLength(len, units));
      inp.dataset.edgeIndex = i;
      inp.addEventListener('change', (() => {
        const idx = i;
        return () => {
          const a2 = room.points[idx], b2 = room.points[(idx + 1) % room.points.length];
          const curLen = Geo.dist(a2, b2);
          const v = Geo.parseLength(inp.value, units);
          if (v != null && v > 1 && curLen > 0.001) {
            const ux = (b2.x - a2.x) / curLen, uy = (b2.y - a2.y) / curLen;
            room.points[(idx + 1) % room.points.length] = { x: a2.x + ux * v, y: a2.y + uy * v };
            Store.pushHistory();
            requestRender();
            renderRoomProps(root, room);
          } else {
            inp.value = Geo.formatLength(curLen, units);
          }
        };
      })());
      inp.addEventListener('keydown', e => { if (e.key === 'Enter') inp.blur(); });
      root.appendChild(makeField(`Wall ${i + 1}`, inp));
    }

    const area = Geo.polygonArea(room.points);
    const perim = Geo.polygonPerimeter(room.points);
    const stats = document.createElement('div');
    stats.innerHTML = `<div class="stat-line"><span>Area</span><span>${Geo.formatArea(area, units)}</span></div>` +
      `<div class="stat-line"><span>Perimeter</span><span>${Geo.formatLength(perim, units)}</span></div>`;
    root.appendChild(stats);

    const btnRow = document.createElement('div'); btnRow.className = 'btn-row'; btnRow.style.marginTop = '10px';
    const delBtn = document.createElement('button'); delBtn.className = 'text-btn danger'; delBtn.textContent = 'Delete room';
    delBtn.addEventListener('click', () => Input.deleteSelection());
    btnRow.appendChild(delBtn);
    root.appendChild(btnRow);
  }

  function renderOpeningProps(root, op) {
    if (!op) return;
    const units = Store.state.settings.units;
    const room = Store.getRoom(op.roomId);
    if (!room) return;

    const badge = document.createElement('p'); badge.className = 'muted';
    badge.textContent = (op.type === 'door' ? 'Door' : 'Window') + ' on ' + room.name;
    root.appendChild(badge);

    const widthInput = makeTextInput('propOpWidth', Geo.formatLength(op.width, units));
    bindLengthInput(widthInput, v => {
      const a = room.points[op.edgeIndex], b = room.points[(op.edgeIndex + 1) % room.points.length];
      const edgeLen = Geo.dist(a, b);
      const margin = room.thickness / 2 + 2;
      op.width = Math.max(20, Math.min(v, edgeLen - 2 * margin));
      op.offset = Math.max(margin + op.width / 2, Math.min(edgeLen - margin - op.width / 2, op.offset));
      requestRender();
    }, () => op.width, units);
    root.appendChild(makeField('Width', widthInput));

    const offsetInput = makeTextInput('propOpOffset', Geo.formatLength(op.offset, units));
    bindLengthInput(offsetInput, v => {
      const a = room.points[op.edgeIndex], b = room.points[(op.edgeIndex + 1) % room.points.length];
      const edgeLen = Geo.dist(a, b);
      const margin = room.thickness / 2 + 2;
      op.offset = Math.max(margin + op.width / 2, Math.min(edgeLen - margin - op.width / 2, v));
      requestRender();
    }, () => op.offset, units);
    root.appendChild(makeField('Distance from corner', offsetInput));

    if (op.type === 'door') {
      const swingRow = document.createElement('div'); swingRow.className = 'btn-row';
      const leftBtn = document.createElement('button'); leftBtn.className = 'text-btn' + (op.swing === 'left' ? ' primary' : ''); leftBtn.textContent = 'Hinge left';
      const rightBtn = document.createElement('button'); rightBtn.className = 'text-btn' + (op.swing === 'right' ? ' primary' : ''); rightBtn.textContent = 'Hinge right';
      leftBtn.addEventListener('click', () => { op.swing = 'left'; Store.pushHistory(); requestRender(); renderProps(); });
      rightBtn.addEventListener('click', () => { op.swing = 'right'; Store.pushHistory(); requestRender(); renderProps(); });
      swingRow.appendChild(leftBtn); swingRow.appendChild(rightBtn);
      root.appendChild(makeField('Hinge side', swingRow));

      const flipBtn = document.createElement('button'); flipBtn.className = 'text-btn'; flipBtn.textContent = 'Flip swing direction';
      flipBtn.style.width = '100%';
      flipBtn.addEventListener('click', () => { op.flip = !op.flip; Store.pushHistory(); requestRender(); });
      root.appendChild(flipBtn);
    }

    const btnRow = document.createElement('div'); btnRow.className = 'btn-row'; btnRow.style.marginTop = '10px';
    const delBtn = document.createElement('button'); delBtn.className = 'text-btn danger'; delBtn.textContent = 'Delete';
    delBtn.addEventListener('click', () => Input.deleteSelection());
    btnRow.appendChild(delBtn);
    root.appendChild(btnRow);
  }

  function setIfExists(id, value) {
    const el = qs(id);
    if (el && document.activeElement !== el) el.value = value;
  }
  function refreshPropsLive() {
    const sel = Store.state.selection;
    const units = Store.state.settings.units;
    if (sel.type === 'furniture' && sel.ids.length === 1) {
      const item = Store.getFurniture(sel.ids[0]);
      if (!item) return;
      setIfExists('propW', Geo.formatLength(item.w, units));
      setIfExists('propD', Geo.formatLength(item.d, units));
      setIfExists('propX', Geo.formatLength(item.x, units));
      setIfExists('propY', Geo.formatLength(item.y, units));
      setIfExists('propRot', Math.round(item.rotation));
    } else if (sel.type === 'opening') {
      const op = Store.getOpening(sel.ids[0]);
      if (!op) return;
      setIfExists('propOpOffset', Geo.formatLength(op.offset, units));
    } else if (sel.type === 'room' && sel.ids.length === 1) {
      const room = Store.getRoom(sel.ids[0]);
      if (!room) return;
      const n = room.points.length;
      for (let i = 0; i < (n >= 3 ? n : n - 1); i++) {
        const a = room.points[i], b = room.points[(i + 1) % n];
        setIfExists('propWallLen_' + i, Geo.formatLength(Geo.dist(a, b), units));
      }
    }
  }

  // ---------------- Room list / stats ----------------
  function renderRoomList() {
    const root = qs('roomList');
    root.innerHTML = '';
    if (!Store.state.rooms.length) {
      root.innerHTML = '<p class="muted">No rooms yet — use Draw Room.</p>';
      return;
    }
    for (const room of Store.state.rooms) {
      const row = document.createElement('div');
      row.className = 'room-row' + (Store.state.selection.type === 'room' && Store.state.selection.ids.includes(room.id) ? ' selected' : '');
      const name = document.createElement('span'); name.textContent = room.name; name.style.flex = '1';
      row.appendChild(name);
      const del = document.createElement('button'); del.className = 'del-btn'; del.textContent = '✕'; del.title = 'Delete room';
      del.addEventListener('click', (e) => {
        e.stopPropagation();
        Store.removeRoom(room.id);
        if (Store.state.selection.ids.includes(room.id)) Store.clearSelection();
        Store.pushHistory();
      });
      row.appendChild(del);
      row.addEventListener('click', () => Store.setSelection('room', [room.id]));
      root.appendChild(row);
    }
  }

  function renderStats() {
    const root = qs('roomStats');
    const units = Store.state.settings.units;
    let totalArea = 0;
    for (const r of Store.state.rooms) totalArea += Geo.polygonArea(r.points);
    root.innerHTML = `<h3>Overview</h3>` +
      `<div class="stat-line"><span>Rooms</span><span>${Store.state.rooms.length}</span></div>` +
      `<div class="stat-line"><span>Total floor area</span><span>${Geo.formatArea(totalArea, units)}</span></div>` +
      `<div class="stat-line"><span>Furniture items</span><span>${Store.state.furniture.length}</span></div>`;
  }

  function updateUndoRedoButtons() {
    qs('undoBtn').disabled = !Store.canUndo();
    qs('redoBtn').disabled = !Store.canRedo();
  }

  function refreshAll() {
    renderRoomList();
    renderStats();
    renderProps();
    updateUndoRedoButtons();
    requestRender();
  }

  // ---------------- Status / hint / zoom ----------------
  function updateStatus(worldPoint) {
    const units = Store.state.settings.units;
    qs('statusCoords').textContent = `${Geo.formatLength(worldPoint.x, units)}, ${Geo.formatLength(worldPoint.y, units)}`;
  }
  function updateZoomLabel() {
    qs('zoomResetBtn').textContent = Math.round(Store.state.view.scale / 2.2 * 100) + '%';
  }
  function flashHint(msg, ms) {
    const el = qs('hint');
    el.textContent = msg;
    el.classList.add('show');
    if (hintTimer) clearTimeout(hintTimer);
    hintTimer = setTimeout(() => {
      const cur = Render.T.tool;
      if (cur !== 'select' && TOOL_HINTS[cur]) { el.textContent = TOOL_HINTS[cur]; }
      else el.classList.remove('show');
    }, ms || 1800);
  }

  function setTool(tool) {
    if (Render.T.tool === 'room' && tool !== 'room' && Render.T.drawingPoints.length) {
      Input.cancelRoomDraw();
    }
    Render.T.tool = tool;
    Render.T.placingOpening = null;
    document.querySelectorAll('.tool-btn').forEach(b => b.classList.toggle('active', b.dataset.tool === tool));
    const canvasEl = qs('canvas');
    canvasEl.classList.remove('tool-room', 'tool-door', 'tool-window');
    if (tool !== 'select') canvasEl.classList.add('tool-' + tool);
    qs('statusTool').textContent = tool.charAt(0).toUpperCase() + tool.slice(1);
    const hintText = TOOL_HINTS[tool] || '';
    const el = qs('hint');
    if (hintText) { el.textContent = hintText; el.classList.add('show'); }
    else el.classList.remove('show');
    requestRender();
  }

  function toggleGridSnap() {
    Store.state.settings.gridSnap = !Store.state.settings.gridSnap;
    qs('gridSnapToggle').checked = Store.state.settings.gridSnap;
    Store.scheduleAutosave();
    requestRender();
  }

  // ---------------- Modals ----------------
  function closeModal() { qs('modalOverlay').classList.add('hidden'); qs('modalBox').innerHTML = ''; }
  function defaultLayoutName() {
    const d = new Date();
    return 'Layout ' + d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  function openSaveModal() {
    const box = qs('modalBox');
    box.innerHTML = `<h2>Save layout</h2><input type="text" id="saveNameInput" placeholder="Layout name">` +
      `<div class="modal-actions"><button class="text-btn" id="saveCancelBtn">Cancel</button><button class="text-btn primary" id="saveConfirmBtn">Save</button></div>`;
    qs('modalOverlay').classList.remove('hidden');
    const nameInput = qs('saveNameInput');
    nameInput.value = defaultLayoutName();
    nameInput.focus(); nameInput.select();
    qs('saveCancelBtn').onclick = closeModal;
    qs('saveConfirmBtn').onclick = () => {
      const name = nameInput.value.trim() || defaultLayoutName();
      Store.saveLayout(name);
      closeModal();
      flashHint('Saved "' + name + '"');
    };
    nameInput.addEventListener('keydown', e => { if (e.key === 'Enter') qs('saveConfirmBtn').click(); });
  }

  function openLoadModal() {
    const layouts = Store.listLayouts();
    const names = Object.keys(layouts).sort((a, b) => (layouts[b].savedAt || 0) - (layouts[a].savedAt || 0));
    const box = qs('modalBox');
    if (!names.length) {
      box.innerHTML = `<h2>Load layout</h2><p class="muted">No saved layouts yet.</p>` +
        `<div class="modal-actions"><button class="text-btn" id="loadCloseBtn">Close</button></div>`;
    } else {
      const rows = names.map(n => `<div class="layout-row" data-name="${escapeHtml(n)}" style="cursor:pointer">` +
        `<span class="name">${escapeHtml(n)}</span><span class="date">${new Date(layouts[n].savedAt || 0).toLocaleDateString()}</span>` +
        `<button class="del-btn" data-del="${escapeHtml(n)}" title="Delete">✕</button></div>`).join('');
      box.innerHTML = `<h2>Load layout</h2>${rows}<div class="modal-actions"><button class="text-btn" id="loadCloseBtn">Close</button></div>`;
      box.querySelectorAll('.layout-row').forEach(rowEl => {
        rowEl.addEventListener('click', (e) => {
          if (e.target.closest('.del-btn')) return;
          const name = rowEl.dataset.name;
          Store.loadLayout(name);
          closeModal();
          flashHint('Loaded "' + name + '"');
          Input.zoomToFit();
        });
      });
      box.querySelectorAll('.del-btn').forEach(btn => {
        btn.addEventListener('click', (e) => { e.stopPropagation(); Store.deleteLayout(btn.dataset.del); openLoadModal(); });
      });
    }
    qs('modalOverlay').classList.remove('hidden');
    qs('loadCloseBtn').onclick = closeModal;
  }

  function openConfirmModal(message, onConfirm) {
    const box = qs('modalBox');
    box.innerHTML = `<h2>Are you sure?</h2><p class="muted">${escapeHtml(message)}</p>` +
      `<div class="modal-actions"><button class="text-btn" id="confirmCancelBtn">Cancel</button><button class="text-btn danger" id="confirmOkBtn">Confirm</button></div>`;
    qs('modalOverlay').classList.remove('hidden');
    qs('confirmCancelBtn').onclick = closeModal;
    qs('confirmOkBtn').onclick = () => { closeModal(); onConfirm(); };
  }

  // ---------------- Dropdown menus ----------------
  function closeAllDropdowns() {
    document.querySelectorAll('.dropdown-menu').forEach(m => { m.hidden = true; });
    document.querySelectorAll('.dropdown > button').forEach(b => b.setAttribute('aria-expanded', 'false'));
  }
  function setupDropdown(btnId, menuId, opts) {
    const btn = qs(btnId), menu = qs(menuId);
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const isOpen = !menu.hidden;
      closeAllDropdowns();
      if (!isOpen) { menu.hidden = false; btn.setAttribute('aria-expanded', 'true'); }
    });
    if (opts && opts.closeOnItemClick) {
      menu.addEventListener('click', (e) => { if (e.target.closest('.dd-item')) closeAllDropdowns(); });
    }
  }
  function wireDropdowns() {
    setupDropdown('settingsBtn', 'settingsMenu');
    setupDropdown('fileMenuBtn', 'fileMenu', { closeOnItemClick: true });
    document.addEventListener('click', (e) => { if (!e.target.closest('.dropdown')) closeAllDropdowns(); });
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeAllDropdowns(); });
  }

  // ---------------- Collapsible side panels ----------------
  function wirePanelToggles() {
    const catalogBtn = qs('catalogToggleBtn'), catalogPanel = qs('catalogPanel');
    const propsBtn = qs('propsToggleBtn'), propsPanel = qs('propertiesPanel');
    catalogBtn.addEventListener('click', () => {
      const hidden = catalogPanel.classList.toggle('panel-hidden');
      catalogBtn.textContent = hidden ? '›' : '‹';
      catalogBtn.title = hidden ? 'Show furniture catalog' : 'Hide furniture catalog';
      window.resizeCanvas();
    });
    propsBtn.addEventListener('click', () => {
      const hidden = propsPanel.classList.toggle('panel-hidden');
      propsBtn.textContent = hidden ? '‹' : '›';
      propsBtn.title = hidden ? 'Show properties panel' : 'Hide properties panel';
      window.resizeCanvas();
    });
  }

  // ---------------- Toolbar wiring ----------------
  function wireToolbar() {
    document.querySelectorAll('.tool-btn').forEach(btn => {
      btn.addEventListener('click', () => setTool(btn.dataset.tool));
    });
    qs('undoBtn').addEventListener('click', () => { Store.undo(); refreshAll(); });
    qs('redoBtn').addEventListener('click', () => { Store.redo(); refreshAll(); });

    qs('gridSnapToggle').addEventListener('change', (e) => {
      Store.state.settings.gridSnap = e.target.checked;
      Store.scheduleAutosave();
    });
    qs('gridSizeSelect').addEventListener('change', (e) => {
      Store.state.settings.gridSize = Number(e.target.value);
      Store.scheduleAutosave();
      requestRender();
    });
    qs('angleSnapToggle').addEventListener('change', (e) => {
      Store.state.settings.angleSnap = e.target.checked;
      Store.scheduleAutosave();
    });
    qs('unitsSelect').addEventListener('change', (e) => {
      Store.state.settings.units = e.target.value;
      Store.scheduleAutosave();
      refreshAll();
    });

    qs('saveBtn').addEventListener('click', openSaveModal);
    qs('loadBtn').addEventListener('click', openLoadModal);
    qs('exportJsonBtn').addEventListener('click', () => {
      const blob = new Blob([Store.exportJson()], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = 'room-plan.json';
      document.body.appendChild(a); a.click(); document.body.removeChild(a);
      URL.revokeObjectURL(url);
    });
    qs('importJsonBtn').addEventListener('click', () => qs('importFileInput').click());
    qs('importFileInput').addEventListener('change', (e) => {
      const file = e.target.files[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = () => {
        try { Store.importJson(reader.result); refreshAll(); Input.zoomToFit(); flashHint('Imported "' + file.name + '"'); }
        catch (err) { flashHint('Could not read that file'); }
      };
      reader.readAsText(file);
      e.target.value = '';
    });
    qs('exportPngBtn').addEventListener('click', exportPng);
    qs('clearBtn').addEventListener('click', () => {
      openConfirmModal('This will delete all rooms, doors/windows and furniture.', () => { Store.clearAll(); refreshAll(); });
    });

    qs('zoomInBtn').addEventListener('click', () => Input.zoomBy(1.25));
    qs('zoomOutBtn').addEventListener('click', () => Input.zoomBy(1 / 1.25));
    qs('zoomResetBtn').addEventListener('click', () => Input.resetZoom());
    qs('zoomFitBtn').addEventListener('click', () => Input.zoomToFit());

    qs('modalOverlay').addEventListener('click', (e) => { if (e.target.id === 'modalOverlay') closeModal(); });

    qs('catalogSearch').addEventListener('input', (e) => renderCatalog(e.target.value));

    qs('customAddBtn').addEventListener('click', () => {
      const label = qs('customLabel').value.trim();
      const units = Store.state.settings.units;
      const w = Geo.parseLength(qs('customWidth').value, units);
      const d = Geo.parseLength(qs('customDepth').value, units);
      if (!label || !w || !d || w <= 0 || d <= 0) { flashHint('Enter a name and valid width/depth'); return; }
      const shape = qs('customShape').value;
      const color = qs('customColor').value;
      Catalog.addCustomItem({ label, w, d, kind: shape, color });
      qs('customLabel').value = ''; qs('customWidth').value = ''; qs('customDepth').value = '';
      renderCatalog(qs('catalogSearch').value);
      qs('customItemDetails').open = false;
      flashHint('Added "' + label + '" to catalog');
    });
  }

  function exportPng() {
    const pts = [];
    for (const r of Store.state.rooms) pts.push(...r.points);
    for (const f of Store.state.furniture) {
      const box = Geo.aabbOfRotatedRect(f.x, f.y, f.w, f.d, f.rotation);
      pts.push({ x: box.x1, y: box.y1 }, { x: box.x2, y: box.y2 });
    }
    if (!pts.length) { flashHint('Nothing to export yet'); return; }
    const xs = pts.map(p => p.x), ys = pts.map(p => p.y);
    const margin = 60;
    const minX = Math.min(...xs) - margin, maxX = Math.max(...xs) + margin;
    const minY = Math.min(...ys) - margin, maxY = Math.max(...ys) + margin;
    const pxPerCm = 4;
    const w = Math.min(6000, Math.ceil((maxX - minX) * pxPerCm));
    const h = Math.min(6000, Math.ceil((maxY - minY) * pxPerCm));
    const off = document.createElement('canvas');
    off.width = w; off.height = h;
    const offCtx = off.getContext('2d');

    const realView = Store.state.view;
    const realSelection = Store.state.selection;
    const savedT = Object.assign({}, Render.T);
    Store.state.view = { scale: pxPerCm, panX: -minX * pxPerCm, panY: -minY * pxPerCm };
    Store.state.selection = { type: null, ids: [] };
    Render.T.drawingPoints = []; Render.T.previewPoint = null; Render.T.guides = [];
    Render.T.rubberBand = null; Render.T.placingOpening = null; Render.T.closeHint = false;

    const fakeCanvas = { clientWidth: w, clientHeight: h };
    Render.render(fakeCanvas, offCtx, Store);

    Store.state.view = realView;
    Store.state.selection = realSelection;
    Object.assign(Render.T, savedT);
    requestRender();

    off.toBlob((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = 'room-plan.png';
      document.body.appendChild(a); a.click(); document.body.removeChild(a);
      URL.revokeObjectURL(url);
    });
  }

  function init() {
    wireToolbar();
    wireDropdowns();
    wirePanelToggles();
    renderCatalog('');
    Store.subscribe(() => refreshAll());
    refreshAll();
    updateZoomLabel();
    setTool('select');
  }

  return {
    init, setTool, refreshAll, refreshPropsLive, updateStatus, updateZoomLabel,
    flashHint, toggleGridSnap, renderCatalog
  };
})();
