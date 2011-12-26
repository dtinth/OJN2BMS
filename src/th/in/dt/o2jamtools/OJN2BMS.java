/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package th.in.dt.o2jamtools;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import th.in.dt.bms.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.open2jam.parser.Chart;
import org.open2jam.parser.OJNChart;
import org.open2jam.parser.ChartList;
import org.open2jam.parser.ChartParser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.open2jam.parser.Event;
import org.open2jam.parser.SampleHandler;

/**
 * This is the main class.
 * @author dttvb
 */
public class OJN2BMS {
    
    /**
     * An implementation of open2jam sample handler that
     * writes to files the OGG samples. It also keeps track of
     * the files to that the keysound index can be resolved to
     * the file name.
     */
    class MySampleHandler implements SampleHandler {

        private HashMap<String,Integer> mapFilenameToKeysoundIndex = new HashMap();
        private HashMap<Integer,String> mapKeysoundIndexToFilename = new HashMap();
        private int numKeysounds = 0;
        private File parent;
        
        /**
         * Constructs a sample handler with the destination directory
         * to write .ogg files to.
         * @param parent ogg output directory
         */
        public MySampleHandler(File parent) {
            this.parent = parent;
        }
        
        /**
         * Returns the keysound filename for a given keysound index.
         * @param index the keysound index
         * @return the filename
         */
        public String getKeysoundFilename(int index) {
            return mapKeysoundIndexToFilename.get(index);
        }
        
        private int keysound(String filename) {
            if (mapFilenameToKeysoundIndex.containsKey(filename)) {
                return mapFilenameToKeysoundIndex.get(filename);
            }
            mapKeysoundIndexToFilename.put(numKeysounds, filename);
            mapFilenameToKeysoundIndex.put(filename, numKeysounds);
            return numKeysounds++;
        }
    
        @Override
        public int handlePCMSample(int sample_id, byte[] name, ByteBuffer buffer, short bits_per_sample, short num_channels, int sample_rate) throws IOException {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public int handleOGGSample(int sample_id, byte[] name, InputStream inputStream) throws IOException {
            String basename = new String(name);
            int nullChar = basename.indexOf("\0");
            if (nullChar > -1) {
                basename = basename.substring(0, nullChar);
                String newBasename = "";
                for (int i = 0; i < basename.length(); i ++) {
                    if (basename.charAt(i) < '\u007f') {
                        newBasename += basename.charAt(i);
                    } else {
                        newBasename += "_" + String.format("%02x", (int)basename.charAt(i));
                    }
                }
                basename = newBasename;
            }
            String filename = sample_id + "_" + basename + ".ogg";
            if (!mapFilenameToKeysoundIndex.containsKey(filename)) {
                File newFile = new File(parent, filename);
                if (!newFile.exists()) {
                    FileOutputStream outputStream = new FileOutputStream(newFile);
                    System.out.println(newFile);
                    byte[] b = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(b)) > 0) {
                        outputStream.write(b, 0, bytesRead);
                    }
                    outputStream.close();
                    inputStream.close();
                }
            }
            return keysound(filename);
        }
       
    }
    
    /**
     * The main entry. The first arguments refers to the .ojn file
     * and the second argument refers to the output directory, if
     * present.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        OJN2BMS converter = new OJN2BMS();
        File file = new File(args[0]);
        try {
            File output = new File(file.getParentFile(), file.getName() + "-out");
            if (args.length >= 2) output = new File(args[1]);
            converter.convert(file, output);
        } catch (IOException ex) {
            Logger.getLogger(OJN2BMS.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static final String[] DIFFICULTY_NAMES = { "Easy", "Normal", "Hard" };
    
    private boolean convert(File file, File output) throws IOException {
        ChartList list = ChartParser.parseFile(file);
        MySampleHandler sampleHandler = new MySampleHandler(output);
        if (!output.exists()) {
            if (!output.mkdirs()) return false;
        }
        if (!output.isDirectory()) return false;
        int index = 0;
        boolean imageWritten = false;
        for (Chart chart : list) {
            if (!imageWritten) {
                BufferedImage image = chart.getCover();
                if (image != null) {
                    File img = new File(output, file.getName() + ".png");
                    ImageIO.write(image, "png", img);
                    imageWritten = true;
                }
            }
            String difficultyName = index < DIFFICULTY_NAMES.length ? DIFFICULTY_NAMES[index] : String.format("%d", index);
            File bms = new File(output, file.getName() + " [" + difficultyName + "].bms");
            System.out.println("Writing to: " + bms.getName());
            OutputStream out = new FileOutputStream(bms);
            out.write(String.format("#PLAYER 1\n").getBytes());
            out.write(String.format("#BPM %f\n", chart.getBPM()).getBytes());
            out.write(String.format("#TITLE %s\n", chart.getTitle()).getBytes());
            out.write(String.format("#ARTIST %s / %s\n", chart.getArtist(), chart.getNoter()).getBytes());
            out.write(String.format("#PLAYLEVEL %d\n", chart.getLevel()).getBytes());
            out.write(String.format("#LNTYPE 1\n", chart.getLevel()).getBytes());
            if (chart instanceof OJNChart) {
                OJNChart ojn = (OJNChart)chart;
                writeSamples(ojn.getSamples(sampleHandler), sampleHandler, out);
            }
            writeChart(chart, out);
            index++;
        }
        return true;
    }
    
    private boolean writeSamples(Map<Integer,Integer> sampleMap, MySampleHandler sampleHandler, OutputStream outputStream) throws IOException {
        for (Map.Entry<Integer,Integer> entry : sampleMap.entrySet()) {
            String line = "#WAV" + BMSWriter.toBase36(entry.getKey() + 1) + " " + sampleHandler.getKeysoundFilename(entry.getValue()) + "\n";
            outputStream.write(line.getBytes());
        }
        return true;
    }
    
    private boolean writeChart(Chart chart, OutputStream outputStream) throws IOException {
        BMSWriter w = new BMSWriter();
        for (Event event : chart.getEvents()) {
            w.addEvent(event);
        }
        w.writeTo(outputStream);
        return true;
    }
    
}
