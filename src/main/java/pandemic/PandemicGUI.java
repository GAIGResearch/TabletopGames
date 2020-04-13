package pandemic;

import components.Card;
import components.Counter;
import components.Deck;
import core.GUI;
import core.Game;
import core.GameState;
import utilities.CounterView;
import utilities.Hash;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class PandemicGUI extends GUI {
    PandemicCardView[] playerCards;
    ArrayList<PandemicCardView>[] playerHands;
    JComponent boardView;

    Game game;
    GameState gs;
    int nPlayers, activePlayer;
    int maxCards = 7;

    public PandemicGUI(Game game) {
        this.game = game;
        gs = game.getGameState();
        nPlayers = game.getPlayers().size();
        activePlayer = gs.getActivePlayer();

        boardView = new PandemicBoardView(game, "data/pandemicBackground.jpg");
        JPanel playerAreas = createPlayerAreas();
        JPanel counterArea = createCounterArea();

        getContentPane().add(playerAreas, BorderLayout.EAST);
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
            gs = game.getGameState();
            Card playerCard = (Card) gs.getAreas().get(i).getComponent(Hash.GetInstance().hash("playerCard"));
            PandemicCardView cv = new PandemicCardView(playerCard, null);
            playerCards[i] = cv;
            playerCardsPanel.add(cv);

            JPanel hand = new JPanel();
            hand.setLayout(new BoxLayout(hand, BoxLayout.Y_AXIS));
            Deck playerHand = (Deck) gs.getAreas().get(i).getComponent(Hash.GetInstance().hash("playerHand"));
            playerHands[i] = new ArrayList<>();
            for (int k = 0; k < maxCards; k++) {
                Card c = null;
                if (k < playerHand.getCards().size()) {
                    c = playerHand.getCards().get(k);
                }
                PandemicCardView cv2 = new PandemicCardView(c, null);
                playerHands[i].add(cv2);
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

    private JPanel createCounterArea() {
        JPanel counterArea = new JPanel();

        Counter cnY = game.getGameState().findCounter("Disease yellow");
        JComponent cY = new CounterView(cnY, Color.yellow, null);
        counterArea.add(cY);
        Counter cnR = game.getGameState().findCounter("Disease red");
        JComponent cR = new CounterView(cnR, Color.red, null);
        counterArea.add(cR);
        Counter cnB = game.getGameState().findCounter("Disease blue");
        JComponent cB = new CounterView(cnB, Color.blue, null);
        counterArea.add(cB);
        Counter cnK = game.getGameState().findCounter("Disease black");
        JComponent cK = new CounterView(cnK, Color.black, null);
        counterArea.add(cK);

        return counterArea;
    }

    public void update() {
        for (int i = 0; i < nPlayers; i++) {
            gs = game.getGameState();
            Card playerCard = (Card) gs.getAreas().get(i).getComponent(Hash.GetInstance().hash("playerCard"));
            playerCards[i].update(playerCard);
            playerCards[i].repaint();

            Deck playerHand = (Deck) gs.getAreas().get(i).getComponent(Hash.GetInstance().hash("playerHand"));
//            playerHands[i].clear();
            for (int j = 0; j < playerHand.getCards().size(); j++) {
                Card c = playerHand.getCards().get(j);
                if (j < playerHands[i].size()) {
                    playerHands[i].get(j).update(c);
                } else {
                    PandemicCardView cv2 = new PandemicCardView(c, null);
                    playerHands[i].add(cv2);
                }
            }
        }
        repaint();
    }
}
