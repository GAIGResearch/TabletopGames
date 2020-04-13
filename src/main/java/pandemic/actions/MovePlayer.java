package pandemic.actions;

import actions.Action;
import components.BoardNode;
import components.Card;
import content.PropertyBoolean;
import content.PropertyIntArrayList;
import content.PropertyString;
import content.PropertyStringArray;
import core.GameState;
import pandemic.PandemicGameState;
import pandemic.Constants;

import static pandemic.Constants.nameHash;


public class MovePlayer implements Action {


    int playerIdx;
    String destination;

    public MovePlayer(int playerIdx, String city) {
        this.playerIdx = playerIdx;
        this.destination = city;
    }

    @Override
    public boolean execute(GameState gs) {
        PropertyString prop = (PropertyString) gs.getAreas().get(playerIdx).getComponent(Constants.playerCardHash).getProperty(Constants.playerLocationHash);
        BoardNode currentCity = ((PandemicGameState)gs).world.getNode(nameHash, prop.value);
        BoardNode destinationCity = ((PandemicGameState)gs).world.getNode(nameHash, destination);

        // todo there are more ways to move the player, when this function is called the player should already know if the move is legal or not
        if (checkNeighbours(currentCity, destinationCity) || checkResearchStations(currentCity, destinationCity)) {
            removePlayer((PandemicGameState)gs, prop.value, playerIdx);
            placePlayer((PandemicGameState)gs, destination, playerIdx);
        }
//        removePlayer((PandemicGameState)gs, prop.value, playerIdx);
//        placePlayer((PandemicGameState)gs, destination, playerIdx);

        return false;
    }

    public static void placePlayer(PandemicGameState gs, String city, int playerIdx) {
        BoardNode bn = gs.world.getNode(nameHash, city);
        PropertyIntArrayList prop = (PropertyIntArrayList) bn.getProperty(Constants.playersBNHash);
        prop.getValues().add(playerIdx);

        Card playerCard = (Card) gs.getAreas().get(playerIdx).getComponent(Constants.playerCardHash);
        playerCard.setProperty(Constants.playerLocationHash, new PropertyString(city));  // TODO: does this exist?
    }

    public static void removePlayer(PandemicGameState gs, String city, int playerIdx) {
        BoardNode bn = gs.world.getNode(nameHash, city);
        PropertyIntArrayList prop = (PropertyIntArrayList) bn.getProperty(Constants.playersBNHash);
        prop.getValues().remove(new Integer(playerIdx));

        Card playerCard = (Card) gs.getAreas().get(playerIdx).getComponent(Constants.playerCardHash);
        playerCard.setProperty(Constants.playerLocationHash, new PropertyString(null));
    }

    boolean checkNeighbours(BoardNode city1, BoardNode city2) {
        PropertyStringArray neighbours = (PropertyStringArray) city1.getProperty(Constants.neighboursHash);
        PropertyString name = (PropertyString) city2.getProperty(nameHash);
        for (String neighbour : neighbours.getValues()) {
            if (name.value.equals(neighbour)) {
                return true;
            }
        }
        return false;
    }

    boolean checkResearchStations(BoardNode city1, BoardNode city2) {
        PropertyBoolean research1 = (PropertyBoolean) city1.getProperty(Constants.researchStationHash);
        PropertyBoolean research2 = (PropertyBoolean) city2.getProperty(Constants.researchStationHash);
        return research1.value && research2.value;
    }
}
