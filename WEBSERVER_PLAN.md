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
  settings is free entry). `GET /api/opponents?game=`: the agent JSON files in the agent directory
  (`json/players/webserver`, or `agents=`; random, one-step look-ahead and MCTS at 0.1 s / 1 s / 5 s by default) and in `data/<game>/agents`.
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

### Stage 7: a nicer page, within the Swing GUIs

Improvements that need no per-game work (Tier 1) and an optional per-game hook (Tier 2). The game loop is unchanged: every
choice still reaches `HumanGUIPlayer` through the `ActionController`, and is one the forward model offered.

**Status:** implemented:

- **Reconnection.** A session outlives its WebSocket (`RelaySender`). The server gives the page a key, kept in
  `sessionStorage`, with which it reconnects (`/ws?resume=...`) after a dropped connection or a reload, and is sent the
  whole page again (full frame, actions, info, history, regions). A game waits `resumeMinutes` (10) for its page; the
  one waiting longest makes way when `maxSessions` is reached. Restart, New game and Setup send `quit`, ending the game.
  Opening the same game in a second window takes it over, and the first is told so rather than taking it back.
- **Touch** through pointer events: a tap clicks, a long press is the right button, a drag drags; two fingers zoom.
- **Turns.** When it becomes the player's turn, a "Since your last move" panel lists the other players' moves; in a
  background tab the title says so and a short sound plays (switchable, remembered).
- **Scoreboard and history.** The info panel's standard lines are read into fields (scores, current player, round,
  turn, phase) where they have their standard form; others (Rummy's hidden scores, Poker's pot) are shown as before. The
  scores are only ever what the GUI shows: `getGameScore` can reveal hidden information. History lines carry their
  player, shown by name. Both still come only from the GUI (`getString(state, perspective)` defaults to the full text,
  so a history built on the server could leak hidden moves, and Diplomacy leaves its history out on purpose).
- **Actions** are grouped by class when there are more than 8 of several kinds (single ones first), with the first nine
  numbered for the keys 1-9.
- **Click regions** (Tier 2): `AbstractGUIManager.getClickRegions()` (default none) lists the parts of the views a click
  may choose an action on, each with its offered actions; `chooseClicked` submits one through `ClickableActions`. The
  page outlines a region under the pointer and answers a click itself: one action is chosen at once, several give a
  menu. Presses on a region are not passed to the GUI unless they become drags. Implemented for Go Fish and Hearts
  (cards); Diplomacy had them too, until its map (Stage 8) replaced them.
- **Server load.** The GUI is no longer repainted twice per tick (`game.updateGUI` repainted the off-screen window as
  well as the streamer painting it); the GUI is updated only while the game is moving, after input, and every 2 s; and a
  frame is painted only when Swing has been asked to repaint something in it (`DirtyTracker`), or every 2 s. An idle
  Diplomacy session went from about 60% to 6% of a core. An extra frame is sent 15 ms after input, for hover effects.
- Checked in Chrome: Hearts (regions, keys, reconnect, reload, quit), Diplomacy (regions with menus, CPU), Dominion
  (scoreboard, history, summary, synthetic touch gestures), a stale key after a server restart; all 65 games started
  over a WebSocket client with no server errors. Not checked: a real phone or tablet.

**Zoom and rules** (added after the first review):

- **Fit and zoom.** The page sends its zoom with its size (none to fit). `GameSession.fitFrame` lays the frame out at
  the GUI's own (preferred layout) size and shows it at scale s, the zoom or the fit (enlarging up to 3x: small GUIs
  were left at their own size in a large window); the page centres it, or scrolls it when larger than the stage. (Laying
  the frame out larger, to fill the stage, left the content against the left edge: many views draw from their top left
  corner and are stretched by a BorderLayout.) A GUI whose layout asks for under 100 pixels gives no real size and is
  laid out to the space instead; ChineseCheckers' board view asked for 50x50, and now gives the size it draws at.
  Swing draws at the new scale, so text stays sharp. Images are capped at 12 megapixels (drawn less sharply beyond
  that). The frame header carries the zoom; the page stretches the image to a new zoom until the server's frame for it
  comes, keeping the point under the pointer still.
