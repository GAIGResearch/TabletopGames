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
    int playerIdx;
    String destination;

    public MovePlayer(int playerIdx, String city) {
        this.playerIdx = playerIdx;
        this.destination = city;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState) gs;
        PropertyString prop = (PropertyString) pgs.getComponent(PandemicConstants.playerCardHash, playerIdx).getProperty(PandemicConstants.playerLocationHash);
        removePlayer((PandemicGameState)gs, prop.value, playerIdx);
        placePlayer((PandemicGameState)gs, destination, playerIdx);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return new MovePlayer(playerIdx, destination);
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
            return destination.equals(otherAction.destination) && playerIdx == otherAction.playerIdx;

        }else return false;
    }

    @Override
    public String toString() {
        return "Move Player " + playerIdx + " to " + destination;
    }

    public int getPlayerIdx() {
        return playerIdx;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerIdx, destination);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
