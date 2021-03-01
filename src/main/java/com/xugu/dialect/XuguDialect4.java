/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package com.xugu.dialect;


import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.MappingException;
import org.hibernate.cfg.Environment; 
import java.sql.Types; 
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.CallableStatement;
public class XuguDialect4 extends Dialect {
 public XuguDialect4() 
  { 
	 
	//registerColumnType(Types.NULL, "NULL");
	 super();
	 
	 registerColumnType(Types.BIT, "BINARY"); 
	 registerColumnType(Types.TINYINT, "TINYINT"); 
	 registerColumnType(Types.SMALLINT, "SMALLINT"); 
	 registerColumnType(Types.INTEGER, "INTEGER"); 
	 registerColumnType(Types.BIGINT, "BIGINT"); 
	 registerColumnType(Types.REAL, "FLOAT"); 
	 registerColumnType(Types.FLOAT, "FLOAT"); 
	 registerColumnType(Types.DOUBLE, "DOUBLE"); 

	 /*registerColumnType(Types.DECIMAL, "NUMERIC"); 
	 registerColumnType(Types.NUMERIC, "NUMERIC"); */
	 registerColumnType( Types.NUMERIC, "NUMERIC($p,$s)" );
	 registerColumnType( Types.DECIMAL, "NUMERIC($p,$s)" );
	registerColumnType(Types.CHAR, 4000L,"CHAR($l)");
	registerColumnType(Types.VARCHAR, 4000L, "VARCHAR($l)");
	registerColumnType(Types.LONGVARCHAR, "VARCHAR");
	registerColumnType(Types.DATE, "DATE");
	registerColumnType(Types.TIME, "TIME");
	registerColumnType(Types.TIMESTAMP, "TIMESTAMP");
	//registerColumnType(com.xugu.jdbc.Types.INTERVAL, "INTERVAL YEAR TO MONTH");
	registerColumnType(Types.BINARY, "BINARY"); 
	registerColumnType(Types.VARBINARY, "BINARY");
	registerColumnType(Types.LONGVARBINARY, "BLOB");
	registerColumnType(Types.BLOB, "BLOB");
	//registerColumnType(com.xugu.jdbc.Types.IMAGE, "IMAGE");
	registerColumnType(Types.CLOB, "CLOB"); 
	registerColumnType(Types.BOOLEAN, "BOOLEAN"); 
	//------------------------------- 数值函数-------------------------
	registerFunction("bin", new StandardSQLFunction("bin",StandardBasicTypes.BINARY)); 
	registerFunction("hex", new StandardSQLFunction("hex",StandardBasicTypes.CHARACTER)); 
	registerFunction("to_char", new StandardSQLFunction("to_char",StandardBasicTypes.STRING)); 
	registerFunction("abs", new StandardSQLFunction("abs")); 
	registerFunction("sin", new StandardSQLFunction("sin",StandardBasicTypes.DOUBLE)); 
	registerFunction("cos", new StandardSQLFunction("cos",StandardBasicTypes.DOUBLE)); 
	registerFunction("tan", new StandardSQLFunction("tan",StandardBasicTypes.DOUBLE)); 
	registerFunction("cot", new StandardSQLFunction("cot",StandardBasicTypes.DOUBLE)); 
	registerFunction("asin", new StandardSQLFunction("asin",StandardBasicTypes.DOUBLE)); 
	registerFunction("acos", new StandardSQLFunction("acos",StandardBasicTypes.DOUBLE)); 
	registerFunction("atan", new StandardSQLFunction("atan",StandardBasicTypes.DOUBLE)); 
	registerFunction("ln", new StandardSQLFunction("ln",StandardBasicTypes.DOUBLE)); 
	registerFunction("log10", new StandardSQLFunction("log10",StandardBasicTypes.DOUBLE)); 
	registerFunction("ceiling", new StandardSQLFunction("ceiling"));  
	registerFunction("floor", new StandardSQLFunction("floor")); 
	registerFunction("exp", new StandardSQLFunction("exp",StandardBasicTypes.DOUBLE)); 
	registerFunction("sqrt", new StandardSQLFunction("sqrt",StandardBasicTypes.DOUBLE)); 
	registerFunction("square", new StandardSQLFunction("square",StandardBasicTypes.DOUBLE)); 
	registerFunction("power", new StandardSQLFunction("power",StandardBasicTypes.DOUBLE)); 
	registerFunction("sign", new StandardSQLFunction("sign",StandardBasicTypes.INTEGER)); 	
	registerFunction("radians", new StandardSQLFunction("radians")); 
	registerFunction("round", new StandardSQLFunction("round"));	
	registerFunction("degress", new StandardSQLFunction("degress"));
	registerFunction("trunc", new StandardSQLFunction("trunc")); 
	registerFunction("is null", new StandardSQLFunction("is null",StandardBasicTypes.BOOLEAN)); 
	registerFunction("isnull", new StandardSQLFunction("isnull",StandardBasicTypes.BOOLEAN)); 
	registerFunction("notnull", new StandardSQLFunction("notnull",StandardBasicTypes.BOOLEAN));
	
	//字符串函数 registerFunction("ascii", new StandardSQLFunction(StandardBasicTypes.INTEGER)); registerFunction("bit_length", new StandardSQLFunction( StandardBasicTypes.LONG)); registerFunction("char", new StandardSQLFunction( StandardBasicTypes.CHARACTER)); registerFunction("char_length", new StandardSQLFunction( StandardBasicTypes.LONG)); registerFunction("character_length", new StandardSQLFunction(StandardBasicTypes.LONG)); registerFunction("chr", new StandardSQLFunction(StandardBasicTypes.CHARACTER)); registerFunction("concat", new StandardSQLFunction(StandardBasicTypes.STRING)); registerFunction("difference", new StandardSQLFunction(
	
	//-----------------------数值数据类型的聚合函数--------------------
	
	registerFunction("var", new StandardSQLFunction("var"));
	registerFunction("stddev", new StandardSQLFunction("stddev"));
	registerFunction("stddevp", new StandardSQLFunction("stddevp"));
	registerFunction("varp", new StandardSQLFunction("varp"));
	registerFunction("min", new StandardSQLFunction("min"));
	registerFunction("max", new StandardSQLFunction("max"));
	registerFunction("avg", new StandardSQLFunction("avg"));
	registerFunction("sum", new StandardSQLFunction("sum"));
	
	//-------------------字符数据类型函数----------
	
	registerFunction("pinyin", new StandardSQLFunction("pinyin", StandardBasicTypes.STRING));
	registerFunction("pinyin1", new StandardSQLFunction("pinyin1", StandardBasicTypes.STRING));
	registerFunction("position", new StandardSQLFunction("position", StandardBasicTypes.INTEGER));
	registerFunction("box", new StandardSQLFunction("box"));
	registerFunction("point", new StandardSQLFunction("point"));
	registerFunction("atof", new StandardSQLFunction("atof",StandardBasicTypes.DOUBLE));
	registerFunction("atoi", new StandardSQLFunction("atoi",StandardBasicTypes.INTEGER));
	registerFunction("ltrim", new StandardSQLFunction("ltrim",StandardBasicTypes.STRING));
	registerFunction("rtrim", new StandardSQLFunction("rtrim",StandardBasicTypes.STRING));
	registerFunction("btrim", new StandardSQLFunction("btrim",StandardBasicTypes.STRING));
	registerFunction("substr", new StandardSQLFunction("substr",StandardBasicTypes.STRING));
	registerFunction("currval", new StandardSQLFunction("currval",StandardBasicTypes.BIG_INTEGER));
	registerFunction("nextval", new StandardSQLFunction("nextval",StandardBasicTypes.BIG_INTEGER));
	registerFunction("currserial", new StandardSQLFunction("currserial",StandardBasicTypes.BIG_INTEGER));
	registerFunction("nextserial", new StandardSQLFunction("nextserial",StandardBasicTypes.BIG_INTEGER));
	registerFunction("len", new StandardSQLFunction("len",StandardBasicTypes.INTEGER));
	registerFunction("heading", new StandardSQLFunction("heading",StandardBasicTypes.STRING));
	registerFunction("tailing", new StandardSQLFunction("tailing",StandardBasicTypes.STRING));
	registerFunction("reverse_str", new StandardSQLFunction("reverse_str",StandardBasicTypes.STRING));
	registerFunction("lower", new StandardSQLFunction("lower",StandardBasicTypes.STRING));
	registerFunction("upper", new StandardSQLFunction("upper",StandardBasicTypes.STRING));
	registerFunction("replicate", new StandardSQLFunction("replicate",StandardBasicTypes.STRING));
	registerFunction("stuff", new StandardSQLFunction("stuff",StandardBasicTypes.STRING));
	registerFunction("replace", new StandardSQLFunction("replace",StandardBasicTypes.STRING));
	registerFunction("concat", new StandardSQLFunction("concat",StandardBasicTypes.STRING));
	registerFunction("character_length", new StandardSQLFunction("character_length",StandardBasicTypes.STRING));
	registerFunction("char_length", new StandardSQLFunction("char_length",StandardBasicTypes.STRING));
	registerFunction("translate", new StandardSQLFunction("translate",StandardBasicTypes.STRING));
	

	//------------------------------时间、日期数据类型函数-------------------------------------
	
	registerFunction("getyear", new StandardSQLFunction("getyear",StandardBasicTypes.INTEGER));
	registerFunction("getmonth", new StandardSQLFunction("getmonth",StandardBasicTypes.INTEGER));
	registerFunction("getday", new StandardSQLFunction("getday",StandardBasicTypes.INTEGER));
	registerFunction("gethour", new StandardSQLFunction("gethour",StandardBasicTypes.INTEGER));
	registerFunction("getminute", new StandardSQLFunction("getminute",StandardBasicTypes.INTEGER));
	registerFunction("getsecond", new StandardSQLFunction("getsecond",StandardBasicTypes.INTEGER));
	registerFunction("next_day", new StandardSQLFunction("next_day",StandardBasicTypes.TIMESTAMP));
	registerFunction("last_day", new StandardSQLFunction("last_day",StandardBasicTypes.TIMESTAMP));
	registerFunction("add_months", new StandardSQLFunction("add_months",StandardBasicTypes.TIMESTAMP));
	registerFunction("months_between", new StandardSQLFunction("months_between",StandardBasicTypes.INTEGER));
	registerFunction("extract", new StandardSQLFunction("extract",StandardBasicTypes.INTEGER));
	registerFunction("now", new StandardSQLFunction("now",StandardBasicTypes.TIMESTAMP));
	registerFunction("to_date", new StandardSQLFunction("to_date",StandardBasicTypes.TIMESTAMP));
	registerFunction("sysdate", new StandardSQLFunction("sysdate",StandardBasicTypes.TIMESTAMP));
	registerFunction("systime", new StandardSQLFunction("systime",StandardBasicTypes.TIME));
	registerFunction("sysdatetime", new StandardSQLFunction("sysdatetime",StandardBasicTypes.TIMESTAMP));
	registerFunction("overlaps", new StandardSQLFunction("overlaps",StandardBasicTypes.BOOLEAN));
	registerFunction("extract_day", new StandardSQLFunction("extract_day",StandardBasicTypes.INTEGER));
	registerFunction("extract_hour", new StandardSQLFunction("extract_hour",StandardBasicTypes.INTEGER));
	registerFunction("extract_minute", new StandardSQLFunction("extract_minute",StandardBasicTypes.INTEGER));
	registerFunction("extract_second", new StandardSQLFunction("extract_second",StandardBasicTypes.DOUBLE));
	registerFunction("extract_month", new StandardSQLFunction("extract_month",StandardBasicTypes.INTEGER));
	registerFunction("extract_year", new StandardSQLFunction("extract_year",StandardBasicTypes.INTEGER));
	
	//-----------------------------特殊表达式---------------------------------------------------------
	/*javax.swing.JOptionPane.showMessageDialog(null, "1111111111111111111111..... ", "aaaaaa", javax.swing.JOptionPane.INFORMATION_MESSAGE);
	javax.swing.JOptionPane.showMessageDialog(null, "2222222222222222222222..... ", "aaaaaa", javax.swing.JOptionPane.INFORMATION_MESSAGE);*/
	registerFunction("count", new StandardSQLFunction("count",StandardBasicTypes.LONG)); 
	registerFunction("current_userid", new StandardSQLFunction("current_userid",StandardBasicTypes.INTEGER));
	registerFunction("current_date", new StandardSQLFunction("current_date",StandardBasicTypes.DATE));
	registerFunction("current_time", new StandardSQLFunction("current_time",StandardBasicTypes.TIME));
	registerFunction("current_timestamp", new StandardSQLFunction("current_timestamp",StandardBasicTypes.TIMESTAMP));
	registerFunction("current_user", new StandardSQLFunction("current_user",StandardBasicTypes.STRING));
	registerFunction("current_database", new StandardSQLFunction("current_database",StandardBasicTypes.STRING));
	registerFunction("current_db", new StandardSQLFunction("current_db",StandardBasicTypes.STRING));
	registerFunction("current_db_id", new StandardSQLFunction("current_db_id",StandardBasicTypes.INTEGER));
	registerFunction("current_ip", new StandardSQLFunction("current_ip",StandardBasicTypes.STRING));
	registerFunction("system_user", new StandardSQLFunction("system_user",StandardBasicTypes.STRING));
	registerFunction("sys_userid", new StandardSQLFunction("sys_userid",StandardBasicTypes.INTEGER));
	getDefaultProperties().setProperty(Environment.USE_GET_GENERATED_KEYS, "true");
	getDefaultProperties().setProperty(Environment.USE_STREAMS_FOR_BINARY, "true");
	//getDefaultProperties().setProperty("StandardBasicTypes.use_outer_join","true");
	//getDefaultProperties().setProperty(Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE);
	//getDefaultProperties().setProperty("StandardBasicTypes.jdbc.batch_size", "1");
	
  } 
 
// public void registerColumnType(int code, int capacity, String name) {
//		super.registerColumnType(code, capacity,name);
//	}
//
// public void registerColumnType(int code, String name) {
//		super.registerColumnType(code,name);
//	}
//该方法用于添加SELECT语句到INSERT语句中,不是很确定????????????????????????????????????//
 public String appendIdentitySelectToInsert(String insertString)
 {
	 String insert_select = insertString.trim();
	 if(this.supportsInsertSelectIdentity())
	 {
		 if(insert_select.indexOf("values") != -1)
		 {
			 insert_select = insert_select.substring(0, insert_select.indexOf("values"));
		 }
		 //insert_select = insert_select+" select ";
	 }
	
		 return insert_select;
 }
 

   
 //在删除表之前是否需要先删除约束?
   public boolean dropConstraints() 
   { 
	   return false; 
	   
   } 
   
	
//  Does the dialect support some form of inserting and selecting the generated IDENTITY value all in the same statement?
	public boolean supportsInsertSelectIdentity()
	{
		return true;
	}
	

//提供向表中添加一列的语法字符串
	public String getAddColumnString() 
	{ 
		//throw new UnsupportedOperationException( "No add column syntax supported by Dialect"); 
		return "add column ";

		
	} 
	
//	提供向表中添加外键约束的语法字符串
	public String getAddForeignKeyConstraintString( String constraintName, String[] foreignKey, String referencedTable, String[] primaryKey)
	{ 
		//return new StringBuffer(30).append(" add constraint ").append(constraintName).append(" foreign key (").append(StringHelper.join(StringHelper.COMMA_SPACE, foreignKey)).append(")references ).append(referencedTable).toString(); " +

      String fk_sql ="add constraint \""+constraintName+"\" foreign key (";
      for(int i =0; i<foreignKey.length; i++)
      {
    	  if(i<foreignKey.length-1)
    	      fk_sql += "\""+foreignKey[i]+"\",";
    	  else
    		  fk_sql += "\""+foreignKey[i]+"\") ";
      }
      
      fk_sql +=" references \""+referencedTable+"\"(";
      for(int j =0; j<primaryKey.length; j++)
      {
    	  if(j<primaryKey.length-1)
    	      fk_sql += "\""+primaryKey[j]+"\",";
    	  else
    		  fk_sql += "\""+primaryKey[j]+"\") ";
      }
      
      return fk_sql;
	} 
	
