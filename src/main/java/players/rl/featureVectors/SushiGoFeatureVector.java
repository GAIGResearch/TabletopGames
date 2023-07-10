package players.rl.featureVectors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import core.AbstractGameState;
import core.components.Deck;
import core.interfaces.IStateFeatureVector;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;
import games.sushigo.cards.SGCard.SGCardType;

public class SushiGoFeatureVector implements IStateFeatureVector {

    List<SGCardType> cardTypes = Arrays.asList(SGCardType.values());

    String[] names = new LinkedList<String>() {
        {
            add("nPlayers");
            add("nTurnsLeft");
            cardTypes.forEach(ct -> addToList(this, "nPlayed", ct));
            cardTypes.forEach(ct -> addToList(this, "nNextPlayer", ct));
            cardTypes.forEach(ct -> addToList(this, "nTotalPlayed", ct));
        }
    }.toArray(String[]::new);

    void addToList(List<String> list, String prefix, SGCardType cardType) {
        switch (cardType) {
            case Maki:
                list.add(prefix + cardType.name() + "1");
                list.add(prefix + cardType.name() + "2");
                list.add(prefix + cardType.name() + "3");
                break;
            default:
                list.add(prefix + cardType.name());
                break;
        }
    }

    public SushiGoFeatureVector() {
        List<SGCardType> allCardTypes = Arrays.asList(SGCardType.values());
        List<SGCardType> makiVariations = new ArrayList<>();

        for (SGCardType cardType : allCardTypes) {
            if (cardType.name().startsWith("Maki")) {
                makiVariations.add(cardType);
            }
        }

        // Print all card types
        for (SGCardType cardType : allCardTypes) {
            System.out.println(cardType);
        }
        System.out.println("--- Maki Variations ---");
        // Print Maki variations separately
        for (SGCardType makiVariation : makiVariations) {
            System.out.println(makiVariation);
        }

        int x = 0;
    }

    @Override
    public double[] featureVector(AbstractGameState state, int playerID) {
        double[] features = new double[names.length];
        SGGameState sggs = (SGGameState) state;
        Deck<SGCard> playerHand = sggs.getPlayerHands().get(playerID);
        int f = 0;
        features[f++] = state.getNPlayers();
        features[f++] = playerHand.getSize();
        for (int i = 0; i < cardTypes.size(); i++) {
            features[f] = playerHand.stream().filter(c -> c.type == SGCardType.Wasabi).count();
        }
        return null;

    }

    @Override
    public String[] names() {
        return names;
    }

}
