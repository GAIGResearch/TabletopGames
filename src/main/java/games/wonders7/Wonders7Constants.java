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
        BasicWild(Wood, Clay, Stone, Ore),
        Wood_Clay(Wood, Clay),
        Stone_Clay(Stone, Clay),
        Ore_Clay(Ore, Clay),
        Wood_Stone(Wood, Stone),
        Wood_Ore(Wood, Ore),
        Stone_Ore(Stone, Ore),
        Glass,
        Papyrus,
        Textile,
        RareWild(Glass, Papyrus, Textile),
        Cog,
        Compass,
        Tablet,
        ScienceWild(Cog, Compass, Tablet),
        Shield,
        Victory,
        Coin;

        public final Resource[] resources;

        Resource() {
            resources = new Resource[]{this};
        }

        Resource(Resource... resources) {
            this.resources = resources;
        }

        public boolean isComposite() {
            return resources.length > 1;
        }

        public boolean isBasic() {
            return this.resources[0] == Wood || this.resources[0] == Stone || this.resources[0] == Clay || this.resources[0] == Ore;
        }

        public boolean isRare() {
            return this.resources[0] == Glass || this.resources[0] == Papyrus || this.resources[0] == Textile;
        }

        public boolean isTradeable() {
            return (isBasic() && this != BasicWild) || (isRare() && this != RareWild);
        }

        public boolean includes(Resource resource) {
            for (Resource r : resources) {
                if (r == resource) {
                    return true;
                }
            }
            return false;
        }
    }

    public record TradeSource(Resource resource, int cost, int fromPlayer) {
    }
}
