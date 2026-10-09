package games.hearts.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.hearts.HeartsGameState;
import games.hearts.HeartsParameters;
import games.hearts.actions.Pass;
import games.tricktaking.PlayCard;
import games.tricktaking.Trick;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import gui.views.RulesView;
import players.human.ActionController;
import utilities.ImageIO;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * GUI for Hearts. A human player passes or plays a card by clicking it in their hand (see
 * {@link gui.ClickableActions}); the action buttons stay as well.
 */
public class HeartsGUIManager extends AbstractGUIManager {
    final static int playerWidth = 300;
    final static int playerHeight = 130;
    final static int cardWidth = 90;
    final static int cardHeight = 115;

    private String lastHistoryEntry = null;

    int width, height;
    HeartsPlayerView[] playerHands;
    HeartsPlayerTrickView[] playerTricks;


    private HeartsGameState gameState;

    int activePlayer = -1;

    Border highlightActive = BorderFactory.createLineBorder(new Color(47,132,220), 3);
    Border[] playerViewBorders;

    static final Color playable = new Color(255, 210, 0);
    // the state last shown, and the trick in progress
    HeartsGameState shown;
    JLabel trickText;

    public HeartsGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanID) {
        super(parent, game, ac, humanID);

        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);

        if (game != null){
            AbstractGameState gameState = game.getGameState();
            if (gameState != null){
                JTabbedPane pane = new JTabbedPane();
                JPanel main = new JPanel();
                main.setOpaque(false);
                main.setLayout(new BorderLayout());
                JPanel rules = new JPanel();
                pane.add("Main", main);
                pane.add("Rules", rules);
                JLabel ruleText = new JLabel(getRuleText((HeartsParameters) gameState.getGameParameters()));
                rules.add(ruleText);
                rules.setBackground(new Color(43, 108, 25, 111));

                activePlayer = gameState.getCurrentPlayer();

                int nPlayers = gameState.getNPlayers();
                int nHorizAreas = 1 + (nPlayers <= 3 ? 2 : nPlayers == 4 ? 3 : nPlayers <= 8 ? 4 : 5);
                double nVertAreas = 3.5;
                this.width = playerWidth * nHorizAreas;
                this.height = (int) (playerHeight * nVertAreas * 2); // double the height
                parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + defaultCardHeight * 2 + 20));
                ruleText.setPreferredSize(new Dimension(width*2/3+60, height*2/3+100));

                HeartsGameState hgs = (HeartsGameState) gameState;
                HeartsParameters bjgp = (HeartsParameters) gameState.getGameParameters();

                parent.setBackground(ImageIO.GetInstance().getImage("data/FrenchCards/table-background.jpg"));

                playerHands = new HeartsPlayerView[nPlayers];
                playerTricks = new HeartsPlayerTrickView[nPlayers];
                playerViewBorders = new Border[nPlayers];
                JPanel mainGameArea = new JPanel();
                mainGameArea.setOpaque(false);
                mainGameArea.setLayout(new BorderLayout());


                String[] locations = new String[]{BorderLayout.NORTH, BorderLayout.EAST, BorderLayout.SOUTH, BorderLayout.WEST};
                JPanel[] sides = new JPanel[nPlayers];


                int rows = nPlayers / 2 + nPlayers % 2;
                int cols = nPlayers / rows;

                mainGameArea.setLayout(new GridLayout(rows, cols));

                for (int i = 0; i < nPlayers; i++) {
                    HeartsPlayerView playerHand = new HeartsPlayerView(hgs.getPlayerDecks().get(i), i, bjgp.getDataPath());
                    HeartsPlayerTrickView playerTrick = new HeartsPlayerTrickView(hgs.getPlayerTrickDecks().get(i), i, bjgp.getDataPath());
                    playerHand.setOpaque(false);
                    playerTrick.setOpaque(false);


                    String agentName = game.getPlayers().get(i).toString();


                    TitledBorder title;

                        title = BorderFactory.createTitledBorder(
                                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i + " [" + agentName + "]",
                                TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);

                    playerViewBorders[i] = title;
                    playerHand.setBorder(title);
                    playerTrick.setBorder(title);

                    int seat = i;
                    playerHand.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            clicked(seat, e);
                        }
                    });
                    playerHand.setToolTips(e -> toolTip(seat, e));

                    sides[i] = new JPanel();
                    sides[i].add(playerHand);
                    sides[i].add(playerTrick);
                    sides[i].setLayout(new GridLayout(1,2));
                    sides[i].setOpaque(false);
                    playerHands[i] = playerHand;
                    playerTricks[i] = playerTrick;

                    mainGameArea.add(sides[i]);
                }


                JPanel infoPanel = createGameStateInfoPanel("Hearts", gameState, width, defaultInfoPanelHeight);

                JComponent actionPanel = createActionPanelOpaque(new IScreenHighlight[0], width, defaultActionPanelHeight, false);


                trickText = new JLabel(" ");
                trickText.setForeground(Color.white);
                trickText.setFont(trickText.getFont().deriveFont(Font.BOLD, 14f));
                trickText.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
                JPanel south = new JPanel(new BorderLayout());
                south.setOpaque(false);
                south.add(trickText, BorderLayout.NORTH);
                south.add(actionPanel, BorderLayout.CENTER);

                main.add(mainGameArea, BorderLayout.CENTER);
                main.add(infoPanel, BorderLayout.NORTH);
                main.add(south, BorderLayout.SOUTH);

                pane.add("Main", main);
                pane.add("Rules", rules);
                pane.add("How to Play", new RulesView(howToPlayHtml(), height));

                parent.setLayout(new BorderLayout());
                parent.add(pane, BorderLayout.CENTER);
                parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + defaultCardHeight + 20));
                parent.revalidate();
                parent.setVisible(true);
                parent.repaint();
            }
        }
    }

    @Override
    public int getMaxActionSpace() {
        return 15;
    }


    @Override
    protected JPanel createGameStateInfoPanel(String gameTitle, AbstractGameState gameState, int width, int height) {
        JPanel gameInfo = new JPanel();
        gameInfo.setOpaque(false);
        gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
        gameInfo.add(new JLabel("<html><h1>" + gameTitle + "</h1></html>"));



        updateGameStateInfo(gameState);


        gameInfo.add(gameStatus);
        gameInfo.add(playerStatus);
        gameInfo.add(playerScores);
        gameInfo.add(gamePhase);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);

        gameInfo.setPreferredSize(new Dimension(width/2 - 10, height));

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new FlowLayout());
        wrapper.add(gameInfo);

        historyInfo.setPreferredSize(new Dimension(width/2 - 10, height));
        historyContainer = new JScrollPane(historyInfo);
        historyContainer.setPreferredSize(new Dimension(width/2 - 25, height));
        wrapper.add(historyContainer);
        historyInfo.setOpaque(false);
        historyContainer.setOpaque(false);
        historyContainer.getViewport().setBackground(new Color(43, 108, 25, 111));
