import { apiGet, apiPostEmpty } from "./client"
import type { ClippingsInfo } from "../dto/clippings"

export function getClippings(): Promise<ClippingsInfo> {
    return apiGet<ClippingsInfo>("/clippings")
}

export function openClippings(): Promise<void> {
    return apiPostEmpty("/clippings/open")
}
