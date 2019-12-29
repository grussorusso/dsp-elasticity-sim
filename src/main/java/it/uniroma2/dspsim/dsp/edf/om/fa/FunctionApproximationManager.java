package it.uniroma2.dspsim.dsp.edf.om.fa;

import it.uniroma2.dspsim.dsp.edf.om.fa.features.Feature;
import it.uniroma2.dspsim.dsp.edf.om.fa.tiling.Tiling;

import java.util.ArrayList;
import java.util.List;

public class FunctionApproximationManager {

    private List<Feature> features;

    public FunctionApproximationManager() {
        this.features = new ArrayList<>();
    }

    public void addFeature(Feature feature) {
        this.features.add(feature);
    }

    public void removeFeature(Feature feature) {
        this.features.remove(feature);
    }

    public Feature removeFeature(int position) {
        return this.features.remove(position);
    }
}