package com.examscheduler.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.examscheduler.realtime.ScheduleEventPublisher;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final ScheduleEventPublisher eventPublisher;

    public EventController(ScheduleEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @GetMapping("/schedules")
    public SseEmitter scheduleEvents() {
        return eventPublisher.subscribe();
    }
}
