package edu.rutgers.MOST.data;

import java.util.ArrayList;

public class ReactantFactory {
	public ModelReactant getReactantByReactionId(Integer reactionId, String sourceType, String databaseName) {
		if("SBML".equals(sourceType)){
			SBMLReactant reactant = new SBMLReactant();
			reactant.setDatabaseName(databaseName);
			reactant.loadByReactionId(reactionId);
			return reactant;
		}
		return new SBMLReactant(); //Default behavior.
	}
	
	public ArrayList<ModelReactant> getReactantsByReactionId(Integer reactionId, String sourceType, String databaseName) {
		SBMLReactantCollection aReactantCollection = new SBMLReactantCollection();
		if("SBML".equals(sourceType)){			
			aReactantCollection.setDatabaseName(databaseName);
			aReactantCollection.loadByReactionId(reactionId);					
		}
		
		return aReactantCollection.getReactantList();		
	}
	
	public ArrayList<ModelReactant> getAllReactants(String sourceType, String databaseName) {
		SBMLReactantCollection aReactantCollection = new SBMLReactantCollection();
		if("SBML".equals(sourceType)){			
			aReactantCollection.setDatabaseName(databaseName);
			aReactantCollection.loadAll();					
		}
		
		return aReactantCollection.getReactantList();		
	}
		
}
