package games.loveletter.actions;

import core.AbstractGameState;
import core.actions.IAction;
import core.components.IDeck;
import core.observations.IPrintable;
import games.explodingkittens.actions.PlayCard;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

public class PrincessAction extends PlayCard<LoveLetterCard> implements IAction, IPrintable {

    private final int playerID;

    public PrincessAction(LoveLetterCard card, IDeck<LoveLetterCard> playerHand, IDeck<LoveLetterCard> discardPile,
                          int playerIndex){
        super(card, playerHand, discardPile);
        this.playerID = playerIndex;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        ((LoveLetterGameState)gs).killPlayer(playerID);
        return false;
    }

    @Override
    public String toString(){
        return "Princess - discard this card and lose the game";
    }

    @Override
    public void printToConsole() {
        System.out.println(toString());
    }
}
