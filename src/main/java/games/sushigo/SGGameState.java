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
        }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        SGGameState copy = new SGGameState(gameParameters.copy(), getNPlayers());
        copy.playerHands = new ArrayList<>();

        for (Deck<SGCard> d : playerHands){
            copy.playerHands.add(d.copy());
        }
        return copy;
    }

    public int[] getPlayerScore() {return playerScore;}

    public List<Deck<SGCard>> getPlayerDecks() {return playerHands;}

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
                }
            }
        }};
    }

    @Override
    protected void _reset() {
        playerHands = new ArrayList<>();

    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }
}
