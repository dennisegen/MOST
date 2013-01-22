package edu.rutgers.MOST.presentation;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.xml.stream.XMLStreamException;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;

import edu.rutgers.MOST.config.ConfigConstants;
import edu.rutgers.MOST.config.LocalConfig;
import edu.rutgers.MOST.data.DatabaseCopier;
import edu.rutgers.MOST.data.DatabaseCreator;
import edu.rutgers.MOST.data.DatabaseErrorChecker;
import edu.rutgers.MOST.data.FBAModel;
import edu.rutgers.MOST.data.JSBMLWriter;
import edu.rutgers.MOST.data.MetaboliteFactory;
import edu.rutgers.MOST.data.MetabolitesMetaColumnManager;
import edu.rutgers.MOST.data.MetabolitesUpdater;
import edu.rutgers.MOST.data.ReactionFactory;
import edu.rutgers.MOST.data.ReactionsMetaColumnManager;
import edu.rutgers.MOST.data.ReactionsUpdater;
import edu.rutgers.MOST.data.SBMLMetabolite;
import edu.rutgers.MOST.data.SBMLModelReader;
import edu.rutgers.MOST.data.SBMLReaction;
import edu.rutgers.MOST.data.SettingsFactory;
import edu.rutgers.MOST.data.TextMetabolitesModelReader;
import edu.rutgers.MOST.data.TextMetabolitesWriter;
import edu.rutgers.MOST.data.TextReactionsModelReader;
import edu.rutgers.MOST.data.TextReactionsWriter;
import edu.rutgers.MOST.logic.ReactionParser;
import edu.rutgers.MOST.optimization.FBA.FBA;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyAdapter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.*;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import javax.swing.event.ChangeListener;

import layout.TableLayout;

public class GraphicalInterface extends JFrame {
	//log4j
	static Logger log = Logger.getLogger(GraphicalInterface.class);

	// Excel calls this a formula bar
	public static JTextField formulaBar = new JTextField();
	
	public static JXTable reactionsTable = new JXTable();
	// based on http://www.coderanch.com/t/345041/GUI/java/Disabling-cell-JTable
	public static JXTable metabolitesTable = new JXTable(){  
		public boolean isCellEditable(int row, int column){	    	  
			Object o = getValueAt(row, column); 
			String s = (String) getValueAt(row, column); 
			int id = 0;
			if (column == GraphicalInterfaceConstants.DB_METABOLITE_ID_COLUMN) {				
				return false;
			} else if (column == GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN && o != null && LocalConfig.getInstance().getMetaboliteUsedMap().containsKey(s)) {
				id = Integer.valueOf((String) getValueAt(row, GraphicalInterfaceConstants.DB_METABOLITE_ID_COLUMN));
				if (LocalConfig.getInstance().getDuplicateIds().contains(id)) {
					return true;
				} else {
					return false;
				}				  
			}
			return true;  
		}  
	};  
	
	public static JTextArea outputTextArea = new JTextArea();
	
	public static JLabel statusBar = new JLabel();

	//set tabs south (bottom) = 3
	public JTabbedPane tabbedPane = new JTabbedPane(3); 

	//Methods of saving current directory
	public static SettingsFactory curSettings;
	
	public static DefaultListModel<String> listModel = new DefaultListModel();
	public static FileList fileList = new FileList();
	static JScrollPane fileListPane = new JScrollPane(fileList);	

	private Task task;	
	public final ProgressBar progressBar = new ProgressBar();	
	javax.swing.Timer timer = new javax.swing.Timer(1000, new TimeListener());
	
	public final CSVLoadInterface csvLoadInterface = new CSVLoadInterface();

	public static boolean highlightUnusedMetabolites;	
	public static boolean highlightParticipatingRxns;
	public static boolean showPrompt;
	public static boolean selectAllRxn;	
	public static boolean includeRxnColumnNames;
	public static boolean selectAllMtb;	
	public static boolean includeMtbColumnNames;
	public static boolean rxnColSelectionMode;;	
	public static boolean mtbColSelectionMode;
	public static boolean isCSVFile;
	public static boolean validPaste;                 // used for error message when pasting non-valid values
	public static boolean showErrorMessage;
	public static boolean saveOptFile;
	public static boolean addReacColumn;              // used to scroll added column to visible
	public static boolean addMetabColumn;             // used to scroll added column to visible
	public static boolean duplicatePromptShown;		  // ensures "Duplicate Metabolite" prompt displayed once per event
	public static boolean renameMetabolite;           // if Rename menu action determines used, is set to true to for OK button action          
	public static boolean reactionsTableEditable;	  // if fileList item index > 0, is false
	//public static boolean exit;
	
	public static ReactionEditor reactionEditor;

	public void setReactionEditor(ReactionEditor reactionEditor) {
		this.reactionEditor = reactionEditor;
	}

	public static ReactionEditor getReactionEditor() {
		return reactionEditor;
	}
	
	public static MetaboliteRenameInterface metaboliteRenameInterface;
	
	public void setMetaboliteRenameInterface(MetaboliteRenameInterface metaboliteRenameInterface) {
		this.metaboliteRenameInterface = metaboliteRenameInterface;
	}

	public static MetaboliteRenameInterface getMetaboliteRenameInterface() {
		return metaboliteRenameInterface;
	}
	
	public static int currentRow;

	public void setCurrentRow(int currentRow){
		this.currentRow = currentRow;
	}

	public static int getCurrentRow() {
		return currentRow;
	}

	public static int currentFileListRow;

	public void setCurrentFileListRow(int currentFileListRow){
		this.currentFileListRow = currentFileListRow;
	}

	public static int getCurrentFileListRow() {
		return currentFileListRow;
	}

    public static int reactionsSortColumnIndex;
	
	public void setReactionsSortColumnIndex(int reactionsSortColumnIndex){
		this.reactionsSortColumnIndex = reactionsSortColumnIndex;
	}
	
	public static int getReactionsSortColumnIndex() {
		return reactionsSortColumnIndex;
	}
	
	public static SortOrder reactionsSortOrder;
	
	public void setReactionsSortOrder(SortOrder reactionsSortOrder){
		this.reactionsSortOrder = reactionsSortOrder;
	}
	
	public static SortOrder getReactionsSortOrder() {
		return reactionsSortOrder;
	}
	
    public static int metabolitesSortColumnIndex;
	
	public void setMetabolitesSortColumnIndex(int metabolitesSortColumnIndex){
		this.metabolitesSortColumnIndex = metabolitesSortColumnIndex;
	}
	
	public static int getMetabolitesSortColumnIndex() {
		return metabolitesSortColumnIndex;
	}
	
	public static SortOrder metabolitesSortOrder;
	
	public void setMetabolitesSortOrder(SortOrder metabolitesSortOrder){
		this.metabolitesSortOrder = metabolitesSortOrder;
	}
	
	public static SortOrder getMetabolitesSortOrder() {
		return metabolitesSortOrder;
	}
	
	public static String databaseName;

	public static void setDatabaseName(String databaseName) {

		LocalConfig localConfig = LocalConfig.getInstance();
		localConfig.setDatabaseName(databaseName);
	}

	public static String getDatabaseName() {
		LocalConfig localConfig = LocalConfig.getInstance();
		return localConfig.getDatabaseName();
	}

	public static String dbPath;

	public void setDBPath(String dbPath) {
		this.dbPath = dbPath;
	}

	public static String getDBPath() {
		return dbPath;
	}

	public static String dbFilename;

	public void setDBFilename(String dbFilename) {
		this.dbFilename = dbFilename;
	}

	public static String getDBFilename() {
		return dbFilename;
	}

	public static File SBMLFile;

	public void setSBMLFile(File SBMLFile) {
		this.SBMLFile = SBMLFile;
	}

	public static File getSBMLFile() {
		return SBMLFile;
	}

	public static String optimizePath;

	public void setOptimizePath(String optimizePath) {
		this.optimizePath = optimizePath;
	}

	public static String getOptimizePath() {
		return optimizePath;
	}

	public static String extension;

	public void setExtension(String extension) {
		this.extension = extension;
	}

	public static String getExtension() {
		return extension;
	}
	
	public static String loadErrorMessage;

	public void setLoadErrorMessage(String loadErrorMessage) {
		this.loadErrorMessage = loadErrorMessage;
	}

	public static String getLoadErrorMessage() {
		return loadErrorMessage;
	}
	
	// menu items
	public final JMenuItem saveSBMLItem = new JMenuItem("Save As SBML");
	public final JMenuItem saveCSVMetabolitesItem = new JMenuItem("Save As CSV Metabolites");
	public final JMenuItem saveCSVReactionsItem = new JMenuItem("Save As CSV Reactions");
	public final JMenuItem saveSQLiteItem = new JMenuItem("Save As SQLite");
	public final JMenuItem fbaItem = new JMenuItem("FBA");
	public final JCheckBoxMenuItem highlightUnusedMetabolitesItem = new JCheckBoxMenuItem("Highlight Unused Metabolites");
	public final JMenuItem deleteUnusedItem = new JMenuItem("Delete All Unused Metabolites");
	public final JMenuItem addReacRowItem = new JMenuItem("Add Row to Reactions Table");
	public final JMenuItem addMetabRowItem = new JMenuItem("Add Row to Metabolites Table");
	public final JMenuItem addReacColumnItem = new JMenuItem("Add Column to Reactions Table");
	public final JMenuItem addMetabColumnItem = new JMenuItem("Add Column to Metabolites Table"); 
	
	ArrayList<Image> icons;

	public void setIconsList(ArrayList<Image> icons) {
		this.icons = icons;
	}

	public ArrayList<Image> getIconsList() {
		return icons;
	}    

	public static OutputPopout popout;

	public void setPopout(OutputPopout popout) {
		this.popout = popout;
	}

	public static OutputPopout getPopout() {
		return popout;
	}

	public static ReactionColAddRenameInterface reactionColAddRenameInterface;
	
	public void setReactionColAddRenameInterface(ReactionColAddRenameInterface reactionColAddRenameInterface) {
		this.reactionColAddRenameInterface = reactionColAddRenameInterface;
	}

	public static ReactionColAddRenameInterface getReactionColAddRenameInterface() {
		return reactionColAddRenameInterface;
	}
	
    public static MetaboliteColAddRenameInterface metaboliteColAddRenameInterface;   
	
	public void setMetaboliteColAddRenameInterface(MetaboliteColAddRenameInterface metaboliteColAddRenameInterface) {
		this.metaboliteColAddRenameInterface = metaboliteColAddRenameInterface;
	}

	public static MetaboliteColAddRenameInterface getMetaboliteColAddRenameInterface() {
		return metaboliteColAddRenameInterface;
	}
	
	public static String pasteError;

	public void setPasteError(String pasteError) {
		this.pasteError = pasteError;
	}

	public static String getPasteError() {
		return pasteError;
	}
	
	public static String oldReaction;

	public void setOldReaction(String oldReaction) {
		this.oldReaction = oldReaction;
	}

	public static String getOldReaction() {
		return oldReaction;
	}
	
	ArrayList<Integer> deletedReactionColumns = new ArrayList<Integer>();
	ArrayList<Integer> deletedMetaboliteColumns = new ArrayList<Integer>();
	
