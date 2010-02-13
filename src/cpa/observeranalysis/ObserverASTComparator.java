package cpa.observeranalysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.eclipse.cdt.core.dom.IASTServiceProvider;
import org.eclipse.cdt.core.dom.ICodeReaderFactory;
import org.eclipse.cdt.core.dom.IParserConfiguration;
import org.eclipse.cdt.core.dom.IASTServiceProvider.UnsupportedDialectException;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.internal.core.dom.InternalASTServiceProvider;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTProblem;
import org.eclipse.core.resources.IFile;

import cmdline.stubs.StubFile;

/**
 * Provides methods for generating, comparing and printing the ASTs generated from String.
 * The ASTs are generated by the Eclipse CDT IDE plugin.
 * @author rhein
 */
@SuppressWarnings("restriction")
public class ObserverASTComparator {
  
  /**
   * Temporary file from which the AST is generated.
   */
  private static final String TEMP_FILE = "src/cpa/observeranalysis/tmp.txt";
  /**
   * Every occurrence of the joker expression $? in the pattern is substituted by JOKER_EXPR.
   * This is necessary because the C-parser cannot parse the pattern if it contains Dollar-Symbols.
   * The JOKER_EXPR must be a valid C-Identifier. It will be used to recognize the jokers in the generated AST.   
   */
  private static final String JOKER_EXPR = " CPAChecker_ObserverAnalysis_JokerExpression ";

  /**
   * Returns whether the ASTs for the argument strings are considered equal.
   * Substitutes wildcard expressions in pattern, generates the 2 ASTs and compares them.
   * 
   * The strings can be any C-Statement/Expression that may appear inside of a block.
   * @param pSourceExpression
   * @param pPattern
   * @return
   */
  static boolean generateAndCompareASTs(String pSourceExpression, String pPattern) {
    IFile file = ObserverASTComparator.writeToIFile(TEMP_FILE, addFunctionDeclaration(pSourceExpression));
    IASTTranslationUnit a = ObserverASTComparator.parse(file);
    
    String tmp = addFunctionDeclaration(pPattern).replaceAll("\\$\\?", JOKER_EXPR);
    file = ObserverASTComparator.writeToIFile(TEMP_FILE, tmp);
    IASTTranslationUnit b = ObserverASTComparator.parse(file);
    
    boolean result = ObserverASTComparator.compareASTs(a, b);
    return result;
  }

  public static boolean generateAndCompareASTs(String pSourceExpression,
      IASTTranslationUnit pPatternAST) {
    IFile file = ObserverASTComparator.writeToIFile(TEMP_FILE, addFunctionDeclaration(pSourceExpression));
    IASTTranslationUnit a = ObserverASTComparator.parse(file);
        
    boolean result = ObserverASTComparator.compareASTs(a, pPatternAST);
    return result;
  }

  static IASTTranslationUnit generatePatternAST(String pPattern) {
    String tmp = addFunctionDeclaration(pPattern).replaceAll("\\$\\?", JOKER_EXPR);
    IFile file = ObserverASTComparator.writeToIFile(TEMP_FILE, tmp);
    return ObserverASTComparator.parse(file);
  }
  
  /**
   * Returns the Problem Message if this AST has a problem node.
   * Returns null otherwise.
   * @param pAST
   * @return
   */
  static String ASTcontatinsProblems(IASTNode pAST) {
    if (pAST instanceof CASTProblem) {
      return ((CASTProblem)pAST).getMessage();
    } else {
      String problem;
      for (IASTNode n : pAST.getChildren()) {
        problem = ASTcontatinsProblems(n);
          if (problem != null) {
            return problem;
        }
      }
    }
    return null;
  }
  
  static void printAST(String pPattern) {
    printAST(generatePatternAST(pPattern), 0);
  }
  
  
  /**
   * Surrounds the argument with a function declaration. 
   * This is necessary so the string can be parsed by the CDT parser. 
   * @param pBody
   * @return "void test() { " + body + ";}";
   */
  private static String addFunctionDeclaration(String pBody) {
    if (pBody.trim().endsWith(";")) {
      return "void test() { " + pBody + "}";
    } else {
      return "void test() { " + pBody + ";}";
    }
      
  }
  
  
  /** Recursive method for comparing the ASTs.
   */
  private static boolean compareASTs(IASTNode pA, IASTNode pB) {
    boolean result = true;
    if (isJoker(pA) || isJoker(pB)) result = true;
    else if (pA.getClass().equals(pB.getClass())) {
      if (pA instanceof IASTName && ! IASTNamesAreEqual((IASTName)pA, (IASTName)pB)) {
        result = false;
      } else if (pA instanceof IASTLiteralExpression && ! IASTLiteralExpressionsAreEqual((IASTLiteralExpression)pA, (IASTLiteralExpression)pB)) {
        result = false;
      } else if (pA.getChildren().length != pB.getChildren().length) {
        result = false;
      } else {
        for (int i = 0; i < pA.getChildren().length; i++) {
          if (compareASTs(pA.getChildren()[i], pB.getChildren()[i]) == false)
            result = false;
          }
      }
    } else {
      result = false;
    }
    return result;
  }

