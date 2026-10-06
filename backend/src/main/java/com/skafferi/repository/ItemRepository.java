package com.skafferi.repository;

import com.skafferi.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, String> {
    
    Optional<Item> findByBarcode(String barcode);

    List<Item> findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(String name, String brand);

    @Query("SELECT i FROM Item i WHERE LOWER(i.name) = LOWER(:name)")
    Optional<Item> findByNameIgnoreCase(@Param("name") String name);
}