- **Controls.** − Fit + in the bar; the wheel zooms, except over the parts of the frame that take the wheel themselves
  (scroll panes that can scroll, views with wheel listeners: Colt Express, Descent, Pandemic), which `ChromeReader`
  sends as `wheelAreas`; Ctrl+wheel (and a touchpad pinch) always zooms, a sideways scroll pans. Dragging with the right
  or middle button pans when the game is larger than the stage (a right press that does not move is still the game's
  right click), as does a drag with any button or one finger on the stage around the game; on touch screens two fingers
  pinch and pan anywhere on the stage (`touch-action: none` on it), and one finger on the game is still the game's (Chess
  drags its pieces). The zoom is kept per game in `localStorage`. A Panel button hides the sidebar.
- Checked: contact sheets of the first frames of all 65 games at a 1400x800 space, drawn as the page shows them
  (centred); Ctrl+wheel zoom and pinch with synthetic events in Chrome. Not yet checked in a browser: the plain wheel
  and wheel areas, dragging to pan, and pinching on the stage around the game.
- **Rules.** A game's rules are Markdown files in `data/rules/<GameType>/`, one for each page, as the single source
  for the desktop and the page. They are a template filled in from the game's parameters (`{param}` values, dotted
  paths, and `<!-- if/elif/else/end -->` sections; `gui.RulesPages`, commonmark with GFM tables and heading anchors), so
  that the rules are those of the variant being played; a name that is not a parameter is an error, in any branch. The
  GUI adds the pages as `RulesView` tabs (`RulesView.addTabs`); `ChromeReader` takes those tabs out of the image (a tab
  strip left with one tab is replaced by that tab where the layout is a `BorderLayout`) and sends the pages, which the
  page shows in a panel beside the game, with the page's fonts and theme, wrapping to the width; links to headings
  scroll within it (on the desktop too: `RulesView` follows ids as well as `<a name>`). The 33 games' rules, built in
  Java before, were converted word for word: a throwaway checker compared each game's old rules tabs with the Markdown
  filled in from the same parameters, for the defaults and for values exercising every branch.
- Found: at its own preferred height Hearts' GUI draws its "0 points" over the player titles (a layout bug of the GUI,
  seen when zoomed in beyond the fit or in a small window).

### Stage 8 (Tier 3): easier decisions on map games

Make the page feel native to the browser where that makes deciding easier, without redrawing what the Swing GUI already
draws well. The streamed image stays, and the board is still the GUI's; the page adds, over and beside it, what helps a
player decide and enter a decision: information where the pointer is, direct ways of entering moves, and a plan of
several moves made and checked before it is sent. Every move still reaches the engine through `HumanGUIPlayer` as one
the forward model offered. Start with three games whose boards are clickable maps: **Diplomacy, Risk and Pandemic**.

**What gets in the player's way now.** A decision means scanning a long action list (Diplomacy offers every support and
convoy of the next unit), or clicking a region and choosing from a menu. The engine sets the order of the decisions
(Diplomacy asks for the units one by one in its own order; Pandemic takes each of the 4 actions as it comes), and a
choice cannot be taken back. What a player needs to judge a move (the armies next door, a continent's bonus, the cubes
on a city and what an outbreak would reach) is spread across the board, the side panels and the player's memory.

**Principles**

- **Information where you look.** Hovering over a region shows a card with what matters there; the GUI already knows
  it. Only what the player could see in the GUI: no hidden information.
- **Point at the thing, then at where it goes.** Select a unit or territory, and the places it can act on are lit up;
  click (or drag to) one. Choices that remain (a support or a move, how many armies) come in a small menu at the target,
  with a sensible default.
