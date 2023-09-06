package games.pandemic.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.*;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import core.interfaces.IGamePhase;
import core.properties.PropertyColor;
import core.properties.PropertyString;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicParameters;
import games.pandemic.PandemicTurnOrder;
import games.pandemic.actions.*;
import gui.IScreenHighlight;
import gui.AbstractGUIManager;
import gui.GamePanel;
import players.human.ActionController;
import players.human.HumanGUIPlayer;
import utilities.Hash;
import utilities.Utils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

import static core.CoreConstants.*;
import static games.pandemic.PandemicConstants.*;
import static games.pandemic.PandemicGameState.PandemicGamePhase.DiscardReaction;
import static games.pandemic.gui.PandemicCardView.*;
import static javax.swing.ScrollPaneConstants.*;

public class PandemicGUIManager extends AbstractGUIManager implements IScreenHighlight {
    PandemicCardView[] playerCards;
    JLabel[][] playerHandCardCounts;
    ArrayList<PandemicCardView>[] playerHands;
    ArrayList<PandemicCardView> bufferDeck;
    PandemicBoardView boardView;
    PandemicCounterView cY, cR, cB, cK;

    PandemicGameState gameState;
    int nPlayers;
    int maxCards;
    int maxBufferCards = 50;
    int panelWidth;

    ArrayList<Integer>[] handCardHighlights;
    HashSet<Integer> playerHighlights;
    ArrayList<Integer> bufferHighlights;

    // Game state info
    JLabel gameTurnStep;

