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
        if (sspgs.getDrawPile().getSize() == 0) {
            throw new RuntimeException("NO CARDS LEFT IN DRAW PILE FOR FISH DUO!!!\n SHOULD ALREADY BE CHECKED BEFORE!");
        }
        SeaSaltPaperCard card = sspgs.getDrawPile().draw();
        sspgs.getPlayerHands().get(playerId).add(card);
        return true;
    }

    @Override
    public FishDuo copy() {
        return new FishDuo(playerId, cardsIdx.clone());
    }

    @Override
    public String toString() {
        return "Fish Duo Actions: Draw the top card from the draw deck";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Fish Duo";
    }
}
