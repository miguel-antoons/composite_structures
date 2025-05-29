package org.maxicp.cp.examples.raw.composite;

import org.maxicp.cp.engine.constraints.CardinalityMaxFWC;
import org.maxicp.cp.engine.constraints.CardinalityMinFWC;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.cp.engine.core.CPSolver;
import org.maxicp.search.DFSearch;

import java.io.File;

import static org.maxicp.cp.CPFactory.*;

public class AtMostSeqCardSim {
    String[] sols;
    CPIntVar[][] seq;
    final private int[] nVars;
    CPSolver cp;
    int[][] cards;
    DFSearch dfs;

    public AtMostSeqCardSim(int[] nVars, int[][] cards) {
        if (nVars.length != cards.length) throw new IllegalArgumentException("nVars and cards must have the same length");
        this.nVars  = nVars;
        this.sols   = new String[nVars.length];
        this.cp     = makeSolver();
        this.cards  = cards;

        for (int i = 0; i < nVars.length; i++) {
            this.sols[i] = "[";
        }
    }

    public void solve() {
        model();
        dfs = makeDfs(cp, CSSearch.lastConflictFirstFail(seq));
        onSolution();
        dfs.solve();
        formatSolution();
    }

    void model() {
        variables();
        minCardinalities();
        maxCardinalities();
        max4Consecutive();
    }

    void minCardinalities() {
        for (int i = 0; i < nVars.length; i++) {
            int[] cardinalities = cards[i];
            cardinalities[1] = Math.floorDiv(cardinalities[1] + cardinalities[3], 2);
            cardinalities[3] = Math.floorDiv(cardinalities[1] + cardinalities[3], 2);
            cp.post(
                new CardinalityMinFWC(
                    seq[i],
                    cardinalities
                )
            );
        }
    }

    void maxCardinalities() {
        for (int i = 0; i < nVars.length; i++) {
            int[] cardinalities = cards[i];
            cardinalities[1] = Math.ceilDiv(cardinalities[1] + cardinalities[3], 2);
            cardinalities[3] = Math.ceilDiv(cardinalities[1] + cardinalities[3], 2);
            cp.post(
                new CardinalityMaxFWC(
                    seq[i],
                    cardinalities
                )
            );
        }
    }

    void max4Consecutive() {
        int[][] table = CompositeStructures.generateTable(false, true);
        for (int i = 0; i < nVars.length; i++) {
            for (int j = 0; j < nVars[i] - 3; j++) {
                CPIntVar[] slice = new CPIntVar[]{
                    seq[i][j],
                    seq[i][j + 1],
                    seq[i][j + 2],
                    seq[i][j + 3],
                };
                cp.post(table(slice, table));
            }
        }
    }

    void variables() {
        seq = new CPIntVar[nVars.length][];
        for (int i = 0; i < nVars.length; i++) {
            seq[i] = makeIntVarArray(
                this.cp,
                nVars[i],
                1,
                4
            );
        }

        for (int i = 0; i < nVars.length; i++) {
            seq[i][0].remove(2);
            seq[i][0].remove(4);
            seq[i][nVars[i] - 1].remove(2);
            seq[i][nVars[i] - 1].remove(4);
        }
    }

    void onSolution() {
        dfs.onSolution(() -> {
            for (int i = 0; i < nVars.length; i++) {
                sols[i] += "|";
                for (int j = 0; j < nVars[i]; j++) {
                    sols[i] += seq[i][j].min();
                    if (j != nVars[i] - 1) sols[i] += ",";
                }}
        });
    }

    void formatSolution() {
        for (String sol : sols) {
            sol += "|];";
            // create a new file and write the solution to it
            File file = new File("solution.txt");
            try (java.io.FileWriter writer = new java.io.FileWriter(file, true)) {
                writer.write(sol + "\n");
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        int[] nVars = {12};
        int[][] cards = {
//            {0, 4, 4, 4, 4},
            {0, 4, 4, 4, 0},
//            {0, 2, 2, 2, 2}
        };
        AtMostSeqCardSim atMostSeqCardSim = new AtMostSeqCardSim(nVars, cards);
        long startTime = System.currentTimeMillis();
        atMostSeqCardSim.solve();
        long endTime = System.currentTimeMillis();
        System.out.println("Time taken: " + (endTime - startTime) + "ms");
    }
}
