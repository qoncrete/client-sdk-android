package com.qoncrete.sdk;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href='mailto:zhaotengfei9@gmail.com'>Tengfei Zhao</a>
 */
class DB {
    private static DB ourInstance = null;
    private com.snappydb.DB snappyDB;
    private static final String DB_NAME = "qoncrete";

    private static final String LOG_TAG = "log:";

    static DB getInstance(Context context) {
        if (ourInstance == null) {
            ourInstance = new DB(context, null);
        }
        return ourInstance;
    }

    DB(Context context, String dbName) {
        try {
            snappyDB = DBFactory.open(context, TextUtils.isEmpty(dbName) ? DB_NAME : dbName); //create or open an existing database using the default name
        } catch (SnappydbException e) {
            e.printStackTrace();
        }
    }

    void put(String key, String value) {
        try {
            _put(key, value);
        } catch (SnappydbException e) {
            e.printStackTrace();
        }
    }

    void putLog(String value) {
        try {
            _put(LOG_TAG + System.currentTimeMillis(), value);
        } catch (SnappydbException e) {
            e.printStackTrace();
        }
    }

    synchronized int logCount() {
        try {
            Log.e("TAG", "logCount: " + _countKeys(LOG_TAG));
            return _countKeys(LOG_TAG);
        } catch (SnappydbException e) {
            e.printStackTrace();
        }
        return 0;
    }

    synchronized List<String> getAllLogs() {
        List<String> logs = new ArrayList<>();
        try {
            String[] keys = _findKeys(LOG_TAG);
            for (String key : keys) {
                logs.add(_get(key));
                _del(key);
            }
            return logs;
        } catch (SnappydbException e) {
            e.printStackTrace();
        }
        return logs;
    }

    String get(String key) {
        try {
            return snappyDB.get(key);
        } catch (SnappydbException e) {
            e.printStackTrace();
        }
        return null;
    }

    void close() {
        try {
            snappyDB.close();
        } catch (SnappydbException e) {
            e.printStackTrace();
        }
        ourInstance = null;
    }

    void destroy() {
        try {
            snappyDB.destroy();
        } catch (SnappydbException e) {
            e.printStackTrace();
        }
    }

    private void _put(String key, String value) throws SnappydbException {
        snappyDB.put(key, value);
    }

    private String _get(String key) throws SnappydbException {
        return snappyDB.get(key);
    }

    private void _del(String key) throws SnappydbException {
        snappyDB.del(key);
    }

    private int _countKeys(String key) throws SnappydbException {
        return snappyDB.countKeys(key);
    }

    private String[] _findKeys(String key) throws SnappydbException {
        return snappyDB.findKeys(key);
    }

    private String[] _findKeys(String key, int number) throws SnappydbException {
        return snappyDB.findKeys(key, number);
    }

    private void _close() throws SnappydbException {
        snappyDB.close();
    }

    private void _destroy() throws SnappydbException {
        snappyDB.destroy();
    }

}
