package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.EyrieRulers;

import java.util.HashMap;
import java.util.Objects;

public class ChooseRuler extends AbstractAction {

    public final EyrieRulers ruler;
    public final int playerID;
    public final boolean passSubGamePhase;
    public ChooseRuler(int playerID, EyrieRulers eyrieRuler, boolean passSubGamePhase){
        ruler = eyrieRuler;
        this.playerID = playerID;
        this.passSubGamePhase =passSubGamePhase;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        RootParameters rp = (RootParameters) gs.getGameParameters();
        if(currentState.getCurrentPlayer() == playerID){
            Deck<EyrieRulers> rulers = currentState.getAvailableRulers();
            if(rulers.contains(ruler)){
                currentState.setActiveRuler(ruler);
                currentState.removeRulerFromList(ruler);
                currentState.increaseActionsPlayed();
                if (currentState.getAvailableRulers().getSize() == 0){
                    //Although in normal game 4 rulers are sufficient for 1 entire game, less intelligent players might struggle
                    for (HashMap.Entry<EyrieRulers.CardType, Boolean[]> entry : rp.eyrieRulers.entrySet()) {
                        EyrieRulers ruler = new EyrieRulers(entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2], entry.getValue()[3]);
                        currentState.addRulerToRulers(ruler);
                    }
                }
                if(passSubGamePhase){
                    currentState.increaseSubGamePhase();
                    currentState.setActionsPlayed(0);
                }
                for(int i = 0; i < 4; i++){
                    if (currentState.getViziers().getSize()==0){
                        return false;
                    }
                    switch (i){
                        case 0:
                            if(ruler.vizierRecruit){
                                currentState.addToDecree(i,currentState.getViziers().draw());
                            }
                            break;
                        case 1:
                            if(ruler.vizierMove){
                                currentState.addToDecree(i,currentState.getViziers().draw());
                            }
                            break;
                        case 2:
                            if(ruler.vizierBattle){
                                currentState.addToDecree(i,currentState.getViziers().draw());
                            }
                            break;
                        case 3:
                            if(ruler.vizierBuild){
                                currentState.addToDecree(i,currentState.getViziers().draw());
                            }
                            break;
                    }
                }
            }

        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return this;//immutable
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ChooseRuler){
            ChooseRuler other = (ChooseRuler)obj;
            return playerID == other.playerID && ruler.equals(other.ruler);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID,ruler);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return ruler.toString() + " is selected";
    }
}
