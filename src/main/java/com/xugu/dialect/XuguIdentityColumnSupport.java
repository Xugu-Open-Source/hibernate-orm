package com.xugu.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.id.insert.GetGeneratedKeysDelegate;

/**
 * @BelongsProject: XuguDialect6
 * @BelongsPackage: com.xugu.dialect
 * @Author: WQY
 * @CreateTime: 2024-03-28  19:58
 * @Description: TODO
 * @Version: 12.1.0
 */
public class XuguIdentityColumnSupport extends IdentityColumnSupportImpl {

    public static final XuguIdentityColumnSupport INSTANCE = new XuguIdentityColumnSupport();
    @Override
    public boolean supportsIdentityColumns() {
        return true;
    }

    @Override
    public String getIdentityColumnString(int type) {
        return "generated as identity";
    }

    @Override
    public GetGeneratedKeysDelegate buildGetGeneratedKeysDelegate(
            PostInsertIdentityPersister persister, Dialect dialect) {
        return new GetGeneratedKeysDelegate( persister, dialect, false );
    }

    @Override
    public String getIdentityInsertString() {
        return "default";
    }
}