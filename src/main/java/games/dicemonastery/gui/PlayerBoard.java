package games.dicemonastery.gui;

import games.dicemonastery.DiceMonasteryGameState;

import javax.swing.*;

public class PlayerBoard extends JComponent {

    int player;
    public PlayerBoard(int playerID) {
        player = playerID;
    }

    public void update(DiceMonasteryGameState state) {
    }
}
