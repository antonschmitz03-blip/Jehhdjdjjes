// Furniture catalog data + minimalist top-down glyph rendering.
'use strict';

const Catalog = (() => {
  // w = width (cm), d = depth (cm), kind controls the glyph drawn on top
  // of the base shape. color is a muted, minimalist swatch.
  const ITEMS = [
    // Living room
    { id: 'sofa2', cat: 'Living Room', label: 'Sofa (2-seat)', w: 150, d: 85, kind: 'sofa', color: '#9bb3c9' },
    { id: 'sofa3', cat: 'Living Room', label: 'Sofa (3-seat)', w: 200, d: 90, kind: 'sofa', color: '#9bb3c9' },
    { id: 'sofaCorner', cat: 'Living Room', label: 'Corner Sofa', w: 220, d: 220, kind: 'sofa-corner', color: '#9bb3c9' },
    { id: 'armchair', cat: 'Living Room', label: 'Armchair', w: 80, d: 85, kind: 'armchair', color: '#9bb3c9' },
    { id: 'coffeeTable', cat: 'Living Room', label: 'Coffee Table', w: 110, d: 60, kind: 'table', color: '#c2a987' },
    { id: 'coffeeTableRound', cat: 'Living Room', label: 'Coffee Table (round)', w: 90, d: 90, kind: 'round', color: '#c2a987' },
    { id: 'tvStand', cat: 'Living Room', label: 'TV Stand', w: 140, d: 40, kind: 'shelf', color: '#a8a39b' },
    { id: 'tv', cat: 'Living Room', label: 'TV', w: 100, d: 8, kind: 'tv', color: '#3a3a3a' },
    { id: 'bookshelf', cat: 'Living Room', label: 'Bookshelf', w: 90, d: 30, kind: 'shelf', color: '#a8a39b' },
    { id: 'sideTable', cat: 'Living Room', label: 'Side Table', w: 50, d: 50, kind: 'table', color: '#c2a987' },
    { id: 'rug', cat: 'Living Room', label: 'Rug', w: 200, d: 140, kind: 'rug', color: '#cdbfa5' },

    // Bedroom
    { id: 'bedSingle', cat: 'Bedroom', label: 'Bed (Single)', w: 90, d: 190, kind: 'bed', color: '#b6a4c9' },
    { id: 'bedDouble', cat: 'Bedroom', label: 'Bed (Double)', w: 135, d: 190, kind: 'bed', color: '#b6a4c9' },
    { id: 'bedQueen', cat: 'Bedroom', label: 'Bed (Queen)', w: 153, d: 203, kind: 'bed', color: '#b6a4c9' },
    { id: 'bedKing', cat: 'Bedroom', label: 'Bed (King)', w: 193, d: 203, kind: 'bed', color: '#b6a4c9' },
    { id: 'nightstand', cat: 'Bedroom', label: 'Nightstand', w: 45, d: 40, kind: 'table', color: '#c2a987' },
    { id: 'wardrobe', cat: 'Bedroom', label: 'Wardrobe', w: 120, d: 60, kind: 'cabinet', color: '#a8a39b' },
    { id: 'dresser', cat: 'Bedroom', label: 'Dresser', w: 100, d: 50, kind: 'cabinet', color: '#a8a39b' },

    // Dining / Kitchen
    { id: 'diningTable4', cat: 'Dining & Kitchen', label: 'Dining Table (4)', w: 120, d: 80, kind: 'table', color: '#c2a987' },
    { id: 'diningTable6', cat: 'Dining & Kitchen', label: 'Dining Table (6)', w: 160, d: 90, kind: 'table', color: '#c2a987' },
    { id: 'diningTableRound', cat: 'Dining & Kitchen', label: 'Dining Table (round)', w: 110, d: 110, kind: 'round', color: '#c2a987' },
    { id: 'diningChair', cat: 'Dining & Kitchen', label: 'Dining Chair', w: 45, d: 45, kind: 'chair', color: '#8d9b87' },
    { id: 'kitchenCounter', cat: 'Dining & Kitchen', label: 'Kitchen Counter', w: 100, d: 60, kind: 'cabinet', color: '#a8a39b' },
    { id: 'kitchenIsland', cat: 'Dining & Kitchen', label: 'Kitchen Island', w: 120, d: 70, kind: 'table', color: '#c2a987' },
    { id: 'fridge', cat: 'Dining & Kitchen', label: 'Refrigerator', w: 75, d: 70, kind: 'cabinet', color: '#a8a39b' },
    { id: 'stove', cat: 'Dining & Kitchen', label: 'Stove', w: 60, d: 60, kind: 'shower', color: '#a8a39b' },

    // Bathroom
    { id: 'toilet', cat: 'Bathroom', label: 'Toilet', w: 38, d: 60, kind: 'toilet', color: '#aac4cb' },
    { id: 'sink', cat: 'Bathroom', label: 'Sink', w: 60, d: 45, kind: 'sink', color: '#aac4cb' },
    { id: 'bathtub', cat: 'Bathroom', label: 'Bathtub', w: 170, d: 75, kind: 'bathtub', color: '#aac4cb' },
    { id: 'showerStall', cat: 'Bathroom', label: 'Shower', w: 90, d: 90, kind: 'shower', color: '#aac4cb' },

    // Office
    { id: 'desk', cat: 'Office', label: 'Desk', w: 120, d: 60, kind: 'table', color: '#c2a987' },
    { id: 'officeChair', cat: 'Office', label: 'Office Chair', w: 60, d: 60, kind: 'chair', color: '#8d9b87' },
    { id: 'filingCabinet', cat: 'Office', label: 'Filing Cabinet', w: 40, d: 50, kind: 'cabinet', color: '#a8a39b' },

    // Other
    { id: 'plantSmall', cat: 'Other', label: 'Plant (small)', w: 40, d: 40, kind: 'plant', color: '#7f9b73' },
    { id: 'plantLarge', cat: 'Other', label: 'Plant (large)', w: 60, d: 60, kind: 'plant', color: '#7f9b73' },
  ];

  const CATEGORY_ORDER = ['Living Room', 'Bedroom', 'Dining & Kitchen', 'Bathroom', 'Office', 'Other', 'Custom'];

  function getCustomItems() {
    try {
      return JSON.parse(localStorage.getItem('roomplanner.customItems') || '[]');
    } catch (e) { return []; }
  }

  function saveCustomItems(items) {
    localStorage.setItem('roomplanner.customItems', JSON.stringify(items));
  }

  function addCustomItem(item) {
    const items = getCustomItems();
    item.id = 'custom_' + Date.now() + '_' + Math.floor(Math.random() * 1000);
    item.cat = 'Custom';
    items.push(item);
    saveCustomItems(items);
    return item;
  }

  function deleteCustomItem(id) {
    const items = getCustomItems().filter(i => i.id !== id);
    saveCustomItems(items);
  }

  function allItems() {
    return ITEMS.concat(getCustomItems());
  }

  function byId(id) {
    return allItems().find(i => i.id === id);
  }

  // Draws the distinguishing minimalist glyph for a furniture kind.
  // Context is expected to already be translated to the item's center
  // and rotated; w/h span -w/2..w/2 and -h/2..h/2 (h = depth).
  function drawGlyph(ctx, kind, w, h, strokeColor) {
    ctx.save();
    ctx.strokeStyle = strokeColor;
    ctx.lineWidth = Math.max(1, Math.min(w, h) * 0.04);
    ctx.lineCap = 'round';
    const hw = w / 2, hh = h / 2;

    function line(x1, y1, x2, y2) {
      ctx.beginPath(); ctx.moveTo(x1, y1); ctx.lineTo(x2, y2); ctx.stroke();
    }
    function rect(x, y, ww, hh2, r) {
      ctx.beginPath();
      ctx.roundRect ? ctx.roundRect(x, y, ww, hh2, r) : ctx.rect(x, y, ww, hh2);
      ctx.stroke();
    }
    function ellipse(x, y, rx, ry) {
      ctx.beginPath(); ctx.ellipse(x, y, rx, ry, 0, 0, Math.PI * 2); ctx.stroke();
    }

    switch (kind) {
      case 'sofa': {
        const backInset = hh * 0.32;
        line(-hw + hw * 0.1, -hh + backInset, hw - hw * 0.1, -hh + backInset);
        const seats = Math.max(1, Math.round(w / 70));
        for (let i = 1; i < seats; i++) {
          const x = -hw + (w / seats) * i;
          line(x, -hh + backInset, x, hh - hh * 0.1);
        }
        rect(-hw + 1, -hh + 1, w * 0.08, h - 2, 2);
        rect(hw - 1 - w * 0.08, -hh + 1, w * 0.08, h - 2, 2);
        break;
      }
      case 'sofa-corner': {
        const inset = Math.min(hw, hh) * 0.3;
        line(-hw + hw * 0.08, -hh + inset, hw - hw * 0.08, -hh + inset);
        line(-hw + inset, -hh + hh * 0.08, -hw + inset, hh - hh * 0.08);
        break;
      }
      case 'armchair': {
        const backInset = hh * 0.28;
        line(-hw + hw * 0.12, -hh + backInset, hw - hw * 0.12, -hh + backInset);
        rect(-hw + 1, -hh + 1, w * 0.12, h - 2, 2);
        rect(hw - 1 - w * 0.12, -hh + 1, w * 0.12, h - 2, 2);
        break;
      }
      case 'chair': {
        line(-hw + hw * 0.15, -hh + hh * 0.15, hw - hw * 0.15, -hh + hh * 0.15);
        break;
      }
      case 'bed': {
        const pillowH = hh * 0.5, pillowInset = hh * 0.12;
        const single = w < 120;
        if (single) {
          rect(-hw * 0.55, -hh + pillowInset, hw * 1.1, pillowH * 0.7, 4);
        } else {
          rect(-hw + hw * 0.12, -hh + pillowInset, hw * 0.7, pillowH * 0.7, 4);
          rect(hw * 0.12, -hh + pillowInset, hw * 0.7, pillowH * 0.7, 4);
        }
        line(-hw, -hh + pillowH + pillowInset, hw, -hh + pillowH + pillowInset);
        break;
      }
      case 'shelf': {
        const n = Math.max(2, Math.round(w / 35));
        for (let i = 1; i < n; i++) {
          const x = -hw + (w / n) * i;
          line(x, -hh + 2, x, hh - 2);
        }
        break;
      }
      case 'cabinet': {
        if (w > 75) line(0, -hh + 2, 0, hh - 2);
        break;
      }
      case 'round': {
        // base shape itself is drawn as ellipse by renderer; no extra glyph
        break;
      }
      case 'toilet': {
        rect(-hw, -hh, w, hh * 0.5, 3);
        ellipse(0, hh * 0.15, hw * 0.7, hh * 0.55);
        break;
      }
      case 'sink': {
        ellipse(0, 0, hw * 0.6, hh * 0.55);
        break;
      }
      case 'bathtub': {
        rect(-hw + w * 0.06, -hh + h * 0.08, w * 0.88, h * 0.84, Math.min(w, h) * 0.15);
        break;
      }
      case 'shower': {
        line(-hw, -hh, hw, hh);
        line(hw, -hh, -hw, hh);
        break;
      }
      case 'plant': {
        line(0, -hh * 0.2, 0, -hh * 0.95);
        line(0, -hh * 0.6, -hw * 0.5, -hh * 0.9);
        line(0, -hh * 0.6, hw * 0.5, -hh * 0.9);
        break;
      }
      case 'tv': {
        rect(-hw + w * 0.04, -hh + h * 0.15, w * 0.92, h * 0.7, 1);
        break;
      }
      case 'rug':
      default:
        break;
    }
    ctx.restore();
  }

  return { ITEMS, CATEGORY_ORDER, allItems, byId, addCustomItem, deleteCustomItem, getCustomItems, drawGlyph };
})();
