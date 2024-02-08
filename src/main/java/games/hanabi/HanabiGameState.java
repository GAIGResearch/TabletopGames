package games.hanabi;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import core.components.Counter;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import core.turnorders.AlternatingTurnOrder;
import games.GameType;



import java.util.ArrayList;
import java.util.List;

public class HanabiGameState extends AbstractGameState implements IPrintable {

    List<PartialObservableDeck<HanabiCard>> playerDecks;
    Deck<HanabiCard> drawDeck;
    Deck<HanabiCard> discardDeck;
    Counter failCounter;
    Counter hintCounter;
    List<HanabiCard> currentCard;
    int endTurn = getNPlayers() + 1;


    public HanabiGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Hanabi;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            addAll(playerDecks);
            add(drawDeck);
            add(discardDeck);
            add(failCounter);
            add(hintCounter);
            addAll(currentCard);
        }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        HanabiGameState copy = new HanabiGameState(gameParameters.copy(), getNPlayers());
        copy.playerDecks = new ArrayList<>();
        for (PartialObservableDeck<HanabiCard> d : playerDecks) {
            copy.playerDecks.add(d.copy(playerId));
        }
        copy.currentCard = new ArrayList<>();
        for (HanabiCard d : currentCard) {
            copy.currentCard.add(d.copy());
        }

        copy.drawDeck = drawDeck.copy();
        copy.discardDeck = discardDeck.copy();
        copy.hintCounter = hintCounter.copy();
        copy.failCounter = failCounter.copy();

        // TODO: This needs to redeterminise the hidden information
        // As currently implemented all players will see the same information
        return copy;
    }
    public Deck<HanabiCard> getDrawDeck() {
        return drawDeck;
    }

    public Deck<HanabiCard> getDiscardDeck() {
        return discardDeck;
    }
    public List<HanabiCard> getCurrentCard() {
        return currentCard;
    }

    public List<PartialObservableDeck<HanabiCard>> getPlayerDecks() {
        return playerDecks;
    }
    public Counter getHintCounter() {
        return hintCounter;
    }
    public Counter getFailCounter() {
        return failCounter;
    }
    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal() || playerResults[playerId] == CoreConstants.GameResult.WIN_GAME) {
            double correctWeight = 0.3;
            double hintWeight = 0.05;
            double failWeight = 0.65;
            int total = 0;
            for (HanabiCard cd : currentCard) {
                total += cd.number;
            }
            return correctWeight * total*1.0 / 25 - hintWeight * (hintCounter.getValue()*1.0/hintCounter.getMaximum()) + failWeight * failCounter.getValue() * 1.0 / failCounter.getMaximum();
        }
        return playerResults[playerId].value;
    }

    @Override
    public double getGameScore(int playerId) {
        int total = 0;
        for(HanabiCard cd: currentCard){
            total += cd.number;
        }
        return total;
    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }
}
