package games.descent2e.pcg;

import com.google.crypto.tink.subtle.Random;
import core.components.BoardNode;
import core.components.GraphBoard;
import core.components.GridBoard;
import games.descent2e.DescentGameData;
import games.descent2e.DescentGameState;
import games.descent2e.components.Monster;
import games.descent2e.concepts.Quest;
import org.apache.hadoop.yarn.state.Graph;
import utilities.Pair;

import java.util.*;

public class GenerateBoards {

    public static final String path = "data/descent2e/";

    public static List<GraphBoard> originalBoards = new ArrayList<>();
    public static List<Quest> originalQuests = new ArrayList<>();

    public static List<GridBoard> tiles = new ArrayList<>();

    public static HashMap<String, HashMap<String, Monster>> monsters;
    public static HashMap<String, HashMap<String, Monster>> lieutenants;

    public static List<Pair<Quest, GraphBoard>> feasible = new ArrayList<>();
    public static List<Pair<Quest, GraphBoard>> infeasible = new ArrayList<>();

    public static void main(String[] args) {
        DescentGameData data = new DescentGameData();
        data.load(path);
        originalBoards = data.getBoardConfigurations();
        originalQuests = data.getQuests();
        tiles = data.getTiles();
        monsters = data.getMonsters();
        lieutenants = data.getLieutenants();
        Quest quest = originalQuests.get(0);
        Quest quest2 = originalQuests.get(1);
        // FitnessFunction.getBoardSize(Objects.requireNonNull(getBoardByName(quest.getBoards().get(0))));
        Pair<Pair<Quest, GraphBoard>, Boolean> offspring = createOffspring(quest, quest2);

        if (offspring.b)
            feasible.add(offspring.a);
        else
            infeasible.add(offspring.a);

        System.out.println("Pootis!");
    }

    static GridBoard getTileByName(String name) {
        if (name.contains("-"))
            name = name.split("-")[0];
        for (GridBoard tile : tiles) {
            if (tile.getComponentName().equals(name))
                return tile;
        }
        return null;
    }

    static int mutate() {
        return Random.randInt(100);
    }

    static Pair<Pair<Quest, GraphBoard>, Boolean> createOffspring(Quest parent1, Quest parent2) {
        Quest newQuest;
        GraphBoard newBoard;
        Quest otherParent;
        GraphBoard otherBoard;

        // Deciding which Parent is the Base Quest
        int choice = Random.randInt(2);
        if (choice == 0) {
            newQuest = parent1;
            otherParent = parent2;
        }
        else {
            newQuest = parent2;
            otherParent = parent1;
        }

        // Now, decide which Parent is the Base Board
        choice = Random.randInt(2);
        if (choice == 0) {
            newBoard = Objects.requireNonNull(getBoardByName(parent1.getBoards().get(0))).copy();
            otherBoard = Objects.requireNonNull(getBoardByName(parent2.getBoards().get(0))).copy();
        }
        else {
            newBoard = Objects.requireNonNull(getBoardByName(parent2.getBoards().get(0))).copy();
            otherBoard = Objects.requireNonNull(getBoardByName(parent1.getBoards().get(0))).copy();
        }

        // -- BOARD MUTATIONS ---

        List<BoardNode> newNodes = newBoard.getComponents();
        List<BoardNode> oldNodes = otherBoard.getComponents();
        List<String> tiles = new ArrayList<>();
        for (BoardNode node : newNodes) {
            tiles.add(node.getComponentName());
        }
        // 10% crossover chance
        for (BoardNode node : oldNodes) {
            if (Random.randInt(10) < 1) {
                // Make sure we don't add duplicate Monsters
                if (!tiles.contains(node.getComponentName()) || node.getComponentName().contains("extender") ||
                        node.getComponentName().contains("endcap") || node.getComponentName().contains("transition"))
                    newNodes.add(node);
            }
        }

        List<BoardNode> finalNodes = new ArrayList<>(newNodes);
        for (BoardNode node : newNodes) {
            if (Random.randInt(10) < 1) {
                finalNodes.remove(node);
            }
        }

        newBoard.setBoardNodes(finalNodes);

        // --- MONSTER MUTATIONS ---

        List<String[]> monsters = newQuest.getMonsters();
        List<String> monsterList = new ArrayList<>();
        for (String[] m : monsters)
            monsterList.add(m[0]);

        // 10% crossover chance
        for (String[] m : otherParent.getMonsters()) {
            if (Random.randInt(10) < 1) {
                // Make sure we don't add duplicate Monsters
                if (!monsterList.contains(m[0]) || m[0].contains("Open"))
                    monsters.add(m);
            }
        }

        // 10% deletion chance
        List<String[]> finalMonsters = new ArrayList<>(monsters);
        for (String[] m : monsters) {
            if (Random.randInt(10) < 1) {
                finalMonsters.remove(m);
            }
        }

        // Force a mutation if the group sizes are now too big or too small
        boolean forceMutate = (finalMonsters.size() < ControlVariables.GROUP_MIN) || (finalMonsters.size() > ControlVariables.GROUP_MAX);

        if (!forceMutate)
            if (mutate() < ControlVariables.MONSTER_MUTATE)
                forceMutate = true;

        while (forceMutate) {
            mutateMonsters(finalMonsters);
            forceMutate = (finalMonsters.size() < ControlVariables.GROUP_MIN) || (finalMonsters.size() > ControlVariables.GROUP_MAX);
        }

        newQuest.setMonsters(finalMonsters);

        mutateAct(newQuest);
        mutateXP(newQuest);
        mutateTraits(newQuest);

        List<Float> scores = FitnessFunction.getFitness(newQuest, newBoard);

        boolean feasible = checkFeasible(scores);

        System.out.println("Pootis");

        Pair<Quest, GraphBoard> offspring = new Pair<>(newQuest, newBoard);

        return new Pair<>(offspring, feasible);
    }

