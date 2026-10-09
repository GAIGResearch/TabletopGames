// The play page shows a game's Swing GUI, streamed from the server as tiles, and sends the mouse, touches and keys back
// to it. The page's query string is the game setup (see web.SessionConfig), and is repeated on the WebSocket URL.
// See web.FrameStreamer for the frame format, web.InputForwarder for the input messages, and web.ChromeReader for the
// actions, information, history and click regions drawn in the page.
'use strict';

const $ = id => document.getElementById(id);
const stage = $('stage');
const canvas = $('screen');
const ctx = canvas.getContext('2d');
const overlay = $('overlay');
const octx = overlay.getContext('2d');
const tooltip = $('tooltip');

let ws;
let lastMouse = {x: 0, y: 0};
let game = null;   // from the server's 'started' message: {game, seed, seat, players}

if (!location.search) location.replace('./');

function displayName(name) {
    return name.replace(/([a-z])([A-Z])/g, '$1 $2').replace(/([A-Z])([A-Z][a-z])/g, '$1 $2');
}

// "PlayCard" or "PLAY_CARD" as "Play card"
function humanise(name) {
    const words = (/^[A-Z0-9_]+$/.test(name) ? name.toLowerCase().replace(/_/g, ' ') : displayName(name).replace(/_/g, ' '))
        .split(' ').filter(w => w);
    return words.map((w, i) => i === 0 ? w[0].toUpperCase() + w.slice(1)
        : /^[A-Z0-9]+$/.test(w) && w.length > 1 ? w : w.toLowerCase()).join(' ');
}

// ---- the connection, which reconnects to the same game when it is lost

// The server gives each game a key with which to reconnect to it. It is kept for this tab, so a reload reconnects too.
const keyStore = `tag-session:${location.search}`;
let sessionKey = sessionStorage.getItem(keyStore);
let ended = false;        // the game is over for this page: no reconnecting
let retries = 0;
let retryTimer = null;

function connect() {
    retryTimer = null;
    const protocol = location.protocol === 'https:' ? 'wss' : 'ws';
    const query = new URLSearchParams(location.search);
    if (sessionKey) query.set('resume', sessionKey);
    ws = new WebSocket(`${protocol}://${location.host}/ws?${query}`);
    ws.binaryType = 'arraybuffer';
    ws.onopen = () => {
        retries = 0;
        sendResize();
    };
    ws.onmessage = e => typeof e.data === 'string' ? onText(JSON.parse(e.data)) : onFrame(e.data);
    ws.onclose = () => {
        if (!ended && sessionKey) {
            scheduleReconnect();
            return;
        }
        if (!$('notice').dataset.error) showNotice('Disconnected. Use Restart or New game to play again.');
        setStatus('Disconnected', '');
    };
}

function scheduleReconnect() {
    setStatus('Reconnecting', 'thinking');
    showNotice('The connection was lost. Reconnecting…');
    clearTimeout(retryTimer);
    retryTimer = setTimeout(connect, Math.min(10000, 500 * 2 ** retries++));
}

// reconnect at once when the network or the tab comes back
function reconnectNow() {
    if (retryTimer === null) return;
    clearTimeout(retryTimer);
    connect();
}

window.addEventListener('online', reconnectNow);
document.addEventListener('visibilitychange', () => {
    if (!document.hidden) {
        reconnectNow();
        document.title = baseTitle;
    }
});

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

// Leaving for another game ends this one on the server, rather than leaving it waiting for this page to come back.
function leave(url) {
    ended = true;
    send({type: 'quit'});
    sessionStorage.removeItem(keyStore);
    if (url) location.href = url;
    else location.reload();
}

function restart() {
    leave(game ? playUrl(game.seed) : null);
}

function newGame() {
    leave(playUrl(null));
}

function backToSetup() {
    leave('./');
}

$('restart').addEventListener('click', restart);
$('new-game').addEventListener('click', newGame);
$('setup').addEventListener('click', backToSetup);
$('results-restart').addEventListener('click', restart);
$('results-new').addEventListener('click', newGame);
$('results-close').addEventListener('click', () => $('results').close());

// ---- server to browser

let baseTitle = document.title;

