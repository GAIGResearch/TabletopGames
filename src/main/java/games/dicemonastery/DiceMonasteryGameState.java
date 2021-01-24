package games.dicemonastery;

import core.*;
import core.components.*;

import java.util.*;

import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static java.util.stream.Collectors.*;


public class DiceMonasteryGameState extends AbstractGameState {


    Map<ActionArea, DMArea> actionAreas = new HashMap<>();
    Map<Integer, Monk> allMonks = new HashMap<>();
    Map<Integer, ActionArea> monkLocations = new HashMap<>();
    List<Map<Resource, Integer>> playerTreasuries = new ArrayList<>();
    Stack<IExtendedSequence> actionsInProgress = new Stack<>();

    public DiceMonasteryGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new DiceMonasteryTurnOrder(nPlayers, (DiceMonasteryParams) gameParameters));
    }

    @Override
    protected void _reset() {
        actionsInProgress = new Stack<>();
        actionAreas = new HashMap<>();
        Arrays.stream(ActionArea.values()).forEach(a ->
                actionAreas.put(a, new DMArea(-1, a.name()))
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
        actionAreas.get(DORMITORY).putComponent(monk);
    }

    public void moveMonk(int id, ActionArea from, ActionArea to) {
        Monk movingMonk = allMonks.get(id);
        if (movingMonk == null)
            throw new IllegalArgumentException("Monk does not exist : " + id);
        if (movingMonk.piety < to.dieMinimum)
            throw new AssertionError(String.format("Monk only has a piety of %d, so cannot move to %s", movingMonk.piety, to));
        monkLocations.put(id, to);
        actionAreas.get(from).removeComponent(movingMonk);
        actionAreas.get(to).putComponent(movingMonk);
    }

    public void addResource(int player, Resource resource, int amount) {
        int currentLevel = getResource(player, resource);
        if (currentLevel + amount < 0)
            throw new IllegalArgumentException(String.format("Only have %d %s in stock; cannot remove %d", currentLevel, resource, -amount));
        playerTreasuries.get(player).put(resource, currentLevel + amount);
    }

    public void moveCube(int player, Resource resource, ActionArea from, ActionArea to) {
        Token cubeMoved = null;
        if (from == STOREROOM) {
            addResource(player, resource, -1);
        } else if (from != SUPPLY) {
            cubeMoved = actionAreas.get(from).take(resource, player);
        }
        if (to == STOREROOM) {
            addResource(player, resource, 1);
        } else if (to != SUPPLY) {
            if (cubeMoved == null)
                cubeMoved = new Token(resource.toString());
            actionAreas.get(to).putComponent(cubeMoved);
        }
    }

    public void useAP(int actionPointsSpent) {
        DiceMonasteryTurnOrder dto = (DiceMonasteryTurnOrder) turnOrder;
        dto.actionPointsLeftForCurrentPlayer -= actionPointsSpent;
        if (dto.actionPointsLeftForCurrentPlayer < 0) {
            throw new IllegalArgumentException("Not enough action points available");
        }
    }

    public int getResource(int player, Resource resource) {
        return playerTreasuries.get(player).getOrDefault(resource, 0);
    }

    public IExtendedSequence currentActionInProgress() {
        return actionsInProgress.isEmpty() ? null : actionsInProgress.peek();
    }

    public boolean isActionInProgress() {
        return !actionsInProgress.empty();
    }

    public void setActionInProgress(IExtendedSequence action) {
        if (action == null && !actionsInProgress.isEmpty())
            actionsInProgress.pop();
        else
            actionsInProgress.push(action);
    }

    public List<Monk> monksIn(ActionArea region, int player) {
        return allMonks.values().stream()
                .filter(m -> (region == null || monkLocations.get(m.getComponentID()) == region) &&
                        (player == -1 || m.getOwnerId() == player))
                .collect(toList());
    }

    public ActionArea getMonkLocation(int id) {
        return monkLocations.get(id);
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<>(allMonks.values());
    }

    /*
        List<Map<Resource, Integer>> playerTreasuries = new ArrayList<>();
    */
    @Override
    protected DiceMonasteryGameState _copy(int playerId) {
        DiceMonasteryGameState retValue = new DiceMonasteryGameState(gameParameters.copy(), getNPlayers());
        for (ActionArea a : actionAreas.keySet()) {
            retValue.actionAreas.put(a, actionAreas.get(a).copy());
        }
        retValue.allMonks.clear();
        retValue.monkLocations.clear();
        for (int monkId : allMonks.keySet()) {
            retValue.allMonks.put(monkId, allMonks.get(monkId).copy());
        }
        // monkLocations contains immutable things, so we just create a new mapping
        retValue.monkLocations = new HashMap<>(monkLocations);

        retValue.actionsInProgress = new Stack<>();
        actionsInProgress.forEach(
                a -> retValue.actionsInProgress.push(a.copy())
        );

        retValue.playerTreasuries = new ArrayList<>();
        for (int p = 0; p < getNPlayers(); p++) {
            retValue.playerTreasuries.add(new HashMap<>(playerTreasuries.get(p)));
        }
        return retValue;
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
                other.playerTreasuries.equals(playerTreasuries) && other.actionsInProgress.equals(actionsInProgress) &&
                        other.actionAreas.equals(actionAreas);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actionAreas, allMonks, monkLocations, playerTreasuries, actionsInProgress, gameStatus, gamePhase,
                gameParameters, turnOrder) + 31 * Arrays.hashCode(playerResults);
    }
}
