package com.capa.repository;

import com.capa.model.LocalTask;
import com.capa.model.User;
import com.capa.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LocalTaskRepository extends JpaRepository<LocalTask, Long> {
    List<LocalTask> findByUser(User user);
    List<LocalTask> findByUserAndStatus(User user, TaskStatus status);
}