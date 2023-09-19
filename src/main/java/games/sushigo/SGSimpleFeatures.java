package games.sushigo;

import core.AbstractGameState;
import core.AbstractParameters;
import core.interfaces.IStateFeatureJSON;
import core.interfaces.IStateFeatureVector;
import evaluation.features.TunableStateFeatures;
import games.sushigo.cards.SGCard;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.List;

public class SGSimpleFeatures extends TunableStateFeatures {

    static String[] allNames = new String[]{"makiCount", "tempuraCount", "dumplingCount", "nigiriCount", "puddingCount",
                "wasabiActive", "chopstickActive"};

    public SGSimpleFeatures() {
        super(allNames);
    }

    @Override
    public double[] fullFeatureVector(AbstractGameState state, int playerID) {
        /* Normalised by default */
        // completed sets
        // - depending on type - some ordinal pos
        // chopsticks / wasabi active
        // N cards in hand
        // player scores - difference between leader
        double[] features = new double[allNames.length];

        // just get player score + ordinal positions + wasabi/chopstick active
        SGGameState sggs = (SGGameState) state;

        // Player features
        int wasabiActive = 0;
        int chopstickActive = 0;
        List<SGCard> playedCardTypes = sggs.getPlayedCards().get(playerID).getComponents();
        for (SGCard cardTypes: playedCardTypes){
            if (cardTypes.type.equals(SGCard.SGCardType.Wasabi)){
                wasabiActive = 1;
            } else if (cardTypes.type.equals(SGCard.SGCardType.Chopsticks)) {
                chopstickActive = 1;
            }
        }
        int makiCount = 0;
        int tempuraCount = 0;
        int dumplingCount = 0;
        int nigiriCount = 0;
        int puddingCount = 0;

        int wasabiPlayed = 0;
        List<SGCard> playedCards = sggs.getPlayedCards().get(playerID).getComponents();
        for (SGCard cardTypes: playedCards){
            if (cardTypes.type.equals(SGCard.SGCardType.Wasabi)){
                wasabiPlayed += 1;
            }
            else if (cardTypes.type.equals(SGCard.SGCardType.Dumpling)){
                dumplingCount += 1;
            } else if (cardTypes.type.equals(SGCard.SGCardType.Tempura)) {
                tempuraCount += 1;
            } else if (cardTypes.type.equals(SGCard.SGCardType.Maki)){
                makiCount += cardTypes.count;
            } else if (cardTypes.type.equals(SGCard.SGCardType.Pudding)){
                puddingCount += 1;
            } else if (cardTypes.type.equals(SGCard.SGCardType.SquidNigiri)){
                int score = ((SGParameters)sggs.getGameParameters()).valueSquidNigiri;
                if (wasabiPlayed > 0){
                    score *= ((SGParameters)sggs.getGameParameters()).multiplierWasabi;
                    wasabiPlayed -= 1;
                }
                nigiriCount += score;
            } else if (cardTypes.type.equals(SGCard.SGCardType.SalmonNigiri)){
                int score = ((SGParameters)sggs.getGameParameters()).valueSalmonNigiri;
                if (wasabiPlayed > 0){
                    score *= ((SGParameters)sggs.getGameParameters()).multiplierWasabi;
                    wasabiPlayed -= 1;
                }
                nigiriCount += score;
            }else if (cardTypes.type.equals(SGCard.SGCardType.EggNigiri)){
                int score = ((SGParameters)sggs.getGameParameters()).valueEggNigiri;
                if (wasabiPlayed > 0){
                    score *= ((SGParameters)sggs.getGameParameters()).multiplierWasabi;
                    wasabiPlayed -= 1;
                }
                nigiriCount += score;
            }
        }

        features[0] = makiCount;
        features[1] = tempuraCount;
        features[2] = dumplingCount;
        features[3] = nigiriCount;
        features[4] = puddingCount;
        features[5] = wasabiActive;
        features[6] = chopstickActive;

        return features;
    }


    @Override
    protected SGSimpleFeatures _copy() {
        return new SGSimpleFeatures();
    }
}