//        historyContainer.getViewport().setOpaque(false);
        historyInfo.setEditable(false);
        return wrapper;
    }

    @Override
    public void update(AbstractPlayer player, AbstractGameState gameState, boolean showActions) {
        super.update(player, gameState, showActions);
        // after the actions are offered (or withdrawn) for this state
        showClickable();
    }

    /**
     * A left click on a card in the hand of the human player to act passes or plays it, if they may.
     */
    private void clicked(int seat, MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1 || seat != clickable.player()) return;
        AbstractAction action = actionFor(playerHands[seat].cardAt(e.getPoint()));
        if (clickable.submit(action))
            showClickable();
    }

    /**
     * The pass or play of the card offered to the human player to act, or null.
     */
    private AbstractAction actionFor(FrenchCard card) {
        if (card == null) return null;
        List<AbstractAction> actions = clickable.matching(a ->
                a instanceof Pass pass && pass.card1.equals(card) && pass.playerID == clickable.player()
                        || a instanceof PlayCard<?> play && card.equals(play.card));
        return actions.isEmpty() ? null : actions.get(0);
    }

    /**
     * Outlines the cards the human player to act may pass or play, and shows the trick in progress.
     */
    private void showClickable() {
        if (shown == null || playerHands == null) return;
        int me = clickable.player();
        for (int i = 0; i < playerHands.length; i++) {
            Map<FrenchCard, Color> outlines = new HashMap<>();
            if (i == me)
                for (FrenchCard card : shown.getPlayerDecks().get(i).getComponents())
                    if (actionFor(card) != null)
                        outlines.put(card, playable);
            playerHands[i].setOutlines(outlines);
        }
        trickText.setText(trickText(shown));
    }

    private String trickText(HeartsGameState state) {
        if (state.getGamePhase() == HeartsGameState.Phase.PASSING)
            return "Passing " + passDirectionName(state) + ": each player passes "
                    + ((HeartsParameters) state.getGameParameters()).cardsPassedPerRound + " cards";
        // the hold hand: say so for the whole hand, or the missing pass looks like a bug
        String noPass = ((HeartsParameters) state.getGameParameters()).passCards && state.getPassDirection() == 0
                ? "No passing this hand (every 4th hand is played as dealt).   " : "";
        Trick<FrenchCard, FrenchCard.Suite> trick = state.currentTrick;
        if (trick.getSize() == 0)
            return noPass + "Player " + state.getCurrentPlayer() + " to lead" + (state.heartsBroken ? "" : " (hearts not broken)");
        StringJoiner plays = new StringJoiner(",  ");
        for (int i = 0; i < trick.getSize(); i++)
            plays.add("P" + trick.playerOf(i) + " " + cardName(trick.get(i)));
        return noPass + "Trick: " + plays + "   (" + trick.getLeadSuit() + " led)";
    }

    /**
     * What clicking the card under the mouse would do, and what would follow.
     */
    private String toolTip(int seat, MouseEvent e) {
        if (shown == null) return null;
        FrenchCard card = playerHands[seat].cardAt(e.getPoint());
        if (card == null || seat != clickable.player()) return null;
        AbstractAction action = actionFor(card);
        HeartsParameters params = (HeartsParameters) shown.getGameParameters();
        if (action instanceof Pass) {
            int to = (seat + shown.getPassDirection()) % shown.getNPlayers();
            int passed = shown.pendingPasses.get(seat).size();
            return html("Click to pass the " + cardName(card) + " to Player " + to + " (" + passDirectionName(shown) + ")",
                    "Card " + (passed + 1) + " of the " + params.cardsPassedPerRound + " you pass",
                    params.cardPoints(card) > 0 ? "It scores " + params.cardPoints(card) + " for whoever takes it" : "");
        }
        Trick<FrenchCard, FrenchCard.Suite> trick = shown.currentTrick;
        if (action == null) {
            FrenchCard.Suite lead = trick.getSize() == 0 ? null : trick.getLeadSuit();
            return html("You cannot play the " + cardName(card) + " now",
                    lead != null && card.suite != lead ? "You must follow " + lead + " if you can" : "");
        }
        List<String> lines = new ArrayList<>();
        lines.add("Click to play the " + cardName(card));
        Trick<FrenchCard, FrenchCard.Suite> after = trick.copy();
        after.play(card);
        if (trick.getSize() == 0)
            lines.add("You lead the trick: the others must follow " + card.suite + " if they can");
        else if (card.suite == trick.getLeadSuit())
            lines.add("Follows " + card.suite);
        else
            lines.add("A discard: you have no " + trick.getLeadSuit());
        if (trick.getSize() > 0) {
            int winner = after.winner(null);
            String who = winner == seat ? "You" : "Player " + winner;
            lines.add(after.isComplete() ? who + " take" + (winner == seat ? "" : "s") + " the trick"
                    : who + (winner == seat ? " would be winning" : " would still be winning") + " the trick so far");
        }
        int points = 0;
        for (FrenchCard c : after.getComponents())
            points += params.cardPoints(c);
        lines.add("The trick would hold " + points + (points == 1 ? " point" : " points"));
        if (card.suite == FrenchCard.Suite.Hearts && !shown.heartsBroken)
            lines.add("This breaks hearts");
        return html(lines.toArray(new String[0]));
    }

    private static String passDirectionName(HeartsGameState state) {
        int direction = state.getPassDirection();
        if (direction == 1) return "to the left";
        if (direction == state.getNPlayers() - 1) return "to the right";
        return "across";
    }

    private static String cardName(FrenchCard card) {
        String rank = card.type == FrenchCard.FrenchCardType.Number ? String.valueOf(card.number) : card.type.name();
        return rank + " of " + card.suite;
    }

    private static String html(String... lines) {
        StringJoiner text = new StringJoiner("<br>", "<html>", "</html>");
        for (String line : lines)
            if (!line.isEmpty()) text.add(line);
        return text.toString();
    }

    private static String howToPlayHtml() {
        return "<h2>How to Play</h2>" +
                "<p>Your hand is shown face up when it is your turn. Instead of using the action buttons below the " +
                "table you can click on your cards.</p>" +
                "<ul><li><b>Passing.</b> At the start of a round, click a card to pass it. Pass three cards, one " +
                "click each. The line above the action buttons says which way the cards go.</li>" +
                "<li><b>Playing.</b> Click a card to play it to the trick. The line above the action buttons shows " +
                "the trick so far: who played what, and the suit led.</li></ul>" +
                "<p>The cards you may pass or play have a yellow outline. A card without one cannot be played now, " +
                "for example because you must follow the suit led.</p>" +
                "<p><b>Tooltips.</b> Rest the mouse on a card to see what clicking it would do: who you would pass it " +
                "to, or whether it follows suit, who would be winning the trick, how many points the trick would " +
                "hold, and whether it breaks hearts. On a card you cannot play it says why.</p>" +
                "<p>A left click on any card in a hand still raises it to the front so you can see it whole, as " +
                "before.</p>";
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {

        if (gameState == null) {
            System.out.println("gameState is null");
        } else {


            playerTricks[gameState.getCurrentPlayer()].setFront(true);
            playerStatus.setText(Arrays.toString(gameState.getPlayerResults()));

            for (int i = 0; i < gameState.getNPlayers(); i++)
                playerHands[i].setFront(showHiddenInfo(gameState, i));

            if (gameState.getCurrentPlayer() != activePlayer) {
                playerHands[activePlayer].setCardHighlight(-1);
                activePlayer = gameState.getCurrentPlayer();
            }


            HeartsGameState hgs = (HeartsGameState) gameState;
            shown = hgs;
            if (hgs.isNotTerminal()) {
                this.gameState = hgs;
            }


            // passing is simultaneous: everyone still to pass is to act, not just the current player
            Collection<Integer> toAct = hgs.getGamePhase() == HeartsGameState.Phase.PASSING && !hgs.isActionInProgress()
                    ? hgs.getPlayersStillToPass() : List.of(gameState.getCurrentPlayer());
            for(int i = 0; i < gameState.getNPlayers(); i++) {
                playerHands[i].update(this.gameState);


                if (toAct.contains(i)) {
                    Border compound = BorderFactory.createCompoundBorder(
                            highlightActive, playerViewBorders[i]);
                    playerHands[i].setBorder(compound);
                } else {
                    playerHands[i].setBorder(playerViewBorders[i]);
                }
            }






            String newHistory = String.join("\n", gameState.getHistoryAsText());
            if (!newHistory.equals(lastHistoryEntry)) {
                historyInfo.setText(newHistory);
                lastHistoryEntry = newHistory;
            }


            historyInfo.setCaretPosition(historyInfo.getDocument().getLength());


        }
    }

    private String getRuleText(HeartsParameters params) {
        String rules = "<html><center><h1>Hearts</h1></center><br/><hr><br/>";
        rules = "<html><p>Hearts is a trick taking game where the objective is to avoid scoring points. The game is played over several rounds, and the player with the fewest points at the end of the game wins.</p>" +
                "<ul><li>Each round starts with players passing three cards to another player. The direction of passing alternates each round. In the first round, players pass to the left. In the second round, they pass to the right. In the third round, they pass across. There is no passing in the fourth round, and then the cycle repeats.</li>" +
                "<li>After the pass, play starts with the player holding the 2 of clubs leading the trick by playing it. Each player, in turn, must follow suit if possible. If a player does not have any cards of the leading suit, they can play any other card. The player who played the highest value card of the leading suit wins the trick and leads the next one.</li>" +
                "<li>The player cannot play a Heart or the Queen of Spades in the first trick, and cannot play them in other tricks unless they have been 'broken', i.e., played in a previous trick. Hearts are broken with the first Heart played in the game.</li>" +
                "<li>Each Heart card in a trick scores 1 point, and the Queen of Spades scores 13. However, if a player manages to take all scoring cards in a round (a move known as 'shooting the moon'), they score 0 points and each other player scores 26 points.</li>" +
                "<li>The game ends when a player reaches or exceeds " + params.matchScore + " points at the end of a round, and the player with the fewest points is the winner.</li></ul>" +
                "<hr><p><b>INTERFACE: </b> Choose a card to play from your hand at the bottom of the screen.</p>";
        rules += "</html>";


        return rules;
    }
}