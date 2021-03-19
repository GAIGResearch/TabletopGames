package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;

import java.util.*;


public class PlayCard extends TMAction {
    final int cardIdx;
    int cardID;

    public PlayCard(int player, int cardIdx, boolean free) {
        super(TMTypes.ActionType.PlayCard, player, free);
        this.cardIdx = cardIdx;
        this.cardID = -1;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        TMGameState gs = (TMGameState) gameState;
        TMGameParameters gp = (TMGameParameters) gameState.getGameParameters();
        int player = this.player;
        if (player == -1) player = gs.getCurrentPlayer();
        TMCard card = gs.getPlayerHands()[player].get(cardIdx);
        cardID = card.getComponentID();
        playCard(gs, player);
        return true;
    }

    private void playCard(TMGameState gs, int player) {
        // Second: remove from hand, resolve on-play effects and add tags etc. to cards played lists
        TMCard card = gs.getPlayerHands()[player].pick(cardIdx);

        // Add info to played cards stats
        for (TMTypes.Tag t: card.tags) {
            gs.getPlayerCardsPlayedTags()[player].get(t).increment(1);
        }
        gs.getPlayerCardsPlayedTypes()[player].get(card.cardType).increment(1);
        gs.getPlayerCardsPlayedActions()[player].addAll(Arrays.asList(card.actions));

        // Add discountEffects to player's discounts
        gs.addDiscountEffects(card.discountEffects);
        gs.addResourceMappings(card.resourceMappings, false);
        // Add persisting effects
        gs.addPersistingEffects(card.persistingEffects);

        // Execute on-play effects
        for (AbstractAction aa: card.immediateEffects) {
            aa.execute(gs);
        }

        super.execute(gs);
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
        return cardIdx == playCard.cardIdx;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardIdx);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Play card idx " + cardIdx;
    }

    @Override
    public String toString() {
        return "Play card idx " + cardIdx;
    }

    public int getCardID() {
        return cardID;
    }
}
