package com.leewug.src.sqliteplus;

/**
 * @author liwugang
 * @title
 * @date 2020-08-24
 * @email 13480020053@163.com
 */
public class DatabaseException extends RuntimeException {
    public DatabaseException() {
    }

    public DatabaseException(String message) {
        super(message);
    }
}
