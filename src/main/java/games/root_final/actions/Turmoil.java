package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.EyrieRulers;
import games.root_final.cards.RootCard;

import java.util.HashMap;
import java.util.Objects;

public class Turmoil extends AbstractAction {
    public final EyrieRulers ruler;
    public final int playerID;
    public final boolean passGamePhase;
    public Turmoil(int playerID, EyrieRulers eyrieRuler, boolean passGamePhase){
        ruler = eyrieRuler;
        this.playerID = playerID;
        this.passGamePhase =passGamePhase;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        RootParameters rp = (RootParameters) currentState.getGameParameters();

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
                if(passGamePhase){
                    currentState.setGamePhase(RootGameState.RootGamePhase.Evening);
                    currentState.setActionsPlayed(0);
                    currentState.setPlayerSubGamePhase(0);
                }
                for(int e = 0; e < 4; e++){
                    Deck<RootCard> decreePart = currentState.getDecree().get(e);
                    for (int card=decreePart.getSize()-1; card>=0;card--){
                        if (decreePart.get(card).suit == RootParameters.ClearingTypes.Bird){
                            currentState.removeGameScorePlayer(playerID);
                        }
                        if(decreePart.get(card).cardtype.equals(RootCard.CardType.Vizier)){
                            currentState.getViziers().add(decreePart.get(card));
                            decreePart.remove(card);
                        }else{
                            currentState.getDiscardPile().add(decreePart.get(card));
                            decreePart.remove(card);
                        }
                    }
                }
                for(int i = 0; i < 4; i++){
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
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if(obj instanceof Turmoil){
            Turmoil other = (Turmoil) obj;
            return playerID == other.playerID && ruler.equals(other.ruler) && passGamePhase == other.passGamePhase;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("Turmoil", playerID, ruler.hashCode(), passGamePhase);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString()  + " goes into turmoil and chooses a new ruler " + ruler.ruler.toString() ;
    }
}
