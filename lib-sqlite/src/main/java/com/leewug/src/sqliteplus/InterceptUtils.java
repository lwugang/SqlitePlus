package com.leewug.src.sqliteplus;

import android.text.TextUtils;

import java.io.File;

/**
 * @author liwugang
 * @title
 * @date 2020-08-04
 * @email 13480020053@163.com
 */
public class InterceptUtils {


    public static final String COLUMN_WAYBILL = "WAYBILL_NO";
    public static final String COLUMN_CREATE_DATE = "CREATE_DATE";

    private static InterceptUtils INSTANCE;
    private String databasePath;
    private String tabName;
    private long dbPtr;
    private long statementPtr;

    private InterceptUtils() {
    }

    public void init(String databasePath, String tabName) {
        if (dbPtr != 0)
            return;
        this.databasePath = databasePath;
        File file = new File(databasePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdir();
        }
        this.tabName = tabName;
        dbPtr = nativeOpenDb(databasePath);
        nativeCreateTable(dbPtr, tabName);
    }

    public static InterceptUtils get() {
        if (INSTANCE == null) {
            synchronized (InterceptUtils.class) {
                if (INSTANCE == null) {
                    INSTANCE = new InterceptUtils();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 打开数据库
     *
     * @param databasePath
     * @return
     */
    native long nativeOpenDb(String databasePath);

    native int nativeCloseDb(long dbPtr);

    native int nativeParseAll(long dbPtr, String interceptFilePath, String sql, String waybillSuffix);

    native int nativeParseIncrement(long dbPtr, String interceptFilePath, String sql, String waybillSuffix);



    native long nativeBeginTransaction(long dbPtr, String tabName, String sql);

    native void nativeBindWaybillNoAndDate(long statementPrt, String waybillNo, String date);

    native int nativeCreateTable(long dbPtr, String tabName);

    native int nativeQueryWaybillNo(long dbPtr, String tabName, String waybillNo);

    native int nativeExecute(long dbPtr, String tabName, String sql);

    native void nativeNextStatement(long statementPrt);

    native int nativeCommitTransaction(long dbPtr, long statementPtr);

    /**
     * 解析全量拦截件数据并插入数据库
     */
    public int parseAll(String interceptFilePath, String waybillSuffix) {
        check();
        String sql = String.format("insert into %s values(?,?);", tabName);
        int ret = nativeParseAll(dbPtr, interceptFilePath, sql, waybillSuffix);
        return ret;
    }

    private void check() {
        if (TextUtils.isEmpty(databasePath))
            throw new RuntimeException("databasePath is null");
        if (dbPtr == 0) {
            dbPtr = nativeOpenDb(databasePath);
            if (dbPtr == 0 || dbPtr == -1) {
                throw new RuntimeException("database open error");
            }
        }
    }


    /**
     * 创建表
     */
    public int createTable() {
        int ret = nativeCreateTable(dbPtr, tabName);
        return ret;
    }

    /**
     * 删除表
     */
    public int dropTable() {
        int ret = nativeExecute(dbPtr, tabName, String.format("drop table IF EXISTS %s;", tabName));
        return ret;
    }

    /**
     * 执行sql
     *
     * @param sql
     */
    public int executeSql(String sql) {
        check();
        int ret = nativeExecute(dbPtr, tabName, sql);
        return ret;
    }

    /**
     * 解析增量拦截件数据并插入数据库
     */
    public int parseIncrement(String interceptFilePath, String waybillSuffix) {
        check();
        String sql = String.format("insert into %s values(?,?);", tabName);
        int ret = nativeParseIncrement(dbPtr, interceptFilePath, sql, waybillSuffix);
        return ret;
    }

    /**
     * 是否存在指定运单号
     *
     * @param waybillNo
     * @return
     */
    public boolean isExistWaybillNo(String waybillNo) {
        check();
        int ret = nativeQueryWaybillNo(dbPtr, tabName, waybillNo);
        return ret > 0;
    }

    /**
     * 开启事务
     */
    public void beginTransaction(String sql) {
        check();
        statementPtr = nativeBeginTransaction(dbPtr, tabName, sql);
    }

    /**
     * 开启事务
     */
    public void beginTransaction() {
        check();
        statementPtr = nativeBeginTransaction(dbPtr, tabName, String.format("insert into %s values(?,?);", tabName));
    }

    /**
     * 执行预编译
     */
    public void executeStatement() {
        if (statementPtr == 0)
            throw new RuntimeException("请先开启事务beginTransaction");
        nativeNextStatement(statementPtr);
    }

    /**
     * 绑定内容
     *
     * @param waybillNo
     * @param date
     */
    public void bindContent(String waybillNo, String date) {
        if (statementPtr == 0)
            throw new RuntimeException("请先开启事务beginTransaction");
        nativeBindWaybillNoAndDate(statementPtr, waybillNo, date);
    }

    /**
     * 提交事务
     */
    public void commitTransaction() {
        if (statementPtr == 0)
            throw new RuntimeException("请先开启事务beginTransaction");
        nativeCommitTransaction(dbPtr, statementPtr);
        statementPtr = 0;
    }

    public void destroy() {
        nativeCloseDb(dbPtr);
        dbPtr = 0;
        statementPtr = 0;
        INSTANCE = null;
    }
}
