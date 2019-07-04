package it.uniroma2.dspsim;

import java.io.*;

public class InputRateFileReader {

	private BufferedReader fileReader;

	public InputRateFileReader (String filename) throws FileNotFoundException {
		fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
	}

	public boolean hasNext() throws IOException {
		return fileReader.ready();
	}

	public double next() throws IOException {
        String line = fileReader.readLine();
        return Double.parseDouble(line);
	}

}