	public String getAddPrimaryKeyConstraintString(String constraintName) 
	{
		return " add constraint \"" + constraintName + "\" primary key "; 
		
	} 
	
	public String getCascadeConstraintsString()
	{
		return " cascade";
	}
	
	public boolean dropTemporaryTableAfterUse() 
	{
		return false;
	}
	
	public boolean forUpdateOfColumns() 
	{
		return true;
	}
	

	//	提供创建临时表的语法
	public String getCreateTemporaryTableString() 
	{
		return "create global temporary table ";
	}
	
	
	 public String getCurrentTimestampSelectString() 
	 {
		 return "select now() ";
	 }
		 
		 
	 public String getForUpdateNowaitString() 
	 {
		 return "for update nowait";
	 }

	 public String getForUpdateNowaitString(String aliases)
	 {
		 return getForUpdateNowaitString()+" of "+aliases+" nowait";
	 }
	 
	 public String getForUpdateString(String aliases) 
	 {
		 return this.getForUpdateString()+" of "+aliases;
	 }

	 public String getLimitString(String query,int offset,int limit) 
	 {
		 if(limit>999999999)
			 limit = 999999999;
		 query = query.trim();
		 return query+" limit "+limit+" offset "+offset;
	 }


//无参数的limit ? offset ?语法
public String getLimitString(String sql) {   
  StringBuffer pagingSelect = new StringBuffer(100);;   
  pagingSelect.append(sql);;   
  pagingSelect.append(" limit ? offset ?");;   
  return pagingSelect.toString();   
}  

	 
//		 实现分页的语句；
	 // Hibernate会首先尝试用特定数据库的分页sql，如果没用，再尝试Scrollable，如果不行，最后采用rset.next()移动的办法。
	 public String getLimitString(String sql,boolean hasOffset) 
	 {
		 StringBuffer seper_sql = new StringBuffer(100);
		 sql = sql.trim();
		 seper_sql.append(sql);
		 if(hasOffset)
		 {
			 seper_sql.append(" limit ? offset ?");
		 }
		 else
		 {
			 seper_sql.append(" limit ?");
		 }
		 return seper_sql.toString();
	 }
	 
