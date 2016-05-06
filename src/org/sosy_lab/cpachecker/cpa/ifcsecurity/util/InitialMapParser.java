/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.ifcsecurity.util;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.policies.PredefinedPolicies;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.policies.SecurityClasses;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
/**
 * Used for Parsing the allowed SecurityClass-Mapping.
 */
public class InitialMapParser {

  /**
   * Internal variable that contains the SecurityClass-Mapping
   */
  private Map<Variable,SecurityClasses> map=new TreeMap<>();

  /**
   * Starts and execute the InitialMapParser for parsing the allowed SecurityClass-Mapping.
  * @param file the file to be parsed.
   */
  public InitialMapParser(LogManager logger, String file) throws FileNotFoundException, IOException{
    map=new TreeMap<>();

    FileInputStream fstream;
    try {
      fstream = new FileInputStream(file);
      BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

      //Read File Line By Line
      String strLine= br.readLine();
      while (strLine  != null)   {
        if(strLine.contains("=") && strLine.contains(";")){
          int eqsign=strLine.indexOf("=");
          int sem=strLine.indexOf(";");
          assert(eqsign<sem);
          Variable var=new Variable(strLine.substring(0, eqsign));
          Field f=PredefinedPolicies.class.getField(strLine.substring(eqsign+1, sem));
          SecurityClasses clas=(SecurityClasses)(f.get(null));
          if(!map.containsKey(var)){
            map.put(var, clas);
          }
        }
        strLine=br.readLine();
      }

      //Close the input stream
      br.close();
    } catch (Exception e) {
      logger.log(Level.WARNING, e.toString());
    }
  }

  /**
   * Returns the parsed allowed SecurityClass-Mapping.
   * @return SecurityClass-Mapping.
   */
  public Map<Variable,SecurityClasses> getInitialMap(){
    return map;
  }
}
