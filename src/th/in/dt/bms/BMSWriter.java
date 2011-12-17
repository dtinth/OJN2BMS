/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package th.in.dt.bms;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.open2jam.parser.Event;

/**
 * This class writes the BMS object notes from open2jam events.
 * It does not write any metadata.
 * 
 * @author dttvb
 */
public class BMSWriter implements BMSValueMaker {
    
    /**
     * Transforms open2jam channel to BMS channel.
     * @param channel the channel to transform
     * @return the resulting BMS channel, or 0 if not applicable
     */
    public static int getBMSChannel(Event.Channel channel) {
        if (channel == Event.Channel.NOTE_1) return 11;
        if (channel == Event.Channel.NOTE_2) return 12;
        if (channel == Event.Channel.NOTE_3) return 13;
        if (channel == Event.Channel.NOTE_4) return 14;
        if (channel == Event.Channel.NOTE_5) return 15;
        if (channel == Event.Channel.NOTE_6) return 18;
        if (channel == Event.Channel.NOTE_7) return 19;
        if (channel == Event.Channel.TIME_SIGNATURE) return 2;
        if (channel == Event.Channel.BPM_CHANGE) return 8;
        if (channel == Event.Channel.AUTO_PLAY) return 1;
        return 0;
    }
    
    /**
     * Creates the line identifier for an open2jam event.
     * @param event the open2jam event
     * @return the line identifier
     */
    public static String getLine(Event event) {
        int channel = getBMSChannel(event.getChannel());
        if (channel == 0) return null;
        if (event.getFlag() == Event.Flag.HOLD || event.getFlag() == Event.Flag.RELEASE) {
            channel += 40;
        }
        String line = String.format("#%03d%02d", event.getMeasure(), channel);
        return line;
    }
    
    /** maps the line identifier to BMS line */
    TreeMap<String,BMSLine> lineMap = new TreeMap();
    
    /** maps the BPM value to BMS bpm reference */
    HashMap<Double,String> bpmMap = new HashMap();
    
    /** list of all BPM references */
    ArrayList<Double> bpmList = new ArrayList();
    
    /**
     * Adds an open2jam event to the chart.
     * @param event the open2jam event
     */
    public void addEvent(Event event) {
        if (event.getChannel() == Event.Channel.TIME_SIGNATURE) {
            return;
        }
        String bmsLine = getLine(event);
        if (bmsLine == null) {
            return;
        }
        BMSLine line;
        if (lineMap.containsKey(bmsLine)) {
            line = lineMap.get(bmsLine);
        } else {
            line = new BMSLine();
            lineMap.put(bmsLine, line);
        }
        line.addEvent(event, this);
    }
    
    /**
     * Writes the added BMS objects to the output stream.
     * @param outputStream the output stream to write to
     * @throws IOException 
     */
    public void writeTo(OutputStream outputStream) throws IOException {
        int i = 0;
        for (double bpm : bpmList) {
            outputStream.write(String.format("#BPM%s %f\n", itemIndexToBMSObjectString(i++), bpm).getBytes());
        }
        for (Map.Entry<String,BMSLine> entry : lineMap.entrySet()) {
            entry.getValue().writeTo(outputStream, entry.getKey());
        }
    }

    /**
     * Converts a number to BMS style base36, with maximum of 2 digits.
     * @param number the number to convert
     * @return the resulting base36 number
     */
    public static String toBase36(int number) {
        String v = Integer.toString(number, 36).toUpperCase();
        return v.length() < 2 ? "0" + v : v.substring(v.length() - 2);
    }
    
    /**
     * Converts an item index to BMS object value.
     * @param index the object index
     * @return the resulting base36 number
     */
    private static String itemIndexToBMSObjectString(int index) {
        return toBase36(index + 1);
    }
    
    /**
     * Converts a BPM to BPM reference.
     * @param value the BPM
     * @return the reference for the specified BPM
     */
    @Override
    public String addBPMChange(double value) {
        if (bpmMap.containsKey(value)) {
            return bpmMap.get(value);
        }
        String objectString = itemIndexToBMSObjectString(bpmList.size());
        bpmList.add(value);
        bpmMap.put(value, objectString);
        return objectString;
    }

    /**
     * Converts the note value to BMS keysound reference reference.
     * @param value the note value
     * @return 
     */
    @Override
    public String addNote(double value) {
        return toBase36((int)Math.round(value));
    }
    
    
}
