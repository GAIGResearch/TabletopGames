package games.explodingkittens;
import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import games.explodingkittens.actions.IsNopeable;
import games.explodingkittens.cards.ExplodingKittenCard;
import utilities.Utils;

public class ExplodingKittensHeuristic implements IStateHeuristic {

    double explodingValue = -1;
    double defuseValue = 1;
    double regularValue = -0.01;
    double seeFutureValue = -0.3;  // Play it
    double nopeValue = -0.5;  // Play it
    double attackValue = -0.4;  // Play it
    double skipValue = 0.2;
    double favorValue = -0.1;
    double shuffleValue = -0.2;

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gs;
        Utils.GameResult gameStatus = gs.getGameStatus();

        if (gameStatus == Utils.GameResult.LOSE)
            return -1;
        if (gameStatus == Utils.GameResult.WIN)
            return 1;

        double cardValues = 0.0;
        for (ExplodingKittenCard card: ekgs.playerHandCards.get(playerId).getComponents()) {
            cardValues += getCardValue(ekgs, card);
        }

        return cardValues/ekgs.playerHandCards.get(playerId).getSize();
    }

    // TODO: check state more
    double getCardValue(ExplodingKittensGameState ekgs, ExplodingKittenCard card) {
        switch(card.cardType) {
            case EXPLODING_KITTEN:
                return explodingValue;
            case DEFUSE:
                return defuseValue;
            case NOPE:
                if (ekgs.actionStack.size() > 0 && ekgs.actionStack.get(0) instanceof IsNopeable) {
                    return nopeValue;
                } else return 0;  // Neutral
            case ATTACK:
                return attackValue;
            case SKIP:
                return skipValue;
            case FAVOR:
                return favorValue;
            case SHUFFLE:
                return shuffleValue;
            case SEETHEFUTURE:
                return seeFutureValue;  // TODO: higher if future not already known, otherwise low
            default:
                return regularValue;
        }
    }
}