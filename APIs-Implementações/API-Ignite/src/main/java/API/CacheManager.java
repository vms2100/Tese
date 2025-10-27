package API;

import jakarta.annotation.PostConstruct;
import org.apache.ignite.client.IgniteClient;

import org.apache.ignite.table.Table;
import org.apache.ignite.table.KeyValueView;
import org.apache.ignite.lang.NullableValue;
import org.springframework.stereotype.Service;

import java.io.*;
import static org.springframework.util.SerializationUtils.serialize;

@Service
public class CacheManager {

    private final IgniteClient client;

    public CacheManager() throws Exception {
        this.client = IgniteClient.builder()
                .addresses("127.0.0.1:10800") // ajuste se seu cluster for diferente
                .build();
    }

    /** Retorna uma KeyValueView (API binária, sem limite de tamanho) */
    public KeyValueView<String, byte[]> getCache(String tableName) {
        Table table = client.tables().table(tableName);
        if (table == null) {
            throw new IllegalArgumentException("Tabela não encontrada: " + tableName);
        }
        return table.keyValueView(String.class, byte[].class);
    }

    /** Insere ou substitui um valor serializável na tabela */
    public void put(String tableName, Object key, Serializable value) throws IOException {
        byte[] serializedValue = serialize(value);
        String stringKey = key.toString();
        getCache(tableName).put(null, stringKey, serializedValue);
    }

    /** Insere apenas se a chave ainda não existir */
    public boolean putIfAbsent(String tableName, Object key, Serializable value) throws IOException {
        byte[] serializedValue = serialize(value);
        String stringKey = key.toString();
        KeyValueView<String, byte[]> kv = getCache(tableName);
        return kv.putIfAbsent(null, stringKey, serializedValue);
    }

    /** Recupera o valor, se existir */
    public Serializable getIfPresent(String tableName, Object key)
            throws IOException, ClassNotFoundException {

        if (tableName == null || key == null) {
            throw new IllegalArgumentException("tableName e key não podem ser nulos");
        }

        String stringKey = key.toString();
        KeyValueView<String, byte[]> kv = getCache(tableName);
        NullableValue<byte[]> nullableValue = kv.getNullable(null, stringKey);

        if (nullableValue == null || nullableValue.get() == null) return null;
        return deserialize(nullableValue.get());
    }

    /** Recupera e converte para o tipo especificado */
    public <V extends Serializable> V get(String tableName, Object key, Class<V> valueClass)
            throws IOException, ClassNotFoundException {

        String stringKey = key.toString();
        byte[] bytes = getCache(tableName).get(null, stringKey);
        if (bytes == null) return null;
        return deserialize(bytes, valueClass);
    }

    // ========================
    //   SERIALIZAÇÃO AUXILIAR
    // ========================

    private <V extends Serializable> V deserialize(byte[] data, Class<V> clazz)
            throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream in = new ObjectInputStream(bis)) {
            return clazz.cast(in.readObject());
        }
    }

    private Serializable deserialize(byte[] data)
            throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream in = new ObjectInputStream(bis)) {
            return (Serializable) in.readObject();
        }
    }

    // ========================
    //   CRIAÇÃO DAS TABELAS
    // ========================

    @PostConstruct
    public void initTables() {
        String[] tables = {
                "stockBookCache",
                "BookCache",
                "AuthorCache",
                "ClientCache",
                "BookSearchCache",
                "AuthorSearchCache",
                "ClientSearchCache",
                "BookSCache",
                "ReservationCache"
        };

        for (String table : tables) {
            try {
                client.sql().execute(null, "DROP TABLE IF EXISTS " + table);

                String createSql = "CREATE TABLE IF NOT EXISTS " + table +
                        " (id VARCHAR PRIMARY KEY, data VARBINARY)";
                client.sql().execute(null, createSql);

                System.out.println("Tabela inicializada: " + table);

            } catch (Exception e) {
                System.err.println("Erro ao inicializar a tabela " + table + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
