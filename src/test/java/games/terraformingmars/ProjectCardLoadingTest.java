package games.terraformingmars;

import com.fasterxml.jackson.databind.JsonNode;
import core.CoreConstants;
import core.components.Deck;
import games.terraformingmars.actions.ModifyPlayerResource;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.rules.requirements.ResourceRequirement;
import org.junit.Test;
import utilities.Json;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Project cards are read from {@code data/terraformingmars/projectCards/} with the shared Jackson mapper, with
 * cards, effects and requirements identified by a {@code "class"} property.
 */
public class ProjectCardLoadingTest {

    private static Deck<TMCard> load(TMTypes.Expansion expansion) {
        Deck<TMCard> deck = new Deck<>("Project cards", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        expansion.loadProjectCards(deck);
        return deck;
    }

    @Test
    public void everyCardLoads() {
        assertEquals(137, load(TMTypes.Expansion.Base).getSize());
        assertEquals(71, load(TMTypes.Expansion.CorporateEra).getSize());
    }

    @Test
    public void everyCardWritesAndReadsBackUnchanged() throws Exception {
        for (TMTypes.Expansion expansion : new TMTypes.Expansion[]{TMTypes.Expansion.Base, TMTypes.Expansion.CorporateEra}) {
            for (TMCard card : load(expansion).getComponents()) {
                JsonNode written = Json.toTree(card);
                TMCard back = Json.mapper().treeToValue(written, TMCard.class);
                assertEquals(card.getComponentName(), written, Json.toTree(back));
            }
        }
    }

    @Test
    public void polymorphicEffectsAndRequirementsKeepTheirClasses() {
        Optional<TMCard> radChem = load(TMTypes.Expansion.Base).getComponents().stream()
                .filter(c -> c.getComponentName().equals("Rad-Chem Factory")).findFirst();
        assertTrue(radChem.isPresent());
        assertEquals(2, radChem.get().immediateEffects.length);
        ModifyPlayerResource energy = (ModifyPlayerResource) radChem.get().immediateEffects[0];
        assertEquals(TMTypes.Resource.Energy, energy.resource);
        assertEquals(-1.0, energy.change, 0.0);
        assertTrue(energy.requirements.stream().anyMatch(r -> r instanceof ResourceRequirement));
    }

    @Test
    public void discountsLoad() {
        long withDiscounts = Arrays.stream(new TMTypes.Expansion[]{TMTypes.Expansion.Base, TMTypes.Expansion.CorporateEra})
                .flatMap(e -> load(e).getComponents().stream())
                .filter(c -> !c.discountEffects.isEmpty())
                .peek(c -> c.discountEffects.forEach(d -> {
                    assertNotNull(c.getComponentName(), d.a);
                    assertTrue(c.getComponentName(), d.b > 0);
                }))
                .count();
        assertTrue(withDiscounts > 0);
    }
}