function onText(msg) {
    switch (msg.type) {
        case 'session':
            sessionKey = msg.key;
            sessionStorage.setItem(keyStore, msg.key);
            break;
        case 'started':
            game = msg;
            $('title').textContent = displayName(msg.game);
            $('seed').textContent = `Seed ${msg.seed}`;
            baseTitle = `${displayName(msg.game)} · TAG Play`;
            document.title = baseTitle;
            $('actions-panel').hidden = !msg.actionsInPage;
            $('info-panel').hidden = !msg.infoInPage;
            $('history-panel').hidden = !msg.infoInPage;
            $('sidebar').hidden = !msg.actionsInPage && !msg.infoInPage;
            $('panel-toggle').hidden = $('sidebar').hidden;
            stageChanged();
            if (!$('notice').dataset.error) showNotice('');
            break;
        case 'actions':
            showActions(msg.actions);
            break;
        case 'info':
            showInfo(msg);
            break;
        case 'history':
            addHistory(msg.lines, msg.reset);
            break;
        case 'wheelAreas':
            wheelAreas = msg.areas;
            break;
        case 'rules':
            setRules(msg.pages);
            break;
        case 'regions':
            setRegions(msg);
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
            onTurn(msg);
            break;
        case 'gameOver':
            showResults(msg.results);
            break;
        case 'tooltip':
            showTooltip(msg.text);
            break;
        case 'error':
            ended = true;
            if (msg.code === 'gone') {
                sessionStorage.removeItem(keyStore);
                sessionKey = null;
            }
            showNotice(msg.code ? `${msg.message} Use Restart or New game to play again.` : msg.message, true);
            break;
    }
}

// Text from the game's own GUI code may be HTML ("<html>..."), as Swing allows.
function setRichText(element, text) {
    if (text.toLowerCase().startsWith('<html>')) element.innerHTML = text;
    else element.textContent = text;
}

// ---- whose turn it is, and what happened while the browser player waited

let wasYourTurn = false;
let summaryFrom = 0;    // the history entry from which the summary starts: the first after the player's last move

function onTurn(msg) {
    if (msg.yourTurn) {
        setStatus('Your turn', 'your-turn');
        if (!wasYourTurn) {
            showSummary();
            notifyTurn();
        }
    } else {
        setStatus(`${playerName(msg.player)} is thinking`, 'thinking');
        if (wasYourTurn) {
            summaryFrom = historyLog.length;
            $('summary-panel').hidden = true;
        }
    }
    wasYourTurn = msg.yourTurn;
}

function showSummary() {
    const others = historyLog.slice(summaryFrom).filter(l => !game || l.player !== game.seat);
    const list = $('summary');
    list.replaceChildren();
    $('summary-panel').hidden = others.length === 0;
    const shown = 8;
    if (others.length > shown) {
        const li = document.createElement('li');
        li.className = 'muted';
        li.textContent = `… ${others.length - shown} earlier`;
        list.append(li);
    }
    for (const line of others.slice(-shown)) list.append(historyItem(line));
}

$('summary-close').addEventListener('click', () => $('summary-panel').hidden = true);

// When it becomes the player's turn while the tab is in the background, the tab's title says so, and a sound plays
// unless switched off.
let soundOn = localStorage.getItem('tag-sound') !== 'off';
let audio = null;

function showSoundSetting() {
    $('sound').textContent = soundOn ? '🔔' : '🔕';
    $('sound').setAttribute('aria-label', soundOn ? 'Sound on' : 'Sound off');
}

$('sound').addEventListener('click', () => {
    soundOn = !soundOn;
    localStorage.setItem('tag-sound', soundOn ? 'on' : 'off');
    showSoundSetting();
});
showSoundSetting();

// A browser only lets a page play sound once the user has interacted with it. Any interaction also shows the player has
// seen the page, so the title no longer needs to say it is their turn.
function unlockAudio() {
    document.title = baseTitle;
    try {
        audio ??= new AudioContext();
        if (audio.state === 'suspended') audio.resume();
    } catch (e) {
        audio = null;
    }
}

window.addEventListener('pointerdown', unlockAudio, {capture: true});
window.addEventListener('keydown', unlockAudio, {capture: true});

function notifyTurn() {
    if (!document.hidden) return;
    document.title = `● Your turn · ${baseTitle}`;
    if (!soundOn || !audio) return;
    const t = audio.currentTime;
    for (const [at, freq] of [[0, 660], [0.16, 880]]) {
        const osc = audio.createOscillator(), gain = audio.createGain();
        osc.frequency.value = freq;
        gain.gain.setValueAtTime(0.0001, t + at);
        gain.gain.exponentialRampToValueAtTime(0.15, t + at + 0.02);
        gain.gain.exponentialRampToValueAtTime(0.0001, t + at + 0.25);
        osc.connect(gain).connect(audio.destination);
        osc.start(t + at);
        osc.stop(t + at + 0.3);
    }
}

// ---- the sidebar: the GUI's actions, game information and history

