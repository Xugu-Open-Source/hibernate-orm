package com.xugu.dialect;

import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.internal.util.config.ConfigurationHelper;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * @BelongsProject: XuguDialect6
 * @BelongsPackage: com.xugu.dialect
 * @Author: WQY
 * @CreateTime: 2024-03-28  20:59
 * @Description: TODO
 * @Version: 12.1.0
 */
public class XuguServerConfiguration {
    private final boolean autonomous;
    private final boolean extended;

    public boolean isAutonomous() {
        return autonomous;
    }

    public boolean isExtended() {
        return extended;
    }

    public XuguServerConfiguration(boolean autonomous, boolean extended) {
        this.autonomous = autonomous;
        this.extended = extended;
    }

    public static XuguServerConfiguration fromDialectResolutionInfo(DialectResolutionInfo info) {
        Boolean extended = null;
        Boolean autonomous = null;
        final DatabaseMetaData databaseMetaData = info.getDatabaseMetadata();
        if ( databaseMetaData != null ) {
            try (final Statement statement = databaseMetaData.getConnection().createStatement()) {
                final ResultSet rs = statement.executeQuery(
                        "select cast('string' as varchar(32000)), " +
                                "sys_context('USERENV','CLOUD_SERVICE') from dual"
                );
                if ( rs.next() ) {
                    // succeeded, so MAX_STRING_SIZE == EXTENDED
                    extended = true;
                    autonomous = isAutonomous( rs.getString( 2 ) );
                }
            }
            catch (SQLException ex) {
                // failed, so MAX_STRING_SIZE == STANDARD, still need to check autonomous
                extended = false;
                autonomous = isAutonomous( databaseMetaData );
            }
        }
        // default to the dialect-specific configuration settings
        if ( extended == null ) {
            extended = ConfigurationHelper.getBoolean(
                    "hibernate.dialect.xugu.extended_string_size",
                    info.getConfigurationValues(),
                    false
            );
        }
        if ( autonomous == null ) {
            autonomous = ConfigurationHelper.getBoolean(
                    "hibernate.dialect.xugu.is_autonomous",
                    info.getConfigurationValues(),
                    false
            );
        }
        return new XuguServerConfiguration( autonomous, extended );
    }

    private static boolean isAutonomous(String cloudServiceParam) {
        return cloudServiceParam != null && List.of( "OLTP", "DWCS", "JDCS" ).contains( cloudServiceParam );
    }

    private static boolean isAutonomous(DatabaseMetaData databaseMetaData) {
        try (final Statement statement = databaseMetaData.getConnection().createStatement()) {
            return statement.executeQuery( "select 1 from dual where sys_context('USERENV','CLOUD_SERVICE') in ('OLTP','DWCS','JDCS')" ).next();
        }
        catch (SQLException ex) {
            // Ignore
        }
        return false;
    }
}