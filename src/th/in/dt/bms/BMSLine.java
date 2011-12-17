/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package th.in.dt.bms;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import org.open2jam.parser.Event;

/**
 * This class represents one or more line with the same
 * measure and channel in a BMS chart.
 * @author dttvb
 */
public class BMSLine {
    
    /** the resolution */
    static final int MAGIC = 192;
    
    /**
     * returns the BMS position inside the measure for an
     * open2jam event.
     * @param e an open2jam event
     * @return the BMS position
     */
    public static int position(Event e) {
        return (int)Math.round(MAGIC * e.getPosition());
    }
    
    /** maps the position to a list of objects */
    HashMap<Integer,ArrayList<String>> data = new HashMap();

    /** number of lines to be written (max size of each array) */
    int maxSize = 0;
    
    /** the gcd of all positions */
    int iterateBy = MAGIC;
    
    /**
     * finds greatest common divisor of a and b.
     * @param a a
     * @param b b
     * @return the greatest common divisor of a and b
     */
    private int gcd(int a, int b) {
        if (b > a) return gcd(b, a);
        if (b == 0) return a;
        return gcd(b, a % b);
    }
    
    /**
     * adds an open2jam event to this line, with a BMS value maker
     * @param e the open2jam event
     * @param vm the BMS value maker
     */
    public void addEvent(Event e, BMSValueMaker vm) {
        int pos = position(e);
        iterateBy = gcd(iterateBy, pos);
        ArrayList<String> place;
        if (data.containsKey(pos)) {
            place = data.get(pos);
        } else {
            place = new ArrayList();
            data.put(pos, place);
        }
        if (e.getChannel() == Event.Channel.BPM_CHANGE) {
            place.add(vm.addBPMChange(e.getValue()));
        } else {
            place.add(vm.addNote(e.getValue()));
        }
        maxSize = Math.max(maxSize, place.size());
    }
    
    /**
     * Writes the current line to an output stream.
     * @param outputStream the output stream to write to
     * @param lineType the measure and channel identifier
     * @throws IOException 
     */
    public void writeTo(OutputStream outputStream, String lineType) throws IOException {
        for (int i = 0; i < maxSize; i ++) {
            String lineData = "";
            for (int j = 0; j < MAGIC; j += iterateBy) {
                String objectData = "00";
                if (data.containsKey(j)) {
                    ArrayList<String> list = data.get(j);
                    if (i < list.size()) {
                        objectData = list.get(i);
                    }
                }
                lineData += objectData;
            }
            String line = lineType + ":" + lineData + "\n";
            outputStream.write(line.getBytes());
        }
    }
    
}
