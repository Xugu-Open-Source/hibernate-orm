package com.xugu.dialect;

import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;

import java.util.Locale;

/**
 * @BelongsProject: XuguDialect6
 * @BelongsPackage: com.xugu.dialect
 * @Author: WQY
 * @CreateTime: 2024-03-28  19:38
 * @Description: TODO
 * @Version: 12.1.0
 */
public class XuguLimitHandler extends AbstractLimitHandler {

    private boolean bindLimitParametersInReverseOrder;
    private boolean useMaxForLimit;
    private boolean supportOffset;

    public static final XuguLimitHandler INSTANCE = new XuguLimitHandler();

    XuguLimitHandler() {}

    @Override
    public String processSql(String sql, Limit limit, QueryOptions queryOptions) {
        boolean hasOffset = hasFirstRow(limit);
        sql = sql.trim();
        String forUpdateClause = null;
        boolean isForUpdate = false;
        int forUpdateIndex = sql.toLowerCase(Locale.ROOT).lastIndexOf("for update");
        if (forUpdateIndex > -1) {
            bindLimitParametersInReverseOrder = true;
            useMaxForLimit = true;
            supportOffset = false;
            forUpdateClause = sql.substring(forUpdateIndex);
            sql = sql.substring(0, forUpdateIndex - 1);
            isForUpdate = true;
        }else {
            bindLimitParametersInReverseOrder = false;
            useMaxForLimit = false;
            supportOffset = true;
        }

        StringBuilder pagingSelect = new StringBuilder(sql.length() + 100);
        pagingSelect.append(sql);
        if (hasOffset) {
            pagingSelect.append(" limit ? offset ?");
        } else {
            pagingSelect.append(" limit ?");
        }

        if (isForUpdate) {
            pagingSelect.append(" ");
            pagingSelect.append(forUpdateClause);
        }

        return pagingSelect.toString();
    }

    @Override
    public final boolean supportsLimit() {
        return true;
    }

    @Override
    public boolean supportsOffset() {
        return supportOffset;
    }

    @Override
    public boolean bindLimitParametersInReverseOrder() {
        return bindLimitParametersInReverseOrder;
    }

    @Override
    public boolean useMaxForLimit() {
        return useMaxForLimit;
    }


}