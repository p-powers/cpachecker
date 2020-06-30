// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

abstract class FlowDepAnalysis extends ReachDefAnalysis<MemoryLocation, CFANode, CFAEdge> {

  private final FunctionEntryNode entryNode;
  private final List<CFAEdge> globalEdges;

  private final Multimap<CFAEdge, ReachDefAnalysis.Def<MemoryLocation, CFAEdge>> dependences;
  private final Multimap<CFAEdge, MemoryLocation> maybeDefs;

  protected FlowDepAnalysis(
      Dominance.DomTraversable<CFANode> pDomTraversable,
      Dominance.DomFrontiers<CFANode> pDomFrontiers,
      FunctionEntryNode pEntryNode,
      List<CFAEdge> pGlobalEdges) {

    super(SingleFunctionGraph.INSTANCE, pDomTraversable, pDomFrontiers);

    entryNode = pEntryNode;
    globalEdges = pGlobalEdges;

    dependences = ArrayListMultimap.create();
    maybeDefs = HashMultimap.create();
  }

  protected abstract Set<MemoryLocation> getPossiblePointees(
      CFAEdge pEdge, CExpression pExpression);

  protected abstract Set<MemoryLocation> getForeignDefs(AFunctionDeclaration pFunction);

  protected abstract Set<MemoryLocation> getForeignUses(AFunctionDeclaration pFunction);

  protected abstract void onDependence(CFAEdge pDefEdge, CFAEdge pUseEdge, MemoryLocation pCause);

  private CFunctionCallEdge getFunctionCallEdge(CFunctionSummaryEdge pSummaryEdge) {

    for (CFAEdge edge : CFAUtils.leavingEdges(pSummaryEdge.getPredecessor())) {
      if (edge instanceof CFunctionCallEdge) {
        return (CFunctionCallEdge) edge;
      }
    }

    throw new AssertionError("CFunctionSummaryEdge has no corresponding CFunctionCallEdge");
  }

  private Set<MemoryLocation> getCallEdgeDefs(CFunctionCallEdge pCallEdge) {

    Set<MemoryLocation> defs = new HashSet<>();
    AFunctionDeclaration function = pCallEdge.getSuccessor().getFunction();

    defs.addAll(getForeignUses(function));

    for (AParameterDeclaration parameter : function.getParameters()) {
      defs.add(MemoryLocation.valueOf(parameter.getQualifiedName()));
    }

    return defs;
  }

  private Set<MemoryLocation> getSummaryEdgeDefs(CFunctionSummaryEdge pSummaryEdge) {

    Set<MemoryLocation> defs = new HashSet<>();

    AFunctionDeclaration function = pSummaryEdge.getFunctionEntry().getFunction();
    CFunctionCallEdge callEdge = getFunctionCallEdge(pSummaryEdge);
    EdgeDefUseData edgeDefUseData = EdgeDefUseData.extract(callEdge);

    defs.addAll(edgeDefUseData.getDefs());
    defs.addAll(getForeignDefs(function));

    maybeDefs.putAll(pSummaryEdge, getForeignDefs(function));

    for (CExpression expression : edgeDefUseData.getPointeeDefs()) {

      Set<MemoryLocation> possibleDefs = getPossiblePointees(callEdge, expression);
      assert possibleDefs != null && possibleDefs.size() > 0 : "No possible pointees";
      defs.addAll(possibleDefs);

      if (possibleDefs.size() > 1) {
        maybeDefs.putAll(pSummaryEdge, possibleDefs);
      }
    }

    return defs;
  }

  private Set<MemoryLocation> getSummaryEdgeUses(CFunctionSummaryEdge pSummaryEdge) {

    Set<MemoryLocation> uses = new HashSet<>();

    AFunctionDeclaration function = pSummaryEdge.getFunctionEntry().getFunction();
    CFunctionCallEdge callEdge = getFunctionCallEdge(pSummaryEdge);
    EdgeDefUseData edgeDefUseData = EdgeDefUseData.extract(callEdge);

    uses.addAll(edgeDefUseData.getUses());
    uses.addAll(getForeignUses(function));

    for (CExpression expression : edgeDefUseData.getPointeeUses()) {

      Set<MemoryLocation> possibleUses = getPossiblePointees(callEdge, expression);
      assert possibleUses != null && possibleUses.size() > 0 : "No possible pointees";
      uses.addAll(possibleUses);
    }

    return uses;
  }

