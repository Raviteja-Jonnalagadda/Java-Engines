import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ResourceBundle;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * SmartCrudEngine provides dynamic SQL query generation and execution for CRUD operations.
 * <br>
 * It supports insert, select, update, and delete operations based on JSON input.
 * 
 * @author Raviteja J
 * @date 12-07-2025
 */
 
public class SmartCrudEngine {
	protected static StringBuilder allclm = new StringBuilder();
	protected static StringBuilder allval = new StringBuilder();
	protected static ResourceBundle rb = ResourceBundle.getBundle("dbdetails");
	protected static String dbdriver = "oracle.jdbc.driver.OracleDriver";
	protected static String dburl = "jdbc:oracle:thin:@//172.22.24.14:9876/ysedev10";
	protected static String dbunm = "DICICICUDB";
	protected static String dbpwd = "DICICICUDB";
	
	
    /**
     * Executes a CRUD operation based on the provided JSON object.
     * Determines operation type (insert, select, update, delete) and calls respective methods.
     * 
     * @param val JSON object containing operation details
     * @return Resulting SQL query string
     * @throws Exception if input is invalid or processing fails
     * @author Raviteja J
     */
	public static String executer(JSONObject val) throws Exception {
		String result = null;
		if (val.equals(null) || val.isEmpty() || val.length() <= 0) {
			throw new Exception("Cant Process Null Values ");
		}
		if (val.has("main_sign")) {
			if (val.optString("main_sign", "").equalsIgnoreCase("insert")) {
				result = getinsertq(val);
			} else if (val.optString("main_sign", "").equalsIgnoreCase("select")) {
				result = getselectq(val);
			} else if (val.optString("main_sign", "").equalsIgnoreCase("delete")) {
				result = getdeleteq(val);
			} else if (val.optString("main_sign", "").equalsIgnoreCase("update")) {
				result = getupdateq(val);
			}
		}
		return result;
	}

    /**
     * Generates an INSERT SQL query from the provided JSON object.
     * 
     * @param jval JSON object containing table name and data
     * @return INSERT SQL query string
     * @throws Exception if required fields are missing
     * @author Raviteja J
     */
	public static String getinsertq(JSONObject jval) throws Exception {
		allclm.setLength(0);
		allval.setLength(0);
		if (!jval.has("qtn") || !jval.has("qdt")) {
			throw new Exception("Some of the important fields are missing");
		}
		String tableName = jval.getString("qtn").toUpperCase();
		JSONObject tdtaObject = jval.getJSONObject("qdt");
		Set<String> keys = tdtaObject.keySet();
		for (String key : keys) {
			allclm.append(key).append(",");
			String rawValue = tdtaObject.getString(key);
			String escapedValue = rawValue.replace("'", "''");
			allval.append("'").append(escapedValue).append("',");
		}
		String columns = allclm.substring(0, allclm.length() - 1).toUpperCase();
		String values = allval.substring(0, allval.length() - 1).toUpperCase();

		String query = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";
		// System.out.println("Query is ---> " + query);
		return query;
	}

    /**
     * Generates a SELECT SQL query from the provided JSON object.
     * 
     * @param jval JSON object containing table name, columns, and condition
     * @return SELECT SQL query string
     * @throws Exception if required fields are missing
     * @author Raviteja J
     */
	public static String getselectq(JSONObject jval) throws Exception {
		allclm.setLength(0);

		if (!jval.has("qtn") || !jval.has("qcl") || !jval.has("qcn")) {
			throw new Exception("Some of the important fields are missing");
		}
		JSONArray qcolm = jval.getJSONArray("qcl");
		for (int i = 0; i < qcolm.length(); i++) {
			allclm.append(qcolm.get(i).toString() + ",");
		}
		String table = jval.getString("qtn").toUpperCase();
		String coloums = allclm.substring(0, allclm.length() - 1).toUpperCase();
		String condition = jval.getString("qcn").toUpperCase();
		String query = "SELECT " + coloums + " FROM " + table + " " + condition;
		// System.out.println("Query is ---> " + query);
		return query;
	}
	
    /**
     * Generates a DELETE SQL query from the provided JSON object.
     * 
     * @param jval JSON object containing table name and condition
     * @return DELETE SQL query string
     * @throws Exception if required fields are missing
     * @author Raviteja J
     */
	public static String getdeleteq(JSONObject jval) throws Exception {
		String qtyp = null;
		String query = null;
		if (!jval.has("qtn") || !jval.has("qcn")) {
			throw new Exception("Some of the important fields are missing");
		}
		String qcon = jval.getString("qcn").toUpperCase();
		qtyp = (qcon == null || qcon.trim().isEmpty()) ? "PLANE" : "CONDT";
		String qtab = jval.getString("qtn").toUpperCase();
		if(qtyp.equalsIgnoreCase("PLANE")) {
			query = "DELETE FROM "+qtab;
		}
		else if(qtyp.equalsIgnoreCase("CONDT")) {
			query = "DELETE FROM "+qtab+" "+qcon;
		}
		// System.out.println("Query is ---> " + query);
		return query;
	}
	
