#include <jni.h>
#include <string>
#include <android/log.h>
#include <exception>
#include "sqlite3.h"
#include "sqdb.h"


extern "C"
JNIEXPORT jint JNICALL
Java_com_leewug_src_sqliteplus_ConnectionPool_nativeCloseDb(JNIEnv *env, jobject thiz,
                                                            jlong db_ptr) {
    if (db_ptr == 0)
        return -1;
    sqlite3 *db = (sqlite3 *) db_ptr;
    sqlite3_db_release_memory(db);
    sqlite3_close_v2(db);
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_leewug_src_sqliteplus_ConnectionPool_nativeExecute(JNIEnv *env, jobject thiz, jlong db_ptr,
                                                            jstring j_sql) {
    const char *sql = env->GetStringUTFChars(j_sql, 0);
    try {
        sqdb::Db db = sqdb::Db((sqlite3 *) db_ptr);
        db.BeginTransaction();
        db.exec(sql);
        db.CommitTransaction();
        env->ReleaseStringUTFChars(j_sql, sql);
        return 0;
    } catch (sqdb::Exception e) {
        env->ReleaseStringUTFChars(j_sql, sql);
        __android_log_print(ANDROID_LOG_ERROR, "sqliteplus", "error %s", e.GetErrorMsg());
        return -1;
    }
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_leewug_src_sqliteplus_ConnectionPool_nativeBeginTransaction(JNIEnv *env, jobject thiz,
                                                                     jlong db_ptr,
                                                                     jstring j_sql) {
    const char *sql = env->GetStringUTFChars(j_sql, 0);
    try {
        sqdb::Db db = sqdb::Db((sqlite3 *) db_ptr);
        sqdb::Statement stmt(db.Query(sql));
        db.BeginTransaction();
        env->ReleaseStringUTFChars(j_sql, sql);
        return (jlong) stmt.toAddr();
    } catch (sqdb::Exception e) {
        __android_log_print(ANDROID_LOG_ERROR, "sqliteplus", "error %s", e.GetErrorMsg());
        env->ReleaseStringUTFChars(j_sql, sql);
        return -1;
    }
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_leewug_src_sqliteplus_ConnectionPool_nativeQuery(JNIEnv *env, jobject thiz, jlong db_ptr,
                                                          jstring j_sql) {
    const char *sql = env->GetStringUTFChars(j_sql, 0);
    try {
        sqdb::Db db = sqdb::Db((sqlite3 *) db_ptr);
        sqdb::Statement stmt(db.Query(sql));
        stmt.Next(false);
        const char *ret = stmt.GetField(0);
        jstring result = env->NewStringUTF(ret);
        env->ReleaseStringUTFChars(j_sql, sql);
        stmt.release();
        return result;
    } catch (sqdb::Exception e) {
        env->ReleaseStringUTFChars(j_sql, sql);
        __android_log_print(ANDROID_LOG_ERROR, "sqliteplus", "error %s", e.GetErrorMsg());
        return NULL;
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_leewug_src_sqliteplus_ConnectionPool_nativeEndTransaction(JNIEnv *env, jobject thiz,
                                                                   jlong db_ptr,
                                                                   jlong statement_ptr,
                                                                   jboolean success) {
    try {
        sqdb::Db db = sqdb::Db((sqlite3 *) db_ptr);
        sqlite3_stmt *pStmt = (sqlite3_stmt *) statement_ptr;
        if (success) {
            db.CommitTransaction();
        } else {
            db.RollbackTransaction();
        }
        sqlite3_clear_bindings(pStmt);
        sqlite3_finalize(pStmt);
    } catch (sqdb::Exception e) {
        __android_log_print(ANDROID_LOG_ERROR, "sqliteplus", "error %s", e.GetErrorMsg());
        return -1;
    }
    return 0;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_leewug_src_sqliteplus_ConnectionPool_nativeNextStatement(JNIEnv *env, jobject thiz,
                                                                  jlong db_ptr,
                                                                  jlong statement_prt,
                                                                  jobjectArray args) {
    try {
        sqlite3_stmt *pStmt = (sqlite3_stmt *) statement_prt;
        sqlite3 *pSqlite3 = (sqlite3 *) db_ptr;
        sqdb::Statement stmt(pSqlite3, pStmt);
        int len = env->GetArrayLength(args);
        for (int i = 0; i < len; i++) {
            jstring str = (jstring) env->GetObjectArrayElement(args, i);
            if (str == NULL) {
                stmt.BindNull(i + 1);
            } else {
                const char *value = env->GetStringUTFChars(str, 0);
                stmt.Bind(i + 1, value);
                env->ReleaseStringUTFChars(str, value);
            }
        }
        stmt.Next(true);
    } catch (sqdb::Exception e) {
        __android_log_print(ANDROID_LOG_ERROR, "sqliteplus", "error %s===", e.GetErrorMsg());
        return -1;
    }
    return 0;
}



extern "C"
JNIEXPORT jlong JNICALL
Java_com_leewug_src_sqliteplus_ConnectionPool_nativeOpenDb(JNIEnv *env, jobject thiz,
                                                           jstring databasePath) {
    const char *dbPath = env->GetStringUTFChars(databasePath, 0);
    long addr;
    try {
        sqdb::Db db = sqdb::Db(dbPath);
        //开启wal模式
        db.exec("PRAGMA journal_mode = WAL;");
        addr = db.toAddr();
    } catch (sqdb::Exception e) {
        env->ReleaseStringUTFChars(databasePath, dbPath);
        __android_log_print(ANDROID_LOG_ERROR, "sqliteplus", "error %s", e.GetErrorMsg());
        return -1;
    }
    env->ReleaseStringUTFChars(databasePath, dbPath);
    return addr;
}


extern "C"
JNIEXPORT jint JNICALL
Java_com_leewug_src_sqliteplus_ConnectionPool_nativeInsert(JNIEnv *env, jobject thiz, jlong db_ptr,
                                                           jstring j_sql, jobjectArray values) {
    const char *sql = env->GetStringUTFChars(j_sql, 0);
    try {
        jclass integerClass = env->FindClass("java/lang/Integer");
        sqdb::Db db = sqdb::Db((sqlite3 *) db_ptr);
        try {
            sqdb::Statement stmt(db.Query(sql));
            int len = env->GetArrayLength(values);
            db.BeginTransaction();
            for (int i = 0; i < len; ++i) {
                jobjectArray columns = (jobjectArray) env->GetObjectArrayElement(values, i);
                int columnSize = env->GetArrayLength(columns);
                for (int j = 0; j < columnSize; ++j) {
                    jobject obj = env->GetObjectArrayElement(columns, j);
                    if (obj == NULL) {
                        stmt.BindNull(j + 1);
                    } else if (env->IsInstanceOf(obj, integerClass)) {
                        int value = (jint) obj;
                        stmt.Bind(j + 1, value);
                    } else {
                        const char *value = env->GetStringUTFChars((jstring) obj, 0);
                        stmt.Bind(j + 1, value);
                        env->ReleaseStringUTFChars((jstring) obj, value);
                    }
                    env->DeleteLocalRef(obj);
                    stmt.Next(true);
                }
                env->DeleteLocalRef(columns);
            }
            db.CommitTransaction();
            __android_log_print(ANDROID_LOG_ERROR, "sqliteplus", "success");
            stmt.release();
        } catch (sqdb::Exception e) {
            env->ReleaseStringUTFChars(j_sql, sql);
            __android_log_print(ANDROID_LOG_ERROR, "sqliteplus", "error %s", e.GetErrorMsg());
            db.RollbackTransaction();
        }
    } catch (sqdb::Exception e) {
        env->ReleaseStringUTFChars(j_sql, sql);
        __android_log_print(ANDROID_LOG_ERROR, "sqliteplus", "error %s", e.GetErrorMsg());
        return -1;
    }
    env->ReleaseStringUTFChars(j_sql, sql);
    return 0;
}