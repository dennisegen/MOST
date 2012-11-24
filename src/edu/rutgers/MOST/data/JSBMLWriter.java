package edu.rutgers.MOST.data;

import java.beans.PropertyChangeEvent;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeNode;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Creator;
import org.sbml.jsbml.History;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.Unit;
import org.sbml.jsbml.Unit.Kind;
import org.sbml.jsbml.UnitDefinition;

import edu.rutgers.MOST.config.ConfigConstants;
import edu.rutgers.MOST.config.LocalConfig;


public class JSBMLWriter implements TreeModelListener{
	public String sourceType;
	public String databaseName;
	public SMetabolites allMeta;
	public LocalConfig curConfig;
	public SReactions allReacts;
	
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
	
	public void setConfig(LocalConfig conf) {
		this.curConfig = conf;
	}
	
	public void formConnect(LocalConfig config) throws Exception{
		//config.setLoadedDatabase(ConfigConstants.DEFAULT_DATABASE_NAME);
		System.out.println(config.getDatabaseName());
		
		
		curConfig = config;
		
		databaseName = config.getDatabaseName();
				
		sourceType = "SBML";
		
		if (sourceType == "SBML") {
			this.create();
		}
				
		
		//Connection con = DriverManager.getConnection("jdbc:sqlite:" + config.getDatabaseName() + ".db");
		//System.out.print(con.getSchema());
	}
	
	public JSBMLWriter() {
		
	}
	
	public void create() throws Exception {
		SBMLDocument doc = new SBMLDocument(2, 4);
		allMeta = new SMetabolites();
		allReacts = new SReactions();
		
		
		// Create a new SBML model, and add a compartment to it.
		Model model = doc.createModel(databaseName + "1");
		UnitDefinition mmolgh = new UnitDefinition();
		
		Unit mole = new Unit();
		mole.setKind(Kind.MOLE);
		mole.setScale(-3);
		
		Unit gram = new Unit();
		gram.setKind(Kind.GRAM);
		gram.setExponent(-1);
		
		Unit second = new Unit();
		second.setKind(Kind.SECOND);
		//second.setMultiplier(.00027777);
		second.setExponent(-1);
		
		mmolgh.setName("mmol_per_gDW_per_hr");
		
		mmolgh.addUnit(mole);
		mmolgh.addUnit(gram);
		mmolgh.addUnit(second);
		mmolgh.setLevel(2);
		mmolgh.setVersion(4);
		
		model.addUnitDefinition(mmolgh);
		
		
		allMeta.setModel(model);
		allReacts.setModel(model);
		
		
		for (Species spec : allMeta.allSpecies) {
			System.out.println(spec.getId());
		}
		
		Compartment compartment = model.createCompartment("default");
		compartment.setSize(1d);
		
		// C
		// Create a model history object and add author information to it.
		//History hist = model.getHistory(); // Will create the History, if it does not exist
		//Creator creator = new Creator("Given Name", "Family Name", "Organisation", "My@EMail.com");
		//hist.addCreator(creator);
		
		// Create some sample content in the SBML model.
		//Species specOne = model.createSpecies("test_spec1", compartment);
		//Species specTwo = model.createSpecies("test_spec2", compartment);
		
		//Reaction sbReaction = model.createReaction("reaction_id");
		
		
		// Add a substrate (SBO:0000015) and product (SBO:0000011) to the reaction.
		/*SpeciesReference subs = sbReaction.createReactant(specOne);
		subs.setSBOTerm(15);
		SpeciesReference prod = sbReaction.createProduct(specTwo);
		prod.setSBOTerm(11);*/
		
		// For brevity, WE DO NOT PERFORM ERROR CHECKING, but you should,
		// using the method doc.checkConsistency() and then checking the error log.
		
		// Write the SBML document to a file.
		SBMLWriter sbmlwrite = new SBMLWriter();
		
		
		sbmlwrite.write(doc, "test.xml", "MOST", "1.0");
	}
	
	public class SMetabolites {
		public Model model;
		public ArrayList<SBMLMetabolite> allMetabolites;
		public ArrayList<Species> allSpecies;
	
		public SMetabolites() {
			allMetabolites = new ArrayList();
			allSpecies = new ArrayList();
		}
		
		public void setDatabase(String name) {
			
		}
		
		/*public SMetabolites(MetaboliteFactory mFactory) {
			this.parseAllMetabolites(mFactory);
		}*/
		
		public void setModel(Model model) {
			this.model = model;
			this.parseAllMetabolites();
		}
		
		public void parseAllMetabolites() {
			MetaboliteFactory mFactory = new MetaboliteFactory();
			int length = mFactory.maximumId(databaseName);
			
			//mFactory.getMetaboliteById(metaboliteId, sourceType, databaseName);
			
			System.out.print("Currently of size: ");
			System.out.print(length);
			System.out.print("\n");
			
			for (int i=1; i <= length; i++) {
				SBMLMetabolite curMeta = (SBMLMetabolite) mFactory.getMetaboliteById(i, sourceType, databaseName);
				System.out.println(curMeta);
				this.allMetabolites.add(curMeta);
				
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
				String mAbrv = cur.getMetaboliteAbbreviation();
				String mName = cur.getMetaboliteName();
				//int charge = Integer.getInteger(cur.getCharge());
				
				
				
				Species curSpec = model.createSpecies(mAbrv, compartment);
				curSpec.setName(mName);
				//curSpec.setCharge(charge);
				
				allSpecies.add(curSpec);
			}
			
		}
	}
	