	 public String getIdentitySelectString(String table,String column,int type) throws MappingException 
	 {
		 table = table.trim();
		 column = column.trim();
		 return "select currserial('"+table+"."+column+"')";
	 }

	 public String getQuerySequencesString()
	 {
		 return "select \"seq_name\" from all_sequences";
	 }
	 
//不确定，出现问题值得检测，？？？？？？？？？？？？？？？？？？？
	  public int registerResultSetOutParameter(CallableStatement statement,int col) throws SQLException 
	 {
		  statement.registerOutParameter(col, Types.OTHER);
		  return col;
	 }
	  

	  public ResultSet getResultSet(CallableStatement ps) throws SQLException 
	  {
		 ps.execute(); 
		 return (ResultSet)ps.getObject(1);
		

	  }
	  
	  public String getSelectSequenceNextValString(String sequenceName)  throws MappingException
	  {
		  return "\""+sequenceName+"\".nextval";
	  }
		
	  public String getSequenceNextValString(String sequenceName) throws MappingException
	  {
		  return "select \"" + sequenceName + "\".nextval"; 
	  }
	 
	  public String getCreateSequenceString(String sequenceName)
		{
			return "create sequence \""+sequenceName+"\"";
		}
		
		public String getDropSequenceString(String sequenceName) 
		{
			return "drop sequence \"" + sequenceName+"\"";
		}
/*//   Should the value returned by getCurrentTimestampSelectString() be treated as callable.
	  public boolean isCurrentTimestampSelectStringCallable()
	  {
		  return true;
	  }*/
	  
