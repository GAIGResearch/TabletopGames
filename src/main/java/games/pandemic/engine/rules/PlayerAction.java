package games.pandemic.engine.rules;

import core.AbstractGameState;
import core.actions.DrawCard;
import core.actions.RearrangeDeckOfCards;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import core.properties.PropertyString;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicTurnOrder;
import games.pandemic.actions.MovePlayer;
import games.pandemic.actions.QuietNight;
import games.pandemic.actions.TreatDisease;
import utilities.Hash;

import static games.pandemic.PandemicConstants.countryHash;
import static core.CoreConstants.nameHash;

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
        PandemicTurnOrder pto = (PandemicTurnOrder) pgs.getTurnOrder();

        if (action instanceof QuietNight) {
            ((PandemicGameState)gs).setQuietNight(true);
        } else if (action instanceof MovePlayer){
            // if player is Medic and a disease has been cured, then it should remove all cubes when entering the city
            int playerIdx = pto.getCurrentPlayer(gs);
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
            Deck<Card> deckTo = (Deck<Card>) gs.getComponentById(((DrawCard) action).getDeckTo());
            if (deckTo.isOverCapacity()) playerHandOverCapacity = deckTo.getOwnerId();
            else playerHandOverCapacity = -1;
        }

        // Check if this was an event action or a reaction. These actions are always played with the event card.
        Card eventCard = action.getCard(gs);
        if (eventCard == null && !(action instanceof RearrangeDeckOfCards) ||  // No card played, and not Forecast - step 2 action played
                eventCard != null && eventCard.getProperty(countryHash) != null  // Card played, but not event
                || pto.reactionsFinished()) {  // Reactions have finished
            // Notify turn step only if an event card was not played, or if this was a reaction.
            // Event cards are free.
            pto.endPlayerTurnStep();
        }
        return true;
    }

    public int getPlayerHandOverCapacity() {
        return playerHandOverCapacity;
    }

    public void setPlayerHandOverCapacity(int playerHandOverCapacity) {
        this.playerHandOverCapacity = playerHandOverCapacity;
    }
}
