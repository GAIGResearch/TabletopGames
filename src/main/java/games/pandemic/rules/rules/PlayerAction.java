package games.pandemic.rules.rules;

import core.AbstractGameStateWithTurnOrder;
import core.actions.DrawCard;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import core.properties.PropertyString;
import core.rules.Node;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicTurnOrder;
import games.pandemic.actions.Forecast;
import games.pandemic.actions.MovePlayer;
import games.pandemic.actions.QuietNight;
import games.pandemic.actions.TreatDisease;
import utilities.Hash;

import static core.CoreConstants.playerHandHash;
import static core.CoreConstants.nameHash;

public class PlayerAction extends core.rules.rulenodes.PlayerAction {

    private int playerHandOverCapacity;
    private int n_initial_disease_cubes;

    public PlayerAction(int n_initial_disease_cubes) {
        super();
        this.n_initial_disease_cubes = n_initial_disease_cubes;
        this.playerHandOverCapacity = -1;
    }

    /**
     * Copy constructor
     * @param playerAction - Node to be copied
     */
    public PlayerAction(PlayerAction playerAction) {
        super(playerAction);
        this.n_initial_disease_cubes = playerAction.n_initial_disease_cubes;
        this.playerHandOverCapacity = playerAction.playerHandOverCapacity;
    }

    @Override
    protected boolean run(AbstractGameStateWithTurnOrder gs) {
        if(super.run(gs)) {
            PandemicGameState pgs = (PandemicGameState) gs;
            PandemicTurnOrder pto = (PandemicTurnOrder) pgs.getTurnOrder();
            int playerIdx = pto.getCurrentPlayer(gs);

            if (action instanceof QuietNight) {
                pgs.setQuietNight(true);
            } else if (action instanceof Forecast) {
                pgs.setGamePhase(PandemicGameState.PandemicGamePhase.Forecast);
            } else if (action instanceof MovePlayer) {
                // if player is Medic and a disease has been cured, then it should remove all cubes when entering the city
                Card playerCard = (Card) pgs.getComponent(PandemicConstants.playerCardHash, playerIdx);
                String roleString = ((PropertyString) playerCard.getProperty(nameHash)).value;

                if (roleString.equals("Medic")) {
                    for (String color : PandemicConstants.colors) {
                        Counter diseaseToken = (Counter) ((PandemicGameState) gs).getComponent(Hash.GetInstance().hash("Disease " + color));
                        String city = ((MovePlayer) action).getDestination();
                        boolean disease_cured = diseaseToken.getValue() > 0;
                        if (disease_cured) {
                            new TreatDisease(n_initial_disease_cubes, color, city, true).execute(gs);
                        }
                    }
                }
            } else if (action instanceof DrawCard) {
                // Player hand may be over capacity, set parameter to inform next decision
                Deck<Card> deckTo = (Deck<Card>) gs.getComponentById(((DrawCard) action).getDeckTo());
                Deck<Card> playerHand = (Deck<Card>) pgs.getComponentActingPlayer(playerHandHash);
                if (deckTo != null && deckTo.isOverCapacity()) playerHandOverCapacity = deckTo.getOwnerId();
                else if (playerHand != null && playerHand.isOverCapacity()) playerHandOverCapacity = playerIdx;
                else playerHandOverCapacity = -1;
            }

            // Check if this was an event action or a reaction. These actions are always played with the event card.
//            Card eventCard = action.getCard(gs);
//            if (eventCard == null && !(action instanceof RearrangeDeckOfCards) ||  // No card played, and not Forecast - step 2 action played
//                    eventCard != null && eventCard.getProperty(countryHash) != null  // Card played, but not event
//                    || pto.reactionsFinished()) {  // Reactions have finished
//                // Notify turn step only if an event card was not played, or if this was a reaction.
//                // Event cards are free.
//            }
            pto.endPlayerTurnStep();
            return true;
        }
        return false;
    }

    public int getPlayerHandOverCapacity() {
        return playerHandOverCapacity;
    }

    public void setPlayerHandOverCapacity(int playerHandOverCapacity) {
        this.playerHandOverCapacity = playerHandOverCapacity;
    }

    @Override
    protected Node _copy() {
        return new PlayerAction(this);
    }
}
