package it.uniroma2.dspsim.dsp.edf.om.rl;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

import java.util.Objects;

public class Action extends AbstractAction {
    private int delta;
    private int resTypeIndex;
    private int index;

    public Action (int index, int delta, int resTypeIndex) {
        this.index = index;
        this.delta = delta;
        this.resTypeIndex = resTypeIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Action)) return false;
        Action action = (Action) o;
        return getDelta() == action.getDelta() &&
                getResTypeIndex() == action.getResTypeIndex() &&
                getIndex() == action.getIndex();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDelta(), getResTypeIndex(), getIndex());
    }

    public int getIndex() {
        return index;
    }

    public int getDelta() {
        return delta;
    }

    public int getResTypeIndex() {
        return resTypeIndex;
    }

    /**
     * Dump action
     */
    public String dump() {
        return String.format("A(%d,%d)", delta, resTypeIndex);
    }
}