	public class SReactions {
		public Model model;
		public ArrayList<SBMLReaction> allReactions;
	
		public SReactions() {
			allReactions = new ArrayList();
		}
		public void setModel(Model model) {
			this.model = model;
			this.parseAllReactions();
		}
		
		public void parseAllReactions() {
			ReactionFactory rFactory = new ReactionFactory();
			
			int length = rFactory.getAllReactions(sourceType, databaseName).size();
			
			
			for (int i = 0 ; i<= length; i++) {
				SBMLReaction curReact = (SBMLReaction) rFactory.getReactionById(i, sourceType, databaseName);
				System.out.println(curReact);
				allReactions.add(curReact);
			}
			
			
			if (this.model != null) {
				this.devModel();
			}
		}
		
		/*public Species getReaction(String mName) {
			Species match = null;
			for (Species cur : allReactions) {
				if (cur.getName() == mName) {
					match = cur;
				}
			}
			return match;	
		}*/
		
		
		public void devModel() {
			Vector<Species> curSpecies;
			
			int count = 0;
			System.out.println();
			for (SBMLReaction cur : allReactions) {
				
				String id = cur.getReactionAbbreviation();
				String name = cur.getReactionName();
				ArrayList<SBMLReactant> curReactants = cur.getReactantsList();
				
				//System.out.println("Reactants [Size]: " + String.valueOf(cur.getReactantsList().size()));
				
				//curReactants.addAll(cur.getReactantsList());
				
				ArrayList<SBMLProduct> curProducts = cur.getProductsList();
				//System.out.println("Products [Size]: " + String.valueOf(curProducts.size()));
				
				
				Boolean reversible = Boolean.valueOf(cur.getReversible());
				String lowerBound = String.valueOf(cur.getLowerBound()); 
				String upperBound = String.valueOf(cur.getUpperBound()); 
				String objectCoeff = "0.000000"; //TODO Find proper value
				String fluxValue = String.valueOf(cur.getFluxValue()); 
				String reducCost = "0.000000"; //TODO Find proper value
				
				
				ArrayList<Parameter> parameters= new ArrayList();
				
				Parameter lbound = new Parameter();
				lbound.setId("LOWER_BOUND");
				lbound.setValue(lowerBound);
				lbound.setUnits("mmol_per_gDW_per_hr");
				parameters.add(lbound);
				
				
				Parameter ubound = new Parameter();
				ubound.setId("UPPER_BOUND");
				ubound.setValue(upperBound);
				ubound.setUnits("mmol_per_gDW_per_hr");
				parameters.add(ubound);
				
				
				Parameter objCoeff = new Parameter();
				objCoeff.setId("OBJECTIVE_COEFFICIENT");
				objCoeff.setValue(objectCoeff);
				parameters.add(objCoeff);
								
				
				Parameter fluxVal = new Parameter();
				fluxVal.setId("FLUX_VALUE");
				fluxVal.setValue(fluxValue);
				fluxVal.setUnits("mmol_per_gDW_per_hr");
				parameters.add(fluxVal);
				
				Parameter redCost = new Parameter();
				redCost.setValue(reducCost);
				redCost.setId("REDUCED_COST");
				parameters.add(redCost);
				
				
				Reaction curReact = model.createReaction(id);
				curReact.setName(name);
				curReact.setReversible(reversible);
				
				
				
				
				
				//"http://www.w3.org/1998/Math/MathML"
				
				KineticLaw law = new KineticLaw();
				
				ASTNode node = new ASTNode();
				
				
				
				
				for (Parameter param : parameters) {
					String curId = param.getId();
					String value = param.getValue();
					String units = param.getUnits();
					
					LocalParameter lParam = new LocalParameter();
					
					lParam.setName(curId);
					
					lParam.setValue(Double.valueOf(value));
					
					if (units != null) {
						lParam.setUnits(units);
					}
					
					law.addLocalParameter(lParam);
				}
								
				curReact.setKineticLaw(law);
				
				/*				
				for (SBMLReactant reactant : curReactants) {
					//System.out.println(reactant);
					//SpeciesReference specref = new SpeciesReference();
					//specref.setId(reactant.getMetaboliteAbbreviation());

					
					//curReact.addReactant(specref);
				}
				*/
				
				//curReact.setCompartment(compartmentID);
				
				
				
				
				
				
				//kl.
				//curReact.setKineticLaw(kineticLaw)
			}
			
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
		
		public String getId() {
			return id;
		}
		
		public String getValue() {
			return value;
		}
		
		public String getUnits() {
			return units;
		}
		
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
