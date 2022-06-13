package games.pandemic.actions;

import core.actions.AbstractAction;
import core.components.BoardNode;
import core.components.Card;
import core.properties.*;
import core.AbstractGameState;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicConstants;

import java.util.Objects;

import static core.CoreConstants.nameHash;
import static core.CoreConstants.playersHash;


public class MovePlayer extends AbstractAction {
    int playerToMove;
    String destination;
    MoveType moveType;

    public enum MoveType {
        Airlift,
        OperationsExpert,
        Dispatcher,
        DriveFerry,
        CharterFlight,
        DirectFlight,
        ShuttleFlight,
    }

    public MovePlayer(MoveType type, int playerIdx, String city) {
        this.moveType = type;
        this.playerToMove = playerIdx;
        this.destination = city;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState) gs;
        PropertyString prop = (PropertyString) pgs.getComponent(PandemicConstants.playerCardHash, playerToMove).getProperty(PandemicConstants.playerLocationHash);
        removePlayer((PandemicGameState)gs, prop.value, playerToMove);
        placePlayer((PandemicGameState)gs, destination, playerToMove);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return new MovePlayer(moveType, playerToMove, destination);
    }


    public static void placePlayer(PandemicGameState gs, String city, int playerIdx) {
        BoardNode bn = gs.getWorld().getNodeByStringProperty(nameHash, city);
        PropertyIntArrayList prop = (PropertyIntArrayList) bn.getProperty(playersHash);
        prop.getValues().add(playerIdx);

        Card playerCard = (Card) gs.getComponent(PandemicConstants.playerCardHash, playerIdx);
        playerCard.setProperty(new PropertyString("playerLocation", city));
    }

    public static void removePlayer(PandemicGameState gs, String city, int playerIdx) {
        BoardNode bn = gs.getWorld().getNodeByStringProperty(nameHash, city);
        PropertyIntArrayList prop = (PropertyIntArrayList) bn.getProperty(playersHash);
        prop.getValues().remove(Integer.valueOf(playerIdx));

        Card playerCard = (Card) gs.getComponent(PandemicConstants.playerCardHash, playerIdx);
        playerCard.setProperty(new PropertyString("playerLocation", null));
    }

    public String getDestination(){
        return destination;
    }


    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if(other instanceof MovePlayer)
        {
            MovePlayer otherAction = (MovePlayer) other;
            return destination.equals(otherAction.destination) && playerToMove == otherAction.playerToMove;

        }else return false;
    }

    @Override
    public String toString() {
        return moveType + ": p" + playerToMove + " to " + destination;
    }

    public int getPlayerToMove() {
        return playerToMove;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerToMove, destination);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
