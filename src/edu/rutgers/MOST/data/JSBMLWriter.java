package edu.rutgers.MOST.data;

import java.beans.PropertyChangeEvent;

import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreeNode;

import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Creator;
import org.sbml.jsbml.History;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;


public class JSBMLWriter {
	public JSBMLWriter() throws Exception {
		SBMLDocument doc = new SBMLDocument(2, 4);
		
		
		// Create a new SBML model, and add a compartment to it.
		Model model = doc.createModel("test_model");
		Compartment compartment = model.createCompartment("default");
		compartment.setSize(1d);
		
		// C
		// Create a model history object and add author information to it.
		History hist = model.getHistory(); // Will create the History, if it does not exist
		Creator creator = new Creator("Given Name", "Family Name", "Organisation", "My@EMail.com");
		hist.addCreator(creator);
		
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

	/**
	 * @param args
	 */
	/** Main routine. This does not take any arguments. */
		public static void main(String[] args) throws Exception {
			new JSBMLWriter();
		}
		
}
