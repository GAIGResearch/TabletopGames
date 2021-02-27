package games.dicemonastery.gui;

import games.dicemonastery.DiceMonasteryGameState;

import javax.swing.*;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea;

public class ActionSpaceView extends JComponent {

    ActionArea area;
    public ActionSpaceView(ActionArea area) {
        this.area = area;
    }

    public void update(DiceMonasteryGameState state) {
    }
}
