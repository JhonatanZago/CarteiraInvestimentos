package com.example.carteirainvestimento.repository;
import com.example.carteirainvestimento.domain.Carteira;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CarteiraRepository extends JpaRepository<Carteira, Long> {
    org.springframework.data.domain.Page<Carteira> findAllByUsuarioId(Long usuarioId, org.springframework.data.domain.Pageable pageable);
    java.util.Optional<Carteira> findByIdAndUsuarioId(Long id, Long usuarioId);
}