    public PandemicGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null || ac == null) return;

        this.game = game;
        gameState = (PandemicGameState) game.getGameState();
        maxCards = ((PandemicParameters)gameState.getGameParameters()).getMaxCardsPerPlayer() + 2;  // 2 over limit before discard
        nPlayers = gameState.getNPlayers();
        this.gameState = (PandemicGameState) game.getGameState();
        boardView = new PandemicBoardView(gameState);

        handCardHighlights = new ArrayList[nPlayers];
        playerHighlights = new HashSet<>();
        bufferHighlights = new ArrayList<>();
        for (int i = 0; i < nPlayers; i++) {
            handCardHighlights[i] = new ArrayList<>();
        }
        Collection[] highlights = new Collection[3+nPlayers];
        highlights[0] = playerHighlights;
        highlights[1] = boardView.getHighlights().keySet();
        highlights[2] = bufferHighlights;
        System.arraycopy(handCardHighlights, 0, highlights, 3, nPlayers);

        boardView.setPlayerHighlights(playerHighlights);
        boardView.setCardHandHighlights(handCardHighlights);

        gameTurnStep = new JLabel();
        JPanel gameStateInfo = createGameStateInfoPanel(gameState);
        JPanel playerAreas = createPlayerAreas();
        JPanel counterArea = createCounterArea();
        JComponent actionPanel = createActionPanel(new IScreenHighlight[]{this}, 300, 80, this::bufferReset);
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.add(gameStateInfo);
        side.add(playerAreas);
        side.add(actionPanel);


        parent.setLayout(new BorderLayout());
        parent.add(side, BorderLayout.EAST);
        parent.add(boardView, BorderLayout.CENTER);
        parent.add(counterArea, BorderLayout.SOUTH);

        Dimension boardSize = boardView.getPreferredSize();
        parent.setPreferredSize(new Dimension((int)(boardSize.getWidth() + panelWidth),
                Math.max((int)boardSize.getHeight(),
                        defaultInfoPanelHeight*2+nPlayers*cardHeight+nPlayers*offset*4) + defaultItemSize + offset));

        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    @Override
    public int getMaxActionSpace() {
        return 750;
    }

    private void bufferReset(ActionButton ab) {
        for (PandemicCardView pcv: bufferDeck) {
            pcv.setVisible(false);
        }
    }

    private JPanel createPlayerAreas() {
        JPanel cardAreas = new JPanel();
        cardAreas.setLayout(new BoxLayout(cardAreas, BoxLayout.Y_AXIS));
        playerCards = new PandemicCardView[nPlayers];
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

            Card playerCard = (Card) gameState.getComponent(PandemicConstants.playerCardHash, i);
            PandemicCardView cv = new PandemicCardView(playerCard);
            int player = i;
            cv.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        playerHighlights.add(player);
                        cv.setBorder(new LineBorder(Color.cyan, 2));
                    } else {
                        playerHighlights.remove(player);
                        cv.setBorder(null);
                    }
                }
            });
            playerCards[i] = cv;
            JPanel wrapInfo = new JPanel();
            wrapInfo.setPreferredSize(new Dimension(cardWidth, (int)(cardHeight*1.5)+offset));
            wrapInfo.setLayout(new BoxLayout(wrapInfo, BoxLayout.Y_AXIS));
            wrapInfo.add(cv);
            String[] txts = new String[colors.length/2+1];
            txts[txts.length-1] = "event: 0";
            int idx = -1;
            for (int c = 0; c < colors.length; c++) {
                if (c % 2 == 0) idx++;
                if (txts[idx] == null) txts[idx] = "";
                txts[idx] += colors[c] + ": 0; ";
            }
            JLabel[] counts = new JLabel[txts.length];
            for (int c = 0; c < txts.length; c++) {
                counts[c] = new JLabel(txts[c]);
                wrapInfo.add(counts[c]);
            }
            playerHandCardCounts[i] = counts;
            playerDef.add(Box.createRigidArea(new Dimension(5,0)));
            playerDef.add(wrapInfo);

            JPanel hand = new JPanel();
            hand.setLayout(new BoxLayout(hand, BoxLayout.X_AXIS));
            Deck<Card> playerHand = (Deck<Card>) gameState.getComponent(playerHandHash, i);
            playerHands[i] = new ArrayList<>();
            for (int k = 0; k < maxCards; k++) {
                Card c = null;
                if (k < playerHand.getSize()) {
                    c = playerHand.peek(k);
                }
                PandemicCardView cv2 = getCardView(i, c, k);
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
            PandemicCardView cv = new PandemicCardView(null);
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

    private PandemicCardView getCardView(int player, Card c, int cardIdx) {
        PandemicCardView cv2 = new PandemicCardView(c);
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
        playerHands[player].add(cv2);
        return cv2;
    }

    protected JPanel createGameStateInfoPanel(AbstractGameState gameState) {

        JPanel gameInfo = new JPanel();
        gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
        gameInfo.add(new JLabel("<html><h1>Pandemic</h1></html>"));

        updateGameStateInfo(gameState);

        gameInfo.add(gameStatus);
        gameInfo.add(gamePhase);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);
        gameInfo.add(gameTurnStep);

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

    protected void updateGameStateInfo(AbstractGameState gameState) {
        super.updateGameStateInfo(gameState);
        PandemicGameState pgs = (PandemicGameState) gameState;
        gameTurnStep.setText("Turn step: " + ((PandemicTurnOrder)pgs.getTurnOrder()).getTurnStep());
    }

    private JPanel createCounterArea() {
        JPanel counterArea = new JPanel();

        counterArea.add(new JLabel("Disease Cubes left:"));
        Counter cnY = (Counter) gameState.getComponent(Hash.GetInstance().hash("Disease Cube yellow"));
        cY = new PandemicCounterView(cnY, Color.yellow, null);
        counterArea.add(cY);
        Counter cnR = (Counter) gameState.getComponent(Hash.GetInstance().hash("Disease Cube red"));
        cR = new PandemicCounterView(cnR, Color.red, null);
        counterArea.add(cR);
        Counter cnB = (Counter) gameState.getComponent(Hash.GetInstance().hash("Disease Cube blue"));
        cB = new PandemicCounterView(cnB, Color.blue, null);
        counterArea.add(cB);
        Counter cnK = (Counter) gameState.getComponent(Hash.GetInstance().hash("Disease Cube black"));
        cK = new PandemicCounterView(cnK, Color.black, null);
        counterArea.add(cK);

        return counterArea;
    }

    int activePlayer;
    IGamePhase currentGamePhase;

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState){
        this.gameState = (PandemicGameState) gameState;
        boardView.gameState = this.gameState;
        boolean newTurn = this.gameState.getTurnOrder().getCurrentPlayer(gameState) != activePlayer || currentGamePhase == null || !currentGamePhase.equals(gameState.getGamePhase());

        currentGamePhase = gameState.getGamePhase();
        activePlayer = this.gameState.getTurnOrder().getCurrentPlayer(gameState);
        // Update counters
        Counter cnY = (Counter) this.gameState.getComponent(Hash.GetInstance().hash("Disease Cube yellow"));
        cY.updateComponent(cnY);
        Counter cnR = (Counter) this.gameState.getComponent(Hash.GetInstance().hash("Disease Cube red"));
        cR.updateComponent(cnR);
        Counter cnB = (Counter) this.gameState.getComponent(Hash.GetInstance().hash("Disease Cube blue"));
        cB.updateComponent(cnB);
        Counter cnK = (Counter) this.gameState.getComponent(Hash.GetInstance().hash("Disease Cube black"));
        cK.updateComponent(cnK);

        for (int i = 0; i < nPlayers; i++) {

            Card playerCard = (Card) this.gameState.getComponent(PandemicConstants.playerCardHash, i);
            playerCards[i].updateComponent(playerCard);
//            playerCards[i].setUsingSecondary(i == activePlayer);
            playerCards[i].repaint();

            Deck<Card> playerHand = (Deck<Card>) this.gameState.getComponent(playerHandHash, i);
            int nCards = playerHand.getSize();
            int[] colourCount = new int[colors.length];
            int eventCount = 0;

            for (int j = 0; j < nCards; j++) {
                Card c = playerHand.peek(j);
                if (j < playerHands[i].size()) {
                    playerHands[i].get(j).updateComponent(c);
                    playerHands[i].get(j).setVisible(true);
                } else {
                    getCardView(i, c, j);
                }
                if (c.getProperty(colorHash) != null) {
                    colourCount[Utils.indexOf(colors, ((PropertyColor) c.getProperty(colorHash)).valueStr)]++;
                } else {
                    eventCount++;
                }
            }
            for (int j = nCards; j < playerHands[i].size(); j++) {
                playerHands[i].get(j).updateComponent(null);
                playerHands[i].get(j).setVisible(j == 0);
            }

            // Update count of cards of colour in hand
            String[] txts = new String[colors.length/2+1];
            txts[txts.length-1] = "event: " + eventCount;
            int idx = -1;
            for (int c = 0; c < colors.length; c++) {
                if (c % 2 == 0) idx++;
                if (txts[idx] == null) txts[idx] = "";
                txts[idx] += colors[c] + ": " + colourCount[c] + "; ";
            }
            for (int c = 0; c < txts.length; c++) {
                playerHandCardCounts[i][c].setText(txts[c]);
            }
        }

        // Update display for human
        if (player instanceof HumanGUIPlayer) {
            updateCardHighlightDisplay();
            if (newTurn) {
                PropertyString playerLocationProperty = (PropertyString) this.gameState.getComponent(playerCardHash, activePlayer)
                        .getProperty(playerLocationHash);
                String playerLocationName = playerLocationProperty.value;
                JOptionPane.showMessageDialog(parent, "It's your turn! You are in " + playerLocationName + ". Current game phase: " + this.gameState.getGamePhase());
            }
        } else {
            // Clear all highlights if it's not human acting
            clearHighlights();
            updateCardHighlightDisplay();
        }

    }

    private void updateCardHighlightDisplay() {
        for (int i = 0; i < playerCards.length; i++) {
            if (!playerHighlights.contains(i)) {
                playerCards[i].setBorder(null);
            }
            for (int j = 0; j < playerHands[i].size(); j++) {
                if (!handCardHighlights[i].contains(j)) {
                    playerHands[i].get(j).setBorder(null);
                }
            }
        }
    }

    public void clearHighlights() {
        playerHighlights.clear();
        for (int i = 0; i < playerCards.length; i++) {
            handCardHighlights[i].clear();
        }
        boardView.getHighlights().clear();
    }

    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        int id = player.getPlayerID();
        List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState);
        resetActionButtons();

        int k = 0;

        Set<String> highlights = boardView.getHighlights().keySet();
        Set<Integer> playerTokenHighlights = new HashSet<>();
        Set<String> bnHighlights = new HashSet<>();
        Set<String> deckHighlights = new HashSet<>();
        for (String highlight: highlights) {
            if (highlight.contains("BN")) {
                String[] splits = highlight.split(" ");
                StringBuilder bn = new StringBuilder();
                for (String s: splits) {
                    s = s.trim();
                    if (!s.equals("BN")) {
                        bn.append(s).append(" ");
                    }
                }
                bnHighlights.add(bn.toString().trim());
            } else if (highlight.contains("player ")) {
                playerTokenHighlights.add(Integer.parseInt(highlight.split(" ")[1]));
            } else {
                deckHighlights.add(highlight);
            }
        }
        for (AbstractAction action : actions) {
            if (action instanceof MovePlayer) {
                int pIdx = ((MovePlayer) action).getPlayerToMove();
                Card c = (action instanceof MovePlayerWithCard) ? ((MovePlayerWithCard) action).getCard(gameState) : null;
                if (action instanceof MovePlayerWithCard && isCardHighlighted(c, pIdx)) {
                    if (c.getProperty(effectHash) == null || bnHighlights.contains(((MovePlayer) action).getDestination()) &&
                                (pIdx == id || playerTokenHighlights.contains(pIdx))) {
                        actionButtons[k].setVisible(true);
                        actionButtons[k++].setButtonAction(action, gameState);
                    }
                } else if (bnHighlights.contains(((MovePlayer) action).getDestination()) &&
                        (pIdx == id || playerTokenHighlights.contains(pIdx))) {
                    actionButtons[k].setVisible(true);
                    actionButtons[k++].setButtonAction(action, gameState);
                }
            } else if (action instanceof AddResearchStation) {
                Card playerRole = (Card) this.gameState.getComponentActingPlayer(playerCardHash);
                String playerLocation = ((PropertyString)playerRole.getProperty(playerLocationHash)).value;
                String toCity = ((AddResearchStation) action).getCity();

                if (bnHighlights.contains(toCity) || playerLocation.equals(toCity)) {
                    if (action instanceof AddResearchStationFrom) {
                        if (bnHighlights.contains(((AddResearchStationFrom) action).getFromCity())) {
                            if (!(action instanceof AddResearchStationWithCardFrom) ||
                                    isCardHighlighted(((AddResearchStationWithCardFrom)action).getCard(gameState), id)) {
                                actionButtons[k].setVisible(true);
                                actionButtons[k++].setButtonAction(action, gameState);
                            }
                        }
                    } else {
                        actionButtons[k].setVisible(true);
                        actionButtons[k++].setButtonAction(action, gameState);
                    }
                }
            } else if (actions.size() > 0 && action instanceof DoNothing || action instanceof TreatDisease) {
                actionButtons[k].setVisible(true);
                actionButtons[k++].setButtonAction(action, gameState);
            } else if (action instanceof CureDisease) {
                ArrayList<Integer> cards = ((CureDisease) action).getCards();
                boolean allSelected = true;
                for (Integer cardId: cards) {
                    if (isCardHighlighted((Card)gameState.getComponentById(cardId), id)) {
                        allSelected = false;
                        break;
                    }
                }
                if (allSelected) {
                    actionButtons[k].setVisible(true);
                    actionButtons[k++].setButtonAction(action, gameState);
                }
            } else if (action instanceof QuietNight || action instanceof Forecast) {  // Event
                actionButtons[k].setVisible(true);
                actionButtons[k++].setButtonAction(action, gameState);
            } else if (action instanceof RearrangeDeckOfCards) {  // Event
                Card eventCard = ((RearrangeDeckOfCards)action).getCard(gameState);
                int[] cardOrder = ((RearrangeDeckOfCards) action).getNewCardOrder();
                int nCards = cardOrder.length;
                Deck<Card> deckFrom = (Deck<Card>) gameState.getComponentById(((RearrangeDeckOfCards) action).getDeckFrom());
                // Show top N card of infection discard deck for player to select order
                fillBufferDeck(deckFrom);

                if (bufferHighlights.size() == nCards) {
                    // Card order selected
                    int[] selectedOrder = new int[nCards];
                    for (int i = 0; i < nCards; i++) {
                        selectedOrder[i] = bufferHighlights.get(i);
                    }

                    if (Arrays.equals(selectedOrder, cardOrder)) {
                        String name = ((PropertyString)eventCard.getProperty(nameHash)).value;
                        actionButtons[k].setVisible(true);
                        actionButtons[k++].setButtonAction(action, "Play: " + name);
                    }
                }

            } else if (action instanceof RemoveComponentFromDeck) {  // Event
                Card eventCard = ((RemoveComponentFromDeck)action).getCard(gameState);
                int infectionCard = ((RemoveComponentFromDeck) action).getComponentIdx();
                Deck<Card> deck = (Deck<Card>) gameState.getComponentById(((RemoveComponentFromDeck) action).getDeck());
                fillBufferDeck(deck);

                if (bufferHighlights.size() == 1) {
                    int selected = bufferHighlights.get(0);

                    if (infectionCard == selected) {
                        String name = ((PropertyString) eventCard.getProperty(nameHash)).value;
                        actionButtons[k].setVisible(true);
                        actionButtons[k++].setButtonAction(action, "Play: " + name);
                    }
                }
            } else if (action instanceof DrawCard) {
                if (this.gameState.getGamePhase() == DiscardReaction) {  // Discarding
                    int idx = ((DrawCard) action).getFromIndex();
                    Card c = (Card) playerHands[id].get(idx).getComponent();
                    if (c != null) {
                        String name = ((PropertyString)c.getProperty(nameHash)).value;
                        // Action name should be just "Discard" for card selected in hand
                        actionButtons[k].setVisible(true);
                        actionButtons[k++].setButtonAction(action, "Discard: " + name);
                    }
                } else {
                    if (this.gameState.getPlayerRole(id).equals("Contingency Planner")) {  // Special role
                        if (deckHighlights.contains("playerDiscard")) {
                            Deck<Card> deck = (Deck<Card>) gameState.getComponentById(((DrawCard) action).getDeckFrom());
                            if (deck != null) {
                                fillBufferDeck(deck);

                                if (bufferHighlights.size() == 1) {
                                    int selected = bufferHighlights.get(0);

                                    if (((DrawCard) action).getFromIndex() == selected) {
                                        Card c = deck.get(selected);
                                        if (c != null) {
                                            String name = ((PropertyString) c.getProperty(nameHash)).value;
                                            actionButtons[k].setVisible(true);
                                            actionButtons[k++].setButtonAction(action, "Choose: " + name);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (action instanceof ShareKnowledge) {
                        // Share knowledge
                        int giverID = ((ShareKnowledge) action).getGiver();
                        int receiverID = ((ShareKnowledge) action).getReceiver();
                        if (isCardHighlighted(((ShareKnowledge) action).getCard(gameState), giverID)) {
                            if (id == giverID && playerHighlights.contains(receiverID)) {
                                // Give card
                                // card in hand selected and other player, show this action as available
                                actionButtons[k].setVisible(true);
                                actionButtons[k++].setButtonAction(action, gameState);
                            } else if (id == receiverID) {
                                //Take card
                                // A card from another player selected
                                actionButtons[k].setVisible(true);
                                actionButtons[k++].setButtonAction(action, gameState);
                            }
                        }
                    } else {
                        actionButtons[k].setVisible(true);
                        actionButtons[k++].setButtonAction(action, gameState);
                    }
                }
            } else {
                System.out.println("Action type unknown: " + action.toString());
            }
        }
    }

    private void fillBufferDeck(Deck<Card> deck) {
        for (int i = 0; i < deck.getSize(); i++) {
            if (i < maxBufferCards) {
                Card c = deck.get(i);
                if (c != null && (bufferDeck.get(i).getComponent() == null || !bufferDeck.get(i).getComponent().equals(c))) {
                    bufferDeck.get(i).updateComponent(c);
                    bufferDeck.get(i).setVisible(true);
                }
            } else {
//                System.out.println("More cards in deck that are not displayed");
            }
        }
    }

    private int indexOfCardInHand(Card c, int player) {
        int n = playerHands[player].size();
        for (int i = 0; i < n; i++) {
            PandemicCardView pcv = playerHands[player].get(i);
            if (pcv.getComponent() != null && pcv.getComponent().equals(c)) return i;
        }
        return -1;
    }

    private boolean isCardHighlighted(Card c, int player) {
        if (player >= 0 && player < nPlayers) {
            return handCardHighlights[player].contains(indexOfCardInHand(c, player));
        }
        return false;
    }

}
