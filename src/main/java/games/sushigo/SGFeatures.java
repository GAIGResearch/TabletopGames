package games.sushigo;

import core.AbstractGameState;
import core.interfaces.IStateFeatureJSON;
import core.interfaces.IStateFeatureVector;
import games.sushigo.cards.SGCard;
import org.json.simple.JSONObject;
import utilities.Pair;

import java.util.Arrays;
import java.util.List;

public class SGFeatures implements IStateFeatureVector, IStateFeatureJSON {

    @Override
    public String[] names() {
        return new String[0];
    }

    @Override
    public String getObservationJson(AbstractGameState gameState, int playerID) {
        SGGameState sggs = (SGGameState) gameState;
        JSONObject json = new JSONObject();
        json.put("PlayerID", playerID);
        json.put("nPlayers", sggs.getNPlayers());
        json.put("rounds", sggs.getRoundCounter());
        json.put("cardsInHand", sggs.getPlayerHands().get(playerID).toString());
        json.put("playedCards", sggs.getPlayedCards().get(playerID).toString());
        json.put("playerScore", sggs.playerScore[playerID]);
        for (int i = 0; i < sggs.getNPlayers(); i++){
            if (i != playerID){
                json.put("opp" + i + "playedCards", sggs.getPlayedCards().get(i).toString());
                json.put("opp" + i + "score", sggs.playerScore[i]);

            }
        }
        return json.toJSONString();
    }

    @Override
    public double[] doubleVector(AbstractGameState state, int playerID) {
        /* Normalised by default */
        // todo would be better in SGParameters -> at least generating a list of strings
        SGGameState sggs = (SGGameState) state;
        int maxCardsInHand = ((SGParameters)sggs.getGameParameters()).nCards;
        int nUnique = Arrays.stream(SGCard.SGCardType.values()).map(e -> e.getIconCountVariation().length).mapToInt(i -> i).sum();
        String uniqueCards[] = new String[nUnique];
        int counter = 0;
        for (SGCard.SGCardType cardType: SGCard.SGCardType.values()){
            if (cardType.getIconCountVariation().length == 1){
                uniqueCards[counter] = cardType.name();
                counter ++;
            } else {
                for (int i = 0; i < cardType.getIconCountVariation().length; i++) {
                    uniqueCards[counter] = cardType.name() + "-" + cardType.getIconCountVariation()[i];
                    counter++;
                }
            }
        }
        /* state representation */
        // rounds
        // cards in hand
        // player score

        // encode player hand - note that this could be one hot encoded
        int playerHand[] = new int[maxCardsInHand];
        List<SGCard> cardsInHand = sggs.playerHands.get(playerID).getComponents();
        for (int i = 0; i < cardsInHand.size(); i++){
            playerHand[i] = Arrays.asList(uniqueCards).indexOf(cardsInHand.get(i).toString());
        }

        // played cards
        for (int i = 0; i < sggs.getNPlayers(); i++){
            List<SGCard> playedCards = sggs.getPlayedCards().get(i).getComponents();
//            Arrays.asList(uniqueCards).indexOf(cardsInHand.get(i).toString());
        }

        // todo this is not finished
        return new double[0];
    }

//    public int[] encodeCardType(List<SGCard> deck){
//        int nUnique = (int) Arrays.stream(SGCard.SGCardType.values()).map(e -> e.getIconCountVariation().length).count();
//        String uniqueCards[] = new String[nUnique];
//        int counter = 0;
//        for (SGCard.SGCardType cardType: SGCard.SGCardType.values()){
//            for (int i = 0; i < cardType.getIconCountVariation().length; i++){
//                uniqueCards[counter] = cardType.name() + cardType.getIconCountVariation()[i];
//                counter ++;
//            }
//        }
//        // todo need to encode different variants correctly
//        for (SGCard cards: deck){
//            cards.getType();
//        }
//        for (Pair<SGCard.SGCardType, Integer> types : ((SGParameters) getGameParameters()).nCardsPerType.keySet()){
//
//        }
//        return new int[0];
//    }

}
