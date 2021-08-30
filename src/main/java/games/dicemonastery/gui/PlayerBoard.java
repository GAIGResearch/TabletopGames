package games.dicemonastery.gui;

import games.dicemonastery.*;
import games.dicemonastery.components.Monk;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.DORMITORY;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.STOREROOM;
import static java.util.stream.Collectors.toList;

public class PlayerBoard extends JComponent {

    static Resource[] allResources = Resource.values();
    int player;
    JPanel dormitory;
    JTable storeroom;
    String[] columnNames = {"Resource", "Amount"};
    Object[][] rowData;
    List<Monk> currentlyDisplayedMonks;
    JScrollPane scrollPane;


    public PlayerBoard(int playerID) {
        player = playerID;
        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(350, 200));
        dormitory = new JPanel(new FlowLayout(FlowLayout.CENTER));
        dormitory.setPreferredSize(new Dimension(100, 200));
        currentlyDisplayedMonks = new ArrayList<>();

        rowData = new Object[allResources.length][2];
        for (int i = 0; i < allResources.length; i++) {
            rowData[i][0] = allResources[i].name();
            rowData[i][1] = 0;
        }
        storeroom = new JTable(rowData, columnNames);
        scrollPane = new JScrollPane(storeroom);
        scrollPane.setPreferredSize(new Dimension(220, 220));
        this.add(dormitory, BorderLayout.WEST);
        this.add(scrollPane, BorderLayout.EAST);
    }

    public void update(DiceMonasteryGameState state) {
        List<Monk> monksPresent = state.monksIn(DORMITORY, player);
        List<Monk> removedMonks = currentlyDisplayedMonks.stream().filter(m -> !monksPresent.contains(m)).collect(toList());
        currentlyDisplayedMonks.removeAll(removedMonks);
        for (Monk m : removedMonks) {
            Component mView = Arrays.stream(dormitory.getComponents())
                    .filter(c -> c instanceof MonkView && ((MonkView) c).monk.equals(m))
                    .findFirst().orElseThrow(() -> new AssertionError("Monk not found"));
            dormitory.remove(mView);
        }

        List<Monk> newMonks = monksPresent.stream().filter(m -> !currentlyDisplayedMonks.contains(m)).collect(toList());
        currentlyDisplayedMonks.addAll(newMonks);
        newMonks.forEach(m -> dormitory.add(new MonkView(m)));

        for (int i = 0; i < allResources.length; i++) {
            rowData[i][1] = state.getResource(player, allResources[i], STOREROOM);
        }
    }
}
