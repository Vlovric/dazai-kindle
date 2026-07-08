package io.github.vlovric.dazaikindle.stats;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import io.github.vlovric.dazaikindle.common.run.RunMetadata;
import io.github.vlovric.dazaikindle.common.run.RunRepository;
import io.github.vlovric.dazaikindle.stats.dto.StatsResponse;

@Service
public class StatsService {

    private final RunRepository runRepository;

    public StatsService(RunRepository runRepository) {
        this.runRepository = runRepository;
    }

    public StatsResponse getStats(){
        return new StatsResponse(
            (int) getHighlightCount(),
            getEntryCount(),
            getLastRunTime()
        );
    }

    private long getHighlightCount(){
        return runRepository.listRunDirs().stream()
            .map(runRepository::readMetadata)
            .flatMap(Optional::stream)
            .mapToLong(RunMetadata::highlightCount)
            .sum();
    }

    private int getEntryCount(){
        return runRepository.listAllDirectories().size();
    }

    private Instant getLastRunTime(){
        return runRepository.listAllArtifactPaths().stream()
            .map(runRepository::lastModified)
            .max(Instant::compareTo)
            .orElse(null);
    }

}
