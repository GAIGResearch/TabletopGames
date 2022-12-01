package games.sirius;

import core.*;
import core.components.*;
import games.GameType;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class SiriusGameState extends AbstractGameState {
    public SiriusGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new SiriusTurnOrder(nPlayers), GameType.Sirius);
        rnd = new Random(gameParameters.getRandomSeed());
    }

    Deck<Card> ammoniaDeck;
    List<PlayerArea> playerAreas;
    List<Moon> moons;
    Random rnd;
    int[] playerLocations;
    int[] moveSelected;

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> retValue = new ArrayList<>();
        retValue.add(ammoniaDeck);
        retValue.addAll(moons);
        for (PlayerArea pa : playerAreas)
            retValue.add(pa.deck);
        return retValue;
    }

    @Override
    protected SiriusGameState _copy(int playerId) {
        SiriusGameState retValue = new SiriusGameState(gameParameters.copy(), getNPlayers());
        retValue.ammoniaDeck = ammoniaDeck.copy();
        retValue.playerAreas = playerAreas.stream().map(PlayerArea::copy).collect(toList());
        retValue.moons = moons.stream().map(Moon::copy).collect(toList());
        retValue.playerLocations = playerLocations.clone();
        retValue.moveSelected = moveSelected.clone();
        if (playerId != -1) {
            // we only know our card choice
            Arrays.fill(retValue.moveSelected, -1);
            retValue.moveSelected[playerId] = moveSelected[playerId];
        }
        return retValue;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return 0;
    }

    @Override
    public double getGameScore(int playerId) {
        return 0;
    }

    @Override
    protected void _reset() {
        ammoniaDeck = new Deck<>("ammoniaDeck", -1, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
        moons = new ArrayList<>();
        playerAreas = new ArrayList<>();
    }

    // This marks a decision as having been made, but does not yet implement this decision
    public void chooseMoveCard(int moon) {
        moveSelected[getCurrentPlayer()] = moon;
    }
    // for testing
    public int[] getMoveSelected() {
        return moveSelected.clone();
    }
    protected boolean allMovesSelected() {
        for (int b : moveSelected) if (b == -1) return false;
        return true;
    }
    protected void applyChosenMoves() {
        for (int i = 0; i < moveSelected.length; i++) {
            movePlayerTo(i, moveSelected[i]);
        }
    }

    public void movePlayerTo(int player, int moon) {
        if (moon > moons.size())
            throw new IllegalArgumentException(String.format("Cannot move to Moon %d as there are only %d", moon, moons.size()));
        playerLocations[player] = moon;
    }

    public int getLocationIndex(int player) {
        return playerLocations[player];
    }

    public Moon getMoon(int index) {
        return moons.get(index);
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof SiriusGameState) {
            return o.hashCode() == hashCode();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int retValue = Objects.hash(playerAreas, turnOrder, gamePhase, moons, ammoniaDeck, gameParameters, gameStatus);
        return retValue + 31 * Arrays.hashCode(playerResults)
                - 255 * Arrays.hashCode(playerLocations)
                - 31 * 255 * Arrays.hashCode(moveSelected);
    }

}