    static boolean checkFeasible(List<Float> scores) {

        // Connectedness Check
        if (scores.get(0) < 1f)
            return false;

        // Geometry Check
        if (scores.get(1) < 1f)
            return false;

        // No Repeating Monsters Check
        if (scores.get(2) < 1f)
            return false;

        // Consistency Check
        if (scores.get(4) < 1f)
            return false;

        // Board Size Check
        float size = scores.get(5);
        if (size > ControlVariables.SIZE_MAX || size < ControlVariables.SIZE_MIN)
            return false;

        // Monster Group Check
        float groups = scores.get(6);
        if (groups > ControlVariables.GROUP_MAX || groups < ControlVariables.GROUP_MIN)
            return false;

        return true;
    }

    static String getRandomMonster(List<String> oldMonsters) {
        String newMonster = "null";
        String monType = "";
        String otherType = "";
        int type = mutate();
        List<String> toAdd = new ArrayList<>();
        List<String> other = new ArrayList<>();

        if (type < ControlVariables.SMALLOPENGROUP)
            return "OpenSmall:group";
        if (type < ControlVariables.OPENGROUP)
            return "Open:group";

        if (type >= (100 - ControlVariables.LIEUTENANT)) {
            toAdd.addAll(lieutenants.keySet());
            monType = ":lieutenant";
            other.addAll(monsters.keySet());
            otherType = ":group";
        }
        else {
            toAdd.addAll(monsters.keySet());
            monType = ":group";
            other.addAll(lieutenants.keySet());
            otherType = ":lieutenant";
        }

        Collections.shuffle(toAdd);
        boolean done = false;
        for (String m : toAdd) {
            if (!oldMonsters.contains(m+monType)) {
                newMonster = m;
                done = true;
                break;
            }
        }
        if (done)
            return newMonster + monType;
        for (String m : other) {
            if (!oldMonsters.contains(m + otherType)) {
                return m + otherType;
            }
        }
        return "Open:group";
    }