// A long list is grouped by the kind of action (its class), with the kinds offered only once listed first. The groups
// the player opens or closes stay so for the rest of the game.
const GROUP_OVER = 8;
const groupChoice = new Map();   // kind -> open

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
        updateShortcuts();
        return;
    }
    const groups = new Map();
    for (const a of actions) {
        const kind = a.kind ?? '';
        if (!groups.has(kind)) groups.set(kind, []);
        groups.get(kind).push(a);
    }
    const multi = [...groups.values()].filter(g => g.length > 1);
    if (actions.length <= GROUP_OVER || groups.size < 2 || multi.length === 0) {
        for (const a of actions) list.append(actionButton(a));
    } else {
        for (const a of actions) if (groups.get(a.kind ?? '').length === 1) list.append(actionButton(a));
        for (const [kind, group] of groups) {
            if (group.length === 1) continue;
            const details = document.createElement('details');
            details.className = 'group';
            details.dataset.kind = kind;
            details.open = groupChoice.get(kind) ?? (multi.length === 1 || group.length <= 6);
            const summary = document.createElement('summary');
            summary.textContent = `${kind ? humanise(kind) : 'Other'} (${group.length})`;
            summary.addEventListener('click', () => {
                groupChoice.set(kind, !details.open);
                // the details open or close after the click
                setTimeout(updateShortcuts);
            });
            details.append(summary, ...group.map(actionButton));
            list.append(details);
        }
    }
    applyFilter();
}

function actionButton(a) {
    const b = document.createElement('button');
    b.className = 'action';
    setRichText(b, a.label);
    b.dataset.label = a.label.toLowerCase();
    b.addEventListener('click', () => {
        // Only one choice per decision. The list is replaced when the server sends the next one.
        actionChosen();
        send({type: 'action', i: a.i, label: a.label});
    });
    b.addEventListener('mouseenter', () => send({type: 'actionHover', i: a.i, enter: true}));
    b.addEventListener('mouseleave', () => send({type: 'actionHover', i: a.i, enter: false}));
    return b;
}

// After a choice (on a button or the board), nothing more can be chosen until the server offers the next decision.
function actionChosen() {
    for (const b of $('action-list').querySelectorAll('button')) b.disabled = true;
    $('summary-panel').hidden = true;
    // the GUI's tooltip described what the choice would do
    tooltip.hidden = true;
    setRegions({v: regionsVersion, regions: []});
}

function applyFilter() {
    const text = $('action-filter').value.trim().toLowerCase();
    for (const b of $('action-list').querySelectorAll('button.action'))
        b.hidden = text !== '' && !b.dataset.label.includes(text);
    for (const details of $('action-list').querySelectorAll('details.group')) {
        const matches = [...details.querySelectorAll('button.action')].some(b => !b.hidden);
        details.hidden = !matches;
        if (text !== '') details.open = matches;
        else details.open = groupChoice.get(details.dataset.kind) ?? details.open;
    }
    updateShortcuts();
}

$('action-filter').addEventListener('input', applyFilter);

// Keys 1 to 9 choose the first nine actions showing, numbered on their buttons.
let shortcuts = [];

function updateShortcuts() {
    for (const b of shortcuts) delete b.dataset.key;
    shortcuts = [...$('action-list').querySelectorAll('button.action')]
        .filter(b => !b.hidden && !b.closest('details:not([open])') && !b.closest('[hidden]'))
        .slice(0, 9);
    shortcuts.forEach((b, i) => b.dataset.key = String(i + 1));
}

const shortcutKeysDown = new Set();
window.addEventListener('keydown', e => {
    if (e.ctrlKey || e.metaKey || e.altKey || !/^[1-9]$/.test(e.key)) return;
    if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;
    const b = shortcuts[Number(e.key) - 1];
    if (!b || b.disabled || $('sidebar').hidden || $('actions-panel').hidden) return;
    // not passed on to the game's GUI
    e.preventDefault();
    e.stopPropagation();
    shortcutKeysDown.add(e.key);
    b.click();
}, {capture: true});
window.addEventListener('keyup', e => {
    if (shortcutKeysDown.delete(e.key)) e.stopPropagation();
}, {capture: true});

