package io.github.vlovric.dazaikindle.clippings;

import java.awt.Desktop;
import java.io.IOException;

import org.springframework.stereotype.Service;

import io.github.vlovric.dazaikindle.clippings.dto.ClippingsResponse;
import io.github.vlovric.dazaikindle.clippings.exceptions.ClippingsNotFoundException;
import io.github.vlovric.dazaikindle.clippings.exceptions.ClippingsOpenException;
import io.github.vlovric.dazaikindle.common.clippings.ClippingsRepository;

@Service
public class ClippingsService {

    private final ClippingsRepository clippingsRepository;

    public ClippingsService(ClippingsRepository clippingsRepository) {
        this.clippingsRepository = clippingsRepository;
    }

    public ClippingsResponse getClippings() {
        requireUploaded();
        return new ClippingsResponse(
            clippingsRepository.path().getFileName().toString(),
            clippingsRepository.lastModified(),
            clippingsRepository.path().toAbsolutePath().toString()
        );
    }

    /** Opens the clippings file's containing folder, not the file itself - same as run artifacts. */
    public void openClippings() {
        requireUploaded();
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            throw new ClippingsOpenException("This server has no desktop environment available to open files with");
        }
        try {
            Desktop.getDesktop().open(clippingsRepository.path().getParent().toFile());
        } catch (IOException e) {
            throw new ClippingsOpenException("Failed to open folder: " + e.getMessage());
        }
    }

    private void requireUploaded() {
        if (!clippingsRepository.exists()) {
            throw new ClippingsNotFoundException();
        }
    }
}
