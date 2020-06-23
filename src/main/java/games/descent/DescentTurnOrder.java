package games.descent;

import core.AbstractGameState;
import core.turnorders.AlternatingTurnOrder;
import games.descent.components.Figure;

public class DescentTurnOrder extends AlternatingTurnOrder {

    // TODO: order is player, overlord(monster group 1), player, overlord (monster group 2) ...
    public DescentTurnOrder(int nPlayers) {
        super(nPlayers);
    }

    @Override
    public void endRound(AbstractGameState gameState) {
        super.endRound(gameState);

        // Reset figures for the next round
        DescentGameState dgs = (DescentGameState) gameState;
        for (Figure f: dgs.getHeroes()) {
            f.resetRound();
        }
    }
}
