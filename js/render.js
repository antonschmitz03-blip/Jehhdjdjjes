// Canvas rendering: grid, rooms/walls, openings, furniture, selection & guides.
'use strict';

const Render = (() => {
  const COLORS = {
    bg: '#f6f5f2',
    gridMinor: 'rgba(30,30,30,0.06)',
    gridMajor: 'rgba(30,30,30,0.13)',
    floor: '#fdfcfa',
    floorAlt: '#f1ede6',
    wall: '#2c2b29',
    wallSelected: '#3b56d9',
    vertexHandle: '#3b56d9',
    edgeHandle: '#8a98ee',
    accent: '#3b56d9',
    accentSoft: 'rgba(59,86,217,0.15)',
    guide: '#ff4d6d',
    window: '#cfe3ee',
    windowBorder: '#7aa7c2',
    label: '#3a3a38',
    dim: 'rgba(58,58,56,0.55)',
  };

  // Transient (per-frame interaction) state, written by input.js.
  const T = {
    tool: 'select',
    drawingPoints: [],     // committed points while drawing a room
    previewPoint: null,    // current cursor world point while drawing
    closeHint: false,      // true if cursor is near the start point
    lengthInputValue: null,
    placingOpening: null,  // {type, roomId, edgeIndex, offset, width} preview
    guides: [],            // [{type:'v'|'h', pos}] alignment guides in world space
    wallSnap: null,        // {a,b} segment being snapped to, for highlight
    rubberBand: null,      // {x1,y1,x2,y2} in screen space
    hoverVertex: null,     // {roomId, index} nearest vertex while drawing
  };

  function worldToScreen(view, p) { return { x: p.x * view.scale + view.panX, y: p.y * view.scale + view.panY }; }
  function screenToWorld(view, p) { return { x: (p.x - view.panX) / view.scale, y: (p.y - view.panY) / view.scale }; }

  function niceGridStep(scale, target) {
    // Choose a grid step (cm) so that step*scale is close to target px.
    const steps = [1, 2, 5, 10, 20, 50, 100, 200, 500, 1000];
    let best = steps[0];
    for (const s of steps) {
      if (s * scale >= target) { best = s; break; }
      best = s;
    }
    return best;
  }

  function drawGrid(ctx, w, h, view, settings) {
    ctx.fillStyle = COLORS.bg;
    ctx.fillRect(0, 0, w, h);
    if (!settings.showGrid) return;
    const tl = screenToWorld(view, { x: 0, y: 0 });
    const br = screenToWorld(view, { x: w, y: h });
    const minor = Math.max(settings.gridSize, niceGridStep(view.scale, 8));
    const major = 100; // 1 meter

    ctx.lineWidth = 1;
    ctx.strokeStyle = COLORS.gridMinor;
    ctx.beginPath();
    let startX = Math.floor(tl.x / minor) * minor;
    for (let x = startX; x <= br.x; x += minor) {
      const sx = worldToScreen(view, { x, y: 0 }).x;
      ctx.moveTo(sx + 0.5, 0); ctx.lineTo(sx + 0.5, h);
    }
    let startY = Math.floor(tl.y / minor) * minor;
    for (let y = startY; y <= br.y; y += minor) {
      const sy = worldToScreen(view, { x: 0, y }).y;
      ctx.moveTo(0, sy + 0.5); ctx.lineTo(w, sy + 0.5);
    }
    ctx.stroke();

    ctx.strokeStyle = COLORS.gridMajor;
    ctx.beginPath();
    startX = Math.floor(tl.x / major) * major;
    for (let x = startX; x <= br.x; x += major) {
      const sx = worldToScreen(view, { x, y: 0 }).x;
      ctx.moveTo(sx + 0.5, 0); ctx.lineTo(sx + 0.5, h);
    }
    startY = Math.floor(tl.y / major) * major;
    for (let y = startY; y <= br.y; y += major) {
      const sy = worldToScreen(view, { x: 0, y }).y;
      ctx.moveTo(0, sy + 0.5); ctx.lineTo(w, sy + 0.5);
    }
    ctx.stroke();
  }

  function wallSolidSegments(edgeLength, openings) {
    const sorted = openings.slice().sort((a, b) => a.start - b.start);
    const solids = [];
    let cursor = 0;
    for (const op of sorted) {
      const s = Math.max(0, Math.min(edgeLength, op.start));
      const e = Math.max(0, Math.min(edgeLength, op.end));
      if (s > cursor) solids.push([cursor, s]);
      cursor = Math.max(cursor, e);
    }
    if (cursor < edgeLength) solids.push([cursor, edgeLength]);
    return solids;
  }

  function openingTransform(room, opening) {
    const a = room.points[opening.edgeIndex];
    const b = room.points[(opening.edgeIndex + 1) % room.points.length];
    const dx = b.x - a.x, dy = b.y - a.y;
    const len = Math.hypot(dx, dy);
    const dirX = dx / len, dirY = dy / len;
    const pos = { x: a.x + dirX * opening.offset, y: a.y + dirY * opening.offset };
    const angle = Math.atan2(dy, dx) * 180 / Math.PI;
    return { pos, angle, dirX, dirY, edgeLen: len, a, b };
  }

  function drawRoom(ctx, view, room, openings, isSelected) {
    const pts = room.points;
    if (pts.length < 2) return;
    const screenPts = pts.map(p => worldToScreen(view, p));

    if (pts.length >= 3) {
      ctx.beginPath();
      ctx.moveTo(screenPts[0].x, screenPts[0].y);
      for (let i = 1; i < screenPts.length; i++) ctx.lineTo(screenPts[i].x, screenPts[i].y);
      ctx.closePath();
      ctx.fillStyle = COLORS.floor;
      ctx.fill();
    }

    const thicknessPx = Math.max(room.thickness * view.scale, 2.5);
    ctx.lineCap = 'butt';
    ctx.lineJoin = 'miter';
    ctx.strokeStyle = isSelected ? COLORS.wallSelected : COLORS.wall;
    ctx.lineWidth = thicknessPx;

    const n = pts.length;
    for (let i = 0; i < (n >= 3 ? n : n - 1); i++) {
      const a = pts[i], b = pts[(i + 1) % n];
      const edgeOpenings = openings.filter(o => o.roomId === room.id && o.edgeIndex === i)
        .map(o => ({ start: o.offset - o.width / 2, end: o.offset + o.width / 2, op: o }));
      const edgeLen = Geo.dist(a, b);
      const solids = wallSolidSegments(edgeLen, edgeOpenings);
      const dirX = (b.x - a.x) / edgeLen, dirY = (b.y - a.y) / edgeLen;
      for (const [s, e] of solids) {
        if (e - s < 0.01) continue;
        const p1 = worldToScreen(view, { x: a.x + dirX * s, y: a.y + dirY * s });
        const p2 = worldToScreen(view, { x: a.x + dirX * e, y: a.y + dirY * e });
        ctx.beginPath(); ctx.moveTo(p1.x, p1.y); ctx.lineTo(p2.x, p2.y); ctx.stroke();
      }
      // openings on this edge: draw door swing / window glass
      for (const eo of edgeOpenings) {
        drawOpeningSymbol(ctx, view, room, eo.op, thicknessPx);
      }
    }
    // corner joins
    ctx.fillStyle = isSelected ? COLORS.wallSelected : COLORS.wall;
    const cornerCount = n >= 3 ? n : n;
    for (let i = 0; i < cornerCount; i++) {
      if (n < 3 && (i === 0 || i === n - 1) === false) continue;
      const sp = worldToScreen(view, pts[i]);
      ctx.beginPath(); ctx.arc(sp.x, sp.y, thicknessPx / 2, 0, Math.PI * 2); ctx.fill();
    }
  }

  function drawOpeningSymbol(ctx, view, room, opening, thicknessPx) {
    const t = openingTransform(room, opening);
    const half = opening.width / 2;
    const p1w = { x: t.pos.x - t.dirX * half, y: t.pos.y - t.dirY * half };
    const p2w = { x: t.pos.x + t.dirX * half, y: t.pos.y + t.dirY * half };
    const p1 = worldToScreen(view, p1w), p2 = worldToScreen(view, p2w);

    if (opening.type === 'window') {
      ctx.save();
      ctx.translate(p1.x, p1.y);
      ctx.rotate(t.angle * Math.PI / 180);
      const lenPx = Geo.dist(p1, p2);
      ctx.fillStyle = COLORS.window;
      ctx.strokeStyle = COLORS.windowBorder;
      ctx.lineWidth = 1.5;
      ctx.fillRect(0, -thicknessPx / 2, lenPx, thicknessPx);
      ctx.strokeRect(0, -thicknessPx / 2, lenPx, thicknessPx);
      ctx.beginPath(); ctx.moveTo(0, 0); ctx.lineTo(lenPx, 0); ctx.stroke();
      ctx.restore();
      return;
    }

    // door: leaf line + quarter-circle swing arc
    const sign = opening.flip ? -1 : 1;
    const hingeAt = opening.swing === 'right' ? p2w : p1w;
    const otherAt = opening.swing === 'right' ? p1w : p2w;
    const hinge = worldToScreen(view, hingeAt);
    const doorLen = Geo.dist(p1, p2);
    const along = { x: (otherAt.x - hingeAt.x) / opening.width, y: (otherAt.y - hingeAt.y) / opening.width };
    const normal = { x: -along.y * sign, y: along.x * sign };
    const leafEndW = { x: hingeAt.x + normal.x * opening.width, y: hingeAt.y + normal.y * opening.width };
    const leafEnd = worldToScreen(view, leafEndW);

    ctx.save();
    ctx.strokeStyle = '#55534c';
    ctx.lineWidth = Math.max(1.2, thicknessPx * 0.18);
    ctx.beginPath(); ctx.moveTo(hinge.x, hinge.y); ctx.lineTo(leafEnd.x, leafEnd.y); ctx.stroke();

    ctx.setLineDash([4, 3]);
    ctx.lineWidth = 1;
    const startAngle = Math.atan2(leafEnd.y - hinge.y, leafEnd.x - hinge.x);
    const endPt = worldToScreen(view, otherAt);
    const endAngle = Math.atan2(endPt.y - hinge.y, endPt.x - hinge.x);
    ctx.beginPath();
    ctx.arc(hinge.x, hinge.y, doorLen, startAngle, endAngle, false);
    ctx.stroke();
    ctx.setLineDash([]);
    ctx.restore();
  }

  function drawRoomHandles(ctx, view, room) {
    const pts = room.points;
    const n = pts.length;
    for (let i = 0; i < n; i++) {
      const a = pts[i], b = pts[(i + 1) % n];
      if (n >= 3 || i < n - 1) {
        const mid = Geo.lerp(a, b, 0.5);
        const sm = worldToScreen(view, mid);
        ctx.beginPath();
        ctx.fillStyle = '#ffffff';
        ctx.strokeStyle = COLORS.edgeHandle;
        ctx.lineWidth = 1.5;
        ctx.arc(sm.x, sm.y, 5, 0, Math.PI * 2);
        ctx.fill(); ctx.stroke();
      }
    }
    for (let i = 0; i < n; i++) {
      const sp = worldToScreen(view, pts[i]);
      ctx.beginPath();
      ctx.fillStyle = COLORS.vertexHandle;
      ctx.strokeStyle = '#fff';
      ctx.lineWidth = 1.5;
      ctx.rect(sp.x - 6, sp.y - 6, 12, 12);
      ctx.fill(); ctx.stroke();
    }
  }

  function drawWallLengthLabels(ctx, view, room, settings) {
    const pts = room.points;
    const n = pts.length;
    for (let i = 0; i < (n >= 3 ? n : n - 1); i++) {
      const a = pts[i], b = pts[(i + 1) % n];
      const len = Geo.dist(a, b);
      const mid = Geo.lerp(a, b, 0.5);
      const sm = worldToScreen(view, mid);
      const angle = Math.atan2(b.y - a.y, b.x - a.x);
      drawLabelOnLine(ctx, sm, angle, Geo.formatLength(len, settings.units));
    }
  }

  function drawLabelOnLine(ctx, screenPos, angleRad, text) {
    ctx.save();
    ctx.translate(screenPos.x, screenPos.y);
    let a = angleRad;
    if (a > Math.PI / 2 || a < -Math.PI / 2) a += Math.PI;
    ctx.rotate(a);
    ctx.font = '12px -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'bottom';
    const w = ctx.measureText(text).width;
    ctx.fillStyle = 'rgba(246,245,242,0.88)';
    ctx.fillRect(-w / 2 - 4, -16, w + 8, 16);
    ctx.fillStyle = COLORS.label;
    ctx.fillText(text, 0, -2);
    ctx.restore();
  }

  function furnitureRectPx(view, item) {
    return { cx: item.x * view.scale + view.panX, cy: item.y * view.scale + view.panY, w: item.w * view.scale, h: item.d * view.scale };
  }

  function drawFurnitureItem(ctx, view, item, isSelected) {
    const { cx, cy, w, h } = furnitureRectPx(view, item);
    ctx.save();
    ctx.translate(cx, cy);
    ctx.rotate(item.rotation * Math.PI / 180);

    const r = Math.min(w, h) * 0.12;
    ctx.beginPath();
    if (item.kind === 'round') {
      ctx.ellipse(0, 0, w / 2, h / 2, 0, 0, Math.PI * 2);
    } else if (item.kind === 'rug') {
      ctx.rect(-w / 2, -h / 2, w, h);
    } else if (ctx.roundRect) {
      ctx.roundRect(-w / 2, -h / 2, w, h, Math.min(r, 10));
    } else {
      ctx.rect(-w / 2, -h / 2, w, h);
    }
    if (item.kind === 'rug') {
      ctx.fillStyle = item.color + '55';
      ctx.setLineDash([5, 4]);
    } else {
      ctx.fillStyle = item.color;
      ctx.setLineDash([]);
    }
    ctx.fill();
    ctx.lineWidth = isSelected ? 2.5 : 1.2;
    ctx.strokeStyle = isSelected ? COLORS.accent : 'rgba(0,0,0,0.35)';
    ctx.stroke();
    ctx.setLineDash([]);

    Catalog.drawGlyph(ctx, item.kind, w, h, 'rgba(0,0,0,0.38)');

    if (Math.min(w, h) > 34) {
      ctx.fillStyle = 'rgba(0,0,0,0.62)';
      ctx.font = `${Math.min(12, Math.max(9, Math.min(w, h) * 0.16))}px -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif`;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(item.label, 0, 0);
    }
    ctx.restore();
  }

  function drawFurnitureSelection(ctx, view, item) {
    const { cx, cy, w, h } = furnitureRectPx(view, item);
    ctx.save();
    ctx.translate(cx, cy);
    ctx.rotate(item.rotation * Math.PI / 180);
    ctx.strokeStyle = COLORS.accent;
    ctx.lineWidth = 1.5;
    ctx.setLineDash([4, 3]);
    ctx.strokeRect(-w / 2 - 4, -h / 2 - 4, w + 8, h + 8);
    ctx.setLineDash([]);

    const handles = [
      [-w / 2 - 4, -h / 2 - 4], [0, -h / 2 - 4], [w / 2 + 4, -h / 2 - 4],
      [-w / 2 - 4, 0], [w / 2 + 4, 0],
      [-w / 2 - 4, h / 2 + 4], [0, h / 2 + 4], [w / 2 + 4, h / 2 + 4],
    ];
    ctx.fillStyle = '#fff';
    for (const [x, y] of handles) {
      ctx.beginPath();
      ctx.rect(x - 4, y - 4, 8, 8);
      ctx.fill(); ctx.stroke();
    }
    // rotate handle
    const rh = -h / 2 - 28;
    ctx.beginPath(); ctx.moveTo(0, -h / 2 - 4); ctx.lineTo(0, rh); ctx.stroke();
    ctx.beginPath(); ctx.arc(0, rh, 6, 0, Math.PI * 2); ctx.fillStyle = '#fff'; ctx.fill(); ctx.stroke();
    ctx.restore();

    ctx.save();
    ctx.font = '11px -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif';
    ctx.fillStyle = COLORS.dim;
    ctx.textAlign = 'center';
    const text = `${Math.round(item.w)} × ${Math.round(item.d)} cm`;
    ctx.fillText(text, cx, cy + h / 2 + 36 * Math.cos(item.rotation * Math.PI / 180) + 0);
    ctx.restore();
  }

  function drawDrawingPreview(ctx, view, settings) {
    if (T.tool !== 'room' || T.drawingPoints.length === 0) return;
    const pts = T.drawingPoints.map(p => worldToScreen(view, p));
    ctx.strokeStyle = COLORS.wall;
    ctx.lineWidth = 3;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(pts[0].x, pts[0].y);
    for (let i = 1; i < pts.length; i++) ctx.lineTo(pts[i].x, pts[i].y);
    ctx.stroke();
    for (const p of pts) {
      ctx.beginPath(); ctx.fillStyle = COLORS.accent; ctx.arc(p.x, p.y, 4, 0, Math.PI * 2); ctx.fill();
    }
    if (T.previewPoint) {
      const last = T.drawingPoints[T.drawingPoints.length - 1];
      const pv = worldToScreen(view, T.previewPoint);
      ctx.setLineDash([6, 5]);
      ctx.strokeStyle = COLORS.accent;
      ctx.lineWidth = 2;
      ctx.beginPath(); ctx.moveTo(pts[pts.length - 1].x, pts[pts.length - 1].y); ctx.lineTo(pv.x, pv.y); ctx.stroke();
      ctx.setLineDash([]);
      const len = Geo.dist(last, T.previewPoint);
      const angle = Math.atan2(T.previewPoint.y - last.y, T.previewPoint.x - last.x);
      const mid = { x: (pts[pts.length - 1].x + pv.x) / 2, y: (pts[pts.length - 1].y + pv.y) / 2 };
      const labelText = T.lengthInputValue != null
        ? (T.lengthInputValue + ' ' + settings.units + ' ✎')
        : Geo.formatLength(len, settings.units);
      drawLabelOnLine(ctx, mid, angle, labelText);
    }
    if (T.closeHint && pts.length > 2) {
      ctx.beginPath();
      ctx.strokeStyle = COLORS.guide;
      ctx.lineWidth = 2;
      ctx.arc(pts[0].x, pts[0].y, 9, 0, Math.PI * 2);
      ctx.stroke();
    }
  }

  function drawOpeningPlacementPreview(ctx, view, transient) {
    if (!transient.placingOpening) return;
    const p = transient.placingOpening;
    const room = p.room;
    const t = openingTransform(room, p);
    const sp = worldToScreen(view, t.pos);
    ctx.beginPath();
    ctx.fillStyle = COLORS.accentSoft;
    ctx.arc(sp.x, sp.y, 8, 0, Math.PI * 2);
    ctx.fill();
  }

  function drawGuides(ctx, w, h, view) {
    if (!T.guides || T.guides.length === 0) return;
    ctx.save();
    ctx.strokeStyle = COLORS.guide;
    ctx.lineWidth = 1;
    ctx.setLineDash([5, 4]);
    for (const g of T.guides) {
      ctx.beginPath();
      if (g.type === 'v') {
        const sx = worldToScreen(view, { x: g.pos, y: 0 }).x;
        ctx.moveTo(sx, 0); ctx.lineTo(sx, h);
      } else {
        const sy = worldToScreen(view, { x: 0, y: g.pos }).y;
        ctx.moveTo(0, sy); ctx.lineTo(w, sy);
      }
      ctx.stroke();
    }
    ctx.setLineDash([]);
    ctx.restore();
  }

  function drawRubberBand(ctx) {
    if (!T.rubberBand) return;
    const r = T.rubberBand;
    ctx.save();
    ctx.fillStyle = COLORS.accentSoft;
    ctx.strokeStyle = COLORS.accent;
    ctx.lineWidth = 1;
    const x = Math.min(r.x1, r.x2), y = Math.min(r.y1, r.y2);
    const ww = Math.abs(r.x2 - r.x1), hh = Math.abs(r.y2 - r.y1);
    ctx.fillRect(x, y, ww, hh);
    ctx.strokeRect(x, y, ww, hh);
    ctx.restore();
  }

  function drawScaleBar(ctx, w, h, view, settings) {
    const targetPx = 90;
    const step = niceGridStep(view.scale, targetPx);
    const lenPx = step * view.scale;
    const x0 = 16, y0 = h - 22;
    ctx.save();
    ctx.strokeStyle = COLORS.label;
    ctx.fillStyle = COLORS.label;
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(x0, y0); ctx.lineTo(x0 + lenPx, y0);
    ctx.moveTo(x0, y0 - 4); ctx.lineTo(x0, y0 + 4);
    ctx.moveTo(x0 + lenPx, y0 - 4); ctx.lineTo(x0 + lenPx, y0 + 4);
    ctx.stroke();
    ctx.font = '11px -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif';
    ctx.textAlign = 'left';
    ctx.fillText(Geo.formatLength(step, settings.units), x0, y0 - 8);
    ctx.restore();
  }

  function render(canvas, ctx, store) {
    const w = canvas.clientWidth, h = canvas.clientHeight;
    const state = store.state;
    const view = state.view, settings = state.settings;

    drawGrid(ctx, w, h, view, settings);

    for (const room of state.rooms) {
      const isSelected = state.selection.type === 'room' && state.selection.ids.includes(room.id);
      drawRoom(ctx, view, room, state.openings, isSelected);
    }
    for (const room of state.rooms) {
      const isSelected = state.selection.type === 'room' && state.selection.ids.includes(room.id);
      if (isSelected) {
        drawWallLengthLabels(ctx, view, room, settings);
        drawRoomHandles(ctx, view, room);
      }
    }

    const furniture = state.furniture.slice().sort((a, b) => (a.kind === 'rug' ? -1 : 0) - (b.kind === 'rug' ? -1 : 0));
    for (const item of furniture) {
      const isSelected = state.selection.type === 'furniture' && state.selection.ids.includes(item.id);
      drawFurnitureItem(ctx, view, item, isSelected);
    }

    drawOpeningPlacementPreview(ctx, view, T);
    drawDrawingPreview(ctx, view, settings);
    drawGuides(ctx, w, h, view);

    if (state.selection.type === 'furniture' && state.selection.ids.length === 1) {
      const item = store.getFurniture(state.selection.ids[0]);
      if (item) drawFurnitureSelection(ctx, view, item);
    } else if (state.selection.type === 'furniture' && state.selection.ids.length > 1) {
      for (const id of state.selection.ids) {
        const item = store.getFurniture(id);
        if (!item) continue;
        const { cx, cy, w: ww, h: hh } = furnitureRectPx(view, item);
        ctx.save();
        ctx.translate(cx, cy); ctx.rotate(item.rotation * Math.PI / 180);
        ctx.strokeStyle = COLORS.accent; ctx.lineWidth = 1.5; ctx.setLineDash([4, 3]);
        ctx.strokeRect(-ww / 2 - 4, -hh / 2 - 4, ww + 8, hh + 8);
        ctx.setLineDash([]);
        ctx.restore();
      }
    } else if (state.selection.type === 'opening') {
      const op = store.getOpening(state.selection.ids[0]);
      if (op) {
        const room = store.getRoom(op.roomId);
        if (room) {
          const t = openingTransform(room, op);
          const sp = worldToScreen(view, t.pos);
          ctx.save();
          ctx.strokeStyle = COLORS.accent; ctx.lineWidth = 2; ctx.setLineDash([3, 3]);
          ctx.beginPath(); ctx.arc(sp.x, sp.y, Math.max(op.width * view.scale / 2 + 6, 12), 0, Math.PI * 2); ctx.stroke();
          ctx.setLineDash([]);
          ctx.restore();
        }
      }
    }

    drawRubberBand(ctx);
    drawScaleBar(ctx, w, h, view, settings);
  }

  return { render, worldToScreen, screenToWorld, openingTransform, T, COLORS, niceGridStep };
})();
