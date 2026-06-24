package io.github.vlovric.dazaikindle.calibration;

/**
 * Result of a least-squares calibration fit.
 * @param bytesPerLocation calibrated bytes per Kindle location
 * @param locationBias     y-intercept bias from the linear fit
 * @param pointsUsed       number of calibration points actually used in the fit
 * @param rmseLocations    root mean square error of the fit in Kindle locations
 */
public record CalibrationFit(
    double bytesPerLocation,
    double locationBias,
    int    pointsUsed,
    double rmseLocations
) {}
