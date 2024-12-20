package games.sushigo;

import core.AbstractGameState;
import evaluation.features.TunableStateFeatures;
import games.sushigo.cards.SGCard;

import java.util.stream.IntStream;

public class SGSimpleFeatures extends TunableStateFeatures {

    static String[] allNames = new String[]{"makiCount", "tempuraCount", "dumplingCount", "nigiriCount", "puddingCount",
            "wasabiActive", "chopstickActive", "otherMaki", "otherPudding", "handSize", "sashimiCount", "tempuraCount", "round"};

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


        features[0] = sggs.getPlayedCardTypes(SGCard.SGCardType.Maki, playerID).getValue();
        features[1] = sggs.getPlayedCardTypes(SGCard.SGCardType.Tempura, playerID).getValue();
        features[2] = sggs.getPlayedCardTypes(SGCard.SGCardType.Dumpling, playerID).getValue();
        features[3] = sggs.getPlayedCardTypes(SGCard.SGCardType.EggNigiri, playerID).getValue() +
                sggs.getPlayedCardTypes(SGCard.SGCardType.SalmonNigiri, playerID).getValue() +
                sggs.getPlayedCardTypes(SGCard.SGCardType.SquidNigiri, playerID).getValue();
        features[4] = sggs.getPlayedCardTypes(SGCard.SGCardType.Pudding, playerID).getValue();
        features[5] = sggs.getPlayedCardTypes(SGCard.SGCardType.Wasabi, playerID).getValue();
        features[6] = sggs.getPlayedCardTypes(SGCard.SGCardType.Chopsticks, playerID).getValue();
        if (active[7]) {
            features[7] = IntStream.range(0, sggs.getNPlayers()).filter(i -> i != playerID)
                    .mapToDouble(i -> sggs.getPlayedCardTypes(SGCard.SGCardType.Maki, i).getValue()).sum();
        }
        if (active[8]) {
            features[8] = IntStream.range(0, sggs.getNPlayers()).filter(i -> i != playerID)
                    .mapToDouble(i -> sggs.getPlayedCardTypes(SGCard.SGCardType.Pudding, i).getValue()).sum();
        }
        features[9] = sggs.getPlayerHands().get(playerID).getSize();
        features[10] = sggs.getPlayedCardTypes(SGCard.SGCardType.Sashimi, playerID).getValue();
        features[11] = sggs.getPlayedCardTypes(SGCard.SGCardType.Tempura, playerID).getValue();
        features[12] = sggs.getRoundCounter();

        return features;
    }


    @Override
    protected SGSimpleFeatures _copy() {
        return new SGSimpleFeatures();
    }
}
