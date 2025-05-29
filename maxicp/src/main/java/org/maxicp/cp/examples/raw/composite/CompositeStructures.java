package org.maxicp.cp.examples.raw.composite;

import static org.maxicp.cp.CPFactory.*;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.BitSet;

import org.maxicp.cp.engine.constraints.*;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.CPSolver;
import org.maxicp.search.DFSearch;
import org.maxicp.search.SearchStatistics;

public class CompositeStructures {

    // * NOTE: in the following code, the following variable value corresponds
    // * to the following:
    // *    - 1: -45
    // *    - 2: 0
    // *    - 3: 45
    // *    - 4: 90
    protected final CompositeStructureInstance instance;
    protected CPSolver cp;
    private DFSearch dfs;
    protected CPIntVar[][] sequences;
    private CPIntVar[][] indexes;
    private final BitSet activeConstraints;
    public final boolean MERGED_CARDINALITIES = true;
    public static final int[][] allowedMiddleEven = new int[][] {
        { 1, 1 }, // -45, -45
        { 2, 2 }, // 0, 0
        { 3, 3 }, // 45, 45
        { 4, 4 }, // 90, 90
        { 1, 3 }, // -45, 45
        { 3, 1 }, // 45, -45
    };
    public static final int[][] allowedMiddleOdd = new int[][] {
        { 1, 2, 1 }, // -45, 0, -45
        { 1, 4, 1 }, // -45, 90, -45
        { 2, 2, 2 }, // 0, 0, 0
        { 2, 4, 2 }, // 0, 90, 0
        { 3, 2, 3 }, // 45, 0, 45
        { 3, 4, 3 }, // 45, 90, 45
        { 4, 2, 4 }, // 90, 0, 90
        { 4, 4, 4 }, // 90, 90, 90
        { 1, 2, 3 }, // -45, 0, 45
        { 1, 4, 3 }, // -45, 90, 45
        { 3, 2, 1 }, // 45, 0, -45
        { 3, 4, 1 }, // 45, 90, -45
    };
    public static final int[] angles = new int[] { -1, -45, 0, 45, 90 };
    public static final double minPlyPercentage = 0.08;
    ArrayList<CompositeStructureSolution> solutions;
    CompositeStructureSolution bestSolution;

    enum activations {
        BLENDING,
        NO_PLY_CROSSING,
        NO_90_GAP,
        NO_4_CONSECUTIVE,
        SYMMETRY,
        MIDDLE_ASYMMETRY,
        MAX_4_DROPPED,
        PLY_PER_DIRECTION,
        UNIFORM_DISTRIBUTION,
        NO_90_GAP_OLD,
        NO_4_CONSECUTIVE_OLD,
        UNIFORM_SURFACE,
        SURFACE_45,
        MIN_CARDINALITIES,
        FIRST_FAIL_SEARCH,
        LAST_CONFLICT_SEARCH,
        CONFLICT_ORDERING_SEARCH,
        HIGH_DEGREE_SEARCH,
        THIN_SEARCH,
        THICK_SEARCH,
        HIGH_THIN_SEARCH,
        HIGH_THICK_SEARCH,
    }

    public CompositeStructures(CompositeStructureInstance instance, BitSet activeConstraints) {
        this.instance = instance;
        this.activeConstraints = activeConstraints;
    }

    public BitSet activeConstraints() {
        return (BitSet) activeConstraints.clone();
    }

    public boolean MERGED_CARDINALITIES() {
        return MERGED_CARDINALITIES;
    }

