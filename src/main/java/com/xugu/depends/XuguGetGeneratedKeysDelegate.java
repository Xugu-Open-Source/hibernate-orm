package com.xugu.depends;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.PostInsertIdentityPersister;

public class XuguGetGeneratedKeysDelegate extends org.hibernate.dialect.identity.GetGeneratedKeysDelegate {
	private String keyColumns[];

	public XuguGetGeneratedKeysDelegate(PostInsertIdentityPersister persister,
			Dialect dialect) {
		super(persister, dialect);
		keyColumns = getPersister().getRootTableKeyColumnNames();
		if (keyColumns.length > 1) {
			throw new HibernateException(
					"Identity generator cannot be used with multi-column keys");
		}

	}

	protected PreparedStatement prepare(String insertSQL,
			SessionImplementor session) throws SQLException {
		return session.getJdbcCoordinator().getStatementPreparer()
				.prepareStatement(insertSQL, keyColumns);
	}

}
