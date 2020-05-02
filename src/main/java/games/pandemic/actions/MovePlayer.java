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


    private int playerIdx;
    private String destination;

    public MovePlayer(int playerIdx, String city) {
        this.playerIdx = playerIdx;
        this.destination = city;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState) gs;
        PropertyString prop = (PropertyString) pgs.getComponent(PandemicConstants.playerCardHash, playerIdx).getProperty(PandemicConstants.playerLocationHash);
        BoardNode currentCity = ((PandemicGameState)gs).world.getNode(nameHash, prop.value);
        BoardNode destinationCity = ((PandemicGameState)gs).world.getNode(nameHash, destination);

        // todo there are more ways to move the player, when this function is called the player should already know if the move is legal or not
//        if (checkNeighbours(currentCity, destinationCity) || checkResearchStations(currentCity, destinationCity)) {
//            removePlayer((PandemicGameState)gs, prop.value, playerIdx);
//            placePlayer((PandemicGameState)gs, destination, playerIdx);
//        }
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
        prop.getValues().remove(new Integer(playerIdx));

        Card playerCard = (Card) gs.getComponent(PandemicConstants.playerCardHash, playerIdx);
        playerCard.setProperty(PandemicConstants.playerLocationHash, new PropertyString(null));
    }

    boolean checkNeighbours(BoardNode city1, BoardNode city2) {
        PropertyStringArray neighbours = (PropertyStringArray) city1.getProperty(PandemicConstants.neighboursHash);
        PropertyString name = (PropertyString) city2.getProperty(nameHash);
        for (String neighbour : neighbours.getValues()) {
            if (name.value.equals(neighbour)) {
                return true;
            }
        }
        return false;
    }

    boolean checkResearchStations(BoardNode city1, BoardNode city2) {
        PropertyBoolean research1 = (PropertyBoolean) city1.getProperty(PandemicConstants.researchStationHash);
        PropertyBoolean research2 = (PropertyBoolean) city2.getProperty(PandemicConstants.researchStationHash);
        return research1.value && research2.value;
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
}