	  public boolean supportsCommentOn() 
	  {
		  return true;
	  }
	  
	  public boolean supportsCurrentTimestampSelection() 
	  {
		  return true;
	  }
	  
//	不明确什么意思，？？？？？？？？？？？？？？？？？/
	  public boolean supportsEmptyInList()
	  {
		  return false;
	  }

	  public boolean supportsLimit() 
	  {
		  return true;
	  }
	  
//DATABASE是否支持初始化序列值和序列值的步长，？？？？？？？？？，自己理解的
	  public boolean supportsPooledSequences() 
	  {
		  return true;
	  }
	  
	  public boolean supportsSequences() 
	  {
		  return true;
	  }
	  
	  public boolean supportsTemporaryTables() 
	  {
		  return true;
		  
	  }
	  
	  public boolean supportsUnionAll() 
	  {
		  return true;
	  }
	  
//	true if the correct order is limit, offset
		public boolean bindLimitParametersInReverseOrder() 
		{ 
			return true; 
			
		}
//orcale end;  
//2009-7-6 cao 添加 MYSQL在DIALECT中重写的方法
	  public String getIdentityColumnString() 
	  {
		  return "identity(1,1)";
	  }
	  
	  public boolean isCurrentTimestampSelectStringCallable() 
	  {
		  return false;
	  }
	  
	   
//是否需要在索引名字前限定模式名
	  public boolean qualifyIndexName() 
	  {
		  return false;
	  }
	
//Does this dialect support identity column key generation? Returns:True if IDENTITY columns are supported; false otherwise.
//?????????????????????????////
	  public boolean supportsIdentityColumns() 
	  {
		  //return true;
		  return false;
		  
	  } 
	  
