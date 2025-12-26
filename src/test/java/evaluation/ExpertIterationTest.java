package evaluation;

import core.AbstractPlayer;
import evaluation.optimisation.ITPSearchSpace;
import evaluation.optimisation.NTBEA;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import players.mcts.MCTSParams;

import static org.mockito.Mockito.*;

public class ExpertIterationTest {

    NTBEA ntbea;

    @Before
    public void setup() {
        ntbea = mock(NTBEA.class);
    }

    @Test
    public void testIdenticalSearchSpaces() {
        // Case 1: Search spaces where every parameter is a tuned dimension
        MCTSParams p1 = new MCTSParams();
        MCTSParams p2 = new MCTSParams();

        String searchSpace = "src/test/java/evaluation/MCTSSearch_Heuristic.json";

        ITPSearchSpace<AbstractPlayer> ss1 = new ITPSearchSpace<>(p1, searchSpace);
        ITPSearchSpace<AbstractPlayer> ss2 = new ITPSearchSpace<>(p2, searchSpace);
        
        ExpertIteration.fixSSDimensions(ntbea, ss1, new int[ss2.nDims()], ss2);
        Mockito.verify(ntbea, never()).fixTunableParameter(any(String.class), any(Object.class));
    }

    @Test
    public void testMatchingMatchingParameters() {
        // Case 2: Search spaces with matching static (non-tuned) parameters
        MCTSParams p1 = new MCTSParams();
        MCTSParams p2 = new MCTSParams();

        String searchSpace1 = "src/test/java/evaluation/MCTSSearch_Heuristic.json";
        String searchSpace2 = "src/test/java/evaluation/MCTSSearch_HeuristicSample.json";

        ITPSearchSpace<AbstractPlayer> ss1 = new ITPSearchSpace<>(p1, searchSpace1);
        ITPSearchSpace<AbstractPlayer> ss2 = new ITPSearchSpace<>(p2, searchSpace2);
        
        ExpertIteration.fixSSDimensions(ntbea, ss1, new int[ss2.nDims()], ss2);
        Mockito.verify(ntbea, never()).fixTunableParameter(any(String.class), any(Object.class));
    }

    @Test
    public void testFixingOfParameterInRange() {
        // Case 2: Search spaces with matching static (non-tuned) parameters
        MCTSParams p1 = new MCTSParams();
        MCTSParams p2 = new MCTSParams();

        String searchSpace1 = "src/test/java/evaluation/MCTSSearch_Heuristic.json";
        String searchSpace2 = "src/test/java/evaluation/MCTSSearch_HeuristicSample.json";

        ITPSearchSpace<AbstractPlayer> ss1 = new ITPSearchSpace<>(p1, searchSpace1);
        ITPSearchSpace<AbstractPlayer> ss2 = new ITPSearchSpace<>(p2, searchSpace2);

        ExpertIteration.fixSSDimensions(ntbea, ss2, new int[ss1.nDims()], ss1);
        Mockito.verify(ntbea, times(8)).fixTunableParameter(any(String.class), any(Object.class));
    }

    @Test(expected = AssertionError.class)
    public void testMismatchingNonTunedParametersThrowsError() {
        // Case 3: Search spaces with non-matching static (non-tuned) parameters
        MCTSParams p1 = new MCTSParams();
        MCTSParams p2 = new MCTSParams();

        String searchSpace1 = "src/test/java/evaluation/MCTSSearch_Heuristic.json";
        String searchSpace2 = "src/test/java/evaluation/MCTSSearch_HeuristicSampleIncorrectII.json";

        ITPSearchSpace<AbstractPlayer> ss1 = new ITPSearchSpace<>(p1, searchSpace1);
        ITPSearchSpace<AbstractPlayer> ss2 = new ITPSearchSpace<>(p2, searchSpace2);

        ExpertIteration.fixSSDimensions(ntbea, ss1, new int[ss2.nDims()], ss2);
    }
}
