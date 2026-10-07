// The play page shows a game's Swing GUI, streamed from the server as tiles, and sends the mouse and keys back to
// it. The page's query string is the game setup (see web.SessionConfig), and is repeated on the WebSocket URL.
// See web.FrameStreamer for the frame format, and web.InputForwarder for the input messages.
'use strict';

const $ = id => document.getElementById(id);
const stage = $('stage');
const canvas = $('screen');
const ctx = canvas.getContext('2d');
const tooltip = $('tooltip');

let ws;
let lastMouse = {x: 0, y: 0};
let game = null;   // from the server's 'started' message: {game, seed, seat, players}

if (!location.search) location.replace('./');

function displayName(name) {
    return name.replace(/([a-z])([A-Z])/g, '$1 $2').replace(/([A-Z])([A-Z][a-z])/g, '$1 $2');
}

function connect() {
    const protocol = location.protocol === 'https:' ? 'wss' : 'ws';
    ws = new WebSocket(`${protocol}://${location.host}/ws${location.search}`);
    ws.binaryType = 'arraybuffer';
    ws.onopen = sendResize;
    ws.onmessage = e => typeof e.data === 'string' ? onText(JSON.parse(e.data)) : onFrame(e.data);
    ws.onclose = () => {
        if (!$('notice').dataset.error) showNotice('Disconnected. Use Restart or New game to play again.');
        setStatus('Disconnected', '');
    };
}

function send(msg) {
    if (ws && ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(msg));
}

function showNotice(text, isError = false) {
    $('notice').textContent = text;
    if (isError) $('notice').dataset.error = '1';
}

function setStatus(text, kind) {
    const status = $('status');
    status.textContent = text;
    status.className = `status ${kind}`;
}

// ---- links to play again

function playUrl(seed) {
    const query = new URLSearchParams(location.search);
    if (seed === null) query.delete('seed');
    else query.set('seed', seed);
    return `play.html?${query}`;
}

function restart() {
    if (game) location.href = playUrl(game.seed);
    else location.reload();
}

function newGame() {
    location.href = playUrl(null);
}

function backToSetup() {
    location.href = './';
}

$('restart').addEventListener('click', restart);
$('new-game').addEventListener('click', newGame);
$('setup').addEventListener('click', backToSetup);
$('results-restart').addEventListener('click', restart);
$('results-new').addEventListener('click', newGame);
$('results-close').addEventListener('click', () => $('results').close());

// ---- server to browser

function onText(msg) {
    switch (msg.type) {
        case 'started':
            game = msg;
            $('title').textContent = displayName(msg.game);
            $('seed').textContent = `Seed ${msg.seed}`;
            document.title = `${displayName(msg.game)} · TAG Play`;
            $('actions-panel').hidden = !msg.actionsInPage;
            $('info-panel').hidden = !msg.infoInPage;
            $('history-panel').hidden = !msg.infoInPage;
            $('sidebar').hidden = !msg.actionsInPage && !msg.infoInPage;
            break;
        case 'actions':
            showActions(msg.actions);
            break;
        case 'info':
            showInfo(msg.lines);
            break;
        case 'history':
            addHistory(msg.lines, msg.reset);
            break;
        case 'message':
            showToast(msg.title, msg.text);
            break;
        case 'insight':
            showInsight(msg);
            break;
        case 'log':
            downloadLog(msg);
            break;
        case 'turn':
            if (msg.yourTurn) setStatus('Your turn', 'your-turn');
            else setStatus(`${playerName(msg.player)} is thinking`, 'thinking');
            break;
        case 'gameOver':
            showResults(msg.results);
            break;
        case 'tooltip':
            showTooltip(msg.text);
            break;
        case 'error':
            showNotice(msg.message, true);
            break;
    }
}

// Text from the game's own GUI code may be HTML ("<html>..."), as Swing allows.
function setRichText(element, text) {
    if (text.toLowerCase().startsWith('<html>')) element.innerHTML = text;
    else element.textContent = text;
}

// ---- the sidebar: the GUI's actions, game information and history

