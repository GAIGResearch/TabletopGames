# Web play: implementation plan (branch `webserver`)

Temporary planning document. Remove once the feature is complete and documented in the code.

**Goal:** a client game developer opens a link in a browser and plays their game against TAG AI agents. The game and the AIs run
server-side at full JVM speed. The existing Swing GUI is rendered on the server and streamed to the browser, with the generic parts
(actions, status, history) redrawn natively in HTML. Lobbies and multiple humans are out of scope for now, but nothing should prevent
them later.

## Findings from the code

- **The remote human is just `HumanGUIPlayer`.** `GameType.createGUIManager` identifies human seats by `instanceof HumanGUIPlayer` and
  builds the GUI from that seat's perspective, so hidden information is already handled. The browser only has to feed the same
  `ActionController`.
- **Click handling is already validated.** Board clicks go through `ClickableActions.submit`, which only accepts actions the forward
  model offered. The same applies to anything that arrives from the browser.
- **The generic chrome is highly standardised.** 60 of the 64 GUI managers use `createActionPanel`, and 57 use
  `createGameStateInfoPanel` (status, scores, history). One change in `AbstractGUIManager` covers almost every game.
- **`Game.runOne` is the template** (`Game.java:140-155`): JFrame, `GamePanel`, GUI manager, and a Swing `Timer` calling `updateGUI`
  every `frameSleepMS`. Sessions use their own copy of this, with a frame that is never shown on a desktop.
- **Problems to deal with:**
  - **Modal dialogs.** ColtExpress, LoveLetter, Pandemic, Poker and TM call `JOptionPane`. A modal dialog would block the single Swing
    thread for every session and would never appear in the browser.
  - **Screen-size assumptions.** `TMGUI` sizes itself from `getScreenSize()`, so Xvfb's screen size matters.
  - **Input beyond clicks.** Some views use drag, wheel or key events (Chess, Descent, ColtExpress, Pandemic and others), so all of
    those must be forwarded.

## How it fits together

```
Browser ──HTTP──▶ Javalin: start page, REST (games, opponents, sessions)
   ▲ │
   │ └─WebSocket─▶ GameSession
   │                ├─ Game (own thread; AI seats + HumanGUIPlayer(ac))
   │                ├─ JFrame + GamePanel + GUIManager (on Xvfb, never on a desktop)
   │                ├─ FrameStreamer: paints rootPane → image, sends it only if changed
   │                ├─ InputForwarder: browser events → AWT events posted to the frame
   │                └─ ChromeReader: reads actions / status / history after each update → JSON
```

Key design choices:

- **Input:** synthetic `MouseEvent`s are posted to the heavyweight JFrame, and Swing routes them to the right component, including
  enter/exit and hover. Not `java.awt.Robot`: Robot clicks real screen positions, so concurrent sessions sharing one Xvfb display would
  hit each other's windows.
