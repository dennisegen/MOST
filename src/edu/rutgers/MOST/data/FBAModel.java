package edu.rutgers.MOST.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class FBAModel {

	private Vector<ModelReaction> reactions;
	private Vector<ModelMetabolite> metabolites;
	private Vector<Double> objective;
	private Vector<Map<Integer, Double>> sMatrix;
	
	public FBAModel(String databaseName) {
		ReactionFactory rFactory = new ReactionFactory();
		this.reactions = rFactory.getAllReactions("SBML", databaseName); 
		this.objective = rFactory.getObjective("SBML", databaseName);
		
		MetaboliteFactory mFactory = new MetaboliteFactory();
		this.metabolites = mFactory.getAllInternalMetabolites("SBML", databaseName);

		ReactantFactory reactantFactory = new ReactantFactory();
		ArrayList<ModelReactant> reactantList = reactantFactory.getAllReactants("SBML", databaseName);		
		ProductFactory productFactory = new ProductFactory();
		ArrayList<ModelProduct> productList = productFactory.getAllProducts("SBML", databaseName);
		
		this.sMatrix = new Vector<Map<Integer, Double>>(metabolites.size());
		for (Integer i = 0; i < reactantList.size(); i++) {
			SBMLReactant reactant = (SBMLReactant) reactantList.get(i);
			Map<Integer, Double> sRow = sMatrix.elementAt(reactant.getMetaboliteId());
			if (sRow == null) {
				sRow = new HashMap<Integer, Double>();
			}
			sRow.put(reactant.getReactionId(), -reactant.getStoic());
		}
		
		for (Integer i = 0; i < productList.size(); i++) {
			SBMLProduct product = (SBMLProduct) productList.get(i);
			Map<Integer, Double> sRow = sMatrix.elementAt(product.getMetaboliteId());
			if (sRow == null) {
				sRow = new HashMap<Integer, Double>();
			}
			sRow.put(product.getReactionId(), product.getStoic());			
		}
	}
	
	public Vector<ModelReaction> getReactions() {
		return this.reactions;
	}	
	
	public Vector<Double> getObjective() {
	    return this.objective;
	}
	
	public Vector<Map<Integer, Double>> getSMatrix() {
		return this.sMatrix;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public String toString() {
		return "FBAModel [reactions=" + reactions + ", metabolites="
				+ metabolites + ", objective=" + objective + "]";
	}

}
