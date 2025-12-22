# README - Configuração do Projeto


## Base de Dados
O SGBD usada neste projeto é o postgresql 15 disponivel no docker desktop.

Apos a instalação da mesma e necessario a configuraçao minima, portas users permissoes.

Ligue-se a database com user permitido.
Exemplo de comando: "psql -U user -d database"

Crie uma base de dados limpa ou ja com o nome final pretendido.
Depois disso, execute o script sql `./Databases/DB_schema.sql` para criar todas as tabelas necessárias na database.
Exemplo de comando:"\i ./Databases/DB_schema.sql" ou "psql -U user -d database -f ./Databases/DB_schema.sql"

O schema da mesma pode ser visualizado em `./Databases/DB_schema.png`.
 
Base de dados criada agora vamos popular-la.
Edite os scripts `./Databases/gen_Data_XXXX.py` com as informações da sua Base de dados, ip,porta user,password,database.
Apos editar guarde e corra os scripts.
Lembrando que este processo pode demorar bastante (Horas ate), não fornece informações de progresso enquanto roda e a database só terá dados completos quando o script terminar.
Este script requer o módulo `psycopg2` para se conectar à database.

Após a conclusão, teste a conexão com a database para garantir que está funcionando corretamente.


## API 
Para configurar a API, faça o build da versão desejada usando Gradle.
As versões da api estao na pasta "./APIs-Implementações/".
Em seguida, configure o arquivo `application.properties` para conectar à database, incluindo ip, user e senha corretos. 
Depois, inicie a API e verifique os endpoints que deseja testar para confirmar que estão funcionando corretamente.
A api corre em springboot nativo.
Nos testes executados foi usado java 21 e com estes parametros  "-Xms4G -Xmx8G -Dapp.name=API -XX:ActiveProcessorCount=4 -XX:ParallelGCThreads=4 " para controlar os recursos usados pea mesma.
Para a versão da aplicação que utiliza Apache Ignite, os passos de instalação e configuração são bastante diferentes e devem ser seguidos conforme a documentação específica do Ignite.



## Scripts e suas funções

Em todos os scripts por conta das metricas e preciso encontra o pid de certos serviços como do Docker da Database e da propria api em java por ultimo o pid do ignite caso seja essa a ser usada.

...\Scripts\Testar_Concorrencia\testar_concorrencia.py	 -> Testa a API para todas as concorrencias indicadas. (É preciso editar PID'S e concorrencias)

...\Scripts\Testes\Run_Final.py ->  Testa a api para a concorrencia pretendida (usada para testar toda a API cold e operational mode).(É preciso editar PID'S e concorrencias)

...\Scripts\...\Monitoring.py -> Neste script é preciso adaptar os recursos consoante os usados no testes (cores/ram usados no teste).
Exemplo: 
	"if object_type == 'Docker':
        cpu = (cpu / (4 * 100)) * 100
    else:
        cpu = (cpu / (4 * 100)) * 100"

...\csv_metrics\api_metrics.py-> Este script cria graficos com a media de tempo por processo interno da api por endpoint, exemplo tempo de parser retorno da api etc, para isso apos cada run de testes deve-se apagar o csv criado pela api.


## Concorrencia

A pasta Concorrencia possui 10 pastas dentro dela variando entre 10 e 100 cada pasta desta corresponde a um nivel de concorrencia e dentro delas todos os graficos criados para essa concorrencia.

## Raw Results

Esta pasta tem os dados brutos tirados de todas as execuções e cenarios da API.

## Resultados_Finais

Esta pasta tem os graficos de dispersao e barras sobre as caches.