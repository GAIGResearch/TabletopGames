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
import games.toads.actions.AssaultCannonInterrupt;
import games.toads.actions.ScoutCards;
import games.toads.actions.SiegeCannonGuess;
import games.toads.components.ToadCard;
import games.tricktaking.gui.CardArt;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        tabs.add("Rules", createRulesPanel());

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
        // the most is a legacy Assault Cannon's guess: one per card type in play (9 in the legacy decks), plus
        // none of these. A Siege Cannon guess offers at most 8, returning or recycling a card at most 5.
        return 10;
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
        // the Attacker plays both cards first
        int current = state.getCurrentPlayer();
        return state.getFieldCard(1 - current) != null && state.getHiddenFlankCard(1 - current) != null ? 1 - current : current;
    }

    private static String decisionText(ToadGameState state) {
        int current = state.getCurrentPlayer();
        return switch ((ToadGamePhase) state.getGamePhase()) {
            case OPENING_RETURN -> "Player " + current + " returns a card to the bottom of their deck";
            case DISCARD -> "Player " + current + " may recycle a card";
            case PLAY -> "Player " + current + " plays a " + (state.getFieldCard(current) == null ? "face-up" : "hidden")
                    + " card";
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

    private JPanel createRulesPanel() {
        JPanel rules = new JPanel();
        rules.setBackground(new Color(43, 108, 25, 111));
        JLabel text = new JLabel("<html><center><h1>War of the Toads</h1></center><hr>" +
                "<p>Two players, each with a deck of nine toads, fight two Wars. <b>Capture more Hostages than your " +
                "opponent.</b></p><ul>" +
                "<li><b>Start of each War:</b> draw 5 cards, then put 1 of them on the bottom of your deck.</li>" +
                "<li><b>Four Battles per War.</b> The Attacker plays a face-up card and a hidden card; the Defender " +
                "then plays one opposite each. Both draw 2 cards, and the hidden cards are revealed. The roles swap " +
                "every Battle.</li>" +
                "<li><b>Tactics:</b> only the hidden card's Tactic acts, in the order Block, Start, During, After; " +
                "Tactics in the same stage act at the same time. A card's <i>Ally</i> is your other card, and its " +
                "<i>Foe</i> is the card opposite the Ally. Special Attributes always apply.</li>" +
                "<li><b>Each lane:</b> a Siege Cannon loses in Defence and wins in Attack, except against a " +
                "Saboteur; an Assassin beats a General; otherwise the higher Strength wins. A tie stands unless one " +
                "side breaks ties (Saboteur).</li>" +
                "<li><b>Hostages:</b> the winner of a lane captures the loser. A tied lane sends both cards to the " +
                "Shrine: each player gains a Flag. If you win both lanes while <i>Calm</i> (not behind on Hostages) " +
                "you keep one Hostage and the other goes to the Shrine (a Flag each); if <i>Angry</i> (behind, or " +
                "made Angry by a Berserker) you keep both.</li>" +
                "<li><b>Between the Wars:</b> the card left in your hand is your Casualty and gives you a Flag for " +
                "War 2. The decks are rebuilt and swapped, and Player 1 attacks first in War 2.</li>" +
                "<li><b>Winning:</b> the winner of War 2 wins, or the winner of War 1 if War 2 is a Stalemate. If " +
                "both are Stalemates, the lower Casualty wins (a Siege Cannon is the lowest).</li>" +
                "</ul><p>Variants (game parameters): the legacy decks and rules, recycling a card before each " +
                "Battle, and who attacks first in War 2.</p>" +
                "<hr><p><b>INTERFACE:</b> choose from the action list at the bottom of the screen. The middle of the " +
                "table shows the two lanes (the last Battle's cards dimmed until the next is played) and the state " +
                "of the War. Each player's area shows their hand, their deck (with its bottom card, if you know " +
                "it), their Casualty, Hostages and Flags; the player to act is outlined in blue. Cards shown to you " +
                "by a Scout or a Siege Cannon guess are face-up in your opponent's hand.</p></html>");
        text.setVerticalAlignment(SwingConstants.TOP);
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(width - 40, height));
        rules.add(scroll);
        return rules;
    }
}
