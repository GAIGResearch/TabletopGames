package evaluation.summarisers;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class TAGSummariser implements Collector<Number, TAGNumericStatSummary, TAGNumericStatSummary> {

    /**
     * A function that creates and returns a new mutable result container.
     *
     * @return a function which returns a new, mutable result container
     */
    @Override
    public Supplier<TAGNumericStatSummary> supplier() {
        return TAGNumericStatSummary::new;
    }

    /**
     * A function that folds a value into a mutable result container.
     *
     * @return a function which folds a value into a mutable result container
     */
    @Override
    public BiConsumer<TAGNumericStatSummary, Number> accumulator() {
        return TAGNumericStatSummary::add;
    }

    /**
     * A function that accepts two partial results and merges them.  The
     * combiner function may fold state from one argument into the other and
     * return that, or may return a new result container.
     *
     * @return a function which combines two partial results into a combined
     * result
     */
    @Override
    public BinaryOperator<TAGNumericStatSummary> combiner() {
        return (ss1, ss2) -> {
            ss1.add(ss2);
            return ss1;
        };
    }

    /**
     * Perform the final transformation from the intermediate accumulation type
     * {@code A} to the final result type {@code R}.
     *
     * <p>If the characteristic {@code IDENTITY_TRANSFORM} is
     * set, this function may be presumed to be an identity transform with an
     * unchecked cast from {@code A} to {@code R}.
     *
     * @return a function which transforms the intermediate result to the final
     * result
     */
    @Override
    public Function<TAGNumericStatSummary, TAGNumericStatSummary> finisher() {
        return ss -> ss;
    }

    /**
     * Returns a {@code Set} of {@code Collector.Characteristics} indicating
     * the characteristics of this Collector.  This set should be immutable.
     *
     * @return an immutable set of collector characteristics
     */
    @Override
    public Set<Characteristics> characteristics() {
        Set<Characteristics> retValue = new HashSet<>();
        retValue.add(Characteristics.IDENTITY_FINISH);
        retValue.add(Characteristics.UNORDERED);
        return retValue;
    }
}
