package edu.rutgers.MOST.data;

public class SettingsTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		Settings set = new Settings();
		//set.writeMethod1();
		set.read();
		System.out.println(set.lastL_SBML);
	}

}
