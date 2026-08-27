package games.descent2e.pcg;

import core.components.BoardNode;
import core.components.GraphBoard;
import games.descent2e.concepts.Quest;
import games.descent2e.gui.DescentGridBoardView;
import org.jdesktop.swingx.border.DropShadowBorder;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;

public class ShowMap {
    private Quest quest;
    private GraphBoard board;

    private ShowMAPElite parentElite;
    private ShowFeasibleBoards parentList;

    private final JFrame window;

    private final int maxWidth = 1400;
    private final int maxHeight = 800;
    private final int shadowSize = 10;

    private final String dataPath = "data/descent2e/img/";

    public ShowMap(CreateOffspring co, Quest quest, GraphBoard board, int id, HashMap<String, Float> scores, ShowMAPElite parentElite, ShowFeasibleBoards parentList) {

        this.parentElite = parentElite;
        this.parentList = parentList;

        window = new JFrame(quest.getName());
        window.setSize(maxWidth, maxHeight);
        window.setResizable(false);
        window.setLocationRelativeTo(null);
        window.setBackground(Color.BLACK);

        window.setLayout(new BorderLayout());

        Border blackline = BorderFactory.createLineBorder(Color.black);

        JPanel top = new JPanel();
        top.setBackground(Color.CYAN);
        top.setBorder(blackline);
        top.setLayout(new FlowLayout());
        JLabel nameLabel = new JLabel(quest.getName());
        top.add(nameLabel);
        window.add(top, BorderLayout.PAGE_START);

        JPanel left = new JPanel();
        left.setLayout(new FlowLayout(FlowLayout.CENTER));
        left.setBackground(Color.CYAN);
        left.setPreferredSize(new Dimension(300, 700));

        JPanel fitness = new JPanel();
        fitness.setLayout(new BoxLayout(fitness, BoxLayout.Y_AXIS));
        fitness.setBackground(Color.CYAN);
        TitledBorder fitnessBorder = new TitledBorder(blackline, "Fitness");
        fitnessBorder.setTitleJustification(TitledBorder.CENTER);
        fitness.setBorder(fitnessBorder);
        fitness.setLayout(new BoxLayout(fitness, BoxLayout.Y_AXIS));
        fitness.setPreferredSize(new Dimension(300, 390));
        JLabel fitnessLabel = new JLabel("Score: " + scores.get("Fitness"));

        float size = scores.get("Size");
        float height = scores.get("Height");
        float width = scores.get("Width");
        JLabel boardSize = new JLabel((int) size + " Spaces (" + (int) height + "x" + (int) width + ")");
        fitness.add(fitnessLabel);
        fitness.add(boardSize);

        float totalWeightsModifier = 10f / (5f + co.W_SIZE + co.W_GROUPS + co.W_HEALTH + co.W_HEIGHT + co.W_WIDTH);
        float connected = scores.get("Connectedness");
        JLabel connectedLabel = new JLabel(connected == 1f ? "Fully Connected Layout" : "Disconnected Layout");
        JLabel connectedWeight = new JLabel(Float.toString(connected * totalWeightsModifier));
        float geometry = scores.get("Geometry");
        JLabel geometryLabel = new JLabel(geometry == 1f ? "Valid Geometry" : "Invalid Geometry");
        JLabel geometryWeight = new JLabel(Float.toString(connected * totalWeightsModifier));
        float repeats = scores.get("Repeats");
        JLabel repeatsLabel = new JLabel(repeats == 1f ? "No Repeating Monster Groups" : "Repeating Monster Groups");
        JLabel repeatsWeight = new JLabel(Float.toString(repeats * totalWeightsModifier));
        float spawning = scores.get("Spawning");
        JLabel spawningLabel = new JLabel(spawning == 1f ? "All Legal Spawning Positions" : "Illegal Spawning Positions");
        JLabel spawningWeight = new JLabel(Float.toString(spawning * totalWeightsModifier));
        float consistency = scores.get("Consistency");
        JLabel consistencyLabel = new JLabel(consistency == 1f ? "Consistent Tile Sides" : "Inconsistent Tile Sides");
        JLabel consistencyWeight = new JLabel(Float.toString(consistency * totalWeightsModifier));

        JLabel sizeLabel = new JLabel("Board Size: " + (int) size);
        JLabel sizeWeight = new JLabel(Float.toString(co.W_SIZE * (1f - (Math.abs(co.IDEAL_SIZE - size) / co.IDEAL_SIZE)) * totalWeightsModifier));
        float groups = scores.get("Groups");
        JLabel groupsLabel = new JLabel("Monster Groups: " + (int) groups);
        JLabel groupsWeight = new JLabel(Float.toString(co.W_GROUPS * (1f - (Math.abs(co.IDEAL_GROUP - groups) / co.IDEAL_GROUP)) * totalWeightsModifier));
        float health = scores.get("Health");
        JLabel healthLabel = new JLabel("Average Health: " + health);
        JLabel healthWeight = new JLabel(Float.toString(co.W_HEALTH * (1f - (Math.abs(co.IDEAL_HEALTH - health) / co.IDEAL_HEALTH)) * totalWeightsModifier));
        JLabel heightLabel = new JLabel("Board Height: " + (int) height);
        JLabel heightWeight = new JLabel(Float.toString(co.W_HEIGHT * (1f - (Math.abs(co.IDEAL_HEIGHT - height) / co.IDEAL_HEIGHT)) * totalWeightsModifier));
        JLabel widthLabel = new JLabel("Board Width: " + (int) width);
        JLabel widthWeight = new JLabel(Float.toString(co.W_WIDTH * (1f - (Math.abs(co.IDEAL_WIDTH - width) / co.IDEAL_WIDTH)) * totalWeightsModifier));

        fitness.add(connectedLabel);
        fitness.add(connectedWeight);
        fitness.add(geometryLabel);
        fitness.add(geometryWeight);
        fitness.add(repeatsLabel);
        fitness.add(repeatsWeight);
        fitness.add(spawningLabel);
        fitness.add(spawningWeight);
        fitness.add(consistencyLabel);
        fitness.add(consistencyWeight);
        fitness.add(sizeLabel);
        fitness.add(sizeWeight);
        fitness.add(groupsLabel);
        fitness.add(groupsWeight);
        fitness.add(healthLabel);
        fitness.add(healthWeight);
        fitness.add(heightLabel);
        fitness.add(heightWeight);
        fitness.add(widthLabel);
        fitness.add(widthWeight);

        JPanel tilesPanel = new JPanel();
        tilesPanel.setLayout(new BoxLayout(tilesPanel, BoxLayout.Y_AXIS));
        TitledBorder tilesBorder = new TitledBorder(blackline, "Tiles");
        tilesBorder.setTitleJustification(TitledBorder.CENTER);
        tilesPanel.setBorder(tilesBorder);
        tilesPanel.setBackground(Color.CYAN);
        tilesPanel.setPreferredSize(new Dimension(300, 300));

        Collection<BoardNode> nodes = board.getBoardNodes();
        JLabel tileCount = new JLabel(nodes.size() + " Tiles Used");
        tilesPanel.add(tileCount);

        for (BoardNode node : nodes) {
            JLabel nodeName = new JLabel(node.getComponentName());
            tilesPanel.add(nodeName);
        }

        left.add(fitness);
        left.add(tilesPanel);
        window.add(left, BorderLayout.LINE_START);

        JPanel viewhold = new JPanel();
        viewhold.setBackground(Color.BLACK);

        DescentGridBoardView view = new DescentGridBoardView(co.boards.get(id), co.boardTiles.get(id), co.gridRefs.get(id), co.tileRefs.get(id), dataPath, shadowSize, maxWidth/2 + 100, maxHeight/2 + 100);
        view.setBackground(Color.BLACK);
        DropShadowBorder shadow = new DropShadowBorder(Color.black, shadowSize, 0.9f, 12, true, true, true, true);
        view.setBorder(shadow);

        viewhold.add(view);
        window.add(viewhold, BorderLayout.CENTER);

        JPanel positions = new JPanel();
        positions.setBackground(Color.CYAN);
        positions.setLayout(new BorderLayout());

        positions.setPreferredSize(new Dimension(300, 700));

        JPanel traits = new JPanel();
        traits.setLayout(new BoxLayout(traits, BoxLayout.Y_AXIS));
        traits.setBackground(Color.CYAN);
        TitledBorder traitsText = new TitledBorder(blackline, "Traits");
        traitsText.setTitleJustification(TitledBorder.CENTER);
        traits.setBorder(traitsText);
        for (String trait: quest.getMonsterTraits()) {
            JLabel t = new JLabel(trait);
            traits.add(t);
        }

        positions.add(traits, BorderLayout.PAGE_START);

        JPanel spawningContainer = new JPanel();
        spawningContainer.setBackground(Color.CYAN);
        spawningContainer.setLayout(new BoxLayout(spawningContainer, BoxLayout.Y_AXIS));
        TitledBorder positionText = new TitledBorder(blackline,"Starting Positions:");
        positionText.setTitleJustification(TitledBorder.CENTER);
        spawningContainer.setBorder(positionText);
        JLabel heroSpawn = new JLabel("Heroes: " + quest.getStartingTile());
        spawningContainer.add(heroSpawn);

        for (String[] monster : quest.getMonsters()) {
            String m = monster[0].split(":")[0];
            String pos = monster[1];

            if (m.equals("OpenSmall"))
                m = "Open Group (Small)";
            else if (m.equals("Open"))
                m += " Group";

            JLabel monsterSpawn = new JLabel(m + ": " + pos);
            spawningContainer.add(monsterSpawn);
        }

        positions.add(spawningContainer, BorderLayout.CENTER);

        JButton save = new JButton("Save");
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                save.setText("Saved!");
                save.setEnabled(false);
            }
        });

        JButton exit = new JButton("Exit");
        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                window.dispose();
                if (parentElite != null)
                    parentElite.show();
                if (parentList != null)
                    parentList.show();
            }
        });
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(1, 2, 5, 5));
        buttons.add(save);
        buttons.add(exit);
        positions.add(buttons, BorderLayout.PAGE_END);

        window.add(positions, BorderLayout.LINE_END);
    }

    public void show() {
        window.setVisible(true);
    }

    public void hide() {
        window.setVisible(false);
    }
}
