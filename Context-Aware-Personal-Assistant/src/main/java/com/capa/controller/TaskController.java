package com.capa.controller;

import com.capa.model.LocalTask;
import com.capa.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<LocalTask>> getAllTasks(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(taskService.getTasksForUser(email));
    }

    @PostMapping
    public ResponseEntity<LocalTask> createManual(@AuthenticationPrincipal String email, @RequestBody LocalTask task) {
        return ResponseEntity.ok(taskService.createManualTask(email, task));
    }

    @PostMapping("/nlp")
    public ResponseEntity<LocalTask> createViaNlp(@AuthenticationPrincipal String email, @RequestBody Map<String, String> payload) {
        String prompt = payload.get("text");
        return ResponseEntity.ok(taskService.createAiParsedTask(email, prompt));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<LocalTask> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.toggleTaskStatus(id));
    }
}