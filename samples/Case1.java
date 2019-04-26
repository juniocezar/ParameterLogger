import java.util.*;
public class Case1 {
    static int global_value;    
    int global_value_instance = 50;    

    public static void main (String[] args) {
        method1_sample(args.length);

        global_value = 10 + args.length;

        method2_sample();

        Case1 c1 = new Case1();
        c1.method3_sample(10);

        List<Case1> sample = new ArrayList<Case1>();
        for (int i = 1; i < 8; i++) {
            sample.add(new Case1());
        }
        c1.method4_sample(sample);
    }

    private static boolean method1_sample (int length) {
        if (length > 10) return true; return false;
    }

    private static boolean method2_sample () {
        if (global_value > 10) return true; return false;
    }

    private boolean method3_sample (int value) {
        if (global_value_instance > value) return true; return false;
    }

    private boolean method4_sample (List<Case1> a) {
        if (a.contains(this)) return true; return false;
    }

}
