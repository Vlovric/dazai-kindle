export interface FullRunRequest {
    bookRef: string
    calibrationRef: string
    clippingsRef: string
    templateRef: string
    title?: string
    debugMode: boolean
    overwriteFyodorTemplate: boolean
}

export interface ExecuteResponse {
    runId: string
}

export interface CalibrationRunRequest {
    bookRef: string
    debugMode: boolean
}
