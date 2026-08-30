package com.example.carteirainvestimento.repository;
import com.example.carteirainvestimento.domain.Carteira;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CarteiraRepository extends JpaRepository<Carteira, Long> {}
