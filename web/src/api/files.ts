import { apiGet, apiPostForm } from "./client"
import type { ArtifactType, FileListResponse, UploadedFileResponse } from "../dto/files"

const UPLOAD_PATH: Record<ArtifactType, string> = {
    book: "/files/book",
    calibration: "/files/calibration",
    template: "/files/template",
    headingsTemplate: "/files/headingsTemplate",
}

// GET /files?type= uses "headingTemplate" (no "s") - a naming mismatch with the
// upload path's "headingsTemplate" that already exists in the backend contract.
const QUERY_TYPE: Record<ArtifactType, string> = {
    book: "book",
    calibration: "calibration",
    template: "template",
    headingsTemplate: "headingTemplate",
}

export function uploadArtifact(type: ArtifactType, file: File, draftId?: string | null): Promise<UploadedFileResponse> {
    const form = new FormData()
    form.append("file", file)
    if (draftId) {
        form.append("draftId", draftId)
    }
    return apiPostForm<UploadedFileResponse>(UPLOAD_PATH[type], form)
}

export function uploadClippings(file: File): Promise<UploadedFileResponse> {
    const form = new FormData()
    form.append("file", file)
    return apiPostForm<UploadedFileResponse>("/files/clippings", form)
}

export function listFiles(type: ArtifactType, search: string, page: number): Promise<FileListResponse> {
    const params = new URLSearchParams({ type: QUERY_TYPE[type], page: String(page) })
    if (search) {
        params.set("search", search)
    }
    return apiGet<FileListResponse>(`/files?${params.toString()}`)
}
