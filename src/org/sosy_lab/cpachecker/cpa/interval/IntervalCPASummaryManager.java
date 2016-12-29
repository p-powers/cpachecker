/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.interval;

import com.google.common.collect.FluentIterable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.summary.blocks.Block;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.SummaryManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Summary manager for the interval CPA.
 */
public class IntervalCPASummaryManager implements SummaryManager {

  private final LogManager logger;
  private final TransferRelation transferRelation;

  IntervalCPASummaryManager(LogManager pLogger, TransferRelation pTransferRelation) {
    logger = pLogger;
    transferRelation = pTransferRelation;
  }

  @Override
  public AbstractState getAbstractSuccessorForSummary(
      AbstractState pFunctionCallState,
      Precision pFunctionCallPrecision,
      List<Summary> pSummaries,
      Block pBlock,
      CFANode pCallSite)
      throws CPAException, InterruptedException {

    // Propagate the intervals for those variables invariant
    // under the function call, use summary for others.

    // todo: how to rely on the assumption that only a single summary exists?
    // Maybe just assert that?
    IntervalSummary iSummary = (IntervalSummary) pSummaries.get(0);
    for (Summary s : pSummaries.subList(1, pSummaries.size())) {
      iSummary = merge(iSummary, s);
    }

    // todo: remove all vars modified inside the block.
    IntervalAnalysisState copy = IntervalAnalysisState.copyOf(
        (IntervalAnalysisState) pFunctionCallState);

    Collection<? extends AbstractState> out =
        transferRelation.getAbstractSuccessorsForEdge(
            iSummary.getStateAtExit(),
            SingletonPrecision.getInstance(),
            findJoinEdge(pBlock, pCallSite)
        );
    assert out.size() == 1;

    IntervalAnalysisState joinedState = (IntervalAnalysisState) out.iterator().next();
    joinedState.getIntervalMap().forEach(
        (var, interval) -> copy.addInterval(var, interval, -1)
    );

    logger.log(Level.INFO, "Postcondition after application of the summary",
        iSummary, "to state", pFunctionCallState, "is", copy);
    return copy;
  }

  /**
   * @return the edge going back into the calling function.
   */
  private CFAEdge findJoinEdge(Block pBlock, CFANode pCallsite) {
    CFANode exitNode = pBlock.getExitNode();
    CFANode joinNode = pCallsite.getLeavingSummaryEdge().getSuccessor();

    for (int i=0; i<exitNode.getNumLeavingEdges(); i++) {
      CFAEdge leavingEdge = exitNode.getLeavingEdge(i);
      if (leavingEdge.getSuccessor() == joinNode) {
        return leavingEdge;
      }
    }
    throw new UnsupportedOperationException("Unexpected state");
  }

  @Override
  public IntervalAnalysisState getWeakenedCallState(
      AbstractState pState, Precision pPrecision, Block pBlock) {
    IntervalAnalysisState iState = (IntervalAnalysisState) pState;
    IntervalAnalysisState clone = IntervalAnalysisState.copyOf(iState);

    // todo: seems to be buggy, maybe does not take into account the variable
    // todo: renaming when applying function calls.

    Set<String> readVarNames = pBlock.getReadVariables().stream()
        .map(w -> w.get().getQualifiedName()).collect(Collectors.toSet());

    iState.getIntervalMap().keySet().stream()
        .filter(v -> readVarNames.contains(v))
        .forEach(v -> clone.removeInterval(v));
    logger.log(Level.INFO, "Weakened ", iState, " to ", clone);
    return clone;
  }

  @Override
  public AbstractState projectToCallsite(Summary pSummary) {
    IntervalSummary iSummary = (IntervalSummary) pSummary;
    return iSummary.getStateAtEntry();
  }

  @Override
  public AbstractState projectToPostcondition(Summary pSummary) {
    IntervalSummary iSummary = (IntervalSummary) pSummary;
    return iSummary.getStateAtExit();
  }

  @Override
  public List<? extends Summary> generateSummaries(
      AbstractState pCallState,
      Precision pEntryPrecision,
      List<? extends AbstractState> pReturnStates,
      List<Precision> pReturnPrecisions,
      CFANode pEntryNode,
      Block pBlock
  ) {
    IntervalAnalysisState iCallState = (IntervalAnalysisState) pCallState;

    assert !pReturnStates.isEmpty();
    Stream<IntervalAnalysisState> stream = StreamSupport.stream(
        FluentIterable.from(pReturnStates).filter(IntervalAnalysisState.class).spliterator(),
        false);
    Optional<IntervalAnalysisState> out = stream.reduce((a, b) -> a.join(b));

    return Collections.singletonList(new IntervalSummary(iCallState, out.get()));
  }

  @Override
  public IntervalSummary merge(
      Summary pSummary1,
      Summary pSummary2) throws CPAException, InterruptedException {

    IntervalSummary iSummary1 = (IntervalSummary) pSummary1;
    IntervalSummary iSummary2 = (IntervalSummary) pSummary2;
    return new IntervalSummary(
        iSummary1.getStateAtEntry().join(iSummary2.getStateAtEntry()),
        iSummary2.getStateAtExit().join(iSummary2.getStateAtExit())
    );
  }

  @Override
  public boolean isDescribedBy(Summary pSummary1, Summary pSummary2)
      throws CPAException, InterruptedException {
    IntervalSummary iSummary1 = (IntervalSummary) pSummary1;
    IntervalSummary iSummary2 = (IntervalSummary) pSummary2;

    return iSummary1.getStateAtEntry().isLessOrEqual(
        iSummary2.getStateAtEntry()
    ) && iSummary2.getStateAtExit().isLessOrEqual(
        iSummary1.getStateAtExit()
    );
  }

  private static class IntervalSummary implements Summary {

    /**
     * Intervals over parameters, read global variables.
     */
    private final IntervalAnalysisState stateAtEntry;

    /**
     * Intervals over returned variable, changed global variables.
     */
    private final IntervalAnalysisState stateAtExit;

    private IntervalSummary(
        IntervalAnalysisState pStateAtEntry,
        IntervalAnalysisState pStateAtExit) {
      stateAtEntry = pStateAtEntry;
      stateAtExit = pStateAtExit;
    }

    IntervalAnalysisState getStateAtEntry() {
      return stateAtEntry;
    }

    IntervalAnalysisState getStateAtExit() {
      return stateAtExit;
    }

    @Override
    public boolean equals(@Nullable Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }
      IntervalSummary that = (IntervalSummary) pO;
      return Objects.equals(stateAtEntry, that.stateAtEntry) &&
          Objects.equals(stateAtExit, that.stateAtExit);
    }

    @Override
    public int hashCode() {
      return Objects.hash(stateAtEntry, stateAtExit);
    }

    @Override
    public String toString() {
      return "IntervalSummary{" +
          "stateAtEntry=" + stateAtEntry +
          ", stateAtExit=" + stateAtExit + '}';
    }
  }
}
