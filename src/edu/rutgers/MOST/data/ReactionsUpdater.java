package edu.rutgers.MOST.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import edu.rutgers.MOST.config.LocalConfig;
import edu.rutgers.MOST.logic.ReactionParser1;
import edu.rutgers.MOST.presentation.GraphicalInterface;
import edu.rutgers.MOST.presentation.GraphicalInterfaceConstants;

public class ReactionsUpdater {

	public void updateReactionRows(ArrayList<Integer> rowList, ArrayList<Integer> reacIdList, ArrayList<String> oldReactionsList, String databaseName) {
		
		//update MetabolitesUsedMap by decrementing count or removing metabolite
		//based on oldReactions that are being replaced
		for (int i = 0; i < oldReactionsList.size(); i++) {
			ReactionParser1 parser1 = new ReactionParser1();
			if (parser1.isValid(oldReactionsList.get(i)) && !LocalConfig.getInstance().getInvalidReactions().contains(oldReactionsList.get(i))) {
				ArrayList<ArrayList> oldReactionList = parser1.reactionList(oldReactionsList.get(i));

				//remove old species from used map
				for (int x = 0; x < oldReactionList.size(); x++) {
					for (int y = 0; y < oldReactionList.get(x).size(); y++) {
						if (((ArrayList) oldReactionList.get(x).get(y)).size() > 1) {
							if (LocalConfig.getInstance().getMetaboliteUsedMap().get((String) ((ArrayList) oldReactionList.get(x).get(y)).get(1)) != null) {
								int usedCount = (Integer) LocalConfig.getInstance().getMetaboliteUsedMap().get((String) ((ArrayList) oldReactionList.get(x).get(y)).get(1));
								if (usedCount > 1) {
									LocalConfig.getInstance().getMetaboliteUsedMap().put((String) ((ArrayList) oldReactionList.get(x).get(y)).get(1), new Integer(usedCount - 1));
								} else {
									LocalConfig.getInstance().getMetaboliteUsedMap().remove((String) ((ArrayList) oldReactionList.get(x).get(y)).get(1));
								}
							}			
						}					
					}
				}
			}
		}
		
		String queryString = "jdbc:sqlite:" + databaseName + ".db";
		
		try{
			Connection conn =
				DriverManager.getConnection(queryString);
			Statement stat = conn.createStatement();

			try {
				stat.executeUpdate("BEGIN TRANSACTION");

				for (int i = 0; i < rowList.size(); i++) {
					
					//remove old reactions from db
					String rrUpdate = "delete from reaction_reactants where reaction_id=" + reacIdList.get(i) + ";";				
					stat.executeUpdate(rrUpdate);
					String rpUpdate = "delete from reaction_products where reaction_id=" + reacIdList.get(i) + ";";				
					stat.executeUpdate(rpUpdate);	
					
					String knockout = "false";
					if (GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.KO_COLUMN) != null) {
						knockout = (String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.KO_COLUMN);
						if (knockout.length() == 0) {
							knockout = GraphicalInterfaceConstants.KO_DEFAULT;
						}
					}
					Double fluxValue = GraphicalInterfaceConstants.FLUX_VALUE_DEFAULT;
					if (GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.FLUX_VALUE_COLUMN) != null) {
						try {
							fluxValue = Double.valueOf((String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.FLUX_VALUE_COLUMN));
						}
					    catch ( NumberFormatException nfe ) {
					       System.out.println( "Number format exception" );
					    }				
					} 
					
					//if strings contain ' (single quote), it will not execute insert statement
					//this code escapes ' as '' - sqlite syntax for escaping '
					String reactionAbbreviation = (String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.REACTION_ABBREVIATION_COLUMN);
					if (reactionAbbreviation != null) {
						if (reactionAbbreviation.contains("'")) {
							reactionAbbreviation = reactionAbbreviation.replaceAll("'", "''");
						}
					} else {
						reactionAbbreviation = " ";
					}
					String reactionName = (String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.REACTION_NAME_COLUMN);
					if (reactionName != null) {
						if (reactionName.contains("'")) {
							reactionName = reactionName.replaceAll("'", "''");
						}
					} else {
						reactionName = " ";
					}
					
					ReactionParser1 parser1 = new ReactionParser1();
					String reactionEquation = (String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.REACTION_STRING_COLUMN);
					if (reactionEquation != null) {
						if (reactionEquation.contains("'")) {
							reactionEquation = reactionEquation.replaceAll("'", "''");
						}
						if (parser1.isValid(reactionEquation)) {
							ArrayList<ArrayList> newReactionList = parser1.reactionList(reactionEquation);

							System.out.println("ru new " + LocalConfig.getInstance().getMetaboliteUsedMap());
							//add new species to used map
							for (int x = 0; x < newReactionList.size(); x++) {
								System.out.println("ru new " + newReactionList.get(x));
								for (int y = 0; y < newReactionList.get(x).size(); y++) {
									if (((ArrayList) newReactionList.get(x).get(y)).size() > 1) {

										if (LocalConfig.getInstance().getMetaboliteUsedMap().containsKey((String) ((ArrayList) newReactionList.get(x).get(y)).get(1))) {
											if (LocalConfig.getInstance().getMetaboliteUsedMap().get((String) ((ArrayList) newReactionList.get(x).get(y)).get(1)) != null) {
												int usedCount = (Integer) LocalConfig.getInstance().getMetaboliteUsedMap().get((String) ((ArrayList) newReactionList.get(x).get(y)).get(1));
												LocalConfig.getInstance().getMetaboliteUsedMap().put((String) ((ArrayList) newReactionList.get(x).get(y)).get(1), new Integer(usedCount + 1));
											}									
										} else {
											LocalConfig.getInstance().getMetaboliteUsedMap().put((String) ((ArrayList) newReactionList.get(x).get(y)).get(1), new Integer(1));
										}
										Integer metabId = (Integer) LocalConfig.getInstance().getMetaboliteIdNameMap().get((String) ((ArrayList) newReactionList.get(x).get(y)).get(1));
										String stoic = ((String) ((ArrayList) newReactionList.get(x).get(y)).get(0));
										if (x == 0) {//reactants
											String rrUpdate2 = "insert into reaction_reactants (reaction_id, metabolite_id, stoic) values (" + reacIdList.get(i) + ", " + metabId + ", " + stoic + ");";				
											stat.executeUpdate(rrUpdate2);				
										}
										if (x == 1) {//products
											String rpUpdate2 = "insert into reaction_products (reaction_id, metabolite_id, stoic) values (" + reacIdList.get(i) + ", " + metabId + ", " + stoic + ");";	
											stat.executeUpdate(rpUpdate2);
										}
									}							
								}
							}
							
						}
						System.out.println("pasted " + LocalConfig.getInstance().getMetaboliteUsedMap());
					} else {
						reactionEquation = " ";						
					}
					
					String reversible = "false";
					if (reactionEquation.contains("<") || (reactionEquation.contains("=") && !reactionEquation.contains(">"))) {
						reversible = "true";
					} else if (reactionEquation.contains("-->") || reactionEquation.contains("->") || reactionEquation.contains("=>")) {
						reversible = "false";		    		
					}
					
					Double lowerBound = GraphicalInterfaceConstants.LOWER_BOUND_DEFAULT;
					if (GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.LOWER_BOUND_COLUMN) != null) {
						try {
							lowerBound = Double.valueOf((String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.LOWER_BOUND_COLUMN));
						}
					    catch ( NumberFormatException nfe ) {
					       System.out.println( "Number format exception" );
					    }				
					} 
					Double upperBound = GraphicalInterfaceConstants.UPPER_BOUND_DEFAULT;
					if (GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.UPPER_BOUND_COLUMN) != null) {
						try {
							upperBound = Double.valueOf((String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.UPPER_BOUND_COLUMN));
						}
					    catch ( NumberFormatException nfe ) {
					       System.out.println( "Number format exception" );
					    }				
					} 
					Double objective = GraphicalInterfaceConstants.BIOLOGICAL_OBJECTIVE_DEFAULT;
					if (GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.BIOLOGICAL_OBJECTIVE_COLUMN) != null) {
						try {
							objective = Double.valueOf((String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.BIOLOGICAL_OBJECTIVE_COLUMN));
						}
					    catch ( NumberFormatException nfe ) {
					       System.out.println( "Number format exception" );
					    }				
					} 
					
					String meta1 = (String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.REACTION_META1_COLUMN);
					if (meta1 != null) {
						if (meta1.contains("'")) {
							meta1 = meta1.replaceAll("'", "''");
						}
					} else {
						meta1 = " ";
					}
					String meta2 = (String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.REACTION_META2_COLUMN);
					if (meta2 != null) {
						if (meta2.contains("'")) {
							meta2 = meta2.replaceAll("'", "''");
						}
					} else {
						meta2 = " ";
					}
					String meta3 = (String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.REACTION_META3_COLUMN);
					if (meta3 != null) {
						if (meta3.contains("'")) {
							meta3 = meta3.replaceAll("'", "''");
						}
					} else {
						meta3 = " ";
					}
					String meta4 = (String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.REACTION_META4_COLUMN);
					if (meta4 != null) {
						if (meta4.contains("'")) {
							meta4 = meta4.replaceAll("'", "''");
						}
					} else {
						meta4 = " ";
					}
					String meta5 = (String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.REACTION_META5_COLUMN);
					if (meta5 != null) {
						if (meta5.contains("'")) {
							meta5 = meta5.replaceAll("'", "''");
						}
					} else {
						meta5 = " ";
					}
					String meta6 = (String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.REACTION_META6_COLUMN);
					if (meta6 != null) {
						if (meta6.contains("'")) {
							meta6 = meta6.replaceAll("'", "''");
						}
					} else {
						meta6 = " ";
					}
					String meta7 = (String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.REACTION_META7_COLUMN);
					if (meta7 != null) {
						if (meta7.contains("'")) {
							meta7 = meta7.replaceAll("'", "''");
						}
					} else {
						meta7 = " ";
					}
					String meta8 = (String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.REACTION_META8_COLUMN);
					if (meta8 != null) {
						if (meta8.contains("'")) {
							meta8 = meta8.replaceAll("'", "''");
						}
					} else {
						meta8 = " ";
					}
					String meta9 = (String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.REACTION_META9_COLUMN);
					if (meta9 != null) {
						if (meta9.contains("'")) {
							meta9 = meta9.replaceAll("'", "''");
						}
					} else {
						meta9 = " ";
					}
					String meta10 = (String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.REACTION_META10_COLUMN);
					if (meta10 != null) {
						if (meta10.contains("'")) {
							meta10 = meta10.replaceAll("'", "''");
						}
					} else {
						meta10 = " ";
					}
					String meta11 = (String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.REACTION_META11_COLUMN);
					if (meta11 != null) {
						if (meta11.contains("'")) {
							meta11 = meta11.replaceAll("'", "''");
						}
					} else {
						meta11 = " ";
					}
					String meta12 = (String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.REACTION_META12_COLUMN);
					if (meta12 != null) {
						if (meta12.contains("'")) {
							meta12 = meta12.replaceAll("'", "''");
						}
					} else {
						meta12 = " ";
					}
					String meta13 = (String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.REACTION_META13_COLUMN);
					if (meta13 != null) {
						if (meta13.contains("'")) {
							meta13 = meta13.replaceAll("'", "''");
						}
					} else {
						meta13 = " ";
					}
					String meta14 = (String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.REACTION_META14_COLUMN);
					if (meta14 != null) {
						if (meta14.contains("'")) {
							meta14 = meta14.replaceAll("'", "''");
						}
					} else {
						meta14 = " ";
					}
					String meta15 = (String) GraphicalInterface.reactionsTable.getModel().getValueAt(rowList.get(i), GraphicalInterfaceConstants.REACTION_META15_COLUMN);
					if (meta15 != null) {
						if (meta15.contains("'")) {
							meta15 = meta15.replaceAll("'", "''");
						}
					} else {
						meta15 = " ";
					}
	
					String update = "update reactions set knockout='" + knockout + "', flux_value=" + fluxValue + ", reaction_abbreviation='" + reactionAbbreviation + "', reaction_name='" + reactionName + "', "
					 	+ " reaction_string='" + reactionEquation + "', reversible='" + reversible + "', lower_bound=" + lowerBound + ", upper_bound=" + upperBound + ", biological_objective=" + objective + ", "
						+ " meta_1='" + meta1 + "', meta_2='" + meta2 + "', meta_3='" + meta3 + "', meta_4='" + meta4 + "', meta_5='" + meta5 + "', "
						+ " meta_6='" + meta6 + "', meta_7='" + meta7 + "', meta_8='" + meta8 + "', meta_9='" + meta9 + "', meta_10='" + meta10 + "', "
						+ " meta_11='" + meta11 + "', meta_12='" + meta12 + "', meta_13='" + meta13 + "', meta_14='" + meta14 + "', meta_15='" + meta15 + "' where id=" + reacIdList.get(i) + ";";
					stat.executeUpdate(update);
				}
				
				stat.executeUpdate("COMMIT");
			} catch (Exception e) {
				e.printStackTrace();
				stat.executeUpdate("ROLLBACK"); // throw away all updates since BEGIN TRANSACTION
			}

		}catch(SQLException e){

			e.printStackTrace();

		}
	}
	
	//used for updating when a single row is edited
	public void updateReactionEquations(int id, String oldEquation, String newEquation, String databaseName) {

		ReactionParser1 parser1 = new ReactionParser1();

		String queryString = "jdbc:sqlite:" + databaseName + ".db";

		try{
			Connection conn =
				DriverManager.getConnection(queryString);
			Statement stat = conn.createStatement();		

			//update for old reaction
			if (parser1.isValid(oldEquation)) {
				ArrayList<ArrayList> oldReactionList = parser1.reactionList(oldEquation);

				//remove old species from used map
				for (int x = 0; x < oldReactionList.size(); x++) {
					for (int y = 0; y < oldReactionList.get(x).size(); y++) {
						if (((ArrayList) oldReactionList.get(x).get(y)).size() > 1) {
							if (LocalConfig.getInstance().getMetaboliteUsedMap().get((String) ((ArrayList) oldReactionList.get(x).get(y)).get(1)) != null) {
								int usedCount = (Integer) LocalConfig.getInstance().getMetaboliteUsedMap().get((String) ((ArrayList) oldReactionList.get(x).get(y)).get(1));
								if (usedCount > 1) {
									LocalConfig.getInstance().getMetaboliteUsedMap().put((String) ((ArrayList) oldReactionList.get(x).get(y)).get(1), new Integer(usedCount - 1));
								} else {
									LocalConfig.getInstance().getMetaboliteUsedMap().remove((String) ((ArrayList) oldReactionList.get(x).get(y)).get(1));
								}
							}			
						}					
					}
				}
			}

			try {
				stat.executeUpdate("BEGIN TRANSACTION");

				String rrUpdate = "delete from reaction_reactants where reaction_id=" + id + ";";				
				stat.executeUpdate(rrUpdate);
				String rpUpdate = "delete from reaction_products where reaction_id=" + id + ";";				
				stat.executeUpdate(rpUpdate);				
				stat.executeUpdate("COMMIT");
			} catch (Exception e) {
				e.printStackTrace();
				stat.executeUpdate("ROLLBACK"); // throw away all updates since BEGIN TRANSACTION
			}
			
			//update for new reaction
			int maxMetabId = LocalConfig.getInstance().getMaxMetaboliteId();
			try {
				ReactionParser1 parser = new ReactionParser1();
				boolean valid = true;
				
				if (parser.isValid(newEquation)) {
					ArrayList<ArrayList> reactants = parser.reactionList(newEquation.trim()).get(0);
					//reactions of the type ==> b will be size 1, assigned the value [0] in parser
					if (reactants.get(0).size() == 1) {
					} else {
						for (int r = 0; r < reactants.size(); r++) {
							if (reactants.get(r).size() == 2) {
								String stoicStr = (String) reactants.get(r).get(0);
								String reactant = (String) reactants.get(r).get(1);
								String addMetab = "insert into metabolites (metabolite_abbreviation, boundary, used) values('"  + reactant + "', 'false', 'true');";
								String update = "update metabolites set metabolite_abbreviation='" + reactant + "', boundary='false' where id=" + (LocalConfig.getInstance().getMaxMetaboliteId() + 1) + ";";	
								
								if (!(LocalConfig.getInstance().getMetaboliteIdNameMap().containsKey(reactant.trim()))) {
									if (GraphicalInterface.showPrompt) {
										Object[] options = {"Yes",
												"Yes to All",
										"No"};

										int choice = JOptionPane.showOptionDialog(null, 
												"The metabolite " + reactant + " does not exist. Do you wish to add it?", 
												"Add Metabolite?", 
												JOptionPane.YES_NO_CANCEL_OPTION, 
												JOptionPane.QUESTION_MESSAGE, 
												null, options, options[0]);
										//options[0] sets "Yes" as default button

										// interpret the user's choice	  
										if (choice == JOptionPane.YES_OPTION)
										{
											//if blank load database starts at 1 instead of 101 ecause the blank load
											// table has 100 rows
											if (LocalConfig.getInstance().getLoadedDatabase().compareTo(GraphicalInterfaceConstants.DEFAULT_DATABASE_NAME) == 0) {
												stat.executeUpdate(update);
											} else {
												stat.executeUpdate(addMetab);			
											}
											
											maxMetabId += 1;
											LocalConfig.getInstance().setMaxMetaboliteId(maxMetabId);
											LocalConfig.getInstance().getMetaboliteIdNameMap().put(reactant, new Integer(LocalConfig.getInstance().getMaxMetaboliteId()));
										}
										//No option actually corresponds to "Yes to All" button
										if (choice == JOptionPane.NO_OPTION)
										{
											GraphicalInterface.showPrompt = false;
											if (LocalConfig.getInstance().getLoadedDatabase().compareTo(GraphicalInterfaceConstants.DEFAULT_DATABASE_NAME) == 0) {
												stat.executeUpdate(update);
											} else {
												stat.executeUpdate(addMetab);				
											}
											
											maxMetabId += 1;
											LocalConfig.getInstance().setMaxMetaboliteId(maxMetabId);
											LocalConfig.getInstance().getMetaboliteIdNameMap().put(reactant, new Integer(LocalConfig.getInstance().getMaxMetaboliteId()));
										}
										//Cancel option actually corresponds to "No" button
										if (choice == JOptionPane.CANCEL_OPTION) {
											//addMetaboliteOption = false;
											//reactionString = "";
											valid = false;
										}	  
									} else {
										if (LocalConfig.getInstance().getLoadedDatabase().compareTo(GraphicalInterfaceConstants.DEFAULT_DATABASE_NAME) == 0) {
											stat.executeUpdate(update);
										} else {
											stat.executeUpdate(addMetab);			
										}
										
										maxMetabId += 1;
										LocalConfig.getInstance().setMaxMetaboliteId(maxMetabId);
										LocalConfig.getInstance().getMetaboliteIdNameMap().put(reactant, new Integer(LocalConfig.getInstance().getMaxMetaboliteId()));
									}											
								}										
								
								Integer metabId = (Integer) LocalConfig.getInstance().getMetaboliteIdNameMap().get(reactant);
								
								String insert = "INSERT INTO reaction_reactants(reaction_id, stoic, metabolite_id) values (" + id + ", " + stoicStr + ", " + metabId + ");";
								stat.executeUpdate(insert);
								
								if (LocalConfig.getInstance().getMetaboliteUsedMap().containsKey(reactant)) {
									int usedCount = (Integer) LocalConfig.getInstance().getMetaboliteUsedMap().get(reactant);
									LocalConfig.getInstance().getMetaboliteUsedMap().put(reactant, new Integer(usedCount + 1));									
								} else {
									LocalConfig.getInstance().getMetaboliteUsedMap().put(reactant, new Integer(1));
								}	
								
							} else {
								//Invalid reaction
								valid = false;
								break;
							}								
						}
					}
					//reactions of the type a ==> will be size 1, assigned the value [0] in parser
					ArrayList<ArrayList> products = parser.reactionList(newEquation.trim()).get(1);
					if (products.get(0).size() == 1) {
					} else {
						for (int p = 0; p < products.size(); p++) {
							if (products.get(p).size() == 2) {
								String stoicStr = (String) products.get(p).get(0);
								String product = (String) products.get(p).get(1);
								String addMetab = "insert into metabolites (metabolite_abbreviation, boundary, used) values('"  + product + "', 'false', 'true');";
								String update = "update metabolites set metabolite_abbreviation='" + product + "', boundary='false' where id=" + (LocalConfig.getInstance().getMaxMetaboliteId() + 1) + ";";	
								
								if (!(LocalConfig.getInstance().getMetaboliteIdNameMap().containsKey(product))) {
									if (GraphicalInterface.showPrompt) {
										Object[] options = {"Yes",
												"Yes to All",
										"No"};

										int choice = JOptionPane.showOptionDialog(null, 
												"The metabolite " + product + " does not exist. Do you wish to add it?", 
												"Add Metabolite?", 
												JOptionPane.YES_NO_CANCEL_OPTION, 
												JOptionPane.QUESTION_MESSAGE, 
												null, options, options[0]);
										//options[0] sets "Yes" as default button

										// interpret the user's choice	  
										if (choice == JOptionPane.YES_OPTION)
										{
											if (LocalConfig.getInstance().getLoadedDatabase().compareTo(GraphicalInterfaceConstants.DEFAULT_DATABASE_NAME) == 0) {
												stat.executeUpdate(update);
											} else {
												stat.executeUpdate(addMetab);				
											}
											
											maxMetabId += 1;
											LocalConfig.getInstance().setMaxMetaboliteId(maxMetabId);
											LocalConfig.getInstance().getMetaboliteIdNameMap().put(product, new Integer(LocalConfig.getInstance().getMaxMetaboliteId()));
										}
										//No option actually corresponds to "Yes to All" button
										if (choice == JOptionPane.NO_OPTION)
										{
											GraphicalInterface.showPrompt = false;
											if (LocalConfig.getInstance().getLoadedDatabase().compareTo(GraphicalInterfaceConstants.DEFAULT_DATABASE_NAME) == 0) {
												stat.executeUpdate(update);
											} else {
												stat.executeUpdate(addMetab);				
											}
											
											maxMetabId += 1;
											LocalConfig.getInstance().setMaxMetaboliteId(maxMetabId);
											LocalConfig.getInstance().getMetaboliteIdNameMap().put(product, new Integer(LocalConfig.getInstance().getMaxMetaboliteId()));
										}
										//Cancel option actually corresponds to "No" button
										if (choice == JOptionPane.CANCEL_OPTION) {
											//addMetaboliteOption = false;
											//reactionString = "";
											valid = false;
										}	  
									} else {
										if (LocalConfig.getInstance().getLoadedDatabase().compareTo(GraphicalInterfaceConstants.DEFAULT_DATABASE_NAME) == 0) {
											stat.executeUpdate(update);
										} else {
											stat.executeUpdate(addMetab);				
										}
										
										maxMetabId += 1;
										LocalConfig.getInstance().setMaxMetaboliteId(maxMetabId);
										LocalConfig.getInstance().getMetaboliteIdNameMap().put(product, new Integer(LocalConfig.getInstance().getMaxMetaboliteId()));
									}		
								}
								
								Integer metabId = (Integer) LocalConfig.getInstance().getMetaboliteIdNameMap().get(product);
								
								String insert = "INSERT INTO reaction_products(reaction_id, stoic, metabolite_id) values (" + id + ", " + stoicStr + ", " + metabId + ");";
								stat.executeUpdate(insert);	
								if (LocalConfig.getInstance().getMetaboliteUsedMap().containsKey(product)) {
									int usedCount = (Integer) LocalConfig.getInstance().getMetaboliteUsedMap().get(product);
									LocalConfig.getInstance().getMetaboliteUsedMap().put(product, new Integer(usedCount + 1));									
								} else {
									LocalConfig.getInstance().getMetaboliteUsedMap().put(product, new Integer(1));
								}
								
							} else {
								//Invalid reaction
								valid = false;
								break;
							}
						}							
					}
				} else {
					//Invalid reaction
					valid = false;
				}
				
				
				if (!valid) {
					String deleteReac = "delete from reaction_reactants where reaction_id=" + id + ";";
					stat.executeUpdate(deleteReac);
					String deleteProd = "delete from reaction_products where reaction_id=" + id + ";";
					stat.executeUpdate(deleteProd);
					if (newEquation != null || newEquation.length() > 0) {
						LocalConfig.getInstance().getInvalidReactions().add(newEquation);
					}	
				}									
			} catch (Throwable t) {
				
			}
			
		}catch(SQLException e){

			e.printStackTrace();

		}
	}
	
	public void deleteRows(ArrayList<Integer> idList, ArrayList<String> deletedReactions, String databaseName) {

		String queryString = "jdbc:sqlite:" + databaseName + ".db";
		
		try{
			Connection conn =
				DriverManager.getConnection(queryString);
			Statement stat = conn.createStatement();

			try {
				stat.executeUpdate("BEGIN TRANSACTION");

				for (int i = 0; i < idList.size(); i++) {
					String delete = "delete from reactions where id = " + idList.get(i) + ";";
					stat.executeUpdate(delete);
					String rrDelete = "delete from reaction_reactants where reaction_id = " + idList.get(i) + ";";
					stat.executeUpdate(rrDelete);
					String rpDelete = "delete from reaction_products where reaction_id = " + idList.get(i) + ";";
					stat.executeUpdate(rpDelete);
				}
				
				stat.executeUpdate("COMMIT");
			} catch (Exception e) {
				e.printStackTrace();
				stat.executeUpdate("ROLLBACK"); // throw away all updates since BEGIN TRANSACTION
			}

		}catch(SQLException e){

			e.printStackTrace();

		}
		
		ReactionParser1 parser1 = new ReactionParser1();
		
		for (int r = 0; r < deletedReactions.size(); r++) {
			if (parser1.isValid(deletedReactions.get(r))) {
				ArrayList<ArrayList> oldReactionList = parser1.reactionList(deletedReactions.get(r));
				//remove old species from used map
				for (int x = 0; x < oldReactionList.size(); x++) {
					for (int y = 0; y < oldReactionList.get(x).size(); y++) {
						if (((ArrayList) oldReactionList.get(x).get(y)).size() > 1) {
							if (LocalConfig.getInstance().getMetaboliteUsedMap().get((String) ((ArrayList) oldReactionList.get(x).get(y)).get(1)) != null) {
								int usedCount = (Integer) LocalConfig.getInstance().getMetaboliteUsedMap().get((String) ((ArrayList) oldReactionList.get(x).get(y)).get(1));
								if (usedCount > 1) {
									LocalConfig.getInstance().getMetaboliteUsedMap().put((String) ((ArrayList) oldReactionList.get(x).get(y)).get(1), new Integer(usedCount - 1));
								} else {
									LocalConfig.getInstance().getMetaboliteUsedMap().remove((String) ((ArrayList) oldReactionList.get(x).get(y)).get(1));
								}
							}			
						}		
					}
				}
			}			
		}
	}	
}
