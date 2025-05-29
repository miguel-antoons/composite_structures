package org.maxicp.cp.examples.raw.composite;

import org.maxicp.cp.CPFactory;
import org.maxicp.cp.engine.constraints.Absolute;
import org.maxicp.cp.engine.constraints.IsLessOrEqualVar;
import org.maxicp.cp.engine.constraints.TableCT;
import org.maxicp.cp.engine.core.CPBoolVar;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.CPSolver;
import org.maxicp.modeling.algebra.VariableNotFixedException;
import org.maxicp.search.DFSearch;
import org.maxicp.search.SearchStatistics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.maxicp.cp.CPFactory.*;
import static org.maxicp.search.Searches.*;

/**
 * Generate a composite structure instance
 */
public class CompositeStructureGenerator {

    protected CPSolver cp;
    protected int width;
    protected int height;
    protected int maxPlyDifference; // maximum number of plies difference between a child and parent sequence
    protected int minNPlies;
    protected int maxThickNess;
    protected CPIntVar[][][] sequences;
    protected CPIntVar[][] nPlies;
    protected CPBoolVar[][] horizontalEdges;
    protected CPBoolVar[][] verticalEdges;
    protected Random random;
    protected DFSearch search;

    public static int PlyMinus45 = 0;
    public static int Ply0 = 1;
    public static int Ply45 = 2;
    public static int Ply90 = 3;

    /**
     * Generator for composite instances.
     *
     * Creates a graph on a grid, each node in the grid being a stacking sequence.
     * Neighboring nodes are connected by a parent-child relation, indicating which is a subsequence from the other.
     *
     * @param width width of the grid
     * @param height length of the grid
     * @param maxPlyDifference maximum number of plies difference between a child and parent sequence
     * @param minNPlies minimum number of plies on a sequence
     * @param maxThickNess maximum sum of thickness of a sequence
     * @param random used for RNG when generating instances
     */
    public CompositeStructureGenerator(int width, int height, int maxPlyDifference, int minNPlies, int maxThickNess, Random random) {
        this.width = width;
        this.height = height;
        this.maxPlyDifference = maxPlyDifference;
        this.minNPlies = minNPlies;
        this.maxThickNess = maxThickNess;
        this.random = random;
    }

