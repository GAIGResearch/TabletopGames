package games.pandemic.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.*;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import core.AbstractGUI;
import core.properties.PropertyString;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicParameters;
import games.pandemic.PandemicTurnOrder;
import games.pandemic.actions.*;
import players.ActionController;
import players.HumanGUIPlayer;
import utilities.Hash;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

import static games.pandemic.PandemicConstants.*;
import static games.pandemic.PandemicGameState.PandemicGamePhase.DiscardReaction;
import static games.pandemic.gui.PandemicCardView.*;
import static javax.swing.ScrollPaneConstants.*;
import static core.CoreConstants.nameHash;
import static core.CoreConstants.playerHandHash;

@SuppressWarnings("rawtypes")
public class PandemicGUI extends AbstractGUI {
    PandemicCardView[] playerCards;
    ArrayList<PandemicCardView>[] playerHands;
    ArrayList<PandemicCardView> bufferDeck;
    PandemicBoardView boardView;

    PandemicGameState gameState;
    int nPlayers;
    int maxCards;
    int maxBufferCards = 10;

    ArrayList<Integer>[] handCardHighlights;
    HashSet<Integer> playerHighlights;
    ArrayList<Integer> bufferHighlights;

    // Game state info
    JLabel gameTurnStep;

    public PandemicGUI(Game game, ActionController ac) {
        super(ac, 721);
        if (game == null || ac == null) return;

        gameState = (PandemicGameState) game.getGameState();
        maxCards = ((PandemicParameters)gameState.getGameParameters()).getMax_cards_per_player() + 2;  // 2 over limit before discard
        nPlayers = gameState.getNPlayers();
        this.gameState = (PandemicGameState) game.getGameState();
        boardView = new PandemicBoardView(gameState);

        handCardHighlights = new ArrayList[nPlayers];
        playerHighlights = new HashSet<>();
        for (int i = 0; i < nPlayers; i++) {
            handCardHighlights[i] = new ArrayList<>();
        }
        Collection[] highlights = new Collection[2+nPlayers];
        highlights[0] = playerHighlights;
        highlights[1] = boardView.getHighlights().keySet();
        System.arraycopy(handCardHighlights, 0, highlights, 2, nPlayers);

        gameTurnStep = new JLabel();
        JPanel gameStateInfo = createGameStateInfoPanel(gameState);
        JPanel playerAreas = createPlayerAreas();
        JPanel counterArea = createCounterArea();
        JComponent actionPanel = createActionPanel(highlights, 300, 80);
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.add(gameStateInfo);
        side.add(playerAreas);
        side.add(actionPanel);

        getContentPane().add(side, BorderLayout.EAST);
        getContentPane().add(boardView, BorderLayout.CENTER);
        getContentPane().add(counterArea, BorderLayout.SOUTH);

        setFrameProperties();
    }

