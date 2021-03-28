package games.dicemonastery.actions;

import core.AbstractGameState;
import games.dicemonastery.DiceMonasteryGameState;

import java.util.*;

import static games.dicemonastery.DiceMonasteryConstants.ILLUMINATED_TEXT;
import static games.dicemonastery.DiceMonasteryConstants.Resource;
import static games.dicemonastery.DiceMonasteryConstants.Resource.CANDLE;
import static games.dicemonastery.DiceMonasteryConstants.Resource.VELLUM;
import static java.util.stream.Collectors.toList;

public class WriteText extends UseMonk {

    final ILLUMINATED_TEXT textType;

    public WriteText(ILLUMINATED_TEXT type) {
        super(type.ap);
        textType = type;
    }


    // TODO: For the moment we will just use the most common ink a player has...this can be extended to IExtendedSequence later
    // so that the precise inks can be chosen

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        int player = state.getCurrentPlayer();

        state.addResource(player, Resource.VELLUM, -textType.vellum);
        state.addResource(player, Resource.CANDLE, -textType.candles);
        for (Resource specialInk : textType.specialInks) {
            state.addResource(player, specialInk, -1);
        }

        Map<Resource, Integer> allInks = state.getStores(player, r -> r.isInk);
        if (allInks.size() < textType.differentInks)
            throw new AssertionError("Not enough different inks to illuminate " + textType);

        List<Resource> inksInOrder = allInks.keySet().stream().sorted(Comparator.comparingInt(allInks::get)).collect(toList());
        Collections.reverse(inksInOrder);
        for (int i = 0; i < textType.differentInks; i++)
            state.addResource(player, inksInOrder.get(i), -1);

        state.writeText(player, textType); // this does the VP rewards based on how many have been written so far
        return true;
    }

    @Override
    public WriteText copy() {
        // immutable
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WriteText) {
            WriteText other = (WriteText) obj;
            return other.textType == textType;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return textType.hashCode();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Write " + textType;
    }


    public static boolean meetsRequirements(ILLUMINATED_TEXT text, Map<Resource, Integer> resources) {
        if (text.vellum > resources.get(VELLUM))
            return false;
        if (text.candles > resources.get(CANDLE))
            return false;

        Map<Resource, Integer> afterSupplyingSpecials = new EnumMap<>(resources);
        for (Resource specialInk : text.specialInks) {
            if (!resources.containsKey(specialInk))
                return false;
            afterSupplyingSpecials.merge(specialInk, -1, Integer::sum);
            if (afterSupplyingSpecials.get(specialInk) == 0)
                afterSupplyingSpecials.remove(specialInk);
        }

        if (text.differentInks > afterSupplyingSpecials.size())
            return false;

        return true;
    }
}
