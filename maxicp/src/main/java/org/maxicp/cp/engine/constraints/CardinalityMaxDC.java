package org.maxicp.cp.engine.constraints;

import org.maxicp.cp.engine.core.AbstractCPConstraint;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.util.GraphUtil;
import org.maxicp.util.exception.InconsistencyException;

import java.util.ArrayList;
import java.util.Arrays;

public class CardinalityMaxDC extends AbstractCPConstraint {
    private final CPIntVar[] x;
    private final int[] lower;
    private int[] gLower;
    private final int[] upper;
    private int[] domain;
    private final int nVars;
    private int minVal;
    private int maxVal;
    private int sink;
    private int nValues;

    private int nNodes;
    private ArrayList<Integer>[] in;
    private ArrayList<Integer>[] out;
    protected GraphUtil.Graph g = new GraphUtil.Graph() {
        @Override
        public int n() {
            return nNodes;
        }

        @Override
        public Iterable<Integer> in(int idx) {
            return in[idx];
        }

        @Override
        public Iterable<Integer> out(int idx) {
            return out[idx];
        }
    };

    private final int[] match;
    private final MaximumMatchingGCC maximumMatching;

    public CardinalityMaxDC(CPIntVar[] x, int[] upper) {
        super(x[0].getSolver());
        this.nVars              = x.length;
        this.x                  = x;
        updateRange();
        this.lower              = new int[maxVal + 1];
        this.upper              = new int[maxVal + 1];
        this.gLower             = new int[maxVal + 1];
        Arrays.fill(this.upper, x.length);
        System.arraycopy(upper, 0, this.upper, 0, upper.length);
        for (int i = 0; i < upper.length; i++) {
            if (upper[i] < 0) throw new IllegalArgumentException("upper bounds must be non negative" + upper[i]);
            if (this.upper[i] > x.length) this.upper[i] = x.length;
        }
        this.maximumMatching    = new MaximumMatchingGCC(this.upper, x);
        this.match              = new int[nVars];
    }

    @Override
    public void post() {
        ArrayList<Integer> boundValues = new ArrayList<>();
        for (int i = 0; i < upper.length; i++) {
            if (upper[i] < x.length) boundValues.add(i);
        }

        for (int i = 0; i < nVars; i++) {
            for (Integer boundValue : boundValues) {
                if (x[i].contains(boundValue)) x[i].propagateOnDomainChange(this);
            }
        }
        nNodes  = nVars + nValues + 1;
        sink    = nNodes - 1;
        in      = new ArrayList[nNodes];
        out     = new ArrayList[nNodes];
        domain  = new int[nValues];
        for (int i = 0; i < nNodes; i++) {
            in[i]   = new ArrayList<>();
            out[i]  = new ArrayList<>();
        }
        propagate();
    }

    @Override
    public void propagate() {
        maximumMatching.compute(match);
        updateGraph();

        int[] scc = GraphUtil.stronglyConnectedComponents(g);
        for (int i = 0; i < nVars; i++) {
            for (int j = 0; j < in[i].size(); j++) {
                if (scc[i] != scc[in[i].get(j)])
                    x[i].remove(nodeToVal(in[i].get(j)));
            }
        }
    }

    public void setGLower(int[] gLower) {
        this.gLower = gLower;
    }

    private void updateRange() {
        minVal = Integer.MAX_VALUE;
        maxVal = Integer.MIN_VALUE;
        for (int i = 0; i < nVars; i++) {
            minVal = Math.min(minVal, x[i].min());
            maxVal = Math.max(maxVal, x[i].max());
        }
        nValues = maxVal - minVal + 1;
    }

    private void updateGraph() {
        Arrays.fill(lower, 0);
        for (int j = 0; j < nNodes; j++) {
            in[j].clear();
            out[j].clear();
        }

        for (int varNode = 0; varNode < nVars; varNode++) {
            if (match[varNode] == -1)   throw InconsistencyException.INCONSISTENCY;
            else                        assignVar(varNode, match[varNode]);
        }

        for (int i = nVars; i < nNodes - 1; i++) {
            if (in[i].size() > upper[nodeToVal(i)]) throw InconsistencyException.INCONSISTENCY;
            int currentSize = in[i].size();
            if (currentSize < upper[nodeToVal(i)]) {
                out[sink].add(i);
                in[i].add(sink);
            }
            lower[nodeToVal(i)] = Math.max(lower[nodeToVal(i)], gLower[nodeToVal(i)]);
            if (currentSize > lower[nodeToVal(i)]) {
                out[i].add(sink);
                in[sink].add(i);
            }
        }
    }

    private void assignVar(int var, int val) {
        if (x[var].isFixed()) lower[val]++;
        int size = x[var].fillArray(domain);
        for (int j = 0; j < size; j++) {
            if (domain[j] == val) {
                out[var].add(valToNode(val));
                in[valToNode(val)].add(var);
            } else {
                out[valToNode(domain[j])].add(var);
                in[var].add(valToNode(domain[j]));
            }
        }
    }

    private int valToNode(int val) {
        return (val - minVal) + nVars;
    }

    private int nodeToVal(int node) {
        return node - nVars + minVal;
    }
}
