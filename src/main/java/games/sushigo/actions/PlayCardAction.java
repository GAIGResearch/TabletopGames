package games.sushigo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard;

import java.awt.font.TextHitInfo;
import java.util.Objects;

public class PlayCardAction extends AbstractAction {
    final int playerId;
    final SGCard.SGCardType cardType;

    public PlayCardAction(int playerId, SGCard.SGCardType cardType)
    {
        this.playerId = playerId;
        this.cardType = cardType;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SGGameState SGGS = (SGGameState) gs;
        Deck<SGCard> hand =  SGGS.getPlayerDecks().get(playerId);
        int cardIndex = -1;
        for (int i = 0; i < hand.getSize(); i++) {
            if (hand.get(i).type == cardType) {
                cardIndex = i;
                break;
            }
        }
        if (cardIndex == -1)
            throw new AssertionError("No card found in hand of type " + cardType);
        if(SGGS.getPlayerChopSticksActivated(playerId) && SGGS.getPlayerExtraTurns(playerId) == 0)
        {
            SGGS.setPlayerExtraCardPick(cardIndex, playerId);
        }
        else SGGS.setPlayerCardPick(cardIndex, playerId);
        SGGS.setPlayerScoreToAdd(playerId,SGGS.getPlayerScoreToAdd(playerId) + GetCardScore(cardType, SGGS, playerId));
        return true;
    }

    public int GetCardScore(SGCard.SGCardType cardType, SGGameState SGGS, int playerId)
    {
        SGParameters parameters = (SGParameters) SGGS.getGameParameters();
        switch (cardType) {
            case Maki_1:
                return 0;
            case Maki_2:
                return 0;
            case Maki_3:
                return 0;
            case Tempura:
                SGGS.setPlayerTempuraAmount(playerId, SGGS.getPlayerTempuraAmount(playerId) + 1);
                if(SGGS.getPlayerTempuraAmount(playerId) % 2 == 0) return parameters.valueTempuraPair;
                else return 0;
            case Sashimi:
                SGGS.setPlayerSashimiAmount(playerId, SGGS.getPlayerSashimiAmount(playerId) + 1);
                if(SGGS.getPlayerSashimiAmount(playerId) % 3 == 0) return parameters.valueSashimiTriss;
                else return 0;
            case Dumpling:
                SGGS.setPlayerDumplingAmount(playerId, SGGS.getPlayerDumplingAmount(playerId) + 1);
                int amount = SGGS.getPlayerDumplingAmount(playerId);
                if(amount == 1) return parameters.valueDumpling;
                else if(amount == 2) return parameters.valueDumplingPair - parameters.valueDumpling;
                else if(amount == 3) return parameters.valueDumplingTriss - parameters.valueDumplingPair;
                else if(amount == 4) return parameters.valueDumplingQuad - parameters.valueDumplingTriss;
                else if(amount == 5) return parameters.valueDumplingPent - parameters.valueDumplingQuad;
                else return 0;
            case SquidNigiri:
                return parameters.valueSquidNigiri;
            case SalmonNigiri:
                return parameters.valueSalmonNigiri;
            case EggNigiri:
                return parameters.valueEggNigiri;
            case Wasabi:
                SGGS.setPlayerWasabiAvailable(playerId, SGGS.getPlayerWasabiAvailable(playerId) + 1);
                return 0;
            case Chopsticks:
                SGGS.setPlayerChopSticksAmount(playerId, SGGS.getPlayerChopSticksAmount(playerId) + 1);
                return 0;
            case Pudding:
                return 0;
            default:
                return -1;
        }
    }

    @Override
    public AbstractAction copy() {
        // immutable state
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof PlayCardAction) {
            // deliberately not including the cardIndex, as these actions are equivalent in play
            PlayCardAction other = (PlayCardAction) obj;
            return other.playerId == playerId && other.cardType == cardType;
        }
        return false;
    }

    @Override
    public int hashCode() {
        // deliberately not including the cardIndex
        return Objects.hash(playerId, cardType) - 12692;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Play " + cardType;
    }


    @Override
    public String toString() {
        return "Play " + cardType;
    }
}