    static List<String[]> mutateMonsters(List<String[]> monsters) {
        List<String[]> newMonsters = new ArrayList<>(monsters);
        int mutate = mutate();

        int size = monsters.size();

        // Force a specific mutation if for whatever reason we're already over or under the limits
        boolean add = size < ControlVariables.GROUP_MIN;
        boolean remove = size > ControlVariables.GROUP_MAX;

        if (!add)
            if (size < ControlVariables.GROUP_MAX)
                if (mutate < ControlVariables.ADD_GROUP)
                    add = true;

        if (!remove)
            if (size > ControlVariables.GROUP_MIN)
                if (mutate >= (100 - ControlVariables.REMOVE_GROUP))
                    remove = true;

        // Add a new Monster to the groups
        if (add) {
            List<String> currentMonsters = new ArrayList<>();
            for (String[] m : monsters) {
                currentMonsters.add(m[0]);
            }
            String[] newMonster = new  String[2];
            newMonster[0] = getRandomMonster(currentMonsters);
            newMonster[1] = "null";
            newMonsters.add(newMonster);
        }
        // Remove a Monster from the groups
        else if (remove) {
            newMonsters.remove(Random.randInt(newMonsters.size()));
        }
        // Replace a Monster from the groups
        else {
            List<String> currentMonsters = new ArrayList<>();
            for (String[] m : monsters) {
                currentMonsters.add(m[0]);
            }
            String newMonster = getRandomMonster(currentMonsters);
            (newMonsters.get(Random.randInt(newMonsters.size())))[0] = newMonster;
        }

        return newMonsters;
    }

    static void mutateAct(Quest quest) {
        int mutate = mutate();
        if (mutate < ControlVariables.ACT_MUTATE) {
            int newAct = (quest.getAct() % 2) + 1;
            if (newAct > 1) {
                quest.setStartingXP(quest.getStartingXP() + 5);
                quest.setGold(quest.getGold() + 125);
            }
            else {
                quest.setStartingXP(quest.getStartingXP() - 5);
                quest.setGold(quest.getGold() - 125);
            }
            quest.setAct(newAct);
        }
    }

    static void mutateXP(Quest quest) {
        int mutate = mutate();
        if (mutate < ControlVariables.XP_MUTATE) {
            int newXP = Random.randInt(5) + 5 * (quest.getAct() - 1);
            int newGold = newXP * 25;
            if (newXP > 1) {
                for (int i = 0; i < Random.randInt(newXP); i++) {
                    newGold += (Random.randInt(5) * 25);
                }
            }

            quest.setStartingXP(newXP);
            quest.setGold(newGold);
        }
    }

    static void mutateTraits(Quest quest) {
        List<String> oldTraits = quest.getMonsterTraits();
        boolean add = oldTraits.size() < ControlVariables.TRAITS_MIN;
        boolean remove = oldTraits.size() > ControlVariables.TRAITS_MAX;
        boolean replace = false;

        int mutate = mutate();

        if (!add)
            if (oldTraits.size() < ControlVariables.TRAITS_MAX)
                if (mutate < ControlVariables.ADD_TRAITS)
                    add = true;
        if (!remove)
            if (oldTraits.size() > ControlVariables.TRAITS_MIN)
                if (mutate < (ControlVariables.ADD_TRAITS + ControlVariables.REMOVE_TRAITS))
                    remove = true;
        if (mutate < (ControlVariables.ADD_TRAITS + ControlVariables.REMOVE_TRAITS + ControlVariables.REPLACE_TRAITS))
            replace = true;

        // Remove, in case of 'All' trait
        if (oldTraits.size() < ControlVariables.TRAITS_MIN)
            oldTraits = new ArrayList<>();

        // Add
        if (add) {
            List<String> traits = new ArrayList<>(ControlVariables.TRAITS);
            Collections.shuffle(traits);
            for (String t : traits) {
                if (!oldTraits.contains(t))
                    oldTraits.add(t);
                if (oldTraits.size() >= ControlVariables.TRAITS_MIN)
                    break;
            }
        }
        // Remove
        else if (remove) {
            oldTraits.remove(Random.randInt(oldTraits.size()));
        }
        // Replace
        else if (replace) {
            int r = Random.randInt(oldTraits.size());
            List<String> traits = new ArrayList<>(ControlVariables.TRAITS);
            Collections.shuffle(traits);
            for (String t : traits) {
                if (!oldTraits.contains(t)) {
                    oldTraits.remove(r);
                    oldTraits.add(t);
                    break;
                }
            }
        }
        // Set Traits to 'All'
        else {
            oldTraits = new ArrayList<>();
            oldTraits.add("All");
        }
        quest.setMonsterTraits(oldTraits);
    }

    static GraphBoard getBoardByName (String name) {
        for (GraphBoard board : originalBoards) {
            if (board.getComponentName().equals(name))
                    return board;
        }
        return null;
    }
}
