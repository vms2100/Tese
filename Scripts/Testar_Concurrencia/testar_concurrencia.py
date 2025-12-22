import asyncio
import aiohttp
import time
from yarl import URL
from Endpoints import EndpointManager
import docker
import threading
from Monitoring import monitoring
from Client import plot_requests,plot_response_time_means

API_BASE_URL = "http://localhost:8080/api"
SEED = 2100
TOTAL_CALLS = 1000
CONCURRENCY_LEVELS = range(10, 101, 10)
manager = EndpointManager(seed=SEED)


async def call_api(session, index, semaphore):
    async with semaphore:
        start = time.perf_counter()
        raw_path = manager.generate_output_string(index)
        full_url = API_BASE_URL.rstrip('/') + raw_path
        try:
            async with session.get(full_url, timeout=None) as response:
                status = response.status
                end = time.perf_counter()
                response_time = end - start
                print(f"[{index}]: {raw_path} - {status} tempo:{response_time}")
                return (end, status, response_time)
        except Exception as e:
            end = time.perf_counter()
            response_time = end - start
            print(f"[{index}]: Error calling {full_url}: {e}")
            return (end, 500, response_time)


async def run_test(concurrency):
    print(f"\n=== Testando com concurrency = {concurrency} ===")
    semaphore = asyncio.Semaphore(concurrency)

    async with aiohttp.ClientSession() as session:
        start_time = time.perf_counter()
        results = await asyncio.gather(
            *(call_api(session, i, semaphore) for i in range(TOTAL_CALLS))
        )
        exec_time = time.perf_counter() - start_time

    # Numero de requests por tempo e respostas
    plot_requests(results,start_time,exec_time, f"Status_response_{concurrency}")

    # Tempo Media Resposta
    plot_response_time_means( results,f"Media_response_time_{concurrency}",True)
    return concurrency, exec_time
    
    
async def main():
    results = []
    Docker_PID =  #get_container_pid("DB_LOG")
    API_PID =    # Ou find_pid_by_cmdline("API")

    for concurrency in CONCURRENCY_LEVELS:
        stop_event = threading.Event()

        # Inicia monitoramentos em threads
        t1 = threading.Thread(target=lambda: monitoring(Docker_PID, 1, 60, f"DB LOG {concurrency}", 2000, "Docker", stop_event,60))
        t2 = threading.Thread(target=lambda: monitoring(API_PID, 1, 60, f"API {concurrency}", 2000, "other", stop_event,60))
        t1.start()
        t2.start()

        # Executa teste
        time.sleep(60)
        start_time = time.time()
        concurrency, total_time = await run_test(concurrency)
        results.append((concurrency, total_time))
        stop_event.set()
        exec_time = time.time() - start_time
        t1.join()
        t2.join()

    print("\n=== Final results ===")
    for concurrency, total_time in results:
        print(f"Concurrency: {concurrency} | Total Time: {int(total_time)}s")

if __name__ == "__main__":
    asyncio.run(main())
