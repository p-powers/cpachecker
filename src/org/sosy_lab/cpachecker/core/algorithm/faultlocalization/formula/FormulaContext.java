/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula;

import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;

public class FormulaContext {

  private Solver solver;
  private PathFormulaManagerImpl manager;
  private ProverEnvironment prover;
  private ExpressionConverter converter;

  public FormulaContext(Solver pSolver, PathFormulaManagerImpl pManager, ExpressionConverter pConverter) {
    solver = pSolver;
    manager = pManager;
    prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
    converter = pConverter;
  }

  public ExpressionConverter getConverter() {
    return converter;
  }

  public Solver getSolver() {
    return solver;
  }

  public PathFormulaManagerImpl getManager() {
    return manager;
  }

  public ProverEnvironment getProver() {
    return prover;
  }
}
