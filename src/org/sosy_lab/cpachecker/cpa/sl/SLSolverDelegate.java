/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.cpachecker.cpa.sl;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.java_smt.api.Formula;

public interface SLSolverDelegate {

  /**
   * Evaluates a CEpression's numeric value.
   *
   * @param pExp - The expression to be evaluated.
   * @return numeric value
   */
  public BigInteger getValueForCExpression(CExpression pExp) throws Exception;

  /**
   * Checks whether two formulae are semantically equivalent.
   *
   * @return f0 <=> f1
   */
  public boolean checkEquivalence(Formula f0, Formula f1);

  public Formula
      getFormulaForVariableName(String pVariable, boolean pIsGlobal, boolean addSSAIndex);

  default public Formula getFormulaForExpression(CIdExpression pExp) {
    return getFormulaForVariableName(pExp.getName(), false, true);
  }

  public Formula getFormulaForExpression(CExpression pExp) throws Exception;
}