package games.dicemonastery.gui;

import games.dicemonastery.DiceMonasteryGameState;

import javax.swing.*;
import java.awt.*;
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

        // We now put these actionSpaceViews into the MainBoard
        // in a 2 x 3 grid
        this.setLayout(new GridLayout(1, 6));
        actionSpaces.forEach(this::add);
    }

    public void update(DiceMonasteryGameState state) {
        actionSpaces.forEach(view -> view.update(state));
        revalidate();
    }
}
