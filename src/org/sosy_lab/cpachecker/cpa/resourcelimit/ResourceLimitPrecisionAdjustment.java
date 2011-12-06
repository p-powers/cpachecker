/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.resourcelimit;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.logging.Level;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;


class ResourceLimitPrecisionAdjustment implements PrecisionAdjustment {

  private final LogManager logger;

  //necessary stuff to query the OperatingSystemMBean for the process cpu time
  private final MBeanServer mbeanServer;
  private final ObjectName osMbean;
  private static final String PROCESS_CPU_TIME = "ProcessCpuTime";
  private static final String MEMORY_SIZE = "CommittedVirtualMemorySize";

  private final MemoryMXBean memory;

  private boolean cpuTimeDisabled = false;
  private boolean processMemoryDisabled = false;

  ResourceLimitPrecisionAdjustment(LogManager pLogger) {
    logger = pLogger;

    mbeanServer = ManagementFactory.getPlatformMBeanServer();
    memory = ManagementFactory.getMemoryMXBean();

    try {
      osMbean = new ObjectName("java.lang", "type", "OperatingSystem");
    } catch (MalformedObjectNameException e) {
      // the name is hard-coded, so this exception should never occur
      throw new AssertionError(e);
    }
  }

  @Override
  public Triple<AbstractElement, Precision, Action> prec(AbstractElement pElement, Precision pPrecision,
      UnmodifiableReachedSet pElements) throws CPAException {

    ResourceLimitPrecision precision = (ResourceLimitPrecision)pPrecision;

    if (checkReachedSetSize(pElements, precision)) {
      logger.log(Level.WARNING, "Reached set size threshold reached, terminating.");
      return Triple.of(pElement, pPrecision, Action.BREAK);
    }

    if (checkWallTime(precision)) {
      logger.log(Level.WARNING, "Wall time threshold reached, terminating.");
      return Triple.of(pElement, pPrecision, Action.BREAK);
    }

    if (checkCpuTime(precision)) {
      logger.log(Level.WARNING, "Cpu time threshold reached, terminating.");
      return Triple.of(pElement, pPrecision, Action.BREAK);
    }

    if (checkHeapMemory(precision)) {
      logger.log(Level.WARNING, "Java heap memory threshold reached, terminating.");
      return Triple.of(pElement, pPrecision, Action.BREAK);
    }

    if (checkProcessMemory(precision)) {
      logger.log(Level.WARNING, "Process memory threshold reached, terminating.");
      return Triple.of(pElement, pPrecision, Action.BREAK);
    }

    return Triple.of(pElement, pPrecision, Action.CONTINUE);
  }


  private boolean checkReachedSetSize(UnmodifiableReachedSet elements, ResourceLimitPrecision precision) {

    long threshold = precision.getReachedSetSizeThreshold();
    if (threshold >= 0) {
      return (elements.size() > threshold);
    }

    return false;
  }

  private boolean checkWallTime(ResourceLimitPrecision precision) {
    return (System.currentTimeMillis() > precision.getWallTimeThreshold());
  }

  private boolean checkCpuTime(ResourceLimitPrecision precision) {
    if (cpuTimeDisabled) {
      return false;
    }
    long threshold = precision.getCpuTimeThreshold();
    if (threshold < 0) {
      return false;
    }

    Object cputimeObject;
    try {
      cputimeObject = mbeanServer.getAttribute(osMbean, PROCESS_CPU_TIME);
    } catch (JMException e) {
      logger.logDebugException(e, "Querying cpu time failed");
      logger.log(Level.WARNING, "Your Java VM does not support measuring the cpu time, cpu time threshold disabled");

      cpuTimeDisabled = true;
      return false;
    }

    if (!(cputimeObject instanceof Long)) {
      logger.log(Level.WARNING, "Invalid value received for cpu time: " + cputimeObject + ", cpu time threshold disabled");

      cpuTimeDisabled = true;
      return false;
    }

    long cputime = ((Long)cputimeObject) / (1000*1000);

    return (cputime > threshold);
  }

  private boolean checkHeapMemory(ResourceLimitPrecision precision) {
    long threshold = precision.getHeapMemoryThreshold();
    if (threshold < 0) {
      return false;
    }

    return (memory.getHeapMemoryUsage().getUsed() > threshold);
  }

  private boolean checkProcessMemory(ResourceLimitPrecision precision) {
    if (processMemoryDisabled) {
      return false;
    }
    long threshold = precision.getProcessMemoryThreshold();
    if (threshold < 0) {
      return false;
    }

    Object memUsedObject;
    try {
      memUsedObject = mbeanServer.getAttribute(osMbean, MEMORY_SIZE);
    } catch (JMException e) {
      logger.logDebugException(e, "Querying memory size failed");
      logger.log(Level.WARNING, "Your Java VM does not support measuring the memory size, process memory threshold disabled");

      processMemoryDisabled = true;
      return false;
    }

    if (!(memUsedObject instanceof Long)) {
      logger.log(Level.WARNING, "Invalid value received for memory size: " + memUsedObject + ", process memory threshold disabled");

      processMemoryDisabled = true;
      return false;
    }

    long memUsed = ((Long)memUsedObject) / (1024*1024);

    return (memUsed > threshold);
  }
}
