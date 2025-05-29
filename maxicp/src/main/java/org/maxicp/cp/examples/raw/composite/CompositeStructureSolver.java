package org.maxicp.cp.examples.raw.composite;

import org.maxicp.search.SearchStatistics;

import java.io.File;
import java.util.BitSet;

/**
 * Main launcher class for solving an instance
 */
public class CompositeStructureSolver {

    String instanceName; // basename of the instance (i.e. not full path)
    CompositeStructureInstance instance; // instance to solve
    int timeoutSeconds; // timeout allowed
    long seed; // seed available for RNG
    String modelName; // name of the model
    String[] args; // exact command line arguments that were used when launching the main

    public CompositeStructureSolver(String[] args) {
        this.modelName = args[0].replace(".mzn", "").toLowerCase();
        String instancePath = args[1];
        timeoutSeconds = Integer.parseInt(args[2]);
        seed = Long.parseLong(args[3]);
        instance = new CompositeStructureInstance(instancePath);
        this.instanceName = new File(instancePath).getName();
        this.args = args;
    }

    /**
     * Gives the bitset corresponding to the constraints to activate, depending on the model name
     * @param modelName model to use
     * @return bitset where 1 indicate that the corresponding constraint must be set
     */
    public BitSet getBitSetForModel(String modelName) {
        BitSet constraintsBitset = new BitSet(32);
        // set first 7 constraints to be active
        constraintsBitset.set(CompositeStructures.activations.BLENDING.ordinal());
        constraintsBitset.set(CompositeStructures.activations.NO_PLY_CROSSING.ordinal());
        constraintsBitset.set(CompositeStructures.activations.MAX_4_DROPPED.ordinal());
        constraintsBitset.set(CompositeStructures.activations.PLY_PER_DIRECTION.ordinal());
        constraintsBitset.set(CompositeStructures.activations.UNIFORM_SURFACE.ordinal());
//        constraintsBitset.set(CompositeStructures.activations.NO_90_GAP_OLD.ordinal());
//        constraintsBitset.set(CompositeStructures.activations.NO_4_CONSECUTIVE_OLD.ordinal());
        constraintsBitset.set(CompositeStructures.activations.NO_90_GAP.ordinal());
        constraintsBitset.set(CompositeStructures.activations.NO_4_CONSECUTIVE.ordinal());
        constraintsBitset.set(CompositeStructures.activations.SYMMETRY.ordinal());
        constraintsBitset.set(CompositeStructures.activations.MIDDLE_ASYMMETRY.ordinal());
        constraintsBitset.set(CompositeStructures.activations.SURFACE_45.ordinal());
        // optional constraints depending on the model type
        switch (modelName) {
            case "firstfail"        -> constraintsBitset.set(CompositeStructures.activations.FIRST_FAIL_SEARCH.ordinal());
            case "lastconflict"     -> constraintsBitset.set(CompositeStructures.activations.LAST_CONFLICT_SEARCH.ordinal());
            case "conflictordering" -> constraintsBitset.set(CompositeStructures.activations.CONFLICT_ORDERING_SEARCH.ordinal());
            case "highdegree"       -> constraintsBitset.set(CompositeStructures.activations.HIGH_DEGREE_SEARCH.ordinal());
            case "thinfirst"        -> constraintsBitset.set(CompositeStructures.activations.THIN_SEARCH.ordinal());
            case "thickfirst"       -> constraintsBitset.set(CompositeStructures.activations.THICK_SEARCH.ordinal());
            case "highthinfirst"    -> constraintsBitset.set(CompositeStructures.activations.HIGH_THIN_SEARCH.ordinal());
            case "highthickfirst"   -> constraintsBitset.set(CompositeStructures.activations.HIGH_THICK_SEARCH.ordinal());
        }
        return constraintsBitset;
    }

    public void solve() {
        BitSet constraintsForModel = getBitSetForModel(modelName);
        CompositeStructures cs = new CompositeStructures(instance, constraintsForModel);
        CompositeStructures.CSStats stats = cs.findAllSolutions(
            1,
            timeoutSeconds * 1000_000_000L,
            false,
            false
        );
        // print the solving information
        System.out.printf("maxicp,%s,%s,%.3f,%d,%s,%.3f,%d,%d,%s,%s%n",
            modelName,
            instanceName,
            (double) timeoutSeconds,
            seed,
            statusString(stats.searchStatistics),
            nanoSecondsToSeconds(stats.runTime),
            stats.searchStatistics.numberOfNodes(),
            stats.searchStatistics.numberOfFailures(),
            stats.solution == null ? "" : stats.solution.minizincOneLiner(),
            oneLinerArguments());
    }

    private double nanoSecondsToSeconds(long nanoSeconds) {
        return nanoSeconds / 1_000_000_000.0;
    }

    private String statusString(SearchStatistics stats) {
        if (stats.numberOfSolutions() >= 1)
            return "SATISFIABLE";
        if (stats.isCompleted())
            return "UNSATISFIABLE";
        return "UNKNOWN";
    }

    private String oneLinerArguments() {
        return String.join(" ", args);
    }

    /**
     * Usage:
     * <p>
     * model instance_file timeoutInSeconds seed
     * <p>
     * prints in one line the solving information
     */
    public static void main(String[] args) {
        new CompositeStructureSolver(args).solve();
    }

}