    private void variables() {
        // sequences[i][j] is the direction of the j-th ply of the i-th stacking sequence
        sequences = new CPIntVar[instance.noNodes][];
        for (int i = 0; i < instance.noNodes; i++) {
            sequences[i] = makeIntVarArray(
                this.cp,
                instance.nodeThickness[i],
                1,
                4
            );
        }

        // indexes[edgeNumber][i] is the index of a ply of the parent sequence that is the same as ply i of the child
        // sequence. There is one array of indexes for each edge present. An edge represents a parent-child relationship
        // between two stacking sequences.
        indexes = new CPIntVar[instance.noEdges][];
        for (int i = 0; i < instance.noEdges; i++) {
            indexes[i] = makeIntVarArray(
                this.cp,
                instance.nodeThickness[instance.edges[i].child],
                0,
                instance.nodeThickness[instance.edges[i].parent] - 1
            );
        }
    }

    private void blending() {
        // blending rule: every child sequence must be a subset of the parent sequence.
        // For every parent node A and child node B: bi = A[indexi]
        for (int i = 0; i < instance.noEdges; i++) {
            for (
                int j = 0;
                j < instance.nodeThickness[instance.edges[i].child];
                j++
            ) {
                cp.post(
                    new Element1DVar(
                        sequences[instance.edges[i].parent],
                        indexes[i][j],
                        sequences[instance.edges[i].child][j]
                    )
                );
            }
        }
    }

    private void noPlyCrossing() {
        // no ply-crossing: indexi < indexi+1
        for (int i = 0; i < instance.noEdges; i++) {
            for (int j = 0; j < indexes[i].length - 1; j++) {
                cp.post(lt(indexes[i][j], indexes[i][j + 1]));
            }
        }
    }

    private void no90GapNo4ConsecutiveTable(
        boolean no90Gap,
        boolean no4Consecutive,
        boolean middleAsymmetry
    ) {
        // Prevents a 90 degree gap between two consecutive plies and prevents 5 consecutive
        // plies of the same direction for every stacking sequence.
        // It does so by applying a table constraint to every 5 consecutive plies of the same
        // stacking sequence.
        int[][] table1 = generateTable(no90Gap, no4Consecutive);
        int[][] table2 = middleAsymmetry ? generateTable(false, no4Consecutive) : table1;
        for (int i = 0; i < instance.noNodes; i++) {
            int low     = Math.floorDiv(instance.nodeThickness[i], 2) - 3;
            int high    = Math.ceilDiv(instance.nodeThickness[i], 2) - 1;
            for (int j = 0; j < instance.nodeThickness[i] - 3; j++) {
                CPIntVar[] slice = new CPIntVar[] {
                    sequences[i][j],
                    sequences[i][j + 1],
                    sequences[i][j + 2],
                    sequences[i][j + 3],
                };
                if (j < low || j > high)    cp.post(table(slice, table1));
                else                        cp.post(table(slice, table2));
            }
        }
    }

    private void uniformDistribution() {
        // TODO
    }

    private void no90GapOld() {
        // no 90 gap: |A[i] - A[i+1]| != 2
        for (int i = 0; i < instance.noNodes; i++) {
            for (int j = 0; j < instance.nodeThickness[i] - 1; j++) {
                cp.post(
                    neq(
                        abs(sum(sequences[i][j], minus(sequences[i][j + 1]))),
                        2
                    )
                );
            }
        }
    }

    private void no4ConsecutiveOld() {
        // constraint max 4 consecutive angles with same value -->
        // seq[i, j] != seq[i, j+1] or seq[i, j] != seq[i, j+2] or seq[i, j] != seq[i, j+3] or seq[i, j] != seq[i, j+4]
        for (int i = 0; i < instance.noNodes; i++) {
            for (int j = 0; j < instance.nodeThickness[i] - 3; j++) {
                cp.post(
                    or(
                        not(isEq(sequences[i][j], sequences[i][j + 1])),
                        not(isEq(sequences[i][j], sequences[i][j + 2])),
                        not(isEq(sequences[i][j], sequences[i][j + 3]))
                    )
                );
            }
        }
    }

