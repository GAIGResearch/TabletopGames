package games.dicemonastery.gui;

import core.components.Token;
import games.dicemonastery.DiceMonasteryConstants;
import gui.views.ComponentView;

import java.awt.*;

public class CubeView extends ComponentView {

    private static Token dummy = new Token("dummy");
    int player;

    public CubeView(int playerID) {
        super(dummy, 10, 10);
        player = playerID;
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(DiceMonasteryConstants.playerColours[player]);
        g.fillRect(3, 3, 5, 5);
    }
}
