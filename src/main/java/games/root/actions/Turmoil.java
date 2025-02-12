package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.cards.EyrieRulers;
import games.root.components.cards.RootCard;

import java.util.HashMap;
import java.util.Objects;

public class Turmoil extends AbstractAction {
    public final EyrieRulers.CardType ruler;
    public final int playerID;
    public final boolean passGamePhase;

    public Turmoil(int playerID, EyrieRulers.CardType eyrieRuler, boolean passGamePhase){
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
            EyrieRulers ruler = null;
            for (EyrieRulers r : rulers) {
                if (r.ruler == this.ruler) {
                    ruler = r;
                    break;
                }
            }
            if(ruler != null){
                currentState.setActiveRuler(ruler);
                currentState.removeRulerFromList(ruler);
                currentState.increaseActionsPlayed();
                if (currentState.getAvailableRulers().getSize() == 0){
                    //Although in normal game 4 rulers are sufficient for 1 entire game, less intelligent players might struggle
                    // todo hack
                    for (HashMap.Entry<EyrieRulers.CardType, Boolean[]> entry : rp.eyrieRulers.entrySet()) {
                        EyrieRulers ruler1 = new EyrieRulers(entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2], entry.getValue()[3]);
                        currentState.addRulerToRulers(ruler1);
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
                        if(decreePart.get(card).cardType.equals(RootCard.CardType.Vizier)){
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
    public Turmoil copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Turmoil turmoil = (Turmoil) o;
        return playerID == turmoil.playerID && passGamePhase == turmoil.passGamePhase && ruler == turmoil.ruler;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruler, playerID, passGamePhase);
    }

    @Override
    public String toString() {
        return "p" + playerID + " goes into turmoil and chooses a new ruler: " + ruler.toString();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString()  + " goes into turmoil and chooses a new ruler " + ruler.toString() ;
    }
}
