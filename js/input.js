// Pointer & keyboard interaction: drawing, editing, dragging, snapping.
'use strict';

const Input = (() => {
  let canvas;
  let dragMode = 'none';
  let dragData = {};
  let spaceDown = false;
  let lastPanPoint = null;
  const HANDLE_HIT_PX = 9;
  const VERTEX_HIT_PX = 10;
  const VERTEX_SNAP_PX = 12;
  const ALIGN_SNAP_PX = 7;
  const ROOM_HIT_PX = 8;
  const OPENING_HIT_PX = 11;

  function view() { return Store.state.view; }
  function settings() { return Store.state.settings; }

  function canvasPoint(e) {
    const rect = canvas.getBoundingClientRect();
    return { x: e.clientX - rect.left, y: e.clientY - rect.top };
  }
  function worldPoint(e) { return Render.screenToWorld(view(), canvasPoint(e)); }

  function gridSnapPoint(p) {
    if (!settings().gridSnap) return p;
    const g = settings().gridSize;
    return { x: Geo.snapToStep(p.x, g), y: Geo.snapToStep(p.y, g) };
  }

  function findNearestVertex(p, excludeRoomId, excludeIndex) {
    let best = null;
    for (const room of Store.state.rooms) {
      for (let i = 0; i < room.points.length; i++) {
        if (room.id === excludeRoomId && i === excludeIndex) continue;
        const v = room.points[i];
        const d = Geo.dist(p, v) * view().scale;
        if (d <= VERTEX_SNAP_PX && (!best || d < best.d)) best = { d, point: { x: v.x, y: v.y } };
      }
    }
    return best ? best.point : null;
  }

  function snapDrawPoint(raw, prev) {
    const vsnap = findNearestVertex(raw, null, null);
    if (vsnap) return vsnap;
    let p = raw;
    if (prev && settings().angleSnap) {
      const dx = raw.x - prev.x, dy = raw.y - prev.y;
      const len = Math.hypot(dx, dy);
      if (len > 0.001) {
        const angle = Geo.snapAngleDeg(Math.atan2(dy, dx) / Geo.DEG, 15) * Geo.DEG;
        p = { x: prev.x + Math.cos(angle) * len, y: prev.y + Math.sin(angle) * len };
      }
    }
    return gridSnapPoint(p);
  }

  // ---------- Hit testing ----------
  function hitFurnitureHandles(item) {
    const { cx, cy, w, h } = Render.worldToScreen ? furnitureRectScreen(item) : null;
    return null; // unused, see below
  }

  function furnitureRectScreen(item) {
    const c = Render.worldToScreen(view(), { x: item.x, y: item.y });
    return { cx: c.x, cy: c.y, w: item.w * view().scale, h: item.d * view().scale };
  }

  function hitResizeHandle(item, screenPt) {
    const { cx, cy, w, h } = furnitureRectScreen(item);
    const rot = item.rotation * Geo.DEG;
    const cos = Math.cos(rot), sin = Math.sin(rot);
    const local = {
      x: (screenPt.x - cx) * cos + (screenPt.y - cy) * sin,
      y: -(screenPt.x - cx) * sin + (screenPt.y - cy) * cos,
    };
    const positions = [
      [-w / 2 - 4, -h / 2 - 4], [0, -h / 2 - 4], [w / 2 + 4, -h / 2 - 4],
      [-w / 2 - 4, 0], [w / 2 + 4, 0],
      [-w / 2 - 4, h / 2 + 4], [0, h / 2 + 4], [w / 2 + 4, h / 2 + 4],
    ];
    for (let i = 0; i < positions.length; i++) {
      const [hx, hy] = positions[i];
      if (Math.abs(local.x - hx) <= HANDLE_HIT_PX && Math.abs(local.y - hy) <= HANDLE_HIT_PX) return i;
    }
    const rh = -h / 2 - 28;
    if (Math.abs(local.x - 0) <= HANDLE_HIT_PX + 2 && Math.abs(local.y - rh) <= HANDLE_HIT_PX + 2) return 'rotate';
    return null;
  }

  function hitRoomVertex(room, screenPt) {
    for (let i = 0; i < room.points.length; i++) {
      const sp = Render.worldToScreen(view(), room.points[i]);
      if (Math.hypot(sp.x - screenPt.x, sp.y - screenPt.y) <= VERTEX_HIT_PX) return i;
    }
    return null;
  }

  function hitRoomEdgeMidpoint(room, screenPt) {
    const n = room.points.length;
    for (let i = 0; i < n; i++) {
      if (n < 3 && i === n - 1) continue;
      const a = room.points[i], b = room.points[(i + 1) % n];
      const mid = Geo.lerp(a, b, 0.5);
      const sp = Render.worldToScreen(view(), mid);
      if (Math.hypot(sp.x - screenPt.x, sp.y - screenPt.y) <= VERTEX_HIT_PX) return i;
    }
    return null;
  }

  function hitOpening(p) {
    let best = null;
    for (const op of Store.state.openings) {
      const room = Store.getRoom(op.roomId);
      if (!room) continue;
      const t = Render.openingTransform(room, op);
      const d = Geo.dist(p, t.pos) * view().scale;
      if (d <= OPENING_HIT_PX + op.width * view().scale / 2 && (!best || d < best.d)) best = { d, opening: op };
    }
    return best ? best.opening : null;
  }

  function hitFurniture(p) {
    const list = Store.state.furniture;
    for (let i = list.length - 1; i >= 0; i--) {
      const item = list[i];
      if (item.kind === 'rug') continue;
      if (Geo.pointInRotatedRect(p, item.x, item.y, item.w, item.d, item.rotation)) return item;
    }
    for (let i = list.length - 1; i >= 0; i--) {
      const item = list[i];
      if (item.kind !== 'rug') continue;
      if (Geo.pointInRotatedRect(p, item.x, item.y, item.w, item.d, item.rotation)) return item;
    }
    return null;
  }

  function hitRoom(p) {
    const rooms = Store.state.rooms;
    for (let i = rooms.length - 1; i >= 0; i--) {
      const room = rooms[i];
      if (room.points.length >= 3 && Geo.pointInPolygon(p, room.points)) return room;
      const edge = Geo.closestPolygonEdge(p, room.points);
      if (edge && edge.distance * view().scale <= Math.max(room.thickness * view().scale / 2, ROOM_HIT_PX)) return room;
    }
    return null;
  }

  // ---------- Snapping for furniture move ----------
  function buildAxisTargets(excludeIds) {
    const xT = [], yT = [];
    for (const room of Store.state.rooms) {
      const n = room.points.length;
      for (let i = 0; i < n; i++) {
        const a = room.points[i], b = room.points[(i + 1) % n];
        const dx = b.x - a.x, dy = b.y - a.y;
        const angle = Geo.normalizeDeg(Math.atan2(dy, dx) / Geo.DEG);
        const half = room.thickness / 2;
        if (Math.abs(angle % 180 - 90) < 8) {
          xT.push(a.x, a.x - half, a.x + half);
        } else if (Math.abs(angle % 180) < 8) {
          yT.push(a.y, a.y - half, a.y + half);
        }
      }
    }
    for (const f of Store.state.furniture) {
      if (excludeIds.includes(f.id)) continue;
      const box = Geo.aabbOfRotatedRect(f.x, f.y, f.w, f.d, f.rotation);
      xT.push(box.x1, (box.x1 + box.x2) / 2, box.x2);
      yT.push(box.y1, (box.y1 + box.y2) / 2, box.y2);
    }
    return { xT, yT };
  }

  function bestAxisSnap(selfValues, targets, thresholdWorld) {
    let best = null;
    for (const sv of selfValues) {
      for (const t of targets) {
        const d = Math.abs(t - sv);
        if (d <= thresholdWorld && (!best || d < best.d)) best = { d, delta: t - sv, target: t };
      }
    }
    return best;
  }

  function snapFurnitureMove(item, candidate, excludeIds, disable) {
    const guides = [];
    let { x, y } = candidate;
    if (!disable) {
      const { xT, yT } = buildAxisTargets(excludeIds);
      const box = Geo.aabbOfRotatedRect(x, y, item.w, item.d, item.rotation);
      const halfW = (box.x2 - box.x1) / 2, halfH = (box.y2 - box.y1) / 2;
      const thresholdWorld = ALIGN_SNAP_PX / view().scale;
      const sx = bestAxisSnap([x - halfW, x, x + halfW], xT, thresholdWorld);
      if (sx) { x += sx.delta; guides.push({ type: 'v', pos: sx.target }); }
      else if (settings().gridSnap) { x = Geo.snapToStep(x, settings().gridSize); }
      const sy = bestAxisSnap([y - halfH, y, y + halfH], yT, thresholdWorld);
      if (sy) { y += sy.delta; guides.push({ type: 'h', pos: sy.target }); }
      else if (settings().gridSnap) { y = Geo.snapToStep(y, settings().gridSize); }
    }
    return { x, y, guides };
  }

  // ---------- Pointer handlers ----------
  function onPointerDown(e) {
    if (e.target !== canvas) return;
    canvas.setPointerCapture(e.pointerId);
    const sp = canvasPoint(e);
    const wp = worldPoint(e);

    if (spaceDown || e.button === 1) {
      dragMode = 'pan';
      lastPanPoint = sp;
      canvas.classList.add('panning');
      return;
    }

    const tool = Render.T.tool;

    if (tool === 'room') {
      Render.T.lengthInputValue = null;
      const pts = Render.T.drawingPoints;
      if (pts.length >= 3) {
        const first = pts[0];
        const sfirst = Render.worldToScreen(view(), first);
        if (Math.hypot(sp.x - sfirst.x, sp.y - sfirst.y) <= VERTEX_SNAP_PX) {
          finishRoom();
          return;
        }
      }
      const prev = pts.length ? pts[pts.length - 1] : null;
      const snapped = snapDrawPoint(wp, prev);
      pts.push(snapped);
      requestRender();
      return;
    }

    if (tool === 'door' || tool === 'window') {
      placeOpening(tool, wp);
      return;
    }

    // select tool
    const sel = Store.state.selection;
    if (sel.type === 'room' && sel.ids.length === 1) {
      const room = Store.getRoom(sel.ids[0]);
      if (room) {
        const vi = hitRoomVertex(room, sp);
        if (vi !== null) {
          dragMode = 'vertex';
          dragData = { roomId: room.id, index: vi };
          return;
        }
        const ei = hitRoomEdgeMidpoint(room, sp);
        if (ei !== null) {
          const a = room.points[ei], b = room.points[(ei + 1) % room.points.length];
          const mid = Geo.lerp(a, b, 0.5);
          room.points.splice(ei + 1, 0, { x: mid.x, y: mid.y });
          dragMode = 'vertex';
          dragData = { roomId: room.id, index: ei + 1 };
          requestRender();
          return;
        }
      }
    }
    if (sel.type === 'furniture' && sel.ids.length === 1) {
      const item = Store.getFurniture(sel.ids[0]);
      if (item) {
        const h = hitResizeHandle(item, sp);
        if (h === 'rotate') {
          dragMode = 'rotate';
          dragData = { id: item.id };
          return;
        }
        if (h !== null) {
          const signs = [[-1, -1], [0, -1], [1, -1], [-1, 0], [1, 0], [-1, 1], [0, 1], [1, 1]][h];
          const [signX, signY] = signs;
          const anchorLocal = { x: signX === 0 ? 0 : -signX * item.w / 2, y: signY === 0 ? 0 : -signY * item.d / 2 };
          const anchorWorld = Geo.rotatePoint(Geo.add(anchorLocal, { x: item.x, y: item.y }), { x: item.x, y: item.y }, item.rotation);
          dragMode = 'resize';
          dragData = { id: item.id, signX, signY, anchorWorld, origW: item.w, origD: item.d, rotation: item.rotation };
          return;
        }
      }
    }

    const openHit = hitOpening(wp);
    if (openHit) {
      Store.setSelection('opening', [openHit.id]);
      dragMode = 'opening';
      dragData = { id: openHit.id };
      return;
    }

    const fHit = hitFurniture(wp);
    if (fHit) {
      if (e.shiftKey) {
        let ids = sel.type === 'furniture' ? sel.ids.slice() : [];
        if (ids.includes(fHit.id)) ids = ids.filter(id => id !== fHit.id);
        else ids.push(fHit.id);
        Store.setSelection('furniture', ids);
      } else if (!(sel.type === 'furniture' && sel.ids.includes(fHit.id))) {
        Store.setSelection('furniture', [fHit.id]);
      }
      const curSel = Store.state.selection;
      if (curSel.type === 'furniture' && curSel.ids.includes(fHit.id)) {
        dragMode = 'move';
        dragData = {
          ids: curSel.ids.slice(),
          startWorld: wp,
          starts: curSel.ids.map(id => { const f = Store.getFurniture(id); return { id, x: f.x, y: f.y }; }),
          primary: fHit.id,
          disable: e.altKey,
        };
      }
      return;
    }

    const rHit = hitRoom(wp);
    if (rHit) {
      Store.setSelection('room', [rHit.id]);
      dragMode = 'moveRoom';
      dragData = { roomId: rHit.id, startWorld: wp, origPoints: rHit.points.map(p => ({ x: p.x, y: p.y })) };
      return;
    }

    if (!e.shiftKey) Store.clearSelection();
    dragMode = 'rubberband';
    dragData = { x1: sp.x, y1: sp.y, keep: e.shiftKey ? Store.state.selection.ids.slice() : [] };
    Render.T.rubberBand = { x1: sp.x, y1: sp.y, x2: sp.x, y2: sp.y };
  }

  function finishRoom() {
    const pts = Render.T.drawingPoints;
    if (pts.length >= 3) {
      const room = Store.addRoom(pts.map(p => ({ x: p.x, y: p.y })), 12);
      Store.setSelection('room', [room.id]);
      Store.pushHistory();
    }
    Render.T.drawingPoints = [];
    Render.T.previewPoint = null;
    Render.T.closeHint = false;
    UI.setTool('select');
  }

  function cancelRoomDraw() {
    Render.T.drawingPoints = [];
    Render.T.previewPoint = null;
    Render.T.closeHint = false;
    requestRender();
  }

  function placeOpening(type, wp) {
    let best = null;
    for (const room of Store.state.rooms) {
      const edge = Geo.closestPolygonEdge(wp, room.points);
      if (edge && (!best || edge.distance < best.edge.distance)) best = { room, edge };
    }
    if (!best) { UI.flashHint('Draw a room first'); return; }
    const room = best.room, edge = best.edge;
    const edgeLen = Geo.dist(edge.a, edge.b);
    const margin = room.thickness / 2 + 2;
    if (edgeLen - 2 * margin < 20) { UI.flashHint('Wall too short here'); return; }
    let width = type === 'door' ? 80 : 100;
    width = Math.min(width, edgeLen - 2 * margin);
    let offset = Geo.snapToStep(edge.t * edgeLen, settings().gridSnap ? settings().gridSize : 1);
    offset = Math.max(margin + width / 2, Math.min(edgeLen - margin - width / 2, offset));
    const opening = Store.addOpening({
      roomId: room.id, edgeIndex: edge.index, offset, width, type,
      swing: 'right', flip: false
    });
    Store.setSelection('opening', [opening.id]);
    Store.pushHistory();
    dragMode = 'opening';
    dragData = { id: opening.id };
    requestRender();
  }

  function onPointerMove(e) {
    const sp = canvasPoint(e);
    const wp = worldPoint(e);
    UI.updateStatus(wp);

    if (dragMode === 'pan') {
      const dx = sp.x - lastPanPoint.x, dy = sp.y - lastPanPoint.y;
      view().panX += dx; view().panY += dy;
      lastPanPoint = sp;
      requestRender();
      return;
    }

    if (Render.T.tool === 'room' && Render.T.drawingPoints.length > 0) {
      const prev = Render.T.drawingPoints[Render.T.drawingPoints.length - 1];
      const snapped = snapDrawPoint(wp, prev);
      Render.T.previewPoint = snapped;
      const first = Render.T.drawingPoints[0];
      const sfirst = Render.worldToScreen(view(), first);
      Render.T.closeHint = Render.T.drawingPoints.length > 2 && Math.hypot(sp.x - sfirst.x, sp.y - sfirst.y) <= VERTEX_SNAP_PX;
      requestRender();
      return;
    }

    if ((Render.T.tool === 'door' || Render.T.tool === 'window') && dragMode === 'none') {
      let best = null;
      for (const room of Store.state.rooms) {
        const edge = Geo.closestPolygonEdge(wp, room.points);
        if (edge && (!best || edge.distance < best.edge.distance)) best = { room, edge };
      }
      if (best) {
        const edgeLen = Geo.dist(best.edge.a, best.edge.b);
        const width = Render.T.tool === 'door' ? 80 : 100;
        Render.T.placingOpening = { room: best.room, roomId: best.room.id, edgeIndex: best.edge.index, offset: Math.max(width / 2, Math.min(edgeLen - width / 2, best.edge.t * edgeLen)), width };
      } else Render.T.placingOpening = null;
      requestRender();
      return;
    }

    if (dragMode === 'move') {
      const f = Store.getFurniture(dragData.primary);
      const start = dragData.starts.find(s => s.id === dragData.primary);
      const dx = wp.x - dragData.startWorld.x, dy = wp.y - dragData.startWorld.y;
      const candidate = { x: start.x + dx, y: start.y + dy };
      const snapped = snapFurnitureMove(f, candidate, dragData.ids, dragData.disable || e.altKey);
      const realDx = snapped.x - start.x, realDy = snapped.y - start.y;
      for (const s of dragData.starts) {
        const item = Store.getFurniture(s.id);
        item.x = s.x + realDx;
        item.y = s.y + realDy;
      }
      Render.T.guides = snapped.guides;
      requestRender();
      return;
    }

    if (dragMode === 'resize') {
      const item = Store.getFurniture(dragData.id);
      const rel = Geo.sub(wp, dragData.anchorWorld);
      const local = Geo.rotatePoint(rel, { x: 0, y: 0 }, -dragData.rotation);
      const { signX, signY } = dragData;
      let newW = signX === 0 ? dragData.origW : Math.max(5, signX * local.x);
      let newD = signY === 0 ? dragData.origD : Math.max(5, signY * local.y);
      if (e.shiftKey && signX !== 0 && signY !== 0) {
        const ratio = dragData.origD / dragData.origW;
        newD = newW * ratio;
      }
      if (settings().gridSnap && !e.altKey) {
        if (signX !== 0) newW = Math.max(5, Geo.snapToStep(newW, settings().gridSize));
        if (signY !== 0) newD = Math.max(5, Geo.snapToStep(newD, settings().gridSize));
      }
      const offsetLocal = { x: signX === 0 ? 0 : signX * newW / 2, y: signY === 0 ? 0 : signY * newD / 2 };
      const newCenter = Geo.add(dragData.anchorWorld, Geo.rotatePoint(offsetLocal, { x: 0, y: 0 }, dragData.rotation));
      item.w = newW; item.d = newD; item.x = newCenter.x; item.y = newCenter.y;
      requestRender();
      UI.refreshPropsLive();
      return;
    }

    if (dragMode === 'rotate') {
      const item = Store.getFurniture(dragData.id);
      const angleToMouse = Math.atan2(wp.y - item.y, wp.x - item.x) / Geo.DEG;
      let rotation = angleToMouse + 90;
      if (!e.altKey) rotation = Geo.snapAngleDeg(rotation, 15);
      item.rotation = Geo.normalizeDeg(rotation);
      requestRender();
      UI.refreshPropsLive();
      return;
    }

    if (dragMode === 'vertex') {
      const room = Store.getRoom(dragData.roomId);
      let p = findNearestVertex(wp, room.id, dragData.index) || gridSnapPoint(wp);
      room.points[dragData.index] = p;
      requestRender();
      return;
    }

    if (dragMode === 'moveRoom') {
      const room = Store.getRoom(dragData.roomId);
      const dx0 = wp.x - dragData.startWorld.x, dy0 = wp.y - dragData.startWorld.y;
      const moved = dragData.origPoints[0];
      const snappedFirst = gridSnapPoint({ x: moved.x + dx0, y: moved.y + dy0 });
      const dx = snappedFirst.x - moved.x, dy = snappedFirst.y - moved.y;
      room.points = dragData.origPoints.map(p => ({ x: p.x + dx, y: p.y + dy }));
      requestRender();
      return;
    }

    if (dragMode === 'opening') {
      const op = Store.getOpening(dragData.id);
      const room = Store.getRoom(op.roomId);
      const a = room.points[op.edgeIndex], b = room.points[(op.edgeIndex + 1) % room.points.length];
      const edgeLen = Geo.dist(a, b);
      const res = Geo.closestPointOnSegment(wp, a, b);
      let offset = res.t * edgeLen;
      if (settings().gridSnap) offset = Geo.snapToStep(offset, settings().gridSize);
      const margin = room.thickness / 2 + 2;
      offset = Math.max(margin + op.width / 2, Math.min(edgeLen - margin - op.width / 2, offset));
      op.offset = offset;
      requestRender();
      UI.refreshPropsLive();
      return;
    }

    if (dragMode === 'rubberband') {
      Render.T.rubberBand.x2 = sp.x;
      Render.T.rubberBand.y2 = sp.y;
      const wx1 = Render.screenToWorld(view(), { x: Render.T.rubberBand.x1, y: Render.T.rubberBand.y1 });
      const wx2 = Render.screenToWorld(view(), { x: sp.x, y: sp.y });
      const rect = { x1: Math.min(wx1.x, wx2.x), y1: Math.min(wx1.y, wx2.y), x2: Math.max(wx1.x, wx2.x), y2: Math.max(wx1.y, wx2.y) };
      const ids = dragData.keep.slice();
      for (const f of Store.state.furniture) {
        const box = Geo.aabbOfRotatedRect(f.x, f.y, f.w, f.d, f.rotation);
        if (Geo.rectsOverlap(rect, box) && !ids.includes(f.id)) ids.push(f.id);
      }
      Store.setSelection(ids.length ? 'furniture' : null, ids);
      requestRender();
      return;
    }
  }

  function onPointerUp(e) {
    if (canvas.hasPointerCapture && canvas.hasPointerCapture(e.pointerId)) canvas.releasePointerCapture(e.pointerId);
    canvas.classList.remove('panning');
    if (['move', 'resize', 'rotate', 'vertex', 'moveRoom', 'opening'].includes(dragMode)) {
      Store.pushHistory();
    }
    Render.T.guides = [];
    Render.T.rubberBand = null;
    dragMode = 'none';
    dragData = {};
    requestRender();
  }

  function onWheel(e) {
    e.preventDefault();
    const sp = canvasPoint(e);
    if (e.ctrlKey || e.metaKey) {
      const factor = Math.exp(-e.deltaY * 0.0025);
      zoomAt(sp, factor);
    } else {
      const dx = e.shiftKey ? e.deltaY : e.deltaX;
      const dy = e.shiftKey ? 0 : e.deltaY;
      view().panX -= dx;
      view().panY -= dy;
      requestRender();
    }
  }

  function zoomAt(screenPt, factor) {
    const v = view();
    const before = Render.screenToWorld(v, screenPt);
    v.scale = Math.max(0.15, Math.min(20, v.scale * factor));
    const after = Render.worldToScreen(v, before);
    v.panX += screenPt.x - after.x;
    v.panY += screenPt.y - after.y;
    requestRender();
    UI.updateZoomLabel();
  }

  function zoomBy(factor) {
    const rect = canvas.getBoundingClientRect();
    zoomAt({ x: rect.width / 2, y: rect.height / 2 }, factor);
  }

  function resetZoom() {
    const v = view();
    v.scale = 2.2;
    requestRender();
    UI.updateZoomLabel();
  }

  function zoomToFit() {
    const v = view();
    const pts = [];
    for (const r of Store.state.rooms) pts.push(...r.points);
    for (const f of Store.state.furniture) {
      const box = Geo.aabbOfRotatedRect(f.x, f.y, f.w, f.d, f.rotation);
      pts.push({ x: box.x1, y: box.y1 }, { x: box.x2, y: box.y2 });
    }
    if (!pts.length) { resetZoom(); v.panX = 80; v.panY = 80; requestRender(); return; }
    const xs = pts.map(p => p.x), ys = pts.map(p => p.y);
    const minX = Math.min(...xs) - 60, maxX = Math.max(...xs) + 60;
    const minY = Math.min(...ys) - 60, maxY = Math.max(...ys) + 60;
    const rect = canvas.getBoundingClientRect();
    const scaleX = rect.width / (maxX - minX), scaleY = rect.height / (maxY - minY);
    v.scale = Math.max(0.15, Math.min(20, Math.min(scaleX, scaleY)));
    v.panX = -minX * v.scale + (rect.width - (maxX - minX) * v.scale) / 2;
    v.panY = -minY * v.scale + (rect.height - (maxY - minY) * v.scale) / 2;
    requestRender();
    UI.updateZoomLabel();
  }

  function nudgeSelection(dx, dy) {
    const sel = Store.state.selection;
    if (sel.type === 'furniture') {
      for (const id of sel.ids) {
        const f = Store.getFurniture(id);
        if (f) { f.x += dx; f.y += dy; }
      }
      requestRender();
      UI.refreshPropsLive();
    } else if (sel.type === 'room' && sel.ids.length === 1) {
      const room = Store.getRoom(sel.ids[0]);
      if (room) { room.points.forEach(p => { p.x += dx; p.y += dy; }); requestRender(); }
    } else if (sel.type === 'opening') {
      const op = Store.getOpening(sel.ids[0]);
      if (op) {
        const room = Store.getRoom(op.roomId);
        const a = room.points[op.edgeIndex], b = room.points[(op.edgeIndex + 1) % room.points.length];
        const edgeLen = Geo.dist(a, b);
        const along = (dx * (b.x - a.x) + dy * (b.y - a.y)) / edgeLen;
        const margin = room.thickness / 2 + 2;
        op.offset = Math.max(margin + op.width / 2, Math.min(edgeLen - margin - op.width / 2, op.offset + along));
        requestRender();
      }
    }
  }

  function rotateSelection(deltaDeg) {
    const sel = Store.state.selection;
    if (sel.type !== 'furniture') return;
    for (const id of sel.ids) {
      const f = Store.getFurniture(id);
      if (f) f.rotation = Geo.normalizeDeg(f.rotation + deltaDeg);
    }
    requestRender();
    UI.refreshPropsLive();
    Store.pushHistory();
  }

  function duplicateSelection() {
    const sel = Store.state.selection;
    if (sel.type !== 'furniture') return;
    const newIds = [];
    for (const id of sel.ids) {
      const f = Store.getFurniture(id);
      if (!f) continue;
      const copy = Object.assign({}, f, { x: f.x + 20, y: f.y + 20 });
      delete copy.id;
      const added = Store.addFurniture(copy);
      newIds.push(added.id);
    }
    Store.setSelection('furniture', newIds);
    Store.pushHistory();
    requestRender();
  }

  function deleteSelection() {
    const sel = Store.state.selection;
    if (sel.type === 'furniture') sel.ids.forEach(id => Store.removeFurniture(id));
    else if (sel.type === 'room') sel.ids.forEach(id => Store.removeRoom(id));
    else if (sel.type === 'opening') sel.ids.forEach(id => Store.removeOpening(id));
    if (sel.type) {
      Store.clearSelection();
      Store.pushHistory();
      requestRender();
    }
  }

  let nudgeKeyActive = false;

  function onKeyDown(e) {
    const tag = (e.target.tagName || '').toLowerCase();
    const inField = tag === 'input' || tag === 'select' || tag === 'textarea';

    if (e.code === 'Space' && !inField) { spaceDown = true; e.preventDefault(); return; }

    if (inField) return;

    if (Render.T.tool === 'room' && Render.T.drawingPoints.length > 0) {
      if (e.key === 'Escape' && Render.T.lengthInputValue) {
        Render.T.lengthInputValue = null;
        requestRender();
        e.preventDefault();
        return;
      }
      if (e.key === 'Enter' && Render.T.lengthInputValue) {
        const val = Geo.parseLength(Render.T.lengthInputValue, settings().units);
        if (val != null && Render.T.previewPoint) {
          const pts = Render.T.drawingPoints;
          const last = pts[pts.length - 1];
          const dx = Render.T.previewPoint.x - last.x, dy = Render.T.previewPoint.y - last.y;
          const len = Math.hypot(dx, dy) || 1;
          pts.push({ x: last.x + (dx / len) * val, y: last.y + (dy / len) * val });
        }
        Render.T.lengthInputValue = null;
        requestRender();
        e.preventDefault();
        return;
      }
      if (e.key === 'Backspace' && Render.T.lengthInputValue) {
        Render.T.lengthInputValue = Render.T.lengthInputValue.slice(0, -1) || null;
        requestRender();
        e.preventDefault();
        return;
      }
      if (e.key.length === 1 && /[0-9.'"a-zA-Z\- ]/.test(e.key) && !e.ctrlKey && !e.metaKey && !e.altKey) {
        Render.T.lengthInputValue = (Render.T.lengthInputValue || '') + e.key;
        requestRender();
        e.preventDefault();
        return;
      }
    }

    if (e.key === 'Escape') {
      if (Render.T.tool === 'room' && Render.T.drawingPoints.length) cancelRoomDraw();
      else if (Render.T.tool !== 'select') UI.setTool('select');
      else Store.clearSelection();
      return;
    }
    if (e.key === 'Enter') {
      if (Render.T.tool === 'room' && Render.T.drawingPoints.length >= 3) finishRoom();
      return;
    }
    if (e.key === 'Backspace' && Render.T.tool === 'room' && Render.T.drawingPoints.length) {
      Render.T.drawingPoints.pop();
      requestRender();
      e.preventDefault();
      return;
    }
    if ((e.key === 'Delete' || e.key === 'Backspace') && Store.state.selection.type) {
      deleteSelection();
      e.preventDefault();
      return;
    }
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') {
      if (e.shiftKey) Store.redo(); else Store.undo();
      UI.refreshAll();
      e.preventDefault();
      return;
    }
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'y') { Store.redo(); UI.refreshAll(); e.preventDefault(); return; }
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'd') { duplicateSelection(); e.preventDefault(); return; }

    if (['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'].includes(e.key)) {
      const step = e.shiftKey ? 1 : (e.ctrlKey || e.metaKey) ? settings().gridSize * 10 : settings().gridSize;
      const d = { ArrowLeft: [-step, 0], ArrowRight: [step, 0], ArrowUp: [0, -step], ArrowDown: [0, step] }[e.key];
      nudgeSelection(d[0], d[1]);
      nudgeKeyActive = true;
      e.preventDefault();
      return;
    }

    if (e.key.toLowerCase() === 'r' && Store.state.selection.type === 'furniture') {
      rotateSelection(e.altKey ? (e.shiftKey ? -1 : 1) : (e.shiftKey ? -15 : 15));
      e.preventDefault();
      return;
    }

    if (!e.ctrlKey && !e.metaKey) {
      if (e.key.toLowerCase() === 'v') UI.setTool('select');
      else if (e.key.toLowerCase() === 'b') UI.setTool('room');
      else if (e.key.toLowerCase() === 'd') UI.setTool('door');
      else if (e.key.toLowerCase() === 'w') UI.setTool('window');
      else if (e.key.toLowerCase() === 'g') UI.toggleGridSnap();
      else if (e.key === '+' || e.key === '=') { zoomBy(1.2); e.preventDefault(); }
      else if (e.key === '-' || e.key === '_') { zoomBy(1 / 1.2); e.preventDefault(); }
      else if (e.key === '0') resetZoom();
      else if (e.key.toLowerCase() === 'f') zoomToFit();
    }
  }

  function onKeyUp(e) {
    if (e.code === 'Space') spaceDown = false;
    if (['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'].includes(e.key) && nudgeKeyActive) {
      Store.pushHistory();
      nudgeKeyActive = false;
    }
  }

  function onDblClick(e) {
    if (e.target !== canvas) return;
    if (Render.T.tool !== 'select') return;
    const wp = worldPoint(e);
    const room = hitRoom(wp);
    if (room && !Geo.pointInPolygon(wp, room.points)) {
      const edge = Geo.closestPolygonEdge(wp, room.points);
      if (edge) {
        room.points.splice(edge.index + 1, 0, gridSnapPoint(edge.point));
        Store.setSelection('room', [room.id]);
        Store.pushHistory();
        requestRender();
      }
    }
  }

  function init(canvasEl) {
    canvas = canvasEl;
    canvas.addEventListener('pointerdown', onPointerDown);
    canvas.addEventListener('pointermove', onPointerMove);
    window.addEventListener('pointerup', onPointerUp);
    canvas.addEventListener('wheel', onWheel, { passive: false });
    canvas.addEventListener('dblclick', onDblClick);
    canvas.addEventListener('contextmenu', e => e.preventDefault());
    window.addEventListener('keydown', onKeyDown);
    window.addEventListener('keyup', onKeyUp);
  }

  return {
    init, zoomBy, resetZoom, zoomToFit, deleteSelection, duplicateSelection,
    rotateSelection, cancelRoomDraw, finishRoom,
    get dragMode() { return dragMode; }
  };
})();
