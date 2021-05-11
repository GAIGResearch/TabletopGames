package games.sushigo;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Card;
import core.components.Component;
import core.components.Deck;
import games.sushigo.cards.SGCard;

import java.util.ArrayList;
import java.util.List;

public class SGGameState extends AbstractGameState {
    List<Deck<SGCard>> playerHands;
    List<Deck<SGCard>> playerFields;
    Deck<SGCard> drawPile;
    Deck<SGCard> discardPile;
    int[] playerScore;
    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers      - amount of players for this game.
     */
    public SGGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new SGTurnOrder(nPlayers));
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            addAll(playerHands);
            addAll(playerFields);
            add(drawPile);
            add(discardPile);
        }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        SGGameState copy = new SGGameState(gameParameters.copy(), getNPlayers());
        copy.playerScore = playerScore.clone();

        //Copy player hands
        copy.playerHands = new ArrayList<>();
        for (Deck<SGCard> d : playerHands){
            copy.playerHands.add(d.copy());
        }

        //Copy player fields
        copy.playerFields = new ArrayList<>();
        for (Deck<SGCard> d : playerFields){
            copy.playerFields.add(d.copy());
        }

        //Other decks
        copy.drawPile = drawPile.copy();
        copy.discardPile = discardPile.copy();
        return copy;
    }

    public int[] getPlayerScore() {return playerScore;}

    public List<Deck<SGCard>> getPlayerFields() {return playerFields;}

    public List<Deck<SGCard>> getPlayerDecks() {return playerHands;}

    public List<Deck<SGCard>> getPlayerFields() {return playerFields;}

    @Override
    protected double _getHeuristicScore(int playerId) {
        return 0;
    }

    @Override
    public double getGameScore(int playerId) {
        return 0;
    }

    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        return new ArrayList<Integer>() {{
            for (int i = 0; i < getNPlayers(); i++){
                if (i != playerId){
                    add(playerHands.get(i).getComponentID());
                    for (Component c: playerHands.get(i).getComponents()){
                        add(c.getComponentID());

                    }
                    add(drawPile.getComponentID());
                }
            }
        }};
    }

    @Override
    protected void _reset() {
        playerHands = new ArrayList<>();
        playerFields = new ArrayList<>();
        drawPile = null;
        discardPile = null;
    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }
}
