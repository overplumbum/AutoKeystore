#include <sqlite3.h>
#include <stdio.h>

int main(int argc, char* argv[])
{
    if (getuid() != 1000) {
        if (getuid() != 0) {
            fprintf(stderr, "I wanna root!!!\n");
            return 45;
        }

        if(setuid(1000)) {
            return 46;
        }
    }

    sqlite3 *db;
    char *zErrMsg = 0;
    int rc;
    int result = 0;

    rc = sqlite3_open("/data/system/locksettings.db", &db);

    if (rc) {
        fprintf(stderr, "Can't open database: %s\n", sqlite3_errmsg(db));
        sqlite3_close(db);
        result = 41;
        goto exit;
    }

    rc = sqlite3_exec(db, "update locksettings set value=0 where name='lockscreen.password_type'", 0, 0, &zErrMsg);
    if (rc != SQLITE_OK) {
        fprintf(stderr, "SQL error: %s\n", zErrMsg);
        sqlite3_free(zErrMsg);
        result = 42;
        goto exit;
    }

    int affected = sqlite3_changes(db);
    if (affected == 0) {
        printf("Nothing changed (setting does not exist)\n");
        result = 43;
        goto exit;
    } else if (affected == 1) {
        printf("Setting applied as it should\n");
    } else {
        fprintf(stderr, ">1 settings changed: %d. WTF???\n", affected);
        result = 44;
        goto exit;
    }

  exit:
    sqlite3_close(db);
    return result;
}