    /**
     * Initialize variables for the problem
     */
    protected void initCPVars() {
        int maxNPlies = maxThickNess - minNPlies * 3; // for one orientation
        cp = CPFactory.makeSolver();
        sequences = new CPIntVar[width][height][4];
        nPlies = new CPIntVar[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                for (int k = 0; k < 4; k++) {
                    sequences[i][j][k] = CPFactory.makeIntVar(cp, minNPlies, maxNPlies);
                }
                nPlies[i][j] = CPFactory.makeIntVar(cp, minNPlies * 4, maxThickNess);
                cp.post(CPFactory.sum(sequences[i][j], nPlies[i][j]));
            }
        }
        // horizontalEdges[i][j] = 1 <-> directed edge (i, j) -> (i+1, j)
        horizontalEdges = new CPBoolVar[width-1][height];
        for (int i = 0; i < horizontalEdges.length; i++) {
            for (int j = 0; j < horizontalEdges[i].length; j++) {
                horizontalEdges[i][j] = CPFactory.makeBoolVar(cp);
            }
        }
        // verticalEdges[i][j] = 1 <-> directed edge (i, j) -> (i, j+1)
        verticalEdges = new CPBoolVar[width][height-1];
        for (int i = 0; i < verticalEdges.length; i++) {
            for (int j = 0; j < verticalEdges[i].length; j++) {
                verticalEdges[i][j] = CPFactory.makeBoolVar(cp);
            }
        }
    }

    /**
     * Add constraints on the variables
     */
    protected void addConstraints() {
        // no orientation can constitute >= 50% of all plies
        for (int i = 0 ; i < sequences.length; i++) {
            for (int j = 0; j < sequences[i].length; j++) {
                CPIntVar maxOrientation = mapTo(nPlies[i][j], v -> v / 2);
                for (int k = 0; k < sequences[i][j].length; k++) {
                    cp.post(CPFactory.le(sequences[i][j][k], maxOrientation));
                }
            }
        }
        // horizontal edges: if there is a link (i, j) -> (i+1, j), then sequences[i][j] is a parent of sequences[i+1][j]
        for (int i = 0; i < horizontalEdges.length; i++) {
            for (int j = 0; j < horizontalEdges[i].length; j++) {
                CPIntVar[] parentSequence = sequences[i][j];
                CPIntVar nPliesParent = nPlies[i][j];
                CPIntVar[] childSequence = sequences[i+1][j];
                CPIntVar nPliesChild = nPlies[i+1][j];
                cp.post(CPFactory.eq(horizontalEdges[i][j], isSubsequence(childSequence, nPliesChild, parentSequence, nPliesParent)));
                // otherwise it's the other way around
                cp.post(CPFactory.eq(not(horizontalEdges[i][j]), isSubsequence(parentSequence, nPliesParent, childSequence, nPliesChild)));
                // plies difference between two neighboring sequences, no matter the direction
                // |nPliesParent - nPliesChild| = [1..maxPlyDifference]
                CPIntVar plyDifference = CPFactory.makeIntVar(cp, 1, maxPlyDifference);
                CPIntVar sumPlyDifference = sum(nPliesChild, minus(nPliesParent));
                cp.post(new Absolute(sumPlyDifference, plyDifference));
            }
        }
        // vertical edges: if there is a link (i, j) -> (i, j+1), then sequences[i][j] is a parent of sequences[i][j+1]
        for (int i = 0; i < verticalEdges.length; i++) {
            for (int j = 0; j < verticalEdges[i].length; j++) {
                CPIntVar[] parentSequence = sequences[i][j];
                CPIntVar nPliesParent = nPlies[i][j];
                CPIntVar[] childSequence = sequences[i][j+1];
                CPIntVar nPliesChild = nPlies[i][j+1];
                cp.post(CPFactory.eq(verticalEdges[i][j], isSubsequence(childSequence, nPliesChild, parentSequence, nPliesParent)));
                // otherwise it's the other way around
                cp.post(CPFactory.eq(not(verticalEdges[i][j]), isSubsequence(parentSequence, nPliesParent, childSequence, nPliesChild)));
                CPIntVar plyDifference = CPFactory.makeIntVar(cp, 1, maxPlyDifference);
                CPIntVar sumPlyDifference = sum(nPliesChild, minus(nPliesParent));
                cp.post(new Absolute(sumPlyDifference, plyDifference));
            }
        }
    }

    /**
     * Creates the search procedure
     */
    protected void makeSearch() {
        // collect all variables
        CPIntVar[] edges1 = Arrays.stream(verticalEdges)
                .flatMap(Arrays::stream)
                .toArray(CPIntVar[]::new);
        CPIntVar[] edges2 = Arrays.stream(horizontalEdges)
                .flatMap(Arrays::stream)
                .toArray(CPIntVar[]::new);
        // Concatenate the two flat arrays:
        CPIntVar[] allEdges = Stream.concat(Arrays.stream(edges1), Arrays.stream(edges2))
                .toArray(CPIntVar[]::new);
        // collect all sequences
        CPIntVar[] allSequences = Arrays.stream(sequences)
                .flatMap(twoD -> Arrays.stream(twoD)         // Stream<CPIntVar[]>
                        .flatMap(Arrays::stream)) // Stream<CPIntVar>
                .toArray(CPIntVar[]::new);
        // branch randomly on the edges first (to tell where are the parents) and on the sequences after
        Supplier<Runnable[]> fixEdges = randomBranching(allEdges, random);
        Supplier<Runnable[]> fixSequences = randomBranching(allSequences, random);
        search = CPFactory.makeDfs(cp, and(fixEdges, fixSequences));
    }

    protected String getInstanceName(int id) {
        return String.format("random_grid_%d_%d_%03d.dzn", width, height, id);
    }

    protected String getDirectory() {
        return String.format("data/composite/asymmetrical/%dx%d/", width, height);
    }

    /**
     * Generates a bunch of instances
     * @param nInstances number of instances to generate
     */
    public void solve(int nInstances) {
        initCPVars();
        addConstraints();
        makeSearch();
        AtomicInteger sol = new AtomicInteger(0);
        search.onSolution(() -> {
            CompositeStructureInstance instance = makeInstance(width, height, sequences, horizontalEdges, verticalEdges);
            String directory = getDirectory();
            createDirectoryIfNotExists(directory);
            String instanceName = getInstanceName(sol.getAndIncrement());
            instance.toDZN(directory + instanceName);
        });
        long initTimeMillis = System.currentTimeMillis();
        boolean failedAtLastIteration = false;
        int maxFailure = 10_000;
        for (int i = 0 ; i < nInstances ; i++) {
            // stop at first solution and creates an instance from it
            // because branching is random, every iteration yields a different instance :-)
            SearchStatistics stats = search.solve(s -> s.numberOfSolutions() >= 1 || s.numberOfFailures() >= maxFailure);
            if (stats.numberOfSolutions() < 1) {
                System.out.println("failed to generate a solution within "  + maxFailure + " failures, trying again...");
                failedAtLastIteration = true;
                i -= 1;
            } else {
                if (failedAtLastIteration) {
                    System.out.println("finally succeeded :-)");
                }
                failedAtLastIteration = false;
            }
            //System.out.println(stats);
        }
        long currentTimeMillis = System.currentTimeMillis();
        double elapsed = ((double) (currentTimeMillis - initTimeMillis)) / 1000.0;
        System.out.printf("instances generated in %.3f[s]%n", elapsed);
    }

    /**
     * Creates a directory (and any nonexistent parent directories)
     * if it does not already exist.
     *
     * @param directoryPath the path of the directory to create
     */
    public static void createDirectoryIfNotExists(String directoryPath) {
        Path path = Paths.get(directoryPath);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                //System.out.println("Directory created at: " + path.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("Failed to create directory: " + e.getMessage());
            }
        }
    }

    private static CompositeStructureInstance makeInstance(int width, int height, CPIntVar[][][] sequences, CPBoolVar[][] horizontalEdges, CPBoolVar[][] verticalEdges) {
        int nSequences = Arrays.stream(sequences).mapToInt(i -> i.length).sum();
        List<int[]> edges = new ArrayList<>();
        int[][] pliesPerDirection = new int[nSequences][4];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int[] p = new int[4];
                for (int k = 0; k < 4; k++) {
                    try {
                        p[k] = sequences[i][j][k].evaluate();
                    } catch (VariableNotFixedException e) {
                        throw new RuntimeException(e);
                    }
                }
                int coordinate = flatten(i, j, width);
                pliesPerDirection[coordinate] = p;
            }
        }
        for (int i = 0; i < horizontalEdges.length; i++) {
            for (int j = 0; j < horizontalEdges[i].length; j++) {
                int origin = flatten(i, j, width);
                int destination = flatten(i+1, j, width);
                try {
                    boolean isParent = horizontalEdges[i][j].evaluateBool();
                    if (isParent) {
                        edges.add(new int[]{origin, destination});
                    } else { // other way around
                        edges.add(new int[]{destination, origin});
                    }
                } catch (VariableNotFixedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        for (int i = 0; i < verticalEdges.length; i++) {
            for (int j = 0; j < verticalEdges[i].length; j++) {
                int origin = flatten(i, j, width);
                int destination = flatten(i, j+1, width);
                try {
                    boolean isParent = verticalEdges[i][j].evaluateBool();
                    if (isParent) {
                        edges.add(new int[]{origin, destination});
                    } else { // other way around
                        edges.add(new int[]{destination, origin});
                    }
                } catch (VariableNotFixedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return new CompositeStructureInstance(edges, pliesPerDirection);
    }

    public static Supplier<Runnable[]> randomBranching(CPIntVar[] variables, Random random) {
        int[] domain = new int[Arrays.stream(variables).mapToInt(CPIntVar::size).max().getAsInt()];
        CPSolver cp = variables[0].getSolver();
        return () -> {
            CPIntVar nPly = selectMin(variables, xi -> !xi.isFixed(), xi -> random.nextDouble());
            if (nPly == null) {
                return EMPTY;
            } else {
                int size = nPly.fillArray(domain);
                int v = domain[random.nextInt(size)];
                return branch(() -> cp.post(eq(nPly, v)), () -> cp.post(neq(nPly, v)));
            }
        };
    }

    public static Supplier<Runnable[]> minDomVarAndRandomValue(CPIntVar[] variables, Random random) {
        int[] domain = new int[Arrays.stream(variables).mapToInt(CPIntVar::size).max().getAsInt()];
        CPSolver cp = variables[0].getSolver();
        return () -> {
            CPIntVar nPly = selectMin(variables, xi -> !xi.isFixed(), xi -> xi.size());
            if (nPly == null) {
                return EMPTY;
            } else {
                int size = nPly.fillArray(domain);
                int v = domain[random.nextInt(size)];
                return branch(() -> cp.post(eq(nPly, v)), () -> cp.post(neq(nPly, v)));
            }
        };
    }

    /**
     * Transforms a 2-d coordinate into a 1-d coordinate
     * @param i x coordinate
     * @param j y coordinate
     * @param x maximum value for the x coordinate
     * @return 1-d coordinate
     */
    public static int flatten(int i, int j, int x) {
        return i + j * x;
    }

    /**
     * Apply a mapping onto a variable to get another variable, through a Table constraint
     * @param var variable to map
     * @param map function for mapping integer of variable into new integer values
     * @return mapped variable
     */
    public static CPIntVar mapTo(CPIntVar var, Function<Integer, Integer> map) {
        CPSolver cp = var.getSolver();
        // bounds of the newly created variable
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int size = var.size();
        int[] domain = new int[size];
        var.fillArray(domain);
        // create a table containing all pairs of (value, mapped_value)
        int[][] allowedPairs = new int[size][2];
        for (int i = 0; i < size; i++) {
            int v = domain[i];
            int mappedValue = map.apply(v);
            allowedPairs[i][0] = v;
            allowedPairs[i][1] = mappedValue;
            // track the min and max
            min = Math.min(min, mappedValue);
            max = Math.max(max, mappedValue);
        }
        // create the variable and map it with a table constraint
        CPIntVar mapped = CPFactory.makeIntVar(cp, min, max);
        cp.post(new TableCT(new CPIntVar[]{var, mapped}, allowedPairs));
        return mapped;
    }

    /**
     * Gives a boolean variable telling if a sequence is the child of another one
     * @param childSubsequence candidate child subsequence
     * @param nPliesChild total number of plies on the child
     * @param parentSubsequence candidate parent subsequence
     * @param nPliesParent total number of plies on the parent
     * @return boolean telling if a sequence is the child of a parent subsequence
     */
    public static CPBoolVar isSubsequence(CPIntVar[] childSubsequence, CPIntVar nPliesChild, CPIntVar[] parentSubsequence, CPIntVar nPliesParent) {
        CPSolver cp = childSubsequence[0].getSolver();
        CPIntVar[] pliesLessOrEqual = new CPIntVar[4];
        for (int i = 0; i < pliesLessOrEqual.length; i++)
            pliesLessOrEqual[i] = CPFactory.makeBoolVar(cp);
        for (int i = 0 ; i < 4; i++)
            cp.post(new IsLessOrEqualVar((CPBoolVar) pliesLessOrEqual[i], childSubsequence[i], parentSubsequence[i]));
        // number of plies orientation that are <= than parent
        CPIntVar nLessOrEqual = CPFactory.sum(pliesLessOrEqual);
        // true if all ply orientations are <= than parent
        CPBoolVar allNLessOrEqual = isEq(nLessOrEqual, pliesLessOrEqual.length);
        // child has strictly less plies
        CPBoolVar childLessPlies = CPFactory.isLt(nPliesChild, nPliesParent);
        cp.post(eq(allNLessOrEqual, childLessPlies));
        return childLessPlies;
    }

    /**
     * Create a grid of n * m nodes, with links between neighboring nodes
     * @param args
     */
    public static void main(String[] args) {
        int nInstances = 20;
        int maxThickNess = 60; // max thickness (i.e. sum of all plies)
        int minNPlies = 2; // for one orientation
        int maxPlyDifference = 10; // number of ply difference between a parent sequence and its child
        // dimensions of the grid
        int[] widths = {2, 3, 4, 5, 6};
        int[] heights = {2, 3, 4, 5, 6};
        for (int i = 0 ; i < widths.length; i++) {
            int width = widths[i];
            int height = heights[i];
            Random random = new Random(i);
            CompositeStructureGenerator generator = new CompositeStructureGenerator(width, height, maxPlyDifference, minNPlies, maxThickNess, random);
            generator.solve(nInstances);
        }
    }

}
