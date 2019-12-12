package it.uniroma2.dspsim.stats.output;

import it.uniroma2.dspsim.stats.metrics.Metric;
import it.uniroma2.dspsim.stats.samplers.MetricSampleInfo;

import java.io.*;

public class CSVMetricOutputWriter extends FileMetricOutputWriter {

    private String[] header;
    private boolean hasHeader;

    public CSVMetricOutputWriter(boolean appendMode, String... header) {
        super(appendMode);
        this.header = header;
        this.hasHeader = header != null;
    }

    public CSVMetricOutputWriter() {
        this(false, null);
    }

    @Override
    protected void fileCreationCallback(String filename, Metric metric, MetricSampleInfo metricSampleInfo, Object... args) {
        if (this.hasHeader) {
            try {
                writeHeader(filename);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected boolean shouldWrite(Metric metric, MetricSampleInfo metricSampleInfo, Object... args) {
        if (this.hasHeader && args.length != header.length)
            throw new IllegalArgumentException(
                    String.format(
                            "args must have same length of header: %d\nYou must pass line values in args",
                            header.length
                    ));
        else return true;
    }

    @Override
    protected String buildLine(Object... objs) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < objs.length; i++) {
            if (!(objs[i] instanceof Number) && !(objs[i] instanceof String))
                throw new IllegalArgumentException(String.format("args[%d] must be a number or string", i));
            line.append(objs[i]);
            if (i < objs.length - 1) {
                line.append(",");
            }
        }
        return line.toString();
    }

    private void writeHeader(String filename) throws FileNotFoundException {
        String h = buildLine(this.header);
        writeLines(filename, h);
    }
}
