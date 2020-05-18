package games.pandemic.engine.rules;

import core.AbstractGameState;
import core.actions.DrawCard;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import core.content.PropertyString;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicTurnOrder;
import games.pandemic.actions.MovePlayer;
import games.pandemic.actions.QuietNight;
import games.pandemic.actions.TreatDisease;
import utilities.Hash;

import static utilities.CoreConstants.nameHash;

public class PlayerAction extends RuleNode {

    private int playerHandOverCapacity;
    private int n_initial_disease_cubes;

    public PlayerAction(int n_initial_disease_cubes) {
        super(true);
        this.n_initial_disease_cubes = n_initial_disease_cubes;
        this.playerHandOverCapacity = -1;
    }

    @Override
    protected boolean run(AbstractGameState gs) {
        action.execute(gs);
        PandemicGameState pgs = (PandemicGameState)gs;

        if (action instanceof QuietNight) {
            ((PandemicGameState)gs).setQuietNight(true);
        } else if (action instanceof MovePlayer){
            // if player is Medic and a disease has been cured, then it should remove all cubes when entering the city
            int playerIdx = pgs.getTurnOrder().getCurrentPlayer(gs);
            Card playerCard = (Card) pgs.getComponent(PandemicConstants.playerCardHash, playerIdx);
            String roleString = ((PropertyString)playerCard.getProperty(nameHash)).value;

            if (roleString.equals("Medic")){
                for (String color: PandemicConstants.colors){
                    Counter diseaseToken = (Counter)((PandemicGameState)gs).getComponent(Hash.GetInstance().hash("Disease " + color));
                    String city = ((MovePlayer)action).getDestination();
                    boolean disease_cured = diseaseToken.getValue() > 0;
                    if (disease_cured){
                        new TreatDisease(n_initial_disease_cubes, color, city, true).execute(gs);
                    }
                }
            }
        } else if (action instanceof DrawCard) {
            // Player hand may be over capacity, set parameter to inform next decision
            Deck<Card> deckTo = ((DrawCard) action).getDeckTo();
            if (deckTo.isOverCapacity()) playerHandOverCapacity = deckTo.getOwnerId();
            else playerHandOverCapacity = -1;
        }

        ((PandemicTurnOrder)gs.getTurnOrder()).endPlayerTurnStep();
        return true;
    }

    public int getPlayerHandOverCapacity() {
        return playerHandOverCapacity;
    }

    public void setPlayerHandOverCapacity(int playerHandOverCapacity) {
        this.playerHandOverCapacity = playerHandOverCapacity;
    }
}
