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
    private String dbPath;

    Database(Context context, String path) {
        this.context = context;
        this.dbPath = path;
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
     * @param tableName 表名
     * @param indexName 索引名
     * @param columns   索引列
     */
    public void createIndex(String tableName, String indexName, String... columns) {
        StringBuilder sb = new StringBuilder(String.format("CREATE INDEX IF NOT EXISTS %s ON %s (", indexName, tableName));
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
     * 删除索引
     *
     * @param indexName 索引名称
     */
    public void dropIndex(String indexName) {
        execute("DROP INDEX " + indexName);
    }

    /**
     * 快速创建一个表，所有字段类型默认 text,默认带有自增列 _id
     *
     * @param tableName 表名
     * @param columns 列
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
     * 删除表
     *
     * @param tableName
     */
    public void dropTable(String tableName) {
        String sql = "DROP TABLE IF EXISTS " + tableName;
        execute(sql);
    }

    /**
     * 删除数据库文件
     */
    public void deleteDatabase() {
        this.close();
        File file = new File(dbPath);
        String fileName = file.getName();
        if (file.exists()) {
            file.delete();
        }
        this.deleteFile(file.getParent(), fileName + "-journal");
        this.deleteFile(file.getParent(), fileName + "-wal");
        this.deleteFile(file.getParent(), fileName + "-shm");
    }

    void deleteFile(String parent, String filePath) {
        File file = new File(parent, filePath);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 批量插入
     *
     * @param sql
     * @param values
     */
    public void insertBatch(String sql, Object[][] values) {
        pool.insert(sql, values);
    }

    /**
     * 关闭数据库连接
     */
    public void close() {
        pool.release();
    }
}