// The standard information is shown as a scoreboard of the players, with whoever is to play marked; the round, turn
// and phase; and any other lines the GUI shows.
function showInfo(msg) {
    const board = $('scoreboard');
    board.replaceChildren();
    const nPlayers = game ? game.players.length : (msg.scores ?? []).length;
    board.hidden = nPlayers === 0 || (msg.scores === undefined && msg.current === undefined);
    for (let i = 0; i < nPlayers; i++) {
        const tr = document.createElement('tr');
        if (i === msg.current) tr.classList.add('current');
        if (game && i === game.seat) tr.classList.add('you');
        const marker = document.createElement('td');
        marker.className = 'marker';
        marker.textContent = i === msg.current ? '▶' : '';
        marker.title = i === msg.current ? 'To play' : '';
        const name = document.createElement('td');
        name.textContent = playerName(i);
        tr.append(marker, name);
        if (msg.scores) {
            const score = document.createElement('td');
            score.className = 'num';
            score.textContent = msg.scores[i] === undefined ? '' : formatScore(msg.scores[i]);
            tr.append(score);
        }
        board.append(tr);
    }
    const progress = [];
    if (msg.round !== undefined) progress.push(`Round ${msg.round}`);
    if (msg.turn !== undefined) progress.push(`Turn ${msg.turn}`);
    if (msg.phase !== undefined) progress.push(humanise(msg.phase));
    $('progress').textContent = progress.join(' · ');
    $('progress').hidden = progress.length === 0;

    const info = $('info');
    info.replaceChildren();
    for (const line of msg.lines) {
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

const historyLog = [];   // {player, text}, player -1 for a line about no one player

function historyItem(line) {
    const li = document.createElement('li');
    if (line.player >= 0) {
        const who = document.createElement('span');
        who.className = 'who';
        who.textContent = shortName(line.player);
        li.append(who, ' ');
        if (game && line.player === game.seat) li.className = 'mine';
    }
    li.append(line.text);
    return li;
}

function addHistory(lines, reset) {
    const history = $('history');
    const atBottom = history.scrollTop + history.clientHeight >= history.scrollHeight - 4;
    if (reset) {
        history.replaceChildren();
        historyLog.length = 0;
    }
    for (const line of lines) {
        historyLog.push(line);
        history.append(historyItem(line));
    }
    summaryFrom = Math.min(summaryFrom, historyLog.length);
    if (atBottom || reset) history.scrollTop = history.scrollHeight;
}

// ---- what the AI players weighed up for their last decisions (only sent if the setup asked for it)

const insights = new Map();   // player -> their latest decision

function showInsight(msg) {
    insights.set(msg.player, msg);
    $('insight-panel').hidden = false;
    if ($('sidebar').hidden) {
        $('sidebar').hidden = false;
        $('panel-toggle').hidden = false;
        stageChanged();
    }
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

// as playerName, shorter, for the history
function shortName(i) {
    if (!game) return `P${i}`;
    return i === game.seat ? 'You' : `P${i} ${game.players[i]}`;
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
    $('summary-panel').hidden = true;
    $('results').showModal();
}

function formatScore(score) {
    return Number.isInteger(score) ? String(score) : score.toFixed(1);
}

// The frame is shown at the zoom the server drew it for: the scale at which the game fits the stage, or the player's
// own zoom (see web.GameSession.fitFrame). Zoomed in beyond the fit, it is larger than the stage, which scrolls.
// Between the player changing the zoom and the server's frame for it, the image is stretched to the new zoom, so that
// a pinch or a turn of the wheel shows at once. Pointer positions are scaled back to the frame's.
let frameSize = null;
let frameZoom = 1;
let previewZoom = null;
let previewTimer;
let scale = 1;

function fitCanvas() {
    if (!frameSize) return;
    scale = previewZoom ?? frameZoom;
    for (const c of [canvas, overlay]) {
        // whole pixels, so that a game that fits its space exactly does not overflow it by a fraction
        c.style.width = `${Math.floor(frameSize.w * scale)}px`;
        c.style.height = `${Math.floor(frameSize.h * scale)}px`;
    }
}

// ---- zoom: fit to the stage (the default), or the player's own, kept for each game

const board = $('board');
const MIN_ZOOM = 0.25, MAX_ZOOM = 4;
const ZOOMS = [0.25, 0.33, 0.5, 0.67, 0.75, 0.8, 0.9, 1, 1.1, 1.25, 1.5, 1.75, 2, 2.5, 3, 4];
const zoomStore = `tag-zoom:${new URLSearchParams(location.search).get('game') ?? ''}`;
let zoom = Number(localStorage.getItem(zoomStore)) || null;   // null to fit
let zoomTimer;

function showZoom() {
    $('zoom-fit').textContent = zoom === null ? 'Fit' : `${Math.round(zoom * 100)}%`;
    $('zoom-fit').title = zoom === null ? 'The game fits the window' : 'Fit the game to the window';
}

// Shows the frame at zoom z until the server's frame for it comes, keeping the point of the game under the anchor (a
// pointer, or the middle of the stage) where it is.
function preview(z, anchor) {
    if (!frameSize) return;
    const before = canvas.getBoundingClientRect();
    const stageBox = stage.getBoundingClientRect();
    const a = anchor ?? {clientX: stageBox.left + stage.clientWidth / 2, clientY: stageBox.top + stage.clientHeight / 2};
    const fx = (a.clientX - before.left) / scale, fy = (a.clientY - before.top) / scale;
    previewZoom = z;
    fitCanvas();
    const after = canvas.getBoundingClientRect();
    stage.scrollLeft += after.left + fx * z - a.clientX;
    stage.scrollTop += after.top + fy * z - a.clientY;
    drawOverlay();
    // in case the server's frame never matches it exactly
    clearTimeout(previewTimer);
    previewTimer = setTimeout(() => {
        previewZoom = null;
        fitCanvas();
    }, 3000);
}

// zooms to z (null to fit), telling the server at once
function setZoom(z, anchor) {
    zoom = z === null ? null : Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, z));
    showZoom();
    if (zoom !== null) preview(zoom, anchor);
    commitZoom();
}

// zooms to z as a gesture goes on, telling the server once it pauses
function zoomTo(z, anchor) {
    zoom = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, z));
    showZoom();
    preview(zoom, anchor);
    clearTimeout(zoomTimer);
    zoomTimer = setTimeout(commitZoom, 150);
}

