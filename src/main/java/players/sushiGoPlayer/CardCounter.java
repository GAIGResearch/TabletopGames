package players.sushiGoPlayer;

import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard;
import utilities.Pair;

import java.util.HashMap;
import java.util.Map;

public class CardCounter {

    // Card types, with each maki kept separate
    public enum CardKey {
        Tempura, Sashimi, Dumpling,
        SquidNigiri, SalmonNigiri, EggNigiri,
        Wasabi, Chopsticks, Pudding,
        Maki1, Maki2, Maki3
    }

    public HashMap<CardKey, Integer> unseenCards = new HashMap<>();
    public HashMap<CardKey, Integer> seenCards = new HashMap<>();

    public CardCounter(SGGameState gameState) {
        for (CardKey key : CardKey.values()) {
            unseenCards.put(key, 0);
            seenCards.put(key, 0);
        }

        SGParameters parameters = (SGParameters) gameState.getGameParameters();
        for (Pair<SGCard.SGCardType, Integer> p : parameters.nCardsPerType.keySet()) {
            int count = parameters.nCardsPerType.get(p);
            CardKey key = keyOf(p.a, p.b);
            unseenCards.put(key, unseenCards.get(key) + count);
        }
    }

    public static CardKey keyOf(SGCard.SGCardType type, int count) {
        if (type == SGCard.SGCardType.Maki) {
            return switch (count) {
                case 1 -> CardKey.Maki1;
                case 2 -> CardKey.Maki2;
                case 3 -> CardKey.Maki3;
                default -> throw new IllegalArgumentException("Unexpected Maki count: " + count);
            };
        }
        return CardKey.valueOf(type.name());
    }

    public void markAsSeen(SGCard card) {
        CardKey cardKey = keyOf(card.type, card.count);
        unseenCards.put(cardKey, unseenCards.get(cardKey) - 1);
        seenCards.put(cardKey, seenCards.get(cardKey) + 1);
    }

    public int countAllUnseenCards() {
        return unseenCards.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int countAllSeenCards() {
        return seenCards.values().stream().mapToInt(Integer::intValue).sum();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CardCounter{");
        for (int i = 0; i < CardKey.values().length; i++) {
            CardKey key = CardKey.values()[i];
            if (i > 0) sb.append(", ");
            sb.append(key)
                    .append("=[seen:")
                    .append(seenCards.getOrDefault(key, 0))
                    .append(", unseen:")
                    .append(unseenCards.getOrDefault(key, 0))
                    .append("]");
        }
        sb.append('}');
        return sb.toString();
    }
}