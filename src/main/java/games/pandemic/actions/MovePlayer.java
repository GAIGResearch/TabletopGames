package games.pandemic.actions;

import core.actions.IAction;
import core.components.BoardNode;
import core.components.Card;
import core.content.*;
import core.AbstractGameState;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicConstants;

import static games.pandemic.PandemicConstants.nameHash;


public class MovePlayer implements IAction {

    protected int playerIdx;
    protected String destination;

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

        return false;
    }

    public static void placePlayer(PandemicGameState gs, String city, int playerIdx) {
        BoardNode bn = gs.world.getNode(nameHash, city);
        PropertyIntArrayList prop = (PropertyIntArrayList) bn.getProperty(PandemicConstants.playersBNHash);
        prop.getValues().add(playerIdx);

        Card playerCard = (Card) gs.getComponent(PandemicConstants.playerCardHash, playerIdx);
        playerCard.setProperty(PandemicConstants.playerLocationHash, new PropertyString(city));  // TODO: does this exist?
    }

    public static void removePlayer(PandemicGameState gs, String city, int playerIdx) {
        BoardNode bn = gs.world.getNode(nameHash, city);
        PropertyIntArrayList prop = (PropertyIntArrayList) bn.getProperty(PandemicConstants.playersBNHash);
        prop.getValues().remove(Integer.valueOf(playerIdx));

        Card playerCard = (Card) gs.getComponent(PandemicConstants.playerCardHash, playerIdx);
        playerCard.setProperty(PandemicConstants.playerLocationHash, new PropertyString(null));
    }

    public String getDestination(){
        return destination;
    }

    public int getPlayerIdx() {
        return playerIdx;
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
        return "MovePlayer{" +
                "player=" + playerIdx +
                ", destination='" + destination + '\'' +
                '}';
    }
}
