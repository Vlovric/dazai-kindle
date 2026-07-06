import { apiPost } from "./client"
import type { ExecuteResponse, FullRunRequest } from "../dto/execute"

export function executeFull(request: FullRunRequest): Promise<ExecuteResponse> {
    return apiPost<ExecuteResponse>("/execute/full", request)
}
