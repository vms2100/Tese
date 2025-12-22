import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import os

def format_time(x):
    m = int(x // 60)
    s = int(x % 60)
    return f"{m:02d}:{s:02d}"

def save(plt, path):
    base_dir = os.path.dirname(__file__)               
    results_dir = os.path.join(base_dir, "Results")   
    os.makedirs(results_dir, exist_ok=True)            
    
    output_path = os.path.join(results_dir, path)      
    plt.savefig(output_path)                           
    plt.close()  # fecha a figura para não acumular em memória

def plot_requests(results, start_time, exec_time, path):
    results = sorted(results, key=lambda x: x[0])  # x[0] = fim da requisição
    results_relative = [(t - start_time, status) for t, status, _ in results]

    # --- Parte para plotagem de contagem de requests no tempo ---
    times_sec = []
    total_requests = []
    responses_200_204 = []
    responses_500 = []

    count_total = count_200_204 = count_500 = 0
    idx = 0
    n = len(results_relative)

    for cp in range(-60, int(exec_time) + 61):
        while idx < n and results_relative[idx][0] <= cp:
            count_total += 1
            status = results_relative[idx][1]
            if status in (200, 204):
                count_200_204 += 1
            elif status == 500:
                count_500 += 1
            idx += 1
        times_sec.append(cp)
        total_requests.append(count_total)
        responses_200_204.append(count_200_204)
        responses_500.append(count_500)

    fig, ax = plt.subplots(figsize=(12, 6))

    ax.plot(times_sec, total_requests, label="Total Requests")
    ax.plot(times_sec, responses_200_204, label="Respostas 200/204")
    ax.plot(times_sec, responses_500, label="Respostas 500")

    ax.set_xlabel("Tempo (mm:ss)")
    ax.set_ylabel("Quantidade")
    
    # Título com valores finais
    title = (
        f"Teste: Total Requests={total_requests[-1]}, "
        f"Response Status:200/204={responses_200_204[-1]}, 500={responses_500[-1]}"
    )
    ax.set_title(title)

    # Limites e linhas de início/fim
    ax.set_xlim(-60, exec_time + 60)
    ax.axvline(0, color="green", linestyle="--")
    ax.axvline(exec_time, color="red", linestyle="--")

    ylim = ax.get_ylim()
    y_min = ylim[0]

    # Anotações início e fim
    ax.annotate("Início\n0s", xy=(0, y_min), xycoords='data',
                xytext=(0, -30), textcoords='offset points',
                ha='center', va='top', fontsize=10, color='green',
                arrowprops=dict(arrowstyle="->", color="green"))
    
    ax.annotate(f"Fim\n{format_time(exec_time)}", xy=(exec_time, y_min), xycoords='data',
                xytext=(0, -30), textcoords='offset points',
                ha='center', va='top', fontsize=10, color='red',
                arrowprops=dict(arrowstyle="->", color="red"))

    ax.legend()
    ax.grid(True)

    ax.xaxis.set_major_formatter(ticker.FuncFormatter(lambda x, pos: format_time(x)))
    save(plt, path)
    
def plot_response_time_means(results, path, final=True):
    tempos_totais = [r[2] for r in results]
    tempos_200_204 = [r[2] for r in results if r[1] in (200, 204)]
    tempos_500 = [r[2] for r in results if r[1] == 500]
    
    media_total = sum(tempos_totais) / len(tempos_totais) if tempos_totais else 0
    media_200_204 = sum(tempos_200_204) / len(tempos_200_204) if tempos_200_204 else 0
    media_500 = sum(tempos_500) / len(tempos_500) if tempos_500 else 0

    if final:
        categorias = ['Total', '200/204', '500']
        medias = [media_total, media_200_204, media_500]
        colors = ['blue', 'green', 'red']
    else:
        categorias = ['Total', '200/204']
        medias = [media_total, media_200_204]
        colors = ['blue', 'green']

    plt.figure(figsize=(8, 5))
    bars = plt.bar(categorias, medias, color=colors)
    plt.title("Tempos médios de resposta")
    plt.ylabel("Tempo médio (s)")
    plt.xlabel("Categoria de resposta")
    plt.grid(axis='y')

    # Adiciona o valor em cima de cada barra
    for bar in bars:
        altura = bar.get_height()
        plt.text(
            bar.get_x() + bar.get_width() / 2,
            altura,
            f'{altura:.2f}',
            ha='center',
            va='bottom'
        )

    save(plt, path)
