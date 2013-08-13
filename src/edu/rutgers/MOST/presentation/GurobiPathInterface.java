package edu.rutgers.MOST.presentation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import edu.rutgers.MOST.config.LocalConfig;
import edu.rutgers.MOST.data.SettingsFactory;

public class GurobiPathInterface  extends JDialog {

	public static JButton fileButton = new JButton(GraphicalInterfaceConstants.GUROBI_PATH_BUTTON);
	public static JButton okButton = new JButton("    OK    ");
	public static JButton cancelButton = new JButton("  Cancel  ");
	public static JButton clearButton = new JButton("Clear");
	public static final JTextField textField = new JTextField();
	
	//Methods of saving current directory
	public static SettingsFactory curSettings;
	
	private String path;

	public void setPath(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public boolean fileSelected;
	
	public GurobiPathInterface() {

		setTitle(GraphicalInterfaceConstants.GUROBI_PATH_INTERFACE_TITLE);		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		getRootPane().setDefaultButton(okButton);
		
		textField.setText("");

	    fileSelected = false;
		
		//box layout
		Box vb = Box.createVerticalBox();

		Box hbLabel = Box.createHorizontalBox();
		Box hbMetab = Box.createHorizontalBox();
		Box hbButton = Box.createHorizontalBox();

		JLabel topLabel = new JLabel();
		topLabel.setText("File Name");
		topLabel.setSize(new Dimension(150, 10));
		//top, left, bottom. right
		topLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
		topLabel.setAlignmentX(CENTER_ALIGNMENT);

		JPanel labelPanel = new JPanel();
		labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
		labelPanel.add(topLabel);
		labelPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		hbLabel.add(labelPanel);
		
		topLabel.setMinimumSize(new Dimension(200, 15));
		textField.setEditable(false);
		textField.setBackground(Color.white);

		okButton.setMnemonic(KeyEvent.VK_O);
		okButton.setEnabled(false);
		JLabel blank = new JLabel("    "); 
		cancelButton.setMnemonic(KeyEvent.VK_C);
		

		textField.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				enableOKButton();
			}
			public void removeUpdate(DocumentEvent e) {
				enableOKButton();
			}
			public void insertUpdate(DocumentEvent e) {
				enableOKButton();
			}

			public void enableOKButton() {
				if (textField.getText() != null && textField.getText().length() > 0) {
					okButton.setEnabled(true);
					//LocalConfig.getInstance().hasMetabolitesFile = true;
				} else {
					//LocalConfig.getInstance().hasMetabolitesFile = false;
				}
			}
		});
		
		JPanel textPanel = new JPanel();
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.X_AXIS));
		textPanel.add(textField);
		textPanel.setBorder(BorderFactory.createEmptyBorder(10,20,10,20));

		JLabel blank2 = new JLabel("      ");
		JLabel blank3 = new JLabel("      ");
		
		hbMetab.add(blank2);
		hbMetab.add(fileButton);
		hbMetab.add(textPanel);
		hbMetab.add(clearButton);
		hbMetab.add(blank3);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		buttonPanel.add(okButton);
		buttonPanel.add(blank);
		buttonPanel.add(cancelButton);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5,20,15,20));

		hbButton.add(buttonPanel);

		vb.add(hbLabel);
		vb.add(hbMetab);
		vb.add(hbButton);
		add(vb);	
		
		ActionListener fileButtonActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent prodActionEvent) {
				/*
				setAlwaysOnTop(false);
				if (!fileSelected) {
					JTextArea output = null;
					JFileChooser fileChooser = new JFileChooser(); 
					fileChooser.setDialogTitle("Browse For Gurobi Path");
					fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);				
					
					fileChooser.setCurrentDirectory(new File("C:\\"));
					
					//... Open a file dialog.
					int retval = fileChooser.showOpenDialog(output);
					if (retval == JFileChooser.APPROVE_OPTION) {
						//... The user selected a file, get it, use it.          	
						File file = fileChooser.getSelectedFile();
						String rawPathName = file.getAbsolutePath();
						textField.setText(rawPathName);	
						setPath(rawPathName);
						fileSelected = true;
					}			
				}
				setAlwaysOnTop(true);
				*/
			}
		};
		
		ActionListener okButtonActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent prodActionEvent) {
				/*
				String lastGurobi_path = GraphicalInterface.curSettings.get("LastGurobi");
				if (lastGurobi_path == null) {
					lastGurobi_path = ".";
				}
				GraphicalInterface.curSettings.add("LastGurobi", getPath());
				setVisible(false);
				dispose();
				*/
			}
		};
		
		ActionListener cancelButtonActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent prodActionEvent) {
				/*
				JOptionPane.showMessageDialog(null,                
						GraphicalInterfaceConstants.NO_GUROBI_PATH_ERROR,                
						"No Gurobi Path",                                
						JOptionPane.ERROR_MESSAGE);
				setVisible(false);
				dispose();
				*/
			}
		}; 
		
		ActionListener clearButtonActionListener = new ActionListener() {
			public void actionPerformed(ActionEvent prodActionEvent) {
				textField.setText("");	 
			}
		}; 
		
		fileButton.addActionListener(fileButtonActionListener);
		okButton.addActionListener(okButtonActionListener);
		cancelButton.addActionListener(cancelButtonActionListener);
		clearButton.addActionListener(clearButtonActionListener);
		
	} 	
	
	public static void main(String[] args) throws Exception {
		curSettings = new SettingsFactory();
		
		//based on code from http://stackoverflow.com/questions/6403821/how-to-add-an-image-to-a-jframe-title-bar
		final ArrayList<Image> icons = new ArrayList<Image>(); 
		icons.add(new ImageIcon("images/most16.jpg").getImage()); 
		icons.add(new ImageIcon("images/most32.jpg").getImage());

		String lastGurobi_path = curSettings.get("LastGurobi");
		if (lastGurobi_path == null) {
			GurobiPathInterface frame = new GurobiPathInterface();
			frame.setIconImages(icons);
			frame.setSize(600, 150);
			frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		} else {
			System.out.println("gui load");
		}
	}
}









