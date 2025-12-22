import matplotlib.pyplot as plt
import numpy as np
import math
from matplotlib.patches import Patch

# Dados
concorrencia = ['30', '50', '70']


#heavy
controle = [895, 608, 355]
cold_runs = {
    'Ehcache': [899, 668, 405],
    'Ignite': [838, 669, 330],
    'Guava': [906, 748, 417],
    'Caffeine': [916, 550, 362]
}

operational_runs = {
    'Ehcache': [915, 633, 438],
    'Ignite': [760, 594, 451],
    'Guava': [911, 685, 443],
    'Caffeine': [906, 596, 369]
}
"""
controle = [1000, 1000, 1000]
cold_runs = {
    'Ehcache': [1000, 1000, 1000],
    'Ignite': [987, 987, 987],
    'Guava': [1000, 1000, 1000],
    'Caffeine': [1000, 1000, 1000]
}

operational_runs = {
    'Ehcache': [1000, 1000, 1000],
    'Ignite': [917, 917, 917],
    'Guava': [1000, 1000, 1000],
    'Caffeine': [1000, 1000, 1000]
}
"""

caches = ['Ehcache', 'Ignite', 'Guava', 'Caffeine']

# Cores
colors = {
    'Ehcache': ['#6baed6','#1f77b4'],
    'Ignite': ['#ffbb78','#ff7f0e'],
    'Guava': ['#74c476','#2ca02c'],
    'Caffeine': ['#fa9fb5', '#de436f']
}

fig, ax = plt.subplots(figsize=(16, 10))

# Largura das barras e espaçamentos menores
largura = 1
espaco_cold_oper = 0.08   # menor que antes
espaco_entre_caches = 0.25
espaco_grupo = 0.75

x_labels_pos = []

# Máximo eixo Y
valores_max = [controle] + list(cold_runs.values()) + list(operational_runs.values())
max_val_real = max(max(lst) for lst in valores_max)
y_max = ((max_val_real // 250) + 1) * 250
#y_max = math.ceil(max_val_real / 250) * 250
y_ticks = np.arange(0, y_max + 1, 250)

# Construção das barras verticais
for i, conc in enumerate(concorrencia):

    x_positions = []
    x_current = 0

    # API Base
    x_positions.append(x_current)
    x_current += largura + espaco_entre_caches

    # Caches
    for cache in caches:

        # Cold
        x_positions.append(x_current)
        x_current += largura + espaco_cold_oper

        # Operational
        x_positions.append(x_current)
        x_current += largura + espaco_entre_caches

    # deslocamento para próximo grupo
    x_positions = [xp + i * (x_current + espaco_grupo) for xp in x_positions]

    # API-BASE
    ax.bar(x_positions[0], controle[i], width=largura, color='gray')
    ax.text(x_positions[0], controle[i] / 2, f'{controle[i]}', va='center',
            ha='center', fontsize=10, fontweight='bold', color='white')

    # Caches
    for j, cache in enumerate(caches):
        idx_cold = 1 + j * 2
        idx_oper = idx_cold + 1

        cold_val = cold_runs[cache][i]
        op_val = operational_runs[cache][i]

        # Cold
        ax.bar(x_positions[idx_cold], cold_val, width=largura, color=colors[cache][0])
        ax.text(x_positions[idx_cold], cold_val / 2, f'{cold_val}',
                va='center', ha='center', fontsize=10, fontweight='bold', color='white')

        # Operational
        ax.bar(x_positions[idx_oper], op_val, width=largura, color=colors[cache][1])
        ax.text(x_positions[idx_oper], op_val / 2, f'{op_val}',
                va='center', ha='center', fontsize=10, fontweight='bold', color='white')

        # Percentuais
        diff_cold = (cold_val - controle[i]) / controle[i] * 100
        diff_op = (op_val - controle[i]) / controle[i] * 100

        ax.text(x_positions[idx_cold], cold_val + 10, f'{diff_cold:+.1f}%',
                va='bottom', ha='center', fontsize=10, color='blue')

        ax.text(x_positions[idx_oper], op_val + 10, f'{diff_op:+.1f}%',
                va='bottom', ha='center', fontsize=9, color='green')

    # Label de concorrência centralizado no grupo
    x_center = (x_positions[0] + x_positions[-1]) / 2
    x_labels_pos.append(x_center)

# Eixos
ax.set_xticks(x_labels_pos)
ax.set_xticklabels(concorrencia)
ax.set_xlabel("Concurrency")
ax.set_ylabel("Number of Successes - Higher is better.")
#ax.set_title("Cache comparison vs API-Base by concurrency (Vertical Bars)")

# Grid horizontal
ax.grid(axis='y', linestyle='--', alpha=0.7)

# Escala Y
ax.set_ylim(0, y_max)
ax.set_yticks(y_ticks)

# Legenda
all_handles = [
    Patch(color='gray', label='API Base'),
    Patch(color='white', label=''),
    Patch(color=colors['Ehcache'][0], label='Ehcache Cold'),
    Patch(color=colors['Ehcache'][1], label='Ehcache Operational'),
    Patch(color=colors['Ignite'][0], label='Ignite Cold'),
    Patch(color=colors['Ignite'][1], label='Ignite Operational'),
    Patch(color=colors['Guava'][0], label='Guava Cold'),
    Patch(color=colors['Guava'][1], label='Guava Operational'),
    Patch(color=colors['Caffeine'][0], label='Caffeine Cold'),
    Patch(color=colors['Caffeine'][1], label='Caffeine Operational'),
]

ax.legend(handles=all_handles, loc='upper center',
          bbox_to_anchor=(0.5, -0.05), ncol=5, fontsize=14,
          title="Legend")

plt.subplots_adjust(bottom=0.25)
plt.tight_layout()
plt.savefig("grafico_caches_Light.png", dpi=300, bbox_inches='tight')
plt.show()
