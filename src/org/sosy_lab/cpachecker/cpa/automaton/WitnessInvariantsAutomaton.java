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
package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.DefaultExpressionTreeVisitor;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;

/**
 * Building a witness invariants automaton out of the invariants from a given correctness witness
 */
public class WitnessInvariantsAutomaton extends Automaton {

  private int amountOfInvariantsWithSameLocations = 0;

  private WitnessInvariantsAutomaton(
      String pName,
      Map<String, AutomatonVariable> pVars,
      List<AutomatonInternalState> pStates,
      String pInitialStateName,
      int amountOfInvariantsWithSameLocations)
      throws InvalidAutomatonException {
    super(pName, pVars, pStates, pInitialStateName);
    this.amountOfInvariantsWithSameLocations = amountOfInvariantsWithSameLocations;
  }

  public int getAmountOfInvariantsWithSameLocations() {
    return this.amountOfInvariantsWithSameLocations;
  }

  private static int computeAmountOfInvariantsWithSameLocation(Set<ExpressionTreeLocationInvariant> invariants) {
    List<CFANode> locations = new ArrayList<>();
    int amountOfInvariantsWithSameLocations = 0;
    for(ExpressionTreeLocationInvariant inv : invariants) {
      if (locations.contains(inv.getLocation())) {
        continue;
      }
      for(ExpressionTreeLocationInvariant other : invariants) {
        if(!inv.equals(other)) {
          if (inv.getLocation().equals(other.getLocation())) {
            amountOfInvariantsWithSameLocations++;
          }
        }
      }
    }
    return amountOfInvariantsWithSameLocations;
  }

  public static Automaton buildWitnessInvariantsAutomaton(
      Set<ExpressionTreeLocationInvariant> invariants,
      ToCExpressionVisitor visitor,
      CBinaryExpressionBuilder builder) {
    try {
      int amountOfInvariantsWithSameLocations =
          computeAmountOfInvariantsWithSameLocation(invariants);
      String automatonName = "WitnessInvariantsAutomaton";
    String initialStateName = "Init";
    Map<String, AutomatonVariable> vars = Collections.emptyMap();
    List<AutomatonInternalState> states = Lists.newLinkedList();
    List<AutomatonTransition> initTransitions = Lists.newLinkedList();
    List<AutomatonInternalState> invStates = Lists.newLinkedList();
      for (ExpressionTreeLocationInvariant invariant : invariants) {
        ExpressionTree<?> expr = applyAssumeTruth(invariant.asExpressionTree(), builder);
          AutomatonTransition errorTransition =
              createTransitionWithCheckLocationAndAssumptionToError(
                  expr, invariant.getLocation(), visitor, builder);
          AutomatonTransition initTransition =
              createTransitionWithCheckLocationAndAssumptionToInit(
                  expr, invariant.getLocation(), visitor);
          initTransitions.add(errorTransition);
          initTransitions.add(initTransition);
      }
      AutomatonInternalState initState =
          new AutomatonInternalState(initialStateName, initTransitions, false, true, false);
    states.add(initState);
    states.addAll(invStates);
      return new WitnessInvariantsAutomaton(
          automatonName, vars, states, initialStateName, amountOfInvariantsWithSameLocations);
    } catch (InvalidAutomatonException | UnrecognizedCodeException e) {
      throw new RuntimeException("The passed invariants prdouce an inconsistent automaton", e);
    }
  }

