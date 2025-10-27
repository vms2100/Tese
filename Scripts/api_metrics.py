import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

# === 1. Ler o CSV (tenta com vírgula e ponto-e-vírgula) ===
csv_file = "C:/Users/vms2100/Desktop/Ubi/Tese/API-Tese/API-Base/api_metrics.csv"
try:
    df = pd.read_csv(csv_file, sep=",")
except:
    df = pd.read_csv(csv_file, sep=";")

# === 2. Normalizar nomes das colunas ===
df.columns = df.columns.str.strip().str.lower()

# === 3. Filtrar apenas status 200 ===
df = df[df["status"].isin([200])]

# === 3b. Filtro opcional: resposta maior que 0 ===
df = df[df["bytes_sent"] > 0]

# === 4. Calcular médias por endpoint ===
means = df.groupby("endpoint")[
    ["db_time_nanos", "parser_time_nanos", "total_time_nanos", "cpu_nanos", "memoria_b", "bytes_sent"]
].mean()


# Converter unidades para microssegundos
means["db_time_s"] = means["db_time_nanos"] / 1_000_000_000
means["parser_time_ms"] = means["parser_time_nanos"] / 1_000_000
means["cpu_ms"] = means["cpu_nanos"] / 1_000_000
means["memoria_mb"] = means["memoria_b"] / (1024*1024)
means["resposta_mb"] = means["bytes_sent"] / (1024*1024) 

# Lista de métricas a manter
metrics = ["db_time_s", "parser_time_ms", "cpu_ms", "memoria_mb", "resposta_mb"]
means = means[(means[metrics] > 0).any(axis=1)]


# === Função para escrever valores nas barras (ignora 0.0) ===
def add_labels(ax, rects, values, offset=0.01):
    for rect, value in zip(rects, values):
        if value <= 0:
            continue
        height = rect.get_height()
        ax.annotate(f"{value:.2f}",
                    xy=(rect.get_x() + rect.get_width() / 2, height + offset),
                    xytext=(0, 3),
                    textcoords="offset points",
                    ha="center", va="bottom", fontsize=8)

## =======================
# === Gráfico 1: DB Time ===
# =======================
fig, ax = plt.subplots(figsize=(10,6))
rects_db = ax.bar(means.index, means["db_time_s"], label="DB Time")

add_labels(ax, rects_db, means["db_time_s"])

ax.set_ylabel("Tempo Médio (s)")
ax.set_title("DB Time por Endpoint (status 200, resposta > 0)")
plt.xticks(rotation=45, ha="right")
plt.tight_layout()
plt.savefig("grafico_db.png")

# =======================
# === Gráfico 2: Parser Time ===
# =======================
if (means["parser_time_ms"] > 0).any():
    fig, ax = plt.subplots(figsize=(10,6))
    rects_parser = ax.bar(means.index, means["parser_time_ms"], label="Parser Time")

    add_labels(ax, rects_parser, means["parser_time_ms"])

    ax.set_ylabel("Tempo Médio (s)")
    ax.set_title("Parser Time por Endpoint (status 200, resposta > 0)")
    plt.xticks(rotation=45, ha="right")
    plt.tight_layout()
    plt.savefig("grafico_parser.png")


# === Gráfico 2: CPU ===
fig, ax = plt.subplots(figsize=(10,6))
rects = ax.bar(means.index, means["cpu_ms"])
ax.set_ylabel("CPU (ms)")
ax.set_title("Uso médio de CPU por Endpoint (status 200, resposta > 0)")
plt.xticks(rotation=45, ha="right")

add_labels(ax, rects, means["cpu_ms"])
plt.tight_layout()
plt.savefig("grafico_cpu.png")

# === Gráfico 3: Memória ===
fig, ax = plt.subplots(figsize=(10,6))
rects = ax.bar(means.index, means["memoria_mb"])
ax.set_ylabel("Memória (MB)")
ax.set_title("Uso médio de Memória por Endpoint (status 200, resposta > 0)")
plt.xticks(rotation=45, ha="right")

add_labels(ax, rects, means["memoria_mb"])
plt.tight_layout()
plt.savefig("grafico_memoria.png")

# === Gráfico 4: Resposta (escala log) ===
fig, ax = plt.subplots(figsize=(10,6))
rects = ax.bar(means.index, means["resposta_mb"])
ax.set_ylabel("Resposta (MB)")
ax.set_title("Tamanho médio da Resposta por Endpoint (status 200, resposta > 0, escala log)")
plt.xticks(rotation=45, ha="right")

# Aplica escala logarítmica no eixo y
ax.set_yscale('log')

add_labels(ax, rects, means["resposta_mb"])
plt.tight_layout()
plt.savefig("grafico_resposta_log.png")


plt.show()
