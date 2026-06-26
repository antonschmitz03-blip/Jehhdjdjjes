// Central application state: data model, history (undo/redo), persistence.
'use strict';

const Store = (() => {
  let uidCounter = 1;
  function uid(prefix) { return prefix + '_' + (uidCounter++) + '_' + Math.floor(Math.random() * 10000); }

  function defaultState() {
    return {
      rooms: [],
      openings: [],
      furniture: [],
      view: { scale: 2.2, panX: 80, panY: 80 }, // scale = px per cm
      settings: { units: 'cm', gridSnap: true, angleSnap: true, gridSize: 5, angleStep: 15, showGrid: true },
      selection: { type: null, ids: [] },
    };
  }

  let state = defaultState();
  let history = [];
  let historyIndex = -1;
  const HISTORY_LIMIT = 100;
  let listeners = [];
  let autosaveTimer = null;

  function subscribe(fn) { listeners.push(fn); }
  function emit() { listeners.forEach(fn => fn(state)); }

  function snapshot() {
    return JSON.stringify({ rooms: state.rooms, openings: state.openings, furniture: state.furniture });
  }

  function pushHistory() {
    const snap = snapshot();
    if (historyIndex >= 0 && history[historyIndex] === snap) return;
    history = history.slice(0, historyIndex + 1);
    history.push(snap);
    if (history.length > HISTORY_LIMIT) history.shift();
    historyIndex = history.length - 1;
    scheduleAutosave();
    emit();
  }

  function undo() {
    if (historyIndex <= 0) return;
    historyIndex--;
    applySnapshot(history[historyIndex]);
  }

  function redo() {
    if (historyIndex >= history.length - 1) return;
    historyIndex++;
    applySnapshot(history[historyIndex]);
  }

  function applySnapshot(snap) {
    const data = JSON.parse(snap);
    state.rooms = data.rooms;
    state.openings = data.openings;
    state.furniture = data.furniture;
    state.selection = { type: null, ids: [] };
    emit();
    scheduleAutosave();
  }

  function canUndo() { return historyIndex > 0; }
  function canRedo() { return historyIndex < history.length - 1; }

  function scheduleAutosave() {
    if (autosaveTimer) clearTimeout(autosaveTimer);
    autosaveTimer = setTimeout(() => {
      try {
        localStorage.setItem('roomplanner.autosave', JSON.stringify({
          rooms: state.rooms, openings: state.openings, furniture: state.furniture, settings: state.settings
        }));
      } catch (e) { /* storage may be unavailable */ }
    }, 500);
  }

  function loadAutosave() {
    try {
      const raw = localStorage.getItem('roomplanner.autosave');
      if (!raw) return false;
      const data = JSON.parse(raw);
      state.rooms = data.rooms || [];
      state.openings = data.openings || [];
      state.furniture = data.furniture || [];
      if (data.settings) Object.assign(state.settings, data.settings);
      pushHistory();
      return true;
    } catch (e) { return false; }
  }

  function init() {
    if (!loadAutosave()) {
      pushHistory();
    }
    emit();
  }

  // ---- Rooms ----
  function addRoom(points, thickness) {
    const room = { id: uid('room'), name: 'Room ' + (state.rooms.length + 1), thickness: thickness || 12, points };
    state.rooms.push(room);
    return room;
  }

  function getRoom(id) { return state.rooms.find(r => r.id === id); }

  function removeRoom(id) {
    state.rooms = state.rooms.filter(r => r.id !== id);
    state.openings = state.openings.filter(o => o.roomId !== id);
  }

  // ---- Openings (doors / windows) ----
  function addOpening(opening) {
    opening.id = uid('open');
    state.openings.push(opening);
    return opening;
  }
  function removeOpening(id) { state.openings = state.openings.filter(o => o.id !== id); }
  function getOpening(id) { return state.openings.find(o => o.id === id); }

  // ---- Furniture ----
  function addFurniture(item) {
    item.id = uid('furn');
    state.furniture.push(item);
    return item;
  }
  function removeFurniture(id) { state.furniture = state.furniture.filter(f => f.id !== id); }
  function getFurniture(id) { return state.furniture.find(f => f.id === id); }

  // ---- Selection ----
  function setSelection(type, ids) {
    state.selection = { type, ids: ids || [] };
    emit();
  }
  function clearSelection() { setSelection(null, []); }

  // ---- Persistence: named layouts ----
  function listLayouts() {
    try { return JSON.parse(localStorage.getItem('roomplanner.layouts') || '{}'); }
    catch (e) { return {}; }
  }
  function saveLayout(name) {
    const layouts = listLayouts();
    layouts[name] = { rooms: state.rooms, openings: state.openings, furniture: state.furniture, settings: state.settings, savedAt: Date.now() };
    localStorage.setItem('roomplanner.layouts', JSON.stringify(layouts));
  }
  function loadLayout(name) {
    const layouts = listLayouts();
    const data = layouts[name];
    if (!data) return false;
    state.rooms = data.rooms || [];
    state.openings = data.openings || [];
    state.furniture = data.furniture || [];
    if (data.settings) Object.assign(state.settings, data.settings);
    clearSelection();
    pushHistory();
    emit();
    return true;
  }
  function deleteLayout(name) {
    const layouts = listLayouts();
    delete layouts[name];
    localStorage.setItem('roomplanner.layouts', JSON.stringify(layouts));
  }

  function exportJson() {
    return JSON.stringify({ rooms: state.rooms, openings: state.openings, furniture: state.furniture, settings: state.settings }, null, 2);
  }
  function importJson(jsonStr) {
    const data = JSON.parse(jsonStr);
    state.rooms = data.rooms || [];
    state.openings = data.openings || [];
    state.furniture = data.furniture || [];
    if (data.settings) Object.assign(state.settings, data.settings);
    clearSelection();
    pushHistory();
    emit();
  }
  function clearAll() {
    state.rooms = [];
    state.openings = [];
    state.furniture = [];
    clearSelection();
    pushHistory();
    emit();
  }

  return {
    get state() { return state; },
    uid, subscribe, emit, pushHistory, undo, redo, canUndo, canRedo,
    addRoom, getRoom, removeRoom, addOpening, removeOpening, getOpening,
    addFurniture, removeFurniture, getFurniture,
    setSelection, clearSelection,
    listLayouts, saveLayout, loadLayout, deleteLayout,
    exportJson, importJson, clearAll, init, scheduleAutosave
  };
})();
