package games.loveletter.actions;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import java.util.ArrayList;
import java.util.List;

/**
 * The Priest allows a player to see another player's hand cards.
 * This has no effect in case the game is fully observable.
 */
public class PriestAction extends PlayCard implements IPrintable {

    private transient LoveLetterCard.CardType opponentCard;

    public PriestAction(int playerID, int opponentID, boolean canExecuteEffect) {
        super(LoveLetterCard.CardType.Priest, playerID, opponentID, null, null, canExecuteEffect);
    }

    @Override
    protected boolean _execute(LoveLetterGameState llgs) {
        PartialObservableDeck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(targetPlayer);

        // Set all cards to be visible by the current player
        for (int i = 0; i < opponentDeck.getComponents().size(); i++)
            opponentDeck.setVisibilityOfComponent(i, playerID, true);

        opponentCard = opponentDeck.get(0).cardType;
        if (llgs.getCoreGameParameters().recordEventHistory) {
            llgs.recordHistory("Priest sees " + opponentCard);
        }
        return true;
    }

    @Override
    public String _toString(){
        return "Priest (" + playerID + " sees " + (opponentCard != null? opponentCard : "card") + " of " + targetPlayer + ")";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriestAction)) return false;
        return super.equals(o);
    }

    @Override
    public PriestAction copy() {
        PriestAction copy = new PriestAction(playerID, targetPlayer, canExecuteEffect);
        copy.opponentCard = opponentCard;
        return copy;
    }

    public static List<? extends PlayCard> generateActions(LoveLetterGameState gs, int playerID) {
        List<PlayCard> cardActions = new ArrayList<>();
        for (int targetPlayer = 0; targetPlayer < gs.getNPlayers(); targetPlayer++) {
            if (targetPlayer == playerID || gs.getPlayerResults()[targetPlayer] == CoreConstants.GameResult.LOSE_ROUND || gs.isProtected(targetPlayer))
                continue;
            cardActions.add(new PriestAction(playerID, targetPlayer, true));
        }
        if (cardActions.size() == 0) cardActions.add(new PriestAction(playerID, -1, false));
        return cardActions;
    }
}
