import socket
import sys
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.request import urlopen


ROOT = Path(__file__).resolve().parent


class AppHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(ROOT), **kwargs)

    def do_GET(self):
        if self.path in {"/", ""}:
            self.path = "/da.html"
        elif self.path == "/favicon.ico":
            self.path = "/favicon.svg"

        return super().do_GET()


def _is_our_server_running(host, port):
    try:
        with urlopen(f"http://{host}:{port}/da.html", timeout=2) as response:
            body = response.read(4096).decode("utf-8", errors="ignore")
            return "<title>Философия Бизнеса | Юридические услуги</title>" in body
    except OSError:
        return False


def _port_is_free(host, port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            sock.bind((host, port))
        except OSError:
            return False

    return True


def _find_free_port(host, preferred_port, attempts=20):
    for port in range(preferred_port, preferred_port + attempts):
        if _port_is_free(host, port):
            return port

    raise OSError(f"No free port found starting from {preferred_port}")


def _get_local_ip():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        sock.connect(("8.8.8.8", 80))
        return sock.getsockname()[0]
    except OSError:
        return "127.0.0.1"
    finally:
        sock.close()


def run():
    host = "0.0.0.0"
    preferred_port = 8000
    local_ip = _get_local_ip()

    if _is_our_server_running("127.0.0.1", preferred_port):
        print(f"Server already running at http://127.0.0.1:{preferred_port}/da.html", flush=True)
        print(f"Open from other devices: http://{local_ip}:{preferred_port}/da.html", flush=True)
        return

    port = _find_free_port(host, preferred_port)
    server = ThreadingHTTPServer((host, port), AppHandler)
    print(f"Local:   http://127.0.0.1:{port}/da.html", flush=True)
    print(f"Network: http://{local_ip}:{port}/da.html", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    try:
        run()
    except KeyboardInterrupt:
        sys.exit(0)
