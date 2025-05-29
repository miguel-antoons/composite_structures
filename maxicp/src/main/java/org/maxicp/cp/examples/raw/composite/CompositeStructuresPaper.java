package org.maxicp.cp.examples.raw.composite;


import org.maxicp.cp.engine.constraints.CardinalityMaxFWC;

import java.util.BitSet;

import static org.maxicp.cp.CPFactory.neq;


public class CompositeStructuresPaper extends CompositeStructures{
    public final boolean MERGED_CARDINALITIES = false;
    public CompositeStructuresPaper(CompositeStructureInstance instance, BitSet activeConstraints) {
        super(instance, activeConstraints);
    }

    @Override
    public boolean MERGED_CARDINALITIES() {
        return MERGED_CARDINALITIES;
    }

    @Override
    protected void plyPerDirection() {
        // enforce a certain number of plies for each direction for the given roots
        for (int i = 0; i < instance.noNodes; i++) {
            int[] ppd = instance.pliesPerDirection[i].toArray();
            cp.post(new CardinalityMaxFWC(sequences[i], ppd));
            cp.post(new CardinalityMaxFWC(sequences[i], ppd));
//            if (ppd[1] % 2 == 1 && ppd[3] % 2 == 1 && instance.nodeThickness[i] % 2 == 0) {
//                sequences[i][instance.nodeThickness[i] / 2 - 1].remove(2);
//                sequences[i][instance.nodeThickness[i] / 2 - 1].remove(4);
//                sequences[i][instance.nodeThickness[i] / 2].remove(2);
//                sequences[i][instance.nodeThickness[i] / 2].remove(4);
//                cp.post(neq(
//                    sequences[i][instance.nodeThickness[i] / 2 - 1],
//                    sequences[i][instance.nodeThickness[i] / 2]
//                ));
//            }
        }
    }

    public static void main(String[] args) {
        final boolean time = true;
        BitSet activeConstraints = getActiveConstraints();

        CompositeStructureInstance instance = new CompositeStructureInstance("data/composite/custom/instance_ez.dzn");
        CompositeStructures cs = new CompositeStructuresPaper(instance, activeConstraints);
        long start = System.currentTimeMillis();
        cs.findAllSolutions(1, 0,false, false);
        long end = System.currentTimeMillis();
        if (time) System.out.println("Time: " + (end - start) + "ms");
    }
}