- **Plan, then commit**, wherever the engine would take a run of the player's own decisions with nothing random or
  hidden revealed between them. The page builds the run in any order, shows it, and lets each step be changed or undone;
  a button sends it all.
- **Show the consequence before it happens**: orders as arrows, the planned state on the board, the odds of an attack.
- **Fewer, clearer steps**: keys for the common actions, defaults that are usually right, the phase and what remains
  (armies to place, actions left) always in view.

**Shared structure**

- **Map regions.** The Tier 2 `ClickRegion` grows into a map description from the GUI manager: each region has an id, a
  name, its shape, an anchor (where an arrow or badge goes), and its card (lines of text from the GUI, as the player
  sees them). The page draws its overlays (highlights, arrows, badges, cards) in the browser, over the image.
- **Actions in map terms.** A per-game mapping from an action to the regions it involves (from, to, and the region it
  acts through, e.g. the unit supported), so that the page can offer an action by selecting and dragging, and draw it as
  an arrow. Actions that involve no region stay in the action list.
- **The planner.** A `GameSession` keeps a copy of the game from the player's view (`getCopy(player)`), advances it with
  the forward model as moves are planned, and sends the actions available next, the regions, and a frame of the GUI
  drawing the planned state (marked as a plan). Undo replays the plan less its last step on a fresh copy. The game says
  how far a plan may run (a hook: true while the next decision is still the player's and nothing random or hidden has
  been revealed). On commit, each planned action is given to `HumanGUIPlayer` when the engine asks; if one is no longer
  offered (an event card, a reaction), the plan stops there and the page shows what is left of it.

**Diplomacy**

- Orders for all units in any order, shown as arrows (move, support, convoy) and markers (hold, disband, build) on the
  map. Click a unit, then a province: one order is made at once; when there are several (move or support someone else's
  move there, a convoyed move) a menu at the province. Units without an order are marked, and Hold is their default.
- A unit's orders do not depend on the other orders, so the planner finds each unit's options on a copy where it is the
  next to order; the plan is sent in the engine's unit order. Builds and disbands work the same way.
- Warnings before sending, not errors: a support for a move nobody ordered, a convoy with no matching move, two own
  units ordered into one province.
- Province card: name, supply centre and its owner, unit, and the orders given to or involving it.

**Risk**

- **Swing first**: the map becomes shaped territories like Diplomacy's, from
  `claude_game_creator/Risk_game_board.svg` (one path per territory, ids such as `alaska`, `eastern_united_states`;
  `Risk_board.svg` is the Inkscape original it is based on), tinted by owner with the army counts on them. This gives the
  click regions and the shapes for both the desktop and the page.
- **Reinforce**: click a territory to place one army, Shift for 5 (or the batch the engine offers), with the armies left
  to place in view; the placements are a plan, sent together. Card trades are offered when they are possible and forced
  when the hand is full.
- **Attack**: drag from a territory to an enemy neighbour. The dice default to the most allowed, with the odds of
  winning the roll and of taking the territory shown; Attack once or Blitz. Rolls are random, so attacks are not planned
  ahead. After a capture, a slider over the numbers `MoveArmiesChoice` offers, defaulting to the most.
- **Fortify**: drag between connected territories, slider for the armies.
- Territory card: owner, armies, continent with its bonus and who holds the rest of it, the enemy armies next door.

**Pandemic**

- The 4 actions are a plan with the actions left in view; cards are drawn and cities infected only after them, so the
  plan is exact. Click a city to see how to get there and what it costs (drive steps, or which card a direct or charter
  flight uses, or a shuttle) and choose one; then treat, build, share or cure there from a menu or keys. Undo any step.
- City card: cubes by colour, research station, pawns there, which cards in the players' hands are that city, and
  whether it is in the infection discard. Outbreak warnings on cities with 3 cubes of a colour.