function commitZoom() {
    clearTimeout(zoomTimer);
    if (zoom === null) localStorage.removeItem(zoomStore);
    else localStorage.setItem(zoomStore, String(zoom));
    sendResize();
}

function zoomStep(direction) {
    const current = zoom ?? frameZoom;
    const next = direction > 0 ? ZOOMS.find(z => z > current * 1.01) : ZOOMS.findLast(z => z < current * 0.99);
    if (next !== undefined) setZoom(next);
}

$('zoom-in').addEventListener('click', () => zoomStep(1));
$('zoom-out').addEventListener('click', () => zoomStep(-1));
$('zoom-fit').addEventListener('click', () => setZoom(null));
showZoom();

// ---- the wheel zooms about the pointer, except over the parts of the game that take the wheel themselves (a list
// that scrolls, a board that zooms itself; see web.ChromeReader), where the game has it. With Ctrl (which is how a
// touchpad's pinch arrives) it always zooms. A sideways scroll (on a touchpad) moves the view.

let wheelAreas = [];   // [x, y, w, h] in frame coordinates

function inWheelArea(p) {
    return wheelAreas.some(([x, y, w, h]) => p.x >= x && p.y >= y && p.x < x + w && p.y < y + h);
}

// the wheel's movement in pixels, whatever its unit
function wheelPixels(e, delta) {
    return e.deltaMode === 1 ? delta * 33 : e.deltaMode === 2 ? delta * stage.clientHeight : delta;
}

stage.addEventListener('wheel', e => {
    // the canvas passes it on to the game
    if (!e.ctrlKey && board.contains(e.target) && inWheelArea(framePoint(e))) return;
    e.preventDefault();
    e.stopPropagation();
    const dx = wheelPixels(e, e.deltaX), dy = wheelPixels(e, e.deltaY);
    if (!e.ctrlKey && Math.abs(dx) > Math.abs(dy)) {
        stage.scrollLeft += dx;
        return;
    }
    const step = Math.max(-200, Math.min(200, dy));
    zoomTo((previewZoom ?? zoom ?? frameZoom) * Math.exp(-step / 300), e);
}, {passive: false, capture: true});

// ---- moving the view about a game larger than its space: a drag with the right or middle button (a right click
// that does not move is still the game's; see the canvas's handlers), or with any button or one finger on the space
// around the game. Two fingers pinch to zoom, and move the view, anywhere on the stage.

function canPan() {
    return stage.scrollWidth > stage.clientWidth + 1 || stage.scrollHeight > stage.clientHeight + 1;
}

function panBy(dx, dy) {
    stage.scrollLeft -= dx;
    stage.scrollTop -= dy;
}

function showPanning(on) {
    document.body.classList.toggle('panning', on);
}

let pan = null;   // a drag on the space around the game: {id, x, y}

stage.addEventListener('pointerdown', e => {
    if (e.target !== stage || gesture) return;
    e.preventDefault();
    pan = {id: e.pointerId, x: e.clientX, y: e.clientY};
    try {
        stage.setPointerCapture(e.pointerId);
    } catch (err) {
        // a pointer no longer active
    }
    showPanning(true);
});
stage.addEventListener('pointermove', e => {
    if (!pan || e.pointerId !== pan.id || gesture) return;
    panBy(e.clientX - pan.x, e.clientY - pan.y);
    pan.x = e.clientX;
    pan.y = e.clientY;
});

function panEnd(e) {
    if (!pan || e.pointerId !== pan.id) return;
    pan = null;
    showPanning(false);
}

stage.addEventListener('pointerup', panEnd);
stage.addEventListener('pointercancel', panEnd);
stage.addEventListener('contextmenu', e => e.preventDefault());

// Two fingers: a press already begun by the first finger is let go, as it was not a click after all.
const touches = new Map();   // the touches down on the stage: id -> {x, y}
let gesture = null;          // {dist, zoom, mid}

function touchPair() {
    const [a, b] = [...touches.values()];
    return {dist: Math.max(1, Math.hypot(a.x - b.x, a.y - b.y)), mid: {clientX: (a.x + b.x) / 2, clientY: (a.y + b.y) / 2}};
}

stage.addEventListener('pointerdown', e => {
    if (e.pointerType !== 'touch') return;
    touches.set(e.pointerId, {x: e.clientX, y: e.clientY});
    if (touches.size === 2 && !gesture) {
        cancelPress(e);
        pan = null;
        showPanning(false);
        const pair = touchPair();
        gesture = {dist: pair.dist, zoom: scale, mid: pair.mid};
    }
    if (gesture) e.stopPropagation();
}, {capture: true});

