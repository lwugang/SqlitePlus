package com.leewug.src.sqliteplus;

import android.util.Log;

import java.util.Arrays;

/**
 * @author liwugang
 * @title
 * @date 2020-08-24
 * @email 13480020053@163.com
 */
public class Statement {
    private long ptr;
    private ConnectionPool pool;
    private Object[] mBindArgs;
    private final int numParameters;

    public Statement(ConnectionPool pool, String sql) {
        this.pool = pool;
        this.ptr = pool.beginTransaction(sql);
        if (ptr == 0 || ptr == -1)
            throw new DatabaseException("beginTransaction error");
        //获取sql中参数个数
        numParameters = getParametersSize(sql);
        mBindArgs = new Object[numParameters];
    }

    private int getParametersSize(String sql) {
        char[] chars = sql.toCharArray();
        int size = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '?') {
                size++;
            }
        }
        return size;
    }


    public void bindInt(int index, int value) {
        bind(index, value);
    }

    public void bindString(int index, String value) {
        if (value == null) {
            throw new IllegalArgumentException("the bind value at index " + index + " is null");
        }
        bind(index, value);
    }

    public void bindBoolean(int index, boolean value) {
        bind(index, value);
    }

    /**
     * 提交事务
     */
    public void commitTransaction() {
        pool.endTransaction(ptr, true);
    }

    public int execute() {
        int ret = pool.nextTransaction(ptr, mBindArgs);
        //执行一次之后，清除参数内容
        Arrays.fill(mBindArgs, null);
        return ret;
    }

    /**
     * 回滚事务
     */
    public void rollbackTransaction() {
        pool.endTransaction(ptr, false);
    }

    private void bind(int index, Object value) {
        if (index < 1 || index > numParameters) {
            throw new IllegalArgumentException("Cannot bind argument at index "
                    + index + " because the index is out of range.  "
                    + "The statement has " + numParameters + " parameters.");
        }
        mBindArgs[index - 1] = value;
    }
}
