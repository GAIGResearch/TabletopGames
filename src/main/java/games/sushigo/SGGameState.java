package games.sushigo;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Card;
import core.components.Component;
import core.components.Deck;
import games.GameType;
import games.sushigo.cards.SGCard;
import games.uno.UnoGameState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SGGameState extends AbstractGameState {
    List<Deck<SGCard>> playerHands;
    List<Deck<SGCard>> playerFields;
    Deck<SGCard> drawPile;
    Deck<SGCard> discardPile;
    int cardAmount = 0;
    int[] playerScore;
    int[] playerCardPicks;
    int[] playerTempuraAmount;
    int[] playerSashimiAmount;
    int[] playerDumplingAmount;
    Integer winningPlayer;
    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers      - amount of players for this game.
     */
    public SGGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new SGTurnOrder(nPlayers), GameType.SushiGO);
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
        copy.playerCardPicks = playerCardPicks.clone();
        copy.playerTempuraAmount = playerTempuraAmount.clone();
        copy.playerSashimiAmount = playerSashimiAmount.clone();
        copy.playerDumplingAmount = playerDumplingAmount.clone();

        copy.cardAmount = cardAmount;

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
    public int[] getPlayerCardPicks() {return playerCardPicks;}

    public void setPlayerCardPick(int cardIndex, int playerId) {
        this.playerCardPicks[playerId] = cardIndex;
    }

    public List<Deck<SGCard>> getPlayerFields() {return playerFields;}

    public List<Deck<SGCard>> getPlayerDecks() {return playerHands;}

    @Override
    protected double _getHeuristicScore(int playerId) {
        return 0;
    }

    @Override
    public double getGameScore(int playerId) {
        return playerScore[playerId];
    }

    public void setGameScore(int playerId, int score)
    {
        playerScore[playerId] = score;
    }

    public int getPlayerTempuraAmount(int playerId)
    {
         return playerTempuraAmount[playerId];
    }

    public int getPlayerSashimiAmount(int playerId)
    {
        return playerSashimiAmount[playerId];
    }

    public int getPlayerDumplingAmount(int playerId)
    {
        return playerDumplingAmount[playerId];
    }

    public void setPlayerTempuraAmount(int playerId, int amount)
    {
        playerTempuraAmount[playerId] = amount;
    }

    public void setPlayerSashimiAmount(int playerId, int amount)
    {
        playerSashimiAmount[playerId] = amount;
    }

    public void setPlayerDumplingAmount(int playerId, int amount)
    {
        playerDumplingAmount[playerId] = amount;
    }

    public void setWinningPlayer(int playerId){
        winningPlayer = playerId;
    }
    
    public Integer getWinningPlayer(){ return winningPlayer; }

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
        cardAmount = 0;
        playerScore = null;
        playerCardPicks = null;
    }

    @Override
    protected boolean _equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof SGGameState)) return false;
        if (!super.equals(o)) return false;
        SGGameState that = (SGGameState) o;
        return Objects.equals(playerHands, that.playerHands) &&
                Objects.equals(playerFields, that.playerFields) &&
                Objects.equals(drawPile, that.drawPile) &&
                Objects.equals(discardPile, that.discardPile) &&
                Arrays.equals(playerScore, that.playerScore) &&
                Arrays.equals(playerCardPicks, that.playerCardPicks);
    }
}
