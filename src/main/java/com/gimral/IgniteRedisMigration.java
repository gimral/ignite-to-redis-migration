package com.gimral;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ignite.Ignition;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import redis.clients.jedis.Jedis;

public class IgniteRedisMigration {
    public static void main(String[] args) {

        // Connect to the Redis

        // Configure the Ignite Thin Client
        ClientConfiguration cfg = new ClientConfiguration().setAddresses("127.0.0.1:10800");
        // Connect to the Ignite cluster
        try (Jedis jedis = new Jedis("localhost", 6379);
             IgniteClient igniteClient = Ignition.startClient(cfg)) {
            generateDataToRedis(jedis);
            generateDataToIgnite(igniteClient);

            showcaseDifferentUsagePatterns(igniteClient, jedis, 1);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static void generateDataToRedis(Jedis jedis){
        // Generate and store 10 sample Customer objects in Redis
        for (int i = 1; i <= 10; i++) {
            String name = "customer" + i;
            int age = i * 3;

            // Create a JSON representation of the Person object
            String json = "{\"id\":" + i + ",\"name\":\"" + name + "\",\"age\":" + age + "}";

            // Store the JSON string in Redis with a key of "Person:i"
            String key = "customer:" + i;
            jedis.set(key, json);

            System.out.println("Stored in Redis: " + key + " -> " + json);
        }
    }

    private static void generateDataToIgnite(IgniteClient igniteClient){

        // Drop and create the table
        igniteClient.query(new SqlFieldsQuery("DROP TABLE IF EXISTS Customer")).getAll();
        igniteClient.query(new SqlFieldsQuery("CREATE TABLE Customer (\n" +
                "    id INT(11) PRIMARY KEY,\n" +
                "    name VARCHAR(255),\n" +
                "    age INT(11)\n" +
                ") WITH \"CACHE_NAME=Customer\";\n")
        ).getAll();

        // Generate and store 10 sample Customer objects in Ignite
        for (int i = 1; i <= 10; i++) {
            String name = "customer" + i;
            int age = i * 3;

            igniteClient.query(new SqlFieldsQuery(
                    "INSERT INTO Customer (id, name, age) VALUES (?, ?, ?);")
                    .setArgs(i, name, age))
                    .getAll();

            System.out.println("Stored in Ignite: " + i);
        }
    }

    private static void showcaseDifferentUsagePatterns(IgniteClient igniteClient,Jedis jedis, int customerId) throws JsonProcessingException {
        // ObjectMapper for JSON serialization/deserialization
        ObjectMapper objectMapper = new ObjectMapper();

        //Ignite BinaryObject
        ClientCache<Integer, BinaryObject> binaryIgniteCache = igniteClient.getOrCreateCache("Customer").withKeepBinary();
        BinaryObject customerBinaryObject = binaryIgniteCache.get(customerId);
        //Use Account fields from Binary Object
        String customerNameBinaryObject = customerBinaryObject.field("name");
        System.out.println(customerNameBinaryObject);

        //Redis JSONNode
        String customerRedisJsonString = jedis.get("customer:" + customerId);
        JsonNode customerJsonNodeFromRedis = objectMapper.readTree(customerRedisJsonString);
        //Use Account fields from JSONNode
        String customerNameJSONNode = customerJsonNodeFromRedis.get("name").asText();
        System.out.println(customerNameJSONNode);

//        //Ignite Java Object
//        ClientCache<Integer, Customer> objectIgniteCache = igniteClient.getOrCreateCache("Customer");
//        Customer customerObjectIgnite = objectIgniteCache.get(customerId);
//        //Use Account fields from Object
//        String customerNameIgniteObject = customerObjectIgnite.getName();
//        System.out.println(customerNameIgniteObject);
//
//        //Redis Java Object
//        String customerRedisObjectJsonString = jedis.get("customer:" + customerId);
//        Customer customerObjectFromRedis = objectMapper.readValue(customerRedisObjectJsonString, Customer.class);
//        //Use Account fields from JSONNode
//        String customerNameJSONObject = customerObjectFromRedis.getName();
//        System.out.println(customerNameJSONObject);
    }

    private static void retrieveAndDeserializeData(Jedis jedis) {
        // ObjectMapper for JSON serialization/deserialization
        ObjectMapper objectMapper = new ObjectMapper();

        // Retrieve and deserialize Person objects from Redis
        for (int i = 1; i <= 10; i++) {
            String key = "Person:" + i;
            String json = jedis.get(key);

            if (json != null) {
                try {
                    // Deserialize JSON string to Person object
                    Customer customer = objectMapper.readValue(json, Customer.class);
                    System.out.println("Retrieved from Redis: " + key + " -> " + customer);
                } catch (Exception e) {
                    System.err.println("Failed to deserialize JSON for key " + key + ": " + e.getMessage());
                }
            } else {
                System.err.println("Key " + key + " not found in Redis.");
            }
        }
    }
}
