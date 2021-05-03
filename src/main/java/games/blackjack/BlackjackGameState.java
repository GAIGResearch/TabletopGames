package games.blackjack;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.Deck;
import core.components.FrenchCard;
import core.interfaces.IPrintable;
import games.GameType;


import java.util.ArrayList;
import java.util.*;

import static core.CoreConstants.PARTIAL_OBSERVABLE;

public class BlackjackGameState extends AbstractGameState implements IPrintable {
    List<Deck<FrenchCard>>  playerDecks;
    Deck<FrenchCard>        drawDeck;
    Deck<FrenchCard>        tableDeck;
    String                  currentSuite;
    int []                  Score;
    int                     point;


    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers      - number of players for this game.
     */
    public BlackjackGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new BlackjackTurnOrder(nPlayers), GameType.Blackjack);
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>(){{
            addAll(playerDecks);
            add(drawDeck);
            add(tableDeck);
        }};
    }


    public Deck<FrenchCard> DrawDeck() {  return drawDeck;}

    public List<Deck<FrenchCard>> PlayerDecks() { return playerDecks; }

    public int point() {  return Score[0]; }

    public Deck<FrenchCard> getTableDeck() { return tableDeck;}

    public int calcPoint(int PlayerID){
        int points = 0;
        int ace = 0;
        for (int other = 0; other < getNPlayers(); other++){
            if (other != PlayerID){
                for (FrenchCard card : playerDecks.get(PlayerID).getComponents()){
                    switch (card.type){
                        case Number:
                            points += card.drawN;
                            break;
                        case Jack:
                            points += 10;
                            break;
                        case Queen:
                            points += 10;
                            break;
                        case King:
                            points += 10;
                            break;
                        case Ace:
                            ace += 1;
                            break;
                    }
                }
                for (int i = 0; i < ace; i++){
                    if(points > 10){
                        points +=1;
                    }
                    else{
                        points += 11;
                    }
                }
            }
        }
        point = points;
        return point;
    }


    @Override
    protected AbstractGameState _copy(int playerId) {
        BlackjackGameState copy = new BlackjackGameState(gameParameters.copy(), getNPlayers());
        copy.playerDecks = new ArrayList<>();

        for (Deck<FrenchCard> d : playerDecks){
            copy.playerDecks.add(d.copy());
        }
        copy.drawDeck = drawDeck.copy();

//        if (PARTIAL_OBSERVABLE && playerId != -1){
//            Random r = new Random(copy.gameParameters.getRandomSeed());
//            for (int i = 0; i< getNPlayers(); i++){
//                if (i != playerId){
//                    copy.drawDeck.add(copy.playerDecks.get(i));
//                }
//            }
//            copy.drawDeck.shuffle(r);
//            for(int i = 0; i < getNPlayers(); i++){
//                if (i != playerId){
//                    Deck<FrenchCard> d = copy.playerDecks.get(i);
//                    int Cards = d.getSize();
//                    d.clear();
//                    for (int j = 0; j < Cards; j++){
//                        d.add(copy.drawDeck().draw());
//                    }
//                }
//            }
//        }
        copy.tableDeck = tableDeck.copy();
        copy.currentSuite = currentSuite;
        copy.Score = Score.clone();
        copy.point = point;
        return copy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return 0;
    }

    @Override
    public double getGameScore(int playerId) {
        return calcPoint(playerId);
    }

    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        return new ArrayList<Integer>(){{
            add(drawDeck.getComponentID());
            for (Component c: drawDeck.getComponents()){
                add(c.getComponentID());
            }
            for (int i = 0; i < getNPlayers(); i++){
                if (i != playerId){
                    add(playerDecks.get(i).getComponentID());
                    for (Component c: playerDecks.get(i).getComponents()){
                        add(c.getComponentID());
                    }
                }
            }
        }};
    }

    @Override
    protected void _reset() {
        playerDecks = new ArrayList<>();
        drawDeck = null;
        playerDecks = null;
        tableDeck = null;

    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlackjackGameState)) return false;
        if (!super.equals(o)) return false;
        BlackjackGameState that = (BlackjackGameState) o;
        return Objects.equals(playerDecks, that.playerDecks) &&
                Objects.equals(drawDeck, that.drawDeck) &&
                Objects.equals(tableDeck, that.drawDeck) &&
                Objects.equals(point, that.point) &&
                Arrays.equals(Score, that.Score);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), playerDecks, drawDeck, tableDeck);
        result = 31 * result + Arrays.hashCode(Score);
        return result;
    }

    @Override
    public void printToConsole(){
        String[] strings = new String[4];

        strings[0] = "Player      : " + getCurrentPlayer();
        strings[1] = "Points      : " +  calcPoint(getCurrentPlayer());
        StringBuilder sb = new StringBuilder();
        sb.append("Player Hand : ");


        for (FrenchCard card : playerDecks.get(getCurrentPlayer()).getComponents()) {
            sb.append(card.toString());
            sb.append(" ");
        }
        strings[2] = sb.toString();
        strings[3] = "----------------------------------------------------";

        for (String s : strings){
            System.out.println(s);
        }
    }

}
