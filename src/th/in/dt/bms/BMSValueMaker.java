/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package th.in.dt.bms;

/**
 * This interface allows the line to know how to make a BMS
 * object string.
 * @author dttvb
 */
public interface BMSValueMaker {
    
    /**
     * Converts a BPM to BPM reference.
     * @param value the BPM
     * @return the reference for the specified BPM
     */
    public String addBPMChange(double value);
    
    /**
     * Converts the note value to BMS keysound reference reference.
     * @param value the note value
     * @return 
     */
    public String addNote(double value);
    
}
