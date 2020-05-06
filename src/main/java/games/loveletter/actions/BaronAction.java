package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.IAction;
import core.components.Deck;
import core.components.IDeck;
import core.observations.IPrintable;
import games.explodingkittens.actions.PlayCard;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

public class BaronAction extends PlayCard<LoveLetterCard> implements IAction, IPrintable {

    private final Deck<LoveLetterCard> opponentDeck;
    private final IDeck<LoveLetterCard> playerDeck;
    private final int playerID;
    private final int opponentID;

    public BaronAction(LoveLetterCard card, IDeck<LoveLetterCard> playerHand, IDeck<LoveLetterCard> discardPile,
                        Deck<LoveLetterCard> opponentDeck, int opponentID, int ownPlayerID){
        super(card, playerHand, discardPile);
        this.opponentDeck = opponentDeck;
        this.playerDeck = playerHand;
        this.playerID = ownPlayerID;
        this.opponentID = opponentID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        if (!((LoveLetterGameState)gs).getProtection(opponentID)){
            if (opponentDeck.peek().cardType.getValue() < playerDeck.peek().cardType.getValue())
                ((LoveLetterGameState) gs).killPlayer(opponentID);
            else if (playerDeck.peek().cardType.getValue() < opponentDeck.peek().cardType.getValue())
                ((LoveLetterGameState) gs).killPlayer(playerID);
        }

        return false;
    }

    @Override
    public String toString(){
        return "Baron - compare the cards with player "+ opponentID;
    }
    @Override
    public void printToConsole() {
        System.out.println(this.toString());
    }
}
