package evaluation;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import players.simple.RandomPlayer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Data-management parts of ExpertIteration: how many rows each iteration gathered, how the sample rates
 * respond to that, and how both survive a restart.
 */
public class ExpertIterationDataTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    String dataDir;

    @Before
    public void setup() {
        dataDir = folder.getRoot().getPath();
    }

    private ExpertIteration createEI() {
        return new ExpertIteration(new String[]{
                "game=TicTacToe",
                "stateFeatures=evaluation.features.TurnAndPlayerOnly",
                "stateLearner=unused.json",
                "valueTarget=Base",
                "expertTrainingMode=Exponential",
                "sampleRate=0.01",
                "maxRecords=40000",
                "expertIterations=10",
                "destDir=" + dataDir
        });
    }

    private void appendRows(File file, int rows) throws IOException {
        List<String> lines = new ArrayList<>();
        if (!file.exists())
            lines.add("A\tB");
        for (int i = 0; i < rows; i++)
            lines.add(i + "\t" + i);
        Files.write(file.toPath(), lines, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    // ---- Fix 2: the sample-rate update rescales the rate that was used ----

    @Test
    public void sampleRateConvergesWhenDataPerIterationIsConstant() {
        double rowsAtRateOne = 1_000_000;
        double target = 5000;
        double rate = 0.01;
        for (int i = 0; i < 10; i++) {
            int gathered = (int) (rate * rowsAtRateOne);
            rate = ExpertIteration.updatedSampleRate(rate, gathered, target, 1.0);
        }
        // (the tolerance allows for rounding down of the rows gathered)
        double expected = target / rowsAtRateOne;
        assertEquals(expected, rate, expected * 1e-3);
        // and it stays there, rather than alternating
        double next = ExpertIteration.updatedSampleRate(rate, (int) (rate * rowsAtRateOne), target, 1.0);
        assertEquals(expected, next, expected * 1e-3);
    }

    @Test
    public void sampleRateDoesNotAlternateFromAFarOffStart() {
        // the restart scenario: the rate starts far too small
        double rowsAtRateOne = 1_000_000;
        double target = 5000;
        double expected = target / rowsAtRateOne;
        double rate = 1e-7;
        double previous = rate;
        for (int i = 0; i < 10; i++) {
            int gathered = (int) (rate * rowsAtRateOne);
            rate = ExpertIteration.updatedSampleRate(rate, gathered, target, 1.0);
            assertTrue("Rate should approach the target monotonically", rate >= previous * (1 - 1e-3));
            assertTrue("Rate should not overshoot the target", rate <= expected * (1 + 1e-3));
            previous = rate;
        }
        assertEquals(expected, rate, expected * 1e-3);
    }

    @Test
    public void rateChangeIsBoundedPerIteration() {
        double rate = 0.01;
        // a tiny iteration
        double up = ExpertIteration.updatedSampleRate(rate, 1, 5000, 1.0);
        assertEquals(rate * ExpertIteration.MAX_RATE_CHANGE, up, 1e-12);
        // a huge iteration
        double down = ExpertIteration.updatedSampleRate(rate, 100_000_000, 5000, 1.0);
        assertEquals(rate / ExpertIteration.MAX_RATE_CHANGE, down, 1e-12);
    }

    @Test
    public void zeroRowsGivesAFiniteRateWithinTheCap() {
        double rate = ExpertIteration.updatedSampleRate(0.01, 0, 5000, 0.05);
        assertFalse(Double.isNaN(rate));
        assertEquals(0.05, rate, 1e-12);
        rate = ExpertIteration.updatedSampleRate(0.001, 0, 5000, 0.05);
        assertEquals(0.01, rate, 1e-12);
    }

    @Test
    public void updateSampleRatesUsesTheCurrentRateNotTheBaseline() {
        ExpertIteration ei = createEI();
        ei.iter = 2;
        ei.actionSampleRate = 0.1;          // ten times the baseline of 0.01
        ei.actionRowsPerIteration[2] = 10000; // target is maxRecords / 8 = 5000
        ei.stateSampleRate = 0.02;
        ei.stateRowsPerIteration[2] = 2500;
        ei.updateSampleRates();
        assertEquals(0.05, ei.actionSampleRate, 1e-12);
        assertEquals(0.04, ei.stateSampleRate, 1e-12);
    }

    // ---- Fix 3: rows are counted per iteration, and survive a restart ----

    @Test
    public void countDataRowsExcludesHeadersAndIgnoresMissingFiles() throws IOException {
        File first = new File(dataDir, "first.txt");
        File second = new File(dataDir, "second.txt");
        appendRows(first, 7);
        appendRows(second, 3);
        assertEquals(10, ExpertIteration.countDataRows(first.getPath(), null, second.getPath(),
                new File(dataDir, "missing.txt").getPath()));
    }

    @Test
    public void rowsGatheredAreTheNewRowsOnlyWhenTheFileAlreadyHasData() throws IOException {
        // A restart in Exponential mode: the single data file already holds earlier iterations
        File actionFile = new File(dataDir, "Action_EI_00.txt");
        appendRows(actionFile, 1000);
        ExpertIteration ei = createEI();
        ei.iter = 7;
        ei.actionDataFilesByIteration[0] = actionFile.getPath();

        int[] before = ei.currentDataRowCounts();
        appendRows(actionFile, 63);
        ei.recordRowsGathered(before);

        assertEquals(63, ei.actionRowsPerIteration[7]);
        assertEquals(0, ei.stateRowsPerIteration[7]);
    }

    @Test
    public void progressSurvivesARestart() {
        ExpertIteration ei = createEI();
        ei.actionRowsPerIteration[0] = 5000;
        ei.actionRowsPerIteration[1] = 6000;
        ei.stateRowsPerIteration[1] = 300;
        ei.actionSampleRate = 0.0042;
        ei.stateSampleRate = 0.0031;
        ei.saveProgress();

        ExpertIteration restarted = createEI();
        assertTrue(restarted.loadProgress());
        assertArrayEquals(ei.actionRowsPerIteration, restarted.actionRowsPerIteration);
        assertArrayEquals(ei.stateRowsPerIteration, restarted.stateRowsPerIteration);
        assertEquals(0.0042, restarted.actionSampleRate, 1e-12);
        assertEquals(0.0031, restarted.stateSampleRate, 1e-12);
    }

    @Test
    public void noProgressFileLeavesTheBaseline() {
        ExpertIteration ei = createEI();
        assertFalse(ei.loadProgress());
        assertEquals(0.01, ei.actionSampleRate, 1e-12);
        assertEquals(0.01, ei.stateSampleRate, 1e-12);
        assertEquals(0, ei.actionRowsPerIteration[0]);
    }

    @Test
    public void progressFromARunWithFewerIterationsLoadsTheOverlap() {
        ExpertIteration shortRun = new ExpertIteration(new String[]{
                "game=TicTacToe",
                "stateFeatures=evaluation.features.TurnAndPlayerOnly",
                "stateLearner=unused.json",
                "valueTarget=Base",
                "expertIterations=3",
                "destDir=" + dataDir
        });
        shortRun.actionRowsPerIteration[3] = 77;
        shortRun.saveProgress();

        ExpertIteration longerRun = createEI();
        assertTrue(longerRun.loadProgress());
        assertEquals(11, longerRun.actionRowsPerIteration.length);
        assertEquals(77, longerRun.actionRowsPerIteration[3]);
        assertEquals(0, longerRun.actionRowsPerIteration[4]);
    }

    // ---- Batch mode: both tournaments of a data gathering go into the iteration's file ----

    private ExpertIteration createGatheringEI(String trainingMode) {
        return new ExpertIteration(new String[]{
                "game=TicTacToe",
                "nPlayers=2",
                "stateFeatures=evaluation.features.TurnAndPlayerOnly",
                "stateLearner=json/learners/LogisticWinLearner.json",
                "valueTarget=Base",
                "expertTrainingMode=" + trainingMode,
                "sampleRate=1.0",
                "destDir=" + dataDir
        });
    }

    private void runTournament(ExpertIteration ei, int games) {
        Map<RunArg, Object> config = new HashMap<>(ei.RGConfig);
        config.put(RunArg.verbose, false);
        config.put(RunArg.listener, new ArrayList<String>());
        config.put(RunArg.mode, "random");
        config.put(RunArg.matchups, games);
        ei.runTournament(List.of(new RandomPlayer(), new RandomPlayer()), config);
    }

    @Test
    public void batchModeKeepsTheDataFromEveryTournamentInAnIteration() throws IOException {
        ExpertIteration ei = createGatheringEI("Batch");
        ei.iter = 2;
        File stateFile = new File(dataDir, "State_EI_02.txt");

        runTournament(ei, 30);
        int afterFirst = ExpertIteration.countDataRows(stateFile.getPath());
        assertTrue(afterFirst > 0);
        runTournament(ei, 2);
        int afterSecond = ExpertIteration.countDataRows(stateFile.getPath());

        assertTrue("Second tournament should add to the data, not replace it: " + afterFirst + " then " + afterSecond,
                afterSecond > afterFirst);
        String header = Files.readAllLines(stateFile.toPath()).getFirst();
        assertEquals("Header should be written once", 1,
                Files.readAllLines(stateFile.toPath()).stream().filter(header::equals).count());
    }

    @Test
    public void batchModeGatheringStartsWithAnEmptyFileForTheIteration() throws IOException {
        // e.g. left over from a run that was aborted part way through gathering
        File stale = new File(dataDir, "State_EI_02.txt");
        File previousIteration = new File(dataDir, "State_EI_01.txt");
        appendRows(stale, 50);
        appendRows(previousIteration, 50);

        ExpertIteration ei = createGatheringEI("Batch");
        ei.iter = 2;
        ei.prepareDataFilesForGathering();

        assertFalse(stale.exists());
        assertTrue(previousIteration.exists());
    }

    @Test
    public void exponentialModeGatheringKeepsTheExistingData() throws IOException {
        File allData = new File(dataDir, "State_EI_00.txt");
        appendRows(allData, 50);

        ExpertIteration ei = createGatheringEI("Exponential");
        ei.iter = 2;
        ei.prepareDataFilesForGathering();
        runTournament(ei, 2);

        assertTrue(ExpertIteration.countDataRows(allData.getPath()) > 50);
    }

    @Test
    public void restartWithTuningUsesTheRecordedRowsForTheSkippedGathering() {
        // the gathering for iteration 3 completed and was recorded; tuning then failed
        ExpertIteration first = createEI();
        first.actionSampleRate = 0.02;
        first.actionRowsPerIteration[3] = 2500;
        first.saveProgress();

        ExpertIteration restarted = createEI();
        restarted.loadProgress();
        restarted.iter = 3;
        restarted.updateSampleRates();
        // 0.02 * 5000 / 2500
        assertEquals(0.04, restarted.actionSampleRate, 1e-12);
    }
}