    /**
     * Generates an UPDATE SQL query from the provided JSON object.
     * 
     * @param jval JSON object containing table name, data, and condition
     * @return UPDATE SQL query string
     * @throws Exception if required fields are missing
     * @author Raviteja J
     */
	public static String getupdateq(JSONObject jval) throws Exception {
		allclm.setLength(0);
		String qtyp = null;
		String query = null;
		if (!jval.has("qtn") || !jval.has("qdt")) {
			throw new Exception("Some of the important fields are missing");
		}
		JSONObject tjo = jval.getJSONObject("qdt");
		Set<String> colkey=tjo.keySet();
		for(String key : colkey) {
			String tempval = tjo.getString(key);
			String actval = tempval.replace("'", "''");
			allclm.append(key+" = "+"'"+actval+"'"+" ,");
		}
		String qdt = allclm.substring(0,allclm.length()-2).toUpperCase();
		String qcon = jval.getString("qcn").toUpperCase();
		qtyp = (qcon == null ||qcon.trim().isEmpty())?"PLANE":"CONDT";
		String qtab = jval.getString("qtn").toUpperCase();
		if(qtyp.equalsIgnoreCase("PLANE")) {
			query = "UPDATE "+qtab+" ("+qdt+")";
		}
		else if(qtyp.equalsIgnoreCase("CONDT")) {
			query = "UPDATE "+qtab+" SET "+qdt+" WHERE "+qcon;
		}
		// System.out.println("Query is ---> " + query);
		return query;
	}
	
    /**
     * Executes the provided SQL query against the database.
     * Supports insert, update, delete, and select operations.
     * 
     * @param query SQL query string to execute
     * @param qtyp Type of query (INSERT, UPDATE, DELETE, SELECT)
     * @return Result of the execution as a string
     * @throws ClassNotFoundException if database driver class is not found
     * @author Raviteja J
     */
	public static String dbexecuter(String query,String qtyp) throws ClassNotFoundException {
		Class.forName(dbdriver);
		int rowsAffected = 0;
		@SuppressWarnings("unused")
		String result = null;
		JSONObject qrst= new JSONObject();
		try(Connection cn = DriverManager.getConnection(dburl, dbpwd, dbdriver);
				PreparedStatement ps = cn.prepareStatement(query);
				){
			if(qtyp.equalsIgnoreCase("INSERT")||qtyp.equalsIgnoreCase("Update")||qtyp.equalsIgnoreCase("Delete")) {
				rowsAffected = ps.executeUpdate();
				qrst.put("sign", "DONE");
				qrst.put("executed_cmd", qtyp.toUpperCase());
				qrst.put("effected_row", rowsAffected);
				qrst.put("message", "["+qtyp.toUpperCase()+"] Command is executed Number of rows effected is ["+rowsAffected+"]");
				result = "["+qtyp.toUpperCase()+"] Command is executed Number of rows effected is ["+rowsAffected+"]";
			}
			else if(qtyp.equalsIgnoreCase("SELECT")) {
				ResultSet rs = ps.executeQuery();
				ResultSetMetaData rmd = rs.getMetaData();
				int rssize = rmd.getColumnCount();
				qrst.put("sign", "DONE");
				qrst.put("executed_cmd", qtyp.toUpperCase());
				qrst.put("effected_row", rowsAffected);
				qrst.put("message", "["+qtyp.toUpperCase()+"] Command is executed Number of rows Selectd is ["+rssize+"]");
				JSONObject qrdt= new JSONObject();
				for(int i=0;i<rssize;i++) {
					while(rs.next()) {
						String tempcol = rmd.getColumnName(i);
						qrdt.put(tempcol, rs.getString(tempcol));
					}
				}
				qrst.put("query_data", qrdt);
				result =qrst.toString();
			}
			else {
				qrst.put("sign", "FAIL");
				qrst.put("executed_cmd", qtyp.toUpperCase());
				qrst.put("effected_row", rowsAffected);
				qrst.put("message", "["+qtyp.toUpperCase()+"] Unknown CRUD Command is given ");
				result = "Unknown CRUD command";
			}
		}catch(Exception e) {
			
		}
		
		
		return "";
	}

    /**
     * Main method for testing the engine.
     * Demonstrates an insert operation with sample JSON.
     * 
     * @param args Command-line arguments
     * @throws Exception if execution fails
     * @author Raviteja J
     */
	public static void main(String[] args) throws Exception {
		 String jsonval = "{\"main_sign\":\"insert\",\"qtn\":\"YourTableName\",\"qdt\":{\"column1\":\"value1\",\"column2\":\"value2\",\"column3\":\"value3\"}}";
		//String jsonval ="{\"main_sign\":\"select\",\"qtn\":\"YourTableName\",\"qcl\":[col1,col2,col3,col4,col5,col6],\"qcn\":\"WHERE col1 is not null\" }";
		//String jsonval = "{\"main_sign\":\"delete\",\"qtn\":\"YourTableName\",\"qcn\":\"where col1 is not null\"}";
		//String jsonval = "{\"main_sign\":\"update\",\"qtn\":\"YourTableName\",\"qdt\":{\"column1\":\"value1\",\"column2\":\"value2\",\"column3\":\"select 'raviteja' from dual\"},\"qcn\":\" col1 is not null\"}";
		JSONObject jo = new JSONObject(jsonval);
		System.out.println(executer(jo));
	}

}