    private void symmetry(boolean middleAsymmetry) {
        // symmetric sequences: for every node A with thickness t: ai = at-i
        // (except for the middle part, see asymmetry constraint)
        for (int i = 0; i < instance.noNodes; i++) {
            int limit = middleAsymmetry
                ? Math.floorDiv(instance.nodeThickness[i], 2) - 1
                : Math.floorDiv(instance.nodeThickness[i], 2);
            for (int j = 0; j < limit; j++) {
                cp.post(
                    eq(
                        sequences[i][j],
                        sequences[i][instance.nodeThickness[i] - j - 1]
                    )
                );
            }
        }
    }

    private void middleAsymmetry() {
        // Allow asymmetry in the middle (cf. paper & constraints).
        // This constraint is enforced by only allowing a certain set
        // of sequences in the 3 or 2 middle plies for odd or even stacking
        // sequences respectively.
        // The allowed sequences in the middle are specified by the
        // allowedMiddleEven and allowedMiddleOdd arrays.
        for (int i = 0; i < instance.noNodes; i++) {
            if (instance.nodeThickness[i] % 2 == 0) {
                CPIntVar[] slice = new CPIntVar[] {
                    sequences[i][Math.floorDiv(instance.nodeThickness[i], 2) - 1],
                    sequences[i][Math.floorDiv(instance.nodeThickness[i], 2)],
                };
                cp.post(table(slice, allowedMiddleEven));
            } else {
                CPIntVar[] slice = new CPIntVar[] {
                    sequences[i][Math.floorDiv(instance.nodeThickness[i], 2) - 1],
                    sequences[i][Math.floorDiv(instance.nodeThickness[i], 2)],
                    sequences[i][Math.floorDiv(instance.nodeThickness[i], 2) + 1],
                };
                cp.post(table(slice, allowedMiddleOdd));
            }
        }
    }

    private void max3Dropped() {
        // A maximum of 4 plies can be dropped in between sequences.
        // indexi+1 - indexi <= 4
        for (int i = 0; i < instance.noEdges; i++) {
            indexes[i][0].removeAbove(3);
            indexes[i][indexes[i].length - 1].removeBelow(instance.nodeThickness[instance.edges[i].parent] - 4);
            for (int j = 0; j < indexes[i].length - 1; j++) {
                cp.post(le(sum(indexes[i][j + 1], minus(indexes[i][j])), 4));
            }
        }
    }

    private void plyPerDirectionMax() {
        // enforce a certain number of plies for each direction for the given roots
        for (int i = 0; i < instance.noNodes; i++) {
            int[] cardinalities = instance.pliesPerDirection[i].toArrayMerged();
            cardinalities[1] = Math.ceilDiv(cardinalities[1], 2);
            cardinalities[3] = Math.ceilDiv(cardinalities[3], 2);
            cp.post(
                new CardinalityMaxFWC(
                    sequences[i],
                    cardinalities
                )
            );
        }
    }

    private void plyPerDirectionMin() {
        // enforce a certain number of plies for each direction for the given roots
        for (int i = 0; i < instance.noNodes; i++) {
            int[] cardinalities = instance.pliesPerDirection[i].toArrayMerged();
            cardinalities[1] = Math.floorDiv(cardinalities[1], 2);
            cardinalities[3] = Math.floorDiv(cardinalities[1], 2);
            cp.post(
                new CardinalityMinFWC(
                    sequences[i],
                    cardinalities
                )
            );
        }
    }

    // TODO use a domain consistent cardinality constraint
    // follows the principle that only 3 cardinalities are given: one for 0° plies, one for +/-45° plies and one for 90° plies
    protected void plyPerDirection() {
        plyPerDirectionMax();
        plyPerDirectionMin();
    }

    private void uniformSurface() {
        for (int i = 0; i < instance.noEdges; i++) {
            indexes[i][0].fix(0);
            indexes[i][indexes[i].length - 1].fix(
                instance.nodeThickness[instance.edges[i].parent] - 1
            );
        }
    }

    private void surface45() {
        for (CPIntVar[] seq : sequences) {
            seq[0].remove(4);
            seq[0].remove(2);
            seq[seq.length - 1].remove(4);
            seq[seq.length - 1].remove(2);
        }
    }

