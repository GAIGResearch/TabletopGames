package games.sushigo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard;

import java.util.Objects;

public class NigiriWasabiAction extends AbstractAction {
    final int playerId;
    final int cardIndex;
    final SGCard.SGCardType cardType;

    public NigiriWasabiAction(int playerId, int cardIndex, SGCard.SGCardType cardType)
    {
        this.playerId = playerId;
        this.cardIndex = cardIndex;
        this.cardType = cardType;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SGGameState SGGS = (SGGameState) gs;
        if(SGGS.getPlayerChopSticksActivated(playerId) && SGGS.getPlayerExtraTurns(playerId) == 0)
        {
            SGGS.setPlayerExtraCardPick(cardIndex, playerId);
        }
        else SGGS.setPlayerCardPick(cardIndex, playerId);
        SGGS.setPlayerScoreToAdd(playerId,SGGS.getPlayerScoreToAdd(playerId) + GetCardScore(cardType, SGGS, playerId));
        SGGS.setPlayerWasabiAvailable(playerId, SGGS.getPlayerWasabiAvailable(playerId) - 1);
        return true;
    }

    private int GetCardScore(SGCard.SGCardType cardType, SGGameState SGGS, int playerId)
    {
        SGParameters parameters = (SGParameters) SGGS.getGameParameters();
        switch (cardType)
        {
            case SquidNigiri:
                return parameters.valueSquidNigiri * parameters.multiplierWasabi;
            case SalmonNigiri:
                return parameters.valueSalmonNigiri * parameters.multiplierWasabi;
            case EggNigiri:
                return parameters.valueEggNigiri * parameters.multiplierWasabi;
            default:
                return -1;
        }
    }

    @Override
    public AbstractAction copy() {
        // immutable
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof NigiriWasabiAction) {
            // deliberately not including the cardIndex, as these actions are equivalent in play
            NigiriWasabiAction other = (NigiriWasabiAction) obj;
            return other.playerId == playerId && other.cardType == cardType;
        }
        return false;
    }

    @Override
    public int hashCode() {
        // deliberately not including the cardIndex
        return Objects.hash(playerId, cardType) - 17;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Play " + cardType + " on Wasabi";
    }
}
