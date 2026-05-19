package evaluation.tournaments;

import core.AbstractPlayer;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class AgentArchiverTest {

    private File tempDir;
    private String destDir;

    @Before
    public void setup() throws IOException {
        tempDir = Files.createTempDirectory("AgentArchiverTest").toFile();
        destDir = new File(tempDir, "dest").getAbsolutePath();
    }

    @After
    public void tearDown() throws IOException {
        if (tempDir.exists()) {
            FileUtils.deleteDirectory(tempDir);
        }
    }

    @Test
    public void testArchiveWithParetoFront() throws IOException {
        // Setup agents with distinct names in constructor
        AbstractPlayer agent1 = new RandomPlayer() {
            @Override
            public String toString() { return "Agent1"; }
        };
        AbstractPlayer agent2 = new RandomPlayer() {
            @Override
            public String toString() { return "Agent2"; }
        };
        AbstractPlayer agent3 = new RandomPlayer() {
            @Override
            public String toString() { return "Agent3"; }
        };

        List<AbstractPlayer> agents = Arrays.asList(agent1, agent2, agent3);

        // Create fake agent files
        File file1 = new File(tempDir, "agent1.json");
        Files.write(file1.toPath(), "agent1 content".getBytes());
        File file2 = new File(tempDir, "agent2.json");
        Files.write(file2.toPath(), "agent2 content".getBytes());
        File file3 = new File(tempDir, "agent3.json");
        Files.write(file3.toPath(), "agent3 content".getBytes());

        List<File> agentFiles = Arrays.asList(file1, file2, file3);

        // Setup tournament results
        TournamentResults results = new TournamentResults();
        results.addResult(agent1, 0, 0, 0, 0);
        results.addResult(agent2, 0, 0, 0, 0);
        results.addResult(agent3, 0, 0, 0, 0);

        assertEquals("Should have 3 agents registered", 3, results.getAllAgentNames().size());

        // Scenario: Agent1 and Agent2 are on Pareto front. Agent3 is dominated by both.
        // Agent1 vs Agent2: Agent1 wins 2-0
        results.updateWins(agent1, agent2, 2);
        results.updateWins(agent2, agent1, 0);
        results.updateGamePlayed(agent1, agent2);
        results.updateGamePlayed(agent1, agent2);
        results.updateGamePlayed(agent2, agent1);
        results.updateGamePlayed(agent2, agent1);

        // Agent1 vs Agent3: 1-1 (winRate 0.5)
        results.updateWins(agent1, agent3, 1);
        results.updateWins(agent3, agent1, 1);
        results.updateGamePlayed(agent1, agent3);
        results.updateGamePlayed(agent1, agent3);
        results.updateGamePlayed(agent3, agent1);
        results.updateGamePlayed(agent3, agent1);

        // Agent2 vs Agent3: 2-0 (winRate 1.0)
        results.updateWins(agent2, agent3, 2);
        results.updateWins(agent3, agent2, 0);
        results.updateGamePlayed(agent2, agent3);
        results.updateGamePlayed(agent2, agent3);
        results.updateGamePlayed(agent3, agent2);
        results.updateGamePlayed(agent3, agent2);

        // ParetoAnalysis also uses getWinRate(otherAgent, a) where a is not either of the two.
        // For Agent1 and Agent2, the only other agent is Agent3.
        // winRate(Agent1, Agent3) = 1.0
        // winRate(Agent2, Agent3) = 1.0
        // Since they are equal and they tie each other, neither dominates the other.
        // Agent3 is dominated by both.

        new AgentArchiver().archive(results, agents, agentFiles, destDir);

        File finalAgentsDir = new File(destDir, "FinalAgents");
        assertTrue("FinalAgents directory should exist", finalAgentsDir.exists());

        File[] files = finalAgentsDir.listFiles();
        assertNotNull(files);
        // Expect Agent1 and Agent2 to be archived.
        assertEquals("Should have 2 archived agents", 2, files.length);

        List<String> filenames = Arrays.stream(files).map(File::getName).toList();
        assertTrue("Should have a rank 1 agent", filenames.stream().anyMatch(n -> n.startsWith("FinalAgent_R01")));
        assertTrue("Should have a rank 2 agent", filenames.stream().anyMatch(n -> n.startsWith("FinalAgent_R02")));
        
        // Check content (just to be sure)
        for (File f : files) {
            String content = new String(Files.readAllBytes(f.toPath()));
            assertTrue("Content should match one of the agents", 
                content.equals("agent1 content") || content.equals("agent2 content"));
        }
    }

    @Test
    public void testArchiveWithNullFiles() {
        new AgentArchiver().archive(new TournamentResults(), new ArrayList<>(), null, destDir);
        File finalAgentsDir = new File(destDir, "FinalAgents");
        assertFalse("FinalAgents directory should not be created if no files provided", finalAgentsDir.exists());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testArchiveWithMismatchingSizes() {
        new AgentArchiver().archive(new TournamentResults(), Arrays.asList(new RandomPlayer()), 
            Arrays.asList(new File("f1.json"), new File("f2.json")), destDir);
    }

    @Test
    public void testArchiveWithDuplicates() throws IOException {
        // Agent1 and Agent2 are identical.
        AbstractPlayer agent1 = new RandomPlayer() {
            @Override
            public String toString() { return "Agent1"; }
        };
        AbstractPlayer agent2 = new RandomPlayer() {
            @Override
            public String toString() { return "Agent2"; }
        };

        List<AbstractPlayer> agents = Arrays.asList(agent1, agent2);

        File file1 = new File(tempDir, "agent1.json");
        Files.write(file1.toPath(), "agent1 content".getBytes());
        File file2 = new File(tempDir, "agent2.json");
        Files.write(file2.toPath(), "agent2 content".getBytes());

        List<File> agentFiles = Arrays.asList(file1, file2);

        TournamentResults results = new TournamentResults();
        results.addResult(agent1, 0, 0, 0, 0);
        results.addResult(agent2, 0, 0, 0, 0);

        // They tie each other
        results.updateWins(agent1, agent2, 1);
        results.updateWins(agent2, agent1, 1);
        results.updateGamePlayed(agent1, agent2);
        results.updateGamePlayed(agent2, agent1);

        new AgentArchiver().archive(results, agents, agentFiles, destDir);

        File finalAgentsDir = new File(destDir, "FinalAgents");
        assertTrue("FinalAgents directory should exist", finalAgentsDir.exists());

        File[] files = finalAgentsDir.listFiles();
        assertNotNull(files);
        // We expect only one of them to be archived due to clustering removal.
        assertEquals("Should have 1 archived agent due to duplicate removal", 1, files.length);
        assertTrue(files[0].getName().matches("FinalAgent_R01_A50\\.json"));
    }
}
