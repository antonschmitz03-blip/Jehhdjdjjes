'use strict';

(() => {
  const canvas = document.getElementById('canvas');
  const ctx = canvas.getContext('2d');

  let renderScheduled = false;
  function requestRender() {
    if (renderScheduled) return;
    renderScheduled = true;
    requestAnimationFrame(() => {
      renderScheduled = false;
      Render.render(canvas, ctx, Store);
    });
  }
  window.requestRender = requestRender;

  function resizeCanvas() {
    const wrap = canvas.parentElement;
    const dpr = window.devicePixelRatio || 1;
    const w = wrap.clientWidth;
    const h = wrap.clientHeight;
    canvas.width = Math.round(w * dpr);
    canvas.height = Math.round(h * dpr);
    canvas.style.width = w + 'px';
    canvas.style.height = h + 'px';
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    requestRender();
  }

  window.addEventListener('resize', resizeCanvas);

  Store.init();
  Input.init(canvas);
  UI.init();
  resizeCanvas();
  requestRender();
})();
