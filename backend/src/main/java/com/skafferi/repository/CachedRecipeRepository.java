package com.skafferi.repository;

import com.skafferi.domain.CachedRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CachedRecipeRepository extends JpaRepository<CachedRecipe, String> {

    Optional<CachedRecipe> findBySlug(String slug);
}
