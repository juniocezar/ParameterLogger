package lac.jinn.exlib;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author juniocezar
 */
public class DataLogger {
    static List<StringBuilder> logs = new ArrayList<StringBuilder>();
    
    public static void log (StringBuilder msg) {
        logs.add(msg);
    }
    
    public static void dump () {
        try {
            BufferedWriter bw = new BufferedWriter(
                                            new FileWriter("runtime-log.txt"));            
            for (StringBuilder str : logs) {
                bw.write(str.toString() + "]\n");
            }
            logs.clear();
            bw.close();
        } catch (IOException e) {
            //
        }
    }
    
}