    private void minCardinalities() {
        for (int i = 0; i < instance.noNodes; i++) {
            // counting 8% of the plies
            int min = (int) Math.ceil(minPlyPercentage * instance.nodeThickness[i]);
            cp.post(
                new CardinalityMinFWC(
                    sequences[i],
                    new int[] { 0, min, min, min, min }
                )
            );
        }
    }

//    private void atMostSeqCardSim() {
//        final int[][] table14 = readDZNArray(
//            "atMostSeqCard14.txt",
//            s -> {
//                s = s.replace("|", "");
//                String[] plies = s.split(",");
//                return new int[] {
//                    Integer.parseInt(plies[0]),
//                    Integer.parseInt(plies[1]),
//                    Integer.parseInt(plies[2]),
//                    Integer.parseInt(plies[3]),
//                    Integer.parseInt(plies[4]),
//                    Integer.parseInt(plies[5]),
//                    Integer.parseInt(plies[6]),
//                    Integer.parseInt(plies[7]),
//                    Integer.parseInt(plies[8]),
//                    Integer.parseInt(plies[9]),
//                    Integer.parseInt(plies[10]),
//                    Integer.parseInt(plies[11]),
//                    Integer.parseInt(plies[12]),
//                    Integer.parseInt(plies[13]),
//                };
//            },
//            "\\|\\d+,\\d+,\\d+,\\d+,\\d+,\\d+,\\d+,\\d+,\\d+,\\d+,\\d+,\\d+,\\d+,\\d+"
//        );
//        final int[][] table12 = readDZNArray(
//            "atMostSeqCard12_2.txt",
//            s -> {
//                s = s.replace("|", "");
//                String[] plies = s.split(",");
//                return new int[] {
//                    Integer.parseInt(plies[0]),
//                    Integer.parseInt(plies[1]),
//                    Integer.parseInt(plies[2]),
//                    Integer.parseInt(plies[3]),
//                    Integer.parseInt(plies[4]),
//                    Integer.parseInt(plies[5]),
//                    Integer.parseInt(plies[6]),
//                    Integer.parseInt(plies[7]),
//                    Integer.parseInt(plies[8]),
//                    Integer.parseInt(plies[9]),
//                    Integer.parseInt(plies[10]),
//                    Integer.parseInt(plies[11]),
//                };
//            },
//            "\\|\\d+,\\d+,\\d+,\\d+,\\d+,\\d+,\\d+,\\d+,\\d+,\\d+,\\d+,\\d+");
//        for (int i = 0; i < instance.noNodes; i++) {
//            if (instance.nodeThickness[i] == 14)
//                cp.post(
//                    new TableCT(
//                        sequences[i],
//                        table14
//                    )
//                );
//            else if (instance.nodeThickness[i] == 12)
//                cp.post(
//                    new TableCT(
//                        sequences[i],
//                        table12
//                    )
//                );
//        }
//    }
//
//    private int[][] readDZNArray(String fileName, Function<String, int[]> fun, String regex) {
//        File file = new File(fileName);
//        Scanner scanner = null;
//        try {
//            scanner = new Scanner(file);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        String stringLine = scanner.nextLine().replaceAll("\\s+", "");
//        Pattern pattern = Pattern.compile(regex);
//        Matcher matcher = pattern.matcher(stringLine);
//
//        List<int[]> array = new ArrayList<>();
//        while (matcher.find()) {
//            String value = matcher.group();
//            array.add(fun.apply(value));
//        }
//
//        int[][] result = new int[array.size()][];
//        for (int i = 0; i < array.size(); i++) {
//            result[i] = array.get(i);
//        }
//        scanner.close();
//        return result;
//    }

