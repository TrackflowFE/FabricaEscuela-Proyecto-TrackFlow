package com.trackflow.bootstrap.security;

import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Usuario desactivado para probar el criterio de HU-08 "un usuario desactivado es
 * rechazado con el mismo mensaje genérico". Mientras no exista la HU-12 no hay forma
 * de desactivar a nadie desde la API, así que se deja uno ya desactivado.
 *
 * Solo existe con los datos semilla activos, igual que los envíos de prueba. Su clave no
 * está en el código: llega de {@code trackflow.seed.inactivo-clave} (variable de entorno
 * TRACKFLOW_SEED_INACTIVO_CLAVE) para quien quiera probar el rechazo con la clave correcta;
 * si no se define, se genera una al azar y nadie la conoce. En ambos casos el usuario está
 * desactivado, así que nunca puede iniciar sesión.
 */
@Component
@ConditionalOnProperty(name = "trackflow.seed.enabled", havingValue = "true")
public class UsuarioInactivoDePrueba implements ApplicationRunner {

    public static final String USUARIO = "inactivo";

    private static final Logger log = LoggerFactory.getLogger(UsuarioInactivoDePrueba.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final String clave;

    public UsuarioInactivoDePrueba(UsuarioRepository usuarios, PasswordEncoder encoder, Clock clock,
            @Value("${trackflow.seed.inactivo-clave:}") String clave) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.clock = clock;
        this.clave = clave == null || clave.isBlank() ? UUID.randomUUID().toString() : clave;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.existsById(USUARIO)) {
            return;
        }

        usuarios.save(new Usuario(USUARIO, encoder.encode(clave), SecurityProperties.ROL_OPERADOR, false,
                clock.instant()));
        log.info("Usuario de prueba '{}' creado desactivado", USUARIO);
    }
}
