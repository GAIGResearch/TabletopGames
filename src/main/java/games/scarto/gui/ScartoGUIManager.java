package games.scarto.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.components.TarotCard;
import games.scarto.ScartoCardOrder;
import games.scarto.ScartoGameState;
import games.scarto.ScartoParameters;
import games.tricktaking.gui.CardArt;
import games.tricktaking.gui.CardHandView;
import games.tricktaking.gui.CardTrickView;
import games.tricktaking.gui.TarotCardFace;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;
import utilities.ImageIO;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The two opponents across the top of the table, player 0 at the bottom, and the trick in progress in the centre.
 */
public class ScartoGUIManager extends AbstractGUIManager {

    // trumps first, then the Fool, then the four suits; the highest card first within each
    static final List<TarotCard.Suit> DISPLAY_SUITS = List.of(TarotCard.Suit.Trumps, TarotCard.Suit.None,
            TarotCard.Suit.Swords, TarotCard.Suit.Batons, TarotCard.Suit.Cups, TarotCard.Suit.Coins);
    static final Comparator<TarotCard> HAND_DISPLAY_ORDER = Comparator
            .comparingInt((TarotCard c) -> DISPLAY_SUITS.indexOf(c.suit))
            .thenComparingInt(c -> -ScartoCardOrder.INSTANCE.rank(c));
    static final TarotCardFace FACE = new TarotCardFace(HAND_DISPLAY_ORDER);

    // up to 28 cards in a hand (the dealer's during the exchange)
    static final int playerAreaWidth = 560;
    // one player area: the cards, the status line underneath, and the titled border below that
    static final int playerAreaHeight = TarotCardFace.cardHeight + 43;

    List<CardHandView<TarotCard, TarotCard.Suit>> playerViews;
    CardTrickView<TarotCard, TarotCard.Suit> trickView;
    Border[] playerViewBorders;
    TitledBorder[] playerTitles;
    String[] agentNames;
    JLabel scoreLabel;

    final Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public ScartoGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        ScartoGameState state = (ScartoGameState) gameState;
        int nPlayers = state.getNPlayers();

        // wide enough for the longest line: the scarto's cards, shown to the dealer who discarded them
        trickView = new CardTrickView<>(FACE, nPlayers, 560);
        this.width = 2 * (playerAreaWidth + 30);
        // three bands: the two opponents, the trick, and player 0
        this.height = playerAreaHeight * 2 + trickView.getPreferredSize().height + 60;

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

        // player 0 at the bottom; players 1 and 2 across the top, in the order of play
        playerViews = new java.util.ArrayList<>();
        playerViewBorders = new Border[nPlayers];
        playerTitles = new TitledBorder[nPlayers];
        agentNames = new String[nPlayers];
        JPanel top = new JPanel(new GridLayout(1, nPlayers - 1));
        top.setOpaque(false);
        JPanel bottom = new JPanel(new GridBagLayout());
        bottom.setOpaque(false);
        for (int i = 0; i < nPlayers; i++) {
            CardHandView<TarotCard, TarotCard.Suit> playerView =
                    new CardHandView<>(FACE, state.getPlayerHand(i), i, playerAreaWidth);
            playerView.setOpaque(false);
            String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
            agentNames[i] = split[split.length - 1];
            TitledBorder title = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i,
                    TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
            title.setTitleColor(Color.white);   // the table background is dark
            playerTitles[i] = title;
            playerViewBorders[i] = title;
            playerView.setBorder(title);
            JPanel cell = new JPanel(new GridBagLayout());
            cell.setOpaque(false);
            cell.add(playerView);
            (i == 0 ? bottom : top).add(cell);
            playerViews.add(playerView);
        }

