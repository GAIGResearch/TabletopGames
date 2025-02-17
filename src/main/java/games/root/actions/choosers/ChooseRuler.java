package games.root.actions.choosers;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.cards.EyrieRulers;

import java.util.HashMap;
import java.util.Objects;

public class ChooseRuler extends AbstractAction {

    public final int playerID;
    public final int rulerIdx, rulerID;
    public final boolean passSubGamePhase;

    public ChooseRuler(int playerID, int rulerIdx, int rulerID, boolean passSubGamePhase){
        this.playerID = playerID;
        this.rulerIdx = rulerIdx;
        this.rulerID = rulerID;
        this.passSubGamePhase =passSubGamePhase;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        RootParameters rp = (RootParameters) gs.getGameParameters();
        if(currentState.getCurrentPlayer() == playerID){
            Deck<EyrieRulers> rulers = currentState.getAvailableRulers();
            EyrieRulers ruler = rulers.pick(rulerIdx);
            currentState.setActiveRuler(ruler);
            currentState.increaseActionsPlayed();
            if (currentState.getAvailableRulers().getSize() == 0){
                //Although in normal game 4 rulers are sufficient for 1 entire game, less intelligent players might struggle
                // todo hack
                for (HashMap.Entry<EyrieRulers.CardType, Boolean[]> entry : rp.eyrieRulers.entrySet()) {
                    EyrieRulers rulern = new EyrieRulers(entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2], entry.getValue()[3]);
                    currentState.addRulerToRulers(rulern);
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
        return false;
    }

    @Override
    public ChooseRuler copy() {
        return this;//immutable
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChooseRuler that = (ChooseRuler) o;
        return playerID == that.playerID && rulerIdx == that.rulerIdx && rulerID == that.rulerID && passSubGamePhase == that.passSubGamePhase;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, rulerIdx, rulerID, passSubGamePhase);
    }

    @Override
    public String toString() {
        return "Ruler " + rulerIdx + " is selected";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return gameState.getComponentById(rulerID).toString() + " is selected";
    }
}
