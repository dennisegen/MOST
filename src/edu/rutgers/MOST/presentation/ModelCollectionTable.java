package edu.rutgers.MOST.presentation;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import au.com.bytecode.opencsv.CSVReader;

// loosely based on http://www.cs.cf.ac.uk/Dave/HCI/HCI_Handout_CALLER/node167.html
// based on http://www.coderanch.com/t/345311/GUI/java/Adding-rows-Jtable
class ModelCollectionTable
		extends 	JFrame
 {
	// Instance attributes used in this example
	private	JPanel		topPanel;
	private	JScrollPane scrollPane;
	private JTable table = new JTable(){  
		public boolean isCellEditable(int row, int column){
			return false;
			
		}	
	};
	private DefaultTableModel model = new DefaultTableModel();
	private	JPanel		bottomLeftPanel;
	private	JPanel		bottomRightPanel;
	private	JPanel		bottomPanel;
	public static JButton okButton = new JButton("  OK  ");
	public static JButton cancelButton = new JButton("Cancel");

	// Constructor of main frame
	public ModelCollectionTable(File file)
	{
		// Set the frame characteristics
		setTitle( GraphicalInterfaceConstants.TITLE + " - " + "Model Collection" );
		setSize( 700, 500 );
		setBackground( Color.gray );
		
		okButton.setEnabled(false);
		
		table.setRowHeight(20);
		table.setColumnSelectionAllowed(false);
		table.setRowSelectionAllowed(true); 
		table.getSelectionModel().addListSelectionListener(new RowListener());

		// Create a panel to hold all other components
		topPanel = new JPanel();
		topPanel.setLayout( new BorderLayout() );
		getContentPane().add( topPanel );

		// Create columns names
		String columnNames[] = { "Column 1", "Column 2", "Column 3",  "Column 4", "Column 5", 
				"Column 6", "Column 7", "Column 8", "Column 9",};		
		
		for (int i = 0; i < columnNames.length; i++) {
			model.addColumn(columnNames[i]);
		}		
		
		table.setModel(model);
		CSVReader reader;
		try {
			reader = new CSVReader(new FileReader(file), ',');
			String [] dataArray;
			try {
				while ((dataArray = reader.readNext()) != null) {
					Vector <String> row = new Vector<String>();
					for (int s = 0; s < dataArray.length; s++) {
						System.out.println(dataArray[s]);						
						row.add(dataArray[s]);
					}
					model.addRow(row);
				}
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	

		// Add the table to a scrolling pane
		scrollPane = new JScrollPane( table );
		topPanel.add( scrollPane, BorderLayout.CENTER );
		bottomLeftPanel = new JPanel();
		bottomLeftPanel.setLayout( new BorderLayout() );
		bottomRightPanel = new JPanel();
		bottomPanel = new JPanel();
		bottomRightPanel.setLayout( new BorderLayout() );
		bottomLeftPanel.add( okButton, BorderLayout.WEST );
		bottomRightPanel.add( cancelButton, BorderLayout.EAST );
		bottomPanel.add(bottomLeftPanel, BorderLayout.WEST);	
		bottomPanel.add(bottomRightPanel, BorderLayout.EAST);
		topPanel.add( bottomPanel, BorderLayout.SOUTH );
		
		int r = table.getModel().getColumnCount();	
		for (int i = 0; i < r; i++) {
			//set background of id column to grey
			ModelCollectionCellRenderer renderer = new ModelCollectionCellRenderer();			
			TableColumn column = table.getColumnModel().getColumn(i);
            column.setCellRenderer(renderer);
            // Column widths can be changed here
            if (i == 1) {
            	column.setPreferredWidth(150);
            }
            if (i == 6) {
            	column.setPreferredWidth(150);
            }
		}	
		
		ActionListener okButtonActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				System.out.println(table.getSelectedRow());				
				setVisible(false);
				dispose();				
			}
		};

		okButton.addActionListener(okButtonActionListener);
		
		ActionListener cancelButtonActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				setVisible(false);
				dispose();				
			}
		};

		cancelButton.addActionListener(cancelButtonActionListener);
		
	}
	
	private class RowListener implements ListSelectionListener {
    	public void valueChanged(ListSelectionEvent event) {
    		if (table.getSelectedRow() > -1) {
    			okButton.setEnabled(true);
				//System.out.println(table.getSelectedRow());
			}
    	}
    }
	
	public static void main( String args[] )
	{
		File f = new File("ModelCollection.csv");
		ModelCollectionTable mainFrame	= new ModelCollectionTable(f);
		mainFrame.setVisible( true );
	}
}
