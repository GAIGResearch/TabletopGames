package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import evaluation.metrics.Event;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.Item;
import games.root_final.components.RootBoardNodeWithRootEdges;
import players.human.HumanGUIPlayer;

import java.util.Objects;

public class VagabondExplore extends AbstractAction {
    public final int playerID;
    public Item.ItemType foundItemType;
    public VagabondExplore(int playerID){
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            RootBoardNodeWithRootEdges currentPosition = currentState.getGameMap().getVagabondClearing();
            if (currentPosition.hasBuilding(RootParameters.BuildingType.Ruins)){
                for (Item item: currentState.getSachel()){
                    if (item.itemType == Item.ItemType.torch && item.refreshed){
                        item.refreshed = false;
                        currentPosition.removeBuilding(RootParameters.BuildingType.Ruins);
                        Item newItem = currentState.getRandomRuinItem();
                        switch (newItem.itemType){
                            case bag:
                                currentState.getBags().add(newItem);
                                break;
                            case tea:
                                currentState.getTeas().add(newItem);
                                break;
                            case coin:
                                currentState.getCoins().add(newItem);
                                break;
                            default:
                                currentState.getSachel().add(newItem);
                                break;
                        }
                        foundItemType = newItem.itemType;
                        currentState.increaseActionsPlayed();
                        currentState.addGameScorePLayer(playerID,1);
                        currentState.logEvent(Event.GameEvent.GAME_EVENT, currentState.getPlayerFaction(playerID).toString() + " found " + foundItemType.toString());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if (obj instanceof VagabondExplore ve){
            return playerID == ve.playerID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("Explore", playerID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        if (foundItemType == null){
            return gs.getPlayerFaction(playerID).toString() + " explores ruins";
        }
        return gs.getPlayerFaction(playerID).toString() + " explored ruins and found " + foundItemType.toString();
    }
}
