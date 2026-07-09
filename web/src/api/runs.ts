import { apiDelete, apiGet, apiGetBlob, apiPostEmpty } from "./client"
import type { RunDetail, RunListResponse, RunSortField, SortOrder } from "../dto/runs"

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

export function getRunDetail(name: string): Promise<RunDetail> {
    return apiGet<RunDetail>(`/runs/${encodeURIComponent(name)}`)
}

// Artifact keys ("book", "calibration", ...) are a fixed vocabulary that can
// never contain a comma, so comma-joining them (per the API spec) is safe -
// unlike run names/titles, which needed the repeated-param workaround above.
function commaJoined(key: string, values: string[]): string {
    return `${key}=${values.map(encodeURIComponent).join(",")}`
}

export function deleteArtifacts(runName: string, artifactNames: string[]): Promise<void> {
    return apiDelete(`/runs/${encodeURIComponent(runName)}/artifacts?${commaJoined("names", artifactNames)}`)
}

export function exportArtifacts(runName: string, artifactNames: string[]): Promise<{ blob: Blob, filename: string }> {
    return apiGetBlob(
        `/runs/${encodeURIComponent(runName)}/export?${commaJoined("artifacts", artifactNames)}`,
        `${runName}.zip`
    )
}

export function openArtifact(runName: string, artifactName: string): Promise<void> {
    return apiPostEmpty(`/runs/${encodeURIComponent(runName)}/artifacts/${encodeURIComponent(artifactName)}/open`)
}
