package edu.brown.benchmark.airline.util;

import java.io.BufferedReader;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.brown.benchmark.airline.AirlineConstants;
import edu.brown.statistics.Histogram;
import edu.brown.utils.FileUtil;

public abstract class HistogramUtil {
    private static final Logger LOG = Logger.getLogger(HistogramUtil.class.getName());

    private static final Pattern p = Pattern.compile("\\|");
    
    public static Map<String, Histogram> loadAirportFlights(String data_path) throws Exception {
        Map<String, Histogram> m = new TreeMap<String, Histogram>();
        
        Histogram h = new Histogram();
        String filename = data_path + File.separator + "histogram." + AirlineConstants.HISTOGRAM_FLIGHTS_PER_AIRPORT.toLowerCase() + ".csv";
        if (FileUtil.exists(filename) == false) filename += ".gz";
        h.load(filename, null);
        
        Pattern pattern = Pattern.compile("-");
        Set<String> values = h.values();
        for (String value : values) {
            String split[] = pattern.split(value);
            Histogram src_h = m.get(split[0]);
            if (src_h == null) {
                src_h = new Histogram();
                m.put(split[0], src_h);
            }
            src_h.put(split[1], h.get(value));
        } // FOR
        
        return (m);
    }
    
    /**
     * Construct a histogram from an airline-benchmark data file
     * @param name
     * @param data_path
     * @param has_header
     * @return
     * @throws Exception
     */
    public static Histogram loadHistogram(String name, String data_path, boolean has_header) throws Exception {
        String filename = data_path + File.separator + "histogram." + name.toLowerCase() + ".csv";
        if (!(new File(filename)).exists()) filename += ".gz";
        
        Histogram histogram = new Histogram();
        BufferedReader reader = FileUtil.getReader(filename);
        boolean first = true;
        int ctr = -1;
        while (reader.ready()) {
            ctr++;
            String line = reader.readLine();
            if (first && has_header) {
                first = false;
                continue;
            }
            if (line.isEmpty()) continue;
            
            String data[] = p.split(line);
            if (data.length != 2) {
                LOG.warn("Unexpected data on line " + ctr + " in '" + name + "'");
            } else {
                try {
                    String key = data[0];
                    Integer value = Integer.valueOf(data[1].trim());
                    histogram.put(key, value);
                } catch (Exception ex) {
                    LOG.error("Failed to parse data on line " + ctr + " in '" + name + "'", ex);
                }
            }
        } // WHILE
        
        return (histogram);
    }
}
