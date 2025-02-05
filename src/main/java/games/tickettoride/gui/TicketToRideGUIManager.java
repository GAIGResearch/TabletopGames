package games.tickettoride.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;

import core.components.Card;
import core.components.Deck;
import core.interfaces.IGamePhase;
import core.properties.PropertyColor;
import games.tickettoride.TicketToRideConstants;
import games.tickettoride.TicketToRideGameState;
import static games.tickettoride.gui.TicketToRideCardView.*;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;
import utilities.Utils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static core.CoreConstants.colorHash;
import static core.CoreConstants.playerHandHash;

import static javax.swing.ScrollPaneConstants.*;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;


/**
 * <p>This class allows the visualisation of the game. The game components (accessible through {@link Game#getGameState()}
 * should be added into {@link javax.swing.JComponent} subclasses (e.g. {@link javax.swing.JLabel},
 * {@link javax.swing.JPanel}, {@link javax.swing.JScrollPane}; or custom subclasses such as those in {@link gui} package).
 * These JComponents should then be added to the <code>`parent`</code> object received in the class constructor.</p>
 *
 * <p>An appropriate layout should be set for the parent GamePanel as well, e.g. {@link javax.swing.BoxLayout} or
 * {@link java.awt.BorderLayout} or {@link java.awt.GridBagLayout}.</p>
 *
 * <p>Check the super class for methods that can be overwritten for a more custom look, or
 * {@link games.terraformingmars.gui.TMGUI} for an advanced game visualisation example.</p>
 *
 * <p>A simple implementation example can be found in {@link games.tictactoe.gui.TicTacToeGUIManager}.</p>
 */
public class TicketToRideGUIManager extends AbstractGUIManager implements IScreenHighlight {

    TicketToRideCardView[] playerCards;
    JLabel[][] playerHandCardCounts;
    ArrayList<TicketToRideCardView>[] playerHands;
    ArrayList<TicketToRideCardView> bufferDeck;
    TicketToRideBoardView boardView;


    TicketToRideGameState gameState;
    int nPlayers;
    int maxCards = 30;
    int maxBufferCards = 50;
    int panelWidth;

    ArrayList<Integer>[] handCardHighlights;
    HashSet<Integer> playerHighlights;
    ArrayList<Integer> bufferHighlights;


    int activePlayer;
    IGamePhase currentGamePhase;



    JLabel gameTurnStep;

    public TicketToRideGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;

        this.game = game;
        gameState = (TicketToRideGameState) game.getGameState();
        nPlayers = gameState.getNPlayers();
        this.gameState = (TicketToRideGameState) game.getGameState();

        System.out.println(this.game + "GAME STATE");

        boardView = new TicketToRideBoardView(gameState);

        System.out.println(boardView + "BOARD VIEW");

        gameTurnStep = new JLabel();

        JPanel gameStateInfo = createGameStateInfoPanel(gameState);
        System.out.println(  gameStateInfo + "GAME STATE INFO");
        JPanel playerAreas = createPlayerAreas();
        JComponent actionPanel = createActionPanel(new IScreenHighlight[]{ this}, 259, 80, this::bufferReset);
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.add(gameStateInfo);
        side.add(playerAreas);
        side.add(actionPanel);

        parent.setLayout(new BorderLayout());
        parent.add(side, BorderLayout.EAST);
        parent.add(boardView, BorderLayout.CENTER);



        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();

        System.out.println("IN GUI MANAGER");



