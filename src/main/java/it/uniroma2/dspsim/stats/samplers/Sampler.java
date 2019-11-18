package it.uniroma2.dspsim.stats.samplers;

import it.uniroma2.dspsim.stats.metrics.Metric;
import it.uniroma2.dspsim.utils.KeyValueStorage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public abstract class Sampler {
    private String id;

    private KeyValueStorage<String, MetricSampleInfo> metricSampleInfo;

    public Sampler(String id) {
        this.id = id;
        this.metricSampleInfo = new KeyValueStorage<>();
    }

    // TODO improve this method
    protected void dump(double step, Metric m) {
        try {
            String filename = this.metricSampleInfo.getValue(m.getId()).getFilename();
            File file = new File(filename);
            if(!file.exists()) {
                if (file.createNewFile()) {
                    PrintWriter printWriter = new PrintWriter(new FileOutputStream(new File(filename), true));
                    printLineOnFile(printWriter, String.format("%s,%s", "Days", "Value"), true);
                }
            }
            PrintWriter printWriter = new PrintWriter(new FileOutputStream(new File(filename), true));
            printLineOnFile(printWriter, String.format("%f,%s", step, m.dumpValue()), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printLineOnFile(PrintWriter printWriter, String line, boolean closePW) {
        printWriter.println(line);
        printWriter.flush();
        if (closePW)
            printWriter.close();
    }

    public abstract void addMetricSampleInfo(Metric metric);

    public abstract void sample(Metric metric, long simulationTime);

    public String getId() {
        return id;
    }

    public KeyValueStorage<String, MetricSampleInfo> getMetricSampleInfo() {
        return metricSampleInfo;
    }
}
