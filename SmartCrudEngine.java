package com.chat.db;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * SmartCrudEngine provides dynamic SQL query generation and execution for CRUD
 * operations. <br>
 * It supports insert, select, update, and delete operations based on JSON
 * input. 
 * 
 * @author Raviteja J
 * @date 12-07-2025
 */
public class SmartCrudEngine {

	protected static StringBuilder allclm = new StringBuilder();
	protected static StringBuilder allval = new StringBuilder();
	protected static ResourceBundle rb = ResourceBundle.getBundle("dbdetails");
	protected static String dbdriver = rb.getString("dbdrv");
	protected static String dburl = rb.getString("dburl");
	protected static String dbunm = rb.getString("dbunm");
	protected static String dbpwd = rb.getString("dbpwd");

	/**
	 * Executes a CRUD operation based on the provided JSON object. Determines
	 * operation type (insert, select, update, delete) and calls respective methods.
	 * 
	 * @param val JSON object containing operation details
	 * @return Resulting SQL query string
	 * @throws Exception if input is invalid or processing fails
	 * @author Raviteja J
	 */
	public static String executer(JSONObject val) throws Exception {
		if (val == null || val.isEmpty()) {
			throw new Exception("Cannot process null or empty JSON");
		}

		String result = null;
		String query = null;
		String exresult = null;
		String type = val.optString("main_sign", "").toLowerCase();
		String rtp = val.optString("rtp", "query").toLowerCase();
		switch (type) {
		case "insert":
			query = getinsertq(val);
			if ("execute".equals(rtp)) {
				exresult = dbexecuter(query, type);
				result = exresult;
			} else {
				result = query;
			}
			break;

		case "select":
			query = getinsertq(val);

			if ("execute".equals(rtp)) {
				exresult = dbexecuter(query, type);
				result = exresult;
			} else {
				result = query;
			}
			break;
		case "update":
			query = getupdateq(val);
			if ("execute".equals(rtp)) {
				exresult = dbexecuter(query, type);
				result = exresult;
			} else {
				result = query;
			}
			break;
		case "delete":
			query = getdeleteq(val);
			if ("execute".equals(rtp)) {
				exresult = dbexecuter(query, type);
				result = exresult;
			} else {
				result = query;
			}
			break;
		default:
			throw new Exception("Unsupported operation: " + type);
		}

		return result;
	}

	/**
	 * Processes a JSON string input to dynamically build and execute a parameterized SQL query.
	 * <p>
	 * The input JSON must contain the following keys:
	 * <ul>
	 *     <li><b>raw_qry</b> – the SQL query with placeholders (e.g., {@code SELECT * FROM table WHERE id = {ID}})</li>
	 *     <li><b>params</b> – a JSON object containing the actual values to replace the placeholders in {@code raw_qry}</li>
	 *     <li><b>rtp</b> – return type. If it's {@code "execute"}, the query is executed against the database</li>
	 *     <li><b>main_sign</b> – the main operation keyword (e.g., SELECT, INSERT, UPDATE, DELETE)</li>
	 * </ul>
	 *
	 * @param sval A JSON string representing the query template and parameters.
	 * @return A result string:
	 *         <ul>
	 *             <li>Execution result if {@code rtp = "execute"}</li>
	 *             <li>Final SQL query string if {@code rtp != "execute"}</li>
	 *             <li>Error JSON string if validation or execution fails</li>
	 *         </ul>
	 * 
	 * @see #errormsgbuilder(String, String)
	 * @author RAVITEJA J
	 */
	public static String paramexecuter(String sval) {
		String result = null;
		String rawQuery = null;
		String return_type = null;
		String main_sign = null;
		JSONObject params = new JSONObject();
		JSONObject val = new JSONObject(sval);
		if (val.isEmpty() || val == null) {
			return errormsgbuilder("JSNNUL", "Json Object is null. Cant Process Null Json .").toString();
		}
		if (!val.has("raw_qry") || !val.has("params") || !val.has("rtp")) {
			return errormsgbuilder("JSPRNL", "JSON Paramaters Are null. Cant Process Null values .").toString();
		}
		rawQuery = val.optString("raw_qry", "NO_DATA");
		return_type = val.optString("rtp", "NO_DATA");
		main_sign = val.optString("main_sign");
		params = val.getJSONObject("params");
		if (rawQuery.trim().isEmpty() || rawQuery == null || rawQuery.equalsIgnoreCase("NO_DATA")
				|| rawQuery.equalsIgnoreCase("null") || params.isEmpty() || return_type.trim().isEmpty()
				|| return_type.equalsIgnoreCase("NO_DATA") || return_type == null || main_sign.trim().isEmpty()
				|| main_sign == null || main_sign.equalsIgnoreCase("NO_DATA")||main_sign.equalsIgnoreCase("null")) {
			return errormsgbuilder("PRVLNL", "Paramaters values Are null. Cant Process Null values .").toString();
		}
		System.out.println("rawQuery -->  " + rawQuery);
		System.out.println("params -->  " + params.toString());
		Set<String> key = params.keySet();
		for (String tky : key) {
			rawQuery = rawQuery.replace("{" + tky + "}", "'" + params.get(tky) + "'");
		}
		System.out.println(rawQuery);
		if ("execute".equals(return_type)) {
			String exresult;
			try {
				exresult = dbexecuter(rawQuery, main_sign);
				result = exresult;
			} catch (ClassNotFoundException e) {
				return errormsgbuilder("DBEXER", "Error While Execution the Query [ "+e.toString()+" ]").toString();
			}
		} else {
			result = rawQuery;
		}
		return result;

	}