  /** Recursive method for printing an AST to System.out .
   */
  private static void printAST (IASTNode pNode, int pInd) {
    String x = "";
    for (int i = 0; i<pInd; i++) {
      x = x + "  ";
    }
    x = x + pNode.getClass().getName() + " "+ pNode.getRawSignature();
    System.out.println(x);
    for (IASTNode n : pNode.getChildren()) {
      printAST(n, pInd +1);
    }
  }

  private static boolean isJoker(IASTNode pNode) {
    if (pNode instanceof IASTName) {
      IASTName name = (IASTName) pNode;
      return String.copyValueOf(name.getSimpleID()).equals(JOKER_EXPR.trim());
      // are there more IASTsomethings that could be Jokers?
    } else if (pNode instanceof IASTName) {
      IASTName name = (IASTName) pNode;
      return String.copyValueOf(name.getSimpleID()).equals(JOKER_EXPR.trim());
    } else if (pNode instanceof IASTIdExpression) {
      IASTIdExpression name = (IASTIdExpression) pNode;
      return name.getRawSignature().equals(JOKER_EXPR.trim());
    } else return false;
  }

  private static boolean IASTNamesAreEqual(IASTName pA, IASTName pB) {
   return String.copyValueOf(pA.getSimpleID()).equals(String.copyValueOf(pB.getSimpleID()));
  }
  
  private static boolean IASTLiteralExpressionsAreEqual(IASTLiteralExpression pA, IASTLiteralExpression pB) {
    return String.copyValueOf(pA.getValue()).equals(String.copyValueOf(pB.getValue()));
   }

  /** Needed for AST generation
   */
  private static IFile writeToIFile(String filename, String text) {
    try {
      FileWriter fw = new FileWriter(new File(filename));
      fw.write(text);
      fw.flush();
      fw.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return new StubFile(filename); 
  }

  /**
   * Parse the content of a file into an AST with the Eclipse CDT parser.
   * If an error occurs, the program is halted.
   * 
   * @param fileName  The file to parse.
   * @return The AST.
   */
  private static IASTTranslationUnit parse(IFile pFile) {
    IASTServiceProvider p = new InternalASTServiceProvider();
    
    ICodeReaderFactory codeReaderFactory = null;
    try {
       codeReaderFactory = createCodeReaderFactory();
    } catch (ClassNotFoundException e) {
      System.err.println("ClassNotFoundException:" +
          "Missing implementation of ICodeReaderFactory, check your CDT version!");
      System.exit(1);
    }
    
    IASTTranslationUnit ast = null;
    try {
      ast = p.getTranslationUnit(pFile, codeReaderFactory, new StubConfiguration());
    } catch (UnsupportedDialectException e) {
      System.err.println("UnsupportedDialectException:" +
          "Unsupported dialect for parser, check parser.dialect option!");
      System.exit(1);
    }
    return ast;
  }

  /**
   * Get the right StubCodeReaderFactory depending on the current CDT version.
   * @return The correct implementation of ICodeReaderFactory.
   * @throws ClassNotFoundException If no matching factory is found.
   */
  private static ICodeReaderFactory createCodeReaderFactory() throws ClassNotFoundException {
    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    
    String factoryClassName;
    // determine CDT version by trying to load the IMacroCollector class which
    // only exists in CDT 4
    try {
      classLoader.loadClass("org.eclipse.cdt.core.dom.IMacroCollector");
      
      // CDT 4.0
      factoryClassName = "cmdline.stubs.StubCodeReaderFactoryCDT4";
    } catch (ClassNotFoundException e) {
      // not CDT 4.0
      factoryClassName = "cmdline.stubs.StubCodeReaderFactory";
    }
  
    // try to load factory class and execute the static getInstance() method
    try {
      Class<?> factoryClass = classLoader.loadClass(factoryClassName);
      Object factoryObject = factoryClass.getMethod("getInstance", (Class<?>[]) null)
                                                                  .invoke(null);
      
      return (ICodeReaderFactory) factoryObject;
    } catch (Exception e) {
      // simply wrap all possible exceptions in a ClassNotFoundException
      // this will terminate the program
      throw new ClassNotFoundException("Exception while instantiating " + factoryClassName, e);
    }
  }
  private static class StubConfiguration implements IParserConfiguration {
    public String getParserDialect() {
      return ("C99");
    }
  
    public IScannerInfo getScannerInfo() {
        return new StubScannerInfo();
    }
  }

  private static class StubScannerInfo implements IScannerInfo {
  
      @SuppressWarnings("unchecked")
      public Map getDefinedSymbols() {
          // the externally defined pre-processor macros  
          return null;
      }
  
      public String[] getIncludePaths() {
          return null;
      }
  }
}
