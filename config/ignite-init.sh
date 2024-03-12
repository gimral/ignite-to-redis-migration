#!/bin/bash

# Wait for Ignite to start
sleep 10

# Define Ignite JDBC connection parameters
IGNITE_URL="jdbc:ignite:thin://localhost/"
IGNITE_USER=""
IGNITE_PASSWORD=""

# Check if the table already exists
TABLE_EXISTS=$(echo "SHOW TABLES LIKE 'Person';" | java -cp /opt/ignite/apache-ignite/libs/ignite-core-${IGNITE_VERSION}.jar:/opt/ignite/apache-ignite/libs/ignite-spring-${IGNITE_VERSION}.jar:/opt/ignite/apache-ignite/libs/ignite-indexing-${IGNITE_VERSION}.jar:/opt/ignite/apache-ignite/libs/ignite-jdbc-${IGNITE_VERSION}.jar:/opt/ignite/apache-ignite/libs/ignite-log4j-${IGNITE_VERSION}.jar:/opt/ignite/apache-ignite/libs/slf4j-api-${SLF4J_VERSION}.jar:/opt/ignite/apache-ignite/libs/slf4j-log4j12-${SLF4J_VERSION}.jar org.apache.ignite.jdbc.IgniteJdbcThinDriver -u "${IGNITE_URL}" -user "${IGNITE_USER}" -password "${IGNITE_PASSWORD}")

# If the table doesn't exist, create it and insert sample data
if [[ "${TABLE_EXISTS}" == "" ]]; then
    SQL_COMMANDS=$(cat <<EOF
CREATE TABLE Person (
    id INT(11) PRIMARY KEY,
    name VARCHAR(255),
    age INT(11)
);

INSERT INTO Person (id, name, age) VALUES (1, 'John Doe', 30);
INSERT INTO Person (id, name, age) VALUES (2, 'Jane Smith', 25);
INSERT INTO Person (id, name, age) VALUES (3, 'Alice Johnson', 35);
EOF
)

    # Execute SQL commands using Ignite JDBC driver
    echo "${SQL_COMMANDS}" | java -cp /opt/ignite/apache-ignite/libs/ignite-core-${IGNITE_VERSION}.jar:/opt/ignite/apache-ignite/libs/ignite-spring-${IGNITE_VERSION}.jar:/opt/ignite/apache-ignite/libs/ignite-indexing-${IGNITE_VERSION}.jar:/opt/ignite/apache-ignite/libs/ignite-jdbc-${IGNITE_VERSION}.jar:/opt/ignite/apache-ignite/libs/ignite-log4j-${IGNITE_VERSION}.jar:/opt/ignite/apache-ignite/libs/slf4j-api-${SLF4J_VERSION}.jar:/opt/ignite/apache-ignite/libs/slf4j-log4j12-${SLF4J_VERSION}.jar org.apache.ignite.jdbc.IgniteJdbcThinDriver -u "${IGNITE_URL}" -user "${IGNITE_USER}" -password "${IGNITE_PASSWORD}"

    echo "Table 'Person' created and sample data inserted."
else
    echo "Table 'Person' already exists. Skipping table creation and data insertion."
fi
