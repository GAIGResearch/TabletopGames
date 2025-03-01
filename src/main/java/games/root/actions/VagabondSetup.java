package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.cards.VagabondCharacter;
import games.root.components.Item;

import java.util.Objects;

public class VagabondSetup extends AbstractAction {
    public final int playerID;
    public final VagabondCharacter.CardType characterType;
    public final boolean passSubGamePhase;

    public VagabondSetup(int playerID, VagabondCharacter.CardType characterType, boolean passSubGamePhase){
        this.playerID = playerID;
        this.characterType = characterType;
        this.passSubGamePhase = passSubGamePhase;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        if(state.getCurrentPlayer()==playerID && state.getPlayerFaction(playerID)== RootParameters.Factions.Vagabond){
            VagabondCharacter character = new VagabondCharacter(characterType, ((RootParameters)gs.getGameParameters()).vagabondCharacterInitializer.get(characterType));
            state.setVagabondCharacter(character);
            if(passSubGamePhase){
                state.increaseSubGamePhase();
            }
            for (int i = state.getStartingItems().size()-1; i >= 0; i--){
                if(character.startsWith(state.getStartingItems().get(i).itemType)){
                    if(state.getStartingItems().get(i).itemType == Item.ItemType.bag){
                        state.getBags().add(state.getStartingItems().get(i));
                    }
                    else if(state.getStartingItems().get(i).itemType == Item.ItemType.coin){
                        state.getCoins().add(state.getStartingItems().get(i));
                    }
                    else if(state.getStartingItems().get(i).itemType == Item.ItemType.tea){
                        state.getTeas().add(state.getStartingItems().get(i));
                    } else {
                        state.getSatchel().add(state.getStartingItems().get(i));
                    }
                    state.getStartingItems().remove(i);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public VagabondSetup copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VagabondSetup that = (VagabondSetup) o;
        return playerID == that.playerID && passSubGamePhase == that.passSubGamePhase && characterType == that.characterType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, characterType, passSubGamePhase);
    }

    @Override
    public String toString() {
        return "p" + playerID + " chooses character " + characterType;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString()  + " chooses character " + characterType.toString();
    }
}
