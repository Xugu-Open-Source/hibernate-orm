package com.xugu.dialect;

import jakarta.persistence.TemporalType;
import org.hibernate.QueryTimeoutException;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.*;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.dialect.unique.CreateTableUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.procedure.internal.StandardCallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.query.SemanticException;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.TemporalUnit;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.*;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static org.hibernate.LockOptions.*;
import static org.hibernate.cfg.BatchSettings.BATCH_VERSIONED_DATA;
import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.query.sqm.TemporalUnit.*;
import static org.hibernate.type.SqlTypes.DATE;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.*;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithNanos;

/**
 * @BelongsProject: XuguDialect6
 * @BelongsPackage: com.xugu.dialect
 * @Author: WQY
 * @CreateTime: 2024-03-28  11:38
 * @Description: TODO
 * @Version: 12.1.0
 */
public class XuguDialect extends Dialect {
    private static final Pattern DISTINCT_KEYWORD_PATTERN = Pattern.compile( "\\bdistinct\\b", CASE_INSENSITIVE );
    private static final Pattern GROUP_BY_KEYWORD_PATTERN = Pattern.compile( "\\bgroup\\s+by\\b", CASE_INSENSITIVE );
    private static final Pattern ORDER_BY_KEYWORD_PATTERN = Pattern.compile( "\\border\\s+by\\b", CASE_INSENSITIVE );
    private static final Pattern UNION_KEYWORD_PATTERN = Pattern.compile( "\\bunion\\b", CASE_INSENSITIVE );

    private static final Pattern SQL_STATEMENT_TYPE_PATTERN =
            Pattern.compile( "^(?:/\\*.*?\\*/)?\\s*(select|insert|update|delete)\\s+.*?", CASE_INSENSITIVE );

    private static final int PARAM_LIST_SIZE_LIMIT_1000 = 1000;

    /** Starting from 23c, 65535 parameters are supported for the IN condition. */
    private static final int PARAM_LIST_SIZE_LIMIT_65535 = 65535;

//    public static final String PREFER_LONG_RAW = "hibernate.dialect.xugu.prefer_long_raw";

    private static final String yqmSelect =
            "(trunc(%2$s, 'MONTH') + numtoyminterval(%1$s, 'MONTH') + (least(extract(day from %2$s), extract(day from last_day(trunc(%2$s, 'MONTH') + numtoyminterval(%1$s, 'MONTH')))) - 1))";

    private static final String ADD_YEAR_EXPRESSION = String.format( yqmSelect, "?2*12", "?3" );
    private static final String ADD_QUARTER_EXPRESSION = String.format( yqmSelect, "?2*3", "?3" );
    private static final String ADD_MONTH_EXPRESSION = String.format( yqmSelect, "?2", "?3" );

    private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 11 );

    private final UniqueDelegate uniqueDelegate = new CreateTableUniqueDelegate(this);

    protected final boolean autonomous;

    // Is MAX_STRING_SIZE set to EXTENDED?
    protected final boolean extended;


    public XuguDialect() {
        this(MINIMUM_VERSION);
    }

    public XuguDialect(DatabaseVersion version) {
        super( version);
        autonomous = false;
        extended = false;
    }

    public XuguDialect(DialectResolutionInfo info) {
        this( info, XuguServerConfiguration.fromDialectResolutionInfo( info ) );
    }

    public XuguDialect(DialectResolutionInfo info, XuguServerConfiguration serverConfiguration) {
        super( info );
        autonomous = serverConfiguration.isAutonomous();
        extended = serverConfiguration.isExtended();
    }

    public boolean isAutonomous() {
        return autonomous;
    }

    public boolean isExtended() {
        return extended;
    }

    @Override
    protected DatabaseVersion getMinimumSupportedVersion() {
        return MINIMUM_VERSION;
    }

    @Override
    public int getPreferredSqlTypeCodeForBoolean() {
        return Types.BIT;
    }

    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);
        final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();

        CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
        //.....
