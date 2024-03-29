package com.xugu.dialect;

import org.hibernate.dialect.sequence.NextvalSequenceSupport;
import org.hibernate.dialect.sequence.OracleSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;

/**
 * @BelongsProject: XuguDialect6
 * @BelongsPackage: com.xugu.dialect
 * @Author: WQY
 * @CreateTime: 2024-03-28  20:56
 * @Description: TODO
 * @Version: 12.1.0
 */
public class XuguSequenceSupport  extends NextvalSequenceSupport {

    public static final SequenceSupport INSTANCE = new OracleSequenceSupport();

    @Override
    public String getFromDual() {
        return " from dual";
    }

    @Override
    public boolean sometimesNeedsStartingValue() {
        return true;
    }
}