package fql.backend.targetgraph;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import cmdline.CPAMain;
import cmdline.CPAchecker;
import cpa.common.LogManager;
import cpaplugin.CPAConfiguration;
import cpaplugin.MainCPAStatistics;
import cpaplugin.CPAConfiguration.InvalidCmdlineArgumentException;
import exceptions.CPAException;
import fql.frontend.ast.filter.Filter;
import fql.frontend.ast.filter.Function;
import fql.frontend.ast.filter.FunctionCall;
import fql.frontend.ast.filter.FunctionCalls;
import fql.frontend.ast.filter.FunctionEntry;
import fql.frontend.ast.filter.Identity;
import fql.frontend.ast.filter.Line;
import fql.frontend.ast.predicate.CIdentifier;
import fql.frontend.ast.predicate.NaturalNumber;
import fql.frontend.ast.predicate.Predicate;

public class TargetGraphTest {
  private String mConfig = "-config";
  private String mPropertiesFile = "test/config/simpleMustMayAnalysis.properties";
  
  @Test
  public void test_01() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    System.out.println(lTargetGraph);
  }
  
  @Test
  public void test_02() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/loop1.c";
        
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    System.out.println(lTargetGraph);
  }
  
  @Test
  public void test_03() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/uninitVars.cil.c";
        
    /*
     * Note: This analysis returns most of the time
     * bottom elements for the must analysis since
     * it can not handle pointers at the moment.
     */
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    System.out.println(lTargetGraph);
  }

  @Test
  public void test_04() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/uninitVars.cil.c";
        
    /*
     * Note: This analysis returns most of the time
     * bottom elements for the must analysis since
     * it can not handle pointers at the moment.
     */
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    TargetGraph lFilteredTargetGraph = TargetGraph.applyFunctionNameFilter(lTargetGraph, "func");
    
    System.out.println(lFilteredTargetGraph);
  }  
  
  @Test
  public void test_05() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/uninitVars.cil.c";
        
    /*
     * Note: This analysis returns most of the time
     * bottom elements for the must analysis since
     * it can not handle pointers at the moment.
     */
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    TargetGraph lFuncTargetGraph = TargetGraph.applyFunctionNameFilter(lTargetGraph, "func");
    TargetGraph lF2TargetGraph = TargetGraph.applyFunctionNameFilter(lTargetGraph, "f2");
    
    TargetGraph lUnionGraph = TargetGraph.applyUnionFilter(lFuncTargetGraph, lF2TargetGraph);
    
    System.out.println(lUnionGraph);
  }  
  
  @Test
  public void test_06() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/uninitVars.cil.c";
        
    /*
     * Note: This analysis returns most of the time
     * bottom elements for the must analysis since
     * it can not handle pointers at the moment.
     */
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    TargetGraph lFuncTargetGraph = TargetGraph.applyFunctionNameFilter(lTargetGraph, "func");
    
    TargetGraph lIntersectionGraph = TargetGraph.applyIntersectionFilter(lTargetGraph, lFuncTargetGraph);
    
    System.out.println(lIntersectionGraph);
  }  
  
  @Test
  public void test_07() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    TargetGraph lFuncTargetGraph = TargetGraph.applyFunctionNameFilter(lTargetGraph, "f");
    
    TargetGraph lMinusGraph = TargetGraph.applyMinusFilter(lTargetGraph, lFuncTargetGraph);
    
    System.out.println(lMinusGraph);
  }
  
  @Test
  public void test_08() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    TargetGraph lFuncTargetGraph = TargetGraph.applyFunctionNameFilter(lTargetGraph, "f");
    
    TargetGraph lPredicatedGraph = TargetGraph.applyPredication(lFuncTargetGraph, new Predicate(new CIdentifier("x"), Predicate.Comparison.LESS, new NaturalNumber(100)));
    
    System.out.println(lPredicatedGraph);
  }
  
  @Test
  public void test_09() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    TargetGraph lFilteredTargetGraph = lTargetGraph.apply(Identity.getInstance());
    
    // identity returns the (physically) same target graph
    assertTrue(lFilteredTargetGraph == lTargetGraph);
  }
  
  @Test
  public void test_10() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    Function lFunctionFilter = new Function("f");
    
    TargetGraph lFilteredTargetGraph1 = lTargetGraph.apply(lFunctionFilter);
    
    TargetGraph lFilteredTargetGraph2 = lTargetGraph.apply(lFunctionFilter);
    
    // caching should return in the same target graphs
    assertTrue(lFilteredTargetGraph1 == lFilteredTargetGraph2);
    
    Function lFunctionFilter2 = new Function("f");
    
    TargetGraph lFilteredTargetGraph3 = lTargetGraph.apply(lFunctionFilter2);
    
    // caching should also work with logically equals filters
    assertTrue(lFilteredTargetGraph1 == lFilteredTargetGraph3);
    
    Function lFunctionFilter3 = new Function("foo");
    
    TargetGraph lFilteredTargetGraph4 = lTargetGraph.apply(lFunctionFilter3);
    
    // a different function name filter should return in a different target graph
    assertFalse(lFilteredTargetGraph3.equals(lFilteredTargetGraph4));
  }
  
  @Test
  public void test_11() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/functionCall.c";
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    FunctionCall lFunctionCallFilter = new FunctionCall("f");
    
    TargetGraph lFilteredTargetGraph = lTargetGraph.apply(lFunctionCallFilter);
    
    System.out.println(lFilteredTargetGraph);
    
    // check caching
    assertTrue(lFilteredTargetGraph == lTargetGraph.apply(lFunctionCallFilter));
  }
  
  @Test
  public void test_12() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/uninitVars.cil.c";
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    FunctionCall lFunctionCallFilter = new FunctionCall("func");
    
    TargetGraph lFilteredTargetGraph = lTargetGraph.apply(lFunctionCallFilter);
    
    System.out.println(lFilteredTargetGraph);
    
    // check caching
    assertTrue(lFilteredTargetGraph == lTargetGraph.apply(lFunctionCallFilter));
  }
  
  @Test
  public void test_13() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/uninitVars.cil.c";
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    Filter lFunctionCallsFilter = FunctionCalls.getInstance();
    
    TargetGraph lFilteredTargetGraph = lTargetGraph.apply(lFunctionCallsFilter);
    
    System.out.println(lFilteredTargetGraph);
    
    // check caching
    assertTrue(lFilteredTargetGraph == lTargetGraph.apply(lFunctionCallsFilter));
  }
  
  @Test
  public void test_14() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/uninitVars.cil.c";
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    Filter lFunctionEntryFilter = new FunctionEntry("func");
    
    TargetGraph lFilteredTargetGraph = lTargetGraph.apply(lFunctionEntryFilter);
    
    System.out.println(lFilteredTargetGraph);
    
    // check caching
    assertTrue(lFilteredTargetGraph == lTargetGraph.apply(lFunctionEntryFilter));
  }
  
  @Test
  public void test_15() throws InvalidCmdlineArgumentException, IOException, CPAException {
    String[] lArguments = new String[3];
    
    lArguments[0] = mConfig;
    lArguments[1] = mPropertiesFile;
    lArguments[2] = "test/tests/single/uninitVars.cil.c";
    
    CPAConfiguration lConfiguration = new CPAConfiguration(lArguments);
    
    // necessary for LogManager
    CPAMain.cpaConfig = lConfiguration;
    
    LogManager lLogManager = LogManager.getInstance();
      
    MainCPAStatistics lStatistics = new MainCPAStatistics();
    
    CPAchecker lCPAchecker = new CPAchecker(lConfiguration, lLogManager, lStatistics);
    
    TargetGraph lTargetGraph = TargetGraph.createTargetGraphFromCFA(lCPAchecker.getMainFunction());
    
    Filter lLineFilter = new Line(102);
    
    TargetGraph lFilteredTargetGraph = lTargetGraph.apply(lLineFilter);
    
    System.out.println(lFilteredTargetGraph);
    
    // check caching
    assertTrue(lFilteredTargetGraph == lTargetGraph.apply(lLineFilter));
  }
  
}
