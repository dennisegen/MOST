package edu.rutgers.MOST.data;

import java.beans.PropertyChangeEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeNode;
import javax.xml.stream.XMLEventWriter;

import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Creator;
import org.sbml.jsbml.History;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;

import edu.rutgers.MOST.config.ConfigConstants;
import edu.rutgers.MOST.config.LocalConfig;
import edu.rutgers.MOST.data.SBMLWriter.ListOfParameters;
import edu.rutgers.MOST.data.SBMLWriter.ListOfProducts;
import edu.rutgers.MOST.data.SBMLWriter.ListOfReactants;
import edu.rutgers.MOST.data.SBMLWriter.Notes;
import edu.rutgers.MOST.data.SBMLWriter.Parameter;


public class JSBMLWriter implements TreeModelListener{
	public String sourceType;
	public String databaseName;
	public SMetabolites allSpecies;
	
	/**
	 * @param args
	 */
	/** Main routine. This does not take any arguments. */
		public static void main(String[] args) throws Exception {
			new JSBMLWriter();
		}

	@Override
	public void treeNodesChanged(TreeModelEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void treeNodesInserted(TreeModelEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void treeNodesRemoved(TreeModelEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void treeStructureChanged(TreeModelEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public void formConnect() throws Exception{
		LocalConfig.getInstance().setLoadedDatabase(ConfigConstants.DEFAULT_DATABASE_NAME);
		Connection con = DriverManager.getConnection("jdbc:sqlite:" + LocalConfig.getInstance().getDatabaseName() + ".db");
		System.out.print(con.getSchema());
	}
	
	public JSBMLWriter() throws Exception {
		SBMLDocument doc = new SBMLDocument(2, 4);
		allSpecies = new SMetabolites();
		
		// Create a new SBML model, and add a compartment to it.
		Model model = doc.createModel("test_model");
		allSpecies.setModel(model);
		
		Compartment compartment = model.createCompartment("default");
		compartment.setSize(1d);
		
		// C
		// Create a model history object and add author information to it.
		//History hist = model.getHistory(); // Will create the History, if it does not exist
		//Creator creator = new Creator("Given Name", "Family Name", "Organisation", "My@EMail.com");
		//hist.addCreator(creator);
		
		// Create some sample content in the SBML model.
		Species specOne = model.createSpecies("test_spec1", compartment);
		Species specTwo = model.createSpecies("test_spec2", compartment);
		
		Reaction sbReaction = model.createReaction("reaction_id");
		
		
		// Add a substrate (SBO:0000015) and product (SBO:0000011) to the reaction.
		SpeciesReference subs = sbReaction.createReactant(specOne);
		subs.setSBOTerm(15);
		SpeciesReference prod = sbReaction.createProduct(specTwo);
		prod.setSBOTerm(11);
		
		// For brevity, WE DO NOT PERFORM ERROR CHECKING, but you should,
		// using the method doc.checkConsistency() and then checking the error log.
		
		// Write the SBML document to a file.
		SBMLWriter sbmlwrite = new SBMLWriter();
		
		
		sbmlwrite.write(doc, "test.xml", "JSBMLexample", "1.0");
	}
	
	public class SMetabolites {
		public Model model;
		public Vector<SBMLMetabolite> allMetabolites;
		public Vector<Species> allSpecies;
	
		public SMetabolites() {
			
		}
		
		
		public SMetabolites(MetaboliteFactory mFactory) {
			this.parseAllMetabolites(mFactory);
		}
		
		public void setModel(Model model) {
			this.model = model;
		}
		
		public void parseAllMetabolites(MetaboliteFactory mFactory) {
			int length = mFactory.metaboliteCount(sourceType, databaseName);
			for (int i=0; i < length; i++) {
				this.allMetabolites.add((SBMLMetabolite) mFactory.getMetaboliteById(i, sourceType, databaseName));
			}
			
			if (this.model != null) {
				this.devModel();
			}
		}
		
		public Species getSpecies(String mName) {
			Species match = null;
			for (Species cur : allSpecies) {
				if (cur.getName() == mName) {
					match = cur;
				}
			}
			return match;	
		}
		
		
		public void devModel() {
			Vector<Species> curSpecies;
			
			int count = 0;
			for (SBMLMetabolite cur : allMetabolites) {
				
				Compartment compartment = model.createCompartment(cur.getCompartment());
				String bound = cur.getBoundary();
				String mName = cur.getMetaboliteName();
				Species curSpec = model.createSpecies(mName, compartment);
				
				allSpecies.add(curSpec);
			}
			
		}
	}
	
	
	
	public class SReaction {
		
		public String id;
		public String name;
		public String reversible;
		public Notes note;
		public ListOfReactants reactants;
		public ListOfProducts products;
		
		public ArrayList<Parameter> parameters;
		public XMLEventWriter eventWriter;
		
		public SBMLReaction sbmlReact;
		
		public Species modelSpec;
		public Model model;
		
		public void setSBMLReaction(SBMLReaction sbmlReact) {
			this.sbmlReact = sbmlReact;
			
			
			
			String id = sbmlReact.getReactionAbbreviation();
			String name = sbmlReact.getReactionName();
			String reversible = sbmlReact.getReversible();
			String lowerBound = String.valueOf(sbmlReact.getLowerBound()); 
			String upperBound = String.valueOf(sbmlReact.getUpperBound()); 
			String objectCoeff = "0.000000"; //TODO Find proper value
			String fluxValue = String.valueOf(sbmlReact.getFluxValue()); 
			String reducCost = "0.000000"; //TODO Find proper value
			
			Reaction sbReaction = model.createReaction(name);
			
			sbReaction.setReversible(Boolean.getBoolean(reversible));
			
			//TODO: KineticLaw kw;
			//TODO: sbReaction.setKineticLaw(kineticLaw);
			
			ArrayList<SBMLProduct> prodList = sbmlReact.getProductsList(); //Convert elements to Product instances
			for (SBMLProduct prod : prodList) {
				String mName = prod.getMetaboliteAbbreviation();
				
				Species curSpec = model.createSpecies(prod.getMetaboliteAbbreviation());
				prod.getMetaboliteAbbreviation();
				Species curS = allSpecies.getSpecies(mName);
				sbReaction.createProduct(curS);
			}
			
			
			ArrayList<SBMLReactant> reactList = sbmlReact.getReactantsList(); //Convert elements to Reactant instances
			for (SBMLReactant reac : reactList) {
				String mName = reac.getMetaboliteAbbreviation();
				
				Species curSpec = model.createSpecies(reac.getMetaboliteAbbreviation());
				reac.getMetaboliteAbbreviation();
				Species curS = allSpecies.getSpecies(mName);
				sbReaction.createReactant(curS);
			}
			
			//this.setId(id);
			//this.setName(name);
			
			
			
			
			
			Parameter lbound = new Parameter();
			lbound.setId("LOWER_BOUND");
			lbound.setValue(lowerBound);
			lbound.setUnits("mmol_per_gDW_per_hr");
			
			
			Parameter ubound = new Parameter();
			ubound.setId("UPPER_BOUND");
			ubound.setValue(upperBound);
			ubound.setUnits("mmol_per_gDW_per_hr");

			
			Parameter objCoeff = new Parameter();
			objCoeff.setId("OBJECTIVE_COEFFICIENT");
			objCoeff.setValue(objectCoeff);
			
			Parameter fluxVal = new Parameter();
			fluxVal.setId("FLUX_VALUE");
			fluxVal.setValue(fluxValue);
			fluxVal.setUnits("mmol_per_gDW_per_hr");
			
			
			Parameter redCost = new Parameter();
			redCost.setValue(reducCost);
			redCost.setId("REDUCED_COST");
			
			
			parameters.add(lbound);
			parameters.add(ubound);
			parameters.add(objCoeff);
			parameters.add(fluxVal);
			parameters.add(redCost);	
		}
		

	
		
	}
	
	public class Parameter{
		/*Class for easy implementation of Parameter node under listofParameters
		 * 
		 * Example:
		 * <parameter id="LOWER_BOUND" value="-999999.000000" units="mmol_per_gDW_per_hr"/>
		 * */
		public String id;
		public String value;
		public String units;
		
		public void setId(String id) {
			this.id = id;
		}
		
		public void setValue(String value) {
			this.value = value;
		}
		
		public void setUnits(String units) {
			this.units = units;
		}
		
		public String[] getKeys() {
			String keys[];
			if (this.units != null) {
				keys = new String[3];
				keys[0] = "id";
				keys[1] = "value";
				keys[2] = "units";
			}
			else {
				keys = new String[2];
				keys[0] = "id";
				keys[1] = "value";
			}
			return keys;
			
		}
		
		public String[] getValues() {
			String[] atr;
			if (this.units != null) {
				atr = new String[3];
				atr[0] = this.id;
				atr[1] = this.value;
				atr[2] = this.units;
			}
			else {
				atr = new String[2];
				atr[0] = this.id;
				atr[1] = this.value;
			}
					
			return atr;
		}
	}
}