	@SuppressWarnings("unchecked")
	public GraphicalInterface(final Connection con)
	throws SQLException {
			
		//System.out.println("max memory " + java.lang.Runtime.getRuntime().maxMemory());

		LocalConfig.getInstance().setProgress(0);
		progressBar.pack();
		progressBar.setIconImages(icons);
		progressBar.setSize(200, 70);
		progressBar.setResizable(false);
		progressBar.setTitle("Loading...");
		progressBar.progress.setIndeterminate(true);
		progressBar.setVisible(false);
		
		csvLoadInterface.setIconImages(icons);					
		csvLoadInterface.setSize(600, 200);
		csvLoadInterface.setResizable(false);
		csvLoadInterface.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		csvLoadInterface.setLocationRelativeTo(null);		
		csvLoadInterface.setVisible(false);			
		csvLoadInterface.okButton.addActionListener(okButtonCSVLoadActionListener);

		setDatabaseName(ConfigConstants.DEFAULT_DATABASE_NAME);
		LocalConfig.getInstance().setLoadedDatabase(ConfigConstants.DEFAULT_DATABASE_NAME);

		setTitle(GraphicalInterfaceConstants.TITLE + " - " + ConfigConstants.DEFAULT_DATABASE_NAME);

		addWindowListener(new WindowAdapter() {
	        public void windowClosing(WindowEvent evt) {
	        	SaveChangesPrompt();
	            // Exit the application
		        System.exit(0);	        	
	        }
		});
		
		final ArrayList<Image> icons = new ArrayList<Image>(); 
		icons.add(new ImageIcon("etc/most16.jpg").getImage()); 
		icons.add(new ImageIcon("etc/most32.jpg").getImage());
		setIconsList(icons);
		
		setReactionsSortColumnIndex(0);
		setMetabolitesSortColumnIndex(0);
		setReactionsSortOrder(SortOrder.ASCENDING);
		setMetabolitesSortOrder(SortOrder.ASCENDING);
		
		selectAllRxn = true;	
		includeRxnColumnNames = true;
		selectAllMtb = true;	
		includeMtbColumnNames = true;	
		rxnColSelectionMode = false;
		mtbColSelectionMode = false;
		isCSVFile = false;
		highlightParticipatingRxns = false;
		validPaste = true;
		LocalConfig.getInstance().noButtonClicked = false;
		LocalConfig.getInstance().reactionsTableChanged = false;
		LocalConfig.getInstance().metabolitesTableChanged = false;
		showErrorMessage = true;
		saveOptFile = false;
		addReacColumn = false;
		addMetabColumn = false;
		duplicatePromptShown = false;
		renameMetabolite = false;
		reactionsTableEditable = true;
		//exit = true;		
		
		listModel.addElement(GraphicalInterfaceConstants.DEFAULT_DATABASE_NAME);
		
		ArrayList<Integer> participatingReactions = new ArrayList<Integer>();
		LocalConfig.getInstance().setParticipatingReactions(participatingReactions);
		Map<String, Object> metaboliteIdNameMap = new HashMap<String, Object>();
		LocalConfig.getInstance().setMetaboliteIdNameMap(metaboliteIdNameMap);
		Map<String, Object> metaboliteUsedMap = new HashMap<String, Object>();
		LocalConfig.getInstance().setMetaboliteUsedMap(metaboliteUsedMap);
		ArrayList<Integer> blankMetabIds = new ArrayList<Integer>();
		LocalConfig.getInstance().setBlankMetabIds(blankMetabIds);
		ArrayList<String> optimizationFilesList = new ArrayList<String>();
		LocalConfig.getInstance().setOptimizationFilesList(optimizationFilesList);
		
		LocalConfig.getInstance().setMaxMetaboliteId(0);
		
		outputTextArea.setEditable(false);
		
		LocalConfig.getInstance().pastedReaction = false;
	
		/**************************************************************************/
		//set up fileList
		/**************************************************************************/

		fileList.setModel(listModel);
		fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 
		fileList.setSelectedIndex(0);

		fileList.addListSelectionListener(new ListSelectionListener() 
		{ 
			public void valueChanged(ListSelectionEvent e) 
			{  
				fileList.saveItem.setEnabled(false);
				fileList.saveAsCSVItem.setEnabled(false);
				fileList.saveAsSBMLItem.setEnabled(false);
				fileList.saveAllItem.setEnabled(false);
				fileList.deleteItem.setEnabled(false);
				fileList.clearItem.setEnabled(false);
				if(fileList.getSelectedIndex() == 0) {
					saveSBMLItem.setEnabled(true);
					saveCSVMetabolitesItem.setEnabled(true);
					saveCSVReactionsItem.setEnabled(true);
					saveSQLiteItem.setEnabled(true);
					fbaItem.setEnabled(true);
					addReacRowItem.setEnabled(true);
					addMetabRowItem.setEnabled(true);
					addReacColumnItem.setEnabled(true);
					addMetabColumnItem.setEnabled(true);
					reactionsTableEditable = true;
					saveOptFile = false;
					formulaBar.setEditable(true);
					clearOutputPane();
					if (getPopout() != null) {
						getPopout().clear();
					}				  
					String fileString = "jdbc:sqlite:" + getDatabaseName() + ".db";
					LocalConfig.getInstance().setLoadedDatabase(getDatabaseName());
					try {
						Class.forName("org.sqlite.JDBC");
						Connection con = DriverManager.getConnection(fileString);			    
						highlightUnusedMetabolites = false;
						highlightUnusedMetabolitesItem.setState(false);
						setUpMetabolitesTable(con);
						setUpReactionsTable(con);						
						setTitle(GraphicalInterfaceConstants.TITLE + " - " + getDatabaseName());
					} catch (ClassNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (SQLException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
				} else if (fileList.getSelectedIndex() == -1) {
					fileList.setSelectedIndex(0);
				} else {
					saveSBMLItem.setEnabled(false);
					saveCSVMetabolitesItem.setEnabled(false);
					saveCSVReactionsItem.setEnabled(false);
					saveSQLiteItem.setEnabled(false);
					fbaItem.setEnabled(false);
					addReacRowItem.setEnabled(false);
					addMetabRowItem.setEnabled(false);
					addReacColumnItem.setEnabled(false);
					addMetabColumnItem.setEnabled(false);
					reactionsTableEditable = false;
					tabbedPane.setSelectedIndex(0);
					formulaBar.setEditable(false);
					formulaBar.setBackground(Color.WHITE);
					if (fileList.getSelectedValue() != null) {
						//gets the full path of optimize
						String optimizePath = getOptimizePath();
						if (optimizePath.contains("\\")) {
							optimizePath = optimizePath.substring(0, optimizePath.lastIndexOf("\\") + 1) + fileList.getSelectedValue().toString();
						} else {
							optimizePath = fileList.getSelectedValue().toString();
						}
						setOptimizePath(optimizePath);
						if (getOptimizePath().endsWith(fileList.getSelectedValue().toString())) {
							loadOutputPane(getOptimizePath() + ".log");
							if (getPopout() != null) {
								getPopout().load(getOptimizePath() + ".log");
							}	

							String fileString = "jdbc:sqlite:" + getOptimizePath() + ".db";
							LocalConfig.getInstance().setLoadedDatabase(getOptimizePath());
							try {
								Class.forName("org.sqlite.JDBC");
								Connection con = DriverManager.getConnection(fileString);			    
								highlightUnusedMetabolites = false;
								highlightUnusedMetabolitesItem.setState(false);
								setUpMetabolitesTable(con);
								setUpReactionsTable(con);
								setTitle(GraphicalInterfaceConstants.TITLE + " - " + getOptimizePath());	
							} catch (ClassNotFoundException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (SQLException e2) {
								// TODO Auto-generated catch block
								e2.printStackTrace();
							}			  
						} 
					}
				}        	  
			} 
		});   

		fileList.saveAsCSVItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) { 
				saveOptFile = true;	
				saveReactionsTextFileChooser();
			}
		});
			
		fileList.saveAsSBMLItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) { 
				// Add action here when save SBML works	
			}
		});
		
		fileList.deleteItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) { 
				String fileString = "jdbc:sqlite:" + getDatabaseName() + ".db";
				LocalConfig.getInstance().setLoadedDatabase(getDatabaseName());
				try {
					Class.forName("org.sqlite.JDBC");
					Connection con = DriverManager.getConnection(fileString);			    
					setUpMetabolitesTable(con);
					setUpReactionsTable(con);
					setTitle(GraphicalInterfaceConstants.TITLE + " - " + getDatabaseName());
					clearOutputPane();
				} catch (ClassNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (SQLException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}				
			}
		});

		fileList.clearItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) { 
				setUpTables();				
			}
		});

		final JPopupMenu outputPopupMenu = new JPopupMenu(); 
		JMenuItem popOutItem = new JMenuItem("Pop Out");
		outputPopupMenu.add(popOutItem);
		popOutItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) { 	
				OutputPopout popout = new OutputPopout();
				setPopout(popout);
				if (getOptimizePath() != null) {
					popout.load(getOptimizePath() + ".log");
				}							
			}
		});

		outputTextArea.addMouseListener(new MouseAdapter() {

			public void mousePressed(MouseEvent e)  {check(e);}
			public void mouseReleased(MouseEvent e) {check(e);}

			public void check(MouseEvent e) {
				if (e.isPopupTrigger()) { //if the event shows the menu
					outputPopupMenu.show(outputTextArea, e.getX(), e.getY()); 
				}
			}
		});	

		/**************************************************************************/
		//create menu bar
		/**************************************************************************/
		JMenuBar menuBar = new JMenuBar();

		setJMenuBar(menuBar);

		JMenu modelMenu = new JMenu("Model");
		modelMenu.setMnemonic(KeyEvent.VK_M);

		JMenuItem loadSBMLItem = new JMenuItem("Load SBML");
		modelMenu.add(loadSBMLItem);
		loadSBMLItem.setMnemonic(KeyEvent.VK_L);
		loadSBMLItem.addActionListener(new LoadSBMLAction());

		JMenuItem loadCSVItem = new JMenuItem("Load CSV");
		modelMenu.add(loadCSVItem);
		loadCSVItem.setMnemonic(KeyEvent.VK_C);
		loadCSVItem.addActionListener(new LoadCSVAction());
		
		JMenuItem loadSQLItem = new JMenuItem("Load SQLite");
		modelMenu.add(loadSQLItem);
		loadSQLItem.setMnemonic(KeyEvent.VK_Q);
		loadSQLItem.addActionListener(new LoadSQLiteItemAction());
		
		modelMenu.addSeparator();

		modelMenu.add(saveSBMLItem);
		saveSBMLItem.setMnemonic(KeyEvent.VK_O);
		saveSBMLItem.addActionListener(new SaveSBMLItemAction());
		
		modelMenu.add(saveCSVMetabolitesItem);
		saveCSVMetabolitesItem.setMnemonic(KeyEvent.VK_O);
		saveCSVMetabolitesItem.addActionListener(new SaveCSVMetabolitesItemAction());

		modelMenu.add(saveCSVReactionsItem);
		saveCSVReactionsItem.setMnemonic(KeyEvent.VK_N);
		saveCSVReactionsItem.addActionListener(new SaveCSVReactionsItemAction());
		
		modelMenu.add(saveSQLiteItem);
		saveSQLiteItem.setMnemonic(KeyEvent.VK_A);
		saveSQLiteItem.addActionListener(new SaveSQLiteItemAction());

		modelMenu.addSeparator();

		JMenuItem clearItem = new JMenuItem("Clear");
		modelMenu.add(clearItem);
		clearItem.setMnemonic(KeyEvent.VK_E);
		clearItem.addActionListener(new ClearAction());

		modelMenu.addSeparator();
		
		JMenuItem exitItem = new JMenuItem("Exit");
		modelMenu.add(exitItem);
		exitItem.setMnemonic(KeyEvent.VK_X);
		exitItem.addActionListener(new ExitAction());
		
		menuBar.add(modelMenu);

		//Simulate menu
		JMenu simulateMenu = new JMenu("Simulate");
		simulateMenu.setMnemonic(KeyEvent.VK_S);
		
		simulateMenu.add(fbaItem);
		
		fbaItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
                //load original db into tables
				fileList.removeSelectionInterval(0, listModel.size());
				String fileString1 = "jdbc:sqlite:" + getDatabaseName() + ".db";
				LocalConfig.getInstance().setLoadedDatabase(getDatabaseName());
				try {
					Class.forName("org.sqlite.JDBC");
					Connection con = DriverManager.getConnection(fileString1);			    
					highlightUnusedMetabolites = false;
					highlightUnusedMetabolitesItem.setState(false);
					setUpMetabolitesTable(con);
					setUpReactionsTable(con);
					setTitle(GraphicalInterfaceConstants.TITLE + " - " + getDatabaseName());
				} catch (ClassNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (SQLException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}

				Date date = new Date();
				Format formatter;
				formatter = new SimpleDateFormat("_yyMMdd_HHmmss");
				String dateTimeStamp = formatter.format(date);

				DatabaseCopier copier = new DatabaseCopier();
				String optimizePath = "";
				if (getDatabaseName().contains("\\")) {
					optimizePath = (getDatabaseName().substring(0,
							getDatabaseName().lastIndexOf("\\") + 1))
							+ GraphicalInterfaceConstants.OPTIMIZATION_PREFIX
							+ (getDatabaseName().substring(
									getDatabaseName().lastIndexOf("\\") + 1) + dateTimeStamp);
				} else {
					optimizePath = GraphicalInterfaceConstants.OPTIMIZATION_PREFIX
					+ getDatabaseName() + dateTimeStamp;
				}

				// TODO: should be in the try/catch since if optimization fails
				// item should not be added to fileList
				copier.copyDatabase(getDatabaseName(), optimizePath);
				listModel.addElement(GraphicalInterfaceConstants.OPTIMIZATION_PREFIX
						+ (getDatabaseName().substring(getDatabaseName().lastIndexOf("\\") + 1) + dateTimeStamp));				
				LocalConfig.getInstance().getOptimizationFilesList().add(optimizePath);
				
				setOptimizePath(optimizePath);
				fileList.setSelectedIndex(listModel.size() - 1);
			
				// DEGEN: Begin optimization

				FBAModel model = new FBAModel(getDatabaseName());

				// TODO should this be in a try/catch loop? so if it fails
				// error message can be displayed
				// also for Gurobi get key error, dialog instead of console printout
				log.debug("create an optimize");
				FBA fba = new FBA();
				fba.setFBAModel(model);
				log.debug("about to optimize");
				ArrayList<Double> soln = fba.run();
				log.debug("optimization complete");
				//End optimization
				
				ReactionFactory rFactory = new ReactionFactory("SBML", getOptimizePath());
				rFactory.setFluxes(soln);

				Writer writer = null;
				try {
					StringBuffer outputText = new StringBuffer();
					outputText.append("FBA\n");
					outputText.append(getDatabaseName() + "\n");
					outputText.append(model.getNumMetabolites() + " metabolites, " + model.getNumReactions() + " reactions\n");
					outputText.append("Maximum objective: "	+ fba.getMaxObj() + "\n");
					
					File file = new File(optimizePath + ".log");
					writer = new BufferedWriter(new FileWriter(file));
					writer.write(outputText.toString());

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						if (writer != null) {
							writer.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				loadOutputPane(getOptimizePath() + ".log");
				if (getPopout() != null) {
					getPopout().load(getOptimizePath() + ".log");
				}				
				String fileString = "jdbc:sqlite:" + getOptimizePath() + ".db";
				LocalConfig.getInstance().setLoadedDatabase(getOptimizePath());
				try {
					Class.forName("org.sqlite.JDBC");
					Connection con = DriverManager.getConnection(fileString);			    
					setUpMetabolitesTable(con);
					setUpReactionsTable(con);
					setTitle(GraphicalInterfaceConstants.TITLE + " - " + getOptimizePath());
				} catch (ClassNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (SQLException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				fileList.setSelectedIndex(listModel.size() - 1);
			}
			
		});
		
		menuBar.add(simulateMenu);

		//Edit menu
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic(KeyEvent.VK_E);

		highlightUnusedMetabolitesItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				tabbedPane.setSelectedIndex(1);
				boolean state = highlightUnusedMetabolitesItem.getState();
				if (state == true) {
					highlightUnusedMetabolites = true;
				} else {
					highlightUnusedMetabolites = false;
				}
				String fileString = "jdbc:sqlite:" + LocalConfig.getInstance().getLoadedDatabase() + ".db";
				try {
					Class.forName("org.sqlite.JDBC");
					Connection con = DriverManager.getConnection(fileString);
					setUpMetabolitesTable(con);
					setUpReactionsTable(con);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}   
			}
		});
		editMenu.add(highlightUnusedMetabolitesItem);

		editMenu.add(deleteUnusedItem);
		deleteUnusedItem.setMnemonic(KeyEvent.VK_D);

		deleteUnusedItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				tabbedPane.setSelectedIndex(1);
				Map<String, Object> usedMap = LocalConfig.getInstance().getMetaboliteUsedMap();
				Map<String, Object> idMap = LocalConfig.getInstance().getMetaboliteIdNameMap();
				ArrayList<String> usedList = new ArrayList<String>(usedMap.keySet());
				ArrayList<String> idList = new ArrayList<String>(idMap.keySet());
				ArrayList<Integer> unusedList = new ArrayList<Integer>();
				// removes unused metabolites from idMap and populates list of
				// unused metabolite id's for deletion from table
				for (int i = 0; i < idList.size(); i++) {						
					if (!usedList.contains(idList.get(i))) {
						int id = (Integer) idMap.get(idList.get(i));
						idMap.remove(idList.get(i));
						unusedList.add(id); 
					}
				}
				
				MetabolitesUpdater updater = new MetabolitesUpdater();				
				updater.deleteUnused(unusedList, LocalConfig.getInstance().getLoadedDatabase());
				try {
					String fileString = "jdbc:sqlite:" + LocalConfig.getInstance().getLoadedDatabase() + ".db";
					Class.forName("org.sqlite.JDBC");
					Connection con = DriverManager.getConnection(fileString);					    	
					setUpMetabolitesTable(con);	
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				highlightUnusedMetabolites = false;
				highlightUnusedMetabolitesItem.setState(false);
			}
		});   
		
		editMenu.addSeparator(); 

		// TODO: make these two menu items same format
		editMenu.add(addReacRowItem);
		addReacRowItem.setMnemonic(KeyEvent.VK_R);

		ActionListener addReacRowActionListener = new ActionListener() {
		//addReacRowItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				tabbedPane.setSelectedIndex(0);
				DatabaseCreator creator = new DatabaseCreator();
				creator.addReactionRow(LocalConfig.getInstance().getLoadedDatabase());
				String fileString = "jdbc:sqlite:" + LocalConfig.getInstance().getLoadedDatabase() + ".db";
				try {
					Class.forName("org.sqlite.JDBC");
					Connection con = DriverManager.getConnection(fileString);
					setUpReactionsTable(con);					
					//set focus to id cell in new row in order to set row visible
					int id = reactionsTable.getModel().getRowCount();
					int viewRow = GraphicalInterface.reactionsTable.convertRowIndexToView(id - 1);
					reactionsTable.changeSelection(viewRow, 1, false, false);
					reactionsTable.requestFocus();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};  
		
		addReacRowItem.addActionListener(addReacRowActionListener);
   
		editMenu.add(addMetabRowItem); 
		addMetabRowItem.setMnemonic(KeyEvent.VK_M);

		//ActionListener addMetabRowActionListener = new ActionListener() {
		addMetabRowItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				tabbedPane.setSelectedIndex(1);
				DatabaseCreator creator = new DatabaseCreator();
				creator.addMetaboliteRow(LocalConfig.getInstance().getLoadedDatabase());
				String fileString = "jdbc:sqlite:" + LocalConfig.getInstance().getLoadedDatabase() + ".db";
				try {
					Class.forName("org.sqlite.JDBC");
					Connection con = DriverManager.getConnection(fileString);
					setUpMetabolitesTable(con);
					
					//set focus to id cell in new row in order to set row visible
					int id = metabolitesTable.getModel().getRowCount();
					int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToView(id - 1);
					metabolitesTable.changeSelection(viewRow, 1, false, false);
					metabolitesTable.requestFocus();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		//addMetabRowItem.addActionListener(addMetabRowActionListener);
		//end TODO
		
		editMenu.addSeparator();
		
		editMenu.add(addReacColumnItem);
		addReacColumnItem.setMnemonic(KeyEvent.VK_C);
		
		addReacColumnItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				tabbedPane.setSelectedIndex(0);
				String fileString = "jdbc:sqlite:" + LocalConfig.getInstance().getLoadedDatabase() + ".db";
				try {
					Class.forName("org.sqlite.JDBC");
					Connection con = DriverManager.getConnection(fileString);			    
					ReactionColAddRenameInterface reactionColAddRenameInterface = new ReactionColAddRenameInterface(con);
					setReactionColAddRenameInterface(reactionColAddRenameInterface);
					reactionColAddRenameInterface.setTitle(GraphicalInterfaceConstants.COLUMN_ADD_INTERFACE_TITLE);
					reactionColAddRenameInterface.setIconImages(icons);
					reactionColAddRenameInterface.setSize(350, 160);
					reactionColAddRenameInterface.setResizable(false);
					reactionColAddRenameInterface.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
					reactionColAddRenameInterface.setLocationRelativeTo(null);
					reactionColAddRenameInterface.setVisible(true);					
				} catch (ClassNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (SQLException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
			}
		});

		ActionListener addColOKButtonActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent prodActionEvent) {
				String fileString = "jdbc:sqlite:" + LocalConfig.getInstance().getLoadedDatabase() + ".db";
				// allows table to scroll to make added column visible
				addReacColumn = true;
				try {
					getReactionColAddRenameInterface().addColumnToMeta(LocalConfig.getInstance().getLoadedDatabase());
					getReactionColAddRenameInterface().textField.setText("");
					getReactionColAddRenameInterface().setVisible(false);
					getReactionColAddRenameInterface().dispose();
					Class.forName("org.sqlite.JDBC");
					Connection con = DriverManager.getConnection(fileString);
					setUpReactionsTable(con);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				addReacColumn = false;
			}
		};
		
		reactionColAddRenameInterface.okButton.addActionListener(addColOKButtonActionListener);
		
		editMenu.add(addMetabColumnItem);
		addMetabColumnItem.setMnemonic(KeyEvent.VK_O);
		
		addMetabColumnItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				tabbedPane.setSelectedIndex(1);
				String fileString = "jdbc:sqlite:" + LocalConfig.getInstance().getLoadedDatabase() + ".db";
				try {
					Class.forName("org.sqlite.JDBC");
					Connection con = DriverManager.getConnection(fileString);			    
					MetaboliteColAddRenameInterface metaboliteColAddRenameInterface = new MetaboliteColAddRenameInterface(con);
					setMetaboliteColAddRenameInterface(metaboliteColAddRenameInterface);
					metaboliteColAddRenameInterface.setTitle(GraphicalInterfaceConstants.COLUMN_ADD_INTERFACE_TITLE);
					metaboliteColAddRenameInterface.setIconImages(icons);
					metaboliteColAddRenameInterface.setSize(350, 160);
					metaboliteColAddRenameInterface.setResizable(false);
					metaboliteColAddRenameInterface.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
					metaboliteColAddRenameInterface.setLocationRelativeTo(null);
					metaboliteColAddRenameInterface.setVisible(true);					
				} catch (ClassNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (SQLException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
			}
		});
		
		ActionListener addMetabColOKButtonActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent prodActionEvent) {				
				String fileString = "jdbc:sqlite:" + LocalConfig.getInstance().getLoadedDatabase() + ".db";
				// allows table to scroll to make added column visible
				addMetabColumn = true;
				try {
					getMetaboliteColAddRenameInterface().addColumnToMeta(LocalConfig.getInstance().getLoadedDatabase());
					getMetaboliteColAddRenameInterface().textField.setText("");
					getMetaboliteColAddRenameInterface().setVisible(false);
					getMetaboliteColAddRenameInterface().dispose();
					Class.forName("org.sqlite.JDBC");
					Connection con = DriverManager.getConnection(fileString);
					setUpMetabolitesTable(con);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				addMetabColumn = false;
			}
		};
		
		metaboliteColAddRenameInterface.okButton.addActionListener(addMetabColOKButtonActionListener);
		
		menuBar.add(editMenu);

		//Optimize menu
		JMenu optimizeMenu = new JMenu("Optimize");
		optimizeMenu.setMnemonic(KeyEvent.VK_O);

		JMenuItem gdbbItem = new JMenuItem("GDBB");
		optimizeMenu.add(gdbbItem);

		menuBar.add(optimizeMenu);

		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic(KeyEvent.VK_H);

		JMenuItem viewHelpTopics = new JMenuItem("View Help Topics");
		helpMenu.add(viewHelpTopics);
		viewHelpTopics.setMnemonic(KeyEvent.VK_H);

		viewHelpTopics.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
				if( !desktop.isSupported( java.awt.Desktop.Action.BROWSE ) ) {
					//System.err.println( "Desktop doesn't support the browse action (fatal)" );
					//System.exit( 1 );
					JOptionPane.showMessageDialog(null,                
							"Default Browser Error. Default Browser May Not Be Set On This System.",                
							"Default Browser Error",                                
							JOptionPane.ERROR_MESSAGE); 
				}

				try{ 
					String url = GraphicalInterfaceConstants.HELP_TOPICS_URL;  
					java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));  
				}  
				catch (java.io.IOException e) {  
					JOptionPane.showMessageDialog(null,                
							GraphicalInterfaceConstants.HELP_URL_NOT_FOUND_MESSAGE,                
							GraphicalInterfaceConstants.HELP_URL_NOT_FOUND_TITLE,                                
							JOptionPane.ERROR_MESSAGE);   
				}
			}    	     
		});

		JMenuItem aboutBox = new JMenuItem("About MOST");
		helpMenu.add(aboutBox);
		aboutBox.setMnemonic(KeyEvent.VK_A);

		aboutBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				JOptionPane.showMessageDialog(null,                
						GraphicalInterfaceConstants.ABOUT_BOX_TEXT,                
						GraphicalInterfaceConstants.ABOUT_BOX_TITLE,                                
						JOptionPane.INFORMATION_MESSAGE);
			}    	     
		});

		menuBar.add(helpMenu);

		/**************************************************************************/
		//end menu bar
		/**************************************************************************/

		/**************************************************************************/
		//set up tables
		/**************************************************************************/
		// register actions
		ActionListener reactionsCopyActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				reactionsCopy();
			}
		};
		
		ActionListener reactionsPasteActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				reactionsPaste();
				LocalConfig.getInstance().pastedReaction = false;
			}
		};
		
		ActionListener reactionsClearActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				reactionsClear();
			}
		};
		
		KeyStroke reacCopy = KeyStroke.getKeyStroke(KeyEvent.VK_C,ActionEvent.CTRL_MASK,false);       
		KeyStroke reacPaste = KeyStroke.getKeyStroke(KeyEvent.VK_V,ActionEvent.CTRL_MASK,false); 		
		KeyStroke reacClear = KeyStroke.getKeyStroke(KeyEvent.VK_E,ActionEvent.CTRL_MASK,false); 
		
		setUpReactionsTable(con);
		TableCellListener tcl = new TableCellListener(reactionsTable, reacAction);
		ReactionsPopupListener reactionsPopupListener = new ReactionsPopupListener();
		reactionsTable.addMouseListener(reactionsPopupListener);
		reactionsTable.setRowHeight(20);
		reactionsTable.registerKeyboardAction(reactionsCopyActionListener,reacCopy,JComponent.WHEN_FOCUSED); 
		reactionsTable.registerKeyboardAction(reactionsPasteActionListener,reacPaste,JComponent.WHEN_FOCUSED); 		
		reactionsTable.registerKeyboardAction(reactionsClearActionListener,reacClear,JComponent.WHEN_FOCUSED); 
		
		ActionListener metabolitesCopyActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				metabolitesCopy();
			}
		};
		
		ActionListener metabolitesPasteActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				metabolitesPaste();
			}
		};
		
		ActionListener metabolitesClearActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				metabolitesClear();
			}
		};
		
		KeyStroke metabCopy = KeyStroke.getKeyStroke(KeyEvent.VK_C,ActionEvent.CTRL_MASK,false);       
		KeyStroke metabPaste = KeyStroke.getKeyStroke(KeyEvent.VK_V,ActionEvent.CTRL_MASK,false);
		KeyStroke metabClear = KeyStroke.getKeyStroke(KeyEvent.VK_E,ActionEvent.CTRL_MASK,false);
		
		setUpMetabolitesTable(con);
		TableCellListener mtcl = new TableCellListener(metabolitesTable, metabAction);
		MetabolitesPopupListener metabolitesPopupListener = new MetabolitesPopupListener();
		metabolitesTable.addMouseListener(metabolitesPopupListener);
		metabolitesTable.setRowHeight(20);
		metabolitesTable.registerKeyboardAction(metabolitesCopyActionListener,metabCopy,JComponent.WHEN_FOCUSED); 
		metabolitesTable.registerKeyboardAction(metabolitesPasteActionListener,metabPaste,JComponent.WHEN_FOCUSED); 
		metabolitesTable.registerKeyboardAction(metabolitesClearActionListener,metabClear,JComponent.WHEN_FOCUSED); 

		metabolitesTable.changeSelection(0, 1, false, false);
		metabolitesTable.requestFocus();
		reactionsTable.changeSelection(0, 1, false, false);
		reactionsTable.requestFocus();
		
		/************************************************************************/
		//end set up tables
		/************************************************************************/

		/************************************************************************/
		//set up other components of gui
		/************************************************************************/
		
		formulaBar.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				int key = e.getKeyCode();
				if (key == KeyEvent.VK_ENTER) {   
					if (tabbedPane.getSelectedIndex() == 0 && reactionsTable.getSelectedRow() > -1 && reactionsTable.getSelectedColumn() > -1) {	
						if (formulaBar.getText() != null) {
							LocalConfig.getInstance().reactionsTableChanged = true;
						}						
						int viewRow = GraphicalInterface.reactionsTable.convertRowIndexToModel(reactionsTable.getSelectedRow());
						String oldValue = ((String) reactionsTable.getModel().getValueAt(viewRow, reactionsTable.getSelectedColumn()));    			
						String newValue = formulaBar.getText();
						updateReactionsCellIfValid(oldValue, newValue, viewRow, reactionsTable.getSelectedColumn());					
					} else if (tabbedPane.getSelectedIndex() == 1 && metabolitesTable.getSelectedRow() > -1 && metabolitesTable.getSelectedColumn() > -1) {
						if (formulaBar.getText() != null) {
							LocalConfig.getInstance().metabolitesTableChanged = true;
						}		
						int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToModel(metabolitesTable.getSelectedRow());
						String oldValue = ((String) metabolitesTable.getModel().getValueAt(viewRow, metabolitesTable.getSelectedColumn()));    			
						String newValue = formulaBar.getText();
						updateMetabolitesCellIfValid(oldValue, newValue, viewRow, metabolitesTable.getSelectedColumn());					
					} 
				}
			}
		}
		);
		
		JScrollPane scrollPaneReac = new JScrollPane(reactionsTable);
		tabbedPane.addTab(GraphicalInterfaceConstants.DEFAULT_REACTION_TABLE_TAB_NAME, scrollPaneReac);
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_R);

		JScrollPane scrollPaneMetab = new JScrollPane(metabolitesTable);
		tabbedPane.addTab(GraphicalInterfaceConstants.DEFAULT_METABOLITE_TABLE_TAB_NAME, scrollPaneMetab);
		tabbedPane.setMnemonicAt(1, KeyEvent.VK_B);  	  
		
		tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				int tabIndex = tabbedPane.getSelectedIndex();
				String reactionRow = Integer.toString((reactionsTable.getSelectedRow() + 1));
				String metaboliteRow = Integer.toString((metabolitesTable.getSelectedRow() + 1));
				if (tabIndex == 0 && reactionsTable.getSelectedRow() > 0) {
					if (LocalConfig.getInstance().getInvalidReactions().size() > 0) {
						setLoadErrorMessage("Model contains invalid reactions.");
						statusBar.setText(reactionRow + "                   " + getLoadErrorMessage());
					} else {
						statusBar.setText(reactionRow);
					}
					if (reactionsTable.getSelectedRow() > -1 && reactionsTable.getSelectedColumn() > -1) {
						int viewRow = GraphicalInterface.reactionsTable.convertRowIndexToModel(reactionsTable.getSelectedRow());
						formulaBar.setText((String) reactionsTable.getModel().getValueAt(viewRow, reactionsTable.getSelectedColumn()));    			
					} 
				} else if (tabIndex == 1 && metabolitesTable.getSelectedRow() > 0) {
					statusBar.setText(metaboliteRow);					
					if (metabolitesTable.getSelectedRow() > -1 && metabolitesTable.getSelectedColumn() > -1) {
						int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToModel(metabolitesTable.getSelectedRow());
						formulaBar.setText((String) metabolitesTable.getModel().getValueAt(viewRow, metabolitesTable.getSelectedColumn()));    			
					} else {
						formulaBar.setText("");
					}
				} else {
					if (LocalConfig.getInstance().getInvalidReactions().size() > 0) {
						setLoadErrorMessage("Model contains invalid reactions.");
						statusBar.setText("1" + "                   " + getLoadErrorMessage());
					} else {
						statusBar.setText("1");
					}
					formulaBar.setText("");
				}
			}
		});
		
		JScrollPane outputPane = new JScrollPane(outputTextArea);
		statusBar.setFont(statusBar.getFont().deriveFont(Font.PLAIN));

		/************************************************************************/
		//end of set up other components of gui
		/************************************************************************/		
		
		/************************************************************************/
		//set frame layout - using TableLayout http://www.clearthought.info/sun/products/jfc/tsc/articles/tablelayout/Simple.html
		/************************************************************************/		
		
		double border = 10;
		double size[][] =
		{{border, TableLayout.FILL, 20, 0.20, border},  //Columns
				{border, 0.04, 10, TableLayout.FILL, 10, 0.15, 5, 0.02, border}}; // Rows
		//{border, 0.04, 10, TableLayout.FILL, 10, 0.15, 10, 0.04, border}}; // Rows

		setLayout (new TableLayout(size)); 

		add (formulaBar, "1, 1, 1, 1");
		add (tabbedPane, "1, 3, 1, 1"); // Left
		add (fileListPane, "3, 1, 1, 5"); // Right
		add (outputPane, "1, 5, 1, 1"); // Bottom
		add (statusBar, "1, 7, 3, 1");

		setBackground(Color.lightGray);
	}
	/********************************************************************************/
	//end constructor and layout
	/********************************************************************************/
	
	/*******************************************************************************/
	//begin Model menu methods and actions
	/*******************************************************************************/ 
	
	/*******************************************************************************/
	//load methods and actions
	/*******************************************************************************/ 
	
	class LoadSBMLAction implements ActionListener {
		public void actionPerformed(ActionEvent ae) { 
			SaveChangesPrompt();
			progressBar.progress.setValue(0);
			LocalConfig.getInstance().setProgress(0);	  
			JTextArea output = null;
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Load SBML File");
			
			//TODO: test the possibility of a global FileChooser
			//Problem is with file filters. May not work in Windows systems.
			
			String lastSBML_path = curSettings.get("LastSBML");
			if (lastSBML_path == null) {
				lastSBML_path = ".";
			}
			fileChooser.setCurrentDirectory(new File(lastSBML_path));
			
			//... Open a file dialog.
			int retval = fileChooser.showOpenDialog(output);
			if (retval == JFileChooser.APPROVE_OPTION) {
				loadSetUp();
				//... The user selected a file, get it, use it.
				File file = fileChooser.getSelectedFile();          	
				String rawPathName = file.getAbsolutePath();
				curSettings.add("LastSBML", rawPathName);
				
				String rawFilename = file.getName();				
				if (!rawFilename.endsWith(".xml") && !rawFilename.endsWith(".sbml")) {
					JOptionPane.showMessageDialog(null,                
							"Not a Valid SBML File.",                
							"Invalid SBML File",                                
							JOptionPane.ERROR_MESSAGE);
				} else {
					fileList.setSelectedIndex(-1);
					listModel.clear();
					fileList.setModel(listModel);
					
					String filename;
					if (rawFilename.endsWith(".xml")) {
						filename = rawFilename.substring(0, rawFilename.length() - 4);
					} else {
						filename = rawFilename.substring(0, rawFilename.length() - 5);
					}
					setSBMLFile(file);
					setDatabaseName(filename); 
					LocalConfig.getInstance().setLoadedDatabase(filename);
					LocalConfig.getInstance().setProgress(0);
					progressBar.setVisible(true);

					timer.start();

					task = new Task();
					task.execute();
				}

			}
		}
	}

	class LoadCSVAction implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			SaveChangesPrompt();
			//setSplitCharacter(',');
			setExtension(".csv");			
			csvLoadInterface.setVisible(true);	
		}
	}
	
	//listens for ok button event in CSVLoadInterface
	ActionListener okButtonCSVLoadActionListener = new ActionListener() {
		public void actionPerformed(ActionEvent ae) {	
			csvLoadInterface.setVisible(false);
			csvLoadInterface.dispose();	
			loadSetUp();
			isCSVFile = true;
			loadCSV();
			progressBar.setTitle("Loading...");
			// Timer used by time listener to set up tables  
			// and set progress bar not visible
			timer.start();
		}
	}; 
	
	public void loadCSV() {		
		fileList.setSelectedIndex(-1);
		listModel.clear();
		fileList.setModel(listModel);   
		try {
			String fileString = "jdbc:sqlite:" + getDatabaseName() + ".db";
			Class.forName("org.sqlite.JDBC");
			Connection con = DriverManager.getConnection(fileString);

			LocalConfig.getInstance().setMetabolitesNextRowCorrection(0);

			if (LocalConfig.getInstance().getMetabolitesCSVFile() != null) {
				TextMetabolitesModelReader reader = new TextMetabolitesModelReader();
				ArrayList<String> columnNamesFromFile = reader.columnNamesFromFile(LocalConfig.getInstance().getMetabolitesCSVFile(), 0);
				MetaboliteColumnNameInterface columnNameInterface = new MetaboliteColumnNameInterface(con, columnNamesFromFile);

				columnNameInterface.setModal(true);
				columnNameInterface.setIconImages(icons);

				columnNameInterface.setSize(600, 360);
				columnNameInterface.setResizable(false);
				columnNameInterface.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
				columnNameInterface.setLocationRelativeTo(null);
				columnNameInterface.setVisible(true);
				columnNameInterface.setAlwaysOnTop(true);
			} else {
				LocalConfig.getInstance().setProgress(100);
			}

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	class LoadSQLiteItemAction implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			SaveChangesPrompt();
			loadSetUp();
			JTextArea output = null;
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Load SQLite Database File");
			
			String lastSQL_path = curSettings.get("LastSQL");
			if (lastSQL_path == null) {
				lastSQL_path = ".";
			}
			fileChooser.setCurrentDirectory(new File(lastSQL_path));
			
			//... Open a file dialog.
			int retval = fileChooser.showOpenDialog(output);
			if (retval == JFileChooser.APPROVE_OPTION) {
				//... The user selected a file, get it, use it.
				String rawFilename = fileChooser.getSelectedFile().getName();
				String rawPathName = fileChooser.getSelectedFile().getAbsolutePath();
				curSettings.add("LastSQL", rawPathName);
				if (!rawFilename.endsWith(".db")) {
					JOptionPane.showMessageDialog(null,                
							"Not a Valid Database File.",                
							"Invalid Database File",                                
							JOptionPane.ERROR_MESSAGE);
				} else {
					fileList.setSelectedIndex(-1);
					listModel.clear();
					fileList.setModel(listModel);
					//String filename = rawFilename.substring(0, rawFilename.length() - 3);
					String rawPath = fileChooser.getSelectedFile().getPath();
					String path = rawPath.substring(0, rawPath.length() - 3);
					setDatabaseName(path);
					DatabaseCopier copier = new DatabaseCopier();
					copier.copyDatabase(path, path + GraphicalInterfaceConstants.DB_COPIER_SUFFIX);
					LocalConfig.getInstance().setLoadedDatabase(path);
					setUpTables();
					// check for invalid reactions and add to invalid list
					DatabaseErrorChecker errorChecker = new DatabaseErrorChecker();
					ArrayList<String> invalidReactions = errorChecker.invalidReactions(path);
					LocalConfig.getInstance().setInvalidReactions(invalidReactions);
					if (LocalConfig.getInstance().getInvalidReactions().size() > 0) {
						setLoadErrorMessage("Model contains invalid reactions.");
						// selected row default at row 1 (index 0)
						statusBar.setText("1" + "                   " + getLoadErrorMessage());
					} else {
						statusBar.setText("1");
					}
				}
			}
		}
	} 
	
	/*******************************************************************************/
	//end load methods and actions
	/*******************************************************************************/
		
	/*******************************************************************************/
	//save methods and actions
	/*******************************************************************************/
	
	class SaveSBMLItemAction implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			System.out.println("Hello");
			try {
				JSBMLWriter jWrite = new JSBMLWriter();
				
				jWrite.formConnect(LocalConfig.getInstance());
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	class SaveCSVMetabolitesItemAction implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			saveMetabolitesTextFileChooser();
		}
	}

	public void saveMetabolitesTextFile(String path, String filename) {
		String oldName = getDatabaseName();
		TextMetabolitesWriter writer = new TextMetabolitesWriter();
		writer.write(path, LocalConfig.getInstance().getLoadedDatabase());				    		
		setTitle(GraphicalInterfaceConstants.TITLE + " - " + filename);
		fileList.setSelectedIndex(-1);
		//list holds dateTime stamps from items in old fileList
		ArrayList<String> suffixList = new ArrayList();
		for (int i = 1; i < listModel.size(); i++) {
			//length of dateTime stamp is 14
			String suffix = listModel.get(i).substring(listModel.get(i).length() - 14);
			suffixList.add(suffix);
		}
		listModel.clear();
		listModel.addElement(filename);
		DatabaseCopier copier = new DatabaseCopier();
		//copies files and assigns new names based on name of saved file to
		//refresh fileList with new names
		for (int i = 0; i < suffixList.size(); i++) {
			listModel.addElement(GraphicalInterfaceConstants.OPTIMIZATION_PREFIX + filename + suffixList.get(i));
			copier.copyDatabase(GraphicalInterfaceConstants.OPTIMIZATION_PREFIX + oldName + suffixList.get(i), GraphicalInterfaceConstants.OPTIMIZATION_PREFIX + filename + suffixList.get(i));
			copier.copyLogFile(GraphicalInterfaceConstants.OPTIMIZATION_PREFIX + oldName + suffixList.get(i), GraphicalInterfaceConstants.OPTIMIZATION_PREFIX + filename + suffixList.get(i));
		}

		fileList.setModel(listModel);
		clearOutputPane();
	}

	public void saveMetabolitesTextFileChooser() {
		JTextArea output = null;
		JFileChooser fileChooser = new JFileChooser(new File(getDatabaseName()));
		fileChooser.setDialogTitle("Save CSV Metabolites File");
		
		String lastCSV_path = curSettings.get("LastCSV");
		if (lastCSV_path == null) {
			lastCSV_path = ".";	
		}
		fileChooser.setCurrentDirectory(new File(lastCSV_path));
		
		boolean done = false;
		while (!done) {
			//... Open a file dialog.
			int retval = fileChooser.showSaveDialog(output);
			if (retval == JFileChooser.CANCEL_OPTION) {
				done = true;
			}
			if (retval == JFileChooser.APPROVE_OPTION) {
				//... The user selected a file, get it, use it.
				String rawPathName = fileChooser.getSelectedFile().getAbsolutePath();
				curSettings.add("LastCSV", rawPathName);

				LocalConfig.getInstance().hasMetabolitesFile = true;
				
				//checks if filename endswith .csv else renames file to end with .csv
				String path = fileChooser.getSelectedFile().getPath();
				String filename = fileChooser.getSelectedFile().getName();
				if (!path.endsWith(".csv")) {
					path = path + ".csv";
				}

				File file = new File(path);
				if (path == null) {
					done = true;
				} else {        	    	  
					if (file.exists()) {
						int confirmDialog = JOptionPane.showConfirmDialog(fileChooser, "Replace existing file?");
						if (confirmDialog == JOptionPane.YES_OPTION) {
							done = true;

							saveMetabolitesTextFile(path, filename);

						} else if (confirmDialog == JOptionPane.NO_OPTION) {        		    	  
							done = false;
						} else {
							done = true;
						}       		    	  
					} else {
						done = true;

						saveMetabolitesTextFile(path, filename);
					}
				}			                  	  
			}
		}
	}
	
	class SaveCSVReactionsItemAction implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			// Message box displayed if model contains invalid reactions
			if (LocalConfig.getInstance().getInvalidReactions().size() > 0) {
				Object[] options = {"    Yes    ", "    No    ",};
				int choice = JOptionPane.showOptionDialog(null, 
						GraphicalInterfaceConstants.INVALID_REACTIONS_ERROR_MESSAGE, 
						GraphicalInterfaceConstants.INVALID_REACTIONS_ERROR_TITLE, 
						JOptionPane.YES_NO_OPTION, 
						JOptionPane.QUESTION_MESSAGE, 
						null, options, options[0]);
				if (choice == JOptionPane.YES_OPTION) {
					saveReactionsTextFileChooser(); 
				}
				if (choice == JOptionPane.NO_OPTION) {

				}							
			} else {
				saveReactionsTextFileChooser(); 
			}
		}
	}

	public void saveReactionsTextFile(String path, String filename) {
		String oldName = getDatabaseName();
		TextReactionsWriter writer = new TextReactionsWriter();
		writer.write(path, LocalConfig.getInstance().getLoadedDatabase());				    
		setTitle(GraphicalInterfaceConstants.TITLE + " - " + filename);	
		if (!saveOptFile) {
			fileList.setSelectedIndex(-1);
			//list holds dateTime stamps from items in old fileList
			ArrayList<String> suffixList = new ArrayList();
			for (int i = 1; i < listModel.size(); i++) {
				//length of dateTime stamp is 14
				String suffix = listModel.get(i).substring(listModel.get(i).length() - 14);
				suffixList.add(suffix);
			}
			listModel.clear();
			listModel.addElement(filename);
			DatabaseCopier copier = new DatabaseCopier();
			for (int i = 0; i < suffixList.size(); i++) {
				listModel.addElement(GraphicalInterfaceConstants.OPTIMIZATION_PREFIX + filename + suffixList.get(i));
				copier.copyDatabase(GraphicalInterfaceConstants.OPTIMIZATION_PREFIX + oldName + suffixList.get(i), GraphicalInterfaceConstants.OPTIMIZATION_PREFIX + filename + suffixList.get(i));
				copier.copyLogFile(GraphicalInterfaceConstants.OPTIMIZATION_PREFIX + oldName + suffixList.get(i), GraphicalInterfaceConstants.OPTIMIZATION_PREFIX + filename + suffixList.get(i));
			}

			fileList.setModel(listModel);
			clearOutputPane();
		}	
		saveOptFile = false;
	}

	public void saveReactionsTextFileChooser() {
		JTextArea output = null;
		JFileChooser fileChooser = new JFileChooser(new File(getDatabaseName()));
		fileChooser.setDialogTitle("Save CSV Reactions File");
		
		String lastCSV_path = curSettings.get("LastCSV");
		if (lastCSV_path == null) {

			lastCSV_path = ".";	
		}
		fileChooser.setCurrentDirectory(new File(lastCSV_path));
		
		boolean done = false;
		while (!done) {
			//... Open a file dialog.
			File file = null;
			if (saveOptFile) {
				file = new File(listModel.getElementAt(fileList.getSelectedIndex()));
				fileChooser.setSelectedFile(file);
			}
			int retval = fileChooser.showSaveDialog(output);
			if (retval == JFileChooser.CANCEL_OPTION) {
				done = true;
			}
			if (retval == JFileChooser.APPROVE_OPTION) {            	  
				//... The user selected a file, get it, use it.
				String rawPathName = fileChooser.getSelectedFile().getAbsolutePath();
				curSettings.add("LastCSV", rawPathName);


				LocalConfig.getInstance().hasReactionsFile = true;
				
				String path = "";
				String filename = "";
				
				//checks if filename endswith .csv else renames file to end with .csv
				path = fileChooser.getSelectedFile().getPath();
				filename = fileChooser.getSelectedFile().getName();
												
				if (!path.endsWith(".csv")) {
					path = path + ".csv";
				}
				
				file = new File(path);
				
				if (path == null) {
					done = true;
				} else {        	    	  
					if (file.exists()) {
						int confirmDialog = JOptionPane.showConfirmDialog(fileChooser, "Replace existing file?");
						if (confirmDialog == JOptionPane.YES_OPTION) {
							done = true;

							saveReactionsTextFile(path, filename);

						} else if (confirmDialog == JOptionPane.NO_OPTION) {        		    	  
							done = false;
						} else {
							done = true;
						}       		    	  
					} else {
						done = true;

						saveReactionsTextFile(path, filename);
					}
				}			                  	  
			}
		}
	}

	public void saveSQLiteFile(){
		String oldName = getDatabaseName();
		DatabaseCopier copier = new DatabaseCopier();
		//filename is rawFilename - extension
		String filename = getDBFilename().substring(0, getDBFilename().length() - 3);

		//compare strings - same = 0
		if (getDatabaseName().compareTo(getDBPath()) == 0) {
			//creates a backup copy to prevent overwriting a db file
			copier.copyDatabase(getDatabaseName(), getDBPath() + GraphicalInterfaceConstants.DB_COPIER_SUFFIX);
		} else {  		  
			copier.copyDatabase(getDatabaseName(), getDBPath());
			//when db is created in a blank interface, this code
			//creates a copy of db so if user modifies it in the future, 
			//there is a revert option or backup copy, since db is
			//able to be modified in real time
			if (getDatabaseName() == ConfigConstants.DEFAULT_DATABASE_NAME) {
				copier.copyDatabase(getDatabaseName(), getDBPath() + GraphicalInterfaceConstants.DB_COPIER_SUFFIX);
			}
			setDatabaseName(getDBPath());    	  
			try {
				String fileString = "jdbc:sqlite:" + getDatabaseName() + ".db";
				Class.forName("org.sqlite.JDBC");
				Connection con = DriverManager.getConnection(fileString);	
				setUpReactionsTable(con);			    
				setUpMetabolitesTable(con);
				setTitle(GraphicalInterfaceConstants.TITLE + " - " + filename);	
				fileList.setSelectedIndex(-1);
				//list holds dateTime stamps from items in old fileList
				ArrayList<String> suffixList = new ArrayList();
				for (int i = 1; i < listModel.size(); i++) {
					//length of dateTime stamp is 14
					String suffix = listModel.get(i).substring(listModel.get(i).length() - 14);
					suffixList.add(suffix);
				}
				listModel.clear();
				listModel.addElement(filename);
				for (int i = 0; i < suffixList.size(); i++) {
					listModel.addElement(GraphicalInterfaceConstants.OPTIMIZATION_PREFIX + filename + suffixList.get(i));
					copier.copyDatabase(GraphicalInterfaceConstants.OPTIMIZATION_PREFIX + oldName + suffixList.get(i), GraphicalInterfaceConstants.OPTIMIZATION_PREFIX + filename + suffixList.get(i));
					copier.copyLogFile(GraphicalInterfaceConstants.OPTIMIZATION_PREFIX + oldName + suffixList.get(i), GraphicalInterfaceConstants.OPTIMIZATION_PREFIX + filename + suffixList.get(i));
				}

				fileList.setModel(listModel);
				clearOutputPane();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void saveSQLiteFileChooser() {
		JTextArea output = null;
		JFileChooser fileChooser = new JFileChooser(new File(getDatabaseName()));
		fileChooser.setDialogTitle("Save SQLite Database File");
		
		String lastSQL_path = curSettings.get("LastSQL");
		if (lastSQL_path == null) {
			lastSQL_path = ".";
		}
		fileChooser.setCurrentDirectory(new File(lastSQL_path));
		
		boolean done = false;
		while (!done) {			
			//... Open a file dialog.
			int retval = fileChooser.showSaveDialog(output);
			if (retval == JFileChooser.CANCEL_OPTION) {
				done = true;
			}
			if (retval == JFileChooser.APPROVE_OPTION) {            	  
				//... The user selected a file, get it, use it.
				//TODO: check if any of these are redundant
				String rawPath = fileChooser.getSelectedFile().getPath();
				setDBPath(rawPath);
				String rawFilename = fileChooser.getSelectedFile().getName();
				String rawPathName = fileChooser.getSelectedFile().getAbsolutePath();
				curSettings.add("LastSQL", rawPathName);
				//checks if filename endswith .db else renames file to end with .db
				if (!rawPath.endsWith(".db")) {
					rawPath = rawPath + ".db";
				}
				if (!rawFilename.endsWith(".db")) {
					setDBFilename(rawFilename + ".db");
				} else {
					setDBFilename(rawFilename);
				}

				File file = new File(rawPath);
				if (rawPath == null) {
					done = true;
				} else {        	    	  
					if (file.exists()) {
						int confirmDialog = JOptionPane.showConfirmDialog(fileChooser, "Replace existing file?");
						if (confirmDialog == JOptionPane.YES_OPTION) {
							done = true;

							saveSQLiteFile();

						} else if (confirmDialog == JOptionPane.NO_OPTION) {        		    	  
							done = false;
						} else {
							done = true;
						}       		    	  
					} else {
						done = true;

						saveSQLiteFile();
					}
				}			                  	  
			}
		}
	}

	class SaveSQLiteItemAction implements ActionListener {
		@SuppressWarnings("unused")
		public void actionPerformed(ActionEvent ae) {
			if (LocalConfig.getInstance().getInvalidReactions().size() > 0) {
				Object[] options = {"    Yes    ", "    No    ",};
				int choice = JOptionPane.showOptionDialog(null, 
						GraphicalInterfaceConstants.INVALID_REACTIONS_ERROR_MESSAGE, 
						GraphicalInterfaceConstants.INVALID_REACTIONS_ERROR_TITLE, 
						JOptionPane.YES_NO_OPTION, 
						JOptionPane.QUESTION_MESSAGE, 
						null, options, options[0]);
				if (choice == JOptionPane.YES_OPTION) {
					saveSQLiteFileChooser();
				}
				if (choice == JOptionPane.NO_OPTION) {

				}							
			} else {
				saveSQLiteFileChooser();
			}
		}
	};
	
	/*******************************************************************************/
	// end save methods and actions
	/*******************************************************************************/
	
	class ClearAction implements ActionListener {
		public void actionPerformed(ActionEvent cae) {
			SaveChangesPrompt();
			loadSetUp();			
			try {
				Class.forName("org.sqlite.JDBC");       
				DatabaseCreator databaseCreator = new DatabaseCreator();
				setDatabaseName(ConfigConstants.DEFAULT_DATABASE_NAME);
				LocalConfig.getInstance().setLoadedDatabase(ConfigConstants.DEFAULT_DATABASE_NAME);
				Connection con = DriverManager.getConnection("jdbc:sqlite:" + ConfigConstants.DEFAULT_DATABASE_NAME + ".db");
				databaseCreator.createDatabase(LocalConfig.getInstance().getDatabaseName());
				databaseCreator.addRows(LocalConfig.getInstance().getDatabaseName(), GraphicalInterfaceConstants.BLANK_DB_METABOLITE_ROW_COUNT, GraphicalInterfaceConstants.BLANK_DB_REACTION_ROW_COUNT);
				setUpReactionsTable(con);	
				setUpMetabolitesTable(con);				
				setTitle(GraphicalInterfaceConstants.TITLE + " - " + ConfigConstants.DEFAULT_DATABASE_NAME);
				listModel.clear();
				listModel.addElement(ConfigConstants.DEFAULT_DATABASE_NAME);
				fileList.setModel(listModel);
				fileList.setSelectedIndex(0);
				metabolitesTable.changeSelection(0, 1, false, false);
				metabolitesTable.requestFocus();
				reactionsTable.changeSelection(0, 1, false, false);
				reactionsTable.requestFocus();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	class ExitAction implements ActionListener {
		public void actionPerformed(ActionEvent cae) {
			SaveChangesPrompt();
			// Exit the application
	        System.exit(0);	
		}
	}
	
	public void SaveChangesPrompt() {
		if (LocalConfig.getInstance().metabolitesTableChanged || LocalConfig.getInstance().reactionsTableChanged || LocalConfig.getInstance().getOptimizationFilesList().size() > 0) {
			Object[] options = {"  Yes  ",
					"   No   "};
			//"Cancel"};

			String message = "";
			String suffix = " Save changes?";
			if (LocalConfig.getInstance().getOptimizationFilesList().size() > 0) {
				message = "Optimizations have not been saved. ";
			}
			if (LocalConfig.getInstance().metabolitesTableChanged && LocalConfig.getInstance().reactionsTableChanged) {
				message += "Reactions table and Metabolites table changed." + suffix;
			} else if (LocalConfig.getInstance().reactionsTableChanged) {
				message += "Reactions table changed." + suffix;
			} else if (LocalConfig.getInstance().metabolitesTableChanged) {
				message += "Metabolites table changed." + suffix;
			} else {
				message += suffix;
			}
			
			int choice = JOptionPane.showOptionDialog(null, 
					message, 
					"Save Changes?", 
					JOptionPane.YES_NO_OPTION, 
					//JOptionPane.YES_NO_CANCEL_OPTION, 
					JOptionPane.QUESTION_MESSAGE, 
					null, options, options[0]);
			//options[0] sets "Yes" as default button
			// interpret the user's choice	  
			if (choice == JOptionPane.YES_OPTION)
			{
				if (LocalConfig.getInstance().metabolitesTableChanged) {
					saveMetabolitesTextFileChooser();
				}
				if (LocalConfig.getInstance().reactionsTableChanged) {
					saveReactionsTextFileChooser();
				}
				if (LocalConfig.getInstance().getOptimizationFilesList().size() > 0) {				
					for (int i = 0; i < LocalConfig.getInstance().getOptimizationFilesList().size(); i++) {
						// TODO: determine where and how to display these messages
						System.out.println(LocalConfig.getInstance().getOptimizationFilesList().get(i) + ".db will be saved.");
					}
				}
				LocalConfig.getInstance().getOptimizationFilesList().clear();
				//System.exit(0);
			}
			if (choice == JOptionPane.NO_OPTION)
			{
				//TODO: if "_orig" db exists rename to db w/out "_orig", delete db w/out "_orig"
				// or delete db
				if (LocalConfig.getInstance().getOptimizationFilesList().size() > 0) {
					for (int i = 0; i < LocalConfig.getInstance().getOptimizationFilesList().size(); i++) {
						// TODO: determine where and how to display these messages, and actually delete these files
						System.out.println(LocalConfig.getInstance().getOptimizationFilesList().get(i) + ".db will be deleted.");
					}
				}		
				//TODO: need to delete these database files in list
				LocalConfig.getInstance().getOptimizationFilesList().clear();
				//System.exit(0);
			}
			/*
			if (choice == JOptionPane.CANCEL_OPTION) {
				exit = false;
			}
			*/	  
		}	
	}
	
	/*******************************************************************************/
	//end Model menu methods and actions
	/*******************************************************************************/ 
	
	/*******************************************************************************/
	//begin table methods and actions
	/*******************************************************************************/ 
	
	/*******************************************************************************/
	//table actions
	/*******************************************************************************/ 
		
	//based on code from http://tips4java.wordpress.com/2009/06/07/table-cell-listener/
	Action reacAction = new AbstractAction()
	{
		public void actionPerformed(ActionEvent ae)
		{		  	
			TableCellListener tcl = (TableCellListener)ae.getSource();
			
			if (tcl.getOldValue() != tcl.getNewValue()) {
				LocalConfig.getInstance().reactionsTableChanged = true;
			}
			updateReactionsCellIfValid(tcl.getOldValue(), tcl.getNewValue(), tcl.getRow(), tcl.getColumn());
		}
	};
	
	Action metabAction = new AbstractAction()
	{
		public void actionPerformed(ActionEvent e)
		{   	  
			TableCellListener mtcl = (TableCellListener)e.getSource();
			
			if (mtcl.getOldValue() != mtcl.getNewValue()) {
				LocalConfig.getInstance().metabolitesTableChanged = true;
			}
			updateMetabolitesCellIfValid(mtcl.getOldValue(), mtcl.getNewValue(), mtcl.getRow(), mtcl.getColumn());
		}
	};
	
	// updates reactions table with new value is valid, else reverts to old value
	public void updateReactionsCellIfValid(String oldValue, String newValue, int rowIndex, int colIndex) {
		int id = Integer.parseInt((String) (reactionsTable.getModel().getValueAt(rowIndex, 0)));
		boolean isNumber = true;		
        if (colIndex == GraphicalInterfaceConstants.REACTION_STRING_COLUMN) {  
			if (oldValue != newValue) {
				ReactionsUpdater updater = new ReactionsUpdater();
				//  if reaction is changed unhighlight unused metabolites since
				//  used status may change
				highlightUnusedMetabolites = false;
				highlightUnusedMetabolitesItem.setState(false);
				// if reaction is reversible, no need to check lower bound
				if (newValue.contains("<") || (newValue.contains("=") && !newValue.contains(">"))) {
					reactionsTable.getModel().setValueAt(newValue, rowIndex, GraphicalInterfaceConstants.REACTION_STRING_COLUMN);
					updateReactionsDatabaseRow(rowIndex, Integer.parseInt((String) (reactionsTable.getModel().getValueAt(rowIndex, 0))), "SBML", LocalConfig.getInstance().getLoadedDatabase());					
					updater.updateReactionEquations(id, oldValue, newValue, LocalConfig.getInstance().getLoadedDatabase());
				// check if lower bound is >= 0 if reversible = false
				} else if (newValue.contains("-->") || newValue.contains("->") || newValue.contains("=>")) {
					// if lower bound < 0, display option dialog
					if (Double.valueOf((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.LOWER_BOUND_COLUMN)) < 0)  {
						Object[] options = {"    Yes    ", "    No    ",};
						int choice = JOptionPane.showOptionDialog(null, 
								GraphicalInterfaceConstants.LOWER_BOUND_ERROR_MESSAGE, 
								GraphicalInterfaceConstants.LOWER_BOUND_ERROR_TITLE, 
								JOptionPane.YES_NO_OPTION, 
								JOptionPane.QUESTION_MESSAGE, 
								null, options, options[0]);
						// set lower bound to 0 and set new equation
						if (choice == JOptionPane.YES_OPTION) {
							reactionsTable.getModel().setValueAt("0.0", rowIndex, GraphicalInterfaceConstants.LOWER_BOUND_COLUMN);
							reactionsTable.getModel().setValueAt("false", rowIndex, GraphicalInterfaceConstants.REVERSIBLE_COLUMN);
							reactionsTable.getModel().setValueAt(newValue, rowIndex, GraphicalInterfaceConstants.REACTION_STRING_COLUMN);
							updateReactionsDatabaseRow(rowIndex, Integer.parseInt((String) (reactionsTable.getModel().getValueAt(rowIndex, 0))), "SBML", LocalConfig.getInstance().getLoadedDatabase());	
							updater.updateReactionEquations(id, oldValue, newValue, LocalConfig.getInstance().getLoadedDatabase());
						}
						// set old equation
						if (choice == JOptionPane.NO_OPTION) {
							reactionsTable.getModel().setValueAt(oldValue, rowIndex, GraphicalInterfaceConstants.REACTION_STRING_COLUMN);
							updateReactionsDatabaseRow(rowIndex, Integer.parseInt((String) (reactionsTable.getModel().getValueAt(rowIndex, 0))), "SBML", LocalConfig.getInstance().getLoadedDatabase());					
						}		
					} else {
						// lower bound >= 0, set new equation
						reactionsTable.getModel().setValueAt(newValue, rowIndex, GraphicalInterfaceConstants.REACTION_STRING_COLUMN);
						updateReactionsDatabaseRow(rowIndex, Integer.parseInt((String) (reactionsTable.getModel().getValueAt(rowIndex, 0))), "SBML", LocalConfig.getInstance().getLoadedDatabase());
						updater.updateReactionEquations(id, oldValue, newValue, LocalConfig.getInstance().getLoadedDatabase());
					}					
				} 
				// if "No" button clicked   
				if (LocalConfig.getInstance().noButtonClicked == true) {
					reactionsTable.getModel().setValueAt(updater.reactionEquation, rowIndex, GraphicalInterfaceConstants.REACTION_STRING_COLUMN);
					updateReactionsDatabaseRow(rowIndex, Integer.parseInt((String) (reactionsTable.getModel().getValueAt(rowIndex, 0))), "SBML", LocalConfig.getInstance().getLoadedDatabase());											
				}
				LocalConfig.getInstance().noButtonClicked = false;
				
				String fileString = "jdbc:sqlite:" + LocalConfig.getInstance().getLoadedDatabase() + ".db";
				try {
					Class.forName("org.sqlite.JDBC");
					Connection con = DriverManager.getConnection(fileString);
					setUpMetabolitesTable(con);
					setUpReactionsTable(con);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
				if (LocalConfig.getInstance().getInvalidReactions().contains(oldValue) && LocalConfig.getInstance().addMetaboliteOption == true) {
					LocalConfig.getInstance().getInvalidReactions().remove(oldValue);
					if (LocalConfig.getInstance().getInvalidReactions().size() > 0) {
						setLoadErrorMessage("Model contains invalid reactions.");
						statusBar.setText((rowIndex + 1) + "                   " + getLoadErrorMessage());
					} else {
						statusBar.setText((rowIndex + 1) + "");
					}
				}
			}
		} else if (colIndex == GraphicalInterfaceConstants.KO_COLUMN) {
			if (newValue.toLowerCase().startsWith(GraphicalInterfaceConstants.VALID_TRUE_VALUES[0])) {
				reactionsTable.getModel().setValueAt(GraphicalInterfaceConstants.BOOLEAN_VALUES[1], rowIndex, GraphicalInterfaceConstants.KO_COLUMN);
			} else if (newValue.toLowerCase().startsWith(GraphicalInterfaceConstants.VALID_FALSE_VALUES[0])) {
				reactionsTable.getModel().setValueAt(GraphicalInterfaceConstants.BOOLEAN_VALUES[0], rowIndex, GraphicalInterfaceConstants.KO_COLUMN);
			} else if (newValue != null) {				
				JOptionPane.showMessageDialog(null,                
						GraphicalInterfaceConstants.BOOLEAN_VALUE_ERROR_TITLE,                
						GraphicalInterfaceConstants.BOOLEAN_VALUE_ERROR_MESSAGE,                                
						JOptionPane.ERROR_MESSAGE);
				reactionsTable.getModel().setValueAt(oldValue, rowIndex, GraphicalInterfaceConstants.KO_COLUMN);
			}
			updateReactionsDatabaseRow(rowIndex, Integer.parseInt((String) (reactionsTable.getModel().getValueAt(rowIndex, 0))), "SBML", LocalConfig.getInstance().getLoadedDatabase());					
		} else if (colIndex == GraphicalInterfaceConstants.FLUX_VALUE_COLUMN || 
				colIndex == GraphicalInterfaceConstants.LOWER_BOUND_COLUMN || 
				colIndex == GraphicalInterfaceConstants.UPPER_BOUND_COLUMN || 
				colIndex == GraphicalInterfaceConstants.BIOLOGICAL_OBJECTIVE_COLUMN) {
			try
			{
				Double.parseDouble(newValue); 				
			}
			catch (NumberFormatException nfe) {
				JOptionPane.showMessageDialog(null,                
						GraphicalInterfaceConstants.NUMERIC_VALUE_ERROR_TITLE,                
						GraphicalInterfaceConstants.NUMERIC_VALUE_ERROR_MESSAGE,                               
						JOptionPane.ERROR_MESSAGE);
				isNumber = false;
			} 
			if (!isNumber) {
				reactionsTable.getModel().setValueAt(oldValue, rowIndex, colIndex);
				updateReactionsDatabaseRow(rowIndex, Integer.parseInt((String) (reactionsTable.getModel().getValueAt(rowIndex, 0))), "SBML", LocalConfig.getInstance().getLoadedDatabase());					
			}
			// check if lower bound is >= 0 if reversible = false, and upper bound > lower bound
			if (isNumber) {			
				if (colIndex == GraphicalInterfaceConstants.LOWER_BOUND_COLUMN) { 
					Double lowerBound = Double.valueOf(newValue);
					Double upperBound = Double.valueOf((String) (reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.UPPER_BOUND_COLUMN)));
					if (reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.REVERSIBLE_COLUMN).toString().compareTo("false") == 0 && lowerBound < 0) {
						Object[] options = {"    Yes    ", "    No    ",};
						int choice = JOptionPane.showOptionDialog(null, 
								GraphicalInterfaceConstants.LOWER_BOUND_ERROR_MESSAGE, 
								GraphicalInterfaceConstants.LOWER_BOUND_ERROR_TITLE, 
								JOptionPane.YES_NO_OPTION, 
								JOptionPane.QUESTION_MESSAGE, 
								null, options, options[0]);
						if (choice == JOptionPane.YES_OPTION) {
							reactionsTable.getModel().setValueAt("0.0", rowIndex, GraphicalInterfaceConstants.LOWER_BOUND_COLUMN);
						}
						if (choice == JOptionPane.NO_OPTION) {
							reactionsTable.getModel().setValueAt(oldValue, rowIndex, GraphicalInterfaceConstants.LOWER_BOUND_COLUMN);
						}							
					} else if (lowerBound > upperBound) {
						Object[] options = {"    Yes    ", "    No    ",};
						int choice = JOptionPane.showOptionDialog(null, 
								GraphicalInterfaceConstants.LOWER_BOUND_ERROR_MESSAGE2, 
								GraphicalInterfaceConstants.LOWER_BOUND_ERROR_TITLE, 
								JOptionPane.YES_NO_OPTION, 
								JOptionPane.QUESTION_MESSAGE, 
								null, options, options[0]);
						if (choice == JOptionPane.YES_OPTION) {
							reactionsTable.getModel().setValueAt("0.0", rowIndex, GraphicalInterfaceConstants.LOWER_BOUND_COLUMN);
						}
						if (choice == JOptionPane.NO_OPTION) {
							reactionsTable.getModel().setValueAt(oldValue, rowIndex, GraphicalInterfaceConstants.LOWER_BOUND_COLUMN);
						}							
					} else {
						reactionsTable.getModel().setValueAt(newValue, rowIndex, GraphicalInterfaceConstants.LOWER_BOUND_COLUMN);
					}
				}
				if (colIndex == GraphicalInterfaceConstants.UPPER_BOUND_COLUMN) { 
					
					Double lowerBound = Double.valueOf((String) (reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.LOWER_BOUND_COLUMN)));
					Double upperBound = Double.valueOf(newValue);
					if (upperBound < lowerBound) {
						Object[] options = {"    Yes    ", "    No    ",};
						int choice = JOptionPane.showOptionDialog(null, 
								GraphicalInterfaceConstants.UPPER_BOUND_ERROR_MESSAGE, 
								GraphicalInterfaceConstants.UPPER_BOUND_ERROR_TITLE, 
								JOptionPane.YES_NO_OPTION, 
								JOptionPane.QUESTION_MESSAGE, 
								null, options, options[0]);
						if (choice == JOptionPane.YES_OPTION) {
							reactionsTable.getModel().setValueAt(GraphicalInterfaceConstants.UPPER_BOUND_DEFAULT_STRING, rowIndex, GraphicalInterfaceConstants.UPPER_BOUND_COLUMN);
						}
						if (choice == JOptionPane.NO_OPTION) {
							reactionsTable.getModel().setValueAt(oldValue, rowIndex, GraphicalInterfaceConstants.UPPER_BOUND_COLUMN);
						}							
					} else {
						reactionsTable.getModel().setValueAt(newValue, rowIndex, GraphicalInterfaceConstants.UPPER_BOUND_COLUMN);
					}
				} 
				if (colIndex == GraphicalInterfaceConstants.FLUX_VALUE_COLUMN || 
				colIndex == GraphicalInterfaceConstants.BIOLOGICAL_OBJECTIVE_COLUMN) {
					reactionsTable.getModel().setValueAt(newValue, rowIndex, colIndex);
				}				
				updateReactionsDatabaseRow(rowIndex, Integer.parseInt((String) (reactionsTable.getModel().getValueAt(rowIndex, 0))), "SBML", LocalConfig.getInstance().getLoadedDatabase());					
			}			
		} else {
			// action for remaining columns
			reactionsTable.getModel().setValueAt(newValue, rowIndex, colIndex);
			updateReactionsDatabaseRow(rowIndex, Integer.parseInt((String) (reactionsTable.getModel().getValueAt(rowIndex, 0))), "SBML", LocalConfig.getInstance().getLoadedDatabase());					
		}
	}
	
	// updates metabolites table with new value is valid, else reverts to old value
	public void updateMetabolitesCellIfValid(String oldValue, String newValue, int rowIndex, int colIndex) {
		int id = Integer.parseInt((String) (metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.DB_METABOLITE_ID_COLUMN)));
		boolean isNumber = true;
		if (colIndex == GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN) { 
			// entry is duplicate
			if (LocalConfig.getInstance().getMetaboliteIdNameMap().containsKey(newValue)) {			
				JOptionPane.showMessageDialog(null,                
						"Duplicate Metabolite.",                
						"Duplicate Metabolite",                                
						JOptionPane.ERROR_MESSAGE);
				metabolitesTable.getModel().setValueAt(oldValue, rowIndex, GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN);
			// duplicate entry changed
			} else if (LocalConfig.getInstance().getDuplicateIds().contains(id)) {
				if (oldValue != newValue) {
					metabolitesTable.getModel().setValueAt(newValue, rowIndex, GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN);
					int index = LocalConfig.getInstance().getDuplicateIds().indexOf(id);
					LocalConfig.getInstance().getDuplicateIds().remove(index);
				}
				metabolitesTable.getModel().setValueAt(newValue, rowIndex, GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN);
			} else {
				//if a blank is entered remove key/value of old value
				if (newValue == null || newValue.length() == 0 || newValue.trim().equals("")) {
					LocalConfig.getInstance().getMetaboliteIdNameMap().remove(oldValue);
				// non-duplicate entry
				} else {
					LocalConfig.getInstance().getMetaboliteIdNameMap().remove(oldValue);
					LocalConfig.getInstance().getMetaboliteIdNameMap().put(newValue, Integer.parseInt((String) (metabolitesTable.getModel().getValueAt(rowIndex, 0))));
				}
				metabolitesTable.getModel().setValueAt(newValue, rowIndex, GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN);
			}
		// if not a number error message displayed
		} else if (colIndex == GraphicalInterfaceConstants.CHARGE_COLUMN) {
			try
			{
				Double.parseDouble(newValue); 				
			}
			catch (NumberFormatException nfe) {
				JOptionPane.showMessageDialog(null,                
						GraphicalInterfaceConstants.NUMERIC_VALUE_ERROR_TITLE,                
						GraphicalInterfaceConstants.NUMERIC_VALUE_ERROR_MESSAGE,                               
						JOptionPane.ERROR_MESSAGE);
				isNumber = false;
				metabolitesTable.getModel().setValueAt(oldValue, rowIndex, colIndex);
			}
			if (!isNumber) {
				metabolitesTable.getModel().setValueAt(oldValue, rowIndex, colIndex);
			} else {
				metabolitesTable.getModel().setValueAt(newValue, rowIndex, colIndex);
			}			
		// any entry not starting with "t" or "f" (any case) will display error
		// if starts with  "t" or "f" will autofill "true" or "false"
		// default values can be changed
		} else if (colIndex == GraphicalInterfaceConstants.BOUNDARY_COLUMN) {
			if (newValue.toLowerCase().startsWith(GraphicalInterfaceConstants.VALID_TRUE_VALUES[0])) {
				metabolitesTable.getModel().setValueAt(GraphicalInterfaceConstants.BOOLEAN_VALUES[1], rowIndex, GraphicalInterfaceConstants.BOUNDARY_COLUMN);
			} else if (newValue.toLowerCase().startsWith(GraphicalInterfaceConstants.VALID_FALSE_VALUES[0])) {
				metabolitesTable.getModel().setValueAt(GraphicalInterfaceConstants.BOOLEAN_VALUES[0], rowIndex, GraphicalInterfaceConstants.BOUNDARY_COLUMN);
			} else if (newValue != null) {				
				JOptionPane.showMessageDialog(null,                
						GraphicalInterfaceConstants.BOOLEAN_VALUE_ERROR_TITLE,                
						GraphicalInterfaceConstants.BOOLEAN_VALUE_ERROR_MESSAGE,                                
						JOptionPane.ERROR_MESSAGE);
				metabolitesTable.getModel().setValueAt(oldValue, rowIndex, GraphicalInterfaceConstants.BOUNDARY_COLUMN);
			}
		} else {
			// action for remaining columns
			metabolitesTable.getModel().setValueAt(newValue, rowIndex, colIndex);
		}
		updateMetabolitesDatabaseRow(rowIndex, Integer.parseInt((String) (metabolitesTable.getModel().getValueAt(rowIndex, 0))), "SBML", LocalConfig.getInstance().getLoadedDatabase()); 	
	}	

	/*****************************************************************************/
	//end table actions
	/*****************************************************************************/

	/******************************************************************************/
	//reload tables methods
	/******************************************************************************/
	
	public void setUpMetabolitesTable(Connection con) {

		try {
			MetabolitesDatabaseTableModel metabModel = new MetabolitesDatabaseTableModel(con, new String("select * from metabolites;"));
			metabolitesTable.setModel(metabModel);
			setMetabolitesTableLayout();
			
			if (getMetabolitesSortColumnIndex() >= 0) {
				metabolitesTable.setSortOrder(getMetabolitesSortColumnIndex(), getMetabolitesSortOrder());
			} else {
				setMetabolitesSortColumnIndex(0);
				setMetabolitesSortOrder(SortOrder.ASCENDING);
			}
			
			if (LocalConfig.getInstance().getInvalidReactions().size() > 0) {
				setLoadErrorMessage("Model contains invalid reactions.");
				// selected row default at row 1 (index 0)
				statusBar.setText("1" + "                   " + getLoadErrorMessage());
			} else {
				statusBar.setText("1");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// TODO create a more elegant fix to this if possible.
			// If csv reactions files are loaded repeatedly with no metabolites file, 
			// sometimes after the third load or maybe more loads, the load will not 
			// create a metabolites db table. There does not seem to be a pattern to
			// this error.
			
			// Error message below did not work out too well.
			/*
			JOptionPane.showMessageDialog(null,                
					"Database Error",                
					"Database Error. Please try restarting MOST and reloading file.",                                
					JOptionPane.ERROR_MESSAGE);
					*/
		}	   
	}

	/*
	 * TODO: make sure not used before deleting
	public void setRowFilter(RowFilter<ReactionsDatabaseTableModel, Integer> filter) {
        if (reactionsTable.getRowSorter() instanceof DefaultRowSorter<?, ?>) {
            DefaultRowSorter sorter = (DefaultRowSorter) reactionsTable.getRowSorter();
            sorter.setRowFilter(filter);
        }
    }
    */
	
	public void setUpReactionsTable(Connection con) {

		try {
			ReactionsDatabaseTableModel reacModel = new ReactionsDatabaseTableModel(con, new String("select * from reactions"));
			reactionsTable.setModel(reacModel);
			setReactionsTableLayout();
			
			if (getReactionsSortColumnIndex() >= 0) {
				reactionsTable.setSortOrder(getReactionsSortColumnIndex(), getReactionsSortOrder());
			} else {
				setReactionsSortColumnIndex(0);
				setReactionsSortOrder(SortOrder.ASCENDING);
			}
			
			if (LocalConfig.getInstance().getInvalidReactions().size() > 0) {
				setLoadErrorMessage("Model contains invalid reactions.");
				// selected row default at row 1 (index 0)
				statusBar.setText("1" + "                   " + getLoadErrorMessage());
			} else {
				statusBar.setText("1");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	   
	}

	//method used only when loading
	public void setUpTables() {
		String titleName = "";
		if (getDatabaseName().contains("\\")) {
			titleName = getDatabaseName().substring(getDatabaseName().lastIndexOf("\\") + 1, getDatabaseName().length());
		} else {
			titleName = getDatabaseName();
		}
		try {
			String fileString = "jdbc:sqlite:" + getDatabaseName() + ".db";
			Class.forName("org.sqlite.JDBC");
			Connection con = DriverManager.getConnection(fileString);			    
			setUpMetabolitesTable(con);	
			setUpReactionsTable(con);
			setTitle(GraphicalInterfaceConstants.TITLE + " - " + titleName);			
			listModel.addElement(titleName);
			fileList.setModel(listModel);
			fileList.setSelectedIndex(0);
			setReactionsSortColumnIndex(0);
			setMetabolitesSortColumnIndex(0);
			setReactionsSortOrder(SortOrder.ASCENDING);
			setMetabolitesSortOrder(SortOrder.ASCENDING);
			//set focus to top left
			metabolitesTable.changeSelection(0, 1, false, false);
			metabolitesTable.requestFocus();
			reactionsTable.changeSelection(0, 1, false, false);
			reactionsTable.requestFocus();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (LocalConfig.getInstance().getInvalidReactions().size() > 0) {
			setLoadErrorMessage("Model contains invalid reactions.");
			// selected row default at row 1 (index 0)
			statusBar.setText("1" + "                   " + getLoadErrorMessage());
		} else {
			statusBar.setText("1");
		}		
		formulaBar.setText("");
	}

	//sets parameters to initial values on load
	public void loadSetUp() {
		clearOutputPane();
		if (getPopout() != null) {
			popout.dispose();
		}
		showPrompt = true;
		highlightUnusedMetabolites = false;
		highlightUnusedMetabolitesItem.setState(false);
		highlightParticipatingRxns = false;
		selectAllRxn = true;	
		includeRxnColumnNames = true;
		selectAllMtb = true;	
		includeMtbColumnNames = true;
		rxnColSelectionMode = false;
		mtbColSelectionMode = false;
		isCSVFile = false;
		setReactionsSortColumnIndex(0);
		setMetabolitesSortColumnIndex(0);
		LocalConfig.getInstance().getInvalidReactions().clear();
		LocalConfig.getInstance().getDuplicateIds().clear();
		LocalConfig.getInstance().getMetaboliteIdNameMap().clear();
		LocalConfig.getInstance().getOptimizationFilesList().clear();
		LocalConfig.getInstance().pastedReaction = false;
		LocalConfig.getInstance().noButtonClicked = false;
		LocalConfig.getInstance().reactionsTableChanged = false;
		LocalConfig.getInstance().metabolitesTableChanged = false;
		showErrorMessage = true;
		saveOptFile = false;
		addReacColumn = false;
		addMetabColumn = false;
		duplicatePromptShown = false;
		reactionsTableEditable = true;
		renameMetabolite = false;
	}

	/******************************************************************************/
	//end reload tables methods
	/******************************************************************************/
	
	/******************************************************************************/
	//update database methods, called when table rows changed 
	/******************************************************************************/

	public void updateReactionsDatabaseRow(int rowIndex, Integer reactionId, String sourceType, String databaseName)  {

		try { 
			ReactionFactory aFactory = new ReactionFactory("SBML", LocalConfig.getInstance().getLoadedDatabase());

			SBMLReaction aReaction = (SBMLReaction)aFactory.getReactionById(reactionId); 
			aReaction.setKnockout((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.KO_COLUMN));			
			aReaction.setFluxValue(Double.valueOf((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.FLUX_VALUE_COLUMN)));
			aReaction.setReactionAbbreviation((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.REACTION_ABBREVIATION_COLUMN));
			aReaction.setReactionName((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.REACTION_NAME_COLUMN));
			aReaction.setReactionString((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.REACTION_STRING_COLUMN));		    

			if (aReaction.getReactionString() != null) {
				if (aReaction.getReactionString().contains("<") || (aReaction.getReactionString().contains("=") && !aReaction.getReactionString().contains(">"))) {
					aReaction.setReversible("true");
				} else if (aReaction.getReactionString().contains("-->") || aReaction.getReactionString().contains("->") || aReaction.getReactionString().contains("=>")) {
					aReaction.setReversible("false");		    		
				}				
			} 

			//string cannot be cast to double but valueOf works, from http://www.java-examples.com/convert-java-string-double-example		    
			aReaction.setLowerBound(Double.valueOf((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.LOWER_BOUND_COLUMN)));
			aReaction.setUpperBound(Double.valueOf((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.UPPER_BOUND_COLUMN)));
			aReaction.setBiologicalObjective(Double.valueOf((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.BIOLOGICAL_OBJECTIVE_COLUMN)));
			
			aReaction.setMeta1((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.REACTION_META1_COLUMN));			
			aReaction.setMeta2((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.REACTION_META2_COLUMN));
			aReaction.setMeta3((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.REACTION_META3_COLUMN));			
			aReaction.setMeta4((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.REACTION_META4_COLUMN));			
			aReaction.setMeta5((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.REACTION_META5_COLUMN));			
			aReaction.setMeta6((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.REACTION_META6_COLUMN));			
			aReaction.setMeta7((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.REACTION_META7_COLUMN));			
			aReaction.setMeta8((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.REACTION_META8_COLUMN));			
			aReaction.setMeta9((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.REACTION_META9_COLUMN));			
			aReaction.setMeta10((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.REACTION_META10_COLUMN));			
			aReaction.setMeta11((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.REACTION_META11_COLUMN));			
			aReaction.setMeta12((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.REACTION_META12_COLUMN));			
			aReaction.setMeta13((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.REACTION_META13_COLUMN));			
			aReaction.setMeta14((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.REACTION_META14_COLUMN));			
			aReaction.setMeta15((String) reactionsTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.REACTION_META15_COLUMN));			

			aReaction.update();
			int viewRow = GraphicalInterface.reactionsTable.convertRowIndexToModel(reactionsTable.getSelectedRow());
			formulaBar.setText((String) reactionsTable.getModel().getValueAt(viewRow, reactionsTable.getSelectedColumn()));
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();			
		}
	}

	public void updateMetabolitesDatabaseRow(int rowIndex, Integer metaboliteId, String sourceType, String databaseName)  {
		try {
			MetaboliteFactory aFactory = new MetaboliteFactory("SBML", LocalConfig.getInstance().getLoadedDatabase());
			SBMLMetabolite aMetabolite = (SBMLMetabolite)aFactory.getMetaboliteById(Integer.parseInt((String) metabolitesTable.getModel().getValueAt(rowIndex, 0))); 
			aMetabolite.setMetaboliteAbbreviation((String) metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN));
			aMetabolite.setMetaboliteName((String) metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.METABOLITE_NAME_COLUMN));
			aMetabolite.setCharge((String) metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.CHARGE_COLUMN));
			aMetabolite.setCompartment((String) metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.COMPARTMENT_COLUMN));
			aMetabolite.setBoundary((String) metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.BOUNDARY_COLUMN));
			aMetabolite.setMeta1((String) metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.METABOLITE_META1_COLUMN));
			aMetabolite.setMeta2((String) metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.METABOLITE_META2_COLUMN));
			aMetabolite.setMeta3((String) metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.METABOLITE_META3_COLUMN));
			aMetabolite.setMeta4((String) metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.METABOLITE_META4_COLUMN));
			aMetabolite.setMeta5((String) metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.METABOLITE_META5_COLUMN));
			aMetabolite.setMeta6((String) metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.METABOLITE_META6_COLUMN));
			aMetabolite.setMeta7((String) metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.METABOLITE_META7_COLUMN));
			aMetabolite.setMeta8((String) metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.METABOLITE_META8_COLUMN));
			aMetabolite.setMeta9((String) metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.METABOLITE_META9_COLUMN));
			aMetabolite.setMeta10((String) metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.METABOLITE_META10_COLUMN));
			aMetabolite.setMeta11((String) metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.METABOLITE_META11_COLUMN));
			aMetabolite.setMeta12((String) metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.METABOLITE_META12_COLUMN));
			aMetabolite.setMeta13((String) metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.METABOLITE_META13_COLUMN));
			aMetabolite.setMeta14((String) metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.METABOLITE_META14_COLUMN));
			aMetabolite.setMeta15((String) metabolitesTable.getModel().getValueAt(rowIndex, GraphicalInterfaceConstants.METABOLITE_META15_COLUMN));

			aMetabolite.update();
			int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToModel(metabolitesTable.getSelectedRow());
			formulaBar.setText((String) metabolitesTable.getModel().getValueAt(viewRow, metabolitesTable.getSelectedColumn()));

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();			
		}    
	}

	/******************************************************************************/
	//end update database methods
	/******************************************************************************/
		
	/*******************************************************************************/
	//table layouts
	/*******************************************************************************/
	
	public void positionColumn(JXTable table,int col_Index) {
		table.moveColumn(table.getColumnCount()-1, col_Index);
	}
	
	/*
	 * TODO: make sure not needed before removing
	public void selectCell(int row,int col, JXTable table)
	{
		if(row!=-1 && col !=-1)            
		{
			table.setRowSelectionInterval(row,row);
			table.setColumnSelectionInterval(col,col);
		}
	}
	*/

	//from http://www.roseindia.net/java/example/java/swing/ChangeColumnName.shtml  
	public void ChangeName(JXTable table, int col_index, String col_name){
		table.getColumnModel().getColumn(col_index).setHeaderValue(col_name);
	}
	
	//used in numerical columns so they are sorted by value and not as strings
	Comparator numberComparator = new Comparator() {
        public int compare(Object o1, Object o2) {
            Double d1 = Double.valueOf(o1 == null ? "0" : (String)o1);
            Double d2 = Double.valueOf(o2 == null ? "0" : (String)o2);
            return d1.compareTo(d2);
        }
    };
	
    private class ReactionsRowListener implements ListSelectionListener {
    	public void valueChanged(ListSelectionEvent event) {
    		String reactionRow = Integer.toString((reactionsTable.getSelectedRow() + 1));
    		if (LocalConfig.getInstance().getInvalidReactions().size() > 0) {
				setLoadErrorMessage("Model contains invalid reactions.");
				// selected row default at row 1 (index 0)
				statusBar.setText(reactionRow + "                   " + getLoadErrorMessage());
			} else {
				statusBar.setText(reactionRow);
			}
    		if (reactionsTable.getSelectedRow() > -1) {
    			int viewRow = GraphicalInterface.reactionsTable.convertRowIndexToModel(reactionsTable.getSelectedRow());
				formulaBar.setText((String) reactionsTable.getModel().getValueAt(viewRow, reactionsTable.getSelectedColumn()));    			
    		}
    		if (event.getValueIsAdjusting()) {
    			return;
    		}
    	}
    }

	private class ReactionsColumnListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent event) {
			if (reactionsTable.getSelectedRow() > -1 && reactionsTable.getSelectedColumn() > -1) {
				int viewRow = GraphicalInterface.reactionsTable.convertRowIndexToModel(reactionsTable.getSelectedRow());
				formulaBar.setText((String) reactionsTable.getModel().getValueAt(viewRow, reactionsTable.getSelectedColumn()));    			
			} 		
			if (event.getValueIsAdjusting()) {
				return;
			}
		}
	}
	
	// saves sort column and order so it is preserved when table is refreshed
	// after editing and updating database
	class ReactionsColumnHeaderListener extends MouseAdapter {
		  public void mouseClicked(MouseEvent evt) {
			  int r = reactionsTable.getSortedColumnIndex();
			  setReactionsSortColumnIndex(r);
			  setReactionsSortOrder(reactionsTable.getSortOrder(reactionsTable.getSortedColumnIndex()));
		  }
	}	  
	
	HighlightPredicate participatingPredicate = new HighlightPredicate() {
		public boolean isHighlighted(Component renderer ,ComponentAdapter adapter) {
			int viewRow = GraphicalInterface.reactionsTable.convertRowIndexToModel(adapter.row);
			int id = Integer.valueOf(reactionsTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.DB_REACTIONS_ID_COLUMN).toString());					
			if (highlightParticipatingRxns == true && LocalConfig.getInstance().getParticipatingReactions().contains(id)) {									
				return true;
			}						
			return false;
		}
	};

	ColorHighlighter participating = new ColorHighlighter(participatingPredicate, Color.GREEN, null);
	
	HighlightPredicate invalidReactionPredicate = new HighlightPredicate() {
		public boolean isHighlighted(Component renderer ,ComponentAdapter adapter) {
			if (adapter.getValue() != null && LocalConfig.getInstance().getInvalidReactions().contains(adapter.getValue().toString())) {
				return true;
			}					
			return false;
		}
	};
	
	ColorHighlighter invalidReaction = new ColorHighlighter(invalidReactionPredicate, Color.RED, null);
	
	public void setReactionsTableLayout() {
		reactionsTable.getSelectionModel().addListSelectionListener(new ReactionsRowListener());
		reactionsTable.getColumnModel().getSelectionModel().
		addListSelectionListener(new ReactionsColumnListener());

		reactionsTable.setAutoResizeMode(JXTable.AUTO_RESIZE_OFF);
		reactionsTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		//allows individual cells to be selected as default
		reactionsTable.setColumnSelectionAllowed(true);
		reactionsTable.setRowSelectionAllowed(true); 
		reactionsTable.setCellSelectionEnabled(true);
		
		// Comparator allows numerical columns to be sorted by numeric value and
		// not like strings
		reactionsTable.getColumnExt("id").setComparator(numberComparator);
		reactionsTable.getColumnExt("flux_value").setComparator(numberComparator);
		reactionsTable.getColumnExt("lower_bound").setComparator(numberComparator);
		reactionsTable.getColumnExt("upper_bound").setComparator(numberComparator);
		reactionsTable.getColumnExt("biological_objective").setComparator(numberComparator);
				
		reactionsTable.addHighlighter(participating);
		reactionsTable.addHighlighter(invalidReaction);
		
		// these columns have names that are too long to fit in cell and need tooltips
		// also KO has Knockout as tooltip
		ColumnHeaderToolTips tips = new ColumnHeaderToolTips();
		for (int c = 0; c < reactionsTable.getColumnCount(); c++) {
			TableColumn col = reactionsTable.getColumnModel().getColumn(c);
			if (c == GraphicalInterfaceConstants.BIOLOGICAL_OBJECTIVE_COLUMN) {
				tips.setToolTip(col, GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES[GraphicalInterfaceConstants.BIOLOGICAL_OBJECTIVE_COLUMN]);
			}	
			if (c == GraphicalInterfaceConstants.REVERSIBLE_COLUMN) {
				tips.setToolTip(col, GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES[GraphicalInterfaceConstants.REVERSIBLE_COLUMN]);
			}
			if (c == GraphicalInterfaceConstants.KO_COLUMN) {
				tips.setToolTip(col, GraphicalInterfaceConstants.KNOCKOUT_TOOLTIP);
			}      
		}
		reactionsTable.getTableHeader().addMouseMotionListener(tips);	
		reactionsTable.getTableHeader().addMouseListener(new ReactionsColumnHeaderListener());
		reactionsTable.getTableHeader().addMouseListener(new ReactionsHeaderPopupListener());
			
		//from http://www.java2s.com/Tutorial/Java/0240__Swing/thelastcolumnismovedtothefirstposition.htm
		// columns cannot be rearranged by dragging
		reactionsTable.getTableHeader().setReorderingAllowed(false);  
		
		ReactionsMetaColumnManager reactionsMetaColumnManager = new ReactionsMetaColumnManager();
		int metaColumnCount = reactionsMetaColumnManager.getMetaColumnCount(LocalConfig.getInstance().getDatabaseName());			
		
		int r = reactionsTable.getModel().getColumnCount();		
		for (int i = 0; i < r; i++) {
			//set background of id column to grey
			ColorTableCellRenderer reacGreyRenderer = new ColorTableCellRenderer();
			ReactionsTableCellRenderer reacRenderer = new ReactionsTableCellRenderer();	
			
			TableColumn column = reactionsTable.getColumnModel().getColumn(i);
			if (i==GraphicalInterfaceConstants.DB_REACTIONS_ID_COLUMN) {
				//sets column not visible
				column.setMaxWidth(0);
				column.setMinWidth(0); 
				column.setWidth(0); 
				column.setPreferredWidth(0);
				ChangeName(reactionsTable, GraphicalInterfaceConstants.DB_REACTIONS_ID_COLUMN, 
						GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES[GraphicalInterfaceConstants.DB_REACTIONS_ID_COLUMN]); 
				column.setCellRenderer(reacGreyRenderer);
				//sets color of id column to grey
				reacGreyRenderer.setHorizontalAlignment(JLabel.CENTER);	
			} else {
				column.setCellRenderer(reacRenderer);
			}
			if (i==GraphicalInterfaceConstants.KO_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.KO_WIDTH); 
				ChangeName(reactionsTable, GraphicalInterfaceConstants.KO_COLUMN, 
						GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES[GraphicalInterfaceConstants.KO_COLUMN]); 
			}
			if (i==GraphicalInterfaceConstants.FLUX_VALUE_COLUMN) {
				ChangeName(reactionsTable, GraphicalInterfaceConstants.FLUX_VALUE_COLUMN, 
						GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES[GraphicalInterfaceConstants.FLUX_VALUE_COLUMN]); 
			}
			if (i==GraphicalInterfaceConstants.REACTION_ABBREVIATION_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.REACTION_ABBREVIATION_WIDTH);//2
				ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_ABBREVIATION_COLUMN, 
						GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES[GraphicalInterfaceConstants.REACTION_ABBREVIATION_COLUMN]); 
			}
			if (i==GraphicalInterfaceConstants.REACTION_NAME_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.REACTION_NAME_WIDTH);
				ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_NAME_COLUMN, 
						GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES[GraphicalInterfaceConstants.REACTION_NAME_COLUMN]);     
			}
			if (i==GraphicalInterfaceConstants.REACTION_STRING_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.REACTION_STRING_WIDTH);//3  
				ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_STRING_COLUMN, 
						GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES[GraphicalInterfaceConstants.REACTION_STRING_COLUMN]); 
			}
			if (i==GraphicalInterfaceConstants.REVERSIBLE_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.REVERSIBLE_WIDTH);        //4
				ChangeName(reactionsTable, GraphicalInterfaceConstants.REVERSIBLE_COLUMN, 
						GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES[GraphicalInterfaceConstants.REVERSIBLE_COLUMN]); 
			}

			if (i==GraphicalInterfaceConstants.BIOLOGICAL_OBJECTIVE_COLUMN) {
				ChangeName(reactionsTable, GraphicalInterfaceConstants.BIOLOGICAL_OBJECTIVE_COLUMN, 
						GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES[GraphicalInterfaceConstants.BIOLOGICAL_OBJECTIVE_COLUMN]); 
			}
			if (i==GraphicalInterfaceConstants.LOWER_BOUND_COLUMN) {
				ChangeName(reactionsTable, GraphicalInterfaceConstants.LOWER_BOUND_COLUMN, 
						GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES[GraphicalInterfaceConstants.LOWER_BOUND_COLUMN]);          
			}
			if (i==GraphicalInterfaceConstants.UPPER_BOUND_COLUMN) {
				ChangeName(reactionsTable, GraphicalInterfaceConstants.UPPER_BOUND_COLUMN, 
						GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES[GraphicalInterfaceConstants.UPPER_BOUND_COLUMN]);          
			} 	  
			//set alignment of columns with numerical values to right, and default width
			if (i==GraphicalInterfaceConstants.BIOLOGICAL_OBJECTIVE_COLUMN || i==GraphicalInterfaceConstants.LOWER_BOUND_COLUMN || i==GraphicalInterfaceConstants.UPPER_BOUND_COLUMN || i==GraphicalInterfaceConstants.FLUX_VALUE_COLUMN) {	  
				reacRenderer.setHorizontalAlignment(JLabel.RIGHT); 
				column.setPreferredWidth(GraphicalInterfaceConstants.DEFAULT_WIDTH);
			} 
			
			if (i==GraphicalInterfaceConstants.REACTION_META1_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.REACTION_META_DEFAULT_WIDTH);
				if (i < (metaColumnCount + GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES.length)) {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META1_COLUMN, 
							reactionsMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 1));     
				} else {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META1_COLUMN, "J");	    					 
				}
			}
			if (i==GraphicalInterfaceConstants.REACTION_META2_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.REACTION_META_DEFAULT_WIDTH);
				if (i < (metaColumnCount + GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES.length)) {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META2_COLUMN, 
							reactionsMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 2));     
				} else {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META2_COLUMN, "K");	    					 
				}
			}
			if (i==GraphicalInterfaceConstants.REACTION_META3_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.REACTION_META_DEFAULT_WIDTH);
				if (i < (metaColumnCount + GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES.length)) {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META3_COLUMN, 
							reactionsMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 3));     
				} else {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META3_COLUMN, "L");	    					 
				}
			}
			if (i==GraphicalInterfaceConstants.REACTION_META4_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.REACTION_META_DEFAULT_WIDTH);
				if (i < (metaColumnCount + GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES.length)) {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META4_COLUMN, 
							reactionsMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 4));     
				} else {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META4_COLUMN, "M");	    					 
				}
			}
			if (i==GraphicalInterfaceConstants.REACTION_META5_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.REACTION_META_DEFAULT_WIDTH);
				if (i < (metaColumnCount + GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES.length)) {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META5_COLUMN, 
							reactionsMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 5));     
				} else {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META5_COLUMN, "N");	    					 
				}
			}
			if (i==GraphicalInterfaceConstants.REACTION_META6_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.REACTION_META_DEFAULT_WIDTH);
				if (i < (metaColumnCount + GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES.length)) {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META6_COLUMN, 
							reactionsMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 6));     
				} else {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META6_COLUMN, "O");	    					 
				}
			}
			if (i==GraphicalInterfaceConstants.REACTION_META7_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.REACTION_META_DEFAULT_WIDTH);
				if (i < (metaColumnCount + GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES.length)) {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META7_COLUMN, 
							reactionsMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 7));     
				} else {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META7_COLUMN, "P");
				}
			}
			if (i==GraphicalInterfaceConstants.REACTION_META8_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.REACTION_META_DEFAULT_WIDTH);
				if (i < (metaColumnCount + GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES.length)) {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META8_COLUMN, 
							reactionsMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 8));     
				} else {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META8_COLUMN, "Q");
				}
			}
			if (i==GraphicalInterfaceConstants.REACTION_META9_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.REACTION_META_DEFAULT_WIDTH);
				if (i < (metaColumnCount + GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES.length)) {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META9_COLUMN, 
							reactionsMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 9));     
				} else {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META9_COLUMN, "R");	
				}
			}
			if (i==GraphicalInterfaceConstants.REACTION_META10_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.REACTION_META_DEFAULT_WIDTH);
				if (i < (metaColumnCount + GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES.length)) {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META10_COLUMN, 
							reactionsMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 10));     
				} else {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META10_COLUMN, "S");
				}
			}
			if (i==GraphicalInterfaceConstants.REACTION_META11_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.REACTION_META_DEFAULT_WIDTH);
				if (i < (metaColumnCount + GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES.length)) {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META11_COLUMN, 
							reactionsMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 11));     
				} else {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META11_COLUMN, "T");
				}
			}
			if (i==GraphicalInterfaceConstants.REACTION_META12_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.REACTION_META_DEFAULT_WIDTH);
				if (i < (metaColumnCount + GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES.length)) {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META12_COLUMN, 
							reactionsMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 12));     
				} else {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META12_COLUMN, "U");
				}
			}
			if (i==GraphicalInterfaceConstants.REACTION_META13_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.REACTION_META_DEFAULT_WIDTH);
				if (i < (metaColumnCount + GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES.length)) {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META13_COLUMN, 
							reactionsMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 13));     
				} else {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META13_COLUMN, "V");
				}
			}
			if (i==GraphicalInterfaceConstants.REACTION_META14_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.REACTION_META_DEFAULT_WIDTH);
				if (i < (metaColumnCount + GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES.length)) {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META14_COLUMN, 
							reactionsMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 14));     
				} else {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META14_COLUMN, "W");
				}
			}
			if (i==GraphicalInterfaceConstants.REACTION_META15_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.REACTION_META_DEFAULT_WIDTH);
				if (i < (metaColumnCount + GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES.length)) {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META15_COLUMN, 
							reactionsMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 15));     
				} else {
					ChangeName(reactionsTable, GraphicalInterfaceConstants.REACTION_META15_COLUMN, "X");
				}
			}
			if (i >= metaColumnCount + GraphicalInterfaceConstants.REACTIONS_DB_COLUMN_NAMES.length || deletedReactionColumns.contains(i)) {
				//sets column not visible
				column.setMaxWidth(0);
				column.setMinWidth(0); 
				column.setWidth(0); 
				column.setPreferredWidth(0);
			}
			// only scrolls all the way to the right when column added
			if (addReacColumn) {
				reactionsTable.scrollRectToVisible(reactionsTable.getCellRect(0, metaColumnCount + GraphicalInterfaceConstants.REACTIONS_DB_COLUMN_NAMES.length, false));
			}
		}
	}

	private class MetabolitesRowListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent event) {
			String metaboliteRow = Integer.toString((metabolitesTable.getSelectedRow() + 1));
			statusBar.setText(metaboliteRow);
			if (metabolitesTable.getSelectedRow() > -1) {
				int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToModel(metabolitesTable.getSelectedRow());
				formulaBar.setText((String) metabolitesTable.getModel().getValueAt(viewRow, metabolitesTable.getSelectedColumn()));    			
			}
			if (event.getValueIsAdjusting()) {
				return;
			}
		}
	}

	private class MetabolitesColumnListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent event) {
			if (metabolitesTable.getSelectedRow() > -1 && metabolitesTable.getSelectedColumn() > -1) {
				int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToModel(metabolitesTable.getSelectedRow());
				formulaBar.setText((String) metabolitesTable.getModel().getValueAt(viewRow, metabolitesTable.getSelectedColumn()));    			
				if (event.getValueIsAdjusting()) {
					return;
				}
			}			
		}
	}
	
	class MetabolitesColumnHeaderListener extends MouseAdapter {
		  public void mouseClicked(MouseEvent evt) {
			  int m = metabolitesTable.getSortedColumnIndex();
			  setMetabolitesSortColumnIndex(m);
			  setMetabolitesSortOrder(metabolitesTable.getSortOrder(metabolitesTable.getSortedColumnIndex()));
		  }
	}
	
	HighlightPredicate unusedPredicate = new HighlightPredicate() {
		public boolean isHighlighted(Component renderer ,ComponentAdapter adapter) {
			int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToModel(adapter.row);			
			if (metabolitesTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN) != null) {
				if (highlightUnusedMetabolites == true && !(LocalConfig.getInstance().getMetaboliteUsedMap().containsKey(metabolitesTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN).toString()))) {					
					return true;
				}
			}						
			return false;
		}
	};
	
	ColorHighlighter unused = new ColorHighlighter(unusedPredicate, Color.YELLOW, null);
	
	HighlightPredicate duplicateMetabPredicate = new HighlightPredicate() {
		public boolean isHighlighted(Component renderer ,ComponentAdapter adapter) {
			int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToModel(adapter.row);
			int id = Integer.valueOf(metabolitesTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.DB_METABOLITE_ID_COLUMN).toString());					
			if (LocalConfig.getInstance().getDuplicateIds().contains(id)) {									
				return true;
			}						
			return false;
		}
	};
	
	ColorHighlighter duplicateMetab = new ColorHighlighter(duplicateMetabPredicate, Color.ORANGE, null);
	
	public void setMetabolitesTableLayout() {	 
		metabolitesTable.getSelectionModel().addListSelectionListener(new MetabolitesRowListener());
		metabolitesTable.getColumnModel().getSelectionModel().
		addListSelectionListener(new MetabolitesColumnListener());

		metabolitesTable.setAutoResizeMode(JXTable.AUTO_RESIZE_OFF);
		metabolitesTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		//allows individual cells to be selected as default
		metabolitesTable.setColumnSelectionAllowed(true);
		metabolitesTable.setRowSelectionAllowed(true); 
		metabolitesTable.setCellSelectionEnabled(true);
		
		metabolitesTable.getColumnExt("id").setComparator(numberComparator);
								
		metabolitesTable.addHighlighter(unused);
		metabolitesTable.addHighlighter(duplicateMetab);
		
		ColumnHeaderToolTips tips = new ColumnHeaderToolTips();		
		
		for (int c = 0; c < metabolitesTable.getColumnCount(); c++) {
			TableColumn col = metabolitesTable.getColumnModel().getColumn(c);	
			if (c == GraphicalInterfaceConstants.CHARGE_COLUMN) {
				tips.setToolTip(col, GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES[GraphicalInterfaceConstants.CHARGE_COLUMN]);     
			}
			if (c == GraphicalInterfaceConstants.BOUNDARY_COLUMN) {
				tips.setToolTip(col, GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES[GraphicalInterfaceConstants.BOUNDARY_COLUMN]);     
			}
		}
		metabolitesTable.getTableHeader().addMouseMotionListener(tips);	
		metabolitesTable.getTableHeader().addMouseListener(new MetabolitesColumnHeaderListener());
		metabolitesTable.getTableHeader().addMouseListener(new MetabolitesHeaderPopupListener());
		
		// from http://www.java2s.com/Tutorial/Java/0240__Swing/thelastcolumnismovedtothefirstposition.htm
		// columns cannot be rearranged by dragging 
		metabolitesTable.getTableHeader().setReorderingAllowed(false);  
		
		MetabolitesMetaColumnManager metabolitesMetaColumnManager = new MetabolitesMetaColumnManager();
		int metabMetaColumnCount = metabolitesMetaColumnManager.getMetaColumnCount(LocalConfig.getInstance().getDatabaseName());	
		
		
		
		int m = metabolitesTable.getModel().getColumnCount();
		for (int w = 0; w < m; w++) {
			ColorTableCellRenderer metabGreyRenderer = new ColorTableCellRenderer();
			MetabolitesTableCellRenderer metabRenderer = new MetabolitesTableCellRenderer();			
			TableColumn column = metabolitesTable.getColumnModel().getColumn(w);
			if (w==GraphicalInterfaceConstants.DB_METABOLITE_ID_COLUMN) {
				//sets column not visible
				column.setMaxWidth(0);
				column.setMinWidth(0); 
				column.setWidth(0); 
				column.setPreferredWidth(0);
				ChangeName(metabolitesTable, GraphicalInterfaceConstants.DB_METABOLITE_ID_COLUMN, 
						GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES[GraphicalInterfaceConstants.DB_METABOLITE_ID_COLUMN]);     
				column.setCellRenderer(metabGreyRenderer);
				metabGreyRenderer.setHorizontalAlignment(JLabel.CENTER);
			} else {	    		  
				column.setCellRenderer(metabRenderer); 	    		  
			}
			if (w==GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_WIDTH);
				ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN, 
						GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES[GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN]);     
			}
			if (w==GraphicalInterfaceConstants.METABOLITE_NAME_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.METABOLITE_NAME_WIDTH);
				ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_NAME_COLUMN, 
						GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES[GraphicalInterfaceConstants.METABOLITE_NAME_COLUMN]);     
			} 
			if (w==GraphicalInterfaceConstants.CHARGE_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.CHARGE_WIDTH);
				ChangeName(metabolitesTable, GraphicalInterfaceConstants.CHARGE_COLUMN, 
						GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES[GraphicalInterfaceConstants.CHARGE_COLUMN]);     
				metabRenderer.setHorizontalAlignment(JLabel.RIGHT);
			}
			if (w==GraphicalInterfaceConstants.COMPARTMENT_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.COMPARTMENT_WIDTH);
				ChangeName(metabolitesTable, GraphicalInterfaceConstants.COMPARTMENT_COLUMN, 
						GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES[GraphicalInterfaceConstants.COMPARTMENT_COLUMN]);     
			}	    	  	    	  
			if (w==GraphicalInterfaceConstants.BOUNDARY_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.BOUNDARY_WIDTH);
				ChangeName(metabolitesTable, GraphicalInterfaceConstants.BOUNDARY_COLUMN, 
						GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES[GraphicalInterfaceConstants.BOUNDARY_COLUMN]);     
			} 

			if (w==GraphicalInterfaceConstants.METABOLITE_META1_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.METABOLITE_META_DEFAULT_WIDTH);
				if (w < (metabMetaColumnCount + GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES.length)) {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META1_COLUMN, 
							metabolitesMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 1));     
				} else {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META1_COLUMN, "E");	    					 
				}
			}
			if (w==GraphicalInterfaceConstants.METABOLITE_META2_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.METABOLITE_META_DEFAULT_WIDTH);
				if (w < (metabMetaColumnCount + GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES.length)) {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META2_COLUMN, 
							metabolitesMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 2));     
				} else {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META2_COLUMN, "F");	    					 
				}
			}
			if (w==GraphicalInterfaceConstants.METABOLITE_META3_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.METABOLITE_META_DEFAULT_WIDTH);
				if (w < (metabMetaColumnCount + GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES.length)) {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META3_COLUMN, 
							metabolitesMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 3));     
				} else {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META3_COLUMN, "G");	    					 
				}
			}
			if (w==GraphicalInterfaceConstants.METABOLITE_META4_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.METABOLITE_META_DEFAULT_WIDTH);
				if (w < (metabMetaColumnCount + GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES.length)) {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META4_COLUMN, 
							metabolitesMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 4));     
				} else {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META4_COLUMN, "H");	    					 
				}
			}
			if (w==GraphicalInterfaceConstants.METABOLITE_META5_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.METABOLITE_META_DEFAULT_WIDTH);
				if (w < (metabMetaColumnCount + GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES.length)) {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META5_COLUMN, 
							metabolitesMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 5));     
				} else {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META5_COLUMN, "I");	    					 
				}
			}
			if (w==GraphicalInterfaceConstants.METABOLITE_META6_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.METABOLITE_META_DEFAULT_WIDTH);
				if (w < (metabMetaColumnCount + GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES.length)) {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META6_COLUMN, 
							metabolitesMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 6));     
				} else {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META6_COLUMN, "J");	    					 
				}
			}
			if (w==GraphicalInterfaceConstants.METABOLITE_META7_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.METABOLITE_META_DEFAULT_WIDTH);
				if (w < (metabMetaColumnCount + GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES.length)) {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META7_COLUMN, 
							metabolitesMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 7));     
				} else {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META7_COLUMN, "K");	    					 
				}
			}
			if (w==GraphicalInterfaceConstants.METABOLITE_META8_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.METABOLITE_META_DEFAULT_WIDTH);
				if (w < (metabMetaColumnCount + GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES.length)) {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META8_COLUMN, 
							metabolitesMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 8));     
				} else {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META8_COLUMN, "L");	    					 
				}
			}
			if (w==GraphicalInterfaceConstants.METABOLITE_META9_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.METABOLITE_META_DEFAULT_WIDTH);
				if (w < (metabMetaColumnCount + GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES.length)) {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META9_COLUMN, 
							metabolitesMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 9));     
				} else {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META9_COLUMN, "M");	    					 
				}
			}
			if (w==GraphicalInterfaceConstants.METABOLITE_META10_COLUMN) {				
				column.setPreferredWidth(GraphicalInterfaceConstants.METABOLITE_META_DEFAULT_WIDTH);
				if (w < (metabMetaColumnCount + GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES.length)) {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META10_COLUMN, 
							metabolitesMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 10));     
				} else {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META10_COLUMN, "N");	    					 
				}
			} 
			if (w==GraphicalInterfaceConstants.METABOLITE_META11_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.METABOLITE_META_DEFAULT_WIDTH);
				if (w < (metabMetaColumnCount + GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES.length)) {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META11_COLUMN, 
							metabolitesMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 11));     
				} else {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META11_COLUMN, "O");	    					 
				}
			}
			if (w==GraphicalInterfaceConstants.METABOLITE_META12_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.METABOLITE_META_DEFAULT_WIDTH);
				if (w < (metabMetaColumnCount + GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES.length)) {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META12_COLUMN, 
							metabolitesMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 12));     
				} else {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META12_COLUMN, "P");	    					 
				}
			}
			if (w==GraphicalInterfaceConstants.METABOLITE_META13_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.METABOLITE_META_DEFAULT_WIDTH);
				if (w < (metabMetaColumnCount + GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES.length)) {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META13_COLUMN, 
							metabolitesMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 13));     
				} else {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META13_COLUMN, "Q");	    					 
				}
			}
			if (w==GraphicalInterfaceConstants.METABOLITE_META14_COLUMN) {
				column.setPreferredWidth(GraphicalInterfaceConstants.METABOLITE_META_DEFAULT_WIDTH);
				if (w < (metabMetaColumnCount + GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES.length)) {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META14_COLUMN, 
							metabolitesMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 14));     
				} else {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META14_COLUMN, "R");	    					 
				}
			}
			if (w==GraphicalInterfaceConstants.METABOLITE_META15_COLUMN) {				
				column.setPreferredWidth(GraphicalInterfaceConstants.METABOLITE_META_DEFAULT_WIDTH);
				if (w < (metabMetaColumnCount + GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES.length)) {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META15_COLUMN, 
							metabolitesMetaColumnManager.getColumnName(LocalConfig.getInstance().getDatabaseName(), 15));     
				} else {
					ChangeName(metabolitesTable, GraphicalInterfaceConstants.METABOLITE_META15_COLUMN, "S");	    					 
				}
			} 
			if (w >= metabMetaColumnCount + GraphicalInterfaceConstants.METABOLITES_DB_COLUMN_NAMES.length || deletedMetaboliteColumns.contains(w)) {
				//sets column not visible
				column.setMaxWidth(0);
				column.setMinWidth(0); 
				column.setWidth(0); 
				column.setPreferredWidth(0);
			}
			// only scrolls all the way to the right when column added
			if (addMetabColumn) {
				metabolitesTable.scrollRectToVisible(metabolitesTable.getCellRect(0, metabMetaColumnCount + GraphicalInterfaceConstants.METABOLITES_DB_COLUMN_NAMES.length, false));
			}
		}
	}
	/************************************************************************************/
	//end table layouts
	/************************************************************************************/

	/*******************************************************************************/
	//end table methods and actions
	/*******************************************************************************/ 	
	
	/************************************************************************************/
	//table header context menus
	/************************************************************************************/
	
	public class ReactionsHeaderPopupListener extends MouseAdapter {

		public void maybeShowPopup(MouseEvent e) {
			if (e.isPopupTrigger() && reactionsTable.isEnabled()) {
				Point p = new Point(e.getX(), e.getY());
				int col = reactionsTable.columnAtPoint(p);
				JPopupMenu reactionsHeaderContextMenu = createReactionsHeaderContextMenu(col);
				// ... and show it
				if (reactionsHeaderContextMenu != null
						&& reactionsHeaderContextMenu.getComponentCount() > 0) {
					reactionsHeaderContextMenu.show(reactionsTable, p.x, p.y);
				}
			}
		}

		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}
	}
	
	private JPopupMenu createReactionsHeaderContextMenu(final int columnIndex) {
		JPopupMenu reactionsHeaderContextMenu = new JPopupMenu();
		
		JMenuItem deleteColumnMenu = new JMenuItem("Delete Column");
		//core columns cannot be deleted
		if (columnIndex < 10) {
			deleteColumnMenu.setEnabled(false);
		}
		deleteColumnMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				deletedReactionColumns.add(columnIndex);
				String fileString = "jdbc:sqlite:" + LocalConfig.getInstance().getLoadedDatabase() + ".db";
				try {
					Class.forName("org.sqlite.JDBC");
					Connection con = DriverManager.getConnection(fileString);
					setUpReactionsTable(con);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}   
			}
		});
		reactionsHeaderContextMenu.add(deleteColumnMenu);	
		
		return reactionsHeaderContextMenu;
		
	}
	
	public class MetabolitesHeaderPopupListener extends MouseAdapter {

		public void maybeShowPopup(MouseEvent e) {
			if (e.isPopupTrigger() && metabolitesTable.isEnabled()) {
				Point p = new Point(e.getX(), e.getY());
				int col = metabolitesTable.columnAtPoint(p);
				JPopupMenu metabolitesHeaderContextMenu = createMetabolitesHeaderContextMenu(col);
				// ... and show it
				if (metabolitesHeaderContextMenu != null
						&& metabolitesHeaderContextMenu.getComponentCount() > 0) {
					metabolitesHeaderContextMenu.show(metabolitesTable, p.x, p.y);
				}
			}
		}

		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}
	}
	
	private JPopupMenu createMetabolitesHeaderContextMenu(final int columnIndex) {
		JPopupMenu metabolitesHeaderContextMenu = new JPopupMenu();
		
		JMenuItem deleteColumnMenu = new JMenuItem("Delete Column");
		//core columns cannot be deleted
		if (columnIndex < 6) {
			deleteColumnMenu.setEnabled(false);
		}
		deleteColumnMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				deletedMetaboliteColumns.add(columnIndex);
				String fileString = "jdbc:sqlite:" + LocalConfig.getInstance().getLoadedDatabase() + ".db";
				try {
					Class.forName("org.sqlite.JDBC");
					Connection con = DriverManager.getConnection(fileString);
					setUpMetabolitesTable(con);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}   
			}
		});
		metabolitesHeaderContextMenu.add(deleteColumnMenu);	
		
		return metabolitesHeaderContextMenu;
		
	}
	
	/************************************************************************************/
	//end header context menus
	/************************************************************************************/	
		
	/*******************************************************************************/
	//Reactions Table context menus
	/*******************************************************************************/	
	
	//based on code from http://www.javakb.com/Uwe/Forum.aspx/java-programmer/21291/popupmenu-for-a-cell-in-a-JXTable
	public class ReactionsPopupListener extends MouseAdapter {

		public void maybeShowPopup(MouseEvent e) {
			if (e.isPopupTrigger() && reactionsTable.isEnabled()) {
				Point p = new Point(e.getX(), e.getY());
				int col = reactionsTable.columnAtPoint(p);
				int row = reactionsTable.rowAtPoint(p);
				setCurrentRow(row);

				if (row >= 0 && row < reactionsTable.getRowCount()) {
					cancelCellEditing();            
					// create reaction equation column popup menu
					if (col == GraphicalInterfaceConstants.REACTION_STRING_COLUMN) {
						JPopupMenu reactionsContextMenu = createReactionsContextMenu(row, col);
						if (reactionsContextMenu != null
								&& reactionsContextMenu.getComponentCount() > 0) {
							reactionsContextMenu.show(reactionsTable, p.x, p.y);
						}
					} else {
						// create popup for remaining columns
						JPopupMenu contextMenu = createContextMenu(row, col);
						// ... and show it
						if (contextMenu != null
								&& contextMenu.getComponentCount() > 0) {
							contextMenu.show(reactionsTable, p.x, p.y);
						}
					}
				}
			}
		}

		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}
	}

	private void cancelCellEditing() {
		CellEditor ce = reactionsTable.getCellEditor();
		if (ce != null) {
			ce.cancelCellEditing();
		}
	}

	/*******************************************************************************/
	//begin reaction equation context menu
	/*******************************************************************************/	
		
	private JPopupMenu createReactionsContextMenu(final int rowIndex,
			final int columnIndex) {
		JPopupMenu reactionsContextMenu = new JPopupMenu();	
		
		JMenu selectAllMenu = new JMenu("Select All");
		
		final JRadioButtonMenuItem inclColNamesItem = new JRadioButtonMenuItem(
        "Include Column Names");
		final JRadioButtonMenuItem selectCellsOnly = new JRadioButtonMenuItem(
        "Selected Cells Only");

		ButtonGroup bg = new ButtonGroup();
		bg.add(inclColNamesItem);
		bg.add(selectCellsOnly);
		inclColNamesItem.setSelected(includeRxnColumnNames);
		selectCellsOnly.setSelected(!includeRxnColumnNames);
		
		inclColNamesItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reactionsTable.selectAll();
				if (inclColNamesItem.isSelected()) {
					includeRxnColumnNames = true;
				} else {
					includeRxnColumnNames = false;
				}				
				selectAllRxn = true;
				selectReactionsRows();
			}
		});
		selectCellsOnly.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reactionsTable.selectAll();
				if (selectCellsOnly.isSelected()) {
					includeRxnColumnNames = false;
				} else {
					includeRxnColumnNames = true;
				}				
				selectAllRxn = true;
				selectReactionsRows();
			}
		});
        
        selectAllMenu.add(inclColNamesItem);
        selectAllMenu.add(selectCellsOnly);
		
		reactionsContextMenu.add(selectAllMenu);
		
		reactionsContextMenu.addSeparator();
			
		JMenuItem copyMenu = new JMenuItem("Copy");
		copyMenu.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		copyMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reactionsCopy();
			}
		});
		reactionsContextMenu.add(copyMenu);
		
		JMenuItem selectReacColMenu = new JMenuItem("Copy Column(s)");
		if (rowIndex > 0) {
			selectReacColMenu.setEnabled(false);
		}
		
		selectReacColMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rxnColSelectionMode = true;
				selectReactionsColumns();
			}
		});
		reactionsContextMenu.add(selectReacColMenu);

		reactionsContextMenu.addSeparator();
		
		JMenuItem pasteMenu = new JMenuItem("Paste");
		if (rowIndex > 0 && rxnColSelectionMode == true) {
			pasteMenu.setEnabled(false);
		}
		pasteMenu.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_V, ActionEvent.CTRL_MASK));
		if (isClipboardContainingText(this)
				&& reactionsTable.getModel().isCellEditable(rowIndex, columnIndex)) {
			pasteMenu.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						reactionsPaste();
						LocalConfig.getInstance().pastedReaction = false;
					} catch (Throwable t) {
						/*
						JOptionPane.showMessageDialog(null,                
								"Paste Error",                
								"Paste Error",                                
								JOptionPane.ERROR_MESSAGE);
								*/
						t.printStackTrace();
					}
				}
			});
		} else {
			pasteMenu.setEnabled(false);
		}
		reactionsContextMenu.add(pasteMenu);
		
		JMenuItem fillUpMenu = new JMenuItem("Fill Up");
		//if fill is done in the sorted row it will sort after fill  
		if (getReactionsSortColumnIndex() == reactionsTable.getSelectedColumn()) {
			fillUpMenu.setEnabled(false);
		}
		fillUpMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int row=reactionsTable.getSelectedRow();
				reactionsFill(0, row);
			}
		});
		reactionsContextMenu.add(fillUpMenu);
		
		JMenuItem fillDownMenu = new JMenuItem("Fill Down");
		//if fill is done in the sorted row it will sort after fill  
		if (getReactionsSortColumnIndex() == reactionsTable.getSelectedColumn()) {
			fillDownMenu.setEnabled(false);
		}
		fillDownMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int row=reactionsTable.getSelectedRow();
				reactionsFill(row + 1, reactionsTable.getRowCount());
			}
		});
		reactionsContextMenu.add(fillDownMenu);

		JMenuItem clearMenu = new JMenuItem("Clear Contents");
		clearMenu.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_E, ActionEvent.CTRL_MASK));
		clearMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reactionsClear();
			}
		});
		reactionsContextMenu.add(clearMenu);
		
		reactionsContextMenu.addSeparator();	
		
		JMenuItem deleteRowMenu = new JMenuItem("Delete Row(s)");
		deleteRowMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				reactionsDeleteRows();			
			}
		});
		reactionsContextMenu.add(deleteRowMenu);
		
		reactionsContextMenu.addSeparator();
			
		JMenuItem editorMenu = new JMenuItem("Launch Reaction Editor");
		if (!reactionsTableEditable) {
			editorMenu.setEnabled(false);
		}
		editorMenu.addActionListener(new ActionListener() {
			@SuppressWarnings("deprecation")
			public void actionPerformed(ActionEvent e) {
				//setCurrentRow(rowIndex);
				try {
					Class.forName("org.sqlite.JDBC");
					Connection con = DriverManager.getConnection("jdbc:sqlite:" + LocalConfig.getInstance().getLoadedDatabase() + ".db");
					ReactionEditor reactionEditor = new ReactionEditor(con);
					setReactionEditor(reactionEditor);
					reactionEditor.setIconImages(icons);
					reactionEditor.setSize(800, 520);
					reactionEditor.setResizable(false);
					reactionEditor.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
					reactionEditor.setLocationRelativeTo(null);
					reactionEditor.setVisible(true);
					reactionEditor.okButton.addActionListener(okButtonActionListener);
				} catch (ClassNotFoundException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				} catch (SQLException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
			}
		});
		reactionsContextMenu.add(editorMenu);

		return reactionsContextMenu;
	}

	//listens for ok button event in ReactionEditor
	ActionListener okButtonActionListener = new ActionListener() {
		public void actionPerformed(ActionEvent ae) {
			boolean okToClose = true;
			int viewRow = GraphicalInterface.reactionsTable.convertRowIndexToModel(GraphicalInterface.reactionsTable.getSelectedRow());
		    int id = (Integer.valueOf((String) GraphicalInterface.reactionsTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.DB_REACTIONS_ID_COLUMN)));	
		    reactionEditor.setReactionEquation(reactionEditor.reactionArea.getText());
			if (reactionEditor.getReactionEquation().contains("<") || (reactionEditor.getReactionEquation().contains("=") && !reactionEditor.getReactionEquation().contains(">"))) {
				reactionsTable.getModel().setValueAt(reactionEditor.getReactionEquation(), viewRow, GraphicalInterfaceConstants.REACTION_STRING_COLUMN);
				reactionsTable.getModel().setValueAt("true", viewRow, GraphicalInterfaceConstants.REVERSIBLE_COLUMN);
			} else if (reactionEditor.getReactionEquation().contains("-->") || reactionEditor.getReactionEquation().contains("->") || reactionEditor.getReactionEquation().contains("=>")) {
				if (Double.valueOf((String) reactionsTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.LOWER_BOUND_COLUMN)) < 0)  {
					JOptionPane.showMessageDialog(null,                
							GraphicalInterfaceConstants.IRREVERSIBLE_REACTION_ERROR_MESSAGE,                
							GraphicalInterfaceConstants.IRREVERSIBLE_REACTION_ERROR_TITLE,                               
							JOptionPane.ERROR_MESSAGE);
					okToClose = false;
				} else {
					reactionsTable.getModel().setValueAt(reactionEditor.getReactionEquation(), viewRow, GraphicalInterfaceConstants.REACTION_STRING_COLUMN);
					reactionsTable.getModel().setValueAt("false", viewRow, GraphicalInterfaceConstants.REVERSIBLE_COLUMN);		    		
				}				
			}
			updateReactionsDatabaseRow(viewRow, Integer.parseInt((String) (reactionsTable.getModel().getValueAt(viewRow, 0))), "SBML", LocalConfig.getInstance().getLoadedDatabase());
			
			ReactionsUpdater updater = new ReactionsUpdater();
			updater.updateReactionEquations(id, reactionEditor.getOldReaction(), reactionEditor.getReactionEquation(), LocalConfig.getInstance().getLoadedDatabase());
			
			if (LocalConfig.getInstance().noButtonClicked) {
				reactionsTable.getModel().setValueAt(updater.reactionEquation, viewRow, GraphicalInterfaceConstants.REACTION_STRING_COLUMN);
				updateReactionsDatabaseRow(viewRow, Integer.parseInt((String) (reactionsTable.getModel().getValueAt(viewRow, 0))), "SBML", LocalConfig.getInstance().getLoadedDatabase());
				String fileString = "jdbc:sqlite:" + getDatabaseName() + ".db";
				LocalConfig.getInstance().setLoadedDatabase(getDatabaseName());
				try {
					Class.forName("org.sqlite.JDBC");
					Connection con = DriverManager.getConnection(fileString);			    
					highlightUnusedMetabolites = false;
					highlightUnusedMetabolitesItem.setState(false);
					setUpMetabolitesTable(con);
				} catch (ClassNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (SQLException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
			}
			
			if (okToClose) {
				reactionEditor.setVisible(false);
				reactionEditor.dispose();
			}
			okToClose = true;
		}
	}; 
	
	/*******************************************************************************/
	//end reaction equation context menu
	/*******************************************************************************/	
	
	/*******************************************************************************/
	//begin reaction context menu for other columns
	/*******************************************************************************/	
		
	private JPopupMenu createContextMenu(final int rowIndex,
			final int columnIndex) {
		JPopupMenu contextMenu = new JPopupMenu();	
		
		JMenu selectAllMenu = new JMenu("Select All");
		
		final JRadioButtonMenuItem inclColNamesItem = new JRadioButtonMenuItem(
        "Include Column Names");
		final JRadioButtonMenuItem selectCellsOnly = new JRadioButtonMenuItem(
        "Selected Cells Only");

		ButtonGroup bg = new ButtonGroup();
		bg.add(inclColNamesItem);
		bg.add(selectCellsOnly);
		inclColNamesItem.setSelected(includeRxnColumnNames);
		selectCellsOnly.setSelected(!includeRxnColumnNames);
		
		inclColNamesItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reactionsTable.selectAll();
				if (inclColNamesItem.isSelected()) {
					includeRxnColumnNames = true;
				} else {
					includeRxnColumnNames = false;
				}				
				selectAllRxn = true;
				selectReactionsRows();
			}
		});
		selectCellsOnly.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reactionsTable.selectAll();
				if (selectCellsOnly.isSelected()) {
					includeRxnColumnNames = false;
				} else {
					includeRxnColumnNames = true;
				}				
				selectAllRxn = true;
				selectReactionsRows();
			}
		});
        
        selectAllMenu.add(inclColNamesItem);
        selectAllMenu.add(selectCellsOnly);

        contextMenu.add(selectAllMenu);
		
		contextMenu.addSeparator();
		
		JMenuItem copyMenu = new JMenuItem("Copy");
		copyMenu.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		copyMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reactionsCopy();
			}
		});
		contextMenu.add(copyMenu);
		
		JMenuItem selectRowMenu = new JMenuItem("Copy Row(s)");
		if (columnIndex > 1) {
			selectRowMenu.setEnabled(false);
		}
		selectRowMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				includeRxnColumnNames = false;
				selectReactionsRows();
			}
		});
		contextMenu.add(selectRowMenu);	
		
		JMenuItem selectReacColMenu = new JMenuItem("Copy Column(s)");
		if (rowIndex > 0) {
			selectReacColMenu.setEnabled(false);
		}
		
		selectReacColMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rxnColSelectionMode = true;
				selectReactionsColumns();
			}
		});
		contextMenu.add(selectReacColMenu);	

		contextMenu.addSeparator();
		
		JMenuItem pasteMenu = new JMenuItem("Paste");
		if (rowIndex > 0 && rxnColSelectionMode == true) {
			pasteMenu.setEnabled(false);
		}
		pasteMenu.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_V, ActionEvent.CTRL_MASK));
		if (isClipboardContainingText(this)
				&& reactionsTable.getModel().isCellEditable(rowIndex, columnIndex)) {
			pasteMenu.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						reactionsPaste();
						LocalConfig.getInstance().pastedReaction = false;
					} catch (Throwable t) {
						/*
						JOptionPane.showMessageDialog(null,                
								"Paste Error",                
								"Paste Error",                                
								JOptionPane.ERROR_MESSAGE);
								*/
						t.printStackTrace();
					}					
				}
			});
		} else {
			pasteMenu.setEnabled(false);
		}
		contextMenu.add(pasteMenu);

		JMenuItem fillUpMenu = new JMenuItem("Fill Up");
		//if fill is done in the sorted row it will sort after fill  
		if (getReactionsSortColumnIndex() == reactionsTable.getSelectedColumn()) {
			fillUpMenu.setEnabled(false);
		}
		fillUpMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int row=reactionsTable.getSelectedRow();
				reactionsFill(0, row);
			}
		});
		contextMenu.add(fillUpMenu);
		
		JMenuItem fillDownMenu = new JMenuItem("Fill Down");
		//if fill is done in the sorted row it will sort after fill  
		if (getReactionsSortColumnIndex() == reactionsTable.getSelectedColumn()) {
			fillDownMenu.setEnabled(false);
		}
		fillDownMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int row=reactionsTable.getSelectedRow();
				reactionsFill(row + 1, reactionsTable.getRowCount());
			}
		});
		contextMenu.add(fillDownMenu);
		
		JMenuItem clearMenu = new JMenuItem("Clear Contents");
		clearMenu.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_E, ActionEvent.CTRL_MASK));
		clearMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reactionsClear();
			}
		});
		contextMenu.add(clearMenu);
		
		contextMenu.addSeparator();
		
		JMenuItem deleteRowMenu = new JMenuItem("Delete Row(s)");
		deleteRowMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				reactionsDeleteRows();		
			}
		});
		contextMenu.add(deleteRowMenu);	
		
		JMenuItem deleteColumnMenu = new JMenuItem("Delete Columns(s)");
		//core columns cannot be deleted
		if (columnIndex < 10) {
			deleteColumnMenu.setEnabled(false);
		}
		deleteColumnMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				int numcols=reactionsTable.getSelectedColumnCount(); 
				int startCol=(reactionsTable.getSelectedColumns())[0];
				for (int i = startCol + numcols - 1; i >= startCol; i--) {
					reactionsTable.getColumnExt(i).setVisible(false);
				}			
			}
		});
		contextMenu.add(deleteColumnMenu);

		return contextMenu;
	}  

	/****************************************************************************/
	// end Reactions Table context menus
	/****************************************************************************/

	/****************************************************************************/
	// Metabolites Table context menus
	/****************************************************************************/

	public class MetabolitesPopupListener extends MouseAdapter {

		public void maybeShowPopup(MouseEvent e) {
			if (e.isPopupTrigger() && metabolitesTable.isEnabled()) {
				Point p = new Point(e.getX(), e.getY());
				int col = metabolitesTable.columnAtPoint(p);
				int row = metabolitesTable.rowAtPoint(p);
				setCurrentRow(row);

				if (row >= 0 && row < metabolitesTable.getRowCount()) {
					cancelCellEditing();            
					// create popup menu for abbreviation column
					if (col == GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN) {
						JPopupMenu abbrevContextMenu = createMetaboliteAbbreviationContextMenu(row, col);
						// ... and show it
						if (abbrevContextMenu != null
								&& abbrevContextMenu.getComponentCount() > 0) {
							abbrevContextMenu.show(metabolitesTable, p.x, p.y);
						}
						// create popup menu for remaining columns	
					} else {
						JPopupMenu contextMenu = createMetabolitesContextMenu(row, col);
						// ... and show it
						if (contextMenu != null
								&& contextMenu.getComponentCount() > 0) {
							contextMenu.show(metabolitesTable, p.x, p.y);
						}	            	
					}
				}
			}
		}

		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}
	}

	/****************************************************************************/
	// begin abbreviation column context menu
	/****************************************************************************/

	private JPopupMenu createMetaboliteAbbreviationContextMenu(final int rowIndex,
			final int columnIndex) {
		JPopupMenu contextMenu = new JPopupMenu();				
		//int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToModel(GraphicalInterface.metabolitesTable.getSelectedRow());
		final int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToModel(rowIndex);
		final int id = Integer.valueOf((String) GraphicalInterface.metabolitesTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.DB_METABOLITE_ID_COLUMN));		
		final String metabAbbrev = (String) GraphicalInterface.metabolitesTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN);
		
		JMenu selectAllMenu = new JMenu("Select All");
		
		final JRadioButtonMenuItem inclColNamesItem = new JRadioButtonMenuItem(
        "Include Column Names");
		final JRadioButtonMenuItem selectCellsOnly = new JRadioButtonMenuItem(
        "Selected Cells Only");

		ButtonGroup bg = new ButtonGroup();
		bg.add(inclColNamesItem);
		bg.add(selectCellsOnly);
		inclColNamesItem.setSelected(includeMtbColumnNames);
		selectCellsOnly.setSelected(!includeMtbColumnNames);
		
		inclColNamesItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				metabolitesTable.selectAll();
				if (inclColNamesItem.isSelected()) {
					includeMtbColumnNames = true;
				} else {
					includeMtbColumnNames = false;
				}				
				selectAllMtb = true;
				selectMetabolitesRows();
			}
		});
		selectCellsOnly.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				metabolitesTable.selectAll();
				if (selectCellsOnly.isSelected()) {
					includeMtbColumnNames = false;
				} else {
					includeMtbColumnNames = true;
				}				
				selectAllMtb = true;
				selectMetabolitesRows();
			}
		});
        
        selectAllMenu.add(inclColNamesItem);
        selectAllMenu.add(selectCellsOnly);

        contextMenu.add(selectAllMenu);
		
		contextMenu.addSeparator();
		
		JMenuItem copyMenu = new JMenuItem("Copy");
		copyMenu.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		copyMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				metabolitesCopy();
			}
		});
		contextMenu.add(copyMenu);

		JMenuItem selectRowMenu = new JMenuItem("Copy Row(s)");
		if (columnIndex > 1) {
			selectRowMenu.setEnabled(false);
		}
		selectRowMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				includeMtbColumnNames = false;
				selectMetabolitesRows();
			}
		});
		contextMenu.add(selectRowMenu);	
		
		JMenuItem selectMetabColMenu = new JMenuItem("Copy Column(s)");
		if (rowIndex > 0) {
			selectMetabColMenu.setEnabled(false);
		}
		
		selectMetabColMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mtbColSelectionMode = true;
				selectMetabolitesColumns();
			}
		});
		contextMenu.add(selectMetabColMenu);
		
		contextMenu.addSeparator();
		
		JMenuItem pasteMenu = new JMenuItem("Paste");
		if (rowIndex > 0 && mtbColSelectionMode == true) {
			pasteMenu.setEnabled(false);
		}
		if (LocalConfig.getInstance().getMetaboliteUsedMap().containsKey(metabAbbrev) && !LocalConfig.getInstance().getDuplicateIds().contains(id)) {
			pasteMenu.setEnabled(false);
		}
		pasteMenu.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_V, ActionEvent.CTRL_MASK));
		if (isClipboardContainingText(this)
				&& metabolitesTable.getModel().isCellEditable(rowIndex, columnIndex)) {
			pasteMenu.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						metabolitesPaste();
					} catch (Throwable t) {
						/*
						JOptionPane.showMessageDialog(null,                
								"Paste Error",                
								"Paste Error",                                
								JOptionPane.ERROR_MESSAGE);
								*/
						t.printStackTrace();
					}					
				}
			});
		} else {
			pasteMenu.setEnabled(false);
		}
		contextMenu.add(pasteMenu);	
		
		contextMenu.addSeparator();
		
		JMenuItem renameMenu = new JMenuItem("Rename");
		if (metabAbbrev == null || metabAbbrev.length() == 0) {
			renameMenu.setEnabled(false);
		}
		contextMenu.add(renameMenu);
		
		renameMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				duplicatePromptShown = false;
				MetaboliteRenameInterface metaboliteRenameInterface = new MetaboliteRenameInterface();
				setMetaboliteRenameInterface(metaboliteRenameInterface);
				metaboliteRenameInterface.setTitle(GraphicalInterfaceConstants.RENAME_METABOLITE_INTERFACE_TITLE);
				metaboliteRenameInterface.setIconImages(icons);
				metaboliteRenameInterface.setSize(350, 160);
				metaboliteRenameInterface.setResizable(false);
				metaboliteRenameInterface.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
				metaboliteRenameInterface.setLocationRelativeTo(null);
				if (LocalConfig.getInstance().getMetaboliteUsedMap().containsKey(metabAbbrev) && !LocalConfig.getInstance().getDuplicateIds().contains(id)) {
					Object[] options = {"    Yes    ", "    No    ",};
					int choice = JOptionPane.showOptionDialog(null, 
							GraphicalInterfaceConstants.PARTICIPATING_METAB_RENAME_MESSAGE_PREFIX + 
								metabAbbrev + GraphicalInterfaceConstants.PARTICIPATING_METAB_RENAME_MESSAGE_SUFFIX, 
							GraphicalInterfaceConstants.PARTICIPATING_METAB_RENAME_TITLE, 
							JOptionPane.YES_NO_OPTION, 
							JOptionPane.QUESTION_MESSAGE, 
							null, options, options[0]);
					if (choice == JOptionPane.YES_OPTION) {
						renameMetabolite = true;
						metaboliteRenameInterface.setVisible(true);
					}
					if (choice == JOptionPane.NO_OPTION) {
						
					}
				// not necessary to use the interface to rename an unused or duplicate
				// metabolite but for consistency, when rename item clicked, interface
				// is displayed and functional. another option would be to disable the
				// menu item if these conditions are true but that may be confusing
				} else {
					metaboliteRenameInterface.setVisible(true);
				}
			}
		});
		
		ActionListener metabRenameOKButtonActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent prodActionEvent) {
				String fileString = "jdbc:sqlite:" + LocalConfig.getInstance().getLoadedDatabase() + ".db";
				String newName = "";
				try {
					metaboliteRenameInterface.setNewName(metaboliteRenameInterface.textField.getText());
					if (metaboliteRenameInterface.getNewName() != null && metaboliteRenameInterface.getNewName().length() > 0) {
						newName = metaboliteRenameInterface.getNewName();
						// check if duplicate metabolite
						if (LocalConfig.getInstance().getMetaboliteIdNameMap().containsKey(newName)) {							
							if (!duplicatePromptShown) {
								JOptionPane.showMessageDialog(null,                
										"Duplicate Metabolite.",                
										"Duplicate Metabolite",                                
										JOptionPane.ERROR_MESSAGE);
							}
							duplicatePromptShown = true;
						} else {
							// update id name map
							Object idValue = LocalConfig.getInstance().getMetaboliteIdNameMap().get(metabAbbrev);
							LocalConfig.getInstance().getMetaboliteIdNameMap().remove(metabAbbrev);	
							LocalConfig.getInstance().getMetaboliteIdNameMap().put(newName, idValue);
							if (renameMetabolite) { 
								MetaboliteFactory aFactory = new MetaboliteFactory("SBML", LocalConfig.getInstance().getLoadedDatabase());
								ArrayList<Integer> participatingReactions = aFactory.participatingReactions(newName);
								// update used list for new name
								Object value = LocalConfig.getInstance().getMetaboliteUsedMap().get(metabAbbrev);
								LocalConfig.getInstance().getMetaboliteUsedMap().remove(metabAbbrev);
								LocalConfig.getInstance().getMetaboliteUsedMap().put(newName, value);
								// rewrite reaction and check if valid
								ReactionsUpdater updater = new ReactionsUpdater();
								updater.rewriteReactions(participatingReactions, LocalConfig.getInstance().getLoadedDatabase());
							}							
							metabolitesTable.getModel().setValueAt(newName, viewRow, GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN);
							updateMetabolitesDatabaseRow(viewRow, Integer.parseInt((String) (metabolitesTable.getModel().getValueAt(viewRow, 0))), "SBML", LocalConfig.getInstance().getLoadedDatabase());					
							
							Class.forName("org.sqlite.JDBC");
							Connection con = DriverManager.getConnection(fileString);
							setUpMetabolitesTable(con);
							setUpReactionsTable(con);
							metaboliteRenameInterface.textField.setText("");
							metaboliteRenameInterface.setVisible(false);
							metaboliteRenameInterface.dispose();
						}						
					}											
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		
		metaboliteRenameInterface.okButton.addActionListener(metabRenameOKButtonActionListener);
		
		contextMenu.addSeparator();

		//TODO: replace these two menu items below with radio buttons
		final JMenuItem participatingReactionsMenu = new JMenuItem("Highlight Participating Reactions");
		if (metabAbbrev == null || !LocalConfig.getMetaboliteUsedMap().containsKey(metabAbbrev)) {
			participatingReactionsMenu.setEnabled(false);
		}
		participatingReactionsMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				highlightParticipatingRxns = true;
				MetaboliteFactory aFactory = new MetaboliteFactory("SBML", LocalConfig.getInstance().getLoadedDatabase());	
				LocalConfig.getInstance().setParticipatingReactions(aFactory.participatingReactions(metabAbbrev));
				tabbedPane.setSelectedIndex(0);
				ArrayList<Integer> participatingReactions = aFactory.participatingReactions(metabAbbrev);
				// sort to get minimum
				Collections.sort(participatingReactions);
				// scroll first participating reaction into view
				reactionsTable.changeSelection(participatingReactions.get(0) - 1, GraphicalInterfaceConstants.REACTION_STRING_COLUMN, false, false);
				reactionsTable.requestFocus();
			}
		});
		contextMenu.add(participatingReactionsMenu);

		final JMenuItem unhighlightParticipatingReactionsMenu = new JMenuItem("Unhighlight Participating Reactions");
		if (metabAbbrev == null || !LocalConfig.getMetaboliteUsedMap().containsKey(metabAbbrev)) {
			unhighlightParticipatingReactionsMenu.setEnabled(false);
		}
		unhighlightParticipatingReactionsMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				highlightParticipatingRxns = false;
			}
		});
		contextMenu.add(unhighlightParticipatingReactionsMenu);

		contextMenu.addSeparator();

		JMenuItem deleteRowMenu = new JMenuItem("Delete Row(s)");

		if (GraphicalInterface.metabolitesTable.getSelectedRow() > -1) {			
			if (LocalConfig.getInstance().getMetaboliteUsedMap().containsKey(metabAbbrev) && !LocalConfig.getInstance().getDuplicateIds().contains(id)) {
				deleteRowMenu.setEnabled(false);
			} else {
				deleteRowMenu.setEnabled(true);
				deleteRowMenu.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						metaboliteDeleteRows();				
					}
				});
			}
		}
		
		contextMenu.add(deleteRowMenu);

		return contextMenu;
	}
	
	/****************************************************************************/
	// end abbreviation column context menu
	/****************************************************************************/

	/****************************************************************************/
	// begin context menu for remaining columns
	/****************************************************************************/
	
	private JPopupMenu createMetabolitesContextMenu(final int rowIndex,
			final int columnIndex) {
		JPopupMenu contextMenu = new JPopupMenu();		
		
		JMenu selectAllMenu = new JMenu("Select All");
		
		final JRadioButtonMenuItem inclColNamesItem = new JRadioButtonMenuItem(
        "Include Column Names");
		final JRadioButtonMenuItem selectCellsOnly = new JRadioButtonMenuItem(
        "Selected Cells Only");

		ButtonGroup bg = new ButtonGroup();
		bg.add(inclColNamesItem);
		bg.add(selectCellsOnly);
		inclColNamesItem.setSelected(includeMtbColumnNames);
		selectCellsOnly.setSelected(!includeMtbColumnNames);
		
		inclColNamesItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				metabolitesTable.selectAll();
				if (inclColNamesItem.isSelected()) {
					includeMtbColumnNames = true;
				} else {
					includeMtbColumnNames = false;
				}				
				selectAllMtb = true;
				selectMetabolitesRows();
			}
		});
		selectCellsOnly.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				metabolitesTable.selectAll();
				if (selectCellsOnly.isSelected()) {
					includeMtbColumnNames = false;
				} else {
					includeMtbColumnNames = true;
				}				
				selectAllMtb = true;
				selectMetabolitesRows();
			}
		});        
        selectAllMenu.add(inclColNamesItem);
        selectAllMenu.add(selectCellsOnly);

        contextMenu.add(selectAllMenu);
		
		contextMenu.addSeparator();
		
		JMenuItem copyMenu = new JMenuItem("Copy");
		copyMenu.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		copyMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToModel(GraphicalInterface.metabolitesTable.getSelectedRow());
				metabolitesCopy();
			}
		});
		contextMenu.add(copyMenu);

		JMenuItem selectRowMenu = new JMenuItem("Copy Row(s)");
		if (columnIndex > 1) {
			selectRowMenu.setEnabled(false);
		}
		selectRowMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				includeMtbColumnNames = false;
				selectMetabolitesRows();
			}
		});
		contextMenu.add(selectRowMenu);	
		
		JMenuItem selectMetabColMenu = new JMenuItem("Copy Column(s)");
		if (rowIndex > 0) {
			selectMetabColMenu.setEnabled(false);
		}
		
		selectMetabColMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mtbColSelectionMode = true;
				selectMetabolitesColumns();
			}
		});
		contextMenu.add(selectMetabColMenu);
		
		contextMenu.addSeparator();
		
		JMenuItem pasteMenu = new JMenuItem("Paste");
		if (rowIndex > 0 && mtbColSelectionMode == true) {
			pasteMenu.setEnabled(false);
		}
		pasteMenu.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_V, ActionEvent.CTRL_MASK));
		if (isClipboardContainingText(this)
				&& metabolitesTable.getModel().isCellEditable(rowIndex, columnIndex)) {
			pasteMenu.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						metabolitesPaste();
					} catch (Throwable t) {
						/*
						JOptionPane.showMessageDialog(null,                
								"Paste Error",                
								"Paste Error",                                
								JOptionPane.ERROR_MESSAGE);
								*/
						t.printStackTrace();
					}
				}
			});
		} else {
			pasteMenu.setEnabled(false);
		}
		contextMenu.add(pasteMenu);

		JMenuItem fillUpMenu = new JMenuItem("Fill Up");
		//if fill is done in the sorted row it will sort after fill  
		if (getMetabolitesSortColumnIndex() == metabolitesTable.getSelectedColumn()) {
			fillUpMenu.setEnabled(false);
		}
		fillUpMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int row=metabolitesTable.getSelectedRow();
				metabolitesFill(0, row);
			}
		});
		contextMenu.add(fillUpMenu);
		
		JMenuItem fillDownMenu = new JMenuItem("Fill Down");
		if (getMetabolitesSortColumnIndex() == metabolitesTable.getSelectedColumn()) {
			fillDownMenu.setEnabled(false);
		}
		fillDownMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int row=metabolitesTable.getSelectedRow();
				metabolitesFill(row + 1, metabolitesTable.getRowCount());
			}
		});
		contextMenu.add(fillDownMenu);
		
		JMenuItem clearMenu = new JMenuItem("Clear Contents");
		clearMenu.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_E, ActionEvent.CTRL_MASK));
		clearMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				metabolitesClear();
			}
		});
		contextMenu.add(clearMenu);
		
		contextMenu.addSeparator();

		JMenuItem deleteRowMenu = new JMenuItem("Delete Row(s)");

		if (GraphicalInterface.metabolitesTable.getSelectedRow() > -1) {
			int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToModel(GraphicalInterface.metabolitesTable.getSelectedRow());
			int id = Integer.valueOf((String) GraphicalInterface.metabolitesTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.DB_METABOLITE_ID_COLUMN));			
			String metabAbbrev = (String) GraphicalInterface.metabolitesTable.getModel().getValueAt(viewRow, 1);
			if (LocalConfig.getInstance().getMetaboliteUsedMap().containsKey(metabAbbrev) && !LocalConfig.getInstance().getDuplicateIds().contains(id)) {
				deleteRowMenu.setEnabled(false);
			} else {
				deleteRowMenu.setEnabled(true);
				deleteRowMenu.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						metaboliteDeleteRows();				
					}
				});
			}
		}
		
		contextMenu.add(deleteRowMenu);
		
		JMenuItem deleteColumnMenu = new JMenuItem("Delete Column(s)");
		//core columns cannot be deleted
		if (columnIndex < 6) {
			deleteColumnMenu.setEnabled(false);
		}
		deleteColumnMenu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				int numcols=metabolitesTable.getSelectedColumnCount(); 
				int startCol=(metabolitesTable.getSelectedColumns())[0];
				for (int i = startCol + numcols - 1; i >= startCol; i--) {
					metabolitesTable.getColumnExt(i).setVisible(false);
				}		
			}
		});		
		contextMenu.add(deleteColumnMenu);
		
		return contextMenu;
	}  

	/*******************************************************************************/
	// end Metabolites Table context menus
	/*******************************************************************************/

	/*******************************************************************************/
	//end context menus  
	/*******************************************************************************/  	
	
	/*******************************************************************************/
	//clipboard
	/*******************************************************************************/

	//from http://www.javakb.com/Uwe/Forum.aspx/java-programmer/21291/popupmenu-for-a-cell-in-a-JXTable
	private static String getClipboardContents(Object requestor) {
		Transferable t = Toolkit.getDefaultToolkit()
		.getSystemClipboard().getContents(requestor);
		if (t != null) {
			DataFlavor df = DataFlavor.stringFlavor;
			if (df != null) {
				try {
					Reader r = df.getReaderForText(t);
					char[] charBuf = new char[512];
					StringBuffer buf = new StringBuffer();
					int n;
					while ((n = r.read(charBuf, 0, charBuf.length)) > 0) {
						buf.append(charBuf, 0, n);
					}
					r.close();
					return (buf.toString());
				} catch (IOException ex) {
					ex.printStackTrace();
				} catch (UnsupportedFlavorException ex) {
					ex.printStackTrace();
				}
			}
		}
		return null;
	}

	private static boolean isClipboardContainingText(Object requestor) {
		Transferable t = Toolkit.getDefaultToolkit()
		.getSystemClipboard().getContents(requestor);
		return t != null
		&& (t.isDataFlavorSupported(DataFlavor.stringFlavor) || t
				.isDataFlavorSupported(DataFlavor.plainTextFlavor));
	}
	
	private static void setClipboardContents(String s) {
	      StringSelection selection = new StringSelection(s);
	      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
	            selection, selection);
	   }

	/*****************************************************************************/
	//end clipboard
	/******************************************************************************/

	/**************************************************************************/
	//reactionsTable context menu methods
	/**************************************************************************/
	//These methods are based partly on code from ExcelAdapter
	//http://stackoverflow.com/questions/4671657/how-to-copy-content-of-the-jtable-to-clipboard
	
	public void selectReactionsRows() {
		setClipboardContents("");
		ReactionsMetaColumnManager reactionsMetaColumnManager = new ReactionsMetaColumnManager();
		int metaColumnCount = reactionsMetaColumnManager.getMetaColumnCount(LocalConfig.getInstance().getLoadedDatabase());	
		
		StringBuffer sbf=new StringBuffer();
		int numrows = reactionsTable.getSelectedRowCount(); 
		LocalConfig.getInstance().setNumberCopiedRows(numrows);
		//for row selection all columns are selected except id column (hidden)
		LocalConfig.getInstance().setNumberCopiedColumns(reactionsTable.getColumnCount() - 1);
		int[] rowsselected=reactionsTable.getSelectedRows();  
		reactionsTable.changeSelection(rowsselected[0], 1, false, false);
		reactionsTable.changeSelection(rowsselected[numrows - 1], reactionsTable.getColumnCount(), false, true);
		reactionsTable.scrollColumnToVisible(1);
		
		if (selectAllRxn == true && includeRxnColumnNames == true) {
			//add column names to clipboard
			for (int c = 1; c < GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES.length; c++) {
				sbf.append(GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES[c]);
				if (c < GraphicalInterfaceConstants.REACTIONS_COLUMN_NAMES.length - 1) {
					sbf.append("\t"); 
				}
			}
			if (metaColumnCount > 0) {
				for (int r = 1; r <= metaColumnCount; r++) {
					sbf.append("\t");
					sbf.append(reactionsMetaColumnManager.getColumnName(LocalConfig.getInstance().getLoadedDatabase(), r));
				}
			}
			sbf.append("\n");
		}
				
		for (int i = 0; i < numrows; i++) {
			//starts at 1 to avoid reading hidden db id column
			for (int j = 1; j < reactionsTable.getColumnCount() - 1; j++) 
			{ 
				if (reactionsTable.getValueAt(rowsselected[i], j) != null) {
					sbf.append(reactionsTable.getValueAt(rowsselected[i], j));
				} else {
					sbf.append(" ");
				}
				if (j < reactionsTable.getColumnCount()-1) sbf.append("\t"); 
			} 
			sbf.append("\n"); 
		}  
		setClipboardContents(sbf.toString());
		//System.out.println(sbf.toString());
	}
	
	public void selectReactionsColumns() {
		//sets columns as selected
		ListSelectionModel selectionModel = reactionsTable.getSelectionModel();
		selectionModel.setSelectionInterval(0, reactionsTable.getModel().getRowCount() - 1);
		
		StringBuffer sbf=new StringBuffer();
		int numcols = reactionsTable.getSelectedColumnCount(); 
		LocalConfig.getInstance().setNumberCopiedColumns(numcols);
		//for column selection all rows are selected
		LocalConfig.getInstance().setNumberCopiedRows(reactionsTable.getRowCount());
		int[] colsselected=reactionsTable.getSelectedColumns();  
		for (int i = 0; i < reactionsTable.getRowCount(); i++) {
			for (int j = 0; j < numcols; j++) 
			{ 
				if (reactionsTable.getValueAt(i, colsselected[j]) != null) {
					sbf.append(reactionsTable.getValueAt(i, colsselected[j]));
				} else {
					sbf.append(" ");
				}
				if (j<numcols-1) sbf.append("\t"); 
			} 
			sbf.append("\n"); 
		}  
		setClipboardContents(sbf.toString());
		//System.out.println(sbf.toString());
	}
	
	public void reactionsCopy() {
		rxnColSelectionMode = false;
		StringBuffer sbf=new StringBuffer(); 
		// Check to ensure we have selected only a contiguous block of cells
		int numcols=reactionsTable.getSelectedColumnCount(); 
		int numrows=reactionsTable.getSelectedRowCount(); 
		LocalConfig.getInstance().setNumberCopiedRows(numrows);
		LocalConfig.getInstance().setNumberCopiedColumns(numcols);
		int[] rowsselected=reactionsTable.getSelectedRows(); 
		int[] colsselected=reactionsTable.getSelectedColumns(); 
		if (!((numrows-1==rowsselected[rowsselected.length-1]-rowsselected[0] && 
				numrows==rowsselected.length) && 
				(numcols-1==colsselected[colsselected.length-1]-colsselected[0] && 
						numcols==colsselected.length))) 
		{ 
			JOptionPane.showMessageDialog(null, "Invalid Copy Selection", 
					"Invalid Copy Selection", 
					JOptionPane.ERROR_MESSAGE); 
			return; 
		} 
		for (int i=0;i<numrows;i++) 
		{ 
			for (int j=0;j<numcols;j++) 
			{ 
				if (reactionsTable.getValueAt(rowsselected[i],colsselected[j]) != null) {
					sbf.append(reactionsTable.getValueAt(rowsselected[i],colsselected[j]));
				} else {
					sbf.append(" ");
				}
				if (j<numcols-1) sbf.append("\t"); 
			} 
			sbf.append("\n"); 
		}  
		setClipboardContents(sbf.toString());
		//System.out.println(sbf.toString());
	}

	public void reactionsPaste() {
		showErrorMessage = true;
		ReactionsUpdater updater = new ReactionsUpdater();
		ArrayList<Integer> rowList = new ArrayList<Integer>();
		ArrayList<Integer> reacIdList = new ArrayList<Integer>();
		ArrayList<String> oldReactionsList = new ArrayList<String>();
		String copiedString = getClipboardContents(GraphicalInterface.this);
		String[] s1 = copiedString.split("\n");
		// if copied row last entry is blank, split truncates selection, length will
		// be less than getNumberCopiedRows and throw ArrayOutOfBoundsError
		int diff = 0;  // if s1 length is less than getNumberCopiedRows(), will be assigned value
		if (s1.length < LocalConfig.getInstance().getNumberCopiedRows()) {
			diff = LocalConfig.getInstance().getNumberCopiedRows() - s1.length;
		}
		int startRow = (reactionsTable.getSelectedRows())[0];
		int startCol = (reactionsTable.getSelectedColumns())[0];
		if (rxnColSelectionMode == true && startRow != 0) {
			//do not paste if column is selected and selected cell is not 
			//in first row since it would result in an index error
		} else {
			if (LocalConfig.getInstance().getNumberCopiedRows() != null && reactionsTable.getSelectedRows().length >= LocalConfig.getInstance().getNumberCopiedRows()) {
				if (LocalConfig.getInstance().getNumberCopiedRows() > 0) {
					//if selected area is larger than copied area, it will fill the same cell
					//contents repeatedly until end of selection, based on integer division
					int quotient = reactionsTable.getSelectedRows().length/LocalConfig.getInstance().getNumberCopiedRows();
					int remainder = reactionsTable.getSelectedRows().length%LocalConfig.getInstance().getNumberCopiedRows();
					for (int q = 0; q < quotient; q++) {	
						//there are two for loops since when pasting into a sorted column the column  
						//sorts itself for each value inserted into table, causing row numbers to change 
						//and erroneous results. making lists of row numbers and db id's, then inserting 
						//values into table and db based on these lists solves this problem
						for (int r = 0; r < LocalConfig.getInstance().getNumberCopiedRows(); r++) {
							int row = reactionsTable.convertRowIndexToModel(startRow + r);
							rowList.add(row);
							int reacId = Integer.valueOf((String) reactionsTable.getModel().getValueAt(row, 0));
							reacIdList.add(reacId);
							String oldReaction = (String) reactionsTable.getModel().getValueAt(row, GraphicalInterfaceConstants.REACTION_STRING_COLUMN);
							setOldReaction(oldReaction);
							oldReactionsList.add(oldReaction);
						}
						for (int r = 0; r < s1.length; r++) {
							int viewRow = GraphicalInterface.reactionsTable.convertRowIndexToView(rowList.get(q * LocalConfig.getInstance().getNumberCopiedRows() + r));
							String[] rowstring = s1[r].split("\t");							
							for (int c = 0; c < LocalConfig.getInstance().getNumberCopiedColumns(); c++) {
								if (c < rowstring.length) {
									if (isReactionsEntryValid(startCol + c, viewRow, rowstring[c])) {
										reactionsTable.setValueAt(rowstring[c], viewRow, startCol + c);			
									} else {
										validPaste = false;
									}				
								} else {
									reactionsTable.setValueAt(" ", viewRow, startCol + c);
								}
							}
						}
						if (diff > 0) {
							for (int r = s1.length; r < LocalConfig.getInstance().getNumberCopiedRows(); r++) {
								int viewRow = GraphicalInterface.reactionsTable.convertRowIndexToView(rowList.get(q * LocalConfig.getInstance().getNumberCopiedRows() + r));						
								for (int c = 0; c < LocalConfig.getInstance().getNumberCopiedColumns(); c++) {
									// check if "" is a valid entry
									if (isReactionsEntryValid(startCol + c, viewRow, "")) {
										reactionsTable.setValueAt("", viewRow, startCol + c);			
									} else {
										validPaste = false;
									}				
								}
							}
						}
						startRow += LocalConfig.getInstance().getNumberCopiedRows();					
					}
					for (int m = 0; m < remainder; m++) {
						int row = reactionsTable.convertRowIndexToModel(startRow + m);
						rowList.add(row);
						int reacId = Integer.valueOf((String) reactionsTable.getModel().getValueAt(row, 0));
						reacIdList.add(reacId);
						String oldReaction = (String) reactionsTable.getModel().getValueAt(row, GraphicalInterfaceConstants.REACTION_STRING_COLUMN);
						setOldReaction(oldReaction);
						oldReactionsList.add(oldReaction);
					}
					int remainderStartIndex = rowList.size() - remainder;
					// remainder of s1 could be larger, use smaller value to avoid
					// array index error
					int min = 0;
					if (s1.length < remainder) {
						min = s1.length;
					} else {
						min = remainder;
					}
					// if remainder is larger, have to fill in difference with ""
					int remDiff = 0;
					if (s1.length < remainder) {
						remDiff = remainder - s1.length;
					}
					for (int m = 0; m < min; m++) {
						int viewRow = GraphicalInterface.reactionsTable.convertRowIndexToView(rowList.get(remainderStartIndex + m));
						String[] rowstring = s1[m].split("\t");
						for (int c = 0; c < LocalConfig.getInstance().getNumberCopiedColumns(); c++) {
							if (c < rowstring.length) {							
								if (isReactionsEntryValid(startCol + c, viewRow, rowstring[c])) {
									reactionsTable.setValueAt(rowstring[c], viewRow, startCol + c);
								} else {
									validPaste = false;
								}
							} else {
								reactionsTable.setValueAt(" ", viewRow, startCol + c);
							}
						}
					}
					if (remDiff > 0) {
						for (int m = s1.length; m < remainder; m++) {
							int viewRow = GraphicalInterface.reactionsTable.convertRowIndexToView(rowList.get(remainderStartIndex + m));
							for (int c = 0; c < LocalConfig.getInstance().getNumberCopiedColumns(); c++) {
								// check if "" is a valid entry
								if (isReactionsEntryValid(startCol + c, viewRow, "")) {
									reactionsTable.setValueAt("", viewRow, startCol + c);			
								} else {
									validPaste = false;
								}				
							}
						}
					}
					if (validPaste) {					
						updater.updateReactionRows(rowList, reacIdList, oldReactionsList, LocalConfig.getInstance().getLoadedDatabase());
					} else {
						if (showErrorMessage = true) {
							JOptionPane.showMessageDialog(null,                
									getPasteError(),                
									"Paste Error",                                
									JOptionPane.ERROR_MESSAGE);
						}						
						validPaste = true;
					}
									
					String fileString = "jdbc:sqlite:" + LocalConfig.getInstance().getLoadedDatabase() + ".db";
					try {
						Class.forName("org.sqlite.JDBC");
						Connection con = DriverManager.getConnection(fileString);
						setUpMetabolitesTable(con);
						setUpReactionsTable(con);
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
				}
				//if selected area is smaller than copied area, fills in copied area
				//from first selected cell as upper left
			} else {
				for (int r = 0; r < LocalConfig.getInstance().getNumberCopiedRows(); r++) {
					int row = reactionsTable.convertRowIndexToModel(startRow + r);
					rowList.add(row);
					int reacId = Integer.valueOf((String) reactionsTable.getModel().getValueAt(row, 0));
					reacIdList.add(reacId);
					String oldReaction = (String) reactionsTable.getModel().getValueAt(row, GraphicalInterfaceConstants.REACTION_STRING_COLUMN);
					setOldReaction(oldReaction);
					oldReactionsList.add(oldReaction);
				}
				pasteReactionRows(rowList, reacIdList, s1, startCol);
				if (validPaste) {
					updater.updateReactionRows(rowList, reacIdList, oldReactionsList, LocalConfig.getInstance().getLoadedDatabase());
				} else {
					if (showErrorMessage = true) {
						JOptionPane.showMessageDialog(null,                
				    			getPasteError(),                
								"Paste Error",                                
								JOptionPane.ERROR_MESSAGE);
					}					
					validPaste = true;
				}
								
				String fileString = "jdbc:sqlite:" + LocalConfig.getInstance().getLoadedDatabase() + ".db";
				try {
					Class.forName("org.sqlite.JDBC");
					Connection con = DriverManager.getConnection(fileString);
					setUpMetabolitesTable(con);
					setUpReactionsTable(con);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
		}		
	}

	public void pasteReactionRows(ArrayList<Integer> rowList, ArrayList<Integer> reacIdList, String[] s1, int startCol) {
		// if copied row last entry is blank, split truncates selection, length will
		// be less than getNumberCopiedRows and throw ArrayOutOfBoundsError
		int diff = 0;  // if s1 length is less than getNumberCopiedRows(), will be assigned value
		if (s1.length < LocalConfig.getInstance().getNumberCopiedRows()) {
			diff = LocalConfig.getInstance().getNumberCopiedRows() - s1.length;
		}
		for (int r = 0; r < s1.length; r++) {
			int viewRow = GraphicalInterface.reactionsTable.convertRowIndexToView(rowList.get(r));
			String[] rowstring = s1[r].split("\t");
			for (int c = 0; c < LocalConfig.getInstance().getNumberCopiedColumns(); c++) {
				if (c < rowstring.length) {
					if (isReactionsEntryValid(startCol + c, viewRow, rowstring[c])) {
						reactionsTable.setValueAt(rowstring[c], viewRow, startCol + c);						
					} else {
						validPaste = false;
					}				
				} else {
					reactionsTable.setValueAt(" ", viewRow, startCol + c);
				}
			}
		}
		if (diff > 0) {
			for (int r = s1.length; r < LocalConfig.getInstance().getNumberCopiedRows(); r++) {
				int viewRow = GraphicalInterface.reactionsTable.convertRowIndexToView(rowList.get(r));
				for (int c = 0; c < LocalConfig.getInstance().getNumberCopiedColumns(); c++) {
					// check if "" is a valid entry
					if (isReactionsEntryValid(startCol + c, viewRow, "")) {
						reactionsTable.setValueAt("", viewRow, startCol + c);			
					} else {
						validPaste = false;
					}				
				}
			}
		}
	}

	public boolean isReactionsEntryValid(int columnIndex, int viewRow, String value) {
		boolean isNumber = true;
		if (columnIndex == GraphicalInterfaceConstants.FLUX_VALUE_COLUMN || 
				columnIndex == GraphicalInterfaceConstants.LOWER_BOUND_COLUMN ||
				columnIndex == GraphicalInterfaceConstants.UPPER_BOUND_COLUMN ||
				columnIndex == GraphicalInterfaceConstants.BIOLOGICAL_OBJECTIVE_COLUMN) {
			try {
				Double.valueOf(value);
			}
		    catch ( NumberFormatException nfe ) {
		    	setPasteError("Number format exception");
		    	isNumber = false;
		        return false;
		    }
		    if (isNumber) {
		    	if (columnIndex == GraphicalInterfaceConstants.LOWER_BOUND_COLUMN) {
					Double lowerBound = Double.valueOf(value);
					Double upperBound = Double.valueOf((String) (reactionsTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.UPPER_BOUND_COLUMN)));
					if (reactionsTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.REVERSIBLE_COLUMN).toString().compareTo("false") == 0 && lowerBound < 0) {					
						setPasteError("Lower Bound Paste Error");
				        return false;					
					} else if (lowerBound > upperBound) {
						setPasteError("Lower Bound Paste Error");
				        return false;						
					}
				} else if (columnIndex == GraphicalInterfaceConstants.UPPER_BOUND_COLUMN) {				
					Double lowerBound = Double.valueOf((String) (reactionsTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.LOWER_BOUND_COLUMN)));
					Double upperBound = Double.valueOf(value);
					if (upperBound < lowerBound) {
						setPasteError("Upper Bound Paste Error");
				        return false;						
					}
				}
		    }
		} else if (columnIndex == GraphicalInterfaceConstants.KO_COLUMN) {
			if (value.compareTo("true") == 0 || value.compareTo("false") == 0) {
				return true;
			} else {
				setPasteError("           Invalid Entry");
				return false;
			}
		} else if (columnIndex == GraphicalInterfaceConstants.REACTION_STRING_COLUMN) {
			if (value != null && value.trim().length() > 0) {
				if (value.contains("=") || value.contains(">")) {
					ReactionParser parser = new ReactionParser();
					if (!parser.isValid(value)) {
						setPasteError("Invalid Reaction Format");
						return false;
					} else {
						LocalConfig.getInstance().pastedReaction = true;
						int id = (Integer.valueOf((String) GraphicalInterface.reactionsTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.DB_REACTIONS_ID_COLUMN)));	
						ReactionsUpdater updater = new ReactionsUpdater();
						updater.updateReactionEquations(id, getOldReaction(), value, LocalConfig.getInstance().getLoadedDatabase());
						return true;
					}				
				} else {
					setPasteError("Invalid Reaction Format");
					return false;
				}
			}
		}
		return true;
		 
	}
		
	public void reactionsClear() {
		//TODO: add if column is reactionEquations add to oldReactionsList
		ReactionsUpdater updater = new ReactionsUpdater();
		ArrayList<Integer> rowList = new ArrayList<Integer>();
		ArrayList<Integer> reacIdList = new ArrayList<Integer>();
		ArrayList<String> oldReactionsList = new ArrayList<String>();
		
		int startRow=(reactionsTable.getSelectedRows())[0]; 
		int startCol=(reactionsTable.getSelectedColumns())[0];
		for (int r = 0; r < reactionsTable.getSelectedRows().length; r++) {
			int row = reactionsTable.convertRowIndexToModel(startRow + r);
			rowList.add(row);
			int reacId = Integer.valueOf((String) reactionsTable.getModel().getValueAt(row, 0));
			reacIdList.add(reacId);
			String oldReaction = (String) reactionsTable.getModel().getValueAt(row, GraphicalInterfaceConstants.REACTION_STRING_COLUMN);
			oldReactionsList.add(oldReaction);
		}
		for(int i=0; i < reactionsTable.getSelectedRows().length ;i++) { 
			for(int j=0; j < reactionsTable.getSelectedColumns().length ;j++) { 					
				int viewRow = GraphicalInterface.reactionsTable.convertRowIndexToView(rowList.get(i));
				reactionsTable.setValueAt(" ", viewRow, startCol + j);
			} 
		}
		updater.updateReactionRows(rowList, reacIdList, oldReactionsList, LocalConfig.getInstance().getLoadedDatabase());	
	}

	//only works for one row, if multiple rows, would need to deal with
	//remainders (see paste - quotient). usually fill is used to fill one
	//value or row of values, not alternating rows
	public void reactionsFill(int start, int end) {
		//TODO: add if column is reactionEquations add to oldReactionsList
		reactionsCopy();
       
		String copiedString = getClipboardContents(GraphicalInterface.this);
		//String[] s1 = copiedString.split("\n");
		ReactionsUpdater updater = new ReactionsUpdater();
		ArrayList<Integer> rowList = new ArrayList<Integer>();
		ArrayList<Integer> reacIdList = new ArrayList<Integer>();
		ArrayList<String> oldReactionsList = new ArrayList<String>();
		int startCol=(reactionsTable.getSelectedColumns())[0];	
		for (int r = start; r < end; r++) {
			int row = reactionsTable.convertRowIndexToModel(r);
			rowList.add(row);
			int reacId = Integer.valueOf((String) reactionsTable.getModel().getValueAt(row, 0));
			reacIdList.add(reacId);
			String oldReaction = (String) reactionsTable.getModel().getValueAt(row, GraphicalInterfaceConstants.REACTION_STRING_COLUMN);
			oldReactionsList.add(oldReaction);
		}
		for (int m = 0; m < rowList.size(); m++) {			
			int viewRow = GraphicalInterface.reactionsTable.convertRowIndexToView(rowList.get(m));
			String[] rowstring = copiedString.split("\t");
			for (int c = 0; c < rowstring.length; c++) {
				reactionsTable.setValueAt(rowstring[c], viewRow, startCol + c);
			}
		}
		updater.updateReactionRows(rowList, reacIdList, oldReactionsList, LocalConfig.getInstance().getLoadedDatabase());			
	}

	public void reactionsDeleteRows() {
		int rowIndexStart = reactionsTable.getSelectedRow();
		int rowIndexEnd = reactionsTable.getSelectionModel().getMaxSelectionIndex();
		ArrayList<Integer> deleteIds = new ArrayList<Integer>();
		ArrayList<String> deleteAbbreviations = new ArrayList<String>();
		ArrayList<String> deletedReactions = new ArrayList<String>();
		for (int r = rowIndexStart; r <= rowIndexEnd; r++) {
			int viewRow = GraphicalInterface.reactionsTable.convertRowIndexToModel(r);
			int id = (Integer.valueOf((String) GraphicalInterface.reactionsTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.DB_REACTIONS_ID_COLUMN)));
			deleteIds.add(id);
			String reactionString = (String) GraphicalInterface.reactionsTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.REACTION_STRING_COLUMN);
			deletedReactions.add(reactionString);			
		}
		
		ReactionsUpdater updater = new ReactionsUpdater();
		updater.deleteRows(deleteIds, deletedReactions, LocalConfig.getInstance().getLoadedDatabase());
		String fileString = "jdbc:sqlite:" + LocalConfig.getInstance().getLoadedDatabase() + ".db";
		try {
			Class.forName("org.sqlite.JDBC");
			Connection con = DriverManager.getConnection(fileString);
			setUpReactionsTable(con);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**************************************************************************/
	//end reactionsTable context menu methods
	/**************************************************************************/

	/**************************************************************************/
	//metabolitesTable context menu methods
	/**************************************************************************/
	
	public void selectMetabolitesRows() {
		setClipboardContents("");
		MetabolitesMetaColumnManager metabolitesMetaColumnManager = new MetabolitesMetaColumnManager();
		int metaColumnCount = metabolitesMetaColumnManager.getMetaColumnCount(LocalConfig.getInstance().getLoadedDatabase());	

		StringBuffer sbf=new StringBuffer();
		int numrows = metabolitesTable.getSelectedRowCount(); 
		LocalConfig.getInstance().setNumberCopiedRows(numrows);
		//for row selection all columns are selected except id column(hidden)
		LocalConfig.getInstance().setNumberCopiedColumns(metabolitesTable.getColumnCount() - 1);
		int[] rowsselected=metabolitesTable.getSelectedRows();  
		
		metabolitesTable.changeSelection(rowsselected[0], 1, false, false);
		metabolitesTable.changeSelection(rowsselected[numrows - 1], metabolitesTable.getColumnCount(), false, true);
		metabolitesTable.scrollColumnToVisible(1);
		
		if (selectAllMtb == true && includeMtbColumnNames == true) {
			//add column names to clipboard
			for (int c = 1; c < GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES.length; c++) {
				sbf.append(GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES[c]);
				if (c < GraphicalInterfaceConstants.METABOLITES_COLUMN_NAMES.length - 1) {
					sbf.append("\t"); 
				}
			}
			if (metaColumnCount > 0) {
				for (int r = 1; r <= metaColumnCount; r++) {
					sbf.append("\t");
					sbf.append(metabolitesMetaColumnManager.getColumnName(LocalConfig.getInstance().getLoadedDatabase(), r));
				}
			}
			sbf.append("\n");
		}
		
		for (int i = 0; i < numrows; i++) {
			//starts at 1 to avoid reading hidden db id column 
			for (int j = 1; j < metabolitesTable.getColumnCount() - 1; j++) 
			{ 
				if (metabolitesTable.getValueAt(rowsselected[i], j) != null) {
					sbf.append(metabolitesTable.getValueAt(rowsselected[i], j));
				} else {
					sbf.append(" ");
				}
				if (j < metabolitesTable.getColumnCount()-1) sbf.append("\t"); 
			} 
			sbf.append("\n"); 
		}  
		setClipboardContents(sbf.toString());
		//System.out.println(sbf.toString());
	}
	
	public void selectMetabolitesColumns() {
		//sets columns as selected
		ListSelectionModel selectionModel = metabolitesTable.getSelectionModel();
		selectionModel.setSelectionInterval(0, metabolitesTable.getModel().getRowCount() - 1);
		
		LocalConfig.getInstance().setNumberCopiedRows(metabolitesTable.getRowCount());
		StringBuffer sbf=new StringBuffer();
		int numcols = metabolitesTable.getSelectedColumnCount();
		LocalConfig.getInstance().setNumberCopiedColumns(numcols);
		//for column selection all rows are selected
		LocalConfig.getInstance().setNumberCopiedRows(metabolitesTable.getRowCount());
		int[] colsselected=metabolitesTable.getSelectedColumns();  
		for (int i = 0; i < metabolitesTable.getRowCount(); i++) {
			for (int j = 0; j < numcols; j++) 
			{ 
				if (metabolitesTable.getValueAt(i, colsselected[j]) != null) {
					sbf.append(metabolitesTable.getValueAt(i, colsselected[j]));
				} else {
					sbf.append(" ");
				}
				if (j<numcols-1) sbf.append("\t"); 
			} 
			sbf.append("\n"); 
		}  
		setClipboardContents(sbf.toString());
		//System.out.println(sbf.toString());
	}
	
	public void metabolitesCopy() {
		mtbColSelectionMode = false;
		StringBuffer sbf=new StringBuffer(); 
		// Check to ensure we have selected only a contiguous block of 
		// cells 
		int numcols=metabolitesTable.getSelectedColumnCount(); 
		int numrows=metabolitesTable.getSelectedRowCount(); 
		LocalConfig.getInstance().setNumberCopiedRows(numrows);
		LocalConfig.getInstance().setNumberCopiedColumns(numcols);
		int[] rowsselected=metabolitesTable.getSelectedRows(); 
		int[] colsselected=metabolitesTable.getSelectedColumns(); 
		if (!((numrows-1==rowsselected[rowsselected.length-1]-rowsselected[0] && 
				numrows==rowsselected.length) && 
				(numcols-1==colsselected[colsselected.length-1]-colsselected[0] && 
						numcols==colsselected.length))) 
		{ 
			JOptionPane.showMessageDialog(null, "Invalid Copy Selection", 
					"Invalid Copy Selection", 
					JOptionPane.ERROR_MESSAGE); 
			return; 
		} 
		for (int i=0;i<numrows;i++) 
		{ 
			for (int j=0;j<numcols;j++) 
			{ 
				if (metabolitesTable.getValueAt(rowsselected[i],colsselected[j]) != null) {
					sbf.append(metabolitesTable.getValueAt(rowsselected[i],colsselected[j]));
				} else {
					sbf.append(" ");
				}
				if (j<numcols-1) sbf.append("\t"); 
			} 
			sbf.append("\n");		 
		}  
		setClipboardContents(sbf.toString());
		//System.out.println(sbf.toString());
	}

	public void metabolitesPaste() {
		showErrorMessage = true;
		MetabolitesUpdater updater = new MetabolitesUpdater();
		ArrayList<Integer> rowList = new ArrayList<Integer>();
		ArrayList<Integer> metabIdList = new ArrayList<Integer>();
		String copiedString = getClipboardContents(GraphicalInterface.this);
		String[] s1 = copiedString.split("\n");
		// if copied row last entry is blank, split truncates selection, length will
		// be less than getNumberCopiedRows and throw ArrayOutOfBoundsError
		int diff = 0;  // if s1 length is less than getNumberCopiedRows(), will be assigned value
		if (s1.length < LocalConfig.getInstance().getNumberCopiedRows()) {
			diff = LocalConfig.getInstance().getNumberCopiedRows() - s1.length;
		}
		int startRow = (metabolitesTable.getSelectedRows())[0];
		int startCol = (metabolitesTable.getSelectedColumns())[0];
		if (mtbColSelectionMode == true && startRow != 0) {
			//do not paste if column is selected and selected cell is not 
			//in first row since it would result in an index error
		} else {
			if (LocalConfig.getInstance().getNumberCopiedRows() != null && metabolitesTable.getSelectedRows().length >= LocalConfig.getInstance().getNumberCopiedRows()) {
				if (LocalConfig.getInstance().getNumberCopiedRows() > 0) {
					//if selected area is larger than copied area, it will fill the same cell
					//contents repeatedly until end of selection, based on integer division
					int quotient = metabolitesTable.getSelectedRows().length/LocalConfig.getInstance().getNumberCopiedRows();
					int remainder = metabolitesTable.getSelectedRows().length%LocalConfig.getInstance().getNumberCopiedRows();
					for (int q = 0; q < quotient; q++) {	
						//there are two for loops since when pasting into a sorted column the column  
						//sorts itself for each value inserted into table, causing row numbers to change 
						//and erroneous results. making lists of row numbers and db id's, then inserting 
						//values into table and db based on these lists solves this problem
						for (int r = 0; r < LocalConfig.getInstance().getNumberCopiedRows(); r++) {
							int row = metabolitesTable.convertRowIndexToModel(startRow + r);
							rowList.add(row);
							int metabId = Integer.valueOf((String) metabolitesTable.getModel().getValueAt(row, 0));
							metabIdList.add(metabId);
						}
						for (int r = 0; r < s1.length; r++) {
							int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToView(rowList.get(q * LocalConfig.getInstance().getNumberCopiedRows() + r));
							int id = Integer.valueOf((String) GraphicalInterface.metabolitesTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.DB_METABOLITE_ID_COLUMN));		
							String metabAbbrev = (String) GraphicalInterface.metabolitesTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN);
							String[] rowstring = s1[r].split("\t");
							for (int c = 0; c < LocalConfig.getInstance().getNumberCopiedColumns(); c++) {
								if (c < rowstring.length) {
									if (LocalConfig.getInstance().getMetaboliteUsedMap().containsKey(metabAbbrev) && !LocalConfig.getInstance().getDuplicateIds().contains(id)) {
										setPasteError(GraphicalInterfaceConstants.PARTICIPATING_METAB_PASTE_ERROR_MESSAGE);
										updater.updateMetaboliteRows(rowList, metabIdList, LocalConfig.getInstance().getLoadedDatabase());
										validPaste = false;
									} else if (isMetabolitesEntryValid(startCol + c, rowstring[c])) {
										metabolitesTable.setValueAt(rowstring[c], viewRow, startCol + c);
									} else {
										validPaste = false;
									}
								} else {
									metabolitesTable.setValueAt(" ", viewRow, startCol + c);
								}
							}
						}
						if (diff > 0) {
							for (int r = s1.length; r < LocalConfig.getInstance().getNumberCopiedRows(); r++) {
								int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToView(rowList.get(q * LocalConfig.getInstance().getNumberCopiedRows() + r));
								int id = Integer.valueOf((String) GraphicalInterface.metabolitesTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.DB_METABOLITE_ID_COLUMN));		
								String metabAbbrev = (String) GraphicalInterface.metabolitesTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN);
								for (int c = 0; c < LocalConfig.getInstance().getNumberCopiedColumns(); c++) {
									if (LocalConfig.getInstance().getMetaboliteUsedMap().containsKey(metabAbbrev) && !LocalConfig.getInstance().getDuplicateIds().contains(id)) {
										setPasteError(GraphicalInterfaceConstants.PARTICIPATING_METAB_PASTE_ERROR_MESSAGE);
										updater.updateMetaboliteRows(rowList, metabIdList, LocalConfig.getInstance().getLoadedDatabase());
										validPaste = false;
									// check if "" is a valid entry
									} else if (isMetabolitesEntryValid(startCol + c, "")) {
										metabolitesTable.setValueAt("", viewRow, startCol + c);
									} else {
										validPaste = false;
									}				
								}
							}
						}
						startRow += LocalConfig.getInstance().getNumberCopiedRows();
					}
					for (int m = 0; m < remainder; m++) {
						int row = metabolitesTable.convertRowIndexToModel(startRow + m);
						rowList.add(row);
						int metabId = Integer.valueOf((String) metabolitesTable.getModel().getValueAt(row, 0));
						metabIdList.add(metabId);						
					}
					int remainderStartIndex = rowList.size() - remainder;
					// remainder of s1 could be larger, use smaller value to avoid
					// array index error
					int min = 0;
					if (s1.length < remainder) {
						min = s1.length;
					} else {
						min = remainder;
					}
					// if remainder is larger, have to fill in difference with ""
					int remDiff = 0;
					if (s1.length < remainder) {
						remDiff = remainder - s1.length;
					}
					for (int m = 0; m < min; m++) {
						int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToView(rowList.get(remainderStartIndex + m));
						int id = Integer.valueOf((String) GraphicalInterface.metabolitesTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.DB_METABOLITE_ID_COLUMN));		
						String metabAbbrev = (String) GraphicalInterface.metabolitesTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN);
						String[] rowstring = s1[m].split("\t");
						for (int c = 0; c < LocalConfig.getInstance().getNumberCopiedColumns(); c++) {
							if (c < rowstring.length) {		
								if (LocalConfig.getInstance().getMetaboliteUsedMap().containsKey(metabAbbrev) && !LocalConfig.getInstance().getDuplicateIds().contains(id)) {
									setPasteError(GraphicalInterfaceConstants.PARTICIPATING_METAB_PASTE_ERROR_MESSAGE);
									updater.updateMetaboliteRows(rowList, metabIdList, LocalConfig.getInstance().getLoadedDatabase());
									validPaste = false;
								} else if (isMetabolitesEntryValid(startCol + c, rowstring[c])) {
									metabolitesTable.setValueAt(rowstring[c], viewRow, startCol + c);
								} else {
									validPaste = false;
								}
							} else {
								metabolitesTable.setValueAt(" ", viewRow, startCol + c);
							}
						}
					}
					if (remDiff > 0) {
						for (int m = s1.length; m < remainder; m++) {
							int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToView(rowList.get(remainderStartIndex + m));
							int id = Integer.valueOf((String) GraphicalInterface.metabolitesTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.DB_METABOLITE_ID_COLUMN));		
							String metabAbbrev = (String) GraphicalInterface.metabolitesTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN);
							for (int c = 0; c < LocalConfig.getInstance().getNumberCopiedColumns(); c++) {
								if (LocalConfig.getInstance().getMetaboliteUsedMap().containsKey(metabAbbrev) && !LocalConfig.getInstance().getDuplicateIds().contains(id)) {
									setPasteError(GraphicalInterfaceConstants.PARTICIPATING_METAB_PASTE_ERROR_MESSAGE);
									updater.updateMetaboliteRows(rowList, metabIdList, LocalConfig.getInstance().getLoadedDatabase());
									validPaste = false;
								// check if "" is a valid entry
								} else if (isMetabolitesEntryValid(startCol + c, "")) {
									metabolitesTable.setValueAt("", viewRow, startCol + c);
								} else {
									validPaste = false;
								}				
							}
						}
					}
					if (validPaste) {						
						updater.updateMetaboliteRows(rowList, metabIdList, LocalConfig.getInstance().getLoadedDatabase());
					} else {
						if (showErrorMessage = true) {
							JOptionPane.showMessageDialog(null,                
									getPasteError(),                
									"Paste Error",                                
									JOptionPane.ERROR_MESSAGE);
						}						
						validPaste = true;
					}					
				}
				//if selected area is smaller than copied area, fills in copied area
				//from first selected cell as upper left
			} else {
				for (int r = 0; r < LocalConfig.getInstance().getNumberCopiedRows(); r++) {
					int row = metabolitesTable.convertRowIndexToModel(startRow + r);
					rowList.add(row);
					int metabId = Integer.valueOf((String) metabolitesTable.getModel().getValueAt(row, 0));
					metabIdList.add(metabId);
				}
				pasteMetaboliteRows(rowList, metabIdList, s1, startCol);
				if (validPaste) {						
					updater.updateMetaboliteRows(rowList, metabIdList, LocalConfig.getInstance().getLoadedDatabase());
				} else {
					if (showErrorMessage = true) {
						JOptionPane.showMessageDialog(null,                
								getPasteError(),                
								"Paste Error",                                
								JOptionPane.ERROR_MESSAGE);
					}						
					validPaste = true;
				}	
			}
		}		
	}
	
	public void pasteMetaboliteRows(ArrayList<Integer> rowList, ArrayList<Integer> metabIdList, String[] s1, int startCol) {
		// if copied row last entry is blank, split truncates selection, length will
		// be less than getNumberCopiedRows and throw ArrayOutOfBoundsError
		int diff = 0;  // if s1 length is less than getNumberCopiedRows(), will be assigned value
		if (s1.length < LocalConfig.getInstance().getNumberCopiedRows()) {
			diff = LocalConfig.getInstance().getNumberCopiedRows() - s1.length;
		}
		for (int r = 0; r < s1.length; r++) {	
			int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToView(rowList.get(r));
			int id = Integer.valueOf((String) GraphicalInterface.metabolitesTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.DB_METABOLITE_ID_COLUMN));		
			String metabAbbrev = (String) GraphicalInterface.metabolitesTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN);
			String[] rowstring = s1[r].split("\t");
			for (int c = 0; c < LocalConfig.getInstance().getNumberCopiedColumns(); c++) {
				if (c < rowstring.length) {		
					if (LocalConfig.getInstance().getMetaboliteUsedMap().containsKey(metabAbbrev) && !LocalConfig.getInstance().getDuplicateIds().contains(id)) {
						setPasteError(GraphicalInterfaceConstants.PARTICIPATING_METAB_PASTE_ERROR_MESSAGE);
						validPaste = false;
					} else if (isMetabolitesEntryValid(startCol + c, rowstring[c])) {
						metabolitesTable.setValueAt(rowstring[c], viewRow, startCol + c);
					} else {
						validPaste = false;
					}
				} else {
					metabolitesTable.setValueAt(" ", viewRow, startCol + c);
				}				
			}
		}
		if (diff > 0) {
			for (int r = s1.length; r < LocalConfig.getInstance().getNumberCopiedRows(); r++) {
				int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToView(rowList.get(r));
				int id = Integer.valueOf((String) GraphicalInterface.metabolitesTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.DB_METABOLITE_ID_COLUMN));		
				String metabAbbrev = (String) GraphicalInterface.metabolitesTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN);
				for (int c = 0; c < LocalConfig.getInstance().getNumberCopiedColumns(); c++) {
					if (LocalConfig.getInstance().getMetaboliteUsedMap().containsKey(metabAbbrev) && !LocalConfig.getInstance().getDuplicateIds().contains(id)) {
						setPasteError(GraphicalInterfaceConstants.PARTICIPATING_METAB_PASTE_ERROR_MESSAGE);
						validPaste = false;
					} else if (isMetabolitesEntryValid(startCol + c, "")) {
						metabolitesTable.setValueAt("", viewRow, startCol + c);
					} else {
						validPaste = false;
					}				
				}
			}
		}
	}

	public boolean isMetabolitesEntryValid(int columnIndex, String value) {
		if (columnIndex == GraphicalInterfaceConstants.CHARGE_COLUMN) {
			if (value != null && value.trim().length() > 0) {
				try {
					Double.valueOf(value);
				}
			    catch ( NumberFormatException nfe ) {
			    	setPasteError("Number format exception");
			        return false;
			    }	           
			}			
		} else if (columnIndex == GraphicalInterfaceConstants.BOUNDARY_COLUMN) {
			if (value.compareTo("true") == 0 || value.compareTo("false") == 0) {
				return true;
			} else {
				setPasteError("           Invalid Entry");
				return false;
			}
		} else if (columnIndex == GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN) {
			if (LocalConfig.getInstance().getMetaboliteIdNameMap().containsKey(value)) {			               
				setPasteError("        Duplicate Metabolite");
				return false;
			}
		}
		
		return true;
		 
	}
	
	public void metabolitesClear() {
		MetabolitesUpdater updater = new MetabolitesUpdater();
		ArrayList<Integer> rowList = new ArrayList<Integer>();
		ArrayList<Integer> metabIdList = new ArrayList<Integer>();
		//TODO: Clear must throw an error if user attempts to clear
		//a used metabolite, but should be able to clear an unused
		//metabolite - see delete, also should not be able to clear boundary
		int startRow=(metabolitesTable.getSelectedRows())[0]; 
		int startCol=(metabolitesTable.getSelectedColumns())[0];
		if (startCol != GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN) {
			for (int r = 0; r < metabolitesTable.getSelectedRows().length; r++) {
				int row = metabolitesTable.convertRowIndexToModel(startRow + r);
				rowList.add(row);
				int metabId = Integer.valueOf((String) metabolitesTable.getModel().getValueAt(row, 0));
				metabIdList.add(metabId);
			}
			for(int i=0; i < metabolitesTable.getSelectedRows().length ;i++) { 
				for(int j=0; j < metabolitesTable.getSelectedColumns().length ;j++) { 					
					int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToView(rowList.get(i));
					metabolitesTable.setValueAt(" ", viewRow, startCol + j);
				} 
			}
			updater.updateMetaboliteRows(rowList, metabIdList, LocalConfig.getInstance().getLoadedDatabase());			
		} else {
			JOptionPane.showMessageDialog(null,                
					"Cannot clear Metab abbrev column, some may be used.",                
					"Clear Error",                                
					JOptionPane.ERROR_MESSAGE);
		}
	}

	//only works for one row, if multiple rows, would need to deal with
	//remainders (see paste - quotient). usually fill is used to fill one
	//value or row of values, not alternating rows
	public void metabolitesFill(int start, int end) {
		//TODO: Fill must throw an error if user attempts to fill over
		//a used metabolite	- see delete
		metabolitesCopy();
       
		String copiedString = getClipboardContents(GraphicalInterface.this);
		//String[] s1 = copiedString.split("\n");
		MetabolitesUpdater updater = new MetabolitesUpdater();
		ArrayList<Integer> rowList = new ArrayList<Integer>();
		ArrayList<Integer> metabIdList = new ArrayList<Integer>();
		int startCol=(metabolitesTable.getSelectedColumns())[0];	
		for (int r = start; r < end; r++) {
			int row = metabolitesTable.convertRowIndexToModel(r);
			rowList.add(row);
			int metabId = Integer.valueOf((String) metabolitesTable.getModel().getValueAt(row, 0));
			metabIdList.add(metabId);
		}
		for (int m = 0; m < rowList.size(); m++) {			
			int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToView(rowList.get(m));
			String[] rowstring = copiedString.split("\t");
			for (int c = 0; c < rowstring.length; c++) {
				metabolitesTable.setValueAt(rowstring[c], viewRow, startCol + c);
			}
		}
		updater.updateMetaboliteRows(rowList, metabIdList, LocalConfig.getInstance().getLoadedDatabase());			
	}

	public void metaboliteDeleteRows() {
		int rowIndexStart = metabolitesTable.getSelectedRow();
		int rowIndexEnd = metabolitesTable.getSelectionModel().getMaxSelectionIndex();
		ArrayList<Integer> deleteIds = new ArrayList<Integer>();
		boolean participant = false;
		for (int r = rowIndexStart; r <= rowIndexEnd; r++) {
			int viewRow = GraphicalInterface.metabolitesTable.convertRowIndexToModel(r);
			String key = (String) GraphicalInterface.metabolitesTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.METABOLITE_ABBREVIATION_COLUMN);
			int id = (Integer.valueOf((String) GraphicalInterface.metabolitesTable.getModel().getValueAt(viewRow, GraphicalInterfaceConstants.DB_METABOLITE_ID_COLUMN)));
			//need to check if used
			// TODO use this same method for clear
			if (LocalConfig.getInstance().getMetaboliteUsedMap().containsKey(key) && !(LocalConfig.getInstance().getDuplicateIds().contains(id))) {
				if (!participant) {
					JOptionPane.showMessageDialog(null,                
							GraphicalInterfaceConstants.PARTICIPATING_METAB_ERROR_MESSAGE,
							GraphicalInterfaceConstants.PARTICIPATING_METAB_ERROR_TITLE,                                
							JOptionPane.ERROR_MESSAGE);
				}
				// may want to print to output pane if used as console
				System.out.println(key + " cannot be deleted since it participates in one or more reactions.");
				// participating metabolite in selected rows
				participant = true; // prevents message from being displayed multiple times
			} else {
				LocalConfig.getInstance().getMetaboliteIdNameMap().remove(key);	
				deleteIds.add(id);
			}						
		}
		
		MetabolitesUpdater updater = new MetabolitesUpdater();
		updater.deleteRows(deleteIds, LocalConfig.getInstance().getLoadedDatabase());
		String fileString = "jdbc:sqlite:" + LocalConfig.getInstance().getLoadedDatabase() + ".db";
		try {
			Class.forName("org.sqlite.JDBC");
			Connection con = DriverManager.getConnection(fileString);
			setUpMetabolitesTable(con);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} 
	
	/**************************************************************************/
	//end metabolitesTable context menu methods
	/**************************************************************************/
	
	/*******************************************************************************/
	//fileList methods
	/*******************************************************************************/
	
	//based on http://www.java2s.com/Code/Java/File-Input-Output/Textfileviewer.htm
	public static void loadOutputPane(String path) {
		File file;
		FileReader in = null;

		try {
			file = new File(path); 
			in = new FileReader(file); 
			char[] buffer = new char[4096]; // Read 4K characters at a time
			int len; 
			outputTextArea.setText(""); 
			while ((len = in.read(buffer)) != -1) { // Read a batch of chars
				String s = new String(buffer, 0, len); 
				outputTextArea.append(s); 
			}
			outputTextArea.setCaretPosition(0); 
		}

		catch (IOException e) {

		}

		finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
			}
		}
	}

	public void clearOutputPane() {
		outputTextArea.setText(""); 
	}

	/*******************************************************************************/
	//end fileList methods
	/*******************************************************************************/
	
	/*******************************************************************************/
	//progressBar methods
	/*******************************************************************************/
	
	class Task extends SwingWorker<Void, Void> {

		@Override
		public void done() {

		}

		@Override
		protected Void doInBackground() throws Exception {
			int progress = 0;
			SBMLDocument doc = new SBMLDocument();
			SBMLReader reader = new SBMLReader();
			try {		  
				doc = reader.readSBML(GraphicalInterface.getSBMLFile());
				SBMLModelReader modelReader = new SBMLModelReader(doc);
				modelReader.setDatabaseName(LocalConfig.getInstance().getDatabaseName());
				modelReader.load();
			} catch (FileNotFoundException e) {
				JOptionPane.showMessageDialog(null,                
						"File does not exist.",                
						"File does not exist.",                                
						JOptionPane.ERROR_MESSAGE);
				//e.printStackTrace();
			} catch (XMLStreamException e) {
				JOptionPane.showMessageDialog(null,                
						"This File is not a Valid SBML File.",                
						"Invalid SBML File.",                                
						JOptionPane.ERROR_MESSAGE);
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}	
			while (progress < 100) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ignore) {
				}			
			}				
			timer.stop();
			return null;
		}
	}

	class TimeListener implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			if (LocalConfig.getInstance().getProgress() > 0) {
				progressBar.progress.setIndeterminate(false);
			}			
			progressBar.progress.setValue(LocalConfig.getInstance().getProgress());
			progressBar.progress.repaint();
			if (LocalConfig.getInstance().getProgress() == 100) {
				if (LocalConfig.getInstance().hasMetabolitesFile || !isCSVFile) {
					setUpTables();
				}				
				timer.stop();
				progressBar.setVisible(false);
				//progressBar.dispose();
				LocalConfig.getInstance().setProgress(0);
				progressBar.progress.setIndeterminate(true);
				if (isCSVFile && LocalConfig.getInstance().getReactionsCSVFile() != null) {
					fileList.setSelectedIndex(-1);
					listModel.clear();
					fileList.setModel(listModel);  
					try {
						String fileString = "jdbc:sqlite:" + getDatabaseName() + ".db";
						Class.forName("org.sqlite.JDBC");
						Connection con = DriverManager.getConnection(fileString);

						LocalConfig.getInstance().setReactionsNextRowCorrection(0);

						TextReactionsModelReader reader = new TextReactionsModelReader();			    
						ArrayList<String> columnNamesFromFile = reader.columnNamesFromFile(LocalConfig.getInstance().getReactionsCSVFile(), 0);	
						ReactionColumnNameInterface columnNameInterface = new ReactionColumnNameInterface(con, columnNamesFromFile);

						columnNameInterface.setModal(true);
						columnNameInterface.setIconImages(icons);

						columnNameInterface.setSize(600, 510);
						columnNameInterface.setResizable(false);
						columnNameInterface.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
						columnNameInterface.setLocationRelativeTo(null);
						columnNameInterface.setVisible(true);	
						columnNameInterface.setAlwaysOnTop(true);
						// sets value to default and loads any new metabolites
						// from reactions file into metabolites table
						LocalConfig.getInstance().hasMetabolitesFile = true;
						timer.start();
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					isCSVFile = false;   // set to default value, also stops reactions from loading again
				}
			}
		}
	}

	/*******************************************************************************/
	//end progressBar methods
	/*******************************************************************************/
	
	public static void main(String[] args) throws Exception {
		curSettings = new SettingsFactory();
		
		Class.forName("org.sqlite.JDBC");       
		DatabaseCreator databaseCreator = new DatabaseCreator();
		setDatabaseName(ConfigConstants.DEFAULT_DATABASE_NAME);
		LocalConfig.getInstance().setLoadedDatabase(ConfigConstants.DEFAULT_DATABASE_NAME);
		Connection con = DriverManager.getConnection("jdbc:sqlite:" + LocalConfig.getInstance().getDatabaseName() + ".db");
		databaseCreator.createDatabase(LocalConfig.getInstance().getDatabaseName());
		databaseCreator.addRows(LocalConfig.getInstance().getDatabaseName(), GraphicalInterfaceConstants.BLANK_DB_METABOLITE_ROW_COUNT, GraphicalInterfaceConstants.BLANK_DB_REACTION_ROW_COUNT);

		//based on code from http://stackoverflow.com/questions/6403821/how-to-add-an-image-to-a-jframe-title-bar
		final ArrayList<Image> icons = new ArrayList<Image>(); 
		icons.add(new ImageIcon("etc/most16.jpg").getImage()); 
		icons.add(new ImageIcon("etc/most32.jpg").getImage());


		final GraphicalInterface frame = new GraphicalInterface(con);	   

		frame.setIconImages(icons);
		frame.setSize(1000, 600);
		frame.setMinimumSize(new Dimension(800, 600));
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
		
		showPrompt = true;
		
		// selected row default at first
		statusBar.setText("1");

	}
}


