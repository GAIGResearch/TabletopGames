package games.seasaltpaper.actions;

import core.AbstractGameState;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.cards.SeaSaltPaperCard;

public class FishDuo extends PlayDuo {
    public FishDuo(int playerId, int[] cardsIdx) {
        super(playerId, cardsIdx);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        SeaSaltPaperGameState sspgs = (SeaSaltPaperGameState) gs;
        SeaSaltPaperCard card = sspgs.getDrawPile().draw();
        sspgs.getPlayerHands().get(playerId).add(card);
        return true;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Fish Duo Actions: Draw the top card from the draw deck";
    }
}
