package games.pandemic.gui;

import gui.GUIMessages;
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
import games.pandemic.PandemicForwardModel;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicParameters;
import games.pandemic.PandemicTurnOrder;
import games.pandemic.actions.*;
import gui.IScreenHighlight;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IMovePlanner;
import gui.MapMove;
import gui.MapRegion;
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

/**
 * GUI for Pandemic. On the desktop, a player picks out cities, pawns and cards on the board and in the hands, and the
 * action buttons offer only the actions that match. Shown in a browser, the cities are a map: the player points at
 * their pawn's city and then at a city to go to (with a menu of the ways there, and what each costs) or at their own
 * city again (to treat, build, share or cure there), and plans the actions of their turn (see
 * {@link PandemicPlanner}), with every action offered. The actions are described in words, and a city's tooltip tells
 * its disease cubes, who is there and who holds its card.
 */
public class PandemicGUIManager extends AbstractGUIManager implements IScreenHighlight {
    PandemicCardView[] playerCards;
    JLabel[][] playerHandCardCounts;
    ArrayList<PandemicCardView>[] playerHands;
    // each player's hand, laid out handColumns cards to a row
    JPanel[] handPanels;
    static final int handColumns = 4, roleCardHeight = 56, sideWidth = 600;
    // shown only while it holds cards
    JScrollPane bufferArea;
    ArrayList<PandemicCardView> bufferDeck;
    PandemicBoardView boardView;
    PandemicCounterView cY, cR, cB, cK;

    PandemicGameState gameState;
    PandemicPlanner planner;
    int nPlayers;
    int maxCards;
    int maxBufferCards = 50;

    ArrayList<Integer>[] handCardHighlights;
    HashSet<Integer> playerHighlights;
    ArrayList<Integer> bufferHighlights;


