package games.toads.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IExtendedSequence;
import games.toads.ToadConstants.ToadGamePhase;
import games.toads.ToadGameState;
import games.toads.ToadParameters;
import games.toads.actions.AssaultCannonInterrupt;
import games.toads.actions.ScoutCards;
import games.toads.actions.SiegeCannonGuess;
import games.toads.components.ToadCard;
import games.tricktaking.gui.CardArt;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import gui.views.RulesView;
import players.human.ActionController;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GUI for War of the Toads: player 1 at the top, the two lanes of the Battle in the middle, player 0 at the bottom.
 */
public class ToadGUIManager extends AbstractGUIManager {

    static final int battlesPerWar = 4, actionPanelHeight = 140;

    ToadBattleView battleView;
    ToadPlayerView[] playerViews;
    String[] agentNames;

    public ToadGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        int nPlayers = gameState.getNPlayers();
        battleView = new ToadBattleView();
        playerViews = new ToadPlayerView[nPlayers];
        agentNames = new String[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            playerViews[i] = new ToadPlayerView();
            String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
            agentNames[i] = split[split.length - 1];
        }

        int gap = 12;
        this.width = ToadBattleView.width + 40;
        this.height = ToadBattleView.height + 2 * ToadPlayerView.height + 2 * gap + 40;

        parent.setBackground(ImageIO.GetInstance().getImage(CardArt.dataPath + "table-background.jpg"));

        // without these the tabbed pane's content area paints over the parent's background image
        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(false);
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        tabs.add("Game", main);
        tabs.add("Rules", new RulesView(rulesHtml((ToadParameters) gameState.getGameParameters()), height));

        // player 1 above the Battle, player 0 below it
        JPanel table = new JPanel(new GridBagLayout());
        table.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.insets = new Insets(gap / 2, 0, gap / 2, 0);
        c.gridy = 0;
        table.add(playerViews[1], c);
        c.gridy = 1;
        table.add(battleView, c);
        c.gridy = 2;
        table.add(playerViews[0], c);

