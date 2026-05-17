package ai.antop.index.repository

import ai.antop.index.domain.Site
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
interface SiteRepository : JpaRepository<Site, Long> {
    fun findAllByEnabledTrueOrderByName(): List<Site>

    fun findByUrl(url: String): Site?

    @Query("SELECT s.url FROM Site s")
    fun findAllUrls(): List<String>

    @Modifying
    @Transactional
    @Query("UPDATE Site s SET s.enabled = false, s.updatedAt = :now WHERE s.url IN :urls")
    fun disableByUrls(
        @Param("urls") urls: List<String>,
        @Param("now") now: LocalDateTime,
    )
}
