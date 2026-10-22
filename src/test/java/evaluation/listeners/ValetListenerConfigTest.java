package evaluation.listeners;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class ValetListenerConfigTest {

    @Test
    public void valetListenersLoadWithAllTheirMetrics() {
        for (String game : new String[]{"Whist", "GoFish", "Scopa"}) {
            IGameListener listener = IGameListener.createListener("json/valet/ValetListener_" + game + ".json");
            assertTrue(game, listener instanceof MetricsGameListener);
            assertTrue(game, ((MetricsGameListener) listener).metrics.keySet().containsAll(
                    List.of("FinalScore", "OrdinalPosition", "PlayerType", "Winner", "ActionsReduced",
                            "GameSummary", "TreeStats")));
        }
    }
}
