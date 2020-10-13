package org.sosy_lab.cpachecker.core.algorithm.legion;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.testcase.XMLTestCaseExport;

public class OutputWriter {

    private int testCaseNumber;
    private LogManager logger;
    private PredicateCPA PredicateCPA;
    private String path;

    public OutputWriter(LogManager pLogger, PredicateCPA pPredicateCPA, String pPath) {
        testCaseNumber = 0;
        logger = pLogger;
        PredicateCPA = pPredicateCPA;
        path = pPath;

        initOutDir(path);
        writeTestMetadata();
    }

    private void initOutDir(String pPath){
        File outpath = new File(pPath);
        outpath.mkdirs();
    }

    /**
     * Write the metadata file necessary for testcomp to the output path.
     * 
     * This only needs to be done once and does not contain testcase specific
     * information.
     */
    private void writeTestMetadata() {
        try (FileWriter metadata = new FileWriter(this.path + "/metadata.xml")) {
            XMLTestCaseExport.writeXMLMetadata(metadata, PredicateCPA.getCfa(), null, "legion");
            metadata.flush();
        } catch (IOException exc) {
            logger.log(Level.SEVERE, "Could not write metadata file", exc);
        }
    }

    /**
     * Handles writing of all testcases necessary for the given reachedSet.
     */
    public void writeTestCases(ReachedSet pReachedSet) {
        AbstractState first = pReachedSet.getFirstState();
        ARGState args = AbstractStates.extractStateByType(first, ARGState.class);

        ArrayList<Entry<MemoryLocation, ValueAndType>> values = new ArrayList<>();
        searchTestCase(args, values);
        try {
            writeVariablesToTestcase(values);
        } catch (IOException exc) {
            logger.log(Level.SEVERE, "Could not write test output", exc);
        }

    }

    /**
     * Search through connected states starting from the state given and return
     * their MemoryLocation and ValueType.
     * 
     * This performs walk along the children of state. The child to walk down to
     * is selected by the highest state id (meaning the newest).
     * This results in a list of values starting at the given state and walking 
     * the newest path through it's children.
     * 
     * @param state The starting state.
     * @param values The list of values to append to.
     */
    private void
            searchTestCase(ARGState state, ArrayList<Entry<MemoryLocation, ValueAndType>> values) {
        // check if is nondet assignment
        LocationState ls = AbstractStates.extractStateByType(state, LocationState.class);
        Iterable<CFAEdge> incoming = ls.getIngoingEdges();
        for (CFAEdge edge : incoming) {
            if (edge.getEdgeType() == CFAEdgeType.StatementEdge) {
                CStatement statement = ((CStatementEdge) edge).getStatement();
                if (statement instanceof CFunctionCallAssignmentStatement) {
                    CFunctionCallAssignmentStatement assignment =
                            ((CFunctionCallAssignmentStatement) statement);
                    CFunctionCallExpression right_hand = assignment.getRightHandSide();
                    if (right_hand.toString().startsWith("__VERIFIER_nondet_")) {
                        // CHECK!
                        String function_name = ls.getLocationNode().getFunctionName();
                        String identifier =
                                ((CIdExpression) assignment.getLeftHandSide()).getName();
                        @Nullable
                        ValueAnalysisState vs =
                                AbstractStates.extractStateByType(state, ValueAnalysisState.class);
                        Entry<MemoryLocation, ValueAndType> vt =
                                getValueTypeFromState(function_name, identifier, vs);
                        values.add(vt);
                    }
                }
            }
        }

        // find largest child state
        ARGState largest_child = null;
        for (ARGState child : state.getChildren()) {
            if (largest_child == null || largest_child.getStateId() < child.getStateId()) {
                largest_child = child;
            }
        }

        // If largest_child still null -> at the bottom of the graph
        if (largest_child == null) {
            return;
        }

        // If not, search in largest_child
        searchTestCase(largest_child, values);
    }

    /**
     * Write variables from values to a testcase file.
     */
    private void writeVariablesToTestcase(ArrayList<Entry<MemoryLocation, ValueAndType>> values)
            throws IOException {

        String filename = String.format("/testcase_%s.xml", this.testCaseNumber);
        logger.log(Level.WARNING, "Writing testcase ", filename);

        try (FileWriter testcase = new FileWriter(this.path + filename)) {
            testcase.write("<testcase>\n");
            for (Entry<MemoryLocation, ValueAndType> v : values) {
                String name = v.getKey().toString();
                String type = v.getValue().getType().toString();
                Value value = v.getValue().getValue();

                String value_str = "";
                if (type.equals("int")) {
                    value_str = String.valueOf(((NumericValue) value).longValue());
                }

                testcase.write(
                        String.format(
                                "\t<input variable=\"%s\" type=\"%s\">%s</input>\n",
                                name,
                                type,
                                value_str));
            }

            testcase.write("</testcase>\n");
            testcase.flush();
        }
        this.testCaseNumber += 1;
    }

    /**
     * Retrieve variables a MemoryLocation and ValueAndType from a
     * ValueAnalysisState by its function name and identifier (=name).
     * 
     * @param function_name Name of the function the variable is contained in.
     * @param identifier The name of the function.
     * @return The constants entry for this value or null.
     */
    private static Entry<MemoryLocation, ValueAndType> getValueTypeFromState(
            String function_name,
            String identifier,
            ValueAnalysisState state) {
        for (Entry<MemoryLocation, ValueAndType> entry : state.getConstants()) {
            MemoryLocation loc = entry.getKey();
            if (loc.getFunctionName().equals(function_name)
                    && loc.getIdentifier().equals(identifier)) {
                return entry;
            }
        }
        return null;
    }
}