package com.ping.interest;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
@Profile("!bootstrap") public interface InterestCatalogStateRepository extends JpaRepository<InterestCatalogState,Long>{
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from InterestCatalogState s where s.id=1") InterestCatalogState lockCatalog();
}
