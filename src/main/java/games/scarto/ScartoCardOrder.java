package games.scarto;

import core.components.TarotCard;
import games.tricktaking.CardOrder;

public enum ScartoCardOrder implements CardOrder<TarotCard, TarotCard.Suit> {
    INSTANCE;

    @Override
    public TarotCard.Suit suitOf(TarotCard card) {
        // the Fool belongs to no suit, so it never wins a trick
        return card.isFool() ? null : card.suit;
    }

    @Override
    public int rank(TarotCard card) {
        return switch (card.suit) {
            // the Angel (20) is highest, then the World (21), then 19 down to the Pagat
            case Trumps -> switch (card.number) {
                case TarotCard.ANGEL -> TarotCard.N_TRUMPS;
                case TarotCard.WORLD -> TarotCard.ANGEL;
                default -> card.number;
            };
            // round suits: King, Queen, Cavalier, Knave, then the pips from Ace (ranked as 10) up to 10 (ranked as 1)
            case Cups, Coins -> card.number < TarotCard.KNAVE ? 11 - card.number : card.number;
            // long suits: King, Queen, Cavalier, Knave, 10 down to Ace
            default -> card.number;
        };
    }
}