stage.addEventListener('pointermove', e => {
    if (e.pointerType !== 'touch' || !touches.has(e.pointerId)) return;
    touches.set(e.pointerId, {x: e.clientX, y: e.clientY});
    if (!gesture) return;
    e.stopPropagation();
    if (touches.size < 2) return;
    const pair = touchPair();
    zoomTo(gesture.zoom * pair.dist / gesture.dist, pair.mid);
    panBy(pair.mid.clientX - gesture.mid.clientX, pair.mid.clientY - gesture.mid.clientY);
    gesture.mid = pair.mid;
}, {capture: true});

function touchEnd(e) {
    if (e.pointerType !== 'touch') return;
    touches.delete(e.pointerId);
    if (!gesture) return;
    e.stopPropagation();
    // the gesture lasts until the last finger is lifted, so that the one left does not click
    if (touches.size === 0) gesture = null;
}

stage.addEventListener('pointerup', touchEnd, {capture: true});
stage.addEventListener('pointercancel', touchEnd, {capture: true});


// ---- the side panel, which the player may hide to give the game the whole width

let sidebarShown = localStorage.getItem('tag-sidebar') !== 'hidden';

function showSidebarSetting() {
    document.body.classList.toggle('no-sidebar', !sidebarShown);
    $('panel-toggle').setAttribute('aria-pressed', String(sidebarShown));
}

$('panel-toggle').addEventListener('click', () => {
    sidebarShown = !sidebarShown;
    localStorage.setItem('tag-sidebar', sidebarShown ? 'shown' : 'hidden');
    showSidebarSetting();
    stageChanged();
});
showSidebarSetting();

// ---- the rules, as a web page beside the game

let rulesPages = [];
let rulesPage = 0;

function setRules(pages) {
    rulesPages = pages;
    rulesPage = Math.min(rulesPage, Math.max(0, pages.length - 1));
    $('rules-button').hidden = pages.length === 0;
    if (!$('rules-panel').hidden) showRulesPage(rulesPage);
}

function openRules(open) {
    $('rules-panel').hidden = !open;
    $('rules-button').setAttribute('aria-pressed', String(open));
    if (open) showRulesPage(rulesPage);
}

function showRulesPage(i) {
    rulesPage = i;
    const tabs = $('rules-tabs');
    tabs.replaceChildren();
    rulesPages.forEach((page, j) => {
        const b = document.createElement('button');
        b.setAttribute('role', 'tab');
        b.setAttribute('aria-selected', String(j === i));
        b.textContent = page.title;
        b.addEventListener('click', () => showRulesPage(j));
        tabs.append(b);
    });
    const body = $('rules-body');
    // The rules are HTML, which may be a whole document (as Swing gives it). Only its body is shown, so that its head
    // and styles do not reach the page.
    const doc = new DOMParser().parseFromString(rulesPages[i]?.html ?? '', 'text/html');
    body.replaceChildren(...doc.body.childNodes);
    body.scrollTop = 0;
    for (const a of body.querySelectorAll('a[href]')) {
        const href = a.getAttribute('href');
        if (href.startsWith('#')) {
            // a link within the page goes to an anchor's name (as Swing's HTML has) or an element's id
            a.addEventListener('click', e => {
                e.preventDefault();
                const id = CSS.escape(decodeURIComponent(href.slice(1)));
                body.querySelector(`[name="${id}"], [id="${id}"]`)?.scrollIntoView({block: 'start'});
            });
        } else {
            a.target = '_blank';
            a.rel = 'noopener';
        }
    }
}

$('rules-button').addEventListener('click', () => openRules($('rules-panel').hidden));
$('rules-close').addEventListener('click', () => openRules(false));

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
        canvas.width = overlay.width = header.w;
        canvas.height = overlay.height = header.h;
        drawOverlay();
    }
    if (!frameSize || frameSize.w !== header.fw || frameSize.h !== header.fh) {
        frameSize = {w: header.fw, h: header.fh};
        drawOverlay();
    }
    const z = header.zoom ?? Math.min(1, stage.clientWidth / header.fw, stage.clientHeight / header.fh);
    if (z !== frameZoom || previewZoom !== null) {
        frameZoom = z;
        // the frame for the zoom previewed has come
        if (previewZoom !== null && Math.abs(previewZoom - z) < 1e-6) previewZoom = null;
        drawOverlay();
    }
    fitCanvas();
    placed.forEach((p, i) => {
        ctx.drawImage(bitmaps[i], p.t.x, p.t.y);
        bitmaps[i].close();
    });
    if (!$('notice').dataset.error && retryTimer === null) showNotice('');
}

