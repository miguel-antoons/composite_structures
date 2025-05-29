package org.maxicp.cp.examples.raw.composite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import org.maxicp.util.exception.InconsistencyException;

public class CompositeStructureChecker {

    private final boolean verbose;

    public CompositeStructureChecker(
        BitSet constraints,
        CompositeStructureSolution solution
    ) {
        this(constraints, solution, true, false);
    }

    public CompositeStructureChecker(
        BitSet constraints,
        ArrayList<CompositeStructureSolution> solutions
    ) {
        this(constraints, solutions, true, false);
    }

    public CompositeStructureChecker(
        BitSet constraints,
        CompositeStructureSolution solution,
        boolean block
    ) {
        this(constraints, solution, block, false);
    }

    public CompositeStructureChecker(
        BitSet constraints,
        ArrayList<CompositeStructureSolution> solutions,
        boolean block
    ) {
        this(constraints, solutions, block, false);
    }

    public CompositeStructureChecker(
        BitSet constraints,
        ArrayList<CompositeStructureSolution> solutions,
        boolean block,
        boolean verbose
    ) {
        this.verbose = verbose;
        for (CompositeStructureSolution solution : solutions) {
            if (check(constraints, solution, block) && !verbose) {
                System.out.println("[+] Solution satisfies all constraints");
            } else if (!verbose) {
                System.out.println("[-] Solution violates constraints, to get more details set verbose to true");
            }
        }
    }

    public CompositeStructureChecker(
        BitSet constraints,
        CompositeStructureSolution solution,
        boolean block,
        boolean verbose
    ) {
        this.verbose = verbose;
        if (check(constraints, solution, block) && !verbose) {
            System.out.println("[+] Solution satisfies all constraints");
        } else if (!verbose) {
            System.out.println("[-] Solution violates constraints, to get more details set verbose to true");
        }
    }

    private boolean check(
        BitSet constraints,
        CompositeStructureSolution solution,
        boolean block
    ) {
        boolean result = true;
        if (constraints.get(CompositeStructures.activations.BLENDING.ordinal()))
            if (!(result = checkBlending(solution)) && block)                       throw new InconsistencyException();
        if (constraints.get(CompositeStructures.activations.NO_PLY_CROSSING.ordinal()))
            if (!(result &= checkCrossing(solution)) && block)                      throw new InconsistencyException();
        if (constraints.get(CompositeStructures.activations.NO_90_GAP.ordinal()) || constraints.get(CompositeStructures.activations.NO_90_GAP_OLD.ordinal()))
            if (!(result &= checkNo90Gap(solution)) && block)                       throw new InconsistencyException();
        if (constraints.get(CompositeStructures.activations.NO_4_CONSECUTIVE.ordinal()) || constraints.get(CompositeStructures.activations.NO_4_CONSECUTIVE_OLD.ordinal()))
            if (!(result &= checkNo4Consecutive(solution)) && block)                throw new InconsistencyException();
        if (constraints.get(CompositeStructures.activations.SYMMETRY.ordinal()))
            if (!(result &= checkSymmetry(solution, constraints.get(4))) && block)  throw new InconsistencyException();
        if (constraints.get(CompositeStructures.activations.MIDDLE_ASYMMETRY.ordinal()))
            if (!(result &= checkMiddleAsymmetry(solution)) && block)               throw new InconsistencyException();
        if (constraints.get(CompositeStructures.activations.MAX_4_DROPPED.ordinal()))
            if (!(result &= checkMax4Dropped(solution)) && block)                   throw new InconsistencyException();
        if (constraints.get(CompositeStructures.activations.PLY_PER_DIRECTION.ordinal()))
            if (!(result &= checkCardinality(solution)) && block)                   throw new InconsistencyException();
        if (constraints.get(CompositeStructures.activations.UNIFORM_DISTRIBUTION.ordinal()))
            if (!(result &= checkUniformDistribution(solution)) && block)           throw new InconsistencyException();
        if (constraints.get(CompositeStructures.activations.UNIFORM_SURFACE.ordinal()))
            if (!(result &= checkUniformSurface(solution)) && block)                throw new InconsistencyException();
        if (constraints.get(CompositeStructures.activations.SURFACE_45.ordinal()))
            if (!(result &= checkSurface45(solution)) && block)                     throw new InconsistencyException();
        if (constraints.get(CompositeStructures.activations.MIN_CARDINALITIES.ordinal()))
            if (!(result &= checkMinCardinalities(solution)) && block)              throw new InconsistencyException();

        return result;
    }