        // TODO: set up GUI components and add to `parent`
    }

    /**
     * Defines how many action button objects will be created and cached for usage if needed. Less is better, but
     * should not be smaller than the number of actions available to players in any game state.
     *
     * @return maximum size of the action space (maximum actions available to a player for any decision point in the game)
     */
    @Override
    public int getMaxActionSpace() {
        // TODO
        return 40;
    }

    /**
     * Updates all GUI elements given current game state and player that is currently acting.
     *
     * @param player    - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        this.gameState = (TicketToRideGameState) gameState;
        boardView.gameState = this.gameState;

        currentGamePhase = gameState.getGamePhase();
        activePlayer = this.gameState.getCurrentPlayer();

        for (int i = 0; i < nPlayers; i++) {



            Deck<Card> playerTrainCardHand = (Deck<Card>) this.gameState.getComponentActingPlayer(i, playerHandHash);
            int numOfTrainCards = playerTrainCardHand.getSize();
            int[] colourCount = new int[TicketToRideConstants.cardColors.length];


            Deck<Card> playerDestinationCardHand = (Deck<Card>) this.gameState.getComponentActingPlayer(i, TicketToRideConstants.playerDestinationHandHash);
            int numOfDestinationCards = playerDestinationCardHand.getSize();


            for (int j = 0; j < numOfTrainCards; j++) {
                Card currentCard = playerTrainCardHand.peek(j);
                if (currentCard.getProperty(colorHash) != null) {
                    colourCount[Utils.indexOf(TicketToRideConstants.cardColors, ((PropertyColor) currentCard.getProperty(colorHash)).valueStr)]++;
                }
            }



            for (int j = 0; j < numOfDestinationCards; j++) {
                Card currentCard = playerDestinationCardHand.peek(j);
                if (j < playerHands[i].size()) { //already rendered
                    System.out.println("already rendered");
                    playerHands[i].get(j).updateComponent(currentCard);
                    playerHands[i].get(j).setVisible(true);
                } else if( currentCard.getProperty(TicketToRideConstants.location1Hash) != null){ //only draw destination cards
                    System.out.println("Destination card getcardview " + currentCard.getProperties());
                    getCardView(i, currentCard, j);

                }

            }

            System.out.println("playerHands size:  " + playerHands[i].size() + " for player " + i) ;

            for (int j = numOfDestinationCards; j < playerHands[i].size(); j++) {
                playerHands[i].get(j).updateComponent(null);
                playerHands[i].get(j).setVisible(j == 0);
                System.out.println("current destination card " + playerHands[i].get(j));
//                playerHands[i].get(j).repaint();
            }



            String[] playerCountInfo = new String[TicketToRideConstants.cardColors.length / 2 + 1];
            playerCountInfo[playerCountInfo.length - 1] = "Train cars: " + this.gameState.getTrainCars(i);
            int idx = -1;
            for (int c = 0; c < TicketToRideConstants.cardColors.length; c++) {
                if (c % 2 == 0) idx++;
                if (playerCountInfo[idx] == null) playerCountInfo[idx] = "";
                playerCountInfo[idx] += TicketToRideConstants.cardColors[c] + ": " + colourCount[c] + "; ";
            }


            for (int c = 0; c < playerCountInfo.length; c++) {
                playerHandCardCounts[i][c].setText(playerCountInfo[c]);
            }


        }
    }

    protected JPanel createGameStateInfoPanel(AbstractGameState gameState) {

        JPanel gameInfo = new JPanel();
        gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
        gameInfo.add(new JLabel("<html><h1>Ticket to Ride</h1></html>"));

        updateGameStateInfo(gameState);

        gameInfo.add(gameStatus);
        gameInfo.add(gamePhase);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);
        gameInfo.add(gameTurnStep);

        System.out.println();

        gameInfo.setPreferredSize(new Dimension(220, defaultInfoPanelHeight+5));
        panelWidth = 220;

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
        wrapper.add(gameInfo);

        historyInfo.setEditable(false);
        historyContainer = new JScrollPane(historyInfo);
        historyContainer.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
        historyContainer.setPreferredSize(new Dimension(300, 30));
        panelWidth += 300 + offset;
        wrapper.add(historyContainer);

        return wrapper;
    }

    private JPanel createPlayerAreas() {
        JPanel cardAreas = new JPanel();
        cardAreas.setLayout(new BoxLayout(cardAreas, BoxLayout.Y_AXIS));
        playerCards = new TicketToRideCardView[nPlayers];
        playerHands = new ArrayList[nPlayers];
        playerHandCardCounts = new JLabel[nPlayers][];

        JPanel playerAreas = new JPanel();
        playerAreas.setLayout(new BoxLayout(playerAreas, BoxLayout.Y_AXIS));
        JPanel[] playerDefs = new JPanel[nPlayers];

        Dimension size = new Dimension(cardWidth * 3+offset, cardHeight+offset*2);
        Dimension size2 = new Dimension((int)(cardWidth * 4.5), cardHeight*4+offset*2);

        for (int i = 0; i < nPlayers; i++) {
            playerDefs[i] = new JPanel();
            playerDefs[i].setLayout(new BoxLayout(playerDefs[i], BoxLayout.Y_AXIS));
            JPanel playerDef = new JPanel();
            playerDef.setLayout(new BoxLayout(playerDef, BoxLayout.X_AXIS));
            playerDefs[i].add(new JLabel("Player " + i + " [" + game.getPlayers().get(i).toString() + "]"));
            playerDefs[i].add(playerDef);


            int player = i;

            JPanel wrapInfo = new JPanel();
            wrapInfo.setPreferredSize(new Dimension(cardWidth, (int)(cardHeight*1.5)+offset));
            wrapInfo.setLayout(new BoxLayout(wrapInfo, BoxLayout.Y_AXIS));

            String[] playerCountInfo = new String[TicketToRideConstants.cardColors.length/2+1];
            playerCountInfo[playerCountInfo.length-1] = "Train cars: ";
            int idx = -1;
            for (int c = 0; c < TicketToRideConstants.cardColors.length; c++) {
                if (c % 2 == 0) idx++;
                if (playerCountInfo[idx] == null) playerCountInfo[idx] = "";
                playerCountInfo[idx] += TicketToRideConstants.cardColors[c] + ": 0; ";
            }
            JLabel[] counts = new JLabel[playerCountInfo.length];
            for (int c = 0; c < playerCountInfo.length; c++) {
                counts[c] = new JLabel(playerCountInfo[c]);
                wrapInfo.add(counts[c]);
            }
            playerHandCardCounts[i] = counts;
            playerDef.add(Box.createRigidArea(new Dimension(5,0)));
            playerDef.add(wrapInfo);

            JPanel hand = new JPanel();
            hand.setLayout(new BoxLayout(hand, BoxLayout.X_AXIS));
            Deck<Card> playerDestinationCardHand = (Deck<Card>) this.gameState.getComponentActingPlayer(i, TicketToRideConstants.playerDestinationHandHash);

            playerHands[i] = new ArrayList<>();
            for (int k = 0; k < maxCards; k++) {
                Card c = null;
                if (k < playerDestinationCardHand.getSize()) {
                    c = playerDestinationCardHand.peek(k);
                }
                TicketToRideCardView cv2 = getCardView(i, c, k);
                hand.add(cv2);
            }
            JScrollPane scrollPane = new JScrollPane(hand);
            scrollPane.setPreferredSize(size);
            scrollPane.setMinimumSize(size);
            scrollPane.setMaximumSize(size);
            scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER);
            scrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_ALWAYS);
            playerDef.add(scrollPane);
            playerDef.add(Box.createRigidArea(new Dimension(5,0)));
            playerAreas.add(playerDefs[i]);
        }

        JScrollPane scrollPane1 = new JScrollPane(playerAreas);
        scrollPane1.setPreferredSize(size2);
        scrollPane1.setMinimumSize(size2);
        scrollPane1.setMaximumSize(size2);
        scrollPane1.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane1.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
        cardAreas.add(scrollPane1);
//        cardAreas.add(playerAreas);

        // Buffer deck space
        JPanel bufferDeckArea = new JPanel();
        bufferDeckArea.setLayout(new BoxLayout(bufferDeckArea, BoxLayout.X_AXIS));
        bufferDeck = new ArrayList<>();
        for (int i = 0; i < maxBufferCards; i++) {
            TicketToRideCardView cv = new TicketToRideCardView(null);
            int idx = i;
            cv.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        bufferHighlights.add(idx);
                        cv.setBorder(new LineBorder(Color.cyan, 2));
                    } else {
                        bufferHighlights.remove(Integer.valueOf(idx));
                        cv.setBorder(null);
                    }
                }
            });
            cv.setVisible(false);
            bufferDeck.add(cv);
            bufferDeckArea.add(cv);
        }

        JScrollPane scrollPane = new JScrollPane(bufferDeckArea);
        scrollPane.setMaximumSize(size);
        scrollPane.setMinimumSize(size);
        scrollPane.setPreferredSize(size);
        scrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER);
        cardAreas.add(scrollPane);

        return cardAreas;
    }

    private TicketToRideCardView getCardView(int player, Card c, int cardIdx) {
        TicketToRideCardView cv2 = new TicketToRideCardView(c);
        cv2.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    handCardHighlights[player].add(cardIdx);
                    cv2.setBorder(new LineBorder(Color.cyan, 2));
                } else {
                    handCardHighlights[player].remove(Integer.valueOf(cardIdx));
                    cv2.setBorder(null);
                }
            }
        });
        cv2.setVisible(c != null);
        System.out.println("putting in card view for player " + player +" cv: " +  cv2   );
        playerHands[player].add(cv2);
        return cv2;
    }

    private void bufferReset(ActionButton ab) {
        for (TicketToRideCardView pcv: bufferDeck) {
            pcv.setVisible(false);
        }
    }

    public void clearHighlights() {
//        playerHighlights.clear();
//        for (int i = 0; i < playerCards.length; i++) {
//            handCardHighlights[i].clear();
//        }
//        boardView.getHighlights().clear();
    }


}
