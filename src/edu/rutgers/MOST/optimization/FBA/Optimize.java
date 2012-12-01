package edu.rutgers.MOST.optimization.FBA;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;

import edu.rutgers.MOST.config.LocalConfig;
import edu.rutgers.MOST.data.*;
import edu.rutgers.MOST.optimization.solvers.*;
import edu.rutgers.MOST.presentation.GraphicalInterface;
import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBVar;

public class Optimize {

	static Logger log = Logger.getLogger(Optimize.class);
	
	private FBAModel model;
	private static Solver solver;
	private Vector<String> varNames;
	private String databaseName;
	private double maxFlux;

	private class Pair {
		public int id;
		public double stoic;

		public Pair(int id, double stoic) {
			this.id = id;
			this.stoic = stoic;
		}
	}

	public Optimize() {
		this.setSolver(SolverFactory.createSolver());
		this.varNames = new Vector<String>();
	}

	public Optimize(FBAModel m) {
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
/*		Hashtable ht = new Hashtable();
		
		ReactantFactory rFactory = new ReactantFactory();
		ProductFactory pFactory = new ProductFactory();
		for (int i = 0; i < reactions.size(); i++) {

			SBMLReaction reac = (SBMLReaction) (reactions.elementAt(i));
			String varName = Integer.toString(reac.getId());
			ArrayList<ModelReactant> reacts = rFactory
					.getReactantsByReactionId(reac.getId(), "SBML",
							databaseName);
			for (int j = 0; j < reacts.size(); j++) {
				SBMLReactant r = (SBMLReactant) reacts.get(j);
				if (ht.containsKey(r.getMetaboliteId())) {
					Vector<Pair> pairs = (Vector<Pair>) ht.get(r
							.getMetaboliteId());
					pairs.add(new Pair(reac.getId(), r.getStoic()));

				} else {
					Vector<Pair> pairs = new Vector<Pair>();
					pairs.add(new Pair(reac.getId(), r.getStoic()));

					ht.put(r.getMetaboliteId(), pairs);
				}
			}

			ArrayList<ModelProduct> products = pFactory
					.getProductsByReactionId(reac.getId(), "SBML", databaseName);
			for (int j = 0; j < products.size(); j++) {
				SBMLProduct p = (SBMLProduct) products.get(j);
				if (ht.containsKey(p.getMetaboliteId())) {
					Vector<Pair> pairs = (Vector<Pair>) ht.get(p
							.getMetaboliteId());
					pairs.add(new Pair(reac.getId(), (-1.0) * p.getStoic()));
				} else {
					Vector<Pair> pairs = new Vector<Pair>();
					pairs.add(new Pair(reac.getId(), (-1.0) * p.getStoic())); //DEGEN: These are on the right side of the equation, so we need to negate
					ht.put(p.getMetaboliteId(), pairs);
				}
			}
		}

		Enumeration e = ht.keys();
		Map<Integer, Double> map = new HashMap<Integer, Double>();
		while (e.hasMoreElements()) {
			int metaboliteId = (Integer) e.nextElement();
			System.out.println("Metabolite ID = " + metaboliteId);
			Vector<Pair> pairs = (Vector<Pair>) ht.get(metaboliteId);
			map.clear();
			for (int i = 0; i < pairs.size(); i++) {
				if (!this.varNames
						.contains(Integer.toString(pairs.elementAt(i).id))) {
					System.out.println("ERROR");
				}
				Pair test = pairs.elementAt(i);
				map.put(this.varNames.indexOf(Integer.toString(pairs
						.elementAt(i).id)), pairs.elementAt(i).stoic);
				System.out.println("reaction:" + pairs
						.elementAt(i).id + "stoic=" + pairs.elementAt(i).stoic);
			}
			this.getSolver().addConstraint(map, conType, bValue);
		}	*/
		
		Vector<Map<Integer, Double>> sMatrix = this.model.getSMatrix();
		for (Integer i = 0; i < sMatrix.size(); i++) {
			this.getSolver().addConstraint(sMatrix.elementAt(i), conType, bValue);
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

	public void optimize() {
		log.debug("Set Vars");
		this.setVars();
		log.debug("setConstraints");
		this.setConstraints();
		log.debug("setObjective");
		this.setObjective();
		log.debug("optimize");
		this.maxFlux = this.getSolver().optimize();
	}

	public double getmaxFlux() {
		return this.maxFlux;
	}

	public static void main(String[] argv) {
		String databaseName = "Ec_core_flux1_no_boundaries";
				
		Optimize opt = new Optimize();
		opt.setDatabaseName(databaseName);//should be optimizePath once the copier is implemented
		
		FBAModel model = new FBAModel(databaseName);		
		opt.setFBAModel(model);
		 
		opt.optimize();		
		
		List<GRBVar> vars = ((GurobiSolver) opt.getSolver()).getVars();
		for (int i = 0; i < vars.size(); i++) {
			// This only works for a Gurobi solver
			try {
				Integer reactionId = Integer.valueOf(vars.get(i).get(
						GRB.StringAttr.VarName));
				
				ReactionFactory rFactory = new ReactionFactory();
				SBMLReaction aReaction = (SBMLReaction) rFactory
						.getReactionById(reactionId, "SBML", databaseName);
				System.out.println("Reaction:"
						+ aReaction.getReactionAbbreviation() + " Flux: "
						+ vars.get(i).get(GRB.DoubleAttr.X));
			} catch (GRBException ex) {
				ex.printStackTrace();

			}
		}
		System.out.println("max objective flux:" + opt.getmaxFlux());
	}
	
	public static Solver getSolver() {
		return solver;
	}

	public static void setSolver(Solver solver) {
		Optimize.solver = solver;
	}
}
