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
