package games.pandemic.engine.conditions;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import games.pandemic.PandemicGameState;
import games.pandemic.engine.rules.PlayerAction;

import static utilities.CoreConstants.playerHandHash;

@SuppressWarnings("unchecked")
public class PlayerHandOverCapacity extends ConditionNode {
    private int playerId;

    public PlayerHandOverCapacity() {
        this.playerId = -2;  // Current player by default
    }

    @Override
    public boolean test(AbstractGameState gs) {
        Deck<Card> playerDeck;
        PandemicGameState pgs = (PandemicGameState)gs;

        if (parent instanceof PlayerAction) {
            playerId = ((PlayerAction) parent).getPlayerHandOverCapacity();
        }

        if (playerId == -2) {
            // This is the current player
            playerDeck = (Deck<Card>) pgs.getComponentActingPlayer(playerHandHash);
        } else if (playerId == -1) {
            return false;
        } else {
            playerDeck = (Deck<Card>) pgs.getComponent(playerHandHash, playerId);
        }

        if (parent instanceof PlayerAction) {
            ((PlayerAction) parent).setPlayerHandOverCapacity(-1);
        }

        return playerDeck != null && playerDeck.isOverCapacity();
    }

}
