package edu.rutgers.MOST.optimization.solvers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import gurobi.*;

public class GurobiSolver extends Solver {

	static Logger log = Logger.getLogger(GurobiSolver.class);
	private GRBEnv env;
	private GRBModel model;
	private ArrayList<GRBVar> vars = new ArrayList<GRBVar>();

	public GurobiSolver(String logName) {
		try {
			log.debug("creating Gurobi environment");
//			System.loadLibrary("GurobiJni50.dll");
			env = new GRBEnv(logName);
			log.debug("setting Gurobi parameters");
			env.set(GRB.IntParam.Presolve, 0);
			env.set(GRB.DoubleParam.FeasibilityTol, 1.0E-9);
			env.set(GRB.DoubleParam.IntFeasTol, 1.0E-9);
			log.debug("creating Gurobi Model");
			model = new GRBModel(env);
			this.objType = ObjType.Minimize;

		} catch (Exception e) {
			log.error("Error code: " + e.getMessage() + ". "
					+ e.getMessage());
		}
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "GurobiSovler";
	}

	@Override
	public void setVar(String varName, VarType types, double lb, double ub) {
		// TODO Auto-generated method stub
		try {
			GRBVar var = this.model.addVar(lb, ub, 0.0, getGRBVarType(types),
					varName);
//			System.out.println("adding var: lb = " + lb + " ub = " + ub +
//			 " type = " + types + " name = " + varName);
			this.vars.add(var);
		} catch (GRBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			model.update();
		} catch (GRBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public ArrayList<Double> getSoln() {
		ArrayList<Double> soln = new ArrayList<Double>(vars.size());
		for (int i = 0; i < this.vars.size(); i++) {
			try {
				soln.add(this.vars.get(i).get(GRB.DoubleAttr.X));
			} catch (GRBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();				
			}
		}
		
		return soln;
	}
	
	@Override
	public void setVars(VarType[] types, double[] lb, double[] ub) {
		// TODO Auto-generated method stub

		if (types.length == lb.length && lb.length == ub.length) {
			for (int i = 0; i < lb.length; i++) {
				try {
					GRBVar var = this.model.addVar(lb[i], ub[i], 0.0,
							getGRBVarType(types[i]), Integer.toString(i));
					// System.out.println("adding var: lb = " + lb[i] + "ub=" +
					// ub[i] + "type =" + types[i] + "name=" +
					// Integer.toString(i));
					this.vars.add(var);
				} catch (GRBException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {

				model.update();
			} catch (GRBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void setObjType(ObjType objType) {
		// TODO Auto-generated method stub
		this.objType = objType;
	}

	@Override
	public void setObj(Map<Integer, Double> map) {
		// TODO Auto-generated method stub
		Set s = map.entrySet();
		Iterator it = s.iterator();
		GRBLinExpr expr = new GRBLinExpr();

		while (it.hasNext()) {
			Map.Entry m = (Map.Entry) it.next();
			int key = (Integer) m.getKey();
			Double value = (Double) m.getValue();
			GRBVar var = this.vars.get(key);
			expr.addTerm(value, var);
			System.out.println("key = " + key + " value = " + value);
			System.out.println("objType: " + this.objType);
		}

		try {

			model.setObjective(expr, getGRBObjType(this.objType));
			//DEGEN: Debugging to see model
			
			
		} catch (GRBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void addConstraint(Map<Integer, Double> map, ConType con,
			double value) {
		// TODO Auto-generated method stub

		try {

			Set s = map.entrySet();
			Iterator it = s.iterator();
			GRBLinExpr expr = new GRBLinExpr();

			while (it.hasNext()) {
				Map.Entry m = (Map.Entry) it.next();
				int key = (Integer) m.getKey();
				Double v = (Double) m.getValue();
				expr.addTerm(v, this.vars.get(key));

			}
			model.addConstr(expr, getGRBConType(con), value, null);

		} catch (GRBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void finalize() {
		// Not guaranteed to be invoked
		this.model.dispose();
		try {
			this.env.dispose();
		} catch (GRBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private char getGRBVarType(VarType type) {
		switch (type) {
		case CONTINUOUS:
			return GRB.CONTINUOUS;
		case BINARY:
			return GRB.BINARY;
		case INTEGER:
			return GRB.INTEGER;
		case SEMICONT:
			return GRB.SEMICONT;
		default:
			return GRB.SEMIINT;
		}
	}

	private int getGRBObjType(ObjType type) {
		switch (type) {
		case Minimize:
			return GRB.MINIMIZE;
		case Maximize:
			return GRB.MAXIMIZE;
		default:
			return GRB.MAXIMIZE;
		}
	}

	private char getGRBConType(ConType type) {
		switch (type) {
		case LESS_EQUAL:
			return GRB.LESS_EQUAL;
		case EQUAL:
			return GRB.EQUAL;
		case GREATER_EQUAL:
			return GRB.GREATER_EQUAL;
		default:
			return GRB.GREATER_EQUAL;
		}
	}

	@Override
	public double optimize() {
		try {
			model.optimize();
//			model.write("model.lp");
//			model.write("model.mps");
		} catch (GRBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();	
		}
		
		try {
			return this.model.get(GRB.DoubleAttr.ObjVal);
		} catch (GRBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return 0;
	}

}
