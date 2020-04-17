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
package org.sosy_lab.cpachecker.util.faultlocalization;

import com.google.common.base.Splitter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.misc.MultiMap;
import org.sosy_lab.common.JSON;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAdditionalInfo;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;

public class FaultLocalizationInfo extends CounterexampleInfo {

  private  CounterexampleInfo parent;

  private List<Fault> rankedList;
  private FaultReportWriter htmlWriter;

  private MultiMap<CFAEdge, Integer> mapEdgeToFaults;
  private Map<CFAEdge, FaultContribution> mapEdgeToFaultContribution;

  /**
   * Fault localization algorithms will result in a set of sets of CFAEdges that are most likely to fix a bug.
   * Transforming it into a Set of Faults enables the possibility to attach reasons of why this edge is in this set.
   * After ranking the set of faults an instance of this class can be created.
   *
   * The class should be used to display information to the user.
   *
   * Note that there is no need to create multiple instances of this object if more than one
   * heuristic should be applied. FaultRankingUtils provides a method that creates a new
   * heuristic out of multiple heuristics.
   *
   * To see the result of FaultLocalizationInfo replace the CounterexampleInfo of the target state by this
   * or simply call apply() on an instance of this class.
   *
   * @param pFaults Ranked list of faults obtained by a fault localization algorithm
   * @param pParent the counterexample info of the target state
   */
  public FaultLocalizationInfo(
      List<Fault> pFaults,
      CounterexampleInfo pParent) {
    super(
        pParent.isSpurious(),
        pParent.getTargetPath(),
        pParent.getCFAPathWithAssignments(),
        pParent.isPreciseCounterExample(),
        CFAPathWithAdditionalInfo.empty());
    initialize(pParent, pFaults);
  }

  /**
   * Fault localization algorithms will result in a set of sets of CFAEdges that are most likely to fix a bug.
   * Transforming it into a Set of Faults enables the possibility to attach reasons of why this edge is in this set.
   * After ranking the set of faults an instance of this class can be created.
   *
   * The class should be used to display information to the user.
   *
   * Note that there is no need to create multiple instances of this object if more than one
   * heuristic should be applied. FaultRankingUtils provides a method that creates a new
   * heuristic out of multiple heuristics.
   *
   * To see the result of FaultLocalizationInfo replace the CounterexampleInfo of the target state by this
   * or simply call apply() on an instance of this class.
   *
   * @param pFaults set of faults obtained by a fault localization algorithm
   * @param pRanking the ranking for pFaults
   * @param pParent the counterexample info of the target state
   */
  public FaultLocalizationInfo(Set<Fault> pFaults, FaultRanking pRanking, CounterexampleInfo pParent){
    super(
        pParent.isSpurious(),
        pParent.getTargetPath(),
        pParent.getCFAPathWithAssignments(),
        pParent.isPreciseCounterExample(),
        CFAPathWithAdditionalInfo.empty());
    List<Fault> rankedFault = pRanking.rank(pFaults);
    for (Fault fault : rankedFault) {
      FaultRankingUtils.assignScoreTo(fault);
      for (FaultContribution faultContribution : fault) {
        FaultRankingUtils.assignScoreTo(faultContribution);
      }
    }
    initialize(pParent, rankedFault);
  }

  private void initialize(CounterexampleInfo pParent, List<Fault> pFaults){
    parent = pParent;

    mapEdgeToFaultContribution = new HashMap<>();
    mapEdgeToFaults = new MultiMap<>();

    for(int i = 0; i < pFaults.size(); i++){
      for (FaultContribution faultContribution : pFaults.get(i)) {
        mapEdgeToFaults.map(faultContribution.correspondingEdge(), i);
        mapEdgeToFaultContribution.put(faultContribution.correspondingEdge(), faultContribution);
      }
    }

    rankedList = pFaults;
    htmlWriter = new FaultReportWriter();
  }

  public int getRankOfSet(Fault set) {
    return rankedList.indexOf(set);
  }

  @Override
  public String toString() {
    StringBuilder toString = new StringBuilder();
    if(!rankedList.isEmpty()){
      toString.append(rankedList.stream().map(l -> l.toString()).collect(Collectors.joining("\n\n")));
    }
    return toString.toString();
  }

  /**
   * Transform a set of sets of CFAEdges to a set of Faults.
   *
   * @param pErrorIndicators possible candidates for the error
   * @return FaultLocalizationOutputs of the CFAEdges.
   */
  public static Set<Fault> transform(
      Set<Set<CFAEdge>> pErrorIndicators) {
    Set<Fault> transformed = new HashSet<>();
    for (Set<CFAEdge> errorIndicator : pErrorIndicators) {
      transformed.add(new Fault(
          errorIndicator.stream().map(FaultContribution::new).collect(Collectors.toSet())));
    }
    return transformed;
  }

  public void faultsToJSON(Writer pWriter) throws IOException {
    List<Map<String, Object>> faults = new ArrayList<>();
    for (int i = 0; i < rankedList.size(); i++) {
      Fault fault = rankedList.get(i);
      Map<String, Object> faultMap = new HashMap<>();
      faultMap.put("rank", (i+1));
      faultMap.put("score", (int) (100 * fault.getScore()));
      faultMap.put("reason", htmlWriter.toHtml(fault));
      faultMap.put("lines", fault.sortedLineNumbers());
      faultMap.put("descriptions", descriptionsOfFault(fault));
      faults.add(faultMap);
    }
    JSON.writeJSONString(faults ,pWriter);
  }

  /**
   * Append additional information to the CounterexampleInfo output
   * @param elem maps a property of edge to an object
   * @param edge the edge that is currently transformed into JSON format.
   */
  @Override
  protected void addAdditionalInfo(Map<String, Object> elem, CFAEdge edge) {
    elem.put("additional", "");
    elem.put("faults", new ArrayList<>());
    FaultContribution fc = mapEdgeToFaultContribution.get(edge);
    if(fc != null){
      if(fc.hasReasons()){
        elem.put("additional", "<br><br><strong>Additional information provided:</strong><br>" + htmlWriter.toHtml(fc));
      }
    }
    if(mapEdgeToFaults.containsKey(edge)){
      elem.put("faults", mapEdgeToFaults.get(edge));
    }
  }

  protected List<String> descriptionsOfFault(Fault fault){
    return fault
        .stream()
        .sorted(Comparator.comparingInt(fc -> fc.correspondingEdge().getFileLocation().getStartingLineInOrigin()))
        .map(fc -> {
          CFAEdge cfaEdge = fc.correspondingEdge();
          if(cfaEdge.getEdgeType().equals(CFAEdgeType.FunctionReturnEdge)){
            return Splitter.on(":").split(cfaEdge.getDescription()).iterator().next();
          }
          return fc.correspondingEdge().getDescription();
        })
        .collect(Collectors.toList());
  }


  public void replaceHtmlWriter(FaultReportWriter pFaultToHtml){
    htmlWriter = pFaultToHtml;
  }

  /**
   * Replace default CounterexampleInfo with this extended version of a CounterexampleInfo.
   */
  public void apply(){
    parent.getTargetPath().getLastState().replaceCounterexampleInformation(this);
  }
}