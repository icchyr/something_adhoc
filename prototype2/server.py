import asyncore, socket

class Server(asyncore.dispatcher):
    def __init__(self, host, port):
        asyncore.dispatcher.__init__(self)
        self.create_socket(socket.AF_INET, socket.SOCK_STREAM)
        self.set_reuse_addr()
        self.bind((host, port))
        self.listen(1)
        self.host = host
        self.port = port
        self.msg = ""

    def handle_accept(self):
        sock, addr = self.accept()
        if sock and addr:
            print 'connection from %s' % str(addr)
            handler = ServerHandler(sock)

    def stop(self):
        self.close()

class ServerHandler(asyncore.dispatcher_with_send):
    def handle_read(self):
        data = ""
        while True:
            try:
                tmp = self.recv(1024)
            except:
                break
            if not tmp:
                break
            data += tmp

        print "received %d bytes data: "%len(data) + repr(data)


import sys
if len(sys.argv) > 1 and sys.argv[1] == "debug":
    server = Server('0.0.0.0', 10080)
    # asyncore.loop()
