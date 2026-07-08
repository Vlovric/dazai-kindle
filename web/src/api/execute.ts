import { apiPost, apiPostBlob } from "./client"
import type { CalibrationRunRequest, ExecuteResponse, FullRunRequest, HeadingsRunRequest } from "../dto/execute"

export function executeFull(request: FullRunRequest): Promise<ExecuteResponse> {
    return apiPost<ExecuteResponse>("/execute/full", request)
}

export function executeCalibration(request: CalibrationRunRequest): Promise<{ blob: Blob, filename: string }> {
    return apiPostBlob("/execute/generate", request, "calibration.txt")
}

export function executeHeadings(request: HeadingsRunRequest): Promise<ExecuteResponse> {
    return apiPost<ExecuteResponse>("/execute/headings", request)
}
