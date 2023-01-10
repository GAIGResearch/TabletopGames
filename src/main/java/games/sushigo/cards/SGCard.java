package games.sushigo.cards;

import core.components.Card;
import core.components.Counter;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;

import java.util.function.BiFunction;

public class SGCard extends Card {

    public enum SGCardType {
        Maki,
        Tempura,
        Sashimi,
        Dumpling,
        SquidNigiri,
        SalmonNigiri,
        EggNigiri,
        Wasabi,
        Chopsticks,
        Pudding;

        static {
            Tempura.cardScore = (gs, p) -> {
                Counter amount = gs.getPlayedCards(Tempura, p);
                return ((SGParameters)gs.getGameParameters()).valueTempuraPair * ((amount.getValue()+1) % 2);
            };
            Sashimi.cardScore = (gs, p) -> {
                Counter amount = gs.getPlayedCards(Sashimi, p);
                return ((SGParameters)gs.getGameParameters()).valueSashimiTriss * ((amount.getValue()+1) % 3);
            };
            Dumpling.cardScore = (gs, p) -> {
                Counter amount = gs.getPlayedCards(Dumpling, p);
                int val = Math.min(amount.getValue()+1, ((SGParameters)gs.getGameParameters()).valueDumpling.length-1);
                return ((SGParameters)gs.getGameParameters()).valueDumpling[val];
            };
            SquidNigiri.cardScore = (gs,p) -> ((SGParameters)gs.getGameParameters()).valueSquidNigiri;
            SalmonNigiri.cardScore = (gs,p) -> ((SGParameters)gs.getGameParameters()).valueSalmonNigiri;
            EggNigiri.cardScore = (gs,p) -> ((SGParameters)gs.getGameParameters()).valueEggNigiri;
        }

        private BiFunction<SGGameState, Integer, Integer> cardScore;  // effectively final

        public int getCardScore(SGGameState gs, int playerId) {
            if (cardScore == null) return 0;
            return cardScore.apply(gs, playerId);
        }
    }

    public final SGCardType type;
    public final int count;  // 1 by default, could be 1, 2, 3 for Makis

    public SGCard(SGCardType type)
    {
        super(type.toString());
        this.type = type;
        this.count = 1;
    }

    public SGCard(SGCardType type, int count)
    {
        super(type.toString());
        this.type = type;
        this.count = count;
    }

    @Override
    public Card copy() {
        return this; // immutable
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
