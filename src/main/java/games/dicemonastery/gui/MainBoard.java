package games.dicemonastery.gui;

import games.dicemonastery.DiceMonasteryGameState;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static java.util.stream.Collectors.toList;

public class MainBoard extends JComponent {

    ActionArea[] actionAreas = {MEADOW, KITCHEN, WORKSHOP, GATEHOUSE, LIBRARY, CHAPEL};
    List<ActionSpaceView> actionSpaces;

    public MainBoard() {
        actionSpaces = Arrays.stream(actionAreas).map(ActionSpaceView::new).collect(toList());
    }

    public void update(DiceMonasteryGameState state) {
        actionSpaces.forEach(view -> view.update(state));
    }
}
