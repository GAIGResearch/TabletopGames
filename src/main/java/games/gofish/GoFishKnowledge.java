package games.gofish;

import java.util.EnumSet;

/**
 * <p>The kinds of public knowledge about the hands in Go Fish. The game records all of them; a player may be set to
 * use only some (see {@link GoFishKnowledgeDecorator}), in which case its redeterminised copies of the state forget
 * the rest.</p>
 *
 * <ul>
 *     <li>CARDS_FISHED_FOR - the cards seen to be handed over in answer to an ask, and a card drawn after "Go fish"
 *     that is shown to take another turn.</li>
 *     <li>VOIDS - a player asked for a rank holds no card of it, until they are next seen to draw.</li>
 *     <li>ASKERS_CARD - the asker holds a card of the rank asked for (in addition to any handed over to them).</li>
 * </ul>
 */
public enum GoFishKnowledge {
    CARDS_FISHED_FOR,
    VOIDS,
    ASKERS_CARD;

    public static EnumSet<GoFishKnowledge> all() {
        return EnumSet.allOf(GoFishKnowledge.class);
    }

    /**
     * The kinds of knowledge whose flag is true.
     */
    public static EnumSet<GoFishKnowledge> of(boolean cardsFishedFor, boolean voids, boolean askersCard) {
        EnumSet<GoFishKnowledge> knowledge = EnumSet.noneOf(GoFishKnowledge.class);
        if (cardsFishedFor) knowledge.add(CARDS_FISHED_FOR);
        if (voids) knowledge.add(VOIDS);
        if (askersCard) knowledge.add(ASKERS_CARD);
        return knowledge;
    }
}
