import psutil
import time
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import numpy as np
import threading
import os

last_bytes_process = {}

def save(plt, name):
    base_dir = os.path.dirname(__file__)               
    results_dir = os.path.join(base_dir, "Results")   
    os.makedirs(results_dir, exist_ok=True)            
    
    output_path = os.path.join(results_dir, name)      
    plt.savefig(output_path)         

def format_time(x, exec_time):
    if x < 0:
        t = abs(x)
        signal = "-"
    elif x <= exec_time:
        t = x
        signal = ""
    else:
        t = x - exec_time
        signal = "+"
    horas = int(t // 3600)
    minutos = int((t % 3600) // 60)
    segundos = int(t % 60)
    if horas > 0:
        return f"{signal}{horas}:{minutos:02d}:{segundos:02d}"
    else:
        return f"{signal}{minutos}:{segundos:02d}"

def get_data_process(pid, object_type, interval):
    try:
        proc = psutil.Process(pid)
    except psutil.NoSuchProcess:
        raise ValueError(f"Processo com PID {pid} não encontrado.")

    cpu = proc.cpu_percent(interval)
    if object_type == 'Docker':
        cpu = (cpu / (4 * 100)) * 100
    else:
        cpu = (cpu / (4 * 100)) * 100
    mem = proc.memory_info().rss / (1024 ** 3)
    io = proc.io_counters()
    read_now = io.read_bytes
    write_now = io.write_bytes

    if pid not in last_bytes_process:
        last_bytes_process[pid] = {"read": None, "write": None}

    if last_bytes_process[pid]["read"] is None:
        read_mbps = 0
        write_mbps = 0
    else:
        read_mbps = (read_now - last_bytes_process[pid]["read"]) / (1024 ** 2) / interval
        write_mbps = (write_now - last_bytes_process[pid]["write"]) / (1024 ** 2) / interval

    last_bytes_process[pid]["read"] = read_now
    last_bytes_process[pid]["write"] = write_now

    return cpu, mem, read_mbps, write_mbps

def make_plot(times, cpu_data, ram_data, read_data, write_data, exec_time, extra_sec, save_path):
    fig, axs = plt.subplots(3, 1, figsize=(16, 12), sharex=True, gridspec_kw={"hspace": 0.5})
    times_arr = np.array(times)
    cpu_arr = np.array(cpu_data)
    ram_arr = np.array(ram_data)
    read_arr = np.array(read_data)
    write_arr = np.array(write_data)

    mask_exec = (times_arr >= 0) & (times_arr <= exec_time)

    def stats(data):
        if np.any(mask_exec):
            return np.mean(data[mask_exec]), np.max(data[mask_exec])
        else:
            return 0, 0

    cpu_mean, cpu_max = stats(cpu_arr)
    ram_mean, ram_max = stats(ram_arr)
    read_mean, read_max = stats(read_arr)
    write_mean, write_max = stats(write_arr)

    # CPU
    axs[0].plot(times, cpu_data, label="CPU (%)")
    axs[0].set_ylabel("CPU (%)")
    axs[0].legend(loc="upper right")
    axs[0].set_title(f"CPU - Média: {cpu_mean:.1f}% | Máx: {cpu_max:.1f}%")

    # RAM
    axs[1].plot(times, ram_data, label="RAM (GB)")
    axs[1].set_ylabel("RAM (GB)")
    axs[1].legend(loc="upper right")
    axs[1].set_title(f"RAM - Média: {ram_mean:.2f} GB | Máx: {ram_max:.2f} GB")

    # Disco
    axs[2].plot(times, read_data, label="Disk Reading (MB/s)")
    axs[2].plot(times, write_data, label="Disk Writing (MB/s)")
    axs[2].set_ylabel("Disk (MB/s)")
    axs[2].legend(loc="upper right")
    axs[2].set_title(f"Disco - Leitura: média {read_mean:.1f} / máx {read_max:.1f} MB/s | "
                     f"Escrita: média {write_mean:.1f} / máx {write_max:.1f} MB/s")

    for ax in axs:
        ax.axvline(x=0, color="green", linestyle="--")
        ax.axvline(x=exec_time, color="red", linestyle="--")
        ylim = ax.get_ylim()
        y_min = ylim[0]

        ax.annotate("Início\n0s", xy=(0, y_min), xycoords='data',
                    xytext=(0, -30), textcoords='offset points',
                    ha='center', va='top', fontsize=9, color='green',
                    arrowprops=dict(arrowstyle="->", color="green"))

        ax.annotate(f"Fim\n{format_time(exec_time, exec_time)}",
                    xy=(exec_time, y_min), xycoords='data',
                    xytext=(0, -30), textcoords='offset points',
                    ha='center', va='top', fontsize=9, color='red',
                    arrowprops=dict(arrowstyle="->", color="red"))

        ax.tick_params(axis='x', which='both', labelbottom=True)
        ax.xaxis.set_major_formatter(
            ticker.FuncFormatter(lambda x, pos: format_time(x, exec_time))
        )
        ax.set_xlim(min(times), exec_time + extra_sec)

    fig.suptitle("Monitoramento de Recursos", fontsize=18)
    fig.tight_layout(rect=[0, 0.05, 1, 0.95])

    save(fig,save_path)

def monitoring(pid, interval_sec, pre_seconds, save_location, max_points, object_type, stop_event, extra_time):
    times, cpu_data, ram_data, read_data, write_data = [], [], [], [], []

    # Pré-monitoramento (antes do teste começar)
    for i in range(int(pre_seconds / interval_sec)):
        times.append(-pre_seconds + i * interval_sec)
        cpu, ram, read_mbps, write_mbps = get_data_process(pid, object_type, interval_sec)
        cpu_data.append(cpu)
        ram_data.append(ram)
        read_data.append(read_mbps)
        write_data.append(write_mbps)

    # Início do teste real
    start_time = time.time()
    while not stop_event.is_set():
        times.append(time.time() - start_time)
        cpu, ram, read_mbps, write_mbps = get_data_process(pid, object_type, interval_sec)
        cpu_data.append(cpu)
        ram_data.append(ram)
        read_data.append(read_mbps)
        write_data.append(write_mbps)

    exec_time = time.time() - start_time
    
    # Pós-monitoramento (extra_time segundos após o teste terminar)
    post_start = time.time()
    while time.time() - post_start < extra_time:
        times.append(time.time() - start_time)  # mantém o eixo X contínuo
        cpu, ram, read_mbps, write_mbps = get_data_process(pid, object_type, interval_sec)
        cpu_data.append(cpu)
        ram_data.append(ram)
        read_data.append(read_mbps)
        write_data.append(write_mbps)

    make_plot(times, cpu_data, ram_data, read_data, write_data, exec_time, 60, save_location)

