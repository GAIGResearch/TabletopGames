package games.findmurderer;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.GridBoard;
import core.turnorders.AlternatingTurnOrder;
import games.GameType;
import games.findmurderer.components.Person;
import utilities.Vector2D;

import java.util.*;

public class MurderGameState extends AbstractGameState {

    // Player mapping sets who is player 0 and who is player 1 (detective or killer)
    public enum PlayerMapping{
        Detective(1),
        Killer(0);
        final int playerIdx;
        PlayerMapping(int playerIdx) {
            this.playerIdx = playerIdx;
        }
        public static PlayerMapping getPlayerTypeByIdx(int idx) {
            for (PlayerMapping pm: PlayerMapping.values()) {
                if (pm.playerIdx == idx) return pm;
            }
            return null;
        }
    }

    // Game state components: the grid, a mapping from each person's ID to their position in the grid for easy access
    // And the person from the grid who has the killer role. The detective does not have a physical person in the game.
    GridBoard<Person> grid;
    HashMap<Integer, Vector2D> personToPositionMap;
    Person killer;

    // What does the detective know about the interactions in the map?
    // Mapping from person ID to the round up until they know about the person. Initially 0.
    // TODO: show in GUI
    HashMap<Integer, Integer> detectiveInformation;
    // Where is the detective looking currently - coordinates of cell, with radius of vision around it according to parameters.
    Vector2D detectiveFocus;

    /**
     * Constructor, using alternating turn order: each player gets to execute 1 action.
     * @param gameParameters - parameters for the game
     * @param nPlayers - number of players
     */
    public MurderGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new AlternatingTurnOrder(nPlayers), GameType.FindMurderer);
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            add(grid);
        }};
    }

    /**
     * Copy the game state. If the 'playerId' is -1, that means the copy should be of the full game state. Otherwise,
     * it should be the partial observation of the player observing the game state.
     * @param playerId - player observing this game state.
     * @return - copy of this game state.
     */
    @Override
    protected MurderGameState _copy(int playerId) {
        // Create new game state object
        MurderGameState gs = new MurderGameState(gameParameters.copy(), getNPlayers());

        // Deep copy of the grid
        gs.grid = grid.emptyCopy();
        for (int i = 0; i < grid.getHeight(); i++) {
            for (int j = 0; j < grid.getWidth(); j++) {
                if (grid.getElement(j, i) != null) {
                    Person copy = grid.getElement(j, i).copy();
                    if (playerId != -1) {
                        // Partial observability
                        if (playerId == PlayerMapping.Detective.playerIdx) {
                            // Detective has partial information about the people's interaction history
                            int lastKnownRound = 0;
                            if (detectiveInformation.containsKey(copy.getComponentID()))
                                lastKnownRound = detectiveInformation.get(copy.getComponentID());
                            copy.interactionHistory.tailMap(lastKnownRound).clear();
                        } else {
                            // Killer doesn't know what detective knows
                            copy.interactionHistory = new TreeMap<>();
                        }
                    }
                    gs.grid.setElement(j, i, copy);
                }
            }
        }
        // Copy the mapping of person to position in grid
        gs.personToPositionMap = new HashMap<>(personToPositionMap);

        // Copy the killer
        if (playerId == -1 || playerId == PlayerMapping.Killer.playerIdx) {
            // Full observability, copy the killer directly
            gs.killer = killer.copy();
        } else {
            // Partial observability, we don't know who the killer is
            // We'll assign it as a random person out of those alive
            ArrayList<Person> alive = new ArrayList<>();
            for (Person p: gs.grid.getNonNullComponents()) {
                if (p.status == Person.Status.Alive) {
                    alive.add(p);
                    p.setPersonType(Person.PersonType.Civilian);
                }
            }
            Random r = new Random(getGameParameters().getRandomSeed());
            int idx = r.nextInt(alive.size());
            gs.killer = alive.get(idx);
            gs.killer.setPersonType(Person.PersonType.Killer);
        }

//        if (playerId == -1 || playerId == PlayerMapping.Detective.playerIdx) {
//            // Detective knows where they're looking
//            gs.detectiveFocus = detectiveFocus.copy();
//        } else {
//            // Killer doesn't?
//            gs.detectiveFocus = new Vector2D();
//        }

        // Everyone knows where the detective is looking
        gs.detectiveFocus = detectiveFocus.copy();

        // Return the copy
        return gs;
    }

    public HashMap<Integer, Integer> getDetectiveInformation() {
        return detectiveInformation;
    }

    public Vector2D getDetectiveFocus() {
        return detectiveFocus;
    }

    public void setDetectiveFocus(Vector2D detectiveFocus) {
        this.detectiveFocus = detectiveFocus;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return getGameScore(playerId);
    }

    @Override
    public double getGameScore(int playerId) {
        // Game score is based on the percentage of dead people: 1-percentage for the detective player, who needs to kill as little as possible.
        double deadPercentage = countDeadPerc();
        if (playerId == PlayerMapping.Detective.playerIdx)
            return 1 - deadPercentage;
        return deadPercentage;
    }

    public int countDead() {
        // Counts the raw number of dead people
        int nDead = 0;
        for (Person p: grid.getNonNullComponents()) {
            if (p.status == Person.Status.Dead) nDead++;
        }
        return nDead;
    }

    /**
     * Counts the percentage of dead people in the game.
     * @return - percentage of dead people, as double.
     */
    public double countDeadPerc() {
        int nDead = 0;
        List<Person> components = grid.getNonNullComponents();
        int total = components.size();
        for (Person p: components) {
            if (p.status == Person.Status.Dead) nDead++;
        }
        return nDead * 1.0 / total;
    }

    // Accessors to game state variables next:
    public GridBoard<Person> getGrid() {
        return grid;
    }
    public Person getKiller() {
        return killer;
    }
    public HashMap<Integer, Vector2D> getPersonToPositionMap() {
        return personToPositionMap;
    }

    @Override
    protected void _reset() {
        grid = null;
        personToPositionMap = null;
        killer = null;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MurderGameState)) return false;
        if (!super.equals(o)) return false;
        MurderGameState that = (MurderGameState) o;
        return Objects.equals(grid, that.grid) && Objects.equals(personToPositionMap, that.personToPositionMap) && Objects.equals(killer, that.killer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), grid, personToPositionMap, killer);
    }
}
