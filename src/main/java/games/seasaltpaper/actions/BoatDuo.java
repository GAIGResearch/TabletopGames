package games.seasaltpaper.actions;

import core.AbstractGameState;
import games.seasaltpaper.SeaSaltPaperGameState;

public class BoatDuo extends PlayDuo {

    public BoatDuo(int playerId, int[] cardsIdx) {
        super(playerId, cardsIdx);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        SeaSaltPaperGameState sspg = (SeaSaltPaperGameState) gs;
        sspg.resetTurn();
        return true;
    }

    @Override
    public BoatDuo copy() {
        return new BoatDuo(playerId, cardsIdx.clone());
    }

    @Override
    public String toString() {
        return "Boat Duo Action: Immediately take another turn";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Boat Duo";
    }
}
