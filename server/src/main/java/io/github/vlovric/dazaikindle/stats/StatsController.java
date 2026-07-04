package io.github.vlovric.dazaikindle.stats;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.vlovric.dazaikindle.stats.dto.StatsResponse;

@RestController
public class StatsController {
    private final StatsService statsService;

    public StatsController(StatsService statsService){
        this.statsService = statsService;
    }


    @GetMapping("/stats")
    public StatsResponse getStats(){
        return statsService.getStats();
    }

}
