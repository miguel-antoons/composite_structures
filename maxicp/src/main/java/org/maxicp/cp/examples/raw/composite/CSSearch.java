package org.maxicp.cp.examples.raw.composite;

import static org.maxicp.search.Searches.*;

import java.util.BitSet;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.modeling.IntVar;
import org.maxicp.modeling.ModelProxy;
import org.maxicp.modeling.algebra.bool.Eq;
import org.maxicp.modeling.algebra.bool.NotEq;
import org.maxicp.modeling.algebra.integer.IntExpression;
import org.maxicp.search.Searches;

public class CSSearch{

    public static Supplier<Runnable[]> firstFail(CPIntVar[][] vars) {
        CPIntVar[] flatVars = getFlattenedVars(vars);
        return Searches.firstFail(flatVars);
    }

    public static Supplier<Runnable[]> lastConflictFirstFail(
            CPIntVar[][] vars
    ) {
        CPIntVar[] flatVars = getFlattenedVars(vars);
        return lastConflict(
            () -> selectMin(flatVars, xi -> xi.size() > 1, IntVar::size),
            IntExpression::min
        );
    }

    public static Supplier<Runnable[]> conflictOrderingFirstFail(
            CPIntVar[][] vars
    ) {
        CPIntVar[] flatVars = getFlattenedVars(vars);
        return conflictOrderingSearch(
            () -> selectMin(flatVars, xi -> xi.size() > 1, IntVar::size),
            IntExpression::min
        );
    }

    public static Supplier<Runnable[]> highDegreeFirst(
        CPIntVar[][] vars,
        CompositeStructureInstance instance,
        CompositeStructures cs
    ) {
        int[] cardinalities = getCardinalities(
            instance.edges,
            instance.noNodes
        );
        Integer[] cardinalitiesBoxed = IntStream.of(cardinalities)
            .boxed()
            .toArray(Integer[]::new);
        ModelProxy model = vars[0][0].getModelProxy();
        int[] sortedIndices = IntStream.range(0, cardinalities.length)
            .boxed()
            .sorted((i, j) ->
                cardinalitiesBoxed[j].compareTo(cardinalitiesBoxed[i])
            )
            .mapToInt(ele -> ele)
            .toArray();

        return searchFromSortedIndices(
            sortedIndices,
            model,
            vars,
            instance,
            cs,
            CSSearch::balancedValueSelector
//            CSSearch::minValueSelector
        );
    }

    public static Supplier<Runnable[]> thinFirst(
        CPIntVar[][] vars,
        CompositeStructureInstance instance,
        CompositeStructures cs
    ) {
        ModelProxy model = vars[0][0].getModelProxy();
        int[] sortedIndices = IntStream.range(0, instance.noNodes)
            .boxed()
            .sorted(Comparator.comparingInt(i -> instance.nodeThickness[i]))
            .mapToInt(ele -> ele)
            .toArray();

        return searchFromSortedIndices(
            sortedIndices,
            model,
            vars,
            instance,
            cs,
            CSSearch::balancedValueSelector
//            CSSearch::minValueSelector
        );
    }

    public static Supplier<Runnable[]> highThinFirst(
        CPIntVar[][] vars,
        CompositeStructureInstance instance,
        CompositeStructures cs
    ) {
        int[] cardinalities = getCardinalities(
            instance.edges,
            instance.noNodes
        );
        int[] sortedIndices = IntStream.range(0, instance.noNodes)
            .boxed()
            .sorted((i, j) ->  cardinalities[j] == cardinalities[i] ?
                instance.nodeThickness[i] - instance.nodeThickness[j] :
                cardinalities[j] - cardinalities[i]
            )
            .mapToInt(ele -> ele)
            .toArray();
        ModelProxy model = vars[0][0].getModelProxy();

        return searchFromSortedIndices(
            sortedIndices,
            model,
            vars,
            instance,
            cs,
            CSSearch::balancedValueSelector
//            CSSearch::minValueSelector
        );
    }

    public static Supplier<Runnable[]> highThickFirst(
        CPIntVar[][] vars,
        CompositeStructureInstance instance,
        CompositeStructures cs
    ) {
        int[] cardinalities = getCardinalities(
            instance.edges,
            instance.noNodes
        );
        int[] sortedIndices = IntStream.range(0, instance.noNodes)
            .boxed()
            .sorted((i, j) ->  cardinalities[j] == cardinalities[i] ?
                instance.nodeThickness[j] - instance.nodeThickness[i] :
                cardinalities[j] - cardinalities[i]
            )
            .mapToInt(ele -> ele)
            .toArray();
        ModelProxy model = vars[0][0].getModelProxy();

        return searchFromSortedIndices(
            sortedIndices,
            model,
            vars,
            instance,
            cs,
            CSSearch::balancedValueSelector
//            CSSearch::minValueSelector
        );
    }

