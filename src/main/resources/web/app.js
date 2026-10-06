// Shows a game's Swing GUI, streamed from the server as tiles, and sends the mouse and keys back to it.
// See web.FrameStreamer for the frame format, and web.InputForwarder for the input messages.
'use strict';

const canvas = document.getElementById('screen');
const ctx = canvas.getContext('2d');
const tooltip = document.getElementById('tooltip');
const notice = document.getElementById('notice');

let ws;
let lastMouse = {x: 0, y: 0};

function connect() {
    const protocol = location.protocol === 'https:' ? 'wss' : 'ws';
    ws = new WebSocket(`${protocol}://${location.host}/ws`);
    ws.binaryType = 'arraybuffer';
    ws.onopen = sendResize;
    ws.onmessage = e => typeof e.data === 'string' ? onText(JSON.parse(e.data)) : onFrame(e.data);
    ws.onclose = () => showNotice('Disconnected. Reload the page to start a new game.');
}

function send(msg) {
    if (ws && ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(msg));
}

function showNotice(text) {
    notice.textContent = text;
}

// ---- server to browser

function onText(msg) {
    switch (msg.type) {
        case 'tooltip':
            showTooltip(msg.text);
            break;
        case 'error':
            showNotice(msg.message);
            break;
    }
}

// Frames are drawn in arrival order, each once all of its tiles are decoded, so a frame never shows half-drawn.
let frameChain = Promise.resolve();

function onFrame(buffer) {
    frameChain = frameChain.then(() => drawFrame(buffer)).catch(err => console.error('frame', err));
}

async function drawFrame(buffer) {
    const headerLength = new DataView(buffer).getInt32(0);
    const header = JSON.parse(new TextDecoder().decode(new Uint8Array(buffer, 4, headerLength)));
    let offset = 4 + headerLength;
    const placed = header.tiles.map(t => {
        const bytes = new Uint8Array(buffer, offset, t.len);
        offset += t.len;
        return {t, bytes};
    });
    const bitmaps = await Promise.all(placed.map(p => createImageBitmap(new Blob([p.bytes], {type: 'image/png'}))));

    if (canvas.width !== header.w || canvas.height !== header.h) {
        canvas.width = header.w;
        canvas.height = header.h;
        const dpr = window.devicePixelRatio || 1;
        canvas.style.width = `${header.w / dpr}px`;
        canvas.style.height = `${header.h / dpr}px`;
    }
    placed.forEach((p, i) => {
        ctx.drawImage(bitmaps[i], p.t.x, p.t.y);
        bitmaps[i].close();
    });
    showNotice('');
}

function showTooltip(text) {
    if (!text) {
        tooltip.hidden = true;
        return;
    }
    // Swing tooltips may be HTML ("<html>...") from the game's own GUI code
    if (text.toLowerCase().startsWith('<html>')) tooltip.innerHTML = text;
    else tooltip.textContent = text;
    tooltip.hidden = false;
    placeTooltip();
}

function placeTooltip() {
    const margin = 14;
    let x = lastMouse.x + margin, y = lastMouse.y + margin;
    const r = tooltip.getBoundingClientRect();
    if (x + r.width > window.innerWidth) x = Math.max(0, lastMouse.x - r.width - margin);
    if (y + r.height > window.innerHeight) y = Math.max(0, lastMouse.y - r.height - margin);
    tooltip.style.left = `${x}px`;
    tooltip.style.top = `${y}px`;
}

// ---- browser to server

function sendResize() {
    send({type: 'resize', w: window.innerWidth, h: window.innerHeight, dpr: window.devicePixelRatio || 1});
}

let resizeTimer;
window.addEventListener('resize', () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(sendResize, 200);
});

function modifiers(e) {
    return {shift: e.shiftKey, ctrl: e.ctrlKey, alt: e.altKey, meta: e.metaKey, buttons: e.buttons ?? 0};
}

function mouse(kind, e) {
    lastMouse = {x: e.clientX, y: e.clientY};
    send({type: 'mouse', kind, x: Math.round(e.offsetX), y: Math.round(e.offsetY), button: e.button,
        clicks: e.detail, ...modifiers(e)});
}

// Moves are sent at most once per animation frame.
let pendingMove = null;
canvas.addEventListener('mousemove', e => {
    lastMouse = {x: e.clientX, y: e.clientY};
    if (!tooltip.hidden) placeTooltip();
    if (pendingMove === null) requestAnimationFrame(() => {
        mouse('move', pendingMove);
        pendingMove = null;
    });
    pendingMove = e;
});
canvas.addEventListener('mousedown', e => {
    canvas.focus();
    mouse('down', e);
});
// mouseup is caught on the window so a drag that ends outside the canvas still releases
window.addEventListener('mouseup', e => {
    if (e.target === canvas) mouse('up', e);
    else send({type: 'mouse', kind: 'up', x: -1, y: -1, button: e.button, clicks: 1, ...modifiers(e)});
});
canvas.addEventListener('mouseenter', e => mouse('enter', e));
canvas.addEventListener('mouseleave', e => mouse('leave', e));
canvas.addEventListener('contextmenu', e => e.preventDefault());
canvas.addEventListener('wheel', e => {
    e.preventDefault();
    send({type: 'wheel', x: Math.round(e.offsetX), y: Math.round(e.offsetY), deltaY: e.deltaY, ...modifiers(e)});
}, {passive: false});

function key(kind, e) {
    // leave browser shortcuts (reload, dev tools, ...) alone
    if (e.ctrlKey || e.metaKey) return;
    e.preventDefault();
    send({type: 'key', kind, key: e.key, ...modifiers(e)});
}

canvas.addEventListener('keydown', e => key('down', e));
canvas.addEventListener('keyup', e => key('up', e));

connect();