	  public boolean supportsIfExistsBeforeTableName() 
	  {
		  return false;
	  }
	  
//Basically, does it support syntax like "... where (FIRST_NAME, LAST_NAME) = ('Steve', 'Ebersole') ...".
	  public boolean supportsRowValueConstructorSyntax() 
	  {
		  return true;
	  }
	 
//end mysql;
	  
//下面是读规范,找到的一些需要重写的方法
	  public String generateTemporaryTableName(String baseTableName) 
	  {
		  return baseTableName.trim();
	  }
	 
	  public String getIdentityInsertString()
	  {
		  //return "default";
		  return null;
		 
	  }
	  
//这个不是十分确定,The keyword used to specify a nullable column.
	/*  public String getNullColumnString() 
	  {
		  return "null";
	  }*/
	  public String getNoColumnsInsertString()
	  {
		  return null;
	  }
	    
	  public String getLowercaseFunction() 
	  { 
			return "lower"; 
			
	  } 
		
		
	  public boolean supportsLimitOffset() 
	  { 
			return supportsLimit(); 
			
	   }
	  
	  public char closeQuote()
	  {
			 return '\'';
	  }
	  public char openQuote() 
	  {
	    	return '\''; 
	    	
	  } 
	  
//目前服务器暂时不支持在PreparedStatement中预处理limit ? offset ?语法
	  public boolean supportsVariableLimit() 
	  {
		return false;
			
	  } 
 
	   
}
	
