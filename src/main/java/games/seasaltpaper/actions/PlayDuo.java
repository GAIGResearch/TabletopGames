package games.seasaltpaper.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.SeaSaltPaperParameters;
import games.seasaltpaper.cards.SeaSaltPaperCard;

import java.util.Arrays;
import java.util.Objects;

public abstract class PlayDuo extends AbstractAction {

    final int playerId;
    final int[] cardsIdx;

    public PlayDuo(int playerId, int[] cardsIdx) {
        this.playerId = playerId;
        this.cardsIdx = cardsIdx;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SeaSaltPaperGameState sspgs = (SeaSaltPaperGameState) gs;
        PartialObservableDeck<SeaSaltPaperCard> playerHand = sspgs.getPlayerHands().get(playerId);
        Deck<SeaSaltPaperCard> playerDiscard = sspgs.getPlayerDiscards().get(playerId);

        // discard duo cards and put them into playerDiscard
        SeaSaltPaperCard[] duoCards = {playerHand.peek(cardsIdx[0]), playerHand.peek(cardsIdx[1])};
        playerHand.remove(duoCards[0]); playerHand.remove(duoCards[1]);
        playerDiscard.add(duoCards[0]); playerDiscard.add(duoCards[1]);
        SeaSaltPaperParameters params = (SeaSaltPaperParameters) sspgs.getGameParameters();
        sspgs.playerPlayedDuoPoints[playerId] += params.duoBonusDict.get(duoCards[0].getCardSuite());
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayDuo playDuo = (PlayDuo) o;
        return playerId == playDuo.playerId && Arrays.equals(cardsIdx, playDuo.cardsIdx);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(playerId);
        result = 31 * result + Arrays.hashCode(cardsIdx);
        return result;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "AbstractClass for Duo Action";
    }
}
