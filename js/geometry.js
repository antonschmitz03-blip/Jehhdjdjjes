// Geometry, math and unit-conversion helpers. No DOM access here.
'use strict';

const Geo = (() => {
  const DEG = Math.PI / 180;

  function dist(a, b) { return Math.hypot(b.x - a.x, b.y - a.y); }
  function add(a, b) { return { x: a.x + b.x, y: a.y + b.y }; }
  function sub(a, b) { return { x: a.x - b.x, y: a.y - b.y }; }
  function scaleV(a, s) { return { x: a.x * s, y: a.y * s }; }
  function dot(a, b) { return a.x * b.x + a.y * b.y; }
  function lerp(a, b, t) { return { x: a.x + (b.x - a.x) * t, y: a.y + (b.y - a.y) * t }; }

  function rotatePoint(p, center, angleDeg) {
    const a = angleDeg * DEG;
    const cos = Math.cos(a), sin = Math.sin(a);
    const dx = p.x - center.x, dy = p.y - center.y;
    return { x: center.x + dx * cos - dy * sin, y: center.y + dx * sin + dy * cos };
  }

  function normalizeDeg(deg) {
    let d = deg % 360;
    if (d < 0) d += 360;
    return d;
  }

  function snapToStep(value, step) {
    if (!step) return value;
    return Math.round(value / step) * step;
  }

  function snapAngleDeg(angleDeg, stepDeg) {
    return normalizeDeg(Math.round(angleDeg / stepDeg) * stepDeg);
  }

  // Closest point on segment a-b to point p. Returns {point, t, distance}.
  function closestPointOnSegment(p, a, b) {
    const abx = b.x - a.x, aby = b.y - a.y;
    const lenSq = abx * abx + aby * aby;
    let t = lenSq > 0 ? ((p.x - a.x) * abx + (p.y - a.y) * aby) / lenSq : 0;
    t = Math.max(0, Math.min(1, t));
    const pt = { x: a.x + abx * t, y: a.y + aby * t };
    return { point: pt, t, distance: dist(p, pt) };
  }

  function polygonArea(points) {
    let area = 0;
    for (let i = 0; i < points.length; i++) {
      const a = points[i], b = points[(i + 1) % points.length];
      area += a.x * b.y - b.x * a.y;
    }
    return Math.abs(area) / 2;
  }

  function polygonPerimeter(points) {
    let p = 0;
    for (let i = 0; i < points.length; i++) {
      p += dist(points[i], points[(i + 1) % points.length]);
    }
    return p;
  }

  function pointInPolygon(p, points) {
    let inside = false;
    for (let i = 0, j = points.length - 1; i < points.length; j = i++) {
      const xi = points[i].x, yi = points[i].y;
      const xj = points[j].x, yj = points[j].y;
      if (((yi > p.y) !== (yj > p.y)) &&
        (p.x < (xj - xi) * (p.y - yi) / (yj - yi) + xi)) {
        inside = !inside;
      }
    }
    return inside;
  }

  // Returns rotated rect corners for a furniture footprint, world space.
  function rectCorners(cx, cy, w, h, rotationDeg) {
    const hw = w / 2, hh = h / 2;
    const local = [
      { x: -hw, y: -hh }, { x: hw, y: -hh }, { x: hw, y: hh }, { x: -hw, y: hh }
    ];
    const center = { x: cx, y: cy };
    return local.map(p => rotatePoint(add(p, center), center, rotationDeg));
  }

  function pointInRotatedRect(p, cx, cy, w, h, rotationDeg) {
    // Inverse-rotate point into the rect's local frame.
    const local = rotatePoint(p, { x: cx, y: cy }, -rotationDeg);
    const dx = local.x - cx, dy = local.y - cy;
    return Math.abs(dx) <= w / 2 && Math.abs(dy) <= h / 2;
  }

  function worldToLocal(p, cx, cy, rotationDeg) {
    const local = rotatePoint(p, { x: cx, y: cy }, -rotationDeg);
    return { x: local.x - cx, y: local.y - cy };
  }

  // Closest edge of a polygon to a point. Returns edge index, projected
  // point, distance and the edge's angle in degrees.
  function closestPolygonEdge(p, points) {
    let best = null;
    for (let i = 0; i < points.length; i++) {
      const a = points[i], b = points[(i + 1) % points.length];
      const res = closestPointOnSegment(p, a, b);
      if (!best || res.distance < best.distance) {
        const angle = Math.atan2(b.y - a.y, b.x - a.x) / DEG;
        best = { index: i, point: res.point, t: res.t, distance: res.distance, angle, a, b };
      }
    }
    return best;
  }

  function rectsOverlap(r1, r2) {
    return !(r2.x1 > r1.x2 || r2.x2 < r1.x1 || r2.y1 > r1.y2 || r2.y2 < r1.y1);
  }

  function aabbOfRotatedRect(cx, cy, w, h, rotationDeg) {
    const corners = rectCorners(cx, cy, w, h, rotationDeg);
    const xs = corners.map(c => c.x), ys = corners.map(c => c.y);
    return { x1: Math.min(...xs), y1: Math.min(...ys), x2: Math.max(...xs), y2: Math.max(...ys) };
  }

  // ---- Units ----
  const CM_PER_INCH = 2.54;
  const CM_PER_FT = 30.48;

  function formatLength(cm, unit) {
    if (unit === 'm') {
      return (cm / 100).toFixed(2).replace(/\.00$/, '') + ' m';
    }
    if (unit === 'ft') {
      const totalInches = cm / CM_PER_INCH;
      let feet = Math.floor(totalInches / 12 + 1e-6);
      let inches = totalInches - feet * 12;
      // round to nearest 1/4 inch
      let quarters = Math.round(inches * 4);
      if (quarters === 48) { quarters = 0; feet += 1; }
      inches = quarters / 4;
      const whole = Math.floor(inches);
      const frac = quarters % 4;
      const fracStr = frac === 0 ? '' : (frac === 2 ? ' 1/2' : (frac === 1 ? ' 1/4' : ' 3/4'));
      return `${feet}'${whole}${fracStr}"`;
    }
    // cm
    const v = Math.round(cm * 10) / 10;
    return (Number.isInteger(v) ? v.toString() : v.toFixed(1)) + ' cm';
  }

  function formatArea(cm2, unit) {
    if (unit === 'ft') {
      return (cm2 / 929.0304).toFixed(1) + ' ft²';
    }
    return (cm2 / 10000).toFixed(2) + ' m²';
  }

  function parseLength(input, defaultUnit) {
    if (input == null) return null;
    let s = String(input).trim().toLowerCase();
    if (!s) return null;
    s = s.replace(/,/g, '.');
    let m;
    // feet + inches: 8'6" / 8' 6" / 8ft 6in / 8ft6
    m = s.match(/^(-?\d+(?:\.\d+)?)\s*(?:'|ft)\s*(\d+(?:\.\d+)?)?\s*(?:"|in)?$/);
    if (m) {
      const ft = parseFloat(m[1]);
      const inch = m[2] ? parseFloat(m[2]) : 0;
      return (ft * 12 + inch) * CM_PER_INCH;
    }
    m = s.match(/^(-?\d+(?:\.\d+)?)\s*(?:"|in)$/);
    if (m) return parseFloat(m[1]) * CM_PER_INCH;
    m = s.match(/^(-?\d+(?:\.\d+)?)\s*cm$/);
    if (m) return parseFloat(m[1]);
    m = s.match(/^(-?\d+(?:\.\d+)?)\s*m$/);
    if (m) return parseFloat(m[1]) * 100;
    m = s.match(/^(-?\d+(?:\.\d+)?)$/);
    if (m) {
      const v = parseFloat(m[1]);
      if (defaultUnit === 'm') return v * 100;
      if (defaultUnit === 'ft') return v * CM_PER_FT;
      return v;
    }
    return null;
  }

  return {
    DEG, dist, add, sub, scaleV, dot, lerp, rotatePoint, normalizeDeg, snapToStep,
    snapAngleDeg, closestPointOnSegment, polygonArea, polygonPerimeter, pointInPolygon,
    rectCorners, pointInRotatedRect, worldToLocal, closestPolygonEdge, rectsOverlap,
    aabbOfRotatedRect, formatLength, formatArea, parseLength, CM_PER_INCH, CM_PER_FT
  };
})();
