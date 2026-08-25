package games.descent2e.pcg;

import core.components.GraphBoard;
import games.descent2e.concepts.Quest;
import games.descent2e.gui.DescentGridBoardView;
import org.jdesktop.swingx.border.DropShadowBorder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

    public ShowMap(CreateOffspring co, Quest quest, GraphBoard board, int id, float fit, ShowMAPElite parentElite, ShowFeasibleBoards parentList) {

        this.parentElite = parentElite;
        this.parentList = parentList;

        window = new JFrame(quest.getName());
        window.setSize(maxWidth, maxHeight);
        window.setResizable(false);
        window.setLocationRelativeTo(null);
        window.setBackground(Color.BLACK);

        window.setLayout(new BorderLayout());

        JPanel top = new JPanel();
        top.setLayout(new GridLayout(2, 1, 5, 5));
        JLabel nameLabel = new JLabel(quest.getName());
        JLabel fitnessLabel = new JLabel("Fitness: " +fit);
        top.add(nameLabel);
        top.add(fitnessLabel);
        window.add(top, BorderLayout.PAGE_START);

        JPanel viewhold = new JPanel();
        viewhold.setBackground(Color.BLACK);

        DescentGridBoardView view = new DescentGridBoardView(co.boards.get(id), co.boardTiles.get(id), co.gridRefs.get(id), co.tileRefs.get(id), dataPath, shadowSize, maxWidth/2 + 100, maxHeight/2 + 100);
        view.setBackground(Color.BLACK);
        DropShadowBorder shadow = new DropShadowBorder(Color.black, shadowSize, 0.9f, 12, true, true, true, true);
        view.setBorder(shadow);

        viewhold.add(view);
        window.add(viewhold, BorderLayout.CENTER);

        JPanel positions = new JPanel();
        positions.setLayout(new BorderLayout());

        positions.setPreferredSize(new Dimension(200, 700));

        JPanel traits = new JPanel();
        traits.setLayout(new BoxLayout(traits, BoxLayout.Y_AXIS));
        JLabel traitsText = new JLabel("Traits:");
        traits.add(traitsText);
        for (String trait: quest.getMonsterTraits()) {
            JLabel t = new JLabel(trait);
            traits.add(t);
        }

        positions.add(traits, BorderLayout.PAGE_START);

        JPanel spawning = new JPanel();
        spawning.setLayout(new BoxLayout(spawning, BoxLayout.Y_AXIS));
        JLabel positionText = new JLabel("Starting Positions:");
        spawning.add(positionText);
        JLabel heroSpawn = new JLabel("Heroes: " + quest.getStartingTile());
        spawning.add(heroSpawn);

        for (String[] monster : quest.getMonsters()) {
            String m = monster[0].split(":")[0];
            String pos = monster[1];

            if (m.equals("OpenSmall"))
                m = "Open Group (Small)";
            else if (m.equals("Open"))
                m += "Group";

            JLabel monsterSpawn = new JLabel(m + ": " + pos);
            spawning.add(monsterSpawn);
        }

        positions.add(spawning, BorderLayout.CENTER);

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
