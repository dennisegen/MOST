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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class Settings {
	public String lastL_SBML;
	public String lastS_SBML;
	
	public String lastL_CSVR;
	public String lastS_CSVM;
	
	public String lastL_SQL;
	public String lastS_SQL;
	
	
	public void writeMethod1() throws Exception {
		lastL_SBML = "testthis, yep";
		
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

	    XMLEventWriter writer = outputFactory.createXMLEventWriter(new FileOutputStream("settings.xml"));
	    
	    
	    XMLEventFactory xmlEventFactory = XMLEventFactory.newInstance();
	    XMLEvent end = xmlEventFactory.createDTD("\n");

	    StartDocument startDocument = xmlEventFactory.createStartDocument("UTF-8", "1.0");
	    writer.add(startDocument);
	    writer.add(end);

	    StartElement startElement = xmlEventFactory.createStartElement("", "", "Settings");
	    writer.add(startElement);

	    Attribute attribute = xmlEventFactory.createAttribute("version", "1");
	    List attributeList = Arrays.asList(attribute);
	    List nsList = Arrays.asList();
	    
	    StartElement startElement2 = xmlEventFactory.createStartElement("", "", "Attributes",
	        attributeList.iterator(), nsList.iterator());
	    
	 
	    writer.add(startElement2);
	    

	    StartElement codeSE = xmlEventFactory.createStartElement("", "", "LastLoadedSBML");
	    writer.add(codeSE);
	    
	    
	    Characters codeChars = xmlEventFactory.createCharacters(lastL_SBML);
	    writer.add(codeChars);
	    EndElement codeEE = xmlEventFactory.createEndElement("", "", "LastLoadedSBML");
	    writer.add(codeEE);

	    

	    StartElement nameSE = xmlEventFactory.createStartElement("", "", "name");
	    writer.add(nameSE);
	    Characters nameChars = xmlEventFactory.createCharacters("a name");
	    writer.add(nameChars);
	    EndElement nameEE = xmlEventFactory.createEndElement("", "", "name");
	    writer.add(nameEE);


	    EndDocument ed = xmlEventFactory.createEndDocument();
	    writer.add(ed);

	    writer.flush();
	    writer.close();
	}
	
	
	public void setlastL_SBML(String value) {
    	this.lastL_SBML = value;
    }
    
    public void setlastS_SBML(String value) {
    	
    }
    
    
    public void setlastL_CSVR(String value) {
    	
    }
	
	public void read() throws Exception {
			XMLInputFactory factory = XMLInputFactory.newInstance();
		    FileReader fileReader = new FileReader("settings.xml");
		    XMLEventReader reader = factory.createXMLEventReader(fileReader);
		    String currentElementValue = "";
		    
		    while (reader.hasNext()) {
		      XMLEvent event = reader.nextEvent();
		      if (event.isStartElement()) {
		        StartElement element = (StartElement) event;
		        currentElementValue = element.getName().toString();
		        
		        
		        System.out.println("Start Element: " + element.getName());

		        Iterator iterator = element.getAttributes();
		        while (iterator.hasNext()) {
		          Attribute attribute = (Attribute) iterator.next();
		          QName name = attribute.getName();
		          String value = attribute.getValue();
		          System.out.println("Attribute name/value: " + name + "/" + value);
		        }
		      }
		      
		      if (event.isEndElement()) {
		        EndElement element = (EndElement) event;
		        System.out.println("End element:" + element.getName());
		      }
		      
		      if (event.isCharacters()) {
		        Characters characters = (Characters) event;
		        if (currentElementValue == "LastLoadedSBML" ) {
		        	this.setlastL_SBML(characters.getData());
		        	currentElementValue = "";
		        }
		        System.out.println("Text: " + characters.getData());
		      }
		    }
	}
}




