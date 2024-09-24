package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.VagabondCharacters;
import games.root_final.components.Item;

import java.util.ArrayList;
import java.util.Objects;

public class VagabondSetup extends AbstractAction {
    public final int playerID;
    public VagabondCharacters character;
    public final boolean passSubGamePhase;

    public VagabondSetup(int playerID, VagabondCharacters character, boolean passSubGamePhase){
        this.playerID = playerID;
        this.character =  character;
        this.passSubGamePhase = passSubGamePhase;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        if(state.getCurrentPlayer()==playerID && state.getPlayerFaction(playerID)== RootParameters.Factions.Vagabond){
            state.setVagabondCharacter(character);
            if(passSubGamePhase){
                state.increaseSubGamePhase();
            }
            for (int i = state.getStartingItems().size()-1; i >= 0; i--){
                if(character.startsWith.contains(state.getStartingItems().get(i).itemType)){
                    if(state.getStartingItems().get(i).itemType == Item.ItemType.bag){
                        state.getBags().add(state.getStartingItems().get(i));
                    }
                    else if(state.getStartingItems().get(i).itemType == Item.ItemType.coin){
                        state.getCoins().add(state.getStartingItems().get(i));
                    }
                    else if(state.getStartingItems().get(i).itemType == Item.ItemType.tea){
                        state.getTeas().add(state.getStartingItems().get(i));
                    } else {
                        state.getSachel().add(state.getStartingItems().get(i));
                    }
                    state.getStartingItems().remove(i);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) {return true;}
        if(obj instanceof VagabondSetup){
            VagabondSetup other = (VagabondSetup) obj;
            return playerID == other.playerID && character.character.equals(other.character.character) && passSubGamePhase == other.passSubGamePhase;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, character);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString()  + " chooses character " + character.character.toString();
    }
}
