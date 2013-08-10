LOCAL_PATH := $(call my-dir)

### ---------------------------- ####

include $(CLEAR_VARS)

LOCAL_MODULE    := keystorecmd
LOCAL_SRC_FILES := keystorecmd.c libcutils/socket_local_client.c

include $(BUILD_EXECUTABLE)

### ---------------------------- ####

include $(CLEAR_VARS)

LOCAL_MODULE    := unlockcmd
LOCAL_SRC_FILES := unlockcmd.c sqlite3.c

# + http://code.google.com/p/sqlite4java/source/browse/trunk/ant/default.properties
# + http://code.google.com/p/sqlite4java/source/browse/trunk/ant/Android.mk.template
LOCAL_CFLAGS    := -DSQLITE_ENABLE_COLUMN_METADATA -DSQLITE_ENABLE_FTS3 -DSQLITE_ENABLE_FTS3_PARENTHESIS -DSQLITE_ENABLE_MEMORY_MANAGEMENT -DSQLITE_ENABLE_STAT2 -DHAVE_READLINE=0 -DSQLITE_THREADSAFE=1 -DSQLITE_THREAD_OVERRIDE_LOCK=-1 -DTEMP_STORE=1  -DSQLITE_OMIT_DEPRECATED -DSQLITE_OS_UNIX=1 -DSQLITE_ENABLE_RTREE=1 -DSQLITE_ENABLE_COLUMN_METADATA -DSQLITE_ENABLE_FTS3 -DSQLITE_ENABLE_FTS3_PARENTHESIS -DSQLITE_ENABLE_MEMORY_MANAGEMENT -DSQLITE_ENABLE_STAT2 -DHAVE_READLINE=0 -DSQLITE_THREADSAFE=1 -DSQLITE_THREAD_OVERRIDE_LOCK=-1 -DTEMP_STORE=1  -DSQLITE_OMIT_DEPRECATED -DSQLITE_OS_UNIX=1 -O2 -DNDEBUG -Dfdatasync=fsync -fno-omit-frame-pointer -fno-strict-aliasing -static-libgcc


include $(BUILD_EXECUTABLE)

### ---------------------------- ####
