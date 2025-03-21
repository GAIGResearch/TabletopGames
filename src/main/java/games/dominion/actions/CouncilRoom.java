package games.dominion.actions;

import core.actions.AbstractAction;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;

// Makes each other player draw a card
public class CouncilRoom extends DominionAction{

    public CouncilRoom(int playerId) {
        super(CardType.COUNCIL_ROOM, playerId);
    }

    public CouncilRoom(int playerId, boolean dummy) {
        super(CardType.COUNCIL_ROOM, playerId, dummy);
    }

    @Override
    boolean _execute(DominionGameState state) {
        for (int i = 0; i < state.getNPlayers(); i++) {
            if (i != player) {
                state.drawCard(i);
            }
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        CouncilRoom retValue = new CouncilRoom(player, dummyAction);
        return retValue;
    }
    
}
