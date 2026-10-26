#!/usr/bin/env python3
"""Local-only SMTP mailbox for development. Saves private .eml files; never sends mail."""
import argparse
from pathlib import Path
import socketserver
import uuid
import base64

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=Path.home() / 'PING-dev-mailbox')
    parser.add_argument('--port', type=int, default=1025)
    args = parser.parse_args()
    root = args.output.expanduser().resolve()
    repo = Path(__file__).resolve().parents[1]
    if root == repo or repo in root.parents:
        parser.error('The mailbox must be outside the repository: messages contain private links.')
    root.mkdir(parents=True, exist_ok=True, mode=0o700)

    class SMTP(socketserver.StreamRequestHandler):
        def handle(self):
            self.request.settimeout(30)
            self.wfile.write(b'220 localhost PING development mailbox\r\n')
            body, size, collecting = [], 0, False
            while line := self.rfile.readline(65537):
                if len(line) > 65536:
                    self.wfile.write(b'552 line too long\r\n'); return
                if collecting:
                    if line == b'.\r\n':
                        path = root / (uuid.uuid4().hex + '.eml')
                        with path.open('xb') as file:
                            file.write(b''.join(body))
                        path.chmod(0o600)
                        body, size, collecting = [], 0, False
                        self.wfile.write(b'250 saved locally\r\n')
                    else:
                        size += len(line)
                        if size > 1024 * 1024:
                            self.wfile.write(b'552 message too large\r\n'); return
                        body.append(line[1:] if line.startswith(b'..') else line)
                    continue
                cmd = line.split(b' ', 1)[0].strip().upper()
                if cmd in (b'EHLO', b'HELO'):
                    # Loopback metadata lets the existing Windows launcher find the actual
                    # private mailbox directory instead of guessing from an open TCP port.
                    encoded = base64.urlsafe_b64encode(str(root).encode('utf-8'))
                    self.wfile.write(b'250-localhost PING development mailbox\r\n250 X-PING-MAILBOX ' + encoded + b'\r\n')
                elif cmd in (b'MAIL', b'RCPT', b'RSET', b'NOOP'):
                    self.wfile.write(b'250 ok\r\n')
                elif cmd == b'DATA':
                    collecting = True; self.wfile.write(b'354 end with dot\r\n')
                elif cmd == b'QUIT':
                    self.wfile.write(b'221 bye\r\n'); return
                else:
                    self.wfile.write(b'502 unsupported\r\n')

    class Server(socketserver.ThreadingTCPServer):
        allow_reuse_address = True
        daemon_threads = True
    print(f'Local development mailbox: 127.0.0.1:{args.port}; files: {root}. No outbound mail.')
    try:
        with Server(('127.0.0.1', args.port), SMTP) as server:
            server.serve_forever()
    except KeyboardInterrupt:
        pass

if __name__ == '__main__':
    main()
