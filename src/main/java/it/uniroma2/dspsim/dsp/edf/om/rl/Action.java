package it.uniroma2.dspsim.dsp.edf.om.rl;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.infrastructure.NodeType;

import java.util.Objects;

public class Action extends AbstractAction {
    private int delta;
    private int resTypeIndex;

    public Action (int delta, int resTypeIndex) {
        this.delta = delta;
        this.resTypeIndex = resTypeIndex;
    }

    public boolean isValidInState (State s, Operator o) {
        if (delta == 0)
            return true;
        if (delta < 0) {
            if (s.getK()[resTypeIndex] + delta < 0)
                return false;
        }

        return s.overallParallelism() + delta >= 1 && s.overallParallelism() + delta <= o.getMaxParallelism();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Action)) return false;
        Action action = (Action) o;
        return getDelta() == action.getDelta() &&
                getResTypeIndex() == action.getResTypeIndex();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDelta(), getResTypeIndex());
    }

    public int getDelta() {
        return delta;
    }

    public int getResTypeIndex() {
        return resTypeIndex;
    }
}
