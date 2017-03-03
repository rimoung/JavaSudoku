package cspSolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cspSolver.BTSolver.ConsistencyCheck;
import cspSolver.BTSolver.ValueSelectionHeuristic;
import cspSolver.BTSolver.VariableSelectionHeuristic;
import sudoku.Converter;
import sudoku.SudokuFile;
/**
 * Backtracking solver. 
 *
 */
public class BTSolver implements Runnable{

	//===============================================================================
	// Properties
	//===============================================================================

	private ConstraintNetwork network;
	private static Trail trail = Trail.getTrail();
	private boolean hasSolution = false;
	private SudokuFile sudokuGrid;

	private int numAssignments;
	private int numBacktracks;
	private long startTime;
	private long endTime;
	private long timeOutSeconds; 	// KK: timeout limit in seconds
	private String stats;		 	// KK: for writing stats to output
	private long totalStartTime;	// KK: start of main; set this arg
	private String status;			// KK: keeps track of status
	
	public enum VariableSelectionHeuristic 	{ None, MinimumRemainingValue, Degree };
	public enum ValueSelectionHeuristic 		{ None, LeastConstrainingValue };
	public enum ConsistencyCheck				{ None, ForwardChecking, ArcConsistency };
	
	private HashSet<VariableSelectionHeuristic> varHeuristics = new HashSet<VariableSelectionHeuristic>();
	private ValueSelectionHeuristic valHeuristics;
	private ConsistencyCheck cChecks;

	//===============================================================================
	// Constructors
	//===============================================================================

	public BTSolver(SudokuFile sf)
	{
		this.network = Converter.SudokuFileToConstraintNetwork(sf);
		this.sudokuGrid = sf;
		numAssignments = 0;
		numBacktracks = 0;
	}

	//===============================================================================
	// Modifiers
	//===============================================================================
	
	public void setVariableSelectionHeuristic(VariableSelectionHeuristic vsh)
	{
//		this.varHeuristics = vsh;
//		this.varHeuristics.add(vsh);
		this.varHeuristics.add(vsh);
		
	}
	
	public void setValueSelectionHeuristic(ValueSelectionHeuristic vsh)
	{
		this.valHeuristics = vsh;
	}
	
	public void setConsistencyChecks(ConsistencyCheck cc)
	{
		this.cChecks = cc;
	}
	
	public void setTotalStartTime(long fromMainRun) 
	{
		this.totalStartTime = fromMainRun;
	}
	
	
	//===============================================================================
	// Accessors
	//===============================================================================

	/** 
	 * @return a String with the output file specification. 
	 */
	// KK: note that all time should be in seconds
	public void solverStats(long endTime, String status) {
		long solutionTime = (endTime-startTime)/1000;
		
		ArrayList<Integer> solution;
		if (hasSolution()) {
			solution = getSolution().boardToArray();
			status = "success";
		}
		else {
			solution = new ArrayList<Integer>(Collections.nCopies(sudokuGrid.getN()*sudokuGrid.getN(),
											  0) );
			if (solutionTime > timeOutSeconds)
				status = "timeout";
			else
				status = "error";
		}
		
		//TODO changed to milliseconds. 
		//To change back to seconds, divide all of the times by 1000
		this.stats = String.format("TOTAL_START=%d 		  \n" + 
								   "PREPROCESSING_START=%d\n" +
								   "PREPROCESSING_DONE=%d \n" +
								   "SEARCH_START=%d 	  \n" +
								   "SEARCH_DONE=%d 		  \n" +
								   "SOLUTION_TIME=%d 	  \n" +
								   "STATUS=%s 			  \n" +
								   "SOLUTION=%s 		  \n" +
								   "COUNT_NODES=%d 	  	  \n" +
								   "COUNT_DEADENDS=%d 	  \n" , 
		(totalStartTime-totalStartTime), 
		(startTime-totalStartTime), 
		(startTime-totalStartTime), 
		(startTime-totalStartTime), 
		(endTime-totalStartTime),	// search_done
		solutionTime, 
		status,
		solution.toString(),
		getNumAssignments(), 
		getNumBacktracks() );
	}
		
	
	public String getSolverStats() 
	{ 
		return this.stats;
	}
	
	public long getEndTime() {
		return this.endTime;
	}
	
	public String getStatus() {
		return this.status;
	}
	
	public void setTimeOutSeconds(long num) 
	{
		this.timeOutSeconds = num;
	}
	
