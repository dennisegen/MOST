package edu.rutgers.MOST.data;



import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.util.List;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.Reader;
import java.util.Iterator;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;


/* 
 * TODO Retrieval of Species and Reactions
 * TODO Main class for testing
 * TODO Reduce redundancies
 * 
 * Notes: 
 * 
 * There are several likely issues with the XML writing. Decided on first writing pseudocode
 * to get an idea of how an XML writer will work for MOST. Just need to gain a better familiarity
 * with the javax.xml library and will be able to correct the issues then.
 * 
 * Additionally, there are a lot of redundancies which can be handled by creating various 
 * super-classes. To be taken care of around testing to allow for changes to be more easily made.
 * 
 * 
 * More information about XML reading and writing can be found at:
 * http://www.vogella.com/articles/JavaXML/article.html
 * 
 * More information about attribute adding can be found at: 
 * http://www.java2s.com/Code/Java/JDK-6/UsingXMLEventFactorytocreatexmldocument.htm
 * 
 */


public class SBMLWriter {
	/* SBML Writer has two approaches, one is to create a SBML from scratch through querying, the other is to examine the current 
	 * SBML document and modify changes to flux and eventually KO status when GDBB optimize is implemented
	 * 
	 */
	public Connection dbCon;
	public String databaseName;
		
	public Vector<SBMLReaction> allReactions;
	public Vector<SBMLMetabolite> allMetabolites;
	
	public ListOfReactions listOfReact;
	public ListOfSpecies listOfSpecies;
	
	
	public String sourceType;
	
	public SBMLWriter() {
		
	}
	
	public SBMLWriter(ReactionFactory rFactory) {
		this.parseAllReactions(rFactory);
	}
	
	public SBMLWriter(MetaboliteFactory mFactory) {
		this.parseAllMetabolites(mFactory);
	}
	
	public SBMLWriter(MetaboliteFactory mFactory, ReactionFactory rFactory) {
		this.parseAllMetabolites(mFactory);
		this.parseAllReactions(rFactory);
	}
	
	
	
	public SBMLWriter(Connection con) {
		/*Writes SBML based on connection and queries
		 * will require 
		*/
		this.setConnection(con);
	}
	
	public void setConnection(Connection con) {
		this.dbCon = con;
	}
	
	public void setDBName(String name) {
		this.databaseName = name;
	}
	
	
	public void setReactions(Vector<SBMLReaction> reactions) {
		this.allReactions = reactions;
	}
	
	
	public boolean isFluxDifferent(String reactionId, String flux) {
		/*TODO*/
		return false;
	}
	
	public boolean isKODifferent(String reactionId, String KO) {
		/*TODO*/
		return false;
	}
	
	public void detectDifferences() {
		/* TODO This method will predominately be used in cases where a SBML document was loaded. 
		 * It will search for KO and Flux values which have changed as a result of optimization and simulation. 
		 * It will approach modification in several possible fashions, one of which is to create an iterable 
		 * to jump to the XML nodes and directly modify those. Writing the document upon completion. 
		 * 
		 */
	}
	
