package org.kandarp;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class StatusController {

    @GetMapping("/api/status")
    public Map<String, Object> status() {
        return Map.of(
                "status", "running",
                "scheduler", "Hybrid PSO-GWO",
                "simulation", "CloudSim Plus",
                "tasks", 0,
                "vms", 4
        );
    }
}