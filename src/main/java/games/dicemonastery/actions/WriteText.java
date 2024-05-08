package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.components.IlluminatedText;

import java.util.*;
import java.util.stream.Collectors;

import static games.dicemonastery.DiceMonasteryConstants.Resource;
import static games.dicemonastery.DiceMonasteryConstants.Resource.CANDLE;
import static games.dicemonastery.DiceMonasteryConstants.Resource.VELLUM;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class WriteText extends UseMonk implements IExtendedSequence {

    public final IlluminatedText textType;
    List<Resource> optionalInks = new ArrayList<>();
    int player = -1;

    public WriteText(IlluminatedText type, int piety) {
        super(piety);
        textType = type;
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        player = state.getCurrentPlayer();
        state.setActionInProgress(this);

        if (!meetsRequirements(textType, state.getStores(player, i -> true)))
            throw new IllegalArgumentException("Does not meet the requirements to write " + textType);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        // we can choose any ink we have not already chosen
        return inksAvailable(textType, state.getStores(player, i -> i.isInk), optionalInks)
                .stream().map(ChooseInk::new).collect(toList());
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    private void completeText(DiceMonasteryGameState state) {
        int player = state.getCurrentPlayer();

        if ((new HashSet<>(optionalInks)).size() < textType.inks)
            throw new AssertionError("Not enough different inks to illuminate " + textType);

        state.addResource(player, Resource.VELLUM, -textType.vellum);
        state.addResource(player, Resource.CANDLE, -textType.candles);

        int vpAward = textType.rewards[state.getNumberWritten(textType)];
        List<Resource> inks = new ArrayList<>(optionalInks);
        inks.addAll(Arrays.asList(textType.specialInks));
        for (Resource ink : inks) {
            state.addResource(player, ink, -1);
            vpAward += ink.vpBonus;
        }

        state.addVP(vpAward, player);
        state.writeText(textType);
    }


    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof ChooseInk) {
            optionalInks.add(((ChooseInk) action).ink);
        }
        if (executionComplete(state)) // we have all the inks, so write the text
            completeText((DiceMonasteryGameState) state);
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return optionalInks.size() >= textType.inks;
    }

    @Override
    public WriteText copy() {
        WriteText retValue = new WriteText(textType, actionPoints);
        retValue.optionalInks = new ArrayList<>(optionalInks);
        retValue.player = player;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WriteText) {
            WriteText other = (WriteText) obj;
            return other.textType.equals(textType) && other.actionPoints == actionPoints &&
                    other.player == player && other.optionalInks.equals(optionalInks);
        }
        return false;
    }

    @Override
    public int hashCode() {
        // we deliberately do not include player in the hashcode
        return textType.hashCode() + actionPoints * 47 + optionalInks.stream().mapToInt(i -> (i.ordinal() * i.ordinal() - 1) * (71 + i.ordinal())).sum();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("Write %s (piety %d) %s", textType.getComponentName(), actionPoints, optionalInks.isEmpty() ? "" : "using " + optionalInks.stream().map(Objects::toString).collect(joining()));
    }


    private static Set<Resource> inksAvailable(IlluminatedText text, Map<Resource, Integer> resources, List<Resource> reservedList) {
        Map<Resource, Integer> afterSupplyingSpecials = new EnumMap<>(resources);
        for (Resource specialInk : text.specialInks) {
            if (!(resources.getOrDefault(specialInk, 0) > 0))
                return new HashSet<>();
            afterSupplyingSpecials.merge(specialInk, -1, Integer::sum);
        }

        Set<Resource> retValue = afterSupplyingSpecials.keySet().stream()
                .filter(i -> i.isInk && afterSupplyingSpecials.get(i) > 0)
                .collect(Collectors.toSet());
        retValue.removeAll(reservedList);
        return retValue;
    }

    public static boolean meetsRequirements(IlluminatedText text, Map<Resource, Integer> resources) {
        if (text.vellum > resources.getOrDefault(VELLUM, 0))
            return false;
        if (text.candles > resources.getOrDefault(CANDLE, 0))
            return false;

        if (text.inks > inksAvailable(text, resources, new ArrayList<>()).size())
            return false;

        return true;
    }


}