    private JPanel createPlayerAreas() {
        JPanel cardAreas = new JPanel();
        JPanel playerCardsPanel = new JPanel();
        JPanel playerHandPanel = new JPanel();
        playerCards = new PandemicCardView[nPlayers];
        playerHands = new ArrayList[nPlayers];

        for (int i = 0; i < nPlayers; i++) {
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
            playerCardsPanel.add(cv);

            JPanel hand = new JPanel();
            hand.setLayout(new BoxLayout(hand, BoxLayout.Y_AXIS));
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
            scrollPane.setPreferredSize(new Dimension(cardWidth + offset*2, cardHeight *3 + offset));
            scrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
            playerHandPanel.add(scrollPane);
        }

        JPanel playerAreas = new JPanel();
        playerAreas.setLayout(new BoxLayout(playerAreas, BoxLayout.Y_AXIS));
        playerAreas.add(new JLabel("Players"));
        playerAreas.add(playerCardsPanel);
        playerAreas.add(new JLabel("Cards in players' hands"));
        playerAreas.add(playerHandPanel);
        cardAreas.add(playerAreas);

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
        scrollPane.setPreferredSize(new Dimension(cardWidth *3 + offset, cardHeight + offset*2));
        scrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER);
        cardAreas.add(bufferDeckArea);

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
        if (c == null) cv2.setVisible(false);
        else cv2.setVisible(true);
        playerHands[player].add(cv2);
        return cv2;
    }

    protected JPanel createGameStateInfoPanel(AbstractGameState gameState) {

        JPanel gameInfo = new JPanel();
        gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
        gameInfo.add(new JLabel("<html><h1>Pandemic</h1></html>"));

        updateGameStateInfo(gameState);

        gameInfo.add(gameStatus);
        gameInfo.add(playerStatus);
        gameInfo.add(gamePhase);
        gameInfo.add(turnOwner);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);
        gameInfo.add(gameTurnStep);

        gameInfo.setPreferredSize(new Dimension(300, defaultInfoPanelHeight+5));

        JPanel wrapper = new JPanel();
        wrapper.add(gameInfo);
        wrapper.setLayout(new GridBagLayout());
        return wrapper;
    }

    protected void updateGameStateInfo(AbstractGameState gameState) {
        super.updateGameStateInfo(gameState);
        gameTurnStep.setText("Turn step: " + ((PandemicTurnOrder)gameState.getTurnOrder()).getTurnStep());
    }

    private JPanel createCounterArea() {
        JPanel counterArea = new JPanel();

        Counter cnY = (Counter) gameState.getComponent(Hash.GetInstance().hash("Disease yellow"));
        JComponent cY = new PandemicCounterView(cnY, Color.yellow, null);
        counterArea.add(cY);
        Counter cnR = (Counter) gameState.getComponent(Hash.GetInstance().hash("Disease red"));
        JComponent cR = new PandemicCounterView(cnR, Color.red, null);
        counterArea.add(cR);
        Counter cnB = (Counter) gameState.getComponent(Hash.GetInstance().hash("Disease blue"));
        JComponent cB = new PandemicCounterView(cnB, Color.blue, null);
        counterArea.add(cB);
        Counter cnK = (Counter) gameState.getComponent(Hash.GetInstance().hash("Disease black"));
        JComponent cK = new PandemicCounterView(cnK, Color.black, null);
        counterArea.add(cK);

        return counterArea;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState){
        this.gameState = (PandemicGameState) gameState;
        boardView.gameState = this.gameState;
        int activePlayer = gameState.getTurnOrder().getCurrentPlayer(gameState);

        for (int i = 0; i < nPlayers; i++) {
            Card playerCard = (Card) this.gameState.getComponent(PandemicConstants.playerCardHash, i);
            playerCards[i].updateComponent(playerCard);
            playerCards[i].setUsingSecondary(i == activePlayer);
            playerCards[i].repaint();

            Deck<Card> playerHand = (Deck<Card>) this.gameState.getComponent(playerHandHash, i);
            int nCards = playerHand.getSize();
            for (int j = 0; j < nCards; j++) {
                Card c = playerHand.peek(j);
                if (j < playerHands[i].size()) {
                    playerHands[i].get(j).updateComponent(c);
                    playerHands[i].get(j).setVisible(true);
                } else {
                    getCardView(i, c, j);
                }
            }
            for (int j = nCards; j < playerHands[i].size(); j++) {
                playerHands[i].get(j).updateComponent(null);
                if (j == 0) playerHands[i].get(j).setVisible(true);
                else playerHands[i].get(j).setVisible(false);
            }
        }

        // Update actions for human
        if (player instanceof HumanGUIPlayer) {
            updateCardHighlightDisplay();
            updateActionButtons(player, gameState);
        } else {
            // Clear all highlights if it's not human acting
            clearAllHighlights();
            updateCardHighlightDisplay();
        }

        repaint();
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

    protected void clearAllHighlights() {
        playerHighlights.clear();
        for (int i = 0; i < playerCards.length; i++) {
            handCardHighlights[i].clear();
        }
        boardView.getHighlights().clear();
    }

    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        int id = player.getPlayerID();
        List<AbstractAction> actions = gameState.getActions();
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
                int pIdx = ((MovePlayer) action).getPlayerIdx();
                Card c = action.getCard(gameState);
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
                                    isCardHighlighted(action.getCard(gameState), id)) {
                                actionButtons[k].setVisible(true);
                                actionButtons[k++].setButtonAction(action, gameState);
                            }
                        }
                    } else {
                        if (!(action instanceof AddResearchStationWithCard) ||
                                isCardHighlighted(action.getCard(gameState), id)) {
                            actionButtons[k].setVisible(true);
                            actionButtons[k++].setButtonAction(action, gameState);
                        }
                    }
                }
            } else if (action instanceof DoNothing || action instanceof TreatDisease) {
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
            } else if (action instanceof QuietNight) {  // Event
                // QuietNight card in hand selected
                for (int i = 0; i < handCardHighlights[id].size(); i++) {
                    Card c = (Card) playerHands[id].get(i).getComponent();
                    if (c != null) {
                        String name = ((PropertyString)c.getProperty(nameHash)).value;
                        if (name.equals("One quiet night")) {
                            actionButtons[k].setVisible(true);
                            actionButtons[k++].setButtonAction(action, "Play: " + name);
                            break;
                        }
                    }
                }

            } else if (action instanceof RearrangeDeckOfCards) {  // Event
                Card eventCard = action.getCard(gameState);
                int[] cardOrder = ((RearrangeDeckOfCards) action).getNewCardOrder();
                int nCards = cardOrder.length;
                Deck<Card> deckFrom = (Deck<Card>) gameState.getComponentById(((RearrangeDeckOfCards) action).getDeckFrom());

                if (isCardHighlighted(eventCard, id)) {
                    // event card is highlighted

                    // Show top N card of infection discard deck for player to select order
                    for (int i = 0; i < nCards; i++) {
                        Card c = deckFrom.peek();
                        if (c != null && !bufferDeck.get(i).getComponent().equals(c)) {
                            bufferDeck.get(i).updateComponent(c);
                        }
                    }

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
                }

            } else if (action instanceof RemoveComponentFromDeck) {  // Event
                Card eventCard = action.getCard(gameState);
                int infectionCard = ((RemoveComponentFromDeck) action).getComponentIdx();
                Deck<Card> deck = (Deck<Card>) gameState.getComponentById(((RemoveComponentFromDeck) action).getDeck());

                if (isCardHighlighted(eventCard, id)) {
                    // event card in hand selected

                    for (int i = 0; i < deck.getSize(); i++) {
                        if (i < maxBufferCards) {
                            Card c = deck.peek();
                            if (c != null && !bufferDeck.get(i).getComponent().equals(c)) {
                                bufferDeck.get(i).updateComponent(c);
                            }
                        } else {
                            System.out.println("More cards in deck that are not displayed");
                        }
                    }

                    if (bufferHighlights.size() == 1) {
                        int selected = bufferHighlights.get(0);

                        if (infectionCard == selected) {
                            String name = ((PropertyString) eventCard.getProperty(nameHash)).value;
                            actionButtons[k].setVisible(true);
                            actionButtons[k++].setButtonAction(action, "Play: " + name);
                        }
                    }
                }
            } else if (action instanceof DrawCard) {
                if (this.gameState.getGamePhase() == DiscardReaction) {  // Discarding
                    int idx = ((DrawCard) action).getFromIndex();
                    if (handCardHighlights[id].contains(idx)) {
                        Card c = (Card) playerHands[id].get(idx).getComponent();
                        if (c != null) {
                            String name = ((PropertyString)c.getProperty(nameHash)).value;
                            // Action name should be just "Discard" for card selected in hand
                            actionButtons[k].setVisible(true);
                            actionButtons[k++].setButtonAction(action, "Discard: " + name);
                        }
                    }
                } else {
                    if (this.gameState.getPlayerRole(id).equals("Contingency Planner")) {  // Special role
                        if (deckHighlights.contains("playerDiscard")) {
                            Deck<Card> deck = (Deck<Card>) gameState.getComponentById(((DrawCard) action).getDeckFrom());

                            for (int i = 0; i < deck.getSize(); i++) {
                                if (i < maxBufferCards) {
                                    Card c = deck.peek();
                                    if (c != null && !bufferDeck.get(i).getComponent().equals(c)) {
                                        bufferDeck.get(i).updateComponent(c);
                                    }
                                } else {
                                    System.out.println("More cards in deck that are not displayed");
                                }
                            }

                            if (bufferHighlights.size() == 1) {
                                int selected = bufferHighlights.get(0);

                                if (((DrawCard) action).getFromIndex() == selected) {
                                    Card c = (Card) playerHands[id].get(selected).getComponent();
                                    if (c != null) {
                                        String name = ((PropertyString) c.getProperty(nameHash)).value;
                                        actionButtons[k].setVisible(true);
                                        actionButtons[k++].setButtonAction(action, "Choose: " + name);
                                    }
                                }
                            }
                        }
                    } else {
                        int deckId = ((DrawCard) action).getDeckTo();
                        int otherId = gameState.getComponentById(deckId).getOwnerId();
                        if (isCardHighlighted(action.getCard(gameState), id) && playerHighlights.contains(otherId)) {
                            // Give card
                            // card in hand selected and other player, show this action as available
                            actionButtons[k].setVisible(true);
                            actionButtons[k++].setButtonAction(action, gameState);
                        } else if (isCardHighlighted(action.getCard(gameState), otherId)) {
                            //Take card
                            // A card from another player selected
                            actionButtons[k].setVisible(true);
                            actionButtons[k++].setButtonAction(action, gameState);
                        }
                    }
                }
            } else {
                System.out.println("Action type unknown: " + action.toString());
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

//    @Override
//    public Dimension getPreferredSize() {
//        Dimension boardSize = boardView.getPreferredSize();
//        return new Dimension((int)(boardSize.getWidth() + (cardWidth + offset*2)*(nPlayers+1)), (int)boardSize.getHeight());
//    }
}
