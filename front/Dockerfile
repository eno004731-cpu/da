FROM python:3.10-slim

WORKDIR /app

# Отдельных Python-зависимостей сейчас нет: frontend раздаётся
# встроенным HTTP server-ом из server.py.
COPY . .

EXPOSE 8000

CMD ["python3", "server.py"]
