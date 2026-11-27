package com.exchangepro.instrument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InstrumentRepository extends JpaRepository<Instrument, UUID> {

    Optional<Instrument> findBySymbol(String symbol);

    List<Instrument> findByActiveTrueOrderBySymbol();

    /**
     * Case-insensitive search on symbol and name. Ranking: exact symbol, then symbol prefix, then everything else
     * alphabetically. {@code :pattern} is already lower-cased, LIKE-escaped and wrapped in %...%.
     */
    @Query(value = """
            select i from Instrument i
             where i.active = true
               and (:sector is null or i.sector = :sector)
               and (lower(i.symbol) like :pattern escape '\\' or lower(i.name) like :pattern escape '\\')
             order by case when lower(i.symbol) = :exact then 0
                           when lower(i.symbol) like :prefix escape '\\' then 1
                           else 2 end,
                      i.symbol""",
            countQuery = """
            select count(i) from Instrument i
             where i.active = true
               and (:sector is null or i.sector = :sector)
               and (lower(i.symbol) like :pattern escape '\\' or lower(i.name) like :pattern escape '\\')""")
    Page<Instrument> search(@Param("pattern") String pattern, @Param("prefix") String prefix,
                            @Param("exact") String exact, @Param("sector") String sector, Pageable pageable);

    @Query("select distinct i.sector from Instrument i where i.active = true order by i.sector")
    List<String> findActiveSectors();
}