	/**
	 * Constructs a standardized JSON error message object.
	 * <p>
	 * The JSON contains:
	 * <ul>
	 *     <li><b>ProcessedBY</b> – Identifies the origin of the error ("Smart CRUD Engine")</li>
	 *     <li><b>Status</b> – Always set to "FAIL"</li>
	 *     <li><b>ecode</b> – The error code (defaults to "Unknown Error Code" if null/empty)</li>
	 *     <li><b>emsg</b> – The error message (defaults to "Unknown Error Message" if null/empty)</li>
	 *     <li><b>TimeStamp</b> – Time of the error in "dd-MMM-yyyy HH:mm:ss" format</li>
	 * </ul>
	 *
	 * @param code    The unique error code to identify the type of failure.
	 * @param message A human-readable message describing the error.
	 * @return A {@link org.json.JSONObject} containing the formatted error structure.
	 * @author RAVITEJA J
	 */
	public static JSONObject errormsgbuilder(String code, String message) {
		JSONObject errormsg = new JSONObject();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		String tstamp = now.format(formatter);

		errormsg.put("ProcessedBY", "Smart CRUD Engine");
		errormsg.put("Status", "FAIL");
		errormsg.put("ecode", (code.trim().isEmpty() || message == null) ? "Unknown Error Code" : code);
		errormsg.put("emsg", (message.trim().isEmpty() || message == null) ? "Unknown Error Message" : message);
		errormsg.put("TimeStamp", tstamp);

		return errormsg;
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

		if (!jval.has("qtn") || !jval.has("qdt"))
			throw new Exception("Missing table name or data");

		String table = jval.getString("qtn").toUpperCase();
		JSONObject data = jval.getJSONObject("qdt");

		for (String key : data.keySet()) {
			allclm.append(key).append(",");
			allval.append("'").append(data.getString(key).replace("'", "''")).append("',");
		}

		String columns = allclm.substring(0, allclm.length() - 1);
		String values = allval.substring(0, allval.length() - 1);

		return "INSERT INTO " + table + " (" + columns + ") VALUES (" + values + ")";
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

		if (!jval.has("qtn") || !jval.has("qcl") || !jval.has("qcn"))
			throw new Exception("Missing table name, columns, or condition");

		JSONArray cols = jval.getJSONArray("qcl");
		for (int i = 0; i < cols.length(); i++) {
			allclm.append(cols.getString(i)).append(",");
		}

		String table = jval.getString("qtn").toUpperCase();
		String condition = jval.getString("qcn");
		String columns = allclm.substring(0, allclm.length() - 1);

		return "SELECT " + columns + " FROM " + table + " " + condition;
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
		if (!jval.has("qtn") || !jval.has("qcn"))
			throw new Exception("Missing table name or condition");

		String table = jval.getString("qtn").toUpperCase();
		String condition = jval.getString("qcn").trim();

		return condition.isEmpty() ? "DELETE FROM " + table : "DELETE FROM " + table + " " + condition;
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

		if (!jval.has("qtn") || !jval.has("qdt"))
			throw new Exception("Missing table name or data");

		JSONObject data = jval.getJSONObject("qdt");
		for (String key : data.keySet()) {
			allclm.append(key).append(" = '").append(data.getString(key).replace("'", "''")).append("', ");
		}

		String updates = allclm.substring(0, allclm.length() - 2);
		String table = jval.getString("qtn").toUpperCase();
		String condition = jval.optString("qcn", "").trim();

		return condition.isEmpty() ? "UPDATE " + table + " SET " + updates
				: "UPDATE " + table + " SET " + updates + " WHERE " + condition;
	}