- **Tooltips:** Swing's own tooltips are disabled. The server reads `getToolTipText(event)` from the component under the mouse and the
  browser shows it as an HTML overlay. (Stage 0: Swing moves tooltips to fit a real screen, so for an off-screen frame they become
  separate windows the capture can't see; and frames of concurrent sessions would overlap on one Xvfb display.)
- **Popups** (`JPopupMenu`): forced lightweight, so they are drawn inside the frame's layered pane and captured, as long as they fit
  inside the frame.
- **Crispness:** the frame is sized to the browser viewport and rescaled when the window resizes. Each capture is painted scaled by
  `devicePixelRatio`.
- **HTML action list:** the hidden Swing `ActionButton`s still exist. The browser's button *i* triggers `actionButtons[i].doClick()`,
  and hovering it sends enter/exit events to the same button, so per-game hover highlights and `onActionSelected` hooks keep working.

### Protocol (WebSocket, per session)

- Server → client
  - `frame` header (seq, width, height, dpr, list of changed tiles), followed by one binary PNG message per changed tile
  - `tooltip` (text or none, position)
  - `actions` (decision seq, player, `[{index, label}]`)
  - `status`, `history` (appended lines), `toast`, `thinking` (player), `gameOver` (results)
- Client → server
  - `mouse` (kind, x, y, button, modifiers, clickCount), `wheel`, `key`
  - `action` (decision seq, index); stale sequence numbers are rejected
  - `actionHover` (index, enter/exit)
  - `resize` (width, height, dpr)
  - `restart` (same seed or new)

## Stages

### Stage 0: de-risking spike (~½ day, decision gate)

**Done (Windows).** A throwaway spike ran LawnAndOrder (human seat 0 vs two `RandomPlayer`s) in an undecorated, non-focusable frame
at (-5000, 0), with the GUI updated by a Swing `Timer` as in `Game.runOne`. Results:

- **Synthetic input: pass.** `MouseEvent`s posted to the frame through the system event queue are routed to the right lightweight
  component. 272 consecutive moves were made by clicking `ActionButton`s, and clicking the Rules tab switched tabs.
- **Capture: pass.** `frame.getRootPane().paint(g)` into a `BufferedImage` scaled by 2 gives a sharp 2560×2000 image identical to the
  desktop GUI.
- **Clean stop: pass.** `game.setStopped(true)` plus interrupting the game thread, while it waits on the human, ends `game.run()` with
  no exceptions.
- **Tooltips: Swing's own don't work off-screen** (see Key design choices). Reading the tooltip text from the component under the
  mouse works anywhere, so tooltips become HTML overlays. Also needed: post `MOUSE_ENTERED` to the frame before the first move event,
  or Swing's hover tracking (enter/exit) doesn't start.
- **Encoding:** a full 2× PNG via `ImageIO` is about 1 MB and 200 ms. Per move, typically 40–70 of 320 tiles (128 px) change, which is
  250–330 KB as PNG tiles: lossless and acceptable. JPEG blurs text; "fast" PNG is about 15 MB. Stage 1 sends changed tiles only,
  compares raster arrays directly (the spike's `getRGB` comparison was slow), and encodes tiles in parallel.
- **Alpine/Xvfb: not tested.** The development machine can't run a VM, so Docker isn't available. To verify on a Linux host or in
  CI before Stage 5; fallback is the Ubuntu Temurin base image.

### Stage 1: end-to-end play, whole frame streamed

- Javalin in `pom.xml`, a new `web` package, and a `WebServer` entry in `core.TAG`.
- One session with a hard-coded game and opponents. The page is a `<canvas>`, and the entire Swing frame is streamed, Swing buttons
  included.
- Mouse (press/release/move/drag), wheel and key forwarding; resize and DPR handling.
- Changed 128 px tiles as PNG binary WebSocket messages, at up to about 10 fps; a full frame on connect and on resize.
- Tooltips as HTML overlays (Swing's `ToolTipManager` disabled).

**Milestone:** play LawnAndOrder against MCTS at `localhost:8080`.

**Status:** implemented (`web` package, `src/main/resources/web`, `TAG WebServer`, Javalin 6.7.0 in `pom.xml`). Checked with a Java
WebSocket test client standing in for `app.js` (it reassembles tiles and sends scripted mouse/wheel events): the first frame is
434 KB at 1×; action-button clicks make moves and the MCTS opponents reply; clicking the Rules tab switches tabs; the wheel scrolls
the rules; about 80 moves over 7 rounds with no exceptions, about 200 KB per move; closing the socket stops the session. Then checked
in Chrome (1920×855, DPR 1): the page renders, the GUI lays out to the viewport, a card play and the opponents' replies show, tab
switching and wheel scrolling work, no console errors. Not yet checked: a DPR 2 display in a browser (the 2× rendering itself was
checked in Stage 0), tooltips end to end, keyboard input.

### Stage 2: sessions and the start page

**Status:** implemented. As built (it differs from the first plan in a few places):

- **The setup lives in the play page's URL**, not in a `POST /api/sessions`: `play.html?game=…&players=…&seat=…&opponents=you,mcts-1000,…
  &seed=…&pause=…&p.<param>=…`, repeated on the WebSocket URL and parsed by `SessionConfig.fromQuery`. So a setup can be bookmarked
  or shared, and Restart (same seed) and New game (seed removed) are just links; no server-side restart logic.
- `GET /api/games`: the offered games with their parameter schema from `TunableParameters` (a parameter declared without a list of
  settings is free entry). `GET /api/opponents?game=`: Random, one-step look-ahead, `MCTSPlayer` with a 0.1 s / 1 s / 5 s time budget
  per decision, plus agent JSON files from `agents=` and `data/<game>/agents`.
- Start page (`index.html`, `setup.js`): game, players, seat, an opponent per seat, seed, pause after AI moves, and the parameter form
  (changed values flagged; only changes go in the URL). The last setup per game is remembered in `localStorage`.
- Play page (`play.html`, `play.js`): a bar with the game, a status pill ("Your turn" / "Player 1 (…) is thinking"), the seed, and
  Restart / New game / Setup; a results dialog at game over.
- Server messages added: `started` (seed, players), `turn`, `gameOver` (position, score, result per player).
- Server arguments: `port`, `games` (comma list or `all`: every game with a GUI, by name, less GameTemplate), `agents`,
  `maxSessions` (3), `idleMinutes` (30), `showFrames`.

Checked in Chrome: the start page for all 65 games and for LawnAndOrder (4 players, changed hand size, mixed opponents, seed 42)
starts the game as set up; Restart deals the same hands; a 2-player game with `maxRounds=1` ends with the results dialog. Checked with
the test client: errors for an unknown opponent, game or bad parameter value; "server busy" beyond `maxSessions`; an idle session is
ended with a message after `idleMinutes`.

### Stage 3: generic chrome in HTML

`AbstractGUIManager` does not change behaviour and gets no web mode. Reasons: 17 games override `updateActionButtons` and 7 override
`updateGameStateInfo`, so publishing from inside those methods would be wrong for those games; and a global mode flag would affect
every GUI in the JVM. Instead the web session, which already drives the update timer, reads what the GUI produced after each
`gui.update(...)`:

- Add read-only accessors to `AbstractGUIManager`:
  - `getActionPanel()` / `getInfoPanel()`: references to the components `createActionPanel` / `createGameStateInfoPanel` return;
  - a snapshot of the visible `ActionButton`s (index, label), whatever the subclass decided;
  - a snapshot of the status label texts and the `history` list.
- The web layer hides those panels with `setVisible(false)` (no space taken in `BorderLayout`) and renders the action list, status,
  scores and history natively. HTML button *i* triggers `actionButtons[i].doClick()`; hover sends enter/exit to the same button.
- Games that don't use the standard panels keep their Swing chrome in the streamed image.
- `GUIMessages.show(parent, text)` replaces the 5 `JOptionPane` call sites (they would block the shared Swing thread). A field on the
  helper, set only by the web server, chooses dialog (desktop) or browser toast.
- Layout check: LawnAndOrder first (its GUI puts both panels in a `BorderLayout`, and its Rules tab tests ordinary Swing widgets),
  then a representative set.

**Status:** implemented as above, plus:

- **Scaling to fit.** Many GUIs are laid out at a fixed size, larger than the browser's space once the sidebar takes its
  320 px. The frame is laid out at the larger of the space and the GUI layout's own preferred size (the layout manager's, not the
  size the GUI set on its panel, which counts the hidden panels), keeping the space's proportions; the browser scales the image
  down to fit and divides mouse positions by the same factor. Re-checked after every update, as GUIs grow during a game.
- `GUIMessages` routes a message to its session by the frame the GUI is in; the page shows it as a toast.
- Checked: all 65 games start and render at a 1600×807 space with Random opponents (contact sheets of the snapshots);
  LawnAndOrder in Chrome with actions chosen from the sidebar, info and history; Colt Express's round message as a toast.
  Games that build their own info panel (Chess, Catan, Colt Express, Dots and Boxes, ...) keep it in the image, as designed.
- Found: Pickomino's GUI is an unimplemented template (draws nothing); Terraforming Mars failed on its first update when a human
  moves first (read the last history entry of an empty history), a desktop bug too; fixed in `TMGUI`.

### Stage 4: appearance

- FlatLaf for the remaining Swing widgets, plus antialiasing and text rendering hints.
- A styled page: responsive layout with the board on the left or top and a sidebar for actions and history; dark/light themes;
  loading/thinking state.
- Optional: tooltips as HTML overlays rather than captured pixels.

**Status:** implemented:

- FlatLaf 3.5.4 (light) for the GUIs' widgets, the default (`lookAndFeel=flat`; `default` for Swing's own). Compared with the
  default look on LawnAndOrder, Catan, Dominion, Chess, Hanabi, Poker, Diplomacy, Pandemic and Terraforming Mars: no broken layouts;
  tabs, tables and fonts are cleaner. Grey-scale text antialiasing is switched on (it matters on Linux).
- The image is drawn at the browser's device pixel ratio times the scale the browser shows the frame at, so a scaled-down GUI
  costs no more pixels than the space it is shown in.
- Tooltips checked end to end (Love Letter's card descriptions, Pandemic's cities and roles, as HTML overlays); empty tooltips
  ("" or "<html></html>") are no tooltip.
- The info lines leave out the list of player results; the page shows results itself. Favicon; a spinner while the game starts.

### Stage 5: Docker and sharing

- `Dockerfile`: `fontconfig ttf-dejavu xvfb-run`, a fixed Xvfb screen size, `EXPOSE 8080`, and the entrypoint wrapped in `xvfb-run`.
  Existing entry points keep working.
- Token check on all routes; README notes on `cloudflared tunnel` and Cloudflare Access, and on `--cpus` / `-Xmx`.
- Run as a single instance that never scales to zero, because sessions live in memory.
- Faster redeploys: put dependencies and classes in separate Docker layers, and possibly a Maven profile for a slim web jar (no
  Spark/Hadoop/langchain4j).

**Status:** implemented, except that the image itself is untested here (no Docker on this machine):

- `token=`: `AccessToken`; the link `/?token=...` sets an HttpOnly cookie and redirects without the token; every page, file,
  API call and WebSocket then needs it (401 page otherwise). Checked with curl and in Chrome.
- `Dockerfile`: **Ubuntu** Temurin 21 JRE rather than Alpine (glibc and standard X11/font packages are the lower-risk choice
  while it cannot be tested), with `xvfb`, `xauth`, `tini`, fontconfig/DejaVu and the X11 client libraries; every entry point
  runs as `tini -- xvfb-run ... java -cp tag-classes.jar:lib/* core.TAG`. The build skips the fat jars; dependencies are a
  layer of their own, so a code change pushes only the classes jar (a few MB) and anything in `data/` that changed.
  The image no longer contains `/tag/TAG.jar`.
- `.github/workflows/web-docker-smoke.yml`: builds the image, checks another entry point runs, starts `WebServer` with a token,
  checks pages need it, plays a move over the WebSocket at DPR 2 (Python) and fails on any exception in the server log. Its
  Python check was run locally against the server; the workflow itself runs when pushed.
- README section "Playing in a browser"; AGENTS.md notes on `WebServer` and `GUIMessages`.

### Stage 6: developer features

**Status:** implemented, except replay:

- **Game log**: the Log button asks the server (`{type: "log"}` over the WebSocket) for a JSON record (game, seed, start
  time, each seat's agent id and name, the changed parameters and every parameter's value, the actions so far described from the
  browser player's perspective, status and results), which the page saves as a file with the page's link added.
- **AI insight**: `insight=1` in the setup (a checkbox on the start page, with a warning) adds an `AIInsight` game listener,
  which sends each AI decision's `getDecisionStats()` (top 8 actions by visits: share of the search, value to that player, which
  was chosen). The sidebar keeps the latest decision of each AI player. Off by default, as it reveals the AI's knowledge,
  including (in simultaneous-move games) the move it is about to reveal.
- **Replay: not done.** A faithful replay needs the AI players to make the same decisions again, but the MCTS opponents seed
  their randomness from the clock and search for a fixed time, so a game re-run from its seed and the browser player's moves
  diverges. Doing it properly means seeding agents from the game seed and offering iteration (not time) budgets, or recording
  and replaying every player's actions (with hidden information, the log only describes them as the browser player saw them).

## Testing

- **Unit tests:** protocol and session logic (action validation, stale action rejection, lifecycle and stop). They run with
  `-Dmaven.test.skip=false` and need a display: fine on Windows; under Xvfb in CI.
- **End-to-end:** drive the real page with browser automation for each milestone.

## Risks

- **Shared Swing thread:** a slow `_update` in one game delays every session. Acceptable with a small session cap.
- **Layout gaps** after removing the chrome may need per-game fixes. Stage 1 still works without Stage 3, so there's a fallback.
- **Stop/interrupt behaviour:** if it doesn't end games cleanly, `Game` needs a small change.

## Decisions

- Reference game: **LawnAndOrder** (Stage 0 spike and Stage 3 layout check).
- Server library: **Javalin**.
- `AbstractGUIManager`: read-only accessors only, no web mode (see Stage 3).