    private boolean checkBlending(CompositeStructureSolution solution) {
        CompositeStructureInstance instance = solution.instance();
        for (int i = 0; i < instance.noEdges; i++) {
            int[] parent = solution.sequences.get(instance.edges[i].parent);
            int[] child = solution.sequences.get(instance.edges[i].child);
            int[] indexes = solution.indexes.get(i);

            for (int j = 0; j < indexes.length; j++) {
                if (child[j] != parent[indexes[j]]) {
                    printEdge(
                "Blending constraint violated at index " + j + " in edge " + i,
                        solution,
                        i
                    );
                    return false;
                }
            }
        }

        if (verbose) System.out.println("[+] Blending constraint satisfied");
        return true;
    }

    private boolean checkCrossing(CompositeStructureSolution solution) {
        for (int i = 0; i < solution.indexes.size(); i++) {
            int[] indexes = solution.indexes.get(i);
            for (int j = 1; j < indexes.length; j++) {
                if (indexes[j] < indexes[j - 1]) {
                    printEdge(
                        "Crossing constraint violated at index " + j + " in edge " + i,
                        solution,
                        i
                    );
                    return false;
                }
            }
        }

        if (verbose) System.out.println("[+] Crossing constraint satisfied");
        return true;
    }

    private boolean checkNo90Gap(CompositeStructureSolution solution) {
        for (int i = 0; i < solution.sequences.size(); i++) {
            int[] sequence = solution.sequences.get(i);
            for (int j = 1; j < sequence.length; j++) {
                if (sequence[j] - sequence[j - 1] == 2) {
                    System.out.println("--------------------");
                    System.out.println("[-] No 90 gap constraint violated at index " + j + " in sequence " + i);
                    System.out.print(fixedLengthString("Node " + i + ":", 12));
                    solution.printSequence(i, true);
                    return false;
                }
            }
        }

        if (verbose) System.out.println("[+] No 90 gap constraint satisfied");
        return true;
    }

    private boolean checkNo4Consecutive(CompositeStructureSolution solution) {
        for (int i = 0; i < solution.sequences.size(); i++) {
            int[] sequence = solution.sequences.get(i);
            for (int j = 4; j < sequence.length; j++) {
                if (
                    sequence[j] == sequence[j - 1] &&
                    sequence[j] == sequence[j - 2] &&
                    sequence[j] == sequence[j - 3]
                ) {
                    System.out.println("--------------------");
                    System.out.println("[-] No 4 consecutive constraint violated at index " + j + " in sequence " + i);
                    System.out.print(fixedLengthString("Node " + i + ":", 12));
                    solution.printSequence(i, true);
                    return false;
                }
            }
        }

        if (verbose) System.out.println("[+] No 4 consecutive constraint satisfied");
        return true;
    }

    private boolean checkSymmetry(
            CompositeStructureSolution solution,
            boolean middleAsymmetry
    ) {
        for (int i = 0; i < solution.sequences.size(); i++) {
            int[] sequence = solution.sequences.get(i);
            int end = middleAsymmetry
                ? sequence.length / 2 - (1 + (sequence.length % 2))
                : sequence.length / 2;
            for (int j = 0; j < end; j++) {
                if (sequence[j] != sequence[sequence.length - j - 1]) {
                    System.out.println("--------------------");
                    System.out.println("[-] Symmetry constraint violated at index " + j + " in sequence " + i);
                    System.out.print(fixedLengthString("Node " + i + ":", 12));
                    solution.printSequence(i, true);
                    return false;
                }
            }
        }

        if (verbose) System.out.println("[+] Symmetry constraint satisfied");
        return true;
    }