  private Set<MemoryLocation> getOtherEdgeDefs(CFAEdge pEdge) {

    Set<MemoryLocation> defs = new HashSet<>();
    EdgeDefUseData edgeDefUseData = EdgeDefUseData.extract(pEdge);

    defs.addAll(edgeDefUseData.getDefs());

    for (CExpression expression : edgeDefUseData.getPointeeDefs()) {

      Set<MemoryLocation> possibleDefs = getPossiblePointees(pEdge, expression);
      assert possibleDefs != null && possibleDefs.size() > 0 : "No possible pointees";
      defs.addAll(possibleDefs);

      if (possibleDefs.size() > 1) {
        maybeDefs.putAll(pEdge, possibleDefs);
      }
    }

    return defs;
  }

  private Set<MemoryLocation> getOtherEdgeUses(CFAEdge pEdge) {

    Set<MemoryLocation> uses = new HashSet<>();
    EdgeDefUseData edgeDefUseData = EdgeDefUseData.extract(pEdge);

    uses.addAll(edgeDefUseData.getUses());

    for (CExpression expression : edgeDefUseData.getPointeeUses()) {

      Set<MemoryLocation> possibleUses = getPossiblePointees(pEdge, expression);
      assert possibleUses != null && possibleUses.size() > 0 : "No possible pointees";
      uses.addAll(possibleUses);
    }

    return uses;
  }

  @Override
  protected Set<MemoryLocation> getEdgeDefs(CFAEdge pEdge) {

    if (pEdge instanceof CFunctionCallEdge) {
      return getCallEdgeDefs((CFunctionCallEdge) pEdge);
    } else if (pEdge instanceof CFunctionSummaryEdge) {
      return getSummaryEdgeDefs((CFunctionSummaryEdge) pEdge);
    } else {
      return getOtherEdgeDefs(pEdge);
    }
  }

  private Set<MemoryLocation> getEdgeUses(CFAEdge pEdge) {

    if (pEdge instanceof CFunctionCallEdge) {
      return Set.of();
    } else if (pEdge instanceof CFunctionSummaryEdge) {
      return getSummaryEdgeUses((CFunctionSummaryEdge) pEdge);
    } else {
      return getOtherEdgeUses(pEdge);
    }
  }

  @Override
  protected Collection<Def<MemoryLocation, CFAEdge>> getReachDefs(MemoryLocation pVariable) {

    List<Def<MemoryLocation, CFAEdge>> reachDefs = new ArrayList<>();

    for (Def<MemoryLocation, CFAEdge> def : iterateDefsNewestFirst(pVariable)) {

      reachDefs.add(def);

      Optional<CFAEdge> edge = def.getEdge();
      if (!edge.isPresent() || !maybeDefs.get(edge.orElseThrow()).contains(pVariable)) {
        break;
      }
    }

    return reachDefs;
  }

  @Override
  protected void insertCombiners(Dominance.DomFrontiers<CFANode> pDomFrontiers) {

    for (AParameterDeclaration declaration : entryNode.getFunctionParameters()) {
      MemoryLocation variable = MemoryLocation.valueOf(declaration.getQualifiedName());
      insertCombiner(entryNode, variable);
    }

    for (MemoryLocation variable : getForeignUses(entryNode.getFunction())) {
      insertCombiner(entryNode, variable);
    }

    super.insertCombiners(pDomFrontiers);
  }

  @Override
  protected void traverseDomTree(Dominance.DomTraversable<CFANode> pDomTraversable) {

    globalEdges.forEach(this::pushEdge);

    // init function parameters
    for (CFAEdge callEdge : CFAUtils.allEnteringEdges(pDomTraversable.getNode())) {
      pushEdge(callEdge);
      popEdge(callEdge);
    }

    super.traverseDomTree(pDomTraversable);
  }

  private void handleDependence(CFAEdge pEdge, Def<MemoryLocation, CFAEdge> pDef) {

    MemoryLocation variable = pDef.getVariable();
    Set<ReachDefAnalysis.Def<MemoryLocation, CFAEdge>> defs = new HashSet<>();
    pDef.collect(defs);

    for (ReachDefAnalysis.Def<MemoryLocation, CFAEdge> def : defs) {

      Optional<CFAEdge> optDefEdge = def.getEdge();
      if (optDefEdge.isPresent()) {
        onDependence(optDefEdge.orElseThrow(), pEdge, variable);
      }
    }
  }

  @Override
  public void run() {

    super.run();

    dependences.forEach(this::handleDependence);

    addFunctionUseDependences();
    addReturnValueDependences();
    addForeignDefDependences();
  }

  private ReachDefAnalysis.Def<MemoryLocation, CFAEdge> getDeclaration(MemoryLocation pVariable) {

    for (ReachDefAnalysis.Def<MemoryLocation, CFAEdge> def : iterateDefsOldestFirst(pVariable)) {
      Optional<CFAEdge> optEdge = def.getEdge();
      if (optEdge.isPresent() && optEdge.orElseThrow() instanceof CDeclarationEdge) {
        return def;
      }
    }

    return null;
  }

