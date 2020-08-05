// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.sl;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.sl.SLState.SLStateError;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.Formula;

public class SLRhsToFormulaVisitor extends ExpressionToFormulaVisitor {

  private final SLMemoryDelegate delegate;
  private final CToFormulaConverterWithSL converter;
  private final Constraints constraints;
  private final CFAEdge edge;
  private final String functionName;
  private final FormulaManagerView fm;

  public SLRhsToFormulaVisitor(
      CtoFormulaConverter pCtoFormulaConverter,
      FormulaManagerView pFmgr,
      CFAEdge pEdge,
      String pFunction,
      SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts,
      Constraints pConstraints) {
    super(pCtoFormulaConverter, pFmgr, pEdge, pFunction, pSsa, pConstraints);
    assert pCtoFormulaConverter instanceof CToFormulaConverterWithSL;
    converter = (CToFormulaConverterWithSL) pCtoFormulaConverter;
    assert pPts instanceof SLMemoryDelegate;
    delegate = (SLMemoryDelegate) pPts;
    constraints = pConstraints;
    edge = pEdge;
    functionName = pFunction;
    fm = pFmgr;
  }

  @Override
  public Formula visit(CArraySubscriptExpression pE) throws UnrecognizedCodeException {
    CExpression subscriptExp = pE.getSubscriptExpression();
    Formula offset = subscriptExp.accept(this);
    CExpression arrayExp = pE.getArrayExpression();
    Formula loc = arrayExp.accept(this);
    int size = getBaseTypeSize(arrayExp.getExpressionType());
    // int size = converter.getSizeof(arrayExp.getExpressionType());
    // offset =
//        converter.makeCast(
//            subscriptExp.getExpressionType(),
//            arrayExp.getExpressionType(),
//            offset,
//            constraints,
//            edge);
    Optional<Formula> value = delegate.dereference(loc, offset, size);
    if (value.isEmpty()) {
      delegate.addError(SLStateError.INVALID_READ);
      return super.visit(pE); // Add dummy variable
    }
    return value.get();
  }

  @Override
  public Formula visit(CPointerExpression pE) throws UnrecognizedCodeException {
    Formula loc = pE.getOperand().accept(this);
    int size = converter.getSizeof(pE.getExpressionType());
    Optional<Formula> value = delegate.dereference(loc, size);
    if (value.isEmpty()) {
      delegate.addError(SLStateError.INVALID_READ);
      return super.visit(pE); // Add dummy variable
    }
    return value.get();
  }

  @Override
  public Formula visit(CUnaryExpression pExp) throws UnrecognizedCodeException {
    UnaryOperator op = pExp.getOperator();
    if (op != UnaryOperator.AMPER) {
      return super.visit(pExp);
    }

    CExpression operand = pExp.getOperand();
    return operand.accept(
        new SLLhsToFormulaVisitor(
            converter,
            edge,
            functionName,
            ssa,
            delegate,
            constraints,
            null,
            fm));
    // assert operand instanceof CIdExpression;
    // CIdExpression idExp = (CIdExpression) operand;
    // CType type = operand.getExpressionType();
    // if (type instanceof CArrayType) {
    // type = ((CArrayType) type).asPointerType();
    // }
    // String varName = idExp.getDeclaration().getQualifiedName();
    // String varNameWithAmper = op.getOperator() + varName;
    //
    // CType t = delegate.makeLocationTypeForVariableType(type);
    // return converter.makeVariable(varNameWithAmper, t, ssa);
  }

  private int getBaseTypeSize(CType type) {
    if (type instanceof CArrayType) {
      type = ((CArrayType) type).asPointerType().getType();
    } else if (type instanceof CPointerType) {
      type = ((CPointerType) type).getType();
    }
    return converter.getSizeof(type);
  }

  @Override
  public Formula visit(CFunctionCallExpression pE) throws UnrecognizedCodeException {
    final List<CExpression> params = pE.getParameterExpressions();
    for (CExpression p : params) {
      p.accept(this);
    }
    Formula loc = super.visit(pE);
    CIdExpression fctExp = (CIdExpression) pE.getFunctionNameExpression();

    Formula sizeValueFormula = null;
    BigInteger size = null;
    switch (SLHeapFunction.get(fctExp.getName())) {
      case CALLOC:
      case MALLOC: // always initialized with 0
        sizeValueFormula = params.get(0).accept(this);
        size = delegate.calculateValue(sizeValueFormula);
        if (size == null) {
          throw new UnrecognizedCodeException(
              "Allocation size passed to malloc could not be determinded.",
              edge);
        }
        delegate.handleMalloc(loc, size.intValueExact());
        break;
      case REALLOC:
        Formula oldLoc = params.get(0).accept(this);
        sizeValueFormula = params.get(1).accept(this);
        size = delegate.calculateValue(sizeValueFormula);
        delegate.handleRealloc(loc, oldLoc, size.intValueExact());
        break;
      case FREE:
        Formula locToFree = params.get(0).accept(this);
        if (!delegate.handleFree(locToFree)) {
          delegate.addError(SLStateError.INVALID_FREE);
        }
        break;
      case ALLOCA:
        sizeValueFormula = params.get(0).accept(this);
        size = delegate.calculateValue(sizeValueFormula);
        if (size == null) {
          throw new UnrecognizedCodeException(
              "Allocation size passed to alloca could not be determinded.",
              edge);
        }
        delegate.handleAlloca(loc, size.intValueExact(), functionName);
        break;
      default:
        break;
    }
    return loc;
  }

  @Override
  public Formula visit(CIdExpression pIdExp) throws UnrecognizedCodeException {
    String varName = UnaryOperator.AMPER.getOperator() + pIdExp.getDeclaration().getQualifiedName();
    CType type = pIdExp.getExpressionType();
    if (type instanceof CArrayType) {
      type = ((CArrayType) type).asPointerType();
    }
    CPointerType t = new CPointerType(type.isConst(), type.isVolatile(), type);
    Formula loc = converter.makeVariable(varName, t, ssa);
    return delegate.dereference(loc, converter.getSizeof(type)).get();
  }

  @Override
  public Formula visit(CFieldReference pFExp) throws UnrecognizedCodeException {
    Formula loc = pFExp.getFieldOwner().accept(this);
    SLFieldToOffsetVisitor v = new SLFieldToOffsetVisitor(converter);
    BigInteger offset = v.getOffset(pFExp, edge);
    Formula off = fm.makeNumber(fm.getFormulaType(loc), offset.longValueExact());
    loc = fm.makePlus(loc, off);
    int size = converter.getSizeof(pFExp.getExpressionType());
    return delegate.dereference(loc, size).get();
  }
}
