package games.terraformingmars.actions;

import core.AbstractGameState;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;

import java.util.*;


public class PlayCard extends TMAction {

    public PlayCard(int player, int cardId, boolean free) {
        super(TMTypes.ActionType.PlayCard, player, free);
        this.cardID = cardId;
        costResource = TMTypes.Resource.MegaCredit;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        TMGameState gs = (TMGameState) gameState;
        TMGameParameters gp = (TMGameParameters) gameState.getGameParameters();
        int player = this.player;
        if (player == -1) player = gs.getCurrentPlayer();
        TMCard card = (TMCard) gs.getComponentById(cardID);
        playCard(gs, player, card);
        return super.execute(gs);
    }

    private void playCard(TMGameState gs, int player, TMCard card) {
        // Second: remove from hand, resolve on-play effects and add tags etc. to cards played lists
        gs.getPlayerHands()[player].remove(card);

        // Add info to played cards stats
        for (TMTypes.Tag t: card.tags) {
            gs.getPlayerCardsPlayedTags()[player].get(t).increment(1);
        }
        gs.getPlayerCardsPlayedTypes()[player].get(card.cardType).increment(1);
        if (card.shouldSaveCard()) {
            gs.getPlayerComplicatedPointCards()[player].add(card);
        } else {
            gs.getPlayedCards()[player].add(card);
            if (card.nPoints != 0) {
                gs.getPlayerCardPoints()[player].increment((int) card.nPoints);
            }
        }

        // Add actions
        for (TMAction a: card.actions) {
            a.player = player;
            gs.getPlayerExtraActions()[player].add(a);
        }

        // Add discountEffects to player's discounts
        gs.addDiscountEffects(card.discountEffects);
        gs.addResourceMappings(card.resourceMappings, false);
        // Add persisting effects
        gs.addPersistingEffects(card.persistingEffects);

        // Force an update of components before executing the effects, they might need something just added
        gs.getAllComponents();

        // Execute on-play effects
        for (TMAction aa: card.immediateEffects) {
            aa.player = player;
            aa.execute(gs);
        }
    }

    @Override
    public PlayCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayCard)) return false;
        if (!super.equals(o)) return false;
        PlayCard playCard = (PlayCard) o;
        return cardID == playCard.cardID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        TMCard card = (TMCard) gameState.getComponentById(cardID);
        return "Play card " + card.getComponentName();
    }

    @Override
    public String toString() {
        return "Play card id " + cardID;
    }

    @Override
    public int getCost(TMGameState gs) {
        TMCard card = (TMCard) gs.getComponentById(cardID);
        return card.cost;
    }

    @Override
    public boolean canBePlayed(TMGameState gs) {
        if (!super.canBePlayed(gs)) return false;
        // Immediate effects must also be playable for the card to be playable
        TMCard card = (TMCard) gs.getComponentById(cardID);
        for (TMAction aa : card.immediateEffects) {
            if (!aa.canBePlayed(gs)) return false;
        }
        return true;
    }
}