    public void model() {
        cp = makeSolver();
        variables();
        if (activeConstraints.get(activations.BLENDING.ordinal()))                  blending();
        if (activeConstraints.get(activations.NO_PLY_CROSSING.ordinal()))           noPlyCrossing();
        no90GapNo4ConsecutiveTable(
            activeConstraints.get(activations.NO_90_GAP.ordinal()),
            activeConstraints.get(activations.NO_4_CONSECUTIVE.ordinal()),
            activeConstraints.get(activations.MIDDLE_ASYMMETRY.ordinal())
        );
        if (activeConstraints.get(activations.SYMMETRY.ordinal()))                  symmetry(activeConstraints.get(4));
        if (activeConstraints.get(activations.MIDDLE_ASYMMETRY.ordinal()))          middleAsymmetry();
        if (activeConstraints.get(activations.MAX_4_DROPPED.ordinal()))             max3Dropped();
        if (activeConstraints.get(activations.PLY_PER_DIRECTION.ordinal()))         plyPerDirection();
        //        if (activeConstraints.get(activations.UNIFORM_DISTRIBUTION.ordinal()))      uniformDistribution();
        if (activeConstraints.get(activations.NO_90_GAP_OLD.ordinal()))             no90GapOld();
        if (activeConstraints.get(activations.NO_4_CONSECUTIVE_OLD.ordinal()))      no4ConsecutiveOld();
        if (activeConstraints.get(activations.UNIFORM_SURFACE.ordinal()))           uniformSurface();
        if (activeConstraints.get(activations.SURFACE_45.ordinal()))                surface45();
        if (activeConstraints.get(activations.MIN_CARDINALITIES.ordinal()))         minCardinalities();
    }

    private void onSolutionAll(boolean verbose) {
        solutions = new ArrayList<>();
        dfs.onSolution(() -> {
            if (verbose) System.out.println("Solution found");
            solutions.add(
                new CompositeStructureSolution(instance, sequences, indexes)
            );
        });
    }

    private void onSolution() {
        dfs.onSolution(() ->
            bestSolution = makeSolution()
        );
    }

    private CompositeStructureSolution makeSolution() {
        return new CompositeStructureSolution(
            instance,
            sequences,
            indexes
        );
    }

    private CompositeStructureSolution solve() {
        model();
        dfs = makeDfs(cp, CSSearch.lastConflictFirstFail(sequences));
        onSolution();
        return bestSolution;
    }

    public CSStats findAllSolutions(
        int noSolutions,
        long maxRunTime, // allowed runtime in nanoseconds
        boolean verify,
        boolean verbose
    ) {
        model();
        if (activeConstraints.get(activations.FIRST_FAIL_SEARCH.ordinal()))
            dfs = makeDfs(cp, CSSearch.firstFail(sequences));
        else if (activeConstraints.get(activations.LAST_CONFLICT_SEARCH.ordinal()))
            dfs = makeDfs(cp, CSSearch.lastConflictFirstFail(sequences));
        else if (activeConstraints.get(activations.CONFLICT_ORDERING_SEARCH.ordinal()))
            dfs = makeDfs(cp, CSSearch.conflictOrderingFirstFail(sequences));
        else if (activeConstraints.get(activations.HIGH_DEGREE_SEARCH.ordinal()))
            dfs = makeDfs(cp, CSSearch.highDegreeFirst(sequences, instance, this));
        else if (activeConstraints.get(activations.THIN_SEARCH.ordinal()))
            dfs = makeDfs(cp, CSSearch.thinFirst(sequences, instance, this));
        else if (activeConstraints.get(activations.THICK_SEARCH.ordinal()))
            dfs = makeDfs(cp, CSSearch.thickFirst(sequences, instance, this));
        else if (activeConstraints.get(activations.HIGH_THIN_SEARCH.ordinal()))
            dfs = makeDfs(cp, CSSearch.highThinFirst(sequences, instance, this));
        else if (activeConstraints.get(activations.HIGH_THICK_SEARCH.ordinal()))
            dfs = makeDfs(cp, CSSearch.highThickFirst(sequences, instance, this));

        onSolutionAll(verbose);

        final long startTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
        CSStats statistics          = new CSStats();
        // record solution if feasible
        if (noSolutions == 1) {
            dfs.onSolution(() -> statistics.solution = makeSolution());
        }
        SearchStatistics stats = dfs.solve(
            s ->
                s.numberOfSolutions() == noSolutions
                || (
                    ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() - startTime > maxRunTime
                    && maxRunTime > 0
                )
        );
        long totalTime              = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() - startTime;
        totalTime                   = Math.min(totalTime, maxRunTime);
        statistics.runTime          = totalTime;
        statistics.searchStatistics = stats;

        if (verbose) {
            int i = 0;
            System.out.format("Statistics: %s\n", stats);
            for (CompositeStructureSolution sol : solutions) {
                System.out.println("--------------------");
                System.out.println("Solution: " + ++i);
                sol.printSequences(true);
//                sol.printWithIndexes(true);
            }
        }

        if (verify) new CompositeStructureChecker(activeConstraints, solutions);

        return statistics;
    }

