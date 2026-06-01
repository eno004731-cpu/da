import json
import os
import socket
import sys
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.request import urlopen


ROOT = Path(__file__).resolve().parent


class AppHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(ROOT), **kwargs)

    def end_headers(self):
        # Локальный сервер должен всегда отдавать свежую статику,
        # чтобы reload сразу подхватывал новые JS/HTML без ручной чистки кеша.
        self.send_header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
        self.send_header("Pragma", "no-cache")
        self.send_header("Expires", "0")
        super().end_headers()

    def do_GET(self):
        request_path = self.path.split("?", 1)[0]

        if request_path in {"/", ""}:
            self.path = "/da.html"
        elif request_path == "/favicon.ico":
            self.path = "/favicon.svg"
        elif request_path == "/runtime-config.js":
            self._serve_runtime_config()
            return

        return super().do_GET()

    def _serve_runtime_config(self):
        runtime_config = {
            "__LEGAL_API_BASE_URL__": os.getenv("API_BASE_URL", "").strip(),
            "__LEGAL_AUTH_API_BASE_URL__": os.getenv("AUTH_API_BASE_URL", "").strip(),
            "__LEGAL_GOOGLE_CLIENT_ID__": os.getenv("GOOGLE_CLIENT_ID", "").strip(),
        }

        lines = [
            "(function configureRuntimeConfig() {",
            "  // Этот файл генерируется на лету из env, чтобы test/prod",
            "  // могли использовать разные backend URL без правки JS-кода.",
        ]

        for key, value in runtime_config.items():
            lines.append(f"  window.{key} ||= {json.dumps(value, ensure_ascii=False)};")

        lines.append("})();")
        body = "\n".join(lines).encode("utf-8")

        self.send_response(200)
        self.send_header("Content-Type", "application/javascript; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def _load_env_file():
    env_file = ROOT / ".env"
    if not env_file.exists():
        return

    for raw_line in env_file.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue

        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")

        if key and key not in os.environ:
            os.environ[key] = value


def _is_our_server_running(host, port):
    try:
        with urlopen(f"http://{host}:{port}/da.html", timeout=2) as response:
            body = response.read(4096).decode("utf-8", errors="ignore")
            return (
                "<title>Философия Бизнеса | Юридическая поддержка бизнеса</title>" in body
                or "<title>Философия Бизнеса | Юридические услуги</title>" in body
            )
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


def _find_free_port(host, preferred_port, attempts=200):
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


def _get_preferred_port():
    if len(sys.argv) > 1:
        return int(sys.argv[1])

    port_from_env = os.getenv("LEGAL_FRONTEND_PORT")
    if port_from_env:
        return int(port_from_env)

    return 8000


def run():
    _load_env_file()

    host = "0.0.0.0"
    preferred_port = _get_preferred_port()
    local_ip = _get_local_ip()

    if _is_our_server_running("127.0.0.1", preferred_port):
        print(f"Server already running at http://127.0.0.1:{preferred_port}/da.html", flush=True)
        print(f"Open from other devices: http://{local_ip}:{preferred_port}/da.html", flush=True)
        print("Expected local APIs:", flush=True)
        print("  site API: disabled locally (orders API planned later)", flush=True)
        print(f"  auth API: {os.getenv('AUTH_API_BASE_URL', 'http://127.0.0.1:8081/api')}", flush=True)
        return

    port = _find_free_port(host, preferred_port)
    server = ThreadingHTTPServer((host, port), AppHandler)
    print(f"Local:   http://127.0.0.1:{port}/da.html", flush=True)
    print(f"Network: http://{local_ip}:{port}/da.html", flush=True)
    print("Expected local APIs:", flush=True)
    print(f"  site API: {os.getenv('API_BASE_URL', 'disabled locally (orders API planned later)')}", flush=True)
    print(f"  auth API: {os.getenv('AUTH_API_BASE_URL', 'http://127.0.0.1:8081/api')}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    try:
        run()
    except KeyboardInterrupt:
        sys.exit(0)
