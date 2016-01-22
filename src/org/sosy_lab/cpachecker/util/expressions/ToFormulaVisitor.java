/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.expressions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor.ToFormulaException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.api.BooleanFormula;

import com.google.common.base.Preconditions;

public class ToFormulaVisitor implements ExpressionTreeVisitor<AExpression, BooleanFormula, ToFormulaException> {

  private final FormulaManagerView formulaManagerView;

  private final PathFormulaManager pathFormulaManager;

  public ToFormulaVisitor(FormulaManagerView pFormulaManagerView, PathFormulaManager pPathFormulaManager) {
    formulaManagerView = pFormulaManagerView;
    pathFormulaManager = pPathFormulaManager;
  }

  @Override
  public BooleanFormula visit(And<AExpression> pAnd) throws ToFormulaException {
    List<BooleanFormula> elements = new ArrayList<>();
    for (ExpressionTree<AExpression> element : pAnd) {
      elements.add(element.accept(this));
    }
    return formulaManagerView.getBooleanFormulaManager().and(elements);
  }

  @Override
  public BooleanFormula visit(Or<AExpression> pOr) throws ToFormulaException {
    List<BooleanFormula> elements = new ArrayList<>();
    for (ExpressionTree<AExpression> element : pOr) {
      elements.add(element.accept(this));
    }
    return formulaManagerView.getBooleanFormulaManager().or(elements);
  }

  @Override
  public BooleanFormula visit(LeafExpression<AExpression> pLeafExpression) throws ToFormulaException {
    CFAEdge edge = null;
    PathFormula invariantPathFormula;
    try {
      invariantPathFormula = pathFormulaManager.makeFormulaForPath(Collections.<CFAEdge>singletonList(edge));
    } catch (CPATransferException e) {
      throw new ToFormulaException(e);
    } catch (InterruptedException e) {
      throw new ToFormulaException(e);
    }
    return formulaManagerView.uninstantiate(invariantPathFormula.getFormula());
  }

  @Override
  public BooleanFormula visitTrue() {
    return formulaManagerView.getBooleanFormulaManager().makeBoolean(true);
  }

  @Override
  public BooleanFormula visitFalse() {
    return formulaManagerView.getBooleanFormulaManager().makeBoolean(false);
  }

  public static class ToFormulaException extends Exception {

    private static final long serialVersionUID = -3849941975554955994L;

    private final CPATransferException transferException;

    private final InterruptedException interruptedException;

    private ToFormulaException(CPATransferException pTransferException) {
      super(pTransferException);
      this.transferException = Objects.requireNonNull(pTransferException);
      this.interruptedException = null;
    }

    private ToFormulaException(InterruptedException pInterruptedException) {
      super(pInterruptedException);
      this.transferException = null;
      this.interruptedException = Objects.requireNonNull(pInterruptedException);
    }

    public boolean isTransferException() {
      return transferException != null;
    }

    public boolean isInterruptedException() {
      return interruptedException != null;
    }

    public CPATransferException asTransferException() {
      Preconditions.checkState(isTransferException());
      return transferException;
    }

    public InterruptedException asInterruptedException() {
      Preconditions.checkState(isInterruptedException());
      return interruptedException;
    }

  }

}
