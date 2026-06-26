// 3D walkthrough view: Three.js scene built from the floor plan, with a
// free-fly pointer-lock camera and basic ambient+directional shading.
'use strict';

const Render3D = (() => {
  const WALL_HEIGHT = 250; // cm
  const EYE_HEIGHT = 165; // cm
  const MOVE_SPEED = 260; // cm/sec

  const ITEM_HEIGHTS = {
    sofa: 80, 'sofa-corner': 80, armchair: 80, chair: 85, bed: 55, table: 75,
    shelf: 190, cabinet: 110, round: 75, toilet: 75, sink: 85, bathtub: 55,
    shower: 200, plant: 130, tv: 60, rug: 2,
  };

  let scene, camera, renderer, canvas, overlay;
  let raf = null;
  let active = false;
  let keys = {};
  let yaw = 0, pitch = -0.12;
  let camPos = { x: 0, y: EYE_HEIGHT / 100, z: 4 };
  let lastT = 0;
  let centerX = 0, centerY = 0;

  function qs(id) { return document.getElementById(id); }

  function ensureThree(cb) {
    if (window.THREE) { cb(); return; }
    qs('view3dLoading').classList.remove('hidden');
    window.addEventListener('three-ready', function handler() {
      window.removeEventListener('three-ready', handler);
      qs('view3dLoading').classList.add('hidden');
      cb();
    });
  }

  function worldToScene(x, y) {
    return { x: (x - centerX) / 100, z: (y - centerY) / 100 };
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

  function buildRoom(room, openings) {
    const pts = room.points;
    const n = pts.length;
    if (n < 2) return;
    const wallMat = new THREE.MeshStandardMaterial({ color: new THREE.Color(room.wallColor || '#2c2b29'), roughness: 0.85, metalness: 0.05 });
    const winMat = new THREE.MeshStandardMaterial({ color: 0xbfdcec, roughness: 0.1, metalness: 0.1, transparent: true, opacity: 0.45 });
    const thicknessM = Math.max(room.thickness, 4) / 100;
    const heightM = WALL_HEIGHT / 100;

    for (let i = 0; i < (n >= 3 ? n : n - 1); i++) {
      const a = pts[i], b = pts[(i + 1) % n];
      const edgeLen = Math.hypot(b.x - a.x, b.y - a.y);
      if (edgeLen < 0.01) continue;
      const edgeOpenings = openings.filter(o => o.edgeIndex === i)
        .map(o => ({ start: o.offset - o.width / 2, end: o.offset + o.width / 2, op: o }));
      const solids = wallSolidSegments(edgeLen, edgeOpenings);
      const dirX = (b.x - a.x) / edgeLen, dirY = (b.y - a.y) / edgeLen;
      const angle = Math.atan2(dirY, dirX);
      for (const [s, e] of solids) {
        const segLen = e - s;
        if (segLen < 0.5) continue;
        const midX = a.x + dirX * (s + segLen / 2), midY = a.y + dirY * (s + segLen / 2);
        const mesh = new THREE.Mesh(new THREE.BoxGeometry(segLen / 100, heightM, thicknessM), wallMat);
        const c = worldToScene(midX, midY);
        mesh.position.set(c.x, heightM / 2, c.z);
        mesh.rotation.y = -angle;
        scene.add(mesh);
      }
      for (const eo of edgeOpenings) {
        if (eo.op.type !== 'window') continue;
        const segLen = eo.end - eo.start;
        if (segLen < 0.5) continue;
        const midX = a.x + dirX * (eo.start + segLen / 2), midY = a.y + dirY * (eo.start + segLen / 2);
        const mesh = new THREE.Mesh(new THREE.BoxGeometry(segLen / 100, heightM * 0.55, thicknessM * 0.5), winMat);
        const c = worldToScene(midX, midY);
        mesh.position.set(c.x, heightM / 2, c.z);
        mesh.rotation.y = -angle;
        scene.add(mesh);
      }
    }

    if (n >= 3) {
      try {
        const shapePts = pts.map(p => { const c = worldToScene(p.x, p.y); return new THREE.Vector2(c.x, c.z); });
        const shape = new THREE.Shape(shapePts);
        const floorMesh = new THREE.Mesh(
          new THREE.ShapeGeometry(shape),
          new THREE.MeshStandardMaterial({ color: 0xf3efe6, roughness: 0.95, side: THREE.DoubleSide })
        );
        floorMesh.rotation.x = Math.PI / 2;
        floorMesh.position.y = 0.001;
        scene.add(floorMesh);
      } catch (e) { /* degenerate polygon, skip floor */ }
    }
  }

  function buildFurniture(item) {
    const heightCm = ITEM_HEIGHTS[item.kind] || 75;
    const w = item.w / 100, d = item.d / 100, h = heightCm / 100;
    const mat = new THREE.MeshStandardMaterial({ color: new THREE.Color(item.color || '#9bb3c9'), roughness: 0.7, metalness: 0.05 });
    const geo = item.kind === 'round' ? new THREE.CylinderGeometry(w / 2, w / 2, h, 28) : new THREE.BoxGeometry(w, h, d);
    const mesh = new THREE.Mesh(geo, mat);
    const c = worldToScene(item.x, item.y);
    mesh.position.set(c.x, item.kind === 'rug' ? 0.011 : h / 2, c.z);
    mesh.rotation.y = -item.rotation * Math.PI / 180;
    scene.add(mesh);
  }

  function buildScene() {
    scene = new THREE.Scene();
    scene.background = new THREE.Color(0xcfe3f0);
    scene.fog = new THREE.Fog(0xcfe3f0, 22, 55);

    scene.add(new THREE.AmbientLight(0xffffff, 0.6));
    const sun = new THREE.DirectionalLight(0xffffff, 0.9);
    sun.position.set(6, 12, 4);
    scene.add(sun);
    const fill = new THREE.DirectionalLight(0xffffff, 0.25);
    fill.position.set(-6, 6, -4);
    scene.add(fill);

    const state = Store.state;
    let allPts = [];
    for (const r of state.rooms) allPts.push(...r.points);
    let radiusM = 3;
    if (allPts.length) {
      const xs = allPts.map(p => p.x), ys = allPts.map(p => p.y);
      centerX = (Math.min(...xs) + Math.max(...xs)) / 2;
      centerY = (Math.min(...ys) + Math.max(...ys)) / 2;
      const halfW = (Math.max(...xs) - Math.min(...xs)) / 100 / 2;
      const halfH = (Math.max(...ys) - Math.min(...ys)) / 100 / 2;
      radiusM = Math.max(Math.hypot(halfW, halfH), 1.5);
    } else { centerX = 0; centerY = 0; }

    for (const room of state.rooms) buildRoom(room, state.openings.filter(o => o.roomId === room.id));
    for (const item of state.furniture) buildFurniture(item);

    const ground = new THREE.Mesh(
      new THREE.PlaneGeometry(400, 400),
      new THREE.MeshStandardMaterial({ color: 0xdedacb, roughness: 1 })
    );
    ground.rotation.x = -Math.PI / 2;
    ground.position.y = -0.02;
    scene.add(ground);

    camPos = { x: 0, y: Math.max(EYE_HEIGHT / 100, WALL_HEIGHT / 100 * 0.95), z: radiusM * 1.7 + 2.5 };
    yaw = 0; pitch = -0.32;
  }

  function onMouseMove(e) {
    if (document.pointerLockElement !== canvas) return;
    yaw -= e.movementX * 0.0022;
    pitch -= e.movementY * 0.0022;
    const lim = Math.PI / 2 - 0.05;
    pitch = Math.max(-lim, Math.min(lim, pitch));
  }
  function onKeyDown(e) {
    keys[e.code] = true;
    e.stopPropagation();
    if (e.code === 'Escape') close();
  }
  function onKeyUp(e) { keys[e.code] = false; e.stopPropagation(); }

  function tick(t) {
    if (!active) return;
    const dt = lastT ? Math.min(0.05, (t - lastT) / 1000) : 0;
    lastT = t;
    const forward = { x: -Math.sin(yaw), z: -Math.cos(yaw) };
    const right = { x: Math.cos(yaw), z: -Math.sin(yaw) };
    const speed = MOVE_SPEED / 100;
    let mx = 0, mz = 0, my = 0;
    if (keys['KeyW'] || keys['ArrowUp']) { mx += forward.x; mz += forward.z; }
    if (keys['KeyS'] || keys['ArrowDown']) { mx -= forward.x; mz -= forward.z; }
    if (keys['KeyD'] || keys['ArrowRight']) { mx += right.x; mz += right.z; }
    if (keys['KeyA'] || keys['ArrowLeft']) { mx -= right.x; mz -= right.z; }
    if (keys['Space']) my += 1;
    if (keys['ShiftLeft'] || keys['ShiftRight']) my -= 1;
    const len = Math.hypot(mx, mz);
    if (len > 0.001) { mx /= len; mz /= len; }
    camPos.x += mx * speed * dt;
    camPos.z += mz * speed * dt;
    camPos.y += my * speed * dt;
    camPos.y = Math.max(0.3, Math.min(WALL_HEIGHT / 100 - 0.1, camPos.y));

    camera.position.set(camPos.x, camPos.y, camPos.z);
    const lookDir = {
      x: Math.cos(pitch) * -Math.sin(yaw),
      y: Math.sin(pitch),
      z: Math.cos(pitch) * -Math.cos(yaw),
    };
    camera.lookAt(camPos.x + lookDir.x, camPos.y + lookDir.y, camPos.z + lookDir.z);

    renderer.render(scene, camera);
    raf = requestAnimationFrame(tick);
  }

  function resize() {
    if (!renderer || !overlay) return;
    const w = overlay.clientWidth, h = overlay.clientHeight;
    renderer.setSize(w, h, false);
    camera.aspect = w / h;
    camera.updateProjectionMatrix();
  }

  function requestLock() {
    if (canvas && canvas.requestPointerLock) canvas.requestPointerLock();
  }
  function onLockChange() {
    const hint = qs('view3dHint');
    if (hint) hint.style.display = document.pointerLockElement === canvas ? 'none' : '';
  }

  function open() {
    overlay = qs('view3dOverlay');
    canvas = qs('canvas3d');
    overlay.classList.remove('hidden');
    ensureThree(() => {
      if (!renderer) {
        renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
        renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
        camera = new THREE.PerspectiveCamera(70, 1, 0.05, 100);
      }
      buildScene();
      resize();
      active = true;
      lastT = 0;
      keys = {};
      document.addEventListener('keydown', onKeyDown, true);
      document.addEventListener('keyup', onKeyUp, true);
      document.addEventListener('mousemove', onMouseMove);
      window.addEventListener('resize', resize);
      canvas.addEventListener('click', requestLock);
      document.addEventListener('pointerlockchange', onLockChange);
      const hint = qs('view3dHint');
      if (hint) hint.style.display = '';
      raf = requestAnimationFrame(tick);
    });
  }

  function close() {
    active = false;
    if (raf) cancelAnimationFrame(raf);
    raf = null;
    if (document.pointerLockElement === canvas) document.exitPointerLock();
    document.removeEventListener('keydown', onKeyDown, true);
    document.removeEventListener('keyup', onKeyUp, true);
    document.removeEventListener('mousemove', onMouseMove);
    window.removeEventListener('resize', resize);
    document.removeEventListener('pointerlockchange', onLockChange);
    if (canvas) canvas.removeEventListener('click', requestLock);
    if (overlay) overlay.classList.add('hidden');
  }

  return { open, close };
})();