	/** 
	 * @return true if a solution has been found, false otherwise. 
	 */
	public boolean hasSolution()
	{
		return hasSolution;
	}

	/**
	 * @return solution if a solution has been found, otherwise returns the unsolved puzzle.
	 */
	public SudokuFile getSolution()
	{
		return sudokuGrid;
	}

	public void printSolverStats()
	{
		System.out.println("Time taken:" + (endTime-startTime) + " ms");
		System.out.println("Number of assignments: " + numAssignments);
		System.out.println("Number of backtracks: " + numBacktracks);
	}

	/**
	 * 
	 * @return time required for the solver to attain in seconds
	 */
	public long getTimeTaken()
	{
		return endTime-startTime;
	}

	public int getNumAssignments()
	{
		return numAssignments;
	}

	public int getNumBacktracks()
	{
		return numBacktracks;
	}

	public ConstraintNetwork getNetwork()
	{
		return network;
	}

	//===============================================================================
	// Helper Methods
	//===============================================================================

	/**
	 * Checks String token to see which heuristic or check needs to be turned on. 
	 * 
	 */
	public void turnOnHeuristics(ArrayList<String> options) {
		this.setConsistencyChecks(ConsistencyCheck.None);
		this.setValueSelectionHeuristic(ValueSelectionHeuristic.None);
		this.setVariableSelectionHeuristic(VariableSelectionHeuristic.None);
		
		if (options.size() > 0) {
			for (String option: options) {
				if (option.equals("FC")) 
					this.setConsistencyChecks(ConsistencyCheck.ForwardChecking);
				else if (options.equals("AC"))
					this.setConsistencyChecks(ConsistencyCheck.ArcConsistency);
				else if (option.equals("MRV")) {
					this.varHeuristics.remove(VariableSelectionHeuristic.None);
					this.setVariableSelectionHeuristic(VariableSelectionHeuristic.MinimumRemainingValue);
				}
				else if (option.equals("DH")) {
					this.setVariableSelectionHeuristic(VariableSelectionHeuristic.Degree);
					this.varHeuristics.remove(VariableSelectionHeuristic.None);
				}
				else if (option.equals("LCV"))
					this.setValueSelectionHeuristic(ValueSelectionHeuristic.LeastConstrainingValue);
				
			}
		}
		//TODO remove this
		System.out.println(this.valHeuristics);
		System.out.println(this.varHeuristics);
		System.out.println(this.cChecks);
	}
	
	
	/**
	 * Checks whether the changes from the last time this method was called are consistent. 
	 * @return true if consistent, false otherwise
	 */
	private boolean checkConsistency()
	{
		boolean isConsistent = false;
		switch(cChecks)
		{
		case None: 				isConsistent = assignmentsCheck();
		break;
		case ForwardChecking: 	isConsistent = forwardChecking();
		break;
		case ArcConsistency: 	isConsistent = arcConsistency();
		break;
		default: 				isConsistent = assignmentsCheck();
		break;
		}
		return isConsistent;
	}
	
