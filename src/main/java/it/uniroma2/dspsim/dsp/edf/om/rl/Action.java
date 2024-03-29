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
        if (this.delta == 0)
            return 0;
        int h = 1 + 2 * this.resTypeIndex;

        if (this.delta < 0)
            h++;

        return h;
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

    @Override
    public String toString() {
        return dump();
    }

    /**
     * Dump action
     */
    public String dump() {
        if (delta == 0) {
            return " ~ ";
        } else if (delta > 0) {
            return String.format("+%d%c", delta, resTypeIndex+'a');
        } else {
            return String.format("-%d%c", -delta, resTypeIndex + 'a');
        }
    }
}
