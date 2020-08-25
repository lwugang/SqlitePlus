package com.leewug.src.sqliteplus;

import java.util.HashMap;

/**
 * @author liwugang
 * @title
 * @date 2020-08-24
 * @email 13480020053@163.com
 */
public final class ConnectionPool {
    final static HashMap<String, Long> referenceCache = new HashMap<>(0);
    private final String path;

    private long dbPtr;

    private ConnectionPool(String path) {
        this.path = path;
        if (!referenceCache.isEmpty()) {
            dbPtr = referenceCache.get(path);
        } else {
            dbPtr = nativeOpenDb(path);
            if (dbPtr == -1) {
                throw new DatabaseException();
            }
            //缓存数据库连接
            referenceCache.put(path, dbPtr);
        }
    }

    public static ConnectionPool create(String path) {
        return new ConnectionPool(path);
    }

    /**
     * 释放连接
     */
    public void release() {
        if (dbPtr != 0)
            nativeCloseDb(dbPtr);
        dbPtr = 0;
        referenceCache.remove(path);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        release();
    }

    /**
     * 打开数据库
     *
     * @param databasePath
     * @return
     */
    native long nativeOpenDb(String databasePath);

    /**
     * 关闭数据库连接
     *
     * @param dbPtr
     * @return
     */
    native int nativeCloseDb(long dbPtr);

    /**
     * 开启事务
     *
     * @param dbPtr
     * @param sql
     * @return
     */
    native long nativeBeginTransaction(long dbPtr, String sql);

    native String nativeQuery(long dbPtr, String sql);

    native int nativeExecute(long dbPtr, String sql);

    native int nativeNextStatement(long dbPtr, long statementPrt, Object[] args);

    native int nativeEndTransaction(long dbPtr, long statementPtr, boolean success);

    native int nativeInsert(long dbPtr, String sql,Object[][] values);

    static {
        System.loadLibrary("sqliteplus");
    }


    public long beginTransaction(String sql) {
        checkDb();
        return nativeBeginTransaction(dbPtr, sql);
    }

    /**
     * 结束事务
     *
     * @param ptr
     */
    public void endTransaction(long ptr, boolean success) {
        checkDb();
        nativeEndTransaction(dbPtr, ptr, success);
    }

    public int nextTransaction(long statementPtr, Object[] args) {
        checkDb();
        return nativeNextStatement(dbPtr, statementPtr, args);
    }

    public int execute(String sql) {
        checkDb();
        return nativeExecute(dbPtr, sql);
    }

    public String executeQuery(String sql) {
        checkDb();
        return nativeQuery(dbPtr, sql);
    }

    private void checkDb() {
        if (dbPtr == 0 || dbPtr == -1) {
            throw new DatabaseException("database open error");
        }
    }

    /**
     * 批量插入
     * @param sql
     * @param values
     */
    public void insert(String sql,Object[][] values){
        checkDb();
        nativeInsert(dbPtr,sql,values);
    }
}
