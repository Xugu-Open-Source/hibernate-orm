package com.xugu.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.dialect.identity.GetGeneratedKeysDelegate;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.sql.ANSIJoinFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import com.xugu.depends.XuguGetGeneratedKeysDelegate;

public class XuguDialect5 extends XuguDialect4{
	 private IdentityColumnSupportImpl identityColumnSupport = null;
	public XuguDialect5(){
		super();
		this.registerFunction("concat", new VarArgsSQLFunction(StandardBasicTypes.STRING, "(", "+", ")"));
		identityColumnSupport = new IdentityColumnSupportImpl(){

            @Override
		    public boolean supportsIdentityColumns()
		    {
		        return true;
		    }

            @Override
		    public boolean supportsInsertSelectIdentity()
		    {
		        return true;
		    }


            @Override
		    public String getIdentityColumnString(int type)
		    {
		        return "identity(1,1)";
		    }

            @Override
		    public GetGeneratedKeysDelegate buildGetGeneratedKeysDelegate(PostInsertIdentityPersister persister, Dialect dialect)
		    {
		        return new XuguGetGeneratedKeysDelegate(persister, dialect);
		    }
		   
		 
	 };
	}

    @Override
	public String getCurrentTimestampSelectString()
    {
        return "select sysdate from dual";
    }


    @Override
    public String getCurrentTimestampSQLFunctionName()
    {
        return "sysdate";
    }


    @Override
    public String getForUpdateString()
    {
        return " for update";
    }


    @Override
    public LimitHandler getLimitHandler()
    {
        return LIMIT_HANDLER;
    }


    @Override
    public String getLimitString(String sql, boolean hasOffset)
    {
        sql = sql.trim();
        String forUpdateString = null;
        boolean isForUpdate = false;
        int forUpdateIndex = sql.toLowerCase(Locale.ROOT).lastIndexOf("for update");
        if(forUpdateIndex > -1)
        {
            forUpdateString = sql.substring(forUpdateIndex);
            sql = sql.substring(0, forUpdateIndex - 1);
            isForUpdate = true;
        }
        StringBuilder pagingSelect = new StringBuilder(sql.length() + 50);
        pagingSelect.append(sql);
        if(hasOffset) {
            pagingSelect.append(" limit ? offset ?");
        } else {
            pagingSelect.append(" limit ?");
        }
        if(isForUpdate)
        {
            pagingSelect.append(" ");
            pagingSelect.append(forUpdateString);
        }
        return pagingSelect.toString();
    }
    private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {

        @Override
        public String processSql(String sql, RowSelection selection)
        {
            boolean hasOffset = LimitHelper.hasFirstRow(selection);
            sql = sql.trim();
            String forUpdateClause = null;
            boolean isForUpdate = false;
            int forUpdateIndex = sql.toLowerCase(Locale.ROOT).lastIndexOf("for update");
            if(forUpdateIndex > -1){
                forUpdateClause = sql.substring(forUpdateIndex);
                sql = sql.substring(0, forUpdateIndex - 1);
                isForUpdate = true;
            }
            StringBuilder pagingSelect = new StringBuilder(sql.length() + 100);
            pagingSelect.append(sql);
            if(hasOffset) {
                pagingSelect.append(" limit ? offset ?");
            } else {
                pagingSelect.append(" limit ?");
            }
            
            if(isForUpdate)
            {
                pagingSelect.append(" ");
                pagingSelect.append(forUpdateClause);
            }
            return pagingSelect.toString();
        }

        @Override
        public boolean supportsLimit()
        {
            return true;
        }

        @Override
        public boolean bindLimitParametersInReverseOrder()
        {
            return true;
        }

        @Override
        public boolean useMaxForLimit()
        {
            return true;
        }

    };

    @Override
    public String getWriteLockString(int timeout)
    {
        if(timeout == 0) {
            return " for update nowait";
        }
        if(timeout > 0){
            float seconds = (float)timeout / 1000F;
            timeout = Math.round(seconds);
            return (new StringBuilder()).append(" for update wait ").append(timeout).toString();
        } else{
            return " for update";
        }
    }

    @Override
    public String getReadLockString(int timeout)
    {
        return getWriteLockString(timeout);
    }

    @Override
    public boolean supportsRowValueConstructorSyntaxInInList()
    {
        return true;
    }

    @Override
    public boolean supportsTupleDistinctCounts()
    {
        return false;
    }
    
    //--------------------------------10--------------------------------------

    @Override
    public JoinFragment createOuterJoinFragment()
    {
        return new ANSIJoinFragment();
    }

    @Override
    public String getCrossJoinSeparator()
    {
        return " cross join ";
    }

    @Override
    public ResultSet getResultSet(CallableStatement statement, int position) throws SQLException 
    {
		return (ResultSet) statement.getObject(position);
	}

    @Override
	public ResultSet getResultSet(CallableStatement statement, String name) throws SQLException 
	{
		return (ResultSet) statement.getObject(name);
	}

    @Override
	public int registerResultSetOutParameter(CallableStatement statement,String name) throws SQLException 
	{
		statement.registerOutParameter(name, com.xugu.cloudjdbc.Types.REFCUR);
		return 1;
	}
	
	
	//------------------------------- 12 -----------------------------------------------
	 protected void registerDefaultProperties() {
		getDefaultProperties().setProperty( "hibernate.jdbc.use_streams_for_binary", "true");
		getDefaultProperties().setProperty( "hibernate.jdbc.batch_size", "32767");
		getDefaultProperties().setProperty( "hibernate.jdbc.use_get_generated_keys", "true");
		getDefaultProperties().setProperty( "hibernate.jdbc.batch_versioned_data", "false");
	}



    @Override
	 public IdentityColumnSupport getIdentityColumnSupport(){
	        return identityColumnSupport;
	 }

    @Override
	 public String getNativeIdentifierGeneratorStrategy(){
	        return "sequence";
	 }

}
