package games.descent.concepts;

import core.components.Token;
import utilities.Group;
import utilities.Pair;

import java.util.ArrayList;

// TODO: figure out how this works
public class Quest {

    private String name;
//    private ArrayList<Group<Monster, String, ArrayList<String>>> monsters;  // monster, location, effects
//    private ArrayList<Group<Pair<Token, String>, Integer, String>> tokens;  // token, meaning, how many, where
//    private ArrayList<DescentRule> rules;  // define these in code
//    private ArrayList<DescentWinCondition> heroWins;
//    private int heroWinsMinSatisfied;
//    private ArrayList<DescentWinCondition> overlordWins;
//    private int overlordWinsMinSatisfied;
//    private ArrayList<DescentReward> overlordRewards;
//    private ArrayList<DescentReward> heroReward;
    private ArrayList<String> boards;
    private ArrayList<Quest> nextMainQuests;
    private ArrayList<Quest> nextSideQuests;

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

    public Quest copy() {
        return new Quest(); // TODO
    }
}
