package org.maxicp.cp.examples.raw.composite;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class CompositeStructureInstance {

    public int noNodes;
    public int noEdges;
    public int[] nodeThickness;
    public int[] cardinalities;
    public DirectedEdge[] edges;
    public PliesPerDirection[] pliesPerDirection;
    public String absolutePath;

    // * CONSTRUCTORS

    public CompositeStructureInstance(List<int[]> edges, int[][] pliesPerDirection) {
        int[][] e = edges.toArray(new int[edges.size()][]);
        int nSequences = pliesPerDirection.length;
        int[] thickness = new int[nSequences];
        for (int i = 0; i < pliesPerDirection.length; i++) {
            thickness[i] = Arrays.stream(pliesPerDirection[i]).sum();
        }
        init(thickness, e, pliesPerDirection);
    }

    public CompositeStructureInstance(
            int[] nodeThickness,
            int[][] edges,
            int[][] pliesPerDirection
    ) {
        init(nodeThickness, edges, pliesPerDirection);
    }

    public CompositeStructureInstance(
        int[][] edges,
        int[][] pliesPerDirection
    ) {
        int[] nodeThickness = new int[edges.length + 1];
        for (int i = 0; i < pliesPerDirection.length; i++) {
            nodeThickness[i] = Arrays.stream(pliesPerDirection[i]).sum();
        }
        int[] roots = IntStream.range(0, pliesPerDirection.length).toArray();
        init(nodeThickness, edges, pliesPerDirection);
    }

    public CompositeStructureInstance(String filename) {
        if (filename.endsWith(".csi") || filename.endsWith(".txt")) {
            fromTXT(filename);
        } else if (filename.endsWith(".dzn")) {
            fromDZN(filename);
        } else {
            throw new IllegalArgumentException("[-] Invalid file extension: " + filename);
        }
    }

    // * CLASS INITIALIZATION

    private void init(
        int[] nodeThickness,
        int[][] edges,
        int[][] pliesPerDirection
    ) {
        this.noNodes        = nodeThickness.length;
        this.noEdges        = edges.length;
        this.nodeThickness  = nodeThickness;
        this.cardinalities  = getCardinalities(edges);

        this.edges = new DirectedEdge[edges.length];
        for (int i = 0; i < edges.length; i++) {
            boolean firstThicker = nodeThickness[edges[i][0]] > nodeThickness[edges[i][1]];
            this.edges[i] = firstThicker
                ? new DirectedEdge(edges[i][0], edges[i][1])
                : new DirectedEdge(edges[i][1], edges[i][0]);
        }

        this.pliesPerDirection = new PliesPerDirection[pliesPerDirection.length];
        for (int i = 0; i < pliesPerDirection.length; i++) {
            this.pliesPerDirection[i] = new PliesPerDirection(
                pliesPerDirection[i][0],
                pliesPerDirection[i][1],
                pliesPerDirection[i][2],
                pliesPerDirection[i][3]
            );
        }
    }

    private int[] getCardinalities(int[][] edges) {
        int[] cardinalities = new int[noNodes];
        for (int[] edge : edges) {
            cardinalities[edge[0]]++;
            cardinalities[edge[1]]++;
        }
        return cardinalities;
    }

    // * FILE READING
    // * READ DZN

    /**
     * Read instance from a .dzn file
     * @param filename path to the file
     */
    private void fromDZN(String filename) {
        // open file and read instance
        File file = new File(filename);

        if (!file.exists()) throw new IllegalArgumentException(
            "[-] File does not exist: " + filename
        );

        absolutePath = file.getAbsolutePath();
        Scanner scanner;
        try {
            scanner = new Scanner(file);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "[-] Could not read file: " + filename
            );
        }

        readDZNInt(scanner);
        readDZNInt(scanner);
        readDZNInt(scanner);
        List<int[]> edges = readDZNArray(
            scanner,
            s -> {
                s = s.replace("(", "").replace(")", "");
                String[] edge = s.split(",");
                return new int[]{Integer.parseInt(edge[0]) - 1, Integer.parseInt(edge[1]) - 1};
            },
            "\\(\\d+,\\d+\\)"
        );
        List<int[]> pliesPerDirection = readDZNArray(
            scanner,
            s -> {
                s = s.replace("|", "");
                String[] plies = s.split(",");
                return new int[] {
                    Integer.parseInt(plies[0]),
                    Integer.parseInt(plies[1]),
                    Integer.parseInt(plies[2]),
                    Integer.parseInt(plies[3])
                };
            },
            "\\|\\d+,\\d+,\\d+,\\d+"
        );
        int[] thicknesses = new int[pliesPerDirection.size()];
        for (int i = 0; i < pliesPerDirection.size(); i++) {
            thicknesses[i] = Arrays.stream(pliesPerDirection.get(i)).sum();
        }
        init(thicknesses, edges.toArray(new int[edges.size()][]), pliesPerDirection.toArray(new int[pliesPerDirection.size()][]));
    }

    private String getDZNLine(Scanner scanner) {
        StringBuilder stringArray = new StringBuilder();
        do {
            stringArray.append(skipDZNComments(scanner));
        } while (!stringArray.toString().endsWith(";"));

        // remove all spaces form the string
        return stringArray.toString().replaceAll("\\s+", "");
    }

    private int readDZNInt(Scanner scanner) {
        String stringLine = getDZNLine(scanner).split("=")[1];
        stringLine = stringLine.substring(0, stringLine.length() - 1);
        return Integer.parseInt(stringLine);
    }

    private<T> List<T> readDZNArray(Scanner scanner, Function<String, T> fromStringFunction, String regex) {
        String stringLine = getDZNLine(scanner).split("=")[1];

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(stringLine);

        List<T> array = new ArrayList<>();
        while (matcher.find()) {
            String value = matcher.group();
            array.add(fromStringFunction.apply(value));
        }

        return array;
    }

    private String skipDZNComments(Scanner scanner) {
        String result = "";
        while (scanner.hasNextLine() && (result = scanner.nextLine()).startsWith("%")) {
            // skip comments
        }
        return result;
    }

    // * READ TXT

    /**
     * Read instance from a .txt file
     * @param filename path to the file
     */
    private void fromTXT(String filename) {
        // open file and read instance
        File file = new File(filename);

        if (!file.exists()) throw new IllegalArgumentException(
            "[-] File does not exist: " + filename
        );

        absolutePath = file.getAbsolutePath();
        Scanner scanner;
        try {
            scanner = new Scanner(file);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "[-] Could not read file: " + filename
            );
        }

