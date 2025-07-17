package com.figmine.backend.repository;

import com.figmine.backend.model.Project;
import com.figmine.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOwner(User owner);
    boolean existsByFileUrlAndOwner(String fileUrl, User owner);
}
