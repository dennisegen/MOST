package edu.rutgers.MOST.optimization.FBA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;

import edu.rutgers.MOST.data.*;
import edu.rutgers.MOST.optimization.solvers.*;

public class FBA {

	static Logger log = Logger.getLogger(FBA.class);
	
	private FBAModel model;
	private static Solver solver;
	private Vector<String> varNames;
	private String databaseName;
	private double maxObj;

	public FBA() {
		this.setSolver(SolverFactory.createSolver());
		this.varNames = new Vector<String>();
	}

	public FBA(FBAModel m) {
		this.model = m;
		this.setSolver(SolverFactory.createSolver());
		this.varNames = new Vector<String>();
	}

	public void setDatabaseName(String name) {
		this.databaseName = name;
	}

	private void setVars() {
		Vector<ModelReaction> reactions = this.model.getReactions();
		for (int i = 0; i < reactions.size(); i++) {
			SBMLReaction reac = (SBMLReaction) (reactions.elementAt(i));
			String varName = Integer.toString(reac.getId());
			double lb = reac.getLowerBound();
			double ub = reac.getUpperBound();

			this.getSolver().setVar(varName, VarType.CONTINUOUS, lb, ub);

			this.varNames.add(varName);
		}
	}
	
	private void setConstraints() {
		Vector<ModelReaction> reactions = this.model.getReactions();
		setConstraints(reactions,ConType.EQUAL,0.0);
	}	
	
	private void setConstraints(Vector<ModelReaction> reactions, ConType conType, double bValue) {
		ArrayList<Map<Integer, Double>> sMatrix = this.model.getSMatrix();
		for (int i = 0; i < sMatrix.size(); i++) {
			this.getSolver().addConstraint(sMatrix.get(i), conType, bValue);
		}
	}

	private void setObjective() {
		this.getSolver().setObjType(ObjType.Maximize);
		Vector<Double> objective = this.model.getObjective();

		Map<Integer, Double> map = new HashMap<Integer, Double>();
		for (int i = 0; i < objective.size(); i++) {
			if (objective.elementAt(i) != 0.0) {
				map.put(i, objective.elementAt(i));
			}
		}
		this.getSolver().setObj(map);
		
	}

	public void setFBAModel(FBAModel m) {
		this.model = m;
	}

	public ArrayList<Double> run() {
		log.debug("Set Vars");
		this.setVars();
		log.debug("setConstraints");
		this.setConstraints();
		log.debug("setObjective");
		this.setObjective();
		log.debug("optimize");
		this.maxObj = this.getSolver().optimize();
		
		return this.getSolver().getSoln();
	}

	public double getMaxObj() {
		return this.maxObj;
	}

	public static void main(String[] argv) {
		String databaseName = "Ec_iAF1260_anaerobic_glc10_acetate";
				
		FBA fba = new FBA();
		fba.setDatabaseName(databaseName);
		
		FBAModel model = new FBAModel(databaseName);	
		fba.setFBAModel(model);
		 
		fba.run();		
		
		System.out.println("Max objective: " + fba.getMaxObj());
	}
	
	public static Solver getSolver() {
		return solver;
	}

	public static void setSolver(Solver solver) {
		FBA.solver = solver;
	}
}
