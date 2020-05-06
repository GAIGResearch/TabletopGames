package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.IAction;
import core.components.Deck;
import core.components.IDeck;
import core.observations.IPrintable;
import games.explodingkittens.actions.PlayCard;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

public class GuardAction extends PlayCard<LoveLetterCard> implements IAction, IPrintable {

    private final Deck<LoveLetterCard> opponentDeck;
    private final int opponentID;
    private final LoveLetterCard.CardType cardType;

    public GuardAction(LoveLetterCard card, IDeck<LoveLetterCard> playerHand, IDeck<LoveLetterCard> discardPile,
                        Deck<LoveLetterCard> opponentDeck, int opponentID, LoveLetterCard.CardType cardtype){
        super(card, playerHand, discardPile);
        this.opponentDeck = opponentDeck;
        this.opponentID = opponentID;
        this.cardType = cardtype;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        if (!((LoveLetterGameState)gs).getProtection(opponentID)){
            for (LoveLetterCard card : opponentDeck.getCards())
                if (card.cardType == this.cardType)
                    ((LoveLetterGameState) gs).killPlayer(opponentID);
        }

        return false;
    }


    @Override
    public String toString(){
        return "Guard - guess if player " + opponentID + " hold card " + cardType.name();
    }

    @Override
    public void printToConsole() {
        System.out.println();
    }
}
