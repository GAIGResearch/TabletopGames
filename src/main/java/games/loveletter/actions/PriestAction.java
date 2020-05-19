package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.IAction;
import core.components.Card;
import core.components.IDeck;
import core.observations.IPrintable;
import games.explodingkittens.actions.PlayCard;
import games.loveletter.LoveLetterGameState;
import core.components.PartialObservableDeck;
import games.loveletter.cards.LoveLetterCard;

public class PriestAction extends PlayCard<LoveLetterCard> implements IAction, IPrintable {

    private final PartialObservableDeck<LoveLetterCard> opponentDeck;
    private final int playerID;
    private final int opponentID;

    public PriestAction(LoveLetterCard card, IDeck<LoveLetterCard> playerHand, IDeck<LoveLetterCard> discardPile,
                        PartialObservableDeck<LoveLetterCard> opponentDeck, int opponentID, int ownPlayerID){
        super(card, playerHand, discardPile);
        this.opponentDeck = opponentDeck;
        this.playerID = ownPlayerID;
        this.opponentID = opponentID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        if (!((LoveLetterGameState)gs).getProtection(opponentID)){
            for (int i = 0; i < opponentDeck.getCards().size(); i++)
                opponentDeck.setVisibilityOfCard(i, playerID, true);
        }

        return false;
    }

    @Override
    public Card getCard() {
        return null;
    }

    @Override
    public String toString(){
        return "Priest - see the cards of player "+ opponentID;
    }

    @Override
    public void printToConsole() {
        System.out.println(toString());
    }
}
