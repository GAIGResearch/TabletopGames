package actions;

import components.BoardNode;
import components.Card;
import content.PropertyBoolean;
import content.PropertyIntArrayList;
import content.PropertyString;
import content.PropertyStringArray;
import core.GameState;
import pandemic.PandemicGameState;
import utilities.Hash;

import static pandemic.PandemicForwardModel.playersBNHash;
import static pandemic.PandemicGameState.playerCardHash;

public class MovePlayer implements Action {
    static int playerLocationHash = Hash.GetInstance().hash("playerLocation");
    static int neighboursHash = Hash.GetInstance().hash("neighbours");
    static int nameHash = Hash.GetInstance().hash("name");
    static int researchStationHash = Hash.GetInstance().hash("researchStation");

    int playerIdx;
    String destination;

    MovePlayer(int playerIdx, String city) {
        this.playerIdx = playerIdx;
        this.destination = city;
    }

    @Override
    public boolean execute(GameState gs) {
        PropertyString prop = (PropertyString) ((Card) gs.getAreas().get(playerIdx).getComponent(playerCardHash)).getProperty(playerLocationHash);
        BoardNode currentCity = ((PandemicGameState)gs).findBoardNode(prop.value);
        BoardNode destinationCity = ((PandemicGameState)gs).findBoardNode(destination);

        if (checkNeighbours(currentCity, destinationCity) || checkResearchStations(currentCity, destinationCity)) {
            removePlayer((PandemicGameState)gs, prop.value, playerIdx);
            placePlayer((PandemicGameState)gs, destination, playerIdx);
        }

        return false;
    }

    public static void placePlayer(PandemicGameState gs, String city, int playerIdx) {
        PropertyIntArrayList prop = (PropertyIntArrayList) gs.findBoardNode(city).getProperty(playersBNHash);
        prop.getValues().add(playerIdx);

        Card playerCard = (Card) gs.getAreas().get(playerIdx).getComponent(playerCardHash);
        playerCard.setProperty(playerLocationHash, new PropertyString("Atlanta"));  // TODO: does this exist?
    }

    public static void removePlayer(PandemicGameState gs, String city, int playerIdx) {
        PropertyIntArrayList prop = (PropertyIntArrayList) gs.findBoardNode(city).getProperty(playersBNHash);
        prop.getValues().remove(playerIdx);

        Card playerCard = (Card) gs.getAreas().get(playerIdx).getComponent(playerCardHash);
        playerCard.setProperty(playerLocationHash, new PropertyString(null));
    }

    boolean checkNeighbours(BoardNode city1, BoardNode city2) {
        PropertyStringArray neighbours = (PropertyStringArray) city1.getProperty(neighboursHash);
        PropertyString name = (PropertyString) city2.getProperty(nameHash);
        for (String neighbour : neighbours.getValues()) {
            if (name.value.equals(neighbour)) {
                return true;
            }
        }
        return false;
    }

    boolean checkResearchStations(BoardNode city1, BoardNode city2) {
        PropertyBoolean research1 = (PropertyBoolean) city1.getProperty(researchStationHash);
        PropertyBoolean research2 = (PropertyBoolean) city2.getProperty(researchStationHash);
        return research1.value && research2.value;
    }
}
