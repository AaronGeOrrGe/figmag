package com.figmine.backend.controller;

import com.figmine.backend.dto.ProjectDto;
import com.figmine.backend.model.User;
import com.figmine.backend.repository.UserRepository;
import com.figmine.backend.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getProjects(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(projectService.getProjects(user));
    }

    @PostMapping
    public ResponseEntity<ProjectDto> createProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ProjectDto dto) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(projectService.createProject(user, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectDto> updateProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody ProjectDto dto) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(projectService.updateProject(user, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        projectService.deleteProject(user, id);
        return ResponseEntity.ok().build();
    }
}