    public PandemicGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null || ac == null) return;

        this.game = game;
        gameState = (PandemicGameState) game.getGameState();
        maxCards = ((PandemicParameters)gameState.getGameParameters()).getMaxCardsPerPlayer() + 2;  // 2 over limit before discard
        nPlayers = gameState.getNPlayers();
        this.gameState = (PandemicGameState) game.getGameState();
        boardView = new PandemicBoardView(gameState);
        boardView.setToolTips(this::toolTip);
        planner = new PandemicPlanner((PandemicForwardModel) game.getForwardModel());

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

        JPanel gameStateInfo = createGameStateInfoPanel("Pandemic", gameState, sideWidth, defaultInfoPanelHeight);
        JPanel playerAreas = createPlayerAreas();
        JPanel counterArea = createCounterArea();
        JComponent actionPanel = createActionPanel(new IScreenHighlight[]{this}, 300, 80, this::bufferReset);
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{gameStateInfo, playerAreas, actionPanel}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            side.add(c);
        }
        side.add(Box.createVerticalGlue());


        parent.setLayout(new BorderLayout());
        parent.add(side, BorderLayout.EAST);
        parent.add(boardView, BorderLayout.CENTER);
        parent.add(counterArea, BorderLayout.SOUTH);

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
        bufferArea.setVisible(false);
    }

    /**
     * Each player's role card and counts of the cards they hold by colour, and their hand in full, a few cards to a
     * row; then the area that shows the cards of a deck being looked through.
     */
    private JPanel createPlayerAreas() {
        JPanel cardAreas = new JPanel();
        cardAreas.setLayout(new BoxLayout(cardAreas, BoxLayout.Y_AXIS));
        playerCards = new PandemicCardView[nPlayers];
        playerHands = new ArrayList[nPlayers];
        handPanels = new JPanel[nPlayers];
        playerHandCardCounts = new JLabel[nPlayers][];

        for (int i = 0; i < nPlayers; i++) {
            JPanel playerArea = new JPanel();
            playerArea.setLayout(new BoxLayout(playerArea, BoxLayout.Y_AXIS));
            playerArea.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            playerArea.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel name = new JLabel("Player " + i + " [" + game.getPlayers().get(i).toString() + "]");
            name.setFont(name.getFont().deriveFont(Font.BOLD));
            // room to spare: text drawn scaled (as the web server draws the GUI) can come out wider than measured
            name.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
            name.setAlignmentX(Component.LEFT_ALIGNMENT);
            playerArea.add(name);

            Card playerCard = (Card) gameState.getComponent(PandemicConstants.playerCardHash, i);
            PandemicCardView cv = new PandemicCardView(playerCard, cardWidth, roleCardHeight);
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
            JPanel counts = new JPanel();
            counts.setLayout(new BoxLayout(counts, BoxLayout.Y_AXIS));
            JLabel[] countLabels = new JLabel[colors.length / 2 + 1];
            for (int c = 0; c < countLabels.length; c++) {
                countLabels[c] = new JLabel(" ");
                countLabels[c].setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
                counts.add(countLabels[c]);
            }
            playerHandCardCounts[i] = countLabels;
            JPanel role = new JPanel();
            role.setLayout(new BoxLayout(role, BoxLayout.Y_AXIS));
            cv.setAlignmentX(Component.LEFT_ALIGNMENT);
            counts.setAlignmentX(Component.LEFT_ALIGNMENT);
            role.add(cv);
            role.add(counts);
            // wide enough for the counts at their longest, so that the column does not change width as they change
            Dimension roleSize = new Dimension(Math.max(cardWidth + offset,
                    new JLabel("yellow: 00; red: 00; ").getPreferredSize().width + 10), role.getPreferredSize().height);
            role.setPreferredSize(roleSize);
            role.setMinimumSize(roleSize);
            role.setMaximumSize(roleSize);

            handPanels[i] = new JPanel(new GridLayout(0, handColumns, 3, 3));
            playerHands[i] = new ArrayList<>();
            Deck<Card> playerHand = (Deck<Card>) gameState.getComponent(playerHandHash, i);
            for (int k = 0; k < maxCards; k++)
                getCardView(i, k < playerHand.getSize() ? playerHand.peek(k) : null, k);
            layOutHand(i, playerHand.getSize());

            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            role.setAlignmentY(Component.TOP_ALIGNMENT);
            handPanels[i].setAlignmentY(Component.TOP_ALIGNMENT);
            row.add(role);
            row.add(Box.createRigidArea(new Dimension(8, 0)));
            row.add(handPanels[i]);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            playerArea.add(row);
            playerArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, playerArea.getPreferredSize().height));
            cardAreas.add(playerArea);
        }

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

        bufferArea = new JScrollPane(bufferDeckArea);
        Dimension size = new Dimension(sideWidth, cardHeight + offset * 2 + 15);
        bufferArea.setMaximumSize(size);
        bufferArea.setPreferredSize(size);
        bufferArea.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_ALWAYS);
        bufferArea.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER);
        bufferArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        bufferArea.setVisible(false);
        cardAreas.add(bufferArea);

        return cardAreas;
    }

    /**
     * Shows the first nCards views of the player's hand, keeping room for at least two rows of cards so that the
     * layout does not move as the hand changes.
     */
    private void layOutHand(int player, int nCards) {
        JPanel hand = handPanels[player];
        int slots = Math.max(nCards, 2 * handColumns);
        while (playerHands[player].size() < slots) getCardView(player, null, playerHands[player].size());
        if (hand.getComponentCount() == slots) return;
        hand.removeAll();
        for (int k = 0; k < slots; k++) hand.add(playerHands[player].get(k));
        hand.revalidate();
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

    protected void updateGameStateInfo(AbstractGameState gameState) {
        super.updateGameStateInfo(gameState);
        PandemicGameState pgs = (PandemicGameState) gameState;
        turn.setText(turn.getText() + "; Turn step: " + ((PandemicTurnOrder) pgs.getTurnOrder()).getTurnStep());
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

            layOutHand(i, nCards);
            for (int j = 0; j < nCards; j++) {
                Card c = playerHand.peek(j);
                playerHands[i].get(j).updateComponent(c);
                playerHands[i].get(j).setVisible(true);
                if (c.getProperty(colorHash) != null) {
                    colourCount[Utils.indexOf(colors, ((PropertyColor) c.getProperty(colorHash)).valueStr)]++;
                } else {
                    eventCount++;
                }
            }
            for (int j = nCards; j < playerHands[i].size(); j++) {
                playerHands[i].get(j).updateComponent(null);
                playerHands[i].get(j).setVisible(false);
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
                GUIMessages.show(parent, "It's your turn! You are in " + playerLocationName + ". Current game phase: " + this.gameState.getGamePhase());
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
        // a plan's options (see PandemicPlanner) are all offered, to be chosen on the map or from the list
        if (offersInstead()) {
            super.updateActionButtons(player, gameState);
            return;
        }
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
                        actionButtons[k++].setButtonAction(action, actionLabel(action, gameState, id));
                    }
                } else if (bnHighlights.contains(((MovePlayer) action).getDestination()) &&
                        (pIdx == id || playerTokenHighlights.contains(pIdx))) {
                    actionButtons[k].setVisible(true);
                    actionButtons[k++].setButtonAction(action, actionLabel(action, gameState, id));
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
                                actionButtons[k++].setButtonAction(action, actionLabel(action, gameState, id));
                            }
                        }
                    } else {
                        actionButtons[k].setVisible(true);
                        actionButtons[k++].setButtonAction(action, actionLabel(action, gameState, id));
                    }
                }
            } else if (actions.size() > 0 && action instanceof DoNothing || action instanceof TreatDisease) {
                actionButtons[k].setVisible(true);
                actionButtons[k++].setButtonAction(action, actionLabel(action, gameState, id));
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
                    actionButtons[k++].setButtonAction(action, actionLabel(action, gameState, id));
                }
            } else if (action instanceof QuietNight || action instanceof Forecast) {  // Event
                actionButtons[k].setVisible(true);
                actionButtons[k++].setButtonAction(action, actionLabel(action, gameState, id));
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
                        // Action name should be just "Discard" for card selected in hand
                        actionButtons[k].setVisible(true);
                        actionButtons[k++].setButtonAction(action, "Discard: " + cardNote(c, id));
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
                                actionButtons[k++].setButtonAction(action, actionLabel(action, gameState, id));
                            } else if (id == receiverID) {
                                //Take card
                                // A card from another player selected
                                actionButtons[k].setVisible(true);
                                actionButtons[k++].setButtonAction(action, actionLabel(action, gameState, id));
                            }
                        }
                    } else {
                        actionButtons[k].setVisible(true);
                        actionButtons[k++].setButtonAction(action, actionLabel(action, gameState, id));
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
                if (c != null) {
                    if (bufferDeck.get(i).getComponent() == null || !bufferDeck.get(i).getComponent().equals(c))
                        bufferDeck.get(i).updateComponent(c);
                    bufferDeck.get(i).setVisible(true);
                    bufferArea.setVisible(true);
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


    @Override
    public IMovePlanner getPlanner() {
        return planner;
    }

    @Override
    public List<MapRegion> getMapRegions() {
        if (boardView == null) return List.of();
        List<MapRegion> regions = new ArrayList<>();
        boardView.cityAreas().forEach((city, area) -> regions.add(new MapRegion(city, city, boardView, area)));
        return regions;
    }

    /**
     * A move goes from the moving pawn's city to its destination; treating, building, sharing and curing are done in
     * the acting player's city (a research station moved by the Operations Expert goes from where the player is to
     * where it is built).
     */
    @Override
    public MapMove getMapMove(AbstractAction action) {
        if (gameState == null) return null;
        int acting = gameState.getTurnOrder().getCurrentPlayer(gameState);
        String here = location(acting);
        if (action instanceof MovePlayer m) return new MapMove(location(m.getPlayerToMove()), m.getDestination());
        if (action instanceof TreatDisease t) return new MapMove(t.getCity(), t.getCity());
        if (action instanceof AddResearchStation a) return new MapMove(here, a.getCity());
        if (action instanceof CureDisease || action instanceof ShareKnowledge) return new MapMove(here, here);
        return null;
    }

    private String location(int player) {
        return ((PropertyString) gameState.getComponent(playerCardHash, player).getProperty(playerLocationHash)).value;
    }

    private String cardName(Card c) {
        return c == null ? "?" : ((PropertyString) c.getProperty(nameHash)).value;
    }

    private String role(int player) {
        return cardName((Card) gameState.getComponent(playerCardHash, player));
    }

    /**
     * The actions in words: how a move gets there and the card it costs, what a cure or a station costs.
     */
    @Override
    public String actionLabel(AbstractAction action, AbstractGameState state, int acting) {
        PandemicGameState s = (PandemicGameState) state;
        if (action instanceof MovePlayer m) {
            String to = m.getDestination();
            String card = m instanceof MovePlayerWithCard c ? cardName(c.getCard(s)) : null;
            String who = m.getPlayerToMove() == acting ? "" : "Move Player " + m.getPlayerToMove() + ": ";
            return who + switch (m.getMoveType()) {
                case DriveFerry -> "Drive to " + to;
                case DirectFlight -> "Direct flight to " + to + " (discard " + card + ")";
                case CharterFlight -> "Charter flight to " + to + " (discard " + card + ")";
                case ShuttleFlight -> "Shuttle flight to " + to;
                case Airlift -> "Airlift to " + to + (card == null ? "" : " (play " + card + ")");
                case OperationsExpert -> "Fly to " + to + " from a research station (discard " + card + ")";
                case Dispatcher -> "Dispatch to " + to + " (to a city with another pawn)";
            };
        }
        if (action instanceof TreatDisease t)
            return "Treat " + t.getColor() + " in " + t.getCity() + (t.toString().endsWith("(all)") ? " (all cubes)" : "");
        if (action instanceof AddResearchStationWithCardFrom a)
            return "Build a research station in " + a.getCity() + ", moving the one in " + a.getFromCity()
                    + " (discard " + cardName(a.getCard(s)) + ")";
        if (action instanceof AddResearchStationFrom a)
            return "Build a research station in " + a.getCity() + ", moving the one in " + a.getFromCity();
        if (action instanceof AddResearchStationWithCard a)
            return "Build a research station in " + a.getCity() + " (discard " + cardName(a.getCard(s)) + ")";
        if (action instanceof AddResearchStation a)
            return "Build a research station in " + a.getCity();
        if (action instanceof CureDisease c)
            return "Cure " + c.getColor() + " (discard " + c.getCards().size() + " " + c.getColor() + " cards)";
        if (action instanceof ShareKnowledge k) {
            String card = cardName(k.getCard(s));
            return k.getGiver() == acting ? "Give " + card + " to Player " + k.getReceiver() + " (" + role(k.getReceiver()) + ")"
                    : "Take " + card + " from Player " + k.getGiver() + " (" + role(k.getGiver()) + ")";
        }
        return super.actionLabel(action, state, acting);
    }

    /**
     * A card's name and what the player needs it for: a city card's colour, with how many of that colour they hold
     * and a cure needs.
     */
    private String cardNote(Card c, int player) {
        String name = cardName(c);
        if (!(c.getProperty(colorHash) instanceof PropertyColor colour)) return name + " (event)";
        Deck<Card> hand = (Deck<Card>) gameState.getComponent(playerHandHash, player);
        long held = hand.getComponents().stream()
                .filter(x -> x.getProperty(colorHash) instanceof PropertyColor pc && pc.valueStr.equals(colour.valueStr)).count();
        PandemicParameters params = (PandemicParameters) gameState.getGameParameters();
        int needed = params.getnCardsForCure() - (role(player).equals("Scientist") ? params.getnCardsForCureReducedBy() : 0);
        return name + " (" + colour.valueStr + ": you hold " + held + ", a cure needs " + needed + ")";
    }

    /**
     * The city under the mouse: its disease cubes (and whether another would cause an outbreak), its research station,
     * the pawns there, and who holds its card.
     */
    private String toolTip(String city) {
        PandemicGameState s = gameState;
        if (s == null) return null;
        core.components.BoardNode node = s.getWorld().getNodeByStringProperty(nameHash, city);
        if (node == null) return null;
        StringBuilder text = new StringBuilder("<html><b>").append(city).append("</b> (")
                .append(((PropertyColor) node.getProperty(colorHash)).valueStr).append(")");
        int[] cubes = ((core.properties.PropertyIntArray) node.getProperty(infectionHash)).getValues();
        int max = ((PandemicParameters) s.getGameParameters()).getMaxCubesPerCity();
        List<String> diseases = new ArrayList<>();
        for (int c = 0; c < cubes.length; c++)
            if (cubes[c] > 0)
                diseases.add(cubes[c] + " " + colors[c] + (cubes[c] >= max ? " - another means an outbreak" : ""));
        text.append("<br>").append(diseases.isEmpty() ? "No disease" : String.join("<br>", diseases));
        if (((core.properties.PropertyBoolean) node.getProperty(researchStationHash)).value)
            text.append("<br>Research station");
        for (int p : ((core.properties.PropertyIntArrayList) node.getProperty(playersHash)).getValues())
            text.append("<br>Player ").append(p).append(" (").append(role(p)).append(") is here");
        for (int p = 0; p < s.getNPlayers(); p++) {
            Deck<Card> hand = (Deck<Card>) s.getComponent(playerHandHash, p);
            for (Card c : hand.getComponents())
                if (city.equals(cardName(c)))
                    text.append("<br>Player ").append(p).append(" holds its card");
        }
        Deck<Card> discard = (Deck<Card>) s.getComponent(infectionDiscardHash);
        if (discard != null && discard.getComponents().stream().anyMatch(c -> city.equals(cardName(c))))
            text.append("<br>In the infection discard pile: not drawn again until an epidemic");
        return text.append("</html>").toString();
    }
}
