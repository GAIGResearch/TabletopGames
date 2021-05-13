package games.sushigo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard;

import java.awt.font.TextHitInfo;

public class PlayCardAction extends AbstractAction {
    int playerId;
    int cardIndex;
    SGCard.SGCardType cardType;

    public PlayCardAction(int playerId, int cardIndex, SGCard.SGCardType cardType)
    {
        this.playerId = playerId;
        this.cardIndex = cardIndex;
        this.cardType = cardType;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SGGameState SGGS = (SGGameState) gs;
        SGGS.setPlayerCardPick(cardIndex, playerId);
        SGGS.setGameScore(SGGS.getCurrentPlayer(), (int)SGGS.getGameScore(SGGS.getCurrentPlayer()) + GetCardScore(cardType, (SGParameters) SGGS.getGameParameters()));
        return true;
    }

    public int GetCardScore(SGCard.SGCardType cardType, SGParameters parameters)
    {
        switch (cardType) {
            case Maki_1:
                return 0;
            case Maki_2:
                return 0;
            case Maki_3:
                return 0;
            case Tempura:
                return 0;
            case Sashimi:
                return 0;
            case Dumpling:
                return parameters.valueDumpling;
            case SquidNigiri:
                return parameters.valueSquidNigiri;
            case SalmonNigiri:
                return parameters.valueSalmonNigiri;
            case EggNigiri:
                return parameters.valueEggNigiri;
            case Wasabi:
                return parameters.multiplierWasabi;
            case Chopsticks:
                return 0;
            case Pudding:
                return 0;
            default:
                return -1;
        }
    }

    @Override
    public AbstractAction copy() {
        return null;
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
        return "Chose " + cardType;
    }
}
