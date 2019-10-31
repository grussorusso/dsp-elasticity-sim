package it.uniroma2.dspsim.dsp.edf.om.rl.utils;

import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;

import java.util.Iterator;

public class ActionIterator implements Iterator<Action> {

    /* We iterate over actions in this order:
     * (do nothing action), (-1, 0), (-1, 1), ...
     * (+1, 0), (+1, 1...)
     */
    private int delta = -1;
    private int resTypeIndex = -1;
    private int actionIndex = 0;
    boolean first = true;

    @Override
    public boolean hasNext() {
        return delta < 1 || resTypeIndex < (ComputingInfrastructure.getInfrastructure().getNodeTypes().length-1);
    }

    @Override
    public Action next() {
        if (first) {
            first = false;
            return new Action(0,0, 0);
        }

        if (!hasNext())
            return null;

        ++resTypeIndex;
        ++actionIndex;
        if (resTypeIndex >= (ComputingInfrastructure.getInfrastructure().getNodeTypes().length)) {
            // start generating actions (1, x)
            resTypeIndex = 0;
            delta = 1;
        }

        return new Action(actionIndex, delta, resTypeIndex);
    }
}