- The discard choice (hand over the limit) shows what each card would be needed for (cures, flights).

**Order of work.** The shared regions and planner with Diplomacy first (its shapes and click regions exist, and
planning all orders is the largest gain), then Risk (the SVG map in Swing, then drag and odds), then Pandemic (routes).

**Status:** implemented for all three games.

- **Map regions.** `AbstractGUIManager.getMapRegions()` (all the regions, id, name, view and shape) and
  `getMapMove(action)` (the regions an offered action involves, `from` and `to`). `ChromeReader` sends the regions
  (`map`, again only when a shape or view position changes) and adds `from`/`to` to each action. The page: click a
  piece to select it (outlined; the places it can act on dashed; the action list narrowed to it, with Show all), then
  a place: one action is chosen, several give a menu (with "Select X instead" when the place has a piece of its own).
  After a choice on the map, a next decision with nothing on the map (Risk's dice, the armies to move) is offered in a
  menu where the player clicked (`offerFollowOn`).
  Actions all on the spot (builds) are offered as soon as the piece is selected. Escape clears the selection, and a
  click that closes a menu does nothing else. The action list is grouped by piece rather than by kind. The Swing
  tooltips stay as the region cards.
- **The planner.** `gui.IMovePlanner` (the game's rules for a plan: when one starts, the options at the planned state,
  applying a step, what replaces what, the fallback for a decision with no planned action, warnings, the button's
  label) from `AbstractGUIManager.getPlanner()`. `web.MovePlan` keeps the steps on a copy of the decision the game is
  waiting for (rebuilt from the decision on every change, dropping steps no longer offered); the GUI is updated with
  the planned state and offered the plan's options (`AbstractGUIManager.offerInstead`), and the session's action
  controller diverts choices to the plan. The page shows the steps (each removable), the warnings, Clear and Send.
  `web.BrowserPlayer` (the seat's `HumanGUIPlayer`) answers the run's decisions from the sent plan on the game thread:
  the first planned action offered, else the fallback, else the plan stops (with a message) and the player is asked.
  The run is the player's turn (turn, round and current player as when it was sent).
- **Diplomacy** (`DiplomacyPlanner`): all the power's orders at once, in any order, from the first decision of the
  phase; a new order for a unit replaces its old one; unordered units hold (retreats: disband; unused builds are
  waived). Warnings: units holding for want of an order, own units ordered into one province, a support for an own
  unit's move it is not ordered to make, a hold support for an own unit ordered to move, a convoy with no matching
  move. `DiplomacyForwardModel.ordersFor(state, unit)` gives any unit's orders. The map draws the planned orders as the
  pending (purple) ones; with several units' orders offered it outlines the units only, not every province an order
  could aim at.
- Tests: `DiplomacyPlannerTest`, `web.MovePlanTest` (the plan, and the browser player carrying it out by hand, as the
  game loop would ask). Checked in Chrome: selecting, a menu, the plan and its warnings, sending (the orders reached
  the game, and Fall's plan began). Not yet checked: retreats and builds in the page, touch.
- Found: the planned orders were thin purple lines, hard to see at the fitted size; they are now drawn more strongly.
- **Risk** (Swing first, then the page):
  - The map is drawn from `data/risk/worldMap.svg` (the map file's new optional `"svg"`; a copy of
    `claude_game_creator/Risk_game_board.svg`, by CMG Lee on Wikimedia Commons, under CC BY-SA 4.0,
    whose credit, with the licence's address, the map file's `"svgCredit"` gives and the map shows in its bottom right
    corner): `RiskBoardShapes` reads the territory paths (any path command but arcs, with
    translate and scale transforms) and finds a label point well inside each. `RiskMapView` tints each territory in
    its owner's colour with its armies in a disc, outlines the continents (painted at the device's scale: Java's
    `Area` goes wrong on these many curves, leaving stripes), and dashes the connections across the sea. A map file
    without an SVG keeps the discs.
  - Desktop: a territory's tooltip (continent and bonus, how much of it the owner holds, owner and armies, the
    neighbours); clicking on the map (a click places or claims; or picks a territory to attack or fortify from, then a
    click on a target chooses, with a menu when there are several ways; a right click drops the pick).
  - Labels in words, with the odds (`RiskOdds`, exact): "Attack Alberta (1) from Ontario (25): taken >99% of the
    time by attacking until it falls"; a roll's chance of each number of armies lost; a Blitz's chance of taking it.
    `AbstractGUIManager.actionLabel` is the new hook, used by the buttons and the plan's steps.
  - The page: claim, place, attack and fortify on the map, the dice and the armies to move in the follow-on menu;
    `RiskPlanner` plans the reinforce phase (trades and placements; nothing random happens in it).
- **Pandemic**:
  - The cities are discs a little larger than the board's (`PandemicBoardView.cityAreas`, following the board's own
    zoom and pan); a move goes from the moving pawn's city to its destination, treating, building, sharing and curing
    are on the acting player's city. Selecting one's city and then another gives the ways there and what each costs
    ("Direct flight to Istanbul (discard Istanbul)"); clicking one's city again gives what can be done there.
  - `PandemicPlanner` plans the turn's actions (not Forecast, which looks at the infection deck; nothing once a hand
    is over its limit). The forward model is rule-based and keeps its place in the turn in itself, so a planned action
    is carried out as its PlayerAction rule does (with the Medic's treating, and a step of the turn), never by running
    the game's forward model on a copy. While a plan offers the actions, the GUI offers all of them (its desktop
    buttons only offer those matching what the player has picked out on the board).
  - A city's tooltip: its cubes (and which another would make an outbreak), research station, pawns, who holds its
    card, and whether it is in the infection discard pile. A card to discard says its colour, how many of it the
    player holds and how many a cure needs.
