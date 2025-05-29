/*
 * MaxiCP is under MIT License
 * Copyright (c)  2023 UCLouvain
 */

package org.maxicp.cp.engine.constraints;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.maxicp.cp.engine.CPSolverTest;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.CPSolver;
import org.maxicp.cp.CPFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MaximumMatchingGCCTest extends CPSolverTest {

    private static CPIntVar makeIVar(CPSolver cp, Integer... values) {
        return CPFactory.makeIntVar(cp, new HashSet<>(Arrays.asList(values)));
    }

    private void check(CPIntVar[] x, int[] matching, int size, int expectedSize) {
        ArrayList<Integer> values = new ArrayList<>();
        for (int i = 0; i < x.length; i++) {
            if (matching[i] >= 0) {
                assertTrue(x[i].contains(matching[i]));
                values.add(matching[i]);
            }

        }
        assertEquals(size, values.size());
        assertEquals(expectedSize, size);
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void test1(CPSolver cp) {
        CPIntVar[] x = new CPIntVar[]{
            makeIVar(cp, 1, 2),
            makeIVar(cp, 1, 2),
            makeIVar(cp, 1, 2, 3, 4)};
        int[] matching = new int[x.length];
        MaximumMatchingGCC maximumMatching = new MaximumMatchingGCC(new int[]{0, 1, 1, 1, 1}, x);


        check(x, matching, maximumMatching.compute(matching), 3);


        x[2].remove(3);
        check(x, matching, maximumMatching.compute(matching), 3);


        x[2].remove(4);
        check(x, matching, maximumMatching.compute(matching), 2);
    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void test2(CPSolver cp) {
        CPIntVar[] x = new CPIntVar[]{
            makeIVar(cp, 1, 4, 5),
            makeIVar(cp, 9, 10), // will be 10
            makeIVar(cp, 1, 4, 5, 8, 9), // will be 8 or 9
            makeIVar(cp, 1, 4, 5), //
            makeIVar(cp, 1, 4, 5, 8, 9), // will be 8 or 9
            makeIVar(cp, 1, 4, 5)
        };
        MaximumMatchingGCC maximumMatching = new MaximumMatchingGCC(new int[]{0, 1, 0, 0, 1, 1, 0, 0, 1, 1, 1}, x);
        int[] matching = new int[x.length];

        check(x, matching, maximumMatching.compute(matching), 6);

        x[5].remove(5);

        check(x, matching, maximumMatching.compute(matching), 6);

        x[0].remove(5);
        x[3].remove(5);

        check(x, matching, maximumMatching.compute(matching), 5);

    }

    @ParameterizedTest
    @MethodSource("getSolver")
    public void test3(CPSolver cp) {
        CPIntVar[] x = new CPIntVar[]{
            makeIVar(cp, 1, 4, 5),
            makeIVar(cp, 9, 10), // will be 10
            makeIVar(cp, 1, 4, 5, 8, 9), // will be 8 or 9
            makeIVar(cp, 1, 4, 5), //
            makeIVar(cp, 1, 4, 5, 8, 9), // will be 8 or 9
            makeIVar(cp, 1, 4, 5)
        };
        MaximumMatchingGCC maximumMatching = new MaximumMatchingGCC(new int[]{0, 1, 0, 0, 1, 1, 0, 0, 0, 2, 1}, x);
        int[] matching = new int[x.length];

        check(x, matching, maximumMatching.compute(matching), 6);

        x[5].remove(5);

        check(x, matching, maximumMatching.compute(matching), 6);

        x[0].remove(5);
        x[3].remove(5);

        check(x, matching, maximumMatching.compute(matching), 5);

    }
}
