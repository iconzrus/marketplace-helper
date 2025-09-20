package com.marketplacehelper.repository;

import com.marketplacehelper.model.DailySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailySnapshotRepository extends JpaRepository<DailySnapshot, Long> {

    List<DailySnapshot> findByWbArticleAndSnapshotDateBetween(String wbArticle, LocalDate from, LocalDate to);

    @Query("SELECT s FROM DailySnapshot s WHERE s.snapshotDate BETWEEN :from AND :to")
    List<DailySnapshot> findAllBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}


