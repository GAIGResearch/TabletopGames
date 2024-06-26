package games.pandemic.rules.conditions;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;
import core.rules.Node;
import core.rules.nodetypes.ConditionNode;
import games.pandemic.PandemicGameState;
import games.pandemic.rules.rules.PlayerAction;

import static core.CoreConstants.playerHandHash;

@SuppressWarnings("unchecked")
public class PlayerHandOverCapacity extends ConditionNode {
    private int playerId;

    public PlayerHandOverCapacity() {
        this.playerId = -2;  // Current player by default
    }

    /**
     * Copy constructor
     * @param playerHandOverCapacity - Node to be copied
     */
    public PlayerHandOverCapacity(PlayerHandOverCapacity playerHandOverCapacity) {
        super(playerHandOverCapacity);
        this.playerId = playerHandOverCapacity.playerId;
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

//        if (playerDeck != null && !playerDeck.isOverCapacity()) {
//            if (parent instanceof PlayerAction) {
//                ((PlayerAction) parent).setPlayerHandOverCapacity(-1);
//            }
//            playerId = -1;
//        }

        return playerDeck != null && playerDeck.isOverCapacity();
    }

    @Override
    protected Node _copy() {
        return new PlayerHandOverCapacity(this);
    }

}