        JPanel infoPanel = createGameStateInfoPanel("War of the Toads", gameState, width, defaultInfoPanelHeight);
        // up to 10 actions at once, so a vertical list, tall enough for the 5 options of the opening return
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, actionPanelHeight, true);

        main.add(infoPanel, BorderLayout.NORTH);
        main.add(table, BorderLayout.CENTER);
        main.add(actionPanel, BorderLayout.SOUTH);

        parent.setLayout(new BorderLayout());
        parent.add(tabs, BorderLayout.CENTER);
        parent.setPreferredSize(new Dimension(width, height + actionPanelHeight + defaultInfoPanelHeight + 40));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    @Override
    public int getMaxActionSpace() {
        // the most is the Defender's choice of a face-up and a hidden card from a hand of 4: 4 x 3 = 12.
        // A legacy Assault Cannon's guess offers one per card type in play (9 in the legacy decks), plus none of
        // these; a Siege Cannon guess at most 8, and returning or recycling a card at most 5.
        return 12;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof ToadGameState state)) return;
        Set<Integer> viewers = viewers(state);
        boolean gameOver = !state.isNotTerminal();
        int current = state.getCurrentPlayer();
        int war = state.getRoundCounter();

        for (int p = 0; p < playerViews.length; p++) {
            PartialObservableDeck<ToadCard> hand = state.getPlayerHand(p);
            List<ToadCard> cards = new ArrayList<>();
            List<Boolean> faceUp = new ArrayList<>();
            for (int i = 0; i < hand.getSize(); i++) {
                // a hidden card that has been played stays in the hand until it is revealed: it is shown in its lane
                if (hand.get(i) == state.getHiddenFlankCard(p))
                    continue;
                cards.add(hand.get(i));
                faceUp.add(gameOver || canSee(viewers, hand, i));
            }
            PartialObservableDeck<ToadCard> deck = state.getPlayerDeck(p);
            int bottom = deck.getSize() - 1;
            String knownBottom = bottom >= 0 && canSee(viewers, deck, bottom) ? deck.get(bottom).getComponentName() : null;
            boolean ownerSees = gameOver || viewers.contains(p);

            int hostages = state.getBattlesWon(war, p);
            String mood = hostages < state.getBattlesWon(war, 1 - p) ? "Angry" : "Calm";
            String warLine = "War " + (war + 1) + ": " + count(hostages, "Hostage") + ", "
                    + count(state.getShrineFlags(war, p), "Flag") + ", " + mood;
            if (war == 1)
                warLine += "      War 1: " + count(state.getBattlesWon(0, p), "Hostage");
            playerViews[p].update(cards, faceUp, deck.getSize(), knownBottom, state.getTieBreaker(p), ownerSees,
                    !gameOver && p == current,
                    "Player " + p + " [" + agentNames[p] + "]" + role(state, p), warLine);
        }

        updateBattleView(state, viewers, gameOver);
        parent.repaint();
    }

    private void updateBattleView(ToadGameState state, Set<Integer> viewers, boolean gameOver) {
        ToadCard[][] cards = new ToadCard[2][2];
        boolean[][] faceUp = new boolean[2][2];
        boolean lastBattle = false;
        for (int p = 0; p < 2; p++) {
            cards[p][0] = state.getFieldCard(p);
            cards[p][1] = state.getHiddenFlankCard(p);
            faceUp[p][0] = true;
            faceUp[p][1] = gameOver || viewers.contains(p);
        }
        if (cards[0][0] == null && cards[1][0] == null) {
            // no card played yet: show the last Battle of this War, whose cards are on top of the discards (the
            // hidden card was discarded last)
            for (int p = 0; p < 2; p++) {
                Deck<ToadCard> discards = state.getDiscards(p);
                if (discards.getSize() >= 2) {
                    cards[p][0] = discards.get(1);
                    cards[p][1] = discards.get(0);
                    faceUp[p][0] = faceUp[p][1] = true;
                    lastBattle = true;
                }
            }
        }

        List<String> lines = new ArrayList<>();
        int fought = state.getBattlesFought();
        if (gameOver) {
            lines.add("Game over: " + resultText(state));
            for (int war = 0; war < 2; war++)
                lines.add("War " + (war + 1) + ": Hostages " + state.getBattlesWon(war, 0) + " (Player 0) v "
                        + state.getBattlesWon(war, 1) + " (Player 1)");
        } else {
            int war = state.getRoundCounter();
            int battle = Math.min(fought - war * battlesPerWar, battlesPerWar - 1) + 1;
            lines.add("War " + (war + 1) + " of 2, Battle " + battle + " of " + battlesPerWar);
            lines.add(decisionText(state));
        }
        if (fought > 0) {
            int last = fought - 1;
            lines.add("Last Battle: Player 0 took " + state.getScoreInBattle(last, 0) + ", Player 1 took "
                    + state.getScoreInBattle(last, 1));
        }
        battleView.update(cards, faceUp, lastBattle, lines.toArray(new String[0]));
    }

    /**
     * The players whose view the screen shows: everyone under full observability, otherwise the human players and,
     * if so set, the current player.
     */
    private Set<Integer> viewers(ToadGameState state) {
        Set<Integer> viewers = new HashSet<>(humanPlayerIds);
        if (state.getCoreGameParameters().alwaysDisplayFullObservable) {
            viewers.add(0);
            viewers.add(1);
        }
        if (state.getCoreGameParameters().alwaysDisplayCurrentPlayer)
            viewers.add(state.getCurrentPlayer());
        return viewers;
    }

    private static String count(int n, String noun) {
        return n + " " + noun + (n == 1 ? "" : "s");
    }

    private static boolean canSee(Set<Integer> viewers, PartialObservableDeck<ToadCard> deck, int index) {
        return viewers.stream().anyMatch(v -> deck.isComponentVisible(index, v));
    }

    /**
     * " (Attacker)" or " (Defender)" while cards are being played, otherwise nothing.
     */
    private static String role(ToadGameState state, int player) {
        if (!state.isNotTerminal() || state.getGamePhase() != ToadGamePhase.PLAY)
            return "";
        return player == attacker(state) ? "   Attacker" : "   Defender";
    }

    private static int attacker(ToadGameState state) {
        return state.getAttacker();
    }

    private static String decisionText(ToadGameState state) {
        int current = state.getCurrentPlayer();
        return switch ((ToadGamePhase) state.getGamePhase()) {
            case OPENING_RETURN -> "Player " + current + " returns a card to the bottom of their deck";
            case DISCARD -> "Player " + current + " may recycle a card";
            case PLAY -> {
                int attacker = state.getAttacker();
                if (state.getFieldCard(attacker) == null)
                    yield "Player " + attacker + " plays a face-up card";
                List<String> choosing = new ArrayList<>();
                for (int p : state.getCurrentSimultaneousPlayers())
                    choosing.add("Player " + p + (p == attacker ? " plays a hidden card" : " plays a face-up and a hidden card"));
                yield String.join("; ", choosing);
            }
            case POST_BATTLE -> {
                IExtendedSequence sequence = state.currentActionInProgress();
                if (sequence instanceof ScoutCards)
                    yield "Player " + current + " shows 3 cards to the Scout's owner";
                if (sequence instanceof SiegeCannonGuess || sequence instanceof AssaultCannonInterrupt)
                    yield "Player " + current + " guesses a card in their opponent's hand";
                yield "Player " + current + " to decide";
            }
        };
    }

    private static String resultText(ToadGameState state) {
        CoreConstants.GameResult[] results = state.getPlayerResults();
        for (int p = 0; p < results.length; p++)
            if (results[p] == CoreConstants.GameResult.WIN_GAME)
                return "Player " + p + " wins";
        return "a draw";
    }

    // the Special Attribute and the Tactic of each Rulebook 3 card, by ability class; legacy abilities have none
    private static final Map<String, String[]> cardText = Map.of(
            "AssassinII", new String[]{"Beats a General.",
                    "Adds 2.5 to the lower of your Ally and its Foe (not in a lane with a Siege Cannon)."},
            "Scout", new String[]{"", "Adds 1 to your Ally. After the Battle your opponent will show you 3 cards "
                    + "from their hand."},
            "SaboteurIII", new String[]{"Beats a Siege Cannon.", "Your Ally breaks ties."},
            "TricksterII", new String[]{"", "Switches lanes with your Ally. It cannot be blocked."},
            "BerserkerII", new String[]{"", "Makes you Angry for this Battle."},
            "Bodyguard", new String[]{"", "Blocks the Tactic of your opponent's hidden card."},
            "GeneralHostages", new String[]{"Loses to an Assassin.",
                    "Adds 1 to your Ally for each Hostage your opponent has this War."},
            "GeneralFlags", new String[]{"Loses to an Assassin.",
                    "Adds 1 to your Ally for each of your Flags this War."},
            "SiegeCannon", new String[]{"Wins in Attack, except against a Saboteur. Loses in Defence.",
                    "After the Battle you will guess a card in your opponent's hand."});

    private static String rulesHtml(ToadParameters params) {
        int handSize = (int) params.getParameterValue("handSize");
        boolean openingReturn = (boolean) params.getParameterValue("openingReturn");
        boolean recycle = (boolean) params.getParameterValue("discardOption");
        boolean useTactics = (boolean) params.getParameterValue("useTactics");
        ToadParameters.SecondRoundStart start =
                (ToadParameters.SecondRoundStart) params.getParameterValue("secondRoundStart");
        List<ToadCard> deck = params.getCardDeck();

        StringBuilder cards = new StringBuilder("<table border=1 cellpadding=4 cellspacing=0><tr>"
                + "<th align=left>Card</th><th>Strength</th><th align=left>Special Attribute</th>"
                + "<th align=left>Tactic</th></tr>");
        Set<String> listed = new HashSet<>();
        for (ToadCard card : deck) {
            if (!listed.add(card.getComponentName())) continue;
            String ability = card.tactics == null ? "" : card.tactics.getClass().getSimpleName();
            String[] text = cardText.getOrDefault(ability, new String[]{"", ""});
            cards.append("<tr><td>").append(card.getComponentName()).append("</td><td align=center>")
                    .append(card.value).append("</td><td>").append(text[0]).append("</td><td>")
                    .append(useTactics ? text[1] : "").append("</td></tr>");
        }
        cards.append("</table>");
        String oneOfEach = listed.size() == deck.size() ? "<p>Each deck holds one of each card.</p>" : "";

        String draw = openingReturn
                ? "Each player draws " + (handSize + 1) + " cards, and then puts one of them on the bottom of their "
                + "deck."
                : "Each player draws " + handSize + " cards.";
        String secondAttacker = switch (start) {
            case ONE -> "Player 0 attacks first in War 2.";
            case TWO -> "Player 1 attacks first in War 2.";
            case LOSER -> "The loser of War 1 attacks first in War 2 (Player 1 after a Stalemate).";
            case WINNER -> "The winner of War 1 attacks first in War 2 (Player 1 after a Stalemate).";
        };
        String tactics = useTactics
                ? "<p>Only the Tactic of each hidden card acts. A card's Ally is the other card on its side, and "
                + "the Ally's Foe is the card opposite the Ally. The Tactics act in four stages, in the order "
                + "Block, Start, During and After. Within a stage both Tactics act at the same time.</p>"
                : "<p>In this game the cards' Tactics do not act.</p>";
        String afterDraw = useTactics
                ? "<li>A Scout's or a Siege Cannon's Tactic then takes effect (see <a href='#cards'>Cards</a>).</li>"
                : "";

        return "<h2>War of the Toads</h2>"
                + "<p><a href='#battle'>A Battle</a> | <a href='#lanes'>Lanes</a> | "
                + "<a href='#hostages'>Hostages and Flags</a> | <a href='#winning'>Winning</a> | "
                + "<a href='#cards'>Cards</a> | <a href='#interface'>Interface</a></p>"
                + "<p>Each of the two players has a deck of " + deck.size() + " toads, and they fight two Wars. "
                + "The aim is to capture more Hostages than your opponent.</p>"
                + "<p><b>Each War</b> starts with a new hand. " + draw + " Player 0 attacks first in War 1. The War "
                + "has " + battlesPerWar + " Battles, and the Attacker and the Defender swap roles after each "
                + "one.</p>"
                + "<h3><a name='battle'>A Battle</a></h3><ol>"
                + (recycle ? "<li>Each player may put one card from their hand on the bottom of their deck, and "
                + "then draws the top card of the deck.</li>" : "")
                + "<li>The Attacker plays a card face up.</li>"
                + "<li>At the same time, the Attacker chooses a hidden card, and the Defender chooses a face-up "
                + "card and a hidden card.</li>"
                + "<li>The hidden cards are revealed" + (useTactics ? " and their Tactics act" : "") + ".</li>"
                + "<li>Each lane is won by one card or tied (see <a href='#lanes'>Lanes</a>).</li>"
                + "<li>Each player draws 2 cards, or the rest of their deck if it has fewer.</li>"
                + afterDraw + "</ol>"
                + tactics
                + "<h3><a name='lanes'>Lanes</a></h3>"
                + "<p>The face-up cards fight in the Face-up lane, and the hidden cards in the Hidden lane. The "
                + "higher Strength wins the lane. A card's Special Attribute applies in either lane, and takes "
                + "the place of the Strength comparison. A tied lane stays tied unless exactly one of its cards "
                + "breaks ties.</p>"
                + "<h3><a name='hostages'>Hostages and Flags</a></h3><ul>"
                + "<li>The winner of a lane captures the losing card as a Hostage.</li>"
                + "<li>In a tied lane both cards go to the Shrine, and each player gains a Flag.</li>"
                + "<li>A player who wins both lanes while Calm keeps one Hostage. The other goes to the Shrine, "
                + "and each player gains a Flag.</li>"
                + "<li>A player who wins both lanes while Angry keeps both Hostages.</li></ul>"
                + "<p>You are Angry when you have fewer Hostages than your opponent in this War, or when your "
                + "Berserker makes you Angry. Otherwise you are Calm.</p>"
                + "<p><b>Between the Wars</b> the card left in each hand becomes that player's Casualty. Each "
                + "player's Casualty gives them one Flag at the start of War 2. The cards each player played in "
                + "War 1 are shuffled to form their opponent's deck for War 2. " + secondAttacker + "</p>"
                + "<h3><a name='winning'>Winning</a></h3>"
                + "<p>The player with more Hostages wins a War. A War with equal Hostages is a Stalemate. The "
                + "winner of War 2 wins the game. If War 2 is a Stalemate, the winner of War 1 wins. If both Wars "
                + "are Stalemates, the lower Casualty wins (the Siege Cannon is the lowest). Equal Casualties "
                + "draw.</p>"
                + "<h3><a name='cards'>Cards</a></h3>" + oneOfEach + cards
                + "<p>After a Scout's Battle, an opponent holding 4 cards chooses one of them to keep hidden. A "
                + "Siege Cannon's owner names a card type, and sees one card of that type if the opponent holds "
                + "it. The owner cannot name their own Casualty, or a card the opponent has already played in "
                + "this War.</p>"
                + "<h3><a name='interface'>Interface</a></h3>"
                + "<p>Player 1's area is at the top and Player 0's at the bottom. Each area shows the player's "
                + "role (Attacker or Defender), their Hostages, Flags and mood in this War, their Hand, their "
                + "Deck with its size, and their Casualty. The bottom card of a deck is named on it when you "
                + "know it. The player to act has a blue outline.</p>"
                + "<p>The middle panel shows the Face-up lane and the Hidden lane, with Player 1's card on the "
                + "left of each. The last Battle's cards stay there, dimmed, until the next Battle starts. The "
                + "text beside the lanes gives the War and Battle number, who is to act, and the Hostages each "
                + "player took in the last Battle.</p>"
                + "<p>A card shows its Strength in the circle, its Special Attribute in italics and its Tactic "
                + "in the box at the bottom. Cards shown to you by a Scout or a Siege Cannon are face up in your "
                + "opponent's hand.</p>"
                + "<p>Choose from the action list at the bottom of the screen. The action buttons call the "
                + "Face-up lane the field, and the Hidden lane the flank.</p>";
    }
}
