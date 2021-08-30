package games.dicemonastery.gui;

import games.dicemonastery.DiceMonasteryConstants;
import games.dicemonastery.components.Monk;
import gui.views.ComponentView;

import java.awt.*;

public class MonkView extends ComponentView {

    private static int[] xPoints = {2, 13, 9, 10, 10, 5, 5, 6};
    private static int[] yPoints = {12, 12, 7, 7, 2, 2, 7, 7};
    private static int nPoints = 8;

    Monk monk;

    public MonkView(Monk m) {
        super(m, 30, 15);
        monk = m;
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(DiceMonasteryConstants.playerColours[monk.getOwnerId()]);
        g.fillPolygon(xPoints, yPoints, nPoints);
        g.setColor(Color.BLACK);
        g.drawString(Integer.toString(monk.getPiety()), 15, 10);
    }
}
