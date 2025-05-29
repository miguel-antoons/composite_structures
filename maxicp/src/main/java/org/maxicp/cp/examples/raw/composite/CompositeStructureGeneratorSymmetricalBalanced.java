package org.maxicp.cp.examples.raw.composite;

import org.maxicp.cp.CPFactory;
import org.maxicp.cp.engine.constraints.Maximum;
import org.maxicp.cp.engine.constraints.TableCT;
import org.maxicp.cp.engine.core.CPIntVar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class CompositeStructureGeneratorSymmetricalBalanced extends CompositeStructureGeneratorSymmetrical {

    protected int maxThicknessLB;
    protected int maxThicknessUB;

    /**
     * Generator for composite instances.
     * <p>
     * Creates a graph on a grid, each node in the grid being a stacking sequence.
     * Neighboring nodes are connected by a parent-child relation, indicating which is a subsequence from the other.
     * Enforces at most 1 number of odd plies for each sequence
     *
     * @param evenNumberPliesEverywhere if true, enforces that the number of plies at every orientation must always be symmetrical
     * @param width                     width of the grid
     * @param height                    length of the grid
     * @param maxPlyDifference          maximum number of plies difference between a child and parent sequence
     * @param minNPlies                 minimum number of plies on a sequence
     * @param maxThickNessUB              maximum sum of thickness of a sequence
     * @param random                    used for RNG when generating instances
     */
    public CompositeStructureGeneratorSymmetricalBalanced(boolean evenNumberPliesEverywhere, int width, int height,
                                                          int maxPlyDifference, int minNPlies, int maxThickNessLB, int maxThickNessUB, Random random) {
        super(evenNumberPliesEverywhere, width, height, maxPlyDifference, minNPlies, maxThickNessUB, random);
        this.maxThicknessLB = maxThickNessLB;
        this.maxThicknessUB = maxThickNessUB;
    }

    @Override
    protected String getInstanceName(int id) {
        return String.format("random_grid_symmetrical_balanced_%d_%d_%03d.dzn", width, height, id);
    }

    protected String getDirectory() {
        return String.format("data/composite/symmetrical_balanced/%dx%d/", width, height);
    }

    @Override
    protected void addConstraints() {
        super.addConstraints();
        // same number of plies at -45 and 45 degrees
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                cp.post(CPFactory.eq(sequences[i][j][PlyMinus45], sequences[i][j][Ply45]));
            }
        }
        // must have at least 1 ply at 45 degree for the surface ply
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                cp.post(CPFactory.ge(sequences[i][j][Ply45], 1));
            }
        }
        // maximum thickness must be between a defined LB and UB
        CPIntVar maxThickness = CPFactory.makeIntVar(cp, maxThicknessLB, maxThicknessUB);
        CPIntVar[] allThickness = Arrays.stream(nPlies)
                .flatMap(Arrays::stream)
                .toArray(CPIntVar[]::new);
        cp.post(new Maximum(allThickness, maxThickness));
    }

    @Override
    protected void makeSearch() {
        // collect all number of plies per sequence
        CPIntVar[] allNPlies = Arrays.stream(nPlies)
                .flatMap(Arrays::stream)
                .toArray(CPIntVar[]::new);
        // collect all sequences
        CPIntVar[] allSequences = Arrays.stream(sequences)
                .flatMap(twoD -> Arrays.stream(twoD)         // Stream<CPIntVar[]>
                        .flatMap(Arrays::stream)) // Stream<CPIntVar>
                .toArray(CPIntVar[]::new);
        // collect everything
        CPIntVar[] allVariables = Stream.concat(Arrays.stream(allSequences), Arrays.stream(allNPlies))
                .toArray(CPIntVar[]::new);
        // branch randomly on the total number of plies and on the content of the sequences
        Supplier<Runnable[]> fixVariables = randomBranching(allVariables, random);
        search = CPFactory.makeDfs(cp, fixVariables);
    }

    /**
     * Create a grid of n * m nodes, with links between neighboring nodes
     *
     * @param args
     */
    public static void main(String[] args) {
        int nInstances = 300;
        int maxThickNessUB = 60; // upper bound on the max thickness (i.e. sum of all plies)
        int maxThickNessLB = 50; // lower bound on the max thickness (i.e. sum of all plies)
        int minNPlies = 2; // for one orientation
        int maxPlyDifference = 6; // max number of ply difference between a parent sequence and its child
        // dimensions of the grid
        int[] widths = {3, 4, 5, 6};
        int[] heights = {3, 4, 5, 6};
        for (int i = 0; i < widths.length; i++) {
            int width = widths[i];
            int height = heights[i];
            Random random = new Random(i);
            CompositeStructureGenerator generator = new CompositeStructureGeneratorSymmetricalBalanced(true, width, height, maxPlyDifference, minNPlies, maxThickNessLB, maxThickNessUB, random);
            generator.solve(nInstances);
        }
    }
}
