package com.capa.service;

import com.capa.model.LocalTask;
import com.capa.model.TaskStatus;
import com.capa.model.User;
import com.capa.repository.LocalTaskRepository;
import com.capa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final LocalTaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ContextEngineService contextEngineService;

    public List<LocalTask> getTasksForUser(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            System.out.println("⚠️ Task lookup warning: User record missing in MySQL database for email: " + email);
            return Collections.emptyList();
        }

        return taskRepository.findByUser(userOpt.get());
    }

    public LocalTask createManualTask(String email, LocalTask task) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        task.setUser(user);
        task.setStatus(TaskStatus.PENDING);
        return taskRepository.save(task);
    }

    /**
     * Processes raw sentence strings using Ollama to extract and automatically save a new task.
     */
    public LocalTask createAiParsedTask(String email, String rawSentence) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String aiResponse = contextEngineService.extractTaskFromContext(rawSentence);

        String title = "AI Generated Task";
        String description = rawSentence;
        long daysFromNow = 0;

        try {
            String[] lines = aiResponse.split("\n");
            for (String line : lines) {
                if (line.startsWith("TITLE:")) title = line.replace("TITLE:", "").trim();
                if (line.startsWith("DESCRIPTION:")) description = line.replace("DESCRIPTION:", "").trim();
                if (line.startsWith("DAYS_FROM_NOW:")) daysFromNow = Long.parseLong(line.replace("DAYS_FROM_NOW:", "").trim());
            }
        } catch (Exception e) {
            // Fallback parsing logic if the response format contains structural variations
        }

        LocalTask aiTask = LocalTask.builder()
                .user(user)
                .title(title)
                .description(description)
                .dueDate(LocalDateTime.now().plusDays(daysFromNow))
                .status(TaskStatus.PENDING)
                .build();

        return taskRepository.save(aiTask);
    }

    public LocalTask toggleTaskStatus(Long taskId) {
        LocalTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setStatus(task.getStatus() == TaskStatus.PENDING ? TaskStatus.COMPLETED : TaskStatus.PENDING);
        return taskRepository.save(task);
    }
}