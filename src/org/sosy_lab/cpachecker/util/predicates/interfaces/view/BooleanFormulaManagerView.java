/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.interfaces.view;

import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.FormulaOperator;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormula;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;


public class BooleanFormulaManagerView extends BaseManagerView<BooleanFormula> implements BooleanFormulaManager {

  private BooleanFormulaManager manager;

  public BooleanFormulaManagerView(BooleanFormulaManager pManager) {
    this.manager = pManager;
  }

  public BooleanFormula makeVariable(String pVar, int pI) {
    return makeVariable(FormulaManagerView.makeName(pVar, pI));
  }
  @Override
  public BooleanFormula not(BooleanFormula pBits) {
    return wrapInView(manager.not(extractFromView(pBits)));
  }

  @Override
  public BooleanFormula and(BooleanFormula pBits1, BooleanFormula pBits2) {
    return wrapInView(manager.and(extractFromView(pBits1),extractFromView(pBits2)));
  }
  @Override
  public BooleanFormula or(BooleanFormula pBits1, BooleanFormula pBits2) {
    return wrapInView(manager.or(extractFromView(pBits1),extractFromView(pBits2)));
  }
  @Override
  public BooleanFormula xor(BooleanFormula pBits1, BooleanFormula pBits2) {
    return wrapInView(manager.xor(extractFromView(pBits1),extractFromView(pBits2)));
  }

  @Override
  public boolean isNot(BooleanFormula pBits) {
    return manager.isNot(extractFromView(pBits));
  }

  @Override
  public boolean isAnd(BooleanFormula pBits) {
    return manager.isAnd(extractFromView(pBits));
  }

  @Override
  public boolean isOr(BooleanFormula pBits) {
    return manager.isOr(extractFromView(pBits));
  }

  @Override
  public boolean isXor(BooleanFormula pBits) {
    return manager.isXor(extractFromView(pBits));
  }

  @Override
  public boolean isBoolean(Formula pF) {
    return pF instanceof BooleanFormula;
  }

  @Override
  public FormulaType<BooleanFormula> getFormulaType() {
    return manager.getFormulaType();
  }

  @Override
  public BooleanFormula makeBoolean(boolean pValue) {
    return wrapInView(manager.makeBoolean(pValue));
  }

  @Override
  public BooleanFormula makeVariable(String pVar) {
    return wrapInView(manager.makeVariable(pVar));
  }

  @Override
  public BooleanFormula equivalence(BooleanFormula pFormula1, BooleanFormula pFormula2) {
    return wrapInView(manager.equivalence(extractFromView(pFormula1), extractFromView(pFormula2)));
  }

  @Override
  public boolean isTrue(BooleanFormula pFormula) {
    return manager.isTrue(extractFromView(pFormula));
  }

  @Override
  public boolean isFalse(BooleanFormula pFormula) {
    return manager.isFalse(extractFromView(pFormula));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> T ifThenElse(BooleanFormula pCond, T pF1, T pF2) {
    FormulaManagerView viewManager = getViewManager();
    return viewManager.wrapInView(manager.ifThenElse(extractFromView(pCond), viewManager.extractFromView(pF1), viewManager.extractFromView(pF2)));
  }

  @Override
  public <T extends Formula> boolean isIfThenElse(T pF) {
    FormulaManagerView viewManager = getViewManager();
    return manager.isIfThenElse(viewManager.extractFromView(pF));
  }

  @Override
  public boolean isEquivalence(BooleanFormula pFormula) {
    return manager.isEquivalence(extractFromView(pFormula));
  }


  public <T extends Formula> T ifTrueThenOneElseZero(FormulaType<T> type, BooleanFormula pCond){
    FormulaManagerView viewManager = getViewManager();
    T one = viewManager.makeNumber(type, 1);
    T zero = viewManager.makeNumber(type, 0);
    return manager.ifThenElse(pCond, one, zero);
  }

  public RationalFormula toNumericFormula(BooleanFormula f) {
    return ifTrueThenOneElseZero(FormulaType.RationalType, f);
  }

  public BitvectorFormula toBitpreciseFormula(FormulaType<BitvectorFormula> t, BooleanFormula f) {
    return ifTrueThenOneElseZero(t, f);
  }

  public BooleanFormula conjunction(List<BooleanFormula> f) {
    BooleanFormula result = manager.makeBoolean(true);
    for (BooleanFormula formula : f) {
      result = manager.and(result, extractFromView(formula));
    }
    return wrapInView(result);
  }

  public BooleanFormula implication(BooleanFormula p, BooleanFormula q) {
    return or(not(p), q);
  }


  public BooleanFormula notEquivalence(BooleanFormula p, BooleanFormula q){
    return not(equivalence(p, q));
  }

  public boolean useBitwiseAxioms() {
    return getViewManager().useBitwiseAxioms();
  }

  public FormulaOperator getOperator(BooleanFormula f) {
    FormulaManagerView viewManager = getViewManager();
    if (viewManager.getUnsafeFormulaManager().isAtom(viewManager.extractFromView(f))){
      return FormulaOperator.ATOM;
    }

    if (isNot(f)){
      return FormulaOperator.NOT;
    }

    if (isAnd(f)){
      return FormulaOperator.AND;
    }
    if (isOr(f)){
      return FormulaOperator.OR;
    }

    if (isIfThenElse(f)){
      return FormulaOperator.ITE;
    }

    if (isEquivalence(f)){
      return FormulaOperator.EQUIV;
    }

//    final long t = getTerm(f);
//    if (msat_term_is_atom(msatEnv, t)) { return FormulaOperator.ATOM; }
//    if (msat_term_is_not(msatEnv, t)) { return FormulaOperator.NOT; }
//    if (msat_term_is_and(msatEnv, t)) { return FormulaOperator.AND; }
//    if (msat_term_is_or(msatEnv, t)) { return FormulaOperator.OR; }
//    if (msat_term_is_iff(msatEnv, t)) { return FormulaOperator.EQUIV; }
//    if (msat_term_is_term_ite(msatEnv, t)) { return FormulaOperator.ITE; }
    throw new NotImplementedException();
  }

}