//        functionFactory.format_toChar();
//        functionFactory.math();
//        functionFactory.trigonometry();
//        functionFactory.cot();
//        functionFactory.log10();
//        functionFactory.square();
//        functionFactory.radians();
//        functionFactory.round();
        functionContributions.getFunctionRegistry().registerNamed("bin",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.BINARY ));
        functionContributions.getFunctionRegistry().registerNamed("hex",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.CHARACTER ));
        functionContributions.getFunctionRegistry().registerNamed("to_char",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("abs");
        functionContributions.getFunctionRegistry().registerNamed("sin",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE ));
        functionContributions.getFunctionRegistry().registerNamed("cos",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE ));
        functionContributions.getFunctionRegistry().registerNamed("tan",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE ));
        functionContributions.getFunctionRegistry().registerNamed("cot",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE ));
        functionContributions.getFunctionRegistry().registerNamed("asin",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE ));
        functionContributions.getFunctionRegistry().registerNamed("acos",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE ));
        functionContributions.getFunctionRegistry().registerNamed("atan",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE ));
        functionContributions.getFunctionRegistry().registerNamed("ln",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE ));
        functionContributions.getFunctionRegistry().registerNamed("log10",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE ));
        functionContributions.getFunctionRegistry().registerNamed("ceiling");
        functionContributions.getFunctionRegistry().registerNamed("floor");
        functionContributions.getFunctionRegistry().registerNamed("exp",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE ));
        functionContributions.getFunctionRegistry().registerNamed("sqrt",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE ));
        functionContributions.getFunctionRegistry().registerNamed("square",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE ));
        functionContributions.getFunctionRegistry().registerNamed("power",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE ));
        functionContributions.getFunctionRegistry().registerNamed("sign",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("radians");
        functionContributions.getFunctionRegistry().registerNamed("round");
        functionContributions.getFunctionRegistry().registerNamed("degress");
        functionContributions.getFunctionRegistry().registerNamed("trunc");
        functionContributions.getFunctionRegistry().registerNamed("is null",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.BOOLEAN ));
        functionContributions.getFunctionRegistry().registerNamed("isnull",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.BOOLEAN ));
        functionContributions.getFunctionRegistry().registerNamed("notnull",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.BOOLEAN ));

        functionContributions.getFunctionRegistry().registerNamed("var");
        functionContributions.getFunctionRegistry().registerNamed("stddev");
        functionContributions.getFunctionRegistry().registerNamed("stddevp");
        functionContributions.getFunctionRegistry().registerNamed("varp");
        functionContributions.getFunctionRegistry().registerNamed("min");
        functionContributions.getFunctionRegistry().registerNamed("max");
        functionContributions.getFunctionRegistry().registerNamed("avg");
        functionContributions.getFunctionRegistry().registerNamed("sum");

        functionContributions.getFunctionRegistry().registerNamed("pinyin",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("pinyin1",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING));
        functionContributions.getFunctionRegistry().registerNamed("position",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("box");
        functionContributions.getFunctionRegistry().registerNamed("point");
        functionContributions.getFunctionRegistry().registerNamed("atof",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE ));
        functionContributions.getFunctionRegistry().registerNamed("atoi",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("ltrim",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("rtrim",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("btrim",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("substr",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("currval",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.BIG_INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("nextval",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.BIG_INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("currserial",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.BIG_INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("nextserial",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.BIG_INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("len",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("heading",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("tailing",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("reverse_str",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("lower",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("upper",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("replicate",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("stuff",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("replace",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("concat",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("character_length",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("char_length",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("translate",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));

        functionContributions.getFunctionRegistry().registerNamed("getyear",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("getmonth",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("getday",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("gethour",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("getminute",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("getsecond",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("next_day",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.TIMESTAMP ));
        functionContributions.getFunctionRegistry().registerNamed("last_day",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.TIMESTAMP ));
        functionContributions.getFunctionRegistry().registerNamed("add_months",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.TIMESTAMP ));
        functionContributions.getFunctionRegistry().registerNamed("months_between",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("extract",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("now",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.TIMESTAMP ));
        functionContributions.getFunctionRegistry().registerNamed("to_date",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.TIMESTAMP ));
        functionContributions.getFunctionRegistry().registerNamed("sysdate",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.TIMESTAMP ));
        functionContributions.getFunctionRegistry().registerNamed("systime",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.TIME ));
        functionContributions.getFunctionRegistry().registerNamed("sysdatetime",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.TIMESTAMP ));
        functionContributions.getFunctionRegistry().registerNamed("overlaps",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.BOOLEAN ));
        functionContributions.getFunctionRegistry().registerNamed("extract_day",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("extract_hour",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("extract_minute",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("extract_second",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE ));
        functionContributions.getFunctionRegistry().registerNamed("extract_month",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("extract_year",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));

        functionContributions.getFunctionRegistry().registerNamed("count",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.LONG ));
        functionContributions.getFunctionRegistry().registerNamed("current_userid",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("current_date",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DATE ));
        functionContributions.getFunctionRegistry().registerNamed("current_time",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.TIME ));
        functionContributions.getFunctionRegistry().registerNamed("current_timestamp",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.TIMESTAMP ));
        functionContributions.getFunctionRegistry().registerNamed("current_user",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("current_database",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("current_db",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("current_db_id",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));
        functionContributions.getFunctionRegistry().registerNamed("current_ip",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("system_user",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ));
        functionContributions.getFunctionRegistry().registerNamed("sys_userid",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));

//        functionContributions.getFunctionRegistry().registerNamed("concat",typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ));
        functionFactory.concat_pipeOperator();


    }

    @Override
    public int getMaxVarcharLength() {
        //with MAX_STRING_SIZE=EXTENDED, changes to 32_767
        return extended ? 32_767 : 4000;
    }

    @Override
    public int getMaxVarbinaryLength() {
        //with MAX_STRING_SIZE=EXTENDED, changes to 32_767
        return extended ? 32_767 : 2000;
    }
    @Override
    public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
        return new StandardSqlAstTranslatorFactory() {
            @Override
            protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
                    SessionFactoryImplementor sessionFactory, Statement statement) {
                return new XuguSqlAstTranslator<>( sessionFactory, statement );
            }
        };
    }

    @Override
    public String currentDate() {
        return "current_date";
    }

    @Override
    public String currentTime() {
        return currentTimestamp();
    }

    @Override
    public String currentTimestamp() {
        return currentTimestampWithTimeZone();
    }

    @Override
    public String currentLocalTime() {
        return currentLocalTimestamp();
    }

    @Override
    public String currentLocalTimestamp() {
        return "localtimestamp";
    }

    @Override
    public String currentTimestampWithTimeZone() {
        return "current_timestamp";
    }

    @Override
    public boolean supportsInsertReturningGeneratedKeys() {
        return true;
    }
    @Override
    public String castPattern(CastType from, CastType to) {
        String result;
        switch ( to ) {
            case INTEGER:
            case LONG:
                result = BooleanDecoder.toInteger( from );
                if ( result != null ) {
                    return result;
                }
                break;
            case INTEGER_BOOLEAN:
                result = BooleanDecoder.toIntegerBoolean( from );
                if ( result != null ) {
                    return result;
                }
                break;
            case YN_BOOLEAN:
                result = BooleanDecoder.toYesNoBoolean( from );
                if ( result != null ) {
                    return result;
                }
                break;
            case BOOLEAN:
            case TF_BOOLEAN:
                result = BooleanDecoder.toTrueFalseBoolean( from );
                if ( result != null ) {
                    return result;
                }
                break;
            case STRING:
                switch ( from ) {
                    case INTEGER_BOOLEAN:
                    case TF_BOOLEAN:
                    case YN_BOOLEAN:
                        return BooleanDecoder.toString( from );
                    case DATE:
                        return "to_char(?1,'YYYY-MM-DD')";
                    case TIME:
                        return "to_char(?1,'HH24:MI:SS')";
                    case TIMESTAMP:
                        return "to_char(?1,'YYYY-MM-DD HH24:MI:SS.FF9')";
                    case OFFSET_TIMESTAMP:
                        return "to_char(?1,'YYYY-MM-DD HH24:MI:SS.FF9TZH:TZM')";
                    case ZONE_TIMESTAMP:
                        return "to_char(?1,'YYYY-MM-DD HH24:MI:SS.FF9 TZR')";
                }
                break;
            case CLOB:
                return "to_clob(?1)";
            case DATE:
                if ( from == CastType.STRING ) {
                    return "to_date(?1,'YYYY-MM-DD')";
                }
                break;
            case TIME:
                if ( from == CastType.STRING ) {
                    return "to_date(?1,'HH24:MI:SS')";
                }
                break;
            case TIMESTAMP:
                if ( from == CastType.STRING ) {
                    return "to_timestamp(?1,'YYYY-MM-DD HH24:MI:SS.FF9')";
                }
                break;
            case OFFSET_TIMESTAMP:
                if ( from == CastType.STRING ) {
                    return "to_timestamp_tz(?1,'YYYY-MM-DD HH24:MI:SS.FF9TZH:TZM')";
                }
                break;
            case ZONE_TIMESTAMP:
                if ( from == CastType.STRING ) {
                    return "to_timestamp_tz(?1,'YYYY-MM-DD HH24:MI:SS.FF9 TZR')";
                }
                break;
        }
        return super.castPattern(from, to);
    }
    @Override
    public long getFractionalSecondPrecisionInNanos() {
        return 1_000_000_000; //seconds
    }
    @Override
    public String extractPattern(TemporalUnit unit) {
        switch (unit) {
            case DAY_OF_WEEK:
                return "to_number(to_char(?2,'D'))";
            case DAY_OF_MONTH:
                return "to_number(to_char(?2,'DD'))";
            case DAY_OF_YEAR:
                return "to_number(to_char(?2,'DDD'))";
            case WEEK:
                return "to_number(to_char(?2,'IW'))"; //the ISO week number
            case WEEK_OF_YEAR:
                return "to_number(to_char(?2,'WW'))";
            case QUARTER:
                return "to_number(to_char(?2,'Q'))";
            case HOUR:
                return "to_number(to_char(?2,'HH24'))";
            case MINUTE:
                return "to_number(to_char(?2,'MI'))";
            case SECOND:
                return "to_number(to_char(?2,'SS'))";
            case EPOCH:
                return "trunc((cast(?2 at time zone 'UTC' as date) - date '1970-1-1')*86400)";
            default:
                return super.extractPattern(unit);
        }
    }
    @Override
    public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
        final StringBuilder pattern = new StringBuilder();
        switch ( unit ) {
            case YEAR:
                pattern.append( ADD_YEAR_EXPRESSION );
                break;
            case QUARTER:
                pattern.append( ADD_QUARTER_EXPRESSION );
                break;
            case MONTH:
                pattern.append( ADD_MONTH_EXPRESSION );
                break;
            case WEEK:
                if ( temporalType != TemporalType.DATE ) {
                    pattern.append( "(?3+numtodsinterval((?2)*7,'day'))" );
                }
                else {
                    pattern.append( "(?3+(?2)" ).append( unit.conversionFactor( DAY, this ) ).append( ")" );
                }
                break;
            case DAY:
                if ( temporalType == TemporalType.DATE ) {
                    pattern.append( "(?3+(?2))" );
                    break;
                }
            case HOUR:
            case MINUTE:
            case SECOND:
                pattern.append( "(?3+numtodsinterval(?2,'?1'))" );
                break;
            case NANOSECOND:
                pattern.append( "(?3+numtodsinterval((?2)/1e9,'second'))" );
                break;
            case NATIVE:
                pattern.append( "(?3+numtodsinterval(?2,'second'))" );
                break;
            default:
                throw new SemanticException( unit + " is not a legal field" );
        }
        return pattern.toString();
    }

    @Override
    public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
        final StringBuilder pattern = new StringBuilder();
        final boolean hasTimePart = toTemporalType != TemporalType.DATE || fromTemporalType != TemporalType.DATE;
        switch ( unit ) {
            case YEAR:
                extractField( pattern, YEAR, unit );
                break;
            case QUARTER:
            case MONTH:
                pattern.append( "(" );
                extractField( pattern, YEAR, unit );
                pattern.append( "+" );
                extractField( pattern, MONTH, unit );
                pattern.append( ")" );
                break;
            case DAY:
                if ( hasTimePart ) {
                    pattern.append( "(cast(?3 as date)-cast(?2 as date))" );
                }
                else {
                    pattern.append( "(?3-?2)" );
                }
                break;
            case WEEK:
            case MINUTE:
            case SECOND:
            case HOUR:
                if ( hasTimePart ) {
                    pattern.append( "((cast(?3 as date)-cast(?2 as date))" );
                }
                else {
                    pattern.append( "((?3-?2)" );
                }
                pattern.append( TemporalUnit.DAY.conversionFactor(unit ,this ) );
                pattern.append( ")" );
                break;
            case NATIVE:
            case NANOSECOND:
                if ( hasTimePart ) {
                    if ( supportsLateral() ) {
                        pattern.append( "(select extract(day from t.i)" ).append( TemporalUnit.DAY.conversionFactor( unit, this ) )
                                .append( "+extract(hour from t.i)" ).append( TemporalUnit.HOUR.conversionFactor( unit, this ) )
                                .append( "+extract(minute from t.i)" ).append( MINUTE.conversionFactor( unit, this ) )
                                .append( "+extract(second from t.i)" ).append( SECOND.conversionFactor( unit, this ) )
                                .append( " from(select ?3-?2 i from dual)t" );
                    }
                    else {
                        pattern.append( "(" );
                        extractField( pattern, DAY, unit );
                        pattern.append( "+" );
                        extractField( pattern, HOUR, unit );
                        pattern.append( "+" );
                        extractField( pattern, MINUTE, unit );
                        pattern.append( "+" );
                        extractField( pattern, SECOND, unit );
                    }
                }
                else {
                    pattern.append( "((?3-?2)" );
                    pattern.append( TemporalUnit.DAY.conversionFactor( unit, this ) );
                }
                pattern.append( ")" );
                break;
            default:
                throw new SemanticException( "Unrecognized field: " + unit );
        }
        return pattern.toString();
    }

    private void extractField(StringBuilder pattern, TemporalUnit unit, TemporalUnit toUnit) {
        pattern.append( "extract(" );
        pattern.append( translateExtractField( unit ) );
        pattern.append( " from (?3-?2)" );
        switch ( unit ) {
            case YEAR:
            case MONTH:
                pattern.append( " year(9) to month" );
                break;
            case DAY:
            case HOUR:
            case MINUTE:
            case SECOND:
                break;
            default:
                throw new SemanticException( unit + " is not a legal field" );
        }
        pattern.append( ")" );
        pattern.append( unit.conversionFactor( toUnit, this ) );
    }

    @Override
    protected String columnType(int sqlTypeCode) {
        switch ( sqlTypeCode ) {
            case BOOLEAN:
                // still, after all these years...
                return "number(1,0)";

            case TINYINT:
                return "number(3,0)";
            case SMALLINT:
                return "number(5,0)";
            case INTEGER:
                return "number(10,0)";
            case BIGINT:
                return "number(19,0)";
            case REAL:
                return "float(24)";
            case DOUBLE:
                return "float(53)";

            case NUMERIC:
            case DECIMAL:
                return "number($p,$s)";

            case DATE:
                return "date";
            case TIME:
                return "timestamp($p)";
            case TIME_WITH_TIMEZONE:
                return "timestamp($p) with time zone";

            case VARCHAR:
                return "varchar2($l char)";
            case NVARCHAR:
                return "nvarchar2($l)";

            case BINARY:
            case VARBINARY:
                return "raw($l)";

            default:
                return super.columnType( sqlTypeCode );
        }
    }
    @Override
    protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.registerColumnTypes( typeContributions, serviceRegistry );
        final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();
        //....
        ddlTypeRegistry.addDescriptor( new DdlTypeImpl( BIT, "BINARY", this ) );
        ddlTypeRegistry.addDescriptor( new DdlTypeImpl( REAL, "FLOAT", this ) );
        ddlTypeRegistry.addDescriptor( new DdlTypeImpl( DECIMAL, "NUMERIC($p,$s)", this ) );
        ddlTypeRegistry.addDescriptor( new DdlTypeImpl( DECIMAL, "NUMERIC($p,$s)", this ) );
        ddlTypeRegistry.addDescriptor( new DdlTypeImpl( CHAR, "CHAR($l)", this ) );
        ddlTypeRegistry.addDescriptor( new DdlTypeImpl( VARCHAR, "VARCHAR($l)", this ) );
        ddlTypeRegistry.addDescriptor( new DdlTypeImpl( LONGVARCHAR, "VARCHAR", this ) );
        ddlTypeRegistry.addDescriptor( new DdlTypeImpl( VARBINARY, "BINARY", this ) );
        ddlTypeRegistry.addDescriptor( new DdlTypeImpl( LONGVARBINARY, "BLOB", this ) );
    }
    @Override
    public TimeZoneSupport getTimeZoneSupport() {
        return TimeZoneSupport.NATIVE;
    }

    @Override
    protected void initDefaultProperties() {
        super.initDefaultProperties();
        getDefaultProperties().setProperty( BATCH_VERSIONED_DATA, "true" );
    }

    @Override
    public int getDefaultStatementBatchSize() {
        return 15;
    }

    /**
     *
     * @return false
     */
    @Override
    public boolean supportsBitType() {
        return false;
    }

    @Override
    public IdentityColumnSupport getIdentityColumnSupport() {
        return XuguIdentityColumnSupport.INSTANCE;
    }

    @Override
    public LimitHandler getLimitHandler() {
        return XuguLimitHandler.INSTANCE;
    }

    @Override
    public boolean supportsIfExistsBeforeTableName() {
        return getVersion().isSameOrAfter( 11 );
    }

    @Override
    public boolean supportsIfExistsAfterAlterTable() {
        return getVersion().isSameOrAfter( 11 );
    }

    @Override
    public boolean supportsIfExistsBeforeTypeName() {
        return getVersion().isSameOrAfter( 11 );
    }

    @Override
    public String getAlterColumnTypeString(String columnName, String columnType, String columnDefinition) {
        return "modify " + columnName + " " + columnType;
    }

    @Override
    public boolean supportsAlterColumnType() {
        return true;
    }

    @Override
    public SequenceSupport getSequenceSupport() {
        return XuguSequenceSupport.INSTANCE;
    }

    @Override
    public SequenceInformationExtractor getSequenceInformationExtractor() {
        return SequenceInformationExtractorXuguDatabaseImpl.INSTANCE;
    }

    @Override
    public String getSelectGUIDString() {
        return getVersion().isSameOrAfter( 11 ) ? "select rawtohex(sys_guid())" : "select rawtohex(sys_guid()) from dual";
    }

    @Override
    public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
        return EXTRACTOR;
    }

    private static final ViolatedConstraintNameExtractor EXTRACTOR =
            new TemplatedViolatedConstraintNameExtractor(sqle -> {
                switch ( JdbcExceptionHelper.extractErrorCode( sqle ) ) {
                    case 1:
                    case 2291:
                    case 2292:
                        return extractUsingTemplate( "(", ")", sqle.getMessage() );
                    case 1400:
                        // simple nullability constraint
                        return null;
                    default:
                        return null;
                }
            } );

    @Override
    public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
        return (sqlException, message, sql) -> {
            switch ( JdbcExceptionHelper.extractErrorCode( sqlException ) ) {

                // lock timeouts ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

                case 30006:
                    // ORA-30006: resource busy; acquire with WAIT timeout expired
                    throw new LockTimeoutException(message, sqlException, sql);
                case 54:
                    // ORA-00054: resource busy and acquire with NOWAIT specified or timeout expired
                    throw new LockTimeoutException(message, sqlException, sql);
                case 4021:
                    // ORA-04021 timeout occurred while waiting to lock object
                    throw new LockTimeoutException(message, sqlException, sql);

                    // deadlocks ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

                case 60:
                    // ORA-00060: deadlock detected while waiting for resource
                    return new LockAcquisitionException( message, sqlException, sql );
                case 4020:
                    // ORA-04020 deadlock detected while trying to lock object
                    return new LockAcquisitionException( message, sqlException, sql );

                // query cancelled ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

                case 1013:
                    // ORA-01013: user requested cancel of current operation
                    throw new QueryTimeoutException(  message, sqlException, sql );

                    // data integrity violation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

                case 1407:
                    // ORA-01407: cannot update column to NULL
                    final String constraintName = getViolatedConstraintNameExtractor().extractConstraintName( sqlException );
                    return new ConstraintViolationException( message, sqlException, sql, constraintName );

                default:
                    return null;
            }
        };
    }

    @Override
    public boolean supportsExistsInSelect() {
        return false;
    }

    @Override
    public int getInExpressionCountLimit() {
        return getVersion().isSameOrAfter( 11 )
                ? PARAM_LIST_SIZE_LIMIT_65535
                : PARAM_LIST_SIZE_LIMIT_1000;
    }

    @Override
    public boolean forceLobAsLastValue() {
        return true;
    }

    @Override
    public boolean isEmptyStringTreatedAsNull() {
        return true;
    }

    @Override
    public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
            EntityMappingType rootEntityDescriptor,
            RuntimeModelCreationContext runtimeModelCreationContext) {
        return new GlobalTemporaryTableMutationStrategy(
                TemporaryTable.createIdTable(
                        rootEntityDescriptor,
                        basename -> TemporaryTable.ID_TABLE_PREFIX + basename,
                        this,
                        runtimeModelCreationContext
                ),
                runtimeModelCreationContext.getSessionFactory()
        );
    }

    @Override
    public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
            EntityMappingType rootEntityDescriptor,
            RuntimeModelCreationContext runtimeModelCreationContext) {
        return new GlobalTemporaryTableInsertStrategy(
                TemporaryTable.createEntityTable(
                        rootEntityDescriptor,
                        name -> TemporaryTable.ENTITY_TABLE_PREFIX + name,
                        this,
                        runtimeModelCreationContext
                ),
                runtimeModelCreationContext.getSessionFactory()
        );
    }

    @Override
    public TemporaryTableKind getSupportedTemporaryTableKind() {
        return TemporaryTableKind.GLOBAL;
    }

    @Override
    public String getTemporaryTableCreateOptions() {
        return "on commit delete rows";
    }

    @Override
    public boolean useFollowOnLocking(String sql, QueryOptions queryOptions) {
        if ( isEmpty( sql ) || queryOptions == null ) {
            // ugh, used by DialectFeatureChecks (gotta be a better way)
            return true;
        }

        return DISTINCT_KEYWORD_PATTERN.matcher( sql ).find()
                || GROUP_BY_KEYWORD_PATTERN.matcher( sql ).find()
                || UNION_KEYWORD_PATTERN.matcher( sql ).find()
                || ORDER_BY_KEYWORD_PATTERN.matcher( sql ).find() && queryOptions.hasLimit()
                || queryOptions.hasLimit() && queryOptions.getLimit().getFirstRow() != null;
    }

    @Override
    public String getQueryHintString(String sql, String hints) {
        final String statementType = statementType( sql );
        final int start = sql.indexOf( statementType );
        if ( start < 0 ) {
            return sql;
        }
        else {
            int end = start + statementType.length();
            return sql.substring( 0, end ) + " /*+ " + hints + " */" + sql.substring( end );
        }
    }

    @Override
    public int getMaxAliasLength() {
        // Max identifier length is 30 for pre 12.2 versions, and 128 for 12.2+
        // but Hibernate needs to add "uniqueing info" so we account for that
        return 118;
    }

    @Override
    public int getMaxIdentifierLength() {
        // Since 12.2 version, maximum identifier length is 128
        return 128;
    }

    @Override
    public CallableStatementSupport getCallableStatementSupport() {
        return StandardCallableStatementSupport.REF_CURSOR_INSTANCE;
    }

    @Override
    public boolean canCreateSchema() {
        return false;
    }

    @Override
    public String getCurrentSchemaCommand() {
        return getVersion().isSameOrAfter( 23 ) ? "select sys_context('USERENV','CURRENT_SCHEMA')" : "SELECT SYS_CONTEXT('USERENV','CURRENT_SCHEMA') FROM DUAL";
    }

    @Override
    public boolean supportsPartitionBy() {
        return true;
    }

    private String statementType(String sql) {
        final Matcher matcher = SQL_STATEMENT_TYPE_PATTERN.matcher( sql );
        if ( matcher.matches() && matcher.groupCount() == 1 ) {
            return matcher.group(1);
        }
        else {
            throw new IllegalArgumentException( "Can't determine SQL statement type for statement: " + sql );
        }
    }

    @Override
    public boolean supportsOffsetInSubquery() {
        return true;
    }

    @Override
    public boolean supportsFetchClause(FetchClauseType type) {
        return true;
    }

    @Override
    public boolean supportsWindowFunctions() {
        return true;
    }

    @Override
    public boolean supportsRecursiveCTE() {
        return true;
    }

    @Override
    public boolean supportsLateral() {
        return true;
    }

    @Override
    public boolean supportsNoWait() {
        return true;
    }

    @Override
    public boolean supportsSkipLocked() {
        return true;
    }

    @Override
    public RowLockStrategy getWriteRowLockStrategy() {
        return RowLockStrategy.COLUMN;
    }

    @Override
    public String getForUpdateSkipLockedString() {
        return " for update skip locked";
    }

    @Override
    public String getForUpdateSkipLockedString(String aliases) {
        return " for update of " + aliases + " skip locked";
    }

    private String withTimeout(String lockString, int timeout) {
        switch ( timeout ) {
            case NO_WAIT:
                return supportsNoWait() ? lockString + " nowait" : lockString;
            case SKIP_LOCKED:
                return supportsSkipLocked() ? lockString + " skip locked" : lockString;
            case WAIT_FOREVER:
                return lockString;
            default:
                return supportsWait() ? lockString + " wait " + getTimeoutInSeconds( timeout ) : lockString;
        }
    }

    @Override
    public String getWriteLockString(String aliases, int timeout) {
        return withTimeout( getForUpdateString(aliases), timeout );
    }

    @Override
    public String getReadLockString(String aliases, int timeout) {
        return getWriteLockString( aliases, timeout );
    }

    @Override
    public void appendDateTimeLiteral(SqlAppender appender, TemporalAccessor temporalAccessor, TemporalType precision, TimeZone jdbcTimeZone) {
        if ( precision == TemporalType.TIMESTAMP && temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
            appender.appendSql( "timestamp '" );
            appendAsTimestampWithNanos( appender, temporalAccessor, true, jdbcTimeZone, false );
            appender.appendSql( '\'' );
        }
        else {
            super.appendDateTimeLiteral( appender, temporalAccessor, precision, jdbcTimeZone );
        }
    }

    @Override
    public void appendDatetimeFormat(SqlAppender appender, String format) {
        appender.appendSql( datetimeFormat( format, true, true ).result() );
    }

    public static Replacer datetimeFormat(String format, boolean useFm, boolean resetFm) {
        String fm = useFm ? "fm" : "";
        String fmReset = resetFm ? fm : "";
        return new Replacer( format, "'", "\"" )
                //era
                .replace("GG", "AD")
                .replace("G", "AD")

                //year
                .replace("yyyy", "YYYY")
                .replace("yyy", fm + "YYYY" + fmReset)
                .replace("yy", "YY")
                .replace("y", fm + "YYYY" + fmReset)

                //month of year
                .replace("MMMM", fm + "Month" + fmReset)
                .replace("MMM", "Mon")
                .replace("MM", "MM")
                .replace("M", fm + "MM" + fmReset)

                //week of year
                .replace("ww", "IW")
                .replace("w", fm + "IW" + fmReset)
                //year for week
                .replace("YYYY", "IYYY")
                .replace("YYY", fm + "IYYY" + fmReset)
                .replace("YY", "IY")
                .replace("Y", fm + "IYYY" + fmReset)

                //week of month
                .replace("W", "W")

                //day of week
                .replace("EEEE", fm + "Day" + fmReset)
                .replace("EEE", "Dy")
                .replace("ee", "D")
                .replace("e", fm + "D" + fmReset)

                //day of month
                .replace("dd", "DD")
                .replace("d", fm + "DD" + fmReset)

                //day of year
                .replace("DDD", "DDD")
                .replace("DD", fm + "DDD" + fmReset)
                .replace("D", fm + "DDD" + fmReset)

                //am pm
                .replace("a", "AM")

                //hour
                .replace("hh", "HH12")
                .replace("HH", "HH24")
                .replace("h", fm + "HH12" + fmReset)
                .replace("H", fm + "HH24" + fmReset)

                //minute
                .replace("mm", "MI")
                .replace("m", fm + "MI" + fmReset)

                //second
                .replace("ss", "SS")
                .replace("s", fm + "SS" + fmReset)

                //fractional seconds
                .replace("SSSSSS", "FF6")
                .replace("SSSSS", "FF5")
                .replace("SSSS", "FF4")
                .replace("SSS", "FF3")
                .replace("SS", "FF2")
                .replace("S", "FF1")

                //timezones
                .replace("zzz", "TZR")
                .replace("zz", "TZR")
                .replace("z", "TZR")
                .replace("ZZZ", "TZHTZM")
                .replace("ZZ", "TZHTZM")
                .replace("Z", "TZHTZM")
                .replace("xxx", "TZH:TZM")
                .replace("xx", "TZHTZM")
                .replace("x", "TZH"); //note special case
    }

    @Override
    public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
        appender.appendSql( "hextoraw('" );
        PrimitiveByteArrayJavaType.INSTANCE.appendString( appender, bytes );
        appender.appendSql( "')" );
    }

    @Override
    public boolean supportsNamedParameters(DatabaseMetaData databaseMetaData) {
        // Not sure if it's a JDBC driver issue, but it doesn't work
        return false;
    }


    @Override
    public String generatedAs(String generatedAs) {
        return " generated always as (" + generatedAs + ")";
    }

    @Override
    public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData)
            throws SQLException {
        builder.setAutoQuoteInitialUnderscore(true);
        return super.buildIdentifierHelper(builder, dbMetaData);
    }

    @Override
    public boolean canDisableConstraints() {
        return true;
    }

    @Override
    public String getDisableConstraintStatement(String tableName, String name) {
        return "alter table " + tableName + " disable constraint " + name;
    }

    @Override
    public String getEnableConstraintStatement(String tableName, String name) {
        return "alter table " + tableName + " enable constraint " + name;
    }

    @Override
    public UniqueDelegate getUniqueDelegate() {
        return uniqueDelegate;
    }

    @Override
    public String getCreateUserDefinedTypeKindString() {
        return "object";
    }

    @Override
    public String rowId(String rowId) {
        return "rowid";
    }

    @Override
    public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
        return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
    }
    /* ora */



    @Override
    public String getCurrentTimestampSelectString()
    {
        return "select sysdate from dual";
    }

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
    public boolean supportsTupleDistinctCounts()
    {
        return false;
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

    @Override
    public String getNativeIdentifierGeneratorStrategy(){
        return "sequence";
    }

    @Override
    public boolean dropConstraints() {
        return false;

    }

    @Override
    public String getAddColumnString() {
        // throw new UnsupportedOperationException( "No add column syntax
        // supported by Dialect");
        return "add column ";

    }
    @Override
    public String getAddPrimaryKeyConstraintString(String constraintName) {
        return " add constraint \"" + constraintName + "\" primary key ";

    }

    @Override
    public String getCascadeConstraintsString() {
        return " cascade";
    }

    @Override
    public String getForUpdateNowaitString() {
        return "for update nowait";
    }

    @Override
    public String getForUpdateNowaitString(String aliases) {
        return getForUpdateNowaitString() + " of " + aliases + " nowait";
    }

    @Override
    public String getForUpdateString(String aliases) {
        return this.getForUpdateString() + " of " + aliases;
    }

    @Override
    public String getQuerySequencesString() {
        return "select 'dbName' as \"sequence_catalog\", 'schemaName' as \"sequence_schema\",seq_name as \"sequence_name\","
                + " min_val as \"start_value\",min_val as \"minimum_value\",max_val as \"maximum_value\",step_val as \"increment\" from all_sequences";
    }

    @Override
    public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
        statement.registerOutParameter(col, Types.OTHER);
        return col;
    }

    @Override
    public ResultSet getResultSet(CallableStatement ps) throws SQLException {
        ps.execute();
        return (ResultSet) ps.getObject(1);
    }

    @Override
    public boolean supportsCommentOn() {
        return true;
    }

    @Override
    public boolean supportsCurrentTimestampSelection() {
        return true;
    }
    @Override
    public boolean isCurrentTimestampSelectStringCallable() {
        return false;
    }

    @Override
    public boolean qualifyIndexName() {
        return false;
    }

}