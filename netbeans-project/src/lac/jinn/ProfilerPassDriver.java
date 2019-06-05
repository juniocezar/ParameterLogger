/*
 * Copyright (C) 2019 juniocezar.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package lac.jinn;

import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.Transform;
import soot.options.Options;

/**
 *
 * @author juniocezar
 */
public class ProfilerPassDriver {
    /*
     * This method configures soot, add the necessary passes to the pack manager
     * and initializes all pipeline.
     * @param args
     */
    public static void main (String[] args) {
        try{
        //
        // Configure soot by setting general options
        configureSoot();
        MethodProfiler mp = new MethodProfiler();
        //
        // Adding our pass to Soot Pass/Package Manager;
        PackManager.v().getPack("jtp").add(new Transform("jtp.aanalyzer", mp));
        //
        // Resolving name of our external logging library
        Scene.v().addBasicClass("lac.jinn.exlib.DataLogger", SootClass.SIGNATURES);        
        Scene.v().addBasicClass("java.lang.StringBuilder", SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.String", SootClass.SIGNATURES);
        //Scene.v().loadClassAndSupport("lac.jinn.exlib.DataLogger");

        //
        // Running Soot        
        soot.Main.main(args);
        } catch (Exception e) {
            System.out.println("Aleluia");
            System.exit(1);
        } 
    }


    private static void configureSoot () {
        Options.v().set_no_bodies_for_excluded(true);
        // Set via command line
        Options.v().set_whole_program(true);
        //Options.v().set_process_dir(Arrays.asList(targetDir));
        Options.v().set_verbose(false);
        Options.v().set_src_prec(Options.src_prec_class);
        Options.v().set_output_format(Options.output_format_class);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_xml_attributes(false);
        Options.v().set_force_overwrite(true);
        Options.v().setPhaseOption("jb","use-original-names:true");
        Options.v().setPhaseOption("jb","preserve-source-annotations:true");
    }
}