//        int[] nodeThickness         = readNodeThicknesses(scanner);
//        int[] roots                 = readRoots(scanner);
        int[][] edges               = readEdges(scanner);
        int[][] pliesPerDirection   = readPliesPerDirection(scanner);
        int[] nodeThickness         = new int[pliesPerDirection.length];
        for (int i = 0; i < pliesPerDirection.length; i++) {
            nodeThickness[i] = Arrays.stream(pliesPerDirection[i]).sum();
        }
        int[] roots = IntStream.range(0, pliesPerDirection.length).toArray();

        scanner.close();
        init(nodeThickness, edges, pliesPerDirection);
    }

    private int[][] readEdges(Scanner scanner) {
        if (!scanner.hasNextLine()) {
            System.out.println("[+] No edges present");
            return new int[0][0];
        }

        ArrayList<String> lines = new ArrayList<>();
        String line;
        while (scanner.hasNextLine() && !Objects.equals(line = skipTXTComments(scanner), "")) {
            lines.add(line);
        }

        int[][] edges = new int[lines.size()][2];
        for (int i = 0; i < lines.size(); i++) {
            String[] edge = lines.get(i).split(" ");
            for (int j = 0; j < edge.length; j++) {
                edges[i][j] = Integer.parseInt(edge[j]);
            }
        }

        return edges;
    }

    private int[][] readPliesPerDirection(Scanner scanner) {
        if (!scanner.hasNextLine()) {
            System.out.println("[+] No plies per direction present");
            return new int[0][0];
        }

        ArrayList<String> lines = new ArrayList<>();
        String line;
        while (scanner.hasNextLine() && !Objects.equals(line = skipTXTComments(scanner), "")) {
            lines.add(line);
        }

        int[][] pliesPerDirection = new int[lines.size()][4];
        for (int i = 0; i < lines.size(); i++) {
            String[] plies = lines.get(i).split(" ");
            for (int j = 0; j < plies.length; j++) {
                pliesPerDirection[i][j] = Integer.parseInt(plies[j]);
            }
        }

        return pliesPerDirection;
    }

    private String skipTXTComments(Scanner scanner) {
        String result = "";
        while (scanner.hasNextLine() && (result = scanner.nextLine()).startsWith("//")) {
            // skip comments
        }
        return result;
    }

    // * FILE WRITING
    // * WRITE DZN

    public boolean toDZN(String filename) {
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
                "% File represents an instance of a Composite Structure problem to be solved by a CP framework\n"
            );
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(
                new java.util.Date()
            );
            writer.write("% Created on " + date + "\n");
            writeDZNInt(writer, "T", Arrays.stream(nodeThickness).max().getAsInt());
            writeDZNInt(writer, "n", noNodes);
            writeDZNInt(writer, "m", noEdges);
            writeDZN1DArray(writer, "edges", edges, DirectedEdge::minizincString);
            writeDZN2DArray(writer, "counts", pliesPerDirection, PliesPerDirection::minizincString);
            writer.close();
        } catch (Exception e) {
            System.out.println("[-] Error while writing to file: " + filename);
            return false;
        }

        System.out.println("[+] Instance written to file: " + file.getAbsolutePath());
        return true;
    }

    private void writeDZNInt(FileWriter writer, String name, int value) {
        try {
            writer.write(name + " = " + value + ";\n");
        } catch (Exception e) {
            System.out.println("[-] Error while writing thickest node value");
        }
    }

    private<T> void writeDZN2DArray(FileWriter writer, String name, T[] array, Function<T, String> toStringFunction) {
        try {
            writer.write(name + " = [" + (array.length > 0 ? "|" : ""));
            for (int i = 0 ; i < array.length ; i++) {
                T entry = array[i];
                String entryString = toStringFunction.apply(entry);
                boolean lastEntry = i == array.length - 1;
                if (lastEntry) {
                    writer.write(entryString + "|];\n");
                } else {
                    writer.write(entryString + ",\n|");
                }
            }
        } catch (Exception e) {
            System.out.println("[-] Error while writing plies per direction");
        }
    }

    private<T> void writeDZN1DArray(FileWriter writer, String name, T[] array, Function<T, String> toStringFunction) {
        try {
            writer.write(name + " = [");
            for (int i = 0 ; i < array.length ; i++) {
                T entry = array[i];
                String entryString = toStringFunction.apply(entry);
                boolean lastEntry = i == array.length - 1;
                if (lastEntry) {
                    writer.write(entryString + "];\n");
                } else {
                    writer.write(entryString + ",\n");
                }
            }
        } catch (Exception e) {
            System.out.println("[-] Error while writing plies per direction");
        }
    }

    // * WRITE TXT

    public boolean toTXT(String filename) {
        filename = filename + ".txt";
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
                "// File represents an instance of a Composite Structure problem to be solved by a CP framework\n"
            );
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(
                new java.util.Date()
            );
            writer.write("// Created on " + date + "\n");
