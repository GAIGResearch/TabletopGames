package games.wonders7.actions;

import core.AbstractGameState;
import core.actions.DrawCard;
import core.components.Deck;
import games.wonders7.Wonders7Constants.Resource;
import games.wonders7.Wonders7Constants.TradeSource;
import games.wonders7.Wonders7GameState;
import games.wonders7.cards.Wonder7Card;
import utilities.Pair;

import java.util.*;

import static games.wonders7.Wonders7Constants.Resource.Coin;

public class PlayCard extends DrawCard {

    public final Wonder7Card.CardType cardType;
    public final int player;
    public final boolean free;
    public final boolean fromDiscard;

    // Player chooses card to play
    public PlayCard(int player, Wonder7Card.CardType card, boolean free, boolean fromDiscard) {
        super();
        this.cardType = card;
        this.player = player;
        this.free = free;
        this.fromDiscard = fromDiscard;
    }

    public PlayCard(int player, Wonder7Card.CardType card, boolean free) {
        this(player, card, free, false);
    }


    @Override
    public boolean execute(AbstractGameState gameState) {
        Wonders7GameState wgs = (Wonders7GameState) gameState;

        Deck<Wonder7Card> playerHand = wgs.getPlayerHand(player);
        Map<Resource, Integer> playerResources = wgs.getPlayerResources(player);

        Wonder7Card card = fromDiscard
                ? wgs.findCardInDiscard(cardType)
                : wgs.findCardInHand(player, cardType);

        cardId = card.getComponentID();

        // Removes coins paid for card
        if (!free) {
            Pair<Boolean, List<TradeSource>> buildDetails = card.isPlayable(player, wgs);
            if (!buildDetails.a) {
                throw new AssertionError("Card not playable");
            }
            // first pay direct coin cost
            playerResources.put(Coin, playerResources.get(Coin) - card.constructionCost.getOrDefault(Coin, 0));
            // then pay trade costs
            List<TradeSource> tradeSources = buildDetails.b;
            for (TradeSource tradeSource : tradeSources) {
                if (tradeSource.fromPlayer() == -1) {
                    throw new AssertionError("Trade source from player not set");
                } else {
                    // give the supplier the coins
                    int cost = tradeSource.cost();
                    int fromPlayer = tradeSource.fromPlayer();
                    playerResources.put(Coin, playerResources.get(Coin) - cost);
                    wgs.getPlayerResources(fromPlayer).put(Coin, wgs.getPlayerResources(fromPlayer).get(Coin) + cost);
                }
            }
        }
        wgs.getPlayedCards(player).add(card);

        // Gives player resources produced from card
        Set<Resource> keys = card.resourcesProduced.keySet(); // Gets all the resources the card provides
        for (Resource resource : keys) {  // Goes through all keys for each resource
            int cardValue = card.getNProduced(resource); // Number of resource the card provides
            int playerValue = playerResources.get(resource); // Number of resource the player owns
            playerResources.put(resource, playerValue + cardValue); // Adds the resources provided by the card to the players resource count
        }

        // trigger any instant effects
        card.applyInstantCardEffects(wgs, player);

        // remove the card from the players hand to the playedDeck
        if (fromDiscard) {
            wgs.getDiscardPile().remove(card);
        } else {
            playerHand.remove(card);
        }

        return true;
    }

    @Override
    public String toString() {
        return "Player " + player + " played card " + cardType + (free ? " (free)" : "");
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayCard)) return false;
        if (!super.equals(o)) return false;
        PlayCard playCard = (PlayCard) o;
        return player == playCard.player && free == playCard.free && cardType == playCard.cardType && fromDiscard == playCard.fromDiscard;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardType.ordinal(), player, free, fromDiscard) + 31 * super.hashCode();
    }

    @Override
    public PlayCard copy() {
        return this;
    }
}