  @SuppressWarnings("unchecked")
  private static <LeafType> ExpressionTree<?> applyAssumeTruth(
      ExpressionTree<?> expr, CBinaryExpressionBuilder builder) throws UnrecognizedCodeException {
    if(expr instanceof And<?>) {
      Set<ExpressionTree<LeafType>> operators = new HashSet<>();
      And<?> exprAnd = (And<?>) expr;
      Iterator<?> operandsIterator = exprAnd.iterator();
      while (operandsIterator.hasNext()) {
        ExpressionTree<?> next = (ExpressionTree<?>) operandsIterator.next();
        ExpressionTree<?> applied = applyAssumeTruth(next, builder);
        operators.add((ExpressionTree<LeafType>) applied);
      }
      return And.of(FluentIterable.from(operators));
    }
    if(expr instanceof Or<?>) {
      Set<ExpressionTree<LeafType>> operators = new HashSet<>();
      Or<?> exprOr = (Or<?>) expr;
      Iterator<?> operandsIterator = exprOr.iterator();
      while (operandsIterator.hasNext()) {
        ExpressionTree<?> next = (ExpressionTree<?>) operandsIterator.next();
        ExpressionTree<?> applied = applyAssumeTruth(next, builder);
        operators.add((ExpressionTree<LeafType>) applied);
      }
      return Or.of(FluentIterable.from(operators));
    }
    if(expr instanceof LeafExpression<?>) {
      LeafExpression<?> leafExpr = (LeafExpression<?>) expr;
      if(!leafExpr.assumeTruth()) {
        if(leafExpr.getExpression() instanceof CBinaryExpression) {
          CBinaryExpression exprC = (CBinaryExpression) leafExpr.getExpression();
          CBinaryExpression cExprNegated = builder.negateExpressionAndSimplify(exprC);
          return LeafExpression.fromCExpression(cExprNegated, true);
        }
      }
    }
    return expr;
  }

  @SuppressWarnings("unchecked")
  private static <LeafType> CExpression convertToCExpression(
      ExpressionTree<?> expr,
      DefaultExpressionTreeVisitor<LeafType, CExpression, UnrecognizedCodeException> visitor)
      throws UnrecognizedCodeException {
    if (expr instanceof And<?>) {
      return visitor.visit((And<LeafType>) expr);
    }
    if (expr instanceof Or<?>) {
      return visitor.visit((Or<LeafType>) expr);
    }
    if (expr instanceof LeafExpression<?>) {
      return visitor.visit((LeafExpression<LeafType>) expr);
    }
    return null;
  }

  private static CExpression negateCExpression(CExpression cExpr, CBinaryExpressionBuilder builder)
      throws UnrecognizedCodeException {
    return builder.negateExpressionAndSimplify(cExpr);
  }

  private static <LeafType>
      AutomatonTransition createTransitionWithCheckLocationAndAssumptionToError(
          ExpressionTree<?> expr,
          CFANode location,
          DefaultExpressionTreeVisitor<LeafType, CExpression, UnrecognizedCodeException> visitor,
          CBinaryExpressionBuilder builder)
          throws UnrecognizedCodeException {
    AutomatonBoolExpr queryString =
        new AutomatonBoolExpr.CPAQuery("location", "nodenumber==" + location.getNodeNumber());
    StringExpression violatedPropertyDesc = new StringExpression("Invariant not valid");
    List<AutomatonAction> stateActions = Collections.emptyList();
    List<AutomatonBoolExpr> stateAssertions = Collections.emptyList();
    AutomatonInternalState followState = AutomatonInternalState.ERROR;
    List<AExpression> assumptions = Lists.newLinkedList();
    CExpression cExpr = convertToCExpression(expr, visitor);
    CBinaryExpression cBinExpr = (CBinaryExpression) cExpr;
    CExpression negCExpr;
    negCExpr = negateCExpression(cBinExpr, builder);
    assumptions.add(negCExpr);
    return new AutomatonTransition(
        queryString, stateAssertions, assumptions, stateActions, followState, violatedPropertyDesc);
  }

  private static AutomatonTransition createTransitionWithCheckLocationAndAssumptionToInit(
      ExpressionTree<?> expr, CFANode location, ToCExpressionVisitor visitor)
      throws UnrecognizedCodeException {
    AutomatonBoolExpr queryString =
        new AutomatonBoolExpr.CPAQuery("location", "nodenumber==" + location.getNodeNumber());
    List<AutomatonAction> stateActions = Collections.emptyList();
    List<AutomatonBoolExpr> stateAssertions = Collections.emptyList();
    String followStateName = "Init";
    List<AExpression> assumptions = Lists.newLinkedList();
    CExpression cExpr = convertToCExpression(expr, visitor);
    assumptions.add(cExpr);
    return new AutomatonTransition(
        queryString, stateAssertions, assumptions, stateActions, followStateName);
  }

}