function showActions(actions) {
    const list = $('action-list');
    list.replaceChildren();
    $('action-count').textContent = actions.length > 1 ? `${actions.length} choices` : '';
    const filter = $('action-filter');
    filter.hidden = actions.length <= 10;
    if (actions.length === 0) {
        const p = document.createElement('p');
        p.className = 'waiting';
        p.textContent = 'Nothing to choose now.';
        list.append(p);
        return;
    }
    for (const a of actions) {
        const b = document.createElement('button');
        setRichText(b, a.label);
        b.dataset.label = a.label.toLowerCase();
        b.addEventListener('click', () => {
            // Only one choice per decision. The list is replaced when the server sends the next one.
            for (const other of list.querySelectorAll('button')) other.disabled = true;
            send({type: 'action', i: a.i, label: a.label});
        });
        b.addEventListener('mouseenter', () => send({type: 'actionHover', i: a.i, enter: true}));
        b.addEventListener('mouseleave', () => send({type: 'actionHover', i: a.i, enter: false}));
        list.append(b);
    }
    applyFilter();
}

function applyFilter() {
    const text = $('action-filter').value.trim().toLowerCase();
    for (const b of $('action-list').querySelectorAll('button'))
        b.hidden = text !== '' && !b.dataset.label.includes(text);
}

$('action-filter').addEventListener('input', applyFilter);

function showInfo(lines) {
    const info = $('info');
    info.replaceChildren();
    for (const line of lines) {
        const at = line.indexOf(': ');
        if (at > 0 && !line.toLowerCase().startsWith('<html>')) {
            const dt = document.createElement('dt');
            dt.textContent = line.slice(0, at);
            const dd = document.createElement('dd');
            dd.textContent = line.slice(at + 2);
            info.append(dt, dd);
        } else {
            const dd = document.createElement('dd');
            dd.className = 'whole';
            setRichText(dd, line);
            info.append(dd);
        }
    }
}

function addHistory(lines, reset) {
    const history = $('history');
    const atBottom = history.scrollTop + history.clientHeight >= history.scrollHeight - 4;
    if (reset) history.replaceChildren();
    for (const line of lines) {
        const li = document.createElement('li');
        li.textContent = line;
        history.append(li);
    }
    if (atBottom || reset) history.scrollTop = history.scrollHeight;
}

// ---- what the AI players weighed up for their last decisions (only sent if the setup asked for it)

const insights = new Map();   // player -> their latest decision

function showInsight(msg) {
    insights.set(msg.player, msg);
    $('insight-panel').hidden = false;
    $('sidebar').hidden = false;
    const container = $('insight');
    container.replaceChildren();
    for (const player of [...insights.keys()].sort((a, b) => a - b)) {
        const decision = insights.get(player);
        const section = document.createElement('li');
        section.className = 'decision';
        const head = document.createElement('p');
        head.className = 'insight-head';
        head.textContent = `${playerName(player)} chose ${decision.chosen}` +
            (decision.considered > 1 ? `, of ${decision.considered} options:` : '.');
        const list = document.createElement('ol');
        list.className = 'candidates';
        for (const c of decision.candidates) {
            const li = document.createElement('li');
            if (c.chosen) li.className = 'chosen';
            const label = document.createElement('span');
            label.className = 'label';
            label.textContent = c.label;
            const bar = document.createElement('span');
            bar.className = 'bar';
            bar.style.setProperty('--share', `${Math.round((c.share ?? 0) * 100)}%`);
            const numbers = document.createElement('span');
            numbers.className = 'numbers';
            numbers.textContent = `${Math.round((c.share ?? 0) * 100)}% of search` +
                (c.value !== undefined ? ` · value ${c.value.toFixed(2)}` : '');
            li.append(label, bar, numbers);
            list.append(li);
        }
        section.append(head, list);
        container.append(section);
    }
}

// ---- the game log

$('log').addEventListener('click', () => send({type: 'log'}));

function downloadLog(log) {
    delete log.type;
    log.link = location.href;
    const blob = new Blob([JSON.stringify(log, null, 2)], {type: 'application/json'});
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `${log.game}-seed${log.seed}-${log.actions.length}-actions.json`;
    document.body.append(a);
    a.click();
    a.remove();
    setTimeout(() => URL.revokeObjectURL(a.href), 1000);
}

// ---- messages the game would show in a dialog

function showToast(title, text) {
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.setAttribute('role', 'status');
    if (title) {
        const strong = document.createElement('strong');
        strong.textContent = title;
        toast.append(strong);
    }
    const body = document.createElement('div');
    setRichText(body, text);
    const close = document.createElement('button');
    close.className = 'close';
    close.textContent = '×';
    close.title = 'Dismiss';
    close.addEventListener('click', () => toast.remove());
    toast.append(body, close);
    $('toasts').append(toast);
    // long messages (results tables) stay until dismissed
    if (text.length < 200) setTimeout(() => toast.remove(), 8000);
}

