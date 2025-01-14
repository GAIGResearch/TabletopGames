package games.seasaltpaper.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.SeaSaltPaperParameters;
import games.seasaltpaper.cards.SeaSaltPaperCard;

public abstract class PlayDuo extends AbstractAction {

    int playerId;
    int[] cardsIdx;

    public PlayDuo(int playerId, int[] cardsIdx) {
        this.playerId = playerId;
        this.cardsIdx = cardsIdx;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SeaSaltPaperGameState sspgs = (SeaSaltPaperGameState) gs;
        PartialObservableDeck<SeaSaltPaperCard> playerHand = sspgs.getPlayerHands().get(playerId);
        Deck<SeaSaltPaperCard> playerDiscard = sspgs.getPlayerDiscards().get(playerId);

//        // make the duo cards visible
//        boolean[] visibility = new boolean[sspg.getNPlayers()];
//        Arrays.fill(visibility, true);
//        playerHand.setVisibilityOfComponent(cardsIdx[0], visibility);
//        playerHand.setVisibilityOfComponent(cardsIdx[1], visibility);

        // discard duo cards and put them into playerDiscard
        SeaSaltPaperCard[] duoCards = {playerHand.peek(cardsIdx[0]), playerHand.peek(cardsIdx[1])};
        playerHand.remove(duoCards[0]); playerHand.remove(duoCards[1]);
        playerDiscard.add(duoCards[0]); playerDiscard.add(duoCards[1]);
        SeaSaltPaperParameters params = (SeaSaltPaperParameters) sspgs.getGameParameters();
        sspgs.playerPlayedDuoPoints[playerId] += params.duoBonusDict.get(duoCards[0].getCardSuite());
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "AbstractClass for Duo Action";
    }
}
