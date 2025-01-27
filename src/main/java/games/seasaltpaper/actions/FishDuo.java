package games.seasaltpaper.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
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
//            System.out.println("NO CARDS LEFT IN DRAW PILE FOR FISH DUO!!!"); // SHOULD NEVER REACH HERE
            throw new RuntimeException("NO CARDS LEFT IN DRAW PILE FOR FISH DUO!!!\n SHOULD ALREADY BE CHECKED BEFORE!");
//            return false;
        }
        SeaSaltPaperCard card = sspgs.getDrawPile().draw();
        card.setVisible(playerId, true);
        sspgs.getPlayerHands().get(playerId).add(card);
        return true;
    }

    @Override
    public FishDuo copy() {
        return this;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Fish Duo Actions: Draw the top card from the draw deck";
    }
}
