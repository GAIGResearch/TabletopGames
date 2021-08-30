package games.dicemonastery.gui;

import games.dicemonastery.DiceMonasteryConstants;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.components.Monk;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.MEADOW;
import static games.dicemonastery.DiceMonasteryConstants.Resource.GRAIN;
import static games.dicemonastery.DiceMonasteryConstants.Resource.SKEP;
import static java.util.stream.Collectors.toList;

public class ActionSpaceView extends JComponent {

    ActionArea area;
    JPanel meepleArea, wheatArea, skepArea;
    JTextArea tokenArea;
    List<Monk> currentlyDisplayedMonks = new ArrayList<>();
    int[] currentWheatLevels = new int[DiceMonasteryConstants.playerColours.length];
    int[] currentSkepLevels = new int[DiceMonasteryConstants.playerColours.length];
    List<DiceMonasteryConstants.BONUS_TOKEN> bonusTokens = new ArrayList<>();

    public ActionSpaceView(ActionArea area) {
        this.area = area;
        this.setPreferredSize(new Dimension(210, 250));

        // Now need to add graphical components
        this.setLayout(new BorderLayout());
        this.add(new JLabel(area.toString(), SwingConstants.CENTER), BorderLayout.NORTH);
        meepleArea = new JPanel(new FlowLayout(FlowLayout.CENTER));
        this.add(meepleArea, BorderLayout.CENTER);
        if (area == MEADOW) {
            wheatArea = new JPanel(new FlowLayout(FlowLayout.LEFT));
            wheatArea.add(new JLabel("WHEAT "));
            this.add(wheatArea, BorderLayout.WEST);
            skepArea = new JPanel(new FlowLayout(FlowLayout.LEFT));
            skepArea.add(new JLabel("SKEPS "));
            this.add(skepArea, BorderLayout.EAST);
        }
        tokenArea = new JTextArea(2, 1);
        this.add(tokenArea, BorderLayout.SOUTH);
        bonusTokens.add(null);
        bonusTokens.add(null);
    }

    public void update(DiceMonasteryGameState state) {
        List<Monk> monksPresent = state.monksIn(area, -1);
        List<Monk> removedMonks = currentlyDisplayedMonks.stream().filter(m -> !monksPresent.contains(m)).collect(toList());
        currentlyDisplayedMonks.removeAll(removedMonks);
        for (Monk m : removedMonks) {
            Component mView = Arrays.stream(meepleArea.getComponents())
                    .filter(c -> c instanceof MonkView && ((MonkView) c).monk.equals(m))
                    .findFirst().orElseThrow(() -> new AssertionError("Monk not found"));
            meepleArea.remove(mView);
        }

        List<Monk> newMonks = monksPresent.stream().filter(m -> !currentlyDisplayedMonks.contains(m)).collect(toList());
        currentlyDisplayedMonks.addAll(newMonks);
        newMonks.forEach(m -> meepleArea.add(new MonkView(m)));


        for (int i = 0; i < 2; i++) {
            DiceMonasteryConstants.BONUS_TOKEN current = state.availableBonusTokens(area).size() <= i ? null : state.availableBonusTokens(area).get(i);
            if (current != bonusTokens.get(i)) {
                bonusTokens.add(i, null);
                if (current != null)
                    bonusTokens.add(i, current);
                StringBuilder newString = new StringBuilder();
                if (bonusTokens.get(0) != null)
                    newString.append(bonusTokens.get(0).name()).append("\n");
                if (bonusTokens.get(1) != null)
                    newString.append(bonusTokens.get(1).name());
                tokenArea.setText(newString.toString());
            }
        }

        if (area == MEADOW) {
            wheatArea.setPreferredSize(new Dimension(60, 250));
            skepArea.setPreferredSize(new Dimension(60, 250));
            meepleArea.setPreferredSize(new Dimension(90, 250));
            // Also need to display the Wheat/Skep levels
            // we get a Map from player to Integer for each
            int[] wheat = IntStream.range(0, state.getNPlayers())
                    .map(p -> state.getResource(p, GRAIN, MEADOW)).toArray();
            int[] skeps = IntStream.range(0, state.getNPlayers())
                    .map(p -> state.getResource(p, SKEP, MEADOW)).toArray();
            if (!Arrays.equals(wheat, currentWheatLevels)) {
                // redraw everything
         //       System.out.printf("Wheat: %s%n", Arrays.toString(wheat));
                wheatArea.removeAll();
                wheatArea.add(new JLabel("WHEAT "));
                currentWheatLevels = wheat;
                IntStream.range(0, wheat.length).forEach(player ->
                        IntStream.range(0, wheat[player]).forEach(i -> wheatArea.add(new CubeView(player))));
            }
            if (!Arrays.equals(skeps, currentSkepLevels)) {
                // redraw everything
                skepArea.removeAll();
                skepArea.add(new JLabel("SKEPS "));
                currentSkepLevels = skeps;
                IntStream.range(0, skeps.length).forEach(player ->
                        IntStream.range(0, skeps[player]).forEach(i -> skepArea.add(new CubeView(player))));
            }
        }
    }
}
