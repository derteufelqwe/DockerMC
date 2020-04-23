import http.server
import socketserver
import time
from multiprocessing import Process


def run():
    PORT = 8000
    Handler = http.server.SimpleHTTPRequestHandler
    server = socketserver.TCPServer(("localhost", PORT), Handler)
    print("serving at port", PORT)
    server.serve_forever()


if __name__ == '__main__':

    p = Process(target=run)
    p.start()

    time.sleep(5)

    p.terminate()
    print('Stopped')
    time.sleep(30)