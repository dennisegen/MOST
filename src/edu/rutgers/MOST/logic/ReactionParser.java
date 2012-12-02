package edu.rutgers.MOST.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReactionParser {
	
	static boolean parse = true;

	public static ArrayList<ArrayList<ArrayList<String>>> reactionList(String reactionEquation){
		reactionEquation = reactionEquation.trim();
		
		String splitString = "";
		//reversible options
		if (reactionEquation.contains("<==>")) {
			//trailing space on splitString gets rid of preceding space on first split
			//of productsAndCoeff
			splitString = "<==> ";
		} else if (reactionEquation.contains("<=>")) {
			splitString = "<=> ";
		} else if (reactionEquation.contains("=") && !reactionEquation.contains(">")) {
			splitString = "= ";
			//not reversible options
		} else if (reactionEquation.contains("=>")) {
			splitString = "=> ";
		} else if (reactionEquation.contains("-->")) {
			splitString = "--> ";
		} else if (reactionEquation.contains("->")) {
			splitString = "-> ";
		}		
		
		//removes compartment prefix such as "[c] :"
		char prefixCompartment = 0;
		if (reactionEquation.startsWith("[") && reactionEquation.indexOf("]") == 2 && reactionEquation.contains(":")) {
			prefixCompartment = reactionEquation.charAt(1);
			reactionEquation = reactionEquation.substring(reactionEquation.indexOf(":") + 1, reactionEquation.length()).trim();
		}
		
		ArrayList<ArrayList<ArrayList<String>>> reactionList = new ArrayList<ArrayList<ArrayList<String>>>();
		
		ArrayList<ArrayList<String>> reactants;
		ArrayList<ArrayList<String>> products;
		if (reactionEquation.startsWith(splitString)) {
			ArrayList<String> reactantAndStoicList = new ArrayList<String>();
			reactantAndStoicList.add("0");
			
			reactants = new ArrayList<ArrayList<String>>();
			reactants.add(reactantAndStoicList);
			
			String productHalfEquation = reactionEquation.substring(splitString.length(), reactionEquation.length());
			List<String> productsAndCoeff = Arrays.asList(productHalfEquation.split("\\s+"));
			
			ArrayList<ArrayList<String>> rawProducts = rawSpeciesAndCoeffList(productsAndCoeff);
			products = stoicAndSpeciesList(rawProducts);			
			
		} else if (reactionEquation.endsWith(splitString.trim())) {			
			
			String reactantHalfEquation = reactionEquation.substring(0, reactionEquation.length() - splitString.length());
			List<String> reactantsAndCoeff = Arrays.asList(reactantHalfEquation.split("\\s+"));			
			
			ArrayList<ArrayList<String>> rawReactants = rawSpeciesAndCoeffList(reactantsAndCoeff);
			reactants = stoicAndSpeciesList(rawReactants);			
			
			ArrayList<String> productAndStoicList = new ArrayList<String>();
			productAndStoicList.add("0");
			
			products = new ArrayList<ArrayList<String>>();
			products.add(productAndStoicList);
			
		} else {	
			List<String> halfEquations = Arrays.asList(reactionEquation.split(splitString));
			
			String reactantHalfEquation = halfEquations.get(0).trim();
			List<String> reactantsAndCoeff = Arrays.asList(reactantHalfEquation.split("\\s+"));
						
			ArrayList<ArrayList<String>> rawReactants = rawSpeciesAndCoeffList(reactantsAndCoeff);
			reactants = stoicAndSpeciesList(rawReactants);
			
			String productHalfEquation = halfEquations.get(1).trim();
			List<String> productsAndCoeff = Arrays.asList(productHalfEquation.split("\\s+"));
			
			ArrayList<ArrayList<String>> rawProducts = rawSpeciesAndCoeffList(productsAndCoeff);
			products = stoicAndSpeciesList(rawProducts);
		}	
		
		if (prefixCompartment != 0) {
			for (int i = 0; i < reactants.size(); i++) {
				ArrayList<String> reactant = reactants.get(i);
				if (reactant.size() > 1) {
					reactant.set(1, reactant.get(1) + "[" + prefixCompartment + "]");
				}
			}
			for (int i = 0; i < products.size(); i++) {
				ArrayList<String> product = products.get(i);
				if (product.size() > 1) {
					product.set(1, product.get(1) + "[" + prefixCompartment + "]");
				}
			}
		}
		reactionList.add(reactants);
		reactionList.add(products);
		
		return reactionList;		
	}
	
	//creates list of raw lists of coeff and species from half equations
	public static ArrayList<ArrayList<String>> rawSpeciesAndCoeffList(List<String> halfEquation) {
		//need to make an array of speciesAndCoeff lists
		ArrayList<ArrayList<String>> rawSpeciesAndCoeffList = new ArrayList<ArrayList<String>>();
		//list of coeff and species or species only
		ArrayList<String> speciesAndCoeff[] = new ArrayList[getNumberOfSpecies(halfEquation)];
		int currentSpecies = 0;
		speciesAndCoeff[currentSpecies] = new ArrayList();
		for (int i = 0; i < halfEquation.size(); i++) {				
			if (halfEquation.get(i).compareTo("+") != 0) {				
				speciesAndCoeff[currentSpecies].add(halfEquation.get(i));				
			} else {
				rawSpeciesAndCoeffList.add(speciesAndCoeff[currentSpecies]);
				currentSpecies += 1;
				speciesAndCoeff[currentSpecies] = new ArrayList();
			}			
		}
		
		rawSpeciesAndCoeffList.add(speciesAndCoeff[currentSpecies]);
		
		return rawSpeciesAndCoeffList;
	}
	
	public static ArrayList<ArrayList<String>> stoicAndSpeciesList(ArrayList<ArrayList<String>> rawSpeciesList) {
		ArrayList<ArrayList<String>> stoicAndSpeciesList = new ArrayList<ArrayList<String>>();
		ArrayList<String> stoicAndSpecies[] = new ArrayList[rawSpeciesList.size()];
		for (int i = 0; i < rawSpeciesList.size(); i++) {
			stoicAndSpecies[i] = stoicAndSpecies((ArrayList) rawSpeciesList.get(i));
			stoicAndSpeciesList.add(stoicAndSpecies[i]);
		}

		return stoicAndSpeciesList;
	}
	
	//converts raw lists of coeff and species or species only from rawSpeciesAndCoeffList to
	//list of stoic and species, if 1 is not expressed, it is added as stoic
	public static ArrayList<String> stoicAndSpecies(ArrayList<String> rawSpeciesAndCoeff) {
		ArrayList<String> stoicAndSpecies = new ArrayList<String>();
		String stoic = "1.0";
		String reactant = "";
		if (rawSpeciesAndCoeff.size() > 1) {
			//if number is in parenthesis
			if (rawSpeciesAndCoeff.get(0).startsWith("(")) {
				String firstString = rawSpeciesAndCoeff.get(0).substring(1, rawSpeciesAndCoeff.get(0).length() - 1);
				if (isNumber(firstString)) {
					stoic = firstString;
					reactant = species(rawSpeciesAndCoeff, 1);
				} else {
					reactant = species(rawSpeciesAndCoeff, 0);
				}
			//number not in parenthesis	
			} else {
				if (isNumber(rawSpeciesAndCoeff.get(0))) {
					stoic = rawSpeciesAndCoeff.get(0);
					reactant = species(rawSpeciesAndCoeff, 1);
				} else 
					reactant = species(rawSpeciesAndCoeff, 0);
				}
			//length 1
		} else {
			reactant = rawSpeciesAndCoeff.get(0);					
		}	
		
		if (reactant.contains("+")) {
			if ((isNumber(reactant.substring(reactant.lastIndexOf("+") + 1, reactant.length()))) || reactant.endsWith("+")) {
				stoicAndSpecies.add(stoic);
				stoicAndSpecies.add(reactant);
			} else {
				parse = false;
			}
		} else {
			stoicAndSpecies.add(stoic);
			stoicAndSpecies.add(reactant);
		}
			
		return stoicAndSpecies;
	}
	
	public static boolean isNumber(String s) {
		try {
			Double.parseDouble(s);
		}
		catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}
	
	//concatenates species if there are spaces
	public static String species(List<String> reactantsAndCoeff, int stoicCorrection) {
		String species = "";
		if (reactantsAndCoeff.size() - stoicCorrection > 2) {
			for (int i = stoicCorrection; i < reactantsAndCoeff.size() - 1; i++) {
				species = species + reactantsAndCoeff.get(i) + " ";
			}
			species = species + reactantsAndCoeff.get(reactantsAndCoeff.size() - 1);
		} else if (reactantsAndCoeff.size() - stoicCorrection == 2) {
			species = reactantsAndCoeff.get(0 + stoicCorrection) + " " + reactantsAndCoeff.get(1 + stoicCorrection);
		} else {
			species = reactantsAndCoeff.get(0 + stoicCorrection);
		}
		return species;
	}
	
	public static int getNumberOfSpecies(java.util.List<String> halfEquation) {
		int numSpecies = 0;
		for (int i = 0; i < halfEquation.size(); i++) {			 
			if (halfEquation.get(i).compareTo("+") != 0) {	
				numSpecies += 1;
			}
		}
		numSpecies += 1;
		return numSpecies;		
	}
	
	public boolean isValid(String reactionEquation) {
		if (reactionEquation != null) {
			if (reactionEquation.contains(">") || reactionEquation.contains("=")) {
				return true;
			}
		}		
		return false;
	}
	
	public static void main(String[] args) {
		System.out.println(reactionList("a+2 + 3 h+ + c => c+ + d"));
		System.out.println(reactionList("[c] : hedacp + nadph + o2 => hdeACP + nadp + h +2 h2o"));
		System.out.println(reactionList("[c]: a+2 + 3 h+ + c =>"));
	}
	
}