	public void parse() {
		/* TODO Parse Connection attaining
		 * listOfSpecies, listOfReactions
		 * 
		 */
	}
	
	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}
	
	
	public void parseAllReactions(ReactionFactory rFactory) {
		int length = rFactory.getAllReactions(sourceType, databaseName).size();
		for (int i=0; i < length; i++) {
			this.allReactions.add((SBMLReaction) rFactory.getReactionById(i, sourceType, databaseName));
		}
		
		for (SBMLReaction react : allReactions) {
			Reaction tempReact = new Reaction();
			tempReact.setSBMLReaction(react);
			this.listOfReact.addReaction(tempReact);
		}
		
	}
	
	public void parseAllMetabolites(MetaboliteFactory mFactory) {
		int length = mFactory.metaboliteCount(sourceType, databaseName);
		for (int i=0; i < length; i++) {
			this.allMetabolites.add((SBMLMetabolite) mFactory.getMetaboliteById(i, sourceType, databaseName));
		}
	}
	
	
	

	
	
	public class Reaction {
		
		public String id;
		public String name;
		public String reversible;
		public Notes note;
		public ListOfReactants reactants;
		public ListOfProducts products;
		
		public ListOfParameters parameters;
		public XMLEventWriter eventWriter;
		
		public SBMLReaction sbmlReact;
		
		
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
			
			
			this.setId(id);
			this.setName(name);
			this.setReversible(reversible);
			
			ArrayList reactList = sbmlReact.getReactantsList(); //Convert elements to Reactant instances
			ArrayList prodList = sbmlReact.getProductsList(); //Convert elements to Product instances
			
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
		
		public void setEventWriter(XMLEventWriter eventWriter) {
			this.eventWriter = eventWriter;
		}
		
		public void setId(String id){
			this.id = id;
		}
		
		public void setName(String name){
			this.name = name;
		}
		
		public void setReversible(String revers) {
			this.reversible = reversible;
		}
		
		public String[] getKeys() {
			String[] keys = new String[3];
			keys[0] = "id";
			keys[1] = "name";
			keys[2] = "reversible";
			return keys;
		}
		
		public String[] getValues() {
			String[] values = new String[3];
			values[0] = this.id;
			values[1] = this.name;
			values[2] = this.reversible;
			return values;
		}
		
		public void setNotes(Notes note) {
			this.note = note;
		}
		
		public void ListOfReactants(ListOfReactants reactants) {
			this.reactants = reactants;
		}
		
		public void ListOfProducts(ListOfProducts products) {
			this.products = products;
		}
		
		public void ListOfParameters(ListOfParameters parameters) {
			this.parameters = parameters;
		}
		
		public void writeMainTag() throws Exception {
			XMLEventFactory eventFactory = XMLEventFactory.newInstance();
			XMLEvent end = eventFactory.createDTD("\n");
			XMLEvent tab = eventFactory.createDTD("\t");
			
				
			String[] keys = this.getKeys();
			String[] values = this.getValues();
			int len = keys.length;
			Attribute[] attributes = new Attribute[len];
			
			for (int i=0; i < len; i++) {
				attributes[i] = eventFactory.createAttribute(keys[i], values[i]);
			}
			
			List attributeList = Arrays.asList(attributes);
			List nsList = Arrays.asList();
			StartElement reacStartElement = eventFactory.createStartElement("", "", "reaction",
		            attributeList.iterator(), nsList.iterator());
			eventWriter.add(tab);
		    eventWriter.add(reacStartElement);
		    
		}
		
		public void write() throws Exception {
			XMLEventFactory eventFactory = XMLEventFactory.newInstance();
			XMLEvent end = eventFactory.createDTD("\n");
			XMLEvent tab = eventFactory.createDTD("\t");
			
			this.writeMainTag();
			this.note.write();
			this.reactants.write();
			this.products.write();
			KineticLaw kl = new KineticLaw();
			kl.setParameters(parameters);
			kl.write();
			
			eventWriter.add(eventFactory.createEndElement("", "", "reaction"));
			
			eventWriter.add(end);
		}
	}
	
	public class ListOfReactions {
		public ArrayList<Reaction> reactionList;
		public XMLEventWriter eventWriter;
		
		public void setEventWriter(XMLEventWriter eventWriter) {
			this.eventWriter = eventWriter;
		}
		
		public void addReaction(Reaction reac) {
			reactionList.add(reac);
		}
		
		public void write() throws Exception {
			
			for (Reaction reaction : reactionList) {
				reaction.setEventWriter(eventWriter);
				reaction.write();
			}
		}
	}
	
	
	public class ListOfSpecies {
		public ArrayList<Species> speciesList;
		public XMLEventWriter eventWriter;
		
		public void addSpecies(Species spec) {
			speciesList.add(spec);
		}
		
		public void setEventWriter(XMLEventWriter eventWriter) {
			this.eventWriter = eventWriter;
		}
		
		public void write() throws Exception {
			XMLEventFactory eventFactory = XMLEventFactory.newInstance();
			XMLEvent end = eventFactory.createDTD("\n");
			XMLEvent tab = eventFactory.createDTD("\t");
			
			for (Species spe: speciesList) {
				
				String[] keys = spe.getKeys();
				String[] values = spe.getValues();
				int len = keys.length;
				Attribute[] attributes = new Attribute[len];
				
				for (int i=0; i < len; i++) {
					attributes[i] = eventFactory.createAttribute(keys[i], values[i]);
				}
				
				List attributeList = Arrays.asList(attributes);
				List nsList = Arrays.asList();
				StartElement paramStartElement = eventFactory.createStartElement("", "", "parameter",
			            attributeList.iterator(), nsList.iterator());
				eventWriter.add(tab);
			    eventWriter.add(paramStartElement);
			    eventWriter.add(eventFactory.createEndElement("", "", "parameter"));
				
				eventWriter.add(end);
		}
	}
		
		private void createNode(XMLEventWriter eventWriter, String name,
			      String value) throws XMLStreamException {

			    XMLEventFactory eventFactory = XMLEventFactory.newInstance();
			    XMLEvent end = eventFactory.createDTD("\n");
			    XMLEvent tab = eventFactory.createDTD("\t");
			    
			    // Create Start node
			    StartElement sElement = eventFactory.createStartElement("", "", name);
			    eventWriter.add(tab);
			    eventWriter.add(sElement);
			    
			    // Create Content
			    Characters characters = eventFactory.createCharacters(value);
			    eventWriter.add(characters);
			    
			    // Create End node
			    EndElement eElement = eventFactory.createEndElement("", "", name);
			    eventWriter.add(eElement);
			    eventWriter.add(end);
		}
	}
	
	public class SpeciesRef{
		/*class for easy implementation of inserting a species node
		 * 
		 * Example:
		 * <speciesReference species="M_ac_c" stoichiometry="1.000000"/>
		 * 
		 */
		public String species;
		public String stoic;
				
		public void setSpecies(String name) {
			this.species = name;
		}
		
		public void setStoic(String value) {
			this.stoic = value;
		}
		
		public String[] getKeys() {
			String[] keys = new String[2];
			keys[0] = "species";
			keys[1] = "stoichiometry";
			return keys;
		}
		
		public String[] getValues() {
			String[] values = new String[2];
			values[0] = species;
			values[1] = stoic;
			return values;
		}
		
	}
	
	public class Reactant extends SpeciesRef{
		
	}
	
	public class Product extends SpeciesRef{
		
	}
	
	
	public class Notes {
		public ArrayList<String> notes;
		public XMLEventWriter eventWriter;
		
		public void setEventWriter(XMLEventWriter eventWriter){
			this.eventWriter = eventWriter;
		}
		public void add(String note) {
			notes.add(note);
		}
		
		public void write() throws Exception {
			XMLEventFactory eventFactory = XMLEventFactory.newInstance();
			XMLEvent end = eventFactory.createDTD("\n");
			XMLEvent tab = eventFactory.createDTD("\t");
			
			for (String note: notes) {
				
				
			    // Create Notes open tag
			    StartElement noteStartElement = eventFactory.createStartElement("",
			        "", "notes");
			    eventWriter.add(noteStartElement);
			    eventWriter.add(end);
			    
			    // Write the different nodes
			    createNode(eventWriter, "html:p", note);
			}
			
			    eventWriter.add(eventFactory.createEndElement("", "", "notes"));
			    eventWriter.add(end);
				
			
		}
		
		private void createNode(XMLEventWriter eventWriter, String name,
			      String value) throws XMLStreamException {

			    XMLEventFactory eventFactory = XMLEventFactory.newInstance();
			    XMLEvent end = eventFactory.createDTD("\n");
			    XMLEvent tab = eventFactory.createDTD("\t");
			    
			    // Create Start node
			    StartElement sElement = eventFactory.createStartElement("", "", name);
			    eventWriter.add(tab);
			    eventWriter.add(sElement);
			    
			    // Create Content
			    Characters characters = eventFactory.createCharacters(value);
			    eventWriter.add(characters);
			    
			    // Create End node
			    EndElement eElement = eventFactory.createEndElement("", "", name);
			    eventWriter.add(eElement);
			    eventWriter.add(end);

		}
		
	}
	public class Species{
		/* class for easy implementation of species node under listofSpecies 
		 * 
		 * Example:
		 * <species id="M_succ_b" name="M_Succinate_C4H4O4" 
		 * compartment="Extra_organism" charge="-2" boundaryCondition="true"/>
		 */
		
		public String id;
		public String name;
		public String compartment;
		public String charge;
		public String boundaryCond;
		
		public void setId(String id) {
			this.id = id;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public void setCompartment(String compart) {
			this.compartment = compart;
		}
		
		public void setCharge(String charge) {
			this.charge = charge;
		}
		
		public void setBoundary(String boundary) {
			this.boundaryCond = boundary;
		}
		
		public String[] getKeys() {
			String[] keys = new String[5];
			keys[0] = "id";
			keys[1] = "name";
			keys[2] = "compartment";
			keys[3] = "charge";
			keys[4] = "boundaryCondition";
			return keys;
		}
		
		public String[] getValues() {
			String[] values = new String[5];
			values[0] = this.id;
			values[1] = this.name;
			values[2] = this.compartment;
			values[3] = this.charge;
			values[4] = this.boundaryCond;
			return values;
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
	
	public class ListOfParameters{
		public ArrayList<Parameter> parameters;
		public XMLEventWriter eventWriter;
		
		public void add(Parameter param) {
			this.parameters.add(param);
		}
		
		
		public void setEventWriter(XMLEventWriter eventWriter) {
			this.eventWriter = eventWriter;
		}
		
		public void write() throws Exception {
			XMLEventFactory eventFactory = XMLEventFactory.newInstance();
			XMLEvent end = eventFactory.createDTD("\n");
			XMLEvent tab = eventFactory.createDTD("\t");
			
			for (Parameter param : parameters) {
				
				String[] keys = param.getKeys();
				String[] values = param.getValues();
				int len = keys.length;
				Attribute[] attributes = new Attribute[len];
				
				for (int i=0; i < len; i++) {
					attributes[i] = eventFactory.createAttribute(keys[i], values[i]);
				}
				
				List attributeList = Arrays.asList(attributes);
				List nsList = Arrays.asList();
				StartElement paramStartElement = eventFactory.createStartElement("", "", "parameter",
			            attributeList.iterator(), nsList.iterator());
				eventWriter.add(tab);
			    eventWriter.add(paramStartElement);
			    eventWriter.add(eventFactory.createEndElement("", "", "parameter"));
				
				eventWriter.add(end);
				
			}
		}
	}
	
	public class ListOf {
		/*TODO Extend several classes with this superclass
		 * 
		 */
		public XMLEventWriter eventWriter;
		
		public void setEventWriter(XMLEventWriter eventWriter) {
			this.eventWriter = eventWriter;
		}
		
		private void createNode(XMLEventWriter eventWriter, String name,
			      String[] attributeKey, String[] attributeValue) throws XMLStreamException {

				assert attributeKey.length == attributeValue.length;
				
				int numAttributes = attributeKey.length;
				
			    XMLEventFactory eventFactory = XMLEventFactory.newInstance();
			    XMLEvent end = eventFactory.createDTD("\n");
			    XMLEvent tab = eventFactory.createDTD("\t");
			    
			    // Create Start node
			    StartElement sElement = eventFactory.createStartElement("", "", name);
			    eventWriter.add(tab);
			    eventWriter.add(sElement);
			    
			    // Create Content
			    for (int i=0; i < numAttributes ; i++) {
			    	Attribute atr = eventFactory.createAttribute(attributeKey[i], attributeValue[i]);
			    	
			    	eventWriter.add(atr);
			    			
			    }
			    
			    // Create End node
			    EndElement eElement = eventFactory.createEndElement("", "", name);
			    eventWriter.add(eElement);
			    eventWriter.add(end);

		}
		
	}
	public class ListOfReactants{
		public ArrayList<Reactant> reactants;
		public XMLEventWriter eventWriter;
		
		
		public void addReactant(Reactant reac) {
			reactants.add(reac);
		}
		
		public void setEventWriter(XMLEventWriter eventWriter) {
			this.eventWriter = eventWriter;
		}
		
		public void write() throws Exception{
			XMLEventFactory eventFactory = XMLEventFactory.newInstance();
			XMLEvent end = eventFactory.createDTD("\n");
			XMLEvent tab = eventFactory.createDTD("\t");
			
			for (Reactant reac: reactants) {
				
				String[] keys = reac.getKeys();
				String[] values = reac.getValues();
				int len = keys.length;
				Attribute[] attributes = new Attribute[len];
				
				for (int i=0; i < len; i++) {
					attributes[i] = eventFactory.createAttribute(keys[i], values[i]);
				}
				
				List attributeList = Arrays.asList(attributes);
				List nsList = Arrays.asList();
				StartElement paramStartElement = eventFactory.createStartElement("", "", "parameter",
			            attributeList.iterator(), nsList.iterator());
				eventWriter.add(tab);
			    eventWriter.add(paramStartElement);
			    eventWriter.add(eventFactory.createEndElement("", "", "parameter"));
				
				eventWriter.add(end);
			}
	}
}
	
	public class ListOfProducts{
		public ArrayList<Product> products;
		public XMLEventWriter eventWriter;
		
		public void addProduct(Product prod) {
			products.add(prod);
		}
		
		public void setEventWriter(XMLEventWriter eventWriter) {
			this.eventWriter = eventWriter;
		}
		
		public void write() throws Exception{
			XMLEventFactory eventFactory = XMLEventFactory.newInstance();
			XMLEvent end = eventFactory.createDTD("\n");
			XMLEvent tab = eventFactory.createDTD("\t");
			
			for (Product prod : products) {
				
				String[] keys = prod.getKeys();
				String[] values = prod.getValues();
				int len = keys.length;
				Attribute[] attributes = new Attribute[len];
				
				for (int i=0; i < len; i++) {
					attributes[i] = eventFactory.createAttribute(keys[i], values[i]);
				}
				
				List attributeList = Arrays.asList(attributes);
				List nsList = Arrays.asList();
				StartElement paramStartElement = eventFactory.createStartElement("", "", "parameter",
			            attributeList.iterator(), nsList.iterator());
				eventWriter.add(tab);
			    eventWriter.add(paramStartElement);
			    eventWriter.add(eventFactory.createEndElement("", "", "parameter"));
				
				eventWriter.add(end);
			}
		}
		
		private void createNode(XMLEventWriter eventWriter, String name,
			      String value) throws XMLStreamException {

			    XMLEventFactory eventFactory = XMLEventFactory.newInstance();
			    XMLEvent end = eventFactory.createDTD("\n");
			    XMLEvent tab = eventFactory.createDTD("\t");
			    
			    // Create Start node
			    StartElement sElement = eventFactory.createStartElement("", "", name);
			    eventWriter.add(tab);
			    eventWriter.add(sElement);
			    
			    // Create Content
			    Characters characters = eventFactory.createCharacters(value);
			    eventWriter.add(characters);
			    
			    // Create End node
			    EndElement eElement = eventFactory.createEndElement("", "", name);
			    eventWriter.add(eElement);
			    eventWriter.add(end);

		}
		
		
	}
	
	
	
	
	public class KineticLaw {
		public String xmlns;
		public XMLEventWriter eventWriter;
		public ListOfParameters parameters;
		
		public KineticLaw() {
			this.initalize();
		}
		
		public void setParameters(ListOfParameters param) {
			this.parameters = param;
		}
		
		public void setEventWriter(XMLEventWriter eventWriter) {
			this.eventWriter = eventWriter;
		}
		
		public void write() throws Exception {
			
			XMLEventFactory eventFactory = XMLEventFactory.newInstance();
			XMLEvent end = eventFactory.createDTD("\n");
			XMLEvent tab = eventFactory.createDTD("\t");
			
			// Create kineticLaw open tag
		    StartElement kineticStartElement = eventFactory.createStartElement("",
		        "", "kineticLaw");
		    
		    Attribute attribute = eventFactory.createAttribute("xmlns", this.xmlns);
		    List attributeList = Arrays.asList(attribute);
		    List nsList = Arrays.asList();
		    
		    		    
		    StartElement mathStartElement = eventFactory.createStartElement("", "", "math",
		            attributeList.iterator(), nsList.iterator());
		    
		    eventWriter.add(tab);
		    eventWriter.add(mathStartElement);
		    
		    createNode(this.eventWriter, "ci", "FLUX_VALUE");
		    
		    eventWriter.add(eventFactory.createEndElement("", "", "math"));
		    eventWriter.add(end);
		    
		    eventWriter.add(eventFactory.createEndElement("", "", "kineticLaw"));
		    eventWriter.add(end);
		    this.parameters.write();
		}
		
		private void createNode(XMLEventWriter eventWriter, String name,
			      String value) throws XMLStreamException {

			    XMLEventFactory eventFactory = XMLEventFactory.newInstance();
			    XMLEvent end = eventFactory.createDTD("\n");
			    XMLEvent tab = eventFactory.createDTD("\t");
			    
			    // Create Start node
			    StartElement sElement = eventFactory.createStartElement("", "", name);
			    eventWriter.add(tab);
			    eventWriter.add(sElement);
			    
			    // Create Content
			    Characters characters = eventFactory.createCharacters(value);
			    eventWriter.add(characters);
			    
			    // Create End node
			    EndElement eElement = eventFactory.createEndElement("", "", name);
			    eventWriter.add(eElement);
			    eventWriter.add(end);

		}
		
		public void initalize() {
			this.xmlns = "http://www.w3.org/1998/Math/MathML";
		}
	}
	
	
			
}

	


	
	
	
	
	
	
	