    public static Supplier<Runnable[]> thickFirst(
        CPIntVar[][] vars,
        CompositeStructureInstance instance,
        CompositeStructures cs
    ) {
        ModelProxy model = vars[0][0].getModelProxy();
        int[] sortedIndices = IntStream.range(0, instance.noNodes)
            .boxed()
            .sorted((i, j) -> instance.nodeThickness[j] - instance.nodeThickness[i])
            .mapToInt(ele -> ele)
            .toArray();

        return searchFromSortedIndices(
            sortedIndices,
            model,
            vars,
            instance,
            cs,
            CSSearch::balancedValueSelector
//            CSSearch::minValueSelector
        );
    }


    private static int[] getCardinalities(
        CompositeStructureInstance.DirectedEdge[] edges,
        int noNodes
    ) {
        int[] cardinalities = new int[noNodes];
        for (CompositeStructureInstance.DirectedEdge edge : edges) {
            cardinalities[edge.parent]++;
            cardinalities[edge.child]++;
        }
        return cardinalities;
    }


    private static CPIntVar[] getFlattenedVars(CPIntVar[][] vars) {
        int length = 0;
        for (CPIntVar[] seq : vars) length += seq.length;

        CPIntVar[] flatVars = new CPIntVar[length];
        int currentLength = 0;
        for (CPIntVar[] seq : vars) {
            System.arraycopy(seq, 0, flatVars, currentLength, seq.length);
            currentLength += seq.length;
        }

        return flatVars;
    }

    private static Supplier<Runnable[]> searchFromSortedIndices(
            int[] sortedIndices,
            ModelProxy model,
            CPIntVar[][] vars,
            CompositeStructureInstance instance,
            CompositeStructures cs,
            Function<Var, Integer> valueSelector
    ) {
        return () -> {
            Var result          = getBestVar(vars, sortedIndices);
            result.instance     = instance;
            result.cs           = cs;
            CPIntVar selected   = result.selected;

            if (selected == null) return EMPTY;

            int v = valueSelector.apply(result);
            if (v == -1) return EMPTY;

            return branch(
                () -> model.add(new Eq(selected, v)),
                () -> model.add(new NotEq(selected, v))
            );
        };
    }

    private static int balancedValueSelector(Var result) {
        CompositeStructureInstance instance = result.instance;
        CompositeStructures cs              = result.cs;
        int[] ppd                           = result.noPliesPerDirection;
        CPIntVar selected                   = result.selected;
        BitSet activeConstraints            = cs.activeConstraints();
        if (activeConstraints.get(CompositeStructures.activations.PLY_PER_DIRECTION.ordinal())) {
            // select the value that is most needed according to the cardinalities
            int[] reqPPD        = instance.pliesPerDirection[result.seqIndex].toArray();
            int[] missingPPD    = instance.pliesPerDirection[result.seqIndex].toArray();
            for (int i = 0; i < ppd.length; i++) {
                missingPPD[i] -= ppd[i];
            }

            int[] reducedSorted = getSortedIndexes(missingPPD);
            int[] values        = new int[]{1, 2, 3, 4};
            for (int i : reducedSorted) {
                boolean merge   = (values[i] == 1 || values[i] == 3) && cs.MERGED_CARDINALITIES();
                int current     = merge ? ppd[1] + ppd[3] : ppd[i + 1];
                int total       = merge ? reqPPD[1] + reqPPD[3] : reqPPD[i + 1];

                if (selected.contains(values[i]) && current < total) return values[i];
            }
        } else {
            int[] reducedSorted = getSortedIndexes(ppd);
            int[] values        = new int[]{1, 2, 3, 4};
            for (int i : reducedSorted) {
                if (selected.contains(values[i])) return values[i];
            }
        }

        return -1;
    }

    private static int[] getSortedIndexes(int[] ppd) {
        int[] reduced = new int[]{ppd[1], ppd[2], ppd[3], ppd[4]};
        return IntStream.range(0, reduced.length)
            .boxed()
            .sorted((i, j) -> reduced[j] - reduced[i])
            .mapToInt(ele -> ele)
            .toArray();
    }

    private static Var getBestVar(CPIntVar[][] vars, int[] sortedIndices) {
        CPIntVar selected       = null;
        int size                = 6;
        int sequence            = -1;
        int[] pliesPerDirection = new int[0];
        for (int i : sortedIndices) {
            pliesPerDirection = new int[5];
            for (int j = 0; j < vars[i].length; j++) {
                if (!vars[i][j].isFixed() && vars[i][j].size() < size) {
                    size        = vars[i][j].size();
                    selected    = vars[i][j];
                    sequence    = i;
                    if (vars[i][j].size() == 2) break;
                } else if (vars[i][j].isFixed()) {
                    pliesPerDirection[vars[i][j].min()]++;
                }
            }
            if (selected != null) break;
        }
        Var result                  = new Var();
        result.selected             = selected;
        result.noPliesPerDirection  = pliesPerDirection;
        result.seqIndex             = sequence;

        return result;
    }

    static class Var {
        CPIntVar selected;
        int[] noPliesPerDirection;
        int seqIndex;
        CompositeStructureInstance instance;
        CompositeStructures cs;
    }

    private static int minValueSelector(Var result) {
        return result.selected.min();
    }
}
