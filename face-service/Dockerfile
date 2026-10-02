FROM python:3.11-slim

# dlib (dependência do face-recognition) precisa compilar C++ na instalação —
# por isso o serviço só roda containerizado (ver NOTES.md): no Windows isso
# exigiria CMake + Visual Studio Build Tools instalados na máquina de dev.
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    cmake \
    libopenblas-dev \
    liblapack-dev \
    libx11-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
