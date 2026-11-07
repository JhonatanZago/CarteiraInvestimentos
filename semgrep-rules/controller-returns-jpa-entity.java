import java.util.List;
import org.springframework.web.bind.annotation.RestController;

class UsuarioResponse {}
class Usuario {}

@RestController
class ControllerReturnsEntityFixture {
    // ruleid: controller-returns-jpa-entity
    Usuario usuario() { return null; }

    // ruleid: controller-returns-jpa-entity
    List<Usuario> usuarios() { return null; }

    // ok: controller-returns-jpa-entity
    UsuarioResponse usuarioResponse() { return null; }
}
