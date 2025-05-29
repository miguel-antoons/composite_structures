package org.maxicp.cp.examples.raw.composite;

import org.maxicp.cp.CPFactory;
import org.maxicp.cp.engine.constraints.TableCT;
import org.maxicp.cp.engine.core.CPIntVar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.maxicp.search.Searches.*;

public class CompositeStructureGeneratorSymmetrical extends CompositeStructureGenerator {

    protected boolean evenNumberPliesEverywhere = false;

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
     * @param maxThickNess              maximum sum of thickness of a sequence
     * @param random                    used for RNG when generating instances
     */
    public CompositeStructureGeneratorSymmetrical(boolean evenNumberPliesEverywhere, int width, int height,
                                                  int maxPlyDifference, int minNPlies, int maxThickNess, Random random) {
        super(width, height, maxPlyDifference, minNPlies, maxThickNess, random);
        this.evenNumberPliesEverywhere = evenNumberPliesEverywhere;
    }

    @Override
    protected String getInstanceName(int id) {
        return String.format("random_grid_symmetrical_%d_%d_%03d.dzn", width, height, id);
    }

    protected String getDirectory() {
        return String.format("data/composite/symmetrical/%dx%d/", width, height);
    }

    @Override
    protected void addConstraints() {
        super.addConstraints();
        // constraint the number of plies per orientation
        // there can be at most one odd cardinality of plies orientation, otherwise symmetry cannot be achieved
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // between 0 and 1 number of odd plies on the full sequence
                // 0 odd plies if specified in the class
                CPIntVar nOddSymmetries = CPFactory.makeIntVar(cp, 0, evenNumberPliesEverywhere ? 0 : 1);
                // domain: 1 if odd number of plies, 0 if even number
                CPIntVar[] nOddOnSequence = new CPIntVar[4];
                for (int orientation = 0; orientation < 4; orientation++) {
                    CPIntVar pliesAtOrientation = sequences[i][j][orientation];
                    CPIntVar isOdd = mapTo(pliesAtOrientation, v -> (v % 2 == 0) ? 0 : 1);
                    nOddOnSequence[orientation] = isOdd;
                }
                cp.post(CPFactory.sum(nOddOnSequence, nOddSymmetries));
            }
        }
        // redundant constraint for the sum, enforcing domain consistency through a table constraint
        // this is only suitable because there is a relatively reasonable number of tuples such that their sum is <= maxThickness
        List<int[]> sumTuplesList = new ArrayList<>();
        for (int i = 0 ; i < maxThickNess ; i++) {
            for (int j = 0 ; i + j < maxThickNess ; j++) {
                for (int k = 0 ; i + j + k < maxThickNess ; k++) {
                    for (int l = 0 ; l + i + j + k < maxThickNess ; l++) {
                        int sum = i + j + k + l;
                        sumTuplesList.add(new int[]{i, j, k, l, sum});
                    }
                }
            }
        }
        int[][] sumTuples = new int[sumTuplesList.size()][];
        for (int i = 0; i < sumTuplesList.size(); i++) {
            sumTuples[i] = sumTuplesList.get(i);
        }
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                cp.post(new TableCT(new CPIntVar[] {sequences[i][j][0], sequences[i][j][1], sequences[i][j][2], sequences[i][j][3], nPlies[i][j]}, sumTuples));
            }
        }
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
        int nInstances = 20;
        int maxThickNess = 60; // max thickness (i.e. sum of all plies)
        int minNPlies = 2; // for one orientation
        int maxPlyDifference = 10; // max number of ply difference between a parent sequence and its child
        // dimensions of the grid
        int[] widths = {2, 3, 4, 5, 6};
        int[] heights = {2, 3, 4, 5, 6};
        //int[] widths = {6};
        //int[] heights = {6};
        for (int i = 0; i < widths.length; i++) {
            int width = widths[i];
            int height = heights[i];
            Random random = new Random(i);
            CompositeStructureGenerator generator = new CompositeStructureGeneratorSymmetrical(true, width, height, maxPlyDifference, minNPlies, maxThickNess, random);
            generator.solve(nInstances);
        }
    }
}