- Found and fixed: the page was not sent a decision that offered the same actions as the one before (placing armies
  again, after an opponent's quick turn), and stayed with its buttons disabled; the actions are now sent again
  whenever the game has moved on.
- Tests: `RiskPlannerTest`, `RiskMapDrawingTest` (shapes, label points, odds), `RiskMapClickTest` (the desktop map,
  headless), `PandemicPlannerTest`. Checked in Chrome: Risk placing, planning and sending reinforcements, an attack
  with its dice and move-in menus, a fortifying move; Pandemic selecting a city, the ways to another, planning and
  sending, the city tooltip. Not yet checked: Diplomacy's retreats and builds in the page, Pandemic's sharing and
  curing on the map, touch.

## Testing

- **Unit tests** (`web.SessionConfigTest`, no display needed): reading a setup from a link, defaults, refusals with their
  messages, setting parameters from strings, free-entry parameters. Run with
  `mvn test -Dmaven.test.skip=false -Dtest=SessionConfigTest`. The existing tests of the packages touched (gui, LawnAndOrder,
  Terraforming Mars, Colt Express, Love Letter, Pandemic, Poker): 141 tests including these, all passing.
- **End to end**: a Java WebSocket client standing in for the page (tile reassembly, scripted mouse, wheel, actions, log),
  Chrome for each stage's milestone, and the CI workflow for the Docker image.

## Risks

- **Shared Swing thread:** a slow `_update` in one game delays every session. Acceptable with a small session cap.
- **Layout gaps** after removing the chrome may need per-game fixes. Stage 1 still works without Stage 3, so there's a fallback.
- **Stop/interrupt behaviour:** if it doesn't end games cleanly, `Game` needs a small change.

## Decisions

- Reference game: **LawnAndOrder** (Stage 0 spike and Stage 3 layout check).
- Server library: **Javalin**.
- `AbstractGUIManager`: read-only accessors only, no web mode (see Stage 3).
