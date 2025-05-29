package org.maxicp.cp.engine.constraints;

import org.maxicp.cp.engine.core.AbstractCPConstraint;
import org.maxicp.cp.engine.core.CPIntVar;
import org.maxicp.util.GraphUtil;
import org.maxicp.util.exception.InconsistencyException;

import java.util.ArrayList;
import java.util.Arrays;

public class CardinalityMinDC extends AbstractCPConstraint {
    private final CPIntVar[] x;
    private final int[] lower;
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

    public CardinalityMinDC(CPIntVar[] x, int[] lower) {
        super(x[0].getSolver());
        int sum = Arrays.stream(lower).sum();
        if (sum > x.length) throw new IllegalArgumentException("sum of lower bounds must be less than or equal to the number of variables");
        this.nVars              = x.length;
        this.x                  = x;
        updateRange();
        this.lower              = new int[maxVal + 1];
        this.upper              = new int[maxVal + 1];
        this.maximumMatching    = new MaximumMatchingGCC(lower, x);
        this.match              = new int[nVars];

        for (int i = 0; i < lower.length; i++) {
            if (lower[i] < 0) throw new IllegalArgumentException("lower bounds must be non negative" + lower[i]);
            this.lower[i] = lower[i];
        }
    }

    @Override
    public void post() {
        ArrayList<Integer> boundValues = new ArrayList<>();
        for (int i = 0; i < lower.length; i++) {
            if (lower[i] > 0) boundValues.add(i);
        }

        for (int i = 0; i < nVars; i++) {
            for (Integer boundValue : boundValues) {
                if (x[i].contains(boundValue)) {
                    x[i].propagateOnDomainChange(this);
                    break;
                }
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
        Arrays.fill(upper, 0);
        for (int j = 0; j < nNodes; j++) {
            in[j].clear();
            out[j].clear();
        }

        for (int varNode = 0; varNode < nVars; varNode++) {
            if (match[varNode] == -1)   assignVar(varNode, x[varNode].min());
            else                        assignVar(varNode, match[varNode]);
        }

        for (int i = nVars; i < nNodes - 1; i++) {
            if (in[i].size() < lower[nodeToVal(i)]) throw InconsistencyException.INCONSISTENCY;
            int currentSize = in[i].size();
            if (currentSize < upper[nodeToVal(i)]) {
                out[sink].add(i);
                in[i].add(sink);
            }
            if (currentSize > lower[nodeToVal(i)]) {
                out[i].add(sink);
                in[sink].add(i);
            }
        }
    }

    private void assignVar(int var, int val) {
        int size = x[var].fillArray(domain);
        for (int j = 0; j < size; j++) {
            upper[domain[j]]++;
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
