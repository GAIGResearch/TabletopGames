package pandemic;

import components.Card;
import components.Deck;
import core.GUI;
import core.Game;
import core.GameState;
import utilities.BoardView;
import utilities.CardView;
import utilities.Hash;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class PandemicGUI extends GUI {
    CardView[] playerCards;
    ArrayList<CardView>[] playerHands;
    Game game;
    GameState gs;
    int nPlayers, activePlayer;

    public PandemicGUI(Game game) {
        JComponent boardView = new BoardView(game.findBoard("Cities"), "data/pandemicBackground.jpg");
        JPanel playerCardsPanel = new JPanel();
        JPanel playerHandPanel = new JPanel();
        this.game = game;
        gs = game.getGameState();
        activePlayer = gs.getActivePlayer();
        nPlayers = game.getPlayers().size();
        playerCards = new CardView[nPlayers];
        playerHands = new ArrayList[nPlayers];

        for (int i = 0; i < nPlayers; i++) {
            gs = game.getGameState();
            Card playerCard = (Card) gs.getAreas().get(i).getComponent(Hash.GetInstance().hash("playerCard"));
            CardView cv = new CardView(playerCard, null);
            playerCards[i] = cv;
            playerCardsPanel.add(cv);

            JPanel hand = new JPanel();
            hand.setLayout(new BoxLayout(hand, BoxLayout.Y_AXIS));
            Deck playerHand = (Deck) gs.getAreas().get(i).getComponent(Hash.GetInstance().hash("playerHand"));
            playerHands[i] = new ArrayList<>();
            for (Card c: playerHand.getCards()) {
                CardView cv2 = new CardView(c, null);
                playerHands[i].add(cv2);
                hand.add(cv2);
            }
            playerHandPanel.add(hand);
        }

        JPanel playerAreas = new JPanel();
        playerAreas.setLayout(new BoxLayout(playerAreas, BoxLayout.Y_AXIS));
        playerAreas.add(playerCardsPanel);
        playerAreas.add(playerHandPanel);

        getContentPane().add(playerAreas, BorderLayout.EAST);
        getContentPane().add(boardView, BorderLayout.CENTER);

        // Frame properties
        pack();
        this.setVisible(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        repaint();
    }

    public void update() {
        for (int i = 0; i < nPlayers; i++) {
            gs = game.getGameState();
            Card playerCard = (Card) gs.getAreas().get(i).getComponent(Hash.GetInstance().hash("playerCard"));
            playerCards[i].update(playerCard);
            playerCards[i].repaint();

            Deck playerHand = (Deck) gs.getAreas().get(i).getComponent(Hash.GetInstance().hash("playerHand"));
            playerHands[i].clear();
            for (int j = 0; j < playerHand.getCards().size(); j++) {
                Card c = playerHand.getCards().get(j);
                if (j < playerHands[i].size()) {
                    playerHands[i].get(j).update(c);
                } else {
                    CardView cv2 = new CardView(c, null);
                    playerHands[i].add(cv2);
                }
            }
        }
    }
}