function playerName(i) {
    if (!game) return `Player ${i}`;
    return i === game.seat ? 'You' : `Player ${i} (${game.players[i]})`;
}

function ordinal(n) {
    const s = ['th', 'st', 'nd', 'rd'], v = n % 100;
    return n + (s[(v - 20) % 10] || s[v] || s[0]);
}

function showResults(results) {
    const sorted = [...results].sort((a, b) => a.position - b.position);
    const body = $('results-body');
    body.replaceChildren();
    for (const r of sorted) {
        const tr = document.createElement('tr');
        if (game && r.player === game.seat) tr.className = 'you';
        for (const [text, cls] of [[ordinal(r.position), ''], [playerName(r.player), ''], [formatScore(r.score), 'num']]) {
            const td = document.createElement('td');
            td.textContent = text;
            if (cls) td.className = cls;
            tr.append(td);
        }
        body.append(tr);
    }
    const you = game && results.find(r => r.player === game.seat);
    const title = !you ? 'Game over'
        : you.position === 1 ? (sorted.filter(r => r.position === 1).length > 1 ? 'A draw for first place' : 'You won!')
            : `You finished ${ordinal(you.position)}`;
    $('results-title').textContent = title;
    setStatus(`Game over: ${title.replace(/!$/, '')}`, '');
    $('results').showModal();
}

function formatScore(score) {
    return Number.isInteger(score) ? String(score) : score.toFixed(1);
}

// The frame is laid out to the stage, unless the GUI needs more room than that; then it is scaled down to fit, and
// mouse positions are scaled back up.
let frameSize = null;
let scale = 1;

function fitCanvas() {
    if (!frameSize) return;
    scale = Math.min(1, stage.clientWidth / frameSize.w, stage.clientHeight / frameSize.h);
    canvas.style.width = `${frameSize.w * scale}px`;
    canvas.style.height = `${frameSize.h * scale}px`;
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
    }
    frameSize = {w: header.fw, h: header.fh};
    fitCanvas();
    placed.forEach((p, i) => {
        ctx.drawImage(bitmaps[i], p.t.x, p.t.y);
        bitmaps[i].close();
    });
    if (!$('notice').dataset.error) showNotice('');
}

function showTooltip(text) {
    if (!text) {
        tooltip.hidden = true;
        return;
    }
    setRichText(tooltip, text);
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

// The GUI is laid out to the stage (the page below the bar).
function sendResize() {
    send({type: 'resize', w: stage.clientWidth, h: stage.clientHeight, dpr: window.devicePixelRatio || 1});
}

let resizeTimer;
new ResizeObserver(() => {
    fitCanvas();
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(sendResize, 200);
}).observe(stage);

function modifiers(e) {
    return {shift: e.shiftKey, ctrl: e.ctrlKey, alt: e.altKey, meta: e.metaKey, buttons: e.buttons ?? 0};
}

function mouse(kind, e) {
    lastMouse = {x: e.clientX, y: e.clientY};
    send({type: 'mouse', kind, x: Math.round(e.offsetX / scale), y: Math.round(e.offsetY / scale), button: e.button,
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
let pressedOnCanvas = false;
canvas.addEventListener('mousedown', e => {
    canvas.focus();
    pressedOnCanvas = true;
    mouse('down', e);
});
// mouseup is caught on the window so a drag that ends outside the canvas still releases
window.addEventListener('mouseup', e => {
    if (!pressedOnCanvas) return;
    pressedOnCanvas = false;
    if (e.target === canvas) mouse('up', e);
    else send({type: 'mouse', kind: 'up', x: -1, y: -1, button: e.button, clicks: 1, ...modifiers(e)});
});
canvas.addEventListener('mouseenter', e => mouse('enter', e));
canvas.addEventListener('mouseleave', e => mouse('leave', e));
canvas.addEventListener('contextmenu', e => e.preventDefault());
canvas.addEventListener('wheel', e => {
    e.preventDefault();
    send({type: 'wheel', x: Math.round(e.offsetX / scale), y: Math.round(e.offsetY / scale), deltaY: e.deltaY, ...modifiers(e)});
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
