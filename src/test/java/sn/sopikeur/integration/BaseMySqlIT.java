package sn.sopikeur.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseMySqlIT {

    @ServiceConnection
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("sopikeur_it")
        .withUsername("test")
        .withPassword("test");

}
