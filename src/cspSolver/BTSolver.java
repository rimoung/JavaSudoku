package cspSolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
	
	public enum VariableSelectionHeuristic 	{ None, MinimumRemainingValue, Degree };
	public enum ValueSelectionHeuristic 		{ None, LeastConstrainingValue };
	public enum ConsistencyCheck				{ None, ForwardChecking, ArcConsistency, NakedPair };
	
	private VariableSelectionHeuristic varHeuristics;
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
		this.varHeuristics = vsh;
	}
	
	public void setValueSelectionHeuristic(ValueSelectionHeuristic vsh)
	{
		this.valHeuristics = vsh;
	}
	
	public void setConsistencyChecks(ConsistencyCheck cc)
	{
		this.cChecks = cc;
	}
	//===============================================================================
	// Accessors
	//===============================================================================

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
		case NakedPair:			isConsistent = nakedPair();	
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
		for(Variable v : network.getVariables())
		{
			if(v.isAssigned())
			{
				for(Variable vOther : network.getNeighborsOfVariable(v))
				{
					if (v.getAssignment() == vOther.getAssignment())
					{
						return false;
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * TODO: Implement forward checking. 
	 */
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
	 * TODO: Implement Naked Pairs
	 * @return true if culling naked pairs doesn't result in inconsistencies, false if graph becomes inconsistent
	 */
	private boolean nakedPair(){
		System.out.println("NAKED PAIRS");
		if(forwardChecking() == false) return false;
		ArrayList<Variable> checkedVars = new ArrayList<Variable>();	//list of tuples that have already been checked in this consistency check
		for(Variable v: network.getVariables()) {
			if(!v.isAssigned() && !checkedVars.contains(v) && v.getDomain().size() == 2) {														//if you've found an unassigned and unchecked pair
				for(Variable pair : network.getNeighborsOfVariable(v)){																			//look through the neighbors of this pair
					if(!pair.isAssigned() && pair.getDomain().size() == 2 && pair.getDomain().getValues().equals(v.getDomain().getValues())){	//if you've found a matching pair
						checkedVars.add(pair);														//add both parts of pair to the checked list, won't check them again this run
						checkedVars.add(v);
						int one = v.getDomain().getValues().get(0);									//get values of this pair
						int two = v.getDomain().getValues().get(1);
						for(Constraint c : network.getConstraintsContainingVariable(v)){			//get all constraints of first pair
							for(Constraint d : network.getConstraintsContainingVariable(pair)){		//get all constraints of second pair
								if(c==d){															//find constraints in common (at most two it would seem)
									for(Variable otherChecker: c.vars){								//look through all variables except "v" and "pair", removing all instances of these pairs' variables
										if(!otherChecker.equals(v) && !otherChecker.equals(pair)){
											otherChecker.getDomain().remove(one);
											otherChecker.getDomain().remove(two);
											if(otherChecker.getDomain().isEmpty()){
												return false;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return true;
	}
	/**
	 * Selects the next variable to check.
	 * @return next variable to check. null if there are no more variables to check. 
	 */
	private Variable selectNextVariable()
	{
		Variable next = null;
		switch(varHeuristics)
		{
		case None: 					next = getfirstUnassignedVariable();
		break;
		case MinimumRemainingValue: next = getMRV();
		break;
		case Degree:				next = getDegree();
		break;
		default:					next = getfirstUnassignedVariable();
		break;
		}
		return next;
	}
	
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

	/**
	 * TODO: Implement MRV heuristic
	 * @return variable with minimum remaining values that isn't assigned, null if all variables are assigned. 
	 */
	private Variable getMRV()	//NOTE: this will be *useless* without some sort of consistency check enabled
	{
		//boolean print = false;
		int minVals = 999999; //arbitrary
		Variable currentReturn = null;
		for(Variable v: network.getVariables()){
			if(!v.isAssigned()){
				if(v.getDomain().isEmpty()){	//if any variable has no valid assignments, immediately return null, nothing can be valid here 
					//System.out.println("NULL RETURNED");
					return null;
				}
				if(v.getDomain().size() <= minVals && v.getDomain().size() > 1){	//second part of this if probably unnecessary, but let's just keep it safe
					/*if(v.getDomain().size() == minVals){	//TODO: rework this to be a toggle, if "tiebreaker" = true
						if(getDegree(v) > getDegree(currentReturn)){
							currentReturn = v;
							minVals = v.getDomain().size();
							//System.out.println("SWITCHING BECAUSE OF DEGREE");
						}
					}
					else{*/
						currentReturn = v;
						minVals = v.getDomain().size();			//TODO: use degree heuristic as tiebreaker, IF that's what we're supposed to do
						//print = true;
					//}
				}
			}
		}
		//if(print) System.out.println("NOW RETURNING" + currentReturn.getName() + " domainsize: " + currentReturn.getDomain().size());
		return currentReturn;
	}
	private int getDegree(Variable v){	//same thing as degree heuristic below, but gets the degree of a specific variable
		int constraints = 0;
		for(Variable n : network.getNeighborsOfVariable(v)){
			if(!n.isAssigned()){
				constraints++;
			}
		}
		System.out.println("SOLVING WITH: " + constraints);
		return constraints;
	}
	
	/**
	 * TODO: Implement Degree heuristic
	 * @return variable constrained by the most unassigned variables, null if all variables are assigned.
	 */
	private Variable getDegree()  //TODO: implement "inverse degree" heuristic, selecting the variable with the *lowest* degree is more helpful for sudoku
	{
		int constraints = 0, maxConstraints = -1;
		Variable returnValue = null;
		boolean print = false;
		for(Variable v: network.getVariables()){
			if(!v.isAssigned()){
				constraints = 0;
				for(Variable n : network.getNeighborsOfVariable(v)){
					if(!n.isAssigned()) constraints++;
				}
				if(constraints >= maxConstraints){
					maxConstraints = constraints;
					returnValue = v;
					print = true;
				}
			}
		}
		//System.out.println("NOW RETURNING" + returnValue.getName() + " constraints: " + maxConstraints);
		return returnValue;
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
	
	/**
	 * TODO: LCV heuristic
	 */
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
		//status = "success";
	}

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
		}catch (VariableSelectionException e)
		{
			System.out.println("error with variable selection heuristic.");
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
		//status = "";
		solve();
		//solverStats(System.currentTimeMillis(), getStatus());
	}
}
