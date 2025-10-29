# README - Configuração do Projeto

Para configurar a base de dados PostgreSQL, primeiro instale o PostgreSQL na versão desejada e configure um user com permissões adequadas. 
Em seguida, crie uma database limpa apenas com o nome que deseja.

Depois disso, execute o script `./Databases/DB_schema.sql` para criar todas as tabelas necessárias na database.
O schema da mesma pode ser visualizado em `./Databases/DB_schema.png`. 
Para popular a database, execute um dos scripts `./Databases/gen_Data_XXXX.py`, lembrando que este processo pode demorar bastante, não fornece informações de progresso enquanto roda e a database só terá dados completos quando o script terminar.
Este script requer o módulo `psycopg2` para se conectar à database, e as configurações de conexão devem ser ajustadas conforme sua instalação.

Após a conclusão, teste a conexão com a database para garantir que está funcionando corretamente.

Para configurar a API, faça o build da versão desejada usando Gradle.
Em seguida, configure o arquivo `application.properties` para conectar à database, incluindo URL, user e senha corretos. 
Depois, inicie a API e verifique os endpoints que deseja testar para confirmar que estão funcionando corretamente.

Para a versão da aplicação que utiliza Apache Ignite, os passos de instalação e configuração são bastante diferentes e devem ser seguidos conforme a documentação específica do Ignite.
