package com.example.carteirainvestimento.repository;
import com.example.carteirainvestimento.domain.Venda;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface VendaRepository extends JpaRepository<Venda, Long> {
    List<Venda> findByCarteiraIdOrderByDataVendaDescIdDesc(Long carteiraId);
}
