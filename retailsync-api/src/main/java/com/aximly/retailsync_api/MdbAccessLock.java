package com.aximly.retailsync_api;

// Single shared lock — ensures only one connection touches recent.mdb at a time,
// whether it's the scheduled read sync or the write-back apply endpoint.
public class MdbAccessLock {
    public static final Object LOCK = new Object();
}