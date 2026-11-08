// The start page sets up a game (players, opponents, seed and game parameters) and opens the play page with the setup
// in its query string (read by web.SessionConfig).
'use strict';

const $ = id => document.getElementById(id);
const STORE_KEY = 'tag-setup';

let games = [];
const opponentsByGame = {};

function displayName(name) {
    return name.replace(/([a-z])([A-Z])/g, '$1 $2').replace(/([A-Z])([A-Z][a-z])/g, '$1 $2');
}

function option(value, label, selected) {
    const o = document.createElement('option');
    o.value = value;
    o.textContent = label;
    if (selected) o.selected = true;
    return o;
}

// The last setup for each game, so the page opens as it was left (per browser only).
function loadSaved() {
    try {
        return JSON.parse(localStorage.getItem(STORE_KEY)) || {};
    } catch {
        return {};
    }
}

function save(gameName, setup) {
    try {
        const all = loadSaved();
        all[gameName] = setup;
        all.lastGame = gameName;
        localStorage.setItem(STORE_KEY, JSON.stringify(all));
    } catch {
        // Storage is unavailable. The page still works, but will not remember the setup.
    }
}

function currentGame() {
    return games.find(g => g.name === $('game').value);
}

async function opponentsFor(game) {
    if (!opponentsByGame[game.name]) {
        const r = await fetch(`api/opponents?game=${encodeURIComponent(game.name)}`);
        opponentsByGame[game.name] = await r.json();
    }
    return opponentsByGame[game.name];
}

async function selectGame(saved) {
    const game = currentGame();
    if (games.length === 1) $('heading').textContent = displayName(game.name);
    saved = saved || loadSaved()[game.name] || {};

    const players = $('players');
    players.replaceChildren();
    const defaultPlayers = saved.players ?? Math.max(game.minPlayers, Math.min(3, game.maxPlayers));
    for (let n = game.minPlayers; n <= game.maxPlayers; n++)
        players.append(option(n, n, n === defaultPlayers));

    renderSeats(saved.seat ?? 0);
    await renderOpponents(saved.opponents || []);
    renderParams(saved.params || {});
    $('seed').value = saved.seed ?? '';
    $('insight').checked = !!saved.insight;
    if (saved.pause !== undefined) $('pause').value = saved.pause;
}

function renderSeats(selected) {
    const n = +$('players').value;
    const seat = $('seat');
    seat.replaceChildren();
    for (let i = 0; i < n; i++)
        seat.append(option(i, `Player ${i}${i === 0 ? ' (first)' : ''}`, i === Math.min(selected, n - 1)));
}

async function renderOpponents(previous) {
    const game = currentGame();
    const choices = await opponentsFor(game);
    const n = +$('players').value, seat = +$('seat').value;
    const container = $('opponents');
    // keep what was chosen for each seat when the player count or seat changes
    const kept = previous.length ? previous : [...container.querySelectorAll('select')].reduce((acc, s) => {
        acc[+s.dataset.seat] = s.value;
        return acc;
    }, []);
    container.replaceChildren();
    for (let i = 0; i < n; i++) {
        if (i === seat) continue;
        const label = document.createElement('label');
        const span = document.createElement('span');
        span.textContent = `Player ${i}`;
        const select = document.createElement('select');
        select.dataset.seat = i;
        const fallback = (choices.find(c => c.default) || choices[0] || {}).id;
        const wanted = kept[i] && kept[i] !== 'you' && choices.some(c => c.id === kept[i]) ? kept[i] : fallback;
        for (const c of choices) {
            const o = option(c.id, c.description ? `${c.id} - ${c.description}` : c.id, c.id === wanted);
            if (c.description) o.title = c.description;
            select.append(o);
        }
        label.append(span, select);
        container.append(label);
    }
}

function renderParams(values) {
    const game = currentGame();
    const container = $('params');
    container.replaceChildren();
    $('params-card').hidden = game.params.length === 0;
    for (const p of game.params) {
        const label = document.createElement('label');
        const span = document.createElement('span');
        span.textContent = p.name;
        let input;
        const choices = p.type === 'boolean' && p.values.length === 0 ? ['true', 'false'] : p.values;
        if (choices.length) {
            input = document.createElement('select');
            for (const v of choices) input.append(option(v, v === p.default ? `${v} (default)` : v, false));
        } else {
            input = document.createElement('input');
            input.inputMode = p.type === 'int' || p.type === 'double' ? 'decimal' : 'text';
        }
        input.dataset.param = p.name;
        input.dataset.default = p.default;
        input.value = values[p.name] ?? p.default;
        input.addEventListener('input', updateParamSummary);
        input.addEventListener('change', updateParamSummary);
        label.append(span, input);
        container.append(label);
    }
    updateParamSummary();
}

function changedParams() {
    const changed = {};
    for (const input of $('params').querySelectorAll('[data-param]'))
        if (input.value.trim() !== input.dataset.default) changed[input.dataset.param] = input.value.trim();
    return changed;
}

function updateParamSummary() {
    for (const input of $('params').querySelectorAll('[data-param]'))
        input.closest('label').classList.toggle('changed', input.value.trim() !== input.dataset.default);
    const n = Object.keys(changedParams()).length;
    $('params-summary').textContent = n === 0 ? 'All at their defaults.' : `${n} changed from the defaults.`;
}

function showError(text) {
    $('error').textContent = text;
    $('error').hidden = !text;
}

function start(e) {
    e.preventDefault();
    const game = currentGame();
    const seed = $('seed').value.trim();
    if (seed && !/^-?\d+$/.test(seed)) {
        showError('The seed must be a whole number, or blank.');
        return;
    }
    const n = +$('players').value, seat = +$('seat').value;
    const opponents = [];
    for (let i = 0; i < n; i++)
        opponents.push(i === seat ? 'you' : $('opponents').querySelector(`[data-seat="${i}"]`).value);
    const params = changedParams();

    const query = new URLSearchParams({game: game.name, players: n, seat, opponents: opponents.join(','), pause: $('pause').value});
    if (seed) query.set('seed', seed);
    if ($('insight').checked) query.set('insight', '1');
    for (const [name, value] of Object.entries(params)) query.set(`p.${name}`, value);

    save(game.name, {players: n, seat, opponents, seed, pause: $('pause').value, params, insight: $('insight').checked});
    location.href = `play.html?${query}`;
}

async function init() {
    try {
        games = await (await fetch('api/games')).json();
    } catch {
        showError('Could not reach the server.');
        return;
    }
    const gameSelect = $('game');
    const saved = loadSaved();
    for (const g of games) gameSelect.append(option(g.name, displayName(g.name), g.name === saved.lastGame));
    $('game-field').hidden = games.length === 1;

    gameSelect.addEventListener('change', () => selectGame());
    $('players').addEventListener('change', () => {
        renderSeats(+$('seat').value);
        renderOpponents([]);
    });
    $('seat').addEventListener('change', () => renderOpponents([]));
    $('params-reset').addEventListener('click', () => renderParams({}));
    $('setup').addEventListener('submit', start);
    await selectGame();
}

init();
