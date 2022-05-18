package games.descent2e.concepts;

import utilities.Vector2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// TODO: figure out how this works
public class Quest {

    private String name;
    private ArrayList<String[]> monsters;  // name, tile, bonus effects for each monster type
//    private ArrayList<Group<Pair<Token, String>, Integer, String>> tokens;  // token, meaning, how many, where
//    private ArrayList<DescentRule> rules;  // define these in code
//    private ArrayList<DescentWinCondition> heroWins;
//    private int heroWinsMinSatisfied;
//    private ArrayList<DescentWinCondition> overlordWins;
//    private int overlordWinsMinSatisfied;
//    private ArrayList<DescentReward> overlordRewards;
//    private ArrayList<DescentReward> heroReward;
    private ArrayList<String> boards;
    private HashMap<String, ArrayList<Vector2D>> startingLocations;
    private ArrayList<Quest> nextMainQuests;
    private ArrayList<Quest> nextSideQuests;
    private int act;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getBoards() {
        return boards;
    }

    public void setBoards(ArrayList<String> boards) {
        this.boards = boards;
    }

    public ArrayList<Quest> getNextMainQuests() {
        return nextMainQuests;
    }

    public void setNextMainQuests(ArrayList<Quest> nextMainQuests) {
        this.nextMainQuests = nextMainQuests;
    }

    public ArrayList<Quest> getNextSideQuests() {
        return nextSideQuests;
    }

    public void setNextSideQuests(ArrayList<Quest> nextSideQuests) {
        this.nextSideQuests = nextSideQuests;
    }

    public void setStartingLocations(HashMap<String, ArrayList<Vector2D>> startingLocations) {
        this.startingLocations = startingLocations;
    }

    public HashMap<String, ArrayList<Vector2D>> getStartingLocations() {
        return startingLocations;
    }

    public void setMonsters(ArrayList<String[]> monsters) {
        this.monsters = monsters;
    }

    public ArrayList<String[]> getMonsters() {
        return monsters;
    }

    public void setAct(int act) {
        this.act = act;
    }

    public int getAct() {
        return act;
    }

    public Quest copy() {
        Quest q = new Quest();
        q.boards = new ArrayList<>(boards);
        q.startingLocations = new HashMap<>();
        for (Map.Entry<String, ArrayList<Vector2D>> e: startingLocations.entrySet()) {
            q.startingLocations.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        q.name = name;
        q.monsters = new ArrayList<>();
        for (String[] s: monsters) {
            q.monsters.add(s.clone());
        }
        q.act = act;
        return q; // TODO
    }
}
