package org.maxicp.cp.examples.raw.composite;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.StringJoiner;

import org.maxicp.cp.engine.core.CPIntVar;

public class CompositeStructureSolution {

    private final CompositeStructureInstance instance;
    public final ArrayList<int[]> sequences;
    public final ArrayList<int[]> indexes;
    private String absolutePath;

    public CompositeStructureSolution(
            CompositeStructureInstance instance,
            CPIntVar[][] sequences,
            CPIntVar[][] indexes
    ) {
        this.instance = instance;
        this.sequences = convertToArrayList(sequences);
        this.indexes = convertToArrayList(indexes);
    }

    public CompositeStructureSolution(
            CompositeStructureInstance instance,
            ArrayList<int[]> sequences,
            ArrayList<int[]> indexes
    ) {
        this.instance = instance;
        this.sequences = sequences;
        this.indexes = indexes;
    }

    private ArrayList<int[]> convertToArrayList(CPIntVar[][] sequences) {
        ArrayList<int[]> result = new ArrayList<>();
        for (CPIntVar[] sequence : sequences) {
            int[] seq = new int[sequence.length];
            for (int i = 0; i < sequence.length; i++) {
                seq[i] = sequence[i].min();
            }
            result.add(seq);
        }
        return result;
    }

    public void printSequences(boolean angles) {
        for (int i = 0; i < sequences.size(); i++) {
            System.out.print(fixedLengthString("Node" + " " + i + ":", 20));
            printSequence(i, angles);
        }
    }

    public void printWithIndexes(boolean angles) {
        for (int i = 0; i < instance.noEdges; i++) {
            int parent = instance.edges[i].parent;
            int child = instance.edges[i].child;
            System.out.print(fixedLengthString("Edge " + i + ":", 12));
            System.out.print(
                fixedLengthString(
                "(" + parent + " (" + "Node" + ") " + "--> " + child + ")",
                20
                )
            );
            System.out.print(
                fixedLengthString(
                    "Parent thickness: " + instance.nodeThickness[parent],
                    30
                )
            );
            System.out.println(
                fixedLengthString(
                    "Child thickness: " + instance.nodeThickness[child],
                    30
                )
            );

            printSequence(parent, angles);
            printIndexes(parent, i);
            printChild(child, angles, i, parent);
            System.out.println();
        }
    }

    public void printSequence(int i, boolean angles) {
        System.out.print("[");
        for (int j = 0; j < sequences.get(i).length; j++) {
            int n = angles
                    ? CompositeStructures.angles[sequences.get(i)[j]]
                    : sequences.get(i)[j];
            System.out.print(fixedLengthString(String.valueOf(n), 5));
        }
        System.out.println("]");
    }

    public void printWhiteSpace(int n, int width) {
        for (int i = 0; i < n; i++) System.out.print(
                fixedLengthString("", width)
        );
    }

    public void printIndexes(int parent, int i) {
        System.out.print("[");
        int nEmpty = indexes.get(i)[0];
        printWhiteSpace(nEmpty, 5);
        for (int j = 0; j < indexes.get(i).length; j++) {
            System.out.print(
                    fixedLengthString(String.valueOf(indexes.get(i)[j]), 5)
            );

            int diff = j < indexes.get(i).length - 1
                    ? indexes.get(i)[j + 1] - indexes.get(i)[j]
                    : sequences.get(parent).length -
                    (indexes.get(i).length + nEmpty) +
                    1;
            for (int k = 0; k < (diff - 1); k++) {
                System.out.print(fixedLengthString("", 5));
                nEmpty++;
            }
        }
        System.out.println("]");
    }

    public void printChild(int child, boolean angles, int i, int parent) {
        System.out.print("[");
        int nEmpty = indexes.get(i)[0];
        printWhiteSpace(indexes.get(i)[0], 5);
        for (int j = 0; j < sequences.get(child).length; j++) {
            int n = angles
                    ? CompositeStructures.angles[sequences.get(child)[j]]
                    : sequences.get(child)[j];
            System.out.print(fixedLengthString(String.valueOf(n), 5));

            int diff = j < indexes.get(i).length - 1
                    ? indexes.get(i)[j + 1] - indexes.get(i)[j]
                    : sequences.get(parent).length -
                    (indexes.get(i).length + nEmpty) +
                    1;
            for (int k = 0; k < (diff - 1); k++) {
                System.out.print(fixedLengthString("", 5));
                nEmpty++;
            }
        }
        //        printWhiteSpace(indexes.get(i)[0], 5);
        System.out.println("]");
    }

    public boolean toFile(String filename) {
        filename = filename + ".csr";
        // create the file
        File file;
        try {
            file = new File(filename);

            if (!file.createNewFile()) {
                System.out.println("[-] File already exists: " + filename);
                return false;
            }
        } catch (Exception e) {
            System.out.println("[-] Could not create file: " + filename);
            return false;
        }

        FileWriter writer;
        try {
            writer = new FileWriter(file);
            writer.write(
                    "// File represents a solution of a Composite Structure problem solved by a CSP\n"
            );
            writer.write(
                    "// Created on " +
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS") +
                            "\n"
            );
            if (instance.absolutePath == null) {
                System.out.println(
                        "[+] Instance path not set, creating an instance file"
                );
                instance.toTXT(filename);
            }
            writeInstancePath(writer);
            writeSequences(writer);
            writeIndexes(writer);
        } catch (Exception e) {
            System.out.println("[-] Error while writing to file: " + filename);
            return false;
        }

        absolutePath = file.getAbsolutePath();
        System.out.println("[+] Result written to file: " + absolutePath);
        return true;
    }

    private void writeInstancePath(FileWriter writer) {
        try {
            writer.write("// Instance path\n");
            writer.write(instance.absolutePath + "\n");
        } catch (Exception e) {
            System.out.println("[-] Error while writing instance path");
        }
    }

    private void writeSequences(FileWriter writer) {
        try {
            writer.write("// Sequences\n");
            for (int[] sequence : sequences) {
                for (int i : sequence) {
                    writer.write(i + " ");
                }
                writer.write("\n");
            }
            writer.write("\n");
        } catch (Exception e) {
            System.out.println("[-] Error while writing sequences");
        }
    }

    private void writeIndexes(FileWriter writer) {
        try {
            writer.write("// Indexes\n");
            for (int[] index : indexes) {
                for (int i : index) {
                    writer.write(i + " ");
                }
                writer.write("\n");
            }
            writer.write("\n");
        } catch (Exception e) {
            System.out.println("[-] Error while writing indexes");
        }
    }

    private static String fixedLengthString(String string, int length) {
        return String.format("%1$" + length + "s", string);
    }

    /**
     * Gives a string corresponding to the solution, as would have been written by minizinc
     * @return minizinc string representation of the solution
     */
    public String minizincOneLiner() {
        // seq = [| 1 2 3 4 | 1 3 4 |]
        StringJoiner joiner = new StringJoiner(" ");
        joiner.add("seq = [|");
        for (int i = 0 ; i < sequences.size() ; i++) {
            int[] sequence = sequences.get(i);
            for (int plyOrientation : sequence) {
                joiner.add(String.valueOf(plyOrientation));
            }
            if (i < sequences.size() - 1)
                joiner.add("|");
        }
        joiner.add("|]");
        return joiner.toString();
    }

    public CompositeStructureInstance instance() {
        return instance;
    }
}