package games.descent2e.concepts;

import games.descent2e.components.tokens.DToken;
import utilities.Vector2D;

import java.util.*;

// TODO: figure out how this works
public class Quest {

    private String name;
    private ArrayList<String[]> monsters;  // name, tile, bonus effects for each monster type
    private ArrayList<DToken.DTokenDef> tokens;  // token, meaning, how many, where
    private ArrayList<GameOverCondition> gameOverConditions;  // How does the game end?
//    private ArrayList<DescentRule> rules;  // define these in code
//    private int heroWinsMinSatisfied;
//    private int overlordWinsMinSatisfied;
    private ArrayList<DescentReward> overlordRewards;
    private ArrayList<DescentReward> heroRewards;
    private ArrayList<DescentReward> commonRewards;
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
    public void setTokens(ArrayList<DToken.DTokenDef> tokens) {
        this.tokens = tokens;
    }
    public ArrayList<DToken.DTokenDef> getTokens() {
        return tokens;
    }
    public void setGameOverConditions(ArrayList<GameOverCondition> gameOverConditions) {
        this.gameOverConditions = gameOverConditions;
    }
    public ArrayList<GameOverCondition> getGameOverConditions() {
        return gameOverConditions;
    }

    public ArrayList<DescentReward> getCommonRewards() {
        return commonRewards;
    }

    public ArrayList<DescentReward> getHeroRewards() {
        return heroRewards;
    }

    public ArrayList<DescentReward> getOverlordRewards() {
        return overlordRewards;
    }

    public void setCommonRewards(ArrayList<DescentReward> commonRewards) {
        this.commonRewards = commonRewards;
    }

    public void setOverlordRewards(ArrayList<DescentReward> overlordRewards) {
        this.overlordRewards = overlordRewards;
    }

    public void setHeroRewards(ArrayList<DescentReward> heroRewards) {
        this.heroRewards = heroRewards;
    }

    public Quest copy() {
        Quest q = new Quest();
        if (boards != null) q.boards = new ArrayList<>(boards);
        if (startingLocations != null) {
            q.startingLocations = new HashMap<>();
            for (Map.Entry<String, ArrayList<Vector2D>> e : startingLocations.entrySet()) {
                q.startingLocations.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
        }
        q.name = name;
        if (monsters != null) {
            q.monsters = new ArrayList<>(Collections.nCopies(monsters.size(), null));
            Collections.copy(q.monsters, monsters);
        }
        q.act = act;
        if (tokens != null)
        {
            q.tokens = new ArrayList<>();
            for (DToken.DTokenDef d : tokens) {
                q.tokens.add(d.copy());
            }
        }
        if (gameOverConditions != null)
        {
            q.gameOverConditions = new ArrayList<>();
            for (GameOverCondition g : gameOverConditions) {
                q.gameOverConditions.add(g.copy());
            }
        }

        if (overlordRewards != null)
        {
            q.overlordRewards = new ArrayList<>();
            for (DescentReward d : overlordRewards) {
                q.overlordRewards.add(d.copy());
            }
        }
        if (heroRewards != null)
        {
            q.heroRewards = new ArrayList<>();
            for (DescentReward d : heroRewards) {
                q.heroRewards.add(d.copy());
            }
        }
        if (commonRewards != null)
        {
            q.commonRewards = new ArrayList<>();
            for (DescentReward d : commonRewards) {
                q.commonRewards.add(d.copy());
            }
        }

        if (nextMainQuests != null)
        {
            q.nextMainQuests = new ArrayList<>();
            for (Quest d : nextMainQuests) {
                q.nextMainQuests.add(d.copy());
            }
        }
        if (nextSideQuests != null)
        {
            q.nextSideQuests = new ArrayList<>();
            for (Quest d : nextSideQuests) {
                q.nextSideQuests.add(d.copy());
            }
        }

        return q; // TODO
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quest quest = (Quest) o;
        return act == quest.act && Objects.equals(name, quest.name) &&
                Objects.equals(monsters, quest.monsters) && Objects.equals(tokens, quest.tokens) &&
                Objects.equals(gameOverConditions, quest.gameOverConditions) &&
                Objects.equals(overlordRewards, quest.overlordRewards) && Objects.equals(heroRewards, quest.heroRewards) &&
                Objects.equals(commonRewards, quest.commonRewards) && Objects.equals(boards, quest.boards) &&
                Objects.equals(startingLocations, quest.startingLocations) && Objects.equals(nextMainQuests, quest.nextMainQuests) &&
                Objects.equals(nextSideQuests, quest.nextSideQuests);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, monsters, tokens, gameOverConditions, overlordRewards, heroRewards, commonRewards, boards, startingLocations, nextMainQuests, nextSideQuests, act);
    }
}
