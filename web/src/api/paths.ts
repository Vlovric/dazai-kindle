import { apiGet, apiPut } from "./client"
import type { FolderPath } from "../dto/paths"

export function getPaths(): Promise<FolderPath[]> {
    return apiGet<FolderPath[]>("/paths")
}

export function updatePath(name: string, path: string): Promise<FolderPath> {
    return apiPut<FolderPath>(`/paths/${name}`, { path })
}
