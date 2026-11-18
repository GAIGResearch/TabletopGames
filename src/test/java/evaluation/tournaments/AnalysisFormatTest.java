package evaluation.tournaments;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class AnalysisFormatTest {

    @Test
    public void largeSamplesUseTheNormal() {
        assertEquals(1.960, AnalysisFormat.criticalValue(0.95, 101), 1e-3);
        assertEquals(2.576, AnalysisFormat.criticalValue(0.99, 5000), 1e-3);
    }

    @Test
    public void smallSamplesUseTWithNMinusOneDegreesOfFreedom() {
        assertEquals(12.706, AnalysisFormat.criticalValue(0.95, 2), 1e-3);
        assertEquals(2.262, AnalysisFormat.criticalValue(0.95, 10), 1e-3);
        assertEquals(3.250, AnalysisFormat.criticalValue(0.99, 10), 1e-3);
        assertEquals(1.984, AnalysisFormat.criticalValue(0.95, 100), 1e-3);
        assertTrue(Double.isNaN(AnalysisFormat.criticalValue(0.95, 1)));
    }

    @Test
    public void distributionMatchesCriticalValue() {
        assertEquals("the t-distribution with 9 degrees of freedom", AnalysisFormat.distribution(10));
        assertEquals("the t-distribution with 99 degrees of freedom", AnalysisFormat.distribution(100));
        assertEquals("the Normal", AnalysisFormat.distribution(101));
    }

    @Test
    public void intervalIsFormattedAsBounds() {
        assertEquals("[0.400, 0.600]", AnalysisFormat.interval(0.5, 0.05, 2.0, 3));
        assertEquals("[1.25, 1.75]", AnalysisFormat.interval(1.5, 0.125, 2.0, 2));
    }

    @Test
    public void tabTableStartsEachColumnAtTheSameTabStop() {
        List<String> lines = AnalysisFormat.tabTable(List.of(
                new String[]{"Agent", "Win rate", "SE"},
                new String[]{"Dominion_HiBudget_Heuristic", "0.816", "0.022"},
                new String[]{"Dominion", "0.456", "0.018"}));
        // the longest name (27 characters) ends in the fourth tab stop, so the second column starts at the fifth;
        // "Win rate" fills a whole tab stop, so one tab takes it to the third column, two stops after the second
        assertEquals("Agent\t\t\t\tWin rate\tSE\n", lines.get(0));
        assertEquals("Dominion_HiBudget_Heuristic\t0.816\t\t0.022\n", lines.get(1));
        assertEquals("Dominion\t\t\t0.456\t\t0.018\n", lines.get(2));
    }
}
