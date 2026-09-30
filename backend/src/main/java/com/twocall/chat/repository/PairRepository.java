package com.twocall.chat.repository;

import com.twocall.chat.domain.entity.Pair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PairRepository extends JpaRepository<Pair, UUID> {
}
