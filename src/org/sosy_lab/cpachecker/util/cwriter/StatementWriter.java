// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import static com.google.common.base.Preconditions.checkState;

import java.util.Optional;
import org.sosy_lab.cpachecker.util.cwriter.Statement.CompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.Statement.EmptyStatement;
import org.sosy_lab.cpachecker.util.cwriter.Statement.FunctionDefinition;
import org.sosy_lab.cpachecker.util.cwriter.Statement.InlinedFunction;
import org.sosy_lab.cpachecker.util.cwriter.Statement.Label;
import org.sosy_lab.cpachecker.util.cwriter.Statement.SimpleStatement;

public class StatementWriter implements StatementVisitor {

  private final StringBuilder sb;
  private int currentIndent = 0;

  StatementWriter(StringBuilder pDestination) {
    sb = pDestination;
  }

  private void addIndent() {
    for (int i = 0; i < currentIndent; i++) {
      // sb.append(" ");
    }
    sb.append(" ");
  }

  private void increaseIndent() {
    checkState(currentIndent >= 0);
    currentIndent += 4;
    checkState(currentIndent >= 0);
  }

  private void decreaseIndent() {
    checkState(currentIndent >= 0);
    currentIndent -= 4;
    checkState(currentIndent >= 0);
  }

  private void addLabelIfNecessary(Statement pS) {
    Optional<String> label = pS.getLabelIfUsed();
    if (label.isPresent()) {
      sb.append(label.get()).append(":;\n");
    }
  }

  @Override
  public void visit(SimpleStatement pS) {
    addLabelIfNecessary(pS);
    addIndent();
    sb.append(pS.getCode());
    sb.append("\n");
  }

  @Override
  public void visit(Label pS) {
    // in contrast to the other statements, we do not call 'addLabelIfNecessary',
    // but always add the label below.
    addIndent();
    sb.append(pS.getLabel()).append(":;");
  }

  @Override
  public void visit(FunctionDefinition pS) {
    addLabelIfNecessary(pS);
    addIndent();
    sb.append(pS.getFunctionHeader()).append("\n");
    pS.getFunctionBody().accept(this);
    sb.append("\n");
  }

  @Override
  public void visit(EmptyStatement pS) {
    addLabelIfNecessary(pS);
  }

  @Override
  public void visit(CompoundStatement pS) {
    visitCompound(pS);
  }

  private void visitCompound(CompoundStatement pS) {
    addLabelIfNecessary(pS);
    addIndent();
    sb.append("{\n");

    for (Statement statement : pS.getStatements()) {
      int indentBefore = currentIndent;
      increaseIndent();
      statement.accept(this);
      decreaseIndent();
      checkState(currentIndent == indentBefore);
    }

    addIndent();
    sb.append("}\n");
  }

  @Override
  public void visit(InlinedFunction pS) {
    visitCompound(pS);
  }
}