//            writeNodeThicknesses(writer);
//            writeRoots(writer);
            writeEdges(writer);
            writePliesPerDirection(writer);
            writer.close();
        } catch (Exception e) {
            System.out.println("[-] Error while writing to file: " + filename);
            return false;
        }

        absolutePath = file.getAbsolutePath();
        System.out.println("[+] Instance written to file: " + absolutePath);
        return true;
    }

//    private void writeNodeThicknesses(FileWriter writer) {
//        try {
//            writer.write("// Node thicknesses\n");
//            for (int thickness : nodeThickness) {
//                writer.write(thickness + " ");
//            }
//            writer.write("\n");
//        } catch (Exception e) {
//            System.out.println("[-] Error while writing node thicknesses");
//        }
//    }

//    private void writeRoots(FileWriter writer) {
//        try {
//            writer.write("// Roots\n");
//            for (int root : roots) {
//                writer.write(root + " ");
//            }
//            writer.write("\n");
//        } catch (Exception e) {
//            System.out.println("[-] Error while writing roots");
//        }
//    }

    private void writeEdges(FileWriter writer) {
        try {
            writer.write("// Edges\n");
            for (DirectedEdge edge : edges) {
                writer.write(edge.parent + " " + edge.child + "\n");
            }
            writer.write("\n");
        } catch (Exception e) {
            System.out.println("[-] Error while writing edges");
        }
    }

    private void writePliesPerDirection(FileWriter writer) {
        try {
            writer.write("// Plies per direction\n");
            for (PliesPerDirection plies : pliesPerDirection) {
                int[] pliesArray = plies.toArray();
                for (int i = 1; i < pliesArray.length; i++) {
                    writer.write(pliesArray[i] + " ");
                }
                writer.write("\n");
            }
            writer.write("\n");
        } catch (Exception e) {
            System.out.println("[-] Error while writing plies per direction");
        }
    }

    // * UTILS

    private Scanner createScanner(String filename) {
        // open file and read instance
        File file = new File(filename);

        if (!file.exists()) throw new IllegalArgumentException(
            "[-] File does not exist: " + filename
        );

        absolutePath = file.getAbsolutePath();
        Scanner scanner;
        try {
            scanner = new Scanner(file);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "[-] Could not read file: " + filename
            );
        }
        return scanner;
    }

    public boolean toDZNFolder(String folder) {
        final String[] path = absolutePath.split("/");
        folder              = folder.substring(folder.length() - 1).equals("/") ? folder : folder + "/";
        String filename     = folder + path[path.length - 1].split("\\.")[0] + ".dzn";
        return toDZN(filename);
    }

    public static class DirectedEdge {
        // directedEdge where from is the parent and to is the child
        public final int parent;
        public final int child;

        public DirectedEdge(int parent, int child) {
            this.parent = parent;
            this.child = child;
        }

        @Override
        public String toString() {
            return String.format("(%d, %d)", parent, child);
        }

        public String minizincString() {
            return String.format("(%d, %d)", parent + 1, child + 1);
        }
    }

    public static class PliesPerDirection {
        private final int nom45;
        private final int no0;
        private final int no45;
        private final int no90;

        public PliesPerDirection(int nom45, int no0, int no45, int no90) {
            this.nom45 = nom45;
            this.no0 = no0;
            this.no45 = no45;
            this.no90 = no90;
        }

        public int[] toArray() {
            return new int[] { 0, nom45, no0, no45, no90 };
        }

        public int[] toArrayMerged() {
            return new int[] { 0, nom45 + no45, no0, nom45 + no45, no90 };
        }

        public String minizincString() {
            return String.format("%d, %d, %d, %d", nom45, no0, no45, no90);
        }
    }

    public static void main(String[] args) {
        CompositeStructureInstance instance = new CompositeStructureInstance("./data/composite/2x2/random_grid_2_2_000.dzn");
    }
}
