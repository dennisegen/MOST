package edu.rutgers.MOST.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SBMLMetabolite implements ModelMetabolite {

	private String databaseName;	
	private Integer id;
	private String metaboliteAbbreviation;
	private String metaboliteName;
	private String compartment;
	private String charge;	
	private String boundary;
	private String meta1;
	private String meta2;
	private String meta3;
	private String meta4;
	private String meta5;
	private String meta6;
	private String meta7;
	private String meta8;
	private String meta9;
	private String meta10;
	private String meta11;
	private String meta12;
	private String meta13;
	private String meta14;
	private String meta15;

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setMetaboliteAbbreviation(String metaboliteAbbreviation) {
		this.metaboliteAbbreviation = metaboliteAbbreviation;
	}
	public String getMetaboliteAbbreviation() {
		return metaboliteAbbreviation;
	}

	public String getMetaboliteName() {
		return metaboliteName;
	}
	public void setMetaboliteName(String metaboliteName) {
		this.metaboliteName = metaboliteName;
	}
	public String getCompartment() {
		return compartment;
	}
	public void setCompartment(String compartment) {
		this.compartment = compartment;
	}
	public String getCharge() {
		return charge;
	}
	public void setCharge(String charge) {
		this.charge = charge;
	}	

	public String getBoundary() {
		return boundary;
	}
	public void setBoundary(String boundary) {
		this.boundary = boundary;
	}


	// SQL Persistence/ORM Below:

	public boolean update() {

		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		Connection conn;
		try {
			conn = DriverManager.getConnection(createConnectionStatement(getDatabaseName())); // TODO:

			PreparedStatement prep = conn
			.prepareStatement("update metabolites set metabolite_abbreviation=?, metabolite_name=?, charge=?, " 
					+ " compartment=?, boundary=?, meta_1=?, meta_2=?, meta_3=?, meta_4=?, meta_5=?, "
					+ " meta_6=?, meta_7=?, meta_8=?, meta_9=?, meta_10=?, "
					+ " meta_11=?, meta_12=?, meta_13=?, meta_14=?, meta_15=? where id=?;");
			prep.setString(1, this.getMetaboliteAbbreviation());
			prep.setString(2, this.getMetaboliteName());
			prep.setString(3, this.getCharge());
			prep.setString(4, this.getCompartment());	
			prep.setString(5, this.getBoundary());
			prep.setString(6, this.getMeta1());
			prep.setString(7, this.getMeta2());
			prep.setString(8, this.getMeta3());
			prep.setString(9, this.getMeta4());
			prep.setString(10, this.getMeta5());
			prep.setString(11, this.getMeta6());
			prep.setString(12, this.getMeta7());
			prep.setString(13, this.getMeta8());
			prep.setString(14, this.getMeta9());
			prep.setString(15, this.getMeta10());
			prep.setString(16, this.getMeta11());
			prep.setString(17, this.getMeta12());
			prep.setString(18, this.getMeta13());
			prep.setString(19, this.getMeta14());
			prep.setString(20, this.getMeta15());
			prep.setInt(21, this.getId());
			conn.setAutoCommit(true);
			prep.executeUpdate();

			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean loadById(Integer id) {

		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		Connection conn;
		try {
			conn = DriverManager.getConnection(createConnectionStatement(getDatabaseName())); // TODO:
			// Make
			// this
			// configurable
			PreparedStatement prep = conn
			.prepareStatement("select id, metabolite_abbreviation, metabolite_name, charge, compartment, " 
					+ " boundary, meta_1, meta_2, meta_3, meta_4, meta_5, "
					+ " meta_6, meta_7, meta_8, meta_9, meta_10, "
					+ " meta_11, meta_12, meta_13, meta_14, meta_15 from metabolites where id = ?;");
			prep.setInt(1, id);
			conn.setAutoCommit(true);
			ResultSet rs = prep.executeQuery();
			while (rs.next()) {
				this.setId(rs.getInt("id"));
				this.setMetaboliteAbbreviation(rs.getString("metabolite_abbreviation"));
				this.setMetaboliteName(rs.getString("metabolite_name"));
				this.setCharge(rs.getString("charge"));			
				this.setCompartment(rs.getString("compartment"));
				this.setBoundary(rs.getString("boundary"));
				this.setMeta1(rs.getString("meta_1"));
				this.setMeta2(rs.getString("meta_2"));
				this.setMeta3(rs.getString("meta_3"));
				this.setMeta4(rs.getString("meta_4"));
				this.setMeta5(rs.getString("meta_5"));
				this.setMeta6(rs.getString("meta_6"));
				this.setMeta7(rs.getString("meta_7"));
				this.setMeta8(rs.getString("meta_8"));
				this.setMeta9(rs.getString("meta_9"));
				this.setMeta10(rs.getString("meta_10"));
				this.setMeta11(rs.getString("meta_11"));
				this.setMeta12(rs.getString("meta_12"));
				this.setMeta13(rs.getString("meta_13"));
				this.setMeta14(rs.getString("meta_14"));
				this.setMeta15(rs.getString("meta_15"));
			}
			rs.close();
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		return "SBMLMetabolite [id=" + id + ", metaboliteAbbreviation=" + metaboliteAbbreviation
		+ ", compartment=" + compartment
		+ ", charge=" + charge
		+ ", metaboliteName=" + metaboliteName + "]";
	}

	public String createConnectionStatement(String databaseName) {
		return "jdbc:sqlite:" + getDatabaseName() + ".db";
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public void setMeta1(String meta1) {
		this.meta1 = meta1;
	}

	public String getMeta1() {
		return meta1;
	}

	public void setMeta2(String meta2) {
		this.meta2 = meta2;
	}

	public String getMeta2() {
		return meta2;
	}

	public void setMeta3(String meta3) {
		this.meta3 = meta3;
	}

	public String getMeta3() {
		return meta3;
	}

	public void setMeta4(String meta4) {
		this.meta4 = meta4;
	}

	public String getMeta4() {
		return meta4;
	}

	public void setMeta5(String meta5) {
		this.meta5 = meta5;
	}

	public String getMeta5() {
		return meta5;
	}

	public void setMeta6(String meta6) {
		this.meta6 = meta6;
	}

	public String getMeta6() {
		return meta6;
	}

	public void setMeta7(String meta7) {
		this.meta7 = meta7;
	}

	public String getMeta7() {
		return meta7;
	}

	public void setMeta8(String meta8) {
		this.meta8 = meta8;
	}

	public String getMeta8() {
		return meta8;
	}

	public void setMeta9(String meta9) {
		this.meta9 = meta9;
	}

	public String getMeta9() {
		return meta9;
	}

	public void setMeta10(String meta10) {
		this.meta10 = meta10;
	}

	public String getMeta10() {
		return meta10;
	}

	public void setMeta11(String meta11) {
		this.meta11 = meta11;
	}

	public String getMeta11() {
		return meta11;
	}

	public void setMeta12(String meta12) {
		this.meta12 = meta12;
	}

	public String getMeta12() {
		return meta12;
	}

	public void setMeta13(String meta13) {
		this.meta13 = meta13;
	}

	public String getMeta13() {
		return meta13;
	}

	public void setMeta14(String meta14) {
		this.meta14 = meta14;
	}

	public String getMeta14() {
		return meta14;
	}

	public void setMeta15(String meta15) {
		this.meta15 = meta15;
	}

	public String getMeta15() {
		return meta15;
	}

}
