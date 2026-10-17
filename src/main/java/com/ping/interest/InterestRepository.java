package com.ping.interest;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

@Profile("!bootstrap")
public interface InterestRepository extends JpaRepository<Interest, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths="category")
    List<Interest> findAllByOrderByNameAsc();
    @org.springframework.data.jpa.repository.Query("select i.id from Interest i left join i.category c where c is null or c.active=true")
    List<Long> selectableIds();
}
