package games.wonders7;

import utilities.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class Wonders7Constants {
    // ENUM OF MATERIALS
    public enum Resource { //Another enum for costs
        Wood,
        Stone,
        Clay,
        Ore,
        BasicWild,
        Glass,
        Papyrus,
        Textile,
        RareWild,
        Cog,
        Compass,
        Tablet,
        Shield,
        Victory,
        Coin;

        public boolean isBasic() {
            return this == Wood || this == Stone || this == Clay || this == Ore;
        }
        public boolean isRare() {
            return this == Glass || this == Papyrus || this == Textile;
        }
    }

    public record TradeSource(Resource resource, int cost, int fromPlayer) {
    }

    @SafeVarargs
    public static List<Map<Resource, Long>> createHashList(Map<Wonders7Constants.Resource, Long>... hashmaps){
        List<Map<Wonders7Constants.Resource, Long>> list = new ArrayList<>();
        Collections.addAll(list, hashmaps);
        return list;
    }

    public static Map<Resource, Long> createCardHash(Resource... resources){
        // This will have to create the resource hashmaps for each card and return them
        return Arrays.stream(resources).collect(Collectors.groupingBy(e -> e, Collectors.counting()));
    }

    @SafeVarargs
    public static Map<Resource, Long> createCardHash(Pair<Resource, Integer>... resources){
        // This will have to create the resource hashmaps for each card and return them
        Map<Resource, Long> map = new HashMap<>();
        for (Pair<Resource, Integer> resource : resources) {
            map.put(resource.a, Long.valueOf(resource.b));
        }
        return map;
    }
}