  @Override
  protected void pushNode(CFANode pNode) {

    super.pushNode(pNode);

    if (pNode instanceof FunctionExitNode) {
      for (MemoryLocation defVar : getForeignDefs(pNode.getFunction())) {
        for (ReachDefAnalysis.Def<MemoryLocation, CFAEdge> def : getReachDefs(defVar)) {
          for (CFAEdge returnEdge : CFAUtils.leavingEdges(pNode)) {
            dependences.put(returnEdge, def);
          }
        }
      }
    }
  }

  @Override
  protected void pushEdge(CFAEdge pEdge) {

    for (MemoryLocation useVar : getEdgeUses(pEdge)) {
      for (ReachDefAnalysis.Def<MemoryLocation, CFAEdge> def : getReachDefs(useVar)) {
        assert def != null
            : String.format("Variable is missing definition: %s @ %s", useVar, pEdge);
        dependences.put(pEdge, def);
      }
    }

    for (MemoryLocation defVar : getEdgeDefs(pEdge)) {
      ReachDefAnalysis.Def<MemoryLocation, CFAEdge> declaration = getDeclaration(defVar);
      if (declaration != null) {
        dependences.put(pEdge, declaration);
      }
    }

    super.pushEdge(pEdge);
  }

  private void addFunctionUseDependences() {

    for (CFAEdge callEdge : CFAUtils.allEnteringEdges(entryNode)) {
      CFAEdge summaryEdge = callEdge.getPredecessor().getLeavingSummaryEdge();
      assert summaryEdge != null : "Missing summary edge for call edge: " + callEdge;
      for (MemoryLocation parameter : getEdgeDefs(callEdge)) {
        onDependence(summaryEdge, callEdge, parameter);
      }
    }
  }

  private void addForeignDefDependences() {

    AFunctionDeclaration function = entryNode.getFunction();

    for (CFAEdge returnEdge : CFAUtils.leavingEdges(entryNode.getExitNode())) {
      CFAEdge summaryEdge = returnEdge.getSuccessor().getEnteringSummaryEdge();
      assert summaryEdge != null : "Missing summary edge for return edge: " + returnEdge;

      for (MemoryLocation defVar : getForeignDefs(function)) {
        onDependence(returnEdge, summaryEdge, defVar);
      }
    }
  }

  private void addReturnValueDependences() {

    Optional<? extends AVariableDeclaration> optRetVar = entryNode.getReturnVariable().toJavaUtil();

    if (optRetVar.isPresent()) {

      MemoryLocation returnVar = MemoryLocation.valueOf(optRetVar.get().getQualifiedName());

      for (CFAEdge defEdge : CFAUtils.allEnteringEdges(entryNode.getExitNode())) {
        for (CFAEdge returnEdge : CFAUtils.allLeavingEdges(entryNode.getExitNode())) {
          onDependence(defEdge, returnEdge, returnVar);
        }
      }

      for (CFAEdge returnEdge : CFAUtils.allLeavingEdges(entryNode.getExitNode())) {
        CFAEdge summaryEdge = returnEdge.getSuccessor().getEnteringSummaryEdge();
        assert summaryEdge != null : "Missing summary edge for return edge: " + returnEdge;
        onDependence(returnEdge, summaryEdge, returnVar);
      }
    }
  }

  private static final class SingleFunctionGraph
      implements ReachDefAnalysis.Graph<CFANode, CFAEdge> {

    private static final SingleFunctionGraph INSTANCE = new SingleFunctionGraph();

    @Override
    public CFANode getPredecessor(CFAEdge pEdge) {
      return pEdge.getPredecessor();
    }

    @Override
    public CFANode getSuccessor(CFAEdge pEdge) {
      return pEdge.getSuccessor();
    }

    @Override
    public Optional<CFAEdge> getEdge(CFANode pPredecessor, CFANode pSuccessor) {

      for (CFAEdge edge : getLeavingEdges(pPredecessor)) {
        if (edge.getSuccessor().equals(pSuccessor)) {
          return Optional.of(edge);
        }
      }

      return Optional.empty();
    }

    private boolean ignoreEdge(CFAEdge pEdge) {
      return !(pEdge instanceof CFunctionCallEdge)
          && !(pEdge instanceof CFunctionReturnEdge)
          && !(pEdge instanceof CFunctionSummaryStatementEdge);
    }

    @Override
    public Iterable<CFAEdge> getLeavingEdges(CFANode pNode) {
      return () -> Iterators.filter(CFAUtils.allLeavingEdges(pNode).iterator(), this::ignoreEdge);
    }

    @Override
    public Iterable<CFAEdge> getEnteringEdges(CFANode pNode) {
      return () -> Iterators.filter(CFAUtils.allEnteringEdges(pNode).iterator(), this::ignoreEdge);
    }
  }
}