	/**
	 * default consistency check. Ensures no two variables are assigned to the same value.
	 * @return true if consistent, false otherwise. 
	 */
	private boolean assignmentsCheck()
	{
		for(Variable v : network.getVariables()) {
			if(v.isAssigned()) {
				for(Variable vOther : network.getNeighborsOfVariable(v)) {
					vOther.removeValueFromDomain(v.getAssignment());
					if (v.getAssignment() == vOther.getAssignment()) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	private boolean forwardChecking()
	{
		for(Variable v: network.getVariables()) {
			if(v.isAssigned()) {
				for(Variable vOther: network.getNeighborsOfVariable(v)) {
					vOther.removeValueFromDomain(v.getAssignment());
					if(vOther.getDomain().isEmpty()) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	
	/**
	 * TODO: Implement Maintaining Arc Consistency.
	 */
	private boolean arcConsistency()
	{
		return false;
	}
	
	/**
	 * Selects the next variable to check.
	 * @return next variable to check. null if there are no more variables to check. 
	 */
	private Variable selectNextVariable()
	{	
		Variable next = null;
		if(varHeuristics.contains(VariableSelectionHeuristic.None)) {
			next = getfirstUnassignedVariable();
		}
		else if (varHeuristics.size() == 2){
			next = getMRVandDH();
		}
		else {
			if(varHeuristics.contains(VariableSelectionHeuristic.MinimumRemainingValue)) {
				next = getMRV();
			}
			else if(varHeuristics.contains(VariableSelectionHeuristic.Degree)) {
				next = getDegree();
			}

			

		}
		return next;
	}
//	{
//		Variable next = null;
//		switch(varHeuristics)
//		{
//		case None: 					next = getfirstUnassignedVariable();
//		break;
//		case MinimumRemainingValue: next = getMRV();
//		break;
//		case Degree:				next = getDegree();
//		break;
//		default:					next = getfirstUnassignedVariable();
//		break;
//		}
//		return next;
//	}
	
	/**
	 * default next variable selection heuristic. Selects the first unassigned variable. 
	 * @return first unassigned variable. null if no variables are unassigned. 
	 */
	private Variable getfirstUnassignedVariable()
	{
		for(Variable v : network.getVariables())
		{
			if(!v.isAssigned())
			{
				return v;
			}
		}
		return null;
	}

	private Variable getMRVandDH() {
		ArrayList<Variable> minVariables = new ArrayList<Variable>();
		int minVariableSize = Integer.MAX_VALUE;
		
		for (Variable v: network.getVariables()) {
			if (!v.isAssigned()) {
				if (v.size() < minVariableSize) {
					minVariableSize = v.size();
					minVariables.clear();
					minVariables.add(v);
				}
				else if(v.size() == minVariableSize) {
					minVariables.add(v);
				}
			}
		}
		return getDegreeHelper(minVariables);
	}
	
	
	/**
	 * @return variable with minimum remaining values that isn't assigned, null if all variables are assigned. 
	 */
	private Variable getMRV()
	{	
		Variable minimumVariable = getfirstUnassignedVariable();
		
		for (Variable v: network.getVariables()) {
			if (!v.isAssigned()) {
				if (v.size() < minimumVariable.size()) {
					minimumVariable = v;
				}
			}
		}		
		return minimumVariable;
	}
	
	/**
	 * Run Degree Heuristic on a particular list of Variables.
	 * @return the variable constrained by the most unassigned variables, null if all vars are assigned.
	 * 
	 */
	private Variable getDegreeHelper(List<Variable> variables) {
//		System.out.println("new move!");

		Variable DHVar = null;
		int highestDegree = 0;
		int currDegree=0;
		for(Variable v: variables) {
			if(!v.isAssigned()) {
				currDegree = getDegreeOfVariable(v);
//				System.out.println(Integer.toString(currDegree) + v);
				if(currDegree > highestDegree) {
					highestDegree = currDegree;
					DHVar = v;
				}	
			}
		}
//		System.out.println("**chosen: " + DHVar);
		return DHVar;
	}	
	
	private int getDegreeOfVariable(Variable v) {
		int currDegree = 0;
		for(Constraint c: network.getConstraintsContainingVariable(v)) {
			for(Variable var: c.vars) {
				if(!var.isAssigned()) {
					currDegree++;
				}
			}
		}
		return currDegree;
	}
	
	
	/**
	 * @return variable constrained by null if all variables are assigned.
	 */
	private Variable getDegree()
	{	
		Variable mostConstrainedVar = null;
		mostConstrainedVar = getDegreeHelper(network.getVariables());
		return mostConstrainedVar;
		
	}
	
	/**
	 * Value Selection Heuristics. Orders the values in the domain of the variable 
	 * passed as a parameter and returns them as a list.
	 * @return List of values in the domain of a variable in a specified order. 
	 */
	public List<Integer> getNextValues(Variable v)
	{
		List<Integer> orderedValues;
		switch(valHeuristics)
		{
		case None: 						orderedValues = getValuesInOrder(v);
		break;
		case LeastConstrainingValue: 	orderedValues = getValuesLCVOrder(v);
		break;
		default:						orderedValues = getValuesInOrder(v);
		break;
		}
		return orderedValues;
	}
	
	/**
	 * Default value ordering. 
	 * @param v Variable whose values need to be ordered
	 * @return values ordered by lowest to highest. 
	 */
	public List<Integer> getValuesInOrder(Variable v)
	{
		List<Integer> values = v.getDomain().getValues();
		
		Comparator<Integer> valueComparator = new Comparator<Integer>(){

			@Override
			public int compare(Integer i1, Integer i2) {
				return i1.compareTo(i2);
			}
		};
		Collections.sort(values, valueComparator);
		return values;
	}
	
	private int getConstraintsOnVarWithValue(Variable v, Integer value) {
		//the one that rules out the fewest values in the remaining variables
		//do we need to deal with fc?
		int LC = 0;
		for(Constraint c: network.getConstraintsContainingVariable(v)) {
			for(Variable var : c.vars) {
				if(!var.isAssigned() && var != v) {
					if(var.getDomain().getValues().contains(value)) {
						LC++;
					}
				}
			}
		}		
		return LC;
	}
	
	public List<Integer> getValuesLCVOrder(Variable v)
	{
		HashMap<Integer, Integer> valuesToConstraints = new HashMap<Integer, Integer>();
		for(Integer value : v.getDomain().getValues()) {
			int numConstraintsOnValue = getConstraintsOnVarWithValue(v, value);
			valuesToConstraints.put(value, numConstraintsOnValue);
		}
		
		getSortedKeys(valuesToConstraints);
		return getSortedKeys(valuesToConstraints);
	}
	
	/*
	 * Takes in a HashMap of the LCV 
	 */
	private List<Integer> getSortedKeys(HashMap<Integer,Integer> hm) {
		List<Entry<Integer, Integer>> list = new LinkedList<Entry<Integer, Integer>>(hm.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>() {
			public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});
		List<Integer> sortedKeys = new ArrayList<Integer>();
		for(Iterator<Entry<Integer, Integer>> it = list.iterator(); it.hasNext();) {
			Entry<Integer, Integer> e = it.next();
			sortedKeys.add(e.getKey());
		}
		return sortedKeys;
	}
	/**
	 * Called when solver finds a solution
	 */
	private void success()
	{
		hasSolution = true;
		sudokuGrid = Converter.ConstraintNetworkToSudokuFile(network, sudokuGrid.getN(), sudokuGrid.getP(), sudokuGrid.getQ());
		status = "success";
		//solverStats(System.currentTimeMillis(), "success");
	}
	
	/**
	 * Called when solver times out to print appropriate stats
	 */
//	private void timeout() 
//	{
//		solverStats(System.currentTimeMillis(), "timeout");
//	}
//	
//	private void error() 
//	{
//		solverStats(System.currentTimeMillis(), "error");
//	}

	//===============================================================================
	// Solver
	//===============================================================================

	/**
	 * Method to start the solver
	 */
	public void solve()
	{
		startTime = System.currentTimeMillis();
		try {
			solve(0);
		}
		catch (VariableSelectionException e)
		{
			System.out.println("error with variable selection heuristic.");
			status = "error";
		}
		endTime = System.currentTimeMillis();
		Trail.clearTrail();
	}

	/**
	 * Solver
	 * @param level How deep the solver is in its recursion. 
	 * @throws VariableSelectionException 
	 */

	private void solve(int level) throws VariableSelectionException
	{	
		if (((System.currentTimeMillis()-startTime)/1000) > timeOutSeconds) 
		{
			status = "timeout";
			return;
		}

		//System.out.println("Time: " + (System.currentTimeMillis()-startTime)/1000);
		if(!Thread.currentThread().isInterrupted())

		{//Check if assignment is completed
			if(hasSolution)
			{
				return;
			}

			//Select unassigned variable
			Variable v = selectNextVariable();		

			//check if the assignment is complete
			if(v == null)
			{
				for(Variable var : network.getVariables())
				{
					if(!var.isAssigned())
					{
						throw new VariableSelectionException("Something happened with the variable selection heuristic");
					}
				}
				success();
				return;
			}

			//loop through the values of the variable being checked LCV

	
			for(Integer i : getNextValues(v))
			{	
//				if ((System.currentTimeMillis()-startTime)/1000 > timeOutSeconds) 
//				{
//					status = "timeout";
//					return;
//				}
				
				trail.placeBreadCrumb();

				//check a value
				v.updateDomain(new Domain(i));
				numAssignments++;
				boolean isConsistent = checkConsistency();
				
				//move to the next assignment
				if(isConsistent)
				{		
					solve(level + 1);
				}

				//if this assignment failed at any stage, backtrack
				if(!hasSolution)
				{
					trail.undo();
					numBacktracks++;
				}
				
				else
				{
					return;
				}
			}	
		}	
	}

	@Override
	public void run() {
		status = "";
		solve();
		solverStats(System.currentTimeMillis(), getStatus());
}
	
}
