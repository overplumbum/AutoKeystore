#include <errno.h>
#include <unistd.h>
#include <stdio.h>

#include <stdint.h>
#include <sys/types.h>
#include <sys/socket.h>
#include "cutils/sockets.h"

int keystore_send(ssize_t len, const char *data)
{
    uint8_t code;
    int sock, i;

    sock = socket_local_client("keystore", ANDROID_SOCKET_NAMESPACE_RESERVED,
                               SOCK_STREAM);
    if (sock == -1) {
        //puts("Failed to connect");
        exit(51);
    }

    send(sock, data, len, 0);
    shutdown(sock, SHUT_WR);

    if (recv(sock, &code, 1, 0) != 1) {
        //puts("Failed to receive");
        exit(52);
    }
    return code;
}

#define CMD_BUFFER_LEN 100

int main(int argc, char* argv[])
{
    if (getuid() != 1000) {
        if(setuid(1000)) {
            return 54;
        }
    }

    char buf[CMD_BUFFER_LEN];
    ssize_t len = read(STDIN_FILENO, buf, CMD_BUFFER_LEN);
    if (len >= CMD_BUFFER_LEN) {
        return 53;
    }
    int code = keystore_send(len, buf);
    printf("%d\n", code);
    return 0;
}
