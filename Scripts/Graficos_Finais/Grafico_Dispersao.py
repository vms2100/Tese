import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
from matplotlib.lines import Line2D

'''
# Dados
dados = [
    ("base",30,"cold",61.89,35.58,895),
    ("base",50,"cold",69.39,28.34,608),
    ("base",70,"cold",75.92,19.23,355),

    ("ignite", 30, "cold", 56.16, 36.09, 838),
    ("ignite", 30, "running", 30.96, 23.11, 760),
    ("ignite", 50, "cold", 81.94, 32.05, 669),
    ("ignite", 50, "running", 36.36, 18.42, 594),
    ("ignite", 70, "cold", 78.27, 20.05, 330),
    ("ignite", 70, "running", 49.67, 16.01, 451),

    ("caffeine", 30, "cold", 59.80, 34.20, 916),
    ("caffeine", 30, "running", 60.38, 34.43, 906),
    ("caffeine", 50, "cold", 60.97, 25.57, 550),
    ("caffeine", 50, "running", 59.59, 24.59, 596),
    ("caffeine", 70, "cold", 72.53, 19.19, 362),
    ("caffeine", 70, "running", 75.10, 19.58, 369),

    ("ehcache", 30, "cold", 64.92, 37.01, 899),
    ("ehcache", 30, "running", 63.09, 36.02, 915),
    ("ehcache", 50, "cold", 75.37, 30.52, 668),
    ("ehcache", 50, "running", 72.84, 29.43, 663),
    ("ehcache", 70, "cold", 75.76, 19.57, 405),
    ("ehcache", 70, "running", 70.11, 18.57, 438),

    ("guava", 30, "cold", 59.69, 34.11, 906),
    ("guava", 30, "running", 59.82, 34.19, 911),
    ("guava", 50, "cold", 72.12, 30.34, 748),
    ("guava", 50, "running", 69.27, 29.01, 685),
    ("guava", 70, "cold", 74.01, 20.31, 417),
    ("guava", 70, "running", 78.70, 20.58, 443),
]
'''
dados = [
    ("base",30,"cold",61.89,121,1000),
    ("base",50,"cold",69.39,118,1000),
    ("base",70,"cold",75.92,117,1000),

    ("ignite", 30, "cold", 56.16, 120, 987),
    ("ignite", 30, "running", 30.96, 34, 917),
    ("ignite", 50, "cold", 81.94, 117, 987),
    ("ignite", 50, "running", 36.36, 34, 917),
    ("ignite", 70, "cold", 78.27, 118, 987),
    ("ignite", 70, "running", 49.67, 35, 917),

    ("caffeine", 30, "cold", 59.80, 118, 1000),
    ("caffeine", 30, "running", 60.38, 54, 1000),
    ("caffeine", 50, "cold", 60.97, 117, 1000),
    ("caffeine", 50, "running", 59.59, 53, 1000),
    ("caffeine", 70, "cold", 72.53, 117, 1000),
    ("caffeine", 70, "running", 75.10, 51, 1000),

    ("ehcache", 30, "cold", 64.92, 120, 1000),
    ("ehcache", 30, "running", 63.09, 44, 1000),
    ("ehcache", 50, "cold", 75.37, 119, 1000),
    ("ehcache", 50, "running", 72.84, 43, 1000),
    ("ehcache", 70, "cold", 75.76, 117, 1000),
    ("ehcache", 70, "running", 70.11, 44, 1000),

    ("guava", 30, "cold", 59.69, 115, 1000),
    ("guava", 30, "running", 59.82, 114, 1000),
    ("guava", 50, "cold", 72.12, 117, 1000),
    ("guava", 50, "running", 69.27, 115.01, 1000),
    ("guava", 70, "cold", 74.01, 113, 1000),
    ("guava", 70, "running", 78.70, 118, 1000),
]

df = pd.DataFrame(dados, columns=[
    "cache", "concorrencia", "modo", "tempo_medio_s", "tempo_total_min", "acertos"
])

# Marcadores por cache (forma)
shapes = {
    "base":"*",
    "ignite": "o",
    "caffeine": "s",
    "ehcache": "^",
    "guava": "D"
}

# Cores por concorrência
cores_conc = {
    30: "#1f77b4",
    50: "#2ca02c",
    70: "#d62728"
}

plt.figure(figsize=(12, 8))

# Loop com jitter
for _, row in df.iterrows():

    jitter_x = row["tempo_total_min"] + np.random.uniform(-0.15, 0.15)
    jitter_y = row["acertos"] + np.random.uniform(-2, 2)

    plt.scatter(
        jitter_x,
        jitter_y,
        marker=shapes[row["cache"]],
        s=130,
        color=cores_conc[row["concorrencia"]],
        edgecolor="black" if row["modo"] == "cold" else "none",
        linewidth=1.5,
        alpha=0.85
    )

#plt.title("Number of Successful Responses vs Total Time \nCategorized by cache type, concurrency, and operation mode.")
plt.xlabel("Total Time (sec) - Lower is better.")
plt.ylabel("Number of Hits - Higher is better.")
plt.grid(True)

# Legenda: caches
legend_cache = [
    Line2D([0], [0], marker='*', color='w', label='API-Base', markerfacecolor='white', markeredgecolor='black', markersize=14),
    Line2D([0], [0], marker='o', color='w', label='API-Ignite', markerfacecolor='white', markeredgecolor='black', markersize=14),
    Line2D([0], [0], marker='s', color='w', label='API-Caffeine', markerfacecolor='white', markeredgecolor='black', markersize=14),
    Line2D([0], [0], marker='^', color='w', label='API-Ehcache', markerfacecolor='white', markeredgecolor='black', markersize=14),
    Line2D([0], [0], marker='D', color='w', label='API-Guava', markerfacecolor='white', markeredgecolor='black', markersize=14),
]

# Legenda: concorrência
legend_conc = [
    Line2D([0], [0], marker='o', color='w', label='Concorrency: 30', markerfacecolor=cores_conc[30], markersize=14),
    Line2D([0], [0], marker='o', color='w', label='Concorrency: 50', markerfacecolor=cores_conc[50], markersize=14),
    Line2D([0], [0], marker='o', color='w', label='Concorrency: 70', markerfacecolor=cores_conc[70], markersize=14),
]

# Legenda: modo
legend_modo = [
    Line2D([0], [0], marker='o', color='w', label='Cold Running', markerfacecolor='orange',
           markeredgecolor='black', markersize=14),
    Line2D([0], [0], marker='o', color='w', label='Operational Running', markerfacecolor='orange',
           markeredgecolor='none', markersize=14),
]

plt.legend(
    handles=legend_cache + legend_conc + legend_modo,
    title='Legend',
    loc='upper center',          
    bbox_to_anchor=(0.5, -0.05),
    fontsize=14,   
    title_fontsize=14,
    ncol=5 
)

plt.tight_layout()
plt.savefig("Acertos_Tempo_Light.png", dpi=300, bbox_inches='tight')
plt.show()
