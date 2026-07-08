import { apiDelete, apiGet, apiGetBlob } from "./client"
import type { RunListResponse, RunSortField, SortOrder } from "../dto/runs"

export function listRuns(search: string, sort: RunSortField, order: SortOrder, page: number): Promise<RunListResponse> {
    const params = new URLSearchParams({ sort, order, page: String(page) })
    if (search) {
        params.set("search", search)
    }
    return apiGet<RunListResponse>(`/runs?${params.toString()}`)
}

// Sent as repeated ?names=&names=... params rather than a single comma-joined
// value - a run name (book title) can itself contain a comma (e.g. "80,000
// Hours"), which would be indistinguishable from the delimiter otherwise.
function namesParams(names: string[]): string {
    const params = new URLSearchParams()
    names.forEach((name) => params.append("names", name))
    return params.toString()
}

export function deleteRuns(names: string[]): Promise<void> {
    return apiDelete(`/runs?${namesParams(names)}`)
}

export function exportRuns(names: string[]): Promise<{ blob: Blob, filename: string }> {
    return apiGetBlob(`/runs/export?${namesParams(names)}`, "runs.zip")
}
