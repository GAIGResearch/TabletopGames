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
        Glass,
        Papyrus,
        Textile,
        Cog,
        Compass,
        Tablet,
        Shield,
        Victory,
        Coin
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
