package com.xugu.dialect;

import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @BelongsProject: XuguDialect6
 * @BelongsPackage: com.xugu.dialect
 * @Author: WQY
 * @CreateTime: 2024-03-28  20:52
 * @Description: TODO
 * @Version: 12.1.0
 */
public class SequenceInformationExtractorXuguDatabaseImpl extends SequenceInformationExtractorLegacyImpl {
    /**
     * Singleton access
     */
    public static final SequenceInformationExtractorXuguDatabaseImpl INSTANCE = new SequenceInformationExtractorXuguDatabaseImpl();

    @Override
    protected String sequenceCatalogColumn() {
        return null;
    }

    @Override
    protected String sequenceSchemaColumn() {
        return null;
    }

    @Override
    protected String sequenceStartValueColumn() {
        return null;
    }

    @Override
    protected String sequenceMinValueColumn() {
        return "minimum_value";
    }

    @Override
    protected String sequenceMaxValueColumn() {
        return "maximum_value";
    }


    @Override
    protected Number resultSetIncrementValue(ResultSet resultSet) throws SQLException {
        return resultSet.getBigDecimal( sequenceIncrementColumn() );
    }

    @Override
    protected Number resultSetMinValue(ResultSet resultSet) throws SQLException {
        return resultSet.getBigDecimal( sequenceMinValueColumn() );
    }

    @Override
    protected Number resultSetMaxValue(ResultSet resultSet) throws SQLException {
        return resultSet.getBigDecimal( sequenceMaxValueColumn() );
    }

    @Override
    protected String sequenceIncrementColumn() {
        return "increment";
    }
}