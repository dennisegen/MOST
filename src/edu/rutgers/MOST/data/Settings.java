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
	
	
	public void write() throws Exception {
		lastL_SBML = "testthis, yep";
		
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

	    XMLEventWriter writer = outputFactory.createXMLEventWriter(new FileOutputStream("settings.xml"));
	    
	    
	    XMLEventFactory xmlEventFactory = XMLEventFactory.newInstance();
	    XMLEvent end = xmlEventFactory.createDTD("\n");

	    StartDocument startDocument = xmlEventFactory.createStartDocument("UTF-8", "1.0");
	    writer.add(startDocument);

	    StartElement startElement = xmlEventFactory.createStartElement("", "", "My-list");
	    writer.add(startElement);

	    Attribute attribute = xmlEventFactory.createAttribute("version", "1");
	    List attributeList = Arrays.asList(attribute);
	    List nsList = Arrays.asList();
	    
	    StartElement startElement2 = xmlEventFactory.createStartElement("", "", "Item",
	        attributeList.iterator(), nsList.iterator());
	    
	    writer.add(startElement2);

	    StartElement codeSE = xmlEventFactory.createStartElement("", "", "code");
	    writer.add(codeSE);
	    Characters codeChars = xmlEventFactory.createCharacters("I001");
	    writer.add(codeChars);
	    EndElement codeEE = xmlEventFactory.createEndElement("", "", "code");
	    writer.add(codeEE);

	    StartElement nameSE = xmlEventFactory.createStartElement(" ", " ", "name");
	    writer.add(nameSE);
	    Characters nameChars = xmlEventFactory.createCharacters("a name");
	    writer.add(nameChars);
	    EndElement nameEE = xmlEventFactory.createEndElement("", "", "name");
	    writer.add(nameEE);

	    StartElement contactSE = xmlEventFactory.createStartElement("", "", "Last Loaded SBML");
	    writer.add(contactSE);
	    Characters contactChars = xmlEventFactory.createCharacters(lastL_SBML);
	    writer.add(contactChars);
	    EndElement contactEE = xmlEventFactory.createEndElement("", "", "contact");
	    writer.add(contactEE);

	    EndDocument ed = xmlEventFactory.createEndDocument();
	    writer.add(ed);

	    writer.flush();
	    writer.close();
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
	
	public void setlastL_SBML(String value) {
    	
    }
    
    public void setlastS_SBML(String value) {
    	
    }
    
    
    public void setlastL_CSVR(String value) {
    	
    }
	
	public void read() throws Exception {
			XMLInputFactory factory = XMLInputFactory.newInstance();
		    FileReader fileReader = new FileReader("settings.xml");
		    XMLEventReader reader = factory.createXMLEventReader(fileReader);

		    while (reader.hasNext()) {
		      XMLEvent event = reader.nextEvent();
		      if (event.isStartElement()) {
		        StartElement element = (StartElement) event;
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
		        System.out.println("Text: " + characters.getData());
		      }
		    }
	}
}