function showTooltip(text) {
    // a tooltip sent before the region menu opened would cover it
    if (!text || !menu.hidden) {
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

// ---- click regions: the parts of the board a click may choose an action on, answered here without waiting for the
// image. A click on one chooses its action, or opens a menu of its actions.

let regions = [];          // {path: Path2D, options: [label]}, in frame coordinates
let regionsVersion = 0;
let hovered = -1;

function setRegions(msg) {
    regionsVersion = msg.v;
    regions = msg.regions.map(r => ({path: new Path2D(r.path), options: r.options}));
    hovered = -1;
    canvas.style.cursor = '';
    hideMenu();
    drawOverlay();
}

function regionAt(p) {
    for (let i = regions.length - 1; i >= 0; i--)
        if (octx.isPointInPath(regions[i].path, p.x, p.y)) return i;
    return -1;
}

function hover(p) {
    const r = p ? regionAt(p) : -1;
    if (r === hovered) return;
    hovered = r;
    canvas.style.cursor = r >= 0 ? 'pointer' : '';
    drawOverlay();
}

function drawOverlay() {
    octx.setTransform(1, 0, 0, 1, 0, 0);
    octx.clearRect(0, 0, overlay.width, overlay.height);
    if (hovered < 0 || !frameSize) return;
    const s = overlay.width / frameSize.w;
    octx.setTransform(s, 0, 0, s, 0, 0);
    const accent = getComputedStyle(document.documentElement).getPropertyValue('--accent').trim() || '#2f6fd6';
    octx.fillStyle = `${accent}40`;
    octx.fill(regions[hovered].path);
    octx.strokeStyle = accent;
    octx.lineWidth = 2.5 / (scale || 1);
    octx.stroke(regions[hovered].path);
    // hit tests are in frame coordinates
    octx.setTransform(1, 0, 0, 1, 0, 0);
}

function chooseRegion(r, clientX, clientY) {
    if (regions[r].options.length === 1) sendRegion(r, 0);
    else showMenu(r, clientX, clientY);
}

function sendRegion(r, o) {
    send({type: 'region', v: regionsVersion, r, o});
    actionChosen();
}

const menu = $('region-menu');

function showMenu(r, clientX, clientY) {
    tooltip.hidden = true;
    menu.replaceChildren();
    regions[r].options.forEach((label, o) => {
        const b = document.createElement('button');
        b.setAttribute('role', 'menuitem');
        setRichText(b, label);
        b.addEventListener('click', () => sendRegion(r, o));
        menu.append(b);
    });
    menu.hidden = false;
    const box = menu.getBoundingClientRect();
    menu.style.left = `${Math.max(4, Math.min(clientX, window.innerWidth - box.width - 4))}px`;
    menu.style.top = `${Math.max(4, Math.min(clientY, window.innerHeight - box.height - 4))}px`;
    menu.querySelector('button')?.focus();
}

function hideMenu() {
    menu.hidden = true;
}

window.addEventListener('pointerdown', e => {
    if (!menu.hidden && !menu.contains(e.target)) hideMenu();
}, {capture: true});
window.addEventListener('keydown', e => {
    if (e.key === 'Escape' && !menu.hidden) {
        hideMenu();
        canvas.focus();
    } else if (e.key === 'Escape' && !$('rules-panel').hidden) {
        openRules(false);
    }
});

// ---- browser to server

// The GUI is laid out to the stage (the page below the bar).
function sendResize() {
    send({type: 'resize', w: stage.clientWidth, h: stage.clientHeight, dpr: window.devicePixelRatio || 1, zoom});
}

let resizeTimer;

function stageChanged() {
    fitCanvas();
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(sendResize, 200);
}

new ResizeObserver(stageChanged).observe(stage);
// A browser may not tell a tab in the background of a resize (nor run its observers), so the changes the page makes
// itself are sent as well, as is a resize of the window.
window.addEventListener('resize', stageChanged);

function modifiers(e, buttons = e.buttons ?? 0) {
    return {shift: e.shiftKey, ctrl: e.ctrlKey, alt: e.altKey, meta: e.metaKey, buttons};
}

// a pointer event's position in the frame
function framePoint(e) {
    const r = canvas.getBoundingClientRect();
    return {x: Math.round((e.clientX - r.left) / scale), y: Math.round((e.clientY - r.top) / scale)};
}

function sendMouse(kind, p, e, button = e.button, clicks = 1, buttons = e.buttons ?? 0) {
    send({type: 'mouse', kind, x: p.x, y: p.y, button, clicks, ...modifiers(e, buttons)});
}

// A press is passed on to the GUI at once, except a press on a click region (the page answers a click there itself)
// and a touch (which may become a long press, for the right button). Either is passed on late if it becomes a drag.
const SLOP = 6;            // CSS pixels a press may move and still be a click
const LONG_PRESS_MS = 550;
let press = null;
let lastDown = {t: 0, x: 0, y: 0, clicks: 0};

function clickCount(e) {
    const repeat = e.timeStamp - lastDown.t < 400 && Math.hypot(e.clientX - lastDown.x, e.clientY - lastDown.y) < SLOP;
    lastDown = {t: e.timeStamp, x: e.clientX, y: e.clientY, clicks: repeat ? lastDown.clicks + 1 : 1};
    return lastDown.clicks;
}

canvas.addEventListener('pointerdown', e => {
    if (!e.isPrimary || press) return;
    canvas.focus();
    try {
        // so the moves and the release come here even outside the canvas
        canvas.setPointerCapture(e.pointerId);
    } catch (err) {
        // a pointer no longer active
    }
    lastMouse = {x: e.clientX, y: e.clientY};
    const p = framePoint(e);
    const touch = e.pointerType !== 'mouse';
    // a touch has no hover, so the GUI first sees the pointer arrive where it is
    if (touch) {
        sendMouse('move', p, e, -1, 1, 0);
        hover(p);
    }
    const region = e.button === 0 ? regionAt(p) : -1;
    // with the right or middle button, a drag moves the view about a game larger than its space
    const pan = !touch && (e.button === 1 || e.button === 2) && canPan();
    if (e.button === 1) e.preventDefault();   // not the browser's own scrolling
    press = {id: e.pointerId, p, clientX: e.clientX, clientY: e.clientY, button: e.button, clicks: clickCount(e),
        region, touch, held: touch || region >= 0 || pan, pan, panning: false, x: e.clientX, y: e.clientY,
        done: false, timer: null};
    if (!press.held) {
        sendMouse('down', p, e, e.button, press.clicks);
    } else if (touch && region < 0) {
        press.timer = setTimeout(() => {
            // a long press is a right click
            press.done = true;
            sendMouse('down', p, e, 2, 1, 2);
            sendMouse('up', p, e, 2, 1, 0);
        }, LONG_PRESS_MS);
    }
});

// lets go of a press that turned out to be part of a two-finger gesture, if the GUI has seen it
function cancelPress(e) {
    if (!press) return;
    clearTimeout(press.timer);
    if (!press.held && !press.done) sendMouse('up', press.p, e, press.button, press.clicks, 0);
    press = null;
}

// a held press that turns into a drag is passed on after all
function releaseHeld(e) {
    clearTimeout(press.timer);
    press.held = false;
    sendMouse('down', press.p, e, press.button, press.clicks, 1);
}

let pendingMove = null;
canvas.addEventListener('pointermove', e => {
    if (!e.isPrimary) return;
    lastMouse = {x: e.clientX, y: e.clientY};
    if (!tooltip.hidden) placeTooltip();
    if (press && press.held && !press.done && !press.panning
        && Math.hypot(e.clientX - press.clientX, e.clientY - press.clientY) > SLOP) {
        if (press.pan) {
            press.panning = true;
            showPanning(true);
        } else {
            releaseHeld(e);
        }
    }
    if (press && press.panning) {
        panBy(e.clientX - press.x, e.clientY - press.y);
        press.x = e.clientX;
        press.y = e.clientY;
        return;
    }
    if (!press || !press.touch) hover(framePoint(e));
    // moves are sent at most once per animation frame
    if (pendingMove === null) requestAnimationFrame(() => {
        const m = pendingMove;
        pendingMove = null;
        // while a press is held back, the GUI sees no button down
        sendMouse('move', framePoint(m), m, -1, 1, press && press.held ? 0 : (m.buttons ?? 0));
    });
    pendingMove = e;
});

canvas.addEventListener('pointerup', e => {
    if (!press || e.pointerId !== press.id) return;
    const p = framePoint(e);
    const held = press;
    press = null;
    clearTimeout(held.timer);
    if (held.panning) showPanning(false);
    if (held.done || held.panning) return;
    if (!held.held) {
        sendMouse('up', p, e, held.button, held.clicks);
    } else if (held.region >= 0 && regionAt(p) === held.region) {
        chooseRegion(held.region, e.clientX, e.clientY);
    } else {
        // a tap is a click where it began
        sendMouse('down', held.p, e, held.button, held.clicks, 1);
        sendMouse('up', held.p, e, held.button, held.clicks, 0);
    }
});

canvas.addEventListener('pointercancel', e => {
    if (!press || e.pointerId !== press.id) return;
    clearTimeout(press.timer);
    if (press.panning) showPanning(false);
    if (!press.held && !press.done) sendMouse('up', framePoint(e), e, press.button, press.clicks, 0);
    press = null;
});

canvas.addEventListener('pointerenter', e => {
    if (e.pointerType === 'mouse') sendMouse('enter', framePoint(e), e, -1);
});
canvas.addEventListener('pointerleave', e => {
    if (e.pointerType !== 'mouse' || press) return;
    sendMouse('leave', framePoint(e), e, -1);
    hover(null);
});
canvas.addEventListener('contextmenu', e => e.preventDefault());
canvas.addEventListener('wheel', e => {
    e.preventDefault();
    const p = framePoint(e);
    send({type: 'wheel', x: p.x, y: p.y, deltaY: e.deltaY, ...modifiers(e)});
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
