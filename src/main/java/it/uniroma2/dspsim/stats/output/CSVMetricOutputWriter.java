package it.uniroma2.dspsim.stats.output;

import it.uniroma2.dspsim.stats.metrics.Metric;
import it.uniroma2.dspsim.stats.samplers.MetricSampleInfo;

import java.io.*;

public class CSVMetricOutputWriter implements MetricOutputWriter {

    private String[] header;
    private boolean hasHeader;

    public CSVMetricOutputWriter() {
        this(null);
    }

    public CSVMetricOutputWriter(String[] header) {
        this.header = header;
        this.hasHeader = header != null;
    }

    @Override
    public void write(Metric metric, MetricSampleInfo metricSampleInfo, Object... args) {
        try {
            if (metric.getId().equals(metricSampleInfo.getMetricID())) {
                String filename = metricSampleInfo.getFilename();
                File file = new File(filename);
                if (!file.exists() && file.createNewFile() && this.hasHeader && this.header != null) {
                    writeHeader(filename);
                }
                if (header != null && args.length != header.length)
                    throw new IllegalArgumentException(
                            String.format(
                                    "args must have same length of header: %d\nYou must pass line values in args",
                                    header.length
                            ));
                String line = buildLine(args);
                writeLines(filename, true, line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeHeader(String filename) throws FileNotFoundException {
        String h = buildLine(this.header);
        writeLines(filename, true, h);
    }

    private void writeLines(String filename, boolean append, String... lines) throws FileNotFoundException {
        PrintWriter printWriter = getPrinter(filename, append);
        for (String line : lines) {
            print(printWriter, line);
        }
        closePrinter(printWriter);
    }

    private String buildLine(Object... objs) {
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

    private PrintWriter getPrinter(String filename, boolean append) throws FileNotFoundException {
        return new PrintWriter(new FileOutputStream(new File(filename), append));
    }

    private void print(PrintWriter printWriter, String line) {
        printWriter.println(line);
    }

    private void closePrinter(PrintWriter printWriter) {
        printWriter.flush();
        printWriter.close();
    }

}
