package games.DiceMonastery;

import core.*;
import core.components.*;

import java.util.*;
import java.util.stream.Collectors.*;

import static games.DiceMonastery.DiceMonasteryGameState.actionArea.*;


public class DiceMonasteryGameState extends AbstractGameState {

    enum actionArea {
        MEADOW(1), KITCHEN(2), WORKSHOP(3),
        GATEHOUSE(1), LIBRARY(4), CHAPEL(1),
        DORMITORY(1);

        public final int dieMinimum;

        actionArea(int dieMinimum) {
            this.dieMinimum = dieMinimum;
        }
    }

    enum resource {
        GRAIN, HONEY, WAX, SKEP, BREAD
    }

    Map<actionArea, Area> actionAreas = new HashMap<>();
    Map<Integer, Monk> allMonks = new HashMap<>();
    Map<Integer, actionArea> monkLocations = new HashMap<>();
    List<Map<resource, Integer>> playerTreasuries = new ArrayList<>();

    public DiceMonasteryGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new DiceMonasteryTurnOrder(nPlayers));
    }

    @Override
    protected void _reset() {
        actionAreas = new HashMap<>();
        Arrays.stream(actionArea.values()).forEach(a ->
                actionAreas.put(a, new Area(-1, a.name()))
        );

        allMonks = new HashMap<>();
        monkLocations = new HashMap<>();
        playerTreasuries = new ArrayList<>();
        for (int p = 0; p < getNPlayers(); p++)
            playerTreasuries.add(new HashMap<>());
    }

    public void createMonk(int piety, int player) {
        Monk monk = new Monk(piety, player);
        int id = monk.getComponentID();
        allMonks.put(id, monk);
        monkLocations.put(id, DORMITORY);
    }

    public void moveMonk(int id, actionArea from, actionArea to) {
        Monk movingMonk = allMonks.get(id);
        if (movingMonk == null)
            throw new IllegalArgumentException("Monk does not exist : " + id);
        if (movingMonk.piety < to.dieMinimum)
            throw new AssertionError(String.format("Monk only has a piety of %d, so cannot move to %s", movingMonk.piety, to.dieMinimum));
        monkLocations.put(id, to);
        actionAreas.get(from).removeComponent(movingMonk);
        actionAreas.get(to).putComponent(movingMonk);
    }

    public void gain(int player, resource resource, int amount) {
        int currentLevel = playerTreasuries.get(player).getOrDefault(resource,0);
        if (currentLevel + amount < 0)
            throw new IllegalArgumentException(String.format("Only have %d %s in stock; cannot remove %d", currentLevel, resource, -amount));
        playerTreasuries.get(player).put(resource, currentLevel + amount);
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<>(allMonks.values());
    }

    @Override
    protected DiceMonasteryGameState _copy(int playerId) {
        return null;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return getGameScore(playerId);
    }

    @Override
    public double getGameScore(int playerId) {
        return allMonks.values().stream()
                .filter(m -> m.getOwnerId() == playerId)
                .mapToInt(Monk::getPiety)
                .sum();
    }

    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        return new ArrayList<>();
    }

    @Override
    protected boolean _equals(Object o) {
        if (!(o instanceof DiceMonasteryGameState))
            return false;
        DiceMonasteryGameState other = (DiceMonasteryGameState) o;
        return other.allMonks.equals(allMonks) && other.monkLocations.equals(monkLocations) &&
                other.playerTreasuries.equals(playerTreasuries);
    }
}
