package edu.rutgers.MOST.presentation;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import edu.rutgers.MOST.config.LocalConfig;

import java.io.*;

//based on code from http://leepoint.net/notes-java/examples/components/editor/nutpad.html
public class OutputPopout extends JFrame {

	private static JTextArea    textArea;
	private JFileChooser fileChooser = new JFileChooser(new java.io.File("."));
	private final JMenuItem outputCopyItem = new JMenuItem("Copy");
	private final JMenuItem outputSelectAllItem = new JMenuItem("Select All");

	public OutputPopout() {
		//... Create scrollable text area.
		textArea = new JTextArea(30, 60);
		textArea.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		//textArea.setFont(new Font("monospaced", Font.PLAIN, 14));
		textArea.setEditable(false);
		JScrollPane scrollingText = new JScrollPane(textArea);

		JPanel content = new JPanel();
		content.setLayout(new BorderLayout());
		content.add(scrollingText, BorderLayout.CENTER);

		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = menuBar.add(new JMenu("File"));
		fileMenu.setMnemonic('F');
		JMenuItem openItem = new JMenuItem("Open");
		fileMenu.add(openItem);
		openItem.setMnemonic('O');
		openItem.addActionListener(new OpenAction()); 
		JMenuItem saveAsItem = new JMenuItem("Save As...");
		fileMenu.add(saveAsItem);
		saveAsItem.setMnemonic('A');
		saveAsItem.addActionListener(new SaveAction());
		fileMenu.addSeparator();
		JMenuItem exitItem = new JMenuItem("Exit");
		fileMenu.add(exitItem);
		exitItem.setMnemonic('X');
		exitItem.addActionListener(new ExitAction());

		setContentPane(content);
		setJMenuBar(menuBar);

		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setTitle(GraphicalInterfaceConstants.TITLE + " - " + LocalConfig.getInstance().getLoadedDatabase());
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
		
		final JPopupMenu outputPopupMenu = new JPopupMenu(); 
		outputPopupMenu.add(outputCopyItem);
		outputCopyItem.setEnabled(false);
		outputCopyItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) { 	
				setClipboardContents(textArea.getSelectedText());							
			}
		});
		outputPopupMenu.add(outputSelectAllItem);
		outputSelectAllItem.setEnabled(false);
		outputSelectAllItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) { 
				textArea.selectAll();							
			}
		});
		
		textArea.addMouseListener(new MouseAdapter() {

			public void mousePressed(MouseEvent e)  {check(e);}
			public void mouseReleased(MouseEvent e) {check(e);}

			public void check(MouseEvent e) {
				if (e.isPopupTrigger()) { //if the event shows the menu
					outputPopupMenu.show(textArea, e.getX(), e.getY()); 
				}
			}
		});	

		textArea.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				fieldChangeAction();
			}
			public void removeUpdate(DocumentEvent e) {
				fieldChangeAction();
			}
			public void insertUpdate(DocumentEvent e) {
				fieldChangeAction();
			}
			public void fieldChangeAction() {
				if (textArea.getText().length() > 0) {
					outputCopyItem.setEnabled(true);
					outputSelectAllItem.setEnabled(true);
				} else {
					outputCopyItem.setEnabled(false);
					outputSelectAllItem.setEnabled(false);
				}
			}
		});
	}

	class OpenAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			int retval = fileChooser.showOpenDialog(OutputPopout.this);
			if (retval == JFileChooser.APPROVE_OPTION) {
				File f = fileChooser.getSelectedFile();
				String filename = fileChooser.getSelectedFile().getName();
				setTitle(GraphicalInterfaceConstants.TITLE + " - " + filename);
				try {
					FileReader reader = new FileReader(f);
					textArea.read(reader, "");  // Use TextComponent read
				} catch (IOException ie) {
					System.exit(1);
				}
			}
		}
	}

	class SaveAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {			
			boolean done = false;
			while (!done) {
				int retval = fileChooser.showSaveDialog(OutputPopout.this);
				if (retval == JFileChooser.CANCEL_OPTION) {
					done = true;
				}
				if (retval == JFileChooser.APPROVE_OPTION) {					
					String path = fileChooser.getSelectedFile().getPath();
					if (!path.endsWith(".txt")) {
						path = path + ".txt";
					}
					File f = new File(path);

					if (path == null) {
						done = true;
					} else {        	    	  
						if (f.exists()) {
							int confirmDialog = JOptionPane.showConfirmDialog(fileChooser, "Replace existing file?");
							if (confirmDialog == JOptionPane.YES_OPTION) {
								done = true;
								writeFile(f);
							} else if (confirmDialog == JOptionPane.NO_OPTION) {        		    	  
								done = false;
							} else {
								done = true;
							}       		    	  
						} else {
							done = true;
							writeFile(f);
						}
					}	
				}
			}
		}
	}
	
	public void writeFile(File f) {
		try {
			FileWriter writer = new FileWriter(f);
			textArea.write(writer);  // Use TextComponent write
		} catch (IOException ie) {
			JOptionPane.showMessageDialog(OutputPopout.this, ie);
			System.exit(1);
		}
	}
	
	//based on http://www.java2s.com/Code/Java/File-Input-Output/Textfileviewer.htm
	public void load(String path) {
		File file;
		FileReader in = null;

		try {
			file = new File(path); 
			in = new FileReader(file); 
			char[] buffer = new char[4096]; // Read 4K characters at a time
			int len; 
			textArea.setText("");  	     
			while ((len = in.read(buffer)) != -1) { // Read a batch of chars
				String s = new String(buffer, 0, len); 
				textArea.append(s); 
			}
			textArea.setCaretPosition(0); 
			setTitle(GraphicalInterfaceConstants.TITLE + " - " + path);
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

	public void clear() {
		textArea.setText(""); 
		setTitle(LocalConfig.getInstance().getDatabaseName());
	}

	class ExitAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			setVisible(false);
		}
	}

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
	
	public static void main(String[] args) {
		new OutputPopout();
		//loadOutputPane("C://CMakeCache.txt");
	}
}