    private boolean checkMiddleAsymmetry(CompositeStructureSolution solution) {
        for (int i = 0; i < solution.sequences.size(); i++) {
            int[] sequence  = solution.sequences.get(i);
            int[][] allowed = sequence.length % 2 == 0
                ? CompositeStructures.allowedMiddleEven
                : CompositeStructures.allowedMiddleOdd;
            int start       = sequence.length / 2 - 1;
            int end         = sequence.length % 2 == 0 ? start + 2 : start + 3;
            int[] middle    = new int[end - start];
            System.arraycopy(sequence, start, middle, 0, end - start);

            boolean found = false;
            for (int[] a : allowed) {
                if (Arrays.equals(a, middle)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                System.out.println("--------------------");
                System.out.println("[-] Middle asymmetry constraint violated at node " + i);
                System.out.print(fixedLengthString("Node " + i + ":", 12));
                solution.printSequence(i, true);
                return false;
            }
        }

        if (verbose) System.out.println("[+] Middle asymmetry constraint satisfied");
        return true;
    }

    private boolean checkMax4Dropped(CompositeStructureSolution solution) {
        for (int i = 0; i < solution.indexes.size(); i++) {
            int[] indexes = solution.indexes.get(i);
            int previous = 0;
            for (int j = 0; j < indexes.length; j++) {
                if (indexes[j] - previous > 5) {
                    printEdge(
                        "Max 4 dropped constraint violated at index " + j + " in edge " + i,
                        solution,
                        i
                    );
                    return false;
                }
                previous = indexes[j];
            }

            if (solution.instance().nodeThickness[solution.instance().edges[i].parent] - indexes[indexes.length - 1] > 5) {
                printEdge(
                    "Max 4 dropped constraint violated at index " + indexes.length + " in edge " + i,
                    solution,
                    i
                );
                return false;
            }
        }

        if (verbose) System.out.println("[+] Max 4 dropped constraint satisfied");
        return true;
    }

    private boolean checkCardinality(CompositeStructureSolution solution) {
        for (int i = 0; i < solution.instance().noNodes; i++) {
            int[] sequence = solution.sequences.get(i);
            // * uncomment below if the given plies per direction contain 4 cardinalities
            //            int[] cardinalities = solution.instance().pliesPerDirection[i].toArray();
            int[] cardinalities = solution
                    .instance()
                    .pliesPerDirection[i].toArrayMerged();
            int[] counts = new int[cardinalities.length];

            for (int k : sequence) {
                // supposing the given plies per direction only contain 3 cardinalities (0, +/-45, 90)
                if (k == 3 || k == 1) {
                    counts[1]++;
                    counts[3]++;
                } else {
                    counts[k]++;
                }
                // * uncomment if the given plies per direction contain 4 cardinalities
                //                counts[k]++;
            }

            if (!Arrays.equals(cardinalities, counts)) {
                System.out.println("--------------------");
                System.out.println("[-] Cardinalities constraint violated at sequence " + i);
                System.out.print(fixedLengthString("Sequence " + i + ":", 12));
                solution.printSequence(i, true);
                System.out.println("Expected cardinalities: " + Arrays.toString(cardinalities));
                System.out.println("Actual cardinalities: " + Arrays.toString(counts));
                return false;
            }
        }

        if (verbose) System.out.println("[+] Root cardinality constraint satisfied");
        return true;
    }

    private boolean checkUniformDistribution(CompositeStructureSolution solution) {
        // TODO
        return true;
    }

    private boolean checkUniformSurface(CompositeStructureSolution solution) {
        for (int i = 0; i < solution.indexes.size(); i++) {
            int parentThickness = solution.instance().nodeThickness[solution.instance().edges[i].parent];
            int[] ind = solution.indexes.get(i);
            if (ind[0] != 0 || ind[ind.length - 1] != parentThickness - 1) {
                printEdge(
                    "Surface is not uniform in indexes in edge " + i,
                    solution,
                    i
                );
                return false;
            }
        }

        for (int i = 0; i < solution.sequences.size() - 1; i++) {
            int[] seq1 = solution.sequences.get(i);
            int[] seq2 = solution.sequences.get(i + 1);
            if (seq1[0] != seq2[0] || seq1[seq1.length - 1] != seq2[seq2.length - 1]
            ) {
                System.out.println("--------------------");
                System.out.println(
                    "[-] A difference in ply angle of the surface has been noted between nodes " + i + " and " + (i + 1)
                );
                System.out.print(fixedLengthString("Node " + i + ":", 12));
                solution.printSequence(i, true);
                System.out.print(fixedLengthString("Node " + (i + 1) + ":", 12));
                solution.printSequence(i + 1, true);
                return false;
            }
        }

        return true;
    }

    private boolean checkSurface45(CompositeStructureSolution solution) {
        for (int i = 0; i < solution.sequences.size(); i++) {
            int[] seq = solution.sequences.get(i);
            if (
                seq[0] == 2 ||
                seq[0] == 4 ||
                seq[seq.length - 1] == 2 ||
                seq[seq.length - 1] == 4
            ) {
                System.out.println("--------------------");
                System.out.println("[-] Surface 45 constraint violated at node " + i);
                System.out.print(fixedLengthString("Node " + i + ":", 12));
                solution.printSequence(i, true);
                return false;
            }
        }
        return true;
    }

    private boolean checkMinCardinalities(CompositeStructureSolution solution) {
        for (int i = 0; i < solution.sequences.size(); i++) {
            int[] counts = new int[5];
            for (int ply : solution.sequences.get(i)) {
                counts[ply]++;
            }
            int min = (int) Math.ceil(CompositeStructures.minPlyPercentage * solution.sequences.get(i).length);
            for (int j = 1; j < counts.length; j++) {
                if (counts[j] < min) {
                    System.out.println("--------------------");
                    System.out.println("[-] Min cardinalities constraint violated at node " + i);
                    System.out.print(fixedLengthString("Node " + i + ":", 12));
                    solution.printSequence(i, true);
                    System.out.println("Minimum cardinality: " + min);
                    System.out.println("Actual cardinalities: " + Arrays.toString(counts));
                    return false;
                }
            }
        }
        return true;
    }

    private static void printEdge(
        String message,
        CompositeStructureSolution solution,
        int i
    ) {
        System.out.println("--------------------");
        System.out.println("[-] " + message);
        System.out.print(fixedLengthString("Edge " + i + ":", 12));
        System.out.print(
            fixedLengthString(
                "(" + solution.instance().edges[i].parent + " --> " + solution.instance().edges[i].child + ")",
                20
            )
        );
        System.out.print(
            fixedLengthString(
                "Parent thickness: " + solution.instance().nodeThickness[solution.instance().edges[i].parent],
                30
            )
        );
        System.out.println(
            fixedLengthString(
                "Child thickness: " + solution.instance().nodeThickness[solution.instance().edges[i].child],
                30
            )
        );
        solution.printSequence(solution.instance().edges[i].parent, true);
        solution.printIndexes(solution.instance().edges[i].parent, i);
        solution.printChild(
            solution.instance().edges[i].child,
            true,
            i,
            solution.instance().edges[i].parent
        );
    }

    private static String fixedLengthString(String string, int length) {
        return String.format("%1$" + length + "s", string);
    }
}