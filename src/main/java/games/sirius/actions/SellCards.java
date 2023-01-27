package games.sirius.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.sirius.*;

import java.util.*;
import java.util.stream.IntStream;

import static games.sirius.SiriusConstants.SiriusCardType.*;
import static java.util.stream.Collectors.toList;

public class SellCards extends AbstractAction implements IExtendedSequence {

    public final SiriusConstants.SiriusCardType salesType;
    int[] saleValues;
    boolean decreaseTrack;
    int decidingPlayer = -1;
    int policeTriggers = 0;

    private SellCards(SellCards toCopy) {
        this.salesType = toCopy.salesType;
        this.decreaseTrack = toCopy.decreaseTrack;
        this.saleValues = toCopy.saleValues.clone();
        this.policeTriggers = toCopy.policeTriggers;
        this.decidingPlayer = toCopy.decidingPlayer;
    }

    public SellCards(List<SiriusCard> cardsToSell) {
        this(cardsToSell, false);
    }

    public SellCards(List<SiriusCard> cardsToSell, boolean negative) {
        // we check that all cards are of the same type
        if (cardsToSell.isEmpty())
            throw new IllegalArgumentException("Must specify at least one card to sell");
        decreaseTrack = negative;
        salesType = cardsToSell.get(0).cardType;
        if (decreaseTrack && salesType != SMUGGLER)
            throw new IllegalArgumentException("Only Smugglers can be used to decrease a track");

        if (cardsToSell.stream().anyMatch(c -> c.cardType != salesType))
            throw new IllegalArgumentException("All cards must have the same type to sell");
        int glowingContrabandCards = (int) cardsToSell.stream().filter(c -> c.cardType == CONTRABAND && c.value == 0).count();
        int glowingContrabandSets = glowingContrabandCards / 3;
        int glowingContrabandSurplus = glowingContrabandCards % 3;
        // We now remove the surplus from cards to Sell
        if (glowingContrabandSurplus > 0) {
            List<SiriusCard> cardsToRemove = cardsToSell.stream()
                    .filter(c -> c.cardType == CONTRABAND && c.value == 0)
                    .limit(glowingContrabandSurplus)
                    .collect(toList());
            cardsToSell.removeAll(cardsToRemove);
        }
        saleValues = cardsToSell.stream().mapToInt(c -> c.value).sorted().toArray();
        // then check for glowing contraband - for each set of 3 we have a value of 10
        for (int gc = 0; gc < glowingContrabandSets; gc++) {
            for (int i = 0; i < saleValues.length; i++)
                if (saleValues[i] == 0) {
                    saleValues[i] = 10;
                    break;
                }
        }
    }

    public static SellCards reverseDirection(SellCards base) {
        SellCards retValue = new SellCards(base);
        retValue.decreaseTrack = !base.decreaseTrack;
        return retValue;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        // We sell each card in turn
        Deck<SiriusCard> hand = state.getPlayerHand(gs.getCurrentPlayer());
        for (int saleValue : saleValues) {
            Optional<SiriusCard> cardToSell = getMatchingCard(hand, saleValue);
            if (cardToSell.isPresent()) {
                SiriusCard card = cardToSell.get();
                if (state.sellCard(card, saleValue * (decreaseTrack ? -1 : 1)))
                    policeTriggers = 1;  // cannot collect more than one Trigger
            } else
                throw new AssertionError("Card not found : " + salesType + " " + saleValue);
        }
        if (salesType == SMUGGLER)
            state.setActionTaken("Betrayed", gs.getCurrentPlayer());
        else
            state.setActionTaken("Sold", gs.getCurrentPlayer());

        if (policeTriggers > 0) {
            // we only branch actions from SellCards if we have to undertake a police action
            decidingPlayer = state.getCurrentPlayer();  // and store this
            state.setActionInProgress(this);
            // this will trigger the next action to pick a moon, and then a card from each player
        }

        return true;
    }

    public int getTotalValue() {
        return Arrays.stream(saleValues).sum() * (decreaseTrack ? -1 : +1);
    }

    public int getTotalCards() {
        return saleValues.length;
    }

    private Optional<SiriusCard> getMatchingCard(Deck<SiriusCard> hand, int v) {
        int val = v == 10 ? 0 : v; // to cater for Glowing Contraband
        return hand.stream().filter(c -> c.cardType == salesType && c.value == val).findFirst();
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        // we pick a moon to move to
        return IntStream.range(1, state.getAllMoons().size())
                .filter(i -> !state.getMoon(i).getPolicePresence())
                .mapToObj(MovePolice::new).collect(toList());
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return decidingPlayer;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        if (action instanceof MovePolice) {
            policeTriggers--;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return policeTriggers == 0;  // none remaining
    }

    @Override
    public SellCards copy() {
        return new SellCards(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SellCards) {
            SellCards other = (SellCards) obj;
            return other.salesType == salesType && other.decreaseTrack == decreaseTrack && other.policeTriggers == policeTriggers &&
                    other.decidingPlayer == decidingPlayer && Arrays.equals(other.saleValues, saleValues);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(salesType.ordinal(), decreaseTrack, decidingPlayer, policeTriggers) + 31 * Arrays.hashCode(saleValues) + 82;
    }

    @Override
    public String toString() {
        return String.format("Sell %s : %s", salesType, Arrays.toString(saleValues));
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