    public static int[][] generateTable(
        boolean no90Gap,
        boolean no5Consecutive
    ) {
        ArrayList<int[]> table = new ArrayList<>();
        final int permutation = (int) Math.pow(4, 5);
        for (int i = 0; i < permutation; i++) {
            int n1 = (i % 4) + 1;
            int n2 = ((i / 4) % 4) + 1;
            int n3 = ((i / 16) % 4) + 1;
            int n4 = ((i / 64) % 4) + 1;

            // prevent sequences with 90 degree gaps
            boolean constr1 = no90Gap &&
                (Math.abs(n1 - n2) == 2 ||
                Math.abs(n2 - n3) == 2 ||
                Math.abs(n3 - n4) == 2);

            // prevent sequences with 5 consecutive plies of the same direction
            boolean constr2 = no5Consecutive &&
                (n1 == n2 && n2 == n3 && n3 == n4);

            if (!constr1 && !constr2) {
                table.add(new int[] { n1, n2, n3, n4 });
            }
        }

        return table.toArray(new int[table.size()][]);
    }

    public static class CSStats {
        SearchStatistics searchStatistics;
        long runTime;
        CompositeStructureSolution solution;
    }

    public static void main(String[] args) {
        final boolean time = true;
        BitSet activeConstraints = getActiveConstraints();

        CompositeStructureInstance instance = new CompositeStructureInstance("data/composite/custom/atMostSeqCard.dzn");
        CompositeStructures cs = new CompositeStructures(instance, activeConstraints);
        long start = System.currentTimeMillis();
        cs.findAllSolutions(6000, 0,false, true);
        long end = System.currentTimeMillis();
        if (time) System.out.println("Time: " + (end - start) + "ms");
    }

    static BitSet getActiveConstraints() {
        BitSet activeConstraints = new BitSet(32);
        // set first 7 constraints to be active
        activeConstraints.set(activations.BLENDING.ordinal());
        activeConstraints.set(activations.NO_PLY_CROSSING.ordinal());
        activeConstraints.set(activations.NO_90_GAP.ordinal());
        activeConstraints.set(activations.NO_4_CONSECUTIVE.ordinal());
        activeConstraints.set(activations.SYMMETRY.ordinal());
        activeConstraints.set(activations.MIDDLE_ASYMMETRY.ordinal());
        activeConstraints.set(activations.MAX_4_DROPPED.ordinal());
        activeConstraints.set(activations.PLY_PER_DIRECTION.ordinal());
        activeConstraints.set(activations.UNIFORM_DISTRIBUTION.ordinal());
        activeConstraints.set(activations.UNIFORM_SURFACE.ordinal());
        activeConstraints.set(activations.SURFACE_45.ordinal());
        if (!activeConstraints.get(activations.PLY_PER_DIRECTION.ordinal()))
            activeConstraints.set(activations.MIN_CARDINALITIES.ordinal());

        // SEARCH
        activeConstraints.set(activations.FIRST_FAIL_SEARCH.ordinal());

        return activeConstraints;
    }
}
