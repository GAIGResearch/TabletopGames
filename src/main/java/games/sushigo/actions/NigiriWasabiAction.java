package games.sushigo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard;

public class NigiriWasabiAction extends AbstractAction {
    int playerId;
    int cardIndex;
    SGCard.SGCardType cardType;

    public NigiriWasabiAction(int playerId, int cardIndex, SGCard.SGCardType cardType)
    {
        this.playerId = playerId;
        this.cardIndex = cardIndex;
        this.cardType = cardType;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SGGameState SGGS = (SGGameState) gs;
        SGGS.setPlayerCardPick(cardIndex, playerId);
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
        return new NigiriWasabiAction(playerId, cardIndex, cardType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof DebugAction;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Play " + cardType + " on Wasabi";
    }
}
