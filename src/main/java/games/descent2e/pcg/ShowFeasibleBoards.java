package games.descent2e.pcg;

import core.components.BoardNode;
import core.components.GraphBoard;
import games.descent2e.concepts.Quest;
import utilities.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ShowFeasibleBoards {
    private final JFrame window;
    private JPanel panel;
    private List<Pair<HashMap<String, Float>, Pair<Quest, GraphBoard>>> feasibleList = new ArrayList<>();
    private CreateOffspring co;

    public ShowFeasibleBoards(CreateOffspring co) {
        this.co = co;
        window = new JFrame();
        window.setTitle("Feasible Procedurally Generated Descent Quests");
        window.setSize(1700, 700);
        window.setResizable(false);
        window.setLocationRelativeTo(null);
        window.setLayout(new GridBagLayout());

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.CYAN);

        JScrollPane feasibleScroll = new JScrollPane(panel);
        feasibleScroll.setPreferredSize(new Dimension(1600, 700));
        window.add(feasibleScroll);
    }

    public void show() {
        window.setVisible(true);
    }

    public void hide() {
        window.setVisible(false);
    }

    public void prepare(List<Pair<Quest, GraphBoard>> feasible, List<HashMap<String, Float>> fitness) {
        feasibleList.clear();
        for (HashMap<String, Float> f : fitness) {
            int id = f.get("ID").intValue();
            Pair<Quest, GraphBoard> quest = getQuest(id, feasible);
            float fit = f.get("Fitness");

            Pair<HashMap<String, Float>, Pair<Quest, GraphBoard>> result = new Pair<>(f, quest);
            if (feasibleList.isEmpty()) {
                feasibleList.add(result);
            }
            else {
                int i;
                for (i = 0; i < feasibleList.size(); i++) {
                    if (fit > feasibleList.get(i).a.get("Fitness"))
                        break;
                }
                feasibleList.add(i, result);
            }
        }

        ShowFeasibleBoards parent = this;

        for (Pair<HashMap<String, Float>, Pair<Quest, GraphBoard>> quest : feasibleList) {
            Quest q = quest.b.a;
            GraphBoard board = quest.b.b;
            String name = q.getName();
            int id = Integer.parseInt(name.split("-")[1]);
            HashMap<String, Float> scores = quest.a;
            float f = scores.get("Fitness");
            JButton button = new JButton(name + " - Fitness: " + f);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ShowMap map = new ShowMap(co, q, board, id, f, null, parent);
                    map.show();
                    hide();
                }
            });

            String monsterList = "";
            for (String[] monster : q.getMonsters()) {
                monsterList += monster[0].split(":")[0] + ", ";
            }
            String traitsList = "";
            for (String trait : q.getMonsterTraits()) {
                traitsList += trait + ", ";
            }

            JLabel tiles = new JLabel("Tiles Count: " + board.getBoardNodes().size() + ", ");
            JLabel size = new JLabel("Size: " + scores.get("Size").intValue() + " (" + scores.get("Height").intValue() + "x" + scores.get("Width").intValue() + "), ");
            JLabel traits = new JLabel("Traits: " + traitsList);
            JLabel groups = new JLabel("Groups: " + scores.get("Groups") + ", ");
            JLabel monsters = new JLabel("Monsters: " + monsterList);
            JLabel health = new JLabel("Total Health: " + (int) Math.ceil(scores.get("Total Health")));
            JPanel container = new JPanel();
            container.setLayout(new FlowLayout(FlowLayout.LEFT));
            container.add(button);
            container.add(tiles);
            container.add(size);
            container.add(traits);
            container.add(groups);
            container.add(monsters);
            container.add(health);
            panel.add(container);
        }
        panel.revalidate();
        panel.repaint();
    }

    Pair<Quest, GraphBoard> getQuest (int target, List<Pair<Quest, GraphBoard>> feasible) {
        for (Pair<Quest, GraphBoard> quest : feasible) {
            int id = Integer.parseInt(quest.a.getName().split("-")[1]);
            if (id == target)
                return quest;
        }
        return feasible.get(feasible.size()-1);
    }
}
