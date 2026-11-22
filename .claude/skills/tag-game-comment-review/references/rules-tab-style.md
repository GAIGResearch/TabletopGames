# Rules tab - style guide

The Rules tab is read by someone playing the game in the GUI, in the middle of a game, looking for one rule. It is a
reference card, not the rulebook. Add to this guide whenever playing a game shows a new problem.

## Content
- **Succinct.** What a player needs to know to make a decision, and nothing else: no designer history, no
  strategy, no examples unless a rule cannot be understood without one. One idea per sentence; prefer a list to a
  paragraph when there are steps or cases.
- **The rules as implemented**: filled in from the parameters (see "Pattern"), and the house rules or variants this
  implementation chose - not another source's rules.
- **Numbers in tables or lists**, not prose ("2 cards 1, 3 cards 2, 4 cards 4..." reads better as a small table).
- **Use the game's own words**, matching the action buttons and the labels on screen (framework notes,
  "Comments": name things as the code does).
- **An Interface section**: what each area of the screen shows, what symbols and highlights mean, and how the
  action buttons map to the rules.

## Writing
The rules text is held to the same sentence-level rules as comments (`tag-game-comment-review`, section 1). Those that
bite in rules text:
- **Elliptical or compressed sentences** (rule 12). Write whole sentences, in the order things happen. Avoid "X: Y"
  joining two different things, noun-phrase shorthand, and a fronted "for each X,".
- **Say what happens, not what is absent** (rule 6): "players with the same total share the place", not "there is
  no tie-break".
- **Only what is special** (rule 13): "Ace 1, court cards 10", not "Ace 1, number cards their number, court cards 10".
- **The exact rule when it is short** (rule 14), rather than a summary of it.
- **Future tense for what happens later** (rule 15): "the crib will be scored by the dealer".
- **No self-justifying clauses** (rule 5), and **no synonyms** for what the code and the screen name (rule 8).
- Plain sentences, no changelog tone ("now", "new"), no first person.

And from the house style for talks (`Google Drive/Academia/Presentations/.claude/skills/slide-review/rules.md`,
rule ids in brackets):
- **Plain sentences** (D/4). Split at an em-dash and cut the aside; no stacked qualifiers, no clever compression.
  Prefer a parenthesis to a trailing comma-clause (R/28).
- **No "N noun, clause" pattern** (U/23): "Three tracks, each scored separately" -> "Each of the three tracks is
  scored separately".
- **Neutral tone** (C/3): no "simply", "obviously", "never X", nothing that blames the reader.
- **No commentary about the page** (B/2): no "as you can see", "note that", "it is important to".
- **One term per concept** (M/13), and the term the player sees on screen, not the one the code or the source uses.
- **Nothing named before it is explained** (F/6), and every symbol on screen defined (S/20): "★ = target reached".
- **Word choice** (J): no transactional metaphors ("buys"); not "carry", "pin" or "land" as verbs.
- **No bold on a single word inside a sentence** (E/5); bold is for the key term that starts a paragraph.
- **A table or prose for the same numbers, never both** (K/22).
- **Centred text for short lines only** (G/19): the title. Everything else is left-aligned.

Examples of rule/comment changes (before and after) are in `Comment Examples.txt` in this directory.

## Layout
- **Never make the reader scroll right.** Scrolling down is fine. `RulesView` (below) wraps every line to its column
  and has no horizontal scroll bar; the web page wraps to its panel. Do not put rules in a `JLabel`, which does not
  wrap.
- **Keep the text column readable: about 70-90 characters a line** (a pane 600-700 pixels wide at the default font).
  A wider window should not produce wider lines; centre the column instead.
- **One Rules tab is the norm.** A card game of the size implemented so far (Whist, Cribbage, Lawn and Order...) fits
  on one page when written succinctly; a long page is fixed by cutting, not by splitting.
- **Several tabs are fine for a complex game**, where they are a good way of breaking up the rules - many card
  types, several phases each with its own rules, a large scoring reference: e.g. `Rules` (overview, turn sequence,
  winning), `Cards` / `Scoring` (reference tables), `Interface`. Each tab should make sense on its own: a link can
  only go to a heading on the same page.
- **Link within a page** when it has more than a few sections: a contents line at the top with links to its
  headings, e.g. `[Scoring](#scoring)` to `### Scoring` (a heading's id is its text in lower case, spaces made
  hyphens, punctuation dropped).
- `##` for the page title and `###` for sections, `**bold**` for the key term at the start of a paragraph, `-` or
  `1.` lists for steps and cases, pipe tables for numbers. Keep to plain Markdown: the desktop shows it through
  Swing's HTML 3.2, which renders these and little more (no images by URL, no HTML of your own).

## Pattern
The rules are Markdown files, the single source for the desktop GUI and the web server's page:
`data/rules/<GameType>/1-rules.md`, then `2-how-to-play.md` or `2-cards.md` and so on, one file per tab in the order
of their names. The tab title comes from the file name ("1-rules.md" is "Rules"), or from a first line
`<!-- title: How to Play -->` when the file name will not give it.

The text is a template filled in from the game's parameters (`gui.RulesPages`; see its class Javadoc), so that the
rules state the variant being played. Use the parameters object's field names:
- `{handSize}` is a parameter's value; a dotted path follows values (`{map.victoryCentres}`, `{board.goSalary}`),
  and a no-argument method may be named without its `get` (`{map}` for `getMap()`).
- Braces may hold arithmetic: `{handSize + 1}`, `{78 - 3 * handSize}`, `{handSize / 2 + 1}` (whole numbers divide
  to a whole number), and calls with numbers: `{tradeValue(3)}`.
- A choice of text goes in `<!-- if nDeals == 1 -->...<!-- elif nDeals < 5 -->...<!-- else -->...<!-- end -->`,
  on its own lines or inside a sentence. Conditions compare a parameter with a number, an enum constant or a quoted
  string (`== != < <= > >=`), or test it alone (true, non-zero, not empty) or with `not`, joined by `and` / `or`.
- `\{` is a literal brace.
A name that is not a parameter is an error, in any branch, so a mistake shows the first time the rules are loaded.
Write a value computed from a constant (not a parameter) out in full.

The GUI adds every page as a `gui.views.RulesView` tab, after its game tab:
```java
RulesView.addTabs(tabs, GameType.Whist, gameState.getGameParameters(), tableHeight);
```
`RulesView` shows the page in a column `RulesView.COLUMN_WIDTH` (660) pixels wide that wraps to the column, opens at
the top and follows links to headings. Add the tabs in one place only: adding them again adds a second set.

## Check
- `mvn -q -o test "-Dmaven.test.skip=false" -Dtest=RulesPagesTest` fills in every game's rules from its default
  parameters, and fails on a name that is not a parameter.
- Fill the rules in for the non-default value of every parameter they read, so that each branch is seen:
  `RulesPages.load(GameType.<Game>, params)` after changing `params`, and read the HTML.
- Render the Rules tab (or tabs) offscreen along with the game (gui-stage.md, step 5) and look at it: no horizontal
  scroll bar, no line wider than the pane, the start of the text visible at the top, and every link going somewhere.
  To see the whole page rather than the top of the scroll pane, find the `JEditorPane` inside each `RulesView`, set
  its width to the column's, and print it at its preferred height. Do this on the event thread
  (`SwingUtilities.invokeAndWait`): laid out from `main`, Swing's HTML views race the event thread and throw an
  `ArrayIndexOutOfBoundsException` from `BoxView`.