	/**
	 * Executes the provided SQL query against the database. Supports insert,
	 * update, delete, and select operations.
	 * 
	 * @param query SQL query string to execute
	 * @param qtyp  Type of query (INSERT, UPDATE, DELETE, SELECT)
	 * @return Result of the execution as a string
	 * @throws ClassNotFoundException if database driver class is not found
	 * @author Raviteja J
	 */
	public static String dbexecuter(String query, String qtyp) throws ClassNotFoundException {
		Class.forName(dbdriver);
		JSONObject response = new JSONObject();
		String result = "";

		System.out.println("Query received: " + query);
		System.out.println("Query type: " + qtyp);

		try (Connection cn = DriverManager.getConnection(dburl, dbunm, dbpwd);
				PreparedStatement ps = cn.prepareStatement(query)) {

			if (qtyp.equalsIgnoreCase("INSERT") || qtyp.equalsIgnoreCase("UPDATE") || qtyp.equalsIgnoreCase("DELETE")) {
				int rows = ps.executeUpdate();
				response.put("sign", "DONE");
				response.put("executed_cmd", qtyp.toUpperCase());
				response.put("effected_row", rows);
				response.put("message", "[" + qtyp.toUpperCase() + "] executed. Rows affected: " + rows);
				result = response.toString();
			} else if (qtyp.equalsIgnoreCase("SELECT")) {
				ResultSet rs = ps.executeQuery();
				ResultSetMetaData meta = rs.getMetaData();
				int colCount = meta.getColumnCount();
				JSONArray data = new JSONArray();

				while (rs.next()) {
					JSONObject row = new JSONObject();
					for (int i = 1; i <= colCount; i++) {
						row.put(meta.getColumnName(i), rs.getObject(i));
					}
					data.put(row);
				}

				response.put("sign", "DONE");
				response.put("executed_cmd", qtyp.toUpperCase());
				response.put("effected_row", data.length());
				response.put("message", "[" + qtyp.toUpperCase() + "] executed. Rows fetched: " + data.length());
				response.put("query_data", data);
				result = response.toString();

				// System.out.println("Query Result: " + result);
			} else {
				response.put("sign", "FAIL");
				response.put("message", "Unknown query type: " + qtyp);
				result = response.toString();
			}

		} catch (Exception e) {
			e.printStackTrace();
			response.put("sign", "ERROR");
			response.put("error", e.getMessage());
			result = response.toString();
		}

		return result;
	}

	/**
	 * Main method for testing the engine. Demonstrates an insert operation with
	 * sample JSON.
	 * 
	 * @param args Command-line arguments
	 * @throws Exception if execution fails
	 */
	public static void main(String[] args) throws Exception {
		String jsonInsert = "{\"main_sign\":\"insert\",\"qtn\":\"ravi\",\"qdt\":{\"name\":\"Akashitha\",\"id\":\"109\"},\"rtp\":\"execute\"}";
		String jsonSelect = "{\"main_sign\":\"select\",\"qtn\":\"TEST_TABLE\",\"qcl\":[\"name\",\"age\"],\"qcn\":\"WHERE age > 25\"}";

		JSONObject hm = new JSONObject();
		hm.put("UNM", "RAVI");
		hm.put("PWD", "Pass12!@");

		// String jsonval =
		// "{\"main_sign\":\"insert\",\"qtn\":\"YourTableName\",\"qdt\":{\"column1\":\"value1\",\"column2\":\"value2\",\"column3\":\"value3\"},\"rtp\":\"query\"}";
		// String jsonval
		// ="{\"main_sign\":\"select\",\"qtn\":\"YourTableName\",\"qcl\":[col1,col2,col3,col4,col5,col6],\"qcn\":\"WHERE
		// col1 is not null\"},\"rtp\":\"execute\"}";
		// String jsonval =
		// "{\"main_sign\":\"delete\",\"qtn\":\"YourTableName\",\"qcn\":\"where col1 is
		// not null\"},\"rtp\":\"execute\"}";
		// String jsonval =
		// "{\"main_sign\":\"update\",\"qtn\":\"YourTableName\",\"qdt\":{\"column1\":\"value1\",\"column2\":\"value2\",\"column3\":\"select
		// 'raviteja' from dual\"},\"qcn\":\" col1 is not null\"},\"rtp\":\"execute\"}";
String jsonval = "{\"main_sign\":\"select\",\"raw_qry\":\"select count(1) as cnt from ctuser_master where user_name={UNM} and password={PWD}\",\"params\":" + hm + ",\"rtp\":\"execute\"}";

		JSONObject input = new JSONObject(jsonInsert);
		//String query = executer(input);
		String paramquery = paramexecuter(jsonval);
		System.out.println(paramquery);
		// String output = dbexecuter(query, input.getString("main_sign"));
		// System.out.println("Final Output: " + output);
	}
}
