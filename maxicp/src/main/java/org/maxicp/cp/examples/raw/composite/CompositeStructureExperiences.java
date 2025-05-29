package org.maxicp.cp.examples.raw.composite;

import java.io.File;
import java.io.FileWriter;
import java.util.BitSet;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;

public class CompositeStructureExperiences {
    private final File destFile;
    private final int noThreads;
    private final long maxRunTimeNS;
    private LinkedBlockingQueue<String> instanceFiles;
    private Model currentModel;

    public CompositeStructureExperiences(
        String destFile,
        String[] instanceFolders,
        int noThreads,
        long maxTimeNS,
        Model[] models
    ) throws InterruptedException {
        this.destFile       = createNewCSVFile(destFile);
        this.noThreads      = noThreads - 1;
        this.maxRunTimeNS   = maxTimeNS;
        for (Model model : models) {
            this.instanceFiles  = getInstanceFiles(instanceFolders);
            currentModel = model;
            launchXPThreads();
        }
    }

    private void launchXPThreads() throws InterruptedException {
        Thread[] threads = new Thread[noThreads];
        for (int i = 0; i < noThreads; i++) {
            threads[i] = new Thread(new XPThread());
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }

    private File createNewCSVFile(String destFile) {
        // verify if the extension is .csv
        if (!destFile.endsWith(".csv")) destFile += ".csv";
        File file = new File(destFile);
        if (file.exists()) {
            file.delete();
        }
        // write csv header
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(XPStats.csvHeader());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return file;
    }

    private LinkedBlockingQueue<String> getInstanceFiles(String[] instanceFolders) throws InterruptedException {
        LinkedBlockingQueue<String> instanceFiles = new LinkedBlockingQueue<>();
        for (String folderStr : instanceFolders) {
            File folder = new File(folderStr);
            for (File file : Objects.requireNonNull(folder.listFiles())) {
                if (file.isFile()) {
                    instanceFiles.put(file.getAbsolutePath());
                }
            }
        }
        return instanceFiles;
    }

    private synchronized void writeStats(XPStats stats) {
        try {
            FileWriter writer   = new FileWriter(destFile, true);
            String statsLine    = stats.toCSVLine();
            System.out.print(statsLine);
            writer.write(statsLine);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class XPThread implements Runnable {
        public void run() {
            String instanceFile;
            while ((instanceFile = instanceFiles.poll()) != null) {
                CompositeStructureInstance instance = new CompositeStructureInstance(instanceFile);
                CompositeStructures cs              = new CompositeStructuresPaper(instance, currentModel.activeConstraints);
                CompositeStructures.CSStats stats   = cs.findAllSolutions(
                    1,
                    maxRunTimeNS,
                    false,
                    false
                );
                int status;
                if (stats.searchStatistics.numberOfSolutions() > 0)                                                 status = 1;
                else if (stats.searchStatistics.numberOfSolutions() <= 0 && stats.searchStatistics.isCompleted())   status = 2;
                else                                                                                                status = 0;
                // get only the file name
                String[] instanceFileParts = instanceFile.split("/");
                XPStats xpStats = new XPStats(
                    currentModel.name,
                    instanceFileParts[instanceFileParts.length - 1],
                    status,
                    stats.runTime,
                    stats.searchStatistics.numberOfNodes(),
                    stats.searchStatistics.numberOfFailures()
                );
                writeStats(xpStats);
            }
        }
    }

    public class XPStats {
        private final String solver = "maxicp";
        private final int seed = 0;
        private final String model;
        private final String instanceFile;
        private final int status;
        private final long runTime;
        private final int nodes;
        private final int fails;

        public XPStats(String model, String instanceFile, int status, long runTime, int nodes, int fails) {
            this.model          = model;
            this.instanceFile   = instanceFile;
            this.status         = status;
            this.runTime        = runTime;
            this.nodes          = nodes;
            this.fails          = fails;
        }

        public String toCSVLine() {
            String status = switch (this.status) {
                case 0 -> "UNKNOWN";
                case 1 -> "SATISFIABLE";
                case 2 -> "UNSATISFIABLE";
                default -> "ERROR";
            };
            return String.format(
                "%s,%s,%s,%d,%d,%s,%d,%d,%d\n",
                solver,
                model,
                instanceFile,
                maxRunTimeNS / 1_000_000_000,
                seed,
                status,
                runTime / 1_000_000_000,
                nodes,
                fails
            );
        }

        public static String csvHeader() {
            return "solver,model,instance,timeLimitS,seed,status,runTimeS,nodes,failures\n";
        }
    }

    private static class Model {
        BitSet activeConstraints;
        String name;
    }


    public static void main(String[] args) throws InterruptedException {
        new CompositeStructureExperiences(
            "../results/maxicp/maxicp-2025-03-29_first_fail.csv",
            new String[]{
                "data/composite/bench/3x3",
                "data/composite/bench/4x4",
                "data/composite/bench/5x5",
                "data/composite/bench/6x6",
            },
            25,
            900_000_000_000L,
            new Model[]{
//                modelTemplate(),
                model1(),
//                model2(),
//                model3(),
//                model4(),
//                model5(),
//                model6(),
//                model7()
            }
        );
    }

    private static Model modelTemplate() {
        Model model             = new Model();
        model.activeConstraints = getTemplateConstraints();
        model.name              = "model_template";

        return model;
    }

    private static Model model1() {
        BitSet activeConstraints = getTemplateConstraints();
        activeConstraints.set(CompositeStructures.activations.NO_90_GAP.ordinal());
        activeConstraints.set(CompositeStructures.activations.NO_4_CONSECUTIVE.ordinal());
        activeConstraints.set(CompositeStructures.activations.SYMMETRY.ordinal());
        activeConstraints.set(CompositeStructures.activations.MIDDLE_ASYMMETRY.ordinal());
        activeConstraints.set(CompositeStructures.activations.SURFACE_45.ordinal());

        Model model             = new Model();
        model.activeConstraints = activeConstraints;
        model.name              = "model1";

        return model;
    }

    private static Model model2() {
        BitSet activeConstraints = getTemplateConstraints();
        activeConstraints.set(CompositeStructures.activations.NO_90_GAP.ordinal());

        Model model             = new Model();
        model.activeConstraints = activeConstraints;
        model.name              = "model2";

        return model;
    }

    private static Model model3() {
        BitSet activeConstraints = getTemplateConstraints();
        activeConstraints.set(CompositeStructures.activations.NO_4_CONSECUTIVE.ordinal());

        Model model             = new Model();
        model.activeConstraints = activeConstraints;
        model.name              = "model3";

        return model;
    }

    private static Model model4() {
        BitSet activeConstraints = getTemplateConstraints();
        activeConstraints.set(CompositeStructures.activations.SYMMETRY.ordinal());
        activeConstraints.set(CompositeStructures.activations.MIDDLE_ASYMMETRY.ordinal());

        Model model             = new Model();
        model.activeConstraints = activeConstraints;
        model.name              = "model4";

        return model;
    }

    private static Model model5() {
        BitSet activeConstraints = getTemplateConstraints();
        activeConstraints.set(CompositeStructures.activations.SURFACE_45.ordinal());

        Model model             = new Model();
        model.activeConstraints = activeConstraints;
        model.name              = "model5";

        return model;
    }

    private static Model model6() {
        BitSet activeConstraints = getTemplateConstraints();
        activeConstraints.set(CompositeStructures.activations.NO_4_CONSECUTIVE.ordinal());
        activeConstraints.set(CompositeStructures.activations.SYMMETRY.ordinal());
        activeConstraints.set(CompositeStructures.activations.MIDDLE_ASYMMETRY.ordinal());

        Model model             = new Model();
        model.activeConstraints = activeConstraints;
        model.name              = "model6";

        return model;
    }

    public static Model model7() {
        BitSet activeConstraints = getTemplateConstraints();
        activeConstraints.set(CompositeStructures.activations.NO_90_GAP.ordinal());
        activeConstraints.set(CompositeStructures.activations.NO_4_CONSECUTIVE.ordinal());
        activeConstraints.set(CompositeStructures.activations.SURFACE_45.ordinal());

        Model model             = new Model();
        model.activeConstraints = activeConstraints;
        model.name              = "model7";

        return model;
    }

    public static BitSet getTemplateConstraints() {
        BitSet activeConstraints = new BitSet(32);
        // set first 7 constraints to be active
        activeConstraints.set(CompositeStructures.activations.BLENDING.ordinal());
        activeConstraints.set(CompositeStructures.activations.NO_PLY_CROSSING.ordinal());
        activeConstraints.set(CompositeStructures.activations.MAX_4_DROPPED.ordinal());
        activeConstraints.set(CompositeStructures.activations.PLY_PER_DIRECTION.ordinal());
        activeConstraints.set(CompositeStructures.activations.UNIFORM_SURFACE.ordinal());

        return activeConstraints;
    }
}