        JPanel mainGameArea = new JPanel(new BorderLayout());
        mainGameArea.setOpaque(false);
        mainGameArea.add(top, BorderLayout.NORTH);
        mainGameArea.add(bottom, BorderLayout.SOUTH);
        JPanel centre = new JPanel(new BorderLayout());
        centre.setOpaque(false);
        JPanel centreWrapper = new JPanel(new GridBagLayout());
        centreWrapper.setOpaque(false);
        centreWrapper.add(trickView);
        centre.add(centreWrapper, BorderLayout.CENTER);
        scoreLabel = new JLabel("", SwingConstants.CENTER);
        scoreLabel.setForeground(Color.white);   // the table background is dark
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, 14f));
        centre.add(scoreLabel, BorderLayout.SOUTH);
        mainGameArea.add(centre, BorderLayout.CENTER);

        JPanel infoPanel = createGameStateInfoPanel("Scarto", gameState, width, defaultInfoPanelHeight);
        // up to 28 cards can be legal at once, so the action panel is the vertical, scrolling list
        int actionPanelHeight = defaultActionPanelHeight * 2;
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, actionPanelHeight, true);

        main.add(infoPanel, BorderLayout.NORTH);
        main.add(mainGameArea, BorderLayout.CENTER);
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
        // in the exchange the dealer holds 28 cards, with the 3 of the scarto, any of which may be discardable; a
        // leader chooses from at most 25
        return 28;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof ScartoGameState state)) return;
        int currentPlayer = state.getCurrentPlayer();
        int dealer = state.getDealer();

        for (int i = 0; i < playerViews.size(); i++) {
            String extra = "won " + state.getCardsWon(i).getSize() + " cards, " + state.getDealScore(i) + " points";
            playerViews.get(i).update(state.getPlayerHand(i), showHand(state, i), state.getKnownVoids().get(i), extra);
            playerTitles[i].setTitle("Player " + i + " [" + agentNames[i] + "]"
                    + (i == dealer ? " - dealer" : "") + (i == state.getCurrentTrick().getLeader() && state.isNotTerminal() ? " - leads" : ""));
            playerViews.get(i).setBorder(i == currentPlayer && state.isNotTerminal()
                    ? BorderFactory.createCompoundBorder(highlightActive, playerViewBorders[i])
                    : playerViewBorders[i]);
        }
        updateTrickView(state);
        StringBuilder scores = new StringBuilder("Scores - ");
        for (int i = 0; i < playerViews.size(); i++)
            scores.append("Player ").append(i).append(": ").append((int) state.getGameScore(i)).append("     ");
        scoreLabel.setText(scores.toString().trim());
        parent.repaint();
    }

    /**
     * The centre panel: the deal and trick numbers, what the players must do, and the scarto.
     */
    private void updateTrickView(ScartoGameState state) {
        ScartoParameters params = (ScartoParameters) state.getGameParameters();
        // every completed trick adds 3 cards to the piles: the Fool goes to its player, the others to the winner
        int tricksDone = 0;
        for (int i = 0; i < state.getNPlayers(); i++)
            tricksDone += state.getCardsWon(i).getSize();
        tricksDone /= state.getNPlayers();
        String header = (params.nDeals > 1 ? "Deal " + (state.getRoundCounter() + 1) + " of " + params.nDeals + "   " : "")
                + "Trick " + Math.min(tricksDone + 1, params.handSize) + " of " + params.handSize;
        TarotCard.Suit lead = state.getCurrentTrick().getLeadSuit();
        String leadText;
        if (!state.isNotTerminal()) {
            header = "Game over";
            leadText = resultText(state);
        } else if (state.isExchanging()) {
            header = header + "   -   the dealer's exchange";
            leadText = "Player " + state.getDealer() + " (dealer) has taken the scarto and discards "
                    + (state.getPlayerHand(state.getDealer()).getSize() - params.handSize) + " more";
        } else if (lead == null) {
            leadText = state.getCurrentTrick().getSize() == 0
                    ? "Player " + state.getCurrentTrick().getLeader() + " to lead"
                    : "The Fool was led: the next card sets the suit";
        } else {
            leadText = "Suit led: " + lead + "  -  follow suit, else trump; the Fool may always be played";
        }
        trickView.update(state.getCurrentTrick(), TarotCard.Suit.Trumps, header, leadText, scartoText(state),
                state.isNotTerminal());
    }

    /**
     * The scarto's size, and its cards if the viewer may see the dealer's discards.
     */
    private String scartoText(ScartoGameState state) {
        int n = state.getScarto().getSize();
        String text = "Scarto (scores for player " + state.getDealer() + "): " + n + (n == 1 ? " card" : " cards");
        if (n > 0 && state.getScarto().getOwnerId() >= 0 && showHand(state, state.getScarto().getOwnerId()))
            text += " - " + state.getScarto().getComponents().stream()
                    .map(TarotCardFace::shortName).collect(Collectors.joining(", "));
        return text;
    }

    private String resultText(ScartoGameState state) {
        int best = Integer.MIN_VALUE;
        for (int i = 0; i < state.getNPlayers(); i++)
            best = Math.max(best, (int) state.getGameScore(i));
        StringBuilder winners = new StringBuilder();
        for (int i = 0; i < state.getNPlayers(); i++)
            if ((int) state.getGameScore(i) == best)
                winners.append(winners.isEmpty() ? "" : " and ").append("player ").append(i);
        return (winners.toString().contains(" and ") ? "Drawn between " : "Won by ") + winners + " with " + best
                + " points";
    }

    private boolean showHand(ScartoGameState state, int playerId) {
        // face-up for a human player, for the current player if the core parameters allow it, or in
        // full-observability mode
        return humanPlayerIds.contains(playerId)
                || state.getCoreGameParameters().alwaysDisplayFullObservable
                || (playerId == state.getCurrentPlayer() && state.getCoreGameParameters().alwaysDisplayCurrentPlayer);
    }

    private JPanel createRulesPanel() {
        JPanel rules = new JPanel();
        rules.setBackground(new Color(43, 108, 25, 111));
        JLabel text = new JLabel("<html><center><h1>Scarto</h1></center><hr>" +
                "<p>A tarot trick-taking game from Piedmont for three players, with a 78-card pack: four suits " +
                "(Swords and Batons, the <i>long</i> suits; Cups and Coins, the <i>round</i> suits) of King, Queen, " +
                "Cavalier (C), Knave (J) and 10 pips, 21 trumps, and the Fool.</p><ul>" +
                "<li>25 cards are dealt to each player. The last 3 (the <b>scarto</b>) go face down to the dealer's " +
                "pile and score for the dealer. (With the exchange option, the dealer first takes them into their " +
                "hand and discards 3 - not a King, the Angel, the Pagat (unless it is their only trump, the Fool " +
                "counting as a trump) or the Fool.)</li>" +
                "<li>The player after the dealer leads to the first trick; the winner of each trick leads the next.</li>" +
                "<li>You must <b>follow the suit led</b> if you can; if not, you must <b>play a trump</b> if you " +
                "can; otherwise play any card. The <b>Fool</b> may be played at any time instead. If the Fool is " +
                "led, the next card played sets the suit.</li>" +
                "<li>Card order: long suits K Q C J 10 ... 1; round suits K Q C J 1 2 ... 10 (the Ace is the " +
                "highest pip); trumps: the <b>Angel (20)</b> is highest, then the World (21), then 19 down to the " +
                "Pagat (1).</li>" +
                "<li>The highest trump wins the trick, or if none, the highest card of the suit led. The Fool never " +
                "wins: it goes back to its player's pile, and the winner takes the other two cards.</li>" +
                "<li>Scoring: Kings, the Pagat and the Angel 4 points; Queens and the Fool 3; Cavaliers 2; Knaves 1; " +
                "other cards 0; plus one point for every three cards in your pile ((cards + 1) / 3, rounded down). " +
                "There are 77 points in a deal. The highest score wins.</li>" +
                "<li>By default a game is one deal. With more deals the next player deals, and scores add up.</li>" +
                "</ul><hr><p><b>INTERFACE:</b> choose a card from the action buttons at the bottom of the screen. " +
                "The centre shows the trick so far, with the winning card outlined, and the scarto. A player's " +
                "area shows the cards they have won, their points this deal, and the suits they are known to be " +
                "void in (Sw, Ba, Cu, Co, Tr). The scores below the trick include earlier deals.</p></html>");
        text.setVerticalAlignment(SwingConstants.TOP);
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(width * 2 / 3 + 60, height * 2 / 3 + 100));
        rules.add(scroll);
        return rules;
    }
}
