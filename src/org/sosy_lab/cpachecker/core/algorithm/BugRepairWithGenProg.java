// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;


import com.google.common.base.Optional;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class BugRepairWithGenProg
    implements Algorithm, StatisticsProvider, Statistics {

  private final Algorithm algorithm;
  private final LogManager logger;
  private final CFA cfa;

  private final StatTimer totalTime = new StatTimer("Total time for bug repair");

  public BugRepairWithGenProg(
      final Algorithm pStoreAlgorithm,
      final LogManager pLogger,
      final CFA pCfa)
      throws InvalidConfigurationException {

    logger = pLogger;
    cfa = pCfa;
    algorithm = pStoreAlgorithm;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    AlgorithmStatus status = algorithm.run(reachedSet);

    totalTime.start();

    try {
      logger.log(Level.INFO, "Starting bug repair...");

      runAlgorithm();

      logger.log(Level.INFO, "Stopping bug repair...");
    } finally{
      totalTime.stop();
    }
    return status;
  }

  private void runAlgorithm(){
      Collection<AAstNode> repairCandidates = calcRepairCandidates(this.cfa.getAllNodes());

      for (AAstNode node : repairCandidates) {
        logger.log(Level.INFO, "This might be the cause: " + node.toASTString());

        if (node instanceof CBinaryExpression){
          CBinaryExpression binNode = (CBinaryExpression) node;

          if (binNode.getOperator().isLogicalOperator()) {
            CBinaryExpression patchCandidate = new CBinaryExpression(binNode.getFileLocation(),
                binNode.getExpressionType(),
                binNode.getCalculationType(),
                binNode.getOperand1(),
                binNode.getOperand2(),
                binNode.getOperator().getOppositLogicalOperator());

            logger.log(Level.INFO, "This might fix it: " + patchCandidate.toASTString());
          }
        }
      }
  }

  private Collection<AAstNode> calcRepairCandidates(Collection<CFANode> cfaNodes){
    Collection<AAstNode> repairCandidates = new ArrayList<>();

    for (CFANode node : cfaNodes) {
      for (int index = 0; index < node.getNumLeavingEdges(); index++){

        CFAEdge edge = node.getLeavingEdge(index);
        Optional<? extends AAstNode> ast = edge.getRawAST();

        if (ast.isPresent() && isCandidate()){
          repairCandidates.add(ast.get());
        }
      }
    }

    return repairCandidates;
  }

  private boolean isCandidate()  {
    return Math.random() >= 0.7;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(this);
    if (algorithm instanceof Statistics) {
      statsCollection.add((Statistics) algorithm);
    }
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) algorithm).collectStatistics(statsCollection);
    }
  }

  @Override
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter.writingStatisticsTo(out).put(totalTime);
  }

  @Override
  public @Nullable String getName() {
    return getClass().getSimpleName();
  }

}
