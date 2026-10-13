package com.ping.interest;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

@Profile("!bootstrap")
public interface InterestRepository extends JpaRepository<Interest, Long> {
    List<Interest> findAllByOrderByNameAsc();
}
