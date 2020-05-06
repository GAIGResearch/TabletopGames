package games.pandemic.gui;

import core.AbstractGameState;
import core.actions.*;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import core.GUI;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import games.pandemic.actions.*;
import players.AbstractPlayer;
import players.ActionController;
import players.HumanGUIPlayer;
import utilities.CounterView;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

@SuppressWarnings("rawtypes")
public class PandemicGUI extends GUI {
    PandemicCardView[] playerCards;
    ArrayList<PandemicCardView>[] playerHands;
    PandemicBoardView boardView;

    PandemicGameState gameState;
    int nPlayers, activePlayer;
    int maxCards = 9; // can go 2 over limit before discarding

    ArrayList<Integer>[] handCardHighlights;
    HashSet<Integer> playerHighlights;

    public PandemicGUI(PandemicGameState gameState, ActionController ac) {
        super(ac, 721);

        nPlayers = gameState.getNPlayers();
        activePlayer = gameState.getTurnOrder().getCurrentPlayer(gameState);
        this.gameState = gameState;
        boardView = new PandemicBoardView(gameState, "data/pandemicBackground.jpg");

        handCardHighlights = new ArrayList[nPlayers];
        playerHighlights = new HashSet<>();
        for (int i = 0; i < nPlayers; i++) {
            handCardHighlights[i] = new ArrayList<>();
        }
        Collection[] highlights = new Collection[2+nPlayers];
        highlights[0] = playerHighlights;
        highlights[1] = boardView.getHighlights().keySet();
        System.arraycopy(handCardHighlights, 0, highlights, 2, nPlayers);

        JPanel playerAreas = createPlayerAreas();
        JPanel counterArea = createCounterArea();
        JComponent actionPanel = createActionPanel(highlights, 300, 200);
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.add(playerAreas);
        side.add(actionPanel);

        getContentPane().add(side, BorderLayout.EAST);
        getContentPane().add(boardView, BorderLayout.CENTER);
        getContentPane().add(counterArea, BorderLayout.SOUTH);

        // Frame properties
        pack();
        this.setVisible(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        repaint();
    }

    private JPanel createPlayerAreas() {
        JPanel playerCardsPanel = new JPanel();
        JPanel playerHandPanel = new JPanel();
        playerCards = new PandemicCardView[nPlayers];
        playerHands = new ArrayList[nPlayers];

        for (int i = 0; i < nPlayers; i++) {
            Card playerCard = (Card) gameState.getComponent(PandemicConstants.playerCardHash, i);
            PandemicCardView cv = new PandemicCardView(playerCard, null);
            int player = i;
            cv.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        playerHighlights.add(player);
                        cv.setBorder(new LineBorder(Color.cyan, 2));
                    } else {
                        playerHighlights.add(player);
                        cv.setBorder(null);
                    }
                }
            });
            playerCards[i] = cv;
            playerCardsPanel.add(cv);

            JPanel hand = new JPanel();
            hand.setLayout(new BoxLayout(hand, BoxLayout.Y_AXIS));
            Deck<Card> playerHand = (Deck<Card>) gameState.getComponent(PandemicConstants.playerHandHash, i);
            playerHands[i] = new ArrayList<>();
            for (int k = 0; k < maxCards; k++) {
                Card c = null;
                if (k < playerHand.getElements().size()) {
                    c = playerHand.peek(k);
                }
                PandemicCardView cv2 = getCardView(i, c, k);
                hand.add(cv2);
            }
            playerHandPanel.add(hand);
        }

        JPanel playerAreas = new JPanel();
        playerAreas.setLayout(new BoxLayout(playerAreas, BoxLayout.Y_AXIS));
        playerAreas.add(playerCardsPanel);
        playerAreas.add(playerHandPanel);

        return playerAreas;
    }

    private PandemicCardView getCardView(int player, Card c, int cardIdx) {
        PandemicCardView cv2 = new PandemicCardView(c, null);
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
        playerHands[player].add(cv2);
        return cv2;
    }

    private JPanel createCounterArea() {
        JPanel counterArea = new JPanel();

        Counter cnY = gameState.getData().findCounter("Disease yellow");
        JComponent cY = new CounterView(cnY, Color.yellow, null);
        counterArea.add(cY);
        Counter cnR = gameState.getData().findCounter("Disease red");
        JComponent cR = new CounterView(cnR, Color.red, null);
        counterArea.add(cR);
        Counter cnB = gameState.getData().findCounter("Disease blue");
        JComponent cB = new CounterView(cnB, Color.blue, null);
        counterArea.add(cB);
        Counter cnK = gameState.getData().findCounter("Disease black");
        JComponent cK = new CounterView(cnK, Color.black, null);
        counterArea.add(cK);

        return counterArea;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState){
        // TODO: message informing of reaction, game phase

        this.gameState = (PandemicGameState) gameState;
        boardView.gameState = this.gameState;
        for (int i = 0; i < nPlayers; i++) {
            Card playerCard = (Card) this.gameState.getComponent(PandemicConstants.playerCardHash, i);
            playerCards[i].updateCard(playerCard);
            playerCards[i].repaint();

            Deck<Card> playerHand = (Deck<Card>) this.gameState.getComponent(PandemicConstants.playerHandHash, i);
            int nCards = playerHand.getElements().size();
            for (int j = 0; j < nCards; j++) {
                Card c = playerHand.peek(j);
                if (j < playerHands[i].size()) {
                    playerHands[i].get(j).updateCard(c);
                } else {
                    getCardView(i, c, j);
                }
            }
            for (int j = nCards; j < playerHands[i].size(); j++) {
                playerHands[i].get(j).updateCard(null);
            }
        }

        // Update actions for human
        if (player instanceof HumanGUIPlayer) {
            updateActionButtons(player, gameState);
        }

        repaint();
    }

    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        int id = player.getPlayerID();
        List<IAction> actions = gameState.getActions();
        int k = 0;

        Set<String> highlights = boardView.getHighlights().keySet();
        Set<Integer> playerTokenHighlights = new HashSet<>();
        Set<String> bnHighlights = new HashSet<>();
        Set<String> deckHighlights = new HashSet<>();
        for (String highlight: highlights) {
            if (highlight.contains("BN")) {
                bnHighlights.add(highlight.split(" ")[1]);
            } else if (highlight.contains("player ")) {
                playerTokenHighlights.add(Integer.parseInt(highlight.split(" ")[1]));
            } else {
                deckHighlights.add(highlight);
            }
        }
        for (IAction action : actions) {
            if (action instanceof MovePlayer) {
                int pIdx = ((MovePlayer) action).getPlayerIdx();
                if (bnHighlights.contains(((MovePlayer) action).getDestination()) &&
                        (pIdx == id || playerTokenHighlights.contains(pIdx))) {
                    if (!(action instanceof MovePlayerWithCard) ||
                            handCardHighlights[pIdx].contains(indexOfCardInHand(((MovePlayerWithCard) action).getCard(), pIdx))) {
                        actionButtons[k].setVisible(true);
                        actionButtons[k++].setButtonAction(action);
                    }
                }
            } else if (action instanceof AddResearchStation && bnHighlights.contains(((AddResearchStation) action).getToCity())) {
                if (action instanceof AddResearchStationFrom) {
                    if (bnHighlights.contains(((AddResearchStationFrom) action).getFromCity())) {
                        if (!(action instanceof AddResearchStationWithCardFrom) ||
                                handCardHighlights[id].contains(indexOfCardInHand(((AddResearchStationWithCardFrom) action).getCard(), id))) {
                            actionButtons[k].setVisible(true);
                            actionButtons[k++].setButtonAction(action);
                        }
                    }
                } else {
                    if (!(action instanceof AddResearchStationWithCard) ||
                            handCardHighlights[id].contains(indexOfCardInHand(((AddResearchStationWithCard) action).getCard(), id))) {
                        actionButtons[k].setVisible(true);
                        actionButtons[k++].setButtonAction(action);
                    }
                }
            } else if (action instanceof DoNothing || action instanceof DrawCard || action instanceof QuietNight
                    || action instanceof TreatDisease) {
                // TODO click player discard deck -> (display all cards, contingency planner selects card to add to
                //  planner deck, other actions related to planner deck ignored)
                actionButtons[k].setVisible(true);
                actionButtons[k++].setButtonAction(action);
            } else if (action instanceof RearrangeCardsWithCard) {
                // TODO: display top N cards of infection deck, player selects order
            } else if (action instanceof CureDisease) {
                ArrayList<Card> cards = ((CureDisease) action).getCards();
                boolean allSelected = true;
                for (Card c: cards) {
                    if (indexOfCardInHand(c, id) != -1) {
                        allSelected = false;
                        break;
                    }
                }
                if (allSelected) {
                    actionButtons[k].setVisible(true);
                    actionButtons[k++].setButtonAction(action);
                }
            } else if (action instanceof GiveCard) {
                // TODO: a card in hand selected, and another player
            } else if (action instanceof TakeCard) {
                // TODO: a card from another player selected
            } else if (action instanceof RemoveCardWithCard) {
                // TODO: RP card in hand selected? + resilient population action possible,
                //  show infection discard cards from which player must select 1
            }
        }
        // TODO: event actions if available

//            for (int i = 0; i < actions.size(); i++) {
//                actionButtons[i].setVisible(true);
//                actionButtons[i].setButtonAction(actions.get(i));
//            }
    }

    private int indexOfCardInHand(Card c, int player) {
        int n = playerHands[player].size();
        for (int i = 0; i < n; i++) {
            PandemicCardView pcv = playerHands[player].get(i);
            if (pcv.getCard().equals(c)) return i;
        }
        return -1;
    }
}
