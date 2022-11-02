package games.terraformingmars.actions;

import core.AbstractGameState;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.rules.requirements.PlayableActionRequirement;


public class PlayCard extends TMAction {

    public PlayCard() { super(); } // This is needed for JSON Deserializer

    public PlayCard(int player, TMCard card, boolean free) {
        super(TMTypes.ActionType.PlayCard, player, free);
        this.setActionCost(TMTypes.Resource.MegaCredit, card.cost, card.getComponentID());
        this.setCardID(card.getComponentID());

        this.requirements.addAll(card.requirements);
        for (TMAction aa : card.immediateEffects) {
            // All immediate effects must also be playable in order for this card to be playable
            this.requirements.add(new PlayableActionRequirement(aa));
        }
    }

    private PlayCard(int player, boolean free) {
        super(player, free);
    }

    @Override
    public boolean _execute(TMGameState gs) {
        TMGameParameters gp = (TMGameParameters) gs.getGameParameters();
        TMCard card = (TMCard) gs.getComponentById(getPlayCardID());
        playCard(gs, card);
        return true;
    }

    private void playCard(TMGameState gs, TMCard card) {
        // Remove from hand, resolve on-play effects and add tags etc. to cards played lists
        gs.getPlayerHands()[player].remove(card);

        // Add info to played cards stats
        if (card.cardType != TMTypes.CardType.Event) {  // Event tags don't count for regular tag counts
            for (TMTypes.Tag t : card.tags) {
                gs.getPlayerCardsPlayedTags()[player].get(t).increment(1);
            }
        } else {
            gs.getPlayerCardsPlayedTags()[player].get(TMTypes.Tag.Event).increment(1);
        }

        gs.getPlayerCardsPlayedTypes()[player].get(card.cardType).increment(1);
        if (card.shouldSaveCard()) {
            gs.getPlayerComplicatedPointCards()[player].add(card);
            gs.getPlayedCards()[player].add(card);
        } else {
            gs.getPlayedCards()[player].add(card);
            if (card.nPoints != 0) {
                gs.getPlayerCardPoints()[player].increment((int) card.nPoints);
            }
        }

        // Add actions
        for (TMAction a: card.actions) {
            a.player = player;
            a.setCardID(card.getComponentID());
            gs.getPlayerExtraActions()[player].add(a);
        }

        // Add discountEffects to player's discounts
        gs.addDiscountEffects(card.discountEffects);
        gs.addResourceMappings(card.resourceMappings, false);
        // Add persisting effects
        gs.addPersistingEffects(card.persistingEffects);

        // Execute on-play effects
        for (TMAction aa: card.immediateEffects) {
            card.actionPlayed = false;  // This is set by each action, preventing the next ones, but we want all to be executed
            aa.player = player;
            aa.execute(gs);
        }
        card.actionPlayed = false;  // We've not executed the active card action
    }

    @Override
    public PlayCard _copy() {
        return new PlayCard(player, freeActionPoint);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        TMCard card = (TMCard) gameState.getComponentById(getPlayCardID());
        return "Play card " + card.getComponentName();
    }

    @Override
    public String toString() {
        return "Play card id " + getPlayCardID();
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PlayCard)) return false;
        return super.equals(o);
    }
}
