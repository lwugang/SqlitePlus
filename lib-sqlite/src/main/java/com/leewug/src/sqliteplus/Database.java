package com.leewug.src.sqliteplus;

import android.content.Context;
import android.util.Log;

import java.io.File;

/**
 * @author liwugang
 * @title
 * @date 2020-08-24
 * @email 13480020053@163.com
 */
public final class Database {
    private final Context context;
    private final ConnectionPool pool;

    public Database(Context context, String path) {
        this.context = context;
        String dbPath = path;
        if (!path.contains(File.separator)) {
            dbPath = context.getDatabasePath(path).getAbsolutePath();
        }
        pool = ConnectionPool.create(dbPath);
    }

    /**
     * 打开或者创建数据库
     *
     * @param path
     * @return
     */
    public static Database openOrCreateDatabase(Context context, String path) {
        return new Database(context, path);
    }

    /**
     * 开启事务并预编译sql
     *
     * @param sql
     */
    public Statement beginTransaction(String sql) {
        return new Statement(pool, sql);
    }

    /**
     * 执行sql
     *
     * @param sql 返回执行状态
     * @return
     */
    public int execute(String sql) {
        return pool.execute(sql);
    }

    /**
     * 执行sql
     *
     * @param sql 返回第一列的结果
     * @return
     */
    public String executeQuery(String sql) {
        return pool.executeQuery(sql);
    }

    /**
     * 快速给表创建索引
     *
     * @param tableName
     */
    public void createIndex(String tableName, String... columns) {
        StringBuilder sb = new StringBuilder(String.format("CREATE INDEX IF NOT EXISTS IDX_%s_QUERY ON %s (", tableName, tableName));
        for (int i = 0; i < columns.length; i++) {
            if (i != 0) {
                sb.append(",");
            }
            sb.append(String.format("\"%s\" ASC", columns[i]));
        }
        sb.append(");");
        execute(sb.toString());
    }

    /**
     * 快速创建一个表，所有字段类型默认 text
     *
     * @param tableName
     * @param columns
     */
    public void createTable(String tableName, String... columns) {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS %s(_id INTEGER PRIMARY KEY AUTOINCREMENT ,");
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                sql.append(",");
            }
            sql.append(columns[i]);
            sql.append(" TEXT");
        }
        sql.append(");");
        String format = String.format(sql.toString(), tableName);
        execute(format);
    }

    /**
     * 批量插入
     * @param sql
     * @param values
     */
    public void insertBatch(String sql,Object[][] values){
        pool.insert(sql, values);
    }

    /**
     * 关闭数据库连接
     */
    public void close() {
        pool.release();
    }
}
