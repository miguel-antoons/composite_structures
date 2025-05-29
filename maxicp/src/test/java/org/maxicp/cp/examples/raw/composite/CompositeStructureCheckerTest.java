package org.maxicp.cp.examples.raw.composite;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import org.maxicp.util.exception.InconsistencyException;

class CompositeStructureCheckerTest {

    final CompositeStructureInstance instance = new CompositeStructureInstance(
        new int[]{16, 12},
        new int[][]{{0, 1}},
        new int[][]{{4, 4, 4, 4}, {4, 2, 2, 4}}
    );

    final CompositeStructureSolution sol = new CompositeStructureSolution(
        instance,
        new ArrayList<>(
            Arrays.asList(
                new int[]{1, 4, 1, 2, 2, 3, 4, 3, 3, 4, 3, 2, 2, 1, 4, 1},
                new int[]{1, 4, 1, 2, 3, 4, 4, 3, 2, 1, 4, 1}
            )
        ),
        new ArrayList<>(
            List.of(new int[]{0, 1, 2, 3, 5, 6, 9, 10, 12, 13, 14, 15})
        )
    );

    @org.junit.jupiter.api.Test
    void testBlendingCheckSuccess() {
        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.BLENDING.ordinal());
        assertDoesNotThrow(
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "Blending check failed when it should have succeeded"
        );
    }

    @org.junit.jupiter.api.Test
    void testBlendingCheckFailure() {
        CompositeStructureSolution sol = new CompositeStructureSolution(
            instance,
            new ArrayList<>(
                Arrays.asList(
                    new int[]{1, 1, 2, 2, 3, 3, 4, 4, 4, 4, 3, 3, 2, 2, 1, 1,},
                    new int[]{1, 2, 2, 2, 3, 4, 4, 3, 2, 2, 1, 1}
                )
            ),
            new ArrayList<>(
                List.of(new int[]{0, 1, 2, 3, 4, 6, 7, 10, 12, 13, 14, 15})
            )
        );

        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.BLENDING.ordinal());
        assertThrows(
            InconsistencyException.class,
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "Blending check succeeded when it should have failed"
        );
    }

    @org.junit.jupiter.api.Test
    void testCrossingCheckSuccess() {
        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.NO_PLY_CROSSING.ordinal());
        assertDoesNotThrow(
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "Crossing check failed when it should have succeeded"
        );
    }

    @org.junit.jupiter.api.Test
    void testCrossingCheckFailure() {
        CompositeStructureSolution sol = new CompositeStructureSolution(
            instance,
            new ArrayList<>(
                Arrays.asList(
                    new int[]{1, 1, 2, 2, 3, 3, 4, 4, 4, 4, 3, 3, 2, 2, 1, 1},
                    new int[]{1, 1, 2, 2, 3, 4, 4, 3, 2, 2, 1, 1}
                )
            ),
            new ArrayList<>(
                List.of(new int[]{1, 0, 2, 3, 4, 6, 7, 10, 12, 13, 14, 15})
            )
        );

        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.NO_PLY_CROSSING.ordinal());
        assertThrows(
            InconsistencyException.class,
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "Crossing check succeeded when it should have failed"
        );
    }

    @org.junit.jupiter.api.Test
    void testNo90GapCheckSuccess() {
        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.NO_90_GAP.ordinal());
        assertDoesNotThrow(
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "No90Gap check failed when it should have succeeded"
        );
    }

    @org.junit.jupiter.api.Test
    void testNo90GapCheckFailure() {
        CompositeStructureSolution sol = new CompositeStructureSolution(
            instance,
            new ArrayList<>(
                Arrays.asList(
                    new int[]{1, 3, 2, 2, 3, 3, 4, 4, 4, 4, 3, 3, 2, 2, 1, 1},
                    new int[]{1, 3, 2, 2, 3, 4, 4, 3, 2, 2, 1, 1}
                )
            ),
            new ArrayList<>(
                List.of(new int[]{0, 1, 2, 3, 4, 6, 7, 10, 12, 13, 14, 15})
            )
        );

        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.NO_90_GAP.ordinal());
        assertThrows(
            InconsistencyException.class,
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "No90Gap check succeeded when it should have failed"
        );
    }

    @org.junit.jupiter.api.Test
    void testNo4ConsecutiveCheckSuccess() {
        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.NO_4_CONSECUTIVE.ordinal());
        assertDoesNotThrow(
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "No5Consecutive check failed when it should have succeeded"
        );
    }

    @org.junit.jupiter.api.Test
    void testNo4ConsecutiveCheckFailure() {
        CompositeStructureSolution sol = new CompositeStructureSolution(
            instance,
            new ArrayList<>(
                Arrays.asList(
                    new int[]{1, 1, 2, 2, 3, 3, 4, 4, 4, 4, 3, 3, 2, 2, 1, 1},
                    new int[]{1, 1, 2, 2, 3, 4, 4, 4, 2, 2, 1, 1}
                )
            ),
            new ArrayList<>(
                List.of(new int[]{0, 1, 2, 3, 4, 6, 7, 10, 12, 13, 14, 15})
            )
        );

        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.NO_4_CONSECUTIVE.ordinal());
        assertThrows(
            InconsistencyException.class,
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "No5Consecutive check succeeded when it should have failed"
        );
    }

    @org.junit.jupiter.api.Test
    void testSymmetryCheckSuccess() {
        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.SYMMETRY.ordinal());
        assertDoesNotThrow(
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "Symmetry check failed when it should have succeeded"
        );
    }

    @org.junit.jupiter.api.Test
    void testSymmetryCheckFailure() {
        CompositeStructureSolution sol = new CompositeStructureSolution(
            instance,
            new ArrayList<>(
                Arrays.asList(
                    new int[]{3, 1, 2, 2, 3, 3, 4, 4, 4, 4, 3, 3, 2, 2, 1, 1},
                    new int[]{3, 1, 2, 2, 3, 4, 4, 3, 2, 2, 1, 1}
                )
            ),
            new ArrayList<>(
                List.of(new int[]{0, 1, 2, 3, 4, 6, 7, 10, 12, 13, 14, 15})
            )
        );

        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.SYMMETRY.ordinal());
        assertThrows(
            InconsistencyException.class,
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "Symmetry check succeeded when it should have failed"
        );
    }

    @org.junit.jupiter.api.Test
    void testMiddleAsymmetryCheckSuccess1() {
        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.MIDDLE_ASYMMETRY.ordinal());
        assertDoesNotThrow(
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "MiddleAsymmetry check failed when it should have succeeded"
        );
    }

    @org.junit.jupiter.api.Test
    void testMiddleAsymmetryCheckFailure1() {
        CompositeStructureSolution sol = new CompositeStructureSolution(
            instance,
            new ArrayList<>(
                Arrays.asList(
                    new int[]{1, 1, 2, 2, 3, 3, 4, 3, 4, 4, 3, 3, 2, 2, 1, 1},
                    new int[]{1, 1, 2, 2, 3, 4, 4, 3, 2, 2, 1, 1}
                )
            ),
            new ArrayList<>(
                List.of(new int[]{0, 1, 2, 3, 4, 6, 7, 10, 12, 13, 14, 15})
            )
        );

        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.MIDDLE_ASYMMETRY.ordinal());
        assertThrows(
            InconsistencyException.class,
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "MiddleAsymmetry check succeeded when it should have failed"
        );
    }

    @org.junit.jupiter.api.Test
    void testMiddleAsymmetryCheckSuccess2() {
        CompositeStructureInstance instance = new CompositeStructureInstance(
            new int[]{16, 7},
            new int[][]{{0, 1}},
            new int[][]{{4, 4, 4, 4}, {4, 0, 0, 3}}
        );
        CompositeStructureSolution sol = new CompositeStructureSolution(
            instance,
            new ArrayList<>(
                Arrays.asList(
                    new int[]{1, 1, 2, 2, 3, 3, 4, 4, 4, 4, 3, 3, 2, 2, 1, 1},
                    new int[]{1, 1, 4, 4, 4, 1, 1}
                )
            ),
            new ArrayList<>(List.of(new int[]{0, 1, 6, 7, 8, 14, 15}))
        );

        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.MIDDLE_ASYMMETRY.ordinal());
        assertDoesNotThrow(
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "MiddleAsymmetry check failed when it should have succeeded"
        );
    }

    @org.junit.jupiter.api.Test
    void testMiddleAsymmetryCheckFailure2() {
        CompositeStructureInstance instance = new CompositeStructureInstance(
            new int[]{16, 7},
            new int[][]{{0, 1}},
            new int[][]{{4, 4, 4, 4}, {4, 0, 1, 2}}
        );
        CompositeStructureSolution sol = new CompositeStructureSolution(
            instance,
            new ArrayList<>(
                Arrays.asList(
                    new int[]{1, 1, 2, 2, 3, 3, 4, 3, 4, 4, 3, 3, 2, 2, 1, 1},
                    new int[]{1, 1, 3, 4, 4, 1, 1}
                )
            ),
            new ArrayList<>(List.of(new int[]{0, 1, 5, 6, 7, 14, 15}))
        );

        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.MIDDLE_ASYMMETRY.ordinal());
        assertThrows(
            InconsistencyException.class,
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "MiddleAsymmetry check succeeded when it should have failed"
        );
    }

    @org.junit.jupiter.api.Test
    void testMax4DroppedCheckSuccess() {
        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.MAX_4_DROPPED.ordinal());
        assertDoesNotThrow(
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "Max4Dropped check failed when it should have succeeded"
        );
    }

    @org.junit.jupiter.api.Test
    void testMax4DroppedCheckFailure() {
        CompositeStructureInstance instance = new CompositeStructureInstance(
                new int[]{16, 8},
                new int[][]{{0, 1}},
                new int[][]{{4, 4, 4, 4}, {4, 4, 0, 0}}
        );
        CompositeStructureSolution sol = new CompositeStructureSolution(
            instance,
            new ArrayList<>(
                Arrays.asList(
                    new int[]{1, 1, 2, 2, 3, 3, 4, 4, 4, 4, 3, 3, 2, 2, 1, 1},
                    new int[]{1, 1, 2, 2, 4, 3, 3, 2}
                )
            ),
            new ArrayList<>(List.of(new int[]{0, 1, 2, 3, 9, 10, 11, 12}))
        );

        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.MAX_4_DROPPED.ordinal());
        assertThrows(
            InconsistencyException.class,
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "Max4Dropped check succeeded when it should have failed"
        );
    }

    @org.junit.jupiter.api.Test
    void testRootCardinalityCheckSuccess() {
        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.PLY_PER_DIRECTION.ordinal());
        assertDoesNotThrow(
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "RootCardinality check failed when it should have succeeded"
        );
    }

    @org.junit.jupiter.api.Test
    void testRootCardinalityCheckFailure() {
        CompositeStructureSolution sol = new CompositeStructureSolution(
            instance,
            new ArrayList<>(
                Arrays.asList(
                    new int[]{1, 1, 1, 2, 3, 3, 4, 4, 4, 4, 3, 3, 2, 1, 1, 1},
                    new int[]{1, 1, 1, 2, 3, 4, 4, 3, 2, 1, 1, 1}
                )
            ),
            new ArrayList<>(
                List.of(new int[]{0, 1, 2, 3, 4, 6, 7, 10, 12, 13, 14, 15})
            )
        );

        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.PLY_PER_DIRECTION.ordinal());
        assertThrows(
            InconsistencyException.class,
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "RootCardinality check succeeded when it should have failed"
        );
    }

    @org.junit.jupiter.api.Test
    void testUniformDistributionCheck() {
        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.UNIFORM_DISTRIBUTION.ordinal());
        assertDoesNotThrow(
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "UniformDistribution check failed when it should have succeeded"
        );
    }

    @org.junit.jupiter.api.Test
    void testUniformDistributionCheckFailure() {
        // TODO
    }

    @org.junit.jupiter.api.Test
    void testUniformSurfaceCheck() {
        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.UNIFORM_SURFACE.ordinal());
        assertDoesNotThrow(
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "UniformSurface check failed when it should have succeeded"
        );
    }

    @org.junit.jupiter.api.Test
    void testUniformSurfaceCheckFailure() {
        CompositeStructureSolution sol = new CompositeStructureSolution(
            instance,
            new ArrayList<>(
                Arrays.asList(
                    new int[]{1, 1, 1, 2, 3, 3, 4, 4, 4, 4, 3, 3, 2, 1, 1, 1},
                    new int[]{1, 1, 2, 3, 3, 4, 4, 3, 3, 2, 1, 1}
                )
            ),
            new ArrayList<>(
                List.of(new int[]{1, 2, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14})
            )
        );
        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.UNIFORM_SURFACE.ordinal());
        assertThrows(
            InconsistencyException.class,
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "UniformSurface check succeeded when it should have failed"
        );
    }

    @org.junit.jupiter.api.Test
    void testSurface45Check() {
        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.SURFACE_45.ordinal());
        assertDoesNotThrow(
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "Surface45 check failed when it should have succeeded"
        );
    }

    @org.junit.jupiter.api.Test
    void testSurface45CheckFailure() {
        CompositeStructureSolution sol = new CompositeStructureSolution(
            instance,
            new ArrayList<>(
                Arrays.asList(
                    new int[]{2, 1, 1, 2, 3, 3, 4, 4, 4, 4, 3, 3, 2, 1, 1, 2},
                    new int[]{2, 1, 2, 3, 3, 4, 4, 3, 3, 2, 1, 2}
                )
            ),
            new ArrayList<>(
                List.of(new int[]{1, 2, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14})
            )
        );
        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.SURFACE_45.ordinal());
        assertThrows(
            InconsistencyException.class,
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "Surface45 check succeeded when it should have failed"
        );
    }

    @org.junit.jupiter.api.Test
    void testMinCardinalitiesCheck() {
        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.MIN_CARDINALITIES.ordinal());
        assertDoesNotThrow(
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "MinCardinalities check failed when it should have succeeded"
        );
    }

    @org.junit.jupiter.api.Test
    void testMinCardinalitiesCheckFailure() {
        CompositeStructureSolution sol = new CompositeStructureSolution(
            instance,
            new ArrayList<>(
                Arrays.asList(
                    new int[]{1, 1, 1, 2, 3, 3, 2, 2, 4, 4, 3, 3, 2, 1, 1, 1},
                    new int[]{1, 1, 2, 3, 3, 2, 2, 3, 3, 2, 1, 1}
                )
            ),
            new ArrayList<>(
                List.of(new int[]{1, 2, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14})
            )
        );
        BitSet activeConstraints = new BitSet();
        activeConstraints.set(CompositeStructures.activations.MIN_CARDINALITIES.ordinal());
        assertThrows(
            InconsistencyException.class,
            () -> new CompositeStructureChecker(activeConstraints, sol),
            "MinCardinalities check succeeded when it should have failed"
        );
    }
}