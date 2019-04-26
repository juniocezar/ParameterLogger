/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lac.jinn;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import soot.ArrayType;

import soot.Body;
import soot.BodyTransformer;
import soot.DoubleType;
import soot.IntType;
import soot.SootClass;
import soot.SootMethod;
import soot.util.Chain;
import soot.SootField;
import soot.Local;
import soot.LongType;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.LengthExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.internal.JInstanceFieldRef;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;


/**
 *
 * @author juniocezar
 */
public class MethodProfiler extends BodyTransformer {
    static Set<SootMethod> visited = new HashSet<SootMethod>();
    Set<Value> globalsRead = new HashSet<Value>();
    
    @Override
    protected void internalTransform(Body body, String string, Map map) {
        SootMethod method = body.getMethod();
        List<Local> parameters  = body.getParameterLocals();
        Set<AssignStmt> globalsRead = findGlobalsRead(body);
                    
        // Logic: Build String: 'methodName, Pars: VAL1, VAL2, VAL3 - GLOBALS: VAL1, VAL2, VAL3'
        // #1 Read parameters and add to string, insert in the beggining
        // #2 Read Globals and add to string, insert after usage;
       
        instrumment(body, parameters, globalsRead);
        
        boolean isMainMethod = body.getMethod().getSubSignature()
                                               .equals("void main(java.lang.String[])");
        boolean isHarness = body.getMethod().getDeclaringClass().getName().
                                                contains("Harness");
        if (isMainMethod && isHarness)
            addDumper(body);
    }
    
    /**
     * Adds a call to a dump method in the external logging library. The library then logs
     * the data to a file (ideally). This method looks for the main method of the target
     * application.
     * @param body Body of the main method.
     */
    private void addDumper (Body body) {
        boolean isMainMethod = body.getMethod().getSubSignature()
                                               .equals("void main(java.lang.String[])");
        List<Unit> exitUnits = findExitUnits(body);
        Chain<Unit> units = body.getUnits();
        SootClass loggerClass  = Scene.v().getSootClass("lac.jinn.exlib.DataLogger");
        for (Unit exit : exitUnits) {
            Unit dump = Jimple.v().newInvokeStmt(
                        Jimple.v().newStaticInvokeExpr(loggerClass.getMethod(
                        "void dump()").makeRef()));
            if (isMainMethod) {
                units.insertBefore(dump, exit);
            } else
              if (exit instanceof Stmt && ((Stmt)exit).containsInvokeExpr()) {
                units.insertBefore(dump, exit);
            }
        }
    }
    
    private void instrumment (Body body, List<Local> pars, 
                                                    Set<AssignStmt> globals) {
        // build stringBuilder, and add the values to construct the output        
        Chain<Unit> units = body.getUnits();
        Unit inPoint = ((JimpleBody)body).getFirstNonIdentityStmt();
        List<Unit> newUnits = new ArrayList<Unit>();
        List<Unit> exitUnits = (new BriefUnitGraph(body)).getTails();
        
        Local var_builder = insertDeclaration("$r_str_builder", 
                                               "java.lang.StringBuilder", body);
        //
        // === method signature
        newUnits.add(Jimple.v().newAssignStmt(var_builder, 
                Jimple.v().newNewExpr(RefType.v("java.lang.StringBuilder"))));
                        
        newUnits.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(
                var_builder, Scene.v().getMethod(
                       "<java.lang.StringBuilder: void <init>()>").makeRef())));

        newUnits.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(
                var_builder, Scene.v().getMethod(
                "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>").
                makeRef(), StringConstant.v(body.getMethod().getSignature() +
                        " -- Pars: ["))));
        //
        // === get parameters
        int getterId = 0;
        for (Local par : pars) {
            // create a new String, get the String.valueOf(parameter) and pass it
            // to the new created string
            Local tmp = insertDeclaration("$r_str_" + par.getName(), 
                                               "java.lang.String", body);
            
            if (par.getType() instanceof ArrayType) {
                // get length value at the beginning of the method body
                Local getter = insertDeclaration("$getter_" +
                                                   getterId++, "int", body);
                LengthExpr lengthOf = Jimple.v().newLengthExpr(par);
                newUnits.add(Jimple.v().newAssignStmt(getter, lengthOf));
                newUnits.add(Jimple.v().newAssignStmt(tmp, Jimple.v().newStaticInvokeExpr(
                Scene.v().getMethod("<java.lang.String: java.lang.String valueOf(int)>"
                        ).makeRef(), getter)));     
            } else if (!(par.getType() instanceof PrimType)) {
                // Collections, get size
                Local getter = insertDeclaration("$getter_" +
                                                   getterId++, "int", body);
                InvokeExpr invokeExpr = null;
                SootClass sclass = ((RefType)par.getType()).getSootClass();
                if (sclass.isInterface()) {
                    invokeExpr = Jimple.v().newInterfaceInvokeExpr(par,
                               Scene.v().getMethod("<" + par.getType().toString() + ": "
                               + "int size()>").makeRef());
                } else {
                    invokeExpr = Jimple.v().newVirtualInvokeExpr(par,
                                Scene.v().getMethod("<" + par.getType().toString() + ": "
                                + "int size()>").makeRef());
                }
                newUnits.add(Jimple.v().newAssignStmt(getter, invokeExpr));
                newUnits.add(Jimple.v().newAssignStmt(tmp, Jimple.v().newStaticInvokeExpr(
                Scene.v().getMethod("<java.lang.String: java.lang.String valueOf(int)>"
                        ).makeRef(), getter)));                     
            } else {
                String ltype =  par.getType().toString();
                String t = (ltype.contains("java.")? 
                        "java.lang.Object" : ltype );
                newUnits.add(Jimple.v().newAssignStmt(tmp, Jimple.v().newStaticInvokeExpr(
                Scene.v().getMethod("<java.lang.String: java.lang.String valueOf("
                        + t + ")>").makeRef(), par)));            
            }
            
            newUnits.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(
                var_builder, Scene.v().getMethod(
                "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>").
                makeRef(), tmp)));
            
            newUnits.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(
                var_builder, Scene.v().getMethod(
                "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>").
                makeRef(), StringConstant.v(", "))));
        }
        newUnits.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(
                var_builder, Scene.v().getMethod(
                "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>").
                makeRef(), StringConstant.v("] Globals: [ "))));
               
        
        //
        // === get globals
        for (AssignStmt globalAssign : globals) {
            Value lvalue = globalAssign.getLeftOp();
            List<Unit> localUnits = new ArrayList<Unit>();
            Local tmp = insertDeclaration("$r_str_" + globalAssign.hashCode(),
                                               "java.lang.String", body);
            
            if (lvalue.getType() instanceof ArrayType) {
                // get length value at the beginning of the method body
                Local getter = insertDeclaration("$getter_" +
                                                   getterId++, "int", body);
                LengthExpr lengthOf = Jimple.v().newLengthExpr(lvalue);
                newUnits.add(Jimple.v().newAssignStmt(getter, lengthOf));
                newUnits.add(Jimple.v().newAssignStmt(tmp, Jimple.v().newStaticInvokeExpr(
                Scene.v().getMethod("<java.lang.String: java.lang.String valueOf(int)>"
                        ).makeRef(), getter)));  
            } else if (!(lvalue.getType() instanceof PrimType)) {
                // Collections, get size
                Local getter = insertDeclaration("$getter_" +
                                                   getterId++, "int", body);
                InvokeExpr invokeExpr = null;
                SootClass sclass = ((RefType)lvalue.getType()).getSootClass();
                if (sclass.isInterface()) {
                    invokeExpr = Jimple.v().newInterfaceInvokeExpr((Local)lvalue,
                               Scene.v().getMethod("<" + lvalue.getType().toString() + ": "
                               + "int size()>").makeRef());
                } else {
                    invokeExpr = Jimple.v().newVirtualInvokeExpr((Local)lvalue,
                                Scene.v().getMethod("<" + lvalue.getType().toString() + ": "
                                + "int size()>").makeRef());
                }
                newUnits.add(Jimple.v().newAssignStmt(getter, invokeExpr));
                newUnits.add(Jimple.v().newAssignStmt(tmp, Jimple.v().newStaticInvokeExpr(
                Scene.v().getMethod("<java.lang.String: java.lang.String valueOf(int)>"
                        ).makeRef(), getter)));   
            } else {
                String ltype =  lvalue.getType().toString();
                String t = (ltype.contains("java.")? 
                        "java.lang.Object" : ltype );
                localUnits.add(Jimple.v().newAssignStmt(tmp, Jimple.v().newStaticInvokeExpr(
                Scene.v().getMethod("<java.lang.String: java.lang.String valueOf("
                        + t + ")>").makeRef(), lvalue)));
            }            
            
            
            localUnits.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(
                var_builder, Scene.v().getMethod(
                "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>").
                makeRef(), tmp)));
            
            localUnits.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(
                var_builder, Scene.v().getMethod(
                "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>").
                makeRef(), StringConstant.v(", "))));
            //
            // we insert the reads to globals, right after they are stored into
            // locals
            units.insertAfter(localUnits, globalAssign);
        }
              
        //
        // we insert reads of all parameters in the begginig of method
        units.insertBefore(newUnits, inPoint);       
        //
        // we call the logger library with the constructed string
        SootClass loggerClass  = Scene.v().getSootClass("lac.jinn.exlib.DataLogger");
        for (Unit exit : exitUnits) {
            Unit dump = Jimple.v().newInvokeStmt(
                        Jimple.v().newStaticInvokeExpr(loggerClass.getMethod(
                        "void log(java.lang.StringBuilder)").makeRef(), 
                        var_builder));
            units.insertBefore(dump, exit);
        }        
    }
    
    /**
     * Method for inserting local declaration in sootMethod.
     * @param name Local ASCII name.
     * @param type Local type.
     * @param body Method's body.
     * @return Reference to the local, already inserted into the method's body.
     */
    private Local insertDeclaration(String name, String type, Body body) {
        Local tmp;
        switch(type) {
            case "long":
                tmp = Jimple.v().newLocal(name, LongType.v());
                break;
            case "int":
                tmp = Jimple.v().newLocal(name, IntType.v());
                break;
            case "double":
                tmp = Jimple.v().newLocal(name, DoubleType.v());
                break;
            default:
                tmp = Jimple.v().newLocal(name, RefType.v(type));
                break;
        }

        // check if we already have the dec in method
        Chain<Local> locals = body.getLocals();
        for (Local l : locals) {
            if(l.equals(tmp))
                return l;
        }

        locals.add(tmp);
        return tmp;
    }
    
    private String buildLoggerSignature (int numParameters, int numGlobals) {
        String signature = "";
        return signature;
    }
    
    private Set<AssignStmt> findGlobalsRead (Body body) {
        Set<AssignStmt> reads = new HashSet<AssignStmt>();
        Chain<Unit> units = body.getUnits();
        for (Unit u : units) {
            if (readGlobal(u)) {
                reads.add((AssignStmt)u);
            }
        }
        return reads;
    }
    
    private int checkGlobalReads (Body body) {
        int reads = 0;
        Chain<Unit> units = body.getUnits();
        for (Unit u : units) {
            if (readGlobal(u)) {
                reads++;
            }
        }        
        return reads;
    }
    
    private boolean readGlobal (Unit u) {
        if (u instanceof AssignStmt) {
            Value rvalue = ((AssignStmt) u).getRightOp();
            if (rvalue instanceof StaticFieldRef) {
                globalsRead.add(((AssignStmt) u).getLeftOp());
                return true;
            } else if (rvalue instanceof JInstanceFieldRef) {
                globalsRead.add(((AssignStmt) u).getLeftOp());
                return true;
            }            
        }
        return false;
    }
    
    /**
     * Get all exit points in the input body.
     * @param body Input body.
     * @return List of all exit points.
     */
    private List<Unit> findExitUnits (Body body) {
        // does not include calls to System.exit
        UnitGraph cfg = new  ExceptionalUnitGraph(body);
        List<Unit> exitNodes = new ArrayList<Unit>(cfg.getTails());
        // adding calls to System.exit()
        Chain<Unit> units = body.getUnits();
        for (Unit u : units) {
            if (u instanceof Stmt && ((Stmt)u).containsInvokeExpr()) {
                InvokeExpr invoke = ((Stmt)u).getInvokeExpr();
                SootMethod callee = invoke.getMethod();
                if (callee.getSignature().equals(
                    "<java.lang.System: void exit(int)>")) {
                    exitNodes.add(u);
                }
            }
        }
        return exitNodes;
    }
}
