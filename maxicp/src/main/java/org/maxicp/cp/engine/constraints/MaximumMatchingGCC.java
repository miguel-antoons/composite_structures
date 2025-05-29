/*
 * MaxiCP is under MIT License
 * Copyright (c)  2023 UCLouvain
 */

package org.maxicp.cp.engine.constraints;

import org.maxicp.cp.engine.core.CPIntVar;

/**
 * Compute and Maintain a Maximum Matching
 * in the variable-value graph
 */
public class MaximumMatchingGCC {

    // For each variable, the setValue it is mached to
    private final Value[] match;
    private final int[] varSeen;

    private int min;

    // For each setValue, the variable idx matched to this setValue, -1 if none of them
    int[] valStart;
    Value[][] values;

    private int sizeMatching;

    private int magic;

    private final CPIntVar[] x;

    public MaximumMatchingGCC(int[] spotsPerVal, CPIntVar... x) {
        this.x = x;

        // find setValue ranges

        min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (CPIntVar cpIntVar : x) {
            min = Math.min(min, cpIntVar.min());
            max = Math.max(max, cpIntVar.max());
        }
        int noValues = max - min + 1;
        valStart = new int[noValues];
        valStart[0] = 0;

        values = new Value[noValues][];
        int count = 0;
        for (int i = min; i <= max; i++) {
            int idx = i - min;
            int amount = i >= spotsPerVal.length ? 0 : spotsPerVal[i];
            values[idx] = new Value[amount];
            for (int j = 0; j < amount; j++) {
                values[idx][j] = new Value(i, count++);
            }
        }

        // initialize

        magic = 0;
        match = new Value[x.length];
        for (int k = 0; k < x.length; k++) {
            match[k] = null; // unmatched
        }
        varSeen = new int[x.length];

        findInitialMatching();
    }


    public int compute(int[] result) {
        for (int k = 0; k < x.length; k++) {
            if (match[k] != null) {
                if (!x[k].contains(match[k].val)) {
                    match[k].linkedVar = -1;
                    match[k] = null;
                    sizeMatching--;
                }
            }
        }
        int sizeMatching = findMaximalMatching();
        for (int k = 0; k < x.length; k++) {
            result[k] = match[k] != null ? match[k].val : -1;
        }
        return sizeMatching;
    }


    private void findInitialMatching() { //returns the size of the maximum matching
        sizeMatching = 0;
        for (int k = 0; k < x.length; k++) {
            int minv = x[k].min();
            int maxv = x[k].max();
            outerLoop:
            for (int i = minv; i <= maxv; i++) {
                int idx = i - min;
                for (int j = 0; j < values[idx].length; j++)
                    if (values[idx][j].linkedVar == -1) // unmatched
                        if (x[k].contains(i)) {
                            match[k] = values[idx][j];
                            match[k].linkedVar = k;
                            sizeMatching++;
                            break outerLoop;
                        }
            }
        }
    }

    private int findMaximalMatching() {
        if (sizeMatching < x.length) {
            for (int k = 0; k < x.length; k++) {
                if (match[k] == null) {
                    magic++;
                    if (findAlternatingPathFromVar(k)) sizeMatching++;
                }
            }
        }
        return sizeMatching;
    }

    private boolean findAlternatingPathFromVar(int i) {
        if (varSeen[i] != magic) {
            varSeen[i]  = magic;
            int xMin    = x[i].min();
            int xMax    = x[i].max();
            for (int v = xMin; v <= xMax; v++)
                if ((match[i] == null || match[i].val != v) && x[i].contains(v))
                    for (int j = 0; j < values[v - min].length; j++)
                        if (findAlternatingPathFromVal(v, j)) {
                            values[v - min][j].linkedVar        = i;
                            match[values[v - min][j].linkedVar] = null;
                            match[i]                            = values[v - min][j];
                            return true;
                        }
        }
        return false;
    }

    private boolean findAlternatingPathFromVal(int v, int j) {
        int idx = v - min;
        if (values[idx][j].magic != magic) {
            values[idx][j].magic = magic;
            if (values[idx][j].linkedVar == -1)
                return true;
            return findAlternatingPathFromVar(values[idx][j].linkedVar);
        }
        return false;
    }

    public static class Value {
        int val;
        int idx;
        int linkedVar;
        int magic;

        Value(int val, int idx) {
            this.val = val;
            this.idx = idx;
            this.linkedVar = -1;
        }
    }

}
