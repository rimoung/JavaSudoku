package scripts;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

import cspSolver.BTSolver;
import cspSolver.BTSolver.ConsistencyCheck;
import cspSolver.BTSolver.ValueSelectionHeuristic;
import cspSolver.BTSolver.VariableSelectionHeuristic;
import sudoku.SudokuBoardGenerator;
import sudoku.SudokuBoardReader;
import sudoku.SudokuFile;

// main class to run backtracking
public class BTRun {

	public static void main(String[] args) {
		String inputFile = ""; 
		String outputFile = "";
		long timeOutSeconds = 10000;
		ArrayList<String> options = new ArrayList<String>(); // for algorithm/search specifications
		long startTime = System.currentTimeMillis();
		
		// extract cmd line args 
		for (int i = 0; i < args.length; i++) {
			if (i == 0) 
				inputFile = args[i];
			else if (i == 1)
				outputFile = args[i];
			else if (i == 2) {
				try {
					timeOutSeconds = Long.parseLong(args[i]);
				} 
				catch (NumberFormatException e) {
					System.out.println("NumberFormatException: " + e.getMessage());
				} 
			}
			else 
				options.add(args[i]);
		}
		
		// i/o files check
		if (inputFile.length() == 0 || outputFile.length() == 0) {
			System.out.println("FILE ARGUMENT ERROR: Invalid arguments.");
			return;
		}
		
//		 //print out cmd line args
		System.out.println("DONE.");
		System.out.println(inputFile + " " + 
						   outputFile + " " + 
						   Long.toString(timeOutSeconds) + " " +  
						   options.toString() );
		
		SudokuFile sudokuBoard = SudokuBoardReader.readFile(inputFile);
		System.out.printf("BOARD\n%s\n------\n",sudokuBoard);
		BTSolver BTsolver = new BTSolver(sudokuBoard);
		BTsolver.turnOnHeuristics(options);
		BTsolver.setTotalStartTime(startTime);
		BTsolver.setTimeOutSeconds(timeOutSeconds);
		
		Thread t1 = new Thread(BTsolver);
		try {
			t1.start();
			t1.join(timeOutSeconds*1000);
			if( t1.isAlive() ) {
				t1.interrupt();
			}
		}
		catch(InterruptedException e){}
		
			
		// write output file
		PrintWriter outputWriter = null;
		try {
			outputWriter = new PrintWriter(outputFile);
			outputWriter.format(BTsolver.getSolverStats());
		}
		catch (Exception e) {
			System.out.println(e.toString());
		}
		finally {
			if (outputWriter != null) {
				outputWriter.close();
			}
		}
		System.out.println("\n\nNumAssignments: " + BTsolver.getNumAssignments());
		System.out.println("NumBacktracks: " + BTsolver.getNumBacktracks());
		System.out.println(BTsolver.getSolution());
		System.out.printf("\nThe file (%s) has been successfully printed.\n", outputFile);
		
	}

}
