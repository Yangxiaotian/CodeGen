import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;



public class CodeGen implements ActionListener{
	JFrame frame = new JFrame("基本代码生成工具");
	JPanel panel = new JPanel();
	JTextArea textarea1 = new JTextArea("结果输出");
	JTextField textfield = new JTextField("请输入表名");
	JButton button1 = new JButton("后台MODEL");
	JButton button2 = new JButton("前台MODEL");
	JButton button3 = new JButton("后台CTRL");
	JButton button4 = new JButton("SQL生成");
	JButton button5 = new JButton("后台Dao");
	JButton button6 = new JButton("后台Service");
	JScrollPane scroll = new JScrollPane(textarea1);
	String author = "";
	public void createUI(){
		frame.setSize(700,500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		panel.setLayout(new BorderLayout());
		textfield.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				// TODO Auto-generated method stub
				if(textfield.getText().equals("")) textfield.setText("输入表名");
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				// TODO Auto-generated method stub
				textfield.setText("");
			}
		});
		panel.add(textfield, BorderLayout.NORTH);
		panel.add(scroll,BorderLayout.CENTER); 
		JPanel btnP = new JPanel();
		btnP.add(button1);
		btnP.add(button2);
		btnP.add(button3);
		btnP.add(button4);
		btnP.add(button5);
		btnP.add(button6);
		panel.add(btnP, BorderLayout.SOUTH);
		frame.getContentPane().add(panel);
		frame.setFocusable(true);
		button1.addActionListener(this);
		button2.addActionListener(this);
		button3.addActionListener(this);
		button4.addActionListener(this);
		button5.addActionListener(this);
		button6.addActionListener(this);
		try {
			checkSetting();
		}catch(Exception e) {
			textarea1.setText(e.getMessage());
		}
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
	}
	public static void main(String[] args) {
		new CodeGen().createUI();
	}
	public static void checkSetting() throws IOException {
		String path = "bin\\setting.txt";
		File file = new File(path);
		if(!file.exists()) {
			File binDir = new File("bin");
			if(!binDir.exists() || !binDir.isDirectory()) {
				binDir.mkdirs();
			}
			file.createNewFile();
			OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(path), "UTF-8");
			osw.write("jdbc.driverClassName=jdbc:mysql://IP:3306/数据库名\r\n"+
                      "jdbc.driverClassName=com.mysql.jdbc.Driver\r\n"+
                      "jdbc.username=root\r\n"+
                      "jdbc.password=\r\n"+
                      "author="
            );
			osw.close();
		}
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		String tblName = textfield.getText();
		
		try {
			checkSetting();
			String path = "bin\\setting.txt";
			InputStreamReader reader = new InputStreamReader(new FileInputStream(path), "UTF-8");
	        BufferedReader br = new BufferedReader(reader);
	        String str = null;
	        String url = null;
	        String userName = null;
	        String password = null;
	        int k = 0;
	        while((str = br.readLine()) != null) {
	        	switch(k) {
	        	case 0: url = str.split("driverClassName=")[1]; break;
	        	case 1: Class.forName(str.split("=")[1]);break;
	        	case 2: userName = str.split("=")[1];break;
	        	case 3: password = str.split("=").length>1?str.split("=")[1]:"";break;
	        	case 4: author = str.split("=").length>1?str.split("=")[1]:"";break;
	        	}
	        	k++;
	        }
	        br.close();
	        String dataBaseName = "";
	        String dbfield = null;
	        if(url.startsWith("jdbc:mysql")) {
	        	dataBaseName = url.split("\\d+/")[1];
	        	dbfield = "TABLE_SCHEMA";
	        }else if(url.startsWith("jdbc:sqlserver")) {
	        	dataBaseName = url.split("DatabaseName=")[1];
	        	dbfield = "TABLE_CATALOG";
	        }
			Connection con = DriverManager.getConnection(url,userName,password); 
			Statement stmt = con.createStatement();
//			ResultSet rs1 = stmt.executeQuery("select * from information_schema.TABLES where table_name = \'" + tblName + "\'" + "and table_schema = \'"+dataBaseName+"\'");
//			rs1.next();
//			 String tblComment = rs1.getString("TABLE_COMMENT");
//			 tblComment = tblComment.replace("\n", "：");
			 ResultSet rs = stmt.executeQuery("select * from information_schema.COLUMNS where table_name = \'" + tblName + "\'" + "and "+dbfield+"= \'"+dataBaseName+"\'");
			 String Code;
			 if(e.getSource() == button1) {
				 Code = getBackCode(tblName, rs);
			 } else if(e.getSource() == button2){
				 Code = getFrontCode(tblName, rs); 
			 } else if(e.getSource() == button3){
				 Code = getBackCtrlCode(tblName); 
			 }else if(e.getSource() == button4){
				 Code = getSqlCode(tblName, rs, "");
			 } else if(e.getSource() == button5) {
				Code = getDaoCode(tblName, "");
			 } else if(e.getSource() == button6) {
				 Code = getServiceCode(tblName, "");
			 } else {
				 Code = "";
			 }
			 textarea1.setText(Code);
		}catch(Exception e1) {
			e1.printStackTrace();
			textarea1.setText(e1.getMessage());
		}
	}
	public String getModelCode(String text) throws Exception {
		List<String[]> list = new ArrayList<String[]>();
		String[] arr = text.split("\n");
		String result;
		int length = arr.length;
		if(arr[0].split(" ")[1].equals("table")&& arr[1].equals("(") && arr[length-1].equals(");")) {
			result = "public class " + getModelName(arr[0].split(" ")[2]) +
					" {\n";
			for(int i = 2; notBase(arr[i]); i++) {
				String[] temp = arr[i].split("\\s{2,}");
				if(temp[2].indexOf("varchar") != -1) {
					temp[2] = "String";
				} else if(temp[2].indexOf("datetime") != -1){
					temp[2] = "Date";
				} else if(temp[2].indexOf("bit") != -1){
					temp[2] = "boolean";
				} else if(temp[2].indexOf("int") != -1) {
					temp[2] = "int";
				} else if(temp[2].indexOf("decimal") != -1) {
					temp[2] = "double";
				} else {
					throw new Exception("输入格式有误\n" + temp[2]);
				}
				list.add(temp);
			}
			for(String[] col: list) {
				result += "\tprivate " + col[2] + " " + col[1] + ";\n";
			}
			result += "\n";
			for(String[] col: list) {
				String bigFirst = col[1].substring(0,1).toUpperCase()+col[1].substring(1); 
				result += "\tpublic void set" + bigFirst +"(" + col[2] + " " + col[1] + ") {\n\t        this." + col[1] + " = " + col[1] + ";\n\t}\n";
				result += "\tpublic " + col[2] + " get" + bigFirst + "() {\n\t        return " + col[1] + ";\n\t}\n"; 
			}
			result += "}\n";
		} else {
			throw new Exception("输入格式有误\n");
		}
		
		return result;
	}
	public String getFrontCode(String tblName, ResultSet rs) throws SQLException {
		String idProperty = "id";
		List<String[]> list = new ArrayList<String[]>();
		while(rs.next()) {
			String[] temp = new String[3];
			getColumnNameType(rs, temp);
			if(temp[2].indexOf("varchar") != -1) {
				temp[2] = "string";
			} else if(temp[2].indexOf("datetime") != -1){
				temp[2] = "date";
			} else if(temp[2].indexOf("bit") != -1){
				temp[2] = "boolean";
			} else if(temp[2].indexOf("int") != -1) {
				temp[2] = "int";
			} else if(temp[2].indexOf("decimal") != -1) {
				temp[2] = "number";
			} 
			list.add(temp);
		}
		String result = "Ext.define(\'yxt.model.XXXX." + getModelName(tblName) + "\', {\n\textend:\'Ext.data.Model\',\n\tidProperty:\'" 
			            + idProperty +"\',\n\tfields:[\n";
		for(String[] col: list) {
			result += "\t\t{name: \'" + col[1] + "\', type: \'" + col[2] + "\'},\n";
		}
		result += "\t]\n});";
		return result;
	}
	private void getColumnNameType(ResultSet rs, String[] temp) throws SQLException {
		try {
			temp[0] = rs.getString("COLUMN_COMMENT");
			temp[1] = rs.getString("COLUMN_NAME");
			temp[2] = rs.getString("COLUMN_TYPE");
		}catch(Exception e) {
			temp[0] = null;
			temp[1] = rs.getString("COLUMN_NAME");
			temp[2] = rs.getString("DATA_TYPE");
		}
	}
	public String getBackCode(String tblName, ResultSet rs) throws SQLException {
		String result = "public class " + getModelName(tblName) +
				" {\n";
		List<String[]> list = new ArrayList<String[]>();
		while(rs.next()) {
			String[] temp = new String[3];
			getColumnNameType(rs, temp);
			
			if(temp[2].indexOf("char") != -1) {
				temp[2] = "String";
			} else if(temp[2].indexOf("datetime") != -1){
				temp[2] = "Date";
			} else if(temp[2].indexOf("bit") != -1){
				temp[2] = "boolean";
			} else if(temp[2].indexOf("int") != -1) {
				temp[2] = "int";
			} else if(temp[2].indexOf("decimal") != -1) {
				temp[2] = "double";
			} 
			list.add(temp);
		}
		for(String[] col: list) {
			result += "\tprivate " + col[2] + " " + col[1] + ";"+(col[0]!=null?("//" + col[0] + "\n"):"\n");
		}
		result += "\n";
		for(String[] col: list) {
			String bigFirst = col[1].substring(0,1).toUpperCase()+col[1].substring(1); 
			result += "\tpublic void set" + bigFirst +"(" + col[2] + " " + col[1] + ") {\n\t        this." + col[1] + " = " + col[1] + ";\n\t}\n";
			result += "\tpublic " + col[2] + " get" + bigFirst + "() {\n\t        return " + col[1] + ";\n\t}\n"; 
		}
		result += "}\n";
		return result;
	}
	public String getBackCtrlCode(String tblName) {
		String result = "";
		String modeName = getModelName(tblName);
		System.out.println(modeName);
		String prefix = modeName.substring(0, 1).toLowerCase()+modeName.substring(1);
		result += "@Controller(value = \"com.XXX.ctrl."+modeName+"Ctrl\")\n";
		result += "@RequestMapping(\"[app]/[model]\")\n";
		result += "public class "+modeName+"Ctrl {\n";
		result += "\t@Autowired\n\tprivate "+modeName+"Service "+prefix+"Service;\n";
		result += "\t@Autowired\n\tprivate CoreService aCoreService;\n";
        result += "\t@RequestMapping(value = \"list\", method = RequestMethod.GET)\n\t@ResponseBody\n";
		result += "\tpublic Map<String, Object> list(\n";
		result += "\t\t@RequestParam(value = \"id\", required = false) Integer id,\n";	
		result += "\t\t@RequestParam(value = \"name\", required = false) String name,\n";	
		result += "\t\t@RequestParam(value = \"ono\", required = false) String ono,\n";			
		result += "\t\t@RequestParam(value = \"start\", required = false) Integer start,\n";			
		result += "\t\t@RequestParam(value = \"limit\", required = false) Integer limit\n";		
		result += "\t) {\n\t\ttry {\n\t\t\taCoreService.assertFunctionAuth(\"\");\n";
		result += "\t\t\tif (ono == null) {\n\t\t\t\tono = aCoreService.getMyOno();\n\t\t\t}\n";
		result += "\t\t\treturn WebUtil.getSuccessMap("+prefix+"Service.get"+modeName+"List(id, name, ono, start, limit), "+prefix+"Service.get"+modeName+"Count(name, ono));\n";
		result += "\t\t} catch(Exception e) {\n\t\t\treturn WebUtil.getFailureMap(e.getMessage());\n\t\t}\n\t}";
		
		result += "\n}";

		return result;
	}
	public boolean notBase(String s) {
		String[] basePool = {"id"};
		for(String elem: basePool) {
			if(s.equals(elem)) {
				return false;
			}
		}
		return true;
	}
	public String getModelName(String tblName) {
		String modelName = "";
		String[] arr = tblName.split("_");
		if(arr.length == 1) return tblName;
		for(int i = 1; i < arr.length; i++) {
			modelName += arr[i].substring(0,1).toUpperCase()+arr[i].substring(1); 
		}
		return modelName;
	}
	public String processSql(String sql) {
		String result = "";
		sql = sql.replaceAll("\ndrop index.*?;\n", "");
		sql = sql.replaceAll("drop index.*?;\n", "");
		result = sql.replaceAll("FK_Reference", "CYTS1_FK_Reference");
		return result;
	}
	public String getSqlCode(String tblName, ResultSet rs, String tblComment) throws Exception{
		String modeName = getModelName(tblName);
		String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n<mapper namespace=\"com.XXX.dao."+modeName+"\">\n";
		List<Map<String,Object>> mapList = new ArrayList<Map<String,Object>>();
		while(rs.next()) {
			String columnName = rs.getString("COLUMN_NAME");
			Map<String,Object> map = new HashMap<String,Object>();
			map.put("columnName", columnName);
			mapList.add(map);
		}
		String countSql = getCountSql(tblName, mapList, tblComment);
		String listSql = getListSql(tblName, mapList, tblComment);
		String insertSql = getInsertSql(tblName, mapList, tblComment);
		String updateSql = getUpdateSql(tblName, mapList, tblComment);
		String delSql = getDelSql(tblName, mapList, tblComment);
		result += countSql + listSql + insertSql + updateSql + delSql + "\n</mapper>";
		return result;
	}
	public String getInsertSql(String tblName, List<Map<String,Object>> mapList, String tblComment) {
		String result;
		String modeName = getModelName(tblName);
		try {
			result = "\t<!-- 新增" + tblComment + " -->\n\t<insert id=\"create" + modeName 
					+ "\" parameterType=\"com.XXX.model."+modeName+"\" useGeneratedKeys=\"true\" keyProperty=\"id\">\n\t\tinsert into " 
					+ tblName + "\n";
			String up = "\t\t(\n\t\t\t";
			String down = "values\n\t\t(\n\t\t\t";
			int i = 0;
			for(Map<String,Object> map: mapList) {
				String columnName = (String)map.get("columnName");
				if(notBase(columnName)) {
					up += columnName +(i+1==mapList.size()?"\n\t\t":",\n\t\t\t");
					down += "#{" + columnName + "}"+(i+1==mapList.size()?"\n\t\t":",\n\t\t\t");
				}
				i++;
			}
			result += up + ")" 
					     + down + ")\n"; 
			result += "\t</insert>\n";
		} catch(Exception e) {
			e.printStackTrace();
			return "输入有误";
		}
		return result;
	}
	public String getUpdateSql(String tblName, List<Map<String,Object>> mapList, String tblComment) {
		String result = "";
		String primaryKey = null;
		String modeName = getModelName(tblName);
		try {
			result = "\t<!-- 更改" + tblComment + " -->\n\t<update id=\"update" + modeName 
					+ "\" parameterType=\"com.XXX.model."+modeName+"\">\n\t\tupdate " + tblName + "\n\t\tset";
			int i = 0;
			for(Map<String,Object> map: mapList) {
				String columnName = (String)map.get("columnName");
				String columnKey = (String)map.get("columnKey");
				if("PRI".equals(columnKey)) {
					 primaryKey = columnName;
				}else if(notBase(columnName)) {
					result += "    " + columnName + " = #{" + columnName + "}"+(i+1==mapList.size()?"":",")+"\n\t\t";
				}
				i++;
			}
			result += "where id = #{id}\n"; 
			result += "\t</update>\n";
		} catch(Exception e) {
			e.printStackTrace();
			return "输入有误";
		}
		return result;
	}
	public String getCountSql(String tblName, List<Map<String,Object>> mapList, String tblComment) {
		String result = "";
		String modeName = getModelName(tblName);
		try {
			result = "\t<!-- 获取" + tblComment + " 数量-->\n\t<select id=\"get" + modeName 
					+ "Count\" parameterType=\"map\" resultType=\"int\">\n\t\tselect count(*) from "
					+ tblName + 
						" where \n\t\t"
					+getIfTest("id", "=", "#{id}")+"\n\t\t"
					+getIfTest("ono", "like", "'${ono}%'")+"\n\t\t"
					+getIfTest("name", "like", "'%${name}%'")+"\n\t\t    ";
			result += "\n\t</select>\n";
		} catch(Exception e) {
			e.printStackTrace();
			return "//获取" + tblComment +"数量输入有误";
		}
		return result;
	}
	public String getListSql(String tblName, List<Map<String,Object>> mapList, String tblComment) {
		String result = "";
		String primaryKey = null;
		String modeName = getModelName(tblName);
		try {
			result = "\t<!-- 获取" + tblComment + " -->\n\t<select id=\"get" + modeName 
					+ "List\" parameterType=\"map\" resultType=\"com.XXX.model." + modeName + "\">\n\t\tselect\n\t";
			int i = 0;
			for(Map<String,Object> map: mapList) {
				String columnName = (String)map.get("columnName");
				String columnKey = (String)map.get("columnKey");
				if("PRI".equals(columnKey)) {
					 primaryKey = columnName;
				}
				if(!columnName.equals("version")){
					result += "\t    " + columnName + ""+(i+1==mapList.size()?"":",")+"\n\t";
				}
				i++;
			}
			result += "\tfrom " + tblName + 
						" where\n\t\t"
					+getIfTest("id", "=", "#{id}")+"\n\t\t"
					+getIfTest("name", "like", "'%${name}%'")+"\n\t\t"
					+getIfTest("ono", "like", "'${ono}%'")+"\n\t\t"
					+getIfTest("start", "", "limit ${start}, ${limit}");
			result += "\n\t</select>\n";
		} catch(Exception e) {
			e.printStackTrace();
			return "输入有误";
		}
		return result;
	}
	public String getIfTest(String columnName) {
		return getIfTest(columnName,  null, null);
	}
	public String getIfTest(String columnName, String flag, String tail) {
		if(flag==null) flag = "=";
		if(tail==null) tail = "#{"+columnName+"}";
		String _and = "and ";
		String col = columnName;
		if(flag.length() == 0) {
			_and = "";
			col = "";
		}
		return "<if test=\""+columnName+"!=null\">"+_and+col+" "+flag+(flag.length()>0?" ":"")+tail+"</if>";
	}
	public String getDelSql(String tblName, List<Map<String,Object>> mapList, String tblComment) {
		String result = "";
		String modeName = getModelName(tblName);
		try {
			result = "\t<!-- 删除" + tblComment + " -->\n\t<delete id=\"delete" + modeName 
					+ "\" parameterType=\"map\">\n\t\tdelete from " + tblName + " where id = #{id}\n\t</delete>\n";
		} catch(Exception e) {
			e.printStackTrace();
			return "输入有误";
		}
		return result;
	}
	public String getDaoCode(String tblName, String tblComment) {
		return getDaoCodeCore("count", tblName, tblComment) 
				+ getDaoCodeCore("list", tblName, tblComment)
				+ getDaoCodeCore("create", tblName, tblComment)
				+ getDaoCodeCore("update", tblName, tblComment)
				+ getDaoCodeCore("delete", tblName, tblComment);
	}
	public String getDaoCodeCore(String method, String tblName, String tblComment) {
		String modeName = getModelName(tblName);
		String prefix = modeName.substring(0,1).toLowerCase()+modeName.substring(1);
		String cnHead ="";
		String cnTail = "";
		String codeBody = "";
		if(method.equals("create")) {
			cnHead = "新增";
			codeBody = "public void create" + modeName + "("+modeName+" "+prefix+");\n";
		}else if(method.equals("update")) {
			cnHead = "更新";
			codeBody = "public void update" + modeName + "("+modeName+" "+prefix+");\n";
		}else if(method.equals("delete")) {
			cnHead = "删除";
			codeBody = "public int delete" + modeName + "(Map<String,Object> p);\n";
		}else if(method.equals("list")) {
			cnHead = "获取";
			cnTail = "列表";
			codeBody = "public List<" + modeName + "> get" + modeName + "List(Map<String,Object> p);\n";
		}else if(method.equals("count")) {
			cnHead = "获取";
			cnTail = "总数";
			codeBody = "public int get" + modeName + "Count(Map<String,Object> p);\n";
		}
		String result = "\t/**\n\t * "+cnHead+tblComment+cnTail+"\n\t * @author "+author+"\n\t */\n\t"+codeBody;
		return result;
	}
	public String getServiceCode(String tblName, String tblComment) {
		return getServiceCodeCore("count", tblName, tblComment) 
				+ getServiceCodeCore("list", tblName, tblComment)
				+ getServiceCodeCore("create", tblName, tblComment)
				+ getServiceCodeCore("update", tblName, tblComment)
				+ getServiceCodeCore("delete", tblName, tblComment);
	}
	public String getServiceCodeCore(String method, String tblName, String tblComment) {
		String modeName = getModelName(tblName);
		String prefix = modeName.substring(0, 1).toLowerCase()+modeName.substring(1);
		String cnHead ="";
		String cnTail = "";
		String codeBody = "";
		String fnName = "";
		if(method.equals("create")) {
			cnHead = "新增";
			fnName = "create" + modeName;
			codeBody = "public void " + fnName + "("+modeName+" "+prefix+") {" + getFnBody(tblName, fnName) + "}";
		}else if(method.equals("update")) {
			cnHead = "更新";
			fnName = "update" + modeName;
			codeBody = "public void " + fnName + "("+modeName+" "+prefix+") {" + getFnBody(tblName, fnName) + "}";
		}else if(method.equals("delete")) {
			cnHead = "删除";
			fnName = "delete" + modeName;
			codeBody = "public void " + fnName + "(int id) {" + getFnBody(tblName, fnName) + "}";
		}else if(method.equals("list")) {
			cnHead = "获取";
			cnTail = "列表";
			fnName = "get" + modeName + "List";
			codeBody = "public List<" + modeName + "> " + fnName + "(Integer id, String name, String ono, Integer start, Integer limit) {" + getFnBody(tblName, fnName) + "}";
		}else if(method.equals("count")) {
			cnHead = "获取";
			cnTail = "总数";
			fnName = "get" + modeName + "Count";
			codeBody = "public int get" + modeName + "Count(String name, String ono) {" + getFnBody(tblName, fnName) + "}";
		}
		String result = "\t/**\n\t * "+cnHead+tblComment+cnTail+"\n\t * @author "+author+"\n\t */\n\t"+codeBody+"\n";
		return result;
	}
	public String getFnBody(String tblName, String fnName) {
		String result = "";
		String modelName = getModelName(tblName);
		String prefix = modelName.substring(0, 1).toLowerCase()+modelName.substring(1);
		if(fnName.startsWith("create") || fnName.startsWith("update")) {
			return "\n\t\t"+prefix+"Dao."+fnName+"("+prefix+");\n\t";
		} else if(fnName.startsWith("delete")) {
			return 	"\n\t\tMap<String,Object> p = new HashMap<String,Object>();"
					+"\n\t\tp.put(\"id\", id);"
					+"\n\t\t"+prefix+"Dao."+fnName+"(p);\n\t";
		} else if(fnName.endsWith("Count")) {
			return  "\n\t\tMap<String,Object> p = new HashMap<String,Object>();"
					+"\n\t\tp.put(\"name\", name);"
					+"\n\t\tp.put(\"ono\", ono);"
					+"\n\t\treturn "+prefix+"Dao."+fnName+"(p);\n\t";
		} else if(fnName.endsWith("List")) {
			return  "\n\t\tMap<String,Object> p = new HashMap<String,Object>();"
					+"\n\t\tp.put(\"id\", id);"
					+"\n\t\tp.put(\"name\", name);"
					+"\n\t\tp.put(\"ono\", ono);"
					+"\n\t\tp.put(\"start\", start);"
					+"\n\t\tp.put(\"limit\", limit);"
					+ "\n\t\treturn "+prefix+"Dao."+fnName+"(p);\n\t";
		} 
		return result;
	}
}
