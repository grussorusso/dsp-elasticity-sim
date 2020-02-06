package it.uniroma2.dspsim.stats.output;

import it.uniroma2.dspsim.stats.metrics.Metric;
import it.uniroma2.dspsim.stats.samplers.MetricSampleInfo;
import org.apache.commons.io.FileUtils;

import java.io.*;

public abstract class FileMetricOutputWriter implements MetricOutputWriter {
    private boolean appendMode;

    public FileMetricOutputWriter(boolean appendMode) {
        this.appendMode = appendMode;
    }

    @Override
    public void write(Metric metric, MetricSampleInfo metricSampleInfo, Object... args) {
        try {
            if (metric.getId().equals(metricSampleInfo.getMetricID())) {
                String filename = metricSampleInfo.getFilename();
                File file = new File(filename);
                if (!file.exists() && (file.getParentFile().mkdirs() || file.createNewFile())) {
                    fileCreationCallback(filename, metric, metricSampleInfo, args);
                }
                if (shouldWrite(metric, metricSampleInfo, args)) {
                    String line = buildLine(args);
                    writeLines(filename, line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void writeLines(String filename, String... lines) throws FileNotFoundException {
        PrintWriter printWriter = getPrinter(filename);
        for (String line : lines) {
            print(printWriter, line);
        }
        closePrinter(printWriter);
    }

    protected PrintWriter getPrinter(String filename) throws FileNotFoundException {
        return new PrintWriter(new FileOutputStream(new File(filename), this.appendMode));
    }

    protected void print(PrintWriter printWriter, String line) {
        printWriter.println(line);
    }

    protected void closePrinter(PrintWriter printWriter) {
        printWriter.flush();
        printWriter.close();
    }

    /**
     * ABSTRACT METHODS
     */
    protected abstract String buildLine(Object... args);

    protected abstract boolean shouldWrite(Metric metric, MetricSampleInfo metricSampleInfo, Object... args);

    protected abstract void fileCreationCallback(String filename, Metric metric, MetricSampleInfo metricSampleInfo, Object... args);
